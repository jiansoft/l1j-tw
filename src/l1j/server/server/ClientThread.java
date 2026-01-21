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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.Config;
import l1j.server.server.Opcodes;
import l1j.server.server.datatables.CharBuffTable;
import l1j.server.server.model.L1DragonSlayer;
import l1j.server.server.model.Getback;
import l1j.server.server.model.L1Trade;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1DollInstance;
import l1j.server.server.model.Instance.L1FollowerInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1PetInstance;
import l1j.server.server.model.Instance.L1SummonInstance;
import l1j.server.server.serverpackets.S_Disconnect;
import l1j.server.server.serverpackets.S_PacketBox;
import l1j.server.server.serverpackets.S_SummonPack;
import l1j.server.server.serverpackets.ServerBasePacket;
import l1j.server.server.utils.StreamUtil;
import l1j.server.server.utils.SystemUtil;

/**
 * 客戶端連線執行緒類別
 *
 * <p>此類別負責處理單一客戶端的網路連線，包含封包收發、加密解密、自動儲存等功能。
 * 每個連線到遊戲伺服器的客戶端都會建立一個獨立的 ClientThread 實例。
 *
 * <h2>主要功能</h2>
 * <ul>
 *   <li>處理客戶端連線的建立與維護</li>
 *   <li>接收並解密來自客戶端的封包</li>
 *   <li>將封包路由至 {@link PacketHandler} 進行處理</li>
 *   <li>發送加密封包至客戶端</li>
 *   <li>定期自動儲存角色資料和道具資料</li>
 *   <li>超時偵測與自動踢除閒置連線</li>
 *   <li>管理封包處理佇列（區分移動封包與一般封包）</li>
 * </ul>
 *
 * <h2>執行緒生命週期</h2>
 * <ol>
 *   <li>連線建立：發送初始化封包（包含加密金鑰）</li>
 *   <li>封包循環：持續接收、解密、處理來自客戶端的封包</li>
 *   <li>自動儲存：定期儲存角色資料和道具資料</li>
 *   <li>斷線處理：清理角色狀態、儲存資料、釋放資源</li>
 * </ol>
 *
 * <h2>封包處理流程</h2>
 * <ol>
 *   <li>從輸入串流讀取封包長度（2 bytes）</li>
 *   <li>讀取完整的封包資料</li>
 *   <li>使用 {@link Cipher} 進行解密</li>
 *   <li>根據 Opcode 判斷封包類型</li>
 *   <li>將封包路由至對應的處理器（移動封包或一般封包佇列）</li>
 * </ol>
 *
 * <h2>封包佇列機制</h2>
 * <p>為了提升效能，封包處理分為兩個獨立佇列：
 * <ul>
 *   <li>移動封包佇列（M_CAPACITY=3）：處理 {@code C_OPCODE_MOVECHAR}，確保即時性</li>
 *   <li>一般封包佇列（H_CAPACITY=2）：處理其他所有動作封包</li>
 * </ul>
 *
 * <h2>自動儲存機制</h2>
 * <p>系統會定期自動儲存角色資料，防止資料遺失：
 * <ul>
 *   <li>角色資料儲存間隔：{@link Config#AUTOSAVE_INTERVAL} 秒</li>
 *   <li>道具資料儲存間隔：{@link Config#AUTOSAVE_INTERVAL_INVENTORY} 秒</li>
 *   <li>儲存時機：每次封包處理循環都會檢查是否達到儲存間隔</li>
 * </ul>
 *
 * <h2>超時踢除機制</h2>
 * <p>透過 {@link ClientThreadObserver} 監控連線狀態：
 * <ul>
 *   <li>超時時間：{@link Config#AUTOMATIC_KICK} 分鐘</li>
 *   <li>例外：正在個人商店的玩家不會被踢除</li>
 *   <li>觸發條件：在超時時間內沒有收到任何封包（{@code C_OPCODE_KEEPALIVE} 除外）</li>
 * </ul>
 *
 * <h2>協定版本</h2>
 * <p>本實作基於 <b>Lineage 1 Taiwan Server 3.80C</b> 協定。
 * 初始化封包使用固定的 11 bytes 握手序列（{@link #FIRST_PACKET}）。
 *
 * @author L1JTW
 * @version 3.80C
 * @see PacketHandler
 * @see Cipher
 * @see GameServer
 * @see L1PcInstance
 */
// Referenced classes of package l1j.server.server:
// PacketHandler, Logins, IpTable, LoginController,
// ClanTable, IdFactory
//
public class ClientThread implements Runnable, PacketOutput {

	/** 日誌記錄器 */
	private static Logger _log = Logger.getLogger(ClientThread.class.getName());

	/** 輸入串流，用於接收來自客戶端的資料 */
	private InputStream _in;

	/** 輸出串流，用於發送資料至客戶端 */
	private OutputStream _out;

	/** 封包處理器，負責處理各類型的客戶端封包 */
	private PacketHandler _handler;

	/** 此連線關聯的遊戲帳號 */
	private Account _account;

