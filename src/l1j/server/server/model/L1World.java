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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import l1j.server.Config;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1PetInstance;
import l1j.server.server.model.Instance.L1SummonInstance;
import l1j.server.server.model.map.L1Map;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.serverpackets.ServerBasePacket;
import l1j.server.server.types.Point;
import l1j.server.server.utils.collections.Lists;
import l1j.server.server.utils.collections.Maps;

/**
 * 遊戲世界管理器 - 管理遊戲世界中所有物件、玩家、NPC、寵物等
 *
 * <p>這是整個遊戲世界的中央管理類別,採用單例模式 (Singleton Pattern)。
 * 負責管理所有遊戲物件的儲存、檢索、可見性判斷等核心功能。
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li><b>物件管理:</b> 儲存和管理所有遊戲物件 (玩家、NPC、道具等)</li>
 *   <li><b>可見性管理:</b> 依地圖分割管理物件的可見性</li>
 *   <li><b>玩家管理:</b> 快速查詢線上玩家</li>
 *   <li><b>寵物/召喚獸管理:</b> 管理所有寵物和召喚獸</li>
 *   <li><b>血盟管理:</b> 管理所有血盟資訊</li>
 *   <li><b>戰爭管理:</b> 管理血盟戰爭狀態</li>
 *   <li><b>天氣系統:</b> 全域天氣狀態</li>
 *   <li><b>廣播功能:</b> 向所有玩家發送訊息或封包</li>
 * </ul>
 *
 * <h3>資料結構設計:</h3>
 * <ul>
 *   <li>{@code _allObjects} - 所有物件的全域索引 (以 ID 為鍵)</li>
 *   <li>{@code _allPlayers} - 所有線上玩家 (以名稱為鍵,快速查詢)</li>
 *   <li>{@code _allPets} - 所有寵物 (以 ID 為鍵)</li>
 *   <li>{@code _allSummons} - 所有召喚獸 (以 ID 為鍵)</li>
 *   <li>{@code _visibleObjects} - 依地圖 ID 分割的可見物件 (包含 L1Inventory,不含 L1ItemInstance)</li>
 *   <li>{@code _allWars} - 所有進行中的戰爭</li>
 *   <li>{@code _allClans} - 所有血盟 (線上/離線)</li>
 * </ul>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * // 取得世界實例
 * L1World world = L1World.getInstance();
 *
 * // 新增玩家到世界
 * world.storeObject(player);
 * world.addVisibleObject(player);
 *
 * // 查詢指定名稱的玩家
 * L1PcInstance target = world.getPlayer("PlayerName");
 *
 * // 取得可見範圍內的所有玩家
 * List&lt;L1PcInstance&gt; nearbyPlayers = world.getVisiblePlayer(player, 10);
 *
 * // 向所有玩家廣播訊息
 * world.broadcastServerMessage("系統維護公告");
 * </pre>
 *
 * <h3>可見性判斷:</h3>
 * <p>提供多種可見性判斷方法:
 * <ul>
 *   <li>{@code getVisibleObjects()} - 取得螢幕可見範圍內的物件</li>
 *   <li>{@code getVisiblePlayer()} - 取得可見範圍內的玩家</li>
 *   <li>{@code getVisibleLineObjects()} - 取得兩點間直線上的物件</li>
 *   <li>{@code getVisibleBoxObjects()} - 取得矩形範圍內的物件</li>
 *   <li>{@code getRecognizePlayer()} - 取得可辨識範圍內的玩家</li>
 * </ul>
 *
 * <h3>地圖 ID 限制:</h3>
 * <p>地圖 ID 上限為 {@code MAX_MAP_ID} (10000)。超過此 ID 的地圖物件不會被加入可見物件列表。
 *
 * <h3>執行緒安全:</h3>
 * <p>所有內部 Map 和 List 都使用執行緒安全的 Concurrent 版本,支援多執行緒並發存取。
 *
 * <h3>地面物品管理:</h3>
 * <p>地面物品使用特殊的 {@link L1GroundInventory} 管理,座標轉換為負數作為唯一鍵值:
 * <pre>
 * inventoryKey = ((x - 30000) * 10000 + (y - 30000)) * -1
 * </pre>
 *
 * @see L1Object
 * @see L1PcInstance
 * @see L1PetInstance
 * @see L1SummonInstance
 * @see L1Clan
 * @see L1War
 * @see L1GroundInventory
 */
