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
package l1j.server.server.model.Instance;

import static l1j.server.server.model.skill.L1SkillId.BLIND_HIDING;
import static l1j.server.server.model.skill.L1SkillId.CANCELLATION;
import static l1j.server.server.model.skill.L1SkillId.COOKING_WONDER_DRUG;
import static l1j.server.server.model.skill.L1SkillId.COUNTER_BARRIER;
import static l1j.server.server.model.skill.L1SkillId.DECREASE_WEIGHT;
import static l1j.server.server.model.skill.L1SkillId.DRESS_EVASION;
import static l1j.server.server.model.skill.L1SkillId.EFFECT_POTION_OF_BATTLE;
import static l1j.server.server.model.skill.L1SkillId.ENTANGLE;
import static l1j.server.server.model.skill.L1SkillId.FOG_OF_SLEEPING;
import static l1j.server.server.model.skill.L1SkillId.GMSTATUS_FINDINVIS;
import static l1j.server.server.model.skill.L1SkillId.GMSTATUS_HPBAR;
import static l1j.server.server.model.skill.L1SkillId.GREATER_HASTE;
import static l1j.server.server.model.skill.L1SkillId.HASTE;
import static l1j.server.server.model.skill.L1SkillId.ILLUSION_AVATAR;
import static l1j.server.server.model.skill.L1SkillId.INVISIBILITY;
import static l1j.server.server.model.skill.L1SkillId.MASS_SLOW;
import static l1j.server.server.model.skill.L1SkillId.MORTAL_BODY;
import static l1j.server.server.model.skill.L1SkillId.SHAPE_CHANGE;
import static l1j.server.server.model.skill.L1SkillId.SLOW;
import static l1j.server.server.model.skill.L1SkillId.SOLID_CARRIAGE;
import static l1j.server.server.model.skill.L1SkillId.STATUS_CHAT_PROHIBITED;
import static l1j.server.server.model.skill.L1SkillId.STATUS_HASTE;
import static l1j.server.server.model.skill.L1SkillId.STATUS_RIBRAVE;
import static l1j.server.server.model.skill.L1SkillId.STATUS_THIRD_SPEED;
import static l1j.server.server.model.skill.L1SkillId.STRIKER_GALE;
import static l1j.server.server.model.skill.L1SkillId.WIND_SHACKLE;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.Config;
import l1j.server.server.ActionCodes;
import l1j.server.server.ClientThread;
import l1j.server.server.GeneralThreadPool;
import l1j.server.server.PacketOutput;
import l1j.server.server.WarTimeController;
import l1j.server.server.command.executor.L1HpBar;
import l1j.server.server.datatables.CastleTable;
import l1j.server.server.datatables.CharacterTable;
import l1j.server.server.datatables.ExpTable;
import l1j.server.server.datatables.ItemTable;
import l1j.server.server.model.AcceleratorChecker;
import l1j.server.server.model.HpRegeneration;
import l1j.server.server.model.HpRegenerationByDoll;
import l1j.server.server.model.ItemMakeByDoll;
import l1j.server.server.model.L1Attack;
import l1j.server.server.model.L1CastleLocation;
import l1j.server.server.model.L1Character;
import l1j.server.server.model.L1ChatParty;
import l1j.server.server.model.L1Clan;
import l1j.server.server.model.L1DwarfForElfInventory;
import l1j.server.server.model.L1DwarfInventory;
import l1j.server.server.model.L1EquipmentSlot;
import l1j.server.server.model.L1ExcludingList;
import l1j.server.server.model.L1Inventory;
import l1j.server.server.model.L1Karma;
import l1j.server.server.model.L1Magic;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1Party;
import l1j.server.server.model.L1PartyRefresh;
import l1j.server.server.model.L1PcDeleteTimer;
import l1j.server.server.model.L1PcInventory;
import l1j.server.server.model.L1PinkName;
import l1j.server.server.model.L1Quest;
import l1j.server.server.model.L1Teleport;
import l1j.server.server.model.L1TownLocation;
import l1j.server.server.model.L1Trade;
import l1j.server.server.model.L1War;
import l1j.server.server.model.L1World;
import l1j.server.server.model.MpReductionByAwake;
import l1j.server.server.model.MpRegeneration;
import l1j.server.server.model.MpRegenerationByDoll;
import l1j.server.server.model.classes.L1ClassFeature;
import l1j.server.server.model.gametime.L1GameTimeCarrier;
import l1j.server.server.model.monitor.L1PcAutoUpdate;
import l1j.server.server.model.monitor.L1PcExpMonitor;
import l1j.server.server.model.monitor.L1PcGhostMonitor;
import l1j.server.server.model.monitor.L1PcHellMonitor;
import l1j.server.server.model.monitor.L1PcInvisDelay;
import l1j.server.server.model.skill.L1SkillId;
import l1j.server.server.model.skill.L1SkillUse;
import l1j.server.server.serverpackets.S_RedMessage;
import l1j.server.server.serverpackets.S_CastleMaster;
import l1j.server.server.serverpackets.S_Disconnect;
import l1j.server.server.serverpackets.S_DoActionGFX;
import l1j.server.server.serverpackets.S_DoActionShop;
import l1j.server.server.serverpackets.S_EquipmentSlot;
import l1j.server.server.serverpackets.S_Fight;
import l1j.server.server.serverpackets.S_Fishing;
import l1j.server.server.serverpackets.S_HPMeter;
import l1j.server.server.serverpackets.S_HPUpdate;
import l1j.server.server.serverpackets.S_Invis;
import l1j.server.server.serverpackets.S_Lawful;
import l1j.server.server.serverpackets.S_Liquor;
import l1j.server.server.serverpackets.S_MPUpdate;
import l1j.server.server.serverpackets.S_OtherCharPacks;
import l1j.server.server.serverpackets.S_OwnCharStatus;
import l1j.server.server.serverpackets.S_PacketBox;
import l1j.server.server.serverpackets.S_Poison;
import l1j.server.server.serverpackets.S_RemoveObject;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_SkillIconGFX;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.serverpackets.S_bonusstats;
import l1j.server.server.serverpackets.ServerBasePacket;
import l1j.server.server.templates.L1BookMark;
import l1j.server.server.templates.L1Castle;
import l1j.server.server.templates.L1Item;
import l1j.server.server.templates.L1MagicDoll;
import l1j.server.server.templates.L1PrivateShopBuyList;
import l1j.server.server.templates.L1PrivateShopSellList;
import l1j.server.server.utils.CalcStat;
import l1j.server.server.utils.Random;
import l1j.server.server.utils.collections.Lists;

// Referenced classes of package l1j.server.server.model:
// L1Character, L1DropTable, L1Object, L1ItemInstance,
// L1World
//

/**
 * 玩家角色實例類別 - 代表遊戲中的一個玩家角色
 *
 * <p>此類別是整個遊戲中最核心的類別之一,繼承自 {@link L1Character},
 * 代表一個完整的玩家角色,包含角色的所有屬性、狀態、技能、物品等。
 *
 * <h3>主要功能領域:</h3>
 * <ul>
 *   <li><b>基本屬性:</b> STR, DEX, CON, INT, WIS, CHA 及其衍生屬性</li>
 *   <li><b>職業系統:</b> 八大職業 (王族、騎士、妖精、法師、黑妖、龍騎士、幻術師)</li>
 *   <li><b>狀態管理:</b> HP/MP 回復、Buff/Debuff、狀態效果</li>
 *   <li><b>物品系統:</b> 背包、裝備、倉庫管理</li>
 *   <li><b>技能系統:</b> 技能學習、使用、冷卻時間</li>
 *   <li><b>社交系統:</b> 血盟、組隊、好友、交易</li>
 *   <li><b>戰鬥系統:</b> 攻擊、防禦、閃避、命中</li>
 *   <li><b>移動系統:</b> 座標、地圖、傳送</li>
 *   <li><b>任務系統:</b> 任務進度追蹤</li>
 *   <li><b>PK 系統:</b> 正義值、PKer 狀態、戰爭</li>
 *   <li><b>網路通訊:</b> 封包發送、客戶端連線管理</li>
 * </ul>
 *
 * <h3>職業 ID (ClassId) 常數:</h3>
 * <ul>
 *   <li>{@link #CLASSID_PRINCE} (0) - 王子</li>
 *   <li>{@link #CLASSID_PRINCESS} (1) - 公主</li>
 *   <li>{@link #CLASSID_KNIGHT_MALE} (61) - 男騎士</li>
 *   <li>{@link #CLASSID_KNIGHT_FEMALE} (48) - 女騎士</li>
 *   <li>{@link #CLASSID_ELF_MALE} (138) - 男妖精</li>
 *   <li>{@link #CLASSID_ELF_FEMALE} (37) - 女妖精</li>
 *   <li>{@link #CLASSID_WIZARD_MALE} (734) - 男法師</li>
 *   <li>{@link #CLASSID_WIZARD_FEMALE} (1186) - 女法師</li>
 *   <li>{@link #CLASSID_DARK_ELF_MALE} (2786) - 男黑妖</li>
 *   <li>{@link #CLASSID_DARK_ELF_FEMALE} (2796) - 女黑妖</li>
 *   <li>{@link #CLASSID_DRAGON_KNIGHT_MALE} (6658) - 男龍騎士</li>
 *   <li>{@link #CLASSID_DRAGON_KNIGHT_FEMALE} (6661) - 女龍騎士</li>
 *   <li>{@link #CLASSID_ILLUSIONIST_MALE} (6671) - 男幻術師</li>
 *   <li>{@link #CLASSID_ILLUSIONIST_FEMALE} (6650) - 女幻術師</li>
 * </ul>
 *
 * <h3>核心子系統:</h3>
 * <ul>
 *   <li><b>L1PcInventory:</b> 角色背包管理</li>
 *   <li><b>L1DwarfInventory:</b> 矮人倉庫</li>
 *   <li><b>L1Quest:</b> 任務系統</li>
 *   <li><b>L1EquipmentSlot:</b> 裝備欄位管理</li>
 *   <li><b>L1Party:</b> 組隊系統</li>
 *   <li><b>L1Clan:</b> 血盟系統</li>
 *   <li><b>L1Trade:</b> 交易系統</li>
 *   <li><b>L1Karma:</b> 正義值系統</li>
 *   <li><b>HpRegeneration:</b> HP 自動回復</li>
 *   <li><b>MpRegeneration:</b> MP 自動回復</li>
 *   <li><b>L1PcAutoUpdate:</b> 角色狀態自動更新</li>
 *   <li><b>L1PcExpMonitor:</b> 經驗值監控</li>
 * </ul>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * // 載入角色
 * L1PcInstance pc = L1PcInstance.load("CharacterName");
 *
 * // 設定網路連線
 * pc.setNetConnection(clientThread);
 * pc.setPacketOutput(clientThread);
 *
 * // 加入世界
 * L1World.getInstance().storeObject(pc);
 * L1World.getInstance().addVisibleObject(pc);
 *
 * // 發送封包
 * pc.sendPackets(new S_SystemMessage("歡迎回來"));
 *
 * // 增加經驗值
 * pc.addExp(1000);
 *
 * // 儲存角色資料
 * pc.save();
 * </pre>
 *
 * <h3>生命週期:</h3>
 * <ol>
 *   <li>載入 - {@code L1PcInstance.load()} 從資料庫載入角色資料</li>
 *   <li>初始化 - 設定網路連線、啟動定時器、載入物品</li>
 *   <li>遊戲中 - 處理各種遊戲邏輯、狀態更新</li>
 *   <li>登出 - 停止所有定時器、儲存資料、清理資源</li>
 * </ol>
 *
 * <h3>狀態更新機制:</h3>
 * <p>角色使用多個定時器進行週期性狀態更新:
 * <ul>
 *   <li>HP 回復定時器 (1秒)</li>
 *   <li>MP 回復定時器 (1秒)</li>
 *   <li>魔法娃娃 HP 回復 (64秒)</li>
 *   <li>魔法娃娃 MP 回復 (64秒)</li>
 *   <li>魔法娃娃製造道具 (240秒)</li>
 *   <li>覺醒技能 MP 消耗</li>
 *   <li>角色自動更新</li>
 *   <li>經驗值監控</li>
 * </ul>
 *
 * <h3>執行緒安全:</h3>
 * <p>此類別的許多方法會在不同執行緒中被呼叫 (網路執行緒、定時器執行緒等),
 * 因此部分關鍵狀態使用同步機制保護。
 *
 * <h3>持久化:</h3>
 * <p>角色資料會定期或在特定事件時儲存到資料庫:
 * <ul>
 *   <li>登出時</li>
 *   <li>死亡時</li>
 *   <li>升級時</li>
 *   <li>重要狀態變更時</li>
 * </ul>
 *
 * @see L1Character
 * @see L1PcInventory
 * @see L1Quest
 * @see L1Party
 * @see L1Clan
 * @see CharacterTable
 */
public class L1PcInstance extends L1Character {
	private static final long serialVersionUID = 1L;

	/** 職業 ID: 男騎士 */
	public static final int CLASSID_KNIGHT_MALE = 61;

	/** 職業 ID: 女騎士 */
	public static final int CLASSID_KNIGHT_FEMALE = 48;

	/** 職業 ID: 男妖精 */
	public static final int CLASSID_ELF_MALE = 138;

	/** 職業 ID: 女妖精 */
	public static final int CLASSID_ELF_FEMALE = 37;

	/** 職業 ID: 男法師 */
	public static final int CLASSID_WIZARD_MALE = 734;

	/** 職業 ID: 女法師 */
	public static final int CLASSID_WIZARD_FEMALE = 1186;

	/** 職業 ID: 男黑妖 */
	public static final int CLASSID_DARK_ELF_MALE = 2786;

	/** 職業 ID: 女黑妖 */
	public static final int CLASSID_DARK_ELF_FEMALE = 2796;

	/** 職業 ID: 王子 */
	public static final int CLASSID_PRINCE = 0;

	/** 職業 ID: 公主 */
	public static final int CLASSID_PRINCESS = 1;

	/** 職業 ID: 男龍騎士 */
	public static final int CLASSID_DRAGON_KNIGHT_MALE = 6658;

	/** 職業 ID: 女龍騎士 */
	public static final int CLASSID_DRAGON_KNIGHT_FEMALE = 6661;

	/** 職業 ID: 男幻術師 */
	public static final int CLASSID_ILLUSIONIST_MALE = 6671;

	/** 職業 ID: 女幻術師 */
	public static final int CLASSID_ILLUSIONIST_FEMALE = 6650;

	/** HP 自然回復力 (經過計算後的最終值,最小為 0) */
	private short _hpr = 0;

	/** HP 自然回復力的真實值 (可能為負數) */
	private short _trueHpr = 0;

	/** 3.3C組隊系統 - 組隊更新是否啟用 */
	boolean _rpActive = false;

	/** 組隊更新器 */
	private L1PartyRefresh _rp;

	/** 組隊類型 */
	private int _partyType;

	/**
	 * 取得 HP 自然回復力
	 * <p>最小值為 0 (不會是負數)
	 *
	 * @return HP 自然回復力
	 */
	public short getHpr() {
		return (short) _hpr ;
	}

	/**
	 * 增加 HP 自然回復力
	 * <p>實際值可能為負數,但顯示值最小為 0
	 *
	 * @param i 要增加的 HP 回復力 (可為負數表示減少)
	 */
	public void addHpr(int i) {
		_trueHpr += i;
		_hpr = (short) Math.max(0, _trueHpr);
	}

	/** MP 自然回復力 (經過計算後的最終值,最小為 0) */
	private short _mpr = 0;

	/** MP 自然回復力的真實值 (可能為負數) */
	private short _trueMpr = 0;

	/**
	 * 取得 MP 自然回復力
	 * <p>最小值為 0 (不會是負數)
	 *
	 * @return MP 自然回復力
	 */
	public short getMpr() {
		return (short) _mpr;
	}

	/**
	 * 增加 MP 自然回復力
	 * <p>實際值可能為負數,但顯示值最小為 0
	 *
	 * @param i 要增加的 MP 回復力 (可為負數表示減少)
	 */
	public void addMpr(int i) {
		_trueMpr += i;
		_mpr = (short) Math.max(0, _trueMpr);
	}

	/** 原始 CON 帶來的 HP 自然回復力 */
	public short _originalHpr = 0;

	/**
	 * 取得原始 HP 自然回復力 (來自 CON)
	 *
	 * @return 原始 HP 自然回復力
	 */
	public short getOriginalHpr() {

		return _originalHpr;
	}

	/** 原始 WIS 帶來的 MP 自然回復力 */
	public short _originalMpr = 0;

	/**
	 * 取得原始 MP 自然回復力 (來自 WIS)
	 *
	 * @return 原始 MP 自然回復力
	 */
	public short getOriginalMpr() {

		return _originalMpr;
	}

	/**
	 * 啟動 HP 自動回復定時器
	 * <p>每 1 秒執行一次 HP 回復檢查
	 * <p>若已經啟動則不會重複啟動
	 */
	public void startHpRegeneration() {
		final int INTERVAL = 1000;

		if (!_hpRegenActive) {
			_hpRegen = new HpRegeneration(this);
			_regenTimer.scheduleAtFixedRate(_hpRegen, INTERVAL, INTERVAL);
			_hpRegenActive = true;
		}
	}

	/**
	 * 停止 HP 自動回復定時器
	 * <p>通常在角色登出或死亡時呼叫
	 */
	public void stopHpRegeneration() {
		if (_hpRegenActive) {
			_hpRegen.cancel();
			_hpRegen = null;
			_hpRegenActive = false;
		}
	}

	/**
	 * 啟動 MP 自動回復定時器
	 * <p>每 1 秒執行一次 MP 回復檢查
	 * <p>若已經啟動則不會重複啟動
	 */
	public void startMpRegeneration() {
		final int INTERVAL = 1000;

		if (!_mpRegenActive) {
			_mpRegen = new MpRegeneration(this);
			_regenTimer.scheduleAtFixedRate(_mpRegen, INTERVAL, INTERVAL);
			_mpRegenActive = true;
		}
	}

	/**
	 * 停止 MP 自動回復定時器
	 * <p>通常在角色登出或死亡時呼叫
	 */
	public void stopMpRegeneration() {
		if (_mpRegenActive) {
			_mpRegen.cancel();
			_mpRegen = null;
			_mpRegenActive = false;
		}
	}

	/**
	 * 啟動魔法娃娃製造道具定時器
	 * <p>每 240 秒 (4 分鐘) 執行一次
	 * <p>需要擁有具備製造道具能力的魔法娃娃才會啟動
	 */
	public void startItemMakeByDoll() {
		final int INTERVAL_BY_DOLL = 240000;
		boolean isExistItemMakeDoll = false;
		if (L1MagicDoll.isItemMake(this)) {
			isExistItemMakeDoll = true;
		}
		if (!_ItemMakeActiveByDoll && isExistItemMakeDoll) {
			_itemMakeByDoll = new ItemMakeByDoll(this);
			_regenTimer.scheduleAtFixedRate(_itemMakeByDoll, INTERVAL_BY_DOLL,
					INTERVAL_BY_DOLL);
			_ItemMakeActiveByDoll = true;
		}
	}

	/**
	 * 停止魔法娃娃製造道具定時器
	 */
	public void stopItemMakeByDoll() {
		if (_ItemMakeActiveByDoll) {
			_itemMakeByDoll.cancel();
			_itemMakeByDoll = null;
			_ItemMakeActiveByDoll = false;
		}
	}

	/**
	 * 啟動魔法娃娃 HP 回復定時器
	 * <p>每 64 秒執行一次
	 * <p>需要擁有具備 HP 回復能力的魔法娃娃才會啟動
	 */
	public void startHpRegenerationByDoll() {
		final int INTERVAL_BY_DOLL = 64000;
		boolean isExistHprDoll = false;
		if (L1MagicDoll.isHpRegeneration(this)) {
			isExistHprDoll = true;
		}
		if (!_hpRegenActiveByDoll && isExistHprDoll) {
			_hpRegenByDoll = new HpRegenerationByDoll(this);
			_regenTimer.scheduleAtFixedRate(_hpRegenByDoll, INTERVAL_BY_DOLL,
					INTERVAL_BY_DOLL);
			_hpRegenActiveByDoll = true;
		}
	}

	/**
	 * 停止魔法娃娃 HP 回復定時器
	 */
	public void stopHpRegenerationByDoll() {
		if (_hpRegenActiveByDoll) {
			_hpRegenByDoll.cancel();
			_hpRegenByDoll = null;
			_hpRegenActiveByDoll = false;
		}
	}

	/**
	 * 啟動由魔法娃娃提供的MP回復機制
	 * <p>
	 * 當玩家裝備具有MP回復效果的魔法娃娃時，此方法會啟動定時任務，
	 * 每64秒自動回復MP。
	 * </p>
	 */
	public void startMpRegenerationByDoll() {
		final int INTERVAL_BY_DOLL = 64000;
		boolean isExistMprDoll = false;
		if (L1MagicDoll.isMpRegeneration(this)) {
			isExistMprDoll = true;
		}
		if (!_mpRegenActiveByDoll && isExistMprDoll) {
			_mpRegenByDoll = new MpRegenerationByDoll(this);
			_regenTimer.scheduleAtFixedRate(_mpRegenByDoll, INTERVAL_BY_DOLL, INTERVAL_BY_DOLL);
			_mpRegenActiveByDoll = true;
		}
	}

	/**
	 * 停止由魔法娃娃提供的MP回復機制
	 * <p>
	 * 當玩家取消裝備魔法娃娃或娃娃效果結束時，停止MP自動回復的定時任務。
	 * </p>
	 */
	public void stopMpRegenerationByDoll() {
		if (_mpRegenActiveByDoll) {
			_mpRegenByDoll.cancel();
			_mpRegenByDoll = null;
			_mpRegenActiveByDoll = false;
		}
	}

	/**
	 * 啟動覺醒技能的MP消耗機制
	 * <p>
	 * 當玩家使用覺醒(Awake)技能時，每4秒會自動扣除一定量的MP。
	 * 此方法啟動定時任務來執行MP扣除。
	 * </p>
	 */
	public void startMpReductionByAwake() {
		final int INTERVAL_BY_AWAKE = 4000;
		if (!_mpReductionActiveByAwake) {
			_mpReductionByAwake = new MpReductionByAwake(this);
			_regenTimer.scheduleAtFixedRate(_mpReductionByAwake, INTERVAL_BY_AWAKE, INTERVAL_BY_AWAKE);
			_mpReductionActiveByAwake = true;
		}
	}

	/**
	 * 停止覺醒技能的MP消耗機制
	 * <p>
	 * 當覺醒技能效果結束時，停止MP自動扣除的定時任務。
	 * </p>
	 */
	public void stopMpReductionByAwake() {
		if (_mpReductionActiveByAwake) {
			_mpReductionByAwake.cancel();
			_mpReductionByAwake = null;
			_mpReductionActiveByAwake = false;
		}
	}

	/**
	 * 啟動物件自動更新機制
	 * <p>
	 * 此方法會移除所有已知物件，並啟動定時任務來自動更新玩家周圍的物件狀態。
	 * 更新頻率由INTERVAL_AUTO_UPDATE常數定義(300毫秒)。
	 * </p>
	 */
	public void startObjectAutoUpdate() {
		removeAllKnownObjects();
		_autoUpdateFuture = GeneralThreadPool.getInstance().pcScheduleAtFixedRate(new L1PcAutoUpdate(getId()), 0L, INTERVAL_AUTO_UPDATE);
	}

