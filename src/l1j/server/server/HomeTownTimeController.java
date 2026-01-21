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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.datatables.TownTable;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.gametime.L1GameTime;
import l1j.server.server.model.gametime.L1GameTimeAdapter;
import l1j.server.server.model.gametime.L1GameTimeClock;
import l1j.server.server.serverpackets.S_PacketBox;
import l1j.server.server.utils.SQLUtil;

/**
 * 城鎮 (HomeTown) 時間控制器
 * <p>管理城鎮系統的每日和每月定時任務，包括稅率更新、貢獻度結算及城主選舉。
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li><b>每日處理 (Daily):</b> 每個遊戲日執行
 *     <ul>
 *       <li>更新城鎮稅率</li>
 *       <li>更新昨日銷售額</li>
 *       <li>重新載入城鎮資料</li>
 *     </ul>
 *   </li>
 *   <li><b>每月處理 (Monthly):</b> 每月 25 日執行
 *     <ul>
 *       <li>結算所有城鎮的貢獻度</li>
 *       <li>選出各城鎮的新城主（貢獻度最高者）</li>
 *       <li>分配貢獻獎勵給所有貢獻者</li>
 *       <li>重置城鎮稅率及銷售資料</li>
 *       <li>清空玩家貢獻度</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>城鎮系統說明:</h3>
 * <p>玩家可以選擇一個城鎮作為家鄉 (HomeTown)，透過繳納稅金來累積貢獻度。
 * 每月結算時，貢獻度最高的玩家將成為該城鎮的新城主，所有貢獻者會根據貢獻比例
 * 獲得城鎮固定稅收的分配獎勵。
 *
 * <h3>貢獻獎勵計算:</h3>
 * <pre>
 * 單位獎勵 = 城鎮固定稅收 ÷ 總貢獻度
 * 玩家獎勵 = 玩家貢獻度 × 單位獎勵
 * </pre>
 *
 * <h3>城鎮 ID 範圍:</h3>
 * <p>系統管理 10 個城鎮 (townId: 1-10)。
 *
 * @see TownTable
 * @see L1GameTimeClock
 * @see L1PcInstance#getHomeTownId()
 * @see L1PcInstance#getContribution()
 */
public class HomeTownTimeController {
	/** 日誌記錄器 */
	private static Logger _log = Logger.getLogger(HomeTownTimeController.class
			.getName());

	/** Singleton 實例 */
	private static HomeTownTimeController _instance;

	/**
	 * 取得 Singleton 實例
	 *
	 * @return HomeTownTimeController 實例
	 */
	public static HomeTownTimeController getInstance() {
		if (_instance == null) {
			_instance = new HomeTownTimeController();
		}
		return _instance;
	}

	/**
	 * 私有建構子，初始化時間監聽器
	 * <p>啟動遊戲時間監聽器，監聽每日變更事件。
	 */
	private HomeTownTimeController() {
		startListener();
	}

	/** 遊戲時間監聽器實例 */
	private static L1TownFixedProcListener _listener;

	/**
	 * 啟動遊戲時間監聽器
	 * <p>註冊監聽器到遊戲時間時鐘，監聽每日變更事件。
	 */
	private void startListener() {
		if (_listener == null) {
			_listener = new L1TownFixedProcListener();
			L1GameTimeClock.getInstance().addListener(_listener);
		}
	}

	/**
	 * 遊戲時間監聽器內部類別
	 * <p>監聽遊戲日期變更事件，觸發城鎮系統的定時處理。
	 */
	private class L1TownFixedProcListener extends L1GameTimeAdapter {
		/**
		 * 當遊戲日期變更時觸發
		 * <p>呼叫 {@link #fixedProc(L1GameTime)} 執行每日或每月處理。
		 *
		 * @param time 當前遊戲時間
		 */
		@Override
		public void onDayChanged(L1GameTime time) {
			fixedProc(time);
		}
	}

	/**
	 * 執行城鎮系統的定時處理
	 * <p>根據遊戲日期判斷執行每日處理或每月處理。
	 *
	 * <h3>處理規則:</h3>
	 * <ul>
	 *   <li>每月 25 日：執行 {@link #monthlyProc()} 月結處理</li>
	 *   <li>其他日期：執行 {@link #dailyProc()} 每日處理</li>
	 * </ul>
	 *
	 * @param time 當前遊戲時間
	 */
	private void fixedProc(L1GameTime time) {
		Calendar cal = time.getCalendar();
		int day = cal.get(Calendar.DAY_OF_MONTH);

		if (day == 25) {
			monthlyProc();
		} else {
			dailyProc();
		}
	}

