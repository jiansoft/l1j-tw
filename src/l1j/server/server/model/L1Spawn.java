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

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.Config;
import l1j.server.server.ActionCodes;
import l1j.server.server.GeneralThreadPool;
import l1j.server.server.IdFactory;
import l1j.server.server.datatables.NpcTable;
import l1j.server.server.model.Instance.L1DoorInstance;
import l1j.server.server.model.Instance.L1MonsterInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.gametime.L1GameTime;
import l1j.server.server.model.gametime.L1GameTimeAdapter;
import l1j.server.server.model.gametime.L1GameTimeClock;
import l1j.server.server.templates.L1Npc;
import l1j.server.server.templates.L1SpawnTime;
import l1j.server.server.types.Point;
import l1j.server.server.utils.Random;
import l1j.server.server.utils.collections.Lists;
import l1j.server.server.utils.collections.Maps;

/**
 * NPC/怪物生成管理類別。
 * <p>
 * 此類別負責管理遊戲世界中 NPC 與怪物的生成（spawn）邏輯，包含：
 * <ul>
 *   <li>初始生成位置的計算與設定</li>
 *   <li>重生（respawn）延遲時間的管理</li>
 *   <li>區域生成與隨機生成的座標計算</li>
 *   <li>時間限制生成的控制</li>
 *   <li>家點（Home Point）系統的管理</li>
 * </ul>
 * <p>
 * 此類別繼承自 {@link L1GameTimeAdapter}，可監聽遊戲時間變化事件，
 * 以實現時間限制生成功能（例如：只在特定時段出現的怪物）。
 *
 * @see L1GameTimeAdapter
 * @see L1NpcInstance
 * @see L1SpawnTime
 */
public class L1Spawn extends L1GameTimeAdapter {

	/** 日誌記錄器 */
	private static Logger _log = Logger.getLogger(L1Spawn.class.getName());

	/** NPC 模板，定義此生成點要生成的 NPC 類型 */
	private final L1Npc _template;

	/** 生成點 ID，用於在 spawn 資料表中識別此生成點 */
	private int _id;

	/** 生成點位置名稱描述 */
	private String _location;

	/** 此生成點的最大生成數量 */
	private int _maximumCount;

	/** 要生成的 NPC ID */
	private int _npcid;

	/** 怪物群組 ID，用於群組生成 */
	private int _groupId;

	/** 生成點中心 X 座標 */
	private int _locx;

	/** 生成點中心 Y 座標 */
	private int _locy;

	/** X 座標的隨機偏移範圍 */
	private int Randomx;

	/** Y 座標的隨機偏移範圍 */
	private int Randomy;

	/** 區域生成的左上角 X 座標 */
	private int _locx1;

	/** 區域生成的左上角 Y 座標 */
	private int _locy1;

	/** 區域生成的右下角 X 座標 */
	private int _locx2;

	/** 區域生成的右下角 Y 座標 */
	private int _locy2;

	/** NPC 生成時的面向方向（0-7） */
	private int _heading;

	/** 最小重生延遲時間（秒） */
	private int _minRespawnDelay;

	/** 最大重生延遲時間（秒） */
	private int _maxRespawnDelay;

	/** 生成點所在的地圖 ID */
	private short _mapid;

	/** 是否允許在玩家畫面內重生 */
	private boolean _respaenScreen;

	/** NPC 的最大移動距離 */
	private int _movementDistance;

	/** NPC 是否處於休息狀態（不主動攻擊） */
	private boolean _rest;

	/** 生成類型（例如：PC 周圍生成） */
	private int _spawnType;

	/** 重生延遲的隨機區間（最大延遲 - 最小延遲） */
	private int _delayInterval;

	/** 時間限制生成設定 */
	private L1SpawnTime _time;

	/**
	 * 家點座標映射表。
	 * <p>
	 * 用於儲存初始生成時各個物件的家點座標，
	 * 在重生時會參考此家點進行位置計算。
	 * Key 為生成編號，Value 為座標點。
	 */
	private Map<Integer, Point> _homePoint = null;

	/** 此生成點目前管理的所有 NPC 實例列表 */
	private List<L1NpcInstance> _mobs = Lists.newList();

	/** NPC 名稱 */
	private String _name;

