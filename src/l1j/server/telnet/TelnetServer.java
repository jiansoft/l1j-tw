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
package l1j.server.telnet;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import l1j.server.Config;

/**
 * Telnet 遠端管理伺服器
 * <p>提供遠端 Telnet 連線介面，允許管理員透過 Telnet 協定執行伺服器管理指令。
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>監聽指定埠號接受 Telnet 連線</li>
 *   <li>為每個連線建立獨立的 {@link TelnetConnection} 處理</li>
 *   <li>提供遠端伺服器管理介面</li>
 *   <li>支援多個同時連線</li>
 * </ul>
 *
 * <h3>配置說明:</h3>
 * <ul>
 *   <li><b>埠號:</b> 由 {@link Config#TELNET_SERVER_PORT} 設定</li>
 *   <li><b>預設埠號:</b> 通常為 23 (標準 Telnet 埠) 或自訂埠號</li>
 * </ul>
 *
 * <h3>安全性考量:</h3>
 * <p><b>警告:</b> Telnet 協定本身不加密，建議：
 * <ul>
 *   <li>僅在內網或信任環境中使用</li>
 *   <li>透過防火牆限制來源 IP</li>
 *   <li>使用強密碼驗證</li>
 *   <li>考慮使用 SSH 替代 Telnet</li>
 * </ul>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * // 啟動 Telnet 伺服器
 * TelnetServer.getInstance().start();
 *
 * // 連線方式
 * telnet localhost [port]
 * </pre>
 *
 * @see TelnetConnection
 * @see Config#TELNET_SERVER_PORT
 */
public class TelnetServer {
	/** Singleton 實例 */
	private static TelnetServer _instance;

	/**
	 * Telnet 伺服器執行緒
	 * <p>獨立執行緒監聽 Telnet 連線請求，並為每個連線建立處理器。
	 */
	private class ServerThread extends Thread {
		/** 伺服器 Socket */
		ServerSocket _sock;

		/**
		 * 執行 Telnet 伺服器主迴圈
		 * <p>監聽指定埠號並接受 Telnet 連線請求。
		 *
		 * <h3>執行流程:</h3>
		 * <ol>
		 *   <li>建立 ServerSocket 監聽 {@link Config#TELNET_SERVER_PORT}</li>
		 *   <li>進入無限迴圈等待連線</li>
		 *   <li>接受連線後建立 {@link TelnetConnection} 處理</li>
		 *   <li>若發生錯誤則關閉 ServerSocket</li>
		 * </ol>
		 *
		 * <p><b>注意:</b> 此方法會阻塞執行緒直到伺服器停止或發生錯誤。
		 */
		@Override
		public void run() {
			try {
				_sock = new ServerSocket(Config.TELNET_SERVER_PORT);

				while (true) {
					Socket sock = _sock.accept();
					new TelnetConnection(sock);
				}
			} catch (IOException e) {
			}
			try {
				_sock.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * 私有建構子，防止外部實例化
	 * <p>使用 Singleton 模式，透過 {@link #getInstance()} 取得實例。
	 */
	private TelnetServer() {
	}

	/**
	 * 啟動 Telnet 伺服器
	 * <p>建立並啟動伺服器執行緒，開始監聽 Telnet 連線。
	 *
	 * <h3>啟動效果:</h3>
	 * <ul>
	 *   <li>建立新的 {@link ServerThread} 執行緒</li>
	 *   <li>執行緒開始監聽 {@link Config#TELNET_SERVER_PORT}</li>
	 *   <li>接受並處理所有 Telnet 連線請求</li>
	 * </ul>
	 *
	 * <p><b>注意:</b> 此方法立即返回，伺服器在背景執行緒中運行。
	 */
	public void start() {
		new ServerThread().start();
	}

	/**
	 * 取得 Singleton 實例
	 * <p>使用延遲初始化模式，首次呼叫時建立實例。
	 *
	 * @return TelnetServer 實例
	 */
	public static TelnetServer getInstance() {
		if (_instance == null) {
			_instance = new TelnetServer();
		}
		return _instance;
	}
}