	/**
	 * 各種モニタータスクを停止する。
	 */
	public void stopEtcMonitor() {
		if (_autoUpdateFuture != null) {
			_autoUpdateFuture.cancel(true);
			_autoUpdateFuture = null;
		}
		if (_expMonitorFuture != null) {
			_expMonitorFuture.cancel(true);
			_expMonitorFuture = null;
		}
		if (_ghostFuture != null) {
			_ghostFuture.cancel(true);
			_ghostFuture = null;
		}

		if (_hellFuture != null) {
			_hellFuture.cancel(true);
			_hellFuture = null;
		}

	}

	/** 物件自動更新的時間間隔(毫秒) */
	private static final long INTERVAL_AUTO_UPDATE = 300;

	/** 物件自動更新的定時任務 */
	private ScheduledFuture<?> _autoUpdateFuture;

	/** 經驗值監控的時間間隔(毫秒) */
	private static final long INTERVAL_EXP_MONITOR = 500;

	/** 經驗值監控的定時任務 */
	private ScheduledFuture<?> _expMonitorFuture;

	/**
	 * 處理經驗值變化事件
	 * <p>
	 * 當玩家經驗值改變時，此方法會檢查是否達到升級或降級條件，
	 * 並執行相應的等級變更處理。
	 * </p>
	 */
	public void onChangeExp() {
		int level = ExpTable.getLevelByExp(getExp());
		int char_level = getLevel();
		int gap = level - char_level;
		if (gap == 0) {
			sendPackets(new S_OwnCharStatus(this));
			//sendPackets(new S_Exp(this));
			return;
		}

		// レベルが変化した場合
		if (gap > 0) {
			levelUp(gap);
		}
		else if (gap < 0) {
			levelDown(gap);
		}
	}

	@Override
	public void onPerceive(L1PcInstance perceivedFrom) {
		// 判斷旅館內是否使用相同鑰匙
		if (perceivedFrom.getMapId() >= 16384 && perceivedFrom.getMapId() <= 25088 // 旅館內判斷
				&& perceivedFrom.getInnKeyId() != getInnKeyId()) {
			return;
		}
		if (isGmInvis() || isGhost()) {
			return;
		}
		if (isInvisble() && !perceivedFrom.hasSkillEffect(GMSTATUS_FINDINVIS)) {
			return;
		}

		perceivedFrom.addKnownObject(this);
		perceivedFrom.sendPackets(new S_OtherCharPacks(this, perceivedFrom.hasSkillEffect(GMSTATUS_FINDINVIS))); // 自分の情報を送る
		if (isInParty() && getParty().isMember(perceivedFrom)) { // PTメンバーならHPメーターも送る
			perceivedFrom.sendPackets(new S_HPMeter(this));
		}

		if (isPrivateShop()) { // 開個人商店中
			perceivedFrom.sendPackets(new S_DoActionShop(getId(), ActionCodes.ACTION_Shop, getShopChat()));
		} else if (isFishing()) { // 釣魚中
			perceivedFrom.sendPackets(new S_Fishing(getId(), ActionCodes.ACTION_Fishing, getFishX(), getFishY()));
		}

		if (isCrown()) { // 君主
			L1Clan clan = L1World.getInstance().getClan(getClanname());
			if (clan != null) {
				if ((getId() == clan.getLeaderId() // 血盟主で城主クラン
						)
						&& (clan.getCastleId() != 0)) {
					perceivedFrom.sendPackets(new S_CastleMaster(clan.getCastleId(), getId()));
				}
			}
		}
	}

	/**
	 * 移除超出範圍的已知物件
	 * <p>
	 * 此方法會檢查玩家已知的所有物件，如果物件已經超出玩家的視野範圍，
	 * 則從已知物件列表中移除，並發送移除物件的封包給客戶端。
	 * </p>
	 */
	private void removeOutOfRangeObjects() {
		for (L1Object known : getKnownObjects()) {
			if (known == null) {
				continue;
			}

			if (Config.PC_RECOGNIZE_RANGE == -1) {
				if (!getLocation().isInScreen(known.getLocation())) { // 画面外
					removeKnownObject(known);
					sendPackets(new S_RemoveObject(known));
				}
			}
			else {
				if (getLocation().getTileLineDistance(known.getLocation()) > Config.PC_RECOGNIZE_RANGE) {
					removeKnownObject(known);
					sendPackets(new S_RemoveObject(known));
				}
			}
		}
	}

	/**
	 * 更新範圍內的物件
	 * <p>
	 * 此方法會執行以下操作：
	 * 1. 移除超出範圍的已知物件
	 * 2. 檢測範圍內的新物件並加入已知物件列表
	 * 3. 處理NPC的隱藏狀態
	 * 4. 如果啟用HP條顯示，發送HP條資訊
	 * 5. 特別處理旅館地圖的房間隔離邏輯
	 * </p>
	 */
	public void updateObject() {
		removeOutOfRangeObjects();

		if (getMapId() <= 10000) {
			for (L1Object visible : L1World.getInstance().getVisibleObjects(this, Config.PC_RECOGNIZE_RANGE)) {
				if (!knownsObject(visible)) {
					visible.onPerceive(this);
				}
				else {
					if (visible instanceof L1NpcInstance) {
						L1NpcInstance npc = (L1NpcInstance) visible;
						if (getLocation().isInScreen(npc.getLocation()) && (npc.getHiddenStatus() != 0)) {
							npc.approachPlayer(this);
						}
					}
				}
				if (hasSkillEffect(GMSTATUS_HPBAR) && L1HpBar.isHpBarTarget(visible)) {
					sendPackets(new S_HPMeter((L1Character) visible));
				}
			}
		} else { // 旅館內判斷
			for (L1Object visible : L1World.getInstance().getVisiblePlayer(this)) {
				if (!knownsObject(visible)) {
					visible.onPerceive(this);
				}
				if (hasSkillEffect(GMSTATUS_HPBAR) && L1HpBar.isHpBarTarget(visible)) {
					if (getInnKeyId() == ((L1Character) visible).getInnKeyId()) {
						sendPackets(new S_HPMeter((L1Character) visible));
					}
				}
			}
		}
	}

	/**
	 * 發送視覺效果封包
	 * <p>
	 * 此方法會根據玩家當前的狀態（中毒、麻痺等）發送對應的視覺效果封包
	 * 給玩家自己及周圍的其他玩家。麻痺效果優先於中毒效果顯示。
	 * </p>
	 */
	private void sendVisualEffect() {
		int poisonId = 0;
		if (getPoison() != null) { // 毒状態
			poisonId = getPoison().getEffectId();
		}
		if (getParalysis() != null) { // 麻痺状態
			// 麻痺エフェクトを優先して送りたい為、poisonIdを上書き。
			poisonId = getParalysis().getEffectId();
		}
		if (poisonId != 0) { // このifはいらないかもしれない
			sendPackets(new S_Poison(getId(), poisonId));
			broadcastPacket(new S_Poison(getId(), poisonId));
		}
	}

	/**
	 * 在登入時發送視覺效果
	 * <p>
	 * 玩家登入時會執行以下處理：
	 * 1. 發送所有城堡的主人資訊
	 * 2. 如果玩家是血盟主且血盟擁有城堡，發送城主標記
	 * 3. 發送玩家當前的中毒、麻痺等狀態效果
	 * </p>
	 */
	public void sendVisualEffectAtLogin() {
		for (L1Castle ca : CastleTable.getInstance().getCastleTableList()) {
			sendPackets(new S_CastleMaster(ca.getId(), ca.getHeldClanId() > 0 ? ca.getHeldClanId() : 0));
		}
		
		if (getClanid() != 0) { // クラン所属
			L1Clan clan = L1World.getInstance().getClan(getClanname());
			if (clan != null) {
				// プリンスまたはプリンセス、かつ、血盟主で自クランが城主
				if (isCrown() && (getId() == clan.getLeaderId()) && (clan.getCastleId() != 0)) {
					sendPackets(new S_CastleMaster(clan.getCastleId(), getId()));
				}
			}
		}

		sendVisualEffect();
	}

	/**
	 * 在傳送時發送視覺效果
	 * <p>
	 * 玩家傳送後需要重新發送某些視覺效果：
	 * 1. 如果玩家處於醉酒狀態，發送醉酒效果
	 * 2. 發送中毒、麻痺等狀態效果
	 * </p>
	 */
	public void sendVisualEffectAtTeleport() {
		if (isDrink()) { // liquorで酔っている
			sendPackets(new S_Liquor(getId(), 1));
		}

		sendVisualEffect();
	}

	/** 玩家已學習的技能列表 */
	private List<Integer> skillList = Lists.newList();

	/**
	 * 設定玩家已學習的技能
	 *
	 * @param skillid 技能ID
	 */
	public void setSkillMastery(int skillid) {
		if (!skillList.contains(skillid)) {
			skillList.add(skillid);
		}
	}

	/**
	 * 移除玩家已學習的技能
	 *
	 * @param skillid 技能ID
	 */
	public void removeSkillMastery(int skillid) {
		if (skillList.contains(skillid)) {
			skillList.remove((Object) skillid);
		}
	}

	/**
	 * 檢查玩家是否已學習指定技能
	 *
	 * @param skillid 技能ID
	 * @return 如果已學習返回true，否則返回false
	 */
	public boolean isSkillMastery(int skillid) {
		return skillList.contains(skillid);
	}

	/**
	 * 清除玩家所有已學習的技能
	 */
	public void clearSkillMastery() {
		skillList.clear();
	}

	/** 寵物競速：當前圈數 */
	private int _lap = 1;

	/**
	 * 設定寵物競速的圈數
	 *
	 * @param i 圈數
	 */
	public void setLap(int i) {
		_lap = i;
	}

	/**
	 * 取得寵物競速的圈數
	 *
	 * @return 當前圈數
	 */
	public int getLap() {
		return _lap;
	}

	/** 寵物競速：圈內檢查點編號 */
	private int _lapCheck = 0;

	/**
	 * 設定寵物競速的檢查點編號
	 *
	 * @param i 檢查點編號
	 */
	public void setLapCheck(int i) {
		_lapCheck = i;
	}

	/**
	 * 取得寵物競速的檢查點編號
	 *
	 * @return 當前檢查點編號
	 */
	public int getLapCheck() {
		return _lapCheck;
	}

	/**
	 * 取得寵物競速的總分數
	 * <p>
	 * 將總圈數的完成進度數量化，用於排名比較。
	 * 計算公式：圈數 * 29 + 檢查點編號
	 * </p>
	 *
	 * @return 競速分數
	 */
	public int getLapScore() {
		return _lap * 29 + _lapCheck;
	}

	/** 是否在命令列表中 */
	private boolean _order_list = false;

	/**
	 * 檢查是否在命令列表中
	 *
	 * @return 如果在列表中返回true
	 */
	public boolean isInOrderList() {
		return _order_list;
	}

	/**
	 * 設定是否在命令列表中
	 *
	 * @param bool 是否在列表中
	 */
	public void setInOrderList(boolean bool) {
		_order_list = bool;
	}

	/**
	 * L1PcInstance建構子
	 * <p>
	 * 初始化玩家實例的基本屬性，包括：
	 * - 存取權限等級
	 * - 當前武器
	 * - 各種背包(一般背包、矮人倉庫、精靈倉庫、交易視窗)
	 * - 書籤列表
	 * - 任務物件
	 * - 裝備欄位
	 * </p>
	 */
	public L1PcInstance() {
		_accessLevel = 0;
		_currentWeapon = 0;
		_inventory = new L1PcInventory(this);
		_dwarf = new L1DwarfInventory(this);
		_dwarfForElf = new L1DwarfForElfInventory(this);
		_tradewindow = new L1Inventory();
		_bookmarks = Lists.newList();
		_quest = new L1Quest(this);
		_equipSlot = new L1EquipmentSlot(this); // コンストラクタでthisポインタを渡すのは安全だろうか・・・
	}

	@Override
	public void setCurrentHp(int i) {
		if (getCurrentHp() == i) {
			return;
		}
		int currentHp = i;
		if (currentHp >= getMaxHp()) {
			currentHp = getMaxHp();
		}
		setCurrentHpDirect(currentHp);
		sendPackets(new S_HPUpdate(currentHp, getMaxHp()));
		if (isInParty()) { // パーティー中
			getParty().updateMiniHP(this);
		}
	}

	@Override
	public void setCurrentMp(int i) {
		if (getCurrentMp() == i) {
			return;
		}
		int currentMp = i;
		if ((currentMp >= getMaxMp()) || isGm()) {
			currentMp = getMaxMp();
		}
		setCurrentMpDirect(currentMp);
		sendPackets(new S_MPUpdate(currentMp, getMaxMp()));
	}

	@Override
	public L1PcInventory getInventory() {
		return _inventory;
	}

	/**
	 * 取得矮人倉庫
	 *
	 * @return 矮人倉庫實例
	 */
	public L1DwarfInventory getDwarfInventory() {
		return _dwarf;
	}

	/**
	 * 取得精靈專用矮人倉庫
	 *
	 * @return 精靈矮人倉庫實例
	 */
	public L1DwarfForElfInventory getDwarfForElfInventory() {
		return _dwarfForElf;
	}

	/**
	 * 取得交易視窗背包
	 *
	 * @return 交易視窗背包實例
	 */
	public L1Inventory getTradeWindowInventory() {
		return _tradewindow;
	}

	/**
	 * 檢查是否處於GM隱身狀態
	 *
	 * @return 如果處於GM隱身狀態返回true
	 */
	public boolean isGmInvis() {
		return _gmInvis;
	}

	/**
	 * 設定GM隱身狀態
	 *
	 * @param flag 是否隱身
	 */
	public void setGmInvis(boolean flag) {
		_gmInvis = flag;
	}

	/**
	 * 取得當前裝備的武器類型
	 *
	 * @return 武器類型ID
	 */
	public int getCurrentWeapon() {
		return _currentWeapon;
	}

	/**
	 * 設定當前裝備的武器類型
	 *
	 * @param i 武器類型ID
	 */
	public void setCurrentWeapon(int i) {
		_currentWeapon = i;
	}

	/**
	 * 取得角色類型
	 *
	 * @return 角色類型
	 */
	public int getType() {
		return _type;
	}

	/**
	 * 設定角色類型
	 *
	 * @param i 角色類型
	 */
	public void setType(int i) {
		_type = i;
	}

	/**
	 * 取得存取權限等級
	 * <p>
	 * 權限等級決定玩家可使用的GM指令範圍
	 * </p>
	 *
	 * @return 存取權限等級
	 */
	public short getAccessLevel() {
		return _accessLevel;
	}

	/**
	 * 設定存取權限等級
	 *
	 * @param i 存取權限等級
	 */
	public void setAccessLevel(short i) {
		_accessLevel = i;
	}

	/**
	 * 取得職業ID
	 *
	 * @return 職業ID
	 */
	public int getClassId() {
		return _classId;
	}

	/**
	 * 設定職業ID
	 * <p>
	 * 設定職業ID時會同時初始化對應的職業特性物件
	 * </p>
	 *
	 * @param i 職業ID
	 */
	public void setClassId(int i) {
		_classId = i;
		_classFeature = L1ClassFeature.newClassFeature(i);
	}

	/** 職業特性物件 */
	private L1ClassFeature _classFeature = null;

	/**
	 * 取得職業特性物件
	 *
	 * @return 職業特性物件
	 */
	public L1ClassFeature getClassFeature() {
		return _classFeature;
	}

	@Override
	public synchronized int getExp() {
		return _exp;
	}

	@Override
	public synchronized void setExp(int i) {
		_exp = i;
	}

	/** PK計數 */
	private int _PKcount;

	/**
	 * 取得PK計數
	 *
	 * @return PK計數值
	 */
	public int get_PKcount() {
		return _PKcount;
	}

	/**
	 * 設定PK計數
	 *
	 * @param i PK計數值
	 */
	public void set_PKcount(int i) {
		_PKcount = i;
	}

	/** PK計數(精靈專用) */
	private int _PkCountForElf;

	/**
	 * 取得精靈的PK計數
	 *
	 * @return 精靈PK計數值
	 */
	public int getPkCountForElf() {
		return _PkCountForElf;
	}

	/**
	 * 設定精靈的PK計數
	 *
	 * @param i 精靈PK計數值
	 */
	public void setPkCountForElf(int i) {
		_PkCountForElf = i;
	}

	/** 血盟ID */
	private int _clanid;

	/**
	 * 取得血盟ID
	 *
	 * @return 血盟ID
	 */
	public int getClanid() {
		return _clanid;
	}

	/**
	 * 設定血盟ID
	 *
	 * @param i 血盟ID
	 */
	public void setClanid(int i) {
		_clanid = i;
	}

	/** 血盟名稱 */
	private String clanname;

	/**
	 * 取得血盟名稱
	 *
	 * @return 血盟名稱
	 */
	public String getClanname() {
		return clanname;
	}

	/**
	 * 設定血盟名稱
	 *
	 * @param s 血盟名稱
	 */
	public void setClanname(String s) {
		clanname = s;
	}

	/**
	 * 取得血盟物件
	 *
	 * @return 血盟物件，如果未加入血盟則返回null
	 */
	public L1Clan getClan() {
		return L1World.getInstance().getClan(getClanname());
	}

	/** 血盟階級(血盟君主、守護騎士、一般、見習) */
	private int _clanRank;

	/**
	 * 取得血盟階級
	 *
	 * @return 血盟階級
	 */
	public int getClanRank() {
		return _clanRank;
	}

	/**
	 * 設定血盟階級
	 *
	 * @param i 血盟階級
	 */
	public void setClanRank(int i) {
		_clanRank = i;
	}

	/** 血盟成員ID */
	private int _clanMemberId;

	/**
	 * 取得血盟成員ID
	 *
	 * @return 血盟成員ID
	 */
	public int getClanMemberId() {
		return _clanMemberId;
	}

	/**
	 * 設定血盟成員ID
	 *
	 * @param i 血盟成員ID
	 */
	public void setClanMemberId(int i) {
		_clanMemberId = i;
	}

	/** 血盟成員備註 */
	private String _clanMemberNotes;

	/**
	 * 取得血盟成員備註
	 *
	 * @return 備註內容
	 */
	public String getClanMemberNotes() {
		return _clanMemberNotes;
	}

	/**
	 * 設定血盟成員備註
	 *
	 * @param s 備註內容
	 */
	public void setClanMemberNotes(String s) {
		_clanMemberNotes = s;
	}
	

	/** 角色生日 */
	private Timestamp _birthday;

	/**
	 * 取得角色生日
	 *
	 * @return 生日時間戳記
	 */
	public Timestamp getBirthday() {
		return _birthday;
	}

	/**
	 * 取得簡化的生日格式
	 * <p>
	 * 將生日轉換為yyyyMMdd格式的整數，例如：20231225
	 * </p>
	 *
	 * @return 生日整數(yyyyMMdd格式)，如果未設定生日則返回0
	 */
	public int getSimpleBirthday(){
		if (_birthday != null){
			SimpleDateFormat SimpleDate = new SimpleDateFormat("yyyyMMdd");
			int BornTime = Integer.parseInt(SimpleDate.format(_birthday.getTime()));
			return BornTime;
		}
		else {
			return 0;
		}
	}	

	/**
	 * 設定角色生日
	 *
	 * @param time 生日時間戳記
	 */
	public void setBirthday(Timestamp time) {
		_birthday = time;
	}

	/**
	 * 設定角色生日為當前時間
	 */
	public void setBirthday(){
		_birthday = new Timestamp(System.currentTimeMillis());
	}

	/** 性別 */
	private byte _sex;

	/**
	 * 取得角色性別
	 *
	 * @return 性別(0:男性, 1:女性)
	 */
	public byte get_sex() {
		return _sex;
	}

	/**
	 * 設定角色性別
	 *
	 * @param i 性別(0:男性, 1:女性)
	 */
	public void set_sex(int i) {
		_sex = (byte) i;
	}

	/**
	 * 檢查是否為GM
	 *
	 * @return 如果是GM返回true
	 */
	public boolean isGm() {
		return _gm;
	}

	/**
	 * 設定是否為GM
	 *
	 * @param flag 是否為GM
	 */
	public void setGm(boolean flag) {
		_gm = flag;
	}

	/**
	 * 檢查是否為監視者
	 *
	 * @return 如果是監視者返回true
	 */
	public boolean isMonitor() {
		return _monitor;
	}

	/**
	 * 設定是否為監視者
	 *
	 * @param flag 是否為監視者
	 */
	public void setMonitor(boolean flag) {
		_monitor = flag;
	}

	private L1PcInstance getStat() {
		return null;
	}

	public void reduceCurrentHp(double d, L1Character l1character) {
		getStat().reduceCurrentHp(d, l1character);
	}

	/**
	 * 指定されたプレイヤー群にログアウトしたことを通知する
	 * 
	 * @param playersList
	 *            通知するプレイヤーの配列
	 */
	private void notifyPlayersLogout(List<L1PcInstance> playersArray) {
		for (L1PcInstance player : playersArray) {
			if (player.knownsObject(this)) {
				player.removeKnownObject(this);
				player.sendPackets(new S_RemoveObject(this));
			}
		}
	}

	/**
	 * 處理玩家登出
	 * <p>
	 * 執行以下操作：
	 * 1. 解除血盟倉庫鎖定(如果正在使用)
	 * 2. 通知其他玩家此玩家已登出
	 * 3. 從世界移除此玩家物件
	 * 4. 清空背包和倉庫物品列表
	 * 5. 停止HP/MP回復
	 * 6. 清除網路連接
	 * </p>
	 */
	public void logout() {
		L1World world = L1World.getInstance();
		if (getClanid() != 0) // クラン所属
		{
			L1Clan clan = world.getClan(getClanname());
			if (clan != null) {
				if (clan.getWarehouseUsingChar() == getId()) // 自キャラがクラン倉庫使用中
				{
					clan.setWarehouseUsingChar(0); // クラン倉庫のロックを解除
				}
			}
		}
		notifyPlayersLogout(getKnownPlayers());
		world.removeVisibleObject(this);
		world.removeObject(this);
		notifyPlayersLogout(world.getRecognizePlayer(this));
		_inventory.clearItems();
		_dwarf.clearItems();
		removeAllKnownObjects();
		stopHpRegeneration();
		stopMpRegeneration();
		setDead(true); // 使い方おかしいかもしれないけど、ＮＰＣに消滅したことをわからせるため
		setNetConnection(null);
		setPacketOutput(null);
	}

	/**
	 * 取得網路連接執行緒
	 *
	 * @return 客戶端執行緒物件
	 */
	public ClientThread getNetConnection() {
		return _netConnection;
	}

	/**
	 * 設定網路連接執行緒
	 *
	 * @param clientthread 客戶端執行緒物件
	 */
	public void setNetConnection(ClientThread clientthread) {
		_netConnection = clientthread;
	}

	/**
	 * 檢查是否在隊伍中
	 *
	 * @return 如果在隊伍中返回true
	 */
	public boolean isInParty() {
		return getParty() != null;
	}

	/**
	 * 取得所屬隊伍
	 *
	 * @return 隊伍物件，如果未組隊則返回null
	 */
	public L1Party getParty() {
		return _party;
	}

