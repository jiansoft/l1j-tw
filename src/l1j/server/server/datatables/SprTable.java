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

import static l1j.server.server.ActionCodes.ACTION_Aggress;
import static l1j.server.server.ActionCodes.ACTION_AltAttack;
import static l1j.server.server.ActionCodes.ACTION_Attack;
import static l1j.server.server.ActionCodes.ACTION_AxeAttack;
import static l1j.server.server.ActionCodes.ACTION_AxeWalk;
import static l1j.server.server.ActionCodes.ACTION_BowAttack;
import static l1j.server.server.ActionCodes.ACTION_BowWalk;
import static l1j.server.server.ActionCodes.ACTION_ClawAttack;
import static l1j.server.server.ActionCodes.ACTION_ClawWalk;
import static l1j.server.server.ActionCodes.ACTION_DaggerAttack;
import static l1j.server.server.ActionCodes.ACTION_DaggerWalk;
import static l1j.server.server.ActionCodes.ACTION_EdoryuAttack;
import static l1j.server.server.ActionCodes.ACTION_EdoryuWalk;
import static l1j.server.server.ActionCodes.ACTION_SkillAttack;
import static l1j.server.server.ActionCodes.ACTION_SkillBuff;
import static l1j.server.server.ActionCodes.ACTION_SpearAttack;
import static l1j.server.server.ActionCodes.ACTION_SpearWalk;
import static l1j.server.server.ActionCodes.ACTION_SpellDirectionExtra;
import static l1j.server.server.ActionCodes.ACTION_StaffAttack;
import static l1j.server.server.ActionCodes.ACTION_StaffWalk;
import static l1j.server.server.ActionCodes.ACTION_SwordAttack;
import static l1j.server.server.ActionCodes.ACTION_SwordWalk;
import static l1j.server.server.ActionCodes.ACTION_Think;
import static l1j.server.server.ActionCodes.ACTION_ThrowingKnifeAttack;
import static l1j.server.server.ActionCodes.ACTION_ThrowingKnifeWalk;
import static l1j.server.server.ActionCodes.ACTION_TwoHandSwordAttack;
import static l1j.server.server.ActionCodes.ACTION_TwoHandSwordWalk;
import static l1j.server.server.ActionCodes.ACTION_Walk;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.utils.SQLUtil;
import l1j.server.server.utils.collections.Maps;