	/** 目前活動的角色實例 */
	private L1PcInstance _activeChar;

	/** 客戶端的 IP 位址 */
	private String _ip;

	/** 客戶端的主機名稱（可能是 IP 或 DNS 反查結果） */
	private String _hostname;

	/** 客戶端的 Socket 連線 */
	private Socket _csocket;

	/**
	 * 登入狀態標記
	 * <ul>
	 *   <li>0：未登入或已完成登入</li>
	 *   <li>1：正在登入過程中（已接收 {@code C_OPCODE_BEANFUNLOGINPACKET} 或 {@code C_OPCODE_CHANGECHAR}）</li>
	 * </ul>
	 */
	private int _loginStatus = 0;

	/**
	 * 3.80C Taiwan Server 初始握手封包
	 *
	 * <p>此為固定的 11 bytes 握手序列，在建立連線時發送給客戶端。
	 * 此封包緊接在加密金鑰之後發送，用於驗證客戶端版本。
	 */
	private static final byte[] FIRST_PACKET = {
	    (byte) 0x9d, (byte) 0xd1, (byte) 0xd6, (byte) 0x7a, (byte) 0xf4,
	    (byte) 0x62, (byte) 0xe7, (byte) 0xa0, (byte) 0x66, (byte) 0x02,
	    (byte) 0xfa
	};

	/**
	 * 預設建構子（僅供測試使用）
	 *
	 * <p>此建構子不初始化任何欄位，僅用於單元測試或特殊情況。
	 * 正常使用應該透過 {@link #ClientThread(Socket)} 建構。
	 */
	protected ClientThread() {
	}

	/**
	 * 建構一個新的客戶端連線執行緒
	 *
	 * <p>此建構子會初始化與客戶端的網路連線，包括：
	 * <ul>
	 *   <li>取得客戶端的 IP 位址和主機名稱</li>
	 *   <li>建立輸入和輸出串流</li>
	 *   <li>初始化封包處理器</li>
	 * </ul>
	 *
	 * @param socket 客戶端的 Socket 連線
	 * @throws IOException 如果無法建立輸入輸出串流
	 * @see PacketHandler
	 */
	public ClientThread(Socket socket) throws IOException {
		_csocket = socket;
		_ip = socket.getInetAddress().getHostAddress();
		if (Config.HOSTNAME_LOOKUPS) {
			_hostname = socket.getInetAddress().getHostName();
		} else {
			_hostname = _ip;
		}
		_in = socket.getInputStream();
		_out = new BufferedOutputStream(socket.getOutputStream());

		// PacketHandler 初始化
		_handler = new PacketHandler(this);
	}

	/**
	 * 取得客戶端的 IP 位址
	 *
	 * @return IP 位址字串（例如："192.168.1.100"）
	 */
	public String getIp() {
		return _ip;
	}

	/**
	 * 取得客戶端的主機名稱
	 *
	 * <p>如果 {@link Config#HOSTNAME_LOOKUPS} 設為 true，則會嘗試進行 DNS 反查；
	 * 否則直接回傳 IP 位址。
	 *
	 * @return 主機名稱或 IP 位址字串
	 */
	public String getHostname() {
		return _hostname;
	}

	/**
	 * 角色重新選擇標記
	 *
	 * <p>此標記用於限制 ClientThread 的定期自動儲存功能：
	 * <ul>
	 *   <li>true：限制自動儲存（角色選擇畫面或重新選擇角色時）</li>
	 *   <li>false：允許自動儲存（角色已登入遊戲世界時）</li>
	 * </ul>
	 *
	 * <p>狀態轉換：
	 * <ul>
	 *   <li>執行 {@code C_LoginToServer} 時設為 false（角色進入遊戲）</li>
	 *   <li>執行 {@code C_NewCharSelect} 時設為 true（重新選擇角色）</li>
	 * </ul>
	 */
	private boolean _charRestart = true;

	/**
	 * 設定角色重新選擇標記
	 *
	 * <p>控制是否限制自動儲存功能。當玩家重新選擇角色時應設為 true，
	 * 當角色進入遊戲時應設為 false。
	 *
	 * @param flag true 表示限制自動儲存，false 表示允許自動儲存
	 * @see #doAutoSave()
	 */
	public void CharReStart(boolean flag) {
		_charRestart = flag;
	}


