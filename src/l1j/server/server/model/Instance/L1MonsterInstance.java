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

import static l1j.server.server.model.skill.L1SkillId.EFFECT_BLOODSTAIN_OF_ANTHARAS;
import static l1j.server.server.model.skill.L1SkillId.FOG_OF_SLEEPING;

import java.io.Serial;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.Config;
import l1j.server.server.ActionCodes;
import l1j.server.server.GeneralThreadPool;
import l1j.server.server.IdFactory;
import l1j.server.server.datatables.DropTable;
import l1j.server.server.datatables.NPCTalkDataTable;
import l1j.server.server.datatables.NpcTable;
import l1j.server.server.datatables.SprTable;
import l1j.server.server.datatables.UBTable;
import l1j.server.server.model.L1Attack;
import l1j.server.server.model.L1Character;
import l1j.server.server.model.L1DragonSlayer;
import l1j.server.server.model.L1Location;
import l1j.server.server.model.L1NpcTalkData;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1UltimateBattle;
import l1j.server.server.model.L1World;
import l1j.server.server.model.skill.L1BuffUtil;
import l1j.server.server.serverpackets.S_ChangeName;
import l1j.server.server.serverpackets.S_CharVisualUpdate;
import l1j.server.server.serverpackets.S_DoActionGFX;
import l1j.server.server.serverpackets.S_NPCPack;
import l1j.server.server.serverpackets.S_NPCTalkReturn;
import l1j.server.server.serverpackets.S_NpcChangeShape;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_SkillBrave;
import l1j.server.server.templates.L1Npc;
import l1j.server.server.utils.CalcExp;
import l1j.server.server.utils.Random;

/**
 * 怪物實例類別
 * <p>
 * 這是遊戲中所有怪物（Monster）的實例類別，繼承自 {@link L1NpcInstance}。
 * 實現了怪物特有的行為邏輯，包括變形、掉落物品、死亡處理、AI 行為等。
 * </p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 * <li><b>變形系統</b>：支援變形怪（Doppelganger）變身為玩家外觀</li>
 * <li><b>掉落系統</b>：怪物死亡時掉落物品和金錢</li>
 * <li><b>經驗值分配</b>：計算並分配經驗值給擊殺者</li>
 * <li><b>AI 行為</b>：覆寫父類的 AI 方法，實現怪物特有行為</li>
 * <li><b>物品使用</b>：怪物可使用藥水（加速、治療）</li>
 * <li><b>特殊怪物</b>：支援 BOSS、龍系怪物等特殊類型</li>
 * </ul>
 *
 * <h3>繼承關係：</h3>
 * <pre>
 * L1Object
 *   └── L1Character
 *         └── L1NpcInstance
 *               └── L1MonsterInstance（怪物）
 * </pre>
 *
 * <h3>核心概念：</h3>
 * <ul>
 * <li><b>變形怪</b>：特殊怪物可變身為玩家外觀，模仿其名稱和外型</li>
 * <li><b>掉落物品</b>：根據掉落表（DropTable）生成掉落物品</li>
 * <li><b>經驗值</b>：根據等級和配置計算經驗值獎勵</li>
 * <li><b>無限大戰</b>：特殊區域的怪物有額外的處理邏輯</li>
 * </ul>
 *
 * <h3>特殊行為：</h3>
 * <ul>
 * <li>變形怪會在首次攻擊時變身為目標玩家</li>
 * <li>血量低於 40% 時會使用治療藥水</li>
 * <li>戰鬥開始時可能使用加速藥水</li>
 * <li>死亡時觸發掉落物品和經驗值分配</li>
 * </ul>
 *
 * @see L1NpcInstance
 * @see DropTable
 * @see CalcExp
 */
public class L1MonsterInstance extends L1NpcInstance {

	@Serial
	private static final long serialVersionUID = 1L;

	/** 日誌記錄器 */
	private static Logger _log = Logger.getLogger(L1MonsterInstance.class
			.getName());

	/** 掉落物品是否已載入完成 */
	private boolean _storeDroped;

	/** 是否已變形（變形怪專用） */
	private boolean isDoppel;

	/**
	 * 物品使用處理
	 * <p>
	 * 覆寫父類方法，實現怪物的物品使用邏輯。
	 * </p>
	 *
	 * <h4>使用邏輯：</h4>
	 * <ul>
	 * <li><b>首次戰鬥</b>：40% 機率使用加速藥水，並可能觸發變形</li>
	 * <li><b>血量低於 40%</b>：50% 機率使用治療藥水</li>
	 * </ul>
	 *
	 * @see L1NpcInstance#onItemUse()
	 * @see #onDoppel(boolean)
	 */
	@Override
	public void onItemUse() {
		if (!isActived() && (_target != null)) {
			useItem(USEITEM_HASTE, 40); // ４０％使用加速藥水
			// 變形判斷
			onDoppel(true);
		}
		if (getCurrentHp() * 100 / getMaxHp() < 40) { // ＨＰが４０％きったら
			useItem(USEITEM_HEAL, 50); // ５０％の確率で回復ポーション使用
		}
	}

	/**
	 * 變形怪變身處理
	 * <p>
	 * 變形怪（Doppelganger）可以變身為目標玩家的外觀。
	 * 此方法處理變身和還原的邏輯。
	 * </p>
	 *
	 * <h4>變身效果：</h4>
	 * <ul>
	 * <li>複製玩家的名稱和外觀</li>
	 * <li>複製玩家的正義值</li>
	 * <li>調整移動和攻擊速度</li>
	 * <li>根據職業設定武器狀態</li>
	 * </ul>
	 *
	 * <h4>還原效果：</h4>
	 * <ul>
	 * <li>恢復原本的 NPC 名稱和外觀</li>
	 * <li>恢復原本的正義值</li>
	 * <li>恢復原本的移動和攻擊速度</li>
	 * </ul>
	 *
	 * @param isChangeShape true：變身為玩家；false：還原為原本外觀
	 * @see L1NpcInstance#onDoppel(boolean)
	 * @see L1Npc#is_doppel()
	 */
	@Override
	public void onDoppel(boolean isChangeShape) {
		if (getNpcTemplate().is_doppel()) {
			boolean updateObject = false;

			if (!isChangeShape) { // 復原
				updateObject = true;
				// setName(getNpcTemplate().get_name());
				// setNameId(getNpcTemplate().get_nameid());
				setTempLawful(getNpcTemplate().get_lawful());
				setGfxId(getNpcTemplate().get_gfxid());
				setTempCharGfx(getNpcTemplate().get_gfxid());
			} else if (!isDoppel && (_target instanceof L1PcInstance)) { // 未變形
				setSleepTime(300);
				L1PcInstance targetPc = (L1PcInstance) _target;
				isDoppel = true;
				updateObject = true;
				setName(targetPc.getName());
				setNameId(targetPc.getName());
				setTempLawful(targetPc.getLawful());
				setGfxId(targetPc.getClassId());
				setTempCharGfx(targetPc.getClassId());

				if (targetPc.getClassId() != 6671) { // 非幻術師拿劍
					setStatus(4);
				} else { // 幻術師拿斧頭
					setStatus(11);
				}
			}
			// 移動、攻擊速度
			setPassispeed(SprTable.getInstance().getMoveSpeed(getTempCharGfx(),
					getStatus()));
			setAtkspeed(SprTable.getInstance().getAttackSpeed(getTempCharGfx(),
					getStatus() + 1));
			// 變形
			if (updateObject) {
				for (L1PcInstance pc : L1World.getInstance()
						.getRecognizePlayer(this)) {
					if (!isChangeShape) {
						pc.sendPackets(new S_ChangeName(getId(),getNpcTemplate().get_nameid()));
					} else {
						pc.sendPackets(new S_ChangeName(getId(), getNameId()));
					}
					pc.sendPackets(new S_NpcChangeShape(getId(), getGfxId(),getTempLawful(), getStatus()));
				}
			}
		}
	}

