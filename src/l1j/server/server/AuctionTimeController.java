/**
 *                            License
 * THE WORK (AS DEFINED BELOW) IS PROVIDED UNDER THE TERMS OF THIS  
 * CREATIVE COMMONS PUBLIC LICENSE ("CCPL" OR "LICENSE"). 
 * THE WORK IS PROTECTED BY COPYRIGHT AND/OR OTHER APPLICABLE LAW.  
 * ANY USE OF THE WORK OTHER THAN AS AUTHORIZED UNDER THIS LICENSE OR  
 * COPYRIGHT LAW IS PROHIBITED.
 * 
 * BY EXERCISING ANY RIGHTS TO THE WORK PROVIDED HERE, YOU ACCEPT AND  
 * AGREE TO BE BOUND BY THE TERMS OF THIS LICENSE. TO THE EXTENT THIS LICENSE  
 * MAY BE CONSIDERED TO BE A CONTRACT, THE LICENSOR GRANTS YOU THE RIGHTS CONTAINED 
 * HERE IN CONSIDERATION OF YOUR ACCEPTANCE OF SUCH TERMS AND CONDITIONS.
 * 
 */
package l1j.server.server;

import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.TimeZone;

import l1j.server.Config;
import l1j.server.server.datatables.AuctionBoardTable;
import l1j.server.server.datatables.ClanTable;
import l1j.server.server.datatables.HouseTable;
import l1j.server.server.datatables.ItemTable;
import l1j.server.server.model.L1Clan;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.identity.L1ItemId;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.storage.CharactersItemStorage;
import l1j.server.server.templates.L1AuctionBoard;
import l1j.server.server.templates.L1House;

/**
 * 血盟小屋拍賣時間控制器
 * <p>定期檢查拍賣截止時間並處理競標結果，管理血盟小屋的所有權轉移。
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>每分鐘檢查所有進行中的拍賣是否到期</li>
 *   <li>處理拍賣結束後的金額結算</li>
 *   <li>管理血盟小屋所有權轉移</li>
 *   <li>發送競標結果通知給相關玩家</li>
 *   <li>處理流標情況 (無人得標或取消拍賣)</li>
 * </ul>
 *
 * <h3>拍賣結果處理:</h3>
 * <ul>
 *   <li><b>正常成交:</b> 前擁有者獲得 90% 成交價，得標者取得小屋所有權</li>
 *   <li><b>無前擁有者:</b> 得標者直接取得小屋所有權</li>
 *   <li><b>流標 (有前擁有者):</b> 所有權退還給前擁有者</li>
 *   <li><b>流標 (無擁有者):</b> 5 天後自動重新開始拍賣</li>
 * </ul>
 *
 * <h3>手續費:</h3>
 * <p>拍賣成交時收取 10% 手續費，前擁有者實際收到成交價的 90%。
 *
 * @see AuctionBoardTable
 * @see L1AuctionBoard
 * @see HouseTable
 * @see L1House
 */
public class AuctionTimeController implements Runnable {
	/** 日誌記錄器 */
	private static Logger _log = Logger.getLogger(AuctionTimeController.class
			.getName());

	/** Singleton 實例 */
	private static AuctionTimeController _instance;

	/**
	 * 取得 Singleton 實例
	 *
	 * @return AuctionTimeController 實例
	 */
	public static AuctionTimeController getInstance() {
		if (_instance == null) {
			_instance = new AuctionTimeController();
		}
		return _instance;
	}

	/**
	 * 執行拍賣時間檢查的主迴圈
	 * <p>每 60 秒 (1 分鐘) 檢查一次所有拍賣的截止時間。
	 *
	 * <h3>執行流程:</h3>
	 * <ol>
	 *   <li>呼叫 {@link #checkAuctionDeadline()} 檢查拍賣截止時間</li>
	 *   <li>休眠 60 秒</li>
	 *   <li>重複上述步驟，持續運行</li>
	 * </ol>
	 *
	 * <p><b>注意:</b> 此方法會無限循環執行，應在獨立執行緒中運行。
	 */
	@Override
	public void run() {
		try {
			while (true) {
				checkAuctionDeadline();
				Thread.sleep(60000);
			}
		} catch (Exception e1) {
		}
	}

