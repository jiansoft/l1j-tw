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

import static l1j.server.server.model.skill.L1SkillId.BLIND_HIDING;
import static l1j.server.server.model.skill.L1SkillId.GMSTATUS_FINDINVIS;
import static l1j.server.server.model.skill.L1SkillId.INVISIBILITY;
import static l1j.server.server.model.skill.L1SkillId.LIGHT;
import static l1j.server.server.model.skill.L1SkillId.STATUS_CURSE_BARLOG;
import static l1j.server.server.model.skill.L1SkillId.STATUS_CURSE_YAHEE;
import static l1j.server.server.model.skill.L1SkillId.STATUS_HOLY_MITHRIL_POWDER;
import static l1j.server.server.model.skill.L1SkillId.STATUS_HOLY_WATER;
import static l1j.server.server.model.skill.L1SkillId.STATUS_HOLY_WATER_OF_EVA;
import static l1j.server.server.model.skill.L1SkillId.SECRET_MEDICINE_OF_DESTRUCTION;

import java.util.List;
import java.util.Map;

import l1j.server.server.model.Instance.L1DollInstance;
import l1j.server.server.model.Instance.L1FollowerInstance;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1PetInstance;
import l1j.server.server.model.Instance.L1SummonInstance;
import l1j.server.server.model.poison.L1Poison;
import l1j.server.server.model.skill.L1SkillTimer;
import l1j.server.server.model.skill.L1SkillTimerCreator;
import l1j.server.server.serverpackets.S_Light;
import l1j.server.server.serverpackets.S_PetCtrlMenu;
import l1j.server.server.serverpackets.S_Poison;
import l1j.server.server.serverpackets.S_RemoveObject;
import l1j.server.server.serverpackets.ServerBasePacket;
import l1j.server.server.templates.L1MagicDoll;
import l1j.server.server.types.Point;
import l1j.server.server.utils.IntRange;
import l1j.server.server.utils.collections.Lists;
import l1j.server.server.utils.collections.Maps;

import java.util.logging.Logger;

// Referenced classes of package l1j.server.server.model:
// L1Object, Die, L1PcInstance, L1MonsterInstance,
// L1World, ActionFailed

/**
 * 角色類別
 * <p>
 * 這是遊戲中所有角色（玩家、NPC、怪物等）的基礎抽象類別。
 * 定義了所有角色共通的屬性和行為，包括生命值、魔力值、狀態效果、移動、戰鬥等核心功能。
 * </p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 * <li><b>屬性管理</b>：HP、MP、力量、敏捷、體質、智力、智慧、魅力</li>
 * <li><b>狀態管理</b>：麻痺、睡眠、中毒、技能效果</li>
 * <li><b>移動系統</b>：位置、方向、移動速度</li>
 * <li><b>戰鬥系統</b>：攻擊力、防禦力、命中、迴避、魔防</li>
 * <li><b>視野管理</b>：視野範圍內物件偵測、封包廣播</li>
 * <li><b>寵物/召喚獸管理</b>：寵物列表、魔法娃娃、跟隨者</li>
 * <li><b>技能效果管理</b>：技能定時器、Buff/Debuff 效果</li>
 * <li><b>物品延遲</b>：物品使用冷卻時間管理</li>
 * </ul>
 *
 * <h3>繼承關係：</h3>
 * <pre>
 * L1Object
 *   └── L1Character
 *         ├── L1PcInstance（玩家）
 *         └── L1NpcInstance（NPC）
 *               ├── L1MonsterInstance（怪物）
 *               ├── L1PetInstance（寵物）
 *               ├── L1SummonInstance（召喚獸）
 *               └── ...（其他 NPC 類型）
 * </pre>
 *
 * <h3>核心概念：</h3>
 * <ul>
 * <li><b>可見性</b>：角色的視野範圍內可以偵測到其他角色和物件</li>
 * <li><b>狀態效果</b>：技能、物品、環境造成的各種 Buff/Debuff</li>
 * <li><b>封包廣播</b>：向視野範圍內的玩家發送狀態更新</li>
 * <li><b>資源管理</b>：HP/MP 的消耗與回復</li>
 * </ul>
 *
 * @see L1Object
 * @see L1PcInstance
 * @see L1NpcInstance
 * @see L1World
 */
public class L1Character extends L1Object {

	private static final long serialVersionUID = 1L;
	private static final Logger _log = Logger.getLogger(L1Character.class.getName());

	/** 中毒狀態物件，管理角色的中毒效果 */
	private L1Poison _poison = null;

	/** 麻痺狀態標記 */
	private boolean _paralyzed;

	/** 睡眠狀態標記 */
	private boolean _sleeped;

	/** 寵物列表，Key: 寵物物件 ID，Value: 寵物實例 */
	private final Map<Integer, L1NpcInstance> _petlist = Maps.newMap();

	/** 魔法娃娃列表，Key: 娃娃物件 ID，Value: 娃娃實例 */
	private final Map<Integer, L1DollInstance> _dolllist = Maps.newMap();

	/** 技能效果列表，Key: 技能 ID，Value: 技能定時器 */
	private final Map<Integer, L1SkillTimer> _skillEffect = Maps.newMap();

	/** 物品使用延遲列表，Key: 延遲類型 ID，Value: 延遲定時器 */
	private final Map<Integer, L1ItemDelay.ItemDelayTimer> _itemdelay = Maps.newMap();

	/** 跟隨者列表，Key: 跟隨者物件 ID，Value: 跟隨者實例 */
	private final Map<Integer, L1FollowerInstance> _followerlist = Maps.newMap();

	public L1Character() {
		_level = 1;
	}

	/**
	 * 復活角色
	 * <p>
	 * 將死亡的角色復活，恢復指定的 HP 並重置狀態。
	 * </p>
	 *
	 * <h4>復活處理：</h4>
	 * <ol>
	 * <li>檢查角色是否已死亡，若未死亡則不執行</li>
	 * <li>設定復活後的 HP（最小為 1）</li>
	 * <li>設定為非死亡狀態</li>
	 * <li>重置狀態為 0</li>
	 * <li>解除變身狀態</li>
	 * <li>向視野內玩家發送移除和更新封包</li>
	 * </ol>
	 *
	 * @param hp 復活後的 HP 值（若 ≤ 0 則設為 1）
	 * @see #isDead()
	 * @see #setDead(boolean)
	 * @see L1PolyMorph#undoPoly(L1Character)
	 */
	public void resurrect(int hp) {
		if (!isDead()) {
			return;
		}
		if (hp <= 0) {
			hp = 1;
		}
		setCurrentHp(hp);
		setDead(false);
		setStatus(0);
		L1PolyMorph.undoPoly(this);
		for (L1PcInstance pc : L1World.getInstance().getRecognizePlayer(this)) {
			pc.sendPackets(new S_RemoveObject(this));
			pc.removeKnownObject(this);
			pc.updateObject();
		}
	}

	/** 當前生命值 */
	private int _currentHp;

	/**
	 * 取得角色當前 HP
	 * @return 當前 HP 值
	 * @see #setCurrentHp(int)
	 */
	public int getCurrentHp() {
		return _currentHp;
	}