	/**
	 * 從輸入串流讀取並解密一個封包
	 *
	 * <p>封包格式：
	 * <pre>
	 * [2 bytes: 封包長度（包含長度欄位本身）]
	 * [N bytes: 加密的封包資料]
	 * </pre>
	 *
	 * <p>讀取流程：
	 * <ol>
	 *   <li>讀取 2 bytes 的封包長度（Little Endian）</li>
	 *   <li>計算資料長度：封包長度 - 2</li>
	 *   <li>驗證資料長度是否合理（1 ~ 65533 bytes）</li>
	 *   <li>讀取完整的封包資料</li>
	 *   <li>使用 {@link Cipher} 解密資料</li>
	 * </ol>
	 *
	 * @return 解密後的封包資料
	 * @throws Exception 如果讀取失敗、封包不完整或解密失敗
	 * @see Cipher#decrypt(byte[])
	 */
	private byte[] readPacket() throws Exception {
		try {
			int hiByte = _in.read();
			int loByte = _in.read();
			if ((loByte < 0) || (hiByte < 0)) {
				throw new RuntimeException();
			}

			final int dataLength = ((loByte << 8) + hiByte) - 2;
			if ((dataLength <= 0) || (dataLength > 65533)) {
				throw new RuntimeException();
			}

			byte data[] = new byte[dataLength];

			int readSize = 0;

			for (int i = 0; i != -1 && readSize < dataLength; readSize += i) {
				i = _in.read(data, readSize, dataLength - readSize);
			}

			if (readSize != dataLength) {
				_log.warning("Incomplete Packet is sent to the server, closing connection.");
				throw new RuntimeException();
			}

			return _cipher.decrypt(data);
		} catch (Exception e) {
			throw e;
		}
	}

	/** 上次儲存角色資料的時間戳記（毫秒） */
	private long _lastSavedTime = System.currentTimeMillis();

	/** 上次儲存道具資料的時間戳記（毫秒） */
	private long _lastSavedTime_inventory = System.currentTimeMillis();

	/** 封包加密/解密器 */
	private Cipher _cipher;

	/**
	 * 執行定期自動儲存
	 *
	 * <p>此方法會檢查是否達到儲存間隔，並在必要時自動儲存角色資料和道具資料。
	 * 儲存間隔由設定檔 {@link Config} 決定。
	 *
	 * <p>自動儲存條件：
	 * <ul>
	 *   <li>角色已登入（{@code _activeChar} 不為 null）</li>
	 *   <li>不在角色選擇狀態（{@code _charRestart} 為 false）</li>
	 *   <li>距離上次儲存已超過設定的間隔時間</li>
	 * </ul>
	 *
	 * <p>儲存內容：
	 * <ul>
	 *   <li>角色資料：等級、經驗值、HP/MP、座標等（間隔：{@link Config#AUTOSAVE_INTERVAL} 秒）</li>
	 *   <li>道具資料：背包內所有道具（間隔：{@link Config#AUTOSAVE_INTERVAL_INVENTORY} 秒）</li>
	 * </ul>
	 *
	 * @throws Exception 如果儲存過程發生錯誤
	 * @see L1PcInstance#save()
	 * @see L1PcInstance#saveInventory()
	 * @see Config#AUTOSAVE_INTERVAL
	 * @see Config#AUTOSAVE_INTERVAL_INVENTORY
	 */
	private void doAutoSave() throws Exception {
		if (_activeChar == null || _charRestart) {
			return;
		}
		try {
			// 自動儲存角色資料
			if (Config.AUTOSAVE_INTERVAL * 1000 < System.currentTimeMillis() - _lastSavedTime) {
				_activeChar.save();
				_lastSavedTime = System.currentTimeMillis();
			}

			// 自動儲存身上道具資料
			if (Config.AUTOSAVE_INTERVAL_INVENTORY * 1000 < System.currentTimeMillis() - _lastSavedTime_inventory) {
				_activeChar.saveInventory();
				_lastSavedTime_inventory = System.currentTimeMillis();
			}
		} catch (Exception e) {
			_log.warning("Client autosave failure.");
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			throw e;
		}
	}