	/**
	 * 設定所屬隊伍
	 *
	 * @param p 隊伍物件
	 */
	public void setParty(L1Party p) {
		_party = p;
	}

	/**
	 * 檢查是否在聊天頻道中
	 *
	 * @return 如果在聊天頻道中返回true
	 */
	public boolean isInChatParty() {
		return getChatParty() != null;
	}

	/**
	 * 取得聊天頻道
	 *
	 * @return 聊天頻道物件，如果未加入則返回null
	 */
	public L1ChatParty getChatParty() {
		return _chatParty;
	}

	/**
	 * 設定聊天頻道
	 *
	 * @param cp 聊天頻道物件
	 */
	public void setChatParty(L1ChatParty cp) {
		_chatParty = cp;
	}

	/**
	 * 取得隊伍ID
	 *
	 * @return 隊伍ID
	 */
	public int getPartyID() {
		return _partyID;
	}

	/**
	 * 設定隊伍ID
	 *
	 * @param partyID 隊伍ID
	 */
	public void setPartyID(int partyID) {
		_partyID = partyID;
	}

	/**
	 * 取得交易對象ID
	 *
	 * @return 交易對象ID
	 */
	public int getTradeID() {
		return _tradeID;
	}

	/**
	 * 設定交易對象ID
	 *
	 * @param tradeID 交易對象ID
	 */
	public void setTradeID(int tradeID) {
		_tradeID = tradeID;
	}

	/**
	 * 設定交易確認狀態
	 *
	 * @param tradeOk 是否已確認交易
	 */
	public void setTradeOk(boolean tradeOk) {
		_tradeOk = tradeOk;
	}

	/**
	 * 取得交易確認狀態
	 *
	 * @return 是否已確認交易
	 */
	public boolean getTradeOk() {
		return _tradeOk;
	}

	/**
	 * 取得臨時ID
	 *
	 * @return 臨時ID
	 */
	public int getTempID() {
		return _tempID;
	}

	/**
	 * 設定臨時ID
	 *
	 * @param tempID 臨時ID
	 */
	public void setTempID(int tempID) {
		_tempID = tempID;
	}

	/**
	 * 檢查是否正在傳送中
	 *
	 * @return 如果正在傳送返回true
	 */
	public boolean isTeleport() {
		return _isTeleport;
	}

	/**
	 * 設定傳送狀態
	 *
	 * @param flag 是否正在傳送
	 */
	public void setTeleport(boolean flag) {
		_isTeleport = flag;
	}

	/**
	 * 檢查是否處於醉酒狀態
	 *
	 * @return 如果醉酒返回true
	 */
	public boolean isDrink() {
		return _isDrink;
	}

	/**
	 * 設定醉酒狀態
	 *
	 * @param flag 是否醉酒
	 */
	public void setDrink(boolean flag) {
		_isDrink = flag;
	}

	/**
	 * 檢查是否處於灰色(Gres)狀態
	 *
	 * @return 如果處於灰色狀態返回true
	 */
	public boolean isGres() {
		return _isGres;
	}

	/**
	 * 設定灰色(Gres)狀態
	 *
	 * @param flag 是否處於灰色狀態
	 */
	public void setGres(boolean flag) {
		_isGres = flag;
	}

	/**
	 * 檢查是否為粉紅名稱(紫變狀態)
	 *
	 * @return 如果為粉紅名稱返回true
	 */
	public boolean isPinkName() {
		return _isPinkName;
	}

	/**
	 * 設定粉紅名稱(紫變狀態)
	 *
	 * @param flag 是否為粉紅名稱
	 */
	public void setPinkName(boolean flag) {
		_isPinkName = flag;
	}

	/** 個人商店販賣物品列表 */
	private List<L1PrivateShopSellList> _sellList = Lists.newList();

	/**
	 * 取得個人商店販賣物品列表
	 *
	 * @return 販賣物品列表
	 */
	public List<L1PrivateShopSellList> getSellList() {
		return _sellList;
	}

	/** 個人商店收購物品列表 */
	private List<L1PrivateShopBuyList> _buyList = Lists.newList();

	/**
	 * 取得個人商店收購物品列表
	 *
	 * @return 收購物品列表
	 */
	public List<L1PrivateShopBuyList> getBuyList() {
		return _buyList;
	}

	/** 個人商店公告訊息 */
	private byte[] _shopChat;

	/**
	 * 設定個人商店公告訊息
	 *
	 * @param chat 公告訊息(位元組陣列)
	 */
	public void setShopChat(byte[] chat) {
		_shopChat = chat;
	}

	/**
	 * 取得個人商店公告訊息
	 *
	 * @return 公告訊息(位元組陣列)
	 */
	public byte[] getShopChat() {
		return _shopChat;
	}

	/** 是否正在開設個人商店 */
	private boolean _isPrivateShop = false;

	/**
	 * 檢查是否正在開設個人商店
	 *
	 * @return 如果正在開設個人商店返回true
	 */
	public boolean isPrivateShop() {
		return _isPrivateShop;
	}

	/**
	 * 設定是否正在開設個人商店
	 *
	 * @param flag 是否正在開設個人商店
	 */
	public void setPrivateShop(boolean flag) {
		_isPrivateShop = flag;
	}

	/** 是否正在與個人商店交易 */
	private boolean _isTradingInPrivateShop = false;

	/**
	 * 檢查是否正在與個人商店交易
	 *
	 * @return 如果正在交易返回true
	 */
	public boolean isTradingInPrivateShop() {
		return _isTradingInPrivateShop;
	}

	/**
	 * 設定是否正在與個人商店交易
	 *
	 * @param flag 是否正在交易
	 */
	public void setTradingInPrivateShop(boolean flag) {
		_isTradingInPrivateShop = flag;
	}

	/** 正在瀏覽的個人商店的物品數量 */
	private int _partnersPrivateShopItemCount = 0;

	/**
	 * 取得正在瀏覽的個人商店的物品數量
	 *
	 * @return 物品數量
	 */
	public int getPartnersPrivateShopItemCount() {
		return _partnersPrivateShopItemCount;
	}

	/**
	 * 設定正在瀏覽的個人商店的物品數量
	 *
	 * @param i 物品數量
	 */
	public void setPartnersPrivateShopItemCount(int i) {
		_partnersPrivateShopItemCount = i;
	}

	/** 封包輸出物件 */
	private PacketOutput _out;

	/**
	 * 設定封包輸出物件
	 *
	 * @param out 封包輸出物件
	 */
	public void setPacketOutput(PacketOutput out) {
		_out = out;
	}

	/**
	 * 發送封包給玩家客戶端
	 * <p>
	 * 如果輸出物件為null則不執行任何操作。
	 * 發送過程中的異常會被捕捉並忽略。
	 * </p>
	 *
	 * @param serverbasepacket 要發送的伺服器封包
	 */
	public void sendPackets(ServerBasePacket serverbasepacket) {
		if (_out == null) {
			return;
		}

		try {
			_out.sendPacket(serverbasepacket);
		}
		catch (Exception e) {}
	}

	@Override
	public void onAction(L1PcInstance attacker) {
		onAction(attacker, 0);
	}

	@Override
	public void onAction(L1PcInstance attacker, int skillId) {
		// XXX:NullPointerException回避。onActionの引数の型はL1Characterのほうが良い？
		if (attacker == null) {
			return;
		}
		// テレポート処理中
		if (isTeleport()) {
			return;
		}
		// 攻撃される側または攻撃する側がセーフティーゾーン
		if ((getZoneType() == 1) || (attacker.getZoneType() == 1)) {
			// 攻撃モーション送信
			L1Attack attack_mortion = new L1Attack(attacker, this, skillId);
			attack_mortion.action();
			return;
		}

		if (checkNonPvP(this, attacker) == true) {
			// 攻撃モーション送信
			L1Attack attack_mortion = new L1Attack(attacker, this, skillId);
			attack_mortion.action();
			return;
		}

		if ((getCurrentHp() > 0) && !isDead()) {
			attacker.delInvis();

			boolean isCounterBarrier = false;
			L1Attack attack = new L1Attack(attacker, this, skillId);
			if (attack.calcHit()) {
				if (hasSkillEffect(COUNTER_BARRIER)) {
					L1Magic magic = new L1Magic(this, attacker);
					boolean isProbability = magic.calcProbabilityMagic(COUNTER_BARRIER);
					boolean isShortDistance = attack.isShortDistance();
					if (isProbability && isShortDistance) {
						isCounterBarrier = true;
					}
				}
				if (!isCounterBarrier) {
					attacker.setPetTarget(this);

					attack.calcDamage();
					attack.calcStaffOfMana();
					attack.addPcPoisonAttack(attacker, this);
					attack.addChaserAttack();
				}
			}
			if (isCounterBarrier) {
				attack.actionCounterBarrier();
				attack.commitCounterBarrier();
			}
			else {
				attack.action();
				attack.commit();
			}
		}
	}