	/**
	 * 生成任務內部類別。
	 * <p>
	 * 實作 {@link Runnable} 介面，用於排程執行 NPC 的延遲生成。
	 * 當重生計時器到期時，此任務會被執行以生成新的 NPC。
	 */
	private class SpawnTask implements Runnable {
		/** 生成編號，用於識別家點 */
		private int _spawnNumber;

		/** 物件 ID，用於重用已死亡 NPC 的 ID */
		private int _objectId;

		/**
		 * 建構生成任務。
		 *
		 * @param spawnNumber 生成編號
		 * @param objectId    物件 ID（0 表示分配新 ID）
		 */
		private SpawnTask(int spawnNumber, int objectId) {
			_spawnNumber = spawnNumber;
			_objectId = objectId;
		}

		/**
		 * 執行生成任務。
		 * <p>
		 * 當排程時間到達時，呼叫 {@link L1Spawn#doSpawn(int, int)} 進行實際生成。
		 */
		@Override
		public void run() {
			doSpawn(_spawnNumber, _objectId);
		}
	}

	/**
	 * 建構 L1Spawn 實例。
	 *
	 * @param mobTemplate NPC 模板，定義要生成的 NPC 類型
	 */
	public L1Spawn(L1Npc mobTemplate) {
		_template = mobTemplate;
	}

	/**
	 * 取得 NPC 名稱。
	 *
	 * @return NPC 名稱
	 */
	public String getName() {
		return _name;
	}

	/**
	 * 設定 NPC 名稱。
	 *
	 * @param name NPC 名稱
	 */
	public void setName(String name) {
		_name = name;
	}

	/**
	 * 取得生成點所在的地圖 ID。
	 *
	 * @return 地圖 ID
	 */
	public short getMapId() {
		return _mapid;
	}

	/**
	 * 設定生成點所在的地圖 ID。
	 *
	 * @param _mapid 地圖 ID
	 */
	public void setMapId(short _mapid) {
		this._mapid = _mapid;
	}

	/**
	 * 檢查是否允許在玩家畫面內重生。
	 *
	 * @return 如果允許在畫面內重生則回傳 {@code true}
	 */
	public boolean isRespawnScreen() {
		return _respaenScreen;
	}

	/**
	 * 設定是否允許在玩家畫面內重生。
	 *
	 * @param flag {@code true} 表示允許在畫面內重生
	 */
	public void setRespawnScreen(boolean flag) {
		_respaenScreen = flag;
	}

	/**
	 * 取得 NPC 的最大移動距離。
	 *
	 * @return 最大移動距離
	 */
	public int getMovementDistance() {
		return _movementDistance;
	}

	/**
	 * 設定 NPC 的最大移動距離。
	 *
	 * @param i 最大移動距離
	 */
	public void setMovementDistance(int i) {
		_movementDistance = i;
	}

	/**
	 * 取得此生成點的最大生成數量。
	 *
	 * @return 最大生成數量
	 */
	public int getAmount() {
		return _maximumCount;
	}

	/**
	 * 取得怪物群組 ID。
	 *
	 * @return 群組 ID
	 */
	public int getGroupId() {
		return _groupId;
	}

	/**
	 * 取得生成點 ID。
	 *
	 * @return 生成點 ID
	 */
	public int getId() {
		return _id;
	}

	/**
	 * 取得生成點位置名稱描述。
	 *
	 * @return 位置名稱
	 */
	public String getLocation() {
		return _location;
	}

	/**
	 * 取得生成點中心 X 座標。
	 *
	 * @return X 座標
	 */
	public int getLocX() {
		return _locx;
	}

	/**
	 * 取得生成點中心 Y 座標。
	 *
	 * @return Y 座標
	 */
	public int getLocY() {
		return _locy;
	}

	/**
	 * 取得要生成的 NPC ID。
	 *
	 * @return NPC ID
	 */
	public int getNpcId() {
		return _npcid;
	}

	/**
	 * 取得 NPC 生成時的面向方向。
	 *
	 * @return 面向方向（0-7）
	 */
	public int getHeading() {
		return _heading;
	}

	/**
	 * 取得 X 座標的隨機偏移範圍。
	 *
	 * @return X 隨機偏移範圍
	 */
	public int getRandomx() {
		return Randomx;
	}

