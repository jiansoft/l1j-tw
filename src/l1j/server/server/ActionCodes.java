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

/**
 * 角色動作代碼定義類別
 *
 * <p>此類別定義了遊戲中所有角色、NPC 和物件的動作代碼常數。
 * 這些代碼用於控制遊戲客戶端顯示的視覺動作效果。
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>定義角色的各種動作狀態 (走路、攻擊、受傷、待機等)</li>
 *   <li>支援不同武器類型的專屬動作 (劍、斧、弓、矛、法杖等)</li>
 *   <li>定義社交表情動作 (敬禮、歡呼、思考等)</li>
 *   <li>定義特殊物件動作 (門、塔、怪物特殊行為)</li>
 * </ul>
 *
 * <h3>動作分類:</h3>
 * <ul>
 *   <li><b>基本動作:</b> Walk, Attack, Damage, Idle, Die</li>
 *   <li><b>劍類武器:</b> SwordWalk, SwordAttack, SwordDamage, SwordIdle</li>
 *   <li><b>斧類武器:</b> AxeWalk, AxeAttack, AxeDamage, AxeIdle</li>
 *   <li><b>弓類武器:</b> BowWalk, BowAttack, BowDamage, BowIdle</li>
 *   <li><b>矛類武器:</b> SpearWalk, SpearAttack, SpearDamage, SpearIdle</li>
 *   <li><b>法杖類:</b> StaffWalk, StaffAttack, StaffDamage, StaffIdle</li>
 *   <li><b>匕首類:</b> DaggerWalk, DaggerAttack, DaggerDamage, DaggerIdle</li>
 *   <li><b>雙手劍:</b> TwoHandSwordWalk, TwoHandSwordAttack, TwoHandSwordDamage, TwoHandSwordIdle</li>
 *   <li><b>鎖鏈劍 (이도류):</b> EdoryuWalk, EdoryuAttack, EdoryuDamage, EdoryuIdle</li>
 *   <li><b>手甲 (Claw):</b> ClawWalk, ClawAttack, ClawDamage, ClawIdle</li>
 *   <li><b>飛刀:</b> ThrowingKnifeWalk, ThrowingKnifeAttack, ThrowingKnifeDamage, ThrowingKnifeIdle</li>
 *   <li><b>魔法動作:</b> Wand, SkillAttack, SkillBuff, SpellDirectionExtra</li>
 *   <li><b>物品動作:</b> Pickup, Throw</li>
 *   <li><b>表情動作:</b> Think (Alt+4), Aggress (Alt+3), Salute (Alt+1), Cheer (Alt+2)</li>
 *   <li><b>特殊動作:</b> Shop, Fishing, Appear, Hide</li>
 *   <li><b>物件狀態:</b> Open/Close, On/Off, South/West</li>
 *   <li><b>塔樓動作:</b> TowerCrack1-3, TowerDie</li>
 *   <li><b>門動作:</b> DoorAction1-5, DoorDie</li>
 *   <li><b>移動相關:</b> Moveup, Movedown</li>
 * </ul>
 *
 * <h3>動作代碼結構:</h3>
 * <p>每種武器類型通常有四種基本動作:
 * <ol>
 *   <li><b>Walk</b> - 持武器走路的動作</li>
 *   <li><b>Attack</b> - 攻擊動作</li>
 *   <li><b>Damage</b> - 受傷動作</li>
 *   <li><b>Idle</b> - 待機/站立動作</li>
 * </ol>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * // 在伺服器封包中發送角色動作
 * pc.broadcastPacket(new S_DoActionGfx(pc.getId(), ActionCodes.ACTION_Attack));
 *
 * // 根據裝備武器類型設定動作
 * if (weaponType == L1ItemWeaponType.SWORD) {
 *     return ActionCodes.ACTION_SwordAttack;
 * }
 *
 * // 顯示社交表情動作
 * pc.broadcastPacket(new S_DoActionGfx(pc.getId(), ActionCodes.ACTION_Salute));
 * </pre>
 *
 * <h3>重要說明:</h3>
 * <ul>
 *   <li>某些動作代碼值重複使用 (例如: ACTION_Hide=11 與 ACTION_AxeWalk=11)</li>
 *   <li>不同物件類型 (角色/NPC/門/塔) 可能使用相同的代碼值但表示不同動作</li>
 *   <li>客戶端根據物件類型和代碼值來顯示對應的動畫</li>
 *   <li>特殊 Boss 怪物 (如 Antharas) 有專屬的動作代碼</li>
 * </ul>
 *
 * @see l1j.server.server.serverpackets.S_DoActionGfx
 * @see l1j.server.server.serverpackets.S_CharVisualUpdate
 * @see l1j.server.server.model.Instance.L1PcInstance
 */
