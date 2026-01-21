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
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Base64;

import l1j.server.Config;
import l1j.server.L1DatabaseFactory;
import l1j.server.server.utils.SQLUtil;

/**
 * 登入驗證工具類別
 * <p>負責處理玩家帳號的密碼驗證及自動建立帳號功能。
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>驗證帳號密碼是否正確</li>
 *   <li>支援自動建立新帳號 (依據 {@link Config#AUTO_CREATE_ACCOUNTS} 設定)</li>
 *   <li>使用 SHA-1 雜湊演算法保護密碼安全</li>
 *   <li>密碼以 Base64 編碼儲存於資料庫</li>
 *   <li>記錄登入來源 IP 與主機名稱</li>
 * </ul>
 *
 * <h3>密碼安全機制:</h3>
 * <ul>
 *   <li><b>雜湊演算法:</b> SHA-1 (Secure Hash Algorithm 1)</li>
 *   <li><b>編碼方式:</b> Base64 編碼儲存</li>
 *   <li><b>比對方式:</b> 逐位元組比對雜湊值</li>
 *   <li><b>字元編碼:</b> UTF-8</li>
 * </ul>
 *
 * <h3>自動建立帳號:</h3>
 * <p>當 {@link Config#AUTO_CREATE_ACCOUNTS} 設為 {@code true} 時:
 * <ul>
 *   <li>若帳號不存在，自動建立新帳號</li>
 *   <li>預設存取等級為 0 (一般玩家)</li>
 *   <li>記錄首次登入的 IP 與主機名稱</li>
 *   <li>lastactive 初始化為 0</li>
 * </ul>
 *
 * <h3>驗證流程:</h3>
 * <ol>
 *   <li>將輸入密碼以 UTF-8 編碼並計算 SHA-1 雜湊</li>
 *   <li>從資料庫查詢帳號的密碼雜湊值</li>
 *   <li>若帳號不存在:
 *     <ul>
 *       <li>AUTO_CREATE_ACCOUNTS=true → 建立新帳號並返回 true</li>
 *       <li>AUTO_CREATE_ACCOUNTS=false → 返回 false</li>
 *     </ul>
 *   </li>
 *   <li>若帳號存在，逐位元組比對兩個雜湊值</li>
 *   <li>雜湊值完全相同返回 true，否則返回 false</li>
 * </ol>
 *
 * <h3>安全性考量:</h3>
 * <p><b>注意:</b> SHA-1 已不再被認為是安全的雜湊演算法，建議升級為 SHA-256 或更強的演算法。
 *
 * @see Config#AUTO_CREATE_ACCOUNTS
 * @see MessageDigest
 * @see Base64
 */
public class Logins {
	/** 日誌記錄器 */
	private static Logger _log = Logger.getLogger(Logins.class.getName());

