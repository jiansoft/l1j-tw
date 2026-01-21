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

import l1j.server.server.utils.IntRange;

/**
 * 業值 (Karma) 系統管理類別
 * <p>處理玩家的業值累積、等級計算及百分比進度。
 *
 * <h3>業值系統說明:</h3>
 * <p>業值反映玩家的善惡行為，可為正值 (正業) 或負值 (負業)。
 * <ul>
 *   <li><b>正業值:</b> 等級 +1 ~ +8，透過善行獲得</li>
 *   <li><b>負業值:</b> 等級 -1 ~ -8，透過惡行獲得 (如 PK)</li>
 *   <li><b>業值範圍:</b> -15,500,000 ~ +15,500,000</li>
 * </ul>
 *
 * <h3>業值等級門檻:</h3>
 * <table border="1">
 *   <tr><th>等級</th><th>所需業值</th></tr>
 *   <tr><td>±1</td><td>10,000</td></tr>
 *   <tr><td>±2</td><td>20,000</td></tr>
 *   <tr><td>±3</td><td>100,000</td></tr>
 *   <tr><td>±4</td><td>500,000</td></tr>
 *   <tr><td>±5</td><td>1,500,000</td></tr>
 *   <tr><td>±6</td><td>3,000,000</td></tr>
 *   <tr><td>±7</td><td>5,000,000</td></tr>
 *   <tr><td>±8</td><td>10,000,000</td></tr>
 *   <tr><td>±8 (Max)</td><td>15,500,000</td></tr>
 * </table>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * L1Karma karma = new L1Karma();
 * karma.set(50000);           // 設定業值
 * int level = karma.getLevel();  // 取得業值等級 (例: 3)
 * int percent = karma.getPercent(); // 取得當前等級進度 (0-100%)
 * karma.add(1000);            // 增加業值
 * </pre>
 *
 * @see l1j.server.server.model.Instance.L1PcInstance#getKarma()
 */
public class L1Karma {
	/**
	 * 業值等級門檻陣列
	 * <p>定義從等級 1 到等級 8 所需的業值點數。
	 */
	private static final int[] KARMA_POINT = { 10000, 20000, 100000, 500000,
			1500000, 3000000, 5000000, 10000000, 15500000 };

	/**
	 * 業值上下限範圍: -15,500,000 ~ +15,500,000
	 * <p>超出範圍的值會被自動限制在此範圍內。
	 */
	private static IntRange KARMA_RANGE = new IntRange(-15500000, 15500000);

	/**
	 * 當前業值
	 * <p>負值表示負業 (惡行)，正值表示正業 (善行)。
	 */
	private int _karma = 0;

	/**
	 * 取得當前業值
	 *
	 * @return 當前業值 (範圍: -15,500,000 ~ +15,500,000)
	 */
	public int get() {
		return _karma;
	}

	/**
	 * 設定業值
	 * <p>自動將超出範圍的值限制在 -15,500,000 ~ +15,500,000 之間。
	 *
	 * @param i 要設定的業值
	 * @see #KARMA_RANGE
	 */
	public void set(int i) {
		_karma = KARMA_RANGE.ensure(i);
	}

	/**
	 * 增加或減少業值
	 * <p>會自動檢查範圍限制，確保業值不會超出上下限。
	 *
	 * @param i 要增加的業值 (負值表示減少)
	 * @see #set(int)
	 */
	public void add(int i) {
		set(_karma + i);
	}

	/**
	 * 取得業值等級
	 * <p>根據當前業值計算對應的等級 (範圍: -8 ~ +8)。
	 *
	 * <h3>計算邏輯:</h3>
	 * <ol>
	 *   <li>取得當前業值的絕對值</li>
	 *   <li>依序比對 {@link #KARMA_POINT} 門檻陣列，計算達到的等級</li>
	 *   <li>最高等級為 8</li>
	 *   <li>若業值為負數，返回負等級</li>
	 * </ol>
	 *
	 * <h3>等級範例:</h3>
	 * <ul>
	 *   <li>業值 0 → 等級 0</li>
	 *   <li>業值 15,000 → 等級 1</li>
	 *   <li>業值 150,000 → 等級 3</li>
	 *   <li>業值 -500,000 → 等級 -4</li>
	 *   <li>業值 15,500,000 → 等級 8</li>
	 * </ul>
	 *
	 * @return 業值等級 (-8 ~ +8)，0 表示無業值
	 * @see #KARMA_POINT
	 */
	public int getLevel() {
		boolean isMinus = false;
		int karmaLevel = 0;

		int karma = get();
		if (karma < 0) {
			isMinus = true;
			karma *= -1;
		}

		for (int point : KARMA_POINT) {
			if (karma >= point) {
				karmaLevel++;
				if (karmaLevel >= 8) {
					break;
				}
			} else {
				break;
			}
		}
		if (isMinus) {
			karmaLevel *= -1;
		}

		return karmaLevel;
	}

	/**
	 * 取得當前等級的業值進度百分比
	 * <p>計算從當前等級到下一等級的進度 (0-100%)。
	 *
	 * <h3>計算公式:</h3>
	 * <pre>
	 * 進度 = 100 × (當前業值 - 當前等級最低門檻) / (下一等級門檻 - 當前等級門檻)
	 * </pre>
	 *
	 * <h3>計算範例:</h3>
	 * <ul>
	 *   <li>等級 0 → 永遠返回 0%</li>
	 *   <li>等級 1 (業值 15,000):
	 *     <ul>
	 *       <li>進度 = 100 × (15000 - 10000) / (20000 - 10000) = 50%</li>
	 *     </ul>
	 *   </li>
	 *   <li>等級 3 (業值 150,000):
	 *     <ul>
	 *       <li>進度 = 100 × (150000 - 100000) / (500000 - 100000) = 12%</li>
	 *     </ul>
	 *   </li>
	 * </ul>
	 *
	 * <p><b>注意:</b> 負業值會先轉換為正值再計算進度。
	 *
	 * @return 當前等級進度百分比 (0-100)，等級 0 時返回 0
	 * @see #getLevel()
	 * @see #KARMA_POINT
	 */
	public int getPercent() {
		int karma = get();
		int karmaLevel = getLevel();
		if (karmaLevel == 0) {
			return 0;
		}

		if (karma < 0) {
			karma *= -1;
			karmaLevel *= -1;
		}

		return 100 * (karma - KARMA_POINT[karmaLevel - 1])
				/ (KARMA_POINT[karmaLevel] - KARMA_POINT[karmaLevel - 1]);
	}
}
