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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.logging.Logger;

import l1j.server.Config;
import l1j.server.L1Message;
import l1j.server.console.ConsoleProcess;
import l1j.server.server.datatables.CastleTable;
import l1j.server.server.datatables.CharacterTable;
import l1j.server.server.datatables.ChatLogTable;
import l1j.server.server.datatables.ClanTable;
import l1j.server.server.datatables.DoorTable;
import l1j.server.server.datatables.DropTable;
import l1j.server.server.datatables.DropItemTable;
import l1j.server.server.datatables.FurnitureItemTable;
import l1j.server.server.datatables.FurnitureSpawnTable;
import l1j.server.server.datatables.GetBackRestartTable;
import l1j.server.server.datatables.InnTable;
import l1j.server.server.datatables.IpTable;
import l1j.server.server.datatables.ItemTable;
import l1j.server.server.datatables.MagicDollTable;
import l1j.server.server.datatables.MailTable;
import l1j.server.server.datatables.MapsTable;
import l1j.server.server.datatables.MobGroupTable;
import l1j.server.server.datatables.NpcActionTable;
import l1j.server.server.datatables.NpcChatTable;
import l1j.server.server.datatables.NpcSpawnTable;
import l1j.server.server.datatables.NpcTable;
import l1j.server.server.datatables.NPCTalkDataTable;
import l1j.server.server.datatables.PetTable;
import l1j.server.server.datatables.PetTypeTable;
import l1j.server.server.datatables.PolyTable;
import l1j.server.server.datatables.RaceTicketTable;
import l1j.server.server.datatables.ResolventTable;
import l1j.server.server.datatables.ShopTable;
import l1j.server.server.datatables.SkillsTable;
import l1j.server.server.datatables.SpawnTable;
import l1j.server.server.datatables.SprTable;
import l1j.server.server.datatables.UBSpawnTable;
import l1j.server.server.datatables.WeaponSkillTable;
import l1j.server.server.model.Dungeon;
import l1j.server.server.model.ElementalStoneGenerator;
import l1j.server.server.model.Getback;
import l1j.server.server.model.L1BossCycle;
import l1j.server.server.model.L1CastleLocation;
import l1j.server.server.model.L1DeleteItemOnGround;
import l1j.server.server.model.L1NpcRegenerationTimer;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.game.L1BugBearRace;
import l1j.server.server.model.gametime.L1GameTimeClock;
import l1j.server.server.model.item.L1TreasureBox;
import l1j.server.server.model.map.L1WorldMap;
import l1j.server.server.model.npc.action.L1NpcDefaultAction;
import l1j.server.server.model.trap.L1WorldTraps;
import l1j.server.server.storage.mysql.MysqlAutoBackup;
import l1j.server.server.utils.MysqlAutoBackupTimer;
import l1j.server.server.utils.SystemUtil;

// Referenced classes of package l1j.server.server:
// ClientThread, Logins, RateTable, IdFactory,
// LoginController, GameTimeController, Announcements,
// MobTable, SpawnTable, SkillsTable, PolyTable,
// TeleportLocations, ShopTable, NPCTalkDataTable, NpcSpawnTable,
// IpTable, Shutdown, NpcTable, MobGroupTable, NpcShoutTable

/**
 * 遊戲伺服器核心類別 - 負責伺服器啟動、初始化及客戶端連線管理。
 *
 * <h3>主要功能</h3>
 * <p>
 * GameServer 是整個遊戲伺服器的核心控制類別，負責以下主要功能：
 * <ul>
 * <li><b>伺服器啟動</b>：建立 ServerSocket 監聽指定埠號，等待客戶端連線</li>
 * <li><b>系統初始化</b>：按順序初始化所有遊戲系統、資料表、時間控制器等</li>
 * <li><b>連線管理</b>：接受客戶端連線請求，為每個連線建立獨立的 ClientThread 處理</li>
 * <li><b>安全控制</b>：檢查 IP 黑名單，阻擋被封鎖的 IP 位址</li>
 * <li><b>關機處理</b>：提供優雅關機機制，確保玩家資料正確儲存</li>
 * </ul>
 * </p>
 *
 * <h3>設計模式</h3>
 * <p>
 * 此類別採用 <b>Singleton（單例）設計模式</b>，確保整個應用程式中只存在一個 GameServer 實例。
 * 透過 {@link #getInstance()} 方法取得唯一實例。
 * </p>
 *
 * <h3>執行緒模型</h3>
 * <p>
 * GameServer 繼承自 {@link Thread}，在獨立的執行緒中運行主迴圈，持續監聽並接受客戶端連線。
 * 每個客戶端連線會被分配到一個 {@link ClientThread} 處理，並提交至執行緒池 {@link GeneralThreadPool} 中執行。
 * </p>
 *
 * <h3>伺服器啟動流程</h3>
 * <ol>
 * <li><b>初始化階段</b>：呼叫 {@link #initialize()} 方法執行完整的系統初始化</li>
 * <li><b>啟動階段</b>：呼叫 {@link #start()} 方法啟動伺服器執行緒</li>
 * <li><b>運行階段</b>：{@link #run()} 方法中的無限迴圈持續接受客戶端連線</li>
 * <li><b>關機階段</b>：透過 Shutdown Hook 或手動呼叫 {@link #shutdown()} 方法關閉伺服器</li>
 * </ol>
 *
 * <h3>主要職責</h3>
 * <ul>
 * <li><b>網路監聽</b>：建立 ServerSocket 監聽指定埠號（預設 2000）</li>
 * <li><b>初始化遊戲系統</b>：
 *   <ul>
 *   <li>ID 生成器、世界地圖、登入控制器</li>
 *   <li>角色資料、帳號系統</li>
 *   <li>遊戲時間系統、各種時間控制器</li>
 *   <li>30+ 個資料表（NPC、物品、技能、商店、掉落等）</li>
 *   <li>世界物件管理、陷阱、地城系統</li>
 *   <li>公告系統、備份系統、GM 指令等</li>
 *   </ul>
 * </li>
 * <li><b>連線處理</b>：
 *   <ul>
 *   <li>接受客戶端 Socket 連線</li>
 *   <li>檢查 IP 是否在黑名單中</li>
 *   <li>建立 ClientThread 並提交至執行緒池</li>
 *   </ul>
 * </li>
 * <li><b>關機管理</b>：
 *   <ul>
 *   <li>註冊 Shutdown Hook 處理異常關機</li>
 *   <li>提供倒數計時關機功能</li>
 *   <li>確保所有玩家資料正確儲存</li>
 *   </ul>
 * </li>
 * </ul>
 *
 * @see ClientThread
 * @see LoginController
 * @see GeneralThreadPool
 * @see L1World
 * @see IdFactory
 * @see L1WorldMap
 * @see UbTimeController
 * @see WarTimeController
 * @see AuctionTimeController
 * @see HouseTaxTimeController
 * @see FishingTimeController
 * @see NpcChatTimeController
 * @see LightTimeController
 * @see HomeTownTimeController
 */
