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

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.Config;
import l1j.server.server.ClientThread;
import l1j.server.server.WarTimeController;
import l1j.server.server.datatables.CharacterTable;
import l1j.server.server.datatables.ClanMembersTable;
import l1j.server.server.datatables.ClanTable;
import l1j.server.server.datatables.HouseTable;
import l1j.server.server.datatables.NpcTable;
import l1j.server.server.datatables.PetTable;
import l1j.server.server.model.L1CastleLocation;
import l1j.server.server.model.L1Character;
import l1j.server.server.model.L1ChatParty;
import l1j.server.server.model.L1Clan;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1Party;
import l1j.server.server.model.L1Quest;
import l1j.server.server.model.L1Teleport;
import l1j.server.server.model.L1War;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1PetInstance;
import l1j.server.server.model.identity.L1ItemId;
import l1j.server.server.model.map.L1Map;
import l1j.server.server.serverpackets.S_ChangeName;
import l1j.server.server.serverpackets.S_CharReset;
import l1j.server.server.serverpackets.S_CharTitle;
import l1j.server.server.serverpackets.S_CharVisualUpdate;
import l1j.server.server.serverpackets.S_ClanAttention;
import l1j.server.server.serverpackets.S_ClanName;
import l1j.server.server.serverpackets.S_OwnCharStatus2;
import l1j.server.server.serverpackets.S_PacketBox;
import l1j.server.server.serverpackets.S_Resurrection;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_SkillSound;
import l1j.server.server.serverpackets.S_Trade;
import l1j.server.server.templates.L1House;
import l1j.server.server.templates.L1Npc;
import l1j.server.server.templates.L1Pet;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket

/**
 * 玩家互動確認封包處理器
 * <p>
 * 處理客戶端發送的各種確認（Yes/No）回應封包。涵蓋了遊戲中所有需要玩家確認的互動，
 * 包括血盟管理、交易、戰鬥、復活、結婚、組隊等功能。
 * </p>
 *
 * <h3>主要功能分類：</h3>
 *
 * <h4>1. 血盟相關 (Clan)</h4>
 * <ul>
 * <li><b>attrcode 97</b>：血盟加入邀請回應</li>
 * <li><b>attrcode 729</b>：血盟召喚回應</li>
 * <li><b>attrcode 512</b>：血盟小屋命名</li>
 * </ul>
 *
 * <h4>2. 戰爭相關 (War)</h4>
 * <ul>
 * <li><b>attrcode 217</b>：戰爭宣言回應</li>
 * <li><b>attrcode 221</b>：投降提案回應</li>
 * <li><b>attrcode 222</b>：結束戰爭提案回應</li>
 * </ul>
 *
 * <h4>3. 交易與戰鬥</h4>
 * <ul>
 * <li><b>attrcode 252</b>：交易邀請回應</li>
 * <li><b>attrcode 630</b>：決鬥邀請回應</li>
 * </ul>
 *
 * <h4>4. 復活與死亡</h4>
 * <ul>
 * <li><b>attrcode 321</b>：復活確認（一般復活）</li>
 * <li><b>attrcode 322</b>：復活確認（祝福復活卷軸/復活術）</li>
 * <li><b>attrcode 738</b>：經驗值恢復確認</li>
 * </ul>
 *
 * <h4>5. 社交系統</h4>
 * <ul>
 * <li><b>attrcode 653</b>：離婚確認</li>
 * <li><b>attrcode 654</b>：結婚提案回應</li>
 * </ul>
 *
 * <h4>6. 組隊系統</h4>
 * <ul>
 * <li><b>attrcode 951</b>：隊伍對話邀請回應</li>
 * <li><b>attrcode 953</b>：組隊邀請回應</li>
 * <li><b>attrcode 954</b>：自動分配組隊邀請回應</li>
 * </ul>
 *
 * <h4>7. 角色成長</h4>
 * <ul>
 * <li><b>attrcode 479</b>：能力值提升（STR/DEX/CON/INT/WIS/CHA）</li>
 * </ul>
 *
 * <h4>8. 其他功能</h4>
 * <ul>
 * <li><b>attrcode 325</b>：寵物命名</li>
 * <li><b>attrcode 1256</b>：寵物競速預約回應</li>
 * </ul>
 *
 * <h3>封包結構：</h3>
 * <p>
 * 封包結構會根據不同的 attrcode 而有所不同：
 * </p>
 * <pre>
 * 標準格式（大部分 attrcode）：
 * [2 bytes] 訊息編號（attrcode）
 * [4 bytes] 計數器（紀錄世界中發送 YesNo 的次數）
 * [2 bytes] 再次確認的 attrcode
 * [2 bytes] 選擇（0=No, 1=Yes）
 * [可選] 其他資料（視 attrcode 而定）
 *
 * 特殊格式（attrcode 479）：
 * [2 bytes] attrcode (479)
 * [1 byte]  確認標記
 * [string]  能力值名稱（str/dex/con/int/wis/cha）
 * </pre>
 *
 * <h3>處理流程：</h3>
 * <ol>
 * <li>讀取封包資料，解析 attrcode</li>
 * <li>根據 attrcode 執行對應的處理邏輯</li>
 * <li>驗證玩家狀態和權限</li>
 * <li>執行相應的遊戲邏輯（加入血盟、復活、交易等）</li>
 * <li>發送結果封包給相關玩家</li>
 * <li>更新資料庫（如需要）</li>
 * </ol>
 *
 * @see ClientBasePacket
 * @see L1Clan
 * @see L1Party
 * @see L1ChatParty
 * @see L1War
 */
public class C_Attr extends ClientBasePacket {

	/** 日誌記錄器 */
	private static final Logger _log = Logger.getLogger(C_Attr.class.getName());

	/**
	 * 封包類型識別字串
	 * <p>
	 * 用於日誌記錄和除錯，標識此封包為玩家互動確認封包。
	 * </p>
	 */
	private static final String C_ATTR = "[C] C_Attr";

	/**
	 * 方向對應的 X 座標偏移表
	 * <p>
	 * 用於血盟召喚功能，根據召喚者的面向計算被召喚者應該出現的 X 座標。
	 * 索引對應方向：0=北, 1=東北, 2=東, 3=東南, 4=南, 5=西南, 6=西, 7=西北
	 * </p>
	 */
	private static final int HEADING_TABLE_X[] = { 0, 1, 1, 1, 0, -1, -1, -1 };

