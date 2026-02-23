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

import static l1j.server.server.Opcodes.C_OPCODE_ADDBUDDY;
import static l1j.server.server.Opcodes.C_OPCODE_AMOUNT;
import static l1j.server.server.Opcodes.C_OPCODE_ARROWATTACK;
import static l1j.server.server.Opcodes.C_OPCODE_ATTACK;
import static l1j.server.server.Opcodes.C_OPCODE_ATTR;
import static l1j.server.server.Opcodes.C_OPCODE_BANCLAN;
import static l1j.server.server.Opcodes.C_OPCODE_BANPARTY;
import static l1j.server.server.Opcodes.C_OPCODE_BOARD;
import static l1j.server.server.Opcodes.C_OPCODE_BOARDDELETE;
import static l1j.server.server.Opcodes.C_OPCODE_BOARDREAD;
import static l1j.server.server.Opcodes.C_OPCODE_BOARDWRITE;
import static l1j.server.server.Opcodes.C_OPCODE_BOOKMARK;
import static l1j.server.server.Opcodes.C_OPCODE_BOOKMARKDELETE;
import static l1j.server.server.Opcodes.C_OPCODE_BUDDYLIST;
import static l1j.server.server.Opcodes.C_OPCODE_BEANFUNLOGINPACKET;
import static l1j.server.server.Opcodes.C_OPCODE_CAHTPARTY;
import static l1j.server.server.Opcodes.C_OPCODE_CALL;
import static l1j.server.server.Opcodes.C_OPCODE_CHANGECHAR;
import static l1j.server.server.Opcodes.C_OPCODE_CHANGEHEADING;
import static l1j.server.server.Opcodes.C_OPCODE_CHANGEWARTIME;
import static l1j.server.server.Opcodes.C_OPCODE_CHARACTERCONFIG;
import static l1j.server.server.Opcodes.C_OPCODE_CHARRESET;
import static l1j.server.server.Opcodes.C_OPCODE_CHAT;
import static l1j.server.server.Opcodes.C_OPCODE_CHATGLOBAL;
import static l1j.server.server.Opcodes.C_OPCODE_CHATWHISPER;
import static l1j.server.server.Opcodes.C_OPCODE_CHECKPK;
import static l1j.server.server.Opcodes.C_OPCODE_CLIENTVERSION;
import static l1j.server.server.Opcodes.C_OPCODE_CREATECLAN;
import static l1j.server.server.Opcodes.C_OPCODE_CREATEPARTY;
import static l1j.server.server.Opcodes.C_OPCODE_DRAWAL;
import static l1j.server.server.Opcodes.C_OPCODE_DELBUDDY;
import static l1j.server.server.Opcodes.C_OPCODE_DEPOSIT;
import static l1j.server.server.Opcodes.C_OPCODE_DELETECHAR;
import static l1j.server.server.Opcodes.C_OPCODE_DELETEINVENTORYITEM;
import static l1j.server.server.Opcodes.C_OPCODE_DOOR;
import static l1j.server.server.Opcodes.C_OPCODE_DROPITEM;
import static l1j.server.server.Opcodes.C_OPCODE_EMBLEMUPLOAD;
import static l1j.server.server.Opcodes.C_OPCODE_EMBLEMDOWNLOAD;
import static l1j.server.server.Opcodes.C_OPCODE_ENTERPORTAL;
import static l1j.server.server.Opcodes.C_OPCODE_EXCLUDE;
import static l1j.server.server.Opcodes.C_OPCODE_EXIT_GHOST;
import static l1j.server.server.Opcodes.C_OPCODE_EXTCOMMAND;
import static l1j.server.server.Opcodes.C_OPCODE_FIGHT;
import static l1j.server.server.Opcodes.C_OPCODE_FISHCLICK;
import static l1j.server.server.Opcodes.C_OPCODE_FIX_WEAPON_LIST;
import static l1j.server.server.Opcodes.C_OPCODE_GIVEITEM;
import static l1j.server.server.Opcodes.C_OPCODE_HIRESOLDIER;
import static l1j.server.server.Opcodes.C_OPCODE_JOINCLAN;
import static l1j.server.server.Opcodes.C_OPCODE_KEEPALIVE;
import static l1j.server.server.Opcodes.C_OPCODE_LEAVECLANE;
import static l1j.server.server.Opcodes.C_OPCODE_LEAVEPARTY;
import static l1j.server.server.Opcodes.C_OPCODE_LOGINPACKET;
import static l1j.server.server.Opcodes.C_OPCODE_LOGINTOSERVER;
import static l1j.server.server.Opcodes.C_OPCODE_LOGINTOSERVEROK;
import static l1j.server.server.Opcodes.C_OPCODE_MAIL;
import static l1j.server.server.Opcodes.C_OPCODE_MOVECHAR;
import static l1j.server.server.Opcodes.C_OPCODE_NEWCHAR;
import static l1j.server.server.Opcodes.C_OPCODE_NPCACTION;
import static l1j.server.server.Opcodes.C_OPCODE_NPCTALK;
import static l1j.server.server.Opcodes.C_OPCODE_PARTYLIST;
import static l1j.server.server.Opcodes.C_OPCODE_PETMENU;
import static l1j.server.server.Opcodes.C_OPCODE_PICKUPITEM;
import static l1j.server.server.Opcodes.C_OPCODE_PLEDGE;
import static l1j.server.server.Opcodes.C_OPCODE_PRIVATESHOPLIST;
import static l1j.server.server.Opcodes.C_OPCODE_PLEDGECONTENT;
import static l1j.server.server.Opcodes.C_OPCODE_PLEDGE_RECOMMENDATION;
import static l1j.server.server.Opcodes.C_OPCODE_PROPOSE;
import static l1j.server.server.Opcodes.C_OPCODE_QUITGAME;
import static l1j.server.server.Opcodes.C_OPCODE_RESTART;
import static l1j.server.server.Opcodes.C_OPCODE_RESULT;
import static l1j.server.server.Opcodes.C_OPCODE_SELECTLIST;
import static l1j.server.server.Opcodes.C_OPCODE_SELECTTARGET;
import static l1j.server.server.Opcodes.C_OPCODE_SENDLOCATION;
import static l1j.server.server.Opcodes.C_OPCODE_SHIP;
import static l1j.server.server.Opcodes.C_OPCODE_SHOP;
import static l1j.server.server.Opcodes.C_OPCODE_SKILLBUY;
import static l1j.server.server.Opcodes.C_OPCODE_SKILLBUYOK;
import static l1j.server.server.Opcodes.C_OPCODE_TELEPORT;
import static l1j.server.server.Opcodes.C_OPCODE_TITLE;
import static l1j.server.server.Opcodes.C_OPCODE_TRADE;
import static l1j.server.server.Opcodes.C_OPCODE_TAXRATE;
import static l1j.server.server.Opcodes.C_OPCODE_TRADEADDCANCEL;
import static l1j.server.server.Opcodes.C_OPCODE_TRADEADDITEM;
import static l1j.server.server.Opcodes.C_OPCODE_TRADEADDOK;
import static l1j.server.server.Opcodes.C_OPCODE_USEITEM;
import static l1j.server.server.Opcodes.C_OPCODE_USEPETITEM;
import static l1j.server.server.Opcodes.C_OPCODE_USESKILL;
import static l1j.server.server.Opcodes.C_OPCODE_WAR;
import static l1j.server.server.Opcodes.C_OPCODE_WAREHOUSELOCK;
import static l1j.server.server.Opcodes.C_OPCODE_WHO;
import static l1j.server.server.Opcodes.C_OPCODE_RESTARTMENU;
import l1j.server.server.clientpackets.C_AddBookmark;
import l1j.server.server.clientpackets.C_AddBuddy;
import l1j.server.server.clientpackets.C_Amount;
import l1j.server.server.clientpackets.C_Attack;
import l1j.server.server.clientpackets.C_Attr;
import l1j.server.server.clientpackets.C_AuthLogin;
import l1j.server.server.clientpackets.C_BanClan;
import l1j.server.server.clientpackets.C_BanParty;
import l1j.server.server.clientpackets.C_Board;
import l1j.server.server.clientpackets.C_BoardDelete;
import l1j.server.server.clientpackets.C_BoardRead;
import l1j.server.server.clientpackets.C_BoardWrite;
import l1j.server.server.clientpackets.C_Buddy;
import l1j.server.server.clientpackets.C_CallPlayer;
import l1j.server.server.clientpackets.C_ChangeHeading;
import l1j.server.server.clientpackets.C_ChangeWarTime;
import l1j.server.server.clientpackets.C_CharReset;
import l1j.server.server.clientpackets.C_CharcterConfig;
import l1j.server.server.clientpackets.C_Chat;
import l1j.server.server.clientpackets.C_ChatParty;
import l1j.server.server.clientpackets.C_ChatWhisper;
import l1j.server.server.clientpackets.C_CheckPK;
import l1j.server.server.clientpackets.C_CreateChar;
import l1j.server.server.clientpackets.C_CreateClan;
import l1j.server.server.clientpackets.C_CreateParty;
import l1j.server.server.clientpackets.C_DelBuddy;
import l1j.server.server.clientpackets.C_DeleteBookmark;
import l1j.server.server.clientpackets.C_DeleteChar;
import l1j.server.server.clientpackets.C_DeleteInventoryItem;
import l1j.server.server.clientpackets.C_Deposit;
import l1j.server.server.clientpackets.C_Door;
import l1j.server.server.clientpackets.C_Drawal;
import l1j.server.server.clientpackets.C_DropItem;
import l1j.server.server.clientpackets.C_EmblemUpload;
import l1j.server.server.clientpackets.C_EmblemDownload;
import l1j.server.server.clientpackets.C_EnterPortal;
import l1j.server.server.clientpackets.C_Exclude;
import l1j.server.server.clientpackets.C_ExitGhost;
import l1j.server.server.clientpackets.C_ExtraCommand;
import l1j.server.server.clientpackets.C_Fight;
import l1j.server.server.clientpackets.C_FishClick;
import l1j.server.server.clientpackets.C_FixWeaponList;
import l1j.server.server.clientpackets.C_GiveItem;
import l1j.server.server.clientpackets.C_HireSoldier;
import l1j.server.server.clientpackets.C_ItemUSe;
import l1j.server.server.clientpackets.C_JoinClan;
import l1j.server.server.clientpackets.C_KeepALIVE;
import l1j.server.server.clientpackets.C_LeaveClan;
import l1j.server.server.clientpackets.C_LeaveParty;
import l1j.server.server.clientpackets.C_LoginToServer;
import l1j.server.server.clientpackets.C_LoginToServerOK;
import l1j.server.server.clientpackets.C_Mail;
import l1j.server.server.clientpackets.C_MoveChar;
import l1j.server.server.clientpackets.C_NPCAction;
import l1j.server.server.clientpackets.C_NPCTalk;
import l1j.server.server.clientpackets.C_NewCharSelect;
import l1j.server.server.clientpackets.C_Party;
import l1j.server.server.clientpackets.C_PetMenu;
import l1j.server.server.clientpackets.C_PickUpItem;
import l1j.server.server.clientpackets.C_Pledge;
import l1j.server.server.clientpackets.C_PledgeContent;
import l1j.server.server.clientpackets.C_PledgeRecommendation;
import l1j.server.server.clientpackets.C_Propose;
import l1j.server.server.clientpackets.C_Restart;
import l1j.server.server.clientpackets.C_RestartMenu;
import l1j.server.server.clientpackets.C_Result;
import l1j.server.server.clientpackets.C_SelectList;
import l1j.server.server.clientpackets.C_SelectTarget;
import l1j.server.server.clientpackets.C_SendLocation;
import l1j.server.server.clientpackets.C_ServerVersion;
import l1j.server.server.clientpackets.C_Ship;
import l1j.server.server.clientpackets.C_Shop;
import l1j.server.server.clientpackets.C_ShopList;
import l1j.server.server.clientpackets.C_SkillBuy;
import l1j.server.server.clientpackets.C_SkillBuyOK;
import l1j.server.server.clientpackets.C_TaxRate;
import l1j.server.server.clientpackets.C_Teleport;
import l1j.server.server.clientpackets.C_Title;
import l1j.server.server.clientpackets.C_Trade;
import l1j.server.server.clientpackets.C_TradeAddItem;
import l1j.server.server.clientpackets.C_TradeCancel;
import l1j.server.server.clientpackets.C_TradeOK;
import l1j.server.server.clientpackets.C_UsePetItem;
import l1j.server.server.clientpackets.C_UseSkill;
import l1j.server.server.clientpackets.C_War;
import l1j.server.server.clientpackets.C_WarePassword;
import l1j.server.server.clientpackets.C_Who;
import l1j.server.server.model.Instance.L1PcInstance;