	/**
	 * 設定角色 HP
	 * <p>
	 * 設定角色的當前 HP，會自動限制在最大 HP 範圍內。
	 * 子類別可覆寫此方法以實現特殊處理（如發送封包更新）。
	 * </p>
	 *
	 * @param i 新的 HP 值
	 * @see #getCurrentHp()
	 * @see #getMaxHp()
	 */
	public void setCurrentHp(int i) {
		_currentHp = i;
		if (_currentHp >= getMaxHp()) {
			_currentHp = getMaxHp();
		}
	}

	/**
	 * 直接設定角色 HP（不進行上限檢查）
	 * <p>
	 * 直接設定 HP 值，不進行任何驗證或上限檢查。
	 * 用於初始化或特殊情況。
	 * </p>
	 *
	 * @param i 新的 HP 值
	 * @see #setCurrentHp(int)
	 */
	public void setCurrentHpDirect(int i) {
		_currentHp = i;
	}

	/** 當前魔力值 */
	private int _currentMp;

	/**
	 * 取得角色當前 MP
	 * @return 當前 MP 值
	 * @see #setCurrentMp(int)
	 */
	public int getCurrentMp() {
		return _currentMp;
	}

	/**
	 * 設定角色 MP
	 * <p>
	 * 設定角色的當前 MP，會自動限制在最大 MP 範圍內。
	 * 子類別可覆寫此方法以實現特殊處理（如發送封包更新）。
	 * </p>
	 *
	 * @param i 新的 MP 值
	 * @see #getCurrentMp()
	 * @see #getMaxMp()
	 */
	public void setCurrentMp(int i) {
		_currentMp = i;
		if (_currentMp >= getMaxMp()) {
			_currentMp = getMaxMp();
		}
	}

	/**
	 * 直接設定角色 MP（不進行上限檢查）
	 * <p>
	 * 直接設定 MP 值，不進行任何驗證或上限檢查。
	 * 用於初始化或特殊情況。
	 * </p>
	 *
	 * @param i 新的 MP 值
	 * @see #setCurrentMp(int)
	 */
	public void setCurrentMpDirect(int i) {
		_currentMp = i;
	}

	/**
	 * 檢查角色是否處於睡眠狀態
	 * @return true：睡眠中；false：清醒
	 * @see #setSleeped(boolean)
	 */
	public boolean isSleeped() {
		return _sleeped;
	}

	/**
	 * 設定角色睡眠狀態
	 * @param sleeped true：睡眠；false：清醒
	 * @see #isSleeped()
	 */
	public void setSleeped(boolean sleeped) {
		_sleeped = sleeped;
	}

	/**
	 * 檢查角色是否處於麻痺狀態
	 * @return true：麻痺中；false：正常
	 * @see #setParalyzed(boolean)
	 */
	public boolean isParalyzed() {
		return _paralyzed;
	}

	/**
	 * 設定角色麻痺狀態
	 * @param paralyzed true：麻痺；false：正常
	 * @see #isParalyzed()
	 */
	public void setParalyzed(boolean paralyzed) {
		_paralyzed = paralyzed;
	}

	/** 麻痺狀態物件 */
	L1Paralysis _paralysis;

	/**
	 * 取得麻痺狀態物件
	 * @return 麻痺狀態物件
	 * @see L1Paralysis
	 */
	public L1Paralysis getParalysis() {
		return _paralysis;
	}

	/**
	 * 設定麻痺狀態物件
	 * @param p 麻痺狀態物件
	 * @see L1Paralysis
	 */
	public void setParalaysis(L1Paralysis p) {
		_paralysis = p;
	}

	/**
	 * 治癒麻痺狀態
	 * <p>
	 * 若角色有麻痺狀態物件，則調用其治癒方法移除麻痺效果。
	 * </p>
	 * @see L1Paralysis#cure()
	 */
	public void cureParalaysis() {
		if (_paralysis != null) {
			_paralysis.cure();
		}
	}

	/**
	 * 向可視範圍內的玩家廣播封包
	 * <p>
	 * 向角色視野範圍內的所有玩家發送封包。
	 * 會檢查旅館權限，只有持有相同旅館鑰匙的玩家才能收到封包。
	 * </p>
	 *
	 * @param packet 要發送的封包物件
	 * @see ServerBasePacket
	 * @see L1World#getVisiblePlayer(L1Character)
	 */
	public void broadcastPacket(ServerBasePacket packet) {
		for (L1PcInstance pc : L1World.getInstance().getVisiblePlayer(this)) {
			// 旅館內判斷
			if (pc.getMapId() < 16384 || pc.getMapId() > 25088 || pc.getInnKeyId() == getInnKeyId())
				pc.sendPackets(packet);
		}
	}

	/**
	 * 向可視範圍內的玩家廣播封包（排除目標視野範圍）
	 * <p>
	 * 向角色視野範圍內的玩家發送封包，但排除在目標角色視野範圍內的玩家。
	 * </p>
	 *
	 * @param packet 要發送的封包物件
	 * @param target 要排除其視野範圍的目標角色
	 * @see ServerBasePacket
	 * @see L1World#getVisiblePlayerExceptTargetSight(L1Character, L1Character)
	 */
	public void broadcastPacketExceptTargetSight(ServerBasePacket packet,
			L1Character target) {
		for (L1PcInstance pc : L1World.getInstance().getVisiblePlayerExceptTargetSight(this, target)) {
			pc.sendPackets(packet);
		}
	}

	/**
	 * 向可視範圍內能/不能看穿隱形的玩家廣播封包
	 * <p>
	 * 根據玩家是否擁有看穿隱形的能力，選擇性地發送封包。
	 * 用於處理隱形狀態的角色的封包發送。
	 * </p>
	 *
	 * @param packet 要發送的封包物件
	 * @param isFindInvis true：只發給能看穿隱形的玩家；false：只發給不能看穿隱形的玩家
	 * @see ServerBasePacket
	 * @see L1SkillId#GMSTATUS_FINDINVIS
	 */
	public void broadcastPacketForFindInvis(ServerBasePacket packet,
			boolean isFindInvis) {
		for (L1PcInstance pc : L1World.getInstance().getVisiblePlayer(this)) {
			if (isFindInvis) {
				if (pc.hasSkillEffect(GMSTATUS_FINDINVIS)) {
					pc.sendPackets(packet);
				}
			} else {
				if (!pc.hasSkillEffect(GMSTATUS_FINDINVIS)) {
					pc.sendPackets(packet);
				}
			}
		}
	}

	/**
	 * 向 50 格範圍內的玩家廣播封包
	 * <p>
	 * 向角色 50 格範圍內的所有玩家發送封包。
	 * 用於需要較大廣播範圍的情況（如大型技能效果）。
	 * </p>
	 *
	 * @param packet 要發送的封包物件
	 * @see ServerBasePacket
	 * @see L1World#getVisiblePlayer(L1Character, int)
	 */
	public void wideBroadcastPacket(ServerBasePacket packet) {
		for (L1PcInstance pc : L1World.getInstance().getVisiblePlayer(this, 50)) {
			pc.sendPackets(packet);
		}
	}

