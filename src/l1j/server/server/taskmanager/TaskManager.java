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
package l1j.server.server.taskmanager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.taskmanager.tasks.TaskRestart;
import l1j.server.server.taskmanager.tasks.TaskShutdown;
import l1j.server.server.utils.SQLUtil;
import l1j.server.server.utils.collections.Lists;
import l1j.server.server.utils.collections.Maps;

/**
 * 全域任務管理器
 * <p>管理伺服器的全域排程任務，支援從資料庫載入與持久化任務狀態。
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>註冊與管理伺服器級別的排程任務</li>
 *   <li>從資料庫載入任務設定與執行狀態</li>
 *   <li>追蹤任務最後執行時間並持久化到資料庫</li>
 *   <li>支援新增唯一任務 (不重複) 與一般任務</li>
 *   <li>管理任務的生命週期 (啟動、停止、銷毀)</li>
 * </ul>
 *
 * <h3>內建任務:</h3>
 * <ul>
 *   <li>{@link TaskRestart} - 伺服器自動重啟任務</li>
 *   <li>{@link TaskShutdown} - 伺服器自動關機任務</li>
 * </ul>
 *
 * <h3>資料庫結構 (global_tasks 表):</h3>
 * <table border="1">
 *   <tr>
 *     <th>欄位</th>
 *     <th>類型</th>
 *     <th>說明</th>
 *   </tr>
 *   <tr>
 *     <td>id</td>
 *     <td>INT</td>
 *     <td>任務唯一識別碼</td>
 *   </tr>
 *   <tr>
 *     <td>task</td>
 *     <td>VARCHAR</td>
 *     <td>任務名稱</td>
 *   </tr>
 *   <tr>
 *     <td>type</td>
 *     <td>VARCHAR</td>
 *     <td>任務類型 (見 {@link TaskTypes})</td>
 *   </tr>
 *   <tr>
 *     <td>last_activation</td>
 *     <td>BIGINT</td>
 *     <td>最後執行時間 (毫秒時間戳)</td>
 *   </tr>
 *   <tr>
 *     <td>param1, param2, param3</td>
 *     <td>VARCHAR</td>
 *     <td>任務參數 (3 個自訂參數)</td>
 *   </tr>
 * </table>
 *
 * <h3>任務類型:</h3>
 * <ul>
 *   <li><b>TYPE_STARTUP:</b> 伺服器啟動時執行一次</li>
 *   <li><b>TYPE_SHEDULED:</b> 排程任務 (定時重複執行)</li>
 *   <li><b>TYPE_FIXED_SHEDULED:</b> 固定排程任務</li>
 *   <li><b>TYPE_TIME:</b> 指定時間執行</li>
 *   <li><b>TYPE_SPECIAL:</b> 特殊任務</li>
 *   <li><b>TYPE_GLOBAL_TASK:</b> 全域任務</li>
 * </ul>
 *
 * <h3>使用範例:</h3>
 * <pre>
 * // 註冊自訂任務
 * TaskManager.getInstance().registerTask(new MyCustomTask());
 *
 * // 新增唯一任務到資料庫
 * TaskManager.addUniqueTask("restart", TaskTypes.TYPE_SHEDULED,
 *                           "03:00:00", "", "");
 *
 * // 新增一般任務到資料庫
 * TaskManager.addTask("backup", TaskTypes.TYPE_SHEDULED,
 *                     "30 * * * *", "", "");
 * </pre>
 *
 * @author Layane
 * @see Task
 * @see TaskTypes
 * @see ExecutedTask
 * @see TaskRestart
 * @see TaskShutdown
 */
public final class TaskManager {
	/** 日誌記錄器 */
	protected static final Logger _log = Logger.getLogger(TaskManager.class.getName());

	/** Singleton 實例 */
	private static TaskManager _instance;