	/**
	 * 檢查是否為非PvP狀態
	 * <p>
	 * 檢查兩個玩家之間是否可以進行PvP戰鬥。考慮以下因素：
	 * - 是否在戰鬥區域
	 * - 是否參與相同的血盟戰爭
	 * - 是否在戰爭區域且戰爭進行中
	 * - 伺服器Non-PvP設定
	 * </p>
	 *
	 * @param pc 玩家角色
	 * @param target 目標角色(可能是玩家、寵物或召喚獸)
	 * @return 如果不可PvP返回true，可PvP返回false
	 */
	public boolean checkNonPvP(L1PcInstance pc, L1Character target) {
		L1PcInstance targetpc = null;
		if (target instanceof L1PcInstance) {
			targetpc = (L1PcInstance) target;
		}
		else if (target instanceof L1PetInstance) {
			targetpc = (L1PcInstance) ((L1PetInstance) target).getMaster();
		}
		else if (target instanceof L1SummonInstance) {
			targetpc = (L1PcInstance) ((L1SummonInstance) target).getMaster();
		}
		if (targetpc == null) {
			return false; // 相手がPC、サモン、ペット以外
		}
		if (!Config.ALT_NONPVP) { // Non-PvP設定
			if (getMap().isCombatZone(getLocation())) {
				return false;
			}

			// 全戦争リストを取得
			for (L1War war : L1World.getInstance().getWarList()) {
				if ((pc.getClanid() != 0) && (targetpc.getClanid() != 0)) { // 共にクラン所属中
					boolean same_war = war.CheckClanInSameWar(pc.getClanname(), targetpc.getClanname());
					if (same_war == true) { // 同じ戦争に参加中
						return false;
					}
				}
			}
			// Non-PvP設定でも戦争中は布告なしで攻撃可能
			if (target instanceof L1PcInstance) {
				L1PcInstance targetPc = (L1PcInstance) target;
				if (isInWarAreaAndWarTime(pc, targetPc)) {
					return false;
				}
			}
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * 檢查玩家是否在戰爭區域且戰爭進行中
	 * <p>
	 * 檢查兩個玩家是否同時位於相同城堡的戰爭區域，且該城堡正在進行攻城戰
	 * </p>
	 *
	 * @param pc 玩家角色
	 * @param target 目標玩家
	 * @return 如果雙方都在戰爭區域且戰爭進行中返回true
	 */
	private boolean isInWarAreaAndWarTime(L1PcInstance pc, L1PcInstance target) {
		int castleId = L1CastleLocation.getCastleIdByArea(pc);
		int targetCastleId = L1CastleLocation.getCastleIdByArea(target);
		if ((castleId != 0) && (targetCastleId != 0) && (castleId == targetCastleId)) {
			if (WarTimeController.getInstance().isNowWar(castleId)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 設定所有寵物和召喚獸的目標
	 * <p>
	 * 將玩家擁有的所有寵物和召喚獸的目標設為指定角色
	 * </p>
	 *
	 * @param target 目標角色
	 */
	public void setPetTarget(L1Character target) {
		Object[] petList = getPetList().values().toArray();
		for (Object pet : petList) {
			if (pet instanceof L1PetInstance) {
				L1PetInstance pets = (L1PetInstance) pet;
				pets.setMasterTarget(target);
			}
			else if (pet instanceof L1SummonInstance) {
				L1SummonInstance summon = (L1SummonInstance) pet;
				summon.setMasterTarget(target);
			}
		}
	}

	/**
	 * 移除隱身狀態
	 * <p>
	 * 移除玩家的隱身(Invisibility)或盲目隱藏(Blind Hiding)狀態，
	 * 並發送顯形封包給自己和周圍玩家
	 * </p>
	 */
	public void delInvis() {
		if (hasSkillEffect(INVISIBILITY)) { // インビジビリティ
			killSkillEffectTimer(INVISIBILITY);
			sendPackets(new S_Invis(getId(), 0));
			broadcastPacket(new S_OtherCharPacks(this));
		}
		if (hasSkillEffect(BLIND_HIDING)) { // ブラインド ハイディング
			killSkillEffectTimer(BLIND_HIDING);
			sendPackets(new S_Invis(getId(), 0));
			broadcastPacket(new S_OtherCharPacks(this));
		}
	}

	/**
	 * 移除盲目隱藏狀態
	 * <p>
	 * 當盲目隱藏技能時間結束時呼叫此方法。
	 * 移除技能效果並發送顯形封包。
	 * </p>
	 */
	public void delBlindHiding() {
		killSkillEffectTimer(BLIND_HIDING);
		sendPackets(new S_Invis(getId(), 0));
		broadcastPacket(new S_OtherCharPacks(this));
	}

	/**
	 * 接收魔法傷害
	 * <p>
	 * 當玩家受到魔法攻擊時使用此方法。會根據魔法防禦力(MR)進行傷害減免。
	 * 如果MR檢定成功，傷害減半。
	 * </p>
	 *
	 * @param attacker 攻擊者
	 * @param damage 原始傷害值
	 * @param attr 魔法屬性 (0:無屬性, 1:地, 2:火, 3:水, 4:風)
	 */
	public void receiveDamage(L1Character attacker, int damage, int attr) {
		int player_mr = getMr();
		int rnd = Random.nextInt(100) + 1;
		if (player_mr >= rnd) {
			damage /= 2;
		}
		receiveDamage(attacker, damage, false);
	}

	/**
	 * 接收MP傷害
	 * <p>
	 * 當玩家受到消耗MP的攻擊時使用此方法。
	 * 會扣除MP並處理相關的遊戲邏輯(隱身解除、粉紅名稱處理等)。
	 * </p>
	 *
	 * @param attacker 攻擊者
	 * @param mpDamage MP傷害值
	 */
	public void receiveManaDamage(L1Character attacker, int mpDamage) {
		if ((mpDamage > 0) && !isDead()) {
			delInvis();
			if (attacker instanceof L1PcInstance) {
				L1PinkName.onAction(this, attacker);
			}
			if ((attacker instanceof L1PcInstance) && ((L1PcInstance) attacker).isPinkName()) {
				// ガードが画面内にいれば、攻撃者をガードのターゲットに設定する
				for (L1Object object : L1World.getInstance().getVisibleObjects(attacker)) {
					if (object instanceof L1GuardInstance) {
						L1GuardInstance guard = (L1GuardInstance) object;
						guard.setTarget(((L1PcInstance) attacker));
					}
				}
			}

			int newMp = getCurrentMp() - mpDamage;
			if (newMp > getMaxMp()) {
				newMp = getMaxMp();
			}

			if (newMp <= 0) {
				newMp = 0;
			}
			setCurrentMp(newMp);
		}
	}

	/** 上次受到魔法傷害的時間，用於連續魔法傷害減免計算 */
	public double _oldTime = 0;

	/**
	 * 接收HP傷害
	 * <p>
	 * 當玩家受到攻擊而扣除HP時使用此方法。
	 * 包含以下處理：
	 * - 連續魔法傷害減免計算
	 * - 隱身狀態解除
	 * - 粉紅名稱處理
	 * - 睡眠之霧解除
	 * - 死亡之身反擊
	 * - 裝備特殊效果處理
	 * </p>
	 *
	 * @param attacker 攻擊者
	 * @param damage 傷害值
	 * @param isMagicDamage 是否為魔法傷害
	 */
	public void receiveDamage(L1Character attacker, double damage, boolean isMagicDamage) {
		if ((getCurrentHp() > 0) && !isDead()) {
			if (attacker != this) {
				if (!(attacker instanceof L1EffectInstance) && !knownsObject(attacker) && (attacker.getMapId() == getMapId())) {
					attacker.onPerceive(this);
				}
			}

			if (isMagicDamage == true) { // 連続魔法ダメージによる軽減
				double nowTime = (double) System.currentTimeMillis();
				double interval = (20D - (nowTime - _oldTime) / 100D) % 20D;

				if (damage > 0) {
					if (interval > 0) 
						damage *= (1D - interval / 30D);

					if (damage < 1) {
						damage = 0;
					}

					_oldTime = nowTime; // 次回のために時間を保存
				}
			}
			if (damage > 0) {
				delInvis();
				if (attacker instanceof L1PcInstance) {
					L1PinkName.onAction(this, attacker);
				}
				if ((attacker instanceof L1PcInstance) && ((L1PcInstance) attacker).isPinkName()) {
					// ガードが画面内にいれば、攻撃者をガードのターゲットに設定する
					for (L1Object object : L1World.getInstance().getVisibleObjects(attacker)) {
						if (object instanceof L1GuardInstance) {
							L1GuardInstance guard = (L1GuardInstance) object;
							guard.setTarget(((L1PcInstance) attacker));
						}
					}
				}
				removeSkillEffect(FOG_OF_SLEEPING);
			}

			if (hasSkillEffect(MORTAL_BODY) && (getId() != attacker.getId())) {
				int rnd = Random.nextInt(100) + 1;
				if ((damage > 0) && (rnd <= 10)) {
					if (attacker instanceof L1PcInstance) {
						L1PcInstance attackPc = (L1PcInstance) attacker;
						attackPc.sendPackets(new S_DoActionGFX(attackPc.getId(), ActionCodes.ACTION_Damage));
						attackPc.broadcastPacket(new S_DoActionGFX(attackPc.getId(), ActionCodes.ACTION_Damage));
						attackPc.receiveDamage(this, 30, false);
					}
					else if (attacker instanceof L1NpcInstance) {
						L1NpcInstance attackNpc = (L1NpcInstance) attacker;
						attackNpc.broadcastPacket(new S_DoActionGFX(attackNpc.getId(), ActionCodes.ACTION_Damage));
						attackNpc.receiveDamage(this, 30);
					}
				}
			}
			if (getInventory().checkEquipped(145) // バーサーカーアックス
					|| getInventory().checkEquipped(149)) { // ミノタウルスアックス
				damage *= 1.5; // 被ダメ1.5倍
			}
			if (hasSkillEffect(ILLUSION_AVATAR)) {// 幻術師魔法 (幻覺：化身)
				damage *= 1.2; // 被ダメ1.2倍
			}
			if (attacker instanceof L1PetInstance) {
				L1PetInstance pet = (L1PetInstance) attacker;
				// 目標在安區、攻擊者在安區、NOPVP
				if ((getZoneType() == 1) || (pet.getZoneType() == 1) || (checkNonPvP(this, pet))) {
					damage = 0;
				}
			} else if (attacker instanceof L1SummonInstance) {
				L1SummonInstance summon = (L1SummonInstance) attacker;
				// 目標在安區、攻擊者在安區、NOPVP
				if ((getZoneType() == 1) || (summon.getZoneType() == 1) || (checkNonPvP(this, summon))) {
					damage = 0;
				}
			}
			int newHp = getCurrentHp() - (int) (damage);
			if (newHp > getMaxHp()) {
				newHp = getMaxHp();
			}
			if (newHp <= 0) {
				if (isGm()) {
					setCurrentHp(getMaxHp());
				}
				else {
					death(attacker);
				}
			}
			if (newHp > 0) {
				setCurrentHp(newHp);
			}
		}
		else if (!isDead()) { // 念のため
			System.out.println("警告：プレイヤーのＨＰ減少処理が正しく行われていない箇所があります。※もしくは最初からＨＰ０");
			death(attacker);
		}
	}

	public void death(L1Character lastAttacker) {
		synchronized (this) {
			if (isDead()) {
				return;
			}
			setDead(true);
			setStatus(ActionCodes.ACTION_Die);
		}
		
		//死亡, 取消交易
		if (getTradeID() != 0) {
	         final L1Trade trade = new L1Trade();
	         trade.TradeCancel(this);
	    }
	      
		GeneralThreadPool.getInstance().execute(new Death(lastAttacker));
	}

	/**
	 * 死亡處理執行緒
	 * <p>
	 * 處理玩家死亡後的所有邏輯，包括：
	 * - 停止HP/MP回復
	 * - 解除各種增益狀態
	 * - 經驗值損失
	 * - 裝備掉落處理
	 * - PK計數和正義值變化
	 * - 血盟戰爭結果處理
	 * </p>
	 */
	private class Death implements Runnable {
		L1Character _lastAttacker;

		Death(L1Character cha) {
			_lastAttacker = cha;
		}

		@Override
		public void run() {
			L1Character lastAttacker = _lastAttacker;
			_lastAttacker = null;
			setCurrentHp(0);
			setGresValid(false); // EXPロストするまでG-RES無効

			while (isTeleport()) { // テレポート中なら終わるまで待つ
				try {
					Thread.sleep(300);
				}
				catch (Exception e) {}
			}

			stopHpRegeneration();
			stopMpRegeneration();

			int targetobjid = getId();
			getMap().setPassable(getLocation(), true);

			// エンチャントを解除する
			// 変身状態も解除されるため、キャンセレーションをかけてから変身状態に戻す
			int tempchargfx = 0;
			if (hasSkillEffect(SHAPE_CHANGE)) {
				tempchargfx = getTempCharGfx();
				setTempCharGfxAtDead(tempchargfx);
			}
			else {
				setTempCharGfxAtDead(getClassId());
			}

			// キャンセレーションをエフェクトなしでかける
			L1SkillUse l1skilluse = new L1SkillUse();
			l1skilluse.handleCommands(L1PcInstance.this, CANCELLATION, getId(), getX(), getY(), null, 0, L1SkillUse.TYPE_LOGIN);

			// 戰鬥藥水
			if (hasSkillEffect(EFFECT_POTION_OF_BATTLE)) {
				removeSkillEffect(EFFECT_POTION_OF_BATTLE);
			}
			// 象牙塔妙藥
			if (hasSkillEffect(COOKING_WONDER_DRUG)) {
				removeSkillEffect(COOKING_WONDER_DRUG);
			}

			sendPackets(new S_DoActionGFX(targetobjid, ActionCodes.ACTION_Die));
			broadcastPacket(new S_DoActionGFX(targetobjid, ActionCodes.ACTION_Die));

			if (lastAttacker != L1PcInstance.this) {
				// セーフティーゾーン、コンバットゾーンで最後に殺したキャラが
				// プレイヤーorペットだったら、ペナルティなし
				if (getZoneType() != 0) {
					L1PcInstance player = null;
					if (lastAttacker instanceof L1PcInstance) {
						player = (L1PcInstance) lastAttacker;
					}
					else if (lastAttacker instanceof L1PetInstance) {
						player = (L1PcInstance) ((L1PetInstance) lastAttacker).getMaster();
					}
					else if (lastAttacker instanceof L1SummonInstance) {
						player = (L1PcInstance) ((L1SummonInstance) lastAttacker).getMaster();
					}
					if (player != null) {
						// 戦争中に戦争エリアに居る場合は例外
						if (!isInWarAreaAndWarTime(L1PcInstance.this, player)) {
							return;
						}
					}
				}

				boolean sim_ret = simWarResult(lastAttacker); // 模擬戦
				if (sim_ret == true) { // 模擬戦中ならペナルティなし
					return;
				}
			}

			if (!getMap().isEnabledDeathPenalty()) {
				return;
			}

			// 決闘中ならペナルティなし
			L1PcInstance fightPc = null;
			if (lastAttacker instanceof L1PcInstance) {
				fightPc = (L1PcInstance) lastAttacker;
			}

			// 判斷是否屬於新手保護階段, 並且是被其他玩家所殺死
			boolean isNovice = false;
			if (hasSkillEffect(L1SkillId.STATUS_NOVICE) && (fightPc != null)) {

				// 判斷是否在新手等級保護範圍內
				if (fightPc.getLevel() > (getLevel() + Config.NOVICE_PROTECTION_LEVEL_RANGE)) {
					isNovice = true;
				}
			}

			if (fightPc != null) {
				if ((getFightId() == fightPc.getId()) && (fightPc.getFightId() == getId())) { // 決闘中
					setFightId(0);
					sendPackets(new S_PacketBox(S_PacketBox.MSG_DUEL, 0, 0));
					fightPc.setFightId(0);
					fightPc.sendPackets(new S_PacketBox(S_PacketBox.MSG_DUEL, 0, 0));
					return;
				}
			}

			/*
			 * deathPenalty(); // EXPロスト
			 * 
			 * setGresValid(true); // EXPロストしたらG-RES有効
			 * 
			 * if (getExpRes() == 0) { setExpRes(1); }
			 */

			// ガードに殺された場合のみ、PKカウントを減らしガードに攻撃されなくなる
			if (lastAttacker instanceof L1GuardInstance) {
				if (get_PKcount() > 0) {
					set_PKcount(get_PKcount() - 1);
				}
				setLastPk(null);
			}
			if (lastAttacker instanceof L1GuardianInstance) {
				if (getPkCountForElf() > 0) {
					setPkCountForElf(getPkCountForElf() - 1);
				}
				setLastPkForElf(null);
			}

			// 增加新手保護階段, 將不會損失道具(不會噴裝)
			if (!isNovice) {
				// 一定の確率でアイテムをDROP
				// アライメント32000以上で0%、以降-1000毎に0.4%
				// アライメントが0未満の場合は-1000毎に0.8%
				// アライメント-32000以下で最高51.2%のDROP率
				int lostRate = (int) (((getLawful() + 32768D) / 1000D - 65D) * 4D);
				if (lostRate < 0) {
					lostRate *= -1;
					if (getLawful() < 0) {
						lostRate *= 2;
					}
					int rnd = Random.nextInt(1000) + 1;
					if (rnd <= lostRate) {
						int count = 1;
						if (getLawful() <= -30000) {
							count = Random.nextInt(4) + 1;
						}
						else if (getLawful() <= -20000) {
							count = Random.nextInt(3) + 1;
						}
						else if (getLawful() <= -10000) {
							count = Random.nextInt(2) + 1;
						}
						else if (getLawful() < 0) {
							count = Random.nextInt(1) + 1;
						}
						caoPenaltyResult(count);
					}
				}
			}

			boolean castle_ret = castleWarResult(); // 攻城戦
			if (castle_ret == true) { // 攻城戦中で旗内なら赤ネームペナルティなし
				return;
			}

			if (!getMap().isEnabledDeathPenalty()) {
				return;
			}

			// 增加新手保護階段, 將不會損失經驗值
			if (!isNovice) {
				deathPenalty(); // EXPロスト
				setGresValid(true); // EXPロストしたらG-RES有効
			}

			if (get_PKcount() > 0) {
				set_PKcount(get_PKcount() - 1);
			}
			setLastPk(null);

			// 最後に殺したキャラがプレイヤーだったら、赤ネームにする
			L1PcInstance player = null;
			if (lastAttacker instanceof L1PcInstance) {
				player = (L1PcInstance) lastAttacker;
			}
			if (player != null) {
				if ((getLawful() >= 0) && (isPinkName() == false)) {
					boolean isChangePkCount = false;
					// アライメントが30000未満の場合はPKカウント増加
					if (player.getLawful() < 30000) {
						player.set_PKcount(player.get_PKcount() + 1);
						isChangePkCount = true;
						if (player.isElf() && isElf()) {
							player.setPkCountForElf(player.getPkCountForElf() + 1);
						}
					}
					player.setLastPk();
					/** 正義值滿不會被警衛追殺 */
					if (player.getLawful() == 32767) {
						player.setLastPk(null);
					}
					if (player.isElf() && isElf()) {
						player.setLastPkForElf();
					}

					// アライメント処理
					// 公式の発表および各LVでのPKからつじつまの合うように変更
					// （PK側のLVに依存し、高LVほどリスクも高い）
					// 48あたりで-8kほど DKの時点で10k強
					// 60で約20k強 65で30k弱
					int lawful;

					if (player.getLevel() < 50) {
						lawful = -1 * (int) ((Math.pow(player.getLevel(), 2) * 4));
					}
					else {
						lawful = -1 * (int) ((Math.pow(player.getLevel(), 3) * 0.08));
					}
					// もし(元々のアライメント-1000)が計算後より低い場合
					// 元々のアライメント-1000をアライメント値とする
					// （連続でPKしたときにほとんど値が変わらなかった記憶より）
					// これは上の式よりも自信度が低いうろ覚えですので
					// 明らかにこうならない！という場合は修正お願いします
					if ((player.getLawful() - 1000) < lawful) {
						lawful = player.getLawful() - 1000;
					}

					if (lawful <= -32768) {
						lawful = -32768;
					}
					player.setLawful(lawful);

					S_Lawful s_lawful = new S_Lawful(player.getId(), player.getLawful());
					player.sendPackets(s_lawful);
					player.broadcastPacket(s_lawful);

					if (isChangePkCount && (player.get_PKcount() >= 5) && (player.get_PKcount() < 10)) {
						// あなたのPK回数が%0になりました。回数が%1になると地獄行きです。
						player.sendPackets(new S_RedMessage(551, String.valueOf(player.get_PKcount()), "10"));
					}
					else if (isChangePkCount && (player.get_PKcount() >= 10)) {
						player.beginHell(true);
					}
				}
				else {
					setPinkName(false);
				}
			}
			_pcDeleteTimer = new L1PcDeleteTimer(L1PcInstance.this);
			_pcDeleteTimer.begin();
		}
	}

	/**
	 * 停止玩家刪除計時器
	 * <p>
	 * 當玩家復活時，停止刪除計時器
	 * </p>
	 */
	public void stopPcDeleteTimer() {
		if (_pcDeleteTimer != null) {
			_pcDeleteTimer.cancel();
			_pcDeleteTimer = null;
		}
	}

	/**
	 * 處理負正義值死亡掉落懲罰
	 * <p>
	 * 當玩家正義值為負時死亡，會隨機掉落背包中的物品。
	 * 掉落數量由正義值決定。
	 * </p>
	 *
	 * @param count 要掉落的物品數量
	 */
	private void caoPenaltyResult(int count) {
		for (int i = 0; i < count; i++) {
			L1ItemInstance item = getInventory().CaoPenalty();

			if (item != null) {
				getInventory().tradeItem(item, item.isStackable() ? item.getCount() : 1,
						L1World.getInstance().getInventory(getX(), getY(), getMapId()));
				sendPackets(new S_ServerMessage(638, item.getLogName())); // %0を失いました。
			}
			else {}
		}
	}

	/**
	 * 處理攻城戰死亡結果
	 * <p>
	 * 檢查玩家是否在攻城戰中死亡：
	 * - 如果是血盟主且在攻城戰中，則結束戰爭
	 * - 如果在城堡旗幟範圍內且正在攻城戰中，不給予死亡懲罰
	 * </p>
	 *
	 * @return 如果在攻城戰旗幟範圍內返回true
	 */
	public boolean castleWarResult() {
		if ((getClanid() != 0) && isCrown()) { // クラン所属中プリのチェック
			L1Clan clan = L1World.getInstance().getClan(getClanname());
			// 全戦争リストを取得
			for (L1War war : L1World.getInstance().getWarList()) {
				int warType = war.GetWarType();
				boolean isInWar = war.CheckClanInWar(getClanname());
				boolean isAttackClan = war.CheckAttackClan(getClanname());
				if ((getId() == clan.getLeaderId()) && // 血盟主で攻撃側で攻城戦中
						(warType == 1) && isInWar && isAttackClan) {
					String enemyClanName = war.GetEnemyClanName(getClanname());
					if (enemyClanName != null) {
						war.CeaseWar(getClanname(), enemyClanName); // 終結
					}
					break;
				}
			}
		}

		int castleId = 0;
		boolean isNowWar = false;
		castleId = L1CastleLocation.getCastleIdByArea(this);
		if (castleId != 0) { // 旗内に居る
			isNowWar = WarTimeController.getInstance().isNowWar(castleId);
		}
		return isNowWar;
	}

	/**
	 * 處理模擬戰死亡結果
	 * <p>
	 * 檢查玩家是否在模擬戰中死亡：
	 * - 如果雙方參與同一場模擬戰，不給予死亡懲罰
	 * - 如果是血盟主，會結束模擬戰
	 * - 根據設定決定是否給予懲罰
	 * </p>
	 *
	 * @param lastAttacker 最後的攻擊者
	 * @return 如果在模擬戰中死亡且無懲罰返回true
	 */
	public boolean simWarResult(L1Character lastAttacker) {
		if (getClanid() == 0) { // クラン所属していない
			return false;
		}
		if (Config.SIM_WAR_PENALTY) { // 模擬戦ペナルティありの場合はfalse
			return false;
		}
		L1PcInstance attacker = null;
		String enemyClanName = null;
		boolean sameWar = false;

		if (lastAttacker instanceof L1PcInstance) {
			attacker = (L1PcInstance) lastAttacker;
		}
		else if (lastAttacker instanceof L1PetInstance) {
			attacker = (L1PcInstance) ((L1PetInstance) lastAttacker).getMaster();
		}
		else if (lastAttacker instanceof L1SummonInstance) {
			attacker = (L1PcInstance) ((L1SummonInstance) lastAttacker).getMaster();
		}
		else {
			return false;
		}

		// 全戦争リストを取得
		for (L1War war : L1World.getInstance().getWarList()) {
			L1Clan clan = L1World.getInstance().getClan(getClanname());

			int warType = war.GetWarType();
			boolean isInWar = war.CheckClanInWar(getClanname());
			if ((attacker != null) && (attacker.getClanid() != 0)) { // lastAttackerがPC、サモン、ペットでクラン所属中
				sameWar = war.CheckClanInSameWar(getClanname(), attacker.getClanname());
			}

			if ((getId() == clan.getLeaderId()) && // 血盟主で模擬戦中
					(warType == 2) && (isInWar == true)) {
				enemyClanName = war.GetEnemyClanName(getClanname());
				if (enemyClanName != null) {
					war.CeaseWar(getClanname(), enemyClanName); // 終結
				}
			}

			if ((warType == 2) && sameWar) {// 模擬戦で同じ戦争に参加中の場合、ペナルティなし
				return true;
			}
		}
		return false;
	}

	/**
	 * 復活時給予經驗值
	 * <p>
	 * 使用復活技能或道具時，根據玩家等級給予不同比例的經驗值：
	 * - 45級以下：下一級所需經驗的5%
	 * - 45級：4.5%
	 * - 46級：4%
	 * - 47級：3.5%
	 * - 48級：3%
	 * - 49級以上：2.5%
	 * </p>
	 */
	public void resExp() {
		int oldLevel = getLevel();
		int needExp = ExpTable.getNeedExpNextLevel(oldLevel);
		int exp = 0;
		if (oldLevel < 45) {
			exp = (int) (needExp * 0.05);
		}
		else if (oldLevel == 45) {
			exp = (int) (needExp * 0.045);
		}
		else if (oldLevel == 46) {
			exp = (int) (needExp * 0.04);
		}
		else if (oldLevel == 47) {
			exp = (int) (needExp * 0.035);
		}
		else if (oldLevel == 48) {
			exp = (int) (needExp * 0.03);
		}
		else if (oldLevel >= 49) {
			exp = (int) (needExp * 0.025);
		}

		if (exp == 0) {
			return;
		}
		addExp(exp);
	}

	/**
	 * 死亡懲罰處理
	 * <p>
	 * 根據角色等級計算並扣除死亡時的經驗值懲罰。
	 * 不同等級範圍有不同的懲罰比例：
	 * </p>
	 * <ul>
	 * <li>1-10級：無懲罰</li>
	 * <li>11-44級：扣除下一級所需經驗值的10%</li>
	 * <li>45級：9%</li>
	 * <li>46級：8%</li>
	 * <li>47級：7%</li>
	 * <li>48級：6%</li>
	 * <li>49級以上：5%</li>
	 * </ul>
	 * <p>
	 * 同時會增加復活經驗值計數器。
	 * </p>
	 */
	public void deathPenalty() {
		int oldLevel = getLevel();
		int needExp = ExpTable.getNeedExpNextLevel(oldLevel);
		int exp = 0;
		if ((oldLevel >= 1) && (oldLevel < 11)) {
			exp = 0;
		}
		else if ((oldLevel >= 11) && (oldLevel < 45)) {
			exp = (int) (needExp * 0.1);
		}
		else if (oldLevel == 45) {
			exp = (int) (needExp * 0.09);
		}
		else if (oldLevel == 46) {
			exp = (int) (needExp * 0.08);
		}
		else if (oldLevel == 47) {
			exp = (int) (needExp * 0.07);
		}
		else if (oldLevel == 48) {
			exp = (int) (needExp * 0.06);
		}
		else if (oldLevel >= 49) {
			exp = (int) (needExp * 0.05);
		}

		if (exp == 0) {
			return;
		}

		if (getExpRes() >= 0) {
			setExpRes(getExpRes() + 1);
		}
		addExp(-exp);
	}

	/**
	 * 原始閃避率（ER）修正值
	 * <p>
	 * 用於儲存來自裝備或其他來源的額外閃避率加成。
	 * </p>
	 */
	private int _originalEr = 0;

	/**
	 * 取得原始閃避率修正值
	 *
	 * @return 原始閃避率修正值
	 */
	public int getOriginalEr() {

		return _originalEr;
	}

	/**
	 * 計算並取得角色的總閃避率（ER）
	 * <p>
	 * 閃避率由以下因素組成：
	 * </p>
	 * <ul>
	 * <li>職業基礎閃避率（等級除以職業係數）：
	 *   <ul>
	 *   <li>騎士：等級/4</li>
	 *   <li>君主、精靈：等級/8</li>
	 *   <li>黑暗精靈：等級/6</li>
	 *   <li>法師：等級/10</li>
	 *   <li>龍騎士：等級/7</li>
	 *   <li>幻術師：等級/9</li>
	 *   </ul>
	 * </li>
	 * <li>敏捷加成：(DEX - 8) / 2</li>
	 * <li>原始閃避率修正（裝備等）</li>
	 * <li>技能加成（如迴避之舞+12、鐵壁防禦+15）</li>
	 * </ul>
	 * <p>
	 * 注意：擁有「疾風」效果時，閃避率強制為0。
	 * </p>
	 *
	 * @return 總閃避率
	 */
	public int getEr() {
		if (hasSkillEffect(STRIKER_GALE)) {
			return 0;
		}

		int er = 0;
		if (isKnight()) {
			er = getLevel() / 4; // ナイト
		}
		else if (isCrown() || isElf()) {
			er = getLevel() / 8; // 君主・エルフ
		}
		else if (isDarkelf()) {
			er = getLevel() / 6; // ダークエルフ
		}
		else if (isWizard()) {
			er = getLevel() / 10; // ウィザード
		}
		else if (isDragonKnight()) {
			er = getLevel() / 7; // ドラゴンナイト
		}
		else if (isIllusionist()) {
			er = getLevel() / 9; // イリュージョニスト
		}

		er += (getDex() - 8) / 2;

		er += getOriginalEr();

		if (hasSkillEffect(DRESS_EVASION)) {
			er += 12;
		}
		if (hasSkillEffect(SOLID_CARRIAGE)) {
			er += 15;
		}
		return er;
	}

	/**
	 * 根據名稱取得記憶座標
	 *
	 * @param name 記憶座標名稱（不區分大小寫）
	 * @return 找到的記憶座標物件，若不存在則返回 null
	 */
	public L1BookMark getBookMark(String name) {
		for (int i = 0; i < _bookmarks.size(); i++) {
			L1BookMark element = _bookmarks.get(i);
			if (element.getName().equalsIgnoreCase(name)) {
				return element;
			}

		}
		return null;
	}

	/**
	 * 根據 ID 取得記憶座標
	 *
	 * @param id 記憶座標 ID
	 * @return 找到的記憶座標物件，若不存在則返回 null
	 */
	public L1BookMark getBookMark(int id) {
		for (int i = 0; i < _bookmarks.size(); i++) {
			L1BookMark element = _bookmarks.get(i);
			if (element.getId() == id) {
				return element;
			}

		}
		return null;
	}

	/**
	 * 取得記憶座標的數量
	 *
	 * @return 記憶座標數量
	 */
	public int getBookMarkSize() {
		return _bookmarks.size();
	}

	/**
	 * 新增記憶座標
	 *
	 * @param book 要新增的記憶座標物件
	 */
	public void addBookMark(L1BookMark book) {
		_bookmarks.add(book);
	}

	/**
	 * 移除記憶座標
	 *
	 * @param book 要移除的記憶座標物件
	 */
	public void removeBookMark(L1BookMark book) {
		_bookmarks.remove(book);
	}

	/**
	 * 取得角色目前裝備的武器
	 *
	 * @return 武器物品實例，若未裝備武器則可能為 null
	 */
	public L1ItemInstance getWeapon() {
		return _weapon;
	}

	/**
	 * 設定角色目前裝備的武器
	 *
	 * @param weapon 武器物品實例
	 */
	public void setWeapon(L1ItemInstance weapon) {
		_weapon = weapon;
	}

	/**
	 * 取得角色的任務資訊物件
	 *
	 * @return 任務資訊物件
	 */
	public L1Quest getQuest() {
		return _quest;
	}

	/**
	 * 判斷角色是否為君主職業
	 *
	 * @return 若為王子或公主則返回 true，否則返回 false
	 */
	public boolean isCrown() {
		return ((getClassId() == CLASSID_PRINCE) || (getClassId() == CLASSID_PRINCESS));
	}

	/**
	 * 判斷角色是否為騎士職業
	 *
	 * @return 若為男騎士或女騎士則返回 true，否則返回 false
	 */
	public boolean isKnight() {
		return ((getClassId() == CLASSID_KNIGHT_MALE) || (getClassId() == CLASSID_KNIGHT_FEMALE));
	}

	/**
	 * 判斷角色是否為精靈職業
	 *
	 * @return 若為男精靈或女精靈則返回 true，否則返回 false
	 */
	public boolean isElf() {
		return ((getClassId() == CLASSID_ELF_MALE) || (getClassId() == CLASSID_ELF_FEMALE));
	}

	/**
	 * 判斷角色是否為法師職業
	 *
	 * @return 若為男法師或女法師則返回 true，否則返回 false
	 */
	public boolean isWizard() {
		return ((getClassId() == CLASSID_WIZARD_MALE) || (getClassId() == CLASSID_WIZARD_FEMALE));
	}

	/**
	 * 判斷角色是否為黑暗精靈職業
	 *
	 * @return 若為男黑暗精靈或女黑暗精靈則返回 true，否則返回 false
	 */
	public boolean isDarkelf() {
		return ((getClassId() == CLASSID_DARK_ELF_MALE) || (getClassId() == CLASSID_DARK_ELF_FEMALE));
	}

	/**
	 * 判斷角色是否為龍騎士職業
	 *
	 * @return 若為男龍騎士或女龍騎士則返回 true，否則返回 false
	 */
	public boolean isDragonKnight() {
		return ((getClassId() == CLASSID_DRAGON_KNIGHT_MALE) || (getClassId() == CLASSID_DRAGON_KNIGHT_FEMALE));
	}

	/**
	 * 判斷角色是否為幻術師職業
	 *
	 * @return 若為男幻術師或女幻術師則返回 true，否則返回 false
	 */
	public boolean isIllusionist() {
		return ((getClassId() == CLASSID_ILLUSIONIST_MALE) || (getClassId() == CLASSID_ILLUSIONIST_FEMALE));
	}

	/** 日誌記錄器 */
	private static Logger _log = Logger.getLogger(L1PcInstance.class.getName());

	/** 網路連線執行緒 */
	private ClientThread _netConnection;

	/** 職業 ID */
	private int _classId;

	/** 角色類型 */
	private int _type;

	/** 經驗值 */
	private int _exp;

	/** 業值（善惡值）管理物件 */
	private final L1Karma _karma = new L1Karma();

	/** 是否為遊戲管理員 */
	private boolean _gm;

	/** 是否為監控者 */
	private boolean _monitor;

	/** 是否為 GM 隱身狀態 */
	private boolean _gmInvis;

	/** 存取權限等級 */
	private short _accessLevel;

	/** 目前使用的武器類型 */
	private int _currentWeapon;

	/** 角色背包 */
	private final L1PcInventory _inventory;

	/** 矮人倉庫 */
	private final L1DwarfInventory _dwarf;

	/** 精靈專用倉庫 */
	private final L1DwarfForElfInventory _dwarfForElf;

	/** 交易視窗 */
	private final L1Inventory _tradewindow;

	/** 裝備的武器實例 */
	private L1ItemInstance _weapon;

	/** 所屬隊伍 */
	private L1Party _party;

	/** 所屬聊天隊伍 */
	private L1ChatParty _chatParty;

	/** 隊伍 ID */
	private int _partyID;

	/** 交易對象 ID */
	private int _tradeID;

	/** 交易確認狀態 */
	private boolean _tradeOk;

	/** 暫存 ID */
	private int _tempID;

	/** 是否正在傳送中 */
	private boolean _isTeleport = false;

	/** 是否正在飲用物品 */
	private boolean _isDrink = false;

	/** 是否擁有復活祝福 */
	private boolean _isGres = false;

	/** 是否為粉紅名（PK 狀態） */
	private boolean _isPinkName = false;

	/** 記憶座標清單 */
	private final List<L1BookMark> _bookmarks;

	/** 任務物件 */
	private L1Quest _quest;

	/** MP 自然恢復任務 */
	private MpRegeneration _mpRegen;

	/** 人偶 MP 恢復任務 */
	private MpRegenerationByDoll _mpRegenByDoll;

	/** 覺醒 MP 消耗任務 */
	private MpReductionByAwake _mpReductionByAwake;

	/** HP 自然恢復任務 */
	private HpRegeneration _hpRegen;

	/** 人偶 HP 恢復任務 */
	private HpRegenerationByDoll _hpRegenByDoll;

	/** 人偶物品製作任務 */
	private ItemMakeByDoll _itemMakeByDoll;

	/** 恢復計時器（共用） */
	private static Timer _regenTimer = new Timer(true);

	/** MP 自然恢復是否啟動 */
	private boolean _mpRegenActive;

	/** 人偶 MP 恢復是否啟動 */
	private boolean _mpRegenActiveByDoll;

	/** 覺醒 MP 消耗是否啟動 */
	private boolean _mpReductionActiveByAwake;

	/** HP 自然恢復是否啟動 */
	private boolean _hpRegenActive;

	/** 人偶 HP 恢復是否啟動 */
	private boolean _hpRegenActiveByDoll;

	/** 人偶物品製作是否啟動 */
	private boolean _ItemMakeActiveByDoll;

	/** 裝備欄位管理物件 */
	private L1EquipmentSlot _equipSlot;

	/** 角色刪除計時器 */
	private L1PcDeleteTimer _pcDeleteTimer;

	/** 帳號名稱 */
	private String _accountName;

	/**
	 * 取得帳號名稱
	 *
	 * @return 帳號名稱
	 */
	public String getAccountName() {
		return _accountName;
	}

	/**
	 * 設定帳號名稱
	 *
	 * @param s 帳號名稱
	 */
	public void setAccountName(String s) {
		_accountName = s;
	}

	/** 基礎最大 HP 值（範圍：1～32767） */
	private short _baseMaxHp = 0;

	/**
	 * 取得基礎最大 HP 值
	 *
	 * @return 基礎最大 HP 值
	 */
	public short getBaseMaxHp() {
		return _baseMaxHp;
	}

	/**
	 * 增加基礎最大 HP 值
	 * <p>
	 * 值會被限制在 1～32767 範圍內。
	 * </p>
	 *
	 * @param i 要增加的 HP 值
	 */
	public void addBaseMaxHp(short i) {
		i += _baseMaxHp;
		if (i >= 32767) {
			i = 32767;
		}
		else if (i < 1) {
			i = 1;
		}
		addMaxHp(i - _baseMaxHp);
		_baseMaxHp = i;
	}

	/** 基礎最大 MP 值（範圍：0～32767） */
	private short _baseMaxMp = 0;

	/**
	 * 取得基礎最大 MP 值
	 *
	 * @return 基礎最大 MP 值
	 */
	public short getBaseMaxMp() {
		return _baseMaxMp;
	}

	/**
	 * 增加基礎最大 MP 值
	 * <p>
	 * 值會被限制在 0～32767 範圍內。
	 * </p>
	 *
	 * @param i 要增加的 MP 值
	 */
	public void addBaseMaxMp(short i) {
		i += _baseMaxMp;
		if (i >= 32767) {
			i = 32767;
		}
		else if (i < 0) {
			i = 0;
		}
		addMaxMp(i - _baseMaxMp);
		_baseMaxMp = i;
	}

	/** 基礎防禦力（AC）值（範圍：-128～127） */
	private int _baseAc = 0;

	/**
	 * 取得基礎防禦力（AC）值
	 *
	 * @return 基礎 AC 值
	 */
	public int getBaseAc() {
		return _baseAc;
	}

	/** 原始敏捷 AC 修正值 */
	private int _originalAc = 0;

	/**
	 * 取得原始敏捷 AC 修正值
	 *
	 * @return 原始 AC 修正值
	 */
	public int getOriginalAc() {

		return _originalAc;
	}

	/** 基礎力量（STR）值（範圍：1～127） */
	private byte _baseStr = 0;

	/**
	 * 取得基礎力量值
	 *
	 * @return 基礎力量值
	 */
	public byte getBaseStr() {
		return _baseStr;
	}

	/**
	 * 增加基礎力量值
	 * <p>
	 * 值會被限制在 1～127 範圍內。
	 * </p>
	 *
	 * @param i 要增加的力量值
	 */
	public void addBaseStr(byte i) {
		i += _baseStr;
		if (i >= 127) {
			i = 127;
		}
		else if (i < 1) {
			i = 1;
		}
		addStr((byte) (i - _baseStr));
		_baseStr = i;
	}

	/** 基礎體質（CON）值（範圍：1～127） */
	private byte _baseCon = 0;

	/**
	 * 取得基礎體質值
	 *
	 * @return 基礎體質值
	 */
	public byte getBaseCon() {
		return _baseCon;
	}

	/**
	 * 增加基礎體質值
	 * <p>
	 * 值會被限制在 1～127 範圍內。
	 * </p>
	 *
	 * @param i 要增加的體質值
	 */
	public void addBaseCon(byte i) {
		i += _baseCon;
		if (i >= 127) {
			i = 127;
		}
		else if (i < 1) {
			i = 1;
		}
		addCon((byte) (i - _baseCon));
		_baseCon = i;
	}

	/** 基礎敏捷（DEX）值（範圍：1～127） */
	private byte _baseDex = 0;

	/**
	 * 取得基礎敏捷值
	 *
	 * @return 基礎敏捷值
	 */
	public byte getBaseDex() {
		return _baseDex;
	}

	/**
	 * 增加基礎敏捷值
	 * <p>
	 * 值會被限制在 1～127 範圍內。
	 * </p>
	 *
	 * @param i 要增加的敏捷值
	 */
	public void addBaseDex(byte i) {
		i += _baseDex;
		if (i >= 127) {
			i = 127;
		}
		else if (i < 1) {
			i = 1;
		}
		addDex((byte) (i - _baseDex));
		_baseDex = i;
	}

	/** 基礎魅力（CHA）值（範圍：1～127） */
	private byte _baseCha = 0;

	/**
	 * 取得基礎魅力值
	 *
	 * @return 基礎魅力值
	 */
	public byte getBaseCha() {
		return _baseCha;
	}

	/**
	 * 增加基礎魅力值
	 * <p>
	 * 值會被限制在 1～127 範圍內。
	 * </p>
	 *
	 * @param i 要增加的魅力值
	 */
	public void addBaseCha(byte i) {
		i += _baseCha;
		if (i >= 127) {
			i = 127;
		}
		else if (i < 1) {
			i = 1;
		}
		addCha((byte) (i - _baseCha));
		_baseCha = i;
	}

	/** 基礎智力（INT）值（範圍：1～127） */
	private byte _baseInt = 0;

	/**
	 * 取得基礎智力值
	 *
	 * @return 基礎智力值
	 */
	public byte getBaseInt() {
		return _baseInt;
	}

	/**
	 * 增加基礎智力值
	 * <p>
	 * 值會被限制在 1～127 範圍內。
	 * </p>
	 *
	 * @param i 要增加的智力值
	 */
	public void addBaseInt(byte i) {
		i += _baseInt;
		if (i >= 127) {
			i = 127;
		}
		else if (i < 1) {
			i = 1;
		}
		addInt((byte) (i - _baseInt));
		_baseInt = i;
	}

	/** 基礎智慧（WIS）值（範圍：1～127） */
	private byte _baseWis = 0;

	/**
	 * 取得基礎智慧值
	 *
	 * @return 基礎智慧值
	 */
	public byte getBaseWis() {
		return _baseWis;
	}

	/**
	 * 增加基礎智慧值
	 * <p>
	 * 值會被限制在 1～127 範圍內。
	 * </p>
	 *
	 * @param i 要增加的智慧值
	 */
	public void addBaseWis(byte i) {
		i += _baseWis;
		if (i >= 127) {
			i = 127;
		}
		else if (i < 1) {
			i = 1;
		}
		addWis((byte) (i - _baseWis));
		_baseWis = i;
	}

	/** 原始力量值（來自裝備等） */
	private int _originalStr = 0;

	/**
	 * 取得原始力量值
	 *
	 * @return 原始力量值
	 */
	public int getOriginalStr() {
		return _originalStr;
	}

	/**
	 * 設定原始力量值
	 *
	 * @param i 力量值
	 */
	public void setOriginalStr(int i) {
		_originalStr = i;
	}

	/** 原始體質值（來自裝備等） */
	private int _originalCon = 0;

	/**
	 * 取得原始體質值
	 *
	 * @return 原始體質值
	 */
	public int getOriginalCon() {
		return _originalCon;
	}

	/**
	 * 設定原始體質值
	 *
	 * @param i 體質值
	 */
	public void setOriginalCon(int i) {
		_originalCon = i;
	}

	/** 原始敏捷值（來自裝備等） */
	private int _originalDex = 0;

	/**
	 * 取得原始敏捷值
	 *
	 * @return 原始敏捷值
	 */
	public int getOriginalDex() {
		return _originalDex;
	}

	/**
	 * 設定原始敏捷值
	 *
	 * @param i 敏捷值
	 */
	public void setOriginalDex(int i) {
		_originalDex = i;
	}

	/** 原始魅力值（來自裝備等） */
	private int _originalCha = 0;

	/**
	 * 取得原始魅力值
	 *
	 * @return 原始魅力值
	 */
	public int getOriginalCha() {
		return _originalCha;
	}

	/**
	 * 設定原始魅力值
	 *
	 * @param i 魅力值
	 */
	public void setOriginalCha(int i) {
		_originalCha = i;
	}

	/** 原始智力值（來自裝備等） */
	private int _originalInt = 0;

	/**
	 * 取得原始智力值
	 *
	 * @return 原始智力值
	 */
	public int getOriginalInt() {
		return _originalInt;
	}

	/**
	 * 設定原始智力值
	 *
	 * @param i 智力值
	 */
	public void setOriginalInt(int i) {
		_originalInt = i;
	}

	/** 原始智慧值（來自裝備等） */
	private int _originalWis = 0;

	/**
	 * 取得原始智慧值
	 *
	 * @return 原始智慧值
	 */
	public int getOriginalWis() {
		return _originalWis;
	}

	/**
	 * 設定原始智慧值
	 *
	 * @param i 智慧值
	 */
	public void setOriginalWis(int i) {
		_originalWis = i;
	}

	/** 原始力量傷害加成（來自裝備等） */
	private int _originalDmgup = 0;

	/**
	 * 取得原始力量傷害加成
	 *
	 * @return 原始傷害加成值
	 */
	public int getOriginalDmgup() {

		return _originalDmgup;
	}

	/** 原始敏捷弓箭傷害加成（來自裝備等） */
	private int _originalBowDmgup = 0;

	/**
	 * 取得原始弓箭傷害加成
	 *
	 * @return 原始弓箭傷害加成值
	 */
	public int getOriginalBowDmgup() {

		return _originalBowDmgup;
	}

	/** 原始力量命中加成（來自裝備等） */
	private int _originalHitup = 0;

	/**
	 * 取得原始命中加成
	 *
	 * @return 原始命中加成值
	 */
	public int getOriginalHitup() {

		return _originalHitup;
	}

	/** 原始敏捷弓箭命中加成（來自裝備等） */
	private int _originalBowHitup = 0;

	/**
	 * 取得原始弓箭命中加成
	 *
	 * @return 原始弓箭命中加成值
	 */
	public int getOriginalBowHitup() {

		return _originalBowHitup;
	}

	/** 原始智慧魔法防禦加成（來自裝備等） */
	private int _originalMr = 0;

	/**
	 * 取得原始魔法防禦加成
	 *
	 * @return 原始魔法防禦值
	 */
	public int getOriginalMr() {

		return _originalMr;
	}

	/** 原始智力魔法命中加成（來自裝備等） */
	private int _originalMagicHit = 0;

	/**
	 * 取得原始魔法命中加成
	 *
	 * @return 原始魔法命中值
	 */
	public int getOriginalMagicHit() {

		return _originalMagicHit;
	}

	/** 原始智力魔法爆擊加成（來自裝備等） */
	private int _originalMagicCritical = 0;

	/**
	 * 取得原始魔法爆擊加成
	 *
	 * @return 原始魔法爆擊值
	 */
	public int getOriginalMagicCritical() {

		return _originalMagicCritical;
	}

	/** 原始智力 MP 消耗減少（來自裝備等） */
	private int _originalMagicConsumeReduction = 0;

	/**
	 * 取得原始 MP 消耗減少值
	 *
	 * @return 原始 MP 消耗減少值
	 */
	public int getOriginalMagicConsumeReduction() {

		return _originalMagicConsumeReduction;
	}

	/** 原始智力魔法傷害加成（來自裝備等） */
	private int _originalMagicDamage = 0;

	/**
	 * 取得原始魔法傷害加成
	 *
	 * @return 原始魔法傷害值
	 */
	public int getOriginalMagicDamage() {

		return _originalMagicDamage;
	}

	/** 原始體質 HP 上升值加成（來自裝備等） */
	private int _originalHpup = 0;

	/**
	 * 取得原始 HP 上升值加成
	 *
	 * @return 原始 HP 上升值
	 */
	public int getOriginalHpup() {

		return _originalHpup;
	}

	/** 原始智慧 MP 上升值加成（來自裝備等） */
	private int _originalMpup = 0;

	/**
	 * 取得原始 MP 上升值加成
	 *
	 * @return 原始 MP 上升值
	 */
	public int getOriginalMpup() {

		return _originalMpup;
	}

	/** 基礎傷害加成值（範圍：-128～127） */
	private int _baseDmgup = 0;

	/**
	 * 取得基礎傷害加成值
	 *
	 * @return 基礎傷害加成值
	 */
	public int getBaseDmgup() {
		return _baseDmgup;
	}

	/** 基礎弓箭傷害加成值（範圍：-128～127） */
	private int _baseBowDmgup = 0;

	/**
	 * 取得基礎弓箭傷害加成值
	 *
	 * @return 基礎弓箭傷害加成值
	 */
	public int getBaseBowDmgup() {
		return _baseBowDmgup;
	}

	/** 基礎命中加成值（範圍：-128～127） */
	private int _baseHitup = 0;

	/**
	 * 取得基礎命中加成值
	 *
	 * @return 基礎命中加成值
	 */
	public int getBaseHitup() {
		return _baseHitup;
	}

	/** 基礎弓箭命中加成值（範圍：-128～127） */
	private int _baseBowHitup = 0;

	/**
	 * 取得基礎弓箭命中加成值
	 *
	 * @return 基礎弓箭命中加成值
	 */
	public int getBaseBowHitup() {
		return _baseBowHitup;
	}

	/** 基礎魔法防禦值（範圍：0～） */
	private int _baseMr = 0;

	/**
	 * 取得基礎魔法防禦值
	 *
	 * @return 基礎魔法防禦值
	 */
	public int getBaseMr() {
		return _baseMr;
	}

	/** 透過進階精靈增加的 HP 值 */
	private int _advenHp;

	/**
	 * 取得進階精靈增加的 HP 值
	 *
	 * @return 增加的 HP 值
	 */
	public int getAdvenHp() {
		return _advenHp;
	}

	/**
	 * 設定進階精靈增加的 HP 值
	 *
	 * @param i HP 值
	 */
	public void setAdvenHp(int i) {
		_advenHp = i;
	}

	/** 透過進階精靈增加的 MP 值 */
	private int _advenMp;

	/**
	 * 取得進階精靈增加的 MP 值
	 *
	 * @return 增加的 MP 值
	 */
	public int getAdvenMp() {
		return _advenMp;
	}

	/**
	 * 設定進階精靈增加的 MP 值
	 *
	 * @param i MP 值
	 */
	public void setAdvenMp(int i) {
		_advenMp = i;
	}

	/** 過去最高等級 */
	private int _highLevel;

	/**
	 * 取得過去最高等級
	 *
	 * @return 最高等級
	 */
	public int getHighLevel() {
		return _highLevel;
	}

	/**
	 * 設定過去最高等級
	 *
	 * @param i 等級值
	 */
	public void setHighLevel(int i) {
		_highLevel = i;
	}

	/** 已分配的額外能力值點數 */
	private int _bonusStats;

	/**
	 * 取得已分配的額外能力值點數
	 *
	 * @return 額外能力值點數
	 */
	public int getBonusStats() {
		return _bonusStats;
	}

	/**
	 * 設定已分配的額外能力值點數
	 *
	 * @param i 額外能力值點數
	 */
	public void setBonusStats(int i) {
		_bonusStats = i;
	}

	/** 透過煉藥提升的能力值點數 */
	private int _elixirStats;

	/**
	 * 取得透過煉藥提升的能力值點數
	 *
	 * @return 煉藥能力值點數
	 */
	public int getElixirStats() {
		return _elixirStats;
	}

	/**
	 * 設定透過煉藥提升的能力值點數
	 *
	 * @param i 煉藥能力值點數
	 */
	public void setElixirStats(int i) {
		_elixirStats = i;
	}

	/** 精靈的屬性類型 */
	private int _elfAttr;

	/**
	 * 取得精靈的屬性類型
	 *
	 * @return 屬性類型值
	 */
	public int getElfAttr() {
		return _elfAttr;
	}

	/**
	 * 設定精靈的屬性類型
	 *
	 * @param i 屬性類型值
	 */
	public void setElfAttr(int i) {
		_elfAttr = i;
	}

	/** 經驗值復活補償計數 */
	private int _expRes;

	/**
	 * 取得經驗值復活補償計數
	 *
	 * @return 復活補償計數
	 */
	public int getExpRes() {
		return _expRes;
	}

	/**
	 * 設定經驗值復活補償計數
	 *
	 * @param i 復活補償計數
	 */
	public void setExpRes(int i) {
		_expRes = i;
	}

	/** 結婚對象的角色 ID */
	private int _partnerId;

	/**
	 * 取得結婚對象的角色 ID
	 *
	 * @return 結婚對象 ID
	 */
	public int getPartnerId() {
		return _partnerId;
	}

	/**
	 * 設定結婚對象的角色 ID
	 *
	 * @param i 結婚對象 ID
	 */
	public void setPartnerId(int i) {
		_partnerId = i;
	}

	/** 線上狀態 */
	private int _onlineStatus;

	/**
	 * 取得線上狀態
	 *
	 * @return 線上狀態值
	 */
	public int getOnlineStatus() {
		return _onlineStatus;
	}

	/**
	 * 設定線上狀態
	 *
	 * @param i 線上狀態值
	 */
	public void setOnlineStatus(int i) {
		_onlineStatus = i;
	}

	/** 所屬村莊 ID */
	private int _homeTownId;

	/**
	 * 取得所屬村莊 ID
	 *
	 * @return 村莊 ID
	 */
	public int getHomeTownId() {
		return _homeTownId;
	}

	/**
	 * 設定所屬村莊 ID
	 *
	 * @param i 村莊 ID
	 */
	public void setHomeTownId(int i) {
		_homeTownId = i;
	}

	/** 村莊貢獻度 */
	private int _contribution;

	/**
	 * 取得村莊貢獻度
	 *
	 * @return 貢獻度值
	 */
	public int getContribution() {
		return _contribution;
	}

	/**
	 * 設定村莊貢獻度
	 *
	 * @param i 貢獻度值
	 */
	public void setContribution(int i) {
		_contribution = i;
	}

	/** 村莊福利金（由 HomeTownTimeController 處理更新） */
	private int _pay;

	/**
	 * 取得村莊福利金
	 *
	 * @return 福利金金額
	 */
	public int getPay() {
		return _pay;
	}

	/**
	 * 設定村莊福利金
	 *
	 * @param i 福利金金額
	 */
	public void setPay(int i) {
		_pay = i;
	}

	/** 在地獄中停留的時間（秒） */
	private int _hellTime;

	/**
	 * 取得在地獄中停留的時間
	 *
	 * @return 停留時間（秒）
	 */
	public int getHellTime() {
		return _hellTime;
	}

	/**
	 * 設定在地獄中停留的時間
	 *
	 * @param i 停留時間（秒）
	 */
	public void setHellTime(int i) {
		_hellTime = i;
	}

	/** 帳號是否被凍結 */
	private boolean _banned;

	/**
	 * 檢查帳號是否被凍結
	 *
	 * @return 若帳號被凍結則返回 true
	 */
	public boolean isBanned() {
		return _banned;
	}

	/**
	 * 設定帳號凍結狀態
	 *
	 * @param flag 凍結狀態
	 */
	public void setBanned(boolean flag) {
		_banned = flag;
	}

	/**
	 * 取得裝備欄位管理物件
	 *
	 * @return 裝備欄位管理物件
	 */
	public L1EquipmentSlot getEquipSlot() {
		return _equipSlot;
	}

	/**
	 * 設定飽食度
	 * <p>
	 * 覆寫父類別方法以同時更新「生存吶喊」技能的充電時間。
	 * </p>
	 *
	 * @param i 飽食度值
	 */
	@Override
	public void set_food(int i) {
		super.set_food(i);
		setCryOfSurvivalTime();
	}

	/** 生存吶喊技能飽食度 100% 充電時間（Unix 時間戳） */
	private long _cryofsurvivaltime;

	/**
	 * 取得生存吶喊技能的充電時間
	 *
	 * @return 充電時間（Unix 時間戳）
	 */
	public long getCryOfSurvivalTime() {
		return _cryofsurvivaltime;
	}

	/**
	 * 更新生存吶喊技能的充電時間
	 * <p>
	 * 當飽食度達到 225（100%）時，記錄當前時間作為充電起始點。
	 * </p>
	 */
	public void setCryOfSurvivalTime() {
		if (get_food() >= 225) {
			_cryofsurvivaltime = System.currentTimeMillis() / 1000;
		}
	}

	/** 擊殺怪物數量統計 */
	private int _monskill = 0;

	/**
	 * 取得擊殺怪物數量
	 *
	 * @return 擊殺數量
	 */
	public int getMonsKill(){
		return _monskill;
	}

	/**
	 * 設定擊殺怪物數量
	 * <p>
	 * 設定後會立即更新客戶端顯示。
	 * </p>
	 *
	 * @param i 擊殺數量
	 */
	public void setMonsKill(int i){
		_monskill = i;
		sendPackets(new S_OwnCharStatus(this));
	}

	/**
	 * 增加擊殺怪物數量
	 * <p>
	 * 每次呼叫增加 1，並立即更新客戶端顯示。
	 * </p>
	 */
	public void addMonsKill(){
		_monskill += 1;
		sendPackets(new S_OwnCharStatus(this));
	}

	/**
	 * 從資料庫載入指定名稱的角色
	 *
	 * @param charName 角色名稱
	 * @return 載入的角色實例，若發生錯誤則返回 null
	 */
	public static L1PcInstance load(String charName) {
		L1PcInstance result = null;
		try {
			result = CharacterTable.getInstance().loadCharacter(charName);
		}
		catch (Exception e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		return result;
	}

	/**
	 * 將此角色的狀態儲存到資料庫
	 * <p>
	 * 如果角色處於幽靈狀態或正在重置中，則不執行儲存。
	 * </p>
	 *
	 * @throws Exception 儲存過程中發生的異常
	 */
	public void save() throws Exception {
		if (isGhost()) {
			return;
		}
		if (isInCharReset()) {
			return;
		}

		CharacterTable.getInstance().storeCharacter(this);
	}

	/**
	 * 將此角色背包中所有物品的狀態儲存到資料庫
	 * <p>
	 * 遍歷背包中的所有物品，逐一儲存物品資料及附魔飾品資料。
	 * </p>
	 */
	public void saveInventory() {
		for (L1ItemInstance item : getInventory().getItems()) {
			getInventory().saveItem(item, item.getRecordingColumns());
			getInventory().saveEnchantAccessory(item, item.getRecordingColumnsEnchantAccessory());
		}
	}

	/** 恢復狀態：無動作 */
	public static final int REGENSTATE_NONE = 4;

	/** 恢復狀態：移動中 */
	public static final int REGENSTATE_MOVE = 2;

	/** 恢復狀態：攻擊中 */
	public static final int REGENSTATE_ATTACK = 1;

	/**
	 * 設定 HP/MP 恢復狀態
	 * <p>
	 * 根據角色行為（無動作、移動、攻擊）調整 HP 和 MP 的恢復速度。
	 * </p>
	 *
	 * @param state 恢復狀態常數（REGENSTATE_NONE、REGENSTATE_MOVE、REGENSTATE_ATTACK）
	 */
	public void setRegenState(int state) {
		_mpRegen.setState(state);
		_hpRegen.setState(state);
	}

	/**
	 * 計算角色的最大負重
	 * <p>
	 * 最大負重由以下因素決定：
	 * </p>
	 * <ul>
	 * <li>基礎負重：150 × floor(0.6×STR + 0.4×CON + 1)</li>
	 * <li>防具重量減免加成</li>
	 * <li>魔法人偶重量減免加成</li>
	 * <li>魔法效果（減重術）：+180</li>
	 * <li>原始能力值重量減免：0.04 × (STR減免 + CON減免)</li>
	 * <li>伺服器負重倍率（Config.RATE_WEIGHT_LIMIT）</li>
	 * </ul>
	 *
	 * @return 最大負重值
	 */
	public double getMaxWeight() {
		int str = getStr();
		int con = getCon();
		double maxWeight = 150 * (Math.floor(0.6 * str + 0.4 * con + 1));

		double weightReductionByArmor = getWeightReduction(); // 防具による重量軽減
		weightReductionByArmor /= 100;

		double weightReductionByDoll = 0; // マジックドールによる重量軽減
		weightReductionByDoll += L1MagicDoll.getWeightReductionByDoll(this);
		weightReductionByDoll /= 100;

		int weightReductionByMagic = 0;
		if (hasSkillEffect(DECREASE_WEIGHT)) { // ディクリースウェイト
			weightReductionByMagic = 180;
		}

		double originalWeightReduction = 0; // オリジナルステータスによる重量軽減
		originalWeightReduction += 0.04 * (getOriginalStrWeightReduction() + getOriginalConWeightReduction());

		double weightReduction = 1 + weightReductionByArmor
				+ weightReductionByDoll + originalWeightReduction;

		maxWeight *= weightReduction;

		maxWeight += weightReductionByMagic;

		maxWeight *= Config.RATE_WEIGHT_LIMIT; // ウェイトレートを掛ける

		return maxWeight;
	}

	/**
	 * 檢查是否擁有生命之樹果實效果
	 * <p>
	 * 生命之樹果實效果會使移動速度提升 15%（×1.15）。
	 * </p>
	 *
	 * @return 若擁有效果則返回 true
	 */
	public boolean isRibrave() {
		return hasSkillEffect(STATUS_RIBRAVE);
	}

	/**
	 * 檢查是否擁有三段加速效果
	 * <p>
	 * 三段加速效果會使移動速度提升 15%（×1.15）。
	 * </p>
	 *
	 * @return 若擁有效果則返回 true
	 */
	public boolean isThirdSpeed() {
		return hasSkillEffect(STATUS_THIRD_SPEED);
	}

	/**
	 * 檢查是否擁有風之枷鎖效果
	 * <p>
	 * 風之枷鎖效果會使攻擊速度減半（÷2）。
	 * </p>
	 *
	 * @return 若擁有效果則返回 true
	 */
	public boolean isWindShackle() {
		return hasSkillEffect(WIND_SHACKLE);
	}

	/** 隱身延遲計數器 */
	private int invisDelayCounter = 0;

	/**
	 * 檢查是否處於隱身延遲狀態
	 * <p>
	 * 隱身延遲期間無法再次進入隱身狀態。
	 * </p>
	 *
	 * @return 若處於延遲狀態則返回 true
	 */
	public boolean isInvisDelay() {
		return (invisDelayCounter > 0);
	}

	/** 隱身計時器同步鎖 */
	private Object _invisTimerMonitor = new Object();

	/**
	 * 增加隱身延遲計數器
	 *
	 * @param counter 要增加的計數值
	 */
	public void addInvisDelayCounter(int counter) {
		synchronized (_invisTimerMonitor) {
			invisDelayCounter += counter;
		}
	}

	/** 隱身延遲時間（毫秒） */
	private static final long DELAY_INVIS = 3000L;

	/**
	 * 開始隱身延遲計時器
	 * <p>
	 * 在隱身後會啟動 3 秒的延遲計時器，期間無法再次進入隱身。
	 * </p>
	 */
	public void beginInvisTimer() {
		addInvisDelayCounter(1);
		GeneralThreadPool.getInstance().pcSchedule(new L1PcInvisDelay(getId()), DELAY_INVIS);
	}

	/**
	 * 增加經驗值
	 * <p>
	 * 同步方法，確保多執行緒安全。經驗值不會超過最大值（ExpTable.MAX_EXP）。
	 * </p>
	 *
	 * @param exp 要增加的經驗值（可為負數以扣除經驗值）
	 */
	public synchronized void addExp(int exp) {
		_exp += exp;
		if (_exp > ExpTable.MAX_EXP) {
			_exp = ExpTable.MAX_EXP;
		}
	}

	/**
	 * 增加村莊貢獻度
	 * <p>
	 * 同步方法，確保多執行緒安全。
	 * </p>
	 *
	 * @param contribution 要增加的貢獻度
	 */
	public synchronized void addContribution(int contribution) {
		_contribution += contribution;
	}

	/**
	 * 開始經驗值監控器
	 * <p>
	 * 定期檢查角色經驗值並處理升級。
	 * </p>
	 */
	public void beginExpMonitor() {
		_expMonitorFuture = GeneralThreadPool.getInstance().pcScheduleAtFixedRate(new L1PcExpMonitor(getId()), 0L, INTERVAL_EXP_MONITOR);
	}

	/**
	 * 角色升級處理
	 * <p>
	 * 處理角色升級時的各項變更，包括：
	 * </p>
	 * <ul>
	 * <li>重置等級相關數值</li>
	 * <li>增加 HP/MP 上限（根據職業和能力值隨機計算）</li>
	 * <li>重置基礎命中、傷害、AC、MR 等數值</li>
	 * <li>更新歷史最高等級</li>
	 * <li>99 級時獲得復活藥水（若伺服器啟用此功能）</li>
	 * <li>51 級以上可獲得額外能力值點數（Bonus Stats）</li>
	 * <li>檢查地圖限制並自動傳送</li>
	 * <li>更新新手保護狀態</li>
	 * </ul>
	 *
	 * @param gap 升級的等級數量
	 */
	private void levelUp(int gap) {
		resetLevel();

		// 復活のポーション
		if ((getLevel() == 99) && Config.ALT_REVIVAL_POTION) {
			try {
				L1Item l1item = ItemTable.getInstance().getTemplate(43000);
				if (l1item != null) {
					getInventory().storeItem(43000, 1);
					sendPackets(new S_ServerMessage(403, l1item.getName()));
				}
				else {
					sendPackets(new S_SystemMessage("返生藥水取得失敗。"));
				}
			}
			catch (Exception e) {
				_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
				sendPackets(new S_SystemMessage("返生藥水取得失敗。"));
			}
		}

		for (int i = 0; i < gap; i++) {
			short randomHp = CalcStat.calcStatHp(getType(), getBaseMaxHp(), getBaseCon(), getOriginalHpup());
			short randomMp = CalcStat.calcStatMp(getType(), getBaseMaxMp(), getBaseWis(), getOriginalMpup());
			addBaseMaxHp(randomHp);
			addBaseMaxMp(randomMp);
		}
		resetBaseHitup();
		resetBaseDmgup();
		resetBaseAc();
		resetBaseMr();
		if (getLevel() > getHighLevel()) {
			setHighLevel(getLevel());
		}

		try {
			// DBにキャラクター情報を書き込む
			save();
		}
		catch (Exception e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		// ボーナスステータス
		if ((getLevel() >= 51) && (getLevel() - 50 > getBonusStats())) {
			if ((getBaseStr() + getBaseDex() + getBaseCon() + getBaseInt() + getBaseWis() + getBaseCha()) < 210) {
				sendPackets(new S_bonusstats(getId(), 1));
			}
		}
		sendPackets(new S_OwnCharStatus(this));

		// 根據等級判斷地圖限制
		if ((getMapId() == 2005 || getMapId() == 86)) { // 新手村
			if (getLevel() >= 13) { // 等級大於13
				if (getQuest().get_step(L1Quest.QUEST_TUTOR) != 255) {
					getQuest().set_step(L1Quest.QUEST_TUTOR, 255);
				}
				L1Teleport.teleport(this, 33084, 33391, (short) 4, 5, true);// 銀騎士村
			}
		} else if (getLevel() >= 52) { // 指定レベル
			if (getMapId() == 777) { // 見捨てられた者たちの地(影の神殿)
				L1Teleport.teleport(this, 34043, 32184, (short) 4, 5, true); // 象牙の塔前
			} else if ((getMapId() == 778) || (getMapId() == 779)) { // 見捨てられた者たちの地(欲望の洞窟)
				L1Teleport.teleport(this, 32608, 33178, (short) 4, 5, true); // WB
			}
		}

		// 處理新手保護系統(遭遇的守護)狀態資料的變動
		checkNoviceType();
	}

	/**
	 * 角色降級處理
	 * <p>
	 * 處理角色降級時的各項變更，包括：
	 * </p>
	 * <ul>
	 * <li>重置等級相關數值</li>
	 * <li>減少 HP/MP 上限（根據職業和能力值隨機計算）</li>
	 * <li>重置基礎命中、傷害、AC、MR 等數值</li>
	 * <li>檢查降級範圍限制，超過則強制斷線</li>
	 * <li>更新新手保護狀態</li>
	 * </ul>
	 * <p>
	 * 注意：若設定了 LEVEL_DOWN_RANGE，當前等級與歷史最高等級相差超過此範圍時，
	 * 角色會被強制斷線以防止異常降級。
	 * </p>
	 *
	 * @param gap 降級的等級數量（負數）
	 */
	private void levelDown(int gap) {
		resetLevel();

		for (int i = 0; i > gap; i--) {
			// レベルダウン時はランダム値をそのままマイナスする為に、base値に0を設定
			short randomHp = CalcStat.calcStatHp(getType(), 0, getBaseCon(), getOriginalHpup());
			short randomMp = CalcStat.calcStatMp(getType(), 0, getBaseWis(), getOriginalMpup());
			addBaseMaxHp((short) -randomHp);
			addBaseMaxMp((short) -randomMp);
		}
		resetBaseHitup();
		resetBaseDmgup();
		resetBaseAc();
		resetBaseMr();
		if (Config.LEVEL_DOWN_RANGE != 0) {
			if (getHighLevel() - getLevel() >= Config.LEVEL_DOWN_RANGE) {
				sendPackets(new S_ServerMessage(64)); // ワールドとの接続が切断されました。
				sendPackets(new S_Disconnect());
				_log.info(String.format("レベルダウンの許容範囲を超えたため%sを強制切断しました。", getName()));
			}
		}

		try {
			// DBにキャラクター情報を書き込む
			save();
		}
		catch (Exception e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		sendPackets(new S_OwnCharStatus(this));

		// 處理新手保護系統(遭遇的守護)狀態資料的變動
		checkNoviceType();
	}

	/**
	 * 開始遊戲時間傳遞器
	 * <p>
	 * 啟動一個執行緒，定期向客戶端同步遊戲內的時間資訊。
	 * </p>
	 */
	public void beginGameTimeCarrier() {
		new L1GameTimeCarrier(this).start();
	}

	/** 是否處於幽靈狀態（觀察者模式） */
	private boolean _ghost = false;

	/**
	 * 檢查是否處於幽靈狀態
	 * <p>
	 * 幽靈狀態下的角色無法進行正常遊戲互動，通常用於觀察模式或特殊事件。
	 * </p>
	 *
	 * @return 若處於幽靈狀態則返回 true
	 */
	public boolean isGhost() {
		return _ghost;
	}

	/**
	 * 設定幽靈狀態
	 *
	 * @param flag 幽靈狀態
	 */
	private void setGhost(boolean flag) {
		_ghost = flag;
	}

	/** 幽靈狀態下是否可與 NPC 對話 */
	private boolean _ghostCanTalk = true;

	/**
	 * 檢查幽靈狀態下是否可與 NPC 對話
	 *
	 * @return 若可對話則返回 true
	 */
	public boolean isGhostCanTalk() {
		return _ghostCanTalk;
	}

	/**
	 * 設定幽靈狀態下是否可與 NPC 對話
	 *
	 * @param flag 對話權限
	 */
	private void setGhostCanTalk(boolean flag) {
		_ghostCanTalk = flag;
	}

	/** 是否準備解除幽靈狀態 */
	private boolean _isReserveGhost = false;

	/**
	 * 檢查是否準備解除幽靈狀態
	 *
	 * @return 若準備解除則返回 true
	 */
	public boolean isReserveGhost() {
		return _isReserveGhost;
	}

	/**
	 * 設定是否準備解除幽靈狀態
	 *
	 * @param flag 準備狀態
	 */
	private void setReserveGhost(boolean flag) {
		_isReserveGhost = flag;
	}

	/**
	 * 開始幽靈狀態（無時間限制）
	 *
	 * @param locx 目標 X 座標
	 * @param locy 目標 Y 座標
	 * @param mapid 目標地圖 ID
	 * @param canTalk 是否可與 NPC 對話
	 */
	public void beginGhost(int locx, int locy, short mapid, boolean canTalk) {
		beginGhost(locx, locy, mapid, canTalk, 0);
	}

	/**
	 * 開始幽靈狀態
	 * <p>
	 * 將角色傳送到指定位置並進入幽靈狀態。會記錄原本的位置以便之後返回。
	 * </p>
	 *
	 * @param locx 目標 X 座標
	 * @param locy 目標 Y 座標
	 * @param mapid 目標地圖 ID
	 * @param canTalk 是否可與 NPC 對話
	 * @param sec 幽靈狀態持續時間（秒），0 表示無時間限制
	 */
	public void beginGhost(int locx, int locy, short mapid, boolean canTalk, int sec) {
		if (isGhost()) {
			return;
		}
		setGhost(true);
		_ghostSaveLocX = getX();
		_ghostSaveLocY = getY();
		_ghostSaveMapId = getMapId();
		_ghostSaveHeading = getHeading();
		setGhostCanTalk(canTalk);
		L1Teleport.teleport(this, locx, locy, mapid, 5, true);
		if (sec > 0) {
			_ghostFuture = GeneralThreadPool.getInstance().pcSchedule(new L1PcGhostMonitor(getId()), sec * 1000);
		}
	}

	/**
	 * 準備結束幽靈狀態
	 * <p>
	 * 將角色傳送回進入幽靈狀態前的原始位置，並標記為準備解除狀態。
	 * </p>
	 */
	public void makeReadyEndGhost() {
		setReserveGhost(true);
		L1Teleport.teleport(this, _ghostSaveLocX, _ghostSaveLocY, _ghostSaveMapId, _ghostSaveHeading, true);
	}

	/**
	 * 結束幽靈狀態
	 * <p>
	 * 清除幽靈狀態標記，恢復正常遊戲狀態。
	 * </p>
	 */
	public void endGhost() {
		setGhost(false);
		setGhostCanTalk(true);
		setReserveGhost(false);
	}

	/** 幽靈狀態計時任務 */
	private ScheduledFuture<?> _ghostFuture;

	/** 進入幽靈狀態前的 X 座標 */
	private int _ghostSaveLocX = 0;

	/** 進入幽靈狀態前的 Y 座標 */
	private int _ghostSaveLocY = 0;

	/** 進入幽靈狀態前的地圖 ID */
	private short _ghostSaveMapId = 0;

	/** 進入幽靈狀態前的面向 */
	private int _ghostSaveHeading = 0;

	/** 地獄懲罰計時任務 */
	private ScheduledFuture<?> _hellFuture;

	/**
	 * 開始地獄懲罰
	 * <p>
	 * 將 PK 值過高的角色傳送到地獄並開始計時懲罰。
	 * 懲罰時間根據 PK 值計算：
	 * </p>
	 * <ul>
	 * <li>PK ≤ 10：5 分鐘</li>
	 * <li>PK > 10：5 分鐘 + (PK - 10) × 5 分鐘</li>
	 * </ul>
	 *
	 * @param isFirst 是否為首次進入地獄（首次會計算懲罰時間）
	 */
	public void beginHell(boolean isFirst) {
		// 地獄以外に居るときは地獄へ強制移動
		if (getMapId() != 666) {
			int locx = 32701;
			int locy = 32777;
			short mapid = 666;
			L1Teleport.teleport(this, locx, locy, mapid, 5, false);
		}

		if (isFirst) {
			if (get_PKcount() <= 10) {
				setHellTime(300);
			}
			else {
				setHellTime(300 * (get_PKcount() - 10) + 300);
			}
			// あなたのPK回数が%0になり、地獄に落とされました。あなたはここで%1分間反省しなければなりません。
			sendPackets(new S_RedMessage(552, String.valueOf(get_PKcount()), String.valueOf(getHellTime() / 60)));
		}
		else {
			// あなたは%0秒間ここにとどまらなければなりません。
			sendPackets(new S_RedMessage(637, String.valueOf(getHellTime())));
		}
		if (_hellFuture == null) {
			_hellFuture = GeneralThreadPool.getInstance().pcScheduleAtFixedRate(new L1PcHellMonitor(getId()), 0L, 1000L);
		}
	}

	public void endHell() {
		if (_hellFuture != null) {
			_hellFuture.cancel(false);
			_hellFuture = null;
		}
		// 地獄から脱出したら火田村へ帰還させる。
		int[] loc = L1TownLocation.getGetBackLoc(L1TownLocation.TOWNID_ORCISH_FOREST);
		L1Teleport.teleport(this, loc[0], loc[1], (short) loc[2], 5, true);
		try {
			save();
		}
		catch (Exception ignore) {
			// ignore
		}
	}

	@Override
	public void setPoisonEffect(int effectId) {
		sendPackets(new S_Poison(getId(), effectId));

		if (!isGmInvis() && !isGhost() && !isInvisble()) {
			broadcastPacket(new S_Poison(getId(), effectId));
		}
		if (isGmInvis() || isGhost()) {}
		else if (isInvisble()) {
			broadcastPacketForFindInvis(new S_Poison(getId(), effectId), true);
		}
		else {
			broadcastPacket(new S_Poison(getId(), effectId));
		}
	}

	@Override
	public void healHp(int pt) {
		super.healHp(pt);

		sendPackets(new S_HPUpdate(this));
	}

	@Override
	public int getKarma() {
		return _karma.get();
	}

	@Override
	public void setKarma(int i) {
		_karma.set(i);
	}

	public void addKarma(int i) {
		synchronized (_karma) {
			_karma.add(i);
		}
	}

	public int getKarmaLevel() {
		return _karma.getLevel();
	}

	public int getKarmaPercent() {
		return _karma.getPercent();
	}

	private Timestamp _lastPk;

	/**
	 * プレイヤーの最終PK時間を返す。
	 * 
	 * @return _lastPk
	 * 
	 */
	public Timestamp getLastPk() {
		return _lastPk;
	}

	/**
	 * プレイヤーの最終PK時間を設定する。
	 * 
	 * @param time
	 *            最終PK時間（Timestamp型） 解除する場合はnullを代入
	 */
	public void setLastPk(Timestamp time) {
		_lastPk = time;
	}

	/**
	 * プレイヤーの最終PK時間を現在の時刻に設定する。
	 */
	public void setLastPk() {
		_lastPk = new Timestamp(System.currentTimeMillis());
	}

	/**
	 * プレイヤーが手配中であるかを返す。
	 * 
	 * @return 手配中であれば、true
	 */
	public boolean isWanted() {
		if (_lastPk == null) {
			return false;
		}
		else if (System.currentTimeMillis() - _lastPk.getTime() > 24 * 3600 * 1000) {
			setLastPk(null);
			return false;
		}
		return true;
	}

	private Timestamp _lastPkForElf;

	public Timestamp getLastPkForElf() {
		return _lastPkForElf;
	}

	public void setLastPkForElf(Timestamp time) {
		_lastPkForElf = time;
	}

	public void setLastPkForElf() {
		_lastPkForElf = new Timestamp(System.currentTimeMillis());
	}

	public boolean isWantedForElf() {
		if (_lastPkForElf == null) {
			return false;
		}
		else if (System.currentTimeMillis() - _lastPkForElf.getTime() > 24 * 3600 * 1000) {
			setLastPkForElf(null);
			return false;
		}
		return true;
	}

	private Timestamp _deleteTime; // キャラクター削除までの時間

	public Timestamp getDeleteTime() {
		return _deleteTime;
	}

	public void setDeleteTime(Timestamp time) {
		_deleteTime = time;
	}

	@Override
	public int getMagicLevel() {
		return getClassFeature().getMagicLevel(getLevel());
	}

	private int _weightReduction = 0;

	public int getWeightReduction() {
		return _weightReduction;
	}

	public void addWeightReduction(int i) {
		_weightReduction += i;
	}

	private int _originalStrWeightReduction = 0; // ● オリジナルSTR 重量軽減

	public int getOriginalStrWeightReduction() {

		return _originalStrWeightReduction;
	}

	private int _originalConWeightReduction = 0; // ● オリジナルCON 重量軽減

	public int getOriginalConWeightReduction() {

		return _originalConWeightReduction;
	}

	private int _hasteItemEquipped = 0;

	public int getHasteItemEquipped() {
		return _hasteItemEquipped;
	}

	public void addHasteItemEquipped(int i) {
		_hasteItemEquipped += i;
	}

	public void removeHasteSkillEffect() {
		if (hasSkillEffect(SLOW)) {
			removeSkillEffect(SLOW);
		}
		if (hasSkillEffect(MASS_SLOW)) {
			removeSkillEffect(MASS_SLOW);
		}
		if (hasSkillEffect(ENTANGLE)) {
			removeSkillEffect(ENTANGLE);
		}
		if (hasSkillEffect(HASTE)) {
			removeSkillEffect(HASTE);
		}
		if (hasSkillEffect(GREATER_HASTE)) {
			removeSkillEffect(GREATER_HASTE);
		}
		if (hasSkillEffect(STATUS_HASTE)) {
			removeSkillEffect(STATUS_HASTE);
		}
	}

	private int _damageReductionByArmor = 0; // 防具によるダメージ軽減

	public int getDamageReductionByArmor() {
		return _damageReductionByArmor;
	}

	public void addDamageReductionByArmor(int i) {
		_damageReductionByArmor += i;
	}

	private int _hitModifierByArmor = 0; // 防具による命中率補正

	public int getHitModifierByArmor() {
		return _hitModifierByArmor;
	}

	public void addHitModifierByArmor(int i) {
		_hitModifierByArmor += i;
	}

	private int _dmgModifierByArmor = 0; // 防具によるダメージ補正

	public int getDmgModifierByArmor() {
		return _dmgModifierByArmor;
	}

	public void addDmgModifierByArmor(int i) {
		_dmgModifierByArmor += i;
	}

	private int _bowHitModifierByArmor = 0; // 防具による弓の命中率補正

	public int getBowHitModifierByArmor() {
		return _bowHitModifierByArmor;
	}

	public void addBowHitModifierByArmor(int i) {
		_bowHitModifierByArmor += i;
	}

	private int _bowDmgModifierByArmor = 0; // 防具による弓のダメージ補正

	public int getBowDmgModifierByArmor() {
		return _bowDmgModifierByArmor;
	}

	public void addBowDmgModifierByArmor(int i) {
		_bowDmgModifierByArmor += i;
	}

	private boolean _gresValid; // G-RESが有効か

	private void setGresValid(boolean valid) {
		_gresValid = valid;
	}

	public boolean isGresValid() {
		return _gresValid;
	}

	private long _fishingTime = 0;

	public long getFishingTime() {
		return _fishingTime;
	}

	public void setFishingTime(long i) {
		_fishingTime = i;
	}

	private boolean _isFishing = false;

	public boolean isFishing() {
		return _isFishing;
	}

	public void setFishing(boolean flag) {
		_isFishing = flag;
	}

	private boolean _isFishingReady = false;

	public boolean isFishingReady() {
		return _isFishingReady;
	}

	public void setFishingReady(boolean flag) {
		_isFishingReady = flag;
	}

	private int _cookingId = 0;

	public int getCookingId() {
		return _cookingId;
	}

	public void setCookingId(int i) {
		_cookingId = i;
	}

	private int _dessertId = 0;

	public int getDessertId() {
		return _dessertId;
	}

	public void setDessertId(int i) {
		_dessertId = i;
	}

	/**
	 * LVによる命中ボーナスを設定する LVが変動した場合などに呼び出せば再計算される
	 * 
	 * @return
	 */
	public void resetBaseDmgup() {
		int newBaseDmgup = 0;
		int newBaseBowDmgup = 0;
		if (isKnight() || isDarkelf() || isDragonKnight()) { // ナイト、ダークエルフ、ドラゴンナイト
			newBaseDmgup = getLevel() / 10;
			newBaseBowDmgup = 0;
		}
		else if (isElf()) { // エルフ
			newBaseDmgup = 0;
			newBaseBowDmgup = getLevel() / 10;
		}
		addDmgup(newBaseDmgup - _baseDmgup);
		addBowDmgup(newBaseBowDmgup - _baseBowDmgup);
		_baseDmgup = newBaseDmgup;
		_baseBowDmgup = newBaseBowDmgup;
	}

	/**
	 * LVによる命中ボーナスを設定する LVが変動した場合などに呼び出せば再計算される
	 * 
	 * @return
	 */
	public void resetBaseHitup() {
		int newBaseHitup = 0;
		int newBaseBowHitup = 0;
		if (isCrown()) { // プリ
			newBaseHitup = getLevel() / 5;
			newBaseBowHitup = getLevel() / 5;
		}
		else if (isKnight()) { // ナイト
			newBaseHitup = getLevel() / 3;
			newBaseBowHitup = getLevel() / 3;
		}
		else if (isElf()) { // エルフ
			newBaseHitup = getLevel() / 5;
			newBaseBowHitup = getLevel() / 5;
		}
		else if (isDarkelf()) { // ダークエルフ
			newBaseHitup = getLevel() / 3;
			newBaseBowHitup = getLevel() / 3;
		}
		else if (isDragonKnight()) { // ドラゴンナイト
			newBaseHitup = getLevel() / 3;
			newBaseBowHitup = getLevel() / 3;
		}
		else if (isIllusionist()) { // イリュージョニスト
			newBaseHitup = getLevel() / 5;
			newBaseBowHitup = getLevel() / 5;
		}
		addHitup(newBaseHitup - _baseHitup);
		addBowHitup(newBaseBowHitup - _baseBowHitup);
		_baseHitup = newBaseHitup;
		_baseBowHitup = newBaseBowHitup;
	}

	/**
	 * キャラクターステータスからACを再計算して設定する 初期設定時、LVUP,LVDown時などに呼び出す
	 */
	public void resetBaseAc() {
		int newAc = CalcStat.calcAc(getLevel(), getBaseDex());
		addAc(newAc - _baseAc);
		_baseAc = newAc;
	}

	/**
	 * キャラクターステータスから素のMRを再計算して設定する 初期設定時、スキル使用時やLVUP,LVDown時に呼び出す
	 */
	public void resetBaseMr() {
		int newMr = 0;
		if (isCrown()) { // プリ
			newMr = 10;
		}
		else if (isElf()) { // エルフ
			newMr = 25;
		}
		else if (isWizard()) { // ウィザード
			newMr = 15;
		}
		else if (isDarkelf()) { // ダークエルフ
			newMr = 10;
		}
		else if (isDragonKnight()) { // ドラゴンナイト
			newMr = 18;
		}
		else if (isIllusionist()) { // イリュージョニスト
			newMr = 20;
		}
		newMr += CalcStat.calcStatMr(getWis()); // WIS分のMRボーナス
		newMr += getLevel() / 2; // LVの半分だけ追加
		addMr(newMr - _baseMr);
		_baseMr = newMr;
	}

	/**
	 * EXPから現在のLvを再計算して設定する 初期設定時、死亡時やLVUP時に呼び出す
	 */
	public void resetLevel() {
		setLevel(ExpTable.getLevelByExp(_exp));

		if (_hpRegen != null) {
			_hpRegen.updateLevel();
		}
	}

	/**
	 * 初期ステータスから現在のボーナスを再計算して設定する 初期設定時、再配分時に呼び出す
	 */
	public void resetOriginalHpup() {
		int originalCon = getOriginalCon();
		if (isCrown()) {
			if ((originalCon == 12) || (originalCon == 13)) {
				_originalHpup = 1;
			}
			else if ((originalCon == 14) || (originalCon == 15)) {
				_originalHpup = 2;
			}
			else if (originalCon >= 16) {
				_originalHpup = 3;
			}
			else {
				_originalHpup = 0;
			}
		}
		else if (isKnight()) {
			if ((originalCon == 15) || (originalCon == 16)) {
				_originalHpup = 1;
			}
			else if (originalCon >= 17) {
				_originalHpup = 3;
			}
			else {
				_originalHpup = 0;
			}
		}
		else if (isElf()) {
			if ((originalCon >= 13) && (originalCon <= 17)) {
				_originalHpup = 1;
			}
			else if (originalCon == 18) {
				_originalHpup = 2;
			}
			else {
				_originalHpup = 0;
			}
		}
		else if (isDarkelf()) {
			if ((originalCon == 10) || (originalCon == 11)) {
				_originalHpup = 1;
			}
			else if (originalCon >= 12) {
				_originalHpup = 2;
			}
			else {
				_originalHpup = 0;
			}
		}
		else if (isWizard()) {
			if ((originalCon == 14) || (originalCon == 15)) {
				_originalHpup = 1;
			}
			else if (originalCon >= 16) {
				_originalHpup = 2;
			}
			else {
				_originalHpup = 0;
			}
		}
		else if (isDragonKnight()) {
			if ((originalCon == 15) || (originalCon == 16)) {
				_originalHpup = 1;
			}
			else if (originalCon >= 17) {
				_originalHpup = 3;
			}
			else {
				_originalHpup = 0;
			}
		}
		else if (isIllusionist()) {
			if ((originalCon == 13) || (originalCon == 14)) {
				_originalHpup = 1;
			}
			else if (originalCon >= 15) {
				_originalHpup = 2;
			}
			else {
				_originalHpup = 0;
			}
		}
	}

	public void resetOriginalMpup() {
		int originalWis = getOriginalWis();
		{
			if (isCrown()) {
				if (originalWis >= 16) {
					_originalMpup = 1;
				}
				else {
					_originalMpup = 0;
				}
			}
			else if (isKnight()) {
				_originalMpup = 0;
			}
			else if (isElf()) {
				if ((originalWis >= 14) && (originalWis <= 16)) {
					_originalMpup = 1;
				}
				else if (originalWis >= 17) {
					_originalMpup = 2;
				}
				else {
					_originalMpup = 0;
				}
			}
			else if (isDarkelf()) {
				if (originalWis >= 12) {
					_originalMpup = 1;
				}
				else {
					_originalMpup = 0;
				}
			}
			else if (isWizard()) {
				if ((originalWis >= 13) && (originalWis <= 16)) {
					_originalMpup = 1;
				}
				else if (originalWis >= 17) {
					_originalMpup = 2;
				}
				else {
					_originalMpup = 0;
				}
			}
			else if (isDragonKnight()) {
				if ((originalWis >= 13) && (originalWis <= 15)) {
					_originalMpup = 1;
				}
				else if (originalWis >= 16) {
					_originalMpup = 2;
				}
				else {
					_originalMpup = 0;
				}
			}
			else if (isIllusionist()) {
				if ((originalWis >= 13) && (originalWis <= 15)) {
					_originalMpup = 1;
				}
				else if (originalWis >= 16) {
					_originalMpup = 2;
				}
				else {
					_originalMpup = 0;
				}
			}
		}
	}

	public void resetOriginalStrWeightReduction() {
		int originalStr = getOriginalStr();
		if (isCrown()) {
			if ((originalStr >= 14) && (originalStr <= 16)) {
				_originalStrWeightReduction = 1;
			}
			else if ((originalStr >= 17) && (originalStr <= 19)) {
				_originalStrWeightReduction = 2;
			}
			else if (originalStr == 20) {
				_originalStrWeightReduction = 3;
			}
			else {
				_originalStrWeightReduction = 0;
			}
		}
		else if (isKnight()) {
			_originalStrWeightReduction = 0;
		}
		else if (isElf()) {
			if (originalStr >= 16) {
				_originalStrWeightReduction = 2;
			}
			else {
				_originalStrWeightReduction = 0;
			}
		}
		else if (isDarkelf()) {
			if ((originalStr >= 13) && (originalStr <= 15)) {
				_originalStrWeightReduction = 2;
			}
			else if (originalStr >= 16) {
				_originalStrWeightReduction = 3;
			}
			else {
				_originalStrWeightReduction = 0;
			}
		}
		else if (isWizard()) {
			if (originalStr >= 9) {
				_originalStrWeightReduction = 1;
			}
			else {
				_originalStrWeightReduction = 0;
			}
		}
		else if (isDragonKnight()) {
			if (originalStr >= 16) {
				_originalStrWeightReduction = 1;
			}
			else {
				_originalStrWeightReduction = 0;
			}
		}
		else if (isIllusionist()) {
			if (originalStr == 18) {
				_originalStrWeightReduction = 1;
			}
			else {
				_originalStrWeightReduction = 0;
			}
		}
	}

	public void resetOriginalDmgup() {
		int originalStr = getOriginalStr();
		if (isCrown()) {
			if ((originalStr >= 15) && (originalStr <= 17)) {
				_originalDmgup = 1;
			}
			else if (originalStr >= 18) {
				_originalDmgup = 2;
			}
			else {
				_originalDmgup = 0;
			}
		}
		else if (isKnight()) {
			if ((originalStr == 18) || (originalStr == 19)) {
				_originalDmgup = 2;
			}
			else if (originalStr == 20) {
				_originalDmgup = 4;
			}
			else {
				_originalDmgup = 0;
			}
		}
		else if (isElf()) {
			if ((originalStr == 12) || (originalStr == 13)) {
				_originalDmgup = 1;
			}
			else if (originalStr >= 14) {
				_originalDmgup = 2;
			}
			else {
				_originalDmgup = 0;
			}
		}
		else if (isDarkelf()) {
			if ((originalStr >= 14) && (originalStr <= 17)) {
				_originalDmgup = 1;
			}
			else if (originalStr == 18) {
				_originalDmgup = 2;
			}
			else {
				_originalDmgup = 0;
			}
		}
		else if (isWizard()) {
			if ((originalStr == 10) || (originalStr == 11)) {
				_originalDmgup = 1;
			}
			else if (originalStr >= 12) {
				_originalDmgup = 2;
			}
			else {
				_originalDmgup = 0;
			}
		}
		else if (isDragonKnight()) {
			if ((originalStr >= 15) && (originalStr <= 17)) {
				_originalDmgup = 1;
			}
			else if (originalStr >= 18) {
				_originalDmgup = 3;
			}
			else {
				_originalDmgup = 0;
			}
		}
		else if (isIllusionist()) {
			if ((originalStr == 13) || (originalStr == 14)) {
				_originalDmgup = 1;
			}
			else if (originalStr >= 15) {
				_originalDmgup = 2;
			}
			else {
				_originalDmgup = 0;
			}
		}
	}

	public void resetOriginalConWeightReduction() {
		int originalCon = getOriginalCon();
		if (isCrown()) {
			if (originalCon >= 11) {
				_originalConWeightReduction = 1;
			}
			else {
				_originalConWeightReduction = 0;
			}
		}
		else if (isKnight()) {
			if (originalCon >= 15) {
				_originalConWeightReduction = 1;
			}
			else {
				_originalConWeightReduction = 0;
			}
		}
		else if (isElf()) {
			if (originalCon >= 15) {
				_originalConWeightReduction = 2;
			}
			else {
				_originalConWeightReduction = 0;
			}
		}
		else if (isDarkelf()) {
			if (originalCon >= 9) {
				_originalConWeightReduction = 1;
			}
			else {
				_originalConWeightReduction = 0;
			}
		}
		else if (isWizard()) {
			if ((originalCon == 13) || (originalCon == 14)) {
				_originalConWeightReduction = 1;
			}
			else if (originalCon >= 15) {
				_originalConWeightReduction = 2;
			}
			else {
				_originalConWeightReduction = 0;
			}
		}
		else if (isDragonKnight()) {
			_originalConWeightReduction = 0;
		}
		else if (isIllusionist()) {
			if (originalCon == 17) {
				_originalConWeightReduction = 1;
			}
			else if (originalCon == 18) {
				_originalConWeightReduction = 2;
			}
			else {
				_originalConWeightReduction = 0;
			}
		}
	}

	public void resetOriginalBowDmgup() {
		int originalDex = getOriginalDex();
		if (isCrown()) {
			if (originalDex >= 13) {
				_originalBowDmgup = 1;
			}
			else {
				_originalBowDmgup = 0;
			}
		}
		else if (isKnight()) {
			_originalBowDmgup = 0;
		}
		else if (isElf()) {
			if ((originalDex >= 14) && (originalDex <= 16)) {
				_originalBowDmgup = 2;
			}
			else if (originalDex >= 17) {
				_originalBowDmgup = 3;
			}
			else {
				_originalBowDmgup = 0;
			}
		}
		else if (isDarkelf()) {
			if (originalDex == 18) {
				_originalBowDmgup = 2;
			}
			else {
				_originalBowDmgup = 0;
			}
		}
		else if (isWizard()) {
			_originalBowDmgup = 0;
		}
		else if (isDragonKnight()) {
			_originalBowDmgup = 0;
		}
		else if (isIllusionist()) {
			_originalBowDmgup = 0;
		}
	}

	public void resetOriginalHitup() {
		int originalStr = getOriginalStr();
		if (isCrown()) {
			if ((originalStr >= 16) && (originalStr <= 18)) {
				_originalHitup = 1;
			}
			else if (originalStr >= 19) {
				_originalHitup = 2;
			}
			else {
				_originalHitup = 0;
			}
		}
		else if (isKnight()) {
			if ((originalStr == 17) || (originalStr == 18)) {
				_originalHitup = 2;
			}
			else if (originalStr >= 19) {
				_originalHitup = 4;
			}
			else {
				_originalHitup = 0;
			}
		}
		else if (isElf()) {
			if ((originalStr == 13) || (originalStr == 14)) {
				_originalHitup = 1;
			}
			else if (originalStr >= 15) {
				_originalHitup = 2;
			}
			else {
				_originalHitup = 0;
			}
		}
		else if (isDarkelf()) {
			if ((originalStr >= 15) && (originalStr <= 17)) {
				_originalHitup = 1;
			}
			else if (originalStr == 18) {
				_originalHitup = 2;
			}
			else {
				_originalHitup = 0;
			}
		}
		else if (isWizard()) {
			if ((originalStr == 11) || (originalStr == 12)) {
				_originalHitup = 1;
			}
			else if (originalStr >= 13) {
				_originalHitup = 2;
			}
			else {
				_originalHitup = 0;
			}
		}
		else if (isDragonKnight()) {
			if ((originalStr >= 14) && (originalStr <= 16)) {
				_originalHitup = 1;
			}
			else if (originalStr >= 17) {
				_originalHitup = 3;
			}
			else {
				_originalHitup = 0;
			}
		}
		else if (isIllusionist()) {
			if ((originalStr == 12) || (originalStr == 13)) {
				_originalHitup = 1;
			}
			else if ((originalStr == 14) || (originalStr == 15)) {
				_originalHitup = 2;
			}
			else if (originalStr == 16) {
				_originalHitup = 3;
			}
			else if (originalStr >= 17) {
				_originalHitup = 4;
			}
			else {
				_originalHitup = 0;
			}
		}
	}

	public void resetOriginalBowHitup() {
		int originalDex = getOriginalDex();
		if (isCrown()) {
			_originalBowHitup = 0;
		}
		else if (isKnight()) {
			_originalBowHitup = 0;
		}
		else if (isElf()) {
			if ((originalDex >= 13) && (originalDex <= 15)) {
				_originalBowHitup = 2;
			}
			else if (originalDex >= 16) {
				_originalBowHitup = 3;
			}
			else {
				_originalBowHitup = 0;
			}
		}
		else if (isDarkelf()) {
			if (originalDex == 17) {
				_originalBowHitup = 1;
			}
			else if (originalDex == 18) {
				_originalBowHitup = 2;
			}
			else {
				_originalBowHitup = 0;
			}
		}
		else if (isWizard()) {
			_originalBowHitup = 0;
		}
		else if (isDragonKnight()) {
			_originalBowHitup = 0;
		}
		else if (isIllusionist()) {
			_originalBowHitup = 0;
		}
	}

	public void resetOriginalMr() {
		int originalWis = getOriginalWis();
		if (isCrown()) {
			if ((originalWis == 12) || (originalWis == 13)) {
				_originalMr = 1;
			}
			else if (originalWis >= 14) {
				_originalMr = 2;
			}
			else {
				_originalMr = 0;
			}
		}
		else if (isKnight()) {
			if ((originalWis == 10) || (originalWis == 11)) {
				_originalMr = 1;
			}
			else if (originalWis >= 12) {
				_originalMr = 2;
			}
			else {
				_originalMr = 0;
			}
		}
		else if (isElf()) {
			if ((originalWis >= 13) && (originalWis <= 15)) {
				_originalMr = 1;
			}
			else if (originalWis >= 16) {
				_originalMr = 2;
			}
			else {
				_originalMr = 0;
			}
		}
		else if (isDarkelf()) {
			if ((originalWis >= 11) && (originalWis <= 13)) {
				_originalMr = 1;
			}
			else if (originalWis == 14) {
				_originalMr = 2;
			}
			else if (originalWis == 15) {
				_originalMr = 3;
			}
			else if (originalWis >= 16) {
				_originalMr = 4;
			}
			else {
				_originalMr = 0;
			}
		}
		else if (isWizard()) {
			if (originalWis >= 15) {
				_originalMr = 1;
			}
			else {
				_originalMr = 0;
			}
		}
		else if (isDragonKnight()) {
			if (originalWis >= 14) {
				_originalMr = 2;
			}
			else {
				_originalMr = 0;
			}
		}
		else if (isIllusionist()) {
			if ((originalWis >= 15) && (originalWis <= 17)) {
				_originalMr = 2;
			}
			else if (originalWis == 18) {
				_originalMr = 4;
			}
			else {
				_originalMr = 0;
			}
		}

		addMr(_originalMr);
	}

	public void resetOriginalMagicHit() {
		int originalInt = getOriginalInt();
		if (isCrown()) {
			if ((originalInt == 12) || (originalInt == 13)) {
				_originalMagicHit = 1;
			}
			else if (originalInt >= 14) {
				_originalMagicHit = 2;
			}
			else {
				_originalMagicHit = 0;
			}
		}
		else if (isKnight()) {
			if ((originalInt == 10) || (originalInt == 11)) {
				_originalMagicHit = 1;
			}
			else if (originalInt == 12) {
				_originalMagicHit = 2;
			}
			else {
				_originalMagicHit = 0;
			}
		}
		else if (isElf()) {
			if ((originalInt == 13) || (originalInt == 14)) {
				_originalMagicHit = 1;
			}
			else if (originalInt >= 15) {
				_originalMagicHit = 2;
			}
			else {
				_originalMagicHit = 0;
			}
		}
		else if (isDarkelf()) {
			if ((originalInt == 12) || (originalInt == 13)) {
				_originalMagicHit = 1;
			}
			else if (originalInt >= 14) {
				_originalMagicHit = 2;
			}
			else {
				_originalMagicHit = 0;
			}
		}
		else if (isWizard()) {
			if (originalInt >= 14) {
				_originalMagicHit = 1;
			}
			else {
				_originalMagicHit = 0;
			}
		}
		else if (isDragonKnight()) {
			if ((originalInt == 12) || (originalInt == 13)) {
				_originalMagicHit = 2;
			}
			else if ((originalInt == 14) || (originalInt == 15)) {
				_originalMagicHit = 3;
			}
			else if (originalInt >= 16) {
				_originalMagicHit = 4;
			}
			else {
				_originalMagicHit = 0;
			}
		}
		else if (isIllusionist()) {
			if (originalInt >= 13) {
				_originalMagicHit = 1;
			}
			else {
				_originalMagicHit = 0;
			}
		}
	}

	public void resetOriginalMagicCritical() {
		int originalInt = getOriginalInt();
		if (isCrown()) {
			_originalMagicCritical = 0;
		}
		else if (isKnight()) {
			_originalMagicCritical = 0;
		}
		else if (isElf()) {
			if ((originalInt == 14) || (originalInt == 15)) {
				_originalMagicCritical = 2;
			}
			else if (originalInt >= 16) {
				_originalMagicCritical = 4;
			}
			else {
				_originalMagicCritical = 0;
			}
		}
		else if (isDarkelf()) {
			_originalMagicCritical = 0;
		}
		else if (isWizard()) {
			if (originalInt == 15) {
				_originalMagicCritical = 2;
			}
			else if (originalInt == 16) {
				_originalMagicCritical = 4;
			}
			else if (originalInt == 17) {
				_originalMagicCritical = 6;
			}
			else if (originalInt == 18) {
				_originalMagicCritical = 8;
			}
			else {
				_originalMagicCritical = 0;
			}
		}
		else if (isDragonKnight()) {
			_originalMagicCritical = 0;
		}
		else if (isIllusionist()) {
			_originalMagicCritical = 0;
		}
	}

	/**
	 * 重置原始 MP 消耗減少值
	 * <p>
	 * 根據職業和原始智力值計算並設定 MP 消耗減少加成。
	 * 不同職業有不同的智力門檻和加成值。
	 * </p>
	 */
	public void resetOriginalMagicConsumeReduction() {
		int originalInt = getOriginalInt();
		if (isCrown()) {
			if ((originalInt == 11) || (originalInt == 12)) {
				_originalMagicConsumeReduction = 1;
			}
			else if (originalInt >= 13) {
				_originalMagicConsumeReduction = 2;
			}
			else {
				_originalMagicConsumeReduction = 0;
			}
		}
		else if (isKnight()) {
			if ((originalInt == 9) || (originalInt == 10)) {
				_originalMagicConsumeReduction = 1;
			}
			else if (originalInt >= 11) {
				_originalMagicConsumeReduction = 2;
			}
			else {
				_originalMagicConsumeReduction = 0;
			}
		}
		else if (isElf()) {
			_originalMagicConsumeReduction = 0;
		}
		else if (isDarkelf()) {
			if ((originalInt == 13) || (originalInt == 14)) {
				_originalMagicConsumeReduction = 1;
			}
			else if (originalInt >= 15) {
				_originalMagicConsumeReduction = 2;
			}
			else {
				_originalMagicConsumeReduction = 0;
			}
		}
		else if (isWizard()) {
			_originalMagicConsumeReduction = 0;
		}
		else if (isDragonKnight()) {
			_originalMagicConsumeReduction = 0;
		}
		else if (isIllusionist()) {
			if (originalInt == 14) {
				_originalMagicConsumeReduction = 1;
			}
			else if (originalInt >= 15) {
				_originalMagicConsumeReduction = 2;
			}
			else {
				_originalMagicConsumeReduction = 0;
			}
		}
	}

	/**
	 * 重置原始魔法傷害加成
	 * <p>
	 * 根據職業和原始智力值計算並設定魔法傷害加成。
	 * 主要適用於法師和龍騎士職業。
	 * </p>
	 */
	public void resetOriginalMagicDamage() {
		int originalInt = getOriginalInt();
		if (isCrown()) {
			_originalMagicDamage = 0;
		}
		else if (isKnight()) {
			_originalMagicDamage = 0;
		}
		else if (isElf()) {
			_originalMagicDamage = 0;
		}
		else if (isDarkelf()) {
			_originalMagicDamage = 0;
		}
		else if (isWizard()) {
			if (originalInt >= 13) {
				_originalMagicDamage = 1;
			}
			else {
				_originalMagicDamage = 0;
			}
		}
		else if (isDragonKnight()) {
			if ((originalInt == 13) || (originalInt == 14)) {
				_originalMagicDamage = 1;
			}
			else if ((originalInt == 15) || (originalInt == 16)) {
				_originalMagicDamage = 2;
			}
			else if (originalInt == 17) {
				_originalMagicDamage = 3;
			}
			else {
				_originalMagicDamage = 0;
			}
		}
		else if (isIllusionist()) {
			if (originalInt == 16) {
				_originalMagicDamage = 1;
			}
			else if (originalInt == 17) {
				_originalMagicDamage = 2;
			}
			else {
				_originalMagicDamage = 0;
			}
		}
	}

	public void resetOriginalAc() {
		int originalDex = getOriginalDex();
		if (isCrown()) {
			if ((originalDex >= 12) && (originalDex <= 14)) {
				_originalAc = 1;
			}
			else if ((originalDex == 15) || (originalDex == 16)) {
				_originalAc = 2;
			}
			else if (originalDex >= 17) {
				_originalAc = 3;
			}
			else {
				_originalAc = 0;
			}
		}
		else if (isKnight()) {
			if ((originalDex == 13) || (originalDex == 14)) {
				_originalAc = 1;
			}
			else if (originalDex >= 15) {
				_originalAc = 3;
			}
			else {
				_originalAc = 0;
			}
		}
		else if (isElf()) {
			if ((originalDex >= 15) && (originalDex <= 17)) {
				_originalAc = 1;
			}
			else if (originalDex == 18) {
				_originalAc = 2;
			}
			else {
				_originalAc = 0;
			}
		}
		else if (isDarkelf()) {
			if (originalDex >= 17) {
				_originalAc = 1;
			}
			else {
				_originalAc = 0;
			}
		}
		else if (isWizard()) {
			if ((originalDex == 8) || (originalDex == 9)) {
				_originalAc = 1;
			}
			else if (originalDex >= 10) {
				_originalAc = 2;
			}
			else {
				_originalAc = 0;
			}
		}
		else if (isDragonKnight()) {
			if ((originalDex == 12) || (originalDex == 13)) {
				_originalAc = 1;
			}
			else if (originalDex >= 14) {
				_originalAc = 2;
			}
			else {
				_originalAc = 0;
			}
		}
		else if (isIllusionist()) {
			if ((originalDex == 11) || (originalDex == 12)) {
				_originalAc = 1;
			}
			else if (originalDex >= 13) {
				_originalAc = 2;
			}
			else {
				_originalAc = 0;
			}
		}

		addAc(0 - _originalAc);
	}

	/**
	 * 重置原始閃避率（ER）加成
	 * <p>
	 * 根據職業和原始敏捷值計算並設定閃避率加成。
	 * 不同職業有不同的敏捷門檻和加成值。
	 * </p>
	 */
	public void resetOriginalEr() {
		int originalDex = getOriginalDex();
		if (isCrown()) {
			if ((originalDex == 14) || (originalDex == 15)) {
				_originalEr = 1;
			}
			else if ((originalDex == 16) || (originalDex == 17)) {
				_originalEr = 2;
			}
			else if (originalDex == 18) {
				_originalEr = 3;
			}
			else {
				_originalEr = 0;
			}
		}
		else if (isKnight()) {
			if ((originalDex == 14) || (originalDex == 15)) {
				_originalEr = 1;
			}
			else if (originalDex == 16) {
				_originalEr = 3;
			}
			else {
				_originalEr = 0;
			}
		}
		else if (isElf()) {
			_originalEr = 0;
		}
		else if (isDarkelf()) {
			if (originalDex >= 16) {
				_originalEr = 2;
			}
			else {
				_originalEr = 0;
			}
		}
		else if (isWizard()) {
			if ((originalDex == 9) || (originalDex == 10)) {
				_originalEr = 1;
			}
			else if (originalDex == 11) {
				_originalEr = 2;
			}
			else {
				_originalEr = 0;
			}
		}
		else if (isDragonKnight()) {
			if ((originalDex == 13) || (originalDex == 14)) {
				_originalEr = 1;
			}
			else if (originalDex >= 15) {
				_originalEr = 2;
			}
			else {
				_originalEr = 0;
			}
		}
		else if (isIllusionist()) {
			if ((originalDex == 12) || (originalDex == 13)) {
				_originalEr = 1;
			}
			else if (originalDex >= 14) {
				_originalEr = 2;
			}
			else {
				_originalEr = 0;
			}
		}
	}

	/**
	 * 重置原始 HP 自然恢復速度加成
	 * <p>
	 * 根據職業和原始體質值計算並設定 HP 自然恢復速度加成。
	 * 不同職業有不同的體質門檻和加成值。
	 * </p>
	 */
	public void resetOriginalHpr() {
		int originalCon = getOriginalCon();
		if (isCrown()) {
			if ((originalCon == 13) || (originalCon == 14)) {
				_originalHpr = 1;
			}
			else if ((originalCon == 15) || (originalCon == 16)) {
				_originalHpr = 2;
			}
			else if (originalCon == 17) {
				_originalHpr = 3;
			}
			else if (originalCon == 18) {
				_originalHpr = 4;
			}
			else {
				_originalHpr = 0;
			}
		}
		else if (isKnight()) {
			if ((originalCon == 16) || (originalCon == 17)) {
				_originalHpr = 2;
			}
			else if (originalCon == 18) {
				_originalHpr = 4;
			}
			else {
				_originalHpr = 0;
			}
		}
		else if (isElf()) {
			if ((originalCon == 14) || (originalCon == 15)) {
				_originalHpr = 1;
			}
			else if (originalCon == 16) {
				_originalHpr = 2;
			}
			else if (originalCon >= 17) {
				_originalHpr = 3;
			}
			else {
				_originalHpr = 0;
			}
		}
		else if (isDarkelf()) {
			if ((originalCon == 11) || (originalCon == 12)) {
				_originalHpr = 1;
			}
			else if (originalCon >= 13) {
				_originalHpr = 2;
			}
			else {
				_originalHpr = 0;
			}
		}
		else if (isWizard()) {
			if (originalCon == 17) {
				_originalHpr = 1;
			}
			else if (originalCon == 18) {
				_originalHpr = 2;
			}
			else {
				_originalHpr = 0;
			}
		}
		else if (isDragonKnight()) {
			if ((originalCon == 16) || (originalCon == 17)) {
				_originalHpr = 1;
			}
			else if (originalCon == 18) {
				_originalHpr = 3;
			}
			else {
				_originalHpr = 0;
			}
		}
		else if (isIllusionist()) {
			if ((originalCon == 14) || (originalCon == 15)) {
				_originalHpr = 1;
			}
			else if (originalCon >= 16) {
				_originalHpr = 2;
			}
			else {
				_originalHpr = 0;
			}
		}
	}

	public void resetOriginalMpr() {
		int originalWis = getOriginalWis();
		if (isCrown()) {
			if ((originalWis == 13) || (originalWis == 14)) {
				_originalMpr = 1;
			}
			else if (originalWis >= 15) {
				_originalMpr = 2;
			}
			else {
				_originalMpr = 0;
			}
		}
		else if (isKnight()) {
			if ((originalWis == 11) || (originalWis == 12)) {
				_originalMpr = 1;
			}
			else if (originalWis == 13) {
				_originalMpr = 2;
			}
			else {
				_originalMpr = 0;
			}
		}
		else if (isElf()) {
			if ((originalWis >= 15) && (originalWis <= 17)) {
				_originalMpr = 1;
			}
			else if (originalWis == 18) {
				_originalMpr = 2;
			}
			else {
				_originalMpr = 0;
			}
		}
		else if (isDarkelf()) {
			if (originalWis >= 13) {
				_originalMpr = 1;
			}
			else {
				_originalMpr = 0;
			}
		}
		else if (isWizard()) {
			if ((originalWis == 14) || (originalWis == 15)) {
				_originalMpr = 1;
			}
			else if ((originalWis == 16) || (originalWis == 17)) {
				_originalMpr = 2;
			}
			else if (originalWis == 18) {
				_originalMpr = 3;
			}
			else {
				_originalMpr = 0;
			}
		}
		else if (isDragonKnight()) {
			if ((originalWis == 15) || (originalWis == 16)) {
				_originalMpr = 1;
			}
			else if (originalWis >= 17) {
				_originalMpr = 2;
			}
			else {
				_originalMpr = 0;
			}
		}
		else if (isIllusionist()) {
			if ((originalWis >= 14) && (originalWis <= 16)) {
				_originalMpr = 1;
			}
			else if (originalWis >= 17) {
				_originalMpr = 2;
			}
			else {
				_originalMpr = 0;
			}
		}
	}

	public void refresh() {
		resetLevel();
		resetBaseHitup();
		resetBaseDmgup();
		resetBaseMr();
		resetBaseAc();
		resetOriginalHpup();
		resetOriginalMpup();
		resetOriginalDmgup();
		resetOriginalBowDmgup();
		resetOriginalHitup();
		resetOriginalBowHitup();
		resetOriginalMr();
		resetOriginalMagicHit();
		resetOriginalMagicCritical();
		resetOriginalMagicConsumeReduction();
		resetOriginalMagicDamage();
		resetOriginalAc();
		resetOriginalEr();
		resetOriginalHpr();
		resetOriginalMpr();
		resetOriginalStrWeightReduction();
		resetOriginalConWeightReduction();
	}

	public void startRefreshParty() {// 組隊更新 3.3C

		final int INTERVAL = 25000;

		if (!_rpActive) {

			_rp = new L1PartyRefresh(this);

			_regenTimer.scheduleAtFixedRate(_rp, INTERVAL, INTERVAL);

			_rpActive = true;

		}

	}

	public void stopRefreshParty() {// 組隊暫停更新 3.3C

		if (_rpActive) {
			_rp.cancel();
			_rp = null;
			_rpActive = false;

		}
	}

	private final L1ExcludingList _excludingList = new L1ExcludingList();

	public L1ExcludingList getExcludingList() {
		return _excludingList;
	}

	// -- 加速器検知機能 --
	private final AcceleratorChecker _acceleratorChecker = new AcceleratorChecker(this);

	public AcceleratorChecker getAcceleratorChecker() {
		return _acceleratorChecker;
	}

	// 使用屠宰者判斷
	private boolean _FoeSlayer = false;

	public void setFoeSlayer(boolean FoeSlayer) {
		_FoeSlayer = FoeSlayer;
	}

	public boolean isFoeSlayer() {
		return _FoeSlayer;
	}

	/**
	 * テレポート先の座標
	 */
	private int _teleportX = 0;

	public int getTeleportX() {
		return _teleportX;
	}

	public void setTeleportX(int i) {
		_teleportX = i;
	}

	private int _teleportY = 0;

	public int getTeleportY() {
		return _teleportY;
	}

	public void setTeleportY(int i) {
		_teleportY = i;
	}

	private short _teleportMapId = 0;

	public short getTeleportMapId() {
		return _teleportMapId;
	}

	public void setTeleportMapId(short i) {
		_teleportMapId = i;
	}

	private int _teleportHeading = 0;

	public int getTeleportHeading() {
		return _teleportHeading;
	}

	public void setTeleportHeading(int i) {
		_teleportHeading = i;
	}

	private int _tempCharGfxAtDead;

	public int getTempCharGfxAtDead() {
		return _tempCharGfxAtDead;
	}

	public void setTempCharGfxAtDead(int i) {
		_tempCharGfxAtDead = i;
	}

	private boolean _isCanWhisper = true;

	public boolean isCanWhisper() {
		return _isCanWhisper;
	}

	public void setCanWhisper(boolean flag) {
		_isCanWhisper = flag;
	}

	private boolean _isShowTradeChat = true;

	public boolean isShowTradeChat() {
		return _isShowTradeChat;
	}

	public void setShowTradeChat(boolean flag) {
		_isShowTradeChat = flag;
	}

	// 血盟
	private boolean _isShowClanChat = true;

	public boolean isShowClanChat() {
		return _isShowClanChat;
	}

	public void setShowClanChat(boolean flag) {
		_isShowClanChat = flag;
	}

	// 組隊
	private boolean _isShowPartyChat = true;

	public boolean isShowPartyChat() {
		return _isShowPartyChat;
	}

	public void setShowPartyChat(boolean flag) {
		_isShowPartyChat = flag;
	}

	private boolean _isShowWorldChat = true;

	public boolean isShowWorldChat() {
		return _isShowWorldChat;
	}

	public void setShowWorldChat(boolean flag) {
		_isShowWorldChat = flag;
	}

	private int _fightId;

	public int getFightId() {
		return _fightId;
	}

	public void setFightId(int i) {
		_fightId = i;
	}

	// 釣魚點
	private int _fishX;

	public int getFishX() {
		return _fishX;
	}

	public void setFishX(int i) {
		_fishX = i;
	}
	private int _fishY;

	public int getFishY() {
		return _fishY;
	}

	public void setFishY(int i) {
		_fishY = i;
	}

	private byte _chatCount = 0;

	private long _oldChatTimeInMillis = 0L;

	public void checkChatInterval() {
		long nowChatTimeInMillis = System.currentTimeMillis();
		if (_chatCount == 0) {
			_chatCount++;
			_oldChatTimeInMillis = nowChatTimeInMillis;
			return;
		}

		long chatInterval = nowChatTimeInMillis - _oldChatTimeInMillis;
		if (chatInterval > 2000) {
			_chatCount = 0;
			_oldChatTimeInMillis = 0;
		}
		else {
			if (_chatCount >= 3) {
				setSkillEffect(STATUS_CHAT_PROHIBITED, 120 * 1000);
				sendPackets(new S_SkillIconGFX(36, 120));
				sendPackets(new S_ServerMessage(153)); // \f3迷惑なチャット流しをしたので、今後2分間チャットを行うことはできません。
				_chatCount = 0;
				_oldChatTimeInMillis = 0;
			}
			_chatCount++;
		}
	}

	private int _callClanId;

	public int getCallClanId() {
		return _callClanId;
	}

	public void setCallClanId(int i) {
		_callClanId = i;
	}

	private int _callClanHeading;

	public int getCallClanHeading() {
		return _callClanHeading;
	}

	public void setCallClanHeading(int i) {
		_callClanHeading = i;
	}

	private boolean _isInCharReset = false;

	public boolean isInCharReset() {
		return _isInCharReset;
	}

	public void setInCharReset(boolean flag) {
		_isInCharReset = flag;
	}

	private int _tempLevel = 1;

	public int getTempLevel() {
		return _tempLevel;
	}

	public void setTempLevel(int i) {
		_tempLevel = i;
	}

	private int _tempMaxLevel = 1;

	public int getTempMaxLevel() {
		return _tempMaxLevel;
	}

	public void setTempMaxLevel(int i) {
		_tempMaxLevel = i;
	}

	private int _awakeSkillId = 0;

	public int getAwakeSkillId() {
		return _awakeSkillId;
	}

	public void setAwakeSkillId(int i) {
		_awakeSkillId = i;
	}

	private boolean _isSummonMonster = false;

	public void setSummonMonster(boolean SummonMonster) {
		_isSummonMonster = SummonMonster;
	}

	public boolean isSummonMonster() {
		return _isSummonMonster;
	}
	
	private int _SummonId = 0;
	
	public void setSummonId(int SummonId) {
		_SummonId = SummonId;
	}

	public int getSummonId() {
		return _SummonId;
	}

	private boolean _isShapeChange = false;

	public void setShapeChange(boolean isShapeChange) {
		_isShapeChange = isShapeChange;
	}

	public boolean isShapeChange() {
		return _isShapeChange;
	}

	public void setPartyType(int type) {
		_partyType = type;
	}

	public int getPartyType() {
		return _partyType;
	}

	/****************************** 戰鬥特化系統 ******************************/
	// 改變戰鬥特化狀態
	public void changeFightType(int oldType, int newType) {
		// 消除既有的戰鬥特化狀態
		switch (oldType) {
			case 1:
				addAc(2);
				addMr(-3);
				sendPackets(new S_Fight(S_Fight.TYPE_JUSTICE_LEVEL1, S_Fight.FLAG_OFF));
				break;

			case 2:
				addAc(4);
				addMr(-6);
				sendPackets(new S_Fight(S_Fight.TYPE_JUSTICE_LEVEL2, S_Fight.FLAG_OFF));
				break;

			case 3:
				addAc(6);
				addMr(-9);
				sendPackets(new S_Fight(S_Fight.TYPE_JUSTICE_LEVEL3, S_Fight.FLAG_OFF));
				break;

			case -1:
				addDmgup(-1);
				addBowDmgup(-1);
				addSp(-1);
				sendPackets(new S_Fight(S_Fight.TYPE_EVIL_LEVEL1, S_Fight.FLAG_OFF));
				break;

			case -2:
				addDmgup(-3);
				addBowDmgup(-3);
				addSp(-2);
				sendPackets(new S_Fight(S_Fight.TYPE_EVIL_LEVEL2, S_Fight.FLAG_OFF));
				break;

			case -3:
				addDmgup(-5);
				addBowDmgup(-5);
				addSp(-3);
				sendPackets(new S_Fight(S_Fight.TYPE_EVIL_LEVEL3, S_Fight.FLAG_OFF));
				break;
		}

		// 增加新的戰鬥特化狀態
		switch (newType) {
			case 1:
				addAc(-2);
				addMr(3);
				sendPackets(new S_Fight(S_Fight.TYPE_JUSTICE_LEVEL1, S_Fight.FLAG_ON));
				break;

			case 2:
				addAc(-4);
				addMr(6);
				sendPackets(new S_Fight(S_Fight.TYPE_JUSTICE_LEVEL2, S_Fight.FLAG_ON));
				break;

			case 3:
				addAc(-6);
				addMr(9);
				sendPackets(new S_Fight(S_Fight.TYPE_JUSTICE_LEVEL3, S_Fight.FLAG_ON));
				break;

			case -1:
				addDmgup(1);
				addBowDmgup(1);
				addSp(1);
				sendPackets(new S_Fight(S_Fight.TYPE_EVIL_LEVEL1, S_Fight.FLAG_ON));
				break;

			case -2:
				addDmgup(3);
				addBowDmgup(3);
				addSp(2);
				sendPackets(new S_Fight(S_Fight.TYPE_EVIL_LEVEL2, S_Fight.FLAG_ON));
				break;

			case -3:
				addDmgup(5);
				addBowDmgup(5);
				addSp(3);
				sendPackets(new S_Fight(S_Fight.TYPE_EVIL_LEVEL3, S_Fight.FLAG_ON));
				break;
		}
	}

	// 確認是否屬於新手, 並設定相關狀態
	public void checkNoviceType() {
		// 判斷是否啟動新手保護系統(遭遇的守護)
		if (!Config.NOVICE_PROTECTION_IS_ACTIVE) {
			return;
		}

		// 判斷目前等級是否已超過新手階段
		if (getLevel() > Config.NOVICE_MAX_LEVEL) {
			// 判斷之前是否具有新手保護狀態
			if (hasSkillEffect(L1SkillId.STATUS_NOVICE)) {
				// 移除新手保護狀態
				removeSkillEffect(L1SkillId.STATUS_NOVICE);

				// 關閉遭遇的守護圖示
				sendPackets(new S_Fight(S_Fight.TYPE_ENCOUNTER, S_Fight.FLAG_OFF));
			}
		}
		else {
			// 判斷是否未具有新手保護狀態
			if (!hasSkillEffect(L1SkillId.STATUS_NOVICE)) {
				// 增加新手保護狀態
				setSkillEffect(L1SkillId.STATUS_NOVICE, 0);

				// 開啟遭遇的守護圖示
				sendPackets(new S_Fight(S_Fight.TYPE_ENCOUNTER, S_Fight.FLAG_ON));
			}
		}
	}
	
	/**
	 * 登入時載入所有裝備欄位
	 */
	public void setEquipments(){
		Map<Integer, Integer> items = new HashMap<Integer, Integer>();
		for (L1ItemInstance item : getInventory().getItems()) {
			if(item.isEquipped() && item.getItem().getType2() == 1){ // L1Weapon
				items.put(8, item.getId());
			} else if (item.isEquipped() && item.getItem().getType2() == 2) { // L1Armor
				if ((item.getItem().getType() == 1)) {
					items.put(1, item.getId());
				} else if ((item.getItem().getType() == 2)) {
					items.put(2, item.getId());
				} else if ((item.getItem().getType() == 3)) {
					items.put(3, item.getId());
				} else if ((item.getItem().getType() == 4)) {
					items.put(4, item.getId());
				} else if ((item.getItem().getType() == 5)) {
					items.put(6, item.getId());
				} else if ((item.getItem().getType() == 6)) {
					items.put(5, item.getId());
				} else if ((item.getItem().getType() == 7)) {
					items.put(7, item.getId());
				} else if ((item.getItem().getType() == 8)) {
					items.put(10, item.getId());
				} else if ((item.getItem().getType() == 9)) {
					items.put(18, item.getId());
				} else if ((item.getItem().getType() == 10)) {
					items.put(11, item.getId());
				} else if ((item.getItem().getType() == 11)) {
					items.put(19, item.getId());
				} else if ((item.getItem().getType() == 12)) {
					items.put(12, item.getId());
				} else if ((item.getItem().getType() == 13)) {
					items.put(7, item.getId());
				} else if ((item.getItem().getType() == 14)) {
					items.put(22, item.getId());
				} else if ((item.getItem().getType() == 15)) {
					items.put(23, item.getId());
				} else if ((item.getItem().getType() == 16)) {
					items.put(24, item.getId());
				} else if ((item.getItem().getType() == 17)) {
					items.put(25, item.getId());
				} else if ((item.getItem().getType() == 18)) {
					items.put(26, item.getId());
				} else if ((item.getItem().getType() == 19)) {
					items.put(20, item.getId());
				} else if ((item.getItem().getType() == 20)) {
					items.put(21, item.getId());
				}
			}
		}
		sendPackets(new S_EquipmentSlot(items));
	}
	
}