	/**
	 * 方向對應的 Y 座標偏移表
	 * <p>
	 * 用於血盟召喚功能，根據召喚者的面向計算被召喚者應該出現的 Y 座標。
	 * 索引對應方向：0=北, 1=東北, 2=東, 3=東南, 4=南, 5=西南, 6=西, 7=西北
	 * </p>
	 */
	private static final int HEADING_TABLE_Y[] = { -1, -1, 0, 1, 1, 1, 0, -1 };

	/**
	 * 建構子：處理玩家互動確認封包
	 * <p>
	 * 根據 attrcode 分派到對應的處理邏輯。每個 attrcode 代表一種特定的玩家確認操作。
	 * </p>
	 *
	 * <h4>封包解析：</h4>
	 * <pre>
	 * 一般格式：
	 * [2 bytes] 訊息編號 i
	 * 若 i != 479:
	 *   [4 bytes] 計數器 count
	 *   [2 bytes] attrcode（實際的操作代碼）
	 * 若 i == 479:
	 *   attrcode = 479（能力值提升）
	 * </pre>
	 *
	 * <h4>主要處理的 attrcode：</h4>
	 * <table border="1">
	 * <tr><th>attrcode</th><th>功能</th><th>資料格式</th></tr>
	 * <tr><td>97</td><td>血盟加入確認</td><td>[2 bytes] 選擇</td></tr>
	 * <tr><td>217</td><td>戰爭宣言確認</td><td>[2 bytes] 選擇</td></tr>
	 * <tr><td>221</td><td>戰爭投降確認</td><td>[2 bytes] 選擇</td></tr>
	 * <tr><td>222</td><td>結束戰爭確認</td><td>[2 bytes] 選擇</td></tr>
	 * <tr><td>252</td><td>交易確認</td><td>[2 bytes] 選擇</td></tr>
	 * <tr><td>321</td><td>復活確認（一般）</td><td>[2 bytes] 選擇</td></tr>
	 * <tr><td>322</td><td>復活確認（祝福）</td><td>[2 bytes] 選擇</td></tr>
	 * <tr><td>325</td><td>寵物命名</td><td>[1 byte] + [string] 名稱</td></tr>
	 * <tr><td>512</td><td>血盟小屋命名</td><td>[2 bytes] + [string] 名稱</td></tr>
	 * <tr><td>630</td><td>決鬥確認</td><td>[2 bytes] 選擇</td></tr>
	 * <tr><td>653</td><td>離婚確認</td><td>[2 bytes] 選擇</td></tr>
	 * <tr><td>654</td><td>結婚確認</td><td>[2 bytes] 選擇</td></tr>
	 * <tr><td>729</td><td>血盟召喚確認</td><td>[2 bytes] 選擇</td></tr>
	 * <tr><td>738</td><td>經驗值恢復確認</td><td>[2 bytes] 選擇</td></tr>
	 * <tr><td>951</td><td>隊伍對話邀請確認</td><td>[2 bytes] 選擇</td></tr>
	 * <tr><td>953</td><td>組隊邀請確認</td><td>[2 bytes] 選擇</td></tr>
	 * <tr><td>954</td><td>自動分配組隊確認</td><td>[2 bytes] 選擇</td></tr>
	 * <tr><td>479</td><td>能力值提升</td><td>[1 byte] + [string] 能力值名稱</td></tr>
	 * <tr><td>1256</td><td>寵物競速預約</td><td>[1 byte] 選擇</td></tr>
	 * </table>
	 *
	 * <h4>選擇值：</h4>
	 * <ul>
	 * <li><b>0</b>：No（拒絕/取消）</li>
	 * <li><b>1</b>：Yes（同意/確認）</li>
	 * </ul>
	 *
	 * <h4>臨時 ID 使用：</h4>
	 * <p>
	 * 許多操作使用 pc.getTempID() 來取得相關的其他玩家或物件：
	 * </p>
	 * <ul>
	 * <li>血盟加入：TempID 儲存申請加入的玩家 ID</li>
	 * <li>復活：TempID 儲存施放復活術的玩家 ID</li>
	 * <li>寵物命名：TempID 儲存寵物物件 ID</li>
	 * <li>結婚：TempID 儲存求婚者的玩家 ID</li>
	 * <li>血盟召喚：TempID 儲存召喚者的玩家 ID</li>
	 * </ul>
	 *
	 * @param abyte0 客戶端發送的封包資料
	 * @param clientthread 客戶端執行緒
	 * @throws Exception 封包解析或處理過程中的例外
	 * @see #resurrection(L1PcInstance, L1PcInstance, short)
	 * @see #changeClan(ClientThread, L1PcInstance, L1PcInstance, int)
	 * @see #renamePet(L1PetInstance, String)
	 * @see #callClan(L1PcInstance)
	 */
	@SuppressWarnings("static-access")
	public C_Attr(byte abyte0[], ClientThread clientthread) throws Exception {
		super(abyte0);

		L1PcInstance pc = clientthread.getActiveChar();
		if (pc == null) {
			return;
		}

		int i = readH(); // 3.51C未知的功能
		int attrcode;

		if (i == 479) {
		   attrcode = i;
		} else {
		   @SuppressWarnings("unused")
		   int count = readD(); // 紀錄世界中發送YesNo的次數
		   attrcode = readH();
		}

		String name ;
		int c;

		switch (attrcode) {
		case 97: // \f3%0%s 想加入你的血盟。你接受嗎。(Y/N)
			c = readH();
			L1PcInstance joinPc = (L1PcInstance) L1World.getInstance().findObject(pc.getTempID());
			pc.setTempID(0);
			if (joinPc != null) {
				if (c == 0) { // No
					joinPc.sendPackets(new S_ServerMessage(96, pc.getName())); //  拒絕你的請求。
				} else if (c == 1) { // Yes
					int clan_id = pc.getClanid();
					String clanName = pc.getClanname();
					L1Clan clan = L1World.getInstance().getClan(clanName);
					if (clan != null) {
						int maxMember = 0;
						int charisma = pc.getCha();
						// 公式
						maxMember = charisma * 3 *( 2+ pc.getLevel() / 50 );
						// 未過45 人數/3
						if (!pc.getQuest().isEnd(L1Quest.QUEST_LEVEL45)) 
							maxMember /= 3;						
						
						if (Config.MAX_CLAN_MEMBER > 0) { // 設定檔中如果有設定血盟的人數上限
							maxMember = Config.MAX_CLAN_MEMBER;
						}

						if (joinPc.getClanid() == 0) { // 加入玩家未加入血盟
							String clanMembersName[] = clan.getAllMembers();
							if (maxMember <= clanMembersName.length) { // 血盟還有空間可以讓玩家加入
								joinPc.sendPackets( // %0%s 無法接受你成為該血盟成員。
								new S_ServerMessage(188, pc.getName()));
								return;
							}
							if(joinPc.isCrown()){ // 如果是王加入，判定收人方是否通過45試煉
								if(!pc.getQuest().isEnd(L1Quest.QUEST_LEVEL45)){
									return;
								}
							}
							for (L1PcInstance clanMembers : clan.getOnlineClanMember()) {
								clanMembers.sendPackets(new S_ServerMessage(94,joinPc.getName())); // \f1你接受%0當你的血盟成員。
							}
							joinPc.setClanid(clan_id);
							joinPc.setClanname(clanName);
							joinPc.setClanRank(L1Clan.CLAN_RANK_PUBLIC);
							joinPc.setClanMemberNotes("");
							joinPc.setTitle("");
							joinPc.sendPackets(new S_CharTitle(joinPc.getId(),""));
							joinPc.broadcastPacket(new S_CharTitle(joinPc.getId(), ""));
							joinPc.save(); // 儲存加入的玩家資料
							clan.addMemberName(joinPc.getName());
							ClanMembersTable.getInstance().newMember(joinPc);
							joinPc.sendPackets(new S_PacketBox(S_PacketBox.MSG_RANK_CHANGED, L1Clan.CLAN_RANK_PUBLIC, joinPc.getName())); // 你的階級變更為
							joinPc.sendPackets(new S_ServerMessage(95, clanName)); // \f1加入%0血盟。
							joinPc.sendPackets(new S_ClanName(joinPc, true));
							joinPc.sendPackets(new S_CharReset(joinPc.getId(), clan.getClanId()));
							joinPc.sendPackets(new S_PacketBox(S_PacketBox.PLEDGE_EMBLEM_STATUS, pc.getClan().getEmblemStatus())); // TODO
							joinPc.sendPackets(new S_ClanAttention());
							for(L1PcInstance player : clan.getOnlineClanMember()){
								player.sendPackets(new S_CharReset(joinPc.getId(), joinPc.getClan().getEmblemId()));
								player.broadcastPacket(new S_CharReset(player.getId(), joinPc.getClan().getEmblemId()));
							}
						} else { // 如果是有血盟的聯盟王加入（聯合血盟）
							if (Config.CLAN_ALLIANCE && pc.getQuest().isEnd(L1Quest.QUEST_LEVEL45)) {
								changeClan(clientthread, pc, joinPc, maxMember);
							} else {
								joinPc.sendPackets(new S_ServerMessage(89)); // \f1你已經有血盟了。
							}
						}
					}
				}
			}
			break;
		case 217: // %0 血盟向你的血盟宣戰。是否接受？(Y/N)
		case 221: // %0 血盟要向你投降。是否接受？(Y/N)
		case 222: // %0 血盟要結束戰爭。是否接受？(Y/N)
			c = readH();
			L1PcInstance enemyLeader = (L1PcInstance) L1World.getInstance().findObject(pc.getTempID());
			if (enemyLeader == null) {
				return;
			}
			pc.setTempID(0);
			String clanName = pc.getClanname();
			String enemyClanName = enemyLeader.getClanname();
			if (c == 0) { // No
				if (i == 217) {
					enemyLeader.sendPackets(new S_ServerMessage(236, clanName)); // %0
																					// 血盟拒絕你的宣戰。
				} else if ((i == 221) || (i == 222)) {
					enemyLeader.sendPackets(new S_ServerMessage(237, clanName)); // %0
																					// 血盟拒絕你的提案。
				}
			} else if (c == 1) { // Yes
				if (i == 217) {
					L1War war = new L1War();
					war.handleCommands(2, enemyClanName, clanName); // 盟戰開始
				} else if ((i == 221) || (i == 222)) {
					// 取得線上所有的盟戰
					for (L1War war : L1World.getInstance().getWarList()) {
						if (war.CheckClanInWar(clanName)) { // 如果有現在的血盟
							if (i == 221) {
								war.SurrenderWar(enemyClanName, clanName); // 投降
							} else if (i == 222) {
								war.CeaseWar(enemyClanName, clanName); // 結束
							}
							break;
						}
					}
				}
			}
			break;

		case 252: // \f2%0%s 要與你交易。願不願交易？ (Y/N)
			c = readH();
			L1PcInstance trading_partner = (L1PcInstance) L1World.getInstance().findObject(pc.getTradeID());
			if (trading_partner != null) {
				if (c == 0) // No
				{
					trading_partner.sendPackets(new S_ServerMessage(253, pc.getName())); // %0%d
											                                            // 拒絕與你交易。
					pc.setTradeID(0);
					trading_partner.setTradeID(0);
				} else if (c == 1) // Yes
				{
					pc.sendPackets(new S_Trade(trading_partner.getName()));
					trading_partner.sendPackets(new S_Trade(pc.getName()));
				}
			}
			break;

		case 321: // 是否要復活？ (Y/N)
			c = readH();
			L1PcInstance resusepc1 = (L1PcInstance) L1World.getInstance().findObject(pc.getTempID());
			pc.setTempID(0);
			if (resusepc1 != null) { // 如果有這個人
				if (c == 0) { // No

				} else if (c == 1) { // Yes
					resurrection( pc, resusepc1, (short) (pc.getMaxHp() / 2));
				}
			}
			break;

		case 322: // 是否要復活？ (Y/N)
			c = readH();
			L1PcInstance resusepc2 = (L1PcInstance) L1World.getInstance().findObject(pc.getTempID());
			pc.setTempID(0);
			if (resusepc2 != null) { // 祝福された 復活スクロール、リザレクション、グレーター リザレクション
				if (c == 0) { // No

				} else if (c == 1) { // Yes
					resurrection( pc, resusepc2, pc.getMaxHp());
					// EXPロストしている、G-RESを掛けられた、EXPロストした死亡
					// 全てを満たす場合のみEXP復旧
					if ((pc.getExpRes() == 1) && pc.isGres() && pc.isGresValid()) {
						pc.resExp();
						pc.setExpRes(0);
						pc.setGres(false);
					}
				}
			}
			break;

		case 325: // 你想叫牠什麼名字？
			c = readC(); // ?
			name = readS();
			L1PetInstance pet = (L1PetInstance) L1World.getInstance().findObject(pc.getTempID());
			pc.setTempID(0);
			renamePet(pet, name);
			break;

		case 512: // 請輸入血盟小屋名稱?
			c = readH(); // ?
			name = readS();
			int houseId = pc.getTempID();
			pc.setTempID(0);
			if (name.length() <= 16) {
				L1House house = HouseTable.getInstance().getHouseTable(houseId);
				house.setHouseName(name);
				HouseTable.getInstance().updateHouse(house); // 更新到資料庫中
			} else {
				pc.sendPackets(new S_ServerMessage(513)); // 血盟小屋名稱太長。
			}
			break;

		case 630: // %0%s 要與你決鬥。你是否同意？(Y/N)
			c = readH();
			L1PcInstance fightPc = (L1PcInstance) L1World.getInstance().findObject(pc.getFightId());
			if (c == 0) {
				pc.setFightId(0);
				fightPc.setFightId(0);
				fightPc.sendPackets(new S_ServerMessage(631, pc.getName())); // %0%dがあなたとの決闘を断りました。
			} else if (c == 1) {
				fightPc.sendPackets(new S_PacketBox(S_PacketBox.MSG_DUEL,fightPc.getFightId(), fightPc.getId()));
				pc.sendPackets(new S_PacketBox(S_PacketBox.MSG_DUEL, pc.getFightId(), pc.getId()));
			}
			break;

		case 653: // 若你離婚，你的結婚戒指將會消失。你決定要離婚嗎？(Y/N)
			c = readH();
			L1PcInstance target653 = (L1PcInstance) L1World.getInstance().findObject(pc.getPartnerId());
			if (c == 0) { // No
				return;
			} else if (c == 1) { // Yes
				if (target653 != null) {
					target653.setPartnerId(0);
					target653.save();
					target653.sendPackets(new S_ServerMessage(662)); // \f1你(妳)目前未婚。
				} else {
					CharacterTable.getInstance().updatePartnerId(
							pc.getPartnerId());
				}
			}
			pc.setPartnerId(0);
			pc.save(); // 將玩家資料儲存到資料庫中
			pc.sendPackets(new S_ServerMessage(662)); // \f1你(妳)目前未婚。
			break;

		case 654: // %0 向你(妳)求婚，你(妳)答應嗎?
			c = readH();
			L1PcInstance partner = (L1PcInstance) L1World.getInstance().findObject(pc.getTempID());
			pc.setTempID(0);
			if (partner != null) {
				if (c == 0) { // No
					partner.sendPackets(new S_ServerMessage(656, pc.getName())); // %0 拒絕你(妳)的求婚。
				} else if (c == 1) { // Yes
					pc.setPartnerId(partner.getId());
					pc.save();
					pc.sendPackets(new S_ServerMessage(790)); // 倆人的結婚在所有人的祝福下完成
					pc.sendPackets(new S_ServerMessage(655, partner.getName())); // 恭喜!! %0  已接受你(妳)的求婚。

					partner.setPartnerId(pc.getId());
					partner.save();
					partner.sendPackets(new S_ServerMessage(790)); // 恭喜!! %0 已接受你(妳)的求婚。
					partner.sendPackets(new S_ServerMessage(655, pc.getName())); // 恭喜!! %0 已接受你(妳)的求婚。
				}
			}
			break;

		// コールクラン
		case 729: // 盟主正在呼喚你，你要接受他的呼喚嗎？(Y/N)
			c = readH();
			if (c == 0) { // No

			} else if (c == 1) { // Yes
				callClan(pc);
			}
			break;

		case 738: // 恢復經驗值需消耗%0金幣。想要恢復經驗值嗎?
			c = readH();
			if ((c == 1) && (pc.getExpRes() == 1)) { // Yes
				int cost = 0;
				int level = pc.getLevel();
				int lawful = pc.getLawful();
				if (level < 45) {
					cost = level * level * 100;
				} else {
					cost = level * level * 200;
				}
				if (lawful >= 0) {
					cost = (cost / 2);
				}
				if (pc.getInventory().consumeItem(L1ItemId.ADENA, cost)) {
					pc.resExp();
					pc.setExpRes(0);
				} else {
					pc.sendPackets(new S_ServerMessage(189)); // \f1金幣不足。
				}
			}
			break;

		case 951: // 您要接受玩家 %0%s 提出的隊伍對話邀請嗎？(Y/N)
			c = readH();
			L1PcInstance chatPc = (L1PcInstance) L1World.getInstance().findObject(pc.getPartyID());
			if (chatPc != null) {
				if (c == 0) { // No
					chatPc.sendPackets(new S_ServerMessage(423, pc.getName())); // %0%s
																				// 拒絕了您的邀請。
					pc.setPartyID(0);
				} else if (c == 1) { // Yes
					if (chatPc.isInChatParty()) {
						if (chatPc.getChatParty().isVacancy() || chatPc.isGm()) {
							chatPc.getChatParty().addMember(pc);
						} else {
							chatPc.sendPackets(new S_ServerMessage(417)); // 你的隊伍已經滿了，無法再接受隊員。
						}
					} else {
						L1ChatParty chatParty = new L1ChatParty();
						chatParty.addMember(chatPc);
						chatParty.addMember(pc);
						chatPc.sendPackets(new S_ServerMessage(424, pc.getName())); // %0%s加入了您的隊伍。
					}
				}
			}
			break;

		case 953: // 玩家 %0%s 邀請您加入隊伍？(Y/N)
			c = readH();
			L1PcInstance target = (L1PcInstance) L1World.getInstance().findObject(pc.getPartyID());
			if (target != null) {
				if (c == 0) // No
				{
					target.sendPackets(new S_ServerMessage(423, pc.getName())); // %0%s 拒絕了您的邀請。
					pc.setPartyID(0);
				} else if (c == 1) // Yes
				{
					if (target.isInParty()) {
						// 隊長組隊中
						if (target.getParty().isVacancy() || target.isGm()) {
							// 組隊是空的
							target.getParty().addMember(pc);
						} else {
							// 組隊滿了
							target.sendPackets(new S_ServerMessage(417)); // 你的隊伍已經滿了，無法再接受隊員。
						}
					} else {
						// 還沒有組隊，建立一個新組隊
						L1Party party = new L1Party();
						party.addMember(target);
						party.addMember(pc);
						target.sendPackets(new S_ServerMessage(424, pc.getName())); // %0%s
												// 加入了您的隊伍。
					}
				}
			}
			break;

			case 954: // 玩家 %0%s 邀請您加入自動分配隊伍？(Y/N)
				c = readH();
				L1PcInstance target2 = (L1PcInstance) L1World.getInstance().findObject(pc.getPartyID());
				if (target2 != null) {
					if (c == 0) { // No
						target2.sendPackets(new S_ServerMessage(423, pc.getName())); // %0%s
																				// 拒絕了您的邀請。
						pc.setPartyID(0);
					}
					else if (c == 1) { // Yes
						if (target2.isInParty()) {
							// 隊長組隊中
							if (target2.getParty().isVacancy() || target2.isGm()) {
								// 組隊是空的
								target2.getParty().addMember(pc);
							}
							else {
								// 組隊滿了
								target2.sendPackets(new S_ServerMessage(417)); // 你的隊伍已經滿了，無法再接受隊員。
							}
						}
						else {
							// 還沒有組隊，建立一個新組隊
							L1Party party = new L1Party();
							party.addMember(target2);
							party.addMember(pc);
							target2.sendPackets(new S_ServerMessage(424, pc.getName())); // %0%s
																						// 加入了您的隊伍。
						}
					}
				}
				break;

		case 479: // 提昇能力值？（str、dex、int、con、wis、cha）
			if (readC() == 1) {
				String s = readS();
				if (!(pc.getLevel() - 50 > pc.getBonusStats())) {
					return;
				}
				if (s.toLowerCase().equals("str".toLowerCase())) {
					// if(l1pcinstance.get_str() < 255)
					if (pc.getBaseStr() < 35) {
						pc.addBaseStr((byte) 1); // 素のSTR値に+1
						pc.setBonusStats(pc.getBonusStats() + 1);
						pc.sendPackets(new S_OwnCharStatus2(pc, 0));
						pc.sendPackets(new S_CharVisualUpdate(pc));
						pc.save(); // 將玩家資料儲存到資料庫中
					} else {
						pc.sendPackets(new S_ServerMessage(481)); // \f1屬性最大值只能到35。
																	// 請重試一次。
					}
				} else if (s.toLowerCase().equals("dex".toLowerCase())) {
					// if(l1pcinstance.get_dex() < 255)
					if (pc.getBaseDex() < 35) {
						pc.addBaseDex((byte) 1); // 素のDEX値に+1
						pc.resetBaseAc();
						pc.setBonusStats(pc.getBonusStats() + 1);
						pc.sendPackets(new S_OwnCharStatus2(pc, 0));
						pc.sendPackets(new S_CharVisualUpdate(pc));
						pc.save(); // 將玩家資料儲存到資料庫中
					} else {
						pc.sendPackets(new S_ServerMessage(481)); // \f1屬性最大值只能到35。
																	// 請重試一次。
					}
				} else if (s.toLowerCase().equals("con".toLowerCase())) {
					// if(l1pcinstance.get_con() < 255)
					if (pc.getBaseCon() < 35) {
						pc.addBaseCon((byte) 1); // 素のCON値に+1
						pc.setBonusStats(pc.getBonusStats() + 1);
						pc.sendPackets(new S_OwnCharStatus2(pc, 0));
						pc.sendPackets(new S_CharVisualUpdate(pc));
						pc.save(); // 將玩家資料儲存到資料庫中
					} else {
						pc.sendPackets(new S_ServerMessage(481)); // \f1屬性最大值只能到35。
																	// 請重試一次。
					}
				} else if (s.toLowerCase().equals("int".toLowerCase())) {
					// if(l1pcinstance.get_int() < 255)
					if (pc.getBaseInt() < 35) {
						pc.addBaseInt((byte) 1); // 素のINT値に+1
						pc.setBonusStats(pc.getBonusStats() + 1);
						pc.sendPackets(new S_OwnCharStatus2(pc, 0));
						pc.sendPackets(new S_CharVisualUpdate(pc));
						pc.save(); // 將玩家資料儲存到資料庫中
					} else {
						pc.sendPackets(new S_ServerMessage(481)); // \f1屬性最大值只能到35。
																	// 請重試一次。
					}
				} else if (s.toLowerCase().equals("wis".toLowerCase())) {
					// if(l1pcinstance.get_wis() < 255)
					if (pc.getBaseWis() < 35) {
						pc.addBaseWis((byte) 1); // 素のWIS値に+1
						pc.resetBaseMr();
						pc.setBonusStats(pc.getBonusStats() + 1);
						pc.sendPackets(new S_OwnCharStatus2(pc, 0));
						pc.sendPackets(new S_CharVisualUpdate(pc));
						pc.save(); // 將玩家資料儲存到資料庫中
					} else {
						pc.sendPackets(new S_ServerMessage(481)); // \f1屬性最大值只能到35。
																	// 請重試一次。
					}
				} else if (s.toLowerCase().equals("cha".toLowerCase())) {
					// if(l1pcinstance.get_cha() < 255)
					if (pc.getBaseCha() < 35) {
						pc.addBaseCha((byte) 1); // 素のCHA値に+1
						pc.setBonusStats(pc.getBonusStats() + 1);
						pc.sendPackets(new S_OwnCharStatus2(pc, 0));
						pc.sendPackets(new S_CharVisualUpdate(pc));
						pc.save(); // 將玩家資料儲存到資料庫中
					} else {
						pc.sendPackets(new S_ServerMessage(481)); // \f1屬性最大值只能到35。
																	// 請重試一次。
					}
				}
			}
			break;
		case 1256:// 寵物競速 預約名單回應
			l1j.server.server.model.game.L1PolyRace.getInstance().requsetAttr(pc, readC());
			break;
		default:
			break;
		}
	}