	/**
	 * SQL 語句陣列
	 * <ul>
	 *   <li>[0] SELECT - 查詢所有任務</li>
	 *   <li>[1] UPDATE - 更新最後執行時間</li>
	 *   <li>[2] SELECT - 檢查任務是否存在</li>
	 *   <li>[3] INSERT - 插入新任務</li>
	 * </ul>
	 */
	protected static final String[] SQL_STATEMENTS =
	{ "SELECT id,task,type,last_activation,param1,param2,param3 FROM global_tasks", "UPDATE global_tasks SET last_activation=? WHERE id=?",
			"SELECT id FROM global_tasks WHERE task=?",
			"INSERT INTO global_tasks (task,type,last_activation,param1,param2,param3) VALUES(?,?,?,?,?,?)" };

	/**
	 * 已註冊的任務集合
	 * <p>Key: 任務名稱的 hashCode, Value: Task 物件
	 */
	private final Map<Integer, Task> _tasks = Maps.newMap();

	/**
	 * 目前執行中的任務列表
	 * <p>包含所有已啟動的 ExecutedTask 實例。
	 */
	protected final List<ExecutedTask> _currentTasks = Lists.newList();

	/**
	 * 已執行任務包裝類別
	 * <p>封裝一個正在執行的任務，包含任務實例、類型、參數及執行狀態。
	 *
	 * <h3>主要功能:</h3>
	 * <ul>
	 *   <li>執行任務邏輯 ({@link Task#onTimeElapsed(ExecutedTask)})</li>
	 *   <li>追蹤任務最後執行時間</li>
	 *   <li>將執行時間持久化到資料庫</li>
	 *   <li>管理排程任務的取消與停止</li>
	 * </ul>
	 *
	 * <h3>生命週期:</h3>
	 * <ol>
	 *   <li>從資料庫載入建立實例</li>
	 *   <li>根據任務類型啟動排程</li>
	 *   <li>定期執行 {@link #run()} 方法</li>
	 *   <li>更新 last_activation 到資料庫</li>
	 *   <li>呼叫 {@link #stopTask()} 停止任務</li>
	 * </ol>
	 *
	 * @see Task
	 * @see TaskTypes
	 */
	public class ExecutedTask implements Runnable {
		/** 任務 ID (對應資料庫 id 欄位) */
		int _id;

		/** 最後執行時間 (毫秒時間戳) */
		long _lastActivation;

		/** 任務實例 */
		Task _task;

		/** 任務類型 */
		TaskTypes _type;

		/** 任務參數 (3 個自訂參數) */
		String[] _params;

		/** 排程 Future 物件 (用於取消排程) */
		ScheduledFuture<?> _scheduled;

		/**
		 * 建構已執行任務
		 * <p>從資料庫 ResultSet 載入任務資料。
		 *
		 * @param task 任務實例
		 * @param type 任務類型
		 * @param rset 資料庫查詢結果集
		 * @throws SQLException 資料庫讀取錯誤
		 */
		public ExecutedTask(Task task, TaskTypes type, ResultSet rset) throws SQLException {
			_task = task;
			_type = type;
			_id = rset.getInt("id");
			_lastActivation = rset.getLong("last_activation");
			_params = new String[]
			{ rset.getString("param1"), rset.getString("param2"), rset.getString("param3") };
		}

		/**
		 * 執行任務
		 * <p>呼叫任務的 {@link Task#onTimeElapsed(ExecutedTask)} 方法，
		 * 並將最後執行時間更新到資料庫。
		 *
		 * <h3>執行流程:</h3>
		 * <ol>
		 *   <li>執行任務邏輯 ({@code _task.onTimeElapsed(this)})</li>
		 *   <li>更新最後執行時間為當前時間</li>
		 *   <li>將最後執行時間寫入資料庫</li>
		 * </ol>
		 *
		 * @see Task#onTimeElapsed(ExecutedTask)
		 */
		@Override
		public void run() {
			_task.onTimeElapsed(this);

			_lastActivation = System.currentTimeMillis();

			java.sql.Connection con = null;
			PreparedStatement pstm = null;
			try {
				con = L1DatabaseFactory.getInstance().getConnection();
				pstm = con.prepareStatement(SQL_STATEMENTS[1]);
				pstm.setLong(1, _lastActivation);
				pstm.setInt(2, _id);
				pstm.executeUpdate();
			}
			catch (SQLException e) {
				_log.warning("cannot updated the Global Task " + _id + ": " + e.getMessage());
			}
			finally {
				SQLUtil.close(pstm);
				SQLUtil.close(con);
			}

		}