	/**
	 * 執行緒主要執行方法
	 *
	 * <p>此方法包含客戶端連線的完整生命週期：
	 * <ol>
	 *   <li><b>連線初始化</b>：
	 *     <ul>
	 *       <li>產生隨機加密金鑰</li>
	 *       <li>發送初始化封包（{@link Opcodes#S_OPCODE_INITPACKET}）</li>
	 *       <li>發送握手序列（{@link #FIRST_PACKET}）</li>
	 *       <li>初始化 {@link Cipher} 加密器</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>啟動封包處理佇列</b>：
	 *     <ul>
	 *       <li>移動封包佇列：處理 {@code C_OPCODE_MOVECHAR}（容量：{@link #M_CAPACITY}）</li>
	 *       <li>一般封包佇列：處理其他動作封包（容量：{@link #H_CAPACITY}）</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>啟動超時監控</b>：
	 *     <ul>
	 *       <li>如果 {@link Config#AUTOMATIC_KICK} > 0，啟動 {@link ClientThreadObserver}</li>
	 *       <li>定期檢查是否在指定時間內收到封包</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>封包處理循環</b>：
	 *     <ul>
	 *       <li>持續從輸入串流讀取封包</li>
	 *       <li>執行自動儲存檢查（{@link #doAutoSave()}）</li>
	 *       <li>解密封包並路由至對應的處理器</li>
	 *       <li>特殊處理：切換角色、丟道具、刪除道具（直接處理，不進入佇列）</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>斷線處理</b>：
	 *     <ul>
	 *       <li>清理角色狀態（交易、決鬥、組隊、寵物、魔法娃娃等）</li>
	 *       <li>儲存角色資料和道具資料</li>
	 *       <li>發送斷線封包（{@link S_Disconnect}）</li>
	 *       <li>關閉網路連線並釋放資源</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 *
	 * <h3>封包佇列設計理念</h3>
	 * <p>為了防止因伺服器或網路延遲導致的誤判作弊，封包處理採用限量佇列：
	 * <ul>
	 *   <li>避免伺服器高負載時，封包瞬間湧入被誤判為作弊</li>
	 *   <li>避免伺服器端網路（下行）延遲造成的誤判</li>
	 *   <li>避免客戶端網路（上行）延遲造成的誤判</li>
	 * </ul>
	 *
	 * @see HcPacket
	 * @see ClientThreadObserver
	 * @see PacketHandler#handlePacket(byte[], L1PcInstance)
	 * @see #quitGame(L1PcInstance)
	 */
	@Override
	public void run() {


		/*
		 * 封包佇列容量限制說明：
		 *
		 * 目的：限制來自客戶端的封包處理速度，避免誤判作弊。
		 *
		 * 原因：如果不限制，可能會發生以下情況導致誤判：
		 *
		 * 1. 伺服器過載：當伺服器負載高時，封包會累積；負載降低後會瞬間處理大量封包，被誤判為異常行為。
		 * 2. 伺服器網路延遲（下行）：延遲導致封包回應慢，客戶端繼續發送封包，當網路恢復時封包瞬間湧入。
		 * 3. 客戶端網路延遲（上行）：客戶端發送的封包因網路問題累積，當網路恢復時同時送達伺服器。
		 *
		 * 注意：若要移除此限制，需先改進作弊偵測機制。
		 */
		HcPacket movePacket = new HcPacket(M_CAPACITY);
		HcPacket hcPacket = new HcPacket(H_CAPACITY);
		GeneralThreadPool.getInstance().execute(movePacket);
		GeneralThreadPool.getInstance().execute(hcPacket);
		
		String keyHax ="";
		int key = 0;
		byte Bogus = 0;
		
		try {
			/** 採取亂數取seed */
			keyHax = Integer.toHexString((int) (Math.random() * 2147483647) + 1);
			key = Integer.parseInt(keyHax, 16);

			Bogus = (byte) (FIRST_PACKET.length + 7);
			_out.write(Bogus & 0xFF);
			_out.write(Bogus >> 8 & 0xFF);
			_out.write(Opcodes.S_OPCODE_INITPACKET);// 3.7C Taiwan Server
			_out.write((byte) (key & 0xFF));
			_out.write((byte) (key >> 8 & 0xFF));
			_out.write((byte) (key >> 16 & 0xFF));
			_out.write((byte) (key >> 24 & 0xFF));

			_out.write(FIRST_PACKET);
			_out.flush();

		}
		catch (Throwable e) {
			try {
				_log.info("異常用戶端(" + _hostname + ") 連結到伺服器, 已中斷該連線。");
				StreamUtil.close(_out, _in);
				if (_csocket != null) {
					_csocket.close();
					_csocket = null;
				}
				return;
			} catch (Throwable ex) {
				return;
			}
		}
		finally {
		
		}



		try {
			_log.info("(" + _hostname + ") 連結到伺服器。");
			System.out.println("使用了 " + SystemUtil.getUsedMemoryMB() + "MB 的記憶體");
			System.out.println("等待客戶端連接...");
			
			ClientThreadObserver observer = new ClientThreadObserver(Config.AUTOMATIC_KICK * 60 * 1000); // 自動斷線的時間（單位:毫秒）

			// 是否啟用自動踢人
			if (Config.AUTOMATIC_KICK > 0) {
				observer.start();
			}
			
			_cipher = new Cipher(key);

			while (true) {
				doAutoSave();

				byte data[] = null;
				try {
					data = readPacket();
				} catch (Exception e) {
					break;
				}
				// _log.finest("[C]\n" + new
				// ByteArrayUtil(data).dumpToString());

				int opcode = data[0] & 0xFF;

				// 處理多重登入
				if (opcode == Opcodes.C_OPCODE_BEANFUNLOGINPACKET || opcode == Opcodes.C_OPCODE_CHANGECHAR) {
					_loginStatus = 1;
				}
				if (opcode == Opcodes.C_OPCODE_LOGINTOSERVER) {
					if (_loginStatus != 1) {
						continue;
					}
				}
				if (opcode == Opcodes.C_OPCODE_LOGINTOSERVEROK) {
					_loginStatus = 0;
				}

				if (opcode != Opcodes.C_OPCODE_KEEPALIVE) {
					// C_OPCODE_KEEPALIVE以外の何かしらのパケットを受け取ったらObserverへ通知
					observer.packetReceived();
				}
				// TODO: 翻譯
				// 如果目前角色為 null はキャラクター選択前なのでOpcodeの取捨選択はせず全て実行
				if (_activeChar == null) {
					_handler.handlePacket(data, _activeChar);
					continue;
				}

				// TODO: 翻譯
				// 以降、PacketHandlerの処理状況がClientThreadに影響を与えないようにする為の処理
				// 目的はOpcodeの取捨選択とClientThreadとPacketHandlerの切り離し

				// 要處理的 OPCODE
				// 切換角色、丟道具到地上、刪除身上道具
				if (opcode == Opcodes.C_OPCODE_CHANGECHAR
						|| opcode == Opcodes.C_OPCODE_DROPITEM
						|| opcode == Opcodes.C_OPCODE_DELETEINVENTORYITEM) {
					_handler.handlePacket(data, _activeChar);
				} else if (opcode == Opcodes.C_OPCODE_MOVECHAR) {
					// 為了確保即時的移動，將移動的封包獨立出來處理
					movePacket.requestWork(data);
				} else {
					// 處理其他數據的傳遞
					hcPacket.requestWork(data);
				}
			}
		} catch (Throwable e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} finally {
			try {
				if (_activeChar != null) {
					quitGame(_activeChar);

					synchronized (_activeChar) {
						// 從線上中登出角色
						_activeChar.logout();
						setActiveChar(null);
					}
				}
				// 玩家離線時, online=0
				if (getAccount() != null) {
					Account.online(getAccount(), false);
				}

				// 送出斷線的封包
				sendPacket(new S_Disconnect());

				StreamUtil.close(_out, _in);
				if (_csocket != null) {
					_csocket.close();
					_csocket = null;
				}
			} catch (Exception e) {
				_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			} finally {
				LoginController.getInstance().logout(this);
			}
		}
		_csocket = null;
		_log.fine("Server thread[C] stopped");
		if (_kick < 1) {
			_log.info("(" + getAccountName() + ":" + _hostname + ")連線終止。");
			System.out.println("使用了 " + SystemUtil.getUsedMemoryMB()
					+ "MB 的記憶體");
			System.out.println("等待客戶端連接...");
			if (getAccount() != null) {
				Account.online(getAccount(), false);
			}
		}
		return;
	}

