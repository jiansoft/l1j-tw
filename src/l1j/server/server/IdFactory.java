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
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.utils.SQLUtil;

/**
 * 唯一ID生成器工廠類別。
 * <p>
 * 此類別負責為遊戲世界中的所有物件(角色、物品、寵物、血盟等)生成唯一的ID。
 * 採用單例模式(Singleton Pattern)確保整個伺服器只有一個ID生成器實例。
 * </p>
 * <p>
 * ID生成範圍從 {@code 0x10000000} (268435456) 開始,初始化時會從資料庫中
 * 查詢所有現有物件的最大ID,並從該值+1開始生成新ID,以避免ID衝突。
 * </p>
 *
 * <h3>涵蓋的資料表:</h3>
 * <ul>
 *   <li>character_items - 角色物品</li>
 *   <li>character_teleport - 角色傳送點</li>
 *   <li>character_warehouse - 角色倉庫</li>
 *   <li>character_elf_warehouse - 妖精倉庫</li>
 *   <li>characters - 角色</li>
 *   <li>clan_data - 血盟資料</li>
 *   <li>clan_members - 血盟成員</li>
 *   <li>clan_warehouse - 血盟倉庫</li>
 *   <li>pets - 寵物</li>
 * </ul>
 *
 * <h3>執行緒安全性:</h3>
 * <p>
 * 此類別使用同步鎖(synchronized)機制確保在多執行緒環境下生成的ID唯一且不重複。
 * </p>
 *
 * @see l1j.server.server.model.Instance.L1ItemInstance
 * @see l1j.server.server.model.Instance.L1PcInstance
 * @see l1j.server.server.model.Instance.L1PetInstance
 */
public class IdFactory {
	private static Logger _log = Logger.getLogger(IdFactory.class.getName());

	/**
	 * 目前的ID值,每次呼叫 {@link #nextId()} 時會遞增
	 */
	private int _curId;

	/**
	 * 同步鎖物件,用於確保 {@link #nextId()} 方法的執行緒安全
	 */
	private Object _monitor = new Object();

	/**
	 * 起始ID常數值: 0x10000000 (十進位: 268435456)
	 * <p>
	 * 此值作為ID的最小值,確保遊戲物件ID不會與系統保留ID衝突。
	 * </p>
	 */
	private static final int FIRST_ID = 0x10000000;

	/**
	 * IdFactory 的唯一實例(Singleton)
	 */
	private static IdFactory _instance = new IdFactory();

	/**
	 * 私有建構子,防止外部直接實例化。
	 * <p>
	 * 建構時會呼叫 {@link #loadState()} 從資料庫載入當前最大ID。
	 * </p>
	 */
	private IdFactory() {
		loadState();
	}

	/**
	 * 取得 IdFactory 的唯一實例。
	 *
	 * @return IdFactory 實例
	 */
	public static IdFactory getInstance() {
		return _instance;
	}

	/**
	 * 生成下一個唯一ID。
	 * <p>
	 * 此方法是執行緒安全的,使用同步鎖確保在多執行緒環境下
	 * 不會產生重複的ID。每次呼叫時,內部計數器會遞增。
	 * </p>
	 *
	 * @return 新的唯一ID
	 */
	public int nextId() {
		synchronized (_monitor) {
			return _curId++;
		}
	}

	/**
	 * 從資料庫載入當前狀態,初始化起始ID。
	 * <p>
	 * 此方法會查詢所有相關資料表(角色、物品、寵物、血盟等)的最大ID值,
	 * 並將起始ID設定為該最大值+1。如果查詢結果小於 {@link #FIRST_ID},
	 * 則使用 {@link #FIRST_ID} 作為起始值。
	 * </p>
	 * <p>
	 * 執行的SQL查詢涵蓋以下資料表的ID欄位:
	 * </p>
	 * <ul>
	 *   <li>character_items.id</li>
	 *   <li>character_teleport.id</li>
	 *   <li>character_warehouse.id</li>
	 *   <li>character_elf_warehouse.id</li>
	 *   <li>characters.objid</li>
	 *   <li>clan_data.clan_id</li>
	 *   <li>clan_members.index_id</li>
	 *   <li>clan_warehouse.id</li>
	 *   <li>pets.objid</li>
	 * </ul>
	 *
	 * @throws SQLException 如果資料庫查詢失敗(會被捕捉並記錄)
	 */
	private void loadState() {
		// 取得資料庫中最大的ID+1
		Connection con = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;

		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement("select max(id)+1 as nextid from (select id from character_items union all select id from character_teleport union all select id from character_warehouse union all select id from character_elf_warehouse union all select objid as id from characters union all select clan_id as id from clan_data union all select index_id as id from clan_members union all select id from clan_warehouse union all select objid as id from pets ) t");
			rs = pstm.executeQuery();

			int id = 0;
			if (rs.next()) {
				id = rs.getInt("nextid");
			}
			if (id < FIRST_ID) {
				id = FIRST_ID;
			}
			_curId = id;
			_log.info("目前的物件ID: " + _curId);
		} catch (SQLException e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} finally {
			SQLUtil.close(rs);
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
	}
}
