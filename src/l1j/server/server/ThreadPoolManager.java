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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import l1j.server.Config;

// import l1j.server.server.network.L2GameClient;

/**
 * 執行緒池管理器
 * <p>統一管理伺服器所有執行緒池，提供排程與非同步執行功能。
 *
 * <h3>執行緒池類型:</h3>
 * <ul>
 *   <li><b>Effects Pool:</b> 處理遊戲效果 (Buff、Debuff 等) 的排程執行緒池</li>
 *   <li><b>General Pool:</b> 處理一般任務的排程執行緒池</li>
 *   <li><b>AI Pool:</b> 處理 NPC AI 邏輯的執行緒池</li>
 *   <li><b>General Packets Pool:</b> 處理一般封包的執行緒池</li>
 *   <li><b>I/O Packets Pool:</b> 處理 I/O 封包的高優先權執行緒池</li>
 *   <li><b>General Tasks Pool:</b> 處理一般背景任務的執行緒池</li>
 * </ul>
 *
 * <h3>執行緒池配置:</h3>
 * <table border="1">
 *   <tr>
 *     <th>執行緒池</th>
 *     <th>核心執行緒數</th>
 *     <th>最大執行緒數</th>
 *     <th>優先權</th>
 *     <th>用途</th>
 *   </tr>
 *   <tr>
 *     <td>Effects Scheduled</td>
 *     <td>Config.THREAD_P_EFFECTS</td>
 *     <td>Config.THREAD_P_EFFECTS</td>
 *     <td>MIN_PRIORITY</td>
 *     <td>技能效果、狀態效果排程</td>
 *   </tr>
 *   <tr>
 *     <td>General Scheduled</td>
 *     <td>Config.THREAD_P_GENERAL</td>
 *     <td>Config.THREAD_P_GENERAL</td>
 *     <td>NORM_PRIORITY</td>
 *     <td>一般排程任務</td>
 *   </tr>
 *   <tr>
 *     <td>I/O Packets</td>
 *     <td>2</td>
 *     <td>Integer.MAX_VALUE</td>
 *     <td>NORM_PRIORITY + 1</td>
 *     <td>高優先權封包處理</td>
 *   </tr>
 *   <tr>
 *     <td>General Packets</td>
 *     <td>4</td>
 *     <td>6</td>
 *     <td>NORM_PRIORITY + 1</td>
 *     <td>一般封包處理</td>
 *   </tr>
 *   <tr>
 *     <td>General Tasks</td>
 *     <td>2</td>
 *     <td>4</td>
 *     <td>NORM_PRIORITY</td>
 *     <td>一般背景任務</td>
 *   </tr>
 *   <tr>
 *     <td>AI</td>
 *     <td>1</td>
 *     <td>Config.AI_MAX_THREAD</td>
 *     <td>NORM_PRIORITY</td>
 *     <td>NPC AI 執行</td>
 *   </tr>
 *   <tr>
 *     <td>AI Scheduled</td>
 *     <td>Config.AI_MAX_THREAD</td>
 *     <td>Config.AI_MAX_THREAD</td>
 *     <td>NORM_PRIORITY</td>
 *     <td>NPC AI 排程</td>
 *   </tr>
 * </table>
 *
 * <h3>排程方法:</h3>
 * <ul>
 *   <li>{@link #scheduleEffect(Runnable, long)} - 延遲執行效果任務</li>
 *   <li>{@link #scheduleEffectAtFixedRate(Runnable, long, long)} - 固定頻率執行效果任務</li>
 *   <li>{@link #scheduleGeneral(Runnable, long)} - 延遲執行一般任務</li>
 *   <li>{@link #scheduleGeneralAtFixedRate(Runnable, long, long)} - 固定頻率執行一般任務</li>
 *   <li>{@link #scheduleAi(Runnable, long)} - 延遲執行 AI 任務</li>
 *   <li>{@link #scheduleAiAtFixedRate(Runnable, long, long)} - 固定頻率執行 AI 任務</li>
 * </ul>
 *
 * <h3>立即執行方法:</h3>
 * <ul>
 *   <li>{@link #executeTask(Runnable)} - 執行一般背景任務</li>
 *   <li>{@link #executeAi(Runnable)} - 執行 AI 任務</li>
 * </ul>
 *
 * <h3>管理功能:</h3>
 * <ul>
 *   <li>{@link #getStats()} - 取得所有執行緒池統計資訊</li>
 *   <li>{@link #shutdown()} - 關閉所有執行緒池</li>
 *   <li>{@link #purge()} - 清理已取消的排程任務</li>
 *   <li>{@link #getPacketStats()} - 取得一般封包執行緒池狀態</li>
 *   <li>{@link #getIOPacketStats()} - 取得 I/O 封包執行緒池狀態</li>
 *   <li>{@link #getGeneralStats()} - 取得一般任務執行緒池狀態</li>
 * </ul>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * // 延遲 1000 毫秒後執行效果
 * ThreadPoolManager.getInstance().scheduleEffect(() -> {
 *     // 效果邏輯
 * }, 1000);
 *
 * // 每 5000 毫秒執行一次 AI 更新
 * ThreadPoolManager.getInstance().scheduleAiAtFixedRate(() -> {
 *     // AI 邏輯
 * }, 0, 5000);
 * </pre>
 *
 * @see ScheduledThreadPoolExecutor
 * @see ThreadPoolExecutor
 * @see Config#THREAD_P_EFFECTS
 * @see Config#THREAD_P_GENERAL
 * @see Config#AI_MAX_THREAD
 */
