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

import java.util.Map;
import java.util.logging.Logger;

import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.L1Trade;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.utils.collections.Maps;

/**
 * 登入控制器
 * <p>管理玩家帳號的登入、登出及連線狀態，控制同時在線人數上限。
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>追蹤所有線上帳號及其對應的客戶端連線</li>
 *   <li>控制伺服器同時在線人數上限</li>
 *   <li>防止同一帳號重複登入 (踢除舊連線)</li>
 *   <li>處理登入、登出流程</li>
 *   <li>GM 帳號可無視人數上限登入</li>
 * </ul>
 *
 * <h3>登入驗證流程:</h3>
 * <ol>
 *   <li>檢查帳號是否已通過認證 ({@link Account#isValid()})</li>
 *   <li>檢查伺服器是否已滿 (GM 帳號可無視此限制)</li>
 *   <li>檢查帳號是否已在線 (若已在線則踢除舊連線)</li>
 *   <li>將帳號加入線上列表</li>
 * </ol>
 *
 * <h3>重複登入處理:</h3>
 * <p>當同一帳號嘗試重複登入時:
 * <ol>
 *   <li>移除該帳號的舊連線</li>
 *   <li>向舊連線發送踢除訊息 (S_ServerMessage 357)</li>
 *   <li>延遲 1 秒後強制斷線</li>
 *   <li>拋出 {@link AccountAlreadyLoginException} 通知新連線稍後重試</li>
 * </ol>
 *
 * <h3>登出處理:</h3>
 * <ul>
 *   <li>自動取消進行中的交易</li>
 *   <li>從線上列表移除帳號</li>
 *   <li>釋放客戶端連線資源</li>
 * </ul>
 *
 * <h3>執行緒安全:</h3>
 * <p>登入與登出方法使用 {@code synchronized} 確保執行緒安全，
 * 內部使用 {@link Maps#newConcurrentMap()} 提供併發存取能力。
 *
 * @see ClientThread
 * @see Account
 * @see GameServerFullException
 * @see AccountAlreadyLoginException
 */
public class LoginController {
	private static Logger _log = Logger.getLogger(LoginController.class.getName());

	/** Singleton 實例 */
	private static LoginController _instance;

	/**
	 * 線上帳號列表
	 * <p>Key: 帳號名稱, Value: 客戶端連線執行緒
	 * <p>使用 ConcurrentMap 確保多執行緒環境下的安全性。
	 */
	private Map<String, ClientThread> _accounts = Maps.newConcurrentMap();

	/**
	 * 伺服器同時在線人數上限
	 * <p>GM 帳號可無視此限制登入。
	 */
	private int _maxAllowedOnlinePlayers;

	/**
	 * 私有建構子，防止外部實例化
	 * <p>使用 Singleton 模式，透過 {@link #getInstance()} 取得實例。
	 */
	private LoginController() {
	}

	/**
	 * 取得 Singleton 實例
	 * <p>使用延遲初始化模式，首次呼叫時建立實例。
	 *
	 * @return LoginController 實例
	 */
	public static LoginController getInstance() {
		if (_instance == null) {
			_instance = new LoginController();
		}
		return _instance;
	}

	/**
	 * 取得所有線上帳號的客戶端連線
	 *
	 * @return 客戶端連線陣列
	 */
	public ClientThread[] getAllAccounts() {
		return _accounts.values().toArray(new ClientThread[_accounts.size()]);
	}

	/**
	 * 取得目前線上人數
	 *
	 * @return 線上帳號數量
	 */
	public int getOnlinePlayerCount() {
		return _accounts.size();
	}

	/**
	 * 取得伺服器同時在線人數上限
	 *
	 * @return 人數上限
	 */
	public int getMaxAllowedOnlinePlayers() {
		return _maxAllowedOnlinePlayers;
	}

	/**
	 * 設定伺服器同時在線人數上限
	 * <p>此設定不影響 GM 帳號，GM 可無視人數上限登入。
	 *
	 * @param maxAllowedOnlinePlayers 人數上限
	 */
	public void setMaxAllowedOnlinePlayers(int maxAllowedOnlinePlayers) {
		_maxAllowedOnlinePlayers = maxAllowedOnlinePlayers;
	}

	/**
	 * 踢除客戶端連線
	 * <p>在獨立執行緒中執行踢除操作，避免阻塞主執行緒。
	 *
	 * <h3>執行流程:</h3>
	 * <ol>
	 *   <li>檢查客戶端是否為 null</li>
	 *   <li>若玩家在線，發送踢除訊息 (S_ServerMessage 357: "由於伺服器問題，您的連線已中斷。")</li>
	 *   <li>延遲 1 秒確保訊息送達</li>
	 *   <li>強制斷開連線</li>
	 * </ol>
	 *
	 * @param client 要踢除的客戶端連線
	 * @see S_ServerMessage
	 * @see ClientThread#kick()
	 */
	private void kickClient(final ClientThread client) {
		if (client == null) {
			return;
		}

		GeneralThreadPool.getInstance().execute(new Runnable() {
			@Override
			public void run() {
				if (client.getActiveChar() != null) {
					client.getActiveChar()
							.sendPackets(new S_ServerMessage(357));
				}

				try {
					Thread.sleep(1000);
				} catch (Exception e) {
				}
				client.kick();
			}
		});
	}