	/**
	 * 踢除標記
	 * <ul>
	 *   <li>0：正常斷線</li>
	 *   <li>1：被踢除的斷線</li>
	 * </ul>
	 */
	private int _kick = 0;

	/**
	 * 強制踢除客戶端連線
	 *
	 * <p>此方法會立即中斷客戶端連線，並執行以下操作：
	 * <ol>
	 *   <li>將帳號狀態設為離線</li>
	 *   <li>發送斷線封包（{@link S_Disconnect}）</li>
	 *   <li>設定踢除標記</li>
	 *   <li>關閉輸入輸出串流</li>
	 *   <li>關閉 Socket 連線</li>
	 * </ol>
	 *
	 * <p>此方法通常由以下情況觸發：
	 * <ul>
	 *   <li>{@link ClientThreadObserver} 偵測到超時</li>
	 *   <li>GM 指令強制踢人</li>
	 *   <li>偵測到異常行為</li>
	 * </ul>
	 *
	 * @see ClientThreadObserver#run()
	 * @see S_Disconnect
	 */
	public void kick() {
		try {
			Account.online(getAccount(), false);
			sendPacket(new S_Disconnect());
			_kick = 1;
			StreamUtil.close(_out, _in);
			if (_csocket != null) {
				_csocket.close();
				_csocket = null;
			}
		}
		catch (Throwable ex) {

		}
	}

	/** 移動封包佇列的最大容量（確保移動的即時性） */
	private static final int M_CAPACITY = 3;

	/** 一般動作封包佇列的最大容量 */
	private static final int H_CAPACITY = 2;

	/**
	 * 封包處理佇列類別
	 *
	 * <p>此內部類別實作一個獨立的封包處理執行緒，用於異步處理客戶端封包。
	 * 每個 ClientThread 會建立兩個 HcPacket 實例：
	 * <ul>
	 *   <li>移動封包佇列：專門處理 {@code C_OPCODE_MOVECHAR}，容量為 {@link #M_CAPACITY}</li>
	 *   <li>一般封包佇列：處理其他所有動作封包，容量為 {@link #H_CAPACITY}</li>
	 * </ul>
	 *
	 * <h3>設計目的</h3>
	 * <ul>
	 *   <li>解耦封包接收和封包處理，避免阻塞主執行緒</li>
	 *   <li>透過佇列容量限制，防止封包湧入造成的誤判作弊</li>
	 *   <li>區分移動和其他動作，確保移動的即時性</li>
	 * </ul>
	 *
	 * <h3>工作流程</h3>
	 * <ol>
	 *   <li>主執行緒呼叫 {@link #requestWork(byte[])} 將封包加入佇列</li>
	 *   <li>處理執行緒持續從佇列取出封包</li>
	 *   <li>呼叫 {@link PacketHandler#handlePacket(byte[], L1PcInstance)} 處理封包</li>
	 *   <li>如果佇列為空，執行緒休眠 10 毫秒</li>
	 * </ol>
	 *
	 * @see PacketHandler
	 * @see LinkedBlockingQueue
	 */
	class HcPacket implements Runnable {
		/** 封包佇列 */
		private final Queue<byte[]> _queue;