public class GameServer extends Thread {
	/**
	 * 日誌記錄器，用於記錄伺服器運行過程中的各種訊息、警告和錯誤。
	 */
	private static Logger _log = Logger.getLogger(GameServer.class.getName());

	/**
	 * YesNo 訊息計數器，用於追蹤伺服器發送給玩家的 YesNo 確認對話框總次數。
	 * 每次發送 YesNo 訊息時遞增，用於生成唯一的訊息 ID。
	 */
	private static int YesNoCount = 0;

	/**
	 * 伺服器啟動時間戳記（以秒為單位）。
	 * 記錄伺服器啟動的 Unix 時間戳，用於計算伺服器運行時間。
	 */
	public final int startTime = (int) (System.currentTimeMillis() / 1000);

	/**
	 * 伺服器 Socket，用於監聽客戶端連線請求。
	 * 綁定於設定檔中指定的埠號（預設 2000），持續接受客戶端連線。
	 */
	private ServerSocket _serverSocket;

	/**
	 * 伺服器監聽埠號。
	 * 從 {@link Config#GAME_SERVER_PORT} 載入，預設為 2000。
	 */
	private int _port;

	/**
	 * 登入控制器，負責管理玩家登入、線上人數限制等。
	 * 控制同時線上玩家數量上限，防止伺服器超載。
	 *
	 * @see LoginController
	 */
	private LoginController _loginController;

	/**
	 * 全域聊天等級限制。
	 * 玩家必須達到此等級才能使用全域聊天功能，防止低等級帳號濫發廣告。
	 * 從 {@link Config#GLOBAL_CHAT_LEVEL} 載入。
	 */
	private int chatlvl;

	/**
	 * 伺服器主執行緒的運行方法，負責監聽並接受客戶端連線。
	 *
	 * <h3>執行流程</h3>
	 * <p>
	 * 此方法在獨立的執行緒中運行，進入無限迴圈持續處理客戶端連線請求：
	 * </p>
	 * <ol>
	 * <li><b>顯示記憶體使用量</b>：在啟動時顯示當前 JVM 記憶體使用狀況</li>
	 * <li><b>顯示就緒訊息</b>：輸出「等待使用者連線」訊息，表示伺服器已準備就緒</li>
	 * <li><b>進入主迴圈</b>：無限迴圈等待並處理連線請求
	 *   <ul>
	 *   <li>呼叫 {@code ServerSocket.accept()} 阻塞等待客戶端連線</li>
	 *   <li>接受連線後，取得客戶端的 Socket 物件</li>
	 *   <li>記錄連線來源 IP 位址</li>
	 *   <li>執行 IP 黑名單檢查（{@link IpTable#isBannedIp(String)}）</li>
	 *   <li>若 IP 被封鎖，記錄警告並拒絕連線</li>
	 *   <li>若 IP 正常，建立 {@link ClientThread} 處理此連線</li>
	 *   <li>將 ClientThread 提交至 {@link GeneralThreadPool} 執行緒池執行</li>
	 *   </ul>
	 * </li>
	 * </ol>
	 *
	 * <h3>IP 黑名單機制</h3>
	 * <p>
	 * 伺服器會檢查連線來源 IP 是否在黑名單中（儲存於資料庫的 ip_table 資料表）。
	 * 被封鎖的 IP 位址無法建立連線，其連線請求會被直接丟棄。
	 * 這個機制用於防止惡意攻擊、作弊程式或被封鎖玩家的連線。
	 * </p>
	 *
	 * <h3>執行緒池處理</h3>
	 * <p>
	 * 每個客戶端連線都會被分配一個獨立的 {@link ClientThread} 執行緒處理。
	 * 這些執行緒不是直接建立，而是提交至 {@link GeneralThreadPool} 執行緒池管理，
	 * 有效控制執行緒數量，避免過多連線導致系統資源耗盡。
	 * </p>
	 *
	 * <h3>無限迴圈設計</h3>
	 * <p>
	 * 此方法採用 {@code while(true)} 無限迴圈設計，伺服器會持續運行直到：
	 * <ul>
	 * <li>手動呼叫 {@link #shutdown()} 方法關機</li>
	 * <li>觸發 Shutdown Hook（例如 Ctrl+C 或系統關機）</li>
	 * <li>發生嚴重錯誤導致 JVM 終止</li>
	 * </ul>
	 * IOException 被捕捉但不處理，這是為了避免偶發的網路錯誤導致伺服器停止服務。
	 * </p>
	 *
	 * @see ServerSocket#accept()
	 * @see ClientThread
	 * @see IpTable
	 * @see GeneralThreadPool
	 */
	@Override
	public void run() {
		_log.info(L1Message.memoryUse + SystemUtil.getUsedMemoryMB() + L1Message.memory);
		_log.info(L1Message.waitingforuser);
		while (true) {
			try {
				Socket socket = _serverSocket.accept();
				_log.info(L1Message.from + socket.getInetAddress()+ L1Message.attempt);
				String host = socket.getInetAddress().getHostAddress();
				if (IpTable.getInstance().isBannedIp(host)) {
					_log.info("banned IP(" + host + ")");
				} else {
					ClientThread client = new ClientThread(socket);
					GeneralThreadPool.getInstance().execute(client);
				}
			} catch (IOException ioexception) {
			}
		}
	}

	/**
	 * GameServer 的唯一實例（Singleton 模式）。
	 * 透過 {@link #getInstance()} 方法取得此實例，確保整個應用程式中只有一個 GameServer 物件。
	 */
	private static GameServer _instance;

	/**
	 * 私有建構子，實作 Singleton 設計模式。
	 *
	 * <p>
	 * 此建構子為 private，防止外部直接建立 GameServer 實例。
	 * 必須透過 {@link #getInstance()} 方法取得唯一的伺服器實例。
	 * </p>
	 *
	 * <p>
	 * 呼叫父類別 Thread 的建構子，設定執行緒名稱為 "GameServer"，
	 * 方便在執行緒監控和除錯時識別此執行緒。
	 * </p>
	 *
	 * @see #getInstance()
	 */
	private GameServer() {
		super("GameServer");
	}

	/**
	 * 取得 GameServer 的唯一實例（Singleton 模式）。
	 *
	 * <p>
	 * 此方法實作執行緒不安全的延遲初始化 Singleton 模式。
	 * 由於 GameServer 在應用程式啟動時就會建立，不會有多執行緒競爭問題，
	 * 因此不需要使用 synchronized 或雙重檢查鎖定。
	 * </p>
	 *
	 * @return GameServer 的唯一實例
	 */
	public static GameServer getInstance() {
		if (_instance == null) {
			_instance = new GameServer();
		}
		return _instance;
	}

