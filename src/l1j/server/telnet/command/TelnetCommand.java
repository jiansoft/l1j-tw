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
package l1j.server.telnet.command;

import java.util.StringTokenizer;

import l1j.server.server.GameServer;
import l1j.server.server.Opcodes;
import l1j.server.server.datatables.ChatLogTable;
import l1j.server.server.model.L1Character;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_ChatPacket;
import l1j.server.server.storage.mysql.MySqlCharacterStorage;
import l1j.server.server.utils.IntRange;
import static l1j.server.telnet.command.TelnetCommandResult.*;

/**
 * Telnet 指令介面
 * <p>定義所有 Telnet 指令的執行介面，所有指令實作類別都必須實作此介面。
 *
 * <h3>設計模式:</h3>
 * <p>使用命令模式 (Command Pattern)，將每個指令封裝為獨立的類別。
 *
 * <h3>指令實作類別:</h3>
 * <ul>
 *   <li>{@link EchoCommand} - 回應測試指令</li>
 *   <li>{@link PlayerIdCommand} - 查詢玩家 ID</li>
 *   <li>{@link CharStatusCommand} - 查詢角色狀態</li>
 *   <li>{@link GlobalChatCommand} - 全域廣播訊息</li>
 *   <li>{@link ShutDownCommand} - 關閉伺服器</li>
 * </ul>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * TelnetCommand cmd = new EchoCommand();
 * TelnetCommandResult result = cmd.execute("test message");
 * System.out.println(result.getCode() + " " + result.getResult());
 * </pre>
 *
 * @see TelnetCommandResult
 * @see TelnetCommandExecutor
 */
public interface TelnetCommand {
	/**
	 * 執行 Telnet 指令
	 * <p>根據傳入的參數執行對應的指令邏輯，並回傳執行結果。
	 *
	 * @param args 指令參數字串
	 * @return 指令執行結果，包含狀態碼、狀態訊息及結果內容
	 * @see TelnetCommandResult
	 */
	TelnetCommandResult execute(String args);
}

/**
 * Echo 測試指令
 * <p>將接收到的參數原封不動回傳，用於測試 Telnet 連線是否正常。
 *
 * <h3>使用方式:</h3>
 * <pre>
 * 輸入: echo Hello World
 * 輸出: Hello World
 * </pre>
 *
 * <h3>用途:</h3>
 * <ul>
 *   <li>測試 Telnet 伺服器連線是否正常</li>
 *   <li>驗證指令執行框架是否運作</li>
 *   <li>測試網路封包傳輸</li>
 * </ul>
 */
class EchoCommand implements TelnetCommand {
	/**
	 * 執行 Echo 指令
	 * <p>直接回傳傳入的參數字串。
	 *
	 * @param args 要回應的字串
	 * @return 包含原始字串的執行結果
	 */
	@Override
	public TelnetCommandResult execute(String args) {
		return new TelnetCommandResult(CMD_OK, args);
	}
}

/**
 * 玩家 ID 查詢指令
 * <p>根據角色名稱查詢對應的角色 ID (ObjectId)。
 *
 * <h3>使用方式:</h3>
 * <pre>
 * 輸入: playerid PlayerName
 * 輸出: 12345 (角色 ID)
 * </pre>
 *
 * <h3>注意事項:</h3>
 * <ul>
 *   <li>僅查詢目前在線上的角色</li>
 *   <li>若角色不存在或離線，回傳 "0"</li>
 *   <li>角色名稱需完全符合 (區分大小寫)</li>
 * </ul>
 *
 * @see L1World#getPlayer(String)
 * @see L1PcInstance#getId()
 */