	/**
	 * 玩家感知怪物時的處理
	 * <p>
	 * 當玩家進入視野範圍，將怪物加入玩家的已知物件列表，
	 * 發送怪物封包給玩家，並啟動怪物 AI。
	 * </p>
	 *
	 * <h4>特殊處理：</h4>
	 * <ul>
	 * <li>檢查怪物血量，若已死亡則不發送封包（水龍特例除外）</li>
	 * <li>若怪物有二段加速狀態，發送加速效果封包</li>
	 * </ul>
	 *
	 * @param perceivedFrom 感知到此怪物的玩家
	 * @see L1NpcInstance#onPerceive(L1PcInstance)
	 * @see #onNpcAI()
	 */
	@Override
	public void onPerceive(L1PcInstance perceivedFrom) {
		perceivedFrom.addKnownObject(this);
		if (0 < getCurrentHp()) {
			perceivedFrom.sendPackets(new S_NPCPack(this));
			onNpcAI(); // モンスターのＡＩを開始
			if (getBraveSpeed() == 1) { // 二段加速狀態
				perceivedFrom.sendPackets(new S_SkillBrave(getId(), 1, 600000));
				setBraveSpeed(1);
			}
		} else {
			// 水龍 階段一、二 死亡隱形
			if (getGfxId() != 7864 && getGfxId() != 7869) {
				perceivedFrom.sendPackets(new S_NPCPack(this));
			}
		}
	}

	/**
	 * 職業對應的外觀 ID 對照表
	 * <p>
	 * 用於判斷特定職業的玩家外觀。每個職業有兩個對應的外觀 ID（男/女）。
	 * </p>
	 * <pre>
	 * 索引 0: 王族 (0, 1)
	 * 索引 1: 騎士 (48, 61)
	 * 索引 2: 妖精 (37, 138)
	 * 索引 3: 法師 (734, 1186)
	 * 索引 4: 黑暗妖精 (2786, 2796)
	 * </pre>
	 */
	public static int[][] _classGfxId = { { 0, 1 }, { 48, 61 }, { 37, 138 },
			{ 734, 1186 }, { 2786, 2796 } };

	/**
	 * 搜尋攻擊目標
	 * <p>
	 * 覆寫父類方法，實現怪物特有的目標搜尋邏輯。
	 * 根據多種條件判斷玩家是否會被怪物主動攻擊。
	 * </p>
	 *
	 * <h4>排除條件（不會被攻擊）：</h4>
	 * <ul>
	 * <li>玩家已死亡、GM 模式、監視模式、幽靈狀態</li>
	 * <li>玩家與怪物陣營友好（正義值符合條件）</li>
	 * <li>玩家變身為對應陣營（如變身成巴羅古或亞希形態）</li>
	 * <li>玩家隱形且怪物無法偵測隱形</li>
	 * </ul>
	 *
	 * <h4>主動攻擊條件：</h4>
	 * <ul>
	 * <li><b>競技場內</b>：對所有可見玩家主動攻擊</li>
	 * <li><b>特殊 NPC</b>：如卡茲對君主和黑暗妖精主動攻擊</li>
	 * <li><b>主動型怪物</b>：is_agro() 為 true</li>
	 * <li><b>對變身主動</b>：is_agrososc() 為 true 且玩家已變身</li>
	 * <li><b>對特定職業主動</b>：agrogfxid1/2 指定特定職業或外觀</li>
	 * <li><b>完全被動型怪物</b>：只對正義值 < -1000 的混沌玩家主動攻擊</li>
	 * </ul>
	 *
	 * @see L1NpcInstance#searchTarget()
	 * @see L1Npc#is_agro()
	 * @see L1Npc#is_agrososc()
	 * @see L1Npc#is_agrocoi()
	 */
	@Override
	public void searchTarget() {
		// 目標捜索
		L1PcInstance lastTarget = null;
		L1PcInstance targetPlayer = null;

		if (_target != null && _target instanceof L1PcInstance ) {
			lastTarget = (L1PcInstance) _target;
			tagertClear();
		}

		for (L1PcInstance pc : L1World.getInstance().getVisiblePlayer(this)) {

			if ( pc == lastTarget || (pc.getCurrentHp() <= 0) || pc.isDead() || pc.isGm()
					|| pc.isMonitor() || pc.isGhost()) {
				continue;
			}

			// 闘技場内は変身／未変身に限らず全てアクティブ
			int mapId = getMapId();
			if ((mapId == 88) || (mapId == 98) || (mapId == 92)
					|| (mapId == 91) || (mapId == 95)) {
				if (!pc.isInvisble() || getNpcTemplate().is_agrocoi()) { // インビジチェック
					targetPlayer = pc;
					break;
				}
			}

			if (getNpcId() == 45600) { // カーツ
				if (pc.isCrown() || pc.isDarkelf()
						|| (pc.getTempCharGfx() != pc.getClassId())) { // 未変身の君主、DEにはアクティブ
					targetPlayer = pc;
					break;
				}
			}

			// どちらかの条件を満たす場合、友好と見なされ先制攻撃されない。
			// ・モンスターのカルマがマイナス値（バルログ側モンスター）でPCのカルマレベルが1以上（バルログ友好）
			// ・モンスターのカルマがプラス値（ヤヒ側モンスター）でPCのカルマレベルが-1以下（ヤヒ友好）
			if (((getNpcTemplate().getKarma() < 0) && (pc.getKarmaLevel() >= 1))
					|| ((getNpcTemplate().getKarma() > 0) && (pc
							.getKarmaLevel() <= -1))) {
				continue;
			}
			// 見棄てられた者たちの地 カルマクエストの変身中は、各陣営のモンスターから先制攻撃されない
			if (((pc.getTempCharGfx() == 6034) && (getNpcTemplate().getKarma() < 0))
					|| ((pc.getTempCharGfx() == 6035) && (getNpcTemplate()
							.getKarma() > 0))
					|| ((pc.getTempCharGfx() == 6035) && (getNpcTemplate()
							.get_npcId() == 46070))
					|| ((pc.getTempCharGfx() == 6035) && (getNpcTemplate()
							.get_npcId() == 46072))) {
				continue;
			}

			if (!getNpcTemplate().is_agro() && !getNpcTemplate().is_agrososc()
					&& (getNpcTemplate().is_agrogfxid1() < 0)
					&& (getNpcTemplate().is_agrogfxid2() < 0)) { // 完全なノンアクティブモンスター
				if (pc.getLawful() < -1000) { // プレイヤーがカオティック
					targetPlayer = pc;
					break;
				}
				continue;
			}

			if (!pc.isInvisble() || getNpcTemplate().is_agrocoi()) { // インビジチェック
				if (pc.hasSkillEffect(67)) { // 変身してる
					if (getNpcTemplate().is_agrososc()) { // 変身に対してアクティブ
						targetPlayer = pc;
						break;
					}
				} else if (getNpcTemplate().is_agro()) { // アクティブモンスター
					targetPlayer = pc;
					break;
				}

				// 特定のクラスorグラフィックＩＤにアクティブ
				if ((getNpcTemplate().is_agrogfxid1() >= 0)
						&& (getNpcTemplate().is_agrogfxid1() <= 4)) { // クラス指定
					if ((_classGfxId[getNpcTemplate().is_agrogfxid1()][0] == pc
							.getTempCharGfx())
							|| (_classGfxId[getNpcTemplate().is_agrogfxid1()][1] == pc
									.getTempCharGfx())) {
						targetPlayer = pc;
						break;
					}
				} else if (pc.getTempCharGfx() == getNpcTemplate()
						.is_agrogfxid1()) { // グラフィックＩＤ指定
					targetPlayer = pc;
					break;
				}

				if ((getNpcTemplate().is_agrogfxid2() >= 0)
						&& (getNpcTemplate().is_agrogfxid2() <= 4)) { // クラス指定
					if ((_classGfxId[getNpcTemplate().is_agrogfxid2()][0] == pc
							.getTempCharGfx())
							|| (_classGfxId[getNpcTemplate().is_agrogfxid2()][1] == pc
									.getTempCharGfx())) {
						targetPlayer = pc;
						break;
					}
				} else if (pc.getTempCharGfx() == getNpcTemplate()
						.is_agrogfxid2()) { // グラフィックＩＤ指定
					targetPlayer = pc;
					break;
				}
			}
		}
		if (targetPlayer != null) {
			_hateList.add(targetPlayer, 0);
			_target = targetPlayer;
		}
	}

