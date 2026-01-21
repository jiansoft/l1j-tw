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

import static l1j.server.server.model.skill.L1SkillId.COOKING_1_0_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_1_0_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_1_6_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_1_6_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_2_0_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_2_0_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_2_6_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_2_6_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_0_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_0_S;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_6_N;
import static l1j.server.server.model.skill.L1SkillId.COOKING_3_6_S;
import static l1j.server.server.model.skill.L1SkillId.EFFECT_BEGIN;
import static l1j.server.server.model.skill.L1SkillId.EFFECT_BLOODSTAIN_OF_ANTHARAS;
import static l1j.server.server.model.skill.L1SkillId.EFFECT_BLOODSTAIN_OF_FAFURION;
import static l1j.server.server.model.skill.L1SkillId.EFFECT_END;
import static l1j.server.server.model.skill.L1SkillId.STATUS_THIRD_SPEED;
import static l1j.server.server.model.skill.L1SkillId.COOKING_WONDER_DRUG;
import static l1j.server.server.model.skill.L1SkillId.MIRROR_IMAGE;
import static l1j.server.server.model.skill.L1SkillId.SHAPE_CHANGE;
import static l1j.server.server.model.skill.L1SkillId.STATUS_BLUE_POTION;
import static l1j.server.server.model.skill.L1SkillId.STATUS_BRAVE;
import static l1j.server.server.model.skill.L1SkillId.STATUS_BRAVE2;
import static l1j.server.server.model.skill.L1SkillId.STATUS_CHAT_PROHIBITED;
import static l1j.server.server.model.skill.L1SkillId.STATUS_ELFBRAVE;
import static l1j.server.server.model.skill.L1SkillId.STATUS_HASTE;
import static l1j.server.server.model.skill.L1SkillId.STATUS_RIBRAVE;
import static l1j.server.server.model.skill.L1SkillId.UNCANNY_DODGE;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.Config;
import l1j.server.L1DatabaseFactory;
import l1j.server.server.Account;
import l1j.server.server.ActionCodes;
import l1j.server.server.ClientThread;
import l1j.server.server.WarTimeController;
import l1j.server.server.datatables.CharacterTable;
import l1j.server.server.datatables.ClanRecommendTable;
import l1j.server.server.datatables.GetBackRestartTable;
import l1j.server.server.datatables.SkillsTable;
import l1j.server.server.model.Getback;
import l1j.server.server.model.L1CastleLocation;
import l1j.server.server.model.L1Clan;
import l1j.server.server.model.L1Cooking;
import l1j.server.server.model.L1PolyMorph;
import l1j.server.server.model.L1War;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1SummonInstance;
import l1j.server.server.model.skill.L1BuffUtil;
import l1j.server.server.model.skill.L1SkillUse;
import l1j.server.server.serverpackets.S_ActiveSpells;
import l1j.server.server.serverpackets.S_AddSkill;
import l1j.server.server.serverpackets.S_Bookmarks;
import l1j.server.server.serverpackets.S_CharTitle;
import l1j.server.server.serverpackets.S_CharacterConfig;
import l1j.server.server.serverpackets.S_ClanAttention;
import l1j.server.server.serverpackets.S_InitialAbilityGrowth;
import l1j.server.server.serverpackets.S_InvList;
import l1j.server.server.serverpackets.S_Karma;
import l1j.server.server.serverpackets.S_Liquor;
import l1j.server.server.serverpackets.S_LoginGame;
import l1j.server.server.serverpackets.S_Mail;
import l1j.server.server.serverpackets.S_MapID;
import l1j.server.server.serverpackets.S_OwnCharPack;
import l1j.server.server.serverpackets.S_OwnCharStatus;
import l1j.server.server.serverpackets.S_OwnCharStatus2;
import l1j.server.server.serverpackets.S_PacketBox;
import l1j.server.server.serverpackets.S_RuneSlot;
import l1j.server.server.serverpackets.S_SPMR;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_SkillBrave;
import l1j.server.server.serverpackets.S_SkillHaste;
import l1j.server.server.serverpackets.S_SkillIconGFX;
import l1j.server.server.serverpackets.S_SkillIconThirdSpeed;
import l1j.server.server.serverpackets.S_SummonPack;
import l1j.server.server.serverpackets.S_War;
import l1j.server.server.serverpackets.S_Weather;
import l1j.server.server.serverpackets.S_bonusstats;
import l1j.server.server.templates.L1GetBackRestart;
import l1j.server.server.templates.L1Skills;
import l1j.server.server.utils.SQLUtil;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket

/**
 * 客戶端登入伺服器封包處理器
 * <p>處理玩家選擇角色並進入遊戲世界的完整流程，是登入系統的核心處理類別。
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>驗證角色登入的合法性 (帳號匹配、防止重複登入)</li>
 *   <li>從資料庫載入角色完整資料</li>
 *   <li>將角色加入 L1World (遊戲世界)</li>
 *   <li>處理特殊位置回傳 (自動回村、戰爭區域檢查)</li>
 *   <li>載入並恢復角色狀態 (裝備、技能、Buff、寵物)</li>
 *   <li>發送所有初始化封包給客戶端</li>
 *   <li>處理血盟、結婚、新手保護等系統狀態</li>
 * </ul>
 *
 * <h3>登入流程 (14 個主要步驟):</h3>
 * <ol>
 *   <li><b>接收角色名稱:</b> 從封包讀取玩家選擇的角色名稱</li>
 *   <li><b>重複登入檢查:</b> 防止同一角色或同帳號雙重登入</li>
 *   <li><b>載入角色資料:</b> {@link L1PcInstance#load(String)} 從資料庫載入</li>
 *   <li><b>帳號匹配驗證:</b> 確認角色屬於該帳號</li>
 *   <li><b>等級下降檢查:</b> 防止非法降級</li>
 *   <li><b>★ 加入 L1World:</b> {@link L1World#storeObject(l1j.server.server.model.L1Object)}
 *       和 {@link L1World#addVisibleObject(l1j.server.server.model.L1Object)}</li>
 *   <li><b>位置處理:</b> 自動回村、戰爭區域檢查</li>
 *   <li><b>載入物品:</b> {@link #items(L1PcInstance)} 載入背包物品</li>
 *   <li><b>載入技能:</b> {@link #skills(L1PcInstance)} 載入已學習技能</li>
 *   <li><b>恢復 Buff:</b> {@link #buff(ClientThread, L1PcInstance)} 恢復所有 Buff 效果</li>
 *   <li><b>載入寵物:</b> {@link #serchSummon(L1PcInstance)} 恢復召喚獸</li>
 *   <li><b>發送初始化封包:</b> 20+ 種封包發送角色完整資訊</li>
 *   <li><b>血盟與結婚處理:</b> 通知血盟成員、檢查結婚對象在線狀態</li>
 *   <li><b>啟動自動更新:</b> HP/MP 恢復、經驗值監控等</li>
 * </ol>
 *
 * <h3>發送的主要封包 (依序):</h3>
 * <table border="1">
 *   <tr><th>封包</th><th>說明</th></tr>
 *   <tr><td>S_LoginGame</td><td>登入成功通知</td></tr>
 *   <tr><td>S_CharacterConfig</td><td>角色設定</td></tr>
 *   <tr><td>S_InvList</td><td>背包物品列表</td></tr>
 *   <tr><td>S_Mail</td><td>郵件系統 (收件匣、已發送、儲存)</td></tr>
 *   <tr><td>S_RuneSlot</td><td>符文欄位狀態</td></tr>
 *   <tr><td>S_ActiveSpells</td><td>啟用中的魔法效果</td></tr>
 *   <tr><td>S_Bookmarks</td><td>記憶座標</td></tr>
 *   <tr><td>S_OwnCharStatus</td><td>角色狀態 (HP/MP/等級/經驗)</td></tr>
 *   <tr><td>S_MapID</td><td>地圖 ID 與水下狀態</td></tr>
 *   <tr><td>S_OwnCharPack</td><td>角色完整資訊封包</td></tr>
 *   <tr><td>S_SPMR</td><td>魔攻/魔防數值</td></tr>
 *   <tr><td>S_CharTitle</td><td>角色稱號</td></tr>
 *   <tr><td>S_OwnCharStatus2</td><td>角色初始素質</td></tr>
 *   <tr><td>S_InitialAbilityGrowth</td><td>能力值成長獎勵</td></tr>
 *   <tr><td>S_Weather</td><td>天氣狀態</td></tr>
 *   <tr><td>S_AddSkill</td><td>已學習技能列表</td></tr>
 *   <tr><td>S_Karma</td><td>友好度 (Karma)</td></tr>
 *   <tr><td>S_PacketBox</td><td>閃避率等數值</td></tr>
 *   <tr><td>S_bonusstats</td><td>額外能力點 (51 級以上)</td></tr>
 *   <tr><td>S_War</td><td>盟戰資訊</td></tr>
 *   <tr><td>S_ServerMessage</td><td>各種系統訊息</td></tr>
 * </table>
 *
 * <h3>安全性檢查:</h3>
 * <ul>
 *   <li><b>重複登入防護:</b> 檢查角色與帳號是否已在線</li>
 *   <li><b>帳號匹配驗證:</b> 確認角色屬於該帳號</li>
 *   <li><b>等級異常檢測:</b> 防止非法降級 (Config.LEVEL_DOWN_RANGE)</li>
 * </ul>
 *
 * <h3>特殊位置處理:</h3>
 * <ul>
 *   <li><b>自動回村:</b> 特定區域自動傳送回重生點 (Config.GET_BACK)</li>
 *   <li><b>戰爭區域檢查:</b> 非城堡擁有者傳送出城堡</li>
 * </ul>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * // PacketHandler 自動處理
 * case Opcodes.C_OPCODE_LOGINTOSERVER:
 *     packet = new C_LoginToServer(decrypt, client);
 *     break;
 * </pre>
 *
 * @see L1PcInstance#load(String)
 * @see L1World#storeObject(l1j.server.server.model.L1Object)
 * @see L1World#addVisibleObject(l1j.server.server.model.L1Object)
 * @see ClientThread#setActiveChar(L1PcInstance)
 */