/**
 * SPR 動作速度資料表管理類別
 *
 * <p>此類別負責管理遊戲中所有 SPR (Sprite) 圖檔的動作速度資料。
 * SPR 是 Lineage 1 使用的圖形資源格式,包含角色、NPC、怪物的外觀和動畫資料。
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>從資料庫載入 SPR 動作速度資訊</li>
 *   <li>根據 SPR ID 和動作類型查詢對應的動作速度</li>
 *   <li>管理不同武器類型的攻擊速度</li>
 *   <li>管理移動速度</li>
 *   <li>管理施法速度 (有向/無向)</li>
 *   <li>管理特殊動作速度 (表情動作等)</li>
 * </ul>
 *
 * <h3>動作速度分類:</h3>
 * <ul>
 *   <li><b>移動速度 (moveSpeed):</b> 各種武器持握狀態下的走路速度</li>
 *   <li><b>攻擊速度 (attackSpeed):</b> 不同武器類型的攻擊動作速度</li>
 *   <li><b>施法速度:</b>
 *     <ul>
 *       <li>有向施法速度 (dirSpellSpeed) - 需要指定目標的魔法</li>
 *       <li>無向施法速度 (nodirSpellSpeed) - 不需要目標的輔助魔法</li>
 *     </ul>
 *   </li>
 *   <li><b>特殊動作速度 (specialSpeed):</b> 表情動作等特殊行為</li>
 * </ul>
 *
 * <h3>速度計算公式:</h3>
 * <pre>
 * 動作速度 (ms) = 畫格數量 × 40 × (24 / 畫格速率)
 * </pre>
 * <p>此公式將 SPR 檔案中的畫格資訊轉換為遊戲中的實際動作持續時間 (毫秒)。
 *
 * <h3>資料來源:</h3>
 * <p>所有資料從資料庫的 {@code spr_action} 資料表載入,包含以下欄位:
 * <ul>
 *   <li>{@code spr_id} - SPR 圖檔 ID</li>
 *   <li>{@code act_id} - 動作代碼 (對應 ActionCodes 中的常數)</li>
 *   <li>{@code framecount} - 動畫畫格數量</li>
 *   <li>{@code framerate} - 動畫播放速率 (FPS)</li>
 * </ul>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * // 取得 NPC 的攻擊速度
 * int attackSpeed = SprTable.getInstance().getAttackSpeed(sprid, ACTION_Attack);
 *
 * // 取得移動速度
 * int moveSpeed = SprTable.getInstance().getMoveSpeed(sprid, ACTION_Walk);
 *
 * // 取得有向施法速度
 * int spellSpeed = SprTable.getInstance().getDirSpellSpeed(sprid);
 *
 * // 通用方法 - 根據動作代碼自動判斷類型
 * int speed = SprTable.getInstance().getSprSpeed(sprid, actid);
 * </pre>
 *
 * <h3>設計模式:</h3>
 * <ul>
 *   <li><b>單例模式 (Singleton):</b> 全伺服器共用唯一實例</li>
 *   <li><b>快取機制:</b> 啟動時一次性載入所有資料至記憶體</li>
 *   <li><b>容錯處理:</b> 當找不到特定動作時,會回傳基本動作的速度</li>
 * </ul>
 *
 * <h3>重要說明:</h3>
 * <ul>
 *   <li>速度值的單位為毫秒 (ms)</li>
 *   <li>如果 SPR 不存在或動作未定義,會返回 0 或預設值</li>
 *   <li>攻擊速度查詢失敗時會自動退回到基本攻擊動作</li>
 *   <li>移動速度查詢失敗時會自動退回到基本走路動作</li>
 *   <li>預設施法速度為 1200ms</li>
 * </ul>
 *
 * @see l1j.server.server.ActionCodes
 * @see l1j.server.server.model.Instance.L1NpcInstance
 * @see l1j.server.server.serverpackets.S_DoActionGfx
 */
public class SprTable {

	/** 日誌記錄器 */
	private static Logger _log = Logger.getLogger(SprTable.class.getName());

	/**
	 * SPR 資料內部類別
	 * <p>儲存單一 SPR 的所有動作速度資訊
	 */
	private static class Spr {
		/** 移動速度對照表 (動作代碼 → 速度 ms) */
		private final Map<Integer, Integer> moveSpeed = Maps.newMap();

		/** 攻擊速度對照表 (動作代碼 → 速度 ms) */
		private final Map<Integer, Integer> attackSpeed = Maps.newMap();

		/** 特殊動作速度對照表 (動作代碼 → 速度 ms) */
		private final Map<Integer, Integer> specialSpeed = Maps.newMap();

		/** 無向施法速度 (不需要目標的輔助魔法) - 預設 1200ms */
		private int nodirSpellSpeed = 1200;

		/** 有向施法速度 (需要指定目標的攻擊魔法) - 預設 1200ms */
		private int dirSpellSpeed = 1200;
	}

	/** SPR 資料快取 (SPR ID → Spr 物件) */
	private static final Map<Integer, Spr> _dataMap = Maps.newMap();

	/** 單例實例 */
	private static final SprTable _instance = new SprTable();

	/**
	 * 私有建構式 - 單例模式
	 * <p>在建構時自動載入所有 SPR 動作資料
	 */
	private SprTable() {
		loadSprAction();
	}

	/**
	 * 取得 SprTable 的單例實例
	 *
	 * @return SprTable 唯一實例
	 */
	public static SprTable getInstance() {
		return _instance;
	}