public class L1World {
	private static Logger _log = Logger.getLogger(L1World.class.getName());

	/** 所有線上玩家 (以玩家名稱為鍵) */
	private final Map<String, L1PcInstance> _allPlayers;

	/** 所有寵物 (以物件 ID 為鍵) */
	private final Map<Integer, L1PetInstance> _allPets;

	/** 所有召喚獸 (以物件 ID 為鍵) */
	private final Map<Integer, L1SummonInstance> _allSummons;

	/** 所有物件的全域索引 (以物件 ID 為鍵, 包含 L1ItemInstance, 不含 L1Inventory) */
	private final Map<Integer, L1Object> _allObjects;

	/** 依地圖分割的可見物件陣列 (包含 L1Inventory, 不含 L1ItemInstance) */
	private final Map<Integer, L1Object>[] _visibleObjects;

	/** 所有進行中的血盟戰爭 */
	private final List<L1War> _allWars;

	/** 所有血盟 (以血盟名稱為鍵, 包含線上與離線) */
	private final Map<String, L1Clan> _allClans;

	/** 目前的天氣狀態 (預設為 4) */
	private int _weather = 4;

	/** 世界聊天是否啟用 */
	private boolean _worldChatEnabled = true;

	/** 是否正在處理貢獻度統計 */
	private boolean _processingContributionTotal = false;

	/** 地圖 ID 的最大值 */
	private static final int MAX_MAP_ID = 10000;

	/** 單例實例 */
	private static L1World _instance;

	@SuppressWarnings("unchecked")
	private L1World() {
		_allPlayers = Maps.newConcurrentMap(); // 全てのプレイヤー
		_allPets = Maps.newConcurrentMap(); // 全てのペット
		_allSummons = Maps.newConcurrentMap(); // 全てのサモンモンスター
		_allObjects = Maps.newConcurrentMap(); // 全てのオブジェクト(L1ItemInstance入り、L1Inventoryはなし)
		_visibleObjects = new Map[MAX_MAP_ID + 1]; // マップ毎のオブジェクト(L1Inventory入り、L1ItemInstanceはなし)
		_allWars = Lists.newConcurrentList(); // 全ての戦争
		_allClans = Maps.newConcurrentMap(); // 所有的血盟物件 (Online/Offline)

		for (int i = 0; i <= MAX_MAP_ID; i++) {
			_visibleObjects[i] = Maps.newConcurrentMap();
		}
	}

	/**
	 * 取得 L1World 的單例實例
	 * <p>執行緒安全的懶漢式單例模式
	 *
	 * @return L1World 實例
	 */
	public static L1World getInstance() {
		if (_instance == null) {
			_instance = new L1World();
		}
		return _instance;
	}

	/**
	 * 清除所有狀態並重新初始化世界
	 * <p><b>警告:</b> 此方法會清除所有遊戲物件、玩家、NPC 等資料
	 * <p>僅用於除錯、測試等特殊目的,<b>正常運作時不應呼叫此方法</b>
	 */
	public void clear() {
		_instance = new L1World();
	}

	/**
	 * 將物件儲存到世界中 (加入全域索引)
	 * <p>自動判斷物件類型並加入對應的集合:
	 * <ul>
	 *   <li>L1PcInstance → 加入 _allPlayers</li>
	 *   <li>L1PetInstance → 加入 _allPets</li>
	 *   <li>L1SummonInstance → 加入 _allSummons</li>
	 * </ul>
	 * <p><b>注意:</b> 此方法僅將物件加入全域索引,不會加入可見物件列表
	 * <p>需要配合 {@link #addVisibleObject(L1Object)} 使用
	 *
	 * @param object 要儲存的物件
	 * @throws NullPointerException 如果 object 為 null
	 */
	public void storeObject(L1Object object) {
		if (object == null) {
			throw new NullPointerException();
		}

		_allObjects.put(object.getId(), object);
		if (object instanceof L1PcInstance) {
			_allPlayers.put(((L1PcInstance) object).getName(), (L1PcInstance) object);
		}
		if (object instanceof L1PetInstance) {
			_allPets.put(object.getId(), (L1PetInstance) object);
		}
		if (object instanceof L1SummonInstance) {
			_allSummons.put(object.getId(), (L1SummonInstance) object);
		}
	}