public class C_LoginToServer extends ClientBasePacket {

	/** 封包類型識別字串 */
	private static final String C_LOGIN_TO_SERVER = "[C] C_LoginToServer";

	/** 日誌記錄器 */
	private static Logger _log = Logger.getLogger(C_LoginToServer.class.getName());

	/**
	 * 建構登入伺服器封包處理器
	 * <p>這是整個登入流程的核心方法，負責處理角色進入遊戲世界的所有邏輯。
	 *
	 * <h3>處理流程:</h3>
	 * <ol>
	 *   <li><b>讀取角色名稱:</b> 從封包讀取客戶端選擇的角色名稱</li>
	 *   <li><b>重複登入檢查:</b>
	 *     <ul>
	 *       <li>檢查該角色是否已在 ClientThread 中 (防止同角色重複登入)</li>
	 *       <li>檢查帳號是否已有其他角色在線 (防止同帳號雙開)</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>載入角色資料:</b> {@code L1PcInstance.load(charName)} 從資料庫載入完整資料</li>
	 *   <li><b>驗證合法性:</b>
	 *     <ul>
	 *       <li>角色是否存在</li>
	 *       <li>角色是否屬於該帳號</li>
	 *       <li>等級下降是否在容許範圍內</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>★ 加入 L1World:</b>
	 *     <ul>
	 *       <li>{@code L1World.getInstance().storeObject(pc)} - 加入全域物件池</li>
	 *       <li>{@code L1World.getInstance().addVisibleObject(pc)} - 加入可見物件列表</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>設定連線:</b>
	 *     <ul>
	 *       <li>{@code pc.setNetConnection(client)} - 設定網路連線</li>
	 *       <li>{@code client.setActiveChar(pc)} - 設定客戶端活躍角色</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>位置處理:</b>
	 *     <ul>
	 *       <li>自動回村檢查 (GetBackRestartTable)</li>
	 *       <li>戰爭區域檢查 (WarTimeController)</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>載入資料:</b>
	 *     <ul>
	 *       <li>{@link #items(L1PcInstance)} - 載入物品</li>
	 *       <li>{@link #skills(L1PcInstance)} - 載入技能</li>
	 *       <li>{@link #buff(ClientThread, L1PcInstance)} - 恢復 Buff</li>
	 *       <li>{@link #serchSummon(L1PcInstance)} - 恢復寵物</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>發送封包:</b> 20+ 種封包發送完整角色資訊給客戶端</li>
	 *   <li><b>血盟處理:</b> 通知血盟成員上線、檢查盟戰狀態</li>
	 *   <li><b>結婚處理:</b> 通知結婚對象上線</li>
	 *   <li><b>啟動系統:</b>
	 *     <ul>
	 *       <li>HP/MP 自動恢復</li>
	 *       <li>物件自動更新</li>
	 *       <li>經驗值監控</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>儲存資料:</b> {@code pc.save()} 儲存登入狀態</li>
	 * </ol>
	 *
	 * @param abyte0 封包位元組陣列
	 * @param client 客戶端執行緒
	 * @throws FileNotFoundException 檔案找不到例外
	 * @throws Exception 其他例外
	 * @see L1PcInstance#load(String)
	 * @see L1World#storeObject(l1j.server.server.model.L1Object)
	 * @see L1World#addVisibleObject(l1j.server.server.model.L1Object)
	 */
	public C_LoginToServer(byte abyte0[], ClientThread client) throws FileNotFoundException, Exception {
		super(abyte0);

		// ==================== 步驟 1: 讀取基本資訊 ====================
		// 取得客戶端的帳號名稱
		String login = client.getAccountName();

		// 從封包中讀取玩家選擇的角色名稱
		String charName = readS();

		// ==================== 步驟 2: 重複登入檢查 ====================
		// 檢查該客戶端連線是否已有活躍角色 (防止同一連線重複登入)
		if (client.getActiveChar() != null) {
			_log.info("同一個角色重複登入，強制切斷 " + client.getHostname() + ") 的連結");
			client.close();
			return;
		}

		// ==================== 步驟 3: 載入角色與帳號資料 ====================
		// 從資料庫載入角色完整資料
		L1PcInstance pc = L1PcInstance.load(charName);
		// 載入帳號資料
		Account account = Account.load(pc.getAccountName());

		// 檢查該帳號是否已有其他角色在線 (防止同帳號雙開)
		if (account.isOnlineStatus()) {
			_log.info("同一個帳號雙重角色登入，強制切斷 " + client.getHostname() + ") 的連結");
			client.close();
			return;
		}

		// ==================== 步驟 4: 驗證角色合法性 ====================
		// 檢查角色是否存在，且角色所屬帳號與登入帳號是否一致
		if ((pc == null) || !login.equals(pc.getAccountName())) {
			_log.info("無效的角色名稱: char=" + charName + " account=" + login + " host=" + client.getHostname());
			client.close();
			return;
		}

		// ==================== 步驟 5: 等級下降檢查 ====================
		// 如果設定檔中有設定等級下降容忍範圍，檢查是否超出範圍 (防止非法降級)
		if (Config.LEVEL_DOWN_RANGE != 0) {
			if (pc.getHighLevel() - pc.getLevel() >= Config.LEVEL_DOWN_RANGE) {
				_log.info("登錄請求超出了容忍的等級下降的角色: char=" + charName + " account=" + login + " host=" + client.getHostname());
				client.kick();
				return;
			}
		}

		// ==================== 步驟 6: 初始化角色狀態 ====================
		_log.info("角色登入到伺服器中: char=" + charName + " account=" + login+ " host=" + client.getHostname());

		// 保存登入時的 HP/MP 值，稍後用於恢復
		int currentHpAtLoad = pc.getCurrentHp();
		int currentMpAtLoad = pc.getCurrentMp();

		// 清除技能熟練度 (稍後會從資料庫重新載入)
		pc.clearSkillMastery();

		// 設定角色為在線狀態
		pc.setOnlineStatus(1);
		CharacterTable.updateOnlineStatus(pc);

		// ★ 將角色加入 L1World 全域物件池 (所有玩家、NPC、物品都存在這裡)
		L1World.getInstance().storeObject(pc);

		// ==================== 步驟 7: 設定網路連線 ====================
		// 設定角色的網路連線參照
		pc.setNetConnection(client);
		// 設定封包輸出介面
		pc.setPacketOutput(client);
		// 設定客戶端的活躍角色 (完成雙向綁定)
		client.setActiveChar(pc);

		// 發送登入成功封包給客戶端
		pc.sendPackets(new S_LoginGame(pc));

		// 設定帳號為在線狀態 (OnlineStatus = 1)
		Account.OnlineStatus(account, true);

		// ==================== 步驟 8: 位置處理 - 特殊區域自動回村 ====================
		// 取得自動回村設定表 (某些特殊區域登入時會自動傳送回村)
		GetBackRestartTable gbrTable = GetBackRestartTable.getInstance();
		L1GetBackRestart[] gbrList = gbrTable.getGetBackRestartTableList();

		// 遍歷自動回村列表，檢查角色是否在需要自動回村的區域
		for (L1GetBackRestart gbr : gbrList) {
			if (pc.getMapId() == gbr.getArea()) {
				// 設定角色位置為指定的回村座標
				pc.setX(gbr.getLocX());
				pc.setY(gbr.getLocY());
				pc.setMap(gbr.getMapId());
				break;
			}
		}

		// ==================== 步驟 9: 位置處理 - 全域自動回村設定 ====================
		// 如果 altsettings.properties 中 GetBack 設定為 true，強制所有角色回村
		if (Config.GET_BACK) {
			int[] loc = Getback.GetBack_Location(pc, true);
			pc.setX(loc[0]);
			pc.setY(loc[1]);
			pc.setMap((short) loc[2]);
		}

		// ==================== 步驟 10: 位置處理 - 戰爭區域檢查 ====================
		// 檢查角色是否位於城堡區域
		int castle_id = L1CastleLocation.getCastleIdByArea(pc);
		if (0 < castle_id) {
			// 檢查該城堡是否正在進行攻城戰
			if (WarTimeController.getInstance().isNowWar(castle_id)) {
				L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
				if (clan != null) {
					// 如果角色有血盟，但血盟不擁有該城堡，則傳送出城堡
					if (clan.getCastleId() != castle_id) {
						int[] loc = new int[3];
						loc = L1CastleLocation.getGetBackLoc(castle_id);
						pc.setX(loc[0]);
						pc.setY(loc[1]);
						pc.setMap((short) loc[2]);
					}
				} else {
					// 如果角色沒有血盟，則傳送出城堡
					int[] loc = new int[3];
					loc = L1CastleLocation.getGetBackLoc(castle_id);
					pc.setX(loc[0]);
					pc.setY(loc[1]);
					pc.setMap((short) loc[2]);
				}
			}
		}

		// ★ 將角色加入可見物件列表 (此時其他玩家可以看到該角色)
		L1World.getInstance().addVisibleObject(pc);
		
		// ==================== 步驟 11: 發送初始化封包 (1/3) ====================
		// 如果設定檔啟用伺服器端角色設定，發送設定封包
		if (Config.CHARACTER_CONFIG_IN_SERVER_SIDE) {
			pc.sendPackets(new S_CharacterConfig(pc.getId()));
		}

		// 載入並發送角色背包道具列表
		items(pc);

		// 發送郵件系統封包 (0: 收件匣, 1: 已發送, 2: 儲存)
		pc.sendPackets(new S_Mail(pc , 0));
		pc.sendPackets(new S_Mail(pc , 1));
		pc.sendPackets(new S_Mail(pc , 2));

		// 開始遊戲時間載體 (計時系統)
		pc.beginGameTimeCarrier();

		// 發送符文欄位狀態 (關閉 3 個欄位、開放 1 個欄位)
		pc.sendPackets(new S_RuneSlot(S_RuneSlot.RUNE_CLOSE_SLOT, 3));
		pc.sendPackets(new S_RuneSlot(S_RuneSlot.RUNE_OPEN_SLOT, 1));

		// 設定裝備狀態 (計算裝備對屬性的影響)
		pc.setEquipments();

		// 發送啟用中的魔法效果列表
		pc.sendPackets(new S_ActiveSpells(pc));

		// 發送記憶座標 (傳送記憶點)
		pc.sendPackets(new S_Bookmarks(pc));

		// ==================== 步驟 12: 發送初始化封包 (2/3) ====================
		// 發送角色狀態 (HP/MP/等級/經驗值)
		pc.sendPackets(new S_OwnCharStatus(pc));

		// 發送地圖 ID 與水下狀態
		pc.sendPackets(new S_MapID(pc.getMapId(), pc.getMap().isUnderwater()));

		// 發送角色完整資訊封包 (外觀、裝備、位置等)
		pc.sendPackets(new S_OwnCharPack(pc));

		// 發送魔攻 (SP) 與魔防 (MR) 數值
		pc.sendPackets(new S_SPMR(pc));

		// 發送並廣播角色稱號
		S_CharTitle s_charTitle = new S_CharTitle(pc.getId(), pc.getTitle());
		pc.sendPackets(s_charTitle);          // 發送給自己
		pc.broadcastPacket(s_charTitle);      // 廣播給周圍玩家

		// 發送視覺效果 (皇冠、中毒、水下等特效顯示)
		pc.sendVisualEffectAtLogin();

		// 發送角色初始素質
		pc.sendPackets(new S_OwnCharStatus2(pc, 1));

		// 發送角色能力值成長獎勵
		pc.sendPackets(new S_InitialAbilityGrowth(pc));

		// 發送當前天氣狀態
		pc.sendPackets(new S_Weather(L1World.getInstance().getWeather()));

		// ==================== 步驟 13: 載入角色技能與 Buff ====================
		// 載入並發送角色已學習的技能列表
		skills(pc);

		// 恢復角色的增益效果 (Buff)
		buff(client, pc);

		// 開啟/關閉角色光源 (根據時間與環境決定)
		pc.turnOnOffLight();

		// 發送友好度 (Karma) 數值
		pc.sendPackets(new S_Karma(pc));

		// 發送閃避率數值 (正值與負值)
		pc.sendPackets(new S_PacketBox(S_PacketBox.DODGE_RATE_PLUS, pc.getDodge()));
		pc.sendPackets(new S_PacketBox(S_PacketBox.DODGE_RATE_MINUS, pc.getNdodge()));

		// 檢查並提示血盟推薦系統狀態
		checkPledgeRecommendation(pc);

		// ==================== 步驟 14: 角色生死狀態設定 ====================
		// 根據當前 HP 判斷角色是否死亡
		if (pc.getCurrentHp() > 0) {
			pc.setDead(false);              // 設定為存活狀態
			pc.setStatus(0);                // 清除狀態碼
		} else {
			pc.setDead(true);               // 設定為死亡狀態
			pc.setStatus(ActionCodes.ACTION_Die);  // 設定死亡動作狀態
		}

		// ==================== 步驟 15: 額外能力點檢查 ====================
		// 檢查 51 級以上角色是否有未分配的額外能力點
		if ((pc.getLevel() >= 51) && (pc.getLevel() - 50 > pc.getBonusStats())) {
			// 檢查基礎能力值總和是否低於 210 (可分配額外點數)
			if ((pc.getBaseStr() + pc.getBaseDex() + pc.getBaseCon()+ pc.getBaseInt() + pc.getBaseWis() + pc.getBaseCha()) < 210) {
				pc.sendPackets(new S_bonusstats(pc.getId(), 1));
			}
		}

		// ==================== 步驟 16: 恢復召喚獸 ====================
		// 搜尋並恢復角色的召喚獸與寵物
		serchSummon(pc);

		// ==================== 步驟 17: 攻城戰檢查 ====================
		// 檢查攻城戰狀態並處理相關邏輯
		WarTimeController.getInstance().checkCastleWar(pc);

		// ==================== 步驟 18: 血盟處理 ====================
		if (pc.getClanid() != 0) { // 角色有血盟
			L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
			if (clan != null) {
				// 驗證血盟 ID 與名稱是否一致 (防止血盟解散後重新創立同名血盟的問題)
				if ((pc.getClanid() == clan.getClanId()) &&
						pc.getClanname().toLowerCase().equals(clan.getClanName().toLowerCase())) {

					// 通知所有在線血盟成員該玩家已上線
					L1PcInstance[] clanMembers = clan.getOnlineClanMember();
					for (L1PcInstance clanMember : clanMembers) {
						if (clanMember.getId() != pc.getId()) {
							// 發送訊息: "血盟員 %0 已連接至遊戲"
							clanMember.sendPackets(new S_ServerMessage(843, pc.getName()));
						}
					}

					// 檢查血盟是否正在進行盟戰
					for (L1War war : L1World.getInstance().getWarList()) {
						boolean ret = war.CheckClanInWar(pc.getClanname());
						if (ret) { // 血盟正在盟戰中
							String enemy_clan_name = war.GetEnemyClanName(pc.getClanname());
							if (enemy_clan_name != null) {
								// 發送訊息: "你的血盟目前正與 [敵對血盟] 交戰中"
								pc.sendPackets(new S_War(8, pc.getClanname(),enemy_clan_name));
							}
							break;
						}
					}
				} else {
					// 血盟資料不一致，清除角色的血盟資訊
					pc.setClanid(0);
					pc.setClanname("");
					pc.setClanRank(0);
					pc.save();
				}
			}
		}

		// ==================== 步驟 19: 結婚系統處理 ====================
		if (pc.getPartnerId() != 0) { // 角色已結婚
			// 尋找結婚對象是否在線
			L1PcInstance partner = (L1PcInstance) L1World.getInstance().findObject(pc.getPartnerId());
			if ((partner != null) && (partner.getPartnerId() != 0)) {
				// 驗證雙方的結婚對象 ID 是否互相匹配
				if ((pc.getPartnerId() == partner.getId())&& (partner.getPartnerId() == pc.getId())) {
					pc.sendPackets(new S_ServerMessage(548));      // "你的伴侶目前在遊戲中"
					partner.sendPackets(new S_ServerMessage(549)); // "你的伴侶剛剛登入遊戲"
				}
			}
		}

		// ==================== 步驟 20: HP/MP 恢復與啟動系統 ====================
		// 如果登入時的 HP/MP 高於當前值，恢復至登入時的值
		if (currentHpAtLoad > pc.getCurrentHp()) {
			pc.setCurrentHp(currentHpAtLoad);
		}
		if (currentMpAtLoad > pc.getCurrentMp()) {
			pc.setCurrentMp(currentMpAtLoad);
		}

		// 啟動 HP 自動恢復
		pc.startHpRegeneration();
		// 啟動 MP 自動恢復
		pc.startMpRegeneration();
		// 啟動物件自動更新 (定期同步角色狀態)
		pc.startObjectAutoUpdate();
		// 設定生存吶喊時間
		pc.setCryOfSurvivalTime();
		// 角色重新啟動 (false = 非重新登入)
		client.CharReStart(false);
		// 開始經驗值監控
		pc.beginExpMonitor();
		// 儲存角色資料到資料庫
		pc.save();

		// ==================== 步驟 21: 特殊狀態處理 ====================
		// 如果角色處於地獄時間 (懲罰狀態)，重新啟動地獄計時
		if (pc.getHellTime() > 0) {
			pc.beginHell(false);
		}

		// 處理新手保護系統 (遭遇的守護) 狀態資料變動
		pc.checkNoviceType();
	}

