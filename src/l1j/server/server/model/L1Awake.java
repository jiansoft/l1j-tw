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
package l1j.server.server.model;

import static l1j.server.server.model.skill.L1SkillId.AWAKEN_ANTHARAS;
import static l1j.server.server.model.skill.L1SkillId.AWAKEN_FAFURION;
import static l1j.server.server.model.skill.L1SkillId.AWAKEN_VALAKAS;
import static l1j.server.server.model.skill.L1SkillId.SHAPE_CHANGE;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_ChangeShape;
import l1j.server.server.serverpackets.S_HPUpdate;
import l1j.server.server.serverpackets.S_OwnCharAttrDef;
import l1j.server.server.serverpackets.S_OwnCharStatus2;
import l1j.server.server.serverpackets.S_SPMR;

// Referenced classes of package l1j.server.server.model:
// L1Cooking

/**
 * 龍之覺醒技能管理類別
 * <p>處理玩家使用龍之覺醒技能 (Dragon Awaken) 的狀態變化及效果套用。
 *
 * <h3>覺醒類型:</h3>
 * <ul>
 *   <li><b>覺醒：安塔瑞斯 (AWAKEN_ANTHARAS)</b> - 增加 HP 上限和防禦力
 *     <ul>
 *       <li>最大 HP +127</li>
 *       <li>AC -12 (防禦力提升)</li>
 *     </ul>
 *   </li>
 *   <li><b>覺醒：法力昂 (AWAKEN_FAFURION)</b> - 增加魔法防禦和屬性防禦
 *     <ul>
 *       <li>MR +30</li>
 *       <li>四屬性防禦 (風/水/火/地) 各 +30</li>
 *     </ul>
 *   </li>
 *   <li><b>覺醒：巴拉卡斯 (AWAKEN_VALAKAS)</b> - 增加所有能力值
 *     <ul>
 *       <li>STR/CON/DEX/CHA/INT/WIS 各 +5</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>覺醒特性:</h3>
 * <ul>
 *   <li>同一時間只能啟動一種覺醒狀態</li>
 *   <li>再次施放相同技能會解除覺醒狀態</li>
 *   <li>覺醒時會變身為龍形態 (PolyId: 6894)</li>
 *   <li>覺醒期間持續消耗 MP</li>
 *   <li>變身時某些武器會被強制解除</li>
 * </ul>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * // 啟動覺醒
 * L1Awake.start(pc, L1SkillId.AWAKEN_ANTHARAS);
 *
 * // 解除覺醒
 * L1Awake.stop(pc);
 * </pre>
 *
 * @see L1PcInstance#getAwakeSkillId()
 * @see L1PcInstance#setAwakeSkillId(int)
 * @see L1PcInstance#startMpReductionByAwake()
 * @see L1PcInstance#stopMpReductionByAwake()
 */
public class L1Awake {
	/**
	 * 私有建構子，防止實例化
	 * <p>此類別僅提供靜態方法。
	 */
	private L1Awake() {
	}

