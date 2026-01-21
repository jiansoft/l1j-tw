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

import l1j.server.server.ClientThread;
import l1j.server.server.datatables.SkillsTable;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.identity.L1ItemId;
import l1j.server.server.serverpackets.S_AddSkill;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_SkillSound;
import l1j.server.server.templates.L1Skills;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket

/**
 * 技能購買確認封包處理器
 * <p>
 * 處理玩家在魔法商店（Magic Doll）確認購買技能後，客戶端發送的封包。
 * 負責驗證玩家等級、職業限制、金錢扣除，並實際給予玩家技能。
 * </p>
 *
 * <h3>封包結構：</h3>
 * <ul>
 * <li><b>技能數量</b>：2 bytes (readH)</li>
 * <li><b>技能 ID 列表</b>：count × 4 bytes (readD)，每個技能一個 ID</li>
 * </ul>
 *
 * <h3>技能等級與價格：</h3>
 * <ul>
 * <li><b>1 級魔法</b>（技能 ID 0-7）：每個 100 金幣</li>
 * <li><b>2 級魔法</b>（技能 ID 8-15）：每個 400 金幣</li>
 * <li><b>3 級魔法</b>（技能 ID 16-23）：每個 900 金幣</li>
 * </ul>
 *
 * <h3>職業等級限制：</h3>
 * <table border="1">
 * <tr><th>職業</th><th>1 級魔法</th><th>2 級魔法</th><th>3 級魔法</th></tr>
 * <tr><td>王族 (0)</td><td>Lv 10</td><td>Lv 20</td><td>不可學習</td></tr>
 * <tr><td>騎士 (1)</td><td>Lv 50</td><td>不可學習</td><td>不可學習</td></tr>
 * <tr><td>妖精 (2)</td><td>Lv 8</td><td>Lv 16</td><td>Lv 24</td></tr>
 * <tr><td>法師 (3)</td><td>Lv 4</td><td>Lv 8</td><td>Lv 12</td></tr>
 * <tr><td>黑暗妖精 (4)</td><td>Lv 12</td><td>Lv 24</td><td>不可學習</td></tr>
 * </table>
 *
 * <h3>技能編碼機制：</h3>
 * <p>
 * 使用位元標記（Bit Flags）方式儲存技能：
 * </p>
 * <ul>
 * <li>技能 ID 0 對應 bit 0 (值 1)</li>
 * <li>技能 ID 1 對應 bit 1 (值 2)</li>
 * <li>技能 ID 2 對應 bit 2 (值 4)</li>
 * <li>技能 ID 3 對應 bit 3 (值 8)</li>
 * <li>...</li>
 * <li>技能 ID 7 對應 bit 7 (值 128)</li>
 * </ul>
 *
 * <h3>處理流程：</h3>
 * <ol>
 * <li>讀取客戶端發送的技能 ID 列表</li>
 * <li>根據技能 ID 計算各等級的位元標記和總價格</li>
 * <li>根據玩家職業和等級，過濾不符合條件的技能</li>
 * <li>驗證玩家金幣是否足夠</li>
 * <li>扣除金幣</li>
 * <li>播放學習技能音效</li>
 * <li>發送技能給客戶端（S_AddSkill）</li>
 * <li>將技能寫入資料庫（spellMastery）</li>
 * </ol>
 *
 * @see ClientBasePacket
 * @see S_AddSkill
 * @see SkillsTable#spellMastery(int, int, String, int, int)
 */
public class C_SkillBuyOK extends ClientBasePacket {

	/**
	 * 封包類型識別字串
	 * <p>
	 * 用於日誌記錄和除錯，標識此封包為技能購買確認封包。
	 * </p>
	 */
	private static final String C_SKILL_BUY_OK = "[C] C_SkillBuyOK";