	/**
	 * 初始化遊戲伺服器的所有系統與資料。
	 *
	 * <p>
	 * 這是 GameServer 最重要的方法，負責按照正確的順序初始化所有遊戲系統、資料表、時間控制器等。
	 * 初始化順序非常重要，因為某些系統依賴於其他系統的先行初始化。
	 * </p>
	 *
	 * <h3>初始化流程總覽</h3>
	 * <ol>
	 * <li>伺服器網路設定</li>
	 * <li>多國語系與顯示設定</li>
	 * <li>ID 生成器與世界地圖</li>
	 * <li>角色與帳號管理</li>
	 * <li>時間控制器</li>
	 * <li>資料表載入</li>
	 * <li>世界與遊戲邏輯</li>
	 * <li>輔助功能</li>
	 * <li>關機處理</li>
	 * <li>啟動控制台與伺服器執行緒</li>
	 * </ol>
	 *
	 * <h3>一、伺服器網路設定</h3>
	 * <ul>
	 * <li>從 {@link Config} 載入伺服器主機名稱（{@link Config#GAME_SERVER_HOST_NAME}）</li>
	 * <li>設定伺服器監聽埠號（{@link Config#GAME_SERVER_PORT}，預設 2000）</li>
	 * <li>建立 {@link ServerSocket}，綁定指定的網路介面與埠號</li>
	 * <li>若設定為 "*"，則監聽所有網路介面；否則綁定特定 IP</li>
	 * </ul>
	 *
	 * <h3>二、多國語系與顯示設定</h3>
	 * <ul>
	 * <li><b>初始化多國語系</b>：{@link L1Message#getInstance()} - 載入訊息資源，支援繁體中文、英文等</li>
	 * <li><b>顯示伺服器版本</b>：輸出 L1J-TW 版本資訊</li>
	 * <li><b>顯示遊戲倍率</b>：
	 *   <ul>
	 *   <li>經驗值倍率（{@link Config#RATE_XP}）</li>
	 *   <li>正義值倍率（{@link Config#RATE_LA}）</li>
	 *   <li>友好度倍率（{@link Config#RATE_KARMA}）</li>
	 *   <li>道具掉落倍率（{@link Config#RATE_DROP_ITEMS}）</li>
	 *   <li>金幣掉落倍率（{@link Config#RATE_DROP_ADENA}）</li>
	 *   <li>武器強化成功率（{@link Config#ENCHANT_CHANCE_WEAPON}）</li>
	 *   <li>防具強化成功率（{@link Config#ENCHANT_CHANCE_ARMOR}）</li>
	 *   </ul>
	 * </li>
	 * <li><b>顯示等級限制</b>：全域聊天等級限制（{@link Config#GLOBAL_CHAT_LEVEL}）</li>
	 * <li><b>顯示 PvP 設定</b>：Non-PvP 模式開關（{@link Config#ALT_NONPVP}）</li>
	 * <li><b>顯示人數上限</b>：最大線上人數（{@link Config#MAX_ONLINE_USERS}）</li>
	 * </ul>
	 *
	 * <h3>三、ID 生成器與世界地圖</h3>
	 * <ul>
	 * <li><b>{@link IdFactory#getInstance()}</b> - 初始化遊戲物件 ID 生成器
	 *   <p>為所有遊戲物件（角色、NPC、道具等）分配唯一 ID，從資料庫載入已使用的 ID</p>
	 * </li>
	 * <li><b>{@link L1WorldMap#getInstance()}</b> - 初始化世界地圖系統
	 *   <p>載入所有地圖檔案（.txt 格式），建立地圖快取，提供地形查詢、尋路等功能</p>
	 * </li>
	 * <li><b>{@link LoginController#getInstance()}</b> - 初始化登入控制器
	 *   <p>設定最大線上人數限制，管理玩家登入流程，防止伺服器超載</p>
	 * </li>
	 * </ul>
	 *
	 * <h3>四、角色與帳號管理</h3>
	 * <ul>
	 * <li><b>{@link CharacterTable#loadAllCharName()}</b> - 載入所有角色名稱
	 *   <p>從資料庫載入所有角色的名稱與 ID 對照表，用於快速查詢角色是否存在</p>
	 * </li>
	 * <li><b>{@link CharacterTable#clearOnlineStatus()}</b> - 清除角色上線狀態
	 *   <p>將資料庫中所有角色的上線狀態設為離線，修正異常關機時的錯誤狀態</p>
	 * </li>
	 * <li><b>{@link Account#InitialOnlineStatus()}</b> - 初始化帳號上線狀態
	 *   <p>清除所有帳號的上線標記，確保啟動時所有帳號都是離線狀態</p>
	 * </li>
	 * </ul>
	 *
	 * <h3>五、時間控制器</h3>
	 * <p>以下時間控制器負責管理各種週期性事件，均在獨立執行緒中運行：</p>
	 * <ul>
	 * <li><b>{@link L1GameTimeClock#init()}</b> - 遊戲時間系統
	 *   <p>初始化遊戲內時間（加速的時間系統），控制日夜循環</p>
	 * </li>
	 * <li><b>{@link UbTimeController}</b> - 無限大戰時間控制器
	 *   <p>管理無限大戰（UB, Underground Battle）的開啟與關閉時間</p>
	 * </li>
	 * <li><b>{@link WarTimeController}</b> - 攻城戰時間控制器
	 *   <p>管理各城堡的攻城戰開始與結束時間，處理攻城戰相關邏輯</p>
	 * </li>
	 * <li><b>{@link ElementalStoneGenerator}</b> - 精靈石產生器
	 *   <p>若設定 {@link Config#ELEMENTAL_STONE_AMOUNT} > 0，則定時在地圖上產生精靈石</p>
	 * </li>
	 * <li><b>{@link HomeTownTimeController}</b> - 城鎮時間控制器
	 *   <p>管理城鎮相關的時間事件</p>
	 * </li>
	 * <li><b>{@link AuctionTimeController}</b> - 拍賣時間控制器
	 *   <p>管理盟屋拍賣的開標與結標時間</p>
	 * </li>
	 * <li><b>{@link HouseTaxTimeController}</b> - 盟屋稅金控制器
	 *   <p>定時收取盟屋稅金，處理欠稅導致的盟屋沒收</p>
	 * </li>
	 * <li><b>{@link FishingTimeController}</b> - 釣魚時間控制器
	 *   <p>管理釣魚活動的開始與結束時間</p>
	 * </li>
	 * <li><b>{@link NpcChatTimeController}</b> - NPC 聊天控制器
	 *   <p>定時讓 NPC 發送環境對話，增加遊戲氛圍</p>
	 * </li>
	 * <li><b>{@link LightTimeController}</b> - 光線控制器
	 *   <p>根據遊戲時間控制白天/夜晚的光線明暗變化</p>
	 * </li>
	 * </ul>
	 *
	 * <h3>六、資料表載入</h3>
	 * <p>以下資料表從資料庫載入遊戲資料，建立記憶體快取：</p>
	 *
	 * <h4>6.1 公告與備份系統</h4>
	 * <ul>
	 * <li><b>{@link Announcements}</b> - 系統公告（固定公告）</li>
	 * <li><b>{@link AnnouncementsCycle}</b> - 循環公告（定時輪播）</li>
	 * <li><b>{@link MysqlAutoBackup}</b> - MySQL 自動備份程序</li>
	 * <li><b>{@link MysqlAutoBackupTimer}</b> - 啟動備份計時器</li>
	 * </ul>
	 *
	 * <h4>6.2 核心遊戲資料表</h4>
	 * <ul>
	 * <li><b>{@link NpcTable}</b> - NPC 模板資料
	 *   <p>載入所有 NPC 的基礎屬性（血量、攻擊力、防禦力、AI 等）</p>
	 * </li>
	 * <li><b>{@link L1NpcDefaultAction}</b> - NPC 預設行為
	 *   <p>定義 NPC 的預設互動行為</p>
	 * </li>
	 * <li><b>{@link DoorTable}</b> - 門資料表
	 *   <p>載入所有門的位置、狀態、開關條件</p>
	 * </li>
	 * <li><b>{@link SpawnTable}</b> - 刷怪點資料表
	 *   <p>定義所有 NPC/怪物的刷新位置、數量、重生時間</p>
	 * </li>
	 * <li><b>{@link MobGroupTable}</b> - 怪物群組資料表
	 *   <p>定義群組刷怪（同時刷新多種怪物）</p>
	 * </li>
	 * <li><b>{@link ItemTable}</b> - 道具資料表
	 *   <p>載入所有道具的屬性（攻擊力、防禦力、重量、價格等）</p>
	 * </li>
	 * <li><b>{@link SkillsTable}</b> - 技能資料表
	 *   <p>載入所有技能的效果、消耗、施放時間、冷卻時間</p>
	 * </li>
	 * <li><b>{@link PolyTable}</b> - 變身資料表
	 *   <p>定義變身的外觀、屬性加成</p>
	 * </li>
	 * </ul>
	 *
	 * <h4>6.3 商店與掉落系統</h4>
	 * <ul>
	 * <li><b>{@link DropTable}</b> - 掉落資料表
	 *   <p>定義怪物死亡時的道具掉落機率與數量</p>
	 * </li>
	 * <li><b>{@link DropItemTable}</b> - 掉落道具資料表
	 *   <p>補充掉落系統的詳細設定</p>
	 * </li>
	 * <li><b>{@link ShopTable}</b> - 商店資料表
	 *   <p>載入所有 NPC 商店的販賣/收購商品與價格</p>
	 * </li>
	 * <li><b>{@link NPCTalkDataTable}</b> - NPC 對話資料表
	 *   <p>載入所有 NPC 的對話腳本（HTML 格式）</p>
	 * </li>
	 * </ul>
	 *
	 * <h4>6.4 世界物件與地圖系統</h4>
	 * <ul>
	 * <li><b>{@link L1World}</b> - 世界物件管理器
	 *   <p>管理所有遊戲世界中的物件（玩家、NPC、道具等），提供物件查詢與廣播功能</p>
	 * </li>
	 * <li><b>{@link L1WorldTraps}</b> - 陷阱系統
	 *   <p>載入地圖上的陷阱位置與效果</p>
	 * </li>
	 * <li><b>{@link Dungeon}</b> - 地城系統
	 *   <p>管理副本地城的建立、進入、離開</p>
	 * </li>
	 * <li><b>{@link L1DeleteItemOnGround}</b> - 地面道具清理
	 *   <p>定時清理地圖上的掉落道具，防止道具堆積影響效能</p>
	 * </li>
	 * <li><b>{@link NpcSpawnTable}</b> - NPC 刷新資料表
	 *   <p>執行 NPC 刷新，將 NPC 加入遊戲世界</p>
	 * </li>
	 * <li><b>{@link MapsTable}</b> - 地圖資料表
	 *   <p>載入地圖的特殊屬性（安全區、傳送限制等）</p>
	 * </li>
	 * <li><b>{@link UBSpawnTable}</b> - 無限大戰刷怪表
	 *   <p>載入無限大戰專用的怪物刷新設定</p>
	 * </li>
	 * </ul>
	 *
	 * <h4>6.5 玩家相關系統</h4>
	 * <ul>
	 * <li><b>{@link PetTable}</b> - 寵物資料表
	 *   <p>載入所有玩家擁有的寵物資料（名稱、等級、忠誠度）</p>
	 * </li>
	 * <li><b>{@link PetTypeTable}</b> - 寵物種類資料表
	 *   <p>定義寵物的成長率、進化條件</p>
	 * </li>
	 * <li><b>{@link ClanTable}</b> - 血盟資料表
	 *   <p>載入所有血盟的資料（名稱、成員、聯盟關係）</p>
	 * </li>
	 * <li><b>{@link CastleTable}</b> - 城堡資料表
	 *   <p>載入城堡的擁有者、稅率、金庫等資料</p>
	 * </li>
	 * <li><b>{@link L1CastleLocation#setCastleTaxRate()}</b> - 設定城堡稅率
	 *   <p><b>重要</b>：必須在 CastleTable 初始化之後執行</p>
	 * </li>
	 * <li><b>{@link IpTable}</b> - IP 黑名單資料表
	 *   <p>載入被封鎖的 IP 位址列表</p>
	 * </li>
	 * <li><b>{@link MailTable}</b> - 郵件資料表
	 *   <p>載入玩家的郵件系統資料</p>
	 * </li>
	 * <li><b>{@link InnTable}</b> - 旅館資料表
	 *   <p>管理旅館系統</p>
	 * </li>
	 * </ul>
	 *
	 * <h4>6.6 遊戲功能系統</h4>
	 * <ul>
	 * <li><b>{@link GetBackRestartTable}</b> - 重生點資料表
	 *   <p>定義角色死亡/重新開始時的重生位置</p>
	 * </li>
	 * <li><b>{@link Getback#loadGetBack()}</b> - 載入返回點資料</li>
	 * <li><b>{@link ChatLogTable}</b> - 聊天記錄表
	 *   <p>若啟用聊天記錄功能，初始化聊天記錄系統</p>
	 * </li>
	 * <li><b>{@link WeaponSkillTable}</b> - 武器技能表
	 *   <p>定義武器特殊技能（如龍騎士的武器技）</p>
	 * </li>
	 * <li><b>{@link NpcActionTable}</b> - NPC 行為表
	 *   <p>載入 NPC 的特殊行為腳本</p>
	 * </li>
	 * <li><b>{@link GMCommandsConfig}</b> - GM 指令設定
	 *   <p>載入 GM 指令的權限與設定（從 gmcommands.xml）</p>
	 * </li>
	 * <li><b>{@link L1BossCycle}</b> - Boss 循環系統
	 *   <p>管理世界 Boss 的刷新週期</p>
	 * </li>
	 * <li><b>{@link L1TreasureBox}</b> - 寶箱系統
	 *   <p>載入寶箱的獎勵內容</p>
	 * </li>
	 * <li><b>{@link SprTable}</b> - 圖檔資料表
	 *   <p>載入遊戲圖檔的索引資料</p>
	 * </li>
	 * <li><b>{@link ResolventTable}</b> - 分解資料表
	 *   <p>定義道具分解系統的規則</p>
	 * </li>
	 * <li><b>{@link FurnitureSpawnTable}</b> - 家具刷新表
	 *   <p>載入盟屋家具的放置位置</p>
	 * </li>
	 * <li><b>{@link FurnitureItemTable}</b> - 家具道具表</li>
	 * <li><b>{@link NpcChatTable}</b> - NPC 聊天表
	 *   <p>載入 NPC 的環境對話內容</p>
	 * </li>
	 * <li><b>{@link RaceTicketTable}</b> - 賽跑彩券表
	 *   <p>載入賽跑（兔熊賽跑）的彩券資料</p>
	 * </li>
	 * <li><b>{@link L1BugBearRace}</b> - 兔熊賽跑系統</li>
	 * <li><b>{@link MagicDollTable}</b> - 魔法娃娃表
	 *   <p>載入魔法娃娃系統的資料</p>
	 * </li>
	 * </ul>
	 *
	 * <h4>6.7 執行緒池與定時器</h4>
	 * <ul>
	 * <li><b>{@link GeneralThreadPool}</b> - 通用執行緒池
	 *   <p>管理所有非網路 IO 的執行緒任務，提供固定/快取執行緒池</p>
	 * </li>
	 * <li><b>{@link L1NpcRegenerationTimer}</b> - NPC 重生計時器
	 *   <p>管理 NPC 死亡後的重生倒數</p>
	 * </li>
	 * </ul>
	 *
	 * <h3>七、關機處理</h3>
	 * <ul>
	 * <li><b>註冊 Shutdown Hook</b>：{@link Runtime#addShutdownHook(Thread)}
	 *   <p>註冊 {@link Shutdown} 執行緒，確保異常關機時也能正確儲存資料</p>
	 * </li>
	 * </ul>
	 *
	 * <h3>八、啟動控制台與伺服器執行緒</h3>
	 * <ul>
	 * <li><b>啟動 CMD 互動指令</b>：{@link ConsoleProcess}
	 *   <p>若 {@link Config} 中啟用控制台，啟動互動式命令列介面，允許執行時管理伺服器</p>
	 * </li>
	 * <li><b>啟動伺服器主執行緒</b>：{@link #start()}
	 *   <p>呼叫 {@link #run()} 方法，開始監聽客戶端連線</p>
	 * </li>
	 * </ul>
	 *
	 * <h3>初始化順序的重要性</h3>
	 * <p>
	 * 資料表的載入順序非常重要，因為某些資料表依賴其他資料表的資料：
	 * </p>
	 * <ul>
	 * <li>{@link IdFactory} 必須最先初始化，因為後續所有物件都需要 ID</li>
	 * <li>{@link L1WorldMap} 必須在刷怪前初始化，確保地圖資料可用</li>
	 * <li>{@link NpcTable} 必須在 {@link SpawnTable} 前初始化，刷怪時需要查詢 NPC 模板</li>
	 * <li>{@link ItemTable} 必須在 {@link DropTable} 前初始化，掉落系統需要驗證道具是否存在</li>
	 * <li>{@link CastleTable} 必須在 {@link L1CastleLocation#setCastleTaxRate()} 前初始化</li>
	 * <li>{@link L1World} 必須在 {@link NpcSpawnTable} 前初始化，刷新的 NPC 需要加入世界</li>
	 * </ul>
	 *
	 * <h3>錯誤處理</h3>
	 * <p>
	 * 若 {@link NpcTable} 初始化失敗（{@link NpcTable#isInitialized()} 返回 false），
	 * 則拋出 Exception 中斷伺服器啟動，因為缺少 NPC 資料會導致遊戲無法正常運行。
	 * </p>
	 *
	 * @throws Exception 當初始化過程中發生錯誤時拋出，例如：
	 *         <ul>
	 *         <li>資料庫連線失敗</li>
	 *         <li>埠號已被占用</li>
	 *         <li>必要的資料表載入失敗</li>
	 *         <li>地圖檔案損壞或遺失</li>
	 *         </ul>
	 *
	 * @see Config
	 * @see IdFactory
	 * @see L1WorldMap
	 * @see LoginController
	 * @see CharacterTable
	 * @see Account
	 * @see L1GameTimeClock
	 * @see NpcTable
	 * @see ItemTable
	 * @see SkillsTable
	 * @see DropTable
	 * @see ShopTable
	 * @see L1World
	 * @see CastleTable
	 * @see GeneralThreadPool
	 * @see Shutdown
	 * @see ConsoleProcess
	 */
	public void initialize() throws Exception {
		String s = Config.GAME_SERVER_HOST_NAME;
		double rateXp = Config.RATE_XP;
		double LA = Config.RATE_LA;
		double rateKarma = Config.RATE_KARMA;
		double rateDropItems = Config.RATE_DROP_ITEMS;
		double rateDropAdena = Config.RATE_DROP_ADENA;

		// Locale 多國語系
		L1Message.getInstance();

		chatlvl = Config.GLOBAL_CHAT_LEVEL;
		_port = Config.GAME_SERVER_PORT;
		if (!"*".equals(s)) {
			InetAddress inetaddress = InetAddress.getByName(s);
			inetaddress.getHostAddress();
			_serverSocket = new ServerSocket(_port, 50, inetaddress);
			_log.info(L1Message.setporton + _port);
		} else {
			_serverSocket = new ServerSocket(_port);
			_log.info(L1Message.setporton + _port);
		}

		_log.info("┌───────────────────────────────┐");
		_log.info("│     " + L1Message.ver + "\t" + "\t" + "│");
		_log.info("└───────────────────────────────┘");

		_log.info(L1Message.settingslist);
		_log.info("┌" + L1Message.exp + ": " + (rateXp) + L1Message.x);
		_log.info("├" + L1Message.justice + ": " + (LA) + L1Message.x);
		_log.info("├" + L1Message.karma + ": " + (rateKarma) + L1Message.x);
		_log.info("├" + L1Message.dropitems + ": " + (rateDropItems)+ L1Message.x);
		_log.info("├" + L1Message.dropadena + ": "+ (rateDropAdena) + L1Message.x);
		_log.info("├"+ L1Message.enchantweapon + ": "+ (Config.ENCHANT_CHANCE_WEAPON) + "%");
		_log.info("├"+ L1Message.enchantarmor + ": " + (Config.ENCHANT_CHANCE_ARMOR)+ "%");
		_log.info("├" + L1Message.chatlevel + ": " + (chatlvl)+ L1Message.level);

		if (Config.ALT_NONPVP) { // Non-PvP設定
			_log.info("└" + L1Message.nonpvpNo);
		} else {
			_log.info("└" + L1Message.nonpvpYes);
		}

		int maxOnlineUsers = Config.MAX_ONLINE_USERS;
		_log.info(L1Message.maxplayer + (maxOnlineUsers) + L1Message.player);

		_log.info("┌───────────────────────────────┐");
		_log.info("│     " + L1Message.ver + "\t" + "\t" + "│");
		_log.info("└───────────────────────────────┘");

		IdFactory.getInstance();
		L1WorldMap.getInstance();
		_loginController = LoginController.getInstance();
		_loginController.setMaxAllowedOnlinePlayers(maxOnlineUsers);

		// 讀取所有角色名稱
		CharacterTable.getInstance().loadAllCharName();

		// 初始化角色的上線狀態
		CharacterTable.clearOnlineStatus();

		// 初始化遊戲時間
		L1GameTimeClock.init();

		// 初始化無限大戰
		UbTimeController ubTimeContoroller = UbTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(ubTimeContoroller);
		
		// 初始化攻城
		WarTimeController warTimeController = WarTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(warTimeController);
		
		// 設定精靈石的產生
		if (Config.ELEMENTAL_STONE_AMOUNT > 0) {
			ElementalStoneGenerator elementalStoneGenerator = ElementalStoneGenerator.getInstance();
			GeneralThreadPool.getInstance().execute(elementalStoneGenerator);
		}

		// 初始化 HomeTown 時間
		HomeTownTimeController.getInstance();

		// 初始化盟屋拍賣
		AuctionTimeController auctionTimeController = AuctionTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(auctionTimeController);

		// 初始化盟屋的稅金
		HouseTaxTimeController houseTaxTimeController = HouseTaxTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(houseTaxTimeController);

		// 初始化釣魚
		FishingTimeController fishingTimeController = FishingTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(fishingTimeController);

		// 初始化 NPC 聊天
		NpcChatTimeController npcChatTimeController = NpcChatTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(npcChatTimeController);

		// 初始化 Light
		LightTimeController lightTimeController = LightTimeController.getInstance();
		GeneralThreadPool.getInstance().execute(lightTimeController);

		// 初始化遊戲公告
		Announcements.getInstance();
		
		// 初始化遊戲循環公告
	    AnnouncementsCycle.getInstance();

		// 初始化MySQL自動備份程序
		MysqlAutoBackup.getInstance();

		// 開始 MySQL自動備份程序 計時器
		MysqlAutoBackupTimer.TimerStart();
		
		// 初始化帳號使用狀態
		Account.InitialOnlineStatus();

		NpcTable.getInstance();
		L1DeleteItemOnGround deleteitem = new L1DeleteItemOnGround();
		deleteitem.initialize();

		if (!NpcTable.getInstance().isInitialized()) {
			throw new Exception("Could not initialize the npc table");
		}
		L1NpcDefaultAction.getInstance();
		DoorTable.initialize();
		SpawnTable.getInstance();
		MobGroupTable.getInstance();
		SkillsTable.getInstance();
		PolyTable.getInstance();
		ItemTable.getInstance();
		DropTable.getInstance();
		DropItemTable.getInstance();
		ShopTable.getInstance();
		NPCTalkDataTable.getInstance();
		L1World.getInstance();
		L1WorldTraps.getInstance();
		Dungeon.getInstance();
		NpcSpawnTable.getInstance();
		IpTable.getInstance();
		MapsTable.getInstance();
		UBSpawnTable.getInstance();
		PetTable.getInstance();
		ClanTable.getInstance();
		CastleTable.getInstance();
		L1CastleLocation.setCastleTaxRate(); // 必須在 CastleTable 初始化之後
		GetBackRestartTable.getInstance();
		GeneralThreadPool.getInstance();
		L1NpcRegenerationTimer.getInstance();
		ChatLogTable.getInstance();
		WeaponSkillTable.getInstance();
		NpcActionTable.load();
		GMCommandsConfig.load();
		Getback.loadGetBack();
		PetTypeTable.load();
		L1BossCycle.load();
		L1TreasureBox.load();
		SprTable.getInstance();
		ResolventTable.getInstance();
		FurnitureSpawnTable.getInstance();
		NpcChatTable.getInstance();
		MailTable.getInstance();
		RaceTicketTable.getInstance();
		L1BugBearRace.getInstance();
		InnTable.getInstance();
		MagicDollTable.getInstance();
		FurnitureItemTable.getInstance();

		_log.info(L1Message.initialfinished);
		Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
		
		// cmd互動指令
		Thread cp = new ConsoleProcess();
		cp.start();
		
		this.start();
	}