	/**
	 * 取得角色正面的座標
	 * <p>
	 * 根據角色當前的方向（heading），計算並返回正面一格的座標。
	 * </p>
	 *
	 * @return 包含 [x, y] 座標的陣列
	 * @see #getHeading()
	 */
	public int[] getFrontLoc() {
		int[] loc = new int[2];
		int x = getX();
		int y = getY();
		int heading = getHeading();
		if (heading == 0) {
			y--;
		} else if (heading == 1) {
			x++;
			y--;
		} else if (heading == 2) {
			x++;
		} else if (heading == 3) {
			x++;
			y++;
		} else if (heading == 4) {
			y++;
		} else if (heading == 5) {
			x--;
			y++;
		} else if (heading == 6) {
			x--;
		} else if (heading == 7) {
			x--;
			y--;
		}
		loc[0] = x;
		loc[1] = y;
		return loc;
	}

	/**
	 * 計算面向指定座標的方向
	 * <p>
	 * 計算角色應該面向哪個方向才能朝向目標座標。
	 * 使用距離加權計算，略微偏好上下左右四個主方向。
	 * </p>
	 *
	 * <h4>方向編號：</h4>
	 * <pre>
	 *   0: 上      1: 右上    2: 右
	 *   7: 左上               3: 右下
	 *   6: 左      5: 左下    4: 下
	 * </pre>
	 *
	 * @param tx 目標 X 座標
	 * @param ty 目標 Y 座標
	 * @return 方向編號（0-7）
	 * @see #getHeading()
	 */
	public int targetDirection(int tx, int ty) {
		float dis_x = Math.abs(getX() - tx); // Ｘ方向のターゲットまでの距離
		float dis_y = Math.abs(getY() - ty); // Ｙ方向のターゲットまでの距離
		float dis = Math.max(dis_x, dis_y); // ターゲットまでの距離
		if (dis == 0) {
			return getHeading(); // 同じ位置ならいま向いてる方向を返しとく
		}
		int avg_x = (int) Math.floor((dis_x / dis) + 0.59f); // 上下左右がちょっと優先な丸め
		int avg_y = (int) Math.floor((dis_y / dis) + 0.59f); // 上下左右がちょっと優先な丸め

		int dir_x = 0;
		int dir_y = 0;
		if (getX() < tx) {
			dir_x = 1;
		}
		if (getX() > tx) {
			dir_x = -1;
		}
		if (getY() < ty) {
			dir_y = 1;
		}
		if (getY() > ty) {
			dir_y = -1;
		}

		if (avg_x == 0) {
			dir_x = 0;
		}
		if (avg_y == 0) {
			dir_y = 0;
		}

		if ((dir_x == 1) && (dir_y == -1)) {
			return 1; // 上
		}
		if ((dir_x == 1) && (dir_y == 0)) {
			return 2; // 右上
		}
		if ((dir_x == 1) && (dir_y == 1)) {
			return 3; // 右
		}
		if ((dir_x == 0) && (dir_y == 1)) {
			return 4; // 右下
		}
		if ((dir_x == -1) && (dir_y == 1)) {
			return 5; // 下
		}
		if ((dir_x == -1) && (dir_y == 0)) {
			return 6; // 左下
		}
		if ((dir_x == -1) && (dir_y == -1)) {
			return 7; // 左
		}
		if ((dir_x == 0) && (dir_y == -1)) {
			return 0; // 左上
		}
		return getHeading(); // ここにはこない。はず
	}

	/**
	 * 檢查到目標座標的直線路徑是否無障礙物
	 * <p>
	 * 檢查從當前位置到目標座標的直線路徑上是否有阻擋箭矢通過的障礙物。
	 * 用於判斷遠程攻擊是否有視線（Line of Sight）。
	 * </p>
	 *
	 * <h4>檢查流程：</h4>
	 * <ol>
	 * <li>從當前位置開始，朝目標方向逐格檢查</li>
	 * <li>最多檢查 15 格距離</li>
	 * <li>檢查每格是否允許箭矢通過</li>
	 * <li>若遇到阻礙則返回 false</li>
	 * </ol>
	 *
	 * @param tx 目標 X 座標
	 * @param ty 目標 Y 座標
	 * @return true：路徑無障礙；false：有障礙物
	 * @see L1Map#isArrowPassable(int, int, int)
	 * @see #targetDirection(int, int)
	 */
	public boolean glanceCheck(int tx, int ty) {
		int chx = getX();
		int chy = getY();
		for (int i = 0; i < 15; i++) {
			if (chx == tx && chy == ty) {
				break;
			}

			if (!getMap().isArrowPassable(chx, chy, targetDirection(tx, ty))) {
				return false;
			}

			// Targetへ1タイル進める
			chx += Math.max(-1, Math.min(1, tx - chx));
			chy += Math.max(-1, Math.min(1, ty - chy));
		}
		return true;
	}

	/**
	 * 檢查是否可以攻擊指定座標
	 * <p>
	 * 綜合檢查距離和視線，判斷是否可以對目標座標進行攻擊。
	 * </p>
	 *
	 * <h4>判斷邏輯：</h4>
	 * <ul>
	 * <li><b>遠程武器（range ≥ 7）</b>：使用棋盤距離（允許斜線）</li>
	 * <li><b>近戰武器（range < 7）</b>：使用直線距離</li>
	 * <li>必須通過視線檢查（無障礙物阻擋）</li>
	 * </ul>
	 *
	 * @param x 目標 X 座標
	 * @param y 目標 Y 座標
	 * @param range 攻擊範圍（格數）
	 * @return true：可以攻擊；false：無法攻擊
	 * @see #glanceCheck(int, int)
	 * @see Point#getTileDistance(Point)
	 * @see Point#getTileLineDistance(Point)
	 */
	public boolean isAttackPosition(int x, int y, int range) {
		if (range >= 7) // 遠隔武器（７以上の場合斜めを考慮すると画面外に出る)
		{
			if (getLocation().getTileDistance(new Point(x, y)) > range) {
				return false;
			}
		} else // 近接武器
		{
			if (getLocation().getTileLineDistance(new Point(x, y)) > range) {
				return false;
			}
		}
		return glanceCheck(x, y);
	}

	/**
	 * 取得角色背包
	 * <p>
	 * 返回角色的物品容器。子類別必須覆寫此方法。
	 * </p>
	 *
	 * @return 角色的背包物件（基礎實作返回 null）
	 * @see L1Inventory
	 */
	public L1Inventory getInventory() {
		return null;
	}

	/**
	 * 為角色添加技能效果
	 * <p>
	 * 私有方法，由 {@link #setSkillEffect(int, int)} 調用。
	 * 創建技能定時器並開始計時。
	 * </p>
	 *
	 * @param skillId 技能 ID
	 * @param timeMillis 持續時間（毫秒），0 表示永久
	 * @see #setSkillEffect(int, int)
	 * @see L1SkillTimer
	 */
	private void addSkillEffect(int skillId, int timeMillis) {
		L1SkillTimer timer = null;
		if (0 < timeMillis) {
			timer = L1SkillTimerCreator.create(this, skillId, timeMillis);
			timer.begin();
		}
		_skillEffect.put(skillId, timer);
	}