	/**
	 * 驗證帳號密碼是否正確
	 * <p>執行密碼雜湊比對，並支援自動建立不存在的帳號。
	 *
	 * <h3>密碼驗證步驟:</h3>
	 * <ol>
	 *   <li><b>計算輸入密碼雜湊:</b>
	 *     <ul>
	 *       <li>將密碼字串轉為 UTF-8 位元組陣列</li>
	 *       <li>使用 SHA-1 演算法計算雜湊值</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>查詢資料庫:</b>
	 *     <ul>
	 *       <li>從 accounts 資料表查詢帳號的密碼欄位</li>
	 *       <li>使用 Base64 解碼取得儲存的雜湊值</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>處理帳號不存在:</b>
	 *     <ul>
	 *       <li>若 AUTO_CREATE_ACCOUNTS=true，建立新帳號並返回 true</li>
	 *       <li>若 AUTO_CREATE_ACCOUNTS=false，記錄警告並返回 false</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>比對雜湊值:</b>
	 *     <ul>
	 *       <li>逐位元組比對輸入密碼雜湊與資料庫雜湊</li>
	 *       <li>所有位元組相同返回 true，否則返回 false</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 *
	 * <h3>自動建立帳號:</h3>
	 * <p>當帳號不存在且 {@code AUTO_CREATE_ACCOUNTS=true} 時，建立新帳號並設定:
	 * <pre>
	 * login        = 帳號名稱
	 * password     = Base64(SHA1(密碼))
	 * lastactive   = 0
	 * access_level = 0 (一般玩家)
	 * ip           = 登入來源 IP
	 * host         = 登入來源主機名稱
	 * </pre>
	 *
	 * <h3>例外處理:</h3>
	 * <ul>
	 *   <li><b>SQLException:</b> 資料庫連線或查詢錯誤</li>
	 *   <li><b>NoSuchAlgorithmException:</b> SHA-1 演算法不可用 (不應發生)</li>
	 *   <li><b>UnsupportedEncodingException:</b> UTF-8 編碼不支援 (不應發生)</li>
	 * </ul>
	 *
	 * <h3>日誌記錄:</h3>
	 * <ul>
	 *   <li>INFO: 記錄連線嘗試 ("Connect from : " + account)</li>
	 *   <li>FINE: 帳號存在 ("account exists")</li>
	 *   <li>INFO: 建立新帳號 ("created new account for " + account)</li>
	 *   <li>WARNING: 帳號不存在且未啟用自動建立 ("account missing for user " + account)</li>
	 *   <li>WARNING: 密碼比對失敗 ("could not check password")</li>
	 *   <li>SEVERE: 資料庫或加密演算法錯誤</li>
	 * </ul>
	 *
	 * @param account 帳號名稱
	 * @param password 明文密碼
	 * @param ip 登入來源 IP 位址
	 * @param host 登入來源主機名稱
	 * @return 密碼驗證結果 (true: 驗證成功或成功建立新帳號, false: 驗證失敗或帳號不存在)
	 * @throws IOException 輸入輸出例外
	 * @see Config#AUTO_CREATE_ACCOUNTS
	 * @see MessageDigest#getInstance(String)
	 * @see Base64#getDecoder()
	 * @see Base64#getEncoder()
	 */
	public static boolean loginValid(String account, String password, String ip, String host) throws IOException {
		boolean flag1 = false;
		_log.info("Connect from : " + account);

		Connection con = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;
		try {
			byte abyte1[];
			byte abyte2[];
			MessageDigest messagedigest = MessageDigest.getInstance("SHA");
			byte abyte0[] = password.getBytes("UTF-8");
			abyte1 = messagedigest.digest(abyte0);
			abyte2 = null;

			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement("SELECT password FROM accounts WHERE login=? LIMIT 1");
			pstm.setString(1, account);
			rs = pstm.executeQuery();
			if (rs.next()) {
				abyte2 = Base64.getDecoder().decode(rs.getString(1));
				_log.fine("account exists");
			}
			SQLUtil.close(rs);
			SQLUtil.close(pstm);
			SQLUtil.close(con);

			if (abyte2 == null) {
				if (Config.AUTO_CREATE_ACCOUNTS) {
					con = L1DatabaseFactory.getInstance().getConnection();
					pstm = con.prepareStatement("INSERT INTO accounts SET login=?,password=?,lastactive=?,access_level=?,ip=?,host=?");
					pstm.setString(1, account);
					pstm.setString(2, Base64.getEncoder().encodeToString(abyte1));
					pstm.setLong(3, 0L);
					pstm.setInt(4, 0);
					pstm.setString(5, ip);
					pstm.setString(6, host);
					pstm.execute();
					_log.info("created new account for " + account);
					return true;
				} else {
					_log.warning("account missing for user " + account);
					return false;
				}
			}

			try {
				flag1 = true;
				int i = 0;
				do {
					if (i >= abyte2.length) {
						break;
					}
					if (abyte1[i] != abyte2[i]) {
						flag1 = false;
						break;
					}
					i++;
				} while (true);
			} catch (Exception e) {
				_log.warning("could not check password:" + e);
				flag1 = false;
			}
		} catch (SQLException e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (UnsupportedEncodingException e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} finally {
			SQLUtil.close(rs);
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
		return flag1;
	}
}