public class ThreadPoolManager {

	/** 日誌記錄器 */
	private static Logger _log = Logger.getLogger(ThreadPoolManager.class
			.getName());

	/** Singleton 實例 */
	private static ThreadPoolManager _instance;

	/** 效果排程執行緒池 (低優先權) */
	private final ScheduledThreadPoolExecutor _effectsScheduledThreadPool;

	/** 一般排程執行緒池 (標準優先權) */
	private final ScheduledThreadPoolExecutor _generalScheduledThreadPool;

	/** 一般封包執行緒池 (高優先權) */
	private final ThreadPoolExecutor _generalPacketsThreadPool;

	/** I/O 封包執行緒池 (高優先權) */
	private final ThreadPoolExecutor _ioPacketsThreadPool;

	/** AI 執行緒池 (標準優先權) */
	private final ThreadPoolExecutor _aiThreadPool;

	/** 一般任務執行緒池 (標準優先權) */
	private final ThreadPoolExecutor _generalThreadPool;

	/** AI 排程執行緒池 (標準優先權) */
	private final ScheduledThreadPoolExecutor _aiScheduledThreadPool;

	/** 關閉狀態旗標 */
	private boolean _shutdown;

	/**
	 * 取得 Singleton 實例
	 * <p>使用延遲初始化模式，首次呼叫時建立實例。
	 *
	 * @return ThreadPoolManager 實例
	 */
	public static ThreadPoolManager getInstance() {
		if (_instance == null) {
			_instance = new ThreadPoolManager();
		}
		return _instance;
	}