	/**
	 * 設定技能效果
	 * <p>
	 * 為角色設定技能效果。若技能已存在，則比較剩餘時間，保留較長的效果。
	 * </p>
	 *
	 * <h4>處理邏輯：</h4>
	 * <ul>
	 * <li><b>技能不存在</b>：直接添加新效果</li>
	 * <li><b>技能已存在</b>：
	 *   <ul>
	 *   <li>若新效果更長或為永久（0），則覆蓋舊效果</li>
	 *   <li>若舊效果更長，則保留舊效果</li>
	 *   </ul>
	 * </li>
	 * </ul>
	 *
	 * @param skillId 技能 ID
	 * @param timeMillis 持續時間（毫秒），0 表示永久
	 * @see #addSkillEffect(int, int)
	 * @see #hasSkillEffect(int)
	 * @see #getSkillEffectTimeSec(int)
	 */
	public void setSkillEffect(int skillId, int timeMillis) {
		if (hasSkillEffect(skillId)) {
			int remainingTimeMills = getSkillEffectTimeSec(skillId) * 1000;

			// 残り時間が有限で、パラメータの効果時間の方が長いか無限の場合は上書きする。
			if ((remainingTimeMills >= 0)
					&& ((remainingTimeMills < timeMillis) || (timeMillis == 0))) {
				killSkillEffectTimer(skillId);
				addSkillEffect(skillId, timeMillis);
			}
		} else {
			addSkillEffect(skillId, timeMillis);
		}
	}

	/**
	 * 移除技能效果
	 * <p>
	 * 從角色身上移除指定的技能效果，並結束其定時器。
	 * </p>
	 *
	 * @param skillId 要移除的技能 ID
	 * @see #setSkillEffect(int, int)
	 * @see L1SkillTimer#end()
	 */
	public void removeSkillEffect(int skillId) {
		L1SkillTimer timer = _skillEffect.remove(skillId);
		if (timer != null) {
			timer.end();
		}
	}

	/**
	 * 刪除技能效果定時器（但保留效果）
	 * <p>
	 * 從角色身上移除技能定時器，但不觸發效果結束處理。
	 * 用於特殊情況下需要保留效果但停止計時的場合。
	 * </p>
	 *
	 * @param skillId 要刪除定時器的技能 ID
	 * @see #removeSkillEffect(int)
	 * @see L1SkillTimer#kill()
	 */
	public void killSkillEffectTimer(int skillId) {
		L1SkillTimer timer = _skillEffect.remove(skillId);
		if (timer != null) {
			timer.kill();
		}
	}

	/**
	 * 清除所有技能效果定時器
	 * <p>
	 * 停止並清除所有技能效果的定時器，但不觸發效果結束處理。
	 * 通常用於角色下線或特殊狀態重置。
	 * </p>
	 *
	 * @see #killSkillEffectTimer(int)
	 */
	public void clearSkillEffectTimer() {
		for (L1SkillTimer timer : _skillEffect.values()) {
			if (timer != null) {
				timer.kill();
			}
		}
		_skillEffect.clear();
	}

	/**
	 * 檢查角色是否擁有指定技能效果
	 *
	 * @param skillId 要檢查的技能 ID
	 * @return true：有該效果；false：沒有該效果
	 * @see #setSkillEffect(int, int)
	 */
	public boolean hasSkillEffect(int skillId) {
		return _skillEffect.containsKey(skillId);
	}

	/**
	 * 取得技能效果剩餘時間
	 *
	 * @param skillId 要查詢的技能 ID
	 * @return 剩餘時間（秒），若技能不存在或為永久效果則返回 -1
	 * @see #hasSkillEffect(int)
	 * @see L1SkillTimer#getRemainingTime()
	 */
	public int getSkillEffectTimeSec(int skillId) {
		L1SkillTimer timer = _skillEffect.get(skillId);
		if (timer == null) {
			return -1;
		}
		return timer.getRemainingTime();
	}

	private boolean _isSkillDelay = false;

	/**
	 * キャラクターへ、スキルディレイを追加する。
	 * 
	 * @param flag
	 */
	public void setSkillDelay(boolean flag) {
		_isSkillDelay = flag;
	}

	/**
	 * キャラクターの毒状態を返す。
	 * 
	 * @return スキルディレイ中か。
	 */
	public boolean isSkillDelay() {
		return _isSkillDelay;
	}

	/**
	 * キャラクターへ、アイテムディレイを追加する。
	 * 
	 * @param delayId
	 *            アイテムディレイID。 通常のアイテムであれば0、インビジビリティ クローク、バルログ ブラッディ クロークであれば1。
	 * @param timer
	 *            ディレイ時間を表す、L1ItemDelay.ItemDelayTimerオブジェクト。
	 */
	public void addItemDelay(int delayId, L1ItemDelay.ItemDelayTimer timer) {
		_itemdelay.put(delayId, timer);
	}

	/**
	 * キャラクターから、アイテムディレイを削除する。
	 * 
	 * @param delayId
	 *            アイテムディレイID。 通常のアイテムであれば0、インビジビリティ クローク、バルログ ブラッディ クロークであれば1。
	 */
	public void removeItemDelay(int delayId) {
		_itemdelay.remove(delayId);
	}

	/**
	 * キャラクターに、アイテムディレイがあるかを返す。
	 * 
	 * @param delayId
	 *            調べるアイテムディレイID。 通常のアイテムであれば0、インビジビリティ クローク、バルログ ブラッディ
	 *            クロークであれば1。
	 * @return アイテムディレイがあればtrue、なければfalse。
	 */
	public boolean hasItemDelay(int delayId) {
		return _itemdelay.containsKey(delayId);
	}

	/**
	 * キャラクターのアイテムディレイ時間を表す、L1ItemDelay.ItemDelayTimerを返す。
	 * 
	 * @param delayId
	 *            調べるアイテムディレイID。 通常のアイテムであれば0、インビジビリティ クローク、バルログ ブラッディ
	 *            クロークであれば1。
	 * @return アイテムディレイ時間を表す、L1ItemDelay.ItemDelayTimer。
	 */
	public L1ItemDelay.ItemDelayTimer getItemDelayTimer(int delayId) {
		return _itemdelay.get(delayId);
	}

	/**
	 * キャラクターへ、新たにペット、サモンモンスター、テイミングモンスター、あるいはクリエイトゾンビを追加する。
	 * 
	 * @param npc
	 *            追加するNpcを表す、L1NpcInstanceオブジェクト。
	 */
	public void addPet(L1NpcInstance npc) {
		_petlist.put(npc.getId(), npc);
		sendPetCtrlMenu(npc, true);// 顯示寵物控制圖形介面
	}

	/**
	 * キャラクターから、ペット、サモンモンスター、テイミングモンスター、あるいはクリエイトゾンビを削除する。
	 * 
	 * @param npc
	 *            削除するNpcを表す、L1NpcInstanceオブジェクト。
	 */
	public void removePet(L1NpcInstance npc) {
		_petlist.remove(npc.getId());
		sendPetCtrlMenu(npc, false);// 關閉寵物控制圖形介面
	}

