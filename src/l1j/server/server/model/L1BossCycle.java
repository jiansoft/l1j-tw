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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import l1j.server.server.datatables.BossSpawnTable;
import l1j.server.server.utils.PerformanceTimer;
import l1j.server.server.utils.Random;
import l1j.server.server.utils.collections.Maps;

/**
 * Boss 怪物出現週期管理類別
 *
 * <p>此類別負責管理遊戲中 Boss 怪物的出現週期系統。Boss 怪物不會固定時間出現,
 * 而是依據設定的週期、基準時間和隨機範圍來決定出現時間,增加遊戲的不可預測性。
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>從 XML 配置檔載入 Boss 出現週期設定</li>
 *   <li>計算 Boss 的出現時間 (基於週期和隨機因素)</li>
 *   <li>管理出現期間的開始與結束時間</li>
 *   <li>支援多個 Boss 的獨立週期管理</li>
 *   <li>支援使用者自定義配置覆蓋預設設定</li>
 * </ul>
 *
 * <h3>週期系統設計:</h3>
 * <ul>
 *   <li><b>基準時間 (Base):</b> 週期計算的起始時間點</li>
 *   <li><b>週期長度 (Period):</b> Boss 重生的時間間隔 (可指定 天/小時/分鐘)</li>
 *   <li><b>出現區間 (Start-End):</b> 在週期內的出現時間範圍</li>
 *   <li><b>隨機機制:</b> 在 Start 到 End 之間隨機選擇出現時間</li>
 * </ul>
 *
 * <h3>時間格式規範:</h3>
 * <pre>
 * 日期格式: yyyy/MM/dd (例: 2024/01/15)
 * 時間格式: HH:mm (例: 14:30)
 * 週期格式: XdYhZm (例: 2d3h30m 表示 2天3小時30分鐘)
 *   - d: 天數
 *   - h: 小時數
 *   - m: 分鐘數
 * </pre>
 *
 * <h3>XML 配置範例:</h3>
 * <pre>
 * &lt;BossCycle Name="安塔瑞斯"&gt;
 *   &lt;Base Date="2024/01/01" Time="00:00"/&gt;
 *   &lt;Cycle Period="7d" Start="0d" End="1d"/&gt;
 * &lt;/BossCycle&gt;
 *
 * 解釋:
 * - 基準時間: 2024/01/01 00:00
 * - 週期: 每 7 天
 * - 出現區間: 週期開始後 0-24 小時內隨機出現
 * </pre>
 *
 * <h3>計算邏輯:</h3>
 * <ol>
 *   <li>根據基準時間計算當前所在的週期</li>
 *   <li>計算該週期的出現開始時間 (base + start)</li>
 *   <li>計算該週期的出現結束時間 (base + end)</li>
 *   <li>在開始與結束時間之間隨機選擇實際出現時間</li>
 * </ol>
 *
 * <h3>配置檔位置:</h3>
 * <ul>
 *   <li>預設配置: {@code ./data/xml/Cycle/BossCycle.xml}</li>
 *   <li>使用者配置: {@code ./data/xml/Cycle/users/BossCycle.xml} (會覆蓋預設)</li>
 * </ul>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * // 載入所有 Boss 週期設定
 * L1BossCycle.load();
 *
 * // 取得特定 Boss 的週期
 * L1BossCycle cycle = L1BossCycle.getBossCycle("安塔瑞斯");
 *
 * // 計算當前週期的出現時間
 * Calendar now = Calendar.getInstance();
 * Calendar spawnTime = cycle.calcSpawnTime(now);
 *
 * // 取得出現期間
 * Calendar startTime = cycle.getSpawnStartTime(now);
 * Calendar endTime = cycle.getSpawnEndTime(now);
 *
 * // 計算下一週期的出現時間
 * Calendar nextSpawn = cycle.nextSpawnTime(now);
 * </pre>
 *
 * <h3>設計模式:</h3>
 * <ul>
 *   <li><b>JAXB 映射:</b> 使用 XML 註解自動序列化/反序列化</li>
 *   <li><b>靜態快取:</b> 所有週期物件快取於 _cycleMap</li>
 *   <li><b>工廠方法:</b> 透過 getBossCycle() 取得週期實例</li>
 * </ul>
 *
 * <h3>重要說明:</h3>
 * <ul>
 *   <li>週期長度必須大於 0,否則會拋出例外</li>
 *   <li>Start 必須小於等於 End,系統會自動修正錯誤設定</li>
 *   <li>Start 與 End 至少相差 1 分鐘,避免週期重疊</li>
 *   <li>如果沒有設定基準時間,會使用系統啟動時間的 00:00 作為基準</li>
 *   <li>配置檔載入失敗會導致伺服器終止啟動</li>
 * </ul>
 *
 * @see l1j.server.server.datatables.BossSpawnTable
 * @see javax.xml.bind.annotation.XmlRootElement
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class L1BossCycle {
	/** Boss 週期名稱 (唯一識別) */
	@XmlAttribute(name = "Name")
	private String _name;

	/** 基準時間設定 (週期計算的起點) */
	@XmlElement(name = "Base")
	private Base _base;

	/**
	 * 基準時間內部類別
	 * <p>定義週期計算的基準日期與時間
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class Base {
		/** 基準日期 (格式: yyyy/MM/dd) */
		@XmlAttribute(name = "Date")
		private String _date;

		/** 基準時間 (格式: HH:mm) */
		@XmlAttribute(name = "Time")
		private String _time;

		/** 取得基準日期 */
		public String getDate() {
			return _date;
		}

		/** 設定基準日期 */
		public void setDate(String date) {
			_date = date;
		}

		/** 取得基準時間 */
		public String getTime() {
			return _time;
		}

		/** 設定基準時間 */
		public void setTime(String time) {
			_time = time;
		}
	}

	/** 週期設定 (包含週期長度與出現區間) */
	@XmlElement(name = "Cycle")
	private Cycle _cycle;

	/**
	 * 週期設定內部類別
	 * <p>定義 Boss 出現的週期長度、開始與結束時間
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class Cycle {
		/** 週期長度 (格式: XdYhZm, 例: 7d12h30m) */
		@XmlAttribute(name = "Period")
		private String _period;

		/** 出現開始時間相對於週期起點的偏移 (格式: XdYhZm) */
		@XmlAttribute(name = "Start")
		private String _start;

		/** 出現結束時間相對於週期起點的偏移 (格式: XdYhZm) */
		@XmlAttribute(name = "End")
		private String _end;

		/** 取得週期長度字串 */
		public String getPeriod() {
			return _period;
		}

		/** 取得開始時間偏移字串 */
		public String getStart() {
			return _start;
		}

		/** 取得結束時間偏移字串 */
		public String getEnd() {
			return _end;
		}
	}

	/** 計算後的基準日期時間 (Calendar 物件) */
	private Calendar _baseDate;

	/** 週期長度 (以分鐘為單位) */
	private int _period;

	/** 週期長度 - 天數部分 */
	private int _periodDay;

	/** 週期長度 - 小時部分 */
	private int _periodHour;

	/** 週期長度 - 分鐘部分 */
	private int _periodMinute;

	/** 出現開始時間相對於週期起點的偏移 (以分鐘為單位) */
	private int _startTime;

	/** 出現結束時間相對於週期起點的偏移 (以分鐘為單位) */
	private int _endTime;

	/** 日期格式化工具 (年/月/日) */
	private static SimpleDateFormat _sdfYmd = new SimpleDateFormat("yyyy/MM/dd");

	/** 時間格式化工具 (時:分) */
	private static SimpleDateFormat _sdfTime = new SimpleDateFormat("HH:mm");

	/** 完整日期時間格式化工具 (年/月/日 時:分) */
	private static SimpleDateFormat _sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	/** 預設初始日期 (當前系統時間) */
	private static Date _initDate = new Date();

	/** 預設初始時間 (00:00) */
	private static String _initTime = "0:00";

	/** 伺服器啟動時間 */
	private static final Calendar START_UP = Calendar.getInstance();

	/**
	 * 初始化週期設定
	 * <p>解析 XML 配置中的基準時間、週期長度、出現區間等參數,
	 * 並進行數值轉換與有效性驗證。
	 *
	 * <h3>初始化流程:</h3>
	 * <ol>
	 *   <li>驗證並設定基準時間 (Base)</li>
	 *   <li>解析週期長度 (Period) - 必須 > 0</li>
	 *   <li>解析出現開始與結束時間 (Start, End)</li>
	 *   <li>轉換所有時間為分鐘單位</li>
	 *   <li>修正無效的時間範圍設定</li>
	 *   <li>將基準時間推進至接近伺服器啟動時間的週期</li>
	 * </ol>
	 *
	 * <h3>自動修正規則:</h3>
	 * <ul>
	 *   <li>如果沒有設定基準時間 → 使用當前日期 00:00</li>
	 *   <li>如果 Start < 0 或 Start > Period → 設為 0</li>
	 *   <li>如果 End < 0 或 End > Period → 設為 Period</li>
	 *   <li>如果 Start > End → 將 Start 設為 End</li>
	 *   <li>如果 Start == End → 自動調整為相差 1 分鐘</li>
	 * </ul>
	 *
	 * @throws Exception 如果 Period 未設定或 ≤ 0
	 */
	public void init() throws Exception {
		// 基準日時の設定
		Base base = getBase();
		// 基準がなければ、現在日付の0:00基準
		if (base == null) {
			setBase(new Base());
			getBase().setDate(_sdfYmd.format(_initDate));
			getBase().setTime(_initTime);
			base = getBase();
		}
		else {
			try {
				_sdfYmd.parse(base.getDate());
			}
			catch (Exception e) {
				base.setDate(_sdfYmd.format(_initDate));
			}
			try {
				_sdfTime.parse(base.getTime());
			}
			catch (Exception e) {
				base.setTime(_initTime);
			}
		}
		// 基準日時を決定
		Calendar baseCal = Calendar.getInstance();
		baseCal.setTime(_sdf.parse(base.getDate() + " " + base.getTime()));

		// 出現周期の初期化,チェック
		Cycle spawn = getCycle();
		if ((spawn == null) || (spawn.getPeriod() == null)) {
			throw new Exception("CycleのPeriodは必須");
		}

		String period = spawn.getPeriod();
		_periodDay = getTimeParse(period, "d");
		_periodHour = getTimeParse(period, "h");
		_periodMinute = getTimeParse(period, "m");

		String start = spawn.getStart();
		int sDay = getTimeParse(start, "d");
		int sHour = getTimeParse(start, "h");
		int sMinute = getTimeParse(start, "m");
		String end = spawn.getEnd();
		int eDay = getTimeParse(end, "d");
		int eHour = getTimeParse(end, "h");
		int eMinute = getTimeParse(end, "m");

		// 分換算
		_period = (_periodDay * 24 * 60) + (_periodHour * 60) + _periodMinute;
		_startTime = (sDay * 24 * 60) + (sHour * 60) + sMinute;
		_endTime = (eDay * 24 * 60) + (eHour * 60) + eMinute;
		if (_period <= 0) {
			throw new Exception("must be Period > 0");
		}
		// start補正
		if ((_startTime < 0) || (_period < _startTime)) { // 補正
			_startTime = 0;
		}
		// end補正
		if ((_endTime < 0) || (_period < _endTime) || (end == null)) { // 補正
			_endTime = _period;
		}
		if (_startTime > _endTime) {
			_startTime = _endTime;
		}
		// start,endの相関補正(最低でも1分の間をあける)
		// start==endという指定でも、出現時間が次の周期に被らないようにするため
		if (_startTime == _endTime) {
			if (_endTime == _period) {
				_startTime--;
			}
			else {
				_endTime++;
			}
		}

		// 最近の周期まで補正(再計算するときに厳密に算出するので、ここでは近くまで適当に補正するだけ)
		while (!(baseCal.after(START_UP))) {
			baseCal.add(Calendar.DAY_OF_MONTH, _periodDay);
			baseCal.add(Calendar.HOUR_OF_DAY, _periodHour);
			baseCal.add(Calendar.MINUTE, _periodMinute);
		}
		_baseDate = baseCal;
	}

	/**
	 * 取得包含指定時間的週期起點
	 * <p>根據指定的目標時間,計算該時間所屬週期的開始時間。
	 * 此方法會從基準時間開始,反覆加減週期長度,找到最接近目標時間的週期起點。
	 *
	 * <h3>計算邏輯:</h3>
	 * <ol>
	 *   <li>從基準時間開始</li>
	 *   <li>如果目標時間在基準之後 → 不斷加上週期直到超過目標</li>
	 *   <li>如果目標時間在基準之前 → 不斷減去週期直到小於目標</li>
	 *   <li>檢查週期結束時間是否已過 → 如果已過則返回下一週期</li>
	 * </ol>
	 *
	 * <h3>範例說明:</h3>
	 * <p>假設週期為 2 小時:
	 * <pre>
	 * target    base    戻り值
	 * 4:59      7:00    3:00   (4:59 屬於 3:00-5:00 週期)
	 * 5:00      7:00    5:00   (5:00 屬於 5:00-7:00 週期)
	 * 5:01      7:00    5:00   (5:01 屬於 5:00-7:00 週期)
	 * 7:00      7:00    7:00   (7:00 屬於 7:00-9:00 週期)
	 * 7:01      7:00    7:00   (7:01 屬於 7:00-9:00 週期)
	 * </pre>
	 *
	 * @param target 要查詢的目標時間
	 * @return 包含該時間的週期起點 (Calendar 物件)
	 */
	private Calendar getBaseCycleOnTarget(Calendar target) {
		// 基準日時取得
		Calendar base = (Calendar) _baseDate.clone();
		if (target.after(base)) {
			// target <= baseとなるまで繰り返す
			while (target.after(base)) {
				base.add(Calendar.DAY_OF_MONTH, _periodDay);
				base.add(Calendar.HOUR_OF_DAY, _periodHour);
				base.add(Calendar.MINUTE, _periodMinute);
			}
		}
		if (target.before(base)) {
			while (target.before(base)) {
				base.add(Calendar.DAY_OF_MONTH, -_periodDay);
				base.add(Calendar.HOUR_OF_DAY, -_periodHour);
				base.add(Calendar.MINUTE, -_periodMinute);
			}
		}
		// 終了時間を算出してみて、過去の時刻ならボス時間が過ぎている→次の周期を返す。
		Calendar end = (Calendar) base.clone();
		end.add(Calendar.MINUTE, _endTime);
		if (end.before(target)) {
			base.add(Calendar.DAY_OF_MONTH, _periodDay);
			base.add(Calendar.HOUR_OF_DAY, _periodHour);
			base.add(Calendar.MINUTE, _periodMinute);
		}
		return base;
	}

	/**
	 * 計算 Boss 在指定時間所屬週期的出現時間
	 * <p>根據指定的當前時間,計算該時間所屬週期內的 Boss 隨機出現時間。
	 * 出現時間會在該週期的 Start 與 End 之間隨機決定。
	 *
	 * <h3>計算流程:</h3>
	 * <ol>
	 *   <li>取得包含當前時間的週期起點</li>
	 *   <li>加上出現開始時間偏移 (Start)</li>
	 *   <li>在 Start 到 End 之間隨機選擇秒數</li>
	 *   <li>返回最終的出現時間</li>
	 * </ol>
	 *
	 * <h3>隨機機制:</h3>
	 * <p>出現時間精確到秒,在整個 Start-End 區間內均勻分布隨機。
	 * 這確保 Boss 出現時間無法被玩家精確預測。
	 *
	 * @param now 當前時間 (用於判斷所屬週期)
	 * @return Boss 的出現時間 (精確到秒)
	 */
	public Calendar calcSpawnTime(Calendar now) {
		// 基準日時取得
		Calendar base = getBaseCycleOnTarget(now);
		// 出現期間の計算
		base.add(Calendar.MINUTE, _startTime);
		// 出現時間の決定 start～end迄の間でランダムの秒
		int diff = (_endTime - _startTime) * 60;
		int random = diff > 0 ? Random.nextInt(diff) : 0;
		base.add(Calendar.SECOND, random);
		return base;
	}

	/**
	 * 取得指定時間所屬週期的出現開始時間
	 * <p>計算該週期中 Boss 可能出現的最早時間 (週期起點 + Start 偏移)。
	 *
	 * @param now 當前時間 (用於判斷所屬週期)
	 * @return 該週期的 Boss 出現開始時間
	 */
	public Calendar getSpawnStartTime(Calendar now) {
		// 基準日時取得
		Calendar startDate = getBaseCycleOnTarget(now);
		// 出現期間の計算
		startDate.add(Calendar.MINUTE, _startTime);
		return startDate;
	}

	/**
	 * 取得指定時間所屬週期的出現結束時間
	 * <p>計算該週期中 Boss 可能出現的最晚時間 (週期起點 + End 偏移)。
	 *
	 * @param now 當前時間 (用於判斷所屬週期)
	 * @return 該週期的 Boss 出現結束時間
	 */
	public Calendar getSpawnEndTime(Calendar now) {
		// 基準日時取得
		Calendar endDate = getBaseCycleOnTarget(now);
		// 出現期間の計算
		endDate.add(Calendar.MINUTE, _endTime);
		return endDate;
	}

	/**
	 * 計算下一個週期的出現時間
	 * <p>將當前時間加上一個完整週期長度 (Period)，然後計算該週期的實際出現時間。
	 *
	 * @param now 當前時間
	 * @return 下一個週期的 Boss 出現時間 (含隨機偏移)
	 * @see #calcSpawnTime(Calendar)
	 */
	public Calendar nextSpawnTime(Calendar now) {
		// 基準日時取得
		Calendar next = (Calendar) now.clone();
		next.add(Calendar.DAY_OF_MONTH, _periodDay);
		next.add(Calendar.HOUR_OF_DAY, _periodHour);
		next.add(Calendar.MINUTE, _periodMinute);
		return calcSpawnTime(next);
	}

	/**
	 * 取得最近一次的出現開始時間
	 * <p>判斷當前時間是否已過該週期的開始時間:
	 * <ul>
	 *   <li>若 now ≥ 開始時間，返回當前週期的開始時間</li>
	 *   <li>若 now < 開始時間，返回上一個週期的開始時間</li>
	 * </ul>
	 *
	 * @param now 當前時間
	 * @return 最近一次的 Boss 出現開始時間
	 */
	public Calendar getLatestStartTime(Calendar now) {
		// 基準日時取得
		Calendar latestStart = getSpawnStartTime(now);
		if (!now.before(latestStart)) { // now >= latestStart
		}
		else {
			// now < latestStartなら1個前が最近の周期
			latestStart.add(Calendar.DAY_OF_MONTH, -_periodDay);
			latestStart.add(Calendar.HOUR_OF_DAY, -_periodHour);
			latestStart.add(Calendar.MINUTE, -_periodMinute);
		}

		return latestStart;
	}

	/**
	 * 解析時間字串中的數值
	 * <p>從格式化時間字串中提取特定單位的數值。
	 * <p>例如: 從 "2d3h30m" 中提取 "d" 會得到 2，提取 "h" 會得到 3。
	 *
	 * @param target 要解析的時間字串 (例: "2d3h30m")
	 * @param search 要搜尋的時間單位 (例: "d", "h", "m")
	 * @return 解析出的數值，若未找到則返回 0
	 */
	private static int getTimeParse(String target, String search) {
		if (target == null) {
			return 0;
		}
		int n = 0;
		Matcher matcher = Pattern.compile("\\d+" + search).matcher(target);
		if (matcher.find()) {
			String match = matcher.group();
			n = Integer.parseInt(match.replace(search, ""));
		}
		return n;
	}

	/**
	 * Boss 週期列表的 XML 映射類別
	 * <p>用於 JAXB 將 XML 檔案反序列化為 Java 物件。
	 * <p>對應的 XML 結構:
	 * <pre>
	 * &lt;BossCycleList&gt;
	 *   &lt;BossCycle name="boss1"&gt;...&lt;/BossCycle&gt;
	 *   &lt;BossCycle name="boss2"&gt;...&lt;/BossCycle&gt;
	 * &lt;/BossCycleList&gt;
	 * </pre>
	 *
	 * @see #load()
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "BossCycleList")
	static class L1BossCycleList {
		/** Boss 週期清單 */
		@XmlElement(name = "BossCycle")
		private List<L1BossCycle> bossCycles;

		/**
		 * 取得 Boss 週期清單
		 * @return Boss 週期清單
		 */
		public List<L1BossCycle> getBossCycles() {
			return bossCycles;
		}

		/**
		 * 設定 Boss 週期清單
		 * @param bossCycles Boss 週期清單
		 */
		public void setBossCycles(List<L1BossCycle> bossCycles) {
			this.bossCycles = bossCycles;
		}
	}

	/**
	 * 從 XML 檔案載入所有 Boss 週期設定
	 * <h3>載入流程:</h3>
	 * <ol>
	 *   <li>載入預設設定檔: {@code ./data/xml/Cycle/BossCycle.xml}</li>
	 *   <li>若存在自訂設定檔 {@code ./data/xml/Cycle/users/BossCycle.xml}，則覆蓋預設設定</li>
	 *   <li>初始化所有週期並註冊到 {@code _cycleMap}</li>
	 *   <li>載入 Boss 出生點資料表</li>
	 * </ol>
	 * <p>若載入失敗，會記錄錯誤並終止伺服器。
	 *
	 * @see L1BossCycleList
	 * @see BossSpawnTable#fillSpawnTable()
	 */
	public static void load() {
		PerformanceTimer timer = new PerformanceTimer();
		_log.info("載入 Boss 週期資料...");
		try {
			// 建立 JAXB 上下文以綁定 L1BossCycleList 類別
			JAXBContext context = JAXBContext.newInstance(L1BossCycle.L1BossCycleList.class);

			// 建立 Unmarshaller 進行 XML -> POJO 轉換
			Unmarshaller um = context.createUnmarshaller();

			// 載入預設 Boss 週期設定
			File file = new File("./data/xml/Cycle/BossCycle.xml");
			L1BossCycleList bossList = (L1BossCycleList) um.unmarshal(file);

			for (L1BossCycle cycle : bossList.getBossCycles()) {
				cycle.init();
				_cycleMap.put(cycle.getName(), cycle);
			}

			// 若存在使用者自訂設定，則覆蓋預設值
			File userFile = new File("./data/xml/Cycle/users/BossCycle.xml");
			if (userFile.exists()) {
				bossList = (L1BossCycleList) um.unmarshal(userFile);

				for (L1BossCycle cycle : bossList.getBossCycles()) {
					cycle.init();
					_cycleMap.put(cycle.getName(), cycle);
				}
			}
			// 從 spawnlist_boss 資料表載入並配置 Boss 出生點
			BossSpawnTable.fillSpawnTable();
		}
		catch (Exception e) {
			_log.log(Level.SEVERE, "Boss 週期資料載入失敗", e);
			System.exit(0);
		}
		_log.info("Boss 週期資料載入完成, 耗時 " + timer.get() + " ms");
	}

	/**
	 * 在主控台輸出 Boss 週期資訊
	 * <p>顯示週期名稱及指定時間的出現期間 (開始時間 - 結束時間)。
	 *
	 * @param now 用於計算週期資訊的時間
	 */
	public void showData(Calendar now) {
		_log.info("[Boss週期] " + getName() + " 出現期間: "
				+ _sdf.format(getSpawnStartTime(now).getTime()) + " - "
				+ _sdf.format(getSpawnEndTime(now).getTime()));
	}

	/** Boss 週期名稱對應表 (週期名稱 -> L1BossCycle 物件) */
	private static Map<String, L1BossCycle> _cycleMap = Maps.newMap();

	/**
	 * 根據週期名稱取得 Boss 週期物件
	 *
	 * @param type 週期名稱 (對應 XML 中的 name 屬性)
	 * @return 對應的 Boss 週期物件，若不存在則返回 {@code null}
	 */
	public static L1BossCycle getBossCycle(String type) {
		return _cycleMap.get(type);
	}

	/**
	 * 取得週期名稱
	 * @return 週期名稱
	 */
	public String getName() {
		return _name;
	}

	/**
	 * 設定週期名稱
	 * @param name 週期名稱
	 */
	public void setName(String name) {
		_name = name;
	}

	/**
	 * 取得基準時間設定
	 * @return 基準時間物件
	 * @see Base
	 */
	public Base getBase() {
		return _base;
	}

	/**
	 * 設定基準時間
	 * @param base 基準時間物件
	 */
	public void setBase(Base base) {
		_base = base;
	}

	/**
	 * 取得週期設定
	 * @return 週期物件
	 * @see Cycle
	 */
	public Cycle getCycle() {
		return _cycle;
	}

	/**
	 * 設定週期
	 * @param cycle 週期物件
	 */
	public void setCycle(Cycle cycle) {
		_cycle = cycle;
	}

	/** 日誌記錄器 */
	private static Logger _log = Logger.getLogger(L1BossCycle.class.getName());
}