	/**
	 * 踢掉世界地圖中所有的玩家並儲存資料。
	 *
	 * <p>
	 * 此方法在伺服器關機時呼叫，確保所有線上玩家正確登出並儲存角色資料。
	 * 這是伺服器優雅關機流程的重要部分，可防止玩家資料遺失或損壞。
	 * </p>
	 *
	 * <h3>執行流程</h3>
	 * <ol>
	 * <li><b>第一階段：中斷連線</b>
	 *   <ul>
	 *   <li>從 {@link L1World} 取得所有線上玩家的集合</li>
	 *   <li>遍歷每個玩家，呼叫 {@code NetConnection.setActiveChar(null)} 解除角色關聯</li>
	 *   <li>呼叫 {@code NetConnection.kick()} 強制中斷客戶端連線</li>
	 *   </ul>
	 * </li>
	 * <li><b>第二階段：儲存資料並清理</b>
	 *   <ul>
	 *   <li>再次遍歷所有玩家</li>
	 *   <li>呼叫 {@link ClientThread#quitGame(L1PcInstance)} 執行登出流程（儲存角色資料）</li>
	 *   <li>從 {@link L1World} 中移除玩家物件</li>
	 *   <li>載入玩家的 {@link Account} 並設定為離線狀態</li>
	 *   </ul>
	 * </li>
	 * </ol>
	 *
	 * <h3>兩階段處理的必要性</h3>
	 * <p>
	 * 此方法採用兩階段處理，先中斷連線再儲存資料，是為了：
	 * <ul>
	 * <li>防止玩家在儲存資料過程中繼續執行遊戲動作</li>
	 * <li>避免客戶端嘗試重新連線或發送封包干擾關機流程</li>
	 * <li>確保資料儲存的完整性與一致性</li>
	 * </ul>
	 * </p>
	 *
	 * <h3>資料儲存內容</h3>
	 * <p>
	 * {@link ClientThread#quitGame(L1PcInstance)} 會儲存以下資料：
	 * <ul>
	 * <li>角色屬性（等級、經驗值、血量、魔力等）</li>
	 * <li>角色位置與狀態</li>
	 * <li>背包道具</li>
	 * <li>倉庫道具</li>
	 * <li>技能與魔法狀態</li>
	 * <li>任務進度</li>
	 * <li>血盟資訊</li>
	 * </ul>
	 * </p>
	 *
	 * @see L1World#getAllPlayers()
	 * @see ClientThread#quitGame(L1PcInstance)
	 * @see Account#online(Account, boolean)
	 * @see #shutdown()
	 * @see #shutdownWithCountdown(int)
	 */
	public void disconnectAllCharacters() {
		Collection<L1PcInstance> players = L1World.getInstance()
				.getAllPlayers();
		for (L1PcInstance pc : players) {
			pc.getNetConnection().setActiveChar(null);
			pc.getNetConnection().kick();
		}
		// 踢除所有在線上的玩家
		for (L1PcInstance pc : players) {
			ClientThread.quitGame(pc);
			L1World.getInstance().removeObject(pc);
			Account account = Account.load(pc.getAccountName());
			Account.online(account, false);
		}
	}