	/**
	 * 從世界中移除物件 (從全域索引移除)
	 * <p>自動判斷物件類型並從對應的集合中移除:
	 * <ul>
	 *   <li>L1PcInstance → 從 _allPlayers 移除</li>
	 *   <li>L1PetInstance → 從 _allPets 移除</li>
	 *   <li>L1SummonInstance → 從 _allSummons 移除</li>
	 * </ul>
	 * <p><b>注意:</b> 此方法僅從全域索引移除,不會從可見物件列表移除
	 * <p>需要配合 {@link #removeVisibleObject(L1Object)} 使用
	 *
	 * @param object 要移除的物件
	 * @throws NullPointerException 如果 object 為 null
	 */
	public void removeObject(L1Object object) {
		if (object == null) {
			throw new NullPointerException();
		}

		_allObjects.remove(object.getId());
		if (object instanceof L1PcInstance) {
			_allPlayers.remove(((L1PcInstance) object).getName());
		}
		if (object instanceof L1PetInstance) {
			_allPets.remove(object.getId());
		}
		if (object instanceof L1SummonInstance) {
			_allSummons.remove(object.getId());
		}
	}

	/**
	 * 根據物件 ID 查詢物件
	 *
	 * @param oID 物件 ID
	 * @return 找到的物件,如果不存在則返回 null
	 */
	public L1Object findObject(int oID) {
		return _allObjects.get(oID);
	}

	/** _allObjects 的不可修改視圖快取 */
	private Collection<L1Object> _allValues;

	/**
	 * 取得所有物件的集合 (不可修改視圖)
	 * <p>返回的集合是不可修改的,任何修改操作都會拋出例外
	 *
	 * @return 所有物件的集合
	 */
	public Collection<L1Object> getObject() {
		Collection<L1Object> vs = _allValues;
		return (vs != null) ? vs : (_allValues = Collections.unmodifiableCollection(_allObjects.values()));
	}

	/**
	 * 取得指定座標的地面物品欄 (L1GroundInventory)
	 * <p>如果該座標尚未有 L1GroundInventory,則自動建立一個新的
	 * <p>座標轉換公式: inventoryKey = ((x - 30000) * 10000 + (y - 30000)) * -1
	 *
	 * @param x X 座標
	 * @param y Y 座標
	 * @param map 地圖 ID
	 * @return 該座標的 L1GroundInventory
	 */
	public L1GroundInventory getInventory(int x, int y, short map) {
		int inventoryKey = ((x - 30000) * 10000 + (y - 30000)) * -1; // xyのマイナス値をインベントリキーとして使用

		Object object = _visibleObjects[map].get(inventoryKey);
		if (object == null) {
			return new L1GroundInventory(inventoryKey, x, y, map);
		}
		else {
			return (L1GroundInventory) object;
		}
	}

	/**
	 * 取得指定位置的地面物品欄 (L1GroundInventory)
	 *
	 * @param loc 位置物件
	 * @return 該位置的 L1GroundInventory
	 */
	public L1GroundInventory getInventory(L1Location loc) {
		return getInventory(loc.getX(), loc.getY(), (short) loc.getMap().getId());
	}

	/**
	 * 將物件加入可見物件列表
	 * <p>物件會被加入到對應地圖 ID 的可見物件 Map 中
	 * <p>只有地圖 ID ≤ MAX_MAP_ID (10000) 的物件才會被加入
	 *
	 * @param object 要加入的物件
	 */
	public void addVisibleObject(L1Object object) {
		if (object.getMapId() <= MAX_MAP_ID) {
			_visibleObjects[object.getMapId()].put(object.getId(), object);
		}
	}