	/**
	 * 3.3C PetMenu 控制
	 * 
	 * @param npc
	 * @param type
	 *            1:顯示 0:關閉
	 */
	public void sendPetCtrlMenu(L1NpcInstance npc, boolean type) {
		if (npc instanceof L1PetInstance) {
			L1PetInstance pet = (L1PetInstance) npc;
			L1Character cha = pet.getMaster();
			if (cha instanceof L1PcInstance) {
				L1PcInstance pc = (L1PcInstance) cha;
				pc.sendPackets(new S_PetCtrlMenu(cha, npc, type));
				// 處理寵物控制圖形介面
			}
		} else if (npc instanceof L1SummonInstance) {
			L1SummonInstance summon = (L1SummonInstance) npc;
			L1Character cha = summon.getMaster();
			if (cha instanceof L1PcInstance) {
				L1PcInstance pc = (L1PcInstance) cha;
				pc.sendPackets(new S_PetCtrlMenu(cha, npc, type));
			}
		}
	}

	/**
	 * キャラクターのペットリストを返す。
	 * 
	 * @return 
	 *         キャラクターのペットリストを表す、HashMapオブジェクト。このオブジェクトのKeyはオブジェクトID、ValueはL1NpcInstance
	 *         。
	 */
	public Map<Integer, L1NpcInstance> getPetList() {
		return _petlist;
	}

	/**
	 * キャラクターへマジックドールを追加する。
	 * 
	 * @param doll
	 *            追加するdollを表す、L1DollInstanceオブジェクト。
	 */
	public void addDoll(L1DollInstance doll) {
		_dolllist.put(doll.getId(), doll);
	}

	/**
	 * キャラクターからマジックドールを削除する。
	 * 
	 * @param doll
	 *            削除するdollを表す、L1DollInstanceオブジェクト。
	 */
	public void removeDoll(L1DollInstance doll) {
		_dolllist.remove(doll.getId());
	}

	/**
	 * キャラクターのマジックドールリストを返す。
	 * 
	 * @return キャラクターの魔法人形リストを表す、HashMapオブジェクト。このオブジェクトのKeyはオブジェクトID、
	 *         ValueはL1DollInstance。
	 */
	public Map<Integer, L1DollInstance> getDollList() {
		return _dolllist;
	}

	/**
	 * キャラクターへ従者を追加する。
	 * 
	 * @param follower
	 *            追加するfollowerを表す、L1FollowerInstanceオブジェクト。
	 */
	public void addFollower(L1FollowerInstance follower) {
		_followerlist.put(follower.getId(), follower);
	}

	/**
	 * キャラクターから従者を削除する。
	 * 
	 * @param follower
	 *            削除するfollowerを表す、L1FollowerInstanceオブジェクト。
	 */
	public void removeFollower(L1FollowerInstance follower) {
		_followerlist.remove(follower.getId());
	}

	/**
	 * キャラクターの従者リストを返す。
	 * 
	 * @return キャラクターの従者リストを表す、HashMapオブジェクト。このオブジェクトのKeyはオブジェクトID、
	 *         ValueはL1FollowerInstance。
	 */
	public Map<Integer, L1FollowerInstance> getFollowerList() {
		return _followerlist;
	}

	/**
	 * キャラクターへ、毒を追加する。
	 * 
	 * @param poison
	 *            毒を表す、L1Poisonオブジェクト。
	 */
	public void setPoison(L1Poison poison) {
		_poison = poison;
	}

	/**
	 * キャラクターの毒を治療する。
	 */
	public void curePoison() {
		if (_poison == null) {
			return;
		}
		_poison.cure();
	}

	/**
	 * キャラクターの毒状態を返す。
	 * 
	 * @return キャラクターの毒を表す、L1Poisonオブジェクト。
	 */
	public L1Poison getPoison() {
		return _poison;
	}

	/**
	 * キャラクターへ毒のエフェクトを付加する
	 * 
	 * @param effectId
	 * @see S_Poison#S_Poison(int, int)
	 */
	public void setPoisonEffect(int effectId) {
		broadcastPacket(new S_Poison(getId(), effectId));
	}

	/**
	 * キャラクターが存在する座標が、どのゾーンに属しているかを返す。
	 * 
	 * @return 座標のゾーンを表す値。セーフティーゾーンであれば1、コンバットゾーンであれば-1、ノーマルゾーンであれば0。
	 */
	public int getZoneType() {
		if (getMap().isSafetyZone(getLocation())) {
			return 1;
		} else if (getMap().isCombatZone(getLocation())) {
			return -1;
		} else { // ノーマルゾーン
			return 0;
		}
	}

	private int _exp; // ● 経験値

	/**
	 * キャラクターが保持している経験値を返す。
	 * 
	 * @return 経験値。
	 */
	public int getExp() {
		return _exp;
	}

	/**
	 * キャラクターが保持する経験値を設定する。
	 * 
	 * @param exp
	 *            経験値。
	 */
	public void setExp(int exp) {
		_exp = exp;
	}

	// ■■■■■■■■■■ L1PcInstanceへ移動するプロパティ ■■■■■■■■■■
	private final List<L1Object> _knownObjects = Lists.newConcurrentList();

	private final List<L1PcInstance> _knownPlayer = Lists.newConcurrentList();

	/**
	 * 指定されたオブジェクトを、キャラクターが認識しているかを返す。
	 * 
	 * @param obj
	 *            調べるオブジェクト。
	 * @return オブジェクトをキャラクターが認識していればtrue、していなければfalse。 自分自身に対してはfalseを返す。
	 */
	public boolean knownsObject(L1Object obj) {
		return _knownObjects.contains(obj);
	}

	/**
	 * キャラクターが認識している全てのオブジェクトを返す。
	 * 
	 * @return キャラクターが認識しているオブジェクトを表すL1Objectが格納されたArrayList。
	 */
	public List<L1Object> getKnownObjects() {
		return _knownObjects;
	}

	/**
	 * キャラクターが認識している全てのプレイヤーを返す。
	 * 
	 * @return キャラクターが認識しているオブジェクトを表すL1PcInstanceが格納されたArrayList。
	 */
	public List<L1PcInstance> getKnownPlayers() {
		return _knownPlayer;
	}

	/**
	 * キャラクターに、新たに認識するオブジェクトを追加する。
	 * 
	 * @param obj
	 *            新たに認識するオブジェクト。
	 */
	public void addKnownObject(L1Object obj) {
		if (!_knownObjects.contains(obj)) {
			_knownObjects.add(obj);
			if (obj instanceof L1PcInstance) {
				_knownPlayer.add((L1PcInstance) obj);
			}
		}
	}

	/**
	 * キャラクターから、認識しているオブジェクトを削除する。
	 * 
	 * @param obj
	 *            削除するオブジェクト。
	 */
	public void removeKnownObject(L1Object obj) {
		_knownObjects.remove(obj);
		if (obj instanceof L1PcInstance) {
			_knownPlayer.remove(obj);
		}
	}

	/**
	 * キャラクターから、全ての認識しているオブジェクトを削除する。
	 */
	public void removeAllKnownObjects() {
		_knownObjects.clear();
		_knownPlayer.clear();
	}

	// ■■■■■■■■■■ プロパティ ■■■■■■■■■■

	private String _name; // ● 名前

	public String getName() {
		return _name;
	}

	public void setName(String s) {
		_name = s;
	}

	private int _level; // ● レベル

	public synchronized int getLevel() {
		return _level;
	}