	/**
	 * 設定連鎖仇恨
	 * <p>
	 * 當怪物尚未有目標時，將指定角色加入仇恨列表。
	 * 用於實現怪物間的連鎖仇恨機制。
	 * </p>
	 *
	 * @param cha 要加入仇恨列表的角色
	 * @see L1NpcInstance#setLink(L1Character)
	 * @see #serchLink(L1PcInstance, int)
	 */
	@Override
	public void setLink(L1Character cha) {
		if ((cha != null) && _hateList.isEmpty()) { // ターゲットがいない場合のみ追加
			_hateList.add(cha, 0);
			checkTarget();
		}
	}

	/**
	 * 建構怪物實例
	 *
	 * @param template NPC 模板數據
	 * @see L1Npc
	 */
	public L1MonsterInstance(L1Npc template) {
		super(template);
		_storeDroped = false;
	}

	/**
	 * NPC AI 觸發處理
	 * <p>
	 * 覆寫父類方法，在 AI 啟動前載入掉落物品表。
	 * </p>
	 *
	 * <h4>初始化流程：</h4>
	 * <ol>
	 * <li>檢查 AI 是否已在運行</li>
	 * <li>若掉落物品未載入，從 DropTable 載入並隨機排序</li>
	 * <li>重置活躍狀態</li>
	 * <li>啟動 AI</li>
	 * </ol>
	 *
	 * @see L1NpcInstance#onNpcAI()
	 * @see DropTable#setDrop(L1NpcInstance, L1Inventory)
	 */
	@Override
	public void onNpcAI() {
		if (isAiRunning()) {
			return;
		}
		if (!_storeDroped) // 無駄なオブジェクトＩＤを発行しないようにここでセット
		{
			DropTable.getInstance().setDrop(this, getInventory());
			getInventory().shuffle();
			_storeDroped = true;
		}
		setActived(false);
		startAI();
	}

	/**
	 * 對話動作處理
	 * <p>
	 * 當玩家與怪物對話時觸發。根據玩家的正義值顯示不同的對話內容。
	 * </p>
	 *
	 * <h4>對話選擇：</h4>
	 * <ul>
	 * <li><b>正義值 < -1000</b>（混沌狀態）：顯示對話選項 2</li>
	 * <li><b>其他</b>：顯示對話選項 1</li>
	 * </ul>
	 *
	 * @param pc 對話的玩家
	 * @see L1NpcInstance#onTalkAction(L1PcInstance)
	 * @see NPCTalkDataTable
	 */
	@Override
	public void onTalkAction(L1PcInstance pc) {
		int objid = getId();
		L1NpcTalkData talking = NPCTalkDataTable.getInstance().getTemplate(
				getNpcTemplate().get_npcId());

		// html表示パケット送信
		if (pc.getLawful() < -1000) { // プレイヤーがカオティック
			pc.sendPackets(new S_NPCTalkReturn(talking, objid, 2));
		} else {
			pc.sendPackets(new S_NPCTalkReturn(talking, objid, 1));
		}
	}

	/**
	 * 玩家對怪物執行動作
	 * <p>
	 * 當玩家對怪物執行動作時觸發（如普通攻擊）。
	 * </p>
	 *
	 * @param pc 執行動作的玩家
	 * @see #onAction(L1PcInstance, int)
	 */
	@Override
	public void onAction(L1PcInstance pc) {
		onAction(pc, 0);
	}

	/**
	 * 玩家對怪物執行動作（含技能）
	 * <p>
	 * 處理玩家對怪物的攻擊動作，包含命中判定、傷害計算、毒性攻擊等。
	 * </p>
	 *
	 * <h4>攻擊流程：</h4>
	 * <ol>
	 * <li>檢查怪物是否存活</li>
	 * <li>創建攻擊實例 {@link L1Attack}</li>
	 * <li>計算命中判定</li>
	 * <li>計算傷害</li>
	 * <li>計算法杖魔力吸收</li>
	 * <li>添加毒性攻擊效果</li>
	 * <li>添加追擊效果</li>
	 * <li>執行攻擊動作並提交</li>
	 * </ol>
	 *
	 * @param pc 執行動作的玩家
	 * @param skillId 使用的技能 ID（0 表示普通攻擊）
	 * @see L1Attack
	 */
	@Override
	public void onAction(L1PcInstance pc, int skillId) {
		if ((getCurrentHp() > 0) && !isDead()) {
			L1Attack attack = new L1Attack(pc, this, skillId);
			if (attack.calcHit()) {
				attack.calcDamage();
				attack.calcStaffOfMana();
				attack.addPcPoisonAttack(pc, this);
				attack.addChaserAttack();
			}
			attack.action();
			attack.commit();
		}
	}

	/**
	 * 接收魔力傷害
	 * <p>
	 * 當怪物受到魔力傷害時觸發。會增加對攻擊者的仇恨值並扣除 MP。
	 * </p>
	 *
	 * <h4>處理流程：</h4>
	 * <ol>
	 * <li>檢查傷害值和怪物存活狀態</li>
	 * <li>增加對攻擊者的仇恨值</li>
	 * <li>觸發 AI</li>
	 * <li>觸發同族怪物的連鎖仇恨</li>
	 * <li>扣除 MP（最低為 0）</li>
	 * </ol>
	 *
	 * @param attacker 攻擊者
	 * @param mpDamage 魔力傷害值
	 * @see L1NpcInstance#ReceiveManaDamage(L1Character, int)
	 * @see #setHate(L1Character, int)
	 * @see #serchLink(L1PcInstance, int)
	 */
	@Override
	public void ReceiveManaDamage(L1Character attacker, int mpDamage) {
		if ((mpDamage > 0) && !isDead()) {
			// int Hate = mpDamage / 10 + 10; // 注意！計算適当 ダメージの１０分の１＋ヒットヘイト１０
			// setHate(attacker, Hate);
			setHate(attacker, mpDamage);

			onNpcAI();

			if (attacker instanceof L1PcInstance) { // 仲間意識をもつモンスターのターゲットに設定
				serchLink((L1PcInstance) attacker, getNpcTemplate()
						.get_family());
			}

			int newMp = getCurrentMp() - mpDamage;
			if (newMp < 0) {
				newMp = 0;
			}
			setCurrentMp(newMp);
		}
	}

