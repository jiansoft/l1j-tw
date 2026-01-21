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

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.Config;
import l1j.server.server.ClientThread;

/**
 * 客戶端封包的基礎抽象類別
 *
 * <p>此類別提供解析客戶端傳送給伺服器封包的基礎功能。所有客戶端封包處理器都應該繼承此類別。
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>提供各種資料型態的讀取方法 (byte, short, int, double, String)</li>
 *   <li>自動處理封包的字節序 (Little-Endian)</li>
 *   <li>支援多語言字串解碼 (根據 CLIENT_LANGUAGE_CODE 設定)</li>
 *   <li>自動管理讀取位置指標</li>
 * </ul>
 *
 * <h3>讀取方法說明:</h3>
 * <ul>
 *   <li>{@code readC()} - 讀取 1 byte (8-bit)</li>
 *   <li>{@code readH()} - 讀取 2 bytes (16-bit, short)</li>
 *   <li>{@code readCH()} - 讀取 3 bytes (24-bit)</li>
 *   <li>{@code readD()} - 讀取 4 bytes (32-bit, int)</li>
 *   <li>{@code readF()} - 讀取 8 bytes (64-bit, double)</li>
 *   <li>{@code readS()} - 讀取字串 (以 null 終止)</li>
 *   <li>{@code readByte()} - 讀取剩餘所有 bytes</li>
 * </ul>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * public class C_MoveChar extends ClientBasePacket {
 *     public C_MoveChar(byte[] decrypt, ClientThread client) {
 *         super(decrypt);
 *         int locX = readH();      // 讀取 X 座標 (2 bytes)
 *         int locY = readH();      // 讀取 Y 座標 (2 bytes)
 *         int heading = readC();   // 讀取方向 (1 byte)
 *
 *         // 處理移動邏輯...
 *     }
 * }
 * </pre>
 *
 * <h3>封包結構:</h3>
 * <ol>
 *   <li>所有封包都以 Opcode 開頭 (1 byte, 由建構式自動跳過)</li>
 *   <li>後續資料依各封包定義依序讀取</li>
 *   <li>讀取位置由內部 {@code _off} 變數自動管理</li>
 * </ol>
 *
 * <h3>字串編碼:</h3>
 * <p>根據 CLIENT_LANGUAGE_CODE 設定自動處理不同語言的字串編碼:
 * <ul>
 *   <li>0: US (UTF-8)</li>
 *   <li>3: Taiwan (BIG5)</li>
 *   <li>4: Japan (SJIS)</li>
 *   <li>5: China (GBK)</li>
 * </ul>
 *
 * @see l1j.server.server.Opcodes
 * @see l1j.server.server.serverpackets.ServerBasePacket
 * @see l1j.server.server.PacketHandler
 */
public abstract class ClientBasePacket {
	private static Logger _log = Logger.getLogger(ClientBasePacket.class.getName());

	private static final String CLIENT_LANGUAGE_CODE = Config.CLIENT_LANGUAGE_CODE;

	/** 解密後的封包資料 */
	private byte _decrypt[];

	/** 目前讀取位置的偏移量 */
	private int _off;

	/**
	 * 建構式 - 從 byte 陣列建立客戶端封包
	 * <p>自動跳過第一個 byte (Opcode), 將讀取位置設定為 1
	 *
	 * @param abyte0 解密後的封包資料 (包含 Opcode)
	 */
	public ClientBasePacket(byte abyte0[]) {
		_log.finest("type=" + getType() + ", len=" + abyte0.length);
		_decrypt = abyte0;
		_off = 1;
	}

	/**
	 * 建構式 - 從 ByteBuffer 建立客戶端封包
	 * <p>此建構式為空實作, 主要用於特殊情況
	 *
	 * @param bytebuffer ByteBuffer 物件
	 * @param clientthread 客戶端連線執行緒
	 */
	public ClientBasePacket(ByteBuffer bytebuffer, ClientThread clientthread) {
	}

	/**
	 * 讀取 4 bytes (32-bit integer, DWORD)
	 * <p>使用 Little-Endian 字節序解析
	 *
	 * @return 讀取的整數值 (32-bit)
	 */
	public int readD() {
		int i = _decrypt[_off++] & 0xff;
		i |= _decrypt[_off++] << 8 & 0xff00;
		i |= _decrypt[_off++] << 16 & 0xff0000;
		i |= _decrypt[_off++] << 24 & 0xff000000;
		return i;
	}

