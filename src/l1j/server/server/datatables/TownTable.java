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
package l1j.server.server.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.templates.L1Town;
import l1j.server.server.utils.SQLUtil;
import l1j.server.server.utils.collections.Maps;

/**
 * 城鎮資料表管理類別
 *
 * <p>此類別負責管理遊戲中所有城鎮的資料,包括城鎮領主、稅率、稅收等資訊。
 * 城鎮是 Lineage 1 中重要的經濟與政治單位,玩家可以透過攻城戰佔領城鎮並成為領主。
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>從資料庫載入所有城鎮資料</li>
 *   <li>管理城鎮領主資訊</li>
 *   <li>管理城鎮稅率設定 (當前稅率與預約稅率)</li>
 *   <li>統計城鎮銷售額與稅收</li>
 *   <li>處理每日稅率更新與銷售額結算</li>
 *   <li>驗證玩家的領主身份</li>
 * </ul>
 *
 * <h3>城鎮資料結構:</h3>
 * <ul>
 *   <li><b>基本資訊:</b> 城鎮 ID、城鎮名稱</li>
 *   <li><b>領主資訊:</b> 領主 ID、領主名稱</li>
 *   <li><b>稅率設定:</b>
 *     <ul>
 *       <li>當前稅率 (tax_rate) - 正在使用的稅率</li>
 *       <li>預約稅率 (tax_rate_reserved) - 下次更新時使用的稅率</li>
 *     </ul>
 *   </li>
 *   <li><b>財務資料:</b>
 *     <ul>
 *       <li>今日銷售額 (sales_money) - 累積的商店交易金額</li>
 *       <li>昨日銷售額 (sales_money_yesterday) - 前一天的銷售總額</li>
 *       <li>城鎮稅收 (town_tax) - 依據稅率徵收的稅金</li>
 *       <li>城鎮固定稅 (town_fix_tax) - 固定 2% 的基礎稅</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>稅收計算規則:</h3>
 * <pre>
 * 城鎮稅 = 銷售額 × 稅率 / 100
 * 固定稅 = 銷售額 × 2 / 100
 *
 * 最低稅額: 如果稅率 > 0 但計算結果 ≤ 0,則設為 1
 * </pre>
 *
 * <h3>每日更新機制:</h3>
 * <ol>
 *   <li>將當前稅率更新為預約稅率 ({@link #updateTaxRate()})</li>
 *   <li>將今日銷售額轉存為昨日銷售額 ({@link #updateSalesMoneyYesterday()})</li>
 *   <li>清零今日銷售額,開始新一天的統計</li>
 * </ol>
 *
 * <h3>資料來源:</h3>
 * <p>所有資料從資料庫的 {@code town} 資料表載入,包含以下欄位:
 * <ul>
 *   <li>{@code town_id} - 城鎮 ID (主鍵)</li>
 *   <li>{@code name} - 城鎮名稱</li>
 *   <li>{@code leader_id} - 領主角色 ID</li>
 *   <li>{@code leader_name} - 領主角色名稱</li>
 *   <li>{@code tax_rate} - 當前稅率 (%)</li>
 *   <li>{@code tax_rate_reserved} - 預約稅率 (%)</li>
 *   <li>{@code sales_money} - 今日銷售額</li>
 *   <li>{@code sales_money_yesterday} - 昨日銷售額</li>
 *   <li>{@code town_tax} - 累積城鎮稅收</li>
 *   <li>{@code town_fix_tax} - 累積固定稅收</li>
 * </ul>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * // 取得城鎮資料
 * L1Town town = TownTable.getInstance().getTownTable(townId);
 *
 * // 檢查是否為領主
 * boolean isLeader = TownTable.getInstance().isLeader(pc, townId);
 *
 * // 記錄商店銷售額 (自動計算並加入稅收)
 * TownTable.getInstance().addSalesMoney(townId, 10000);
 *
 * // 定時任務: 每日稅率更新
 * TownTable.getInstance().updateTaxRate();
 * TownTable.getInstance().updateSalesMoneyYesterday();
 * </pre>
 *
 * <h3>設計模式:</h3>
 * <ul>
 *   <li><b>單例模式 (Singleton):</b> 全伺服器共用唯一實例</li>
 *   <li><b>執行緒安全:</b> 使用 ConcurrentMap 確保多執行緒存取安全</li>
 *   <li><b>同步方法:</b> addSalesMoney 使用 synchronized 確保金額計算正確性</li>
 *   <li><b>快取機制:</b> 啟動時載入所有資料至記憶體</li>
 * </ul>
 *
 * <h3>相關系統:</h3>
 * <ul>
 *   <li>攻城戰系統 - 決定城鎮領主</li>
 *   <li>商店交易系統 - 產生銷售額</li>
 *   <li>定時任務系統 - 處理每日更新</li>
 * </ul>
 *
 * @see l1j.server.server.templates.L1Town
 * @see l1j.server.server.controllers.HomeTownTimeController
 * @see l1j.server.server.model.Instance.L1PcInstance
 */