	/**
	 * 從可見物件列表中移除物件
	 * <p>物件會從對應地圖 ID 的可見物件 Map 中移除
	 *
	 * @param object 要移除的物件
	 */
	public void removeVisibleObject(L1Object object) {
		if (object.getMapId() <= MAX_MAP_ID) {
			_visibleObjects[object.getMapId()].remove(object.getId());
		}
	}

	/**
	 * 移動物件到新地圖的可見物件列表
	 * <p><b>重要:</b> 必須在呼叫 {@code object.setMap()} 設定新地圖 ID <b>之前</b>呼叫此方法
	 * <p>此方法會將物件從舊地圖的可見物件列表移除,並加入到新地圖的可見物件列表
	 *
	 * @param object 要移動的物件
	 * @param newMap 新地圖的 ID
	 */
	public void moveVisibleObject(L1Object object, int newMap) {
		if (object.getMapId() != newMap) {
			if (object.getMapId() <= MAX_MAP_ID) {
				_visibleObjects[object.getMapId()].remove(object.getId());
			}
			if (newMap <= MAX_MAP_ID) {
				_visibleObjects[newMap].put(object.getId(), object);
			}
		}
	}

	/**
	 * 建立兩點間直線路徑的座標 Map (使用 Bresenham 演算法)
	 * <p>參考: http://www2.starcat.ne.jp/~fussy/algo/algo1-1.htm
	 * <p>座標轉換為 key: (x << 16) + y
	 *
	 * @param src 起點座標
	 * @param target 終點座標
	 * @return 包含直線路徑上所有座標點的 Map
	 */
	private Map<Integer, Integer> createLineMap(Point src, Point target) {
		Map<Integer, Integer> lineMap = Maps.newConcurrentMap();

		/*
		 * http://www2.starcat.ne.jp/~fussy/algo/algo1-1.htmより
		 */
		int E;
		int x;
		int y;
		int key;
		int i;
		int x0 = src.getX();
		int y0 = src.getY();
		int x1 = target.getX();
		int y1 = target.getY();
		int sx = (x1 > x0) ? 1 : -1;
		int dx = (x1 > x0) ? x1 - x0 : x0 - x1;
		int sy = (y1 > y0) ? 1 : -1;
		int dy = (y1 > y0) ? y1 - y0 : y0 - y1;

		x = x0;
		y = y0;
		/* 傾きが1以下の場合 */
		if (dx >= dy) {
			E = -dx;
			for (i = 0; i <= dx; i++) {
				key = (x << 16) + y;
				lineMap.put(key, key);
				x += sx;
				E += 2 * dy;
				if (E >= 0) {
					y += sy;
					E -= 2 * dx;
				}
			}
			/* 傾きが1より大きい場合 */
		}
		else {
			E = -dy;
			for (i = 0; i <= dy; i++) {
				key = (x << 16) + y;
				lineMap.put(key, key);
				y += sy;
				E += 2 * dx;
				if (E >= 0) {
					x += sx;
					E -= 2 * dy;
				}
			}
		}

		return lineMap;
	}

	/**
	 * 取得兩個物件之間直線路徑上的所有物件
	 * <p>使用 Bresenham 直線演算法計算路徑
	 * <p>可用於檢查射線攻擊的目標、視線遮蔽等
	 *
	 * @param src 起點物件
	 * @param target 終點物件
	 * @return 直線路徑上的所有物件列表 (不包含 src 本身)
	 */
	public List<L1Object> getVisibleLineObjects(L1Object src, L1Object target) {
		Map<Integer, Integer> lineMap = createLineMap(src.getLocation(), target.getLocation());

		int map = target.getMapId();
		List<L1Object> result = Lists.newList();

		if (map <= MAX_MAP_ID) {
			for (L1Object element : _visibleObjects[map].values()) {
				if (element.equals(src)) {
					continue;
				}

				int key = (element.getX() << 16) + element.getY();
				if (lineMap.containsKey(key)) {
					result.add(element);
				}
			}
		}

		return result;
	}