		/**
		 * 比較兩個 ExecutedTask 是否相同
		 * <p>根據任務 ID 判斷是否為同一任務。
		 *
		 * @param object 要比較的物件
		 * @return true 表示 ID 相同，false 表示不同
		 */
		@Override
		public boolean equals(Object object) {
			return _id == ((ExecutedTask) object)._id;
		}

		/**
		 * 取得任務實例
		 *
		 * @return Task 物件
		 */
		public Task getTask() {
			return _task;
		}

		/**
		 * 取得任務類型
		 *
		 * @return 任務類型
		 */
		public TaskTypes getType() {
			return _type;
		}

		/**
		 * 取得任務 ID
		 *
		 * @return 任務 ID
		 */
		public int getId() {
			return _id;
		}

		/**
		 * 取得任務參數
		 *
		 * @return 包含 3 個參數的字串陣列
		 */
		public String[] getParams() {
			return _params;
		}

		/**
		 * 取得最後執行時間
		 *
		 * @return 最後執行時間 (毫秒時間戳)
		 */
		public long getLastActivation() {
			return _lastActivation;
		}

		/**
		 * 停止任務
		 * <p>取消排程並清理資源。
		 *
		 * <h3>停止流程:</h3>
		 * <ol>
		 *   <li>呼叫 {@link Task#onDestroy()} 清理任務資源</li>
		 *   <li>取消排程 Future ({@code _scheduled.cancel(true)})</li>
		 *   <li>從執行中任務列表移除</li>
		 * </ol>
		 *
		 * @see Task#onDestroy()
		 * @see ScheduledFuture#cancel(boolean)
		 */
		public void stopTask() {
			_task.onDestroy();

			if (_scheduled != null) {
				_scheduled.cancel(true);
			}

			_currentTasks.remove(this);
		}

	}

	/**
	 * 取得 Singleton 實例
	 * <p>使用延遲初始化模式，首次呼叫時建立實例。
	 *
	 * @return TaskManager 實例
	 */
	public static TaskManager getInstance() {
		if (_instance == null) {
			_instance = new TaskManager();
		}
		return _instance;
	}

	/**
	 * 建構任務管理器
	 * <p>初始化內建任務並從資料庫載入所有任務。
	 *
	 * <h3>初始化流程:</h3>
	 * <ol>
	 *   <li>呼叫 {@link #initializate()} 註冊內建任務</li>
	 *   <li>呼叫 {@link #startAllTasks()} 從資料庫載入並啟動所有任務</li>
	 * </ol>
	 *
	 * @see #initializate()
	 * @see #startAllTasks()
	 */
	public TaskManager() {
		initializate();
		startAllTasks();
	}

	/**
	 * 初始化內建任務
	 * <p>註冊伺服器預設的全域任務。
	 *
	 * <h3>內建任務:</h3>
	 * <ul>
	 *   <li>{@link TaskRestart} - 伺服器自動重啟</li>
	 *   <li>{@link TaskShutdown} - 伺服器自動關機</li>
	 * </ul>
	 *
	 * @see TaskRestart
	 * @see TaskShutdown
	 */
	private void initializate() {
		registerTask(new TaskRestart());
		registerTask(new TaskShutdown());
	}