	/**
	 * 讀取 1 byte (8-bit, BYTE)
	 *
	 * @return 讀取的整數值 (0-255)
	 */
	public int readC() {
		int i = _decrypt[_off++] & 0xff;
		return i;
	}

	/**
	 * 讀取 2 bytes (16-bit short, WORD)
	 * <p>使用 Little-Endian 字節序解析
	 *
	 * @return 讀取的整數值 (16-bit)
	 */
	public int readH() {
		int i = _decrypt[_off++] & 0xff;
		i |= _decrypt[_off++] << 8 & 0xff00;
		return i;
	}

	/**
	 * 讀取 3 bytes (24-bit)
	 * <p>使用 Little-Endian 字節序解析
	 * <p>此方法用於特殊的 24-bit 整數格式
	 *
	 * @return 讀取的整數值 (24-bit)
	 */
	public int readCH() {
		int i = _decrypt[_off++] & 0xff;
		i |= _decrypt[_off++] << 8 & 0xff00;
		i |= _decrypt[_off++] << 16 & 0xff0000;
		return i;
	}

	/**
	 * 讀取 8 bytes (64-bit double, IEEE 754)
	 * <p>使用 Little-Endian 字節序解析
	 * <p>將 8 bytes 組合成 long, 再轉換為 double
	 *
	 * @return 讀取的浮點數值 (64-bit double)
	 */
	public double readF() {
		long l = _decrypt[_off++] & 0xff;
		l |= _decrypt[_off++] << 8 & 0xff00;
		l |= _decrypt[_off++] << 16 & 0xff0000;
		l |= _decrypt[_off++] << 24 & 0xff000000;
		l |= (long) _decrypt[_off++] << 32 & 0xff00000000L;
		l |= (long) _decrypt[_off++] << 40 & 0xff0000000000L;
		l |= (long) _decrypt[_off++] << 48 & 0xff000000000000L;
		l |= (long) _decrypt[_off++] << 56 & 0xff00000000000000L;
		return Double.longBitsToDouble(l);
	}

	/**
	 * 讀取字串 (以 null 字節終止)
	 * <p>字串編碼依據 CLIENT_LANGUAGE_CODE 設定:
	 * <ul>
	 *   <li>0: US (UTF-8)</li>
	 *   <li>3: Taiwan (BIG5)</li>
	 *   <li>4: Japan (SJIS)</li>
	 *   <li>5: China (GBK)</li>
	 * </ul>
	 * <p>讀取從目前位置到 null 字節 (0x00) 之間的所有資料
	 * <p>讀取後自動移動偏移量到字串結尾之後 (包含 null 字節)
	 *
	 * @return 讀取的字串 (不包含 null 終止字元), 發生錯誤時返回 null
	 */
	public String readS() {
		String s = null;
		try {
			s = new String(_decrypt, _off, _decrypt.length - _off, CLIENT_LANGUAGE_CODE);
			s = s.substring(0, s.indexOf('\0'));
			_off += s.getBytes(CLIENT_LANGUAGE_CODE).length + 1;
		} catch (Exception e) {
			_log.log(Level.SEVERE, "OpCode=" + (_decrypt[0] & 0xff), e);
		}
		return s;
	}

	/**
	 * 讀取剩餘所有 bytes
	 * <p>讀取從目前位置到封包結尾的所有資料
	 * <p>讀取後將偏移量移動到封包結尾
	 * <p>常用於讀取封包中的原始二進制資料或未知長度的資料
	 *
	 * @return 剩餘的所有 bytes, 發生錯誤時返回空陣列
	 */
	public byte[] readByte() {
		byte[] result = new byte[_decrypt.length - _off];
		try {
			System.arraycopy(_decrypt, _off, result, 0, _decrypt.length - _off);
			_off = _decrypt.length;
		} catch (Exception e) {
			_log.log(Level.SEVERE, "OpCode=" + (_decrypt[0] & 0xff), e);
		}
		return result;
	}

	/**
	 * 取得客戶端封包的類型名稱
	 * <p>格式: "[C] 類別名稱" (例如: "[C] C_MoveChar")
	 * <p>用於日誌記錄和除錯
	 *
	 * @return 封包類型字串
	 */
	public String getType() {
		return "[C] " + this.getClass().getSimpleName();
	}
}