	/**
	 * 伺服器關機執行緒 - 負責倒數計時並執行關機流程。
	 *
	 * <p>
	 * 此內部類別實作伺服器的優雅關機機制，在關機前提供倒數計時，
	 * 讓玩家有充足時間移動到安全區域並登出，避免資料遺失。
	 * </p>
	 *
	 * <h3>倒數計時機制</h3>
	 * <ul>
	 * <li>關機前會廣播系統訊息通知所有線上玩家</li>
	 * <li>每分鐘廣播一次剩餘時間（當剩餘時間 > 30 秒時）</li>
	 * <li>剩餘 30 秒內，每秒廣播一次倒數</li>
	 * <li>倒數結束後執行 {@link #shutdown()} 方法</li>
	 * </ul>
	 *
	 * <h3>中斷處理</h3>
	 * <p>
	 * 此執行緒可透過 {@link #abortShutdown()} 方法中斷，
	 * 中斷後會廣播「已取消伺服器關機」訊息，伺服器繼續正常運作。
	 * </p>
	 *
	 * @see #shutdownWithCountdown(int)
	 * @see #abortShutdown()
	 * @see #shutdown()
	 */
	private class ServerShutdownThread extends Thread {
		/**
		 * 關機倒數秒數。
		 * 從此秒數開始倒數至 0，然後執行關機。
		 */
		private final int _secondsCount;