public class TownTable {

	/** 日誌記錄器 */
	private static Logger _log = Logger.getLogger(TownTable.class.getName());

	/** 單例實例 */
	private static TownTable _instance;

	/** 城鎮資料快取 (城鎮 ID → L1Town 物件) - 使用執行緒安全的 ConcurrentMap */
	private final Map<Integer, L1Town> _towns = Maps.newConcurrentMap();

	/**
	 * 取得 TownTable 的單例實例
	 * <p>使用延遲初始化 (Lazy Initialization) 模式
	 *
	 * @return TownTable 唯一實例
	 */
	public static TownTable getInstance() {
		if (_instance == null) {
			_instance = new TownTable();
		}

		return _instance;
	}

	/**
	 * 私有建構式 - 單例模式
	 * <p>在建構時自動載入所有城鎮資料
	 */
	private TownTable() {
		load();
	}

	/**
	 * 從資料庫載入所有城鎮資料
	 * <p>此方法從 {@code town} 資料表讀取所有城鎮的完整資訊,
	 * 包括領主、稅率、銷售額、稅收等資料,並快取至記憶體。
	 *
	 * <h3>載入流程:</h3>
	 * <ol>
	 *   <li>清空現有快取資料</li>
	 *   <li>從資料庫查詢所有城鎮記錄</li>
	 *   <li>為每個城鎮建立 L1Town 物件</li>
	 *   <li>設定城鎮的所有屬性</li>
	 *   <li>將城鎮物件加入快取 Map</li>
	 * </ol>
	 *
	 * <h3>載入的資料欄位:</h3>
	 * <ul>
	 *   <li>town_id - 城鎮 ID</li>
	 *   <li>name - 城鎮名稱</li>
	 *   <li>leader_id - 領主角色 ID</li>
	 *   <li>leader_name - 領主角色名稱</li>
	 *   <li>tax_rate - 當前稅率</li>
	 *   <li>tax_rate_reserved - 預約稅率</li>
	 *   <li>sales_money - 今日銷售額</li>
	 *   <li>sales_money_yesterday - 昨日銷售額</li>
	 *   <li>town_tax - 累積城鎮稅</li>
	 *   <li>town_fix_tax - 累積固定稅</li>
	 * </ul>
	 *
	 * <p><b>注意:</b> 此方法在物件建構時自動呼叫,通常不需要手動執行。
	 * 如需重新載入資料,可以手動呼叫此方法。
	 *
	 * @throws SQLException 如果資料庫查詢發生錯誤
	 */
	public void load() {
		Connection con = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;

		_towns.clear();

		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement("SELECT * FROM town");

			int townid;
			rs = pstm.executeQuery();

			while (rs.next()) {
				L1Town town = new L1Town();
				townid = rs.getInt("town_id");
				town.set_townid(townid);
				town.set_name(rs.getString("name"));
				town.set_leader_id(rs.getInt("leader_id"));
				town.set_leader_name(rs.getString("leader_name"));
				town.set_tax_rate(rs.getInt("tax_rate"));
				town.set_tax_rate_reserved(rs.getInt("tax_rate_reserved"));
				town.set_sales_money(rs.getInt("sales_money"));
				town.set_sales_money_yesterday(rs.getInt("sales_money_yesterday"));
				town.set_town_tax(rs.getInt("town_tax"));
				town.set_town_fix_tax(rs.getInt("town_fix_tax"));

				_towns.put(new Integer(townid), town);
			}
		}
		catch (SQLException e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		finally {
			SQLUtil.close(rs);
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
	}

	/**
	 * 取得所有城鎮資料陣列
	 * <p>返回快取中所有城鎮物件的陣列,可用於迭代處理所有城鎮。
	 *
	 * <h3>使用範例:</h3>
	 * <pre>
	 * L1Town[] towns = TownTable.getInstance().getTownTableList();
	 * for (L1Town town : towns) {
	 *     System.out.println(town.get_name() + " - " + town.get_leader_name());
	 * }
	 * </pre>
	 *
	 * @return 包含所有城鎮資料的 L1Town 陣列
	 */
	public L1Town[] getTownTableList() {
		return _towns.values().toArray(new L1Town[_towns.size()]);
	}

	/**
	 * 取得指定 ID 的城鎮資料
	 * <p>根據城鎮 ID 從快取中查詢對應的城鎮物件。
	 *
	 * <h3>常見城鎮 ID:</h3>
	 * <ul>
	 *   <li>1 - 奇岩城 (Giran)</li>
	 *   <li>2 - 古魯丁村 (Gludio)</li>
	 *   <li>3 - 肯特城 (Kent)</li>
	 *   <li>4 - 威頓村 (Windawood)</li>
	 *   <li>5 - 歐瑞村 (Oren)</li>
	 *   <li>6 - 銀騎士村 (Silver Knight Town)</li>
	 *   <li>7 - 亞丁城 (Aden)</li>
	 * </ul>
	 *
	 * @param id 城鎮 ID
	 * @return 對應的 L1Town 物件,如果不存在返回 null
	 */
	public L1Town getTownTable(int id) {
		return _towns.get(id);
	}

	/**
	 * 檢查玩家是否為指定城鎮的領主
	 * <p>透過比對玩家角色 ID 與城鎮領主 ID 來判斷。
	 *
	 * <h3>使用情境:</h3>
	 * <ul>
	 *   <li>驗證領主專屬功能的使用權限</li>
	 *   <li>顯示領主專屬 NPC 對話選項</li>
	 *   <li>處理稅率設定等領主操作</li>
	 * </ul>
	 *
	 * @param pc 要檢查的玩家角色
	 * @param town_id 城鎮 ID
	 * @return 如果玩家是該城鎮的領主返回 true,否則返回 false
	 * @see L1PcInstance#getId()
	 * @see L1Town#get_leader_id()
	 */
	public boolean isLeader(L1PcInstance pc, int town_id) {
		L1Town town = getTownTable(town_id);
		return (town.get_leader_id() == pc.getId());
	}

	/**
	 * 新增銷售額並計算稅收 (執行緒安全)
	 * <p>當城鎮內的商店產生交易時,記錄銷售額並自動計算對應的稅收。
	 * 此方法使用 synchronized 確保多執行緒環境下的資料一致性。
	 *
	 * <h3>稅收計算:</h3>
	 * <pre>
	 * 城鎮稅 = 銷售額 × 稅率 / 100
	 * 固定稅 = 銷售額 × 2 / 100  (固定 2% 的基礎稅)
	 *
	 * 最低稅額保護:
	 * - 如果稅率 > 0 但計算出的城鎮稅 ≤ 0,則設為 1
	 * - 如果稅率 > 0 但計算出的固定稅 ≤ 0,則設為 1
	 * </pre>
	 *
	 * <h3>處理流程:</h3>
	 * <ol>
	 *   <li>取得城鎮當前稅率</li>
	 *   <li>計算城鎮稅與固定稅</li>
	 *   <li>應用最低稅額保護規則</li>
	 *   <li>更新資料庫 (銷售額、城鎮稅、固定稅)</li>
	 *   <li>同步更新記憶體快取</li>
	 * </ol>
	 *
	 * <h3>執行緒安全:</h3>
	 * <p>此方法使用 synchronized 關鍵字,確保同一時間只有一個執行緒能執行,
	 * 避免並發修改造成的金額錯誤。
	 *
	 * <h3>使用範例:</h3>
	 * <pre>
	 * // 商店交易 10000 金幣
	 * TownTable.getInstance().addSalesMoney(townId, 10000);
	 *
	 * // 假設稅率為 10%:
	 * // 城鎮稅 = 10000 × 10 / 100 = 1000
	 * // 固定稅 = 10000 × 2 / 100 = 200
	 * </pre>
	 *
	 * @param town_id 城鎮 ID
	 * @param salesMoney 銷售金額
	 * @throws SQLException 如果資料庫更新發生錯誤
	 */
	public synchronized void addSalesMoney(int town_id, int salesMoney) {
		Connection con = null;
		PreparedStatement pstm = null;

		L1Town town = TownTable.getInstance().getTownTable(town_id);
		int townTaxRate = town.get_tax_rate();

		int townTax = salesMoney / 100 * townTaxRate;
		int townFixTax = salesMoney / 100 * 2;

		if ((townTax <= 0) && (townTaxRate > 0)) {
			townTax = 1;
		}
		if ((townFixTax <= 0) && (townTaxRate > 0)) {
			townFixTax = 1;
		}

		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con
					.prepareStatement("UPDATE town SET sales_money = sales_money + ?, town_tax = town_tax + ?, town_fix_tax = town_fix_tax + ? WHERE town_id = ?");
			pstm.setInt(1, salesMoney);
			pstm.setInt(2, townTax);
			pstm.setInt(3, townFixTax);
			pstm.setInt(4, town_id);
			pstm.execute();

			town.set_sales_money(town.get_sales_money() + salesMoney);
			town.set_town_tax(town.get_town_tax() + townTax);
			town.set_town_fix_tax(town.get_town_fix_tax() + townFixTax);

		}
		catch (SQLException e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		finally {
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
	}

	/**
	 * 更新所有城鎮的稅率
	 * <p>將所有城鎮的當前稅率 (tax_rate) 更新為預約稅率 (tax_rate_reserved)。
	 * 此方法通常由定時任務在每日特定時間自動執行。
	 *
	 * <h3>更新機制:</h3>
	 * <p>遊戲中的稅率變更不會立即生效,而是設定為「預約稅率」,
	 * 待下次稅率更新時間到來時,才會正式套用新稅率。
	 *
	 * <h3>執行時機:</h3>
	 * <ul>
	 *   <li>每日固定時間 (通常為伺服器時間的特定時刻)</li>
	 *   <li>由 HomeTownTimeController 或類似的定時控制器觸發</li>
	 * </ul>
	 *
	 * <h3>SQL 操作:</h3>
	 * <pre>
	 * UPDATE town SET tax_rate = tax_rate_reserved
	 * </pre>
	 *
	 * <h3>使用範例:</h3>
	 * <pre>
	 * // 定時任務中執行
	 * public void dailyUpdate() {
	 *     TownTable.getInstance().updateTaxRate();  // 套用新稅率
	 *     TownTable.getInstance().updateSalesMoneyYesterday();  // 結算昨日銷售
	 * }
	 * </pre>
	 *
	 * <p><b>注意:</b> 此方法只更新資料庫,不會自動重新載入快取。
	 * 如需同步快取,請在執行後呼叫 {@link #load()} 方法。
	 *
	 * @throws SQLException 如果資料庫更新發生錯誤
	 * @see #updateSalesMoneyYesterday()
	 */
	public void updateTaxRate() {
		Connection con = null;
		PreparedStatement pstm = null;

		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement("UPDATE town SET tax_rate = tax_rate_reserved");
			pstm.execute();
		}
		catch (SQLException e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		finally {
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
	}

	/**
	 * 結算每日銷售額
	 * <p>將所有城鎮的今日銷售額 (sales_money) 轉存為昨日銷售額 (sales_money_yesterday),
	 * 並將今日銷售額歸零,開始新一天的統計。此方法通常由定時任務在每日結束時自動執行。
	 *
	 * <h3>結算流程:</h3>
	 * <ol>
	 *   <li>將 sales_money_yesterday 設為當前的 sales_money</li>
	 *   <li>將 sales_money 重置為 0</li>
	 *   <li>保留累積的稅收數據 (town_tax, town_fix_tax)</li>
	 * </ol>
	 *
	 * <h3>執行時機:</h3>
	 * <ul>
	 *   <li>每日午夜或特定時間 (伺服器時間)</li>
	 *   <li>通常與 {@link #updateTaxRate()} 一起執行</li>
	 *   <li>由 HomeTownTimeController 或類似的定時控制器觸發</li>
	 * </ul>
	 *
	 * <h3>SQL 操作:</h3>
	 * <pre>
	 * UPDATE town SET
	 *   sales_money_yesterday = sales_money,
	 *   sales_money = 0
	 * </pre>
	 *
	 * <h3>資料保存:</h3>
	 * <p>昨日銷售額可用於:
	 * <ul>
	 *   <li>統計報表與歷史記錄</li>
	 *   <li>領主查看前一日的營收狀況</li>
	 *   <li>城鎮經濟活動分析</li>
	 * </ul>
	 *
	 * <h3>使用範例:</h3>
	 * <pre>
	 * // 每日結算任務
	 * public void dailySettlement() {
	 *     TownTable.getInstance().updateSalesMoneyYesterday();  // 結算銷售額
	 *     TownTable.getInstance().updateTaxRate();  // 套用新稅率
	 *     TownTable.getInstance().load();  // 重新載入快取
	 * }
	 * </pre>
	 *
	 * <p><b>注意:</b> 此方法只更新資料庫,不會自動重新載入快取。
	 * 如需同步快取,請在執行後呼叫 {@link #load()} 方法。
	 *
	 * @throws SQLException 如果資料庫更新發生錯誤
	 * @see #updateTaxRate()
	 */
	public void updateSalesMoneyYesterday() {
		Connection con = null;
		PreparedStatement pstm = null;

		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement("UPDATE town SET sales_money_yesterday = sales_money, sales_money = 0");
			pstm.execute();
		}
		catch (SQLException e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		finally {
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
	}
}