	/**
	 * 取得 Y 座標的隨機偏移範圍。
	 *
	 * @return Y 隨機偏移範圍
	 */
	public int getRandomy() {
		return Randomy;
	}

	/**
	 * 取得區域生成的左上角 X 座標。
	 *
	 * @return 左上角 X 座標
	 */
	public int getLocX1() {
		return _locx1;
	}

	/**
	 * 取得區域生成的左上角 Y 座標。
	 *
	 * @return 左上角 Y 座標
	 */
	public int getLocY1() {
		return _locy1;
	}

	/**
	 * 取得區域生成的右下角 X 座標。
	 *
	 * @return 右下角 X 座標
	 */
	public int getLocX2() {
		return _locx2;
	}

	/**
	 * 取得區域生成的右下角 Y 座標。
	 *
	 * @return 右下角 Y 座標
	 */
	public int getLocY2() {
		return _locy2;
	}

	/**
	 * 取得最小重生延遲時間。
	 *
	 * @return 最小重生延遲時間（秒）
	 */
	public int getMinRespawnDelay() {
		return _minRespawnDelay;
	}

	/**
	 * 取得最大重生延遲時間。
	 *
	 * @return 最大重生延遲時間（秒）
	 */
	public int getMaxRespawnDelay() {
		return _maxRespawnDelay;
	}

	/**
	 * 設定此生成點的最大生成數量。
	 *
	 * @param amount 最大生成數量
	 */
	public void setAmount(int amount) {
		_maximumCount = amount;
	}

	/**
	 * 設定生成點 ID。
	 *
	 * @param id 生成點 ID
	 */
	public void setId(int id) {
		_id = id;
	}

	/**
	 * 設定怪物群組 ID。
	 *
	 * @param i 群組 ID
	 */
	public void setGroupId(int i) {
		_groupId = i;
	}

	/**
	 * 設定生成點位置名稱描述。
	 *
	 * @param location 位置名稱
	 */
	public void setLocation(String location) {
		_location = location;
	}

	/**
	 * 設定生成點中心 X 座標。
	 *
	 * @param locx X 座標
	 */
	public void setLocX(int locx) {
		_locx = locx;
	}

	/**
	 * 設定生成點中心 Y 座標。
	 *
	 * @param locy Y 座標
	 */
	public void setLocY(int locy) {
		_locy = locy;
	}

	/**
	 * 設定要生成的 NPC ID。
	 *
	 * @param npcid NPC ID
	 */
	public void setNpcid(int npcid) {
		_npcid = npcid;
	}

	/**
	 * 設定 NPC 生成時的面向方向。
	 *
	 * @param heading 面向方向（0-7）
	 */
	public void setHeading(int heading) {
		_heading = heading;
	}

	/**
	 * 設定 X 座標的隨機偏移範圍。
	 *
	 * @param randomx X 隨機偏移範圍
	 */
	public void setRandomx(int randomx) {
		Randomx = randomx;
	}

	/**
	 * 設定 Y 座標的隨機偏移範圍。
	 *
	 * @param randomy Y 隨機偏移範圍
	 */
	public void setRandomy(int randomy) {
		Randomy = randomy;
	}

	/**
	 * 設定區域生成的左上角 X 座標。
	 *
	 * @param locx1 左上角 X 座標
	 */
	public void setLocX1(int locx1) {
		_locx1 = locx1;
	}

	/**
	 * 設定區域生成的左上角 Y 座標。
	 *
	 * @param locy1 左上角 Y 座標
	 */
	public void setLocY1(int locy1) {
		_locy1 = locy1;
	}

	/**
	 * 設定區域生成的右下角 X 座標。
	 *
	 * @param locx2 右下角 X 座標
	 */
	public void setLocX2(int locx2) {
		_locx2 = locx2;
	}

	/**
	 * 設定區域生成的右下角 Y 座標。
	 *
	 * @param locy2 右下角 Y 座標
	 */
	public void setLocY2(int locy2) {
		_locy2 = locy2;
	}

	/**
	 * 設定最小重生延遲時間。
	 *
	 * @param i 最小重生延遲時間（秒）
	 */
	public void setMinRespawnDelay(int i) {
		_minRespawnDelay = i;
	}