// Referenced classes of package l1j.server.server:
// Opcodes, LoginController, ClientThread, Logins

/**
 * 客戶端封包路由處理器。
 * <p>
 * 此類別負責接收從客戶端傳來的原始封包資料，解析封包中的 Opcode（操作碼），
 * 並根據 Opcode 將封包分派到對應的客戶端封包處理器類別進行處理。
 *
 * <h3>工作原理</h3>
 * <ol>
 * <li>從原始封包資料中讀取第一個 byte 作為 Opcode</li>
 * <li>使用 switch-case 結構比對 Opcode 與預定義的常數</li>
 * <li>根據比對結果建立對應的封包處理器實例（C_* 類別）</li>
 * <li>封包處理器的建構子會自動執行封包解析與業務邏輯</li>
 * </ol>
 *
 * <h3>封包處理流程</h3>
 * <pre>
 * ClientThread 接收原始 bytes
 *     ↓
 * PacketHandler.handlePacket()
 *     ↓
 * 提取 Opcode (第一個 byte)
 *     ↓
 * Switch-case 比對 Opcode
 *     ↓
 * 建立對應的 C_* 處理器實例
 *     ↓
 * 處理器執行業務邏輯並回傳結果給客戶端
 * </pre>
 *
 * <h3>封包分類</h3>
 * 此處理器支援以下主要封包類型：
 * <ul>
 * <li><b>帳號與角色管理</b>：登入驗證、角色選擇、創建角色、刪除角色、登出</li>
 * <li><b>移動與位置</b>：角色移動、改變朝向、傳送、進入傳送門</li>
 * <li><b>戰鬥系統</b>：近戰攻擊、遠程攻擊、使用技能、選擇目標</li>
 * <li><b>物品系統</b>：撿取物品、丟棄物品、使用物品、刪除物品、交易</li>
 * <li><b>社交系統</b>：聊天（一般、密語、全域）、好友管理、組隊、結婚提議</li>
 * <li><b>血盟系統</b>：創建血盟、加入血盟、離開血盟、驅逐成員、血盟戰爭、上傳/下載徽章、血盟推薦</li>
 * <li><b>NPC 互動</b>：對話、商店購買、技能學習、雇傭士兵、傭兵選單</li>
 * <li><b>介面操作</b>：書籤管理、倉庫存取、郵件、告示板、物品數量輸入</li>
 * <li><b>城堡與領地</b>：稅率設定、戰爭時間、雇傭守衛</li>
 * <li><b>寵物系統</b>：寵物選單、使用寵物物品</li>
 * <li><b>其他功能</b>：心跳包、客戶端版本檢查、屬性點分配、角色重置、定點釣魚、角色配置</li>
 * </ul>
 *
 * <h3>設計特點</h3>
 * <ul>
 * <li><b>無狀態設計</b>：每次封包處理都會建立新的處理器實例，避免狀態污染</li>
 * <li><b>命名規則</b>：所有客戶端封包處理器類別以 "C_" 開頭，對應的伺服器封包以 "S_" 開頭</li>
 * <li><b>統一路由</b>：所有客戶端封包都經過此類別進行統一分派</li>
 * <li><b>錯誤容忍</b>：未知的 Opcode 會被忽略，不會導致伺服器崩潰</li>
 * </ul>
 *
 * <h3>與其他類別的關係</h3>
 * <ul>
 * <li>{@link ClientThread}：負責維護與客戶端的網路連線，並呼叫此類別處理封包</li>
 * <li>{@link Opcodes}：定義所有封包的 Opcode 常數</li>
 * <li>C_* 類別：各種具體的客戶端封包處理器實作</li>
 * <li>{@link L1PcInstance}：代表當前處理封包的玩家角色實例</li>
 * </ul>
 *
 * @see ClientThread
 * @see Opcodes
 * @see l1j.server.server.clientpackets.C_Chat
 * @see l1j.server.server.clientpackets.C_MoveChar
 * @see l1j.server.server.clientpackets.C_Attack
 * @see l1j.server.server.clientpackets.C_AuthLogin
 */