class PlayerIdCommand implements TelnetCommand {
	/**
	 * 執行玩家 ID 查詢指令
	 * <p>在世界中搜尋指定名稱的玩家並回傳其 ID。
	 *
	 * @param args 玩家角色名稱
	 * @return 角色 ID，若不存在則回傳 "0"
	 */
	@Override
	public TelnetCommandResult execute(String args) {
		L1PcInstance pc = L1World.getInstance().getPlayer(args);
		String result = pc == null ? "0" : String.valueOf(pc.getId());
		return new TelnetCommandResult(CMD_OK, result);
	}
}

/**
 * 角色狀態查詢指令
 * <p>根據角色 ID 查詢角色的詳細狀態資訊。
 *
 * <h3>使用方式:</h3>
 * <pre>
 * 輸入: charstatus 12345
 * 輸出:
 * Name: PlayerName
 * Level: 50
 * MaxHp: 500
 * CurrentHp: 450
 * MaxMp: 200
 * CurrentMp: 180
 * </pre>
 *
 * <h3>回傳資訊:</h3>
 * <ul>
 *   <li><b>Name:</b> 角色名稱</li>
 *   <li><b>Level:</b> 角色等級</li>
 *   <li><b>MaxHp:</b> 最大 HP</li>
 *   <li><b>CurrentHp:</b> 目前 HP</li>
 *   <li><b>MaxMp:</b> 最大 MP</li>
 *   <li><b>CurrentMp:</b> 目前 MP</li>
 * </ul>
 *
 * <h3>錯誤處理:</h3>
 * <ul>
 *   <li>若 ObjectId 不存在，回傳錯誤訊息</li>
 *   <li>若 ObjectId 不是角色 (例如物品)，回傳錯誤訊息</li>
 * </ul>
 *
 * @see L1World#findObject(int)
 * @see L1Character
 */
class CharStatusCommand implements TelnetCommand {
	/**
	 * 執行角色狀態查詢指令
	 * <p>根據 ObjectId 查詢角色並回傳其狀態資訊。
	 *
	 * @param args 角色的 ObjectId (數字字串)
	 * @return 角色狀態資訊，若失敗則回傳錯誤訊息
	 */
	@Override
	public TelnetCommandResult execute(String args) {
		int id = Integer.valueOf(args);
		L1Object obj = L1World.getInstance().findObject(id);
		if (obj == null) {
			return new TelnetCommandResult(CMD_INTERNAL_ERROR, "ObjectId " + id
					+ " not found");
		}
		if (!(obj instanceof L1Character)) {
			return new TelnetCommandResult(CMD_INTERNAL_ERROR, "ObjectId " + id
					+ " is not a character");
		}
		L1Character cha = (L1Character) obj;
		StringBuilder result = new StringBuilder();
		result.append("Name: " + cha.getName() + "\r\n");
		result.append("Level: " + cha.getLevel() + "\r\n");
		result.append("MaxHp: " + cha.getMaxHp() + "\r\n");
		result.append("CurrentHp: " + cha.getCurrentHp() + "\r\n");
		result.append("MaxMp: " + cha.getMaxMp() + "\r\n");
		result.append("CurrentMp: " + cha.getCurrentMp() + "\r\n");
		return new TelnetCommandResult(CMD_OK, result.toString());
	}
}

/**
 * 全域廣播指令
 * <p>以指定角色名義向全伺服器所有玩家發送廣播訊息。
 *
 * <h3>使用方式:</h3>
 * <pre>
 * 輸入: globalchat CharacterName 廣播訊息內容
 * 效果: 所有在線玩家收到 [CharacterName] 的全域訊息
 * </pre>
 *
 * <h3>功能說明:</h3>
 * <ul>
 *   <li>從資料庫載入指定角色資料</li>
 *   <li>以該角色名義發送全域訊息</li>
 *   <li>訊息會記錄到聊天日誌</li>
 *   <li>所有在線玩家都會收到訊息</li>
 * </ul>
 *
 * <h3>注意事項:</h3>
 * <ul>
 *   <li>角色不需在線上，會從資料庫載入</li>
 *   <li>若角色不存在，回傳錯誤訊息</li>
 *   <li>訊息會被記錄到聊天日誌 (類型 3: 全域)</li>
 *   <li>角色位置會設為 (-1, -1, 0) 表示系統訊息</li>
 * </ul>
 *
 * <h3>安全性考量:</h3>
 * <p><b>警告:</b> 此指令允許冒用任何角色名義發送訊息，應嚴格控制存取權限。
 *
 * @see MySqlCharacterStorage#loadCharacter(String)
 * @see ChatLogTable#storeChat(L1PcInstance, L1PcInstance, String, int)
 * @see L1World#broadcastPacketToAll(l1j.server.server.serverpackets.ServerBasePacket)
 */