	/**
	 * 設定最大重生延遲時間。
	 *
	 * @param i 最大重生延遲時間（秒）
	 */
	public void setMaxRespawnDelay(int i) {
		_maxRespawnDelay = i;
	}

	/**
	 * 計算重生延遲時間。
	 * <p>
	 * 延遲時間由最小延遲時間加上隨機區間計算而得。
	 * 若設定了時間限制生成，且當前時間不在指定時段內，
	 * 則延遲時間會調整為距離下次指定時段開始的時間。
	 *
	 * @return 重生延遲時間（毫秒）
	 */
	private int calcRespawnDelay() {
		int respawnDelay = _minRespawnDelay * 1000;
		if (_delayInterval > 0) {
			respawnDelay += Random.nextInt(_delayInterval) * 1000;
		}
		L1GameTime currentTime = L1GameTimeClock.getInstance().currentTime();
		// 如果不在指定時間內，則加上距離指定時間開始的延遲
		if ((_time != null) && !_time.getTimePeriod().includes(currentTime)) {
			long diff = (_time.getTimeStart().getTime() - currentTime.toTime().getTime());
			if (diff < 0) {
				diff += 24 * 1000L * 3600L;
			}
			diff /= 6; // 真實時間轉換為遊戲時間
			respawnDelay = (int) diff;
		}
		return respawnDelay;
	}

	/**
	 * 執行生成任務排程。
	 * <p>
	 * 建立 {@link SpawnTask} 並排程在計算出的重生延遲時間後執行。
	 *
	 * @param spawnNumber 生成編號，用於 L1Spawn 管理。若無家點設定，可指定任意值。
	 * @param objectId    物件 ID（0 表示分配新 ID，非 0 表示重用指定 ID）
	 */
	public void executeSpawnTask(int spawnNumber, int objectId) {
		SpawnTask task = new SpawnTask(spawnNumber, objectId);
		GeneralThreadPool.getInstance().schedule(task, calcRespawnDelay());
	}

	/** 是否為初始生成階段的標記 */
	private boolean _initSpawn = false;

	/** 是否啟用家點系統 */
	private boolean _spawnHomePoint;

	/**
	 * 初始化生成點並執行初始生成。
	 * <p>
	 * 此方法執行以下操作：
	 * <ol>
	 *   <li>若設定了時間限制刪除，則註冊遊戲時間監聽器</li>
	 *   <li>計算重生延遲區間</li>
	 *   <li>根據設定決定是否啟用家點系統</li>
	 *   <li>依據最大生成數量執行初始生成</li>
	 * </ol>
	 * <p>
	 * 家點系統啟用條件：
	 * <ul>
	 *   <li>Config.SPAWN_HOME_POINT 為 true</li>
	 *   <li>生成數量達到 Config.SPAWN_HOME_POINT_COUNT</li>
	 *   <li>最小重生延遲達到 Config.SPAWN_HOME_POINT_DELAY</li>
	 *   <li>使用區域生成模式</li>
	 * </ul>
	 */
	public void init() {
		if ((_time != null) && _time.isDeleteAtEndTime()) {
			// 若設定了時間外刪除，則監聽時間變化事件
			L1GameTimeClock.getInstance().addListener(this);
		}
		_delayInterval = _maxRespawnDelay - _minRespawnDelay;
		_initSpawn = true;
		// 判斷是否啟用家點系統
		if (Config.SPAWN_HOME_POINT && (Config.SPAWN_HOME_POINT_COUNT <= getAmount()) && (Config.SPAWN_HOME_POINT_DELAY >= getMinRespawnDelay())
				&& isAreaSpawn()) {
			_spawnHomePoint = true;
			_homePoint = Maps.newMap();
		}

		int spawnNum = 0;
		while (spawnNum < _maximumCount) {
			// spawnNum 從 1 到 maximumCount
			doSpawn(++spawnNum);
		}
		_initSpawn = false;
	}