	/**
	 * 執行城鎮系統的每日處理
	 * <p>每個遊戲日執行一次，更新城鎮相關資料。
	 *
	 * <h3>處理內容:</h3>
	 * <ol>
	 *   <li>更新城鎮稅率 (套用預定稅率)</li>
	 *   <li>更新昨日銷售額 (將今日銷售額移至昨日)</li>
	 *   <li>重新載入城鎮資料</li>
	 * </ol>
	 *
	 * @see TownTable#updateTaxRate()
	 * @see TownTable#updateSalesMoneyYesterday()
	 */
	public void dailyProc() {
		_log.info("城鎮系統：開始處理每日事項");
		TownTable.getInstance().updateTaxRate();
		TownTable.getInstance().updateSalesMoneyYesterday();
		TownTable.getInstance().load();
	}

	/**
	 * 執行城鎮系統的每月處理
	 * <p>每月 25 日執行一次，進行貢獻度結算及城主選舉。
	 *
	 * <h3>處理流程:</h3>
	 * <ol>
	 *   <li><b>設定處理標記:</b> 防止貢獻度在處理期間變動</li>
	 *   <li><b>儲存線上玩家資料:</b> 確保資料完整性</li>
	 *   <li><b>處理所有城鎮 (1-10):</b>
	 *     <ul>
	 *       <li>呼叫 {@link #totalContribution(int)} 結算該城鎮貢獻度</li>
	 *       <li>選出新城主 (貢獻度最高者)</li>
	 *       <li>分配獎勵給所有貢獻者</li>
	 *       <li>發送城主選舉通知給該城鎮玩家</li>
	 *       <li>清空該城鎮玩家的貢獻度</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>重新載入城鎮資料:</b> 套用新的城主及稅率資料</li>
	 *   <li><b>清理玩家資料:</b>
	 *     <ul>
	 *       <li>將 HomeTownId = -1 的玩家重置為 0</li>
	 *       <li>清空所有玩家的貢獻度</li>
	 *       <li>儲存玩家資料</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>清理資料庫:</b> 呼叫 {@link #clearHomeTownID()} 清理離線玩家資料</li>
	 *   <li><b>清除處理標記:</b> 解除貢獻度鎖定</li>
	 * </ol>
	 *
	 * <p><b>注意:</b> 處理期間設定 {@code ProcessingContributionTotal} 標記，
	 * 防止玩家貢獻度在結算過程中變動。
	 *
	 * @see #totalContribution(int)
	 * @see #clearHomeTownID()
	 * @see L1World#setProcessingContributionTotal(boolean)
	 */
	public void monthlyProc() {
		_log.info("城鎮系統：開始處理每月事項");
		L1World.getInstance().setProcessingContributionTotal(true);
		Collection<L1PcInstance> players = L1World.getInstance().getAllPlayers();
		for (L1PcInstance pc : players) {
			try {
				// 儲存所有線上玩家的資訊
				pc.save();
			} catch (Exception e) {
				_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}

		for (int townId = 1; townId <= 10; townId++) {
			String leaderName = totalContribution(townId);
			if (leaderName != null) {
				S_PacketBox packet = new S_PacketBox(S_PacketBox.MSG_TOWN_LEADER, leaderName);
				for (L1PcInstance pc : players) {
					if (pc.getHomeTownId() == townId) {
						pc.setContribution(0);
						pc.sendPackets(packet);
					}
				}
			}
		}
		TownTable.getInstance().load();

		for (L1PcInstance pc : players) {
			if (pc.getHomeTownId() == -1) {
				pc.setHomeTownId(0);
			}
			pc.setContribution(0);
			try {
				// 儲存所有線上玩家的資訊
				pc.save();
			} catch (Exception e) {
				_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
		clearHomeTownID();
		L1World.getInstance().setProcessingContributionTotal(false);
	}

	/**
	 * 結算指定城鎮的貢獻度並選出新城主
	 * <p>執行貢獻度結算、城主選舉及獎勵分配的核心邏輯。
	 *
	 * <h3>處理流程:</h3>
	 * <ol>
	 *   <li><b>選出新城主:</b> 查詢該城鎮貢獻度最高的玩家</li>
	 *   <li><b>計算總貢獻度:</b> 加總該城鎮所有玩家的貢獻度</li>
	 *   <li><b>查詢城鎮固定稅收:</b> 取得本月累積的城鎮稅收</li>
	 *   <li><b>計算單位獎勵:</b>
	 *     <pre>單位獎勵 = 城鎮固定稅收 ÷ 總貢獻度</pre>
	 *   </li>
	 *   <li><b>分配獎勵:</b> 更新所有玩家的 Pay 欄位
	 *     <pre>玩家獎勵 = 玩家貢獻度 × 單位獎勵</pre>
	 *   </li>
	 *   <li><b>清空玩家貢獻度:</b> 將所有玩家的 Contribution 重置為 0</li>
	 *   <li><b>更新城鎮資料:</b> 設定新城主、重置稅率及銷售資料</li>
	 * </ol>
	 *
	 * <h3>資料庫更新:</h3>
	 * <ul>
	 *   <li><b>characters 表:</b> 更新 Pay、Contribution</li>
	 *   <li><b>town 表:</b> 更新 leader_id、leader_name、稅率、銷售額</li>
	 * </ul>
	 *
	 * @param townId 城鎮 ID (1-10)
	 * @return 新城主的角色名稱，若無貢獻者則返回 {@code null}
	 */
	private static String totalContribution(int townId) {
		Connection con = null;
		PreparedStatement pstm1 = null;
		ResultSet rs1 = null;
		PreparedStatement pstm2 = null;
		ResultSet rs2 = null;
		PreparedStatement pstm3 = null;
		ResultSet rs3 = null;
		PreparedStatement pstm4 = null;
		PreparedStatement pstm5 = null;

		int leaderId = 0;
		String leaderName = null;

		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm1 = con.prepareStatement("SELECT objid, char_name FROM characters WHERE HomeTownID = ? ORDER BY Contribution DESC");

			pstm1.setInt(1, townId);
			rs1 = pstm1.executeQuery();

			if (rs1.next()) {
				leaderId = rs1.getInt("objid");
				leaderName = rs1.getString("char_name");
			}

			double totalContribution = 0;
			pstm2 = con.prepareStatement("SELECT SUM(Contribution) AS TotalContribution FROM characters WHERE HomeTownID = ?");
			pstm2.setInt(1, townId);
			rs2 = pstm2.executeQuery();
			if (rs2.next()) {
				totalContribution = rs2.getInt("TotalContribution");
			}

			double townFixTax = 0;
			pstm3 = con.prepareStatement("SELECT town_fix_tax FROM town WHERE town_id = ?");
			pstm3.setInt(1, townId);
			rs3 = pstm3.executeQuery();
			if (rs3.next()) {
				townFixTax = rs3.getInt("town_fix_tax");
			}

			double contributionUnit = 0;
			if (totalContribution != 0) {
				contributionUnit = Math.floor(townFixTax / totalContribution * 100) / 100;
			}
			pstm4 = con.prepareStatement("UPDATE characters SET Contribution = 0, Pay = Contribution * ? WHERE HomeTownID = ?");
			pstm4.setDouble(1, contributionUnit);
			pstm4.setInt(2, townId);
			pstm4.execute();

			pstm5 = con.prepareStatement("UPDATE town SET leader_id = ?, leader_name = ?, tax_rate = 0, tax_rate_reserved = 0, sales_money = 0, sales_money_yesterday = sales_money, town_tax = 0, town_fix_tax = 0 WHERE town_id = ?");
			pstm5.setInt(1, leaderId);
			pstm5.setString(2, leaderName);
			pstm5.setInt(3, townId);
			pstm5.execute();
		} catch (SQLException e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} finally {
			SQLUtil.close(rs1);
			SQLUtil.close(rs2);
			SQLUtil.close(rs3);
			SQLUtil.close(pstm1);
			SQLUtil.close(pstm2);
			SQLUtil.close(pstm3);
			SQLUtil.close(pstm4);
			SQLUtil.close(pstm5);
			SQLUtil.close(con);
		}

		return leaderName;
	}

	/**
	 * 清理資料庫中的無效城鎮 ID
	 * <p>將資料庫中所有 HomeTownID = -1 的玩家重置為 0。
	 * <p>-1 通常表示玩家正在變更城鎮或處於無效狀態。
	 */
	private static void clearHomeTownID() {
		Connection con = null;
		PreparedStatement pstm = null;

		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement("UPDATE characters SET HomeTownID = 0 WHERE HomeTownID = -1");
			pstm.execute();
		} catch (SQLException e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} finally {
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
	}

	/**
	 * 取得並清空玩家的貢獻獎勵
	 * <p>查詢指定玩家的 Pay 欄位（貢獻獎勵金額），並將其重置為 0。
	 *
	 * <h3>使用時機:</h3>
	 * <p>當玩家向城鎮 NPC 領取貢獻獎勵時呼叫此方法。
	 *
	 * <h3>交易安全:</h3>
	 * <p>使用 {@code FOR UPDATE} 鎖定該筆記錄，防止並發領取造成獎勵重複發放。
	 *
	 * @param objid 玩家角色 ID
	 * @return 玩家的貢獻獎勵金額，若查詢失敗則返回 0
	 */
	public static int getPay(int objid) {
		Connection con = null;
		PreparedStatement pstm1 = null;
		PreparedStatement pstm2 = null;
		ResultSet rs1 = null;
		int pay = 0;

		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm1 = con.prepareStatement("SELECT Pay FROM characters WHERE objid = ? FOR UPDATE");

			pstm1.setInt(1, objid);
			rs1 = pstm1.executeQuery();

			if (rs1.next()) {
				pay = rs1.getInt("Pay");
			}

			pstm2 = con.prepareStatement("UPDATE characters SET Pay = 0 WHERE objid = ?");
			pstm2.setInt(1, objid);
			pstm2.execute();
		} catch (SQLException e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} finally {
			SQLUtil.close(rs1);
			SQLUtil.close(pstm1);
			SQLUtil.close(pstm2);
			SQLUtil.close(con);
		}

		return pay;
	}
}
