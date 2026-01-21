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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import l1j.server.server.utils.StreamUtil;
import l1j.server.telnet.command.TelnetCommandExecutor;
import l1j.server.telnet.command.TelnetCommandResult;

/**
 * Telnet 連線處理器
 * <p>處理單一 Telnet 客戶端連線，接收指令並執行，然後回傳結果。
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>建立獨立執行緒處理每個 Telnet 連線</li>
 *   <li>接收客戶端輸入的指令 (以換行符號分隔)</li>
 *   <li>透過 {@link TelnetCommandExecutor} 執行指令</li>
 *   <li>將執行結果回傳給客戶端</li>
 *   <li>處理連線斷開和例外狀況</li>
 * </ul>
 *
 * <h3>通訊協定:</h3>
 * <p><b>請求格式:</b> 客戶端發送指令 + "\n"
 * <p><b>回應格式:</b>
 * <pre>
 * [狀態碼] [狀態訊息]\r\n
 * [執行結果]\r\n
 * </pre>
 *
 * <h3>執行流程:</h3>
 * <ol>
 *   <li>接受客戶端 Socket 連線</li>
 *   <li>建立獨立執行緒處理此連線</li>
 *   <li>進入讀取迴圈，逐行讀取指令</li>
 *   <li>呼叫 {@link TelnetCommandExecutor} 執行指令</li>
 *   <li>將結果寫回客戶端</li>
 *   <li>客戶端斷線或發生錯誤時關閉連線</li>
 * </ol>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * // 由 TelnetServer 自動建立
 * Socket clientSocket = serverSocket.accept();
 * new TelnetConnection(clientSocket); // 自動啟動處理執行緒
 * </pre>
 *
 * @see TelnetServer
 * @see TelnetCommandExecutor
 * @see TelnetCommandResult
 */
public class TelnetConnection {
	/**
	 * Telnet 連線處理執行緒
	 * <p>獨立執行緒負責處理單一客戶端連線的指令接收與執行。
	 */
	private class ConnectionThread extends Thread {
		/** 客戶端 Socket 連線 */
		private Socket _socket;

		/**
		 * 建構連線處理執行緒
		 *
		 * @param sock 客戶端 Socket 連線
		 */
		public ConnectionThread(Socket sock) {
			_socket = sock;
		}

		/**
		 * 執行 Telnet 連線處理主迴圈
		 * <p>持續讀取客戶端指令並執行，直到連線斷開。
		 *
		 * <h3>處理流程:</h3>
		 * <ol>
		 *   <li><b>建立輸入輸出串流:</b>
		 *     <ul>
		 *       <li>InputStreamReader + BufferedReader：讀取客戶端指令</li>
		 *       <li>OutputStreamWriter + BufferedWriter：寫回執行結果</li>
		 *     </ul>
		 *   </li>
		 *   <li><b>進入讀取迴圈:</b> 逐行讀取客戶端指令 (readLine)</li>
		 *   <li><b>執行指令:</b> 透過 {@link TelnetCommandExecutor#execute(String)} 執行</li>
		 *   <li><b>回傳結果:</b> 寫入狀態碼、狀態訊息及執行結果</li>
		 *   <li><b>清理資源:</b> 關閉串流及 Socket 連線</li>
		 * </ol>
		 *
		 * <h3>回應格式:</h3>
		 * <pre>
		 * [result.getCode()] [result.getCodeMessage()]\r\n
		 * [result.getResult()]\r\n
		 * </pre>
		 *
		 * <h3>終止條件:</h3>
		 * <ul>
		 *   <li>客戶端斷線 (readLine() 返回 null)</li>
		 *   <li>發生 IOException</li>
		 * </ul>
		 *
		 * @see TelnetCommandExecutor#execute(String)
		 * @see TelnetCommandResult
		 */
		@Override
		public void run() {
			InputStreamReader isr = null;
			BufferedReader in = null;
			OutputStreamWriter osw = null;
			BufferedWriter out = null;
			try {
				isr = new InputStreamReader(_socket.getInputStream());
				in = new BufferedReader(isr);
				osw = new OutputStreamWriter(_socket.getOutputStream());
				out = new BufferedWriter(osw);

				String cmd = null;
				while (null != (cmd = in.readLine())) {
					TelnetCommandResult result = TelnetCommandExecutor
							.getInstance().execute(cmd);
					out.write(result.getCode() + " " + result.getCodeMessage()
							+ "\r\n");
					out.write(result.getResult() + "\r\n");
					out.flush();
					// // for debug
					// System.out.println(result.getCode() + " " +
					// result.getCodeMessage());
					// System.out.println(result.getResult());
				}
			} catch (IOException e) {
				StreamUtil.close(isr, in);
				StreamUtil.close(osw, out);
			}
			try {
				_socket.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * 建構 Telnet 連線處理器
	 * <p>建立並啟動獨立執行緒處理此 Socket 連線。
	 *
	 * <h3>執行流程:</h3>
	 * <ol>
	 *   <li>建立 {@link ConnectionThread} 執行緒</li>
	 *   <li>傳入客戶端 Socket 連線</li>
	 *   <li>啟動執行緒 (start())</li>
	 *   <li>建構子立即返回，執行緒在背景處理連線</li>
	 * </ol>
	 *
	 * <p><b>注意:</b> 建構子會立即返回，實際的連線處理在獨立執行緒中執行。
	 *
	 * @param sock 客戶端 Socket 連線
	 */
	public TelnetConnection(Socket sock) {
		new ConnectionThread(sock).start();
	}
}
