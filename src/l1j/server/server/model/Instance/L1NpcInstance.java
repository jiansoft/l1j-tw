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

import static l1j.server.server.model.identity.L1ItemId.B_POTION_OF_GREATER_HASTE_SELF;
import static l1j.server.server.model.identity.L1ItemId.B_POTION_OF_HASTE_SELF;
import static l1j.server.server.model.identity.L1ItemId.POTION_OF_EXTRA_HEALING;
import static l1j.server.server.model.identity.L1ItemId.POTION_OF_GREATER_HASTE_SELF;
import static l1j.server.server.model.identity.L1ItemId.POTION_OF_GREATER_HEALING;
import static l1j.server.server.model.identity.L1ItemId.POTION_OF_HASTE_SELF;
import static l1j.server.server.model.identity.L1ItemId.POTION_OF_HEALING;
import static l1j.server.server.model.skill.L1SkillId.CANCELLATION;
import static l1j.server.server.model.skill.L1SkillId.COUNTER_BARRIER;
import static l1j.server.server.model.skill.L1SkillId.POLLUTE_WATER;
import static l1j.server.server.model.skill.L1SkillId.STATUS_HASTE;
import static l1j.server.server.model.skill.L1SkillId.WIND_SHACKLE;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.Config;
import l1j.server.server.ActionCodes;
import l1j.server.server.GeneralThreadPool;
import l1j.server.server.datatables.NpcChatTable;
import l1j.server.server.datatables.NpcTable;
import l1j.server.server.datatables.SprTable;
import l1j.server.server.model.L1Attack;
import l1j.server.server.model.L1Character;
import l1j.server.server.model.L1GroundInventory;
import l1j.server.server.model.L1HateList;
import l1j.server.server.model.L1Inventory;
import l1j.server.server.model.L1Magic;
import l1j.server.server.model.L1MobGroupInfo;
import l1j.server.server.model.L1MobSkillUse;
import l1j.server.server.model.L1NpcChatTimer;
import l1j.server.server.model.L1NpcRegenerationTimer;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1Spawn;
import l1j.server.server.model.L1World;
import l1j.server.server.model.map.L1Map;
import l1j.server.server.model.map.L1WorldMap;
import l1j.server.server.model.npc.action.L1NpcDefaultAction;
import l1j.server.server.model.skill.L1SkillUse;
import l1j.server.server.serverpackets.S_CharVisualUpdate;
import l1j.server.server.serverpackets.S_DoActionGFX;
import l1j.server.server.serverpackets.S_MoveCharPacket;
import l1j.server.server.serverpackets.S_NPCPack;
import l1j.server.server.serverpackets.S_NpcChangeShape;
import l1j.server.server.serverpackets.S_RemoveObject;
import l1j.server.server.serverpackets.S_SkillHaste;
import l1j.server.server.serverpackets.S_SkillSound;
import l1j.server.server.templates.L1Npc;
import l1j.server.server.templates.L1NpcChat;
import l1j.server.server.types.Point;
import l1j.server.server.utils.Random;
import l1j.server.server.utils.TimerPool;
import l1j.server.server.utils.collections.Lists;
import l1j.server.server.utils.collections.Maps;

/**
 * NPC 實例類別
 * <p>
 * 這是遊戲中所有 NPC（Non-Player Character）的基礎類別，包括怪物、商人、守衛等。
 * 實現了完整的 NPC AI 系統，包括目標搜尋、移動、攻擊、技能使用等核心功能。
 * </p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 * <li><b>AI 系統</b>：實現自動化的 NPC 行為邏輯</li>
 * <li><b>目標管理</b>：搜尋、鎖定和追蹤攻擊目標</li>
 * <li><b>移動系統</b>：路徑尋找、移動執行、傳送</li>
 * <li><b>戰鬥系統</b>：物理攻擊、技能使用、道具使用</li>
 * <li><b>仇恨系統</b>：管理多目標仇恨值</li>
 * <li><b>物品管理</b>：撿拾、使用、掉落物品</li>
 * </ul>
 *
 * <h3>AI 系統架構：</h3>
 * <p>
 * AI 系統基於定時循環執行的 {@link #AIProcess()} 方法，支援兩種實現方式：
 * </p>
 * <ol>
 * <li><b>Timer 池實現</b>（NpcAITimerImpl）：使用共享 Timer 池，適合大量 NPC</li>
 * <li><b>執行緒實現</b>（NpcAIThreadImpl）：每個 NPC 使用獨立執行緒，適合重要 NPC</li>
 * </ol>
 *
 * <h3>使用範例：</h3>
 * <pre>
 * // 創建 NPC 實例
 * L1Npc template = NpcTable.getInstance().getTemplate(npcId);
 * L1NpcInstance npc = new L1MonsterInstance(template);
 *
 * // 設定位置並加入世界
 * npc.setX(x);
 * npc.setY(y);
 * npc.setMap(mapId);
 * L1World.getInstance().storeObject(npc);
 *
 * // 啟動 AI
 * npc.onNpcAI();
 * </pre>
 *
 * @author L1J-TW
 * @version 3.80c
 * @see L1Character
 * @see L1MonsterInstance
 * @see L1MerchantInstance
 * @see L1GuardInstance
 */
public class L1NpcInstance extends L1Character {
	private static final long serialVersionUID = 1L;

	// ========== 速度類型常數 ==========

	/** 移動速度類型 - 用於計算移動間隔時間 */
	public static final int MOVE_SPEED = 0;

	/** 攻擊速度類型 - 用於計算攻擊間隔時間 */
	public static final int ATTACK_SPEED = 1;

	/** 施法速度類型 - 用於計算施法間隔時間 */
	public static final int MAGIC_SPEED = 2;

	// ========== 隱藏狀態常數 ==========

	/** 正常狀態（可見） */
	public static final int HIDDEN_STATUS_NONE = 0;

	/** 地底狀態（遁地） - NPC 在地底移動，無法被攻擊 */
	public static final int HIDDEN_STATUS_SINK = 1;

	/** 飛行狀態 - NPC 在空中飛行，無視地形障礙 */
	public static final int HIDDEN_STATUS_FLY = 2;

	/** 冰凍狀態 - NPC 被冰凍封印 */
	public static final int HIDDEN_STATUS_ICE = 3;

	// ========== 聊天時機常數 ==========

	/** 聊天時機：出現時 */
	public static final int CHAT_TIMING_APPEARANCE = 0;

	/** 聊天時機：死亡時 */
	public static final int CHAT_TIMING_DEAD = 1;

	/** 聊天時機：隱藏時 */
	public static final int CHAT_TIMING_HIDE = 2;

	/** 聊天時機：遊戲時間觸發 */
	public static final int CHAT_TIMING_GAME_TIME = 3;

	/** 日誌記錄器 */
	private static Logger _log = Logger.getLogger(L1NpcInstance.class.getName());

	// ========== 核心屬性 ==========

	/** NPC 模板數據，包含 NPC 的基礎屬性（HP、MP、攻擊力等） */
	private L1Npc _npcTemplate;

	/** 生成點管理器，記錄 NPC 的生成位置和重生時間 */
	private L1Spawn _spawn;

	/** 生成編號，在 L1Spawn 中管理的編號 */
	private int _spawnNumber;

	/** 寵物消耗值，當 NPC 變成寵物時的消耗 */
	private int _petcost;

	/** NPC 背包，存儲 NPC 持有的物品和掉落物 */
	public L1Inventory _inventory = new L1Inventory();

	/** 技能使用管理器，管理 NPC 的技能釋放邏輯 */
	private L1MobSkillUse mobSkill;

	/** 是否首次發現目標（用於傳送判定） */
	private boolean firstFound = true;

	/**
	 * 路徑搜尋範圍（半徑）
	 * <p>
	 * 警告：數值越大計算越耗時，建議值：10-20
	 * </p>
	 */
	public static int courceRange = 15;

	/** 被吸取的 MP 數量 */
	private int _drainedMana = 0;

	/** 是否處於休息狀態 */
	private boolean _rest = false;

	/** 隨機移動時的距離 */
	private int _randomMoveDistance = 0;

	/** 隨機移動時的方向 */
	private int _randomMoveDirection = 0;

	// ========== AI 系統相關 ==========

	/**
	 * NPC AI 接口
	 * <p>
	 * 定義 AI 的基本啟動方法，由 NpcAITimerImpl 和 NpcAIThreadImpl 實現
	 * </p>
	 */
	interface NpcAI {
		/**
		 * 啟動 AI 執行
		 */
		public void start();
	}

	/**
	 * 啟動 AI 系統
	 * <p>
	 * 根據配置文件的 NpcAIImplType 參數選擇實現方式：
	 * <ul>
	 * <li>Type 1：使用 Timer 池實現（預設，適合大量 NPC）</li>
	 * <li>Type 2：使用獨立執行緒實現（適合重要 NPC）</li>
	 * </ul>
	 * </p>
	 *
	 * @see NpcAITimerImpl
	 * @see NpcAIThreadImpl
	 */
	protected void startAI() {
		if (Config.NPCAI_IMPLTYPE == 1) {
			new NpcAITimerImpl().start();
		} else if (Config.NPCAI_IMPLTYPE == 2) {
			new NpcAIThreadImpl().start();
		} else {
			new NpcAITimerImpl().start();
		}
	}

	/**
	 * 多核心處理器支援的 Timer 池
	 * <p>
	 * AI 實現類型為 Timer 時使用，共享 4 個 Timer 實例
	 * </p>
	 */
	private static final TimerPool _timerPool = new TimerPool(4);

	/**
	 * NPC AI Timer 池實現
	 * <p>
	 * 使用共享的 Timer 池來執行 AI 邏輯，降低執行緒開銷，適合大量 NPC。
	 * </p>
	 */
	class NpcAITimerImpl extends TimerTask implements NpcAI {
		/**
		 * 死亡同步計時器
		 * <p>
		 * 等待 NPC 死亡處理完成後再清理 AI 資源
		 * </p>
		 */
		private class DeathSyncTimer extends TimerTask {
			private void schedule(int delay) {
				_timerPool.getTimer().schedule(new DeathSyncTimer(), delay);
			}

			@Override
			public void run() {
				if (isDeathProcessing()) {
					schedule(getSleepTime());
					return;
				}
				allTargetClear();
				setAiRunning(false);
			}
		}

		@Override
		public void start() {
			setAiRunning(true);
			_timerPool.getTimer().schedule(NpcAITimerImpl.this, 0);
		}

		private void stop() {
			mobSkill.resetAllSkillUseCount();
			_timerPool.getTimer().schedule(new DeathSyncTimer(), 0); // 死亡同期を開始
		}

		// 同じインスタンスをTimerへ登録できない為、苦肉の策。
		private void schedule(int delay) {
			_timerPool.getTimer().schedule(new NpcAITimerImpl(), delay);
		}

		@Override
		public void run() {
			try {
				if (notContinued()) {
					stop();
					return;
				}

				// XXX 同期がとても怪しげな麻痺判定
				if (0 < _paralysisTime) {
					schedule(_paralysisTime);
					_paralysisTime = 0;
					setParalyzed(false);
					return;
				} else if (isParalyzed() || isSleeped()) {
					schedule(200);
					return;
				}

				if (!AIProcess()) { // AIを続けるべきであれば、次の実行をスケジュールし、終了
					schedule(getSleepTime());
					return;
				}
				stop();
			} catch (Exception e) {
				_log.log(Level.WARNING, "NpcAIで例外が発生しました。", e);
			}
		}

		private boolean notContinued() {
			return _destroyed || isDead() || (getCurrentHp() <= 0)
					|| (getHiddenStatus() != HIDDEN_STATUS_NONE);
		}
	}

	/**
	 * NPC AI 執行緒實現
	 * <p>
	 * 每個 NPC 使用獨立執行緒執行 AI 邏輯，提供更精確的時間控制，但執行緒開銷較高。
	 * 適合重要 NPC（如 Boss）使用。
	 * </p>
	 */
	class NpcAIThreadImpl implements Runnable, NpcAI {
		/**
		 * 啟動 AI 執行緒
		 */
		@Override
		public void start() {
			GeneralThreadPool.getInstance().execute(NpcAIThreadImpl.this);
		}