	/**
	 * 接收傷害
	 * <p>
	 * 當怪物受到傷害時觸發。會增加對攻擊者的仇恨值並扣除 HP，
	 * 若 HP 歸零則觸發死亡處理。
	 * </p>
	 *
	 * <h4>處理流程：</h4>
	 * <ol>
	 * <li>檢查怪物存活狀態</li>
	 * <li>增加對攻擊者的仇恨值</li>
	 * <li>觸發 AI</li>
	 * <li>觸發同族怪物的連鎖仇恨</li>
	 * <li>扣除 HP</li>
	 * <li>若 HP ≤ 0，設定為死亡並調用 death()</li>
	 * </ol>
	 *
	 * @param attacker 攻擊者
	 * @param damage 傷害值
	 * @see L1NpcInstance#receiveDamage(L1Character, int)
	 * @see #setHate(L1Character, int)
	 * @see #death(L1Character)
	 */
	@Override
	public void receiveDamage(L1Character attacker, int damage) {
		if ((getCurrentHp() > 0) && !isDead()) {
			if ((getHiddenStatus() == HIDDEN_STATUS_SINK)
					|| (getHiddenStatus() == HIDDEN_STATUS_FLY)) {
				return;
			}
			if (damage >= 0) {
				if (!(attacker instanceof L1EffectInstance)) { // FWはヘイトなし
					setHate(attacker, damage);
				}
			}
			if (damage > 0) {
				removeSkillEffect(FOG_OF_SLEEPING);
			}

			onNpcAI();

			if (attacker instanceof L1PcInstance) { // 仲間意識をもつモンスターのターゲットに設定
				serchLink((L1PcInstance) attacker, getNpcTemplate()
						.get_family());
			}

			// 血痕相剋傷害增加 1.5倍
			if ((getNpcTemplate().get_npcId() == 97044
					|| getNpcTemplate().get_npcId() == 97045 || getNpcTemplate()
					.get_npcId() == 97046)
					&& (attacker.hasSkillEffect(EFFECT_BLOODSTAIN_OF_ANTHARAS))) { // 有安塔瑞斯的血痕時對法利昂增傷
				damage *= 1.5;
			}

			if ((attacker instanceof L1PcInstance) && (damage > 0)) {
				L1PcInstance player = (L1PcInstance) attacker;
				player.setPetTarget(this);
			}

			int newHp = getCurrentHp() - damage;
			if ((newHp <= 0) && !isDead()) {
				int transformId = getNpcTemplate().getTransformId();
				// 変身しないモンスター
				if (transformId == -1) {
					if (getPortalNumber() != -1) {
						if (getNpcTemplate().get_npcId() == 97006
								|| getNpcTemplate().get_npcId() == 97044) {
							// 準備階段二
							L1DragonSlayer.getInstance().startDragonSlayer2rd(
									getPortalNumber());
						} else if (getNpcTemplate().get_npcId() == 97007
								|| getNpcTemplate().get_npcId() == 97045) {
							// 準備階段三
							L1DragonSlayer.getInstance().startDragonSlayer3rd(
									getPortalNumber());
						} else if (getNpcTemplate().get_npcId() == 97008
								|| getNpcTemplate().get_npcId() == 97046) {
							bloodstain();
							// 結束屠龍副本
							L1DragonSlayer.getInstance().endDragonSlayer(
									getPortalNumber());
						}
					}
					setCurrentHpDirect(0);
					setDead(true);
					setStatus(ActionCodes.ACTION_Die);
					openDoorWhenNpcDied(this);
					Death death = new Death(attacker);
					GeneralThreadPool.getInstance().execute(death);
					// Death(attacker);
					if (getPortalNumber() == -1
							&& (getNpcTemplate().get_npcId() == 97006
									|| getNpcTemplate().get_npcId() == 97007
									|| getNpcTemplate().get_npcId() == 97044 || getNpcTemplate()
									.get_npcId() == 97045)) {
						doNextDragonStep(attacker, getNpcTemplate().get_npcId());
					}
				} else { // 変身するモンスター
							// distributeExpDropKarma(attacker);
					transform(transformId);
				}
			}
			if (newHp > 0) {
				setCurrentHp(newHp);
				hide();
			}
		} else if (!isDead()) { // 念のため
			setDead(true);
			setStatus(ActionCodes.ACTION_Die);
			Death death = new Death(attacker);
			GeneralThreadPool.getInstance().execute(death);
			// Death(attacker);
			if (getPortalNumber() == -1
					&& (getNpcTemplate().get_npcId() == 97006
							|| getNpcTemplate().get_npcId() == 97007
							|| getNpcTemplate().get_npcId() == 97044 || getNpcTemplate()
							.get_npcId() == 97045)) {
				doNextDragonStep(attacker, getNpcTemplate().get_npcId());
			}
		}
	}

	/**
	 * 當特定 NPC 死亡時開啟對應的門
	 * <p>
	 * 針對水晶洞窟中的守護者怪物（NPC ID 46143-46152），
	 * 當其死亡時會自動開啟對應的門（Door ID 5001-5010）。
	 * </p>
	 *
	 * <h4>對應關係：</h4>
	 * <ul>
	 * <li>NPC 46143 → 門 5001</li>
	 * <li>NPC 46144 → 門 5002</li>
	 * <li>...</li>
	 * <li>NPC 46152 → 門 5010</li>
	 * </ul>
	 *
	 * @param npc 死亡的 NPC 實例
	 */
	private static void openDoorWhenNpcDied(L1NpcInstance npc) {
		int[] npcId = { 46143, 46144, 46145, 46146, 46147, 46148, 46149, 46150,
				46151, 46152 };
		int[] doorId = { 5001, 5002, 5003, 5004, 5005, 5006, 5007, 5008, 5009,
				5010 };

		for (int i = 0; i < npcId.length; i++) {
			if (npc.getNpcTemplate().get_npcId() == npcId[i]) {
				openDoorInCrystalCave(doorId[i]);
				break;
			}
		}
	}

	/**
	 * 開啟水晶洞窟中的指定門
	 * <p>
	 * 遍歷世界中的所有物件，找到指定 ID 的門並開啟。
	 * </p>
	 *
	 * @param doorId 門的 ID
	 */
	private static void openDoorInCrystalCave(int doorId) {
		for (L1Object object : L1World.getInstance().getObject()) {
			if (object instanceof L1DoorInstance) {
				L1DoorInstance door = (L1DoorInstance) object;
				if (door.getDoorId() == doorId) {
					door.open();
				}
			}
		}
	}

	/**
	 * 距離が5以上離れているpcを距離3～4の位置に引き寄せる。
	 * 
	 * @param pc
	 */
	/*
	 * private void recall(L1PcInstance pc) { if (getMapId() != pc.getMapId()) {
	 * return; } if (getLocation().getTileLineDistance(pc.getLocation()) > 4) {
	 * for (int count = 0; count < 10; count++) { L1Location newLoc =
	 * getLocation().randomLocation(3, 4, false); if (glanceCheck(newLoc.getX(),
	 * newLoc.getY())) { L1Teleport.teleport(pc, newLoc.getX(), newLoc.getY(),
	 * getMapId(), 5, true); break; } } } }
	 */

	/**
	 * 設定當前 HP
	 * <p>
	 * 設定怪物的當前 HP 值。若設定值超過最大 HP，則自動調整為最大 HP。
	 * 若 HP 低於最大值，則啟動 HP 回復機制。
	 * </p>
	 *
	 * @param i 要設定的 HP 值
	 * @see L1Character#setCurrentHp(int)
	 * @see #startHpRegeneration()
	 */
	@Override
	public void setCurrentHp(int i) {
		int currentHp = i;
		if (currentHp >= getMaxHp()) {
			currentHp = getMaxHp();
		}
		setCurrentHpDirect(currentHp);

		if (getMaxHp() > getCurrentHp()) {
			startHpRegeneration();
		}
	}

	/**
	 * 設定當前 MP
	 * <p>
	 * 設定怪物的當前 MP 值。若設定值超過最大 MP，則自動調整為最大 MP。
	 * 若 MP 低於最大值，則啟動 MP 回復機制。
	 * </p>
	 *
	 * @param i 要設定的 MP 值
	 * @see L1Character#setCurrentMp(int)
	 * @see #startMpRegeneration()
	 */
	@Override
	public void setCurrentMp(int i) {
		int currentMp = i;
		if (currentMp >= getMaxMp()) {
			currentMp = getMaxMp();
		}
		setCurrentMpDirect(currentMp);

		if (getMaxMp() > getCurrentMp()) {
			startMpRegeneration();
		}
	}