	/**
	 * 取得矩形範圍內的所有物件 (考慮方向旋轉)
	 * <p>以物件為起點,依指定方向 (heading) 建立一個旋轉的矩形範圍
	 * <p>使用座標旋轉演算法將物件位置轉換為矩形邊界判斷
	 * <p>常用於範圍技能、扇形攻擊等判定
	 *
	 * @param object 中心物件
	 * @param heading 方向 (0-7,對應 8 個方向)
	 * @param width 寬度 (左右範圍)
	 * @param height 高度 (前方距離/射程)
	 * @return 矩形範圍內的所有物件列表 (不包含 object 本身)
	 */
	public List<L1Object> getVisibleBoxObjects(L1Object object, int heading, int width, int height) {
		int x = object.getX();
		int y = object.getY();
		int map = object.getMapId();
		L1Location location = object.getLocation();
		List<L1Object> result = Lists.newList();
		int headingRotate[] =
		{ 6, 7, 0, 1, 2, 3, 4, 5 };
		double cosSita = Math.cos(headingRotate[heading] * Math.PI / 4);
		double sinSita = Math.sin(headingRotate[heading] * Math.PI / 4);

		if (map <= MAX_MAP_ID) {
			for (L1Object element : _visibleObjects[map].values()) {
				if (element.equals(object)) {
					continue;
				}
				if (map != element.getMapId()) {
					continue;
				}

				// 同じ座標に重なっている場合は範囲内とする
				if (location.isSamePoint(element.getLocation())) {
					result.add(element);
					continue;
				}

				int distance = location.getTileLineDistance(element.getLocation());
				// 直線距離が高さ、幅どちらよりも大きい場合、計算するまでもなく範囲外
				if ((distance > height) && (distance > width)) {
					continue;
				}

				// objectの位置を原点とするための座標補正
				int x1 = element.getX() - x;
				int y1 = element.getY() - y;

				// Z軸回転させ角度を0度にする。
				int rotX = (int) Math.round(x1 * cosSita + y1 * sinSita);
				int rotY = (int) Math.round(-x1 * sinSita + y1 * cosSita);

				int xmin = 0;
				int xmax = height;
				int ymin = -width;
				int ymax = width;

				// 奥行きが射程とかみ合わないので直線距離で判定するように変更。
				// if (rotX > xmin && rotX <= xmax && rotY >= ymin && rotY <=
				// ymax) {
				if ((rotX > xmin) && (distance <= xmax) && (rotY >= ymin) && (rotY <= ymax)) {
					result.add(element);
				}
			}
		}

		return result;
	}

	/**
	 * 取得物件螢幕可見範圍內的所有物件
	 * <p>預設使用螢幕可見距離判斷 (radius = -1)
	 *
	 * @param object 中心物件
	 * @return 可見範圍內的所有物件列表
	 * @see #getVisibleObjects(L1Object, int)
	 */
	public List<L1Object> getVisibleObjects(L1Object object) {
		return getVisibleObjects(object, -1);
	}

	/**
	 * 取得物件指定範圍內的所有物件
	 *
	 * @param object 中心物件
	 * @param radius 半徑範圍
	 *               <ul>
	 *                 <li>-1: 螢幕可見範圍 (使用 Point.isInScreen 判斷)</li>
	 *                 <li>0: 相同座標 (使用 Point.isSamePoint 判斷)</li>
	 *                 <li>>0: 指定格數範圍 (使用 Point.getTileLineDistance 判斷)</li>
	 *               </ul>
	 * @return 範圍內的所有物件列表 (不包含 object 本身)
	 */
	public List<L1Object> getVisibleObjects(L1Object object, int radius) {
		L1Map map = object.getMap();
		Point pt = object.getLocation();
		List<L1Object> result = Lists.newList();
		if (map.getId() <= MAX_MAP_ID) {
			for (L1Object element : _visibleObjects[map.getId()].values()) {
				if (element.equals(object)) {
					continue;
				}
				if (map != element.getMap()) {
					continue;
				}

				if (radius == -1) {
					if (pt.isInScreen(element.getLocation())) {
						result.add(element);
					}
				}
				else if (radius == 0) {
					if (pt.isSamePoint(element.getLocation())) {
						result.add(element);
					}
				}
				else {
					if (pt.getTileLineDistance(element.getLocation()) <= radius) {
						result.add(element);
					}
				}
			}
		}

		return result;
	}