	public synchronized void setLevel(long level) {
		_level = (int) level;
	}

	private short _maxHp = 0; // ● ＭＡＸＨＰ（1～32767）

	private int _trueMaxHp = 0; // ● 本当のＭＡＸＨＰ

	public short getMaxHp() {
		return _maxHp;
	}

	public void setMaxHp(int hp) {
		_trueMaxHp = hp;
		_maxHp = (short) IntRange.ensure(_trueMaxHp, 1, 32767);
		_currentHp = Math.min(_currentHp, _maxHp);
	}

	public void addMaxHp(int i) {
		setMaxHp(_trueMaxHp + i);
	}

	private short _maxMp = 0; // ● ＭＡＸＭＰ（0～32767）

	private int _trueMaxMp = 0; // ● 本当のＭＡＸＭＰ

	public short getMaxMp() {
		return _maxMp;
	}

	public void setMaxMp(int mp) {
		_trueMaxMp = mp;
		_maxMp = (short) IntRange.ensure(_trueMaxMp, 0, 32767);
		_currentMp = Math.min(_currentMp, _maxMp);
	}

	public void addMaxMp(int i) {
		setMaxMp(_trueMaxMp + i);
	}

	private int _ac = 0; // ● ＡＣ（-128～127）

	private int _trueAc = 0; // ● 本当のＡＣ

	public int getAc() {
		return _ac + L1MagicDoll.getAcByDoll(this); // TODO 魔法娃娃效果 - 防禦增加
	}

	public void setAc(int i) {
		_trueAc = i;
		_ac = IntRange.ensure(i, -128, 127);
	}

	public void addAc(int i) {
		setAc(_trueAc + i);
	}

	private byte _str = 0; // ● ＳＴＲ（1～127）

	private short _trueStr = 0; // ● 本当のＳＴＲ

	public byte getStr() {
		return _str;
	}

	public void setStr(int i) {
		_trueStr = (short) i;
		_str = (byte) IntRange.ensure(i, 1, 127);
	}

	public void addStr(int i) {
		setStr(_trueStr + i);
	}

	private byte _con = 0; // ● ＣＯＮ（1～127）

	private short _trueCon = 0; // ● 本当のＣＯＮ

	public byte getCon() {
		return _con;
	}

	public void setCon(int i) {
		_trueCon = (short) i;
		_con = (byte) IntRange.ensure(i, 1, 127);
	}

	public void addCon(int i) {
		setCon(_trueCon + i);
	}

	private byte _dex = 0; // ● ＤＥＸ（1～127）

	private short _trueDex = 0; // ● 本当のＤＥＸ

	public byte getDex() {
		return _dex;
	}

	public void setDex(int i) {
		_trueDex = (short) i;
		_dex = (byte) IntRange.ensure(i, 1, 127);
	}

	public void addDex(int i) {
		setDex(_trueDex + i);
	}

	private byte _cha = 0; // ● ＣＨＡ（1～127）

	private short _trueCha = 0; // ● 本当のＣＨＡ

	public byte getCha() {
		return _cha;
	}

	public void setCha(int i) {
		_trueCha = (short) i;
		_cha = (byte) IntRange.ensure(i, 1, 127);
	}

	public void addCha(int i) {
		setCha(_trueCha + i);
	}

	private byte _int = 0; // ● ＩＮＴ（1～127）

	private short _trueInt = 0; // ● 本当のＩＮＴ

	public byte getInt() {
		return _int;
	}

	public void setInt(int i) {
		_trueInt = (short) i;
		_int = (byte) IntRange.ensure(i, 1, 127);
	}

	public void addInt(int i) {
		setInt(_trueInt + i);
	}

	private byte _wis = 0; // ● ＷＩＳ（1～127）

	private short _trueWis = 0; // ● 本当のＷＩＳ

	public byte getWis() {
		return _wis;
	}

	public void setWis(int i) {
		_trueWis = (short) i;
		_wis = (byte) IntRange.ensure(i, 1, 127);
	}

	public void addWis(int i) {
		setWis(_trueWis + i);
	}

	private int _wind = 0; // ● 風防御（-128～127）

	private int _trueWind = 0; // ● 本当の風防御

	public int getWind() {
		return _wind;
	} // 使用するとき

	public void addWind(int i) {
		_trueWind += i;
		if (_trueWind >= 127) {
			_wind = 127;
		} else if (_trueWind <= -128) {
			_wind = -128;
		} else {
			_wind = _trueWind;
		}
	}

	private int _water = 0; // ● 水防御（-128～127）

	private int _trueWater = 0; // ● 本当の水防御

	public int getWater() {
		return _water;
	} // 使用するとき

	public void addWater(int i) {
		_trueWater += i;
		if (_trueWater >= 127) {
			_water = 127;
		} else if (_trueWater <= -128) {
			_water = -128;
		} else {
			_water = _trueWater;
		}
	}

	private int _fire = 0; // ● 火防御（-128～127）

	private int _trueFire = 0; // ● 本当の火防御

	public int getFire() {
		return _fire;
	} // 使用するとき

	public void addFire(int i) {
		_trueFire += i;
		if (_trueFire >= 127) {
			_fire = 127;
		} else if (_trueFire <= -128) {
			_fire = -128;
		} else {
			_fire = _trueFire;
		}
	}

	private int _earth = 0; // ● 地防御（-128～127）

	private int _trueEarth = 0; // ● 本当の地防御

	public int getEarth() {
		return _earth;
	} // 使用するとき

	public void addEarth(int i) {
		_trueEarth += i;
		if (_trueEarth >= 127) {
			_earth = 127;
		} else if (_trueEarth <= -128) {
			_earth = -128;
		} else {
			_earth = _trueEarth;
		}
	}

	private int _addAttrKind; // エレメンタルフォールダウンで減少した属性の種類

	public int getAddAttrKind() {
		return _addAttrKind;
	}

	public void setAddAttrKind(int i) {
		_addAttrKind = i;
	}

	// 昏迷耐性
	private int _registStun = 0;

	private int _trueRegistStun = 0;

	public int getRegistStun() {
		return (_registStun + L1MagicDoll.getRegistStunByDoll(this));
	}

	public void addRegistStun(int i) {
		_trueRegistStun += i;
		if (_trueRegistStun > 127) {
			_registStun = 127;
		} else if (_trueRegistStun < -128) {
			_registStun = -128;
		} else {
			_registStun = _trueRegistStun;
		}
	}

	// 石化耐性
	private int _registStone = 0;

	private int _trueRegistStone = 0;

	public int getRegistStone() {
		return (_registStone + L1MagicDoll.getRegistStoneByDoll(this));
	}

	public void addRegistStone(int i) {
		_trueRegistStone += i;
		if (_trueRegistStone > 127) {
			_registStone = 127;
		} else if (_trueRegistStone < -128) {
			_registStone = -128;
		} else {
			_registStone = _trueRegistStone;
		}
	}

	// 睡眠耐性
	private int _registSleep = 0;

	private int _trueRegistSleep = 0;

	public int getRegistSleep() {
		return (_registSleep + L1MagicDoll.getRegistSleepByDoll(this));
	}