	/**
	 * 處理帳號登入
	 * <p>執行帳號登入驗證並加入線上列表，確保同一帳號不會重複登入。
	 *
	 * <h3>驗證流程:</h3>
	 * <ol>
	 *   <li><b>帳號認證檢查:</b> 確認帳號已通過密碼驗證</li>
	 *   <li><b>人數上限檢查:</b> 若伺服器已滿且非 GM 帳號，拋出 {@link GameServerFullException}</li>
	 *   <li><b>重複登入檢查:</b> 若帳號已在線，踢除舊連線並拋出 {@link AccountAlreadyLoginException}</li>
	 *   <li><b>加入線上列表:</b> 將帳號與客戶端連線加入 _accounts Map</li>
	 * </ol>
	 *
	 * <h3>執行緒安全:</h3>
	 * <p>使用 {@code synchronized} 確保登入流程的原子性，避免競態條件。
	 *
	 * @param client 客戶端連線執行緒
	 * @param account 要登入的帳號
	 * @throws GameServerFullException 伺服器人數已滿 (GM 除外)
	 * @throws AccountAlreadyLoginException 帳號已在線 (舊連線已被踢除)
	 * @throws IllegalArgumentException 帳號未通過認證
	 * @see Account#isValid()
	 * @see Account#isGameMaster()
	 * @see #kickClient(ClientThread)
	 */
	public synchronized void login(ClientThread client, Account account)
			throws GameServerFullException, AccountAlreadyLoginException {
		_log.info("LoginController.login: 開始登入 account=" + account.getName() + " isValid=" + account.isValid());

		if (!account.isValid()) {
			// 密碼驗證未指定或不驗證帳戶。
			// 此代碼只存在的錯誤檢測。
			_log.warning("LoginController.login: 帳戶未通過認證 account=" + account.getName());
			throw new IllegalArgumentException("帳戶沒有通過認證");
		}

		_log.info("LoginController.login: 檢查人數上限 maxPlayers=" + getMaxAllowedOnlinePlayers()
				+ " currentPlayers=" + getOnlinePlayerCount() + " isGM=" + account.isGameMaster());
		if ((getMaxAllowedOnlinePlayers() <= getOnlinePlayerCount())
				&& !account.isGameMaster()) {
			_log.info("LoginController.login: 伺服器已滿 account=" + account.getName());
			throw new GameServerFullException();
		}

		if (_accounts.containsKey(account.getName())) {
			_log.info("LoginController.login: 帳號已在線，踢除舊連線 account=" + account.getName());
			kickClient(_accounts.remove(account.getName()));
			throw new AccountAlreadyLoginException();
		}

		_accounts.put(account.getName(), client);
		_log.info("LoginController.login: 登入成功，已加入線上列表 account=" + account.getName()
				+ " onlineCount=" + getOnlinePlayerCount());
	}

	/**
	 * 處理帳號登出
	 * <p>清理客戶端連線狀態並從線上列表移除帳號。
	 *
	 * <h3>登出流程:</h3>
	 * <ol>
	 *   <li>檢查帳號名稱是否存在 (null 表示未完成登入)</li>
	 *   <li>若玩家正在交易，自動取消交易</li>
	 *   <li>從線上列表移除帳號</li>
	 * </ol>
	 *
	 * <h3>交易處理:</h3>
	 * <p>若玩家登出時正在進行交易 ({@code player.getTradeID() != 0})，
	 * 會自動呼叫 {@link L1Trade#TradeCancel(L1PcInstance)} 取消交易，
	 * 確保交易雙方的物品不會遺失。
	 *
	 * <h3>執行緒安全:</h3>
	 * <p>使用 {@code synchronized} 確保登出流程的原子性。
	 *
	 * @param client 客戶端連線執行緒
	 * @return 是否成功登出 (true: 成功, false: 帳號不在線上列表中)
	 * @see ClientThread#getAccountName()
	 * @see ClientThread#getActiveChar()
	 * @see L1Trade#TradeCancel(L1PcInstance)
	 */
	public synchronized boolean logout(ClientThread client) {
		if (client.getAccountName() == null) {
			return false;
		}

		//重登, 取消交易
		L1PcInstance player = client.getActiveChar();
		if (player != null) {
			if (player.getTradeID() != 0) {
				final L1Trade trade = new L1Trade();
				trade.TradeCancel(player);
			}
		}

		return _accounts.remove(client.getAccountName()) != null;
	}
}