		/**
		 * AI 執行緒主循環
		 * <p>
		 * 持續執行 AI 邏輯直到 NPC 死亡或被銷毀
		 * </p>
		 */
		@Override
		public void run() {
			try {
				setAiRunning(true);
				while (!_destroyed && !isDead() && (getCurrentHp() > 0)
						&& (getHiddenStatus() == HIDDEN_STATUS_NONE)) {
					/*
					 * if (_paralysisTime > 0) { try {
					 * Thread.sleep(_paralysisTime); } catch (Exception
					 * exception) { break; } finally { setParalyzed(false);
					 * _paralysisTime = 0; } }
					 */
					while (isParalyzed() || isSleeped()) {
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							setParalyzed(false);
						}
					}

					if (AIProcess()) {
						break;
					}
					try {
						// 指定時間分スレッド停止
						Thread.sleep(getSleepTime());
					} catch (Exception e) {
						break;
					}
				}
				mobSkill.resetAllSkillUseCount();
				do {
					try {
						Thread.sleep(getSleepTime());
					} catch (Exception e) {
						break;
					}
				} while (isDeathProcessing());
				allTargetClear();
				setAiRunning(false);
			} catch (Exception e) {
				_log.log(Level.WARNING, "NpcAIで例外が発生しました。", e);
			}
		}
	}

	/**
	 * AI 核心處理邏輯
	 * <p>
	 * 每個 AI 週期執行一次（預設 300ms），處理目標搜尋、移動、攻擊等行為。
	 * </p>
	 *
	 * <h4>執行流程：</h4>
	 * <ol>
	 * <li>設定預設休眠時間（300ms）</li>
	 * <li>檢查當前目標有效性 {@link #checkTarget()}</li>
	 * <li>無目標且無主人時，搜尋新目標 {@link #searchTarget()}</li>
	 * <li>變形怪變形判定 {@link #onDoppel(boolean)}</li>
	 * <li>道具使用判定 {@link #onItemUse()}</li>
	 * <li>根據目標情況執行動作：
	 *     <ul>
	 *     <li>有目標：執行攻擊或移動 {@link #onTarget()}</li>
	 *     <li>無目標：搜尋物品或待機 {@link #noTarget()}</li>
	 *     </ul>
	 * </li>
	 * </ol>
	 *
	 * @return true 結束 AI 循環，false 繼續 AI 循環
	 */
	private boolean AIProcess() {
		setSleepTime(300);

		checkTarget();
		if ((_target == null) && (_master == null)) {
			// 無目標且無主人時，搜尋新目標
			// （有主人的寵物/召喚獸不會自動搜尋目標）
			searchTarget();
		}

		onDoppel(true);
		onItemUse();

		if (_target == null) {
			// 無目標時
			checkTargetItem();
			if (isPickupItem() && (_targetItem == null)) {
				// アイテム拾う子の場合はアイテムを探してみる
				searchTargetItem();
			}

			if (_targetItem == null) {
				if (noTarget()) {
					return true;
				}
			} else {
				// onTargetItem();
				L1Inventory groundInventory = L1World.getInstance()
						.getInventory(_targetItem.getX(), _targetItem.getY(),
								_targetItem.getMapId());
				if (groundInventory.checkItem(_targetItem.getItemId())) {
					onTargetItem();
				} else {
					_targetItemList.remove(_targetItem);
					_targetItem = null;
					setSleepTime(1000);
					return false;
				}
			}
		} else { // ターゲットがいる場合
			if (getHiddenStatus() == HIDDEN_STATUS_NONE) {
				onTarget();
			} else {
				return true;
			}
		}

		return false; // 繼續 AI 處理
	}

	/**
	 * 變形怪變形判定
	 * <p>
	 * 基類為空實現，由子類（如 L1MonsterInstance）覆寫實現變形邏輯。
	 * 變形怪會複製目標玩家的外觀、名稱等資訊。
	 * </p>
	 *
	 * @param isChangeShape true 變形，false 恢復原形
	 * @see L1MonsterInstance#onDoppel(boolean)
	 */
	public void onDoppel(boolean isChangeShape) {
	}

	/**
	 * 道具使用處理
	 * <p>
	 * 基類為空實現，由子類（如 L1MonsterInstance）覆寫實現道具使用邏輯。
	 * 例如：HP 低時使用回復藥水，戰鬥時使用加速藥水。
	 * </p>
	 *
	 * @see L1MonsterInstance#onItemUse()
	 */
	public void onItemUse() {
	}

	/**
	 * 搜尋攻擊目標
	 * <p>
	 * 基類實現為清除目標，由子類（如 L1MonsterInstance）覆寫實現具體搜尋邏輯。
	 * 子類實現通常包括：
	 * </p>
	 * <ul>
	 * <li>獲取可見範圍內的玩家</li>
	 * <li>根據主動性判定是否攻擊</li>
	 * <li>檢查陣營友好關係</li>
	 * <li>將目標加入仇恨列表</li>
	 * </ul>
	 *
	 * @see L1MonsterInstance#searchTarget()
	 */
	public void searchTarget() {
		tagertClear();
	}

	/**
	 * 檢查當前目標有效性
	 * <p>
	 * 驗證目標是否仍然可以攻擊，無效則從仇恨列表選擇次高仇恨目標。
	 * </p>
	 *
	 * <h4>判定條件：</h4>
	 * <ul>
	 * <li>目標為 null</li>
	 * <li>目標在不同地圖</li>
	 * <li>目標 HP ≤ 0 或已死亡</li>
	 * <li>目標隱形（且 NPC 不具備反隱形能力）</li>
	 * <li>目標距離超過 30 格</li>
	 * </ul>
	 */
	public void checkTarget() {
		if ((_target == null)
				|| (_target.getMapId() != getMapId())
				|| (_target.getCurrentHp() <= 0)
				|| _target.isDead()
				|| (_target.isInvisble() && !getNpcTemplate().is_agrocoi() && !_hateList
						.containsKey(_target))
				// 目標距離超過30以上
				|| _target.getTileLineDistance(this) > 30 ) { 
			if (_target != null) {
				tagertClear();
			}
			if (!_hateList.isEmpty()) {
				_target = _hateList.getMaxHateCharacter();
				checkTarget();
			}
		}
	}

	/**
	 * 設定對某個角色的仇恨值
	 * <p>
	 * 將角色加入仇恨列表並更新當前攻擊目標。首次攻擊的角色會獲得 FA 獎勵仇恨值。
	 * </p>
	 *
	 * <h4>仇恨值來源：</h4>
	 * <ul>
	 * <li>物理/魔法傷害：造成的實際傷害值</li>
	 * <li>治療隊友：治療量 × 0.5</li>
	 * <li>Buff 隊友：固定值</li>
	 * <li>首次攻擊 (FA)：額外獲得 MaxHP / 10 的獎勵仇恨值</li>
	 * </ul>
	 *
	 * @param cha 目標角色
	 * @param hate 仇恨值
	 */
	public void setHate(L1Character cha, int hate) {
		if ((cha != null) && (cha.getId() != getId())) {
			if (!isFirstAttack() && (hate != 0)) {
				hate += getMaxHp() / 10; // FA 獎勵仇恨值
				setFirstAttack(true);
			}

			_hateList.add(cha, hate);
			_dropHateList.add(cha, hate);
			_target = _hateList.getMaxHateCharacter();
			checkTarget();
		}
	}

	/**
	 * 設定連動目標
	 * <p>
	 * 基類為空實現，由子類（如 L1MonsterInstance）覆寫實現連動邏輯。
	 * </p>
	 *
	 * @param cha 連動目標
	 * @see L1MonsterInstance#setLink(L1Character)
	 */
	public void setLink(L1Character cha) {
	}

	/**
	 * 搜尋並連動附近的同族 NPC
	 * <p>
	 * 當玩家攻擊 NPC 時，觸發此方法讓附近的 NPC 一起加入戰鬥。
	 * </p>
	 *
	 * <h4>連動類型（由 agrofamily 欄位控制）：</h4>
	 * <ul>
	 * <li><b>0</b>：無連動</li>
	 * <li><b>1</b>：同族連動（只有相同 family 的 NPC 會支援）</li>
	 * <li><b>2</b>：全體連動（附近所有 NPC 都會加入戰鬥）</li>
	 * </ul>
	 *
	 * @param targetPlayer 攻擊者（玩家）
	 * @param family 族群編號
	 */
	public void serchLink(L1PcInstance targetPlayer, int family) {
		List<L1Object> targetKnownObjects = targetPlayer.getKnownObjects();
		for (Object knownObject : targetKnownObjects) {
			if (knownObject instanceof L1NpcInstance) {
				L1NpcInstance npc = (L1NpcInstance) knownObject;
				if (npc.getNpcTemplate().get_agrofamily() > 0) {
					// 仲間に対してアクティブになる場合
					if (npc.getNpcTemplate().get_agrofamily() == 1) {
						// 同種族に対してのみ仲間意識
						if (npc.getNpcTemplate().get_family() == family) {
							npc.setLink(targetPlayer);
						}
					} else {
						// 全てのＮＰＣに対して仲間意識
						npc.setLink(targetPlayer);
					}
				}
				L1MobGroupInfo mobGroupInfo = getMobGroupInfo();
				if (mobGroupInfo != null) {
					if ((getMobGroupId() != 0)
							&& (getMobGroupId() == npc.getMobGroupId())) { // 同じグループ
						npc.setLink(targetPlayer);
					}
				}
			}
		}
	}

	/**
	 * 對目標執行動作（移動或攻擊）
	 * <p>
	 * 根據 NPC 類型和目標距離決定行為：
	 * </p>
	 *
	 * <h4>決策邏輯：</h4>
	 * <ul>
	 * <li><b>逃跑型 NPC</b>（AtkSpeed == 0）：計算遠離方向並移動</li>
	 * <li><b>攻擊型 NPC</b>（AtkSpeed > 0）：
	 *     <ul>
	 *     <li>在攻擊範圍內：優先使用技能，否則物理攻擊</li>
	 *     <li>不在範圍內：嘗試遠程技能 → 傳送接近 → 移動接近</li>
	 *     </ul>
	 * </li>
	 * </ul>
	 *
	 * <h4>傳送機制：</h4>
	 * <p>
	 * 具備傳送能力的 NPC（is_teleport = true）在距離 6-15 格時，
	 * 有 20% 機率傳送到目標附近 3 格內，消耗 10 MP。
	 * </p>
	 *
	 * @see #attackTarget(L1Character)
	 * @see #nearTeleport(int, int)
	 */
	public void onTarget() {
		setActived(true);
		_targetItemList.clear();
		_targetItem = null;
		L1Character target = _target; // 保存目標引用，避免在處理過程中被修改
		if (getAtkspeed() == 0) { // 逃跑型 NPC
			if (getPassispeed() > 0) { // 移動できるキャラ
				int escapeDistance = 15;
				if (hasSkillEffect(40) == true) {
					escapeDistance = 1;
				}
				if (getLocation().getTileLineDistance(target.getLocation()) > escapeDistance) { // ターゲットから逃げるの終了
					tagertClear();
				} else { // ターゲットから逃げる
					int dir = targetReverseDirection(target.getX(),
							target.getY());
					dir = checkObject(getX(), getY(), getMapId(), dir);
					setDirectionMove(dir);
					setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
				}
			}
		} else { // 逃げないキャラ
			if (isAttackPosition(target.getX(), target.getY(), getAtkRanged())) { // 攻撃可能位置
				if (mobSkill.isSkillTrigger(target)) { // トリガの条件に合うスキルがある
					if (mobSkill.skillUse(target, true)) { // スキル使用(mobskill.sqlのTriRndに従う)
						setSleepTime(calcSleepTime(mobSkill.getSleepTime(),
								MAGIC_SPEED));
					} else { // スキル使用が失敗したら物理攻撃
						setHeading(targetDirection(target.getX(),
								target.getY()));
						attackTarget(target);
					}
				} else {
					setHeading(targetDirection(target.getX(), target.getY()));
					attackTarget(target);
				}
			} else { // 攻撃不可能位置
				if (mobSkill.skillUse(target, false)) { // スキル使用(mobskill.sqlのTriRndに従わず、発動確率は100%。ただしサモン、強制変身は常にTriRndに従う。)
					setSleepTime(calcSleepTime(mobSkill.getSleepTime(),
							MAGIC_SPEED));
					return;
				}

				if (getPassispeed() > 0) {
					// 移動できるキャラ
					int distance = getLocation().getTileDistance(
							target.getLocation());
					if ((firstFound == true) && getNpcTemplate().is_teleport()
							&& (distance > 3) && (distance < 15)) {
						if (nearTeleport(target.getX(), target.getY()) == true) {
							firstFound = false;
							return;
						}
					}

					if (getNpcTemplate().is_teleport()
							&& (20 > Random.nextInt(100))
							&& (getCurrentMp() >= 10) && (distance > 6)
							&& (distance < 15)) { // テレポート移動
						if (nearTeleport(target.getX(), target.getY()) == true) {
							return;
						}
					}
					int dir = moveDirection(target.getX(), target.getY());
					if (dir == -1) {
						// 假如怪物走不過去  就找附近下一個玩家攻擊
						searchTarget();
					} else {
						setDirectionMove(dir);
						setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
					}
				} else {
					// 移動できないキャラ（ターゲットから排除、ＰＴのときドロップチャンスがリセットされるけどまぁ自業自得）
					tagertClear();
				}
			}
		}
	}

	/**
	 * 對指定目標執行物理攻擊
	 * <p>
	 * 創建攻擊實例，計算命中和傷害，然後執行攻擊動作。
	 * </p>
	 *
	 * <h4>執行流程：</h4>
	 * <ol>
	 * <li>驗證目標狀態（是否傳送中、隱藏狀態等）</li>
	 * <li>創建 {@link L1Attack} 攻擊實例</li>
	 * <li>計算命中判定 {@link L1Attack#calcHit()}</li>
	 * <li>計算傷害數值 {@link L1Attack#calcDamage()}</li>
	 * <li>檢查特殊效果（反擊屏障等）</li>
	 * <li>執行攻擊動作 {@link L1Attack#action()}</li>
	 * <li>提交傷害結果 {@link L1Attack#commit()}</li>
	 * <li>設定下次攻擊時間</li>
	 * </ol>
	 *
	 * @param target 攻擊目標
	 * @see L1Attack
	 */
	public void attackTarget(L1Character target) {
		if (target instanceof L1PcInstance) {
			L1PcInstance player = (L1PcInstance) target;
			if (player.isTeleport()) { // 傳送處理中
				return;
			}
		} else if (target instanceof L1PetInstance) {
			L1PetInstance pet = (L1PetInstance) target;
			L1Character cha = pet.getMaster();
			if (cha instanceof L1PcInstance) {
				L1PcInstance player = (L1PcInstance) cha;
				if (player.isTeleport()) { // テレポート処理中
					return;
				}
			}
		} else if (target instanceof L1SummonInstance) {
			L1SummonInstance summon = (L1SummonInstance) target;
			L1Character cha = summon.getMaster();
			if (cha instanceof L1PcInstance) {
				L1PcInstance player = (L1PcInstance) cha;
				if (player.isTeleport()) { // テレポート処理中
					return;
				}
			}
		}
		if (this instanceof L1PetInstance) {
			L1PetInstance pet = (L1PetInstance) this;
			L1Character cha = pet.getMaster();
			if (cha instanceof L1PcInstance) {
				L1PcInstance player = (L1PcInstance) cha;
				if (player.isTeleport()) { // テレポート処理中
					return;
				}
			}
		} else if (this instanceof L1SummonInstance) {
			L1SummonInstance summon = (L1SummonInstance) this;
			L1Character cha = summon.getMaster();
			if (cha instanceof L1PcInstance) {
				L1PcInstance player = (L1PcInstance) cha;
				if (player.isTeleport()) { // テレポート処理中
					return;
				}
			}
		}

		if (target instanceof L1NpcInstance) {
			L1NpcInstance npc = (L1NpcInstance) target;
			if (npc.getHiddenStatus() != HIDDEN_STATUS_NONE) { // 地中に潜っているか、飛んでいる
				allTargetClear();
				return;
			}
		}

		boolean isCounterBarrier = false;
		L1Attack attack = new L1Attack(this, target);
		if (attack.calcHit()) {
			if (target.hasSkillEffect(COUNTER_BARRIER)) {
				L1Magic magic = new L1Magic(target, this);
				boolean isProbability = magic
						.calcProbabilityMagic(COUNTER_BARRIER);
				boolean isShortDistance = attack.isShortDistance();
				if (isProbability && isShortDistance) {
					isCounterBarrier = true;
				}
			}
			if (!isCounterBarrier) {
				attack.calcDamage();
			}
		}
		if (isCounterBarrier) {
			attack.actionCounterBarrier();
			attack.commitCounterBarrier();
		} else {
			attack.action();
			attack.commit();
		}
		setSleepTime(calcSleepTime(getAtkspeed(), ATTACK_SPEED));
	}

	/**
	 * 搜尋目標物品
	 * <p>
	 * 在 NPC 視野範圍內搜尋地面上可撿拾的物品，並將符合條件的物品加入目標清單。
	 * </p>
	 *
	 * <h4>搜尋邏輯：</h4>
	 * <ol>
	 * <li>獲取視野內所有地面物品容器 {@link L1GroundInventory}</li>
	 * <li>隨機選擇一個物品容器</li>
	 * <li>檢查物品是否可以放入 NPC 背包</li>
	 * <li>將符合條件的物品加入 {@link #_targetItemList}</li>
	 * </ol>
	 *
	 * @see #checkTargetItem()
	 * @see #onTargetItem()
	 * @see #pickupTargetItem(L1ItemInstance)
	 */
	public void searchTargetItem() {
		List<L1GroundInventory> gInventorys = Lists.newList();

		for (L1Object obj : L1World.getInstance().getVisibleObjects(this)) {
			if ((obj != null) && (obj instanceof L1GroundInventory)) {
				gInventorys.add((L1GroundInventory) obj);
			}
		}
		if (gInventorys.size() == 0) {
			return;
		}

		// 拾うアイテム(のインベントリ)をランダムで選定
		int pickupIndex = Random.nextInt(gInventorys.size());
		L1GroundInventory inventory = gInventorys.get(pickupIndex);
		for (L1ItemInstance item : inventory.getItems()) {
			if (getInventory().checkAddItem(item, item.getCount()) == L1Inventory.OK) { // 持てるならターゲットアイテムに加える
				_targetItem = item;
				_targetItemList.add(_targetItem);
			}
		}
	}

	/**
	 * 從空中搜尋特定物品（飛行狀態專用）
	 * <p>
	 * 當 NPC 處於飛行狀態時，若發現藥水或食物類物品，會解除飛行狀態並降落撿拾。
	 * </p>
	 *
	 * <h4>功能特點：</h4>
	 * <ul>
	 * <li>僅搜尋藥水（type=6）和食物（type=7）類物品</li>
	 * <li>發現目標物品後自動解除飛行狀態</li>
	 * <li>播放降落動作（ACTION_Movedown）</li>
	 * <li>重新啟動 AI 處理</li>
	 * </ul>
	 *
	 * @see #searchTargetItem()
	 * @see #HIDDEN_STATUS_FLY
	 */
	public void searchItemFromAir() {
		List<L1GroundInventory> gInventorys = Lists.newList();

		for (L1Object obj : L1World.getInstance().getVisibleObjects(this)) {
			if ((obj != null) && (obj instanceof L1GroundInventory)) {
				gInventorys.add((L1GroundInventory) obj);
			}
		}
		if (gInventorys.isEmpty()) {
			return;
		}

		int pickupIndex = Random.nextInt(gInventorys.size());
		L1GroundInventory inventory = gInventorys.get(pickupIndex);
		for (L1ItemInstance item : inventory.getItems()) {
			if ((item.getItem().getType() == 6) // potion
					|| (item.getItem().getType() == 7)) { // food
				if (getInventory().checkAddItem(item, item.getCount()) == L1Inventory.OK) {
					if (getHiddenStatus() == HIDDEN_STATUS_FLY) {
						setHiddenStatus(HIDDEN_STATUS_NONE);
						setStatus(L1NpcDefaultAction.getInstance().getStatus(getTempCharGfx()));
						broadcastPacket(new S_DoActionGFX(getId(), ActionCodes.ACTION_Movedown));
						onNpcAI();
						startChat(CHAT_TIMING_HIDE);
						_targetItem = item;
						_targetItemList.add(_targetItem);
					}
				}
			}
		}
	}

	/**
	 * 隨機打亂陣列順序（Fisher-Yates 洗牌算法）
	 * <p>
	 * 使用 Fisher-Yates 算法對陣列進行隨機排序，確保每個元素出現在任意位置的機率相等。
	 * </p>
	 *
	 * @param arr 要打亂的物件陣列
	 */
	public static void shuffle(L1Object[] arr) {
		for (int i = arr.length - 1; i > 0; i--) {
			int t = Random.nextInt(i);

			// 選ばれた値と交換する
			L1Object tmp = arr[i];
			arr[i] = arr[t];
			arr[t] = tmp;
		}
	}

	/**
	 * 驗證目標物品有效性並設定下一個目標
	 * <p>
	 * 檢查當前目標物品是否仍然有效（存在、在同地圖、距離合理），
	 * 若無效則從目標清單中取下一個物品作為新目標。
	 * </p>
	 *
	 * <h4>無效條件：</h4>
	 * <ul>
	 * <li>目標物品為 null</li>
	 * <li>目標物品不在同一地圖</li>
	 * <li>目標物品距離超過 15 格</li>
	 * </ul>
	 *
	 * <p>
	 * 使用遞迴方式檢查，直到找到有效目標或清單為空。
	 * </p>
	 *
	 * @see #searchTargetItem()
	 * @see #onTargetItem()
	 */
	public void checkTargetItem() {
		if ((_targetItem == null)
				|| (_targetItem.getMapId() != getMapId())
				|| (getLocation().getTileDistance(_targetItem.getLocation()) > 15)) {
			if (!_targetItemList.isEmpty()) {
				_targetItem = _targetItemList.get(0);
				_targetItemList.remove(0);
				checkTargetItem();
			} else {
				_targetItem = null;
			}
		}
	}

	/**
	 * 處理目標物品相關行為
	 * <p>
	 * 當 NPC 有目標物品時，執行移動到物品位置並撿拾的邏輯。
	 * </p>
	 *
	 * <h4>處理邏輯：</h4>
	 * <ul>
	 * <li><b>距離為 0</b>：在可撿拾位置，直接撿拾物品</li>
	 * <li><b>距離大於 0</b>：
	 *   <ul>
	 *   <li>計算移動方向 {@link #moveDirection(int, int)}</li>
	 *   <li>若無法移動（dir=-1），放棄該物品</li>
	 *   <li>若可移動，向目標物品方向移動一步</li>
	 *   </ul>
	 * </li>
	 * </ul>
	 *
	 * @see #pickupTargetItem(L1ItemInstance)
	 * @see #moveDirection(int, int)
	 * @see #setDirectionMove(int)
	 */
	public void onTargetItem() {
		if (getLocation().getTileLineDistance(_targetItem.getLocation()) == 0) { // ピックアップ可能位置
			pickupTargetItem(_targetItem);
		} else { // ピックアップ不可能位置
			int dir = moveDirection(_targetItem.getX(), _targetItem.getY());
			if (dir == -1) { // 拾うの諦め
				_targetItemList.remove(_targetItem);
				_targetItem = null;
			} else { // ターゲットアイテムへ移動
				setDirectionMove(dir);
				setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
			}
		}
	}

	/**
	 * 撿拾目標物品
	 * <p>
	 * 將地面上的物品轉移到 NPC 的背包中，並觸發相關事件。
	 * </p>
	 *
	 * <h4>執行步驟：</h4>
	 * <ol>
	 * <li>獲取物品所在地面容器 {@link L1World#getInventory(int, int, short)}</li>
	 * <li>執行物品轉移 {@link L1Inventory#tradeItem(L1ItemInstance, long, L1Inventory)}</li>
	 * <li>更新燈光狀態 {@link #turnOnOffLight()}</li>
	 * <li>觸發物品獲得事件 {@link #onGetItem(L1ItemInstance)}</li>
	 * <li>從目標清單移除該物品</li>
	 * <li>設定休息時間為 1 秒</li>
	 * </ol>
	 *
	 * @param targetItem 要撿拾的物品
	 * @see #onTargetItem()
	 * @see #onGetItem(L1ItemInstance)
	 */
	public void pickupTargetItem(L1ItemInstance targetItem) {
		L1Inventory groundInventory = L1World.getInstance().getInventory(
				targetItem.getX(), targetItem.getY(), targetItem.getMapId());
		L1ItemInstance item = groundInventory.tradeItem(targetItem,
				targetItem.getCount(), getInventory());
		turnOnOffLight();
		onGetItem(item);
		_targetItemList.remove(_targetItem);
		_targetItem = null;
		setSleepTime(1000);
	}

	/**
	 * 處理無目標狀態下的行為
	 * <p>
	 * 當 NPC 沒有攻擊目標時，根據不同情況執行對應的行為邏輯。
	 * </p>
	 *
	 * <h4>行為決策樹：</h4>
	 * <ul>
	 * <li><b>有主人且距離超過 2 格</b>：追隨主人</li>
	 * <li><b>周圍無玩家</b>：返回 true，終止 AI 處理</li>
	 * <li><b>可移動的自由 NPC</b>：
	 *   <ul>
	 *   <li>若為群組領導者或非群組成員：隨機移動（有機率向家點方向移動）</li>
	 *   <li>若為群組成員：追隨群組領導者</li>
	 *   </ul>
	 * </li>
	 * </ul>
	 *
	 * <h4>隨機移動機制：</h4>
	 * <ol>
	 * <li>隨機決定移動距離（1-5 格）和方向</li>
	 * <li>有 1/3 機率調整為朝向家點方向</li>
	 * <li>每步遞減剩餘移動距離</li>
	 * </ol>
	 *
	 * @return true：終止 AI 處理；false：繼續 AI 處理
	 * @see #moveDirection(int, int)
	 * @see #setDirectionMove(int)
	 */
	public boolean noTarget() {
		if ((_master != null)
				&& (_master.getMapId() == getMapId())
				&& (getLocation().getTileLineDistance(_master.getLocation()) > 2)) { // 主人が同じマップにいて離れてる場合は追尾
			int dir = moveDirection(_master.getX(), _master.getY());
			if (dir != -1) {
				setDirectionMove(dir);
				setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
			} else {
				return true;
			}
		} else {
			if (L1World.getInstance().getRecognizePlayer(this).isEmpty()) {
				return true; // 周りにプレイヤーがいなくなったらＡＩ処理終了
			}
			// 移動できるキャラはランダムに動いておく
			if ((_master == null) && (getPassispeed() > 0) && !isRest()) {
				// グループに属していないorグループに属していてリーダーの場合、ランダムに動いておく
				L1MobGroupInfo mobGroupInfo = getMobGroupInfo();
				if ((mobGroupInfo == null)
						|| ((mobGroupInfo != null) && mobGroupInfo
								.isLeader(this))) {
					// 移動する予定の距離を移動し終えたら、新たに距離と方向を決める
					// そうでないなら、移動する予定の距離をデクリメント
					if (_randomMoveDistance == 0) {
						_randomMoveDistance = Random.nextInt(5) + 1;
						_randomMoveDirection = Random.nextInt(20);
						// ホームポイントから離れすぎないように、一定の確率でホームポイントの方向に補正
						if ((getHomeX() != 0) && (getHomeY() != 0)
								&& (_randomMoveDirection < 8)
								&& (Random.nextInt(3) == 0)) {
							_randomMoveDirection = moveDirection(getHomeX(),
									getHomeY());
						}
					} else {
						_randomMoveDistance--;
					}
					int dir = checkObject(getX(), getY(), getMapId(),
							_randomMoveDirection);
					if (dir != -1) {
						setDirectionMove(dir);
						setSleepTime(calcSleepTime(getPassispeed(), MOVE_SPEED));
					}
				} else { // リーダーを追尾
					L1NpcInstance leader = mobGroupInfo.getLeader();
					if (getLocation().getTileLineDistance(leader.getLocation()) > 2) {
						int dir = moveDirection(leader.getX(), leader.getY());
						if (dir == -1) {
							return true;
						} else {
							setDirectionMove(dir);
							setSleepTime(calcSleepTime(getPassispeed(),
									MOVE_SPEED));
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * NPC 最終動作處理（由子類別覆寫）
	 * <p>
	 * 當 NPC 與玩家互動結束後的最終處理。基礎實作為空，由子類別覆寫實現特定邏輯。
	 * </p>
	 *
	 * @param pc 互動的玩家
	 * @param s 動作指令字串
	 */
	public void onFinalAction(L1PcInstance pc, String s) {
	}

	/**
	 * 清除當前目標
	 * <p>
	 * 將當前鎖定的目標從仇恨列表中移除，並清空目標參考。
	 * </p>
	 *
	 * @see #targetRemove(L1Character)
	 * @see #allTargetClear()
	 */
	public void tagertClear() {
		if (_target == null) {
			return;
		}
		_hateList.remove(_target);
		_target = null;
	}

	/**
	 * 移除指定目標
	 * <p>
	 * 從仇恨列表中移除指定的角色，若該角色為當前目標則同時清空目標參考。
	 * </p>
	 *
	 * @param target 要移除的目標角色
	 * @see #tagertClear()
	 * @see #allTargetClear()
	 */
	public void targetRemove(L1Character target) {
		_hateList.remove(target);
		if ((_target != null) && _target.equals(target)) {
			_target = null;
		}
	}

	/**
	 * 清除所有目標
	 * <p>
	 * 清空所有仇恨列表、目標物品列表，並重置當前目標。
	 * 用於重置 NPC 的攻擊狀態。
	 * </p>
	 *
	 * <h4>清除內容：</h4>
	 * <ul>
	 * <li>仇恨列表 {@link #_hateList}</li>
	 * <li>掉落仇恨列表 {@link #_dropHateList}</li>
	 * <li>當前攻擊目標 {@link #_target}</li>
	 * <li>目標物品列表 {@link #_targetItemList}</li>
	 * <li>當前目標物品 {@link #_targetItem}</li>
	 * </ul>
	 *
	 * @see #tagertClear()
	 * @see #targetRemove(L1Character)
	 */
	public void allTargetClear() {
		_hateList.clear();
		_dropHateList.clear();
		_target = null;
		_targetItemList.clear();
		_targetItem = null;
	}

	/**
	 * 設定主人
	 * <p>
	 * 設定此 NPC 的主人（召喚者或擁有者）。主要用於寵物、召喚獸等從屬型 NPC。
	 * </p>
	 *
	 * @param cha 主人角色
	 * @see #getMaster()
	 */
	public void setMaster(L1Character cha) {
		_master = cha;
	}

	/**
	 * 取得主人
	 * <p>
	 * 返回此 NPC 的主人（召喚者或擁有者）。
	 * </p>
	 *
	 * @return 主人角色，若無主人則返回 null
	 * @see #setMaster(L1Character)
	 */
	public L1Character getMaster() {
		return _master;
	}

	/**
	 * NPC AI 觸發器（由子類別覆寫）
	 * <p>
	 * 當 NPC 的 AI 被觸發時調用。基礎實作為空，由子類別覆寫實現特定的 AI 行為。
	 * </p>
	 *
	 * <h4>觸發時機：</h4>
	 * <ul>
	 * <li>NPC 感知到玩家時</li>
	 * <li>NPC 受到攻擊時</li>
	 * <li>NPC 狀態改變時（如從飛行降落）</li>
	 * </ul>
	 *
	 * @see #startAI()
	 * @see #AIProcess()
	 */
	public void onNpcAI() {
	}

	/**
	 * 物品精製
	 * <p>
	 * 特定 NPC（如布羅布）使用背包內的材料自動精製成目標物品。
	 * 用於實現 NPC 的物品製作功能。
	 * </p>
	 *
	 * <h4>精製邏輯：</h4>
	 * <ol>
	 * <li>檢查 NPC 是否擁有經驗值（表示可精製）</li>
	 * <li>檢查是否已持有目標成品（避免重複製作）</li>
	 * <li>驗證背包內是否有足夠的材料</li>
	 * <li>消耗材料並創建成品</li>
	 * </ol>
	 *
	 * <h4>支援的精製配方：</h4>
	 * <ul>
	 * <li>奧里哈鋼劍刀身（需要奧里哈鋼、秘銀、鑽石）</li>
	 * <li>長劍刀身（需要秘銀、鑽石）</li>
	 * <li>短劍刀身（需要秘銀、鑽石）</li>
	 * <li>奧里哈鋼號角（需要號角、奧里哈鋼、鑽石）</li>
	 * <li>秘銀號角（需要號角、秘銀）</li>
	 * </ul>
	 *
	 * @see L1Inventory#checkItem(int[], int[])
	 * @see L1Inventory#consumeItem(int, int)
	 * @see L1Inventory#storeItem(int, int)
	 */
	public void refineItem() {

		int[] materials = null;
		int[] counts = null;
		int[] createitem = null;
		int[] createcount = null;

		if (_npcTemplate.get_npcId() == 45032) { // ブロッブ
			// オリハルコンソードの刀身
			if ((getExp() != 0) && !_inventory.checkItem(20)) {
				materials = new int[] { 40508, 40521, 40045 };
				counts = new int[] { 150, 3, 3 };
				createitem = new int[] { 20 };
				createcount = new int[] { 1 };
				if (_inventory.checkItem(materials, counts)) {
					for (int i = 0; i < materials.length; i++) {
						_inventory.consumeItem(materials[i], counts[i]);
					}
					for (int j = 0; j < createitem.length; j++) {
						_inventory.storeItem(createitem[j], createcount[j]);
					}
				}
			}
			// ロングソードの刀身
			if ((getExp() != 0) && !_inventory.checkItem(19)) {
				materials = new int[] { 40494, 40521 };
				counts = new int[] { 150, 3 };
				createitem = new int[] { 19 };
				createcount = new int[] { 1 };
				if (_inventory.checkItem(materials, counts)) {
					for (int i = 0; i < materials.length; i++) {
						_inventory.consumeItem(materials[i], counts[i]);
					}
					for (int j = 0; j < createitem.length; j++) {
						_inventory.storeItem(createitem[j], createcount[j]);
					}
				}
			}
			// ショートソードの刀身
			if ((getExp() != 0) && !_inventory.checkItem(3)) {
				materials = new int[] { 40494, 40521 };
				counts = new int[] { 50, 1 };
				createitem = new int[] { 3 };
				createcount = new int[] { 1 };
				if (_inventory.checkItem(materials, counts)) {
					for (int i = 0; i < materials.length; i++) {
						_inventory.consumeItem(materials[i], counts[i]);
					}
					for (int j = 0; j < createitem.length; j++) {
						_inventory.storeItem(createitem[j], createcount[j]);
					}
				}
			}
			// オリハルコンホーン
			if ((getExp() != 0) && !_inventory.checkItem(100)) {
				materials = new int[] { 88, 40508, 40045 };
				counts = new int[] { 4, 80, 3 };
				createitem = new int[] { 100 };
				createcount = new int[] { 1 };
				if (_inventory.checkItem(materials, counts)) {
					for (int i = 0; i < materials.length; i++) {
						_inventory.consumeItem(materials[i], counts[i]);
					}
					for (int j = 0; j < createitem.length; j++) {
						_inventory.storeItem(createitem[j], createcount[j]);
					}
				}
			}
			// ミスリルホーン
			if ((getExp() != 0) && !_inventory.checkItem(89)) {
				materials = new int[] { 88, 40494 };
				counts = new int[] { 2, 80 };
				createitem = new int[] { 89 };
				createcount = new int[] { 1 };
				if (_inventory.checkItem(materials, counts)) {
					for (int i = 0; i < materials.length; i++) {
						_inventory.consumeItem(materials[i], counts[i]);
					}
					for (int j = 0; j < createitem.length; j++) {
						L1ItemInstance item = _inventory.storeItem(
								createitem[j], createcount[j]);
						if (getNpcTemplate().get_digestitem() > 0) {
							setDigestItem(item);
						}
					}
				}
			}
		} else if (_npcTemplate.get_npcId() == 81069) { // ドッペルゲンガー（クエスト）
			// ドッペルゲンガーの体液
			if ((getExp() != 0) && !_inventory.checkItem(40542)) {
				materials = new int[] { 40032 };
				counts = new int[] { 1 };
				createitem = new int[] { 40542 };
				createcount = new int[] { 1 };
				if (_inventory.checkItem(materials, counts)) {
					for (int i = 0; i < materials.length; i++) {
						_inventory.consumeItem(materials[i], counts[i]);
					}
					for (int j = 0; j < createitem.length; j++) {
						_inventory.storeItem(createitem[j], createcount[j]);
					}
				}
			}
		} else if ((_npcTemplate.get_npcId() == 45166 // ジャックオーランタン
				)
				|| (_npcTemplate.get_npcId() == 45167)) {
			// パンプキンの種
			if ((getExp() != 0) && !_inventory.checkItem(40726)) {
				materials = new int[] { 40725 };
				counts = new int[] { 1 };
				createitem = new int[] { 40726 };
				createcount = new int[] { 1 };
				if (_inventory.checkItem(materials, counts)) {
					for (int i = 0; i < materials.length; i++) {
						_inventory.consumeItem(materials[i], counts[i]);
					}
					for (int j = 0; j < createitem.length; j++) {
						_inventory.storeItem(createitem[j], createcount[j]);
					}
				}
			}
		}
	}

	private boolean _aiRunning = false; // ＡＩが実行中か

	// ※ＡＩをスタートさせる時にすでに実行されてないか確認する時に使用
	private boolean _actived = false; // ＮＰＣがアクティブか

	// ※この値がfalseで_targetがいる場合、アクティブになって初行動とみなしヘイストポーション等を使わせる判定で使用
	private boolean _firstAttack = false; // ファーストアッタクされたか

	private int _sleep_time; // ＡＩを停止する時間(ms) ※行動を起こした場合に所要する時間をセット

	protected L1HateList _hateList = new L1HateList();

	protected L1HateList _dropHateList = new L1HateList();

	// ※攻撃するターゲットの判定とＰＴ時のドロップ判定で使用
	protected List<L1ItemInstance> _targetItemList = Lists.newList(); // ダーゲットアイテム一覧

	protected L1Character _target = null; // 現在のターゲット

	protected L1ItemInstance _targetItem = null; // 現在のターゲットアイテム

	protected L1Character _master = null; // 主人orグループリーダー

	private boolean _deathProcessing = false; // 死亡処理中か

	// EXP、Drop分配中はターゲットリスト、ヘイトリストをクリアしない

	private int _paralysisTime = 0; // Paralysis RestTime

	/**
	 * 設定麻痺時間
	 * <p>
	 * 設定 NPC 的麻痺休息時間（毫秒）。麻痺期間 NPC 無法行動。
	 * </p>
	 *
	 * @param ptime 麻痺時間（毫秒）
	 * @see #getParalysisTime()
	 */
	public void setParalysisTime(int ptime) {
		_paralysisTime = ptime;
	}

	/**
	 * 取得仇恨列表
	 * <p>
	 * 返回此 NPC 的仇恨列表，用於追蹤所有攻擊目標及其仇恨值。
	 * </p>
	 *
	 * @return 仇恨列表
	 * @see L1HateList
	 * @see #setHate(L1Character, int)
	 */
	public L1HateList getHateList() {
		return _hateList;
	}

	/**
	 * 取得麻痺時間
	 * <p>
	 * 返回 NPC 剩餘的麻痺休息時間（毫秒）。
	 * </p>
	 *
	 * @return 麻痺時間（毫秒）
	 * @see #setParalysisTime(int)
	 */
	public int getParalysisTime() {
		return _paralysisTime;
	}

	/**
	 * 啟動 HP 自然回復
	 * <p>
	 * 根據 NPC 模板設定的回復間隔和回復量，啟動定時 HP 回復任務。
	 * </p>
	 *
	 * <h4>啟動條件：</h4>
	 * <ul>
	 * <li>回復任務未運行中</li>
	 * <li>回復間隔大於 0</li>
	 * <li>回復量大於 0</li>
	 * </ul>
	 *
	 * @see #stopHpRegeneration()
	 * @see L1NpcRegenerationTimer
	 * @see L1Npc#get_hprinterval()
	 * @see L1Npc#get_hpr()
	 */
	public final void startHpRegeneration() {
		int hprInterval = getNpcTemplate().get_hprinterval();
		int hpr = getNpcTemplate().get_hpr();
		if (!_hprRunning && (hprInterval > 0) && (hpr > 0)) {
			_hprTimer = new HprTimer(hpr);
			L1NpcRegenerationTimer.getInstance().scheduleAtFixedRate(_hprTimer,
					hprInterval, hprInterval);
			_hprRunning = true;
		}
	}

	/**
	 * 停止 HP 自然回復
	 * <p>
	 * 取消 HP 回復定時任務，停止自然回復。
	 * </p>
	 *
	 * @see #startHpRegeneration()
	 */
	public final void stopHpRegeneration() {
		if (_hprRunning) {
			_hprTimer.cancel();
			_hprRunning = false;
		}
	}

	/**
	 * 啟動 MP 自然回復
	 * <p>
	 * 根據 NPC 模板設定的回復間隔和回復量，啟動定時 MP 回復任務。
	 * </p>
	 *
	 * <h4>啟動條件：</h4>
	 * <ul>
	 * <li>回復任務未運行中</li>
	 * <li>回復間隔大於 0</li>
	 * <li>回復量大於 0</li>
	 * </ul>
	 *
	 * @see #stopMpRegeneration()
	 * @see L1NpcRegenerationTimer
	 * @see L1Npc#get_mprinterval()
	 * @see L1Npc#get_mpr()
	 */
	public final void startMpRegeneration() {
		int mprInterval = getNpcTemplate().get_mprinterval();
		int mpr = getNpcTemplate().get_mpr();
		if (!_mprRunning && (mprInterval > 0) && (mpr > 0)) {
			_mprTimer = new MprTimer(mpr);
			L1NpcRegenerationTimer.getInstance().scheduleAtFixedRate(_mprTimer,
					mprInterval, mprInterval);
			_mprRunning = true;
		}
	}

	/**
	 * 停止 MP 自然回復
	 * <p>
	 * 取消 MP 回復定時任務，停止自然回復。
	 * </p>
	 *
	 * @see #startMpRegeneration()
	 */
	public final void stopMpRegeneration() {
		if (_mprRunning) {
			_mprTimer.cancel();
			_mprRunning = false;
		}
	}

	// ■■■■■■■■■■■■ タイマー関連 ■■■■■■■■■■

	// ＨＰ自然回復
	private boolean _hprRunning = false;

	private HprTimer _hprTimer;

	class HprTimer extends TimerTask {
		@Override
		public void run() {
			try {
				if ((!_destroyed && !isDead())
						&& ((getCurrentHp() > 0) && (getCurrentHp() < getMaxHp()))) {
					setCurrentHp(getCurrentHp() + _point);
				} else {
					cancel();
					_hprRunning = false;
				}
			} catch (Exception e) {
				_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}

		public HprTimer(int point) {
			if (point < 1) {
				point = 1;
			}
			_point = point;
		}

		private final int _point;
	}

	// ＭＰ自然回復
	private boolean _mprRunning = false;

	private MprTimer _mprTimer;

	class MprTimer extends TimerTask {
		@Override
		public void run() {
			try {
				if ((!_destroyed && !isDead())
						&& ((getCurrentHp() > 0) && (getCurrentMp() < getMaxMp()))) {
					setCurrentMp(getCurrentMp() + _point);
				} else {
					cancel();
					_mprRunning = false;
				}
			} catch (Exception e) {
				_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}

		public MprTimer(int point) {
			if (point < 1) {
				point = 1;
			}
			_point = point;
		}

		private final int _point;
	}

	// アイテム消化
	private Map<Integer, Integer> _digestItems;

	public boolean _digestItemRunning = false;

	class DigestItemTimer implements Runnable {
		@Override
		public void run() {
			_digestItemRunning = true;
			while (!_destroyed && (_digestItems.size() > 0)) {
				try {
					Thread.sleep(1000);
				} catch (Exception exception) {
					break;
				}

				Object[] keys = _digestItems.keySet().toArray();
				for (Object key2 : keys) {
					Integer key = (Integer) key2;
					Integer digestCounter = _digestItems.get(key);
					digestCounter -= 1;
					if (digestCounter <= 0) {
						_digestItems.remove(key);
						L1ItemInstance digestItem = getInventory().getItem(key);
						if (digestItem != null) {
							getInventory().removeItem(digestItem,
									digestItem.getCount());
						}
					} else {
						_digestItems.put(key, digestCounter);
					}
				}
			}
			_digestItemRunning = false;
		}
	}

	// ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■

	public L1NpcInstance(L1Npc template) {
		setStatus(0);
		setMoveSpeed(0);
		setDead(false);
		setreSpawn(false);

		if (template != null) {
			setting_template(template);
		}
	}

	// 指定のテンプレートで各種値を初期化
	public void setting_template(L1Npc template) {
		_npcTemplate = template;
		int randomlevel = 0;
		double rate = 0;
		double diff = 0;
		setName(template.get_name());
		setNameId(template.get_nameid());
		if (template.get_randomlevel() == 0) { // ランダムLv指定なし
			setLevel(template.get_level());
		} else { // ランダムLv指定あり（最小値:get_level(),最大値:get_randomlevel()）
			randomlevel = Random.nextInt(template.get_randomlevel()
					- template.get_level() + 1);
			diff = template.get_randomlevel() - template.get_level();
			rate = randomlevel / diff;
			randomlevel += template.get_level();
			setLevel(randomlevel);
		}
		if (template.get_randomhp() == 0) {
			setMaxHp(template.get_hp());
			setCurrentHpDirect(template.get_hp());
		} else {
			double randomhp = rate
					* (template.get_randomhp() - template.get_hp());
			int hp = (int) (template.get_hp() + randomhp);
			setMaxHp(hp);
			setCurrentHpDirect(hp);
		}
		if (template.get_randommp() == 0) {
			setMaxMp(template.get_mp());
			setCurrentMpDirect(template.get_mp());
		} else {
			double randommp = rate
					* (template.get_randommp() - template.get_mp());
			int mp = (int) (template.get_mp() + randommp);
			setMaxMp(mp);
			setCurrentMpDirect(mp);
		}
		if (template.get_randomac() == 0) {
			setAc(template.get_ac());
		} else {
			double randomac = rate
					* (template.get_randomac() - template.get_ac());
			int ac = (int) (template.get_ac() + randomac);
			setAc(ac);
		}
		if (template.get_randomlevel() == 0) {
			setStr(template.get_str());
			setCon(template.get_con());
			setDex(template.get_dex());
			setInt(template.get_int());
			setWis(template.get_wis());
			setMr(template.get_mr());
		} else {
			setStr((byte) Math.min(template.get_str() + diff, 127));
			setCon((byte) Math.min(template.get_con() + diff, 127));
			setDex((byte) Math.min(template.get_dex() + diff, 127));
			setInt((byte) Math.min(template.get_int() + diff, 127));
			setWis((byte) Math.min(template.get_wis() + diff, 127));
			setMr((byte) Math.min(template.get_mr() + diff, 127));

			addHitup((int) diff * 2);
			addDmgup((int) diff * 2);
		}
		setAgro(template.is_agro());
		setAgrocoi(template.is_agrocoi());
		setAgrososc(template.is_agrososc());
		setTempCharGfx(template.get_gfxid());
		setGfxId(template.get_gfxid());
		setStatus(L1NpcDefaultAction.getInstance().getStatus(getTempCharGfx()));
		setPolyAtkRanged(template.get_ranged());
		setPolyArrowGfx(template.getBowActId());

		// 移動
		if (template.get_passispeed() != 0) {
			setPassispeed(SprTable.getInstance().getSprSpeed(getTempCharGfx(), getStatus()));
		} else {
			setPassispeed(0);
		}
		// 攻擊
		if (template.get_atkspeed() != 0) {
			int actid = (getStatus() + 1);
			if (L1NpcDefaultAction.getInstance().getDefaultAttack(getTempCharGfx()) != actid) {
				actid = L1NpcDefaultAction.getInstance().getDefaultAttack(getTempCharGfx());
			}
			setAtkspeed(SprTable.getInstance().getSprSpeed(getTempCharGfx(), actid));
		} else {
			setAtkspeed(0);
		}

		if (template.get_randomexp() == 0) {
			setExp(template.get_exp());
		} else {
			int level = getLevel();
			int exp = level * level;
			exp += 1;
			setExp(exp);
		}
		if (template.get_randomlawful() == 0) {
			setLawful(template.get_lawful());
			setTempLawful(template.get_lawful());
		} else {
			double randomlawful = rate
					* (template.get_randomlawful() - template.get_lawful());
			int lawful = (int) (template.get_lawful() + randomlawful);
			setLawful(lawful);
			setTempLawful(lawful);
		}
		setPickupItem(template.is_picupitem());
		if (template.is_bravespeed()) {
			setBraveSpeed(1);
		} else {
			setBraveSpeed(0);
		}
		if (template.get_digestitem() > 0) {
			_digestItems = Maps.newMap();
		}
		setKarma(template.getKarma());
		setLightSize(template.getLightSize());

		mobSkill = new L1MobSkillUse(this);
	}

	/**
	 * 設定 NPC 行動延遲時間
	 * <p>
	 * 根據動作編號和類型（移動/攻擊/施法），從精靈表中查詢動作速度並計算延遲時間。
	 * </p>
	 *
	 * @param i 動作編號（來自精靈表）
	 * @param type 速度類型（{@link #MOVE_SPEED}、{@link #ATTACK_SPEED}、{@link #MAGIC_SPEED}）
	 * @see #calcSleepTime(int, int)
	 * @see SprTable#getSprSpeed(int, int)
	 */
	public void npcSleepTime(int i, int type) {
		setSleepTime(calcSleepTime(SprTable.getInstance()
				.getSprSpeed(getTempCharGfx(), i), type));
	}

	private int _passispeed;

	/**
	 * 取得移動速度
	 * @return 移動速度值（來自精靈表）
	 */
	public int getPassispeed() {
		return _passispeed;
	}

	/**
	 * 設定移動速度
	 * @param i 移動速度值
	 */
	public void setPassispeed(int i) {
		_passispeed = i;
	}

	private int _atkspeed;

	/**
	 * 取得攻擊速度
	 * @return 攻擊速度值（來自精靈表）
	 */
	public int getAtkspeed() {
		return _atkspeed;
	}

	/**
	 * 設定攻擊速度
	 * @param i 攻擊速度值
	 */
	public void setAtkspeed(int i) {
		_atkspeed = i;
	}

	private boolean _pickupItem;

	/**
	 * 檢查 NPC 是否會撿拾物品
	 * @return true：會撿拾；false：不會撿拾
	 */
	public boolean isPickupItem() {
		return _pickupItem;
	}

	/**
	 * 設定 NPC 是否撿拾物品
	 * @param flag true：會撿拾；false：不會撿拾
	 */
	public void setPickupItem(boolean flag) {
		_pickupItem = flag;
	}

	/**
	 * 取得 NPC 背包
	 * @return NPC 的物品容器
	 */
	@Override
	public L1Inventory getInventory() {
		return _inventory;
	}

	/**
	 * 設定 NPC 背包
	 * @param inventory 物品容器
	 */
	public void setInventory(L1Inventory inventory) {
		_inventory = inventory;
	}

	/**
	 * 取得 NPC 模板數據
	 * @return NPC 模板對象（包含基礎屬性）
	 * @see L1Npc
	 */
	public L1Npc getNpcTemplate() {
		return _npcTemplate;
	}

	/**
	 * 取得 NPC ID
	 * @return NPC 的唯一識別碼
	 */
	public int getNpcId() {
		return _npcTemplate.get_npcId();
	}

	/**
	 * 設定寵物維護費用
	 * @param i 維護費用
	 */
	public void setPetcost(int i) {
		_petcost = i;
	}

	/**
	 * 取得寵物維護費用
	 * @return 維護費用
	 */
	public int getPetcost() {
		return _petcost;
	}

	/**
	 * 設定生成點
	 * @param spawn 生成點管理器
	 * @see L1Spawn
	 */
	public void setSpawn(L1Spawn spawn) {
		_spawn = spawn;
	}

	/**
	 * 取得生成點
	 * @return 生成點管理器
	 * @see L1Spawn
	 */
	public L1Spawn getSpawn() {
		return _spawn;
	}

	/**
	 * 設定生成編號
	 * @param number 編號
	 */
	public void setSpawnNumber(int number) {
		_spawnNumber = number;
	}

	/**
	 * 取得生成編號
	 * @return 編號
	 */
	public int getSpawnNumber() {
		return _spawnNumber;
	}

	/**
	 * NPC 消失處理
	 * <p>
	 * NPC 死亡後消失，並觸發重生機制。可選擇是否重用物件 ID。
	 * </p>
	 *
	 * <h4>注意事項：</h4>
	 * <ul>
	 * <li>群組怪物不重用 ID（避免複雜度）</li>
	 * <li>重生由 {@link L1Spawn} 的 SpawnTask 處理</li>
	 * </ul>
	 *
	 * @param isReuseId 是否重用物件 ID（true：重用；false：分配新 ID）
	 * @see L1Spawn#executeSpawnTask(int, int)
	 */
	public void onDecay(boolean isReuseId) {
		int id = 0;
		if (isReuseId) {
			id = getId();
		} else {
			id = 0;
		}
		_spawn.executeSpawnTask(_spawnNumber, id);
	}

	/**
	 * 玩家感知 NPC 時的處理
	 * <p>
	 * 當玩家進入視野範圍，將 NPC 加入玩家的已知物件列表，
	 * 發送 NPC 封包給玩家，並觸發 NPC AI。
	 * </p>
	 *
	 * @param perceivedFrom 感知到此 NPC 的玩家
	 * @see S_NPCPack
	 * @see #onNpcAI()
	 */
	@Override
	public void onPerceive(L1PcInstance perceivedFrom) {
		perceivedFrom.addKnownObject(this);
		perceivedFrom.sendPackets(new S_NPCPack(this));
		onNpcAI();
	}

	/**
	 * 刪除 NPC
	 * <p>
	 * 完全移除 NPC，清除所有資源、目標、背包，並從世界中移除。
	 * 根據重生設定決定是否觸發重生機制。
	 * </p>
	 *
	 * <h4>清除內容：</h4>
	 * <ul>
	 * <li>設定為已銷毀狀態</li>
	 * <li>清空背包物品</li>
	 * <li>清除所有目標</li>
	 * <li>清除主人參考</li>
	 * <li>從世界視野物件列表移除</li>
	 * <li>發送移除封包給所有認知的玩家</li>
	 * </ul>
	 *
	 * <h4>重生邏輯：</h4>
	 * <ul>
	 * <li>非群組怪物：重用物件 ID 重生</li>
	 * <li>群組怪物：全滅後不重用 ID 重生</li>
	 * </ul>
	 *
	 * @see #onDecay(boolean)
	 * @see #allTargetClear()
	 */
	public void deleteMe() {
		_destroyed = true;
		if (getInventory() != null) {
			getInventory().clearItems();
		}
		allTargetClear();
		_master = null;
		L1World.getInstance().removeVisibleObject(this);
		L1World.getInstance().removeObject(this);
		List<L1PcInstance> players = L1World.getInstance().getRecognizePlayer(
				this);
		if (players.size() > 0) {
			S_RemoveObject s_deleteNewObject = new S_RemoveObject(this);
			for (L1PcInstance pc : players) {
				if (pc != null) {
					pc.removeKnownObject(this);
					// if(!L1Character.distancepc(user, this))
					pc.sendPackets(s_deleteNewObject);
				}
			}
		}
		removeAllKnownObjects();

		// リスパウン設定
		L1MobGroupInfo mobGroupInfo = getMobGroupInfo();
		if (mobGroupInfo == null) {
			if (isReSpawn()) {
				onDecay(true);
			}
		} else {
			if (mobGroupInfo.removeMember(this) == 0) { // グループメンバー全滅
				setMobGroupInfo(null);
				if (isReSpawn()) {
					onDecay(false);
				}
			}
		}
	}

	/**
	 * 接收魔力傷害（由子類別覆寫）
	 * <p>
	 * 當 NPC 受到魔力傷害時調用。基礎實作為空，由子類別覆寫實現特定處理。
	 * </p>
	 *
	 * @param attacker 攻擊者
	 * @param damageMp 魔力傷害值
	 */
	public void ReceiveManaDamage(L1Character attacker, int damageMp) {
	}

	/**
	 * 接收傷害（由子類別覆寫）
	 * <p>
	 * 當 NPC 受到傷害時調用。基礎實作為空，由子類別覆寫實現特定處理。
	 * </p>
	 *
	 * @param attacker 攻擊者
	 * @param damage 傷害值
	 */
	public void receiveDamage(L1Character attacker, int damage) {
	}

	/**
	 * 設定待消化物品
	 * <p>
	 * 將物品加入消化佇列，並啟動消化定時器。用於實現 NPC 吞噬物品的功能。
	 * </p>
	 *
	 * @param item 要消化的物品
	 * @see #onGetItem(L1ItemInstance)
	 * @see L1Npc#get_digestitem()
	 */
	public void setDigestItem(L1ItemInstance item) {
		_digestItems.put(new Integer(item.getId()), new Integer(
				getNpcTemplate().get_digestitem()));
		if (!_digestItemRunning) {
			DigestItemTimer digestItemTimer = new DigestItemTimer();
			GeneralThreadPool.getInstance().execute(digestItemTimer);
		}
	}

	/**
	 * 獲得物品時的處理
	 * <p>
	 * 當 NPC 撿拾物品時觸發，執行物品精製、洗牌背包、設定消化等操作。
	 * </p>
	 *
	 * <h4>執行步驟：</h4>
	 * <ol>
	 * <li>嘗試精製物品 {@link #refineItem()}</li>
	 * <li>隨機打亂背包物品順序</li>
	 * <li>若 NPC 可消化物品，加入消化佇列</li>
	 * </ol>
	 *
	 * @param item 獲得的物品
	 * @see #pickupTargetItem(L1ItemInstance)
	 * @see #refineItem()
	 * @see #setDigestItem(L1ItemInstance)
	 */
	public void onGetItem(L1ItemInstance item) {
		refineItem();
		getInventory().shuffle();
		if (getNpcTemplate().get_digestitem() > 0) {
			setDigestItem(item);
		}
	}

	/**
	 * 玩家接近 NPC 時的處理
	 * <p>
	 * 當玩家接近隱藏狀態的 NPC 時，根據 NPC 的隱藏狀態觸發不同的反應。
	 * </p>
	 *
	 * <h4>隱藏狀態反應：</h4>
	 * <ul>
	 * <li><b>遁地狀態（SINK）</b>：滿血時，玩家距離 ≤ 2 格，從地面出現</li>
	 * <li><b>飛行狀態（FLY）</b>：
	 *   <ul>
	 *   <li>滿血時，玩家距離 ≤ 1 格，降落地面</li>
	 *   <li>非滿血時，搜尋地面上的藥水和食物</li>
	 *   </ul>
	 * </li>
	 * <li><b>冰凍狀態（ICE）</b>：非滿血時，從冰凍狀態解凍</li>
	 * </ul>
	 *
	 * <p>
	 * 玩家處於隱形或盲目潛行狀態時不觸發。
	 * </p>
	 *
	 * @param pc 接近的玩家
	 * @see #appearOnGround(L1PcInstance)
	 * @see #searchItemFromAir()
	 */
	public void approachPlayer(L1PcInstance pc) {
		if (pc.hasSkillEffect(60) || pc.hasSkillEffect(97)) { // インビジビリティ、ブラインドハイディング中
			return;
		}
		if (getHiddenStatus() == HIDDEN_STATUS_SINK) {
			if (getCurrentHp() == getMaxHp()) {
				if (pc.getLocation().getTileLineDistance(getLocation()) <= 2) {
					appearOnGround(pc);
				}
			}
		} else if (getHiddenStatus() == HIDDEN_STATUS_FLY) {
			if (getCurrentHp() == getMaxHp()) {
				if (pc.getLocation().getTileLineDistance(getLocation()) <= 1) {
					appearOnGround(pc);
				}
			} else {
				// if (getNpcTemplate().get_npcId() != 45681) { // リンドビオル以外
				searchItemFromAir();
				// }
			}
		} else if (getHiddenStatus() == HIDDEN_STATUS_ICE) {
			if (getCurrentHp() < getMaxHp()) {
				appearOnGround(pc);
			}
		}
	}

	/**
	 * 從隱藏狀態出現到地面
	 * <p>
	 * 解除 NPC 的遁地、飛行或冰凍狀態，並在地面顯現。播放相應的出現動畫。
	 * </p>
	 *
	 * <h4>處理邏輯：</h4>
	 * <ul>
	 * <li><b>遁地狀態</b>：播放 ACTION_Appear 動畫，更新視覺狀態，將玩家加入仇恨列表</li>
	 * <li><b>飛行狀態</b>：播放 ACTION_Movedown 動畫，更新視覺狀態，將玩家加入仇恨列表</li>
	 * <li><b>冰凍狀態</b>：播放 ACTION_IceArrow 動畫，更新視覺狀態</li>
	 * </ul>
	 *
	 * <p>
	 * 玩家處於隱形、盲目潛行狀態或 GM 模式時不會被加入仇恨列表。
	 * </p>
	 *
	 * @param pc 觸發出現的玩家
	 * @see #approachPlayer(L1PcInstance)
	 * @see #setHate(L1Character, int)
	 */
	public void appearOnGround(L1PcInstance pc) {
		if (getHiddenStatus() == HIDDEN_STATUS_SINK) {
			setHiddenStatus(HIDDEN_STATUS_NONE);
			setStatus(L1NpcDefaultAction.getInstance().getStatus(getTempCharGfx()));
			broadcastPacket(new S_DoActionGFX(getId(), ActionCodes.ACTION_Appear));
			broadcastPacket(new S_CharVisualUpdate(this, getStatus()));
			if (!pc.hasSkillEffect(60) && !pc.hasSkillEffect(97) // インビジビリティ、ブラインドハイディング中以外、GM以外
					&& !pc.isGm()) {
				_hateList.add(pc, 0);
				_target = pc;
			}
			onNpcAI(); // モンスターのＡＩを開始
			startChat(CHAT_TIMING_HIDE);
		} else if (getHiddenStatus() == HIDDEN_STATUS_FLY) {
			setHiddenStatus(HIDDEN_STATUS_NONE);
			setStatus(L1NpcDefaultAction.getInstance().getStatus(getTempCharGfx()));
			broadcastPacket(new S_DoActionGFX(getId(), ActionCodes.ACTION_Movedown));
			if (!pc.hasSkillEffect(60) && !pc.hasSkillEffect(97) // インビジビリティ、ブラインドハイディング中以外、GM以外
					&& !pc.isGm()) {
				_hateList.add(pc, 0);
				_target = pc;
			}
			onNpcAI(); // モンスターのＡＩを開始
			startChat(CHAT_TIMING_HIDE);
		} else if (getHiddenStatus() == HIDDEN_STATUS_ICE) {
			setHiddenStatus(HIDDEN_STATUS_NONE);
			setStatus(L1NpcDefaultAction.getInstance().getStatus(getTempCharGfx()));
			broadcastPacket(new S_DoActionGFX(getId(), ActionCodes.ACTION_AxeWalk));
			broadcastPacket(new S_CharVisualUpdate(this, getStatus()));
			if (!pc.hasSkillEffect(60) && !pc.hasSkillEffect(97) // インビジビリティ、ブラインドハイディング中以外、GM以外
					&& !pc.isGm()) {
				_hateList.add(pc, 0);
				_target = pc;
			}
			onNpcAI(); // モンスターのＡＩを開始
			startChat(CHAT_TIMING_HIDE);
		}
	}

	// ■■■■■■■■■■■■■ 移動関連 ■■■■■■■■■■■

	// 指定された方向に移動させる
	public void setDirectionMove(int dir) {
		if (dir >= 0) {
			int nx = 0;
			int ny = 0;

			switch (dir) {
			case 1:
				nx = 1;
				ny = -1;
				setHeading(1);
				break;

			case 2:
				nx = 1;
				ny = 0;
				setHeading(2);
				break;

			case 3:
				nx = 1;
				ny = 1;
				setHeading(3);
				break;

			case 4:
				nx = 0;
				ny = 1;
				setHeading(4);
				break;

			case 5:
				nx = -1;
				ny = 1;
				setHeading(5);
				break;

			case 6:
				nx = -1;
				ny = 0;
				setHeading(6);
				break;

			case 7:
				nx = -1;
				ny = -1;
				setHeading(7);
				break;

			case 0:
				nx = 0;
				ny = -1;
				setHeading(0);
				break;

			default:
				break;

			}

			getMap().setPassable(getLocation(), true);

			int nnx = getX() + nx;
			int nny = getY() + ny;
			setX(nnx);
			setY(nny);

			getMap().setPassable(getLocation(), false);

			broadcastPacket(new S_MoveCharPacket(this));

			// movement_distanceマス以上離れたらホームポイントへテレポート
			if (getMovementDistance() > 0) {
				if ((this instanceof L1GuardInstance)
						|| (this instanceof L1MerchantInstance)
						|| (this instanceof L1MonsterInstance)) {
					if (getLocation().getLineDistance(
							new Point(getHomeX(), getHomeY())) > getMovementDistance()) {
						teleport(getHomeX(), getHomeY(), getHeading());
					}
				}
			}
			// 判斷士兵的怨靈、怨靈、哈蒙將軍的怨靈離開墓園範圍時傳送回墓園！
			if ((getNpcTemplate().get_npcId() >= 45912)
					&& (getNpcTemplate().get_npcId() <= 45916)) {
				if (!((getX() >= 32591) && (getX() <= 32644)
						&& (getY() >= 32643) && (getY() <= 32688) && (getMapId() == 4))) {
					teleport(getHomeX(), getHomeY(), getHeading());
				}
			}
		}
	}

	public int moveDirection(int x, int y) { // 目標点Ｘ 目標点Ｙ
		return moveDirection(x, y,
				getLocation().getLineDistance(new Point(x, y)));
	}

	// 目標までの距離に応じて最適と思われるルーチンで進む方向を返す
	public int moveDirection(int x, int y, double d) { // 目標点Ｘ 目標点Ｙ 目標までの距離
		int dir = 0;
		if ((hasSkillEffect(40) == true) && (d >= 2D)) { // ダークネスが掛かっていて、距離が2以上の場合追跡終了
			return -1;
		} else if (d > 30D) { // 距離が激しく遠い場合は追跡終了
			return -1;
		} else if (d > courceRange) { // 距離が遠い場合は単純計算
			dir = targetDirection(x, y);
			dir = checkObject(getX(), getY(), getMapId(), dir);
		} else { // 目標までの最短経路を探索
			dir = _serchCource(x, y);
			if (dir == -1) { // 目標までの経路がなっかた場合はとりあえず近づいておく
				dir = targetDirection(x, y);
				if (!isExsistCharacterBetweenTarget(dir)) {
					dir = checkObject(getX(), getY(), getMapId(), dir);
				}
			}
		}
		return dir;
	}

	private boolean isExsistCharacterBetweenTarget(int dir) {
		if (!(this instanceof L1MonsterInstance)) { // モンスター以外は対象外
			return false;
		}
		if (_target == null) { // ターゲットがいない場合
			return false;
		}

		int locX = getX();
		int locY = getY();
		int targetX = locX;
		int targetY = locY;

		if (dir == 1) {
			targetX = locX + 1;
			targetY = locY - 1;
		} else if (dir == 2) {
			targetX = locX + 1;
		} else if (dir == 3) {
			targetX = locX + 1;
			targetY = locY + 1;
		} else if (dir == 4) {
			targetY = locY + 1;
		} else if (dir == 5) {
			targetX = locX - 1;
			targetY = locY + 1;
		} else if (dir == 6) {
			targetX = locX - 1;
		} else if (dir == 7) {
			targetX = locX - 1;
			targetY = locY - 1;
		} else if (dir == 0) {
			targetY = locY - 1;
		}

		for (L1Object object : L1World.getInstance().getVisibleObjects(this, 1)) {
			// PC, Summon, Petがいる場合
			if ((object instanceof L1PcInstance)
					|| (object instanceof L1SummonInstance)
					|| (object instanceof L1PetInstance)) {
				L1Character cha = (L1Character) object;
				// 進行方向に立ちふさがっている場合、ターゲットリストに加える
				if ((cha.getX() == targetX) && (cha.getY() == targetY)
						&& (cha.getMapId() == getMapId())) {
					if (object instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) object;
						if (pc.isGhost()) { // UB観戦中のPCは除く
							continue;
						}
					}
					_hateList.add(cha, 0);
					_target = cha;
					return true;
				}
			}
		}
		return false;
	}

	// 目標の逆方向を返す
	public int targetReverseDirection(int tx, int ty) { // 目標点Ｘ 目標点Ｙ
		int dir = targetDirection(tx, ty);
		dir += 4;
		if (dir > 7) {
			dir -= 8;
		}
		return dir;
	}

	// 進みたい方向に障害物がないか確認、ある場合は前方斜め左右も確認後進める方向を返す
	// ※従来あった処理に、バックできない仕様を省いて、目標の反対（左右含む）には進まないようにしたもの
	public static int checkObject(int x, int y, short m, int d) { // 起点Ｘ 起点Ｙ
																	// マップＩＤ
		// 進行方向
		L1Map map = L1WorldMap.getInstance().getMap(m);
		if (d == 1) {
			if (map.isPassable(x, y, 1)) {
				return 1;
			} else if (map.isPassable(x, y, 0)) {
				return 0;
			} else if (map.isPassable(x, y, 2)) {
				return 2;
			}
		} else if (d == 2) {
			if (map.isPassable(x, y, 2)) {
				return 2;
			} else if (map.isPassable(x, y, 1)) {
				return 1;
			} else if (map.isPassable(x, y, 3)) {
				return 3;
			}
		} else if (d == 3) {
			if (map.isPassable(x, y, 3)) {
				return 3;
			} else if (map.isPassable(x, y, 2)) {
				return 2;
			} else if (map.isPassable(x, y, 4)) {
				return 4;
			}
		} else if (d == 4) {
			if (map.isPassable(x, y, 4)) {
				return 4;
			} else if (map.isPassable(x, y, 3)) {
				return 3;
			} else if (map.isPassable(x, y, 5)) {
				return 5;
			}
		} else if (d == 5) {
			if (map.isPassable(x, y, 5)) {
				return 5;
			} else if (map.isPassable(x, y, 4)) {
				return 4;
			} else if (map.isPassable(x, y, 6)) {
				return 6;
			}
		} else if (d == 6) {
			if (map.isPassable(x, y, 6)) {
				return 6;
			} else if (map.isPassable(x, y, 5)) {
				return 5;
			} else if (map.isPassable(x, y, 7)) {
				return 7;
			}
		} else if (d == 7) {
			if (map.isPassable(x, y, 7)) {
				return 7;
			} else if (map.isPassable(x, y, 6)) {
				return 6;
			} else if (map.isPassable(x, y, 0)) {
				return 0;
			}
		} else if (d == 0) {
			if (map.isPassable(x, y, 0)) {
				return 0;
			} else if (map.isPassable(x, y, 7)) {
				return 7;
			} else if (map.isPassable(x, y, 1)) {
				return 1;
			}
		}
		return -1;
	}

	// 目標までの最短経路の方向を返す
	// ※目標を中心とした探索範囲のマップで探索
	private int _serchCource(int x, int y) // 目標点Ｘ 目標点Ｙ
	{
		int i;
		int locCenter = courceRange + 1;
		int diff_x = x - locCenter; // Ｘの実際のロケーションとの差
		int diff_y = y - locCenter; // Ｙの実際のロケーションとの差
		int[] locBace = { getX() - diff_x, getY() - diff_y, 0, 0 }; // Ｘ Ｙ
		// 方向
		// 初期方向
		int[] locNext = new int[4];
		int[] locCopy;
		int[] dirFront = new int[5];
		boolean serchMap[][] = new boolean[locCenter * 2 + 1][locCenter * 2 + 1];
		LinkedList<int[]> queueSerch = new LinkedList<int[]>();

		// 探索用マップの設定
		for (int j = courceRange * 2 + 1; j > 0; j--) {
			for (i = courceRange - Math.abs(locCenter - j); i >= 0; i--) {
				serchMap[j][locCenter + i] = true;
				serchMap[j][locCenter - i] = true;
			}
		}

		// 初期方向の設置
		int[] firstCource = { 2, 4, 6, 0, 1, 3, 5, 7 };
		for (i = 0; i < 8; i++) {
			System.arraycopy(locBace, 0, locNext, 0, 4);
			_moveLocation(locNext, firstCource[i]);
			if ((locNext[0] - locCenter == 0) && (locNext[1] - locCenter == 0)) {
				// 最短経路が見つかった場合:隣
				return firstCource[i];
			}
			if (serchMap[locNext[0]][locNext[1]]) {
				int tmpX = locNext[0] + diff_x;
				int tmpY = locNext[1] + diff_y;
				boolean found = false;
				if (i == 0) {
					found = getMap().isPassable(tmpX, tmpY + 1, i);
				} else if (i == 1) {
					found = getMap().isPassable(tmpX - 1, tmpY + 1, i);
				} else if (i == 2) {
					found = getMap().isPassable(tmpX - 1, tmpY, i);
				} else if (i == 3) {
					found = getMap().isPassable(tmpX - 1, tmpY - 1, i);
				} else if (i == 4) {
					found = getMap().isPassable(tmpX, tmpY - 1, i);
				} else if (i == 5) {
					found = getMap().isPassable(tmpX + 1, tmpY - 1, i);
				} else if (i == 6) {
					found = getMap().isPassable(tmpX + 1, tmpY, i);
				} else if (i == 7) {
					found = getMap().isPassable(tmpX + 1, tmpY + 1, i);
				}
				if (found)// 移動経路があった場合
				{
					locCopy = new int[4];
					System.arraycopy(locNext, 0, locCopy, 0, 4);
					locCopy[2] = firstCource[i];
					locCopy[3] = firstCource[i];
					queueSerch.add(locCopy);
				}
				serchMap[locNext[0]][locNext[1]] = false;
			}
		}
		locBace = null;

		// 最短経路を探索
		while (queueSerch.size() > 0) {
			locBace = queueSerch.removeFirst();
			_getFront(dirFront, locBace[2]);
			for (i = 4; i >= 0; i--) {
				System.arraycopy(locBace, 0, locNext, 0, 4);
				_moveLocation(locNext, dirFront[i]);
				if ((locNext[0] - locCenter == 0)
						&& (locNext[1] - locCenter == 0)) {
					return locNext[3];
				}
				if (serchMap[locNext[0]][locNext[1]]) {
					int tmpX = locNext[0] + diff_x;
					int tmpY = locNext[1] + diff_y;
					boolean found = false;
					if (i == 0) {
						found = getMap().isPassable(tmpX, tmpY + 1, i);
					} else if (i == 1) {
						found = getMap().isPassable(tmpX - 1, tmpY + 1, i);
					} else if (i == 2) {
						found = getMap().isPassable(tmpX - 1, tmpY, i);
					} else if (i == 3) {
						found = getMap().isPassable(tmpX - 1, tmpY - 1, i);
					} else if (i == 4) {
						found = getMap().isPassable(tmpX, tmpY - 1, i);
					}
					if (found) // 移動経路があった場合
					{
						locCopy = new int[4];
						System.arraycopy(locNext, 0, locCopy, 0, 4);
						locCopy[2] = dirFront[i];
						queueSerch.add(locCopy);
					}
					serchMap[locNext[0]][locNext[1]] = false;
				}
			}
			locBace = null;
		}
		return -1; // 目標までの経路がない場合
	}

	private void _moveLocation(int[] ary, int d) {
		if (d == 1) {
			ary[0] = ary[0] + 1;
			ary[1] = ary[1] - 1;
		} else if (d == 2) {
			ary[0] = ary[0] + 1;
		} else if (d == 3) {
			ary[0] = ary[0] + 1;
			ary[1] = ary[1] + 1;
		} else if (d == 4) {
			ary[1] = ary[1] + 1;
		} else if (d == 5) {
			ary[0] = ary[0] - 1;
			ary[1] = ary[1] + 1;
		} else if (d == 6) {
			ary[0] = ary[0] - 1;
		} else if (d == 7) {
			ary[0] = ary[0] - 1;
			ary[1] = ary[1] - 1;
		} else if (d == 0) {
			ary[1] = ary[1] - 1;
		}
		ary[2] = d;
	}

	private void _getFront(int[] ary, int d) {
		if (d == 1) {
			ary[4] = 2;
			ary[3] = 0;
			ary[2] = 1;
			ary[1] = 3;
			ary[0] = 7;
		} else if (d == 2) {
			ary[4] = 2;
			ary[3] = 4;
			ary[2] = 0;
			ary[1] = 1;
			ary[0] = 3;
		} else if (d == 3) {
			ary[4] = 2;
			ary[3] = 4;
			ary[2] = 1;
			ary[1] = 3;
			ary[0] = 5;
		} else if (d == 4) {
			ary[4] = 2;
			ary[3] = 4;
			ary[2] = 6;
			ary[1] = 3;
			ary[0] = 5;
		} else if (d == 5) {
			ary[4] = 4;
			ary[3] = 6;
			ary[2] = 3;
			ary[1] = 5;
			ary[0] = 7;
		} else if (d == 6) {
			ary[4] = 4;
			ary[3] = 6;
			ary[2] = 0;
			ary[1] = 5;
			ary[0] = 7;
		} else if (d == 7) {
			ary[4] = 6;
			ary[3] = 0;
			ary[2] = 1;
			ary[1] = 5;
			ary[0] = 7;
		} else if (d == 0) {
			ary[4] = 2;
			ary[3] = 6;
			ary[2] = 0;
			ary[1] = 1;
			ary[0] = 7;
		}
	}

	// ■■■■■■■■■■■■ アイテム関連 ■■■■■■■■■■

	private void useHealPotion(int healHp, int effectId) {
		broadcastPacket(new S_SkillSound(getId(), effectId));
		if (hasSkillEffect(POLLUTE_WATER)) { // ポルートウォーター中は回復量1/2倍
			healHp /= 2;
		}
		if (this instanceof L1PetInstance) {
			((L1PetInstance) this).setCurrentHp(getCurrentHp() + healHp);
		} else if (this instanceof L1SummonInstance) {
			((L1SummonInstance) this).setCurrentHp(getCurrentHp() + healHp);
		} else {
			setCurrentHpDirect(getCurrentHp() + healHp);
		}
	}

	private void useHastePotion(int time) {
		broadcastPacket(new S_SkillHaste(getId(), 1, time));
		broadcastPacket(new S_SkillSound(getId(), 191));
		setMoveSpeed(1);
		setSkillEffect(STATUS_HASTE, time * 1000);
	}

	// アイテムの使用判定及び使用
	public static final int USEITEM_HEAL = 0;

	public static final int USEITEM_HASTE = 1;

	public static int[] healPotions = { POTION_OF_GREATER_HEALING,
			POTION_OF_EXTRA_HEALING, POTION_OF_HEALING };

	public static int[] haestPotions = { B_POTION_OF_GREATER_HASTE_SELF,
			POTION_OF_GREATER_HASTE_SELF, B_POTION_OF_HASTE_SELF,
			POTION_OF_HASTE_SELF };

	public void useItem(int type, int chance) { // 使用する種類 使用する可能性(％)
		if (hasSkillEffect(71)) {
			return; // ディケイ ポーション状態かチェック
		}

		if (Random.nextInt(100) > chance) {
			return; // 使用する可能性
		}

		if (type == USEITEM_HEAL) { // 回復系ポーション
			// 回復量の大きい順
			if (getInventory().consumeItem(POTION_OF_GREATER_HEALING, 1)) {
				useHealPotion(75, 197);
			} else if (getInventory().consumeItem(POTION_OF_EXTRA_HEALING, 1)) {
				useHealPotion(45, 194);
			} else if (getInventory().consumeItem(POTION_OF_HEALING, 1)) {
				useHealPotion(15, 189);
			}
		} else if (type == USEITEM_HASTE) { // ヘイスト系ポーション
			if (hasSkillEffect(1001)) {
				return; // ヘイスト状態チェック
			}

			// 効果の長い順
			if (getInventory().consumeItem(B_POTION_OF_GREATER_HASTE_SELF, 1)) {
				useHastePotion(2100);
			} else if (getInventory().consumeItem(POTION_OF_GREATER_HASTE_SELF,
					1)) {
				useHastePotion(1800);
			} else if (getInventory().consumeItem(B_POTION_OF_HASTE_SELF, 1)) {
				useHastePotion(350);
			} else if (getInventory().consumeItem(POTION_OF_HASTE_SELF, 1)) {
				useHastePotion(300);
			}
		}
	}

	// ■■■■■■■■■■■■■ スキル関連(npcskillsテーブル実装されたら消すかも) ■■■■■■■■■■■

	// 目標の隣へテレポート
	public boolean nearTeleport(int nx, int ny) {
		int rdir = Random.nextInt(8);
		int dir;
		for (int i = 0; i < 8; i++) {
			dir = rdir + i;
			if (dir > 7) {
				dir -= 8;
			}
			if (dir == 1) {
				nx++;
				ny--;
			} else if (dir == 2) {
				nx++;
			} else if (dir == 3) {
				nx++;
				ny++;
			} else if (dir == 4) {
				ny++;
			} else if (dir == 5) {
				nx--;
				ny++;
			} else if (dir == 6) {
				nx--;
			} else if (dir == 7) {
				nx--;
				ny--;
			} else if (dir == 0) {
				ny--;
			}
			if (getMap().isPassable(nx, ny)) {
				dir += 4;
				if (dir > 7) {
					dir -= 8;
				}
				teleport(nx, ny, dir);
				setCurrentMp(getCurrentMp() - 10);
				return true;
			}
		}
		return false;
	}

	// 目標へテレポート
	public void teleport(int nx, int ny, int dir) {
		for (L1PcInstance pc : L1World.getInstance().getRecognizePlayer(this)) {
			pc.sendPackets(new S_SkillSound(getId(), 169));
			pc.sendPackets(new S_RemoveObject(this));
			pc.removeKnownObject(this);
		}
		setX(nx);
		setY(ny);
		setHeading(dir);
	}

	// ----------From L1Character-------------
	private String _nameId; // ● ネームＩＤ

	public String getNameId() {
		return _nameId;
	}

	public void setNameId(String s) {
		_nameId = s;
	}

	private boolean _Agro; // ● アクティブか

	public boolean isAgro() {
		return _Agro;
	}

	public void setAgro(boolean flag) {
		_Agro = flag;
	}

	private boolean _Agrocoi; // ● インビジアクティブか

	public boolean isAgrocoi() {
		return _Agrocoi;
	}

	public void setAgrocoi(boolean flag) {
		_Agrocoi = flag;
	}

	private boolean _Agrososc; // ● 変身アクティブか

	public boolean isAgrososc() {
		return _Agrososc;
	}

	public void setAgrososc(boolean flag) {
		_Agrososc = flag;
	}

	private int _homeX; // ● ホームポイントＸ（モンスターの戻る位置とかペットの警戒位置）

	public int getHomeX() {
		return _homeX;
	}

	public void setHomeX(int i) {
		_homeX = i;
	}

	private int _homeY; // ● ホームポイントＹ（モンスターの戻る位置とかペットの警戒位置）

	public int getHomeY() {
		return _homeY;
	}

	public void setHomeY(int i) {
		_homeY = i;
	}

	private boolean _reSpawn; // ● 再ポップするかどうか

	public boolean isReSpawn() {
		return _reSpawn;
	}

	public void setreSpawn(boolean flag) {
		_reSpawn = flag;
	}

	private int _lightSize; // ● ライト ０．なし １～１４．大きさ

	public int getLightSize() {
		return _lightSize;
	}

	public void setLightSize(int i) {
		_lightSize = i;
	}

	private boolean _weaponBreaked; // ● ウェポンブレイク中かどうか

	public boolean isWeaponBreaked() {
		return _weaponBreaked;
	}

	public void setWeaponBreaked(boolean flag) {
		_weaponBreaked = flag;
	}

	private int _hiddenStatus; // ● 地中に潜ったり、空を飛んでいる状態

	public int getHiddenStatus() {
		return _hiddenStatus;
	}

	public void setHiddenStatus(int i) {
		_hiddenStatus = i;
	}

	// 行動距離
	private int _movementDistance = 0;

	public int getMovementDistance() {
		return _movementDistance;
	}

	public void setMovementDistance(int i) {
		_movementDistance = i;
	}

	// 表示用ロウフル
	private int _tempLawful = 0;

	public int getTempLawful() {
		return _tempLawful;
	}

	public void setTempLawful(int i) {
		_tempLawful = i;
	}

	protected int calcSleepTime(int sleepTime, int type) {
		switch (getMoveSpeed()) {
		case 0: // 通常
			break;
		case 1: // ヘイスト
			sleepTime -= (sleepTime * 0.25);
			break;
		case 2: // スロー
			sleepTime *= 2;
			break;
		}
		if (getBraveSpeed() == 1) {
			sleepTime -= (sleepTime * 0.25);
		}
		if (hasSkillEffect(WIND_SHACKLE)) {
			if ((type == ATTACK_SPEED) || (type == MAGIC_SPEED)) {
				sleepTime += (sleepTime * 0.25);
			}
		}
		return sleepTime;
	}

	protected void setAiRunning(boolean aiRunning) {
		_aiRunning = aiRunning;
	}

	protected boolean isAiRunning() {
		return _aiRunning;
	}

	protected void setActived(boolean actived) {
		_actived = actived;
	}

	protected boolean isActived() {
		return _actived;
	}

	protected void setFirstAttack(boolean firstAttack) {
		_firstAttack = firstAttack;
	}

	protected boolean isFirstAttack() {
		return _firstAttack;
	}

	protected void setSleepTime(int sleep_time) {
		_sleep_time = sleep_time;
	}

	protected int getSleepTime() {
		return _sleep_time;
	}

	protected void setDeathProcessing(boolean deathProcessing) {
		_deathProcessing = deathProcessing;
	}

	protected boolean isDeathProcessing() {
		return _deathProcessing;
	}

	public int drainMana(int drain) {
		if (_drainedMana >= Config.MANA_DRAIN_LIMIT_PER_NPC) {
			return 0;
		}
		int result = Math.min(drain, getCurrentMp());
		if (_drainedMana + result > Config.MANA_DRAIN_LIMIT_PER_NPC) {
			result = Config.MANA_DRAIN_LIMIT_PER_NPC - _drainedMana;
		}
		_drainedMana += result;
		return result;
	}

	public boolean _destroyed = false; // このインスタンスが破棄されているか

	// ※破棄後に動かないよう強制的にＡＩ等のスレッド処理中止（念のため）

	// NPCが別のNPCに変わる場合の処理
	protected void transform(int transformId) {
		stopHpRegeneration();
		stopMpRegeneration();
		int transformGfxId = getNpcTemplate().getTransformGfxId();
		if (transformGfxId != 0) {
			broadcastPacket(new S_SkillSound(getId(), transformGfxId));
		}
		L1Npc npcTemplate = NpcTable.getInstance().getTemplate(transformId);
		setting_template(npcTemplate);

		broadcastPacket(new S_NpcChangeShape(getId(), getTempCharGfx(), getLawful(), getStatus()));
		for (L1PcInstance pc : L1World.getInstance().getRecognizePlayer(this)) {
			onPerceive(pc);
		}

	}

	public void setRest(boolean _rest) {
		this._rest = _rest;
	}

	public boolean isRest() {
		return _rest;
	}

	private boolean _isResurrect;

	public boolean isResurrect() {
		return _isResurrect;
	}

	public void setResurrect(boolean flag) {
		_isResurrect = flag;
	}

	/** 妖精森林 物品掉落*/
	private boolean _isDropitems = false;

	public boolean isDropitems() {
		return _isDropitems;
	}

	public void setDropItems(boolean i) {
		_isDropitems = i;
	}

	private boolean _forDropitems = false;

	public boolean forDropitems() {
		return _forDropitems;
	}

	public void giveDropItems(boolean i) {
		_forDropitems = i;
	}

	@Override
	public synchronized void resurrect(int hp) {
		if (_destroyed) {
			return;
		}
		if (_deleteTask != null) {
			if (!_future.cancel(false)) { // キャンセルできない
				return;
			}
			_deleteTask = null;
			_future = null;
		}
		super.resurrect(hp);

		// キャンセレーションをエフェクトなしでかける
		// 本来は死亡時に行うべきだが、負荷が大きくなるため復活時に行う
		L1SkillUse skill = new L1SkillUse();
		skill.handleCommands(null, CANCELLATION, getId(), getX(), getY(), null,
				0, L1SkillUse.TYPE_LOGIN, this);
	}

	// 死んでから消えるまでの時間計測用
	private DeleteTimer _deleteTask;

	private ScheduledFuture<?> _future = null;

	protected synchronized void startDeleteTimer() {
		if (_deleteTask != null) {
			return;
		}
		_deleteTask = new DeleteTimer(getId());
		_future = GeneralThreadPool.getInstance().schedule(_deleteTask,
				Config.NPC_DELETION_TIME * 1000);
	}

	protected static class DeleteTimer extends TimerTask {
		private int _id;

		protected DeleteTimer(int oId) {
			_id = oId;
			if (!(L1World.getInstance().findObject(_id) instanceof L1NpcInstance)) {
				throw new IllegalArgumentException("allowed only L1NpcInstance");
			}
		}

		@Override
		public void run() {
			L1NpcInstance npc = (L1NpcInstance) L1World.getInstance()
					.findObject(_id);
			if ((npc == null) || !npc.isDead() || npc._destroyed) {
				return; // 復活してるか、既に破棄済みだったら抜け
			}
			try {
				npc.deleteMe();
			} catch (Exception e) { // 絶対例外を投げないように
				e.printStackTrace();
			}
		}
	}

	private L1MobGroupInfo _mobGroupInfo = null;

	public boolean isInMobGroup() {
		return getMobGroupInfo() != null;
	}

	public L1MobGroupInfo getMobGroupInfo() {
		return _mobGroupInfo;
	}

	public void setMobGroupInfo(L1MobGroupInfo m) {
		_mobGroupInfo = m;
	}

	private int _mobGroupId = 0;

	public int getMobGroupId() {
		return _mobGroupId;
	}

	public void setMobGroupId(int i) {
		_mobGroupId = i;
	}

	public void startChat(int chatTiming) {
		// 出現時のチャットにも関わらず死亡中、死亡時のチャットにも関わらず生存中
		if ((chatTiming == CHAT_TIMING_APPEARANCE) && isDead()) {
			return;
		}
		if ((chatTiming == CHAT_TIMING_DEAD) && !isDead()) {
			return;
		}
		if ((chatTiming == CHAT_TIMING_HIDE) && isDead()) {
			return;
		}
		if ((chatTiming == CHAT_TIMING_GAME_TIME) && isDead()) {
			return;
		}

		int npcId = getNpcTemplate().get_npcId();
		L1NpcChat npcChat = null;
		if (chatTiming == CHAT_TIMING_APPEARANCE) {
			npcChat = NpcChatTable.getInstance().getTemplateAppearance(npcId);
		} else if (chatTiming == CHAT_TIMING_DEAD) {
			npcChat = NpcChatTable.getInstance().getTemplateDead(npcId);
		} else if (chatTiming == CHAT_TIMING_HIDE) {
			npcChat = NpcChatTable.getInstance().getTemplateHide(npcId);
		} else if (chatTiming == CHAT_TIMING_GAME_TIME) {
			npcChat = NpcChatTable.getInstance().getTemplateGameTime(npcId);
		}
		if (npcChat == null) {
			return;
		}

		Timer timer = new Timer(true);
		L1NpcChatTimer npcChatTimer = new L1NpcChatTimer(this, npcChat);
		if (!npcChat.isRepeat()) {
			timer.schedule(npcChatTimer, npcChat.getStartDelayTime());
		} else {
			timer.scheduleAtFixedRate(npcChatTimer,
					npcChat.getStartDelayTime(), npcChat.getRepeatInterval());
		}
	}

	public int getAtkRanged() {
		if (_polyAtkRanged == -1) {
			return getNpcTemplate().get_ranged();
		}
		return _polyAtkRanged;
	}

	private int _polyAtkRanged = -1;

	public int getPolyAtkRanged() {
		return _polyAtkRanged;
	}

	public void setPolyAtkRanged(int i) {
		_polyAtkRanged = i;
	}

	private int _polyArrowGfx = 0;

	public int getPolyArrowGfx() {
		return _polyArrowGfx;
	}

	public void setPolyArrowGfx(int i) {
		_polyArrowGfx = i;
	}

}