	public void addRegistSleep(int i) {
		_trueRegistSleep += i;
		if (_trueRegistSleep > 127) {
			_registSleep = 127;
		} else if (_trueRegistSleep < -128) {
			_registSleep = -128;
		} else {
			_registSleep = _trueRegistSleep;
		}
	}

	// 寒冰耐性
	private int _registFreeze = 0;

	private int _trueRegistFreeze = 0;

	public int getRegistFreeze() {
		return (_registFreeze
				+ L1MagicDoll.getRegistFreezeByDoll(this)); // TODO 魔法娃娃效果 - 寒冰耐性增加
	}

	public void add_regist_freeze(int i) {
		_trueRegistFreeze += i;
		if (_trueRegistFreeze > 127) {
			_registFreeze = 127;
		} else if (_trueRegistFreeze < -128) {
			_registFreeze = -128;
		} else {
			_registFreeze = _trueRegistFreeze;
		}
	}

	// 支撐耐性
	private int _registSustain = 0;

	private int _trueRegistSustain = 0;

	public int getRegistSustain() {
		return (_registSustain + L1MagicDoll.getRegistSustainByDoll(this));
	}

	public void addRegistSustain(int i) {
		_trueRegistSustain += i;
		if (_trueRegistSustain > 127) {
			_registSustain = 127;
		} else if (_trueRegistSustain < -128) {
			_registSustain = -128;
		} else {
			_registSustain = _trueRegistSustain;
		}
	}

	// 闇黑耐性
	private int _registBlind = 0;

	private int _trueRegistBlind = 0;

	public int getRegistBlind() {
		return (_registBlind + L1MagicDoll.getRegistBlindByDoll(this));
	}

	public void addRegistBlind(int i) {
		_trueRegistBlind += i;
		if (_trueRegistBlind > 127) {
			_registBlind = 127;
		} else if (_trueRegistBlind < -128) {
			_registBlind = -128;
		} else {
			_registBlind = _trueRegistBlind;
		}
	}

	private int _dmgup = 0; // ● 近距離傷害補正（-128～127）

	private int _trueDmgup = 0; // ● 本当のダメージ補正

	public int getDmgup() {
		return _dmgup + L1MagicDoll.getDamageAddByDoll(this); // 魔法娃娃效果 - 近距離的攻擊力增加
	} // 使用するとき

	public void addDmgup(int i) {
		_trueDmgup += i;
		if (_trueDmgup >= 127) {
			_dmgup = 127;
		} else if (_trueDmgup <= -128) {
			_dmgup = -128;
		} else {
			_dmgup = _trueDmgup;
		}
	}

	private int _bowDmgup = 0; // ● 弓傷害補正（-128～127）

	private int _trueBowDmgup = 0; // ● 本当の弓ダメージ補正

	public int getBowDmgup() {
		return _bowDmgup + L1MagicDoll.getBowDamageByDoll(this); // 魔法娃娃效果 - 遠距離的攻擊力增加
	}

	public void addBowDmgup(int i) {
		_trueBowDmgup += i;
		if (_trueBowDmgup >= 127) {
			_bowDmgup = 127;
		} else if (_trueBowDmgup <= -128) {
			_bowDmgup = -128;
		} else {
			_bowDmgup = _trueBowDmgup;
		}
	}

	private int _hitup = 0; // ● 命中補正（-128～127）

	private int _trueHitup = 0; // ● 本当の命中補正

	public int getHitup() {
		return (_hitup
				+ L1MagicDoll.getHitAddByDoll(this)); // TODO 魔法娃娃效果 - 近距離的命中力增加
	}

	public void addHitup(int i) {
		_trueHitup += i;
		if (_trueHitup >= 127) {
			_hitup = 127;
		} else if (_trueHitup <= -128) {
			_hitup = -128;
		} else {
			_hitup = _trueHitup;
		}
	}

	private int _bowHitup = 0; // ● 弓命中補正（-128～127）

	private int _trueBowHitup = 0; // ● 本当の弓命中補正

	public int getBowHitup() {
		return (_bowHitup
				+ L1MagicDoll.getBowHitAddByDoll(this)); // TODO 魔法娃娃效果 - 弓的命中力增加
	}

	public void addBowHitup(int i) {
		_trueBowHitup += i;
		if (_trueBowHitup >= 127) {
			_bowHitup = 127;
		} else if (_trueBowHitup <= -128) {
			_bowHitup = -128;
		} else {
			_bowHitup = _trueBowHitup;
		}
	}

	private int _mr = 0; // ● 魔法防御（0～）

	private int _trueMr = 0; // ● 本当の魔法防御

	public int getMr() {
		if (hasSkillEffect(153) == true) {
			return _mr / 4;
		} else {
			return _mr;
		}
	} // 使用するとき

	public int getTrueMr() {
		return _trueMr;
	} // セットするとき

	public void addMr(int i) {
		_trueMr += i;
		if (_trueMr <= 0) {
			_mr = 0;
		} else {
			_mr = _trueMr;
		}
	}

	private int _sp = 0; // ● 増加したＳＰ

	public int getSp() {
		return getTrueSp() + _sp;
	}

	public int getTrueSp() {
		return getMagicLevel() + getMagicBonus();
	}

	public void addSp(int i) {
		_sp += i;
	}

	private boolean _isDead; // ● 死亡状態

	public boolean isDead() {
		return _isDead;
	}

	public void setDead(boolean flag) {
		_isDead = flag;
	}

	private int _status; // ● 状態？

	public int getStatus() {
		return _status;
	}

	public void setStatus(int i) {
		_status = i;
	}

	private String _title; // ● 頭銜

	public String getTitle() {
		return _title;
	}

	public void setTitle(String s) {
		_title = s;
	}

	private int _lawful; // ● 正義值

	public int getLawful() {
		return _lawful;
	}

	public void setLawful(int i) {
		_lawful = i;
	}

	public synchronized void addLawful(int i) {
		_lawful += i;
		if (_lawful > 32767) {
			_lawful = 32767;
		} else if (_lawful < -32768) {
			_lawful = -32768;
		}
	}

	private int _heading; // ● 面向 0.左上 1.上 2.右上 3.右 4.右下 5.下 6.左下 7.左

	public int getHeading() {
		return _heading;
	}

	public void setHeading(int i) {
		_heading = i;
	}

	private int _moveSpeed; // ● 速度 0.通常 1.加速 2.緩速

	public int getMoveSpeed() {
		return _moveSpeed;
	}

	public void setMoveSpeed(int i) {
		_moveSpeed = i;
	}

	private int _braveSpeed; // ● 勇敢狀態 0，通常1。勇敢

	public int getBraveSpeed() {
		return _braveSpeed;
	}

	public void setBraveSpeed(int i) {
		_braveSpeed = i;
	}

	private int _tempCharGfx; // ● 暫時變身的ID

	public int getTempCharGfx() {
		return _tempCharGfx;
	}

	public void setTempCharGfx(int i) {
		_tempCharGfx = i;
	}

	private int _gfxid; // ● 原本圖型的ＩＤ

	public int getGfxId() {
		return _gfxid;
	}

	public void setGfxId(int i) {
		_gfxid = i;
	}

	public int getMagicLevel() {
		return getLevel() / 4;
	}