		/**
		 * 建構伺服器關機執行緒。
		 *
		 * @param secondsCount 關機倒數秒數，必須 > 0
		 */
		public ServerShutdownThread(int secondsCount) {
			_secondsCount = secondsCount;
		}

		/**
		 * 執行關機倒數與廣播通知流程。
		 *
		 * <h3>執行流程</h3>
		 * <ol>
		 * <li>取得 {@link L1World} 實例用於廣播訊息</li>
		 * <li>初始廣播：通知玩家伺服器即將關閉</li>
		 * <li>進入倒數迴圈：
		 *   <ul>
		 *   <li>若剩餘時間 <= 30 秒：每秒廣播倒數</li>
		 *   <li>若剩餘時間 > 30 秒且為 60 的倍數：每分鐘廣播一次</li>
		 *   <li>等待 1 秒（{@code Thread.sleep(1000)}）</li>
		 *   <li>秒數遞減</li>
		 *   </ul>
		 * </li>
		 * <li>倒數結束後呼叫 {@link #shutdown()} 執行關機</li>
		 * </ol>
		 *
		 * <h3>中斷處理</h3>
		 * <p>
		 * 若執行緒被中斷（透過 {@link #abortShutdown()}），
		 * 捕捉 {@link InterruptedException}，廣播取消訊息後結束執行緒。
		 * </p>
		 */
		@Override
		public void run() {
			L1World world = L1World.getInstance();
			try {
				int secondsCount = _secondsCount;
				world.broadcastServerMessage("伺服器即將關閉。");
				world.broadcastServerMessage("請玩家移動到安全區域先行登出");
				while (0 < secondsCount) {
					if (secondsCount <= 30) {
						world.broadcastServerMessage("伺服器將在" + secondsCount
								+ "秒後關閉，請玩家移動到安全區域先行登出。");
					} else {
						if (secondsCount % 60 == 0) {
							world.broadcastServerMessage("伺服器將在" + secondsCount
									/ 60 + "分鐘後關閉。");
						}
					}
					Thread.sleep(1000);
					secondsCount--;
				}
				shutdown();
			} catch (InterruptedException e) {
				world.broadcastServerMessage("已取消伺服器關機。伺服器將會正常運作。");
				return;
			}
		}
	}