class GlobalChatCommand implements TelnetCommand {
	/**
	 * 執行全域廣播指令
	 * <p>載入指定角色並以其名義發送全域訊息。
	 *
	 * <h3>參數格式:</h3>
	 * <pre>
	 * args = "CharacterName 訊息內容"
	 * </pre>
	 *
	 * @param args 角色名稱與訊息內容 (以空格分隔)
	 * @return 執行結果，若角色不存在則回傳錯誤
	 */
	@Override
	public TelnetCommandResult execute(String args) {
		StringTokenizer tok = new StringTokenizer(args, " ");
		String name = tok.nextToken();
		String text = args.substring(name.length() + 1);
		L1PcInstance pc = new MySqlCharacterStorage().loadCharacter(name);
		if (pc == null) {
			return new TelnetCommandResult(CMD_INTERNAL_ERROR, "キャラクターが存在しません。");
		}
		pc.getLocation().set(-1, -1, 0);
		ChatLogTable.getInstance().storeChat(pc, null, text, 3);

		L1World.getInstance().broadcastPacketToAll(new S_ChatPacket(pc, text, Opcodes.S_OPCODE_GLOBALCHAT, 3));
		return new TelnetCommandResult(CMD_OK, "");
	}
}

/**
 * 伺服器關機指令
 * <p>啟動伺服器關閉程序，可指定倒數時間。
 *
 * <h3>使用方式:</h3>
 * <pre>
 * 輸入: shutdown          (立即關閉)
 * 輸入: shutdown 300      (300 秒後關閉)
 * </pre>
 *
 * <h3>功能說明:</h3>
 * <ul>
 *   <li>若未指定時間，立即關閉伺服器</li>
 *   <li>若指定時間，啟動倒數計時後關閉</li>
 *   <li>最小倒數時間為 30 秒</li>
 *   <li>關閉前會通知所有在線玩家</li>
 * </ul>
 *
 * <h3>關機流程:</h3>
 * <ol>
 *   <li>驗證倒數時間 (最小 30 秒)</li>
 *   <li>啟動倒數計時器</li>
 *   <li>定期向玩家廣播剩餘時間</li>
 *   <li>時間到達時執行關機程序</li>
 *   <li>儲存所有玩家資料</li>
 *   <li>關閉伺服器</li>
 * </ol>
 *
 * <h3>安全性考量:</h3>
 * <p><b>警告:</b> 此指令會關閉伺服器，應嚴格限制存取權限，避免誤用或濫用。
 *
 * @see GameServer#shutdownWithCountdown(int)
 * @see IntRange#ensure(int, int, int)
 */
class ShutDownCommand implements TelnetCommand {
	/**
	 * 執行伺服器關機指令
	 * <p>啟動帶倒數的伺服器關閉程序。
	 *
	 * @param args 倒數秒數 (空字串表示立即關閉，最小值 30 秒)
	 * @return 執行結果
	 */
	@Override
	public TelnetCommandResult execute(String args) {
		int sec = args.isEmpty() ? 0 : Integer.parseInt(args);
		sec = IntRange.ensure(sec, 30, Integer.MAX_VALUE);

		GameServer.getInstance().shutdownWithCountdown(sec);
		return new TelnetCommandResult(CMD_OK, "");
	}
}
