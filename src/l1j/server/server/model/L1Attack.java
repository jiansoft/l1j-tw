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
package l1j.server.server.model;

import l1j.server.Config;
import l1j.server.server.ActionCodes;
import l1j.server.server.WarTimeController;
import l1j.server.server.datatables.SkillsTable;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1PetInstance;
import l1j.server.server.model.Instance.L1SummonInstance;
import l1j.server.server.model.gametime.L1GameTimeClock;
import l1j.server.server.model.npc.action.L1NpcDefaultAction;
import l1j.server.server.model.poison.L1DamagePoison;
import l1j.server.server.model.poison.L1ParalysisPoison;
import l1j.server.server.model.poison.L1SilencePoison;
import l1j.server.server.serverpackets.S_AttackMissPacket;
import l1j.server.server.serverpackets.S_AttackPacket;
import l1j.server.server.serverpackets.S_DoActionGFX;
import l1j.server.server.serverpackets.S_EffectLocation;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_SkillIconGFX;
import l1j.server.server.serverpackets.S_UseArrowSkill;
import l1j.server.server.serverpackets.S_UseAttackSkill;
import l1j.server.server.templates.L1MagicDoll;
import l1j.server.server.templates.L1Skills;
import l1j.server.server.types.Point;
import l1j.server.server.utils.Random;

import static l1j.server.server.model.skill.L1SkillId.*;

/**
 * 天堂I戰鬥攻擊計算系統核心類別
 *
 * <p>此類別負責處理天堂I遊戲中所有類型的戰鬥攻擊計算，包括命中率判定、傷害計算、
 * 武器系統整合、技能效果、屬性傷害、狀態效果等複雜的戰鬥機制。</p>
 *
 * <h2>主要功能</h2>
 * <ul>
 *   <li><b>命中率計算：</b>根據攻擊者和目標的屬性、裝備、技能效果計算攻擊命中機率</li>
 *   <li><b>傷害計算：</b>計算物理傷害、屬性傷害、額外效果傷害等</li>
 *   <li><b>武器系統：</b>支援近戰武器、遠程武器（弓箭、鐵手甲）、特殊武器</li>
 *   <li><b>技能整合：</b>支援各種戰鬥技能的效果計算</li>
 *   <li><b>特殊機制：</b>暴擊、雙倍傷害、屬性附魔、狀態效果等</li>
 * </ul>
 *
 * <h2>攻擊類型</h2>
 * 系統支援四種基本的攻擊計算類型：
 * <ul>
 *   <li><b>PC vs PC (PC_PC)：</b>玩家對玩家的PvP戰鬥</li>
 *   <li><b>PC vs NPC (PC_NPC)：</b>玩家對怪物的PvE戰鬥</li>
 *   <li><b>NPC vs PC (NPC_PC)：</b>怪物對玩家的攻擊</li>
 *   <li><b>NPC vs NPC (NPC_NPC)：</b>怪物之間的戰鬥（如寵物、召喚獸）</li>
 * </ul>
 *
 * <h2>武器系統</h2>
 * <ul>
 *   <li><b>近戰武器：</b>使用力量(STR)作為主要傷害屬性，受武器耐久度影響</li>
 *   <li><b>弓箭 (Type 20)：</b>使用敏捷(DEX)作為主要傷害屬性，需要消耗箭矢</li>
 *   <li><b>鐵手甲 (Type 62)：</b>使用敏捷(DEX)作為主要傷害屬性，需要消耗飛刀</li>
 *   <li><b>武器強化：</b>支援武器強化等級、祝福狀態、屬性附魔等</li>
 * </ul>
 *
 * <h2>命中率計算公式</h2>
 * <p>玩家對玩家的命中率計算：</p>
 * <pre>
 * 基礎命中 = (等級 + 職業補正 + STR補正 + DEX補正 + 武器補正 + 強化值/2 + 魔法補正) × 0.68 - 10
 * 最終命中率受目標AC、閃避率影響，範圍在5%~95%之間
 * </pre>
 *
 * <h2>傷害計算機制</h2>
 * <ul>
 *   <li><b>基礎傷害：</b>武器傷害 + 屬性補正（STR或DEX）+ 強化值傷害</li>
 *   <li><b>額外傷害：</b>技能傷害、屬性附魔傷害、種族特效傷害</li>
 *   <li><b>傷害減免：</b>目標防禦力、傷害減免、屬性抗性</li>
 *   <li><b>特殊效果：</b>暴擊、雙倍傷害、吸血、吸魔等</li>
 * </ul>
 *
 * <h2>屬性系統</h2>
 * 支援武器屬性附魔系統，包括：
 * <ul>
 *   <li>火屬性傷害（對不死系額外傷害）</li>
 *   <li>水屬性傷害</li>
 *   <li>風屬性傷害</li>
 *   <li>地屬性傷害</li>
 * </ul>
 *
 * <h2>狀態效果</h2>
 * 攻擊可能觸發以下狀態效果：
 * <ul>
 *   <li>中毒（傷害中毒、麻痺中毒、沉默中毒）</li>
 *   <li>吸血（HP吸取）</li>
 *   <li>吸魔（MP吸取）</li>
 *   <li>暈眩、麻痺、石化等控制效果</li>
 * </ul>
 *
 * <h2>無敵機制</h2>
 * 以下技能效果會使目標進入無敵狀態，不受任何傷害：
 * <ul>
 *   <li>絕對屏障 (ABSOLUTE_BARRIER)</li>
 *   <li>冰矛術 (ICE_LANCE)</li>
 *   <li>暴風雪 (FREEZING_BLIZZARD)</li>
 *   <li>寒冰吐息 (FREEZING_BREATH)</li>
 *   <li>大地束縛 (EARTH_BIND)</li>
 * </ul>
 *
 * <h2>使用範例</h2>
 * <pre>
 * // 建立攻擊實例
 * L1Attack attack = new L1Attack(attacker, target);
 *
 * // 計算命中
 * if (attack.calcHit()) {
 *     // 計算傷害
 *     attack.calcDamage();
 *
 *     // 執行攻擊動作
 *     attack.action();
 *
 *     // 提交傷害
 *     attack.commit();
 * }
 *
 * // 使用技能攻擊
 * L1Attack skillAttack = new L1Attack(attacker, target, skillId);
 * skillAttack.calcHit();
 * skillAttack.calcDamage();
 * skillAttack.action();
 * skillAttack.commit();
 * </pre>
 *
 * <h2>重要注意事項</h2>
 * <ul>
 *   <li>所有攻擊計算都會考慮遊戲配置檔中的倍率設定（rates.properties）</li>
 *   <li>武器射程計算會考慮障礙物阻擋（使用glanceCheck）</li>
 *   <li>遠程武器會消耗彈藥（箭矢或飛刀）</li>
 *   <li>武器耐久度會在攻擊後降低（近戰武器）</li>
 *   <li>PvP和PvE的傷害計算公式有所不同</li>
 * </ul>
 *
 * @author L1JTW 99nets
 * @version 3.80c
 * @see L1Character 遊戲角色基礎類別
 * @see L1PcInstance 玩家角色類別
 * @see L1NpcInstance NPC/怪物類別
 * @see L1ItemInstance 物品實例類別
 * @see L1Skills 技能模板類別
 * @see ActionCodes 動作代碼定義
 */
public class L1Attack {
	/* ========== 攻擊者與目標資訊 ========== */

	/** 攻擊者（當攻擊者為玩家時） */
	private L1PcInstance _pc = null;

	/** 攻擊目標（基礎角色類型） */
	private L1Character _target = null;

	/** 攻擊目標（當目標為玩家時） */
	private L1PcInstance _targetPc = null;

	/** 攻擊者（當攻擊者為NPC/怪物時） */
	private L1NpcInstance _npc = null;

	/** 攻擊目標（當目標為NPC/怪物時） */
	private L1NpcInstance _targetNpc = null;

	/** 目標的唯一識別ID */
	private final int _targetId;

	/** 目標的X座標（用於距離檢查） */
	private int _targetX;

	/** 目標的Y座標（用於距離檢查） */
	private int _targetY;

	/* ========== 攻擊計算相關 ========== */

	/** 屬性補正傷害（STR或DEX的傷害加成） */
	private int _statusDamage = 0;

	/** 命中率數值 */
	private int _hitRate = 0;

	/**
	 * 攻擊計算類型
	 * @see #PC_PC 玩家對玩家
	 * @see #PC_NPC 玩家對NPC
	 * @see #NPC_PC NPC對玩家
	 * @see #NPC_NPC NPC對NPC
	 */
	private int _calcType;

	/** 攻擊計算類型：玩家 vs 玩家 */
	private static final int PC_PC = 1;

	/** 攻擊計算類型：玩家 vs NPC */
	private static final int PC_NPC = 2;

	/** 攻擊計算類型：NPC vs 玩家 */
	private static final int NPC_PC = 3;

	/** 攻擊計算類型：NPC vs NPC */
	private static final int NPC_NPC = 4;

	/** 是否命中目標 */
	private boolean _isHit = false;

	/** 最終傷害值 */
	private int _damage = 0;

	/** 吸取魔力值 */
	private int _drainMana = 0;

	/** 吸取生命值 */
	private int _drainHp = 0;

	/** 特效ID */
	private byte _effectId = 0;

	/** 攻擊圖形ID（用於客戶端顯示） */
	private int _attckGrfxId = 0;

	/** 攻擊動作ID（用於客戶端顯示） */
	private int _attckActId = 0;

	/* ========== 武器資訊（當攻擊者為玩家時） ========== */

	/** 攻擊者裝備的武器實例 */
	private L1ItemInstance weapon = null;

	/** 武器物品ID */
	private int _weaponId = 0;

	/** 武器類型1（大分類） */
	private int _weaponType = 0;

	/** 武器類型2（細分類） */
	private int _weaponType2 = 0;

	/** 武器命中加成 */
	private int _weaponAddHit = 0;

	/** 武器傷害加成 */
	private int _weaponAddDmg = 0;

	/** 武器對小型目標的傷害 */
	private int _weaponSmall = 0;

	/** 武器對大型目標的傷害 */
	private int _weaponLarge = 0;

	/** 武器射程（1=近戰） */
	private int _weaponRange = 1;

	/** 武器祝福狀態（0=詛咒, 1=未祝福, 2=祝福） */
	private int _weaponBless = 1;

	/** 武器強化等級（+0 ~ +9以上） */
	private int _weaponEnchant = 0;

	/** 武器材質類型 */
	private int _weaponMaterial = 0;

	/** 武器雙倍傷害機率 */
	private int _weaponDoubleDmgChance = 0;

	/** 武器屬性附魔種類（火/水/風/地） */
	private int _weaponAttrEnchantKind = 0;

	/** 武器屬性附魔等級 */
	private int _weaponAttrEnchantLevel = 0;

	/** 箭矢物品實例（弓箭類武器使用） */
	private L1ItemInstance _arrow = null;

	/** 飛刀物品實例（鐵手甲類武器使用） */
	private L1ItemInstance _sting = null;

	/**
	 * 槓桿倍率（以1/10表示，預設10表示1.0倍）
	 * 用於調整最終傷害的倍數計算
	 */
	private int _leverage = 10;

	/** 使用的技能ID（0表示普通攻擊） */
	private int _skillId;

	/** 技能基礎傷害值 */
	@SuppressWarnings("unused")
	private double _skillDamage = 0;

	/**
	 * 設定槓桿倍率
	 *
	 * @param i 槓桿倍率值（以1/10表示，例如10表示1.0倍，20表示2.0倍）
	 */
	public void setLeverage(int i) {
		_leverage = i;
	}

	/**
	 * 取得槓桿倍率
	 *
	 * @return 槓桿倍率值（以1/10表示）
	 */
	private int getLeverage() {
		return _leverage;
	}

	/* ========== 屬性值對命中率與傷害的補正表 ========== */

	/**
	 * 力量(STR)對命中率的補正表
	 * <p>索引為力量值-1（例如STR 8使用索引7），返回對應的命中率加成</p>
	 * <ul>
	 *   <li>STR 1-7: -2 命中</li>
	 *   <li>STR 8-26: -2 ~ +6 命中（逐漸遞增）</li>
	 *   <li>STR 27-44: +7 ~ +12 命中（每3點+1）</li>
	 *   <li>STR 45-59: +13 ~ +17 命中（每3點+1）</li>
	 * </ul>
	 */
	private static final int[] strHit = { -2, -2, -2, -2, -2, -2, -2, // 1～7まで
			-2, -1, -1, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 5, 6, 6, 6, // 8～26まで
			7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10, 10, 11, 11, 11, 12, 12, 12, // 27～44まで
			13, 13, 13, 14, 14, 14, 15, 15, 15, 16, 16, 16, 17, 17, 17 }; // 45～59まで