	/**
	 * 目前正在執行的關機執行緒。
	 * 若為 null，表示沒有進行中的關機流程。
	 */
	private ServerShutdownThread _shutdownThread = null;

	/**
	 * 啟動伺服器倒數計時關機流程。
	 *
	 * <p>
	 * 此方法建立並啟動一個 {@link ServerShutdownThread} 執行緒，
	 * 在指定秒數後關閉伺服器。倒數過程中會定時廣播訊息通知玩家。
	 * </p>
	 *
	 * <h3>使用情境</h3>
	 * <ul>
	 * <li>伺服器維護前的正常關機</li>
	 * <li>GM 透過指令進行的計劃性關機</li>
	 * <li>自動化維護腳本觸發的關機</li>
	 * </ul>
	 *
	 * <h3>倒數廣播規則</h3>
	 * <ul>
	 * <li>立即廣播：「伺服器即將關閉，請玩家移動到安全區域先行登出」</li>
	 * <li>剩餘時間 > 30 秒：每分鐘廣播一次剩餘時間</li>
	 * <li>剩餘時間 <= 30 秒：每秒廣播倒數</li>
	 * </ul>
	 *
	 * <h3>並行控制</h3>
	 * <p>
	 * 此方法為 synchronized，確保不會有多個關機流程同時執行。
	 * 若已有進行中的關機流程，再次呼叫此方法將被忽略（不會拋出錯誤）。
	 * </p>
	 *
	 * <h3>取消關機</h3>
	 * <p>
	 * 可透過 {@link #abortShutdown()} 方法取消進行中的關機倒數。
	 * </p>
	 *
	 * @param secondsCount 關機倒數秒數，建議至少 60 秒讓玩家有時間反應
	 *
	 * @see ServerShutdownThread
	 * @see #abortShutdown()
	 * @see #shutdown()
	 */
	public synchronized void shutdownWithCountdown(int secondsCount) {
		if (_shutdownThread != null) {
			// 如果正在關閉
			// TODO 可能要有錯誤通知之類的
			return;
		}
		_shutdownThread = new ServerShutdownThread(secondsCount);
		GeneralThreadPool.getInstance().execute(_shutdownThread);
	}

