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
package l1j.server.server.utils;

/**
 * <p>
 * 最低値lowと最大値highによって囲まれた、数値の範囲を指定するクラス。
 * </p>
 * <p>
 * <b>このクラスは同期化されない。</b> 複数のスレッドが同時にこのクラスのインスタンスにアクセスし、
 * 1つ以上のスレッドが範囲を変更する場合、外部的な同期化が必要である。
 * </p>
 *
 * <p>
 * 由最低值 low 和最高值 high 界定的整數範圍指定類。
 * </p>
 * <p>
 * <b>此類不是線程安全的。</b> 如果多個線程同時存取此類的實例，
 * 並且其中一個或多個線程修改範圍，則需要外部同步。
 * </p>
 */
public class IntRange {
	private int _low;
	private int _high;

	/**
	 * 使用指定的下界和上界構造新的整數範圍。
	 *
	 * @param low
	 *            範圍的最低值
	 * @param high
	 *            範圍的最高值
	 */
	public IntRange(int low, int high) {
		_low = low;
		_high = high;
	}

	/**
	 * 複製構造函式，基於現有的 IntRange 物件建立新的實例。
	 *
	 * @param range
	 *            要複製的範圍物件
	 */
	public IntRange(IntRange range) {
		this(range._low, range._high);
	}

	/**
	 * 数値iが、範囲内にあるかを返す。
	 *
	 * @param i
	 *            数値
	 * @return 範囲内であればtrue
	 *
	 * 檢查整數 i 是否在此範圍內。
	 *
	 * @param i
	 *            整數值
	 * @return 若在範圍內則返回 true，否則返回 false
	 */
	public boolean includes(int i) {
		return (_low <= i) && (i <= _high);
	}

	/**
	 * 檢查整數 i 是否在指定的範圍 [low, high] 內。
	 *
	 * @param i
	 *            整數值
	 * @param low
	 *            範圍下界（包含）
	 * @param high
	 *            範圍上界（包含）
	 * @return 若在範圍內則返回 true，否則返回 false
	 */
	public static boolean includes(int i, int low, int high) {
		return (low <= i) && (i <= high);
	}

	/**
	 * 数値iを、この範囲内に丸める。
	 *
	 * @param i
	 *            数値
	 * @return 丸められた値
	 *
	 * 將整數 i 限制在此範圍 [low, high] 內。
	 *
	 * @param i
	 *            整數值
	 * @return 若 i < low 則返回 low；若 i > high 則返回 high；否則返回 i
	 */
	public int ensure(int i) {
		int r = i;
		r = (_low <= r) ? r : _low;
		r = (r <= _high) ? r : _high;
		return r;
	}

	/**
	 * 將整數 n 限制在指定的範圍 [low, high] 內。
	 *
	 * @param n
	 *            整數值
	 * @param low
	 *            範圍下界（包含）
	 * @param high
	 *            範圍上界（包含）
	 * @return 若 n < low 則返回 low；若 n > high 則返回 high；否則返回 n
	 */
	public static int ensure(int n, int low, int high) {
		int r = n;
		r = (low <= r) ? r : low;
		r = (r <= high) ? r : high;
		return r;
	}

	/**
	 * この範囲内からランダムな値を生成する。
	 *
	 * @return 範囲内のランダムな値
	 *
	 * 從此範圍內生成隨機整數值。
	 *
	 * @return [low, high] 範圍內的隨機整數
	 */
	public int randomValue() {
		return Random.nextInt(getWidth() + 1) + _low;
	}

	/**
	 * 取得此範圍的下界值。
	 *
	 * @return 範圍的最低值
	 */
	public int getLow() {
		return _low;
	}

	/**
	 * 取得此範圍的上界值。
	 *
	 * @return 範圍的最高值
	 */
	public int getHigh() {
		return _high;
	}

	/**
	 * 取得此範圍的寬度（範圍大小）。
	 *
	 * @return high - low 的結果
	 */
	public int getWidth() {
		return _high - _low;
	}

	/**
	 * 比較此範圍與另一個物件是否相等。
	 * 兩個 IntRange 物件相等當且唯當它們的 low 和 high 值相同。
	 *
	 * @param obj
	 *            要比較的物件
	 * @return 若兩者範圍相等則返回 true，否則返回 false
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof IntRange)) {
			return false;
		}
		IntRange range = (IntRange) obj;
		return (this._low == range._low) && (this._high == range._high);
	}

	/**
	 * 返回此範圍的字符串表示。
	 *
	 * @return 格式為 "low=X, high=Y" 的字符串
	 */
	@Override
	public String toString() {
		return "low=" + _low + ", high=" + _high;
	}
}
