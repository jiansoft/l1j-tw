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
package l1j.server.server.clientpackets;

import java.util.logging.Logger;

import l1j.server.Config;
import l1j.server.server.Account;
import l1j.server.server.AccountAlreadyLoginException;
import l1j.server.server.ClientThread;
import l1j.server.server.GameServerFullException;
import l1j.server.server.LoginController;
import l1j.server.server.model.L1CharList;
import l1j.server.server.serverpackets.S_LoginResult;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket

/**
 * 處理 beanfun相關的帳號密碼取得登入與登出
 */
public class C_AuthLogin extends ClientBasePacket {

	private static final String C_AUTH_LOGIN = "[C] C_AuthLogin";
	private static Logger _log = Logger.getLogger(C_AuthLogin.class.getName());

	public C_AuthLogin(byte[] decrypt, ClientThread client) {
		super(decrypt);
		int action = readC();

		_log.info("C_AuthLogin: action=0x" + Integer.toHexString(action) + " (" + action + "), 封包長度=" + decrypt.length);

		switch (action) {
		case 0x06: // 登入請求
			String accountName = readS().toLowerCase();
			String password = readS();
			String ip = client.getIp();
			String host = client.getHostname();

			_log.finest("Request AuthLogin from user : " + accountName);
			if (!Config.ALLOW_2PC) {
				for (ClientThread tempClient : LoginController.getInstance().getAllAccounts()) {
					if (ip.equalsIgnoreCase(tempClient.getIp())) {
						_log.info("拒絕 2P 登入。account=" + accountName + " host=" + host);
						client.sendPacket(new S_LoginResult(S_LoginResult.REASON_USER_OR_PASS_WRONG));
						return;
					}
				}
			}

			_log.info("C_AuthLogin: 開始載入帳號 " + accountName);
			Account account = Account.load(accountName);
			if (account == null) {
				if (Config.AUTO_CREATE_ACCOUNTS) {
					_log.info("C_AuthLogin: 帳號不存在，自動創建帳號 " + accountName);
					account = Account.create(accountName, password, ip, host);
				} else {
					_log.warning("account missing for user " + accountName);
				}
			} else {
				_log.info("C_AuthLogin: 帳號已存在，載入成功 " + accountName);
			}

			_log.info("C_AuthLogin: 開始驗證密碼 accountName=" + accountName);
			if (account == null || !account.validatePassword(password)) {
				_log.info("C_AuthLogin: 密碼驗證失敗 accountName=" + accountName);
				client.sendPacket(new S_LoginResult(S_LoginResult.REASON_USER_OR_PASS_WRONG));
				return;
			}
			_log.info("C_AuthLogin: 密碼驗證成功 accountName=" + accountName);

			if (account.isOnlined()) {
				_log.info("C_AuthLogin: 帳號已在線上 accountName=" + accountName);
				client.sendPacket(new S_LoginResult(S_LoginResult.REASON_ACCOUNT_ALREADY_EXISTS));//原碼 REASON_ACCOUNT_IN_USE
				return;
			}
			if (account.isBanned()) { // BANアカウント
				_log.info("禁止登入的帳號嘗試登入。account=" + accountName + " host="+ host);
				client.sendPacket(new S_LoginResult(S_LoginResult.REASON_USER_OR_PASS_WRONG));
				return;
			}

			try {
				_log.info("C_AuthLogin: 開始執行 LoginController.login() accountName=" + accountName);
				LoginController.getInstance().login(client, account);
				_log.info("C_AuthLogin: LoginController.login() 成功 accountName=" + accountName);

				_log.info("C_AuthLogin: 更新最後活動時間 accountName=" + accountName);
				Account.updateLastActive(account, ip); // 更新最後一次登入的時間與IP

				_log.info("C_AuthLogin: 設置帳號到 ClientThread accountName=" + accountName);
				client.setAccount(account);

				_log.info("C_AuthLogin: 發送 S_LoginResult(REASON_LOGIN_OK) accountName=" + accountName);
				client.sendPacket(new S_LoginResult(S_LoginResult.REASON_LOGIN_OK));

				//client.sendPacket(new S_CommonNews());
				_log.info("C_AuthLogin: 發送角色列表 L1CharList accountName=" + accountName);
				new L1CharList(client);

				_log.info("C_AuthLogin: 標記帳號在線 accountName=" + accountName);
				Account.online(account, true);

				_log.info("C_AuthLogin: 登入流程完成 accountName=" + accountName);
			} catch (GameServerFullException e) {
				client.kick();
				_log.info("線上人數已經飽和，切斷 (" + client.getHostname() + ") 的連線。");
				return;
			} catch (AccountAlreadyLoginException e) {
				client.kick();
				_log.info("同個帳號已經登入，切斷 (" + client.getHostname() + ") 的連線。");
				return;
			} catch (Exception e) {
				_log.severe("C_AuthLogin: 登入過程發生異常 accountName=" + accountName + " error=" + e.getMessage());
				e.printStackTrace();
				return;
			}
			break;
		case 0x0b:  // 重新選擇角色
			break;
		case 0x1c:  // 登出請求
			break;
		default:
			_log.warning("C_AuthLogin: 未知的 action=0x" + Integer.toHexString(action) + " (" + action + ")");
			// 嘗試輸出原始封包內容（十六進位）
			StringBuilder sb = new StringBuilder("封包內容: ");
			for (int i = 0; i < Math.min(decrypt.length, 32); i++) {
				sb.append(String.format("%02X ", decrypt[i] & 0xFF));
			}
			_log.warning(sb.toString());
			break;
		}
	}

	@Override
	public String getType() {
		return C_AUTH_LOGIN;
	}

}