	/**
	 * 怪物死亡處理執行緒
	 * <p>
	 * 負責處理怪物死亡後的所有相關邏輯，包括動畫播放、經驗值分配、
	 * 掉落物品生成、Karma 分配、無限大戰勇者之證發放等。
	 * </p>
	 *
	 * <h4>處理流程：</h4>
	 * <ol>
	 * <li>標記為死亡處理中</li>
	 * <li>設定 HP 為 0，設定死亡狀態</li>
	 * <li>設定地圖該位置為可通行</li>
	 * <li>廣播死亡動畫</li>
	 * <li>處理變形怪還原</li>
	 * <li>觸發死亡時的對話</li>
	 * <li>分配經驗值、掉落物品、Karma</li>
	 * <li>發放無限大戰勇者之證（如適用）</li>
	 * <li>清除所有目標和仇恨值</li>
	 * <li>啟動刪除計時器</li>
	 * </ol>
	 *
	 * @see #distributeExpDropKarma(L1Character)
	 * @see #giveUbSeal()
	 * @see L1NpcInstance#startDeleteTimer()
	 */
	class Death implements Runnable {
		/** 最後的攻擊者 */
		L1Character _lastAttacker;

		/**
		 * 建構子
		 *
		 * @param lastAttacker 最後的攻擊者
		 */
		public Death(L1Character lastAttacker) {
			_lastAttacker = lastAttacker;
		}

		/**
		 * 執行死亡處理
		 */
		@Override
		public void run() {
			setDeathProcessing(true);
			setCurrentHpDirect(0);
			setDead(true);
			setStatus(ActionCodes.ACTION_Die);

			getMap().setPassable(getLocation(), true);

			broadcastPacket(new S_DoActionGFX(getId(), ActionCodes.ACTION_Die));
			// 變形判斷
			onDoppel(false);

			startChat(CHAT_TIMING_DEAD);

			distributeExpDropKarma(_lastAttacker);
			giveUbSeal();

			setDeathProcessing(false);

			setExp(0);
			setKarma(0);
			allTargetClear();

			startDeleteTimer();
		}
	}

	/**
	 * 分配經驗值、掉落物品與 Karma
	 * <p>
	 * 當怪物死亡或變身時調用。根據仇恨列表分配經驗值，
	 * 若怪物死亡（非變身）則額外分配掉落物品和 Karma。
	 * </p>
	 *
	 * <h4>處理邏輯：</h4>
	 * <ol>
	 * <li>判斷最後攻擊者類型（玩家、寵物、召喚獸）</li>
	 * <li>取得仇恨列表</li>
	 * <li>根據仇恨值計算並分配經驗值</li>
	 * <li>若怪物死亡（isDead() == true），分配掉落物品與 Karma</li>
	 * <li>若怪物被 FW（力場）擊殺，找出最大仇恨者並視為擊殺者</li>
	 * </ol>
	 *
	 * @param lastAttacker 最後的攻擊者
	 * @see CalcExp#calcExp(L1PcInstance, int, ArrayList, ArrayList, int)
	 * @see #distributeDrop()
	 * @see #giveKarma(L1PcInstance)
	 */
	private void distributeExpDropKarma(L1Character lastAttacker) {
		if (lastAttacker == null) {
			return;
		}
		L1PcInstance pc = null;
		if (lastAttacker instanceof L1PcInstance) {
			pc = (L1PcInstance) lastAttacker;
		} else if (lastAttacker instanceof L1PetInstance) {
			pc = (L1PcInstance) ((L1PetInstance) lastAttacker).getMaster();
		} else if (lastAttacker instanceof L1SummonInstance) {
			pc = (L1PcInstance) ((L1SummonInstance) lastAttacker).getMaster();
		}

		if (pc != null) {
			ArrayList<L1Character> targetList = _hateList.toTargetArrayList();
			ArrayList<Integer> hateList = _hateList.toHateArrayList();
			int exp = getExp();
			CalcExp.calcExp(pc, getId(), targetList, hateList, exp);
			// 死亡した場合はドロップとカルマも分配、死亡せず変身した場合はEXPのみ
			if (isDead()) {
				distributeDrop();
				giveKarma(pc);
			}
		} else if (lastAttacker instanceof L1EffectInstance) { // FWが倒した場合
			ArrayList<L1Character> targetList = _hateList.toTargetArrayList();
			ArrayList<Integer> hateList = _hateList.toHateArrayList();
			// ヘイトリストにキャラクターが存在する
			if (!hateList.isEmpty()) {
				// 最大ヘイトを持つキャラクターが倒したものとする
				int maxHate = 0;
				for (int i = hateList.size() - 1; i >= 0; i--) {
					if (maxHate < (hateList.get(i))) {
						maxHate = (hateList.get(i));
						lastAttacker = targetList.get(i);
					}
				}
				if (lastAttacker instanceof L1PcInstance) {
					pc = (L1PcInstance) lastAttacker;
				} else if (lastAttacker instanceof L1PetInstance) {
					pc = (L1PcInstance) ((L1PetInstance) lastAttacker)
							.getMaster();
				} else if (lastAttacker instanceof L1SummonInstance) {
					pc = (L1PcInstance) ((L1SummonInstance) lastAttacker)
							.getMaster();
				}
				if (pc != null) {
					int exp = getExp();
					CalcExp.calcExp(pc, getId(), targetList, hateList, exp);
					// 死亡した場合はドロップとカルマも分配、死亡せず変身した場合はEXPのみ
					if (isDead()) {
						distributeDrop();
						giveKarma(pc);
					}
				}
			}
		}
	}