	/**
	 * 啟動龍之覺醒狀態
	 * <p>根據技能 ID 套用對應的覺醒效果，並執行變身及 MP 消耗。
	 *
	 * <h3>處理邏輯:</h3>
	 * <ol>
	 *   <li>若再次施放相同覺醒技能，則解除當前覺醒狀態</li>
	 *   <li>若已有其他覺醒狀態，則無法啟動新的覺醒</li>
	 *   <li>套用對應覺醒技能的能力值加成</li>
	 *   <li>執行變身為龍形態</li>
	 *   <li>啟動 MP 持續消耗</li>
	 * </ol>
	 *
	 * <h3>覺醒效果:</h3>
	 * <ul>
	 *   <li><b>AWAKEN_ANTHARAS:</b> MaxHP +127, AC -12</li>
	 *   <li><b>AWAKEN_FAFURION:</b> MR +30, 四屬性防禦 +30</li>
	 *   <li><b>AWAKEN_VALAKAS:</b> 所有能力值 +5</li>
	 * </ul>
	 *
	 * @param pc 玩家角色實例
	 * @param skillId 覺醒技能 ID (AWAKEN_ANTHARAS / AWAKEN_FAFURION / AWAKEN_VALAKAS)
	 * @see #stop(L1PcInstance)
	 * @see #doPoly(L1PcInstance)
	 */
	public static void start(L1PcInstance pc, int skillId) {
		if (skillId == pc.getAwakeSkillId()) { // 再次咏唱時解除覺醒狀態
			stop(pc);
		}
		else if (pc.getAwakeSkillId() != 0) { // 無法與其他覺醒狀態並存
			return;
		}
		else {
			if (skillId == AWAKEN_ANTHARAS) { // 覺醒：安塔瑞斯
				pc.addMaxHp(127);
				pc.sendPackets(new S_HPUpdate(pc.getCurrentHp(), pc.getMaxHp()));
				if (pc.isInParty()) { // 組隊中
					pc.getParty().updateMiniHP(pc);
				}
				pc.addAc(-12);
				pc.sendPackets(new S_OwnCharStatus2(pc, 0));
			}
			else if (skillId == AWAKEN_FAFURION) { // 覺醒：法力昂
				pc.addMr(30);
				pc.sendPackets(new S_SPMR(pc));
				pc.addWind(30);
				pc.addWater(30);
				pc.addFire(30);
				pc.addEarth(30);
				pc.sendPackets(new S_OwnCharAttrDef(pc));
			}
			else if (skillId == AWAKEN_VALAKAS) { // 覺醒：巴拉卡斯
				pc.addStr(5);
				pc.addCon(5);
				pc.addDex(5);
				pc.addCha(5);
				pc.addInt(5);
				pc.addWis(5);
				pc.sendPackets(new S_OwnCharStatus2(pc, 0));
			}
			pc.setAwakeSkillId(skillId);
			doPoly(pc);
			pc.startMpReductionByAwake();
		}
	}

	/**
	 * 解除龍之覺醒狀態
	 * <p>移除當前覺醒狀態的所有能力值加成，恢復原始外觀並停止 MP 消耗。
	 *
	 * <h3>處理流程:</h3>
	 * <ol>
	 *   <li>根據當前覺醒技能 ID 移除對應的能力值加成</li>
	 *   <li>將覺醒技能 ID 重置為 0</li>
	 *   <li>解除變身，恢復原始外觀</li>
	 *   <li>停止 MP 持續消耗</li>
	 * </ol>
	 *
	 * <h3>移除效果:</h3>
	 * <ul>
	 *   <li><b>AWAKEN_ANTHARAS:</b> MaxHP -127, AC +12</li>
	 *   <li><b>AWAKEN_FAFURION:</b> MR -30, 四屬性防禦 -30</li>
	 *   <li><b>AWAKEN_VALAKAS:</b> 所有能力值 -5</li>
	 * </ul>
	 *
	 * @param pc 玩家角色實例
	 * @see #start(L1PcInstance, int)
	 * @see #undoPoly(L1PcInstance)
	 */
	public static void stop(L1PcInstance pc) {
		int skillId = pc.getAwakeSkillId();
		if (skillId == AWAKEN_ANTHARAS) { // 覺醒：安塔瑞斯
			pc.addMaxHp(-127);
			pc.sendPackets(new S_HPUpdate(pc.getCurrentHp(), pc.getMaxHp()));
			if (pc.isInParty()) { // 組隊中
				pc.getParty().updateMiniHP(pc);
			}
			pc.addAc(12);
			pc.sendPackets(new S_OwnCharAttrDef(pc));
		}
		else if (skillId == AWAKEN_FAFURION) { // 覺醒：法力昂
			pc.addMr(-30);
			pc.addWind(-30);
			pc.addWater(-30);
			pc.addFire(-30);
			pc.addEarth(-30);
			pc.sendPackets(new S_SPMR(pc));
			pc.sendPackets(new S_OwnCharAttrDef(pc));
		}
		else if (skillId == AWAKEN_VALAKAS) { // 覺醒：巴拉卡斯
			pc.addStr(-5);
			pc.addCon(-5);
			pc.addDex(-5);
			pc.addCha(-5);
			pc.addInt(-5);
			pc.addWis(-5);
			pc.sendPackets(new S_OwnCharStatus2(pc, 0));
		}
		pc.setAwakeSkillId(0);
		undoPoly(pc);
		pc.stopMpReductionByAwake();
	}