	/**
	 * 註冊任務
	 * <p>將任務加入任務集合，並呼叫任務的初始化方法。
	 *
	 * <h3>註冊流程:</h3>
	 * <ol>
	 *   <li>計算任務名稱的 hashCode 作為 Key</li>
	 *   <li>檢查任務是否已註冊 (避免重複)</li>
	 *   <li>將任務加入 _tasks Map</li>
	 *   <li>呼叫 {@link Task#initializate()} 初始化任務</li>
	 * </ol>
	 *
	 * <p><b>注意:</b> 相同名稱的任務只會註冊一次。
	 *
	 * @param task 要註冊的任務
	 * @see Task#getName()
	 * @see Task#initializate()
	 */
	public void registerTask(Task task) {
		int key = task.getName().hashCode();
		if (!_tasks.containsKey(key)) {
			_tasks.put(key, task);
			task.initializate();
		}
	}

	/**
	 * 啟動所有任務
	 * <p>從資料庫載入所有任務設定並啟動對應的任務。
	 *
	 * <h3>載入流程:</h3>
	 * <ol>
	 *   <li>查詢 global_tasks 表取得所有任務</li>
	 *   <li>根據任務名稱找到對應的 Task 實例</li>
	 *   <li>建立 ExecutedTask 包裝任務</li>
	 *   <li>根據任務類型啟動排程</li>
	 * </ol>
	 *
	 * <p><b>注意:</b> 若任務名稱未註冊，該任務會被跳過。
	 *
	 * @see #registerTask(Task)
	 * @see ExecutedTask
	 */
	private void startAllTasks() {
		java.sql.Connection con = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;
		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement(SQL_STATEMENTS[0]);
			rs = pstm.executeQuery();

			while (rs.next()) {
				Task task = _tasks.get(rs.getString("task").trim().toLowerCase().hashCode());

				if (task == null) {
					continue;
				}

			}

		}
		catch (Exception e) {
			_log.log(Level.SEVERE, "error while loading Global Task table", e);
		}
		finally {
			if (null != rs) {
				try {
					rs.close();
				}
				catch (SQLException ignore) {
					// ignore
				}
				rs = null;
			}

			if (null != pstm) {
				try {
					pstm.close();
				}
				catch (SQLException ignore) {
					// ignore
				}
				pstm = null;
			}

			if (null != con) {
				try {
					con.close();
				}
				catch (SQLException ignore) {
					// ignore
				}
				con = null;
			}
		}

	}

	/**
	 * 新增唯一任務 (不指定最後執行時間)
	 * <p>若任務名稱不存在則新增，否則不重複新增。
	 *
	 * @param task 任務名稱
	 * @param type 任務類型
	 * @param param1 參數 1
	 * @param param2 參數 2
	 * @param param3 參數 3
	 * @return true 表示新增成功或任務已存在，false 表示發生錯誤
	 * @see #addUniqueTask(String, TaskTypes, String, String, String, long)
	 */
	public static boolean addUniqueTask(String task, TaskTypes type, String param1, String param2, String param3) {
		return addUniqueTask(task, type, param1, param2, param3, 0);
	}

	/**
	 * 新增唯一任務到資料庫
	 * <p>檢查任務名稱是否已存在，若不存在則插入新任務。
	 *
	 * <h3>新增流程:</h3>
	 * <ol>
	 *   <li>查詢資料庫檢查任務名稱是否存在</li>
	 *   <li>若不存在，插入新任務記錄</li>
	 *   <li>若已存在，不執行任何操作</li>
	 * </ol>
	 *
	 * <h3>用途:</h3>
	 * <p>用於確保同名任務只有一筆記錄，適合用於伺服器重啟、關機等全域唯一任務。
	 *
	 * <h3>使用範例:</h3>
	 * <pre>
	 * // 新增伺服器每日重啟任務 (凌晨 3 點)
	 * TaskManager.addUniqueTask("restart", TaskTypes.TYPE_SHEDULED,
	 *                           "03:00:00", "", "");
	 * </pre>
	 *
	 * @param task 任務名稱 (唯一識別)
	 * @param type 任務類型
	 * @param param1 任務參數 1 (用途依任務而定)
	 * @param param2 任務參數 2 (用途依任務而定)
	 * @param param3 任務參數 3 (用途依任務而定)
	 * @param lastActivation 最後執行時間 (毫秒時間戳，0 表示從未執行)
	 * @return true 表示新增成功或任務已存在，false 表示發生資料庫錯誤
	 * @see TaskTypes
	 */
	public static boolean addUniqueTask(String task, TaskTypes type, String param1, String param2, String param3, long lastActivation) {
		java.sql.Connection con = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;

		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement(SQL_STATEMENTS[2]);
			pstm.setString(1, task);
			rs = pstm.executeQuery();

			if (!rs.next()) {
				pstm = con.prepareStatement(SQL_STATEMENTS[3]);
				pstm.setString(1, task);
				pstm.setString(2, type.toString());
				pstm.setLong(3, lastActivation);
				pstm.setString(4, param1);
				pstm.setString(5, param2);
				pstm.setString(6, param3);
				pstm.execute();
			}

			return true;
		}
		catch (SQLException e) {
			_log.warning("cannot add the unique task: " + e.getMessage());
		}
		finally {
			SQLUtil.close(rs);
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}

		return false;
	}

	/**
	 * 新增任務 (不指定最後執行時間)
	 * <p>直接插入新任務，允許重複的任務名稱。
	 *
	 * @param task 任務名稱
	 * @param type 任務類型
	 * @param param1 參數 1
	 * @param param2 參數 2
	 * @param param3 參數 3
	 * @return true 表示新增成功，false 表示發生錯誤
	 * @see #addTask(String, TaskTypes, String, String, String, long)
	 */
	public static boolean addTask(String task, TaskTypes type, String param1, String param2, String param3) {
		return addTask(task, type, param1, param2, param3, 0);
	}

	/**
	 * 新增任務到資料庫
	 * <p>直接插入新任務記錄，不檢查是否重複。
	 *
	 * <h3>新增流程:</h3>
	 * <ol>
	 *   <li>直接插入新任務記錄到 global_tasks 表</li>
	 *   <li>不檢查任務名稱是否已存在</li>
	 * </ol>
	 *
	 * <h3>用途:</h3>
	 * <p>用於允許重複執行的任務，例如定期備份、定期清理等可以同時存在多筆的任務。
	 *
	 * <h3>使用範例:</h3>
	 * <pre>
	 * // 新增每 30 分鐘執行一次的備份任務
	 * TaskManager.addTask("backup", TaskTypes.TYPE_SHEDULED,
	 *                     "30 * * * *", "/backup/path", "");
	 * </pre>
	 *
	 * <h3>與 addUniqueTask 的差異:</h3>
	 * <ul>
	 *   <li><b>addTask:</b> 不檢查重複，允許同名任務多筆記錄</li>
	 *   <li><b>addUniqueTask:</b> 檢查重複，同名任務僅保留一筆</li>
	 * </ul>
	 *
	 * @param task 任務名稱
	 * @param type 任務類型
	 * @param param1 任務參數 1 (用途依任務而定)
	 * @param param2 任務參數 2 (用途依任務而定)
	 * @param param3 任務參數 3 (用途依任務而定)
	 * @param lastActivation 最後執行時間 (毫秒時間戳，0 表示從未執行)
	 * @return true 表示新增成功，false 表示發生資料庫錯誤
	 * @see TaskTypes
	 * @see #addUniqueTask(String, TaskTypes, String, String, String, long)
	 */
	public static boolean addTask(String task, TaskTypes type, String param1, String param2, String param3, long lastActivation) {
		java.sql.Connection con = null;
		PreparedStatement pstm = null;

		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement(SQL_STATEMENTS[3]);
			pstm.setString(1, task);
			pstm.setString(2, type.toString());
			pstm.setLong(3, lastActivation);
			pstm.setString(4, param1);
			pstm.setString(5, param2);
			pstm.setString(6, param3);
			pstm.execute();

			return true;
		}
		catch (SQLException e) {
			_log.log(Level.SEVERE, "cannot add the task", e);
		}
		finally {
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}

		return false;
	}

}