		/** 封包處理器 */
		private PacketHandler _handler;

		/**
		 * 建構一個無容量限制的封包處理佇列
		 *
		 * <p>使用 {@link ConcurrentLinkedQueue}，理論上可以容納無限封包。
		 * 此建構子目前未被使用，保留以供特殊需求。
		 */
		public HcPacket() {
			_queue = new ConcurrentLinkedQueue<byte[]>();
			_handler = new PacketHandler(ClientThread.this);
		}

		/**
		 * 建構一個有容量限制的封包處理佇列
		 *
		 * <p>使用 {@link LinkedBlockingQueue}，當佇列滿時，新封包會被丟棄（{@code offer} 會回傳 false）。
		 * 這是實際使用的建構子，用於建立移動封包佇列和一般封包佇列。
		 *
		 * @param capacity 佇列容量上限
		 * @see #M_CAPACITY
		 * @see #H_CAPACITY
		 */
		public HcPacket(int capacity) {
			_queue = new LinkedBlockingQueue<byte[]>(capacity);
			_handler = new PacketHandler(ClientThread.this);
		}

		/**
		 * 請求處理一個封包
		 *
		 * <p>將封包加入佇列等待處理。如果佇列已滿，封包會被丟棄。
		 *
		 * @param data 封包的原始資料（已解密）
		 * @return true 表示成功加入佇列，false 表示佇列已滿
		 */
		public void requestWork(byte data[]) {
			_queue.offer(data);
		}

		/**
		 * 封包處理執行緒的主要執行方法
		 *
		 * <p>持續從佇列取出封包並處理，直到客戶端連線關閉（{@code _csocket} 為 null）。
		 * 如果佇列為空，執行緒會休眠 10 毫秒以避免空轉消耗 CPU。
		 *
		 * @see PacketHandler#handlePacket(byte[], L1PcInstance)
		 */
		@Override
		public void run() {
			byte[] data;
			while (_csocket != null) {
				data = _queue.poll();
				if (data != null) {
					try {
						_handler.handlePacket(data, _activeChar);
					} catch (Exception e) {
					}
				} else {
					try {
						Thread.sleep(10);
					} catch (Exception e) {
					}
				}
			}
			return;
		}
	}

	/** 共用的計時器，用於所有客戶端連線的超時監控 */
	private static Timer _observerTimer = new Timer();

	/**
	 * 客戶端超時監控類別
	 *
	 * <p>此內部類別實作一個定時任務，用於監控客戶端是否在指定時間內有封包活動。
	 * 如果超過設定時間沒有收到封包，且不符合例外條件（例如個人商店），則會強制踢除連線。
	 *
	 * <h3>監控機制</h3>
	 * <ul>
	 *   <li>定時器間隔：{@link Config#AUTOMATIC_KICK} 分鐘</li>
	 *   <li>封包計數器（{@code _checkct}）：每次收到封包時遞增</li>
	 *   <li>檢查邏輯：如果計數器為 0，表示在間隔時間內沒有收到封包</li>
	 * </ul>
	 *
	 * <h3>例外情況（不會被踢除）</h3>
	 * <ul>
	 *   <li>正在開設個人商店的玩家（{@link L1PcInstance#isPrivateShop()}）</li>
	 * </ul>
	 *
	 * <h3>踢除條件</h3>
	 * <ul>
	 *   <li>超過設定時間沒有收到封包（{@code _checkct} 為 0）</li>
	 *   <li>玩家在角色選擇畫面（{@code _activeChar} 為 null）</li>
	 *   <li>玩家已進入遊戲但不在個人商店</li>
	 * </ul>
	 *
	 * @see Config#AUTOMATIC_KICKTimer
	 * @see #kick()
	 * @see TimerTask
	 */
	class ClientThreadObserver extends TimerTask {
		/**
		 * 封包計數器
		 * <p>每次收到封包時遞增，每次定時檢查時歸零。
		 * 如果檢查時發現計數器為 0，表示在間隔時間內沒有收到封包。
		 */
		private int _checkct = 1;

		/** 斷線檢查間隔時間（毫秒） */
		private final int _disconnectTimeMillis;

		/**
		 * 建構客戶端超時監控器
		 *
		 * @param disconnectTimeMillis 斷線檢查間隔時間（毫秒）
		 */
		public ClientThreadObserver(int disconnectTimeMillis) {
			_disconnectTimeMillis = disconnectTimeMillis;
		}

		/**
		 * 啟動超時監控
		 *
		 * <p>將此監控任務註冊到共用的計時器，以固定間隔執行檢查。
		 */
		public void start() {
			_observerTimer.scheduleAtFixedRate(ClientThreadObserver.this, 0, _disconnectTimeMillis);
		}