	/**
	 * 敏捷(DEX)對命中率的補正表
	 * <p>索引為敏捷值-1（例如DEX 10使用索引9），返回對應的命中率加成</p>
	 * <ul>
	 *   <li>DEX 1-6: -2 命中</li>
	 *   <li>DEX 7-10: -1 ~ 0 命中</li>
	 *   <li>DEX 11-30: +1 ~ +16 命中（遞增較快）</li>
	 *   <li>DEX 31-45: +17 ~ +23 命中（每3點+1）</li>
	 *   <li>DEX 46-60: +23 ~ +28 命中（每3點+1）</li>
	 * </ul>
	 */
	private static final int[] dexHit = { -2, -2, -2, -2, -2, -2, -1, -1, 0, 0, // 1～10まで
			1, 1, 2, 2, 3, 3, 4, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, // 11～30まで
			17, 18, 19, 19, 19, 20, 20, 20, 21, 21, 21, 22, 22, 22, 23, // 31～45まで
			23, 23, 24, 24, 24, 25, 25, 25, 26, 26, 26, 27, 27, 27, 28 }; // 46～60まで

	/**
	 * 力量(STR)對傷害的補正表
	 * <p>支援STR 0-127的傷害補正計算，使用靜態初始化區塊動態生成數值</p>
	 * <ul>
	 *   <li>STR 0-22: -6 ~ +5 傷害（每2點+1）</li>
	 *   <li>STR 23-28: +5 ~ +7 傷害（每3點+1）</li>
	 *   <li>STR 29-32: +7 ~ +9 傷害（每2點+1）</li>
	 *   <li>STR 33-34: +9 ~ +11 傷害（每1點+1）</li>
	 *   <li>STR 35-127: +11 ~ +34 傷害（每4點+1）</li>
	 * </ul>
	 */
	private static final int[] strDmg = new int[128];

	static {
		// STR傷害補正初始化
		int dmg = -6;
		for (int str = 0; str <= 22; str++) { // 0～22每2點+1
			if (str % 2 == 1) {
				dmg++;
			}
			strDmg[str] = dmg;
		}
		for (int str = 23; str <= 28; str++) { // 23～28每3點+1
			if (str % 3 == 2) {
				dmg++;
			}
			strDmg[str] = dmg;
		}
		for (int str = 29; str <= 32; str++) { // 29～32每2點+1
			if (str % 2 == 1) {
				dmg++;
			}
			strDmg[str] = dmg;
		}
		for (int str = 33; str <= 34; str++) { // 33～34每1點+1
			dmg++;
			strDmg[str] = dmg;
		}
		for (int str = 35; str <= 127; str++) { // 35～127每4點+1
			if (str % 4 == 1) {
				dmg++;
			}
			strDmg[str] = dmg;
		}
	}

	/**
	 * 敏捷(DEX)對傷害的補正表
	 * <p>支援DEX 0-127的傷害補正計算，使用靜態初始化區塊動態生成數值</p>
	 * <ul>
	 *   <li>DEX 0-14: 0 傷害（無加成）</li>
	 *   <li>DEX 15-18: +1 ~ +4 傷害</li>
	 *   <li>DEX 19-23: +4 ~ +5 傷害</li>
	 *   <li>DEX 24-35: +5 ~ +9 傷害（每3點+1）</li>
	 *   <li>DEX 36-127: +9 ~ +32 傷害（每4點+1）</li>
	 * </ul>
	 */
	private static final int[] dexDmg = new int[128];

	static {
		// DEX傷害補正初始化
		for (int dex = 0; dex <= 14; dex++) {
			// 0～14無加成
			dexDmg[dex] = 0;
		}
		dexDmg[15] = 1;
		dexDmg[16] = 2;
		dexDmg[17] = 3;
		dexDmg[18] = 4;
		dexDmg[19] = 4;
		dexDmg[20] = 4;
		dexDmg[21] = 5;
		dexDmg[22] = 5;
		dexDmg[23] = 5;
		int dmg = 5;
		for (int dex = 24; dex <= 35; dex++) { // 24～35每3點+1
			if (dex % 3 == 1) {
				dmg++;
			}
			dexDmg[dex] = dmg;
		}
		for (int dex = 36; dex <= 127; dex++) { // 36～127每4點+1
			if (dex % 4 == 1) {
				dmg++;
			}
			dexDmg[dex] = dmg;
		}
	}

	/**
	 * 設定攻擊動作ID
	 *
	 * @param actId 動作ID（用於客戶端顯示攻擊動作）
	 */
	public void setActId(int actId) {
		_attckActId = actId;
	}

	/**
	 * 設定攻擊圖形ID
	 *
	 * @param gfxId 圖形ID（用於客戶端顯示攻擊特效）
	 */
	public void setGfxId(int gfxId) {
		_attckGrfxId = gfxId;
	}

	/**
	 * 取得攻擊動作ID
	 *
	 * @return 動作ID
	 */
	public int getActId() {
		return _attckActId;
	}

	/**
	 * 取得攻擊圖形ID
	 *
	 * @return 圖形ID
	 */
	public int getGfxId() {
		return _attckGrfxId;
	}

	/**
	 * 建立普通攻擊實例
	 * <p>此建構子用於一般的物理攻擊，不包含技能效果</p>
	 *
	 * @param attacker 攻擊者（可以是玩家或NPC）
	 * @param target 攻擊目標（可以是玩家或NPC）
	 * @see #L1Attack(L1Character, L1Character, int) 技能攻擊建構子
	 */
	public L1Attack(L1Character attacker, L1Character target) {
		this(attacker, target, 0);
	}

	/**
	 * 建立攻擊實例（支援技能攻擊）
	 * <p>此建構子會根據攻擊者和目標的類型，自動初始化所有相關的攻擊參數，包括：</p>
	 * <ul>
	 *   <li>判定攻擊計算類型（PC_PC、PC_NPC、NPC_PC、NPC_NPC）</li>
	 *   <li>讀取武器資訊（當攻擊者為玩家時）</li>
	 *   <li>載入技能資料（當指定技能ID時）</li>
	 *   <li>設定彈藥（弓箭或飛刀）</li>
	 *   <li>計算屬性補正（STR或DEX）</li>
	 * </ul>
	 *
	 * <h3>武器類型處理</h3>
	 * <ul>
	 *   <li><b>Type 20（弓箭）：</b>使用DEX傷害補正，需要箭矢</li>
	 *   <li><b>Type 62（鐵手甲）：</b>使用DEX傷害補正，需要飛刀</li>
	 *   <li><b>其他近戰武器：</b>使用STR傷害補正，會計算耐久度損耗</li>
	 * </ul>
	 *
	 * @param attacker 攻擊者（L1PcInstance或L1NpcInstance）
	 * @param target 攻擊目標（L1PcInstance或L1NpcInstance）
	 * @param skillId 技能ID（0表示普通攻擊，非0表示使用技能攻擊）
	 * @see L1PcInstance 玩家角色類別
	 * @see L1NpcInstance NPC/怪物類別
	 * @see SkillsTable#getTemplate(int) 取得技能模板
	 */
	public L1Attack(L1Character attacker, L1Character target, int skillId) {
		_skillId = skillId;
		if (_skillId != 0) {
			L1Skills skills = SkillsTable.getInstance().getTemplate(_skillId);
			_skillDamage = skills.getDamageValue();
		}
		if (attacker instanceof L1PcInstance) {
			_pc = (L1PcInstance) attacker;
			if (target instanceof L1PcInstance) {
				_targetPc = (L1PcInstance) target;
				_calcType = PC_PC;
			} else if (target instanceof L1NpcInstance) {
				_targetNpc = (L1NpcInstance) target;
				_calcType = PC_NPC;
			}
			// 武器情報の取得
			weapon = _pc.getWeapon();
			if (weapon != null) {
				_weaponId = weapon.getItem().getItemId();
				_weaponType = weapon.getItem().getType1();
				_weaponType2 = weapon.getItem().getType();
				_weaponAddHit = weapon.getItem().getHitModifier()
						+ weapon.getHitByMagic();
				_weaponAddDmg = weapon.getItem().getDmgModifier()
						+ weapon.getDmgByMagic();
				_weaponSmall = weapon.getItem().getDmgSmall();
				_weaponLarge = weapon.getItem().getDmgLarge();
				_weaponRange = weapon.getItem().getRange();
				_weaponBless = weapon.getItem().getBless();
				_weaponEnchant = weapon.getEnchantLevel();
				_weaponMaterial = weapon.getItem().getMaterial();
				_statusDamage = dexDmg[_pc.getDex()]; // 傷害預設用敏捷補正

				if (_weaponType == 20) { // 弓箭
					_arrow = _pc.getInventory().getArrow();
					if (_arrow != null) {
						_weaponBless = _arrow.getItem().getBless();
						_weaponMaterial = _arrow.getItem().getMaterial();
					}
				} else if (_weaponType == 62) { // 鐵手甲
					_sting = _pc.getInventory().getSting();
					if (_sting != null) {
						_weaponBless = _sting.getItem().getBless();
						_weaponMaterial = _sting.getItem().getMaterial();
					}
				} else { // 近戰類武器
					_weaponEnchant = weapon.getEnchantLevel()
							- weapon.get_durability(); // 計算武器損傷
					_statusDamage = strDmg[_pc.getStr()]; // 傷害用力量補正
				}
				_weaponDoubleDmgChance = weapon.getItem().getDoubleDmgChance();
				_weaponAttrEnchantKind = weapon.getAttrEnchantKind();
				_weaponAttrEnchantLevel = weapon.getAttrEnchantLevel();
			}
		} else if (attacker instanceof L1NpcInstance) {
			_npc = (L1NpcInstance) attacker;
			if (target instanceof L1PcInstance) {
				_targetPc = (L1PcInstance) target;
				_calcType = NPC_PC;
			} else if (target instanceof L1NpcInstance) {
				_targetNpc = (L1NpcInstance) target;
				_calcType = NPC_NPC;
			}
		}
		_target = target;
		_targetId = target.getId();
		_targetX = target.getX();
		_targetY = target.getY();
	}

	/* ■■■■■■■■■■■■■■■■ 命中判定 ■■■■■■■■■■■■■■■■ */

	/**
	 * 無敵狀態技能列表
	 * <p>擁有這些技能效果的角色將不會受到任何攻擊傷害</p>
	 */
	private static final int[] INVINCIBLE = { ABSOLUTE_BARRIER, ICE_LANCE,
			FREEZING_BLIZZARD, FREEZING_BREATH, EARTH_BIND,
			ICE_LANCE_COCKATRICE, ICE_LANCE_BASILISK };