	/**
	 * 取得指定位置指定範圍內的所有物件
	 * <p>與 {@link #getVisibleObjects(L1Object, int)} 類似,但使用位置而非物件作為中心
	 *
	 * @param loc 中心位置
	 * @param radius 半徑範圍 (格數)
	 * @return 範圍內的所有物件列表
	 */
	public List<L1Object> getVisiblePoint(L1Location loc, int radius) {
		List<L1Object> result = Lists.newList();
		int mapId = loc.getMapId(); // ループ内で呼ぶと重いため

		if (mapId <= MAX_MAP_ID) {
			for (L1Object element : _visibleObjects[mapId].values()) {
				if (mapId != element.getMapId()) {
					continue;
				}

				if (loc.getTileLineDistance(element.getLocation()) <= radius) {
					result.add(element);
				}
			}
		}

		return result;
	}

	/**
	 * 取得物件螢幕可見範圍內的所有玩家
	 * <p>預設使用螢幕可見距離判斷 (radius = -1)
	 *
	 * @param object 中心物件
	 * @return 可見範圍內的所有玩家列表
	 * @see #getVisiblePlayer(L1Object, int)
	 */
	public List<L1PcInstance> getVisiblePlayer(L1Object object) {
		return getVisiblePlayer(object, -1);
	}

	/**
	 * 取得物件指定範圍內的所有玩家
	 * <p>只遍歷 _allPlayers 集合,效率比 getVisibleObjects 更高
	 *
	 * @param object 中心物件
	 * @param radius 半徑範圍
	 *               <ul>
	 *                 <li>-1: 螢幕可見範圍</li>
	 *                 <li>0: 相同座標</li>
	 *                 <li>>0: 指定格數範圍</li>
	 *               </ul>
	 * @return 範圍內的所有玩家列表 (不包含 object 本身若為玩家)
	 */
	public List<L1PcInstance> getVisiblePlayer(L1Object object, int radius) {
		int map = object.getMapId();
		Point pt = object.getLocation();
		List<L1PcInstance> result = Lists.newList();

		for (L1PcInstance element : _allPlayers.values()) {
			if (element.equals(object)) {
				continue;
			}

			if (map != element.getMapId()) {
				continue;
			}

			if (radius == -1) {
				if (pt.isInScreen(element.getLocation())) {
					result.add(element);
				}
			}
			else if (radius == 0) {
				if (pt.isSamePoint(element.getLocation())) {
					result.add(element);
				}
			}
			else {
				if (pt.getTileLineDistance(element.getLocation()) <= radius) {
					result.add(element);
				}
			}
		}
		return result;
	}

	/**
	 * 取得在 object 可見範圍內但不在 target 可見範圍內的所有玩家
	 * <p>用於判斷哪些玩家看得到 object 但看不到 target
	 * <p>常用於物件移動、消失等事件的封包發送判斷
	 *
	 * @param object 參考物件
	 * @param target 目標物件
	 * @return 符合條件的玩家列表
	 */
	public List<L1PcInstance> getVisiblePlayerExceptTargetSight(L1Object object, L1Object target) {
		int map = object.getMapId();
		Point objectPt = object.getLocation();
		Point targetPt = target.getLocation();
		List<L1PcInstance> result = Lists.newList();

		for (L1PcInstance element : _allPlayers.values()) {
			if (element.equals(object)) {
				continue;
			}

			if (map != element.getMapId()) {
				continue;
			}

			if (Config.PC_RECOGNIZE_RANGE == -1) {
				if (objectPt.isInScreen(element.getLocation())) {
					if (!targetPt.isInScreen(element.getLocation())) {
						result.add(element);
					}
				}
			}
			else {
				if (objectPt.getTileLineDistance(element.getLocation()) <= Config.PC_RECOGNIZE_RANGE) {
					if (targetPt.getTileLineDistance(element.getLocation()) > Config.PC_RECOGNIZE_RANGE) {
						result.add(element);
					}
				}
			}
		}
		return result;
	}