	/**
	 * 執行龍之覺醒變身
	 * <p>將玩家變身為龍形態 (PolyId: 6894)，並處理武器及裝備的兼容性。
	 *
	 * <h3>處理流程:</h3>
	 * <ol>
	 *   <li>若玩家有變身術 (SHAPE_CHANGE) 效果，先移除該效果</li>
	 *   <li>檢查當前武器是否與龍形態兼容</li>
	 *   <li>若武器不兼容，強制解除武器</li>
	 *   <li>設定臨時外觀為龍形態 (6894)</li>
	 *   <li>根據玩家可見狀態發送變身封包:
	 *     <ul>
	 *       <li>GM 隱身: 不發送廣播封包</li>
	 *       <li>一般隱身: 僅發送給能看見隱身的玩家</li>
	 *       <li>可見狀態: 發送給所有周圍玩家</li>
	 *     </ul>
	 *   </li>
	 *   <li>強制解除與龍形態不兼容的裝備</li>
	 * </ol>
	 *
	 * @param pc 玩家角色實例
	 * @see L1PolyMorph#isEquipableWeapon(int, int)
	 * @see #undoPoly(L1PcInstance)
	 */
	public static void doPoly(L1PcInstance pc) {
		int polyId = 6894;
		if (pc.hasSkillEffect(SHAPE_CHANGE)) {
			pc.killSkillEffectTimer(SHAPE_CHANGE);
		}
		L1ItemInstance weapon = pc.getWeapon();
		boolean weaponTakeoff = (weapon != null && !L1PolyMorph.isEquipableWeapon(polyId, weapon.getItem().getType()));
		if (weaponTakeoff) { // 解除武器時
			pc.setCurrentWeapon(0);
		}
		pc.setTempCharGfx(polyId);
		pc.sendPackets(new S_ChangeShape(pc.getId(), polyId, pc.getCurrentWeapon()));
		if (pc.isGmInvis()) { // GM隱身
		} else if (pc.isInvisble()) { // 一般隱身
			pc.broadcastPacketForFindInvis(new S_ChangeShape(pc.getId(), polyId, pc.getCurrentWeapon()), true);
		} else {
			pc.broadcastPacket(new S_ChangeShape(pc.getId(), polyId, pc.getCurrentWeapon()));
		}
		pc.getInventory().takeoffEquip(polyId); // 是否將裝備的武器強制解除。
	}

	/**
	 * 解除龍之覺醒變身
	 * <p>將玩家外觀恢復為原始職業外觀。
	 *
	 * <h3>處理流程:</h3>
	 * <ol>
	 *   <li>取得玩家的職業 ID (ClassId)</li>
	 *   <li>將臨時外觀設定為職業 ID</li>
	 *   <li>若玩家未死亡，發送外觀變更封包給自己及周圍玩家</li>
	 * </ol>
	 *
	 * <p><b>注意:</b> 若玩家已死亡，不發送外觀變更封包，避免覆蓋死亡狀態的外觀。
	 *
	 * @param pc 玩家角色實例
	 * @see #doPoly(L1PcInstance)
	 * @see L1PcInstance#getClassId()
	 */
	public static void undoPoly(L1PcInstance pc) {
		int classId = pc.getClassId();
		pc.setTempCharGfx(classId);
		if (!pc.isDead()) {
			pc.sendPackets(new S_ChangeShape(pc.getId(), classId, pc.getCurrentWeapon()));
			pc.broadcastPacket(new S_ChangeShape(pc.getId(), classId, pc.getCurrentWeapon()));
		}
	}

}