	/**
	 * 計算攻擊是否命中
	 * <p>此方法是命中判定的主入口，會根據攻擊類型調用對應的命中計算方法</p>
	 *
	 * <h3>命中判定流程</h3>
	 * <ol>
	 *   <li>檢查目標是否處於無敵狀態（絕對屏障、冰矛術等）</li>
	 *   <li>檢查武器射程是否足夠</li>
	 *   <li>檢查遠程武器是否有彈藥（箭矢或飛刀）</li>
	 *   <li>檢查攻擊路徑是否有障礙物阻擋</li>
	 *   <li>檢查特殊武器限制（試練之劍無法攻擊）</li>
	 *   <li>根據攻擊類型計算實際命中率</li>
	 * </ol>
	 *
	 * <h3>特殊情況處理</h3>
	 * <ul>
	 *   <li>武器ID 190（沙哈之弓）不需要箭矢</li>
	 *   <li>武器ID 247-249（試練之劍）無法進行攻擊</li>
	 *   <li>射程-1表示全畫面攻擊</li>
	 *   <li>對大型怪物射程+1補正</li>
	 * </ul>
	 *
	 * @return true表示命中，false表示未命中
	 * @see #calcPcPcHit() 玩家對玩家命中計算
	 * @see #calcPcNpcHit() 玩家對NPC命中計算
	 * @see #calcNpcPcHit() NPC對玩家命中計算
	 * @see #calcNpcNpcHit() NPC對NPC命中計算
	 */
	public boolean calcHit() {
		// 檢查無敵狀態
		for (int skillId : INVINCIBLE) {
			if (_target.hasSkillEffect(skillId)) {
				_isHit = false;
				return _isHit;
			}
		}

		if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
			if (_weaponRange != -1) {
				if (_pc.getLocation()
						.getTileLineDistance(_target.getLocation()) > _weaponRange + 1) { // BIGのモンスターに対応するため射程範囲+1
					_isHit = false; // 射程範囲外
					return _isHit;
				}
			} else {
				if (!_pc.getLocation().isInScreen(_target.getLocation())) {
					_isHit = false; // 射程範囲外
					return _isHit;
				}
			}
			if ((_weaponType == 20) && (_weaponId != 190) && (_arrow == null)) {
				_isHit = false; // 沒有箭
			} else if ((_weaponType == 62) && (_sting == null)) {
				_isHit = false; // 沒有飛刀
			} else if ( _weaponRange != 1 && !_pc.glanceCheck(_targetX, _targetY)) {
				_isHit = false; // 兩格以上武器 直線距離上有障礙物
			} else if ((_weaponId == 247) || (_weaponId == 248)
					|| (_weaponId == 249)) {
				_isHit = false; // 試練の剣B～C 攻撃無効
			} else if (_calcType == PC_PC) {
				_isHit = calcPcPcHit();
			} else if (_calcType == PC_NPC) {
				_isHit = calcPcNpcHit();
			}
		} else if (_calcType == NPC_PC) {
			_isHit = calcNpcPcHit();
		} else if (_calcType == NPC_NPC) {
			_isHit = calcNpcNpcHit();
		}
		return _isHit;
	}

	/**
	 * 計算近戰武器的命中率加成
	 * <p>包含裝備加成、魔法加成、料理效果等</p>
	 *
	 * @param hitRate 基礎命中率
	 * @return 加上近戰加成後的命中率
	 */
	private int calShortRageHit(int hitRate) {
		int shortHit = hitRate + _pc.getHitup() + _pc.getOriginalHitup();
		// 防具增加命中
		shortHit += _pc.getHitModifierByArmor();

		if (_pc.hasSkillEffect(COOKING_2_0_N) // 料理追加命中
				|| _pc.hasSkillEffect(COOKING_2_0_S))
			shortHit += 1;
		if (_pc.hasSkillEffect(COOKING_3_2_N) // 料理追加命中
				|| _pc.hasSkillEffect(COOKING_3_2_S))
			shortHit += 2;
		return shortHit;
	}

	/**
	 * 計算遠程武器的命中率加成
	 * <p>包含弓箭/鐵手甲裝備加成、魔法加成、料理效果等</p>
	 *
	 * @param hitRate 基礎命中率
	 * @return 加上遠程加成後的命中率
	 */
	private int calLongRageHit(int hitRate) {
		int longHit = hitRate + _pc.getBowHitup() + _pc.getOriginalBowHitup();
		// 防具增加命中
		longHit += _pc.getBowHitModifierByArmor();

		if (_pc.hasSkillEffect(COOKING_2_3_N) // 料理追加命中
				|| _pc.hasSkillEffect(COOKING_2_3_S)
				|| _pc.hasSkillEffect(COOKING_3_0_N)
				|| _pc.hasSkillEffect(COOKING_3_0_S))
			longHit += 1;
		return longHit;
	}

	/**
	 * 計算玩家對玩家的命中判定
	 * <p>PvP命中計算使用D20骰子系統，結合攻擊者命中值與防禦者AC進行判定</p>
	 *
	 * <h3>命中率計算公式</h3>
	 * <pre>
	 * 基礎命中 = 等級 + STR補正 + DEX補正 + 武器命中 + 強化值/2 + 裝備加成 + 魔法加成
	 * 攻擊骰 = 1d20 + 基礎命中 - 10 - 目標閃避 + 目標負閃避
	 * 防禦骰 = 10 - 目標AC（AC為負時加入隨機值）
	 * </pre>
	 *
	 * <h3>判定機制</h3>
	 * <ul>
	 *   <li><b>大失敗(Fumble)：</b>攻擊骰 ≤ 基礎命中-9 → 必定Miss</li>
	 *   <li><b>爆擊(Critical)：</b>攻擊骰 ≥ 基礎命中+10 → 必定命中</li>
	 *   <li><b>一般判定：</b>攻擊骰 > 防禦骰 → 命中</li>
	 * </ul>
	 *
	 * <h3>特殊規則</h3>
	 * <ul>
	 *   <li>奇古獸（Type 17/19）命中率固定100%</li>
	 *   <li>負重影響：81-121(-1)、122-160(-3)、161-200(-5)</li>
	 *   <li>弓箭命中後需額外進行ER閃避判定</li>
	 *   <li>魔法娃娃傷害迴避效果可強制Miss</li>
	 * </ul>
	 *
	 * @return true表示命中，false表示未命中
	 * @see #calShortRageHit(int) 近戰命中加成計算
	 * @see #calLongRageHit(int) 遠程命中加成計算
	 * @see #calcErEvasion() ER閃避判定
	 */
	private boolean calcPcPcHit() {
		_hitRate = _pc.getLevel();

		if (_pc.getStr() > 59) {
			_hitRate += strHit[58];
		} else {
			_hitRate += strHit[_pc.getStr() - 1];
		}

		if (_pc.getDex() > 60) {
			_hitRate += dexHit[59];
		} else {
			_hitRate += dexHit[_pc.getDex() - 1];
		}

		// 命中計算 與魔法、食物buff
		_hitRate += _weaponAddHit + (_weaponEnchant / 2);
		if (_weaponType == 20 || _weaponType == 62)
			_hitRate = calLongRageHit(_hitRate);
		else
			_hitRate = calShortRageHit(_hitRate);

		if ((80 < _pc.getInventory().getWeight242() // 重量による命中補正
				)
				&& (121 >= _pc.getInventory().getWeight242())) {
			_hitRate -= 1;
		} else if ((122 <= _pc.getInventory().getWeight242())
				&& (160 >= _pc.getInventory().getWeight242())) {
			_hitRate -= 3;
		} else if ((161 <= _pc.getInventory().getWeight242())
				&& (200 >= _pc.getInventory().getWeight242())) {
			_hitRate -= 5;
		}

		int attackerDice = Random.nextInt(20) + 1 + _hitRate - 10;

		// 閃避率
		attackerDice -= _targetPc.getDodge();
		attackerDice += _targetPc.getNdodge();

		int defenderDice = 0;

		int defenderValue = (int) (_targetPc.getAc() * 1.5) * -1;

		if (_targetPc.getAc() >= 0) {
			defenderDice = 10 - _targetPc.getAc();
		} else if (_targetPc.getAc() < 0) {
			defenderDice = 10 + Random.nextInt(defenderValue) + 1;
		}

		int fumble = _hitRate - 9;
		int critical = _hitRate + 10;

		if (attackerDice <= fumble) {
			_hitRate = 0;
		} else if (attackerDice >= critical) {
			_hitRate = 100;
		} else {
			if (attackerDice > defenderDice) {
				_hitRate = 100;
			} else if (attackerDice <= defenderDice) {
				_hitRate = 0;
			}
		}

		if (_weaponType2 == 17 || _weaponType2 == 19) {
			_hitRate = 100; // 奇古獸命中率100%
		}

		// TODO 魔法娃娃效果 - 傷害迴避
		else if (L1MagicDoll.getDamageEvasionByDoll(_targetPc) > 0) {
			_hitRate = 0;
		}

		int rnd = Random.nextInt(100) + 1;
		if ((_weaponType == 20) && (_hitRate > rnd)) { // 弓の場合、ヒットした場合でもERでの回避を再度行う。
			return calcErEvasion();
		}

		return _hitRate >= rnd;

		/*
		 * final int MIN_HITRATE = 5;
		 * 
		 * _hitRate = _pc.getLevel();
		 * 
		 * if (_pc.getStr() > 39) { _hitRate += strHit[39]; } else { _hitRate +=
		 * strHit[_pc.getStr()]; }
		 * 
		 * if (_pc.getDex() > 39) { _hitRate += dexHit[39]; } else { _hitRate +=
		 * dexHit[_pc.getDex()]; }
		 * 
		 * if (_weaponType != 20 && _weaponType != 62) { _hitRate +=
		 * _weaponAddHit + _pc.getHitup() + _pc.getOriginalHitup() +
		 * (_weaponEnchant / 2); } else { _hitRate += _weaponAddHit +
		 * _pc.getBowHitup() + _pc .getOriginalBowHitup() + (_weaponEnchant /
		 * 2); }
		 * 
		 * if (_weaponType != 20 && _weaponType != 62) { // 防具による追加命中 _hitRate
		 * += _pc.getHitModifierByArmor(); } else { _hitRate +=
		 * _pc.getBowHitModifierByArmor(); }
		 * 
		 * int hitAc = (int) (_hitRate * 0.68 - 10) * -1;
		 * 
		 * if (hitAc <= _targetPc.getAc()) { _hitRate = 95; } else { _hitRate =
		 * 95 - (hitAc - _targetPc.getAc()); }
		 * 
		 * if (_targetPc.hasSkillEffect(UNCANNY_DODGE)) { _hitRate -= 20; }
		 * 
		 * if (_targetPc.hasSkillEffect(MIRROR_IMAGE)) { _hitRate -= 20; }
		 * 
		 * if (_pc.hasSkillEffect(COOKING_2_0_N) // 料理による追加命中 ||
		 * _pc.hasSkillEffect(COOKING_2_0_S)) { if (_weaponType != 20 &&
		 * _weaponType != 62) { _hitRate += 1; } } if
		 * (_pc.hasSkillEffect(COOKING_3_2_N) // 料理による追加命中 ||
		 * _pc.hasSkillEffect(COOKING_3_2_S)) { if (_weaponType != 20 &&
		 * _weaponType != 62) { _hitRate += 2; } } if
		 * (_pc.hasSkillEffect(COOKING_2_3_N) // 料理による追加命中 ||
		 * _pc.hasSkillEffect(COOKING_2_3_S) ||
		 * _pc.hasSkillEffect(COOKING_3_0_N) ||
		 * _pc.hasSkillEffect(COOKING_3_0_S)) { if (_weaponType == 20 ||
		 * _weaponType == 62) { _hitRate += 1; } }
		 * 
		 * if (_hitRate < MIN_HITRATE) { _hitRate = MIN_HITRATE; }
		 * 
		 * if (_weaponType2 == 17) { _hitRate = 100; // キーリンクの命中率は100% }
		 * 
		 * if (_targetPc.hasSkillEffect(ABSOLUTE_BARRIER)) { _hitRate = 0; } if
		 * (_targetPc.hasSkillEffect(ICE_LANCE)) { _hitRate = 0; } if
		 * (_targetPc.hasSkillEffect(FREEZING_BLIZZARD)) { _hitRate = 0; } if
		 * (_targetPc.hasSkillEffect(FREEZING_BREATH)) { _hitRate = 0; } if
		 * (_targetPc.hasSkillEffect(EARTH_BIND)) { _hitRate = 0; } int rnd =
		 * Random.nextInt(100) + 1; if (_weaponType == 20 && _hitRate > rnd) {
		 * // 弓の場合、ヒットした場合でもERでの回避を再度行う。 return calcErEvasion(); }
		 * 
		 * return _hitRate >= rnd;
		 */
	}

	/**
	 * 計算玩家對NPC/怪物的命中判定
	 * <p>PvE命中計算同樣使用D20骰子系統，但判定機制與PvP略有不同</p>
	 *
	 * <h3>命中率計算</h3>
	 * <pre>
	 * 基礎命中 = 等級 + STR補正 + DEX補正 + 武器命中 + 強化值/2 + 裝備加成
	 * 攻擊骰 = 1d20 + 基礎命中 - 10 - 怪物閃避
	 * 防禦骰 = 10 - 怪物AC
	 * </pre>
	 *
	 * <h3>特殊規則</h3>
	 * <ul>
	 *   <li>奇古獸（Type 17/19）命中率固定100%</li>
	 *   <li>負重影響命中率（同PvP）</li>
	 *   <li>特定狀態下限制攻擊某些NPC（isAttackMiss檢查）</li>
	 * </ul>
	 *
	 * @return true表示命中，false表示未命中
	 */
	private boolean calcPcNpcHit() {
		_hitRate = _pc.getLevel();

		if (_pc.getStr() > 59) {
			_hitRate += strHit[58];
		} else {
			_hitRate += strHit[_pc.getStr() - 1];
		}

		if (_pc.getDex() > 60) {
			_hitRate += dexHit[59];
		} else {
			_hitRate += dexHit[_pc.getDex() - 1];
		}

		// 命中計算 與魔法、食物buff
		_hitRate += _weaponAddHit + (_weaponEnchant / 2);
		if (_weaponType == 20 || _weaponType == 62)
			_hitRate = calLongRageHit(_hitRate);
		else
			_hitRate = calShortRageHit(_hitRate);

		if ((80 < _pc.getInventory().getWeight242() // 重量による命中補正
				)
				&& (121 >= _pc.getInventory().getWeight242())) {
			_hitRate -= 1;
		} else if ((122 <= _pc.getInventory().getWeight242())
				&& (160 >= _pc.getInventory().getWeight242())) {
			_hitRate -= 3;
		} else if ((161 <= _pc.getInventory().getWeight242())
				&& (200 >= _pc.getInventory().getWeight242())) {
			_hitRate -= 5;
		}

		int attackerDice = Random.nextInt(20) + 1 + _hitRate - 10;

		// 閃避率
		attackerDice -= _targetNpc.getDodge();
		attackerDice += _targetNpc.getNdodge();

		int defenderDice = 10 - _targetNpc.getAc();

		int fumble = _hitRate - 9;
		int critical = _hitRate + 10;

		if (attackerDice <= fumble) {
			_hitRate = 0;
		} else if (attackerDice >= critical) {
			_hitRate = 100;
		} else {
			if (attackerDice > defenderDice) {
				_hitRate = 100;
			} else if (attackerDice <= defenderDice) {
				_hitRate = 0;
			}
		}

		if (_weaponType2 == 17 || _weaponType2 == 19) {
			_hitRate = 100; // 奇古獸 命中率 100%
		}

		// 特定狀態下才可攻擊 NPC
		if (_pc.isAttackMiss(_pc, _targetNpc.getNpcTemplate().get_npcId())) {
			_hitRate = 0;
		}

		int rnd = Random.nextInt(100) + 1;

		return _hitRate >= rnd;
	}

	/**
	 * 計算NPC/怪物對玩家的命中判定
	 * <p>怪物攻擊玩家的命中計算，包含寵物和召喚獸的特殊處理</p>
	 *
	 * <h3>命中計算</h3>
	 * <pre>
	 * 基礎命中 = 怪物等級 + 怪物命中加成 + 寵物武器加成（如適用）
	 * 攻擊骰 = 1d20 + 基礎命中 - 1 - 玩家閃避
	 * Fumble = 基礎命中, Critical = 基礎命中+19
	 * </pre>
	 *
	 * <h3>特殊規則</h3>
	 * <ul>
	 *   <li>寵物/召喚獸在安全區無法攻擊</li>
	 *   <li>遠程攻擊（攻擊距離≥10且距離≥2格）需ER閃避判定</li>
	 *   <li>魔法娃娃傷害迴避可強制Miss</li>
	 * </ul>
	 *
	 * @return true表示命中，false表示未命中
	 */
	private boolean calcNpcPcHit() {

		_hitRate += _npc.getLevel();

		if (_npc instanceof L1PetInstance) { // ペットの武器による追加命中
			_hitRate += ((L1PetInstance) _npc).getHitByWeapon();
		}

		_hitRate += _npc.getHitup();

		int attackerDice = Random.nextInt(20) + 1 + _hitRate - 1;

		// 閃避率
		attackerDice -= _targetPc.getDodge();
		attackerDice += _targetPc.getNdodge();

		int defenderDice = 0;

		int defenderValue = (_targetPc.getAc()) * -1;

		if (_targetPc.getAc() >= 0) {
			defenderDice = 10 - _targetPc.getAc();
		} else if (_targetPc.getAc() < 0) {
			defenderDice = 10 + Random.nextInt(defenderValue) + 1;
		}

		int fumble = _hitRate;
		int critical = _hitRate + 19;

		if (attackerDice <= fumble) {
			_hitRate = 0;
		} else if (attackerDice >= critical) {
			_hitRate = 100;
		} else {
			if (attackerDice > defenderDice) {
				_hitRate = 100;
			} else if (attackerDice <= defenderDice) {
				_hitRate = 0;
			}
		}

		if ((_npc instanceof L1PetInstance)
				|| (_npc instanceof L1SummonInstance)) {
			// 目標在安區、攻擊者在安區、NOPVP
			if ((_targetPc.getZoneType() == 1) || (_npc.getZoneType() == 1)
					|| (_targetPc.checkNonPvP(_targetPc, _npc))) {
				_hitRate = 0;
			}
		}
		// TODO 魔法娃娃效果 - 傷害迴避
		else if (L1MagicDoll.getDamageEvasionByDoll(_targetPc) > 0) {
			_hitRate = 0;
		}

		int rnd = Random.nextInt(100) + 1;

		// NPCの攻撃レンジが10以上の場合で、2以上離れている場合弓攻撃とみなす
		if ((_npc.getAtkRanged() >= 10)
				&& (_hitRate > rnd)
				&& (_npc.getLocation().getTileLineDistance(
						new Point(_targetX, _targetY)) >= 2)) {
			return calcErEvasion();
		}
		return _hitRate >= rnd;
	}

	/**
	 * 計算NPC對NPC的命中判定
	 * <p>用於寵物/召喚獸互相攻擊或怪物間戰鬥的命中計算</p>
	 *
	 * @return true表示命中，false表示未命中
	 */
	private boolean calcNpcNpcHit() {

		_hitRate += _npc.getLevel();

		if (_npc instanceof L1PetInstance) { // ペットの武器による追加命中
			_hitRate += ((L1PetInstance) _npc).getHitByWeapon();
		}

		_hitRate += _npc.getHitup();

		int attackerDice = Random.nextInt(20) + 1 + _hitRate - 1;

		// 閃避率
		attackerDice -= _targetNpc.getDodge();
		attackerDice += _targetNpc.getNdodge();

		int defenderDice = 0;

		int defenderValue = (_targetNpc.getAc()) * -1;

		if (_targetNpc.getAc() >= 0) {
			defenderDice = 10 - _targetNpc.getAc();
		} else if (_targetNpc.getAc() < 0) {
			defenderDice = 10 + Random.nextInt(defenderValue) + 1;
		}

		int fumble = _hitRate;
		int critical = _hitRate + 19;

		if (attackerDice <= fumble) {
			_hitRate = 0;
		} else if (attackerDice >= critical) {
			_hitRate = 100;
		} else {
			if (attackerDice > defenderDice) {
				_hitRate = 100;
			} else if (attackerDice <= defenderDice) {
				_hitRate = 0;
			}
		}
		if (((_npc instanceof L1PetInstance) || (_npc instanceof L1SummonInstance))
				&& ((_targetNpc instanceof L1PetInstance) || (_targetNpc instanceof L1SummonInstance))) {
			// 目標在安區、攻擊者在安區、NOPVP
			if ((_targetNpc.getZoneType() == 1) || (_npc.getZoneType() == 1)) {
				_hitRate = 0;
			}
		}

		int rnd = Random.nextInt(100) + 1;
		return _hitRate >= rnd;
	}

	/**
	 * 計算ER（Evasion Rate）閃避判定
	 * <p>遠程攻擊命中後的額外閃避檢查，主要用於弓箭攻擊</p>
	 *
	 * @return true表示無法閃避（攻擊成功），false表示成功閃避
	 */
	private boolean calcErEvasion() {
		int er = _targetPc.getEr();

		int rnd = Random.nextInt(100) + 1;
		return er < rnd;
	}

	/* ■■■■■■■■■■■■■■■ ダメージ算出 ■■■■■■■■■■■■■■■ */

	/**
	 * 計算攻擊傷害
	 * <p>根據攻擊類型調用對應的傷害計算方法</p>
	 *
	 * @return 最終計算的傷害值
	 * @see #calcPcPcDamage() 玩家對玩家傷害
	 * @see #calcPcNpcDamage() 玩家對NPC傷害
	 * @see #calcNpcPcDamage() NPC對玩家傷害
	 * @see #calcNpcNpcDamage() NPC對NPC傷害
	 */
	public int calcDamage() {
		if (_calcType == PC_PC) {
			_damage = calcPcPcDamage();
		} else if (_calcType == PC_NPC) {
			_damage = calcPcNpcDamage();
		} else if (_calcType == NPC_PC) {
			_damage = calcNpcPcDamage();
		} else if (_calcType == NPC_NPC) {
			_damage = calcNpcNpcDamage();
		}
		return _damage;
	}

	/**
	 * 計算武器基礎傷害
	 * <p>根據武器類型和屬性計算武器本身造成的傷害</p>
	 *
	 * <h3>計算要素</h3>
	 * <ul>
	 *   <li>武器基礎傷害（1 ~ 武器最大傷害的隨機值）</li>
	 *   <li>武器額外傷害加成</li>
	 *   <li>武器強化等級加成</li>
	 *   <li>祝福/銀製武器對不死系額外傷害（PvE）</li>
	 *   <li>武器屬性附魔傷害</li>
	 * </ul>
	 *
	 * <h3>特殊武器處理</h3>
	 * <ul>
	 *   <li><b>鋼爪（Type 58）：</b>黑妖專用，有機率造成最大傷害並顯示爪痕特效</li>
	 *   <li><b>雙刀（Type 54）：</b>有機率觸發雙擊（傷害x2）</li>
	 *   <li><b>弓箭/鐵手甲：</b>武器本身不計傷害（彈藥計算在遠程傷害中）</li>
	 *   <li><b>火焰之魂技能：</b>武器傷害直接取最大值</li>
	 *   <li><b>雙重斬技能：</b>黑妖武器有33%機率傷害x2</li>
	 * </ul>
	 *
	 * @param weaponMaxDamage 武器最大傷害值
	 * @return 計算後的武器總傷害
	 * @see #calcMaterialBlessDmg() 材質祝福額外傷害
	 * @see #calcAttrEnchantDmg() 屬性附魔傷害
	 */
	private int calcWeponDamage(int weaponMaxDamage) {
		int weaponDamage = Random.nextInt(weaponMaxDamage) + 1;
		// 判斷魔法輔助
		if (_pc.hasSkillEffect(SOUL_OF_FLAME))
			weaponDamage = weaponMaxDamage;

		// 判斷武器類型
		boolean darkElfWeapon = false ;
		if (_pc.isDarkelf() && (_weaponType == 58)) { // 鋼爪 (追加判斷持有者為黑妖，避免與幻術師奇谷獸相衝)
			darkElfWeapon = true ;
			if ((Random.nextInt(100) + 1) <= _weaponDoubleDmgChance) { // 出現最大值的機率
				weaponDamage = weaponMaxDamage;
			}
			if (weaponDamage == weaponMaxDamage) { // 出現最大值時 - 爪痕
				_effectId = 2;
			}
		} else if (_weaponType == 20 || _weaponType == 62) {// 弓、鐵手甲 不算武器傷害
			weaponDamage = 0;
		}

		weaponDamage +=  _weaponAddDmg + _weaponEnchant ; // 加上武器(額外點數+祝福魔法武器)跟武卷數

		if (_calcType == PC_NPC)
			weaponDamage += calcMaterialBlessDmg(); // 銀祝福武器加傷害
		if (_weaponType == 54) {
			darkElfWeapon = true ;
			if ((Random.nextInt(100) + 1) <= _weaponDoubleDmgChance) { // 雙刀雙擊
				weaponDamage *= 2;
				_effectId = 4;
			}
		}
		weaponDamage += calcAttrEnchantDmg(); // 属性強化傷害

		if (darkElfWeapon && _pc.hasSkillEffect(DOUBLE_BRAKE)) 
			if ((Random.nextInt(100) + 1) <= 33) 
				weaponDamage *= 2;

		return weaponDamage;
	}

	/**
	 * 計算遠程武器傷害加成
	 * <p>計算弓箭和鐵手甲的傷害，包含彈藥傷害和裝備加成</p>
	 *
	 * <h3>計算要素</h3>
	 * <ul>
	 *   <li>弓箭傷害加成（getBowDmgup）</li>
	 *   <li>箭矢/飛刀傷害（對小型或大型目標）</li>
	 *   <li>防具傷害加成</li>
	 *   <li>料理buff傷害加成</li>
	 * </ul>
	 *
	 * <h3>特殊規則</h3>
	 * <ul>
	 *   <li>沙哈之弓（ID 190）無需箭矢，固定15點傷害</li>
	 *   <li>堅硬怪物（is_hard）箭矢傷害減半</li>
	 *   <li>大型怪物使用箭矢的大型傷害值</li>
	 * </ul>
	 *
	 * @param dmg 基礎傷害
	 * @return 加上遠程加成後的傷害
	 */
	private double calLongRageDamage(double dmg) {
		double longdmg = dmg + _pc.getBowDmgup() + _pc.getOriginalBowDmgup();

		int add_dmg = 1;
		if (_weaponType == 20) { // 弓
			if (_arrow != null) {
				add_dmg = _arrow.getItem().getDmgSmall();
				if (_calcType == PC_NPC) {
					if (_targetNpc.getNpcTemplate().get_size()
							.equalsIgnoreCase("large"))
						add_dmg = _arrow.getItem().getDmgLarge();
					if (_targetNpc.getNpcTemplate().is_hard())
						add_dmg /= 2;
				}
			} else if (_weaponId == 190)  // 沙哈之弓
				add_dmg = 15;
		} else if (_weaponType == 62) { // 鐵手甲
			add_dmg = _sting.getItem().getDmgSmall();
			if (_calcType == PC_NPC)
				if (_targetNpc.getNpcTemplate().get_size()
						.equalsIgnoreCase("large"))
					add_dmg = _sting.getItem().getDmgLarge();
		}
		
		if ( add_dmg > 0) 
			longdmg += Random.nextInt(add_dmg) + 1;

		// 防具增傷
		longdmg += _pc.getDmgModifierByArmor();

		if (_pc.hasSkillEffect(COOKING_2_3_N) // 料理
				|| _pc.hasSkillEffect(COOKING_2_3_S)
				|| _pc.hasSkillEffect(COOKING_3_0_N)
				|| _pc.hasSkillEffect(COOKING_3_0_S))
			longdmg += 1;

		return longdmg;
	}

	/**
	 * 計算近戰武器傷害加成
	 * <p>計算近戰武器的各種傷害加成，包含魔法buff和特殊武器效果</p>
	 *
	 * <h3>計算要素</h3>
	 * <ul>
	 *   <li>近戰傷害加成（getDmgup）</li>
	 *   <li>弱點曝光效果判定</li>
	 *   <li>魔法buff傷害加成（calcBuffDamage）</li>
	 *   <li>防具傷害加成</li>
	 *   <li>料理buff傷害加成</li>
	 * </ul>
	 *
	 * <h3>特殊武器處理</h3>
	 * <ul>
	 *   <li>空手（Type 0）：固定傷害計算</li>
	 *   <li>奇古獸（Type 17/19）：特殊傷害計算公式</li>
	 * </ul>
	 *
	 * @param dmg 基礎傷害
	 * @return 加上近戰加成後的傷害
	 * @see #WeaknessExposure() 弱點曝光判定
	 * @see #calcBuffDamage(double) 魔法buff傷害
	 */
	private double calShortRageDamage(double dmg) {
		double shortdmg = dmg + _pc.getDmgup() + _pc.getOriginalDmgup();
		// 弱點曝光發動判斷
		WeaknessExposure();
		// 近戰魔法增傷
		shortdmg = calcBuffDamage(shortdmg);
		// 防具增傷
		shortdmg += _pc.getBowDmgModifierByArmor();

		if (_weaponType == 0) // 空手
			shortdmg = (Random.nextInt(5) + 4) / 4;
		else if (_weaponType2 == 17 || _weaponType2 == 19) // 奇古獸
			shortdmg = L1WeaponSkill.getKiringkuDamage(_pc, _target);

		if (_pc.hasSkillEffect(COOKING_2_0_N) // 料理
				|| _pc.hasSkillEffect(COOKING_2_0_S)
				|| _pc.hasSkillEffect(COOKING_3_2_N)
				|| _pc.hasSkillEffect(COOKING_3_2_S))
			shortdmg += 1;

		return shortdmg;
	}

	/**
	 * 計算玩家對玩家的傷害
	 * <p>PvP傷害計算，包含所有武器技能、魔法效果和傷害減免</p>
	 *
	 * <h3>傷害計算流程</h3>
	 * <ol>
	 *   <li>計算武器基礎傷害</li>
	 *   <li>加上屬性補正（STR或DEX）</li>
	 *   <li>加上遠程/近戰加成</li>
	 *   <li>加上特殊武器技能傷害</li>
	 *   <li>減去目標防具減免</li>
	 *   <li>減去魔法娃娃減免</li>
	 *   <li>減去料理/技能減免</li>
	 * </ul>
	 *
	 * <h3>特殊技能</h3>
	 * <ul>
	 *   <li><b>破壞（SMASH）：</b>額外+15傷害</li>
	 *   <li><b>骷髏毀壞（BONE_BREAK）：</b>額外+10傷害，可能觸發debuff</li>
	 *   <li><b>免疫傷害（IMMUNE_TO_HARM）：</b>目標傷害減半</li>
	 * </ul>
	 *
	 * @return 最終傷害值
	 */
	public int calcPcPcDamage() {
		// 計算武器總傷害
		int weaponTotalDamage = calcWeponDamage(_weaponSmall);

		if ((_weaponId == 262) && (Random.nextInt(100) + 1 <= 75)) { // ディストラクション装備かつ成功確率(暫定)75%
			weaponTotalDamage += calcDestruction(weaponTotalDamage);
		}

		// 計算 遠程 或 近戰武器 傷害 與魔法、食物buff
		double dmg = weaponTotalDamage + _statusDamage;
		if (_weaponType == 20 || _weaponType == 62)
			dmg = calLongRageDamage(dmg);
		else
			dmg = calShortRageDamage(dmg);

		if (_weaponId == 124 || _weaponId == 289 || _weaponId == 290
				|| _weaponId == 291 || _weaponId == 292 || _weaponId == 293
				|| _weaponId == 294 || _weaponId == 295 || _weaponId == 296
				|| _weaponId == 297 || _weaponId == 298 || _weaponId == 299
				|| _weaponId == 300 || _weaponId == 301 || _weaponId == 302
				|| _weaponId == 303) { // バフォメットスタッフ
			dmg += L1WeaponSkill.getBaphometStaffDamage(_pc, _target);
		} else if (_weaponId == 2 || _weaponId == 200002) { // ダイスダガー
			dmg += L1WeaponSkill.getDiceDaggerDamage(_pc, _targetPc, weapon);
		} else if (_weaponId == 204 || _weaponId == 100204) { // 真紅のクロスボウ
			L1WeaponSkill.giveFettersEffect(_pc, _targetPc);
		} else if (_weaponId == 264 || _weaponId == 288) { // ライトニングエッジ
			dmg += L1WeaponSkill.getLightningEdgeDamage(_pc, _target);
		} else if (_weaponId == 260 || _weaponId == 263 || _weaponId == 287) { // レイジングウィンド、フリージングランサー
			dmg += L1WeaponSkill.getAreaSkillWeaponDamage(_pc, _target,
					_weaponId);
		} else if (_weaponId == 261) { // アークメイジスタッフ
			L1WeaponSkill.giveArkMageDiseaseEffect(_pc, _target);
		} else {
			dmg += L1WeaponSkill.getWeaponSkillDamage(_pc, _target, _weaponId);
		}

		dmg -= _targetPc.getDamageReductionByArmor(); // 防具によるダメージ軽減

		// 魔法娃娃效果 - 傷害減免
		dmg -= L1MagicDoll.getDamageReductionByDoll(_targetPc);

		if (_targetPc.hasSkillEffect(COOKING_1_0_S) // 料理によるダメージ軽減
				|| _targetPc.hasSkillEffect(COOKING_1_1_S)
				|| _targetPc.hasSkillEffect(COOKING_1_2_S)
				|| _targetPc.hasSkillEffect(COOKING_1_3_S)
				|| _targetPc.hasSkillEffect(COOKING_1_4_S)
				|| _targetPc.hasSkillEffect(COOKING_1_5_S)
				|| _targetPc.hasSkillEffect(COOKING_1_6_S)
				|| _targetPc.hasSkillEffect(COOKING_2_0_S)
				|| _targetPc.hasSkillEffect(COOKING_2_1_S)
				|| _targetPc.hasSkillEffect(COOKING_2_2_S)
				|| _targetPc.hasSkillEffect(COOKING_2_3_S)
				|| _targetPc.hasSkillEffect(COOKING_2_4_S)
				|| _targetPc.hasSkillEffect(COOKING_2_5_S)
				|| _targetPc.hasSkillEffect(COOKING_2_6_S)
				|| _targetPc.hasSkillEffect(COOKING_3_0_S)
				|| _targetPc.hasSkillEffect(COOKING_3_1_S)
				|| _targetPc.hasSkillEffect(COOKING_3_2_S)
				|| _targetPc.hasSkillEffect(COOKING_3_3_S)
				|| _targetPc.hasSkillEffect(COOKING_3_4_S)
				|| _targetPc.hasSkillEffect(COOKING_3_5_S)
				|| _targetPc.hasSkillEffect(COOKING_3_6_S)) {
			dmg -= 5;
		}
		if (_targetPc.hasSkillEffect(COOKING_1_7_S) // デザートによるダメージ軽減
				|| _targetPc.hasSkillEffect(COOKING_2_7_S)
				|| _targetPc.hasSkillEffect(COOKING_3_7_S)) {
			dmg -= 5;
		}

		if (_targetPc.hasSkillEffect(REDUCTION_ARMOR)) {
			int targetPcLvl = _targetPc.getLevel();
			if (targetPcLvl < 50) {
				targetPcLvl = 50;
			}
			dmg -= (targetPcLvl - 50) / 5 + 1;
		}
		if (_targetPc.hasSkillEffect(DRAGON_SKIN)
				|| _targetPc.hasSkillEffect(PATIENCE)) {
			dmg -= 2;
		}
		if (_targetPc.hasSkillEffect(IMMUNE_TO_HARM)) {
			dmg /= 2;
		}
		// 使用暴擊增加15點傷害，而奇古獸固定15點傷害
		if (_skillId == SMASH) {
			dmg += 15;
			if (_weaponType2 == 17 || _weaponType2 == 19) {
				dmg = 15;
			}
		}
		// 使用骷髏毀壞增加10點傷害，而奇古獸固定10點傷害
		else if (_skillId == BONE_BREAK) {
			dmg += 10;
			if (_weaponType2 == 17 || _weaponType2 == 19) {
				dmg = 10;
			}
			// 再次發動判斷
			if (!_targetPc.hasSkillEffect(BONE_BREAK)) {
				int change = Random.nextInt(100) + 1;
				if (change < (30 + Random.nextInt(11))) { // 30 ~ 40%
					L1EffectSpawn.getInstance().spawnEffect(93001, 1700,
							_targetPc.getX(), _targetPc.getY(),
							_targetPc.getMapId());
					_targetPc.setSkillEffect(BONE_BREAK, 2 * 1000); // 發動後再次發動間隔
																	// 2秒
					_targetPc.setSkillEffect(BONE_BREAK_START, 700);
				}
			}
		}
		if (dmg <= 0) {
			_isHit = false;
			_drainHp = 0; // ダメージ無しの場合は吸収による回復はしない
		}

		return (int) dmg;
	}

	/**
	 * 計算玩家對NPC/怪物的傷害
	 * <p>PvE傷害計算，包含目標體型判定和特殊武器效果</p>
	 *
	 * <h3>重要特性</h3>
	 * <ul>
	 *   <li>根據怪物體型（small/large）選擇對應的武器傷害</li>
	 *   <li>銀製/祝福武器對不死系額外傷害</li>
	 *   <li>非攻城時期對寵物/召喚獸傷害÷8</li>
	 *   <li>怪物傷害減免計算</li>
	 * </ul>
	 *
	 * @return 最終傷害值
	 */
	private int calcPcNpcDamage() {
		int weaponMaxDamage = 0;
		if (_targetNpc.getNpcTemplate().get_size().equalsIgnoreCase("small")
				&& (_weaponSmall > 0)) {
			weaponMaxDamage = _weaponSmall;
		} else if (_targetNpc.getNpcTemplate().get_size()
				.equalsIgnoreCase("large")
				&& (_weaponLarge > 0)) {
			weaponMaxDamage = _weaponLarge;
		}

		// 計算武器總傷害
		int weaponTotalDamage = calcWeponDamage(weaponMaxDamage) ;
		
		if ((_weaponId == 262) && (Random.nextInt(100) + 1 <= 75)) { // ディストラクション装備かつ成功確率(暫定)75%
			weaponTotalDamage += calcDestruction(weaponTotalDamage);
		}

		// 計算傷害 遠程 或 近戰武器 及buff
		double dmg = weaponTotalDamage + _statusDamage;
		if (_weaponType == 20 || _weaponType == 62)
			dmg = calLongRageDamage(dmg);
		else
			dmg = calShortRageDamage(dmg);

		if (_weaponId == 124 || _weaponId == 289 || _weaponId == 290
				|| _weaponId == 291 || _weaponId == 292 || _weaponId == 293
				|| _weaponId == 294 || _weaponId == 295 || _weaponId == 296
				|| _weaponId == 297 || _weaponId == 298 || _weaponId == 299
				|| _weaponId == 300 || _weaponId == 301 || _weaponId == 302
				|| _weaponId == 303) {
			dmg += L1WeaponSkill.getBaphometStaffDamage(_pc, _target);
		} else if ((_weaponId == 2) || (_weaponId == 200002)) { // ダイスダガー
			dmg += L1WeaponSkill.getDiceDaggerDamage(_pc, _targetNpc, weapon);
		} else if ((_weaponId == 204) || (_weaponId == 100204)) { // 真紅のクロスボウ
			L1WeaponSkill.giveFettersEffect(_pc, _targetNpc);
		//} else if (_weaponId == 264 || _weaponId == 291) { // ライトニングエッジ
		} else if (_weaponId == 264 || _weaponId == 288) { // ライトニングエッジ, 天雷劍能發動的修正
			dmg += L1WeaponSkill.getLightningEdgeDamage(_pc, _target);
		} else if ((_weaponId == 260) || (_weaponId == 263 || _weaponId == 287)) { // レイジングウィンド、フリージングランサー
			dmg += L1WeaponSkill.getAreaSkillWeaponDamage(_pc, _target,
					_weaponId);
		} else if (_weaponId == 261) { // アークメイジスタッフ
			L1WeaponSkill.giveArkMageDiseaseEffect(_pc, _target);
		} else {
			dmg += L1WeaponSkill.getWeaponSkillDamage(_pc, _target, _weaponId);
		}

		dmg -= calcNpcDamageReduction();

		// 使用暴擊增加15點傷害，而奇古獸固定15點傷害
		if (_skillId == SMASH) {
			dmg += 15;
			if (_weaponType2 == 17 || _weaponType2 == 19) {
				dmg = 15;
			}
		}
		// 使用骷髏毀壞增加10點傷害，而奇古獸固定10點傷害
		else if (_skillId == BONE_BREAK) {
			dmg += 10;
			if (_weaponType2 == 17 || _weaponType2 == 19) {
				dmg = 10;
			}
			// 再次發動判斷
			if (!_targetNpc.hasSkillEffect(BONE_BREAK)) {
				int change = Random.nextInt(100) + 1;
				if (change < (30 + Random.nextInt(11))) { // 30 ~ 40%
					L1EffectSpawn.getInstance().spawnEffect(93001, 1700,
							_targetNpc.getX(), _targetNpc.getY(),
							_targetNpc.getMapId());
					_targetNpc.setSkillEffect(BONE_BREAK, 2 * 1000); // 發動後再次發動間隔
																		// 2秒
					_targetNpc.setSkillEffect(BONE_BREAK_START, 700);
				}
			}
		}

		// 非攻城區域對寵物、召喚獸傷害減少
		boolean isNowWar = false;
		int castleId = L1CastleLocation.getCastleIdByArea(_targetNpc);
		if (castleId > 0) {
			isNowWar = WarTimeController.getInstance().isNowWar(castleId);
		}
		if (!isNowWar) {
			if (_targetNpc instanceof L1PetInstance)
				dmg /= 8;
			else if (_targetNpc instanceof L1SummonInstance) {
				L1SummonInstance summon = (L1SummonInstance) _targetNpc;
				if (summon.isExsistMaster())
					dmg /= 8;
			}
		}
		if (dmg <= 0) {
			_isHit = false;
			_drainHp = 0; // ダメージ無しの場合は吸収による回復はしない
		}

		return (int) dmg;
	}

	// ●●●● ＮＰＣ から プレイヤー へのダメージ算出 ●●●●
	private int calcNpcPcDamage() {
		int lvl = _npc.getLevel();
		double dmg = 0D;
		if (lvl < 10) {
			dmg = Random.nextInt(lvl) + 10D + _npc.getStr() / 2 + 1;
		} else {
			dmg = Random.nextInt(lvl) + _npc.getStr() / 2 + 1;
		}

		if (_npc instanceof L1PetInstance) {
			dmg += (lvl / 16); // ペットはLV16毎に追加打撃
			dmg += ((L1PetInstance) _npc).getDamageByWeapon();
		}

		dmg += _npc.getDmgup();

		if (isUndeadDamage()) {
			dmg *= 1.1;
		}

		dmg = dmg * getLeverage() / 10;

		dmg -= calcPcDefense();

		if (_npc.isWeaponBreaked()) { // ＮＰＣがウェポンブレイク中。
			dmg /= 2;
		}

		dmg -= _targetPc.getDamageReductionByArmor(); // 防具によるダメージ軽減

		// 魔法娃娃效果 - 傷害減免
		dmg -= L1MagicDoll.getDamageReductionByDoll(_targetPc);

		if (_targetPc.hasSkillEffect(COOKING_1_0_S) // 料理によるダメージ軽減
				|| _targetPc.hasSkillEffect(COOKING_1_1_S)
				|| _targetPc.hasSkillEffect(COOKING_1_2_S)
				|| _targetPc.hasSkillEffect(COOKING_1_3_S)
				|| _targetPc.hasSkillEffect(COOKING_1_4_S)
				|| _targetPc.hasSkillEffect(COOKING_1_5_S)
				|| _targetPc.hasSkillEffect(COOKING_1_6_S)
				|| _targetPc.hasSkillEffect(COOKING_2_0_S)
				|| _targetPc.hasSkillEffect(COOKING_2_1_S)
				|| _targetPc.hasSkillEffect(COOKING_2_2_S)
				|| _targetPc.hasSkillEffect(COOKING_2_3_S)
				|| _targetPc.hasSkillEffect(COOKING_2_4_S)
				|| _targetPc.hasSkillEffect(COOKING_2_5_S)
				|| _targetPc.hasSkillEffect(COOKING_2_6_S)
				|| _targetPc.hasSkillEffect(COOKING_3_0_S)
				|| _targetPc.hasSkillEffect(COOKING_3_1_S)
				|| _targetPc.hasSkillEffect(COOKING_3_2_S)
				|| _targetPc.hasSkillEffect(COOKING_3_3_S)
				|| _targetPc.hasSkillEffect(COOKING_3_4_S)
				|| _targetPc.hasSkillEffect(COOKING_3_5_S)
				|| _targetPc.hasSkillEffect(COOKING_3_6_S)) {
			dmg -= 5;
		}
		if (_targetPc.hasSkillEffect(COOKING_1_7_S) // デザートによるダメージ軽減
				|| _targetPc.hasSkillEffect(COOKING_2_7_S)
				|| _targetPc.hasSkillEffect(COOKING_3_7_S)) {
			dmg -= 5;
		}

		if (_targetPc.hasSkillEffect(REDUCTION_ARMOR)) {
			int targetPcLvl = _targetPc.getLevel();
			if (targetPcLvl < 50) {
				targetPcLvl = 50;
			}
			dmg -= (targetPcLvl - 50) / 5 + 1;
		}
		if (_targetPc.hasSkillEffect(DRAGON_SKIN)) {
			dmg -= 2;
		}
		if (_targetPc.hasSkillEffect(PATIENCE)) {
			dmg -= 2;
		}
		if (_targetPc.hasSkillEffect(IMMUNE_TO_HARM)) {
			dmg /= 2;
		}
		// ペット、サモンからプレイヤーに攻撃
		boolean isNowWar = false;
		int castleId = L1CastleLocation.getCastleIdByArea(_targetPc);
		if (castleId > 0) {
			isNowWar = WarTimeController.getInstance().isNowWar(castleId);
		}
		if (!isNowWar) {
			if (_npc instanceof L1PetInstance) {
				dmg /= 8;
			} else if (_npc instanceof L1SummonInstance) {
				L1SummonInstance summon = (L1SummonInstance) _npc;
				if (summon.isExsistMaster()) {
					dmg /= 8;
				}
			}
		}

		if (dmg <= 0) {
			_isHit = false;
		}

		addNpcPoisonAttack(_npc, _targetPc);

		return (int) dmg;
	}

	// ●●●● ＮＰＣ から ＮＰＣ へのダメージ算出 ●●●●
	private int calcNpcNpcDamage() {
		int lvl = _npc.getLevel();
		double dmg = 0;

		if (_npc instanceof L1PetInstance) {
			dmg = Random.nextInt(_npc.getNpcTemplate().get_level())
					+ _npc.getStr() / 2 + 1;
			dmg += (lvl / 16); // ペットはLV16毎に追加打撃
			dmg += ((L1PetInstance) _npc).getDamageByWeapon();
		} else {
			dmg = Random.nextInt(lvl) + _npc.getStr() / 2 + 1;
		}

		if (isUndeadDamage()) {
			dmg *= 1.1;
		}

		dmg = dmg * getLeverage() / 10;

		dmg -= calcNpcDamageReduction();

		if (_npc.isWeaponBreaked()) { // ＮＰＣがウェポンブレイク中。
			dmg /= 2;
		}

		addNpcPoisonAttack(_npc, _targetNpc);

		if (dmg <= 0) {
			_isHit = false;
		}

		return (int) dmg;
	}

	// ●●●● 強化魔法近戰用 ●●●●
	private double calcBuffDamage(double dmg) {
		// 火武器、バーサーカーのダメージは1.5倍しない
		if (_pc.hasSkillEffect(BURNING_SPIRIT)
				|| _pc.hasSkillEffect(ELEMENTAL_FIRE)) {
			if ((Random.nextInt(100) + 1) <= 33) {
				double tempDmg = dmg;
				if (_pc.hasSkillEffect(FIRE_WEAPON)) {
					tempDmg -= 4;
				} else if (_pc.hasSkillEffect(FIRE_BLESS)) {
					tempDmg -= 5;
				} else if (_pc.hasSkillEffect(BURNING_WEAPON)) {
					tempDmg -= 6;
				}
				if (_pc.hasSkillEffect(BERSERKERS)) {
					tempDmg -= 5;
				}
				double diffDmg = dmg - tempDmg;
				dmg = tempDmg * 1.5 + diffDmg;
			}
		}
		// 鎖鏈劍
		if (_weaponType2 == 18) {
			// 弱點曝光 - 傷害加成
			if (_pc.hasSkillEffect(SPECIAL_EFFECT_WEAKNESS_LV3)) {
				dmg += 9;
			} else if (_pc.hasSkillEffect(SPECIAL_EFFECT_WEAKNESS_LV2)) {
				dmg += 6;
			} else if (_pc.hasSkillEffect(SPECIAL_EFFECT_WEAKNESS_LV1)) {
				dmg += 3;
			}
		}
		// 屠宰者 & 弱點曝光LV3 - 傷害 *1.3
		if (_pc.isFoeSlayer()
				&& _pc.hasSkillEffect(SPECIAL_EFFECT_WEAKNESS_LV3)) {
			dmg *= 1.3;
		}
		if (_pc.hasSkillEffect(BURNING_SLASH)) { // 燃燒擊砍
			dmg += 10;
			_pc.sendPackets(new S_EffectLocation(_targetX, _targetY, 6591));
			_pc.broadcastPacket(new S_EffectLocation(_targetX, _targetY, 6591));
			_pc.killSkillEffectTimer(BURNING_SLASH);
		}

		return dmg;
	}

	// ●●●● プレイヤーのＡＣによるダメージ軽減 ●●●●
	private int calcPcDefense() {
		int ac = Math.max(0, 10 - _targetPc.getAc());
		int acDefMax = _targetPc.getClassFeature().getAcDefenseMax(ac);
		return Random.nextInt(acDefMax + 1);
	}

	// ●●●● ＮＰＣのダメージリダクションによる軽減 ●●●●
	private int calcNpcDamageReduction() {
		return _targetNpc.getNpcTemplate().get_damagereduction();
	}

	// ●●●● 武器の材質と祝福による追加ダメージ算出 ●●●●
	private int calcMaterialBlessDmg() {
		int damage = 0;
		int undead = _targetNpc.getNpcTemplate().get_undead();
		if (((_weaponMaterial == 14) || (_weaponMaterial == 17) || (_weaponMaterial == 22))
				&& ((undead == 1) || (undead == 3) || (undead == 5))) { // 銀・ミスリル・オリハルコン、かつ、アンデッド系・アンデッド系ボス・銀特効モンスター
			damage += Random.nextInt(20) + 1;
		} else if (((_weaponMaterial == 17) || (_weaponMaterial == 22))
				&& (undead == 2)) { // ミスリル・オリハルコン、かつ、悪魔系
			damage += Random.nextInt(3) + 1;
		}
		if ((_weaponBless == 0)
				&& ((undead == 1) || (undead == 2) || (undead == 3))) { // 祝福武器、かつ、アンデッド系・悪魔系・アンデッド系ボス
			damage += Random.nextInt(4) + 1;
		}
		if ((_pc.getWeapon() != null) && (_weaponType != 20)
				&& (_weaponType != 62) && (weapon.getHolyDmgByMagic() != 0)
				&& ((undead == 1) || (undead == 3))) {
			damage += weapon.getHolyDmgByMagic();
		}
		return damage;
	}

	// ●●●● 武器の属性強化による追加ダメージ算出 ●●●●
	private int calcAttrEnchantDmg() {
		int damage = 0;
		// int weakAttr = _targetNpc.getNpcTemplate().get_weakAttr();
		// if ((weakAttr & 1) == 1 && _weaponAttrEnchantKind == 1 // 地
		// || (weakAttr & 2) == 2 && _weaponAttrEnchantKind == 2 // 火
		// || (weakAttr & 4) == 4 && _weaponAttrEnchantKind == 4 // 水
		// || (weakAttr & 8) == 8 && _weaponAttrEnchantKind == 8) { // 風
		// damage = _weaponAttrEnchantLevel;
		// }
		if (_weaponAttrEnchantLevel == 1) {
			damage = 1;
		} else if (_weaponAttrEnchantLevel == 2) {
			damage = 3;
		} else if (_weaponAttrEnchantLevel == 3) {
			damage = 5;
		}

		// XXX 耐性処理は本来、耐性合計値ではなく、各値を個別に処理して総和する。
		int resist = 0;
		if (_calcType == PC_PC) {
			if (_weaponAttrEnchantKind == 1) { // 地
				resist = _targetPc.getEarth();
			} else if (_weaponAttrEnchantKind == 2) { // 火
				resist = _targetPc.getFire();
			} else if (_weaponAttrEnchantKind == 4) { // 水
				resist = _targetPc.getWater();
			} else if (_weaponAttrEnchantKind == 8) { // 風
				resist = _targetPc.getWind();
			}
		} else if (_calcType == PC_NPC) {
			int weakAttr = _targetNpc.getNpcTemplate().get_weakAttr();
			if (((_weaponAttrEnchantKind == 1) && (weakAttr == 1)) // 地
					|| ((_weaponAttrEnchantKind == 2) && (weakAttr == 2)) // 火
					|| ((_weaponAttrEnchantKind == 4) && (weakAttr == 4)) // 水
					|| ((_weaponAttrEnchantKind == 8) && (weakAttr == 8))) { // 風
				resist = -50;
			}
		}

		int resistFloor = (int) (0.32 * Math.abs(resist));
		if (resist >= 0) {
			resistFloor *= 1;
		} else {
			resistFloor *= -1;
		}

		double attrDeffence = resistFloor / 32.0;
		double attrCoefficient = 1 - attrDeffence;

		damage *= attrCoefficient;

		return damage;
	}

	// ●●●● ＮＰＣのアンデッドの夜間攻撃力の変化 ●●●●
	private boolean isUndeadDamage() {
		boolean flag = false;
		int undead = _npc.getNpcTemplate().get_undead();
		boolean isNight = L1GameTimeClock.getInstance().currentTime().isNight();
		if (isNight && ((undead == 1) || (undead == 3) || (undead == 4))) { // 18～6時、かつ、アンデッド系・アンデッド系ボス・弱点無効のアンデッド系
			flag = true;
		}
		return flag;
	}

	// ●●●● ＮＰＣの毒攻撃を付加 ●●●●
	private void addNpcPoisonAttack(L1Character attacker, L1Character target) {
		if (_npc.getNpcTemplate().get_poisonatk() != 0) { // 毒攻撃あり
			if (15 >= Random.nextInt(100) + 1) { // 15%の確率で毒攻撃
				if (_npc.getNpcTemplate().get_poisonatk() == 1) { // 通常毒
					// 3秒周期でダメージ5
					L1DamagePoison.doInfection(attacker, target, 3000, 5);
				} else if (_npc.getNpcTemplate().get_poisonatk() == 2) { // 沈黙毒
					L1SilencePoison.doInfection(target);
				} else if (_npc.getNpcTemplate().get_poisonatk() == 4) { // 麻痺毒
					// 20秒後に45秒間麻痺
					L1ParalysisPoison.doInfection(target, 20000, 45000);
				}
			}
		} else if (_npc.getNpcTemplate().get_paralysisatk() != 0) { // 麻痺攻撃あり
		}
	}

	// ■■■■ マナスタッフ、鋼鉄のマナスタッフ、マナバーラードのMP吸収量算出 ■■■■
	public void calcStaffOfMana() {
		if ((_weaponId == 126) || (_weaponId == 127)) { // SOMまたは鋼鉄のSOM
			int som_lvl = _weaponEnchant + 3; // 最大MP吸収量を設定
			if (som_lvl < 0) {
				som_lvl = 0;
			}
			// MP吸収量をランダム取得
			_drainMana = Random.nextInt(som_lvl) + 1;
			// 最大MP吸収量を9に制限
			if (_drainMana > Config.MANA_DRAIN_LIMIT_PER_SOM_ATTACK) {
				_drainMana = Config.MANA_DRAIN_LIMIT_PER_SOM_ATTACK;
			}
		} else if (_weaponId == 259) { // マナバーラード
			if (_calcType == PC_PC) {
				if (_targetPc.getMr() <= Random.nextInt(100) + 1) { // 確率はターゲットのMRに依存
					_drainMana = 1; // 吸収量は1固定
				}
			} else if (_calcType == PC_NPC) {
				if (_targetNpc.getMr() <= Random.nextInt(100) + 1) { // 確率はターゲットのMRに依存
					_drainMana = 1; // 吸収量は1固定
				}
			}
		}
	}

	// ■■■■ ディストラクションのHP吸収量算出 ■■■■
	private int calcDestruction(int dmg) {
		_drainHp = (dmg / 8) + 1;
		return _drainHp > 0 ? _drainHp : 1;
	}

	// ■■■■ ＰＣの毒攻撃を付加 ■■■■
	public void addPcPoisonAttack(L1Character attacker, L1Character target) {
		int chance = Random.nextInt(100) + 1;
		if (((_weaponId == 13) || (_weaponId == 44 // FOD、古代のダークエルフソード
				) || ((_weaponId != 0) && _pc.hasSkillEffect(ENCHANT_VENOM))) // エンチャント
																				// ベノム中
				&& (chance <= 10)) {
			// 通常毒、3秒周期、ダメージHP-5
			L1DamagePoison.doInfection(attacker, target, 3000, 5);
		} else {
			// 魔法娃娃效果 - 中毒
			if (L1MagicDoll.getEffectByDoll(attacker, (byte) 1) == 1) {
				L1DamagePoison.doInfection(attacker, target, 3000, 5);
			}
		}
	}

	// ■■■■ 底比斯武器攻撃付加 ■■■■
	public void addChaserAttack() {
		if (5 > Random.nextInt(100) + 1) {
			if (_weaponId == 265 || _weaponId == 266 || _weaponId == 267
					|| _weaponId == 268 || _weaponId == 280 || _weaponId == 281) {
				L1Chaser chaser = new L1Chaser(_pc, _target,
						L1Skills.ATTR_EARTH, 7025);
				chaser.begin();
			} else if (_weaponId == 276 || _weaponId == 277) { 
				L1Chaser chaser = new L1Chaser(_pc, _target,
						L1Skills.ATTR_WATER, 7179);
				chaser.begin();
			} else if (_weaponId == 304 || _weaponId == 307 || _weaponId == 308) { 
				L1Chaser chaser = new L1Chaser(_pc, _target,
						L1Skills.ATTR_WATER, 8150);
				chaser.begin();
			} else if (_weaponId == 305 || _weaponId == 306 || _weaponId == 309) { 
				L1Chaser chaser = new L1Chaser(_pc, _target,
						L1Skills.ATTR_WATER, 8152);
				chaser.begin();
			}
		}
	}

	/* ■■■■■■■■■■■■■■ 攻撃モーション送信 ■■■■■■■■■■■■■■ */
	public void action() {
		if (_calcType == PC_PC || _calcType == PC_NPC) {
			actionPc();
		} else if (_calcType == NPC_PC || _calcType == NPC_NPC) {
			actionNpc();
		}
	}

	// ●●●● ＰＣ攻擊動作 ●●●●
	public void actionPc() {
		_attckActId = 1;
		boolean isFly = false;
		_pc.setHeading(_pc.targetDirection(_targetX, _targetY)); // 改變面向

		if (_weaponType == 20 && (_arrow != null || _weaponId == 190)) { // 弓 有箭或沙哈之弓
			if (_arrow != null) { // 弓 - 有箭
				_pc.getInventory().removeItem(_arrow, 1);
				_attckGrfxId = 66; // 箭
			} else if (_weaponId == 190)  // 沙哈 - 無箭
				_attckGrfxId = 2349; // 魔法箭
			
			if (_pc.getTempCharGfx() == 8719)  // 柑橘
				_attckGrfxId = 8721; // 橘子籽
			
			if (_pc.getTempCharGfx() == 8900)  // 海露拜
				_attckGrfxId = 8904; // 魔法箭

			if (_pc.getTempCharGfx() == 8913)  // 朱里安
				_attckGrfxId = 8916; // 魔法箭
			
			isFly = true;
		} else if ((_weaponType == 62) && (_sting != null)) { // 鐵手甲 - 有飛刀
			_pc.getInventory().removeItem(_sting, 1);
			_attckGrfxId = 2989; // 飛刀
			isFly = true;
		}

		if (!_isHit) { // Miss
			_damage = 0;
		}

		int[] data = null;

		if (isFly) { // 遠距離攻擊
			data = new int[] { _attckActId, _damage, _attckGrfxId };
			_pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, _targetX,
					_targetY, data));
			_pc.broadcastPacket(new S_UseArrowSkill(_pc, _targetId, _targetX,
					_targetY, data));
		} else { // 近距離攻擊
			data = new int[] { _attckActId, _damage, _effectId };
			_pc.sendPackets(new S_AttackPacket(_pc, _targetId, data));
			_pc.broadcastPacket(new S_AttackPacket(_pc, _targetId, data));
		}

		if (_isHit) {
			_target.broadcastPacketExceptTargetSight(new S_DoActionGFX(
					_targetId, ActionCodes.ACTION_Damage), _pc);
		}
	}

	// ●●●● ＮＰＣ攻擊動作 ●●●●
	private void actionNpc() {
		int bowActId = 0;
		int npcGfxid = _npc.getTempCharGfx();
		int actId = L1NpcDefaultAction.getInstance().getSpecialAttack(npcGfxid); // 特殊攻擊動作
		double dmg = _damage;
		int[] data = null;

		_npc.setHeading(_npc.targetDirection(_targetX, _targetY)); // 改變面向

		// 與目標距離2格以上
		boolean isLongRange = false;
		if (npcGfxid == 4521 || npcGfxid == 4550 || npcGfxid == 5062 || npcGfxid == 5317
				|| npcGfxid == 5324 || npcGfxid == 5331 || npcGfxid == 5338 || npcGfxid == 5412) {
			isLongRange = (_npc.getLocation().getTileLineDistance(
					new Point(_targetX, _targetY)) > 2);
		} else {
			isLongRange = (_npc.getLocation().getTileLineDistance(
					new Point(_targetX, _targetY)) > 1);
		}
		bowActId = _npc.getPolyArrowGfx(); // 被變身後的遠距圖像
		if (bowActId == 0) {
			bowActId = _npc.getNpcTemplate().getBowActId();
		}
		if (getActId() == 0) {
			if ((actId != 0) && ((Random.nextInt(100) + 1) <= 40)) {
				dmg *= 1.2;
			} else {
				if (!isLongRange || bowActId == 0) { // 近距離
					actId = L1NpcDefaultAction.getInstance().getDefaultAttack(npcGfxid);
					if (bowActId > 0) { // 遠距離怪物，近距離時攻擊力加成
						dmg *= 1.2;
					}
				} else { // 遠距離
					actId = L1NpcDefaultAction.getInstance().getRangedAttack(npcGfxid);
				}
			}
		} else {
			actId = getActId(); // 攻擊動作由 mobskill控制
		}
		_damage = (int) dmg;

		if (!_isHit) { // Miss
			_damage = 0;
		}

		// 距離2格以上攻使用 弓 攻擊
		if (isLongRange && (bowActId > 0)) {
			data = new int[] { actId, _damage, bowActId }; // data = {actid,
															// dmg, spellgfx}
			_npc.broadcastPacket(new S_UseArrowSkill(_npc, _targetId, _targetX,
					_targetY, data));
		} else {
			if (getGfxId() > 0) {
				data = new int[] { actId, _damage, getGfxId(), 6 }; // data =
																	// {actid,
																	// dmg,
																	// spellgfx,
																	// use_type}
				_npc.broadcastPacket(new S_UseAttackSkill(_npc, _targetId,
						_targetX, _targetY, data));
			} else {
				data = new int[] { actId, _damage, 0 }; // data = {actid, dmg,
														// effect}
				_npc.broadcastPacket(new S_AttackPacket(_npc, _targetId, data));
			}
		}
		if (_isHit) {
			_target.broadcastPacketExceptTargetSight(new S_DoActionGFX(
					_targetId, ActionCodes.ACTION_Damage), _npc);
		}
	}

	/*
	 * // 飛び道具（矢、スティング）がミスだったときの軌道を計算 public void calcOrbit(int cx, int cy, int
	 * head) // 起点Ｘ 起点Ｙ 今向いてる方向 { float dis_x = Math.abs(cx - _targetX); //
	 * Ｘ方向のターゲットまでの距離 float dis_y = Math.abs(cy - _targetY); // Ｙ方向のターゲットまでの距離
	 * float dis = Math.max(dis_x, dis_y); // ターゲットまでの距離 float avg_x = 0; float
	 * avg_y = 0; if (dis == 0) { // 目標と同じ位置なら向いてる方向へ真っ直ぐ if (head == 1) { avg_x
	 * = 1; avg_y = -1; } else if (head == 2) { avg_x = 1; avg_y = 0; } else if
	 * (head == 3) { avg_x = 1; avg_y = 1; } else if (head == 4) { avg_x = 0;
	 * avg_y = 1; } else if (head == 5) { avg_x = -1; avg_y = 1; } else if (head
	 * == 6) { avg_x = -1; avg_y = 0; } else if (head == 7) { avg_x = -1; avg_y
	 * = -1; } else if (head == 0) { avg_x = 0; avg_y = -1; } } else { avg_x =
	 * dis_x / dis; avg_y = dis_y / dis; }
	 * 
	 * int add_x = (int) Math.floor((avg_x * 15) + 0.59f); // 上下左右がちょっと優先な丸め int
	 * add_y = (int) Math.floor((avg_y * 15) + 0.59f); // 上下左右がちょっと優先な丸め
	 * 
	 * if (cx > _targetX) { add_x *= -1; } if (cy > _targetY) { add_y *= -1; }
	 * 
	 * _targetX = _targetX + add_x; _targetY = _targetY + add_y; }
	 */

	/* ■■■■■■■■■■■■■■■ 計算結果反映 ■■■■■■■■■■■■■■■ */

	public void commit() {
		if (_isHit) {
			if ((_calcType == PC_PC) || (_calcType == NPC_PC)) {
				commitPc();
			} else if ((_calcType == PC_NPC) || (_calcType == NPC_NPC)) {
				commitNpc();
			}
		}

		// ダメージ値及び命中率確認用メッセージ
		if (!Config.ALT_ATKMSG) {
			return;
		}
		if (Config.ALT_ATKMSG) {
			if (((_calcType == PC_PC) || (_calcType == PC_NPC)) && !_pc.isGm()) {
				return;
			}
			if (((_calcType == PC_PC) || (_calcType == NPC_PC))
					&& !_targetPc.isGm()) {
				return;
			}
		}
		String msg0 = "";
		String msg1 = " 造成 ";
		String msg2 = "";
		String msg3 = "";
		String msg4 = "";
		if ((_calcType == PC_PC) || (_calcType == PC_NPC)) { // アタッカーがＰＣの場合
			msg0 = "物攻 對";
		} else if (_calcType == NPC_PC) { // アタッカーがＮＰＣの場合
			msg0 = _npc.getNameId() + "(物攻)：";
		}

		if ((_calcType == NPC_PC) || (_calcType == PC_PC)) { // ターゲットがＰＣの場合
			msg4 = _targetPc.getName();
			msg2 = "，剩餘 " + _targetPc.getCurrentHp() + "，命中	" + _hitRate + "%";
		} else if (_calcType == PC_NPC) { // ターゲットがＮＰＣの場合
			msg4 = _targetNpc.getNameId();
			msg2 = "，剩餘 " + _targetNpc.getCurrentHp() + "，命中 " + _hitRate + "%";
		}
		msg3 = _isHit ? _damage + " 傷害" : "  0 傷害";

		// 物攻 對 目標 造成 X 傷害，剩餘 Y，命中 Z %。
		if ((_calcType == PC_PC) || (_calcType == PC_NPC)) {
			_pc.sendPackets(new S_ServerMessage(166, msg0, msg1, msg2, msg3,
					msg4));
		}
		// 攻擊者(物攻)： X傷害，剩餘 Y，命中%。
		else if ((_calcType == NPC_PC)) {
			_targetPc.sendPackets(new S_ServerMessage(166, msg0, null, msg2,
					msg3, null));
		}
	}

	// ●●●● プレイヤーに計算結果を反映 ●●●●
	private void commitPc() {
		if (_calcType == PC_PC) {
			if ((_drainMana > 0) && (_targetPc.getCurrentMp() > 0)) {
				if (_drainMana > _targetPc.getCurrentMp()) {
					_drainMana = _targetPc.getCurrentMp();
				}
				short newMp = (short) (_targetPc.getCurrentMp() - _drainMana);
				_targetPc.setCurrentMp(newMp);
				newMp = (short) (_pc.getCurrentMp() + _drainMana);
				_pc.setCurrentMp(newMp);
			}
			if (_drainHp > 0) { // HP吸収による回復
				short newHp = (short) (_pc.getCurrentHp() + _drainHp);
				_pc.setCurrentHp(newHp);
			}
			damagePcWeaponDurability(); // 武器を損傷させる。
			_targetPc.receiveDamage(_pc, _damage, false);
		} else if (_calcType == NPC_PC) {
			_targetPc.receiveDamage(_npc, _damage, false);
		}
	}

	// ●●●● ＮＰＣに計算結果を反映 ●●●●
	private void commitNpc() {
		if (_calcType == PC_NPC) {
			if (_drainMana > 0) {
				int drainValue = _targetNpc.drainMana(_drainMana);
				int newMp = _pc.getCurrentMp() + drainValue;
				_pc.setCurrentMp(newMp);
				if (drainValue > 0) {
					int newMp2 = _targetNpc.getCurrentMp() - drainValue;
					_targetNpc.setCurrentMpDirect(newMp2);
				}
			}
			if (_drainHp > 0) { // HP吸収による回復
				short newHp = (short) (_pc.getCurrentHp() + _drainHp);
				_pc.setCurrentHp(newHp);
			}
			damageNpcWeaponDurability(); // 武器を損傷させる。
			_targetNpc.receiveDamage(_pc, _damage);
		} else if (_calcType == NPC_NPC) {
			_targetNpc.receiveDamage(_npc, _damage);
		}
	}

	/* ■■■■■■■■■■■■■■■ カウンターバリア ■■■■■■■■■■■■■■■ */

	// ■■■■ カウンターバリア時の攻撃モーション送信 ■■■■
	public void actionCounterBarrier() {
		if (_calcType == PC_PC) {
			_pc.setHeading(_pc.targetDirection(_targetX, _targetY)); // 向きのセット
			_pc.sendPackets(new S_AttackMissPacket(_pc, _targetId));
			_pc.broadcastPacket(new S_AttackMissPacket(_pc, _targetId));
			_pc.sendPackets(new S_DoActionGFX(_pc.getId(),
					ActionCodes.ACTION_Damage));
			_pc.broadcastPacket(new S_DoActionGFX(_pc.getId(),
					ActionCodes.ACTION_Damage));
		} else if (_calcType == NPC_PC) {
			int actId = 0;
			_npc.setHeading(_npc.targetDirection(_targetX, _targetY)); // 向きのセット
			if (getActId() > 0) {
				actId = getActId();
			} else {
				actId = ActionCodes.ACTION_Attack;
			}
			if (getGfxId() > 0) {
				int[] data = { actId, 0, getGfxId(), 6 }; // data = {actId, dmg, getGfxId(), use_type}
				_npc.broadcastPacket(new S_UseAttackSkill(_target,
						_npc.getId(), _targetX, _targetY, data));
			} else {
				_npc.broadcastPacket(new S_AttackMissPacket(_npc, _targetId,
						actId));
			}
			_npc.broadcastPacket(new S_DoActionGFX(_npc.getId(),
					ActionCodes.ACTION_Damage));
		}
	}

	// ■■■■ 相手の攻撃に対してカウンターバリアが有効かを判別 ■■■■
	public boolean isShortDistance() {
		boolean isShortDistance = true;
		if (_calcType == PC_PC) {
			if ((_weaponType == 20) || (_weaponType == 62)) { // 弓かガントレット
				isShortDistance = false;
			}
		} else if (_calcType == NPC_PC) {
			boolean isLongRange = (_npc.getLocation().getTileLineDistance(
					new Point(_targetX, _targetY)) > 1);
			int bowActId = _npc.getPolyArrowGfx();
			if (bowActId == 0) {
				bowActId = _npc.getNpcTemplate().getBowActId();
			}
			// 距離が2以上、攻撃者の弓のアクションIDがある場合は遠攻撃
			if (isLongRange && (bowActId > 0)) {
				isShortDistance = false;
			}
		}
		return isShortDistance;
	}

	// ■■■■ カウンターバリアのダメージを反映 ■■■■
	public void commitCounterBarrier() {
		int damage = calcCounterBarrierDamage();
		if (damage == 0) {
			return;
		}
		if (_calcType == PC_PC) {
			_pc.receiveDamage(_targetPc, damage, false);
		} else if (_calcType == NPC_PC) {
			_npc.receiveDamage(_targetPc, damage);
		}
	}

	// ●●●● カウンターバリアのダメージを算出 ●●●●
	private int calcCounterBarrierDamage() {
		int damage = 0;
		L1ItemInstance weapon = null;
		weapon = _targetPc.getWeapon();
		if (weapon != null) {
			if (weapon.getItem().getType() == 3) { // 両手剣
				// (BIG最大ダメージ+強化数+追加ダメージ)*2
				damage = (weapon.getItem().getDmgLarge()
						+ weapon.getEnchantLevel() + weapon.getItem()
						.getDmgModifier()) * 2;
			}
		}
		return damage;
	}

	/*
	 * 武器を損傷させる。 対NPCの場合、損傷確率は10%とする。祝福武器は3%とする。
	 */
	private void damageNpcWeaponDurability() {
		int chance = 10;
		int bchance = 3;

		/*
		 * 損傷しないNPC、素手、損傷しない武器使用、SOF中の場合何もしない。
		 */
		if ((_calcType != PC_NPC)
				|| (_targetNpc.getNpcTemplate().is_hard() == false)
				|| (_weaponType == 0) || (weapon.getItem().get_canbedmg() == 0)
				|| _pc.hasSkillEffect(SOUL_OF_FLAME)) {
			return;
		}
		// 通常の武器・呪われた武器
		if (((_weaponBless == 1) || (_weaponBless == 2))
				&& ((Random.nextInt(100) + 1) < chance)) {
			// \f1あなたの%0が損傷しました。
			_pc.sendPackets(new S_ServerMessage(268, weapon.getLogName()));
			_pc.getInventory().receiveDamage(weapon);
		}
		// 祝福された武器
		if ((_weaponBless == 0) && ((Random.nextInt(100) + 1) < bchance)) {
			// \f1あなたの%0が損傷しました。
			_pc.sendPackets(new S_ServerMessage(268, weapon.getLogName()));
			_pc.getInventory().receiveDamage(weapon);
		}
	}

	/*
	 * バウンスアタックにより武器を損傷させる。 バウンスアタックの損傷確率は10%
	 */
	private void damagePcWeaponDurability() {
		// PvP以外、素手、弓、ガントトレット、相手がバウンスアタック未使用、SOF中の場合何もしない
		if ((_calcType != PC_PC) || (_weaponType == 0) || (_weaponType == 20)
				|| (_weaponType == 62)
				|| (_targetPc.hasSkillEffect(BOUNCE_ATTACK) == false)
				|| _pc.hasSkillEffect(SOUL_OF_FLAME)) {
			return;
		}

		if (Random.nextInt(100) + 1 <= 10) {
			// \f1あなたの%0が損傷しました。
			_pc.sendPackets(new S_ServerMessage(268, weapon.getLogName()));
			_pc.getInventory().receiveDamage(weapon);
		}
	}

	/** 弱點曝光 */
	private void WeaknessExposure() {
		if (weapon != null) {
			int random = Random.nextInt(100) + 1;
			if (_weaponType2 == 18) { // 鎖鏈劍
				// 使用屠宰者...
				if (_pc.isFoeSlayer()) {
					return;
				}
				if (_pc.hasSkillEffect(SPECIAL_EFFECT_WEAKNESS_LV3)) { // 目前階段三
					if (random > 30 && random <= 60) { // 階段三
						_pc.killSkillEffectTimer(SPECIAL_EFFECT_WEAKNESS_LV3);
						_pc.setSkillEffect(SPECIAL_EFFECT_WEAKNESS_LV3,
								16 * 1000);
						_pc.sendPackets(new S_SkillIconGFX(75, 3));
					}
				} else if (_pc.hasSkillEffect(SPECIAL_EFFECT_WEAKNESS_LV2)) { // 目前階段二
					if (random <= 30) { // 階段二
						_pc.killSkillEffectTimer(SPECIAL_EFFECT_WEAKNESS_LV2);
						_pc.setSkillEffect(SPECIAL_EFFECT_WEAKNESS_LV2,
								16 * 1000);
						_pc.sendPackets(new S_SkillIconGFX(75, 2));
					} else if (random >= 70) { // 階段三
						_pc.killSkillEffectTimer(SPECIAL_EFFECT_WEAKNESS_LV2);
						_pc.setSkillEffect(SPECIAL_EFFECT_WEAKNESS_LV3,
								16 * 1000);
						_pc.sendPackets(new S_SkillIconGFX(75, 3));
					}
				} else if (_pc.hasSkillEffect(SPECIAL_EFFECT_WEAKNESS_LV1)) { // 目前階段一
					if (random <= 40) { // 階段一
						_pc.killSkillEffectTimer(SPECIAL_EFFECT_WEAKNESS_LV1);
						_pc.setSkillEffect(SPECIAL_EFFECT_WEAKNESS_LV1,
								16 * 1000);
						_pc.sendPackets(new S_SkillIconGFX(75, 1));
					} else if (random >= 70) { // 階段二
						_pc.killSkillEffectTimer(SPECIAL_EFFECT_WEAKNESS_LV1);
						_pc.setSkillEffect(SPECIAL_EFFECT_WEAKNESS_LV2,
								16 * 1000);
						_pc.sendPackets(new S_SkillIconGFX(75, 2));
					}
				} else {
					if (random <= 40) { // 階段一
						_pc.setSkillEffect(SPECIAL_EFFECT_WEAKNESS_LV1,
								16 * 1000);
						_pc.sendPackets(new S_SkillIconGFX(75, 1));
					}
				}
			}
		}
	}
}