	/**
	 * 執行初始生成。
	 * <p>
	 * 若設定了家點，則根據 spawnNumber 進行生成；
	 * 否則 spawnNumber 不使用。
	 * <p>
	 * 若當前時間不在指定時段內，則排程延遲生成並結束。
	 *
	 * @param spawnNumber 生成編號
	 */
	protected void doSpawn(int spawnNumber) {
		// 若不在指定時間內，則排程下次生成後結束
		if ((_time != null) && !_time.getTimePeriod().includes(L1GameTimeClock.getInstance().currentTime())) {
			executeSpawnTask(spawnNumber, 0);
			return;
		}
		doSpawn(spawnNumber, 0);
	}

	/**
	 * 執行 NPC 生成（重生）。
	 * <p>
	 * 此方法為核心生成邏輯，包含：
	 * <ul>
	 *   <li>根據生成類型計算生成座標</li>
	 *   <li>建立 NPC 實例並設定屬性</li>
	 *   <li>處理特殊 NPC 的地圖隨機選擇</li>
	 *   <li>確保生成位置可通行且不在玩家畫面內（若設定不允許）</li>
	 *   <li>將 NPC 加入遊戲世界</li>
	 *   <li>啟動怪物 AI 與群組生成</li>
	 * </ul>
	 *
	 * @param spawnNumber 生成編號，用於家點計算
	 * @param objectId    物件 ID（0 表示分配新 ID，非 0 表示重用）
	 */
	protected void doSpawn(int spawnNumber, int objectId) {
		L1NpcInstance mob = null;
		try {
			int newlocx = getLocX();
			int newlocy = getLocY();
			int tryCount = 0;

			mob = NpcTable.getInstance().newNpcInstance(_template);
			synchronized (_mobs) {
				_mobs.add(mob);
			}
			if (objectId == 0) {
				mob.setId(IdFactory.getInstance().nextId());
			}
			else {
				mob.setId(objectId); // オブジェクトID再利用
			}

			if ((0 <= getHeading()) && (getHeading() <= 7)) {
				mob.setHeading(getHeading());
			}
			else {
				// heading値が正しくない
				mob.setHeading(5);
			}

			int npcId = mob.getNpcTemplate().get_npcId();
			if ((npcId == 45488) && (getMapId() == 9)) { // 卡士伯
				mob.setMap((short) (getMapId() + Random.nextInt(2)));
			}
			else if ((npcId == 45601) && (getMapId() == 11)) { // 死亡騎士
				mob.setMap((short) (getMapId() + Random.nextInt(3)));
			}
			else if ((npcId == 81322) && (getMapId() == 25)) { // 黑騎士副隊長
				mob.setMap((short) (getMapId() + Random.nextInt(2)));
			}
			else {
				mob.setMap(getMapId());
			}
			mob.setMovementDistance(getMovementDistance());
			mob.setRest(isRest());
			while (tryCount <= 50) {
				switch (getSpawnType()) {
					case SPAWN_TYPE_PC_AROUND: // PC周辺に湧くタイプ
						if (!_initSpawn) { // 初期配置では無条件に通常spawn
							List<L1PcInstance> players = Lists.newList();
							for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
								if (getMapId() == pc.getMapId()) {
									players.add(pc);
								}
							}
							if (players.size() > 0) {
								L1PcInstance pc = players.get(Random.nextInt(players.size()));
								L1Location loc = pc.getLocation().randomLocation(PC_AROUND_DISTANCE, false);
								newlocx = loc.getX();
								newlocy = loc.getY();
								break;
							}
						}
						// フロアにPCがいなければ通常の出現方法
					default:
						if (isAreaSpawn()) { // 座標が範囲指定されている場合
							Point pt = null;
							if (_spawnHomePoint && (null != (pt = _homePoint.get(spawnNumber)))) { // ホームポイントを元に再出現させる場合
								L1Location loc = new L1Location(pt, getMapId()).randomLocation(Config.SPAWN_HOME_POINT_RANGE, false);
								newlocx = loc.getX();
								newlocy = loc.getY();
							}
							else {
								int rangeX = getLocX2() - getLocX1();
								int rangeY = getLocY2() - getLocY1();
								newlocx = Random.nextInt(rangeX) + getLocX1();
								newlocy = Random.nextInt(rangeY) + getLocY1();
							}
							if (tryCount > 49) { // 出現位置が決まらない時はlocx,locyの値
								newlocx = getLocX();
								newlocy = getLocY();
							}
						}
						else if (isRandomSpawn()) { // 座標のランダム値が指定されている場合
							newlocx = (getLocX() + (Random.nextInt(getRandomx()) - Random.nextInt(getRandomx())));
							newlocy = (getLocY() + (Random.nextInt(getRandomy()) - Random.nextInt(getRandomy())));
						}
						else { // どちらも指定されていない場合
							newlocx = getLocX();
							newlocy = getLocY();
						}
				}
				mob.setX(newlocx);
				mob.setHomeX(newlocx);
				mob.setY(newlocy);
				mob.setHomeY(newlocy);

				if (mob.getMap().isInMap(mob.getLocation()) && mob.getMap().isPassable(mob.getLocation())) {
					if (mob instanceof L1MonsterInstance) {
						if (isRespawnScreen()) {
							break;
						}
						L1MonsterInstance mobtemp = (L1MonsterInstance) mob;
						if (L1World.getInstance().getVisiblePlayer(mobtemp).isEmpty()) {
							break;
						}
						// 画面内にPCが居て出現できない場合は、3秒後にスケジューリングしてやり直し
						SpawnTask task = new SpawnTask(spawnNumber, mob.getId());
						GeneralThreadPool.getInstance().schedule(task, 3000L);
						return;
					}
				}
				tryCount++;
			}
			if (mob instanceof L1MonsterInstance) {
				((L1MonsterInstance) mob).initHide();
			}

			mob.setSpawn(this);
			mob.setreSpawn(true);
			mob.setSpawnNumber(spawnNumber); // L1Spawnでの管理番号(ホームポイントに使用)
			if (_initSpawn && _spawnHomePoint) { // 初期配置でホームポイントを設定
				Point pt = new Point(mob.getX(), mob.getY());
				_homePoint.put(spawnNumber, pt); // ここで保存したpointを再出現時に使う
			}

			if (mob instanceof L1MonsterInstance) {
				if (mob.getMapId() == 666) {
					((L1MonsterInstance) mob).set_storeDroped(true);
				}
			}
			if ((npcId == 45573) && (mob.getMapId() == 2)) { // バフォメット
				for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
					if (pc.getMapId() == 2) {
						L1Teleport.teleport(pc, 32664, 32797, (short) 2, 0, true);
					}
				}
			}