	/**
	 * 立即關閉伺服器（無倒數計時）。
	 *
	 * <p>
	 * 此方法執行伺服器的緊急關機流程，會立即踢除所有線上玩家並儲存資料，
	 * 然後終止 JVM 程序。這是最終的關機方法，不可逆轉。
	 * </p>
	 *
	 * <h3>執行流程</h3>
	 * <ol>
	 * <li>呼叫 {@link #disconnectAllCharacters()} 踢除所有玩家並儲存資料</li>
	 * <li>呼叫 {@code System.exit(0)} 終止 JVM 程序</li>
	 * </ol>
	 *
	 * <h3>使用情境</h3>
	 * <ul>
	 * <li>{@link ServerShutdownThread} 倒數結束後自動呼叫</li>
	 * <li>Shutdown Hook 捕捉到程序終止訊號時呼叫</li>
	 * <li>發生嚴重錯誤需要緊急關機時呼叫</li>
	 * <li><b>不建議</b>直接呼叫此方法，應優先使用 {@link #shutdownWithCountdown(int)}</li>
	 * </ul>
	 *
	 * <h3>注意事項</h3>
	 * <p>
	 * <b>警告</b>：此方法會立即關閉伺服器，不會有任何倒數或警告訊息。
	 * 直接呼叫可能導致玩家資料遺失或玩家體驗不佳。
	 * 除非緊急狀況，否則應使用 {@link #shutdownWithCountdown(int)} 提供緩衝時間。
	 * </p>
	 *
	 * <h3>Shutdown Hook</h3>
	 * <p>
	 * 伺服器啟動時會透過 {@code Runtime.addShutdownHook()} 註冊 {@link Shutdown} 執行緒，
	 * 確保在 JVM 異常終止時（例如 Ctrl+C、kill 指令、系統關機）也能呼叫此方法正確儲存資料。
	 * </p>
	 *
	 * @see #disconnectAllCharacters()
	 * @see #shutdownWithCountdown(int)
	 * @see Shutdown
	 */
	public void shutdown() {
		disconnectAllCharacters();
		System.exit(0);
	}

	/**
	 * 取消進行中的伺服器關機倒數。
	 *
	 * <p>
	 * 此方法中斷由 {@link #shutdownWithCountdown(int)} 啟動的關機倒數流程，
	 * 伺服器將繼續正常運作。系統會廣播「已取消伺服器關機」訊息通知玩家。
	 * </p>
	 *
	 * <h3>執行流程</h3>
	 * <ol>
	 * <li>檢查是否有進行中的關機執行緒（{@code _shutdownThread != null}）</li>
	 * <li>若無關機執行緒，直接返回（不做任何動作）</li>
	 * <li>若有關機執行緒：
	 *   <ul>
	 *   <li>呼叫 {@code _shutdownThread.interrupt()} 中斷執行緒</li>
	 *   <li>將 {@code _shutdownThread} 設為 null</li>
	 *   <li>{@link ServerShutdownThread} 捕捉 {@link InterruptedException} 並廣播取消訊息</li>
	 *   </ul>
	 * </li>
	 * </ol>
	 *
	 * <h3>使用情境</h3>
	 * <ul>
	 * <li>GM 發現不需要維護，取消已排程的關機</li>
	 * <li>玩家請求延後維護時間</li>
	 * <li>自動化腳本偵測到不應關機的條件</li>
	 * <li>測試關機流程後需要取消</li>
	 * </ul>
	 *
	 * <h3>並行控制</h3>
	 * <p>
	 * 此方法為 synchronized，與 {@link #shutdownWithCountdown(int)} 使用相同的鎖，
	 * 確保取消操作的執行緒安全性。
	 * </p>
	 *
	 * <h3>安全性</h3>
	 * <p>
	 * 若沒有進行中的關機流程，呼叫此方法不會產生任何影響或錯誤，
	 * 可以安全地重複呼叫。
	 * </p>
	 *
	 * @see #shutdownWithCountdown(int)
	 * @see ServerShutdownThread
	 */
	public synchronized void abortShutdown() {
		if (_shutdownThread == null) {
			// 如果正在關閉
			// TODO 可能要有錯誤通知之類的
			return;
		}

		_shutdownThread.interrupt();
		_shutdownThread = null;
	}

	/**
	 * 取得並遞增 YesNo 訊息計數器。
	 *
	 * <p>
	 * 此方法提供唯一的 YesNo 訊息 ID，每次呼叫時自動遞增計數器。
	 * YesNo 訊息是遊戲中的確認對話框，需要玩家選擇「是」或「否」。
	 * </p>
	 *
	 * <h3>用途</h3>
	 * <ul>
	 * <li>產生唯一的 YesNo 訊息 ID，用於追蹤玩家的回應</li>
	 * <li>當伺服器發送 YesNo 確認對話框時呼叫（例如刪除物品確認、交易確認等）</li>
	 * <li>計數器從伺服器啟動時開始累計，直到伺服器關閉</li>
	 * </ul>
	 *
	 * <h3>YesNo 訊息系統</h3>
	 * <p>
	 * 遊戲中許多重要操作需要玩家確認，例如：
	 * <ul>
	 * <li>刪除道具：「確定要刪除此道具嗎？」</li>
	 * <li>接受交易：「確定要接受此交易嗎？」</li>
	 * <li>學習技能：「確定要學習此技能嗎？」</li>
	 * <li>離開血盟：「確定要離開血盟嗎？」</li>
	 * </ul>
	 * 每個 YesNo 訊息都需要唯一 ID，以便伺服器正確處理玩家的回應。
	 * </p>
	 *
	 * <h3>執行緒安全性</h3>
	 * <p>
	 * <b>注意</b>：此方法並非執行緒安全（非 synchronized）。
	 * 在多執行緒環境下可能發生競態條件（race condition），導致：
	 * <ul>
	 * <li>兩個執行緒可能取得相同的 ID（機率極低）</li>
	 * <li>計數器遺失某些遞增操作</li>
	 * </ul>
	 * 不過由於 YesNo ID 主要用於追蹤，偶爾的重複 ID 不會造成嚴重問題，
	 * 且效能損失極小，因此沒有使用同步機制。
	 * </p>
	 *
	 * <h3>計數器範圍</h3>
	 * <p>
	 * 計數器為 int 型別，最大值約 21 億（2^31 - 1）。
	 * 即使伺服器每秒處理 1000 個 YesNo 訊息，也需要約 24 天才會達到上限。
	 * 達到上限後會溢位變成負數，但不影響功能（因為只需要唯一性，正負無關）。
	 * </p>
	 *
	 * <h3>使用範例</h3>
	 * <pre>
	 * // 伺服器發送刪除道具確認訊息
	 * int yesNoId = GameServer.getYesNoCount();
	 * S_MessageYN message = new S_MessageYN(yesNoId, "確定要刪除此道具嗎？");
	 * player.sendPackets(message);
	 *
	 * // 玩家回應時，透過 yesNoId 識別這是哪個確認訊息
	 * </pre>
	 *
	 * @return 遞增後的 YesNo 訊息計數，每次呼叫都會返回不同的值
	 *
	 * @see l1j.server.server.serverpackets.S_MessageYN
	 */
	public static int getYesNoCount() {
		YesNoCount += 1;
		return YesNoCount;
	}
}