	/**
	 * 取得可辨識範圍內的玩家
	 * <p>使用設定檔中的 {@code Config.PC_RECOGNIZE_RANGE} 作為範圍
	 * <p>辨識範圍通常大於或等於螢幕可見範圍
	 *
	 * @param object 中心物件
	 * @return 可辨識範圍內的玩家列表
	 */
	public List<L1PcInstance> getRecognizePlayer(L1Object object) {
		return getVisiblePlayer(object, Config.PC_RECOGNIZE_RANGE);
	}

	/** _allPlayers 的不可修改視圖快取 */
	private Collection<L1PcInstance> _allPlayerValues;

	/**
	 * 取得所有線上玩家的集合 (不可修改視圖)
	 * <p>返回的集合是不可修改的,任何修改操作都會拋出例外
	 *
	 * @return 所有線上玩家的集合
	 */
	public Collection<L1PcInstance> getAllPlayers() {
		Collection<L1PcInstance> vs = _allPlayerValues;
		return (vs != null) ? vs : (_allPlayerValues = Collections.unmodifiableCollection(_allPlayers.values()));
	}

	/**
	 * 根據玩家名稱取得線上玩家
	 * <p>名稱比對不區分大小寫
	 * <p>先嘗試精確匹配,若失敗則使用 equalsIgnoreCase 逐一比對
	 *
	 * @param name 玩家名稱 (不區分大小寫)
	 * @return 找到的玩家,若不存在則返回 null
	 */
	public L1PcInstance getPlayer(String name) {
		if (_allPlayers.containsKey(name)) {
			return _allPlayers.get(name);
		}
		for (L1PcInstance each : getAllPlayers()) {
			if (each.getName().equalsIgnoreCase(name)) {
				return each;
			}
		}
		return null;
	}

	/** _allPets 的不可修改視圖快取 */
	private Collection<L1PetInstance> _allPetValues;

	/**
	 * 取得所有寵物的集合 (不可修改視圖)
	 *
	 * @return 所有寵物的集合
	 */
	public Collection<L1PetInstance> getAllPets() {
		Collection<L1PetInstance> vs = _allPetValues;
		return (vs != null) ? vs : (_allPetValues = Collections.unmodifiableCollection(_allPets.values()));
	}

	/** _allSummons 的不可修改視圖快取 */
	private Collection<L1SummonInstance> _allSummonValues;

	/**
	 * 取得所有召喚獸的集合 (不可修改視圖)
	 *
	 * @return 所有召喚獸的集合
	 */
	public Collection<L1SummonInstance> getAllSummons() {
		Collection<L1SummonInstance> vs = _allSummonValues;
		return (vs != null) ? vs : (_allSummonValues = Collections.unmodifiableCollection(_allSummons.values()));
	}

	/**
	 * 取得所有可見物件的 Map (全域索引)
	 * <p>包含 L1ItemInstance,不含 L1Inventory
	 *
	 * @return 所有可見物件的 Map (以物件 ID 為鍵)
	 */
	public final Map<Integer, L1Object> getAllVisibleObjects() {
		return _allObjects;
	}

	/**
	 * 取得依地圖分割的可見物件陣列
	 * <p>包含 L1Inventory,不含 L1ItemInstance
	 *
	 * @return 可見物件陣列 (索引為地圖 ID)
	 */
	public final Map<Integer, L1Object>[] getVisibleObjects() {
		return _visibleObjects;
	}

	/**
	 * 取得指定地圖的可見物件 Map
	 *
	 * @param mapId 地圖 ID
	 * @return 該地圖的可見物件 Map (以物件 ID 為鍵)
	 */
	public final Map<Integer, L1Object> getVisibleObjects(int mapId) {
		return _visibleObjects[mapId];
	}

	/**
	 * 取得物件所在的區域 (未實作)
	 * <p>此方法目前總是返回 null
	 *
	 * @param object 物件
	 * @return null
	 * @deprecated 此方法未實作
	 */
	public Object getRegion(Object object) {
		return null;
	}

	/**
	 * 新增血盟戰爭到列表
	 * <p>若戰爭已存在則不重複新增
	 *
	 * @param war 要新增的戰爭
	 */
	public void addWar(L1War war) {
		if (!_allWars.contains(war)) {
			_allWars.add(war);
		}
	}