	/**
	 * 分配掉落物品
	 * <p>
	 * 根據掉落仇恨列表分配怪物的掉落物品。使用 DropTable 的共享機制，
	 * 依據各角色的仇恨值分配掉落權重。
	 * </p>
	 *
	 * <h4>特殊處理：</h4>
	 * <ul>
	 * <li>NPC ID 45640 且外觀非 2332 時不掉落物品</li>
	 * </ul>
	 *
	 * @see DropTable#dropShare(L1MonsterInstance, ArrayList, ArrayList)
	 */
	private void distributeDrop() {
		ArrayList<L1Character> dropTargetList = _dropHateList
				.toTargetArrayList();
		ArrayList<Integer> dropHateList = _dropHateList.toHateArrayList();
		try {
			int npcId = getNpcTemplate().get_npcId();
			if ((npcId != 45640)
					|| ((npcId == 45640) && (getTempCharGfx() == 2332))) {
				DropTable.getInstance().dropShare(L1MonsterInstance.this,
						dropTargetList, dropHateList);
			}
		} catch (Exception e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	/**
	 * 給予 Karma 值
	 * <p>
	 * 根據怪物的 Karma 值和玩家的 Karma 等級，給予玩家相應的 Karma 獎勵或懲罰。
	 * </p>
	 *
	 * <h4>計算規則：</h4>
	 * <ul>
	 * <li><b>一般情況</b>：Karma = 怪物 Karma × 伺服器倍率</li>
	 * <li><b>背信行為</b>：若玩家與怪物 Karma 符號相反（例如正義玩家擊殺善良怪物），
	 * Karma 變化量 × 5</li>
	 * </ul>
	 *
	 * <h4>背信範例：</h4>
	 * <ul>
	 * <li>正義玩家（Karma > 0）擊殺善良怪物（Karma > 0）→ Karma × 5 倍懲罰</li>
	 * <li>邪惡玩家（Karma < 0）擊殺邪惡怪物（Karma < 0）→ Karma × 5 倍懲罰</li>
	 * </ul>
	 *
	 * @param pc 擊殺怪物的玩家
	 * @see L1PcInstance#addKarma(int)
	 */
	private void giveKarma(L1PcInstance pc) {
		int karma = getKarma();
		if (karma != 0) {
			int karmaSign = Integer.signum(karma);
			int pcKarmaLevel = pc.getKarmaLevel();
			int pcKarmaLevelSign = Integer.signum(pcKarmaLevel);
			// カルマ背信行為は5倍
			if ((pcKarmaLevelSign != 0) && (karmaSign != pcKarmaLevelSign)) {
				karma *= 5;
			}
			// カルマは止めを刺したプレイヤーに設定。ペットorサモンで倒した場合も入る。
			pc.addKarma((int) (karma * Config.RATE_KARMA));
		}
	}

	/**
	 * 發放無限大戰勇者之證
	 * <p>
	 * 當怪物在無限大戰副本中被擊殺時，發放勇者之證（物品 ID: 41402）
	 * 給所有存活的副本參與者。
	 * </p>
	 *
	 * <h4>發放條件：</h4>
	 * <ul>
	 * <li>怪物設定了勇者之證數量（_ubSealCount > 0）</li>
	 * <li>怪物屬於有效的無限大戰副本</li>
	 * <li>參與者存活且非幽靈狀態</li>
	 * </ul>
	 *
	 * @see #getUbSealCount()
	 * @see #getUbId()
	 * @see L1UltimateBattle
	 */
	private void giveUbSeal() {
		if (getUbSealCount() != 0) { // UBの勇者の証
			L1UltimateBattle ub = UBTable.getInstance().getUb(getUbId());
			if (ub != null) {
				for (L1PcInstance pc : ub.getMembersArray()) {
					if ((pc != null) && !pc.isDead() && !pc.isGhost()) {
						L1ItemInstance item = pc.getInventory().storeItem(
								41402, getUbSealCount());
						pc.sendPackets(new S_ServerMessage(403, item
								.getLogName())); // %0を手に入れました。
					}
				}
			}
		}
	}

	/**
	 * 取得掉落物品是否已載入
	 *
	 * @return true 表示掉落物品已載入完成，false 表示尚未載入
	 */
	public boolean is_storeDroped() {
		return _storeDroped;
	}

	/**
	 * 設定掉落物品載入狀態
	 *
	 * @param flag true 表示掉落物品已載入，false 表示尚未載入
	 */
	public void set_storeDroped(boolean flag) {
		_storeDroped = flag;
	}

	/**
	 * 無限大戰勇者之證數量
	 * <p>
	 * 當怪物在無限大戰（UB）中被擊殺時，發放給參加者的勇者之證數量。
	 * </p>
	 */
	private int _ubSealCount = 0; // UBで倒された時、参加者に与えられる勇者の証の個数

	/**
	 * 取得勇者之證數量
	 *
	 * @return 勇者之證數量
	 */
	public int getUbSealCount() {
		return _ubSealCount;
	}

	/**
	 * 設定勇者之證數量
	 *
	 * @param i 勇者之證數量
	 */
	public void setUbSealCount(int i) {
		_ubSealCount = i;
	}

	/**
	 * 無限大戰 ID
	 * <p>
	 * 標識怪物所屬的無限大戰副本 ID。
	 * </p>
	 */
	private int _ubId = 0; // UBID

	/**
	 * 取得無限大戰 ID
	 *
	 * @return 無限大戰 ID
	 */
	public int getUbId() {
		return _ubId;
	}

	/**
	 * 設定無限大戰 ID
	 *
	 * @param i 無限大戰 ID
	 */
	public void setUbId(int i) {
		_ubId = i;
	}

	/**
	 * 怪物隱藏機制
	 * <p>
	 * 當怪物 HP 低於特定閾值時，部分怪物會隱藏（潛入地下或飛到空中）。
	 * 隱藏期間怪物無法被攻擊，並清除所有目標。
	 * </p>
	 *
	 * <h4>支援的怪物類型：</h4>
	 * <ul>
	 * <li><b>潛入地下（SINK）</b>：
	 *   <ul>
	 *   <li>斯巴達（45061, 45161, 45181, 45455）：HP < 最大 HP / 3 時，20% 機率潛入</li>
	 *   <li>安塔瑞斯（45682）：HP < 最大 HP / 3 時，2% 機率潛入</li>
	 *   <li>曼德拉草（46107, 46108）：HP < 最大 HP / 4 時，20% 機率潛入</li>
	 *   </ul>
	 * </li>
	 * <li><b>飛到空中（FLY）</b>：
	 *   <ul>
	 *   <li>哈比、獅鷲（45067, 45264, 45452, 45090, 45321, 45445）：HP < 最大 HP / 3 時，20% 機率飛起</li>
	 *   <li>林德維爾（45681）：HP < 最大 HP / 3 時，2% 機率飛起</li>
	 *   </ul>
	 * </li>
	 * </ul>
	 *
	 * @see L1NpcInstance#setHiddenStatus(int)
	 * @see L1NpcInstance#HIDDEN_STATUS_SINK
	 * @see L1NpcInstance#HIDDEN_STATUS_FLY
	 */
	private void hide() {
		int npcid = getNpcTemplate().get_npcId();
		if ((npcid == 45061 // カーズドスパルトイ
				)
				|| (npcid == 45161 // スパルトイ
				) || (npcid == 45181 // スパルトイ
				) || (npcid == 45455)) { // デッドリースパルトイ
			if (getMaxHp() / 3 > getCurrentHp()) {
				int rnd = Random.nextInt(10);
				if (2 > rnd) {
					allTargetClear();
					setHiddenStatus(HIDDEN_STATUS_SINK);
					broadcastPacket(new S_DoActionGFX(getId(),
							ActionCodes.ACTION_Hide));
					setStatus(11);
					broadcastPacket(new S_CharVisualUpdate(this, getStatus()));
				}
			}
		} else if (npcid == 45682) { // アンタラス
			if (getMaxHp() / 3 > getCurrentHp()) {
				int rnd = Random.nextInt(50);
				if (1 > rnd) {
					allTargetClear();
					setHiddenStatus(HIDDEN_STATUS_SINK);
					broadcastPacket(new S_DoActionGFX(getId(),
							ActionCodes.ACTION_AntharasHide));
					setStatus(20);
					broadcastPacket(new S_CharVisualUpdate(this, getStatus()));
				}
			}
		} else if ((npcid == 45067 // バレーハーピー
				)
				|| (npcid == 45264 // ハーピー
				) || (npcid == 45452 // ハーピー
				) || (npcid == 45090 // バレーグリフォン
				) || (npcid == 45321 // グリフォン
				) || (npcid == 45445)) { // グリフォン
			if (getMaxHp() / 3 > getCurrentHp()) {
				int rnd = Random.nextInt(10);
				if (2 > rnd) {
					allTargetClear();
					setHiddenStatus(HIDDEN_STATUS_FLY);
					broadcastPacket(new S_DoActionGFX(getId(),
							ActionCodes.ACTION_Moveup));
				}
			}
		} else if (npcid == 45681) { // リンドビオル
			if (getMaxHp() / 3 > getCurrentHp()) {
				int rnd = Random.nextInt(50);
				if (1 > rnd) {
					allTargetClear();
					setHiddenStatus(HIDDEN_STATUS_FLY);
					broadcastPacket(new S_DoActionGFX(getId(),
							ActionCodes.ACTION_Moveup));
				}
			}
		} else if ((npcid == 46107 // テーベ マンドラゴラ(白)
				)
				|| (npcid == 46108)) { // テーベ マンドラゴラ(黒)
			if (getMaxHp() / 4 > getCurrentHp()) {
				int rnd = Random.nextInt(10);
				if (2 > rnd) {
					allTargetClear();
					setHiddenStatus(HIDDEN_STATUS_SINK);
					broadcastPacket(new S_DoActionGFX(getId(),
							ActionCodes.ACTION_Hide));
					setStatus(11);
					broadcastPacket(new S_CharVisualUpdate(this, getStatus()));
				}
			}
		}
	}

	/**
	 * 初始化怪物的隱藏狀態
	 * <p>
	 * 當怪物剛生成時調用。部分怪物類型會以隱藏狀態出現。
	 * </p>
	 *
	 * <h4>初始隱藏機率：</h4>
	 * <ul>
	 * <li><b>潛入地下（SINK）</b>：
	 *   <ul>
	 *   <li>斯巴達（45061, 45161, 45181, 45455）：33% 機率初始潛入</li>
	 *   <li>石頭人（45045, 45126, 45134, 45281）：33% 機率初始潛入</li>
	 *   <li>曼德拉草（46107, 46108）：33% 機率初始潛入</li>
	 *   </ul>
	 * </li>
	 * <li><b>飛在空中（FLY）</b>：
	 *   <ul>
	 *   <li>哈比、獅鷲（45067, 45264, 45452, 45090, 45321, 45445）：100% 初始飛行</li>
	 *   <li>林德維爾（45681）：100% 初始飛行</li>
	 *   </ul>
	 * </li>
	 * <li><b>冰凍狀態（ICE）</b>：
	 *   <ul>
	 *   <li>冰凍怪物（46125-46128）：100% 初始冰凍</li>
	 *   </ul>
	 * </li>
	 * </ul>
	 *
	 * @see L1NpcInstance#setHiddenStatus(int)
	 */
	public void initHide() {
		// 出現直後の隠れる動作
		// 潜るMOBは一定の確率で地中に潜った状態に、
		// 飛ぶMOBは飛んだ状態にしておく
		int npcid = getNpcTemplate().get_npcId();
		if ((npcid == 45061 // カーズドスパルトイ
				)
				|| (npcid == 45161 // スパルトイ
				) || (npcid == 45181 // スパルトイ
				) || (npcid == 45455)) { // デッドリースパルトイ
			int rnd = Random.nextInt(3);
			if (1 > rnd) {
				setHiddenStatus(HIDDEN_STATUS_SINK);
				setStatus(11);
			}
		} else if ((npcid == 45045 // クレイゴーレム
				)
				|| (npcid == 45126 // ストーンゴーレム
				) || (npcid == 45134 // ストーンゴーレム
				) || (npcid == 45281)) { // ギランストーンゴーレム
			int rnd = Random.nextInt(3);
			if (1 > rnd) {
				setHiddenStatus(HIDDEN_STATUS_SINK);
				setStatus(4);
			}
		} else if ((npcid == 45067 // バレーハーピー
				)
				|| (npcid == 45264 // ハーピー
				) || (npcid == 45452 // ハーピー
				) || (npcid == 45090 // バレーグリフォン
				) || (npcid == 45321 // グリフォン
				) || (npcid == 45445)) { // グリフォン
			setHiddenStatus(HIDDEN_STATUS_FLY);
		} else if (npcid == 45681) { // リンドビオル
			setHiddenStatus(HIDDEN_STATUS_FLY);
		} else if ((npcid == 46107 // テーベ マンドラゴラ(白)
				)
				|| (npcid == 46108)) { // テーベ マンドラゴラ(黒)
			int rnd = Random.nextInt(3);
			if (1 > rnd) {
				setHiddenStatus(HIDDEN_STATUS_SINK);
				setStatus(11);
			}
		} else if ((npcid >= 46125) && (npcid <= 46128)) {
			setHiddenStatus(L1NpcInstance.HIDDEN_STATUS_ICE);
			setStatus(4);
		}
	}

	/**
	 * 初始化隨從怪物的隱藏狀態
	 * <p>
	 * 當怪物作為群組成員生成時調用。隨從怪物會模仿領隊的隱藏狀態，
	 * 確保整個怪物群組保持一致的行為。
	 * </p>
	 *
	 * <h4>模仿規則：</h4>
	 * <ul>
	 * <li><b>領隊潛入地下（SINK）</b>：
	 *   <ul>
	 *   <li>斯巴達隨從 → 同樣潛入地下</li>
	 *   <li>石頭人隨從 → 同樣潛入地下</li>
	 *   <li>曼德拉草隨從 → 同樣潛入地下</li>
	 *   </ul>
	 * </li>
	 * <li><b>領隊飛在空中（FLY）</b>：
	 *   <ul>
	 *   <li>哈比、獅鷲隨從 → 同樣飛到空中</li>
	 *   <li>林德維爾隨從 → 同樣飛到空中</li>
	 *   </ul>
	 * </li>
	 * <li><b>冰凍狀態（ICE）</b>：
	 *   <ul>
	 *   <li>冰凍怪物隨從（46125-46128）→ 同樣保持冰凍</li>
	 *   </ul>
	 * </li>
	 * </ul>
	 *
	 * @param leader 群組領隊
	 * @see #initHide()
	 * @see L1NpcInstance#getHiddenStatus()
	 */
	public void initHideForMinion(L1NpcInstance leader) {
		// グループに属するモンスターの出現直後の隠れる動作（リーダーと同じ動作にする）
		int npcid = getNpcTemplate().get_npcId();
		if (leader.getHiddenStatus() == HIDDEN_STATUS_SINK) {
			if ((npcid == 45061 // カーズドスパルトイ
					)
					|| (npcid == 45161 // スパルトイ
					) || (npcid == 45181 // スパルトイ
					) || (npcid == 45455)) { // デッドリースパルトイ
				setHiddenStatus(HIDDEN_STATUS_SINK);
				setStatus(11);
			} else if ((npcid == 45045 // クレイゴーレム
					)
					|| (npcid == 45126 // ストーンゴーレム
					) || (npcid == 45134 // ストーンゴーレム
					) || (npcid == 45281)) { // ギランストーンゴーレム
				setHiddenStatus(HIDDEN_STATUS_SINK);
				setStatus(4);
			} else if ((npcid == 46107 // テーベ マンドラゴラ(白)
					)
					|| (npcid == 46108)) { // テーベ マンドラゴラ(黒)
				setHiddenStatus(HIDDEN_STATUS_SINK);
				setStatus(11);
			}
		} else if (leader.getHiddenStatus() == HIDDEN_STATUS_FLY) {
			if ((npcid == 45067 // バレーハーピー
					)
					|| (npcid == 45264 // ハーピー
					) || (npcid == 45452 // ハーピー
					) || (npcid == 45090 // バレーグリフォン
					) || (npcid == 45321 // グリフォン
					) || (npcid == 45445)) { // グリフォン
				setHiddenStatus(HIDDEN_STATUS_FLY);
				setStatus(4);
			} else if (npcid == 45681) { // リンドビオル
				setHiddenStatus(HIDDEN_STATUS_FLY);
				setStatus(11);
			}
		} else if ((npcid >= 46125) && (npcid <= 46128)) {
			setHiddenStatus(L1NpcInstance.HIDDEN_STATUS_ICE);
			setStatus(4);
		}
	}

	/**
	 * 怪物變形
	 * <p>
	 * 當怪物 HP 歸零但有變形設定時，會變形為另一個怪物。
	 * 變形後會重新設定掉落物品清單。
	 * </p>
	 *
	 * <h4>處理流程：</h4>
	 * <ol>
	 * <li>調用父類別的變形邏輯（更新外觀、屬性等）</li>
	 * <li>清空原有的掉落物品</li>
	 * <li>重新載入變形後的掉落物品</li>
	 * <li>隨機打亂掉落順序</li>
	 * </ol>
	 *
	 * @param transformId 變形目標的 NPC Template ID
	 * @see L1NpcInstance#transform(int)
	 * @see DropTable#setDrop(L1NpcInstance, L1Inventory)
	 */
	@Override
	protected void transform(int transformId) {
		super.transform(transformId);

		// DROPの再設定
		getInventory().clearItems();
		DropTable.getInstance().setDrop(this, getInventory());
		getInventory().shuffle();
	}

	/** 屠龍副本階段變化是否正在執行中 */
	private boolean _nextDragonStepRunning = false;

	/**
	 * 設定屠龍副本階段變化執行狀態
	 *
	 * @param nextDragonStepRunning true 表示階段變化正在執行，false 表示未執行
	 */
	protected void setNextDragonStepRunning(boolean nextDragonStepRunning) {
		_nextDragonStepRunning = nextDragonStepRunning;
	}

	/**
	 * 取得屠龍副本階段變化執行狀態
	 *
	 * @return true 表示階段變化正在執行，false 表示未執行
	 */
	protected boolean isNextDragonStepRunning() {
		return _nextDragonStepRunning;
	}

	/**
	 * 施放龍之血痕
	 * <p>
	 * 當屠龍副本的最終 BOSS（安塔瑞斯或法利昂）死亡時，
	 * 對周圍 50 格範圍內的所有玩家施放血痕效果。
	 * </p>
	 *
	 * <h4>血痕效果：</h4>
	 * <ul>
	 * <li><b>安塔瑞斯之血痕（NPC 97008）</b>：
	 *   <ul>
	 *   <li>類型：0（安塔瑞斯）</li>
	 *   <li>持續時間：4320 分鐘（3 天）</li>
	 *   <li>伺服器訊息：1580（安塔瑞斯的遺言）</li>
	 *   </ul>
	 * </li>
	 * <li><b>法利昂之血痕（NPC 97046）</b>：
	 *   <ul>
	 *   <li>類型：1（法利昂）</li>
	 *   <li>持續時間：4320 分鐘（3 天）</li>
	 *   <li>伺服器訊息：1668（法利昂的遺言）</li>
	 *   </ul>
	 * </li>
	 * </ul>
	 *
	 * @see L1BuffUtil#bloodstain(L1PcInstance, byte, int, boolean)
	 */
	private void bloodstain() {
		for (L1PcInstance pc : L1World.getInstance().getVisiblePlayer(this, 50)) {
			if (getNpcTemplate().get_npcId() == 97008) {
				pc.sendPackets(new S_ServerMessage(1580)); // 安塔瑞斯：黑暗的詛咒將會降臨到你們身上！席琳，
															// 我的母親，請讓我安息吧...
				L1BuffUtil.bloodstain(pc, (byte) 0, 4320, true);
			} else if (getNpcTemplate().get_npcId() == 97046) {
				pc.sendPackets(new S_ServerMessage(1668)); // 法利昂：莎爾...你這個傢伙...怎麼...對得起我的母親...席琳啊...請拿走...我的生命吧...
				L1BuffUtil.bloodstain(pc, (byte) 1, 4320, true);
			}
		}
	}

	/**
	 * 執行屠龍副本的下一階段
	 * <p>
	 * 當屠龍副本的特定 BOSS（安塔瑞斯/法利昂的前幾階段）死亡時，
	 * 延遲後生成下一階段的 BOSS。僅在非副本模式下生效。
	 * </p>
	 *
	 * <h4>階段變化對應表：</h4>
	 * <ul>
	 * <li>97006（安塔瑞斯階段一）→ 97007（安塔瑞斯階段二）</li>
	 * <li>97007（安塔瑞斯階段二）→ 97008（安塔瑞斯階段三/最終）</li>
	 * <li>97044（法利昂階段一）→ 97045（法利昂階段二）</li>
	 * <li>97045（法利昂階段二）→ 97046（法利昂階段三/最終）</li>
	 * </ul>
	 *
	 * <p>
	 * 註：僅當 PortalNumber == -1（非副本模式）時才會自動進入下一階段。
	 * </p>
	 *
	 * @param attacker 擊殺者
	 * @param npcid 當前死亡的 BOSS NPC ID
	 * @see NextDragonStep
	 */
	private void doNextDragonStep(L1Character attacker, int npcid) {
		if (!isNextDragonStepRunning()) {
			int[] dragonId = { 97006, 97007, 97044, 97045 };
			int[] nextStepId = { 97007, 97008, 97045, 97046 };
			int nextSpawnId = 0;
			for (int i = 0; i < dragonId.length; i++) {
				if (npcid == dragonId[i]) {
					nextSpawnId = nextStepId[i];
					break;
				}
			}
			if (attacker != null && nextSpawnId > 0) {
				L1PcInstance _pc = null;
				if (attacker instanceof L1PcInstance) {
					_pc = (L1PcInstance) attacker;
				} else if (attacker instanceof L1PetInstance) {
					L1PetInstance pet = (L1PetInstance) attacker;
					L1Character cha = pet.getMaster();
					if (cha instanceof L1PcInstance) {
						_pc = (L1PcInstance) cha;
					}
				} else if (attacker instanceof L1SummonInstance) {
					L1SummonInstance summon = (L1SummonInstance) attacker;
					L1Character cha = summon.getMaster();
					if (cha instanceof L1PcInstance) {
						_pc = (L1PcInstance) cha;
					}
				}
				if (_pc != null) {
					NextDragonStep nextDragonStep = new NextDragonStep(_pc,
							this, nextSpawnId);
					GeneralThreadPool.getInstance().execute(nextDragonStep);
				}
			}
		}
	}

	/**
	 * 屠龍副本下一階段執行緒
	 * <p>
	 * 負責在延遲後生成下一階段的 BOSS。延遲 10.5 秒後在原 BOSS 位置
	 * 生成新的 BOSS，並播放出現動畫。
	 * </p>
	 *
	 * <h4>執行流程：</h4>
	 * <ol>
	 * <li>設定階段變化標記為執行中</li>
	 * <li>延遲 10.5 秒</li>
	 * <li>建立新的 BOSS 實例</li>
	 * <li>設定位置、方向、地圖等屬性</li>
	 * <li>廣播 BOSS 出現封包與動畫</li>
	 * <li>將 BOSS 加入世界</li>
	 * <li>觸發出現時的對話</li>
	 * <li>清除階段變化標記</li>
	 * </ol>
	 *
	 * @see #doNextDragonStep(L1Character, int)
	 * @see L1NpcInstance#startChat(int)
	 */
	class NextDragonStep implements Runnable {
		/** 擊殺者 */
		L1PcInstance _pc;

		/** 當前怪物實例 */
		L1MonsterInstance _mob;

		/** 怪物 NPC ID */
		int _npcid;

		/** 下一階段的 NPC Template ID */
		int _transformId;

		/** X 座標 */
		int _x;

		/** Y 座標 */
		int _y;

		/** 面向 */
		int _h;

		/** 地圖 ID */
		short _m;

		/** 位置物件 */
		L1Location _loc = new L1Location();

		/**
		 * 建構子
		 *
		 * @param pc 擊殺者
		 * @param mob 當前怪物實例
		 * @param transformId 下一階段的 NPC Template ID
		 */
		public NextDragonStep(L1PcInstance pc, L1MonsterInstance mob,
				int transformId) {
			_pc = pc;
			_mob = mob;
			_transformId = transformId;
			_x = mob.getX();
			_y = mob.getY();
			_h = mob.getHeading();
			_m = mob.getMapId();
			_loc = mob.getLocation();
		}

		/**
		 * 執行階段變化
		 */
		@Override
		public void run() {
			setNextDragonStepRunning(true);
			try {
				Thread.sleep(10500);
				L1NpcInstance npc = NpcTable.getInstance().newNpcInstance(
						_transformId);
				npc.setId(IdFactory.getInstance().nextId());
				npc.setMap((short) _m);
				npc.setHomeX(_x);
				npc.setHomeY(_y);
				npc.setHeading(_h);
				npc.getLocation().set(_loc);
				npc.getLocation().forward(_h);
				npc.setPortalNumber(getPortalNumber());

				broadcastPacket(new S_NPCPack(npc));
				broadcastPacket(new S_DoActionGFX(npc.getId(),
						ActionCodes.ACTION_Hide));

				L1World.getInstance().storeObject(npc);
				L1World.getInstance().addVisibleObject(npc);
				npc.turnOnOffLight();
				npc.startChat(L1NpcInstance.CHAT_TIMING_APPEARANCE); // チャット開始
				setNextDragonStepRunning(false);
			} catch (InterruptedException e) {
			}
		}
	}
}