	/**
	 * 載入並發送角色背包道具列表
	 * <p>從資料庫中恢復角色的完整背包資料，並將道具列表發送給客戶端顯示。
	 *
	 * <h3>主要功能:</h3>
	 * <ul>
	 *   <li>從資料庫載入角色所有背包道具 (character_items 資料表)</li>
	 *   <li>將道具資料恢復到角色的 {@link l1j.server.server.model.Instance.L1ItemInstance} 物件中</li>
	 *   <li>發送 {@link l1j.server.server.serverpackets.S_InvList} 封包給客戶端</li>
	 *   <li>客戶端接收後顯示完整的背包介面</li>
	 * </ul>
	 *
	 * <h3>處理流程:</h3>
	 * <ol>
	 *   <li><b>載入道具資料:</b> 呼叫 {@link l1j.server.server.datatables.CharacterTable#restoreInventory(L1PcInstance)}</li>
	 *   <li><b>查詢資料庫:</b> 從 character_items 資料表查詢該角色的所有道具記錄</li>
	 *   <li><b>建立道具物件:</b> 為每個道具記錄建立 L1ItemInstance 物件</li>
	 *   <li><b>加入背包:</b> 將道具物件加入角色的 Inventory 管理器</li>
	 *   <li><b>發送封包:</b> 打包所有道具資料並發送給客戶端</li>
	 * </ol>
	 *
	 * <h3>資料表結構 (character_items):</h3>
	 * <ul>
	 *   <li>id: 道具唯一識別碼</li>
	 *   <li>item_id: 道具模板 ID</li>
	 *   <li>char_id: 角色 ID</li>
	 *   <li>item_name: 道具名稱</li>
	 *   <li>count: 數量</li>
	 *   <li>is_equipped: 是否裝備中</li>
	 *   <li>enchantlvl: 強化等級</li>
	 *   <li>其他屬性欄位 (祝福狀態、耐久度等)</li>
	 * </ul>
	 *
	 * <h3>執行時機:</h3>
	 * <p>在角色登入流程中，於載入角色基本資料後立即執行，
	 * 確保玩家進入遊戲時可以看到完整的背包內容。
	 *
	 * @param pc 要載入背包資料的角色實例
	 * @see l1j.server.server.datatables.CharacterTable#restoreInventory(L1PcInstance)
	 * @see l1j.server.server.serverpackets.S_InvList
	 * @see l1j.server.server.model.Instance.L1ItemInstance
	 * @see l1j.server.server.model.L1PcInventory
	 */
	private void items(L1PcInstance pc) {
		// 從資料庫載入角色的所有背包道具 (查詢 character_items 資料表)
		// 建立 L1ItemInstance 物件並加入角色的 Inventory 管理器
		CharacterTable.getInstance().restoreInventory(pc);

		// 將完整的道具列表打包成封包發送給客戶端
		// 客戶端收到後會顯示在背包介面 (Inventory UI)
		pc.sendPackets(new S_InvList(pc.getInventory().getItems()));
	}