	/**
	 * 從資料庫載入 SPR 動作速度資料
	 * <p>此方法從 {@code spr_action} 資料表讀取所有 SPR 的動作資訊,
	 * 包括畫格數量和畫格速率,並計算出實際的動作速度 (毫秒)。
	 *
	 * <h3>載入流程:</h3>
	 * <ol>
	 *   <li>從資料庫查詢所有 SPR 動作記錄</li>
	 *   <li>根據畫格數量和畫格速率計算動作速度</li>
	 *   <li>依據動作類型分類儲存 (移動/攻擊/施法/特殊)</li>
	 *   <li>將資料快取至記憶體供快速查詢</li>
	 * </ol>
	 *
	 * <h3>動作分類規則:</h3>
	 * <ul>
	 *   <li>Walk 系列 → moveSpeed</li>
	 *   <li>Attack 系列 → attackSpeed</li>
	 *   <li>SkillAttack (有向施法) → dirSpellSpeed</li>
	 *   <li>SkillBuff (無向施法) → nodirSpellSpeed</li>
	 *   <li>Think/Aggress → specialSpeed</li>
	 * </ul>
	 *
	 * <p><b>注意:</b> 此方法在物件建構時自動呼叫,通常不需要手動執行。
	 *
	 * @throws SQLException 如果資料庫查詢發生錯誤
	 */
	public void loadSprAction() {
		Connection con = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;
		Spr spr = null;
		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement("SELECT * FROM spr_action");
			rs = pstm.executeQuery();
			while (rs.next()) {
				int key = rs.getInt("spr_id");
				if (!_dataMap.containsKey(key)) {
					spr = new Spr();
					_dataMap.put(key, spr);
				}
				else {
					spr = _dataMap.get(key);
				}

				int actid = rs.getInt("act_id");
				int frameCount = rs.getInt("framecount");
				int frameRate = rs.getInt("framerate");
				int speed = calcActionSpeed(frameCount, frameRate);

				switch (actid) {
					case ACTION_Walk:
					case ACTION_SwordWalk:
					case ACTION_AxeWalk:
					case ACTION_BowWalk:
					case ACTION_SpearWalk:
					case ACTION_StaffWalk:
					case ACTION_DaggerWalk:
					case ACTION_TwoHandSwordWalk:
					case ACTION_EdoryuWalk:
					case ACTION_ClawWalk:
					case ACTION_ThrowingKnifeWalk:
						spr.moveSpeed.put(actid, speed);
						break;
					case ACTION_SkillAttack:
						spr.dirSpellSpeed = speed;
						break;
					case ACTION_SkillBuff:
						spr.nodirSpellSpeed = speed;
						break;
					case ACTION_Attack:
					case ACTION_SwordAttack:
					case ACTION_AxeAttack:
					case ACTION_BowAttack:
					case ACTION_SpearAttack:
					case ACTION_AltAttack:
					case ACTION_SpellDirectionExtra:
					case ACTION_StaffAttack:
					case ACTION_DaggerAttack:
					case ACTION_TwoHandSwordAttack:
					case ACTION_EdoryuAttack:
					case ACTION_ClawAttack:
					case ACTION_ThrowingKnifeAttack:
						spr.attackSpeed.put(actid, speed);
						break;
					case ACTION_Think:
					case ACTION_Aggress:
						spr.specialSpeed.put(actid, speed);
						break;
					default:
						break;
				}
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
		_log.config("SPRデータ " + _dataMap.size() + "件ロード");
	}

	/**
	 * 計算動作速度 (毫秒)
	 * <p>根據畫格數量和畫格速率計算動作的總持續時間。
	 *
	 * <h3>計算公式:</h3>
	 * <pre>
	 * 速度 (ms) = 畫格數量 × 40 × (24 / 畫格速率)
	 * </pre>
	 *
	 * <h3>公式說明:</h3>
	 * <ul>
	 *   <li>40ms - 每畫格的基礎時間 (25 FPS 標準)</li>
	 *   <li>24 - 標準畫格速率</li>
	 *   <li>(24 / frameRate) - 速率調整係數</li>
	 * </ul>
	 *
	 * @param frameCount 動畫畫格數量
	 * @param frameRate 動畫播放速率 (FPS)
	 * @return 動作的總持續時間 (毫秒)
	 */
	private int calcActionSpeed(int frameCount, int frameRate) {
		return (int) (frameCount * 40 * (24D / frameRate));
	}

	/**
	 * 取得指定 SPR 的攻擊速度
	 * <p>根據 SPR ID 和動作代碼查詢對應的攻擊速度。
	 * 如果指定的武器類型動作不存在,會自動退回使用基本攻擊動作的速度。
	 *
	 * <h3>容錯機制:</h3>
	 * <ol>
	 *   <li>如果 SPR ID 不存在 → 返回 0</li>
	 *   <li>如果動作代碼為 ACTION_Attack 但未設定 → 返回 0</li>
	 *   <li>如果特定武器動作未設定 → 返回 ACTION_Attack 的速度</li>
	 * </ol>
	 *
	 * <h3>支援的動作代碼:</h3>
	 * <ul>
	 *   <li>ACTION_Attack - 基本攻擊</li>
	 *   <li>ACTION_SwordAttack - 劍攻擊</li>
	 *   <li>ACTION_AxeAttack - 斧攻擊</li>
	 *   <li>ACTION_BowAttack - 弓攻擊</li>
	 *   <li>ACTION_SpearAttack - 矛攻擊</li>
	 *   <li>ACTION_StaffAttack - 法杖攻擊</li>
	 *   <li>ACTION_DaggerAttack - 匕首攻擊</li>
	 *   <li>ACTION_TwoHandSwordAttack - 雙手劍攻擊</li>
	 *   <li>ACTION_EdoryuAttack - 鎖鏈劍攻擊</li>
	 *   <li>ACTION_ClawAttack - 手甲攻擊</li>
	 *   <li>ACTION_ThrowingKnifeAttack - 飛刀攻擊</li>
	 * </ul>
	 *
	 * @param sprid SPR 圖檔 ID
	 * @param actid 動作代碼 (武器類型相關的攻擊動作)
	 * @return 攻擊動作速度 (毫秒), 如果不存在返回 0
	 * @see l1j.server.server.ActionCodes
	 */
	public int getAttackSpeed(int sprid, int actid) {
		if (_dataMap.containsKey(sprid)) {
			if (_dataMap.get(sprid).attackSpeed.containsKey(actid)) {
				return _dataMap.get(sprid).attackSpeed.get(actid);
			}
			else if (actid == ACTION_Attack) {
				return 0;
			}
			else {
				return _dataMap.get(sprid).attackSpeed.get(ACTION_Attack);
			}
		}
		return 0;
	}

	/**
	 * 取得指定 SPR 的移動速度
	 * <p>根據 SPR ID 和動作代碼查詢對應的移動速度。
	 * 如果指定的武器類型移動動作不存在,會自動退回使用基本走路動作的速度。
	 *
	 * <h3>容錯機制:</h3>
	 * <ol>
	 *   <li>如果 SPR ID 不存在 → 返回 0</li>
	 *   <li>如果動作代碼為 ACTION_Walk 但未設定 → 返回 0</li>
	 *   <li>如果特定武器移動動作未設定 → 返回 ACTION_Walk 的速度</li>
	 * </ol>
	 *
	 * <h3>支援的動作代碼:</h3>
	 * <ul>
	 *   <li>ACTION_Walk - 基本走路</li>
	 *   <li>ACTION_SwordWalk - 持劍走路</li>
	 *   <li>ACTION_AxeWalk - 持斧走路</li>
	 *   <li>ACTION_BowWalk - 持弓走路</li>
	 *   <li>ACTION_SpearWalk - 持矛走路</li>
	 *   <li>ACTION_StaffWalk - 持法杖走路</li>
	 *   <li>ACTION_DaggerWalk - 持匕首走路</li>
	 *   <li>ACTION_TwoHandSwordWalk - 持雙手劍走路</li>
	 *   <li>ACTION_EdoryuWalk - 持鎖鏈劍走路</li>
	 *   <li>ACTION_ClawWalk - 持手甲走路</li>
	 *   <li>ACTION_ThrowingKnifeWalk - 持飛刀走路</li>
	 * </ul>
	 *
	 * @param sprid SPR 圖檔 ID
	 * @param actid 動作代碼 (武器類型相關的移動動作)
	 * @return 移動動作速度 (毫秒), 如果不存在返回 0
	 * @see l1j.server.server.ActionCodes
	 */
	public int getMoveSpeed(int sprid, int actid) {
		if (_dataMap.containsKey(sprid)) {
			if (_dataMap.get(sprid).moveSpeed.containsKey(actid)) {
				return _dataMap.get(sprid).moveSpeed.get(actid);
			}
			else if (actid == ACTION_Walk) {
				return 0;
			}
			else {
				return _dataMap.get(sprid).moveSpeed.get(ACTION_Walk);
			}
		}
		return 0;
	}

	/**
	 * 取得指定 SPR 的有向施法速度
	 * <p>有向施法是指需要選擇目標或方向的魔法,通常是攻擊性魔法。
	 * 對應動作代碼為 {@code ACTION_SkillAttack}。
	 *
	 * <h3>適用魔法類型:</h3>
	 * <ul>
	 *   <li>單體攻擊魔法 (需指定目標)</li>
	 *   <li>方向性魔法 (需指定施放方向)</li>
	 *   <li>範圍攻擊魔法 (需指定中心位置)</li>
	 * </ul>
	 *
	 * @param sprid SPR 圖檔 ID
	 * @return 有向施法速度 (毫秒), 預設 1200ms, 如果 SPR 不存在返回 0
	 */
	public int getDirSpellSpeed(int sprid) {
		if (_dataMap.containsKey(sprid)) {
			return _dataMap.get(sprid).dirSpellSpeed;
		}
		return 0;
	}

	/**
	 * 取得指定 SPR 的無向施法速度
	 * <p>無向施法是指不需要選擇目標的魔法,通常是輔助性或自身增益魔法。
	 * 對應動作代碼為 {@code ACTION_SkillBuff}。
	 *
	 * <h3>適用魔法類型:</h3>
	 * <ul>
	 *   <li>自身增益魔法 (加速、防禦提升等)</li>
	 *   <li>範圍增益魔法 (不需指定目標)</li>
	 *   <li>召喚魔法</li>
	 *   <li>瞬間移動魔法</li>
	 * </ul>
	 *
	 * @param sprid SPR 圖檔 ID
	 * @return 無向施法速度 (毫秒), 預設 1200ms, 如果 SPR 不存在返回 0
	 */
	public int getNodirSpellSpeed(int sprid) {
		if (_dataMap.containsKey(sprid)) {
			return _dataMap.get(sprid).nodirSpellSpeed;
		}
		return 0;
	}

	/**
	 * 取得指定 SPR 的特殊動作速度
	 * <p>特殊動作包括角色的社交表情動作等非戰鬥行為。
	 *
	 * <h3>支援的特殊動作:</h3>
	 * <ul>
	 *   <li>ACTION_Think - 思考動作 (Alt+4)</li>
	 *   <li>ACTION_Aggress - 挑釁動作 (Alt+3)</li>
	 * </ul>
	 *
	 * @param sprid SPR 圖檔 ID
	 * @param actid 動作代碼 (特殊動作類型)
	 * @return 特殊動作速度 (毫秒), 預設 1200ms, 如果 SPR 或動作不存在返回 0
	 * @see l1j.server.server.ActionCodes#ACTION_Think
	 * @see l1j.server.server.ActionCodes#ACTION_Aggress
	 */
	public int getSpecialSpeed(int sprid, int actid) {
		if (_dataMap.containsKey(sprid)) {
			if (_dataMap.get(sprid).specialSpeed.containsKey(actid)) {
				return _dataMap.get(sprid).specialSpeed.get(actid);
			}
			else {
				return 1200;
			}
		}
		return 0;
	}

	/**
	 * 取得 SPR 的動作速度 (通用方法)
	 * <p>根據動作代碼自動判斷動作類型,並調用對應的速度查詢方法。
	 * 這是最常用的速度查詢方法,可以處理所有類型的動作。
	 *
	 * <h3>自動分類邏輯:</h3>
	 * <ul>
	 *   <li><b>移動動作:</b> Walk 系列 → 調用 {@link #getMoveSpeed(int, int)}</li>
	 *   <li><b>攻擊動作:</b> Attack 系列 → 調用 {@link #getAttackSpeed(int, int)}</li>
	 *   <li><b>有向施法:</b> SkillAttack → 調用 {@link #getDirSpellSpeed(int)}</li>
	 *   <li><b>無向施法:</b> SkillBuff → 調用 {@link #getNodirSpellSpeed(int)}</li>
	 *   <li><b>特殊動作:</b> Think/Aggress → 調用 {@link #getSpecialSpeed(int, int)}</li>
	 * </ul>
	 *
	 * <h3>支援的動作分類:</h3>
	 * <table border="1">
	 *   <tr><th>類型</th><th>動作代碼</th></tr>
	 *   <tr><td>移動</td><td>Walk, SwordWalk, AxeWalk, BowWalk, SpearWalk, StaffWalk, DaggerWalk, TwoHandSwordWalk, EdoryuWalk, ClawWalk, ThrowingKnifeWalk</td></tr>
	 *   <tr><td>攻擊</td><td>Attack, SwordAttack, AxeAttack, BowAttack, SpearAttack, StaffAttack, DaggerAttack, TwoHandSwordAttack, EdoryuAttack, ClawAttack, ThrowingKnifeAttack, AltAttack, SpellDirectionExtra</td></tr>
	 *   <tr><td>有向施法</td><td>SkillAttack</td></tr>
	 *   <tr><td>無向施法</td><td>SkillBuff</td></tr>
	 *   <tr><td>特殊</td><td>Think, Aggress</td></tr>
	 * </table>
	 *
	 * <h3>使用範例:</h3>
	 * <pre>
	 * // 取得 NPC 的移動延遲時間
	 * int delay = SprTable.getInstance().getSprSpeed(npc.getSpriteId(), ACTION_Walk);
	 *
	 * // 取得攻擊動作延遲時間
	 * int attackDelay = SprTable.getInstance().getSprSpeed(sprId, ACTION_SwordAttack);
	 * </pre>
	 *
	 * @param sprid SPR 圖檔 ID
	 * @param actid 動作代碼 (來自 ActionCodes 的任何動作常數)
	 * @return 動作延遲時間 (毫秒), 如果動作類型不支援或 SPR 不存在則返回 0
	 * @see l1j.server.server.ActionCodes
	 */
	public int getSprSpeed(int sprid, int actid) {
		switch (actid) {
			case ACTION_Walk:
			case ACTION_SwordWalk:
			case ACTION_AxeWalk:
			case ACTION_BowWalk:
			case ACTION_SpearWalk:
			case ACTION_StaffWalk:
			case ACTION_DaggerWalk:
			case ACTION_TwoHandSwordWalk:
			case ACTION_EdoryuWalk:
			case ACTION_ClawWalk:
			case ACTION_ThrowingKnifeWalk:
				// 移動
				return getMoveSpeed(sprid, actid);
			case ACTION_SkillAttack:
				// 有向施法
				return getDirSpellSpeed(sprid);
			case ACTION_SkillBuff:
				// 無向施法
				return getNodirSpellSpeed(sprid);
			case ACTION_Attack:
			case ACTION_SwordAttack:
			case ACTION_AxeAttack:
			case ACTION_BowAttack:
			case ACTION_SpearAttack:
			case ACTION_AltAttack:
			case ACTION_SpellDirectionExtra:
			case ACTION_StaffAttack:
			case ACTION_DaggerAttack:
			case ACTION_TwoHandSwordAttack:
			case ACTION_EdoryuAttack:
			case ACTION_ClawAttack:
			case ACTION_ThrowingKnifeAttack:
				// 攻擊
				return getAttackSpeed(sprid, actid);
			case ACTION_Think:
			case ACTION_Aggress:
				// 魔法娃娃表情動作
				return getSpecialSpeed(sprid, actid);
			default:
				break;
		}
		return 0;
	}
}