public class ActionCodes {

	/**
	 * 建構式
	 */
	public ActionCodes() {
	}

	// ========================================
	// 基本動作
	// ========================================

	/** 基本走路動作 */
	public static final int ACTION_Walk = 0;

	/** 基本攻擊動作 */
	public static final int ACTION_Attack = 1;

	/** 基本受傷動作 */
	public static final int ACTION_Damage = 2;

	/** 基本待機/站立動作 */
	public static final int ACTION_Idle = 3;

	/** 死亡動作 */
	public static final int ACTION_Die = 8;

	// ========================================
	// 特殊顯示效果
	// ========================================

	/** 出現效果 (隱形狀態解除或物件生成) */
	public static final int ACTION_Appear = 4;

	/** 隱藏效果 (進入隱形狀態或物件消失) */
	public static final int ACTION_Hide = 11;

	/** 安塔瑞斯 (Antharas) 特殊隱藏動作 */
	public static final int ACTION_AntharasHide = 20;

	// ========================================
	// 劍類武器動作
	// ========================================

	/** 持劍走路動作 */
	public static final int ACTION_SwordWalk = 4;

	/** 持劍攻擊動作 */
	public static final int ACTION_SwordAttack = 5;

	/** 持劍受傷動作 */
	public static final int ACTION_SwordDamage = 6;

	/** 持劍待機動作 */
	public static final int ACTION_SwordIdle = 7;

	// ========================================
	// 斧類武器動作
	// ========================================

	/** 持斧走路動作 */
	public static final int ACTION_AxeWalk = 11;

	/** 持斧攻擊動作 */
	public static final int ACTION_AxeAttack = 12;

	/** 持斧受傷動作 */
	public static final int ACTION_AxeDamage = 13;

	/** 持斧待機動作 */
	public static final int ACTION_AxeIdle = 14;

	// ========================================
	// 隱身狀態專用動作
	// ========================================

	/** 隱身狀態下的受傷動作 */
	public static final int ACTION_HideDamage = 13;

	/** 隱身狀態下的待機動作 */
	public static final int ACTION_HideIdle = 14;

	// ========================================
	// 物品互動動作
	// ========================================

	/** 拾取物品動作 */
	public static final int ACTION_Pickup = 15;

	/** 丟擲物品動作 */
	public static final int ACTION_Throw = 16;

	// ========================================
	// 魔法相關動作
	// ========================================

	/** 使用魔杖動作 */
	public static final int ACTION_Wand = 17;

	/** 施放攻擊魔法動作 */
	public static final int ACTION_SkillAttack = 18;

	/** 施放輔助魔法動作 */
	public static final int ACTION_SkillBuff = 19;

	/** 方向性魔法額外效果 */
	public static final int ACTION_SpellDirectionExtra = 31;

	// ========================================
	// 弓類武器動作
	// ========================================

	/** 持弓走路動作 */
	public static final int ACTION_BowWalk = 20;

	/** 持弓攻擊動作 */
	public static final int ACTION_BowAttack = 21;

	/** 持弓受傷動作 */
	public static final int ACTION_BowDamage = 22;

	/** 持弓待機動作 */
	public static final int ACTION_BowIdle = 23;

	// ========================================
	// 矛類武器動作
	// ========================================

	/** 持矛走路動作 */
	public static final int ACTION_SpearWalk = 24;

	/** 持矛攻擊動作 */
	public static final int ACTION_SpearAttack = 25;

	/** 持矛受傷動作 */
	public static final int ACTION_SpearDamage = 26;

	/** 持矛待機動作 */
	public static final int ACTION_SpearIdle = 27;

	// ========================================
	// 物件狀態切換動作 (門、機關等)
	// ========================================

	/** 開啟/啟動狀態 */
	public static final int ACTION_On = 28;

	/** 關閉/停用狀態 */
	public static final int ACTION_Off = 29;

	/** 門開啟動作 */
	public static final int ACTION_Open = 28;

	/** 門關閉動作 */
	public static final int ACTION_Close = 29;

	/** 朝南方向 */
	public static final int ACTION_South = 28;

	/** 朝西方向 */
	public static final int ACTION_West = 29;

	// ========================================
	// 其他特殊攻擊動作
	// ========================================

	/** 替代攻擊動作 */
	public static final int ACTION_AltAttack = 30;

	// ========================================
	// 塔樓損壞狀態動作
	// ========================================

	/** 塔樓裂痕階段 1 (輕度損壞) */
	public static final int ACTION_TowerCrack1 = 32;

	/** 塔樓裂痕階段 2 (中度損壞) */
	public static final int ACTION_TowerCrack2 = 33;