	/**
	 * 私有建構子，初始化所有執行緒池
	 * <p>建立並配置 7 個不同用途的執行緒池。
	 *
	 * <h3>執行緒池初始化:</h3>
	 * <ol>
	 *   <li><b>Effects Scheduled Pool:</b> 低優先權，用於處理遊戲效果</li>
	 *   <li><b>General Scheduled Pool:</b> 標準優先權，用於一般排程任務</li>
	 *   <li><b>I/O Packets Pool:</b> 高優先權 (NORM+1)，彈性執行緒數 (2 ~ MAX)</li>
	 *   <li><b>General Packets Pool:</b> 高優先權 (NORM+1)，執行緒數 4~6</li>
	 *   <li><b>General Tasks Pool:</b> 標準優先權，執行緒數 2~4</li>
	 *   <li><b>AI Pool:</b> 標準優先權，執行緒數 1 ~ AI_MAX_THREAD</li>
	 *   <li><b>AI Scheduled Pool:</b> 標準優先權，用於 AI 排程任務</li>
	 * </ol>
	 *
	 * @see PriorityThreadFactory
	 * @see Config#THREAD_P_EFFECTS
	 * @see Config#THREAD_P_GENERAL
	 * @see Config#AI_MAX_THREAD
	 */
	private ThreadPoolManager() {
		_effectsScheduledThreadPool = new ScheduledThreadPoolExecutor(
				Config.THREAD_P_EFFECTS, new PriorityThreadFactory(
						"EffectsSTPool", Thread.MIN_PRIORITY));
		_generalScheduledThreadPool = new ScheduledThreadPoolExecutor(
				Config.THREAD_P_GENERAL, new PriorityThreadFactory(
						"GerenalSTPool", Thread.NORM_PRIORITY));

		_ioPacketsThreadPool = new ThreadPoolExecutor(2, Integer.MAX_VALUE, 5L,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
				new PriorityThreadFactory("I/O Packet Pool",
						Thread.NORM_PRIORITY + 1));

		_generalPacketsThreadPool = new ThreadPoolExecutor(4, 6, 15L,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
				new PriorityThreadFactory("Normal Packet Pool",
						Thread.NORM_PRIORITY + 1));

		_generalThreadPool = new ThreadPoolExecutor(2, 4, 5L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory(
						"General Pool", Thread.NORM_PRIORITY));

		// will be really used in the next AI implementation.
		_aiThreadPool = new ThreadPoolExecutor(1, Config.AI_MAX_THREAD, 10L,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

		_aiScheduledThreadPool = new ScheduledThreadPoolExecutor(
				Config.AI_MAX_THREAD, new PriorityThreadFactory("AISTPool",
						Thread.NORM_PRIORITY));
	}

	/**
	 * 排程延遲執行效果任務
	 * <p>在指定延遲後執行一次任務 (如 Buff 效果結束)。
	 *
	 * @param r 要執行的任務
	 * @param delay 延遲時間 (毫秒)，負數會自動轉為 0
	 * @return ScheduledFuture 物件，可用於取消任務；若執行緒池已關閉則返回 null
	 * @see ScheduledThreadPoolExecutor#schedule(Runnable, long, TimeUnit)
	 */
	public ScheduledFuture<?> scheduleEffect(Runnable r, long delay) {
		try {
			if (delay < 0) {
				delay = 0;
			}
			return _effectsScheduledThreadPool.schedule(r, delay,
					TimeUnit.MILLISECONDS);
		} catch (RejectedExecutionException e) {
			return null; /* shutdown, ignore */
		}
	}

	/**
	 * 排程固定頻率執行效果任務
	 * <p>以固定頻率重複執行任務 (如持續性 Buff 效果)。
	 *
	 * @param r 要執行的任務
	 * @param initial 初始延遲時間 (毫秒)，負數會自動轉為 0
	 * @param delay 執行間隔時間 (毫秒)，負數會自動轉為 0
	 * @return ScheduledFuture 物件，可用於取消任務；若執行緒池已關閉則返回 null
	 * @see ScheduledThreadPoolExecutor#scheduleAtFixedRate(Runnable, long, long, TimeUnit)
	 */
	public ScheduledFuture<?> scheduleEffectAtFixedRate(Runnable r,
			long initial, long delay) {
		try {
			if (delay < 0) {
				delay = 0;
			}
			if (initial < 0) {
				initial = 0;
			}
			return _effectsScheduledThreadPool.scheduleAtFixedRate(r, initial,
					delay, TimeUnit.MILLISECONDS);
		} catch (RejectedExecutionException e) {
			return null; /* shutdown, ignore */
		}
	}

	/**
	 * 排程延遲執行一般任務
	 * <p>在指定延遲後執行一次任務 (如延遲傳送、延遲復活等)。
	 *
	 * @param r 要執行的任務
	 * @param delay 延遲時間 (毫秒)，負數會自動轉為 0
	 * @return ScheduledFuture 物件，可用於取消任務；若執行緒池已關閉則返回 null
	 * @see ScheduledThreadPoolExecutor#schedule(Runnable, long, TimeUnit)
	 */
	public ScheduledFuture<?> scheduleGeneral(Runnable r, long delay) {
		try {
			if (delay < 0) {
				delay = 0;
			}
			return _generalScheduledThreadPool.schedule(r, delay,
					TimeUnit.MILLISECONDS);
		} catch (RejectedExecutionException e) {
			return null; /* shutdown, ignore */
		}
	}

	/**
	 * 排程固定頻率執行一般任務
	 * <p>以固定頻率重複執行任務 (如自動儲存、定時廣播等)。
	 *
	 * @param r 要執行的任務
	 * @param initial 初始延遲時間 (毫秒)，負數會自動轉為 0
	 * @param delay 執行間隔時間 (毫秒)，負數會自動轉為 0
	 * @return ScheduledFuture 物件，可用於取消任務；若執行緒池已關閉則返回 null
	 * @see ScheduledThreadPoolExecutor#scheduleAtFixedRate(Runnable, long, long, TimeUnit)
	 */
	public ScheduledFuture<?> scheduleGeneralAtFixedRate(Runnable r,
			long initial, long delay) {
		try {
			if (delay < 0) {
				delay = 0;
			}
			if (initial < 0) {
				initial = 0;
			}
			return _generalScheduledThreadPool.scheduleAtFixedRate(r, initial,
					delay, TimeUnit.MILLISECONDS);
		} catch (RejectedExecutionException e) {
			return null; /* shutdown, ignore */
		}
	}

	/**
	 * 排程延遲執行 AI 任務
	 * <p>在指定延遲後執行一次 AI 任務 (如 NPC 延遲反應)。
	 *
	 * @param r 要執行的 AI 任務
	 * @param delay 延遲時間 (毫秒)，負數會自動轉為 0
	 * @return ScheduledFuture 物件，可用於取消任務；若執行緒池已關閉則返回 null
	 * @see ScheduledThreadPoolExecutor#schedule(Runnable, long, TimeUnit)
	 */
	public ScheduledFuture<?> scheduleAi(Runnable r, long delay) {
		try {
			if (delay < 0) {
				delay = 0;
			}
			return _aiScheduledThreadPool.schedule(r, delay,
					TimeUnit.MILLISECONDS);
		} catch (RejectedExecutionException e) {
			return null; /* shutdown, ignore */
		}
	}

	/**
	 * 排程固定頻率執行 AI 任務
	 * <p>以固定頻率重複執行 AI 任務 (如 NPC 巡邏、定時攻擊等)。
	 *
	 * @param r 要執行的 AI 任務
	 * @param initial 初始延遲時間 (毫秒)，負數會自動轉為 0
	 * @param delay 執行間隔時間 (毫秒)，負數會自動轉為 0
	 * @return ScheduledFuture 物件，可用於取消任務；若執行緒池已關閉則返回 null
	 * @see ScheduledThreadPoolExecutor#scheduleAtFixedRate(Runnable, long, long, TimeUnit)
	 */
	public ScheduledFuture<?> scheduleAiAtFixedRate(Runnable r, long initial,
			long delay) {
		try {
			if (delay < 0) {
				delay = 0;
			}
			if (initial < 0) {
				initial = 0;
			}
			return _aiScheduledThreadPool.scheduleAtFixedRate(r, initial,
					delay, TimeUnit.MILLISECONDS);
		} catch (RejectedExecutionException e) {
			return null; /* shutdown, ignore */
		}
	}

	/*
	 * public void executePacket(ReceivablePacket<L2GameClient> pkt) {
	 * _generalPacketsThreadPool.execute(pkt); }
	 *
	 * public void executeIOPacket(ReceivablePacket<L2GameClient> pkt) {
	 * _ioPacketsThreadPool.execute(pkt); }
	 */

	/**
	 * 執行一般背景任務
	 * <p>在一般任務執行緒池中立即執行任務 (不延遲)。
	 *
	 * @param r 要執行的任務
	 * @see ThreadPoolExecutor#execute(Runnable)
	 */
	public void executeTask(Runnable r) {
		_generalThreadPool.execute(r);
	}

	/**
	 * 執行 AI 任務
	 * <p>在 AI 執行緒池中立即執行任務 (不延遲)。
	 *
	 * @param r 要執行的 AI 任務
	 * @see ThreadPoolExecutor#execute(Runnable)
	 */
	public void executeAi(Runnable r) {
		_aiThreadPool.execute(r);
	}

	/**
	 * 取得所有執行緒池的統計資訊
	 * <p>回傳包含所有執行緒池詳細統計資訊的字串陣列。
	 *
	 * <h3>統計資訊包含:</h3>
	 * <ul>
	 *   <li><b>ActiveThreads:</b> 目前活躍執行緒數</li>
	 *   <li><b>CorePoolSize:</b> 核心執行緒池大小</li>
	 *   <li><b>PoolSize:</b> 目前執行緒池大小</li>
	 *   <li><b>MaximumPoolSize:</b> 最大執行緒池大小</li>
	 *   <li><b>LargestPoolSize:</b> 歷史最大執行緒數</li>
	 *   <li><b>CompletedTasks:</b> 已完成任務數</li>
	 *   <li><b>ScheduledTasks:</b> 排程中任務數</li>
	 *   <li><b>QueuedTasks:</b> 佇列中等待執行的任務數</li>
	 * </ul>
	 *
	 * <h3>包含的執行緒池:</h3>
	 * <ul>
	 *   <li>Effects Scheduled Thread Pool</li>
	 *   <li>General Scheduled Thread Pool</li>
	 *   <li>AI Scheduled Thread Pool</li>
	 *   <li>General Packets Thread Pool</li>
	 *   <li>I/O Packets Thread Pool</li>
	 *   <li>General Tasks Thread Pool</li>
	 * </ul>
	 *
	 * @return 統計資訊字串陣列
	 */
	public String[] getStats() {
		return new String[] {
				"STP:",
				" + Effects:",
				" |- ActiveThreads:   "
						+ _effectsScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: "
						+ _effectsScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        "
						+ _effectsScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: "
						+ _effectsScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  "
						+ _effectsScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  "
						+ (_effectsScheduledThreadPool.getTaskCount() - _effectsScheduledThreadPool
								.getCompletedTaskCount()),
				" | -------",
				" + General:",
				" |- ActiveThreads:   "
						+ _generalScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: "
						+ _generalScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        "
						+ _generalScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: "
						+ _generalScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  "
						+ _generalScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  "
						+ (_generalScheduledThreadPool.getTaskCount() - _generalScheduledThreadPool
								.getCompletedTaskCount()),
				" | -------",
				" + AI:",
				" |- ActiveThreads:   "
						+ _aiScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: "
						+ _aiScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        " + _aiScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: "
						+ _aiScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  "
						+ _aiScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  "
						+ (_aiScheduledThreadPool.getTaskCount() - _aiScheduledThreadPool
								.getCompletedTaskCount()),
				"TP:",
				" + Packets:",
				" |- ActiveThreads:   "
						+ _generalPacketsThreadPool.getActiveCount(),
				" |- getCorePoolSize: "
						+ _generalPacketsThreadPool.getCorePoolSize(),
				" |- MaximumPoolSize: "
						+ _generalPacketsThreadPool.getMaximumPoolSize(),
				" |- LargestPoolSize: "
						+ _generalPacketsThreadPool.getLargestPoolSize(),
				" |- PoolSize:        "
						+ _generalPacketsThreadPool.getPoolSize(),
				" |- CompletedTasks:  "
						+ _generalPacketsThreadPool.getCompletedTaskCount(),
				" |- QueuedTasks:     "
						+ _generalPacketsThreadPool.getQueue().size(),
				" | -------",
				" + I/O Packets:",
				" |- ActiveThreads:   " + _ioPacketsThreadPool.getActiveCount(),
				" |- getCorePoolSize: "
						+ _ioPacketsThreadPool.getCorePoolSize(),
				" |- MaximumPoolSize: "
						+ _ioPacketsThreadPool.getMaximumPoolSize(),
				" |- LargestPoolSize: "
						+ _ioPacketsThreadPool.getLargestPoolSize(),
				" |- PoolSize:        " + _ioPacketsThreadPool.getPoolSize(),
				" |- CompletedTasks:  "
						+ _ioPacketsThreadPool.getCompletedTaskCount(),
				" |- QueuedTasks:     "
						+ _ioPacketsThreadPool.getQueue().size(),
				" | -------",
				" + General Tasks:",
				" |- ActiveThreads:   " + _generalThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _generalThreadPool.getCorePoolSize(),
				" |- MaximumPoolSize: "
						+ _generalThreadPool.getMaximumPoolSize(),
				" |- LargestPoolSize: "
						+ _generalThreadPool.getLargestPoolSize(),
				" |- PoolSize:        " + _generalThreadPool.getPoolSize(),
				" |- CompletedTasks:  "
						+ _generalThreadPool.getCompletedTaskCount(),
				" |- QueuedTasks:     " + _generalThreadPool.getQueue().size(),
				" | -------", " + AI:", " |- Not Done" };
	}

	/**
	 * 優先權執行緒工廠
	 * <p>建立具有指定優先權與名稱的執行緒。
	 *
	 * <h3>主要功能:</h3>
	 * <ul>
	 *   <li>為執行緒設定自訂優先權</li>
	 *   <li>為執行緒設定有意義的名稱 (包含遞增編號)</li>
	 *   <li>將執行緒歸入同一個 ThreadGroup 便於管理</li>
	 *   <li>自動產生遞增的執行緒編號</li>
	 * </ul>
	 *
	 * <h3>執行緒命名規則:</h3>
	 * <pre>
	 * {name}-{number}
	 * 例如: EffectsSTPool-1, EffectsSTPool-2, ...
	 * </pre>
	 *
	 * @see ThreadFactory
	 * @see ThreadGroup
	 */
	private class PriorityThreadFactory implements ThreadFactory {
		/** 執行緒優先權 */
		private final int _prio;

		/** 執行緒池名稱 */
		private final String _name;

		/** 執行緒編號計數器 (原子遞增) */
		private final AtomicInteger _threadNumber = new AtomicInteger(1);

		/** 執行緒群組 */
		private final ThreadGroup _group;

		/**
		 * 建構優先權執行緒工廠
		 *
		 * @param name 執行緒池名稱
		 * @param prio 執行緒優先權 (Thread.MIN_PRIORITY ~ Thread.MAX_PRIORITY)
		 */
		public PriorityThreadFactory(String name, int prio) {
			_prio = prio;
			_name = name;
			_group = new ThreadGroup(_name);
		}

		/**
		 * 建立新執行緒
		 * <p>建立具有指定優先權與命名規則的執行緒。
		 *
		 * <h3>執行緒屬性:</h3>
		 * <ul>
		 *   <li><b>名稱:</b> {name}-{number} (number 自動遞增)</li>
		 *   <li><b>優先權:</b> 建構時指定的優先權</li>
		 *   <li><b>群組:</b> 歸屬於同一個 ThreadGroup</li>
		 * </ul>
		 *
		 * @param r 要執行的任務
		 * @return 新建立的執行緒
		 */
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(_group, r);
			t.setName(_name + "-" + _threadNumber.getAndIncrement());
			t.setPriority(_prio);
			return t;
		}

		/**
		 * 取得執行緒群組
		 *
		 * @return 此工廠建立的所有執行緒所屬的 ThreadGroup
		 */
		public ThreadGroup getGroup() {
			return _group;
		}
	}