	/**
	 * 取得當前真實時間
	 * <p>根據伺服器設定的時區 ({@link Config#TIME_ZONE}) 取得當前時間。
	 *
	 * @return 當前時間的 Calendar 物件
	 */
	public Calendar getRealTime() {
		TimeZone tz = TimeZone.getTimeZone(Config.TIME_ZONE);
		Calendar cal = Calendar.getInstance(tz);
		return cal;
	}

	/**
	 * 檢查所有拍賣的截止時間
	 * <p>遍歷所有拍賣告示，若截止時間已到，則呼叫 {@link #endAuction(L1AuctionBoard)} 結束拍賣。
	 *
	 * @see #endAuction(L1AuctionBoard)
	 */
	private void checkAuctionDeadline() {
		AuctionBoardTable boardTable = new AuctionBoardTable();
		for (L1AuctionBoard board : boardTable.getAuctionBoardTableList()) {
			if (board.getDeadline().before(getRealTime())) {
				endAuction(board);
			}
		}
	}

	/**
	 * 結束拍賣並處理競標結果
	 * <p>根據前擁有者及得標者的存在情況，執行不同的結算邏輯。
	 *
	 * <h3>處理情境:</h3>
	 * <ol>
	 *   <li><b>前擁有者與得標者都存在:</b>
	 *     <ul>
	 *       <li>前擁有者收到成交價的 90% (扣除 10% 手續費)</li>
	 *       <li>得標者取得小屋所有權</li>
	 *       <li>發送通知訊息給雙方</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>僅有得標者 (無前擁有者):</b>
	 *     <ul>
	 *       <li>得標者直接取得小屋所有權</li>
	 *       <li>發送得標通知</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>僅有前擁有者 (無人得標):</b>
	 *     <ul>
	 *       <li>所有權退還給前擁有者</li>
	 *       <li>發送流標通知</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>無前擁有者也無得標者:</b>
	 *     <ul>
	 *       <li>設定 5 天後重新開始拍賣</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 *
	 * @param board 拍賣告示板資料
	 * @see #deleteHouseInfo(int)
	 * @see #setHouseInfo(int, int)
	 * @see #deleteNote(int)
	 */
	private void endAuction(L1AuctionBoard board) {
		int houseId = board.getHouseId();
		int price = board.getPrice();
		int oldOwnerId = board.getOldOwnerId();
		String bidder = board.getBidder();
		int bidderId = board.getBidderId();

		if (oldOwnerId != 0 && bidderId != 0) { // 在前主人與得標者都存在的情況下
			L1PcInstance oldOwnerPc = (L1PcInstance) L1World.getInstance()
					.findObject(oldOwnerId);
			int payPrice = (int) (price * 0.9);
			if (oldOwnerPc != null) { // 如果有前主人
				oldOwnerPc.getInventory().storeItem(L1ItemId.ADENA, payPrice);
				// あなたが所有していた家が最終価格%1アデナで落札されました。%n
				// 手数料10%%を除いた残りの金額%0アデナを差し上げます。%nありがとうございました。%n%n
				oldOwnerPc.sendPackets(new S_ServerMessage(527, String
						.valueOf(payPrice)));
			} else { // 沒有前主人
				L1ItemInstance item = ItemTable.getInstance().createItem(
						L1ItemId.ADENA);
				item.setCount(payPrice);
				try {
					CharactersItemStorage storage = CharactersItemStorage
							.create();
					storage.storeItem(oldOwnerId, item);
				} catch (Exception e) {
					_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
				}
			}

			L1PcInstance bidderPc = (L1PcInstance) L1World.getInstance()
					.findObject(bidderId);
			if (bidderPc != null) { // 如果有得標者
				// おめでとうございます。%nあなたが参加された競売は最終価格%0アデナの価格で落札されました。%n
				// 様がご購入された家はすぐにご利用できます。%nありがとうございました。%n%n
				bidderPc.sendPackets(new S_ServerMessage(524, String
						.valueOf(price), bidder));
			}
			deleteHouseInfo(houseId);
			setHouseInfo(houseId, bidderId);
			deleteNote(houseId);
		} else if (oldOwnerId == 0 && bidderId != 0) { // 在先前的擁有者沒有中標
			L1PcInstance bidderPc = (L1PcInstance) L1World.getInstance()
					.findObject(bidderId);
			if (bidderPc != null) { // 落札者がオンライン中
				// おめでとうございます。%nあなたが参加された競売は最終価格%0アデナの価格で落札されました。%n
				// 様がご購入された家はすぐにご利用できます。%nありがとうございました。%n%n
				bidderPc.sendPackets(new S_ServerMessage(524, String
						.valueOf(price), bidder));
			}
			
			setHouseInfo(houseId, bidderId);
			deleteNote(houseId);
		} else if (oldOwnerId != 0 && bidderId == 0) { // 以前沒有人成功競投無
			L1PcInstance oldOwnerPc = (L1PcInstance) L1World.getInstance()
					.findObject(oldOwnerId);
			if (oldOwnerPc != null) { // 以前の所有者がオンライン中
				// あなたが申請なさった競売は、競売期間内に提示した金額以上での支払いを表明した方が現れなかったため、結局取り消されました。%n
				// 従って、所有権があなたに戻されたことをお知らせします。%nありがとうございました。%n%n
				oldOwnerPc.sendPackets(new S_ServerMessage(528));
			}
			deleteNote(houseId);
		} else if (oldOwnerId == 0 && bidderId == 0) { // 在先前的擁有者沒有中標
			// 設定五天之後再次競標
			Calendar cal = getRealTime();
			cal.add(Calendar.DATE, 5); // 5天後
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			board.setDeadline(cal);
			AuctionBoardTable boardTable = new AuctionBoardTable();
			boardTable.updateAuctionBoard(board);
		}
	}