public class PacketHandler {

	/**
	 * 建構客戶端封包路由處理器。
	 * <p>
	 * 初始化封包處理器並綁定到指定的客戶端連線執行緒。
	 * 此處理器將使用綁定的 ClientThread 來回傳處理結果給客戶端。
	 *
	 * @param clientthread 客戶端連線執行緒，用於回傳封包處理結果
	 * @see ClientThread
	 */
	public PacketHandler(ClientThread clientthread) {
		_client = clientthread;
	}

	/**
	 * 處理客戶端封包的核心方法。
	 * <p>
	 * 此方法是整個封包路由系統的核心，負責解析封包中的 Opcode 並分派到對應的處理器。
	 *
	 * <h3>處理流程</h3>
	 * <ol>
	 * <li>從封包資料的第一個 byte 提取 Opcode（使用 & 0xff 轉換為無符號整數）</li>
	 * <li>使用 switch-case 結構比對 Opcode</li>
	 * <li>根據 Opcode 建立對應的客戶端封包處理器實例</li>
	 * <li>處理器實例在建構子中自動執行封包解析與業務邏輯</li>
	 * <li>如果 Opcode 未被識別，則忽略此封包（不會拋出例外）</li>
	 * </ol>
	 *
	 * <h3>主要封包類型說明</h3>
	 *
	 * <h4>帳號與角色管理</h4>
	 * <ul>
	 * <li>{@code C_OPCODE_LOGINPACKET} / {@code C_OPCODE_BEANFUNLOGINPACKET} - 帳號登入驗證</li>
	 * <li>{@code C_OPCODE_LOGINTOSERVER} - 選擇伺服器</li>
	 * <li>{@code C_OPCODE_LOGINTOSERVEROK} - 確認進入伺服器</li>
	 * <li>{@code C_OPCODE_NEWCHAR} - 創建新角色</li>
	 * <li>{@code C_OPCODE_DELETECHAR} - 刪除角色</li>
	 * <li>{@code C_OPCODE_CHANGECHAR} - 切換角色</li>
	 * <li>{@code C_OPCODE_RESTART} - 重新開始（返回角色選擇畫面）</li>
	 * <li>{@code C_OPCODE_RESTARTMENU} - 重新開始選單</li>
	 * <li>{@code C_OPCODE_CLIENTVERSION} - 客戶端版本檢查</li>
	 * <li>{@code C_OPCODE_CHARRESET} - 角色重置</li>
	 * </ul>
	 *
	 * <h4>移動與位置</h4>
	 * <ul>
	 * <li>{@code C_OPCODE_MOVECHAR} - 角色移動</li>
	 * <li>{@code C_OPCODE_CHANGEHEADING} - 改變朝向</li>
	 * <li>{@code C_OPCODE_TELEPORT} - 傳送</li>
	 * <li>{@code C_OPCODE_ENTERPORTAL} - 進入傳送門</li>
	 * <li>{@code C_OPCODE_SENDLOCATION} - 發送位置資訊</li>
	 * <li>{@code C_OPCODE_SHIP} - 船隻相關</li>
	 * </ul>
	 *
	 * <h4>戰鬥系統</h4>
	 * <ul>
	 * <li>{@code C_OPCODE_ATTACK} - 近戰攻擊</li>
	 * <li>{@code C_OPCODE_ARROWATTACK} - 遠程攻擊（弓箭）</li>
	 * <li>{@code C_OPCODE_USESKILL} - 使用技能</li>
	 * <li>{@code C_OPCODE_SELECTTARGET} - 選擇目標</li>
	 * <li>{@code C_OPCODE_FIGHT} - 戰鬥模式切換</li>
	 * <li>{@code C_OPCODE_FIX_WEAPON_LIST} - 固定武器列表</li>
	 * </ul>
	 *
	 * <h4>物品系統</h4>
	 * <ul>
	 * <li>{@code C_OPCODE_PICKUPITEM} - 撿取物品</li>
	 * <li>{@code C_OPCODE_DROPITEM} - 丟棄物品</li>
	 * <li>{@code C_OPCODE_USEITEM} - 使用物品</li>
	 * <li>{@code C_OPCODE_DELETEINVENTORYITEM} - 刪除背包物品</li>
	 * <li>{@code C_OPCODE_GIVEITEM} - 給予物品（交易/丟給其他玩家）</li>
	 * <li>{@code C_OPCODE_AMOUNT} - 物品數量輸入</li>
	 * </ul>
	 *
	 * <h4>交易系統</h4>
	 * <ul>
	 * <li>{@code C_OPCODE_TRADE} - 開始交易</li>
	 * <li>{@code C_OPCODE_TRADEADDITEM} - 交易中加入物品</li>
	 * <li>{@code C_OPCODE_TRADEADDOK} - 確認交易</li>
	 * <li>{@code C_OPCODE_TRADEADDCANCEL} - 取消交易</li>
	 * <li>{@code C_OPCODE_PRIVATESHOPLIST} - 個人商店列表</li>
	 * </ul>
	 *
	 * <h4>社交系統</h4>
	 * <ul>
	 * <li>{@code C_OPCODE_CHAT} - 一般聊天</li>
	 * <li>{@code C_OPCODE_CHATWHISPER} - 密語</li>
	 * <li>{@code C_OPCODE_CHATGLOBAL} - 全域聊天</li>
	 * <li>{@code C_OPCODE_CAHTPARTY} - 組隊聊天</li>
	 * <li>{@code C_OPCODE_ADDBUDDY} - 新增好友</li>
	 * <li>{@code C_OPCODE_DELBUDDY} - 刪除好友</li>
	 * <li>{@code C_OPCODE_BUDDYLIST} - 好友列表</li>
	 * <li>{@code C_OPCODE_WHO} - 查詢線上玩家</li>
	 * <li>{@code C_OPCODE_CALL} - 呼叫玩家</li>
	 * <li>{@code C_OPCODE_PROPOSE} - 結婚提議</li>
	 * </ul>
	 *
	 * <h4>組隊系統</h4>
	 * <ul>
	 * <li>{@code C_OPCODE_CREATEPARTY} - 創建隊伍</li>
	 * <li>{@code C_OPCODE_PARTYLIST} - 隊伍列表</li>
	 * <li>{@code C_OPCODE_LEAVEPARTY} - 離開隊伍</li>
	 * <li>{@code C_OPCODE_BANPARTY} - 驅逐隊員</li>
	 * </ul>
	 *
	 * <h4>血盟系統</h4>
	 * <ul>
	 * <li>{@code C_OPCODE_CREATECLAN} - 創建血盟</li>
	 * <li>{@code C_OPCODE_JOINCLAN} - 加入血盟</li>
	 * <li>{@code C_OPCODE_LEAVECLANE} - 離開血盟</li>
	 * <li>{@code C_OPCODE_BANCLAN} - 驅逐血盟成員</li>
	 * <li>{@code C_OPCODE_PLEDGE} - 血盟相關操作</li>
	 * <li>{@code C_OPCODE_PLEDGECONTENT} - 血盟內容</li>
	 * <li>{@code C_OPCODE_PLEDGE_RECOMMENDATION} - 血盟推薦</li>
	 * <li>{@code C_OPCODE_WAR} - 血盟戰爭</li>
	 * <li>{@code C_OPCODE_CHANGEWARTIME} - 變更戰爭時間</li>
	 * <li>{@code C_OPCODE_EMBLEMUPLOAD} - 上傳血盟徽章</li>
	 * <li>{@code C_OPCODE_EMBLEMDOWNLOAD} - 下載血盟徽章</li>
	 * </ul>
	 *
	 * <h4>NPC 互動</h4>
	 * <ul>
	 * <li>{@code C_OPCODE_NPCTALK} - NPC 對話</li>
	 * <li>{@code C_OPCODE_NPCACTION} - NPC 動作</li>
	 * <li>{@code C_OPCODE_SHOP} - 商店購買</li>
	 * <li>{@code C_OPCODE_SKILLBUY} - 技能學習</li>
	 * <li>{@code C_OPCODE_SKILLBUYOK} - 確認技能學習</li>
	 * <li>{@code C_OPCODE_SELECTLIST} - 選擇列表（NPC 選項）</li>
	 * <li>{@code C_OPCODE_HIRESOLDIER} - 雇傭士兵</li>
	 * </ul>
	 *
	 * <h4>倉庫與郵件</h4>
	 * <ul>
	 * <li>{@code C_OPCODE_DEPOSIT} - 存入倉庫</li>
	 * <li>{@code C_OPCODE_DRAWAL} - 取出倉庫</li>
	 * <li>{@code C_OPCODE_WAREHOUSELOCK} - 倉庫密碼</li>
	 * <li>{@code C_OPCODE_MAIL} - 郵件</li>
	 * </ul>
	 *
	 * <h4>介面與配置</h4>
	 * <ul>
	 * <li>{@code C_OPCODE_BOOKMARK} - 新增書籤</li>
	 * <li>{@code C_OPCODE_BOOKMARKDELETE} - 刪除書籤</li>
	 * <li>{@code C_OPCODE_CHARACTERCONFIG} - 角色配置</li>
	 * <li>{@code C_OPCODE_ATTR} - 屬性點分配</li>
	 * <li>{@code C_OPCODE_TITLE} - 稱號</li>
	 * <li>{@code C_OPCODE_EXCLUDE} - 排除（屏蔽玩家）</li>
	 * </ul>
	 *
	 * <h4>告示板</h4>
	 * <ul>
	 * <li>{@code C_OPCODE_BOARD} - 告示板</li>
	 * <li>{@code C_OPCODE_BOARDREAD} - 讀取告示</li>
	 * <li>{@code C_OPCODE_BOARDWRITE} - 撰寫告示</li>
	 * <li>{@code C_OPCODE_BOARDDELETE} - 刪除告示</li>
	 * </ul>
	 *
	 * <h4>城堡與領地</h4>
	 * <ul>
	 * <li>{@code C_OPCODE_TAXRATE} - 稅率設定</li>
	 * <li>{@code C_OPCODE_DOOR} - 城門控制</li>
	 * </ul>
	 *
	 * <h4>寵物系統</h4>
	 * <ul>
	 * <li>{@code C_OPCODE_PETMENU} - 寵物選單</li>
	 * <li>{@code C_OPCODE_USEPETITEM} - 使用寵物物品</li>
	 * </ul>
	 *
	 * <h4>其他功能</h4>
	 * <ul>
	 * <li>{@code C_OPCODE_KEEPALIVE} - 心跳包（保持連線）</li>
	 * <li>{@code C_OPCODE_RESULT} - 結果確認</li>
	 * <li>{@code C_OPCODE_EXTCOMMAND} - 擴展指令</li>
	 * <li>{@code C_OPCODE_CHECKPK} - 檢查 PK 狀態</li>
	 * <li>{@code C_OPCODE_EXIT_GHOST} - 退出觀察者模式</li>
	 * <li>{@code C_OPCODE_FISHCLICK} - 釣魚點擊</li>
	 * <li>{@code C_OPCODE_QUITGAME} - 退出遊戲（客戶端發送快捷列與背包狀態）</li>
	 * </ul>
	 *
	 * <h3>錯誤處理</h3>
	 * <ul>
	 * <li>未知的 Opcode 會進入 default 分支，被靜默忽略</li>
	 * <li>不會因為未知封包而中斷連線或拋出例外</li>
	 * <li>有助於容忍客戶端版本差異或惡意封包</li>
	 * </ul>
	 *
	 * <h3>設計考量</h3>
	 * <ul>
	 * <li><b>無狀態</b>：每個封包都建立新的處理器實例，避免狀態衝突</li>
	 * <li><b>自動執行</b>：處理器的建構子會自動執行業務邏輯，無需手動呼叫方法</li>
	 * <li><b>集中管理</b>：所有封包路由邏輯集中在此方法中，便於維護</li>
	 * </ul>
	 *
	 * @param abyte0 客戶端傳送的原始封包資料（已解密），第一個 byte 為 Opcode
	 * @param object 當前處理封包的玩家角色實例，登入階段可能為 null
	 * @throws Exception 處理封包過程中可能拋出的例外
	 *
	 * @see Opcodes
	 * @see ClientThread
	 * @see L1PcInstance
	 */
	public void handlePacket(byte abyte0[], L1PcInstance object) throws Exception {
		int i = abyte0[0] & 0xff;

		switch (i) {
			case C_OPCODE_EXCLUDE:
				new C_Exclude(abyte0, _client);
				break;

			case C_OPCODE_CHARACTERCONFIG:
				new C_CharcterConfig(abyte0, _client);
				break;

			case C_OPCODE_DOOR:
				new C_Door(abyte0, _client);
				break;

			case C_OPCODE_TITLE:
				new C_Title(abyte0, _client);
				break;

			case C_OPCODE_BOARDDELETE:
				new C_BoardDelete(abyte0, _client);
				break;

			case C_OPCODE_PLEDGE:
				new C_Pledge(abyte0, _client);
				break;

			case C_OPCODE_CHANGEHEADING:
				new C_ChangeHeading(abyte0, _client);
				break;

			case C_OPCODE_NPCACTION:
				new C_NPCAction(abyte0, _client);
				break;

			case C_OPCODE_USESKILL:
				new C_UseSkill(abyte0, _client);
				break;

			case C_OPCODE_EMBLEMUPLOAD:
				new C_EmblemUpload(abyte0, _client);
				break;
				
			case C_OPCODE_TAXRATE:
				new C_TaxRate(abyte0, _client);
				break;
				
			case C_OPCODE_EMBLEMDOWNLOAD:
				new C_EmblemDownload(abyte0, _client);
				break;

			case C_OPCODE_TRADEADDCANCEL:
				new C_TradeCancel(abyte0, _client);
				break;

			case C_OPCODE_CHANGEWARTIME:
				new C_ChangeWarTime(abyte0, _client);
				break;

			case C_OPCODE_BOOKMARK:
				new C_AddBookmark(abyte0, _client);
				break;

			case C_OPCODE_CREATECLAN:
				new C_CreateClan(abyte0, _client);
				break;

			case C_OPCODE_CLIENTVERSION:
				new C_ServerVersion(abyte0, _client);
				break;
			
			case C_OPCODE_DRAWAL:
				new C_Drawal(abyte0, _client);
				break;
				
			case C_OPCODE_DEPOSIT:
				new C_Deposit(abyte0, _client);
				break;

			case C_OPCODE_PROPOSE:
				new C_Propose(abyte0, _client);
				break;

			case C_OPCODE_SKILLBUY:
				new C_SkillBuy(abyte0, _client);
				break;

			case C_OPCODE_SHOP:
				new C_Shop(abyte0, _client);
				break;

			case C_OPCODE_BOARDREAD:
				new C_BoardRead(abyte0, _client);
				break;

			case C_OPCODE_TRADE:
				new C_Trade(abyte0, _client);
				break;

			case C_OPCODE_DELETECHAR:
				new C_DeleteChar(abyte0, _client);
				break;

			case C_OPCODE_KEEPALIVE:
				new C_KeepALIVE(abyte0, _client);
				break;

			case C_OPCODE_ATTR:
				new C_Attr(abyte0, _client);
				break;

			case C_OPCODE_LOGINPACKET:
				new C_AuthLogin(abyte0, _client);
				break;
				
			case C_OPCODE_BEANFUNLOGINPACKET:
				new C_AuthLogin(abyte0, _client);
				break;

			case C_OPCODE_RESULT:
				new C_Result(abyte0, _client);
				break;

			case C_OPCODE_LOGINTOSERVEROK:
				new C_LoginToServerOK(abyte0, _client);
				break;

		    case C_OPCODE_SKILLBUYOK:
				new C_SkillBuyOK(abyte0, _client);
				break;

			case C_OPCODE_TRADEADDITEM:
				new C_TradeAddItem(abyte0, _client);
				break;
				
			case C_OPCODE_ADDBUDDY:
				new C_AddBuddy(abyte0, _client);
				break;

			case C_OPCODE_CHAT:
				new C_Chat(abyte0, _client);
				break;

			case C_OPCODE_TRADEADDOK:
				new C_TradeOK(abyte0, _client);
				break;

			case C_OPCODE_CHECKPK:
				new C_CheckPK(abyte0, _client);
				break;

			case C_OPCODE_CHANGECHAR:
				new C_NewCharSelect(abyte0, _client);
				break;

			case C_OPCODE_BUDDYLIST:
				new C_Buddy(abyte0, _client);
				break;

			case C_OPCODE_DROPITEM:
				new C_DropItem(abyte0, _client);
				break;

			case C_OPCODE_LEAVEPARTY:
				new C_LeaveParty(abyte0, _client);
				break;
				
			case C_OPCODE_PLEDGECONTENT:
				new C_PledgeContent(abyte0, _client);
				break;
				
			case C_OPCODE_PLEDGE_RECOMMENDATION:
				new C_PledgeRecommendation(abyte0, _client);
				break;
				
			case C_OPCODE_ATTACK:
			case C_OPCODE_ARROWATTACK:
				new C_Attack(abyte0, _client);
				break;

			// TODO 翻譯
			// キャラクターのショートカットやインベントリの状態がプレイ中に変動した場合に
			// ショートカットやインベントリの状態を付加してクライアントから送信されてくる
			// 送られてくるタイミングはクライアント終了時
			case C_OPCODE_QUITGAME:
				break;

			case C_OPCODE_BANCLAN:
				new C_BanClan(abyte0, _client);
				break;

			case C_OPCODE_BOARD:
				new C_Board(abyte0, _client);
				break;

			case C_OPCODE_DELETEINVENTORYITEM:
				new C_DeleteInventoryItem(abyte0, _client);
				break;

			case C_OPCODE_CHATWHISPER:
				new C_ChatWhisper(abyte0, _client);
				break;

			case C_OPCODE_PARTYLIST:
				new C_Party(abyte0, _client);
				break;

			case C_OPCODE_PICKUPITEM:
				new C_PickUpItem(abyte0, _client);
				break;

			case C_OPCODE_WHO:
				new C_Who(abyte0, _client);
				break;

			case C_OPCODE_GIVEITEM:
				new C_GiveItem(abyte0, _client);
				break;

			case C_OPCODE_MOVECHAR:
				System.out.println("PacketHandler: 處理 C_OPCODE_MOVECHAR");
				new C_MoveChar(abyte0, _client);
				break;

			case C_OPCODE_BOOKMARKDELETE:
				new C_DeleteBookmark(abyte0, _client);
				break;

			case C_OPCODE_RESTART:
				new C_Restart(abyte0, _client);
				break;

			case C_OPCODE_LEAVECLANE:
				new C_LeaveClan(abyte0, _client);
				break;

			case C_OPCODE_NPCTALK:
				new C_NPCTalk(abyte0, _client);
				break;

			case C_OPCODE_BANPARTY:
				new C_BanParty(abyte0, _client);
				break;

			case C_OPCODE_DELBUDDY:
				new C_DelBuddy(abyte0, _client);
				break;

			case C_OPCODE_WAR:
				new C_War(abyte0, _client);
				break;

			case C_OPCODE_LOGINTOSERVER:
				new C_LoginToServer(abyte0, _client);
				break;

			case C_OPCODE_PRIVATESHOPLIST:
				new C_ShopList(abyte0, _client);
				break;

			case C_OPCODE_CHATGLOBAL:
				new C_Chat(abyte0, _client);
				break;

			case C_OPCODE_JOINCLAN:
				new C_JoinClan(abyte0, _client);
				break;

			case C_OPCODE_NEWCHAR:
				new C_CreateChar(abyte0, _client);
				break;

			case C_OPCODE_EXTCOMMAND:
				new C_ExtraCommand(abyte0, _client);
				break;

			case C_OPCODE_BOARDWRITE:
				new C_BoardWrite(abyte0, _client);
				break;

			case C_OPCODE_USEITEM:
				new C_ItemUSe(abyte0, _client);
				break;

			case C_OPCODE_CREATEPARTY:
				new C_CreateParty(abyte0, _client);
				break;

			case C_OPCODE_ENTERPORTAL:
				new C_EnterPortal(abyte0, _client);
				break;

			case C_OPCODE_AMOUNT:
				new C_Amount(abyte0, _client);
				break;

			case C_OPCODE_FIX_WEAPON_LIST:
				new C_FixWeaponList(abyte0, _client);
				break;
		    
			case C_OPCODE_SELECTLIST:
				new C_SelectList(abyte0, _client);
				break;

			case C_OPCODE_EXIT_GHOST:
				new C_ExitGhost(abyte0, _client);
				break;

			case C_OPCODE_CALL:
				new C_CallPlayer(abyte0, _client);
				break;

			case C_OPCODE_HIRESOLDIER:
				new C_HireSoldier(abyte0, _client);
				break;

			case C_OPCODE_FISHCLICK:
				new C_FishClick(abyte0, _client);
				break;

			case C_OPCODE_SELECTTARGET:
				new C_SelectTarget(abyte0, _client);
				break;

			case C_OPCODE_PETMENU:
				new C_PetMenu(abyte0, _client);
				break;

			case C_OPCODE_USEPETITEM:
				new C_UsePetItem(abyte0, _client);
				break;

			case C_OPCODE_TELEPORT:
				new C_Teleport(abyte0, _client);
				break;

			case C_OPCODE_CAHTPARTY:
				new C_ChatParty(abyte0, _client);
				break;

			case C_OPCODE_FIGHT:
				new C_Fight(abyte0, _client);
				break;

			case C_OPCODE_SHIP:
				new C_Ship(abyte0, _client);
				break;

			case C_OPCODE_MAIL:
				new C_Mail(abyte0, _client);
				break;

			case C_OPCODE_CHARRESET:
				new C_CharReset(abyte0, _client);
				break;

			case C_OPCODE_SENDLOCATION:
				new C_SendLocation(abyte0, _client);
				break;

			case C_OPCODE_WAREHOUSELOCK:
				new C_WarePassword(abyte0, _client);
				break;
				
			case C_OPCODE_RESTARTMENU:
				new C_RestartMenu(abyte0, _client);
				break;

			default:
				// String s = Integer.toHexString(abyte0[0] & 0xff);
				// _log.warning("用途不明オペコード:データ内容");
				// _log.warning((new StringBuilder()).append("オペコード ").append(s)
				// .toString());
				// _log.warning(new ByteArrayUtil(abyte0).dumpToString());
				break;
		}
		// _log.warning((new StringBuilder()).append("オペコード
		// ").append(i).toString());
	}

	/**
	 * 客戶端連線執行緒。
	 * <p>
	 * 此欄位保存與客戶端建立連線的 ClientThread 實例，
	 * 用於在封包處理完成後將伺服器回應封包傳送回客戶端。
	 * <p>
	 * 所有封包處理器（C_* 類別）都會使用此連線來發送回應。
	 *
	 * @see ClientThread
	 */
	private final ClientThread _client;
}