		/**
		 * 定時檢查客戶端是否超時
		 *
		 * <p>檢查邏輯：
		 * <ol>
		 *   <li>如果連線已關閉（{@code _csocket} 為 null），取消監控任務</li>
		 *   <li>如果封包計數器 > 0，表示有收到封包，重置計數器並繼續監控</li>
		 *   <li>如果封包計數器 = 0，表示超時，根據條件決定是否踢除：
		 *     <ul>
		 *       <li>角色選擇畫面（{@code _activeChar} 為 null）：踢除</li>
		 *       <li>已進入遊戲但不在個人商店：踢除</li>
		 *       <li>正在個人商店：不踢除</li>
		 *     </ul>
		 *   </li>
		 * </ol>
		 */
		@Override
		public void run() {
			try {
				if (_csocket == null) {
					cancel();
					return;
				}

				if (_checkct > 0) {
					_checkct = 0;
					return;
				}

				if (_activeChar == null // 選角色之前
						|| _activeChar != null && !_activeChar.isPrivateShop()) { // 正在個人商店
					kick();
					_log.warning("一定時間沒有收到封包回應，所以強制切斷 (" + _hostname + ") 的連線。");
					Account.online(getAccount(), false);
					cancel();
					return;
				}
			} catch (Exception e) {
				_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
				cancel();
			}
		}

		/**
		 * 通知監控器已收到封包
		 *
		 * <p>此方法由主執行緒在每次收到封包時呼叫（{@code C_OPCODE_KEEPALIVE} 除外），
		 * 用於重置超時計時。
		 */
		public void packetReceived() {
			_checkct++;
		}
	}

