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
import java.sql.SQLException;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import l1j.server.server.utils.LeakCheckedConnection;

/**
 * 提供存取資料庫的各種介面.
 * 使用 HikariCP 高性能連接池
 */
public class L1DatabaseFactory {
	/** 資料庫的實例 */
	private static L1DatabaseFactory _instance;

	/** HikariCP 資料來源 */
	private HikariDataSource dataSource;

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

	/** HikariCP 連接池設定 */
	private static int _minimumIdle = 10;
	private static int _maximumPoolSize = 20;
	private static long _connectionTimeout = 30000;
	private static long _idleTimeout = 600000;
	private static long _maxLifetime = 1800000;

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
	 * 設定 HikariCP 連接池參數
	 *
	 * @param minimumIdle 最小閒置連接數
	 * @param maximumPoolSize 最大連接池大小
	 * @param connectionTimeout 連接超時時間（毫秒）
	 * @param idleTimeout 閒置超時時間（毫秒）
	 * @param maxLifetime 連接最大存活時間（毫秒）
	 */
	public static void setHikariConfig(int minimumIdle, int maximumPoolSize,
			long connectionTimeout, long idleTimeout, long maxLifetime) {
		_minimumIdle = minimumIdle;
		_maximumPoolSize = maximumPoolSize;
		_connectionTimeout = connectionTimeout;
		_idleTimeout = idleTimeout;
		_maxLifetime = maxLifetime;
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

			// 配置 HikariCP
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(_url);
			config.setUsername(_user);
			config.setPassword(_password);
			config.setDriverClassName(_driver);

			// 連接池參數設定
			config.setMinimumIdle(_minimumIdle);
			config.setMaximumPoolSize(_maximumPoolSize);
			config.setConnectionTimeout(_connectionTimeout);
			config.setIdleTimeout(_idleTimeout);
			config.setMaxLifetime(_maxLifetime);

			// 連接測試與健康檢查
			config.setConnectionTestQuery("SELECT 1");
			config.setPoolName("L1J-HikariCP");

			// 性能優化設定
			config.addDataSourceProperty("cachePrepStmts", "true");
			config.addDataSourceProperty("prepStmtCacheSize", "250");
			config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
			config.addDataSourceProperty("useServerPrepStmts", "true");
			config.addDataSourceProperty("useLocalSessionState", "true");
			config.addDataSourceProperty("rewriteBatchedStatements", "true");
			config.addDataSourceProperty("cacheResultSetMetadata", "true");
			config.addDataSourceProperty("cacheServerConfiguration", "true");
			config.addDataSourceProperty("elideSetAutoCommits", "true");
			config.addDataSourceProperty("maintainTimeStats", "false");

			// 初始化 HikariCP 資料來源
			dataSource = new HikariDataSource(config);

			_log.info("HikariCP connection pool initialized");
			_log.info("  - Minimum Idle: " + _minimumIdle);
			_log.info("  - Maximum Pool Size: " + _maximumPoolSize);
			_log.info("  - Connection Timeout: " + _connectionTimeout + "ms");
			_log.info("  - Idle Timeout: " + _idleTimeout + "ms");
			_log.info("  - Max Lifetime: " + _maxLifetime + "ms");

			/* Test the connection */
			Connection testConn = getConnection();
			if (testConn != null) {
				testConn.close();
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
	 * 伺服器關閉的時候要關閉與資料庫的連結
	 */
	public void shutdown() {
		try {
			if (dataSource != null && !dataSource.isClosed()) {
				dataSource.close();
				_log.info("HikariCP connection pool closed");
			}
		} catch (Exception e) {
			_log.warning("Error closing HikariCP connection pool: " + e.getMessage());
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
	public Connection getConnection() {
		Connection conn = null;

		try {
			// 從 HikariCP 獲取連接
			conn = dataSource.getConnection();
		} catch (SQLException e) {
			_log.warning("L1DatabaseFactory: getConnection() failed: " + e.getMessage());
			throw new RuntimeException("Failed to get database connection", e);
		}

		// 如果啟用了連接洩漏檢測，返回包裝後的連接
		return l1j.server.Config.DETECT_DB_RESOURCE_LEAKS ? LeakCheckedConnection.create(conn) : conn;
	}

	/**
	 * 釋放連接回連接池
	 * 注意：使用 HikariCP 時，只需調用 Connection.close() 即可將連接歸還池中
	 * 此方法保留是為了向後兼容，但實際上直接關閉連接即可
	 *
	 * @param conn 要釋放的連接
	 * @deprecated 建議直接使用 Connection.close() 代替
	 */
	@Deprecated
	public void releaseConnection(Connection conn) {
		if (conn != null) {
			try {
				conn.close(); // HikariCP 會自動將連接歸還池中
			} catch (SQLException e) {
				_log.warning("Error releasing connection: " + e.getMessage());
			}
		}
	}
}