	/**
	 * 載入並發送角色已學習技能列表
	 * <p>從資料庫中恢復角色所有已學習的技能，並按技能等級分類打包發送給客戶端。
	 *
	 * <h3>主要功能:</h3>
	 * <ul>
	 *   <li>從資料庫載入角色所有已學習的技能 (character_skills 資料表)</li>
	 *   <li>將技能按照等級 (1-28 級) 進行位元遮罩運算分類</li>
	 *   <li>恢復角色的技能熟練度 (Skill Mastery) 狀態</li>
	 *   <li>發送 {@link l1j.server.server.serverpackets.S_AddSkill} 封包給客戶端顯示技能書</li>
	 * </ul>
	 *
	 * <h3>處理流程:</h3>
	 * <ol>
	 *   <li><b>查詢資料庫:</b> 從 character_skills 資料表查詢該角色的所有技能記錄</li>
	 *   <li><b>載入技能模板:</b> 透過 {@link l1j.server.server.datatables.SkillsTable#getTemplate(int)} 取得技能資料</li>
	 *   <li><b>分級處理:</b> 根據技能等級 (1-28) 對技能 ID 進行位元 OR 運算分類:
	 *     <ul>
	 *       <li>1 級技能累積到 lv1 變數</li>
	 *       <li>2 級技能累積到 lv2 變數</li>
	 *       <li>依此類推至 28 級技能</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>設定熟練度:</b> 呼叫 {@code pc.setSkillMastery(skillId)} 恢復技能熟練度</li>
	 *   <li><b>發送封包:</b> 若有任何技能，打包 28 個等級的技能資料並發送</li>
	 * </ol>
	 *
	 * <h3>資料表結構 (character_skills):</h3>
	 * <table border="1">
	 *   <tr><th>欄位</th><th>類型</th><th>說明</th></tr>
	 *   <tr><td>char_obj_id</td><td>INT</td><td>角色 ID</td></tr>
	 *   <tr><td>skill_id</td><td>INT</td><td>技能 ID</td></tr>
	 *   <tr><td>skill_name</td><td>VARCHAR</td><td>技能名稱</td></tr>
	 *   <tr><td>is_active</td><td>TINYINT</td><td>是否為主動技能</td></tr>
	 * </table>
	 *
	 * <h3>技能等級分類演算法:</h3>
	 * <p>使用位元遮罩 (Bitmask) 技術將同等級技能壓縮成單一整數:
	 * <pre>
	 * 範例: 假設 1 級有 3 個技能 (ID: 2, 4, 8)
	 * lv1 = 0
	 * lv1 |= 2  → lv1 = 2  (二進制: 0010)
	 * lv1 |= 4  → lv1 = 6  (二進制: 0110)
	 * lv1 |= 8  → lv1 = 14 (二進制: 1110)
	 *
	 * 客戶端收到 lv1=14 後，反向解析出擁有技能 2、4、8
	 * </pre>
	 *
	 * <h3>技能等級範圍:</h3>
	 * <p>遊戲支援 1-28 級技能，不同職業的技能等級分布不同:
	 * <ul>
	 *   <li><b>1-10 級:</b> 基礎職業技能 (王族、騎士、妖精、法師、黑妖)</li>
	 *   <li><b>11-20 級:</b> 進階職業技能</li>
	 *   <li><b>21-28 級:</b> 高階職業技能與特殊技能</li>
	 * </ul>
	 *
	 * <h3>執行時機:</h3>
	 * <p>在角色登入流程中，於載入背包道具後執行，
	 * 確保玩家進入遊戲時技能書 (F10) 顯示正確的技能列表。
	 *
	 * <h3>例外處理:</h3>
	 * <ul>
	 *   <li><b>SQLException:</b> 資料庫查詢失敗，記錄錯誤日誌並繼續</li>
	 * </ul>
	 *
	 * @param pc 要載入技能資料的角色實例
	 * @see l1j.server.server.datatables.SkillsTable
	 * @see l1j.server.server.serverpackets.S_AddSkill
	 * @see l1j.server.server.templates.L1Skills
	 * @see l1j.server.server.model.Instance.L1PcInstance#setSkillMastery(int)
	 */
	private void skills(L1PcInstance pc) {
		Connection con = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;
		try {
			// ==================== 查詢資料庫 ====================
			// 取得資料庫連線
			con = L1DatabaseFactory.getInstance().getConnection();
			// 準備 SQL 查詢 (查詢該角色的所有已學習技能)
			pstm = con.prepareStatement("SELECT * FROM character_skills WHERE char_obj_id=?");
			pstm.setInt(1, pc.getId());
			rs = pstm.executeQuery();

			// ==================== 初始化技能等級變數 ====================
			// i 用於計算技能總數
			int i = 0;
			// lv1~lv28 分別存儲 1~28 級技能的位元遮罩值
			// 使用位元 OR 運算 (|=) 將同等級技能 ID 壓縮成單一整數
			int lv1 = 0;
			int lv2 = 0;
			int lv3 = 0;
			int lv4 = 0;
			int lv5 = 0;
			int lv6 = 0;
			int lv7 = 0;
			int lv8 = 0;
			int lv9 = 0;
			int lv10 = 0;
			int lv11 = 0;
			int lv12 = 0;
			int lv13 = 0;
			int lv14 = 0;
			int lv15 = 0;
			int lv16 = 0;
			int lv17 = 0;
			int lv18 = 0;
			int lv19 = 0;
			int lv20 = 0;
			int lv21 = 0;
			int lv22 = 0;
			int lv23 = 0;
			int lv24 = 0;
			int lv25 = 0;
			int lv26 = 0;
			int lv27 = 0;
			int lv28 = 0;

			// ==================== 遍歷技能記錄 ====================
			while (rs.next()) {
				// 取得技能 ID
				int skillId = rs.getInt("skill_id");
				// 從技能表取得技能模板資料
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(skillId);

				// ==================== 按技能等級分類 (位元遮罩運算) ====================
				// 根據技能等級，使用位元 OR 運算將技能 ID 加入對應等級的變數
				// 例: lv1 |= 2 表示將技能 ID=2 加入 1 級技能列表
				if (l1skills.getSkillLevel() == 1) {
					lv1 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 2) {
					lv2 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 3) {
					lv3 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 4) {
					lv4 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 5) {
					lv5 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 6) {
					lv6 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 7) {
					lv7 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 8) {
					lv8 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 9) {
					lv9 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 10) {
					lv10 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 11) {
					lv11 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 12) {
					lv12 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 13) {
					lv13 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 14) {
					lv14 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 15) {
					lv15 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 16) {
					lv16 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 17) {
					lv17 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 18) {
					lv18 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 19) {
					lv19 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 20) {
					lv20 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 21) {
					lv21 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 22) {
					lv22 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 23) {
					lv23 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 24) {
					lv24 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 25) {
					lv25 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 26) {
					lv26 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 27) {
					lv27 |= l1skills.getId();
				}
				if (l1skills.getSkillLevel() == 28) {
					lv28 |= l1skills.getId();
				}

				// 計算技能總數 (用於檢查是否有技能需要發送)
				i = lv1 + lv2 + lv3 + lv4 + lv5 + lv6 + lv7 + lv8 + lv9 + lv10
						+ lv11 + lv12 + lv13 + lv14 + lv15 + lv16 + lv17 + lv18
						+ lv19 + lv20 + lv21 + lv22 + lv23 + lv24 + lv25 + lv26
						+ lv27 + lv28;

				// 設定技能熟練度 (恢復技能的使用次數/熟練度狀態)
				pc.setSkillMastery(skillId);
			}

			// ==================== 發送技能封包 ====================
			// 如果有任何技能，將 28 個等級的技能資料打包發送給客戶端
			if (i > 0) {
				pc.sendPackets(new S_AddSkill(lv1, lv2, lv3, lv4, lv5, lv6,
						lv7, lv8, lv9, lv10, lv11, lv12, lv13, lv14, lv15,
						lv16, lv17, lv18, lv19, lv20, lv21, lv22, lv23, lv24,
						lv25, lv26, lv27, lv28));
			}
		} catch (SQLException e) {
			// 記錄資料庫查詢錯誤
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} finally {
			// 確保資料庫資源被正確釋放 (防止連線洩漏)
			SQLUtil.close(rs);
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
	}

	/**
	 * 搜尋並恢復角色的召喚獸與寵物
	 * <p>在角色登入時，重新建立玩家與其召喚獸/寵物之間的關聯，並向周圍玩家發送召喚獸資訊。
	 *
	 * <h3>主要功能:</h3>
	 * <ul>
	 *   <li>遍歷世界中所有的召喚獸實例</li>
	 *   <li>找出屬於該角色的召喚獸並重新建立主人關聯</li>
	 *   <li>將召喚獸加入角色的寵物列表</li>
	 *   <li>向周圍可見範圍內的玩家發送召喚獸封包</li>
	 * </ul>
	 *
	 * <h3>處理流程:</h3>
	 * <ol>
	 *   <li><b>遍歷所有召喚獸:</b> 從 {@link l1j.server.server.model.L1World#getAllSummons()} 取得所有召喚獸</li>
	 *   <li><b>比對主人 ID:</b> 檢查 {@code summon.getMaster().getId()} 是否等於當前登入角色的 ID</li>
	 *   <li><b>重新建立關聯:</b> 呼叫 {@code summon.setMaster(pc)} 將召喚獸的主人設為當前角色</li>
	 *   <li><b>加入寵物列表:</b> 呼叫 {@code pc.addPet(summon)} 將召喚獸加入角色管理</li>
	 *   <li><b>廣播給周圍玩家:</b> 向召喚獸可見範圍內的所有玩家發送 {@link l1j.server.server.serverpackets.S_SummonPack} 封包</li>
	 * </ol>
	 *
	 * <h3>召喚獸類型:</h3>
	 * <p>此方法處理的召喚獸包括:
	 * <ul>
	 *   <li><b>魔法召喚獸:</b> 透過技能召喚的生物 (如 Summon Monster)</li>
	 *   <li><b>寵物:</b> 使用道具馴服的寵物</li>
	 *   <li><b>傀儡:</b> 召喚系職業的常駐傀儡</li>
	 * </ul>
	 *
	 * <h3>為何需要此方法:</h3>
	 * <p>當玩家登出時，召喚獸物件仍保留在 {@link l1j.server.server.model.L1World} 中，
	 * 但與玩家物件 (L1PcInstance) 的參照已斷開。玩家重新登入後，
	 * 必須透過此方法重新建立參照關係，否則:
	 * <ul>
	 *   <li>玩家無法控制召喚獸</li>
	 *   <li>召喚獸不會跟隨玩家移動</li>
	 *   <li>召喚獸狀態無法正確同步</li>
	 *   <li>其他玩家看不到召喚獸</li>
	 * </ul>
	 *
	 * <h3>執行時機:</h3>
	 * <p>在角色登入流程的後期執行，於角色完全載入並加入世界後，
	 * 確保召喚獸可以正確顯示並響應玩家指令。
	 *
	 * <h3>效能考量:</h3>
	 * <p>此方法會遍歷世界中所有召喚獸，當伺服器召喚獸數量龐大時可能造成效能問題。
	 * 建議在設計上維持召喚獸數量的合理限制，或使用索引機制優化搜尋。
	 *
	 * @param pc 要恢復召喚獸關聯的角色實例
	 * @see l1j.server.server.model.Instance.L1SummonInstance
	 * @see l1j.server.server.model.L1World#getAllSummons()
	 * @see l1j.server.server.model.Instance.L1PcInstance#addPet(L1NpcInstance)
	 * @see l1j.server.server.serverpackets.S_SummonPack
	 */
	private void serchSummon(L1PcInstance pc) {
		// 遍歷世界中所有的召喚獸實例
		for (L1SummonInstance summon : L1World.getInstance().getAllSummons()) {
			// 檢查召喚獸的主人 ID 是否與當前登入角色 ID 相同
			if (summon.getMaster().getId() == pc.getId()) {
				// 重新設定召喚獸的主人參照 (恢復玩家與召喚獸的綁定關係)
				summon.setMaster(pc);
				// 將召喚獸加入角色的寵物列表
				pc.addPet(summon);

				// 向召喚獸可見範圍內的所有玩家發送召喚獸封包
				// 讓其他玩家可以看到該召喚獸
				for (L1PcInstance visiblePc : L1World.getInstance().getVisiblePlayer(summon)) {
					visiblePc.sendPackets(new S_SummonPack(summon, visiblePc));
				}
			}
		}
	}

	/**
	 * 恢復角色的增益效果 (Buff) 與狀態
	 * <p>從資料庫中載入角色登出前的所有增益效果，並恢復剩餘時間與效果狀態。
	 *
	 * <h3>主要功能:</h3>
	 * <ul>
	 *   <li>從資料庫載入角色所有未過期的 Buff 資料 (character_buff 資料表)</li>
	 *   <li>根據不同的 Buff 類型執行對應的恢復邏輯</li>
	 *   <li>恢復 Buff 的剩餘時間與視覺效果</li>
	 *   <li>向客戶端發送 Buff 圖示與效果封包</li>
	 *   <li>恢復角色屬性變化 (速度、閃避、變身等)</li>
	 * </ul>
	 *
	 * <h3>處理流程:</h3>
	 * <ol>
	 *   <li><b>查詢資料庫:</b> 從 character_buff 資料表查詢該角色的所有 Buff 記錄</li>
	 *   <li><b>遍歷 Buff 記錄:</b> 逐筆處理每個 Buff 的恢復邏輯</li>
	 *   <li><b>類型判斷:</b> 根據 skill_id 判斷 Buff 類型並執行對應處理</li>
	 *   <li><b>恢復效果:</b> 呼叫對應的技能處理器或直接設定角色狀態</li>
	 *   <li><b>發送封包:</b> 向客戶端發送 Buff 圖示與效果顯示封包</li>
	 * </ol>
	 *
	 * <h3>資料表結構 (character_buff):</h3>
	 * <table border="1">
	 *   <tr><th>欄位</th><th>類型</th><th>說明</th></tr>
	 *   <tr><td>char_obj_id</td><td>INT</td><td>角色 ID</td></tr>
	 *   <tr><td>skill_id</td><td>INT</td><td>Buff 技能 ID</td></tr>
	 *   <tr><td>remaining_time</td><td>INT</td><td>剩餘時間 (秒)</td></tr>
	 *   <tr><td>poly_id</td><td>INT</td><td>變身 ID (僅變身 Buff)</td></tr>
	 * </table>
	 *
	 * <h3>支援的 Buff 類型:</h3>
	 * <table border="1">
	 *   <tr><th>Buff 類型</th><th>Skill ID 常數</th><th>效果說明</th><th>特殊處理</th></tr>
	 *   <tr><td>變身</td><td>SHAPE_CHANGE</td><td>角色外觀變身</td><td>需恢復 poly_id</td></tr>
	 *   <tr><td>勇敢藥水</td><td>STATUS_BRAVE</td><td>速度 +1</td><td>發送 S_SkillBrave 封包</td></tr>
	 *   <tr><td>精靈餅乾</td><td>STATUS_ELFBRAVE</td><td>速度 +3</td><td>發送 S_SkillBrave 封包</td></tr>
	 *   <tr><td>超級加速</td><td>STATUS_BRAVE2</td><td>速度 +5</td><td>發送 S_SkillBrave 封包</td></tr>
	 *   <tr><td>加速術</td><td>STATUS_HASTE</td><td>移動速度 +1</td><td>發送 S_SkillHaste 封包</td></tr>
	 *   <tr><td>藍色藥水</td><td>STATUS_BLUE_POTION</td><td>MP 回復加速</td><td>圖示 ID: 34</td></tr>
	 *   <tr><td>禁言狀態</td><td>STATUS_CHAT_PROHIBITED</td><td>無法發言</td><td>圖示 ID: 36</td></tr>
	 *   <tr><td>三段加速</td><td>STATUS_THIRD_SPEED</td><td>速度 * 1.15</td><td>時間 / 4</td></tr>
	 *   <tr><td>鏡像</td><td>MIRROR_IMAGE</td><td>閃避率 +50%</td><td>時間 / 16</td></tr>
	 *   <tr><td>暗影閃避</td><td>UNCANNY_DODGE</td><td>閃避率 +50%</td><td>時間 / 16</td></tr>
	 *   <tr><td>安塔瑞斯血痕</td><td>EFFECT_BLOODSTAIN_OF_ANTHARAS</td><td>龍族 Buff</td><td>時間 / 60</td></tr>
	 *   <tr><td>法利昂血痕</td><td>EFFECT_BLOODSTAIN_OF_FAFURION</td><td>龍族 Buff</td><td>時間 / 60</td></tr>
	 *   <tr><td>魔法料理</td><td>COOKING_*</td><td>屬性加成</td><td>呼叫 L1Cooking.eatCooking()</td></tr>
	 *   <tr><td>其他魔法</td><td>default</td><td>一般技能效果</td><td>呼叫 L1SkillUse.handleCommands()</td></tr>
	 * </table>
	 *
	 * <h3>特殊處理說明:</h3>
	 * <ul>
	 *   <li><b>速度類 Buff:</b> 恢復 BraveSpeed 或 MoveSpeed 屬性，並發送視覺效果封包</li>
	 *   <li><b>閃避類 Buff:</b> 直接增加角色閃避率屬性 (+5 = +50%)</li>
	 *   <li><b>變身 Buff:</b> 呼叫 {@link l1j.server.server.model.L1PolyMorph#doPoly} 恢復變身外觀</li>
	 *   <li><b>魔法料理:</b> 透過 {@link l1j.server.server.model.L1Cooking#eatCooking} 恢復屬性加成</li>
	 *   <li><b>一般魔法:</b> 透過 {@link l1j.server.server.model.L1SkillUse#handleCommands} 重新施放技能</li>
	 * </ul>
	 *
	 * <h3>時間單位轉換:</h3>
	 * <ul>
	 *   <li><b>一般 Buff:</b> remaining_time 單位為秒，需轉換為毫秒 (* 1000)</li>
	 *   <li><b>三段加速:</b> 時間除以 4，實際生效時間 = remaining_time / 4 秒</li>
	 *   <li><b>閃避類:</b> 時間除以 16，實際生效時間 = remaining_time / 16 秒</li>
	 *   <li><b>龍族血痕:</b> 時間除以 60，實際生效時間 = remaining_time / 60 分鐘</li>
	 * </ul>
	 *
	 * <h3>執行時機:</h3>
	 * <p>在角色登入流程中，於載入技能後執行，
	 * 確保玩家進入遊戲時可以看到正確的 Buff 圖示與享受 Buff 效果。
	 *
	 * <h3>例外處理:</h3>
	 * <ul>
	 *   <li><b>SQLException:</b> 資料庫查詢失敗，記錄錯誤日誌並繼續</li>
	 * </ul>
	 *
	 * @param clientthread 客戶端連線執行緒 (用於技能恢復時的封包發送)
	 * @param pc 要恢復 Buff 的角色實例
	 * @see l1j.server.server.model.L1SkillUse
	 * @see l1j.server.server.model.L1PolyMorph
	 * @see l1j.server.server.model.L1Cooking
	 * @see l1j.server.server.utils.L1BuffUtil
	 * @see l1j.server.server.serverpackets.S_SkillBrave
	 * @see l1j.server.server.serverpackets.S_SkillHaste
	 * @see l1j.server.server.serverpackets.S_SkillIconGFX
	 */
	private void buff(ClientThread clientthread, L1PcInstance pc) {
		Connection con = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;
		try {
			// ==================== 查詢資料庫 ====================
			// 取得資料庫連線
			con = L1DatabaseFactory.getInstance().getConnection();
			// 查詢該角色所有未過期的 Buff 資料
			pstm = con.prepareStatement("SELECT * FROM character_buff WHERE char_obj_id=?");
			pstm.setInt(1, pc.getId());
			rs = pstm.executeQuery();

			// ==================== 遍歷 Buff 記錄 ====================
			while (rs.next()) {
				// 取得 Buff 技能 ID
				int skillid = rs.getInt("skill_id");
				// 取得剩餘時間 (秒)
				int remaining_time = rs.getInt("remaining_time");
				// time 用於某些需要時間轉換的 Buff
				int time = 0;

				// ==================== 根據 Buff 類型執行對應處理 ====================
				switch (skillid) {
				case SHAPE_CHANGE: // 變身 Buff
					// 取得變身 ID (需要知道變成哪種生物)
					int poly_id = rs.getInt("poly_id");
					// 恢復變身狀態 (外觀、屬性變化)
					L1PolyMorph.doPoly(pc, poly_id, remaining_time,L1PolyMorph.MORPH_BY_LOGIN);
					break;

				case STATUS_BRAVE: // 勇敢藥水 (速度 +1)
					// 發送給自己 (顯示剩餘時間)
					pc.sendPackets(new S_SkillBrave(pc.getId(), 1,remaining_time));
					// 廣播給周圍玩家 (不顯示時間)
					pc.broadcastPacket(new S_SkillBrave(pc.getId(), 1, 0));
					// 設定勇敢速度 +1
					pc.setBraveSpeed(1);
					// 設定技能效果持續時間 (轉換為毫秒)
					pc.setSkillEffect(skillid, remaining_time * 1000);
					break;

				case STATUS_ELFBRAVE: // 精靈餅乾 (速度 +3)
					pc.sendPackets(new S_SkillBrave(pc.getId(), 3,remaining_time));
					pc.broadcastPacket(new S_SkillBrave(pc.getId(), 3, 0));
					pc.setBraveSpeed(3);
					pc.setSkillEffect(skillid, remaining_time * 1000);
					break;

				case STATUS_BRAVE2: // 超級加速 (速度 +5)
					pc.sendPackets(new S_SkillBrave(pc.getId(), 5,remaining_time));
					pc.broadcastPacket(new S_SkillBrave(pc.getId(), 5, 0));
					pc.setBraveSpeed(5);
					pc.setSkillEffect(skillid, remaining_time * 1000);
					break;

				case STATUS_HASTE: // 加速術 (移動速度 +1)
					pc.sendPackets(new S_SkillHaste(pc.getId(), 1,remaining_time));
					pc.broadcastPacket(new S_SkillHaste(pc.getId(), 1, 0));
					// 設定移動速度 +1
					pc.setMoveSpeed(1);
					pc.setSkillEffect(skillid, remaining_time * 1000);
					break;

				case STATUS_BLUE_POTION: // 藍色藥水 (MP 回復加速)
					// 發送 Buff 圖示 (圖示 ID: 34)
					pc.sendPackets(new S_SkillIconGFX(34, remaining_time));
					pc.setSkillEffect(skillid, remaining_time * 1000);
					break;

				case STATUS_CHAT_PROHIBITED: // 禁言狀態
					// 發送禁言圖示 (圖示 ID: 36)
					pc.sendPackets(new S_SkillIconGFX(36, remaining_time));
					pc.setSkillEffect(skillid, remaining_time * 1000);
					break;

				case STATUS_THIRD_SPEED: // 三段加速 (速度 * 1.15)
					// 時間需要除以 4 (特殊計算方式)
					time = remaining_time / 4;
					// 發送醉酒效果封包 (速度加成效果)
					pc.sendPackets(new S_Liquor(pc.getId(), 8));      // 人物速度 * 1.15
					pc.broadcastPacket(new S_Liquor(pc.getId(), 8));  // 廣播給周圍玩家
					// 發送三段加速圖示
					pc.sendPackets(new S_SkillIconThirdSpeed(time));
					// 設定效果時間 (需乘回 4 並轉換為毫秒)
					pc.setSkillEffect(skillid, time * 4 * 1000);
					break;

				case MIRROR_IMAGE: // 鏡像 (閃避率 +50%)
				case UNCANNY_DODGE: // 暗影閃避 (閃避率 +50%)
					// 時間需要除以 16 (特殊計算方式)
					time = remaining_time / 16;
					// 增加閃避率 +5 (等於 +50%)
					pc.addDodge((byte) 5);
					// 更新閃避率顯示 (S_PacketBox 類型 88)
					pc.sendPackets(new S_PacketBox(88, pc.getDodge()));
					// 發送效果持續時間 (S_PacketBox 類型 21)
					pc.sendPackets(new S_PacketBox(21, time));
					// 設定效果時間 (需乘回 16 並轉換為毫秒)
					pc.setSkillEffect(skillid, time * 16 * 1000);
					break;

				case EFFECT_BLOODSTAIN_OF_ANTHARAS: // 安塔瑞斯的血痕 (龍族 Buff)
					// 時間需要除以 60 (分鐘轉換)
					remaining_time = remaining_time / 60;
					if (remaining_time != 0) {
						// 恢復安塔瑞斯血痕效果 (type: 0)
						L1BuffUtil.bloodstain(pc, (byte) 0, remaining_time, false);
					}
					break;

				case EFFECT_BLOODSTAIN_OF_FAFURION: // 法利昂的血痕 (龍族 Buff)
					// 時間需要除以 60 (分鐘轉換)
					remaining_time = remaining_time / 60;
					if (remaining_time != 0) {
						// 恢復法利昂血痕效果 (type: 1)
						L1BuffUtil.bloodstain(pc, (byte) 1, remaining_time, false);
					}
					break;

				default:
					// ==================== 魔法料理 Buff ====================
					// 檢查是否為魔法料理技能 ID 範圍
					if (((skillid >= COOKING_1_0_N) && (skillid <= COOKING_1_6_N))
							|| ((skillid >= COOKING_1_0_S) && (skillid <= COOKING_1_6_S))
							|| ((skillid >= COOKING_2_0_N) && (skillid <= COOKING_2_6_N))
							|| ((skillid >= COOKING_2_0_S) && (skillid <= COOKING_2_6_S))
							|| ((skillid >= COOKING_3_0_N) && (skillid <= COOKING_3_6_N))
							|| ((skillid >= COOKING_3_0_S) && (skillid <= COOKING_3_6_S))) {
						// 恢復料理 Buff 效果 (屬性加成)
						L1Cooking.eatCooking(pc, skillid, remaining_time);
					}
					// ==================== 生命之樹果實、商城道具 ====================
					// 這些特殊 Buff 不需要額外處理，跳過
					else if (skillid == STATUS_RIBRAVE
							|| (skillid >= EFFECT_BEGIN && skillid <= EFFECT_END)
							|| skillid == COOKING_WONDER_DRUG) {
						; // 空處理
					}
					// ==================== 一般魔法 Buff ====================
					else {
						// 使用技能處理器恢復一般魔法 Buff
						L1SkillUse l1skilluse = new L1SkillUse();
						// TYPE_LOGIN 表示這是登入時恢復 Buff
						l1skilluse.handleCommands(clientthread.getActiveChar(),
								skillid, pc.getId(), pc.getX(), pc.getY(),
								null, remaining_time, L1SkillUse.TYPE_LOGIN);
					}
					break;
				}
			}
		} catch (SQLException e) {
			// 記錄資料庫查詢錯誤
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} finally {
			// 確保資料庫資源被正確釋放
			SQLUtil.close(rs);
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
	}
	
	/**
	 * 檢查並提示血盟推薦系統狀態
	 * <p>在角色登入時，檢查血盟推薦系統的相關狀態，並向玩家發送適當的提示訊息。
	 *
	 * <h3>主要功能:</h3>
	 * <ul>
	 *   <li>檢查玩家是否為血盟成員</li>
	 *   <li>檢查血盟是否已登錄至推薦系統</li>
	 *   <li>檢查是否有玩家申請加入血盟</li>
	 *   <li>檢查玩家是否為王族職業可建立血盟</li>
	 *   <li>檢查玩家是否已申請加入其他血盟</li>
	 *   <li>向符合條件的玩家發送提示訊息</li>
	 * </ul>
	 *
	 * <h3>處理流程:</h3>
	 * <h4>情境 1: 玩家已加入血盟 (pc.getClanid() > 0)</h4>
	 * <ol>
	 *   <li><b>發送血盟徽章狀態:</b> 向客戶端發送血盟徽章資訊與注意事項</li>
	 *   <li><b>檢查職位權限:</b> 只有以下職位有權限處理推薦系統:
	 *     <ul>
	 *       <li>君主 (CLAN_RANK_PRINCE)</li>
	 *       <li>守護騎士 (CLAN_RANK_GUARDIAN)</li>
	 *       <li>聯盟守護騎士 (CLAN_RANK_LEAGUE_GUARDIAN)</li>
	 *       <li>聯盟副君主 (CLAN_RANK_LEAGUE_VICEPRINCE)</li>
	 *       <li>聯盟君主 (CLAN_RANK_LEAGUE_PRINCE)</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>檢查登錄狀態:</b>
	 *     <ul>
	 *       <li>若血盟已登錄推薦系統:
	 *         <ul>
	 *           <li>檢查是否有玩家申請加入</li>
	 *           <li>若有申請，發送訊息 3248 (提示有待審核申請)</li>
	 *         </ul>
	 *       </li>
	 *       <li>若血盟未登錄推薦系統:
	 *         <ul>
	 *           <li>發送訊息 3246 (建議登錄至推薦系統)</li>
	 *         </ul>
	 *       </li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 *
	 * <h4>情境 2: 玩家未加入血盟 (pc.getClanid() == 0)</h4>
	 * <ol>
	 *   <li><b>王族職業 (pc.isCrown()):</b>
	 *     <ul>
	 *       <li>發送訊息 3247 (提示可建立血盟或申請加入血盟)</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>非王族職業:</b>
	 *     <ul>
	 *       <li>若玩家未申請任何血盟，發送訊息 3245 (建議申請加入血盟)</li>
	 *       <li>若玩家已提交申請，不發送任何訊息</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 *
	 * <h3>伺服器訊息 ID 說明:</h3>
	 * <table border="1">
	 *   <tr><th>訊息 ID</th><th>發送對象</th><th>內容說明</th></tr>
	 *   <tr><td>3245</td><td>未加入血盟的非王族玩家</td><td>建議申請加入血盟</td></tr>
	 *   <tr><td>3246</td><td>血盟管理階層</td><td>建議將血盟登錄至推薦系統</td></tr>
	 *   <tr><td>3247</td><td>未加入血盟的王族玩家</td><td>提示可建立或申請加入血盟</td></tr>
	 *   <tr><td>3248</td><td>血盟管理階層</td><td>提示有玩家申請待審核</td></tr>
	 * </table>
	 *
	 * <h3>血盟推薦系統:</h3>
	 * <p>血盟推薦系統是一個媒合平台，允許:
	 * <ul>
	 *   <li><b>血盟方:</b> 登錄血盟資訊至推薦系統，開放讓玩家申請</li>
	 *   <li><b>玩家方:</b> 瀏覽已登錄的血盟並提交加入申請</li>
	 *   <li><b>管理方:</b> 血盟管理階層審核加入申請並決定是否接受</li>
	 * </ul>
	 *
	 * <h3>執行時機:</h3>
	 * <p>在角色登入流程的最後階段執行，於角色完全載入後，
	 * 向符合條件的玩家發送血盟推薦系統的相關提示。
	 *
	 * <h3>設計目的:</h3>
	 * <ul>
	 *   <li>鼓勵血盟主動招募成員</li>
	 *   <li>幫助獨行玩家找到適合的血盟</li>
	 *   <li>提升血盟活躍度與社交互動</li>
	 *   <li>減少玩家手動尋找血盟的困難</li>
	 * </ul>
	 *
	 * @param pc 要檢查推薦系統狀態的角色實例
	 * @see l1j.server.server.datatables.ClanRecommendTable
	 * @see l1j.server.server.model.L1Clan
	 * @see l1j.server.server.serverpackets.S_ClanAttention
	 * @see l1j.server.server.serverpackets.S_PacketBox
	 * @see l1j.server.server.serverpackets.S_ServerMessage
	 */
	private void checkPledgeRecommendation(L1PcInstance pc){
		// ==================== 情境 1: 玩家已加入血盟 ====================
		if(pc.getClanid() > 0){
			// 發送血盟徽章注意事項
			pc.sendPackets(new S_ClanAttention());
			// 發送血盟徽章狀態
			pc.sendPackets(new S_PacketBox(S_PacketBox.PLEDGE_EMBLEM_STATUS, pc.getClan().getEmblemStatus()));

			// 檢查玩家是否有管理血盟的權限
			// 只有君主、守護騎士、聯盟守護騎士、聯盟副君主、聯盟君主有權限
			if(pc.getClanRank() == L1Clan.CLAN_RANK_PRINCE || pc.getClanRank() == L1Clan.CLAN_RANK_GUARDIAN
			 ||pc.getClanRank() == L1Clan.CLAN_RANK_LEAGUE_GUARDIAN || pc.getClanRank() == L1Clan.CLAN_RANK_LEAGUE_VICEPRINCE
		     ||pc.getClanRank() == L1Clan.CLAN_RANK_LEAGUE_PRINCE){

				// ==================== 檢查血盟是否已登錄推薦系統 ====================
				if(ClanRecommendTable.getInstance().isRecorded(pc.getClanid())){
					// 血盟已登錄推薦系統，檢查是否有玩家申請加入
					if(ClanRecommendTable.getInstance().isClanApplyByPlayer(pc.getClanid())){
						// 發送訊息 3248: 提示有玩家申請待審核
						pc.sendPackets(new S_ServerMessage(3248));
					}
				} else {
					// 血盟尚未登錄推薦系統
					// 重新發送血盟徽章狀態與注意事項
					pc.sendPackets(new S_PacketBox(S_PacketBox.PLEDGE_EMBLEM_STATUS, pc.getClan().getEmblemStatus()));
					pc.sendPackets(new S_ClanAttention());
					// 發送訊息 3246: 建議將血盟登錄至推薦系統
					pc.sendPackets(new S_ServerMessage(3246));
				}
			}
		}
		// ==================== 情境 2: 玩家未加入血盟 ====================
		else {
			// ==================== 王族職業 ====================
			if(pc.isCrown()){
				// 發送訊息 3247: 提示可建立血盟或申請加入血盟
				pc.sendPackets(new S_ServerMessage(3247));
			}
			// ==================== 非王族職業 ====================
			else {
				// 檢查玩家是否已申請加入其他血盟
				if(ClanRecommendTable.getInstance().isApplied(pc.getName())){
					// 已提交申請，不發送任何訊息
				} else {
					// 尚未申請，發送訊息 3245: 建議申請加入血盟
					pc.sendPackets(new S_ServerMessage(3245));
				}
			}
		}

	}
	

	/**
	 * 取得封包類型識別字串
	 * <p>返回此封包處理器的類型識別常數，用於封包路由與日誌記錄。
	 *
	 * <h3>用途:</h3>
	 * <ul>
	 *   <li><b>封包路由:</b> {@link l1j.server.server.PacketHandler} 使用此識別字串將封包路由至正確的處理器</li>
	 *   <li><b>日誌記錄:</b> 記錄封包類型以便追蹤與除錯</li>
	 *   <li><b>效能監控:</b> 統計各類型封包的處理次數與效能</li>
	 *   <li><b>安全檢查:</b> 根據封包類型進行特定的安全驗證</li>
	 * </ul>
	 *
	 * <h3>常數定義:</h3>
	 * <p>{@code C_LOGIN_TO_SERVER} 常數定義於 {@link l1j.server.server.Opcodes} 類別中，
	 * 對應客戶端發送的角色登入封包 Opcode。
	 *
	 * <h3>設計模式:</h3>
	 * <p>此方法實作了 {@link l1j.server.server.clientpackets.ClientBasePacket#getType()} 抽象方法，
	 * 使用模板方法模式 (Template Method Pattern) 確保所有封包處理器都提供類型識別。
	 *
	 * @return 封包類型識別字串 (C_LOGIN_TO_SERVER)
	 * @see l1j.server.server.Opcodes#C_LOGIN_TO_SERVER
	 * @see l1j.server.server.PacketHandler
	 * @see l1j.server.server.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType() {
		// 返回封包類型識別常數，用於封包路由與日誌記錄
		return C_LOGIN_TO_SERVER;
	}
}