	/**
	 * 建構子：處理技能購買封包
	 * <p>
	 * 當玩家在魔法商店確認購買技能時，客戶端會發送此封包。
	 * 本方法負責解析封包、驗證條件、扣除金幣、給予技能。
	 * </p>
	 *
	 * <h4>封包資料結構：</h4>
	 * <pre>
	 * [2 bytes] 技能數量 (count)
	 * [4 bytes] 技能 ID 1
	 * [4 bytes] 技能 ID 2
	 * ...
	 * [4 bytes] 技能 ID count
	 * </pre>
	 *
	 * <h4>處理邏輯：</h4>
	 * <ol>
	 * <li><b>驗證玩家狀態</b>：檢查玩家是否在線且非幽靈狀態</li>
	 * <li><b>解析技能列表</b>：讀取技能數量和各技能 ID</li>
	 * <li><b>計算位元標記</b>：根據技能 ID 計算各等級的位元標記值
	 *   <ul>
	 *   <li>1 級魔法（ID 0-7）：level1 變數，每個技能 100 金幣</li>
	 *   <li>2 級魔法（ID 8-15）：level2 變數，每個技能 400 金幣</li>
	 *   <li>3 級魔法（ID 16-23）：level3 變數，每個技能 900 金幣</li>
	 *   </ul>
	 * </li>
	 * <li><b>職業等級限制</b>：根據玩家職業和等級，清除不符資格的技能
	 *   <ul>
	 *   <li>王族：10 級可學 1 級，20 級可學 2 級，不可學 3 級</li>
	 *   <li>騎士：50 級可學 1 級，不可學 2、3 級</li>
	 *   <li>妖精：8 級可學 1 級，16 級可學 2 級，24 級可學 3 級</li>
	 *   <li>法師：4 級可學 1 級，8 級可學 2 級，12 級可學 3 級</li>
	 *   <li>黑暗妖精：12 級可學 1 級，24 級可學 2 級，不可學 3 級</li>
	 *   <li>GM：無限制</li>
	 *   </ul>
	 * </li>
	 * <li><b>驗證金幣</b>：檢查玩家是否有足夠金幣</li>
	 * <li><b>扣除金幣</b>：消耗對應數量的金幣</li>
	 * <li><b>播放音效</b>：播放學習技能音效（技能 ID 224）</li>
	 * <li><b>發送封包</b>：發送 S_AddSkill 封包給客戶端</li>
	 * <li><b>寫入資料庫</b>：使用位元檢查，將每個技能寫入資料庫
	 *   <pre>
	 *   例如：level1 = 5 (二進位 00000101)
	 *        表示玩家學習了技能 ID 0 (bit 0) 和技能 ID 2 (bit 2)
	 *        使用 (level1 & 1) == 1 檢查 bit 0
	 *        使用 (level1 & 4) == 4 檢查 bit 2
	 *   </pre>
	 * </li>
	 * <li><b>錯誤處理</b>：金幣不足時發送錯誤訊息（S_ServerMessage 189）</li>
	 * </ol>
	 *
	 * <h4>位元標記範例：</h4>
	 * <pre>
	 * 技能 ID 0 (1 級第 1 個魔法)：level1 += 1   (2^0 = 1)
	 * 技能 ID 1 (1 級第 2 個魔法)：level1 += 2   (2^1 = 2)
	 * 技能 ID 2 (1 級第 3 個魔法)：level1 += 4   (2^2 = 4)
	 * 技能 ID 3 (1 級第 4 個魔法)：level1 += 8   (2^3 = 8)
	 * ...
	 * 技能 ID 8 (2 級第 1 個魔法)：level2 += 1   (2^0 = 1)
	 * ...
	 * 技能 ID 16 (3 級第 1 個魔法)：level3 += 1  (2^0 = 1)
	 * </pre>
	 *
	 * @param abyte0 客戶端發送的封包資料
	 * @param clientthread 客戶端執行緒
	 * @throws Exception 封包解析或處理過程中的例外
	 * @see S_AddSkill
	 * @see S_SkillSound
	 * @see S_ServerMessage
	 * @see SkillsTable#spellMastery(int, int, String, int, int)
	 */
	public C_SkillBuyOK(byte abyte0[], ClientThread clientthread) throws Exception {
		super(abyte0);
		
		L1PcInstance pc = clientthread.getActiveChar();
		if ((pc == null) || pc.isGhost()) {
			return;
		}

		int count = readH();
		int sid[] = new int[count];
		int price = 0;
		int level1 = 0;
		int level2 = 0;
		int level3 = 0;
		int level1_cost = 0;
		int level2_cost = 0;
		int level3_cost = 0;
		String skill_name = null;
		int skill_id = 0;
		
		for (int i = 0; i < count; i++) {
			sid[i] = readD();
			switch (sid[i]) {
				// Lv1魔法
				case 0:
					level1 += 1;
					level1_cost += 100;
					break;
				case 1:
					level1 += 2;
					level1_cost += 100;
					break;
				case 2:
					level1 += 4;
					level1_cost += 100;
					break;
				case 3:
					level1 += 8;
					level1_cost += 100;
					break;
				case 4:
					level1 += 16;
					level1_cost += 100;
					break;
				case 5:
					level1 += 32;
					level1_cost += 100;
					break;
				case 6:
					level1 += 64;
					level1_cost += 100;
					break;
				case 7:
					level1 += 128;
					level1_cost += 100;
					break;

				// Lv2魔法
				case 8:
					level2 += 1;
					level2_cost += 400;
					break;
				case 9:
					level2 += 2;
					level2_cost += 400;
					break;
				case 10:
					level2 += 4;
					level2_cost += 400;
					break;
				case 11:
					level2 += 8;
					level2_cost += 400;
					break;
				case 12:
					level2 += 16;
					level2_cost += 400;
					break;
				case 13:
					level2 += 32;
					level2_cost += 400;
					break;
				case 14:
					level2 += 64;
					level2_cost += 400;
					break;
				case 15:
					level2 += 128;
					level2_cost += 400;
					break;

				// Lv3魔法
				case 16:
					level3 += 1;
					level3_cost += 900;
					break;
				case 17:
					level3 += 2;
					level3_cost += 900;
					break;
				case 18:
					level3 += 4;
					level3_cost += 900;
					break;
				case 19:
					level3 += 8;
					level3_cost += 900;
					break;
				case 20:
					level3 += 16;
					level3_cost += 900;
					break;
				case 21:
					level3 += 32;
					level3_cost += 900;
					break;
				case 22:
					level3 += 64;
					level3_cost += 900;
					break;
				case 23:
					level3 += 128;
					level3_cost += 900;
					break;

				default:
					break;
			}
		}

		if (!pc.isGm()) {
			switch (pc.getType()) {
				case 0: // 君主
					if (pc.getLevel() < 10) {
						level1 = 0;
						level1_cost = 0;
					}
					if (pc.getLevel() < 20) {
						level2 = 0;
						level2_cost = 0;
					}
					level3 = 0;
					level3_cost = 0;
					break;

				case 1: // ナイト
					if (pc.getLevel() < 50) {
						level1 = 0;
						level1_cost = 0;
					}
					level2 = 0;
					level2_cost = 0;
					level3 = 0;
					level3_cost = 0;
					break;

				case 2: // エルフ
					if (pc.getLevel() < 8) {
						level1 = 0;
						level1_cost = 0;
					}
					if (pc.getLevel() < 16) {
						level2 = 0;
						level2_cost = 0;
					}
					if (pc.getLevel() < 24) {
						level3 = 0;
						level3_cost = 0;
					}
					break;

				case 3: // WIZ
					if (pc.getLevel() < 4) {
						level1 = 0;
						level1_cost = 0;
					}
					if (pc.getLevel() < 8) {
						level2 = 0;
						level2_cost = 0;
					}
					if (pc.getLevel() < 12) {
						level3 = 0;
						level3_cost = 0;
					}
					break;

				case 4: // DE
					if (pc.getLevel() < 12) {
						level1 = 0;
						level1_cost = 0;
					}
					if (pc.getLevel() < 24) {
						level2 = 0;
						level2_cost = 0;
					}
					level3 = 0;
					level3_cost = 0;
					break;

				default:
					break;
			}
		}

		if ((level1 == 0) && (level2 == 0) && (level3 == 0)) {
			return;
		}
		price = level1_cost + level2_cost + level3_cost;
		if (pc.getInventory().checkItem(L1ItemId.ADENA, price)) {
			pc.getInventory().consumeItem(L1ItemId.ADENA, price);
			S_SkillSound s_skillSound = new S_SkillSound(pc.getId(), 224);
			pc.sendPackets(s_skillSound);
			pc.broadcastPacket(s_skillSound);
			pc.sendPackets(new S_AddSkill(level1, level2, level3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

			if ((level1 & 1) == 1) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(1);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level1 & 2) == 2) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(2);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level1 & 4) == 4) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(3);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level1 & 8) == 8) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(4);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level1 & 16) == 16) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(5);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level1 & 32) == 32) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(6);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level1 & 64) == 64) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(7);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level1 & 128) == 128) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(8);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}

			if ((level2 & 1) == 1) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(9);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level2 & 2) == 2) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(10);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level2 & 4) == 4) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(11);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level2 & 8) == 8) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(12);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level2 & 16) == 16) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(13);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level2 & 32) == 32) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(14);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level2 & 64) == 64) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(15);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level2 & 128) == 128) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(16);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}

			if ((level3 & 1) == 1) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(17);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level3 & 2) == 2) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(18);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level3 & 4) == 4) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(19);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level3 & 8) == 8) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(20);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level3 & 16) == 16) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(21);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level3 & 32) == 32) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(22);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
			if ((level3 & 64) == 64) {
				L1Skills l1skills = SkillsTable.getInstance().getTemplate(23);
				skill_name = l1skills.getName();
				skill_id = l1skills.getSkillId();
				SkillsTable.getInstance().spellMastery(pc.getId(), skill_id, skill_name, 0, 0);
			}
		}
		else {
			pc.sendPackets(new S_ServerMessage(189)); // \f1アデナが不足しています。
		}
	}

	/**
	 * 取得封包類型
	 * <p>
	 * 返回此封包的類型識別字串，用於日誌記錄和除錯。
	 * </p>
	 *
	 * @return 封包類型字串 "[C] C_SkillBuyOK"
	 */
	@Override
	public String getType() {
		return C_SKILL_BUY_OK;
	}

}