	/**
	 * 發送封包至客戶端
	 *
	 * <p>此方法會將伺服器封包加密後發送給客戶端。發送流程：
	 * <ol>
	 *   <li>取得封包內容（{@link ServerBasePacket#getContent()}）</li>
	 *   <li>複製封包資料（避免加密影響原始資料）</li>
	 *   <li>使用 {@link Cipher} 加密資料</li>
	 *   <li>計算封包長度（資料長度 + 2 bytes 長度欄位）</li>
	 *   <li>寫入封包長度（2 bytes，Little Endian）</li>
	 *   <li>寫入加密後的封包資料</li>
	 *   <li>刷新輸出緩衝區</li>
	 * </ol>
	 *
	 * <p>此方法使用 synchronized 確保執行緒安全，避免多個執行緒同時發送封包造成資料混亂。
	 *
	 * @param packet 要發送的伺服器封包
	 * @see ServerBasePacket
	 * @see Cipher#encrypt(byte[])
	 * @see PacketOutput#sendPacket(ServerBasePacket)
	 */
	@Override
	public void sendPacket(ServerBasePacket packet) {
		synchronized (this) {
			try {
				byte content[] = packet.getContent();
				byte data[] = Arrays.copyOf(content, content.length);
				_cipher.encrypt(data);
				int length = data.length + 2;

				_out.write(length & 0xff);
				_out.write(length >> 8 & 0xff);
				_out.write(data);
				_out.flush();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 關閉客戶端連線
	 *
	 * <p>此方法會關閉 Socket 連線並將參考設為 null。
	 * 通常在斷線處理的 finally 區塊中呼叫。
	 *
	 * @throws IOException 如果關閉連線時發生 I/O 錯誤
	 */
	public void close() throws IOException {
		if (_csocket != null) {
			_csocket.close();
			_csocket = null;
		}
	}

	/**
	 * 設定目前活動的角色
	 *
	 * <p>當玩家選擇角色進入遊戲時，或是登出時設為 null。
	 *
	 * @param pc 角色實例，或 null 表示沒有活動角色
	 */
	public void setActiveChar(L1PcInstance pc) {
		_activeChar = pc;
	}

	/**
	 * 取得目前活動的角色
	 *
	 * @return 角色實例，如果尚未選擇角色或已登出則回傳 null
	 */
	public L1PcInstance getActiveChar() {
		return _activeChar;
	}

	/**
	 * 設定此連線關聯的遊戲帳號
	 *
	 * <p>通常在帳號登入成功後設定。
	 *
	 * @param account 帳號實例
	 */
	public void setAccount(Account account) {
		_account = account;
	}

	/**
	 * 取得此連線關聯的遊戲帳號
	 *
	 * @return 帳號實例，如果尚未登入則回傳 null
	 */
	public Account getAccount() {
		return _account;
	}

	/**
	 * 取得帳號名稱
	 *
	 * @return 帳號名稱，如果尚未登入則回傳 null
	 */
	public String getAccountName() {
		if (_account == null) {
			return null;
		}
		return _account.getName();
	}

	/**
	 * 角色登出遊戲的處理
	 *
	 * <p>此靜態方法處理角色登出時的所有清理工作，包括：
	 *
	 * <h3>1. 死亡處理</h3>
	 * <ul>
	 *   <li>如果角色死亡，傳送回重生點</li>
	 *   <li>恢復少量 HP（等級值）</li>
	 *   <li>設定飽食度為 40</li>
	 * </ul>
	 *
	 * <h3>2. 清理進行中的活動</h3>
	 * <ul>
	 *   <li>終止交易</li>
	 *   <li>終止決鬥</li>
	 *   <li>離開組隊</li>
	 *   <li>離開聊天組隊</li>
	 * </ul>
	 *
	 * <h3>3. 清理寵物和召喚物</h3>
	 * <ul>
	 *   <li>寵物：停止飽食度計時器，掉落攜帶的道具，從世界移除</li>
	 *   <li>召喚物：通知其他玩家召喚物消失</li>
	 *   <li>魔法娃娃：從世界移除</li>
	 *   <li>跟隨者：重新生成為 NPC</li>
	 * </ul>
	 *
	 * <h3>4. 清理副本和狀態</h3>
	 * <ul>
	 *   <li>從屠龍副本移除玩家記錄</li>
	 *   <li>儲存魔法狀態（Buff）</li>
	 *   <li>清理技能效果計時器</li>
	 *   <li>檢查離開變身競賽</li>
	 * </ul>
	 *
	 * <h3>5. 儲存資料</h3>
	 * <ul>
	 *   <li>停止角色監控執行緒</li>
	 *   <li>設定線上狀態為離線</li>
	 *   <li>儲存角色資料和道具資料</li>
	 * </ul>
	 *
	 * @param pc 要登出的角色
	 * @see L1PcInstance#save()
	 * @see L1PcInstance#saveInventory()
	 * @see L1Trade#TradeCancel(L1PcInstance)
	 * @see Getback#GetBack_Location(L1PcInstance, boolean)
	 */
	public static void quitGame(L1PcInstance pc) {
		// 如果死掉回到城中，設定飽食度
		if (pc.isDead()) {
			try {
				Thread.sleep(2000);// 暫停該執行續，優先權讓給expmonitor
				int[] loc = Getback.GetBack_Location(pc, true);
				pc.setX(loc[0]);
				pc.setY(loc[1]);
				pc.setMap((short) loc[2]);
				pc.setCurrentHp(pc.getLevel());
				pc.set_food(40);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// 終止交易
		if (pc.getTradeID() != 0) { // トレード中
			L1Trade trade = new L1Trade();
			trade.TradeCancel(pc);
		}

		// 終止決鬥
		if (pc.getFightId() != 0) {
			L1PcInstance fightPc = (L1PcInstance) L1World.getInstance().findObject(pc.getFightId());
			pc.setFightId(0);
			if (fightPc != null) {
				fightPc.setFightId(0);
				fightPc.sendPackets(new S_PacketBox(S_PacketBox.MSG_DUEL, 0, 0));
			}
		}

		// 離開組隊
		if (pc.isInParty()) { // 如果有組隊
			pc.getParty().leaveMember(pc);
		}

		// TODO: 離開聊天組隊(?)
		if (pc.isInChatParty()) { // 如果在聊天組隊中(?)
			pc.getChatParty().leaveMember(pc);
		}

		// 移除世界地圖上的寵物
		// 變更召喚怪物的名稱
		for (L1NpcInstance petNpc : pc.getPetList().values()) {
			if (petNpc instanceof L1PetInstance) {
				L1PetInstance pet = (L1PetInstance) petNpc;
				// 停止飽食度計時
				pet.stopFoodTimer(pet);
				pet.dropItem();
				pc.getPetList().remove(pet.getId());
				pet.deleteMe();
			} else if (petNpc instanceof L1SummonInstance) {
				L1SummonInstance summon = (L1SummonInstance) petNpc;
				for (L1PcInstance visiblePc : L1World.getInstance().getVisiblePlayer(summon)) {
					visiblePc.sendPackets(new S_SummonPack(summon, visiblePc,false));
				}
			}
		}

		// 移除世界地圖上的魔法娃娃
		for (L1DollInstance doll : pc.getDollList().values())
			doll.deleteDoll();

		// 重新建立跟隨者
		for (L1FollowerInstance follower : pc.getFollowerList().values()) {
			follower.setParalyzed(true);
			follower.spawn(follower.getNpcTemplate().get_npcId(),follower.getX(), follower.getY(), follower.getHeading(),follower.getMapId());
			follower.deleteMe();
		}

		// 刪除屠龍副本此玩家紀錄
		if (pc.getPortalNumber() != -1) {
			L1DragonSlayer.getInstance().removePlayer(pc, pc.getPortalNumber());
		}

		// 儲存魔法狀態
		CharBuffTable.DeleteBuff(pc);
		CharBuffTable.SaveBuff(pc);
		pc.clearSkillEffectTimer();
		l1j.server.server.model.game.L1PolyRace.getInstance().checkLeaveGame(pc);

		// 停止玩家的偵測
		pc.stopEtcMonitor();
		// 設定線上狀態為下線
		pc.setOnlineStatus(0);
		// 設定帳號為下線
		//Account account = Account.load(pc.getAccountName());
		//Account.online(account, false);
		// 設定帳號的角色為下線
		Account account = Account.load(pc.getAccountName());
		Account.OnlineStatus(account, false);

		try {
			pc.save();
			pc.saveInventory();
		} catch (Exception e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}
}
