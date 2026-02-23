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
package l1j.server.server.serverpackets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.Config;

/**
 * 伺服器封包的基礎抽象類別
 *
 * <p>此類別提供建構伺服器傳送給客戶端封包的基礎功能。所有伺服器封包類別都應該繼承此類別。
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>提供各種資料型態的寫入方法 (byte, short, int, long, double, String)</li>
 *   <li>自動處理封包的字節序 (Little-Endian)</li>
 *   <li>支援多語言字串編碼 (根據 CLIENT_LANGUAGE_CODE 設定)</li>
 *   <li>自動補齊封包至 4 字節對齊</li>
 * </ul>
 *
 * <h3>寫入方法說明:</h3>
 * <ul>
 *   <li>{@code writeC(int)} - 寫入 1 byte (8-bit)</li>
 *   <li>{@code writeH(int)} - 寫入 2 bytes (16-bit, short)</li>
 *   <li>{@code writeD(int)} - 寫入 4 bytes (32-bit, int)</li>
 *   <li>{@code writeL(long)} - 寫入 1 byte from long (僅低 8 位元)</li>
 *   <li>{@code writeF(double)} - 寫入 8 bytes (64-bit, double)</li>
 *   <li>{@code writeS(String)} - 寫入字串 (以 null 終止)</li>
 *   <li>{@code writeByte(byte[])} - 寫入 byte 陣列</li>
 *   <li>{@code writeP(int)} - 寫入原始 byte 值</li>
 * </ul>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * public class S_MyPacket extends ServerBasePacket {
 *     public S_MyPacket(int value, String message) {
 *         writeC(Opcodes.S_OPCODE_MYPACKET);  // Opcode
 *         writeD(value);                       // 4 bytes integer
 *         writeS(message);                     // null-terminated string
 *     }
 *
 *     &#64;Override
 *     public byte[] getContent() {
 *         return getBytes();
 *     }
 * }
 * </pre>
 *
 * <h3>封包結構:</h3>
 * <ol>
 *   <li>所有封包都以 Opcode 開頭 (1 byte)</li>
 *   <li>後續資料依各封包定義寫入</li>
 *   <li>自動補齊至 4 字節倍數 (padding)</li>
 * </ol>
 *
 * @see l1j.server.server.Opcodes
 * @see l1j.server.server.clientpackets.ClientBasePacket
 */
public abstract class ServerBasePacket {
	private static Logger _log = Logger.getLogger(ServerBasePacket.class.getName());

	private static final String CLIENT_LANGUAGE_CODE = Config.CLIENT_LANGUAGE_CODE;

	ByteArrayOutputStream _bao = new ByteArrayOutputStream();

	/**
	 * 建構式
	 */
	protected ServerBasePacket() {
	}

	/**
	 * 寫入 4 bytes (32-bit integer, DWORD)
	 * <p>使用 Little-Endian 字節序
	 *
	 * @param value 要寫入的整數值 (32-bit)
	 */
	protected void writeD(int value) {
		_bao.write(value & 0xff);
		_bao.write(value >> 8 & 0xff);
		_bao.write(value >> 16 & 0xff);
		_bao.write(value >> 24 & 0xff);
	}

	/**
	 * 寫入 2 bytes (16-bit short, WORD)
	 * <p>使用 Little-Endian 字節序
	 *
	 * @param value 要寫入的整數值 (僅使用低 16 位元)
	 */
	protected void writeH(int value) {
		_bao.write(value & 0xff);
		_bao.write(value >> 8 & 0xff);
	}

	/**
	 * 寫入 1 byte (8-bit, BYTE)
	 *
	 * @param value 要寫入的整數值 (僅使用低 8 位元)
	 */
	protected void writeC(int value) {
		_bao.write(value & 0xff);
	}

	/**
	 * 寫入原始 byte 值 (不做遮罩處理)
	 *
	 * @param value 要寫入的 byte 值
	 */
	protected void writeP(int value) {
		_bao.write(value);
	}

	/**
	 * 寫入 long 值的低 8 位元 (1 byte)
	 *
	 * @param value 要寫入的 long 值 (僅使用低 8 位元)
	 */
	protected void writeL(long value) {
		_bao.write((int) (value & 0xff));
	}

	/**
	 * 寫入 8 bytes (64-bit double, IEEE 754)
	 * <p>使用 Little-Endian 字節序
	 *
	 * @param org 要寫入的浮點數值 (64-bit double)
	 */
	protected void writeF(double org) {
		long value = Double.doubleToRawLongBits(org);
		_bao.write((int) (value & 0xff));
		_bao.write((int) (value >> 8 & 0xff));
		_bao.write((int) (value >> 16 & 0xff));
		_bao.write((int) (value >> 24 & 0xff));
		_bao.write((int) (value >> 32 & 0xff));
		_bao.write((int) (value >> 40 & 0xff));
		_bao.write((int) (value >> 48 & 0xff));
		_bao.write((int) (value >> 56 & 0xff));
	}

	/**
	 * 寫入字串 (以 null 字節終止)
	 * <p>字串編碼依據 CLIENT_LANGUAGE_CODE 設定:
	 * <ul>
	 *   <li>0: US (UTF-8)</li>
	 *   <li>3: Taiwan (BIG5)</li>
	 *   <li>4: Japan (SJIS)</li>
	 *   <li>5: China (GBK)</li>
	 * </ul>
	 *
	 * @param text 要寫入的字串 (null 時僅寫入終止字節)
	 */
	protected void writeS(String text) {
		try {
			if (text != null) {
				_bao.write(text.getBytes(CLIENT_LANGUAGE_CODE));
			}
		} catch (Exception e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}

		_bao.write(0);
	}

	/**
	 * 寫入 byte 陣列
	 *
	 * @param text 要寫入的 byte 陣列 (null 時不寫入任何內容)
	 */
	protected void writeByte(byte[] text) {
		try {
			if (text != null) {
				_bao.write(text);
			}
		} catch (Exception e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	/**
	 * 取得封包總長度 (包含 2 bytes 標頭)
	 *
	 * @return 封包長度
	 */
	public int getLength() {
		return _bao.size() + 2;
	}

	/**
	 * 取得封包的 byte 陣列 (自動補齊至 4 字節對齊)
	 * <p>如果封包長度不是 4 的倍數,會自動補 0x00 至對齊
	 *
	 * @return 封包的 byte 陣列
	 */
	public byte[] getBytes() {
		int padding = _bao.size() % 4;

		if (padding != 0) {
			for (int i = padding; i < 4; i++) {
				writeC(0x00);
			}
		}

		return _bao.toByteArray();
	}

	/**
	 * 取得封包的完整內容 (子類別必須實作此方法)
	 * <p>通常實作為 {@code return getBytes();}
	 *
	 * @return 封包的完整 byte 陣列
	 * @throws IOException IO 錯誤
	 */
	public abstract byte[] getContent() throws IOException;

	/**
	 * 取得伺服器封包的類型名稱
	 * <p>格式: "[S] 類別名稱" (例如: "[S] S_LoginResult")
	 *
	 * @return 封包類型字串
	 */
	public String getType() {
		return "[S] " + this.getClass().getSimpleName();
	}

	/**
	 * 取得封包參數描述（供日誌輸出用）
	 * <p>子類別可覆寫此方法來提供有意義的參數資訊
	 * <p>預設回傳 null 表示不輸出參數
	 *
	 * @return 參數描述字串，例如 "currentMp=100, maxMp=200"
	 */
	public String getParams() {
		return null;
	}
}
