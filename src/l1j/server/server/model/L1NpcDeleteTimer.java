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

import java.util.Timer;
import java.util.TimerTask;

import l1j.server.server.ActionCodes;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.serverpackets.S_DoActionGFX;

/**
 * NPC 延遲刪除計時器
 * <p>用於在指定時間後自動刪除 NPC，特別處理龍之門扉 (Dragon Portal) 等特殊 NPC。
 *
 * <h3>主要用途:</h3>
 * <ul>
 *   <li>召喚物、臨時 NPC 的生命週期管理</li>
 *   <li>龍之門扉 (屠龍副本入口) 的自動關閉</li>
 *   <li>限時活動 NPC 的自動清除</li>
 * </ul>
 *
 * <h3>特殊處理的 NPC:</h3>
 * <ul>
 *   <li><b>81273-81276:</b> 龍之門扉 (一般副本入口)</li>
 *   <li><b>81277:</b> 隱匿的巨龍谷入口 (Hidden Dragon Valley)</li>
 * </ul>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * // 建立一個 10 秒後刪除 NPC 的計時器
 * L1NpcDeleteTimer timer = new L1NpcDeleteTimer(npc, 10000);
 * timer.begin();  // 啟動計時器
 * </pre>
 *
 * @see L1DragonSlayer
 * @see L1NpcInstance#deleteMe()
 */
public class L1NpcDeleteTimer extends TimerTask {
	/**
	 * 建構 NPC 延遲刪除計時器
	 *
	 * @param npc 要刪除的 NPC 實例
	 * @param timeMillis 延遲時間 (毫秒)
	 */
	public L1NpcDeleteTimer(L1NpcInstance npc, int timeMillis) {
		_npc = npc;
		_timeMillis = timeMillis;
	}

	/**
	 * 計時器到期時執行的任務
	 * <p>執行 NPC 刪除動作，針對龍之門扉進行特殊處理。
	 *
	 * <h3>處理流程:</h3>
	 * <ol>
	 *   <li>檢查 NPC 是否為龍之門扉 (NpcId: 81273-81277)</li>
	 *   <li>若為龍之門扉:
	 *     <ul>
	 *       <li>若為隱匿的巨龍谷入口 (81277)，關閉該入口狀態</li>
	 *       <li>清除該門扉的副本資料</li>
	 *       <li>結束屠龍副本</li>
	 *       <li>播放門扉消失動作 (ACTION_Die)</li>
	 *     </ul>
	 *   </li>
	 *   <li>刪除 NPC (從世界中移除)</li>
	 *   <li>取消計時器任務</li>
	 * </ol>
	 *
	 * @see L1DragonSlayer#setHiddenDragonValleyStstus(int)
	 * @see L1DragonSlayer#setPortalPack(int, L1NpcInstance)
	 * @see L1DragonSlayer#endDragonPortal(int)
	 * @see L1NpcInstance#deleteMe()
	 */
	@Override
	public void run() {
		// 龍之門扉存在時間到時
		if (_npc != null) {
			if (_npc.getNpcId() == 81273 || _npc.getNpcId() == 81274 || _npc.getNpcId() == 81275
					|| _npc.getNpcId() == 81276 || _npc.getNpcId() == 81277) {
				if (_npc.getNpcId() == 81277) { // 隱匿的巨龍谷入口關閉
					L1DragonSlayer.getInstance().setHiddenDragonValleyStstus(0);
				}
				// 結束屠龍副本
				L1DragonSlayer.getInstance().setPortalPack(_npc.getPortalNumber(), null);
				L1DragonSlayer.getInstance().endDragonPortal(_npc.getPortalNumber());
				// 門扉消失動作
				_npc.setStatus(ActionCodes.ACTION_Die);
				_npc.broadcastPacket(new S_DoActionGFX(_npc.getId(), ActionCodes.ACTION_Die));
			}
			_npc.deleteMe();
			cancel();
		}
	}

	/**
	 * 啟動計時器
	 * <p>建立一個新的 Timer 並排程在指定時間後執行 {@link #run()} 方法。
	 *
	 * <p><b>注意:</b> 此方法會建立新的 Timer 實例，務必確保計時器最終會被取消以避免資源洩漏。
	 *
	 * @see Timer#schedule(TimerTask, long)
	 */
	public void begin() {
		Timer timer = new Timer();
		timer.schedule(this, _timeMillis);
	}

	/**
	 * 要刪除的 NPC 實例
	 * <p>計時器到期時會呼叫此 NPC 的 {@link L1NpcInstance#deleteMe()} 方法。
	 */
	private final L1NpcInstance _npc;

	/**
	 * 延遲時間 (毫秒)
	 * <p>從呼叫 {@link #begin()} 到執行 {@link #run()} 的延遲時間。
	 */
	private final int _timeMillis;
}