	/**
	 * 從列表中移除血盟戰爭
	 *
	 * @param war 要移除的戰爭
	 */
	public void removeWar(L1War war) {
		if (_allWars.contains(war)) {
			_allWars.remove(war);
		}
	}

	/** _allWars 的不可修改視圖快取 */
	private List<L1War> _allWarList;

	/**
	 * 取得所有戰爭的列表 (不可修改視圖)
	 *
	 * @return 所有戰爭的列表
	 */
	public List<L1War> getWarList() {
		List<L1War> vs = _allWarList;
		return (vs != null) ? vs : (_allWarList = Collections.unmodifiableList(_allWars));
	}

	/**
	 * 儲存血盟到世界中
	 * <p>若血盟已存在則不重複儲存
	 *
	 * @param clan 要儲存的血盟
	 */
	public void storeClan(L1Clan clan) {
		L1Clan temp = getClan(clan.getClanName());
		if (temp == null) {
			_allClans.put(clan.getClanName(), clan);
		}
	}

	/**
	 * 從世界中移除血盟
	 *
	 * @param clan 要移除的血盟
	 */
	public void removeClan(L1Clan clan) {
		L1Clan temp = getClan(clan.getClanName());
		if (temp != null) {
			_allClans.remove(clan.getClanName());
		}
	}

	/**
	 * 根據血盟名稱取得血盟
	 *
	 * @param clan_name 血盟名稱
	 * @return 找到的血盟,若不存在則返回 null
	 */
	public L1Clan getClan(String clan_name) {
		return _allClans.get(clan_name);
	}

	/** _allClans 的不可修改視圖快取 */
	private Collection<L1Clan> _allClanValues;

	/**
	 * 取得所有血盟的集合 (不可修改視圖)
	 * <p>包含線上與離線的所有血盟
	 *
	 * @return 所有血盟的集合
	 */
	public Collection<L1Clan> getAllClans() {
		Collection<L1Clan> vs = _allClanValues;
		return (vs != null) ? vs : (_allClanValues = Collections.unmodifiableCollection(_allClans.values()));
	}

	/**
	 * 設定目前的天氣狀態
	 *
	 * @param weather 天氣代碼
	 */
	public void setWeather(int weather) {
		_weather = weather;
	}

	/**
	 * 取得目前的天氣狀態
	 *
	 * @return 天氣代碼
	 */
	public int getWeather() {
		return _weather;
	}

	/**
	 * 設定世界聊天是否啟用
	 *
	 * @param flag true 為啟用, false 為停用
	 */
	public void set_worldChatElabled(boolean flag) {
		_worldChatEnabled = flag;
	}

	/**
	 * 取得世界聊天是否啟用
	 *
	 * @return true 為啟用, false 為停用
	 */
	public boolean isWorldChatElabled() {
		return _worldChatEnabled;
	}

	/**
	 * 設定是否正在處理貢獻度統計
	 * <p>用於防止重複計算
	 *
	 * @param flag true 為處理中, false 為非處理中
	 */
	public void setProcessingContributionTotal(boolean flag) {
		_processingContributionTotal = flag;
	}

	/**
	 * 取得是否正在處理貢獻度統計
	 *
	 * @return true 為處理中, false 為非處理中
	 */
	public boolean isProcessingContributionTotal() {
		return _processingContributionTotal;
	}

	/**
	 * 向世界上所有線上玩家廣播封包
	 * <p>遍歷所有線上玩家並發送指定的封包
	 *
	 * @param packet 要廣播的封包
	 */
	public void broadcastPacketToAll(ServerBasePacket packet) {
		_log.finest("players to notify : " + getAllPlayers().size());
		for (L1PcInstance pc : getAllPlayers()) {
			pc.sendPackets(packet);
		}
	}

	/**
	 * 向世界上所有線上玩家廣播伺服器訊息
	 * <p>使用 S_SystemMessage 封包發送訊息
	 *
	 * @param message 要廣播的訊息內容
	 */
	public void broadcastServerMessage(String message) {
		broadcastPacketToAll(new S_SystemMessage(message));
	}
}