	/** 塔樓裂痕階段 3 (重度損壞) */
	public static final int ACTION_TowerCrack3 = 34;

	/** 塔樓倒塌/摧毀動作 */
	public static final int ACTION_TowerDie = 35;

	// ========================================
	// 門的多階段動作
	// ========================================

	/** 門動作階段 1 */
	public static final int ACTION_DoorAction1 = 32;

	/** 門動作階段 2 */
	public static final int ACTION_DoorAction2 = 33;

	/** 門動作階段 3 */
	public static final int ACTION_DoorAction3 = 34;

	/** 門動作階段 4 */
	public static final int ACTION_DoorAction4 = 35;

	/** 門動作階段 5 */
	public static final int ACTION_DoorAction5 = 36;

	/** 門損壞/破壞動作 */
	public static final int ACTION_DoorDie = 37;

	// ========================================
	// 法杖類武器動作
	// ========================================

	/** 持法杖走路動作 */
	public static final int ACTION_StaffWalk = 40;

	/** 持法杖攻擊動作 */
	public static final int ACTION_StaffAttack = 41;

	/** 持法杖受傷動作 */
	public static final int ACTION_StaffDamage = 42;

	/** 持法杖待機動作 */
	public static final int ACTION_StaffIdle = 43;

	// ========================================
	// 垂直移動動作
	// ========================================

	/** 向上移動 (飛行、浮空等) */
	public static final int ACTION_Moveup = 44;

	/** 向下移動 (降落、下潛等) */
	public static final int ACTION_Movedown = 45;

	// ========================================
	// 匕首類武器動作
	// ========================================

	/** 持匕首走路動作 */
	public static final int ACTION_DaggerWalk = 46;

	/** 持匕首攻擊動作 */
	public static final int ACTION_DaggerAttack = 47;

	/** 持匕首受傷動作 */
	public static final int ACTION_DaggerDamage = 48;

	/** 持匕首待機動作 */
	public static final int ACTION_DaggerIdle = 49;

	// ========================================
	// 雙手劍武器動作
	// ========================================

	/** 持雙手劍走路動作 */
	public static final int ACTION_TwoHandSwordWalk = 50;

	/** 持雙手劍攻擊動作 */
	public static final int ACTION_TwoHandSwordAttack = 51;

	/** 持雙手劍受傷動作 */
	public static final int ACTION_TwoHandSwordDamage = 52;

	/** 持雙手劍待機動作 */
	public static final int ACTION_TwoHandSwordIdle = 53;

	// ========================================
	// 鎖鏈劍 (이도류/Edoryu) 武器動作
	// ========================================

	/** 持鎖鏈劍走路動作 */
	public static final int ACTION_EdoryuWalk = 54;

	/** 持鎖鏈劍攻擊動作 */
	public static final int ACTION_EdoryuAttack = 55;

	/** 持鎖鏈劍受傷動作 */
	public static final int ACTION_EdoryuDamage = 56;

	/** 持鎖鏈劍待機動作 */
	public static final int ACTION_EdoryuIdle = 57;

	// ========================================
	// 手甲 (Claw) 武器動作
	// ========================================

	/** 持手甲走路動作 */
	public static final int ACTION_ClawWalk = 58;

	/** 持手甲攻擊動作 */
	public static final int ACTION_ClawAttack = 59;

	/** 持手甲受傷動作 */
	public static final int ACTION_ClawDamage = 60;

	/** 持手甲待機動作 */
	public static final int ACTION_ClawIdle = 61;

	// ========================================
	// 飛刀 (Throwing Knife) 武器動作
	// ========================================

	/** 持飛刀走路動作 */
	public static final int ACTION_ThrowingKnifeWalk = 62;

	/** 持飛刀攻擊動作 */
	public static final int ACTION_ThrowingKnifeAttack = 63;

	/** 持飛刀受傷動作 */
	public static final int ACTION_ThrowingKnifeDamage = 64;

	/** 持飛刀待機動作 */
	public static final int ACTION_ThrowingKnifeIdle = 65;

	// ========================================
	// 社交表情動作 (Alt 快捷鍵)
	// ========================================

	/** 思考動作 (Alt+4) */
	public static final int ACTION_Think = 66;

	/** 挑釁動作 (Alt+3) */
	public static final int ACTION_Aggress = 67;

	/** 敬禮動作 (Alt+1) */
	public static final int ACTION_Salute = 68;

	/** 歡呼動作 (Alt+2) */
	public static final int ACTION_Cheer = 69;

	// ========================================
	// 特殊活動動作
	// ========================================

	/** 開設個人商店動作 */
	public static final int ACTION_Shop = 70;

	/** 釣魚動作 */
	public static final int ACTION_Fishing = 71;

}