			if (((npcId == 46142) && (mob.getMapId() == 73)) || ((npcId == 46141) && (mob.getMapId() == 74))) {
				for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
					if ((pc.getMapId() >= 72) && (pc.getMapId() <= 74)) {
						L1Teleport.teleport(pc, 32840, 32833, (short) 72, pc.getHeading(), true);
					}
				}
			}
			if ((npcId == 81341) && ((mob.getMapId() == 2000) || (mob.getMapId() == 2001) || (mob.getMapId() == 2002) || (mob.getMapId() == 2003))) { // 再生之祭壇
				for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
					if ((pc.getMapId() >= 2000) && (pc.getMapId() <= 2003)) {
						L1Teleport.teleport(pc, 32933, 32988, (short) 410, 5, true);
					}
				}
			}

			doCrystalCave(npcId);

			L1World.getInstance().storeObject(mob);
			L1World.getInstance().addVisibleObject(mob);

			if (mob instanceof L1MonsterInstance) {
				L1MonsterInstance mobtemp = (L1MonsterInstance) mob;
				if (!_initSpawn && (mobtemp.getHiddenStatus() == 0)) {
					mobtemp.onNpcAI(); // モンスターのＡＩを開始
				}
			}
			if (getGroupId() != 0) {
				L1MobGroupSpawn.getInstance().doSpawn(mob, getGroupId(), isRespawnScreen(), _initSpawn);
			}
			mob.turnOnOffLight();
			mob.startChat(L1NpcInstance.CHAT_TIMING_APPEARANCE); // チャット開始
		}
		catch (Exception e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	/**
	 * 設定 NPC 是否處於休息狀態。
	 * <p>
	 * 休息狀態的 NPC 不會主動攻擊玩家。
	 *
	 * @param flag {@code true} 表示休息狀態
	 */
	public void setRest(boolean flag) {
		_rest = flag;
	}

	/**
	 * 檢查 NPC 是否處於休息狀態。
	 *
	 * @return 如果處於休息狀態則回傳 {@code true}
	 */
	public boolean isRest() {
		return _rest;
	}

	/** 生成類型：在玩家周圍生成 */
	private static final int SPAWN_TYPE_PC_AROUND = 1;

	/** 在玩家周圍生成時的距離（格數） */
	private static final int PC_AROUND_DISTANCE = 30;

	/**
	 * 取得生成類型。
	 *
	 * @return 生成類型
	 * @see #SPAWN_TYPE_PC_AROUND
	 */
	private int getSpawnType() {
		return _spawnType;
	}

	/**
	 * 設定生成類型。
	 *
	 * @param type 生成類型（例如：{@link #SPAWN_TYPE_PC_AROUND}）
	 */
	public void setSpawnType(int type) {
		_spawnType = type;
	}

	/**
	 * 檢查是否為區域生成模式。
	 * <p>
	 * 區域生成模式表示使用 (locX1, locY1) 到 (locX2, locY2) 定義的矩形範圍內隨機選擇生成位置。
	 *
	 * @return 如果四個座標邊界都已設定則回傳 {@code true}
	 */
	private boolean isAreaSpawn() {
		return (getLocX1() != 0) && (getLocY1() != 0) && (getLocX2() != 0) && (getLocY2() != 0);
	}

	/**
	 * 檢查是否為隨機偏移生成模式。
	 * <p>
	 * 隨機偏移生成模式表示在中心座標的基礎上，加上 randomX 和 randomY 範圍內的隨機偏移。
	 *
	 * @return 如果設定了 X 或 Y 的隨機偏移範圍則回傳 {@code true}
	 */
	private boolean isRandomSpawn() {
		return (getRandomx() != 0) || (getRandomy() != 0);
	}

	/**
	 * 取得時間限制生成設定。
	 *
	 * @return 時間限制生成設定，若無設定則為 {@code null}
	 */
	public L1SpawnTime getTime() {
		return _time;
	}

	/**
	 * 設定時間限制生成設定。
	 * <p>
	 * 設定後，NPC 只會在指定的時間段內生成。
	 *
	 * @param time 時間限制生成設定
	 */
	public void setTime(L1SpawnTime time) {
		_time = time;
	}

	/**
	 * 遊戲時間分鐘變化事件處理。
	 * <p>
	 * 當遊戲時間不在指定時段內時，刪除所有已生成的 NPC。
	 * 此方法覆寫自 {@link L1GameTimeAdapter}。
	 *
	 * @param time 當前遊戲時間
	 */
	@Override
	public void onMinuteChanged(L1GameTime time) {
		if (_time.getTimePeriod().includes(time)) {
			return;
		}
		synchronized (_mobs) {
			if (_mobs.isEmpty()) {
				return;
			}
			// 若不在指定時間內則刪除所有 NPC
			for (L1NpcInstance mob : _mobs) {
				mob.setCurrentHpDirect(0);
				mob.setDead(true);
				mob.setStatus(ActionCodes.ACTION_Die);
				mob.deleteMe();
			}
			_mobs.clear();
		}
	}

	/**
	 * 處理水晶洞穴的門關閉邏輯。
	 * <p>
	 * 當特定 NPC（ID 46143-46152）生成時，關閉對應的門（ID 5001-5010）。
	 *
	 * @param npcId 生成的 NPC ID
	 */
	public static void doCrystalCave(int npcId) {
		int[] npcId2 =
		{ 46143, 46144, 46145, 46146, 46147, 46148, 46149, 46150, 46151, 46152 };
		int[] doorId =
		{ 5001, 5002, 5003, 5004, 5005, 5006, 5007, 5008, 5009, 5010 };

		for (int i = 0; i < npcId2.length; i++) {
			if (npcId == npcId2[i]) {
				closeDoorInCrystalCave(doorId[i]);
			}
		}
	}

	/**
	 * 關閉水晶洞穴中指定 ID 的門。
	 *
	 * @param doorId 要關閉的門 ID
	 */
	private static void closeDoorInCrystalCave(int doorId) {
		for (L1Object object : L1World.getInstance().getObject()) {
			if (object instanceof L1DoorInstance) {
				L1DoorInstance door = (L1DoorInstance) object;
				if (door.getDoorId() == doorId) {
					door.close();
				}
			}
		}
	}
}