	/**
	 * 關閉所有執行緒池
	 * <p>優雅地關閉所有執行緒池，等待執行中任務完成。
	 *
	 * <h3>關閉流程:</h3>
	 * <ol>
	 *   <li>設定關閉旗標 (_shutdown = true)</li>
	 *   <li>等待所有執行緒池完成目前任務 (最多等待 1 秒)</li>
	 *   <li>呼叫所有執行緒池的 shutdown() 方法</li>
	 *   <li>輸出關閉完成訊息</li>
	 * </ol>
	 *
	 * <h3>關閉的執行緒池:</h3>
	 * <ul>
	 *   <li>Effects Scheduled Thread Pool</li>
	 *   <li>General Scheduled Thread Pool</li>
	 *   <li>General Packets Thread Pool</li>
	 *   <li>I/O Packets Thread Pool</li>
	 *   <li>General Tasks Thread Pool</li>
	 *   <li>AI Thread Pool</li>
	 * </ul>
	 *
	 * <p><b>注意:</b> 關閉後無法重新啟動，需重新建立 ThreadPoolManager 實例。
	 *
	 * @see ThreadPoolExecutor#shutdown()
	 * @see ThreadPoolExecutor#awaitTermination(long, TimeUnit)
	 */
	public void shutdown() {
		_shutdown = true;
		try {
			_effectsScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_generalScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_generalPacketsThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_ioPacketsThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_generalThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_aiThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_effectsScheduledThreadPool.shutdown();
			_generalScheduledThreadPool.shutdown();
			_generalPacketsThreadPool.shutdown();
			_ioPacketsThreadPool.shutdown();
			_generalThreadPool.shutdown();
			_aiThreadPool.shutdown();
			_log.info("所有執行緒池已停止");

		} catch (InterruptedException e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);

		}
	}

	/**
	 * 檢查執行緒池是否已關閉
	 *
	 * @return true 表示已關閉，false 表示正在運行
	 */
	public boolean isShutdown() {
		return _shutdown;
	}

	/**
	 * 清理所有執行緒池中已取消的任務
	 * <p>移除所有已被取消但尚未執行的排程任務，釋放記憶體。
	 *
	 * <h3>清理範圍:</h3>
	 * <ul>
	 *   <li>Effects Scheduled Thread Pool</li>
	 *   <li>General Scheduled Thread Pool</li>
	 *   <li>AI Scheduled Thread Pool</li>
	 *   <li>I/O Packets Thread Pool</li>
	 *   <li>General Packets Thread Pool</li>
	 *   <li>General Tasks Thread Pool</li>
	 *   <li>AI Thread Pool</li>
	 * </ul>
	 *
	 * <p><b>用途:</b> 定期清理可避免記憶體洩漏，特別是在頻繁建立與取消任務的情況下。
	 *
	 * @see ThreadPoolExecutor#purge()
	 */
	public void purge() {
		_effectsScheduledThreadPool.purge();
		_generalScheduledThreadPool.purge();
		_aiScheduledThreadPool.purge();
		_ioPacketsThreadPool.purge();
		_generalPacketsThreadPool.purge();
		_generalThreadPool.purge();
		_aiThreadPool.purge();
	}

	/**
	 * 取得一般封包執行緒池的詳細狀態
	 * <p>包含佇列中任務數量及所有執行緒的堆疊追蹤資訊。
	 *
	 * <h3>回傳資訊:</h3>
	 * <ul>
	 *   <li>佇列中等待處理的任務數量</li>
	 *   <li>執行緒群組中的執行緒數量</li>
	 *   <li>每個執行緒的名稱</li>
	 *   <li>每個執行緒的完整堆疊追蹤 (用於除錯)</li>
	 * </ul>
	 *
	 * <p><b>用途:</b> 除錯封包處理效能問題或死鎖狀況。
	 *
	 * @return 格式化的統計資訊字串
	 * @see Thread#getStackTrace()
	 */
	public String getPacketStats() {
		TextBuilder tb = new TextBuilder();
		ThreadFactory tf = _generalPacketsThreadPool.getThreadFactory();
		if (tf instanceof PriorityThreadFactory) {
			tb.append("General Packet Thread Pool:\r\n");
			tb.append("Tasks in the queue: "
					+ _generalPacketsThreadPool.getQueue().size() + "\r\n");
			tb.append("Showing threads stack trace:\r\n");
			PriorityThreadFactory ptf = (PriorityThreadFactory) tf;
			int count = ptf.getGroup().activeCount();
			Thread[] threads = new Thread[count + 2];
			ptf.getGroup().enumerate(threads);
			tb.append("There should be " + count + " Threads\r\n");
			for (Thread t : threads) {
				if (t == null) {
					continue;
				}
				tb.append(t.getName() + "\r\n");
				for (StackTraceElement ste : t.getStackTrace()) {
					tb.append(ste.toString());
					tb.append("\r\n");
				}
			}
		}
		tb.append("Packet Tp stack traces printed.\r\n");
		return tb.toString();
	}

	/**
	 * 取得 I/O 封包執行緒池的詳細狀態
	 * <p>包含佇列中任務數量及所有執行緒的堆疊追蹤資訊。
	 *
	 * <h3>回傳資訊:</h3>
	 * <ul>
	 *   <li>佇列中等待處理的任務數量</li>
	 *   <li>執行緒群組中的執行緒數量</li>
	 *   <li>每個執行緒的名稱</li>
	 *   <li>每個執行緒的完整堆疊追蹤 (用於除錯)</li>
	 * </ul>
	 *
	 * <p><b>用途:</b> 除錯高優先權 I/O 封包處理問題。
	 *
	 * @return 格式化的統計資訊字串
	 * @see Thread#getStackTrace()
	 */
	public String getIOPacketStats() {
		TextBuilder tb = new TextBuilder();
		ThreadFactory tf = _ioPacketsThreadPool.getThreadFactory();
		if (tf instanceof PriorityThreadFactory) {
			tb.append("I/O Packet Thread Pool:\r\n");
			tb.append("Tasks in the queue: "
					+ _ioPacketsThreadPool.getQueue().size() + "\r\n");
			tb.append("Showing threads stack trace:\r\n");
			PriorityThreadFactory ptf = (PriorityThreadFactory) tf;
			int count = ptf.getGroup().activeCount();
			Thread[] threads = new Thread[count + 2];
			ptf.getGroup().enumerate(threads);
			tb.append("There should be " + count + " Threads\r\n");
			for (Thread t : threads) {
				if (t == null) {
					continue;
				}
				tb.append(t.getName() + "\r\n");
				for (StackTraceElement ste : t.getStackTrace()) {
					tb.append(ste.toString());
					tb.append("\r\n");
				}
			}
		}
		tb.append("Packet Tp stack traces printed.\r\n");
		return tb.toString();
	}

	/**
	 * 取得一般任務執行緒池的詳細狀態
	 * <p>包含佇列中任務數量及所有執行緒的堆疊追蹤資訊。
	 *
	 * <h3>回傳資訊:</h3>
	 * <ul>
	 *   <li>佇列中等待處理的任務數量</li>
	 *   <li>執行緒群組中的執行緒數量</li>
	 *   <li>每個執行緒的名稱</li>
	 *   <li>每個執行緒的完整堆疊追蹤 (用於除錯)</li>
	 * </ul>
	 *
	 * <p><b>用途:</b> 除錯一般背景任務處理問題。
	 *
	 * @return 格式化的統計資訊字串
	 * @see Thread#getStackTrace()
	 */
	public String getGeneralStats() {
		TextBuilder tb = new TextBuilder();
		ThreadFactory tf = _generalThreadPool.getThreadFactory();
		if (tf instanceof PriorityThreadFactory) {
			tb.append("General Thread Pool:\r\n");
			tb.append("Tasks in the queue: "
					+ _generalThreadPool.getQueue().size() + "\r\n");
			tb.append("Showing threads stack trace:\r\n");
			PriorityThreadFactory ptf = (PriorityThreadFactory) tf;
			int count = ptf.getGroup().activeCount();
			Thread[] threads = new Thread[count + 2];
			ptf.getGroup().enumerate(threads);
			tb.append("There should be " + count + " Threads\r\n");
			for (Thread t : threads) {
				if (t == null) {
					continue;
				}
				tb.append(t.getName() + "\r\n");
				for (StackTraceElement ste : t.getStackTrace()) {
					tb.append(ste.toString());
					tb.append("\r\n");
				}
			}
		}
		tb.append("Packet Tp stack traces printed.\r\n");
		return tb.toString();
	}
}