	public int getMagicBonus() {
		int i = getInt();
		if (i <= 5) {
			return -2;
		} else if (i <= 8) {
			return -1;
		} else if (i <= 11) {
			return 0;
		} else if (i <= 14) {
			return 1;
		} else if (i <= 17) {
			return 2;
		} else if (i <= 24) {
			return i - 15;
		} else if (i <= 35) {
			return 10;
		} else if (i <= 42) {
			return 11;
		} else if (i <= 49) {
			return 12;
		} else if (i <= 50) {
			return 13;
		} else {
			return i - 25;
		}
	}

	public boolean isInvisble() {
		return (hasSkillEffect(INVISIBILITY) || hasSkillEffect(BLIND_HIDING));
	}

	public void healHp(int pt) {
		setCurrentHp(getCurrentHp() + pt);
	}

	private int _karma;

	/**
	 * キャラクターが保持しているカルマを返す。
	 * 
	 * @return カルマ。
	 */
	public int getKarma() {
		return _karma;
	}

	/**
	 * キャラクターが保持するカルマを設定する。
	 * 
	 * @param karma
	 *            カルマ。
	 */
	public void setKarma(int karma) {
		_karma = karma;
	}

	public void setMr(int i) {
		_trueMr = i;
		if (_trueMr <= 0) {
			_mr = 0;
		} else {
			_mr = _trueMr;
		}
	}

	public void turnOnOffLight() {
		int lightSize = 0;
		if (this instanceof L1NpcInstance) {
			L1NpcInstance npc = (L1NpcInstance) this;
			lightSize = npc.getLightSize(); // npc.sqlのライトサイズ
		}
		if (hasSkillEffect(LIGHT)) {
			lightSize = 14;
		}

		for (L1ItemInstance item : getInventory().getItems()) {
			if ((item.getItem().getType2() == 0)
					&& (item.getItem().getType() == 2)) { // light系アイテム
				int itemlightSize = item.getItem().getLightRange();
				if ((itemlightSize != 0) && item.isNowLighting()) {
					if (itemlightSize > lightSize) {
						lightSize = itemlightSize;
					}
				}
			}
		}

		if (this instanceof L1PcInstance) {
			L1PcInstance pc = (L1PcInstance) this;
			pc.sendPackets(new S_Light(pc.getId(), lightSize));
		}
		if (!isInvisble()) {
			broadcastPacket(new S_Light(getId(), lightSize));
		}

		setOwnLightSize(lightSize); // S_OwnCharPackのライト範囲
		setChaLightSize(lightSize); // S_OtherCharPack, S_NPCPackなどのライト範囲
	}

	private int _chaLightSize; // ● ライトの範囲

	public int getChaLightSize() {
		if (isInvisble()) {
			return 0;
		}
		return _chaLightSize;
	}

	public void setChaLightSize(int i) {
		_chaLightSize = i;
	}

	private int _ownLightSize; // ● ライトの範囲(S_OwnCharPack用)

	public int getOwnLightSize() {
		return _ownLightSize;
	}

	public void setOwnLightSize(int i) {
		_ownLightSize = i;
	}

	private int _portalNumber = -1; // 龍之門扉編號

	public int getPortalNumber() {
		return _portalNumber;
	}

	public void setPortalNumber(int portalNumber) {
		_portalNumber = portalNumber;
	}

	// 飽食度
	private int _food;

	public int get_food() {
		return _food;
	}

	public void set_food(int i) {
		_food = i;
	}

	// 附魔石階級
	private byte _magicStoneLevel;

	public byte getMagicStoneLevel() {
		return _magicStoneLevel;
	}

	public void setMagicStoneLevel(byte i) {
		_magicStoneLevel = i;
	}

	// 閃避率 +
	private byte _dodge = 0;

	public byte getDodge() {
		return _dodge;
	}

	public void addDodge(byte i) {
		_dodge += i;
		if (_dodge >= 10) {
			_dodge = 10;
		} else if (_dodge <= 0){
			_dodge = 0;
		}
	}

	// 閃避率 -
	private byte _nDodge = 0;

	public byte getNdodge() {
		return _nDodge;
	}

	public void addNdodge(byte i) {
		_nDodge += i;
		if (_nDodge >= 10) {
			_nDodge = 10;
		} else if (_nDodge <= 0){
			_nDodge = 0;
		}
	}

	// 旅館
	private int _innRoomNumber;

	public int getInnRoomNumber() {
		return _innRoomNumber;
	}

	public void setInnRoomNumber(int i) {
		_innRoomNumber = i;
	}

	private int _innKeyId;

	public int getInnKeyId() {
		return _innKeyId;
	}

	public void setInnKeyId(int i) {
		_innKeyId = i;
	}

	private boolean _isHall;

	public boolean checkRoomOrHall() {
		return _isHall;
	}

	public void setHall(boolean i) {
		_isHall = i;
	}

	// 判斷特定狀態下才可攻擊 NPC
	public boolean isAttackMiss(L1Character cha , int npcId) {
		switch (npcId) {
			case 45912: // 士兵的怨靈
			case 45913: // 士兵的怨靈
			case 45914: // 怨靈
			case 45915: // 怨靈
				if (!cha.hasSkillEffect(STATUS_HOLY_WATER)) {
					return true;
				}
				return false;
			case 45916: // 哈蒙將軍的怨靈
				if (!cha.hasSkillEffect(STATUS_HOLY_MITHRIL_POWDER)) {
					return true;
				}
				return false;
			case 45941: // 受詛咒的巫女莎爾
				if (!cha.hasSkillEffect(STATUS_HOLY_WATER_OF_EVA)) {
					return true;
				}
				return false;
			case 45752: // 炎魔(變身前)
				if (!cha.hasSkillEffect(STATUS_CURSE_BARLOG)) {
					return true;
				}
				return false;
			case 45753: // 炎魔(變身後)
				if (!cha.hasSkillEffect(STATUS_CURSE_BARLOG)) {
					return true;
				}
				return false;
			case 45675: // 火焰之影(變身前)
				if (!cha.hasSkillEffect(STATUS_CURSE_YAHEE)) {
					return true;
				}
				return false;
			case 81082: // 火焰之影(變身後)
				if (!cha.hasSkillEffect(STATUS_CURSE_YAHEE)) {
					return true;
				}
				return false;
			case 45625: // 混沌
				if (!cha.hasSkillEffect(STATUS_CURSE_YAHEE)) {
					return true;
				}
				return false;
			case 45674: // 死亡
				if (!cha.hasSkillEffect(STATUS_CURSE_YAHEE)) {
					return true;
				}
				return false;
			case 45685: // 墮落
				if (!cha.hasSkillEffect(STATUS_CURSE_YAHEE)) {
					return true;
				}
				return false;
			case 81341: // 再生之祭壇
				if (!cha.hasSkillEffect(SECRET_MEDICINE_OF_DESTRUCTION)) {
					return true;
				}
			default:
				if ((npcId >= 46068) && (npcId <= 46091) // 原生魔族
						&& (cha.getTempCharGfx() == 6035)) {
					return true;
				}
				else if ((npcId >= 46092) && (npcId <= 46106) // 不死魔族
						&& (cha.getTempCharGfx() == 6034)) {
					return true;
				}
				return false;
		}
	}
}