	/**
	 * 取消血盟小屋的所有權
	 * <p>遍歷所有血盟，將擁有指定小屋的血盟之小屋 ID 設為 0。
	 *
	 * @param houseId 血盟小屋的編號
	 * @see L1Clan#setHouseId(int)
	 * @see ClanTable#updateClan(L1Clan)
	 */
	private void deleteHouseInfo(int houseId) {
		for (L1Clan clan : L1World.getInstance().getAllClans()) {
			if (clan.getHouseId() == houseId) {
				clan.setHouseId(0);
				ClanTable.getInstance().updateClan(clan);
			}
		}
	}

	/**
	 * 設定得標者血盟的小屋所有權
	 * <p>找出得標者所屬的血盟 (得標者必須是血盟盟主)，並將小屋 ID 設定給該血盟。
	 *
	 * @param houseId 血盟小屋的編號
	 * @param bidderId 得標者 (血盟盟主) 的角色 ID
	 * @see L1Clan#setHouseId(int)
	 * @see L1Clan#getLeaderId()
	 * @see ClanTable#updateClan(L1Clan)
	 */
	private void setHouseInfo(int houseId, int bidderId) {
		for (L1Clan clan : L1World.getInstance().getAllClans()) {
			if (clan.getLeaderId() == bidderId) {
				clan.setHouseId(houseId);
				ClanTable.getInstance().updateClan(clan);
				break;
			}
		}
	}

	/**
	 * 取消血盟小屋的拍賣告示並設定為不拍賣狀態
	 * <p>執行拍賣結束後的清理工作。
	 *
	 * <h3>處理流程:</h3>
	 * <ol>
	 *   <li>將小屋狀態設為不拍賣 ({@code setOnSale(false)})</li>
	 *   <li>設定下次繳稅期限 (依據 {@link Config#HOUSE_TAX_INTERVAL} 設定)</li>
	 *   <li>更新小屋資料到資料庫</li>
	 *   <li>從拍賣告示板刪除該拍賣記錄</li>
	 * </ol>
	 *
	 * @param houseId 血盟小屋的編號
	 * @see L1House#setOnSale(boolean)
	 * @see L1House#setTaxDeadline(Calendar)
	 * @see HouseTable#updateHouse(L1House)
	 * @see AuctionBoardTable#deleteAuctionBoard(int)
	 */
	private void deleteNote(int houseId) {
		// 將血盟小屋的狀態設定為不拍賣
		L1House house = HouseTable.getInstance().getHouseTable(houseId);
		house.setOnSale(false);
		Calendar cal = getRealTime();
		cal.add(Calendar.DATE, Config.HOUSE_TAX_INTERVAL);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		house.setTaxDeadline(cal);
		HouseTable.getInstance().updateHouse(house);

		// 取消拍賣告示
		AuctionBoardTable boardTable = new AuctionBoardTable();
		boardTable.deleteAuctionBoard(houseId);
	}

}