	/**
	 * 執行玩家復活
	 * <p>
	 * 當玩家同意被其他玩家復活時調用。處理復活的所有相關邏輯，
	 * 包括設定 HP、啟動回復、播放音效、發送封包等。
	 * </p>
	 *
	 * <h4>復活流程：</h4>
	 * <ol>
	 * <li>播放復活音效（技能 ID 230）</li>
	 * <li>設定玩家為復活狀態</li>
	 * <li>設定當前 HP 為復活 HP 值</li>
	 * <li>啟動 HP/MP 自然回復</li>
	 * <li>啟動娃娃 HP/MP 回復</li>
	 * <li>停止玩家刪除計時器（死亡後自動登出計時器）</li>
	 * <li>發送復活封包給自己和周圍玩家</li>
	 * <li>更新角色外觀（3.80C 版本可能已不需要）</li>
	 * </ol>
	 *
	 * <h4>復活 HP 值：</h4>
	 * <ul>
	 * <li><b>一般復活</b>（attrcode 321）：最大 HP 的 50%</li>
	 * <li><b>祝福復活</b>（attrcode 322）：最大 HP 的 100%</li>
	 * </ul>
	 *
	 * @param pc 要復活的玩家
	 * @param resusepc 施放復活術的玩家
	 * @param resHp 復活後的 HP 值
	 * @see S_Resurrection
	 * @see S_SkillSound
	 * @see L1PcInstance#resurrect(int)
	 */
	private void resurrection(L1PcInstance pc, L1PcInstance resusepc, short resHp) {
		// 由其他角色復活
		pc.sendPackets(new S_SkillSound(pc.getId(), '\346'));
		pc.broadcastPacket(new S_SkillSound(pc.getId(), '\346'));
		pc.resurrect(resHp);
		pc.setCurrentHp(resHp);
		pc.startHpRegeneration();
		pc.startMpRegeneration();
		pc.startHpRegenerationByDoll();
		pc.startMpRegenerationByDoll();
		pc.stopPcDeleteTimer();
		pc.sendPackets(new S_Resurrection(pc, resusepc, 0));
		pc.broadcastPacket(new S_Resurrection(pc, resusepc, 0));
		pc.sendPackets(new S_CharVisualUpdate(pc));        // 3.80C可能已經不需要
		pc.broadcastPacket(new S_CharVisualUpdate(pc));    // 3.80C可能已經不需要
	}

