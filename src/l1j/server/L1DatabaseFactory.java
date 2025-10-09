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
package l1j.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.server.utils.LeakCheckedConnection;

/**
 * DBへのアクセスするための各種インターフェースを提供する.
 * 使用簡單的連接池實現，不依賴外部庫
 */
public class L1DatabaseFactory {
	/** 資料庫的實例 */
	private static L1DatabaseFactory _instance;

	/** 連接池 */
	private final List<Connection> connectionPool;
	private final List<Connection> usedConnections = new ArrayList<>();
	private static final int INITIAL_POOL_SIZE = 10;
	private static final int MAX_POOL_SIZE = 20;

	/** 紀錄用 */
	private static Logger _log = Logger.getLogger(L1DatabaseFactory.class.getName());

	/* 連結資料庫相關的資訊 */
	/** 資料庫連結的驅動程式 */
	private static String _driver;

	/** 資料庫連結的位址 */
	private static String _url;

	/** 登入資料庫的使用者名稱 */
	private static String _user;

	/** 登入資料庫的密碼 */
	private static String _password;

	/**
	 * 設定資料庫登入的相關資訊
	 * 
	 * @param driver 
	 *            資料庫連結的驅動程式
	 * @param url
	 *            資料庫連結的位址
	 * @param user
	 *            登入資料庫的使用者名稱 
	 * @param password
	 *            登入資料庫的密碼
	 */
	public static void setDatabaseSettings(final String driver,
			final String url, final String user, final String password) {
		_driver = driver;
		_url = url;
		_user = user;
		_password = password;
	}

	/**
	 * 資料庫連結的設定與配置
	 * 
	 * @throws SQLException
	 */
	public L1DatabaseFactory() throws SQLException {
		try {
			// 載入 JDBC 驅動程式
			Class.forName(_driver);
			
			_log.info("Attempting to connect to database: " + _url);
			_log.info("Using driver: " + _driver);
			_log.info("User: " + _user);
			
			// 初始化連接池
			connectionPool = new ArrayList<>(INITIAL_POOL_SIZE);
			for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
				connectionPool.add(createConnection());
			}
			
			_log.info("Database connection pool initialized with " + INITIAL_POOL_SIZE + " connections");
			
			/* Test the connection */
			Connection testConn = getConnection();
			if (testConn != null) {
				releaseConnection(testConn);
				_log.info("Database connection test successful");
			}
		} catch (ClassNotFoundException e) {
			_log.severe("Database driver not found: " + _driver);
			throw new SQLException("Database driver not found: " + e.getMessage());
		} catch (SQLException x) {
			_log.severe("Database Connection FAILED: " + x.getMessage());
			throw x;
		} catch (Exception e) {
			_log.severe("Database Connection FAILED: " + e.getMessage());
			throw new SQLException("Could not init DB connection: " + e.getMessage());
		}
	}

	/**
	 * 創建新的資料庫連接
	 */
	private Connection createConnection() throws SQLException {
		return DriverManager.getConnection(_url, _user, _password);
	}

	/**
	 * 伺服器關閉的時候要關閉與資料庫的連結
	 */
	public void shutdown() {
		try {
			// 關閉所有連接池中的連接
			for (Connection conn : connectionPool) {
				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			}
			connectionPool.clear();
			
			// 關閉所有正在使用的連接
			for (Connection conn : usedConnections) {
				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			}
			usedConnections.clear();
			
			_log.info("Database connections closed");
		} catch (Exception e) {
			_log.log(Level.WARNING, "Error closing database connections", e);
		}
	}

	/**
	 * 取得資料庫的實例（第一次實例為 null 的時候才新建立一個).
	 * 
	 * @return L1DatabaseFactory
	 * @throws SQLException
	 */
	public static L1DatabaseFactory getInstance() throws SQLException {
		if (_instance == null) {
			_instance = new L1DatabaseFactory();
		}
		return _instance;
	}

	/**
	 * 取得資料庫連結時的連線
	 * 
	 * @return Connection 連結對象
	 * @throws SQLException
	 */
	public synchronized Connection getConnection() {
		Connection conn = null;

		try {
			// 如果連接池中有可用連接，從池中獲取
			if (!connectionPool.isEmpty()) {
				conn = connectionPool.remove(connectionPool.size() - 1);
				
				// 檢查連接是否仍然有效
				if (conn.isClosed() || !conn.isValid(2)) {
					conn = createConnection();
				}
			} 
			// 如果池中沒有連接，創建新連接（如果未達到最大限制）
			else if (usedConnections.size() < MAX_POOL_SIZE) {
				conn = createConnection();
			}
			// 如果已達到最大限制，等待可用連接
			else {
				_log.warning("Connection pool exhausted, waiting for available connection...");
				// 可以在這裡實現等待邏輯或拋出異常
				conn = createConnection(); // 暫時創建新連接
			}
			
			usedConnections.add(conn);
			
		} catch (SQLException e) {
			_log.warning("L1DatabaseFactory: getConnection() failed: " + e.getMessage());
			throw new RuntimeException("Failed to get database connection", e);
		}
		
		return l1j.server.Config.DETECT_DB_RESOURCE_LEAKS ? LeakCheckedConnection.create(conn) : conn;
	}

	/**
	 * 釋放連接回連接池
	 * 
	 * @param conn 要釋放的連接
	 */
	public synchronized void releaseConnection(Connection conn) {
		if (conn != null) {
			try {
				usedConnections.remove(conn);
				
				// 只有當連接仍然有效時才放回池中
				if (!conn.isClosed() && conn.isValid(2)) {
					connectionPool.add(conn);
				} else {
					conn.close();
				}
			} catch (SQLException e) {
				_log.log(Level.WARNING, "Error releasing connection", e);
			}
		}
	}
}