	/**
	 * 處理血盟聯盟（血盟合併）
	 * <p>
	 * 當開啟血盟聯盟功能且盟主完成 45 級試煉後，允許其他血盟的盟主加入，
	 * 將整個血盟合併進來，形成聯盟血盟。原盟主成為聯盟王，所有成員成為聯盟成員。
	 * </p>
	 *
	 * <h4>聯盟條件：</h4>
	 * <ul>
	 * <li>伺服器設定啟用血盟聯盟功能（Config.CLAN_ALLIANCE）</li>
	 * <li>邀請方盟主完成 45 級試煉</li>
	 * <li>加入方必須是其他血盟的盟主</li>
	 * <li>合併後的總人數不超過邀請方的血盟人數上限</li>
	 * </ul>
	 *
	 * <h4>處理流程：</h4>
	 * <ol>
	 * <li>驗證兩個血盟都存在且加入方是盟主</li>
	 * <li>檢查合併後的總人數是否超過上限</li>
	 * <li>將邀請方盟主升級為聯盟王（CLAN_RANK_LEAGUE_PRINCE）</li>
	 * <li>遍歷被合併血盟的所有成員（線上和離線）：
	 *   <ul>
	 *   <li>從舊血盟資料中刪除</li>
	 *   <li>更改血盟 ID 和名稱</li>
	 *   <li>設定階級為聯盟成員（CLAN_RANK_LEAGUE_PUBLIC）</li>
	 *   <li>儲存到新血盟</li>
	 *   <li>發送相關封包更新客戶端</li>
	 *   </ul>
	 * </li>
	 * <li>刪除舊血盟的盟徽檔案</li>
	 * <li>從資料庫中刪除舊血盟</li>
	 * </ol>
	 *
	 * <h4>階級變化：</h4>
	 * <ul>
	 * <li><b>邀請方盟主</b>：一般階級 → 聯盟王（CLAN_RANK_LEAGUE_PRINCE）</li>
	 * <li><b>加入方盟主</b>：原盟主 → 聯盟成員（CLAN_RANK_LEAGUE_PUBLIC）</li>
	 * <li><b>加入方所有成員</b>：原階級 → 聯盟成員（CLAN_RANK_LEAGUE_PUBLIC）</li>
	 * </ul>
	 *
	 * @param clientthread 客戶端執行緒（未使用）
	 * @param pc 邀請方的盟主
	 * @param joinPc 加入方的盟主
	 * @param maxMember 邀請方血盟的最大人數上限
	 * @see L1Clan#CLAN_RANK_LEAGUE_PRINCE
	 * @see L1Clan#CLAN_RANK_LEAGUE_PUBLIC
	 * @see ClanMembersTable
	 */
	private void changeClan(ClientThread clientthread, L1PcInstance pc, L1PcInstance joinPc, int maxMember) {
		int clanId = pc.getClanid();
		String clanName = pc.getClanname();
		L1Clan clan = L1World.getInstance().getClan(clanName);

		String oldClanName = joinPc.getClanname();
		L1Clan oldClan = L1World.getInstance().getClan(oldClanName);
		
		if ((clan != null) && (oldClan != null) && joinPc.isCrown() && (joinPc.getId() == oldClan.getLeaderId())) {
			if (maxMember < clan.getAllMembers().length + oldClan.getAllMembers().length) { // 沒有空缺
				joinPc.sendPackets( // %0%s 無法接受你成為該血盟成員。
				new S_ServerMessage(188, pc.getName()));
				return;
			}
			
			for (L1PcInstance element : clan.getOnlineClanMember()) {
				element.sendPackets(new S_ServerMessage(94, joinPc.getName())); // \f1你接受%0當你的血盟成員。
			}
			
			/** 變更為聯盟王 */
			pc.setClanRank(L1Clan.CLAN_RANK_LEAGUE_PRINCE);
			pc.sendPackets(new S_PacketBox(S_PacketBox.MSG_RANK_CHANGED, L1Clan.CLAN_RANK_LEAGUE_PRINCE, pc.getName())); // 你的階級變更為
			try {
				pc.save();
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			for (String element : oldClan.getAllMembers()) {
				L1PcInstance oldClanMember = L1World.getInstance().getPlayer(element);
				if (oldClanMember != null) { // 舊血盟成員在線上
					ClanMembersTable.getInstance().deleteMember(oldClanMember.getId());
					oldClanMember.setClanid(clanId);
					oldClanMember.setClanname(clanName);
					oldClanMember.setClanRank(L1Clan.CLAN_RANK_LEAGUE_PUBLIC);
					try {
						// 儲存玩家資料到資料庫中
						oldClanMember.save();
					} catch (Exception e) {
						_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
					}
					clan.addMemberName(oldClanMember.getName());
					ClanMembersTable.getInstance().newMember(oldClanMember); // 加入成員資料
					oldClanMember.sendPackets(new S_PacketBox(S_PacketBox.MSG_RANK_CHANGED, L1Clan.CLAN_RANK_PUBLIC, oldClanMember.getName())); // 你的階級變更為
					oldClanMember.sendPackets(new S_ServerMessage(95, clanName)); // \f1加入%0血盟。
					oldClanMember.sendPackets(new S_ClanName(oldClanMember, true));
					oldClanMember.sendPackets(new S_CharReset(oldClanMember.getId(), clan.getClanId()));
					oldClanMember.sendPackets(new S_PacketBox(S_PacketBox.PLEDGE_EMBLEM_STATUS, pc.getClan().getEmblemStatus()));
					oldClanMember.sendPackets(new S_ClanAttention());
					for(L1PcInstance player : clan.getOnlineClanMember()){
						player.sendPackets(new S_CharReset(oldClanMember.getId(), oldClanMember.getClan().getEmblemId()));
						player.broadcastPacket(new S_CharReset(player.getId(), oldClanMember.getClan().getEmblemId()));
					}
				} else { // 舊血盟成員不在線上
					try {
						L1PcInstance offClanMember = CharacterTable.getInstance().restoreCharacter(element);
						ClanMembersTable.getInstance().deleteMember(offClanMember.getId());
						offClanMember.setClanid(clanId);
						offClanMember.setClanname(clanName);
						offClanMember.setClanRank(L1Clan.CLAN_RANK_LEAGUE_PUBLIC);
						offClanMember.save(); // 儲存玩家資料到資料庫中
						clan.addMemberName(offClanMember.getName());
						ClanMembersTable.getInstance().newMember(offClanMember); // 加入成員資料
					} catch (Exception e) {
						_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
					}
				}
			}
			// 刪除舊盟徽
			String emblem_file = String.valueOf(oldClan.getEmblemId());
			File file = new File("emblem/" + emblem_file);
			file.delete();
			ClanTable.getInstance().deleteClan(oldClanName);
		}
	}

	/**
	 * 為寵物命名
	 * <p>
	 * 當玩家馴服寵物後首次命名時調用。寵物名稱一經設定後無法更改，
	 * 且名稱在全伺服器必須唯一。
	 * </p>
	 *
	 * <h4>命名規則：</h4>
	 * <ul>
	 * <li>寵物名稱在全伺服器必須唯一</li>
	 * <li>寵物只能命名一次，一旦命名後無法更改</li>
	 * <li>只有尚未命名的寵物（名稱仍為 NPC 預設名稱）才能命名</li>
	 * </ul>
	 *
	 * <h4>處理流程：</h4>
	 * <ol>
	 * <li>驗證寵物和名稱不為 null</li>
	 * <li>取得寵物資料和寵物模板</li>
	 * <li>檢查名稱是否已存在（全伺服器唯一性檢查）</li>
	 * <li>檢查寵物是否尚未命名（名稱是否為 NPC 預設名稱）</li>
	 * <li>設定寵物新名稱</li>
	 * <li>更新寵物資料到資料庫</li>
	 * <li>更新背包中的寵物項目</li>
	 * <li>發送名稱變更封包給自己和周圍玩家</li>
	 * </ol>
	 *
	 * <h4>錯誤訊息：</h4>
	 * <ul>
	 * <li><b>S_ServerMessage 327</b>：同樣的名稱已經存在（名稱不唯一）</li>
	 * <li><b>S_ServerMessage 326</b>：一旦你已決定就不能再變更（已命名過）</li>
	 * </ul>
	 *
	 * @param pet 要命名的寵物實例
	 * @param name 寵物的新名稱
	 * @throws NullPointerException 若寵物或名稱為 null，或寵物模板不存在
	 * @see PetTable#isNameExists(String)
	 * @see PetTable#storePet(L1Pet)
	 * @see S_ChangeName
	 */
	private static void renamePet(L1PetInstance pet, String name) {
		if ((pet == null) || (name == null)) {
			throw new NullPointerException();
		}

		int petItemObjId = pet.getItemObjId();
		L1Pet petTemplate = PetTable.getInstance().getTemplate(petItemObjId);
		if (petTemplate == null) {
			throw new NullPointerException();
		}

		L1PcInstance pc = (L1PcInstance) pet.getMaster();
		if (PetTable.isNameExists(name)) {
			pc.sendPackets(new S_ServerMessage(327)); // 同樣的名稱已經存在。
			return;
		}
		L1Npc l1npc = NpcTable.getInstance().getTemplate(pet.getNpcId());
		if (!(pet.getName().equalsIgnoreCase(l1npc.get_name()))) {
			pc.sendPackets(new S_ServerMessage(326)); // 一旦你已決定就不能再變更。
			return;
		}
		pet.setName(name);
		petTemplate.set_name(name);
		PetTable.getInstance().storePet(petTemplate); // 儲存寵物資料到資料庫中
		L1ItemInstance item = pc.getInventory().getItem(pet.getItemObjId());
		pc.getInventory().updateItem(item);
		pc.sendPackets(new S_ChangeName(pet.getId(), name));
		pc.broadcastPacket(new S_ChangeName(pet.getId(), name));
	}

	/**
	 * 執行血盟召喚
	 * <p>
	 * 當盟主使用「呼叫血盟」技能時，血盟成員收到邀請並同意後，
	 * 會被傳送到盟主面前。此方法處理傳送的各種限制和位置計算。
	 * </p>
	 *
	 * <h4>召喚限制：</h4>
	 * <ul>
	 * <li><b>地圖限制</b>：被召喚者當前地圖必須允許瞬間移動（isEscapable）</li>
	 * <li><b>地圖 ID 限制</b>：召喚者必須在特定地圖（0, 4, 304）</li>
	 * <li><b>戰爭區域限制</b>：
	 *   <ul>
	 *   <li>非戰爭時間：城堡區域內無法召喚</li>
	 *   <li>戰爭時間：可以召喚（支援攻城戰）</li>
	 *   </ul>
	 * </li>
	 * <li><b>目標位置限制</b>：
	 *   <ul>
	 *   <li>目標位置必須可通行（isPassable）</li>
	 *   <li>目標位置不能有其他角色</li>
	 *   <li>目標位置不能是 (0, 0)</li>
	 *   </ul>
	 * </li>
	 * </ul>
	 *
	 * <h4>位置計算：</h4>
	 * <p>
	 * 被召喚者會出現在盟主面前（根據盟主的面向）：
	 * </p>
	 * <ul>
	 * <li>使用 HEADING_TABLE_X 和 HEADING_TABLE_Y 計算偏移</li>
	 * <li>目標位置 = 盟主位置 + 面向偏移</li>
	 * <li>被召喚者面向 = (盟主面向 + 4) % 8（面對面）</li>
	 * </ul>
	 *
	 * <h4>錯誤訊息：</h4>
	 * <ul>
	 * <li><b>S_ServerMessage 647</b>：這附近的能量影響到瞬間移動（地圖不可逃脫）</li>
	 * <li><b>S_ServerMessage 79</b>：沒有任何事情發生（地圖 ID 限制或戰爭區域）</li>
	 * <li><b>S_ServerMessage 627</b>：因你要去的地方有障礙物以致於無法直接傳送到該處（位置被阻擋）</li>
	 * </ul>
	 *
	 * <h4>處理流程：</h4>
	 * <ol>
	 * <li>從 TempID 取得召喚者（盟主）</li>
	 * <li>檢查被召喚者當前地圖是否允許瞬移</li>
	 * <li>驗證 ID 匹配（防止作弊）</li>
	 * <li>檢查召喚者是否在戰爭區域</li>
	 * <li>檢查召喚者地圖 ID 是否符合條件</li>
	 * <li>計算目標傳送位置和面向</li>
	 * <li>檢查目標位置是否可用</li>
	 * <li>執行傳送</li>
	 * </ol>
	 *
	 * @param pc 被召喚的血盟成員
	 * @see L1Teleport#teleport(L1PcInstance, int, int, short, int, boolean, int)
	 * @see WarTimeController
	 * @see L1CastleLocation
	 */
	private void callClan(L1PcInstance pc) {
		L1PcInstance callClanPc = (L1PcInstance) L1World.getInstance()
				.findObject(pc.getTempID());
		pc.setTempID(0);
		if (callClanPc == null) {
			return;
		}
		if (!pc.getMap().isEscapable() && !pc.isGm()) {
			// 這附近的能量影響到瞬間移動。在此地無法使用瞬間移動。
			pc.sendPackets(new S_ServerMessage(647));
			L1Teleport.teleport(pc, pc.getLocation(), pc.getHeading(), false);
			return;
		}
		if (pc.getId() != callClanPc.getCallClanId()) {
			return;
		}

		boolean isInWarArea = false;
		int castleId = L1CastleLocation.getCastleIdByArea(callClanPc);
		if (castleId != 0) {
			isInWarArea = true;
			if (WarTimeController.getInstance().isNowWar(castleId)) {
				isInWarArea = false; // 戰爭也可以在時間的旗
			}
		}
		short mapId = callClanPc.getMapId();
		if (((mapId != 0) && (mapId != 4) && (mapId != 304)) || isInWarArea) {
			// 沒有任何事情發生。
			pc.sendPackets(new S_ServerMessage(79));
			return;
		}

		L1Map map = callClanPc.getMap();
		int locX = callClanPc.getX();
		int locY = callClanPc.getY();
		int heading = callClanPc.getCallClanHeading();
		locX += HEADING_TABLE_X[heading];
		locY += HEADING_TABLE_Y[heading];
		heading = (heading + 4) % 4;

		boolean isExsistCharacter = false;
		for (L1Object object : L1World.getInstance().getVisibleObjects(
				callClanPc, 1)) {
			if (object instanceof L1Character) {
				L1Character cha = (L1Character) object;
				if ((cha.getX() == locX) && (cha.getY() == locY)
						&& (cha.getMapId() == mapId)) {
					isExsistCharacter = true;
					break;
				}
			}
		}

		if (((locX == 0) && (locY == 0)) || !map.isPassable(locX, locY)
				|| isExsistCharacter) {
			// 因你要去的地方有障礙物以致於無法直接傳送到該處。
			pc.sendPackets(new S_ServerMessage(627));
			return;
		}
		L1Teleport.teleport(pc, locX, locY, mapId, heading, true,
				L1Teleport.CALL_CLAN);
	}

	/**
	 * 取得封包類型
	 * <p>
	 * 返回此封包的類型識別字串，用於日誌記錄和除錯。
	 * </p>
	 *
	 * @return 封包類型字串 "[C] C_Attr"
	 */
	@Override
	public String getType() {
		return C_ATTR;
	}
}