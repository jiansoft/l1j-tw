# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

L1J-TW is a Java-based game server emulator for Lineage 1 (Taiwan version 3.80c). This is a MMORPG server implementation that handles game logic, networking, database operations, and game world simulation.

## Build System

**Using Apache Ant:**

Build the project:
```bash
# Windows
build\ant\bin\ant

# Full build with debug symbols (default)
build\ant\bin\ant all

# Minimal build without debug symbols
build\ant\bin\ant mini
```

The build system is configured in `build.xml` and produces two JAR files:
- `l1jserver.jar` - Main game server
- `l1jloader.jar` - Server loader utility

Build targets:
- `all` - Clean, compile loader, compile server, package both JARs (with debug)
- `mini` - Same as all but without debug symbols
- `clean` - Delete compiled classes
- `compile_server` - Compile server with debug symbols
- `compile_server_mini` - Compile server without debug symbols
- `jar_server` - Package server JAR
- `compile_loader` - Compile loader with debug symbols
- `jar_loader` - Package loader JAR

The build requires Java 8 (release="8") and uses UTF-8 encoding.

## Running the Server

**Start the server:**
```bash
java -jar l1jserver.jar
```

The main entry point is `l1j.server.Server.main()` which:
1. Initializes logging from `config/log.properties`
2. Loads configuration from `config/*.properties`
3. Initializes the database connection pool
4. Starts `GameServer` which loads all game data and spawns

**Database Setup:**

Configure database connection in `config/server.properties`:
```properties
Driver=com.mysql.cj.jdbc.Driver
URL=jdbc:mysql://localhost:3306/l1j-tw?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Taipei
Login=root
Password=your_password
```

Import the database schema from `db/l1jdb_Taiwan.sql`.

## Architecture

### Package Structure

- `l1j.server` - Core server infrastructure
  - `Server.java` - Main entry point, initializes logging and config
  - `Config.java` - Configuration manager (loads all .properties files)
  - `L1DatabaseFactory.java` - Database connection pool manager
  - `L1Message.java` - Localization/message strings

- `l1j.server.server` - Game server core
  - `GameServer.java` - Main game server, network listener, initializes all game systems
  - `ClientThread.java` - Handles individual client connections
  - `PacketHandler.java` - Routes incoming packets to handlers
  - `Opcodes.java` - Network protocol opcodes

- `l1j.server.server.clientpackets` - Client → Server packet handlers
  - Each class handles a specific client action (e.g., `C_MoveChar`, `C_Attack`, `C_Chat`)

- `l1j.server.server.serverpackets` - Server → Client packet constructors
  - Each class builds a specific server response (e.g., `S_HPUpdate`, `S_ChatPacket`)

- `l1j.server.server.datatables` - Database-backed data loaders
  - Singleton classes that load and cache game data (NPCs, items, skills, etc.)

- `l1j.server.server.model` - Game world model
  - `L1World.java` - Central world manager (all objects, players, NPCs)
  - `Instance/` - Game entities (L1PcInstance for players, L1NpcInstance for NPCs)

- `l1j.server.server.templates` - Data templates loaded from database

- `l1j.server.server.utils` - Utility classes

- `l1j.server.console` - Console command interface for server management

### Server Initialization Flow

1. `Server.main()` → Load config → Initialize database
2. `GameServer.getInstance().initialize()` starts initialization sequence:
   - Initialize ID factory
   - Load world maps (with optional caching via `CacheMapFiles`)
   - Initialize login controller
   - Load character data, clear online statuses
   - Start game time clock (`L1GameTimeClock`)
   - Initialize time-based controllers (UB, War, Auction, House Tax, Fishing, etc.)
   - Load all data tables (NPCs, items, skills, spawns, shops, etc.)
   - Initialize world, traps, dungeons
   - Spawn NPCs
   - Start thread pools
   - Add shutdown hook
   - Start console command processor
3. `GameServer.run()` → Accept client connections in loop

### Database Connection Management

The project uses a custom connection pool implementation in `L1DatabaseFactory.java`:
- Initial pool size: 10 connections
- Maximum pool size: 20 connections
- Connections are validated before reuse (`isValid(2)`)
- Uses MySQL Connector/J driver (version 9.4.0 in lib/)
- Pool automatically grows on demand up to max size

**Important:** Always release connections back to the pool after use. Enable leak detection during development:
```properties
# In server.properties
EnableDatabaseResourceLeaksDetection = true
```

### Network Protocol

The server uses a custom binary protocol:
- `Opcodes.java` defines all packet opcodes
- Client packets: Named `C_*` (e.g., `C_MoveChar.java`)
- Server packets: Named `S_*` (e.g., `S_MoveCharPacket.java`)
- Each packet class handles serialization/deserialization
- `PacketHandler.java` dispatches incoming packets to appropriate handlers

### Configuration System

Configuration is split across multiple `.properties` files in `config/`:
- `server.properties` - Server and database settings
- `rates.properties` - Game rates (XP, drop rates, enchant chances)
- `altsettings.properties` - Alternative game features
- `charsettings.properties` - Character class HP/MP and level EXP tables
- `fights.properties` - Combat and PvP settings
- `record.properties` - Logging and backup settings

All loaded by `Config.load()` at startup. The `Config` class uses static fields for all settings.

### Threading Model

The server uses configurable thread pools (see `GeneralThreadPool.java`, `ThreadPoolManager.java`):
- Type 0: Regular threads (no pooling)
- Type 1: Fixed-size thread pool
- Type 2: Cached thread pool (reuses idle threads)

Configure in `server.properties`:
```properties
GeneralThreadPoolType = 2
GeneralThreadPoolSize = 0
```

Separate thread implementations for:
- Skill timers (`SkillTimerImplType`)
- NPC AI (`NpcAIImplType`)

### Time-Based Systems

Multiple time controllers run as scheduled tasks:
- `WarTimeController` - Castle siege wars
- `UbTimeController` - Underground battles (무한대전)
- `AuctionTimeController` - House auctions
- `HouseTaxTimeController` - House tax collection
- `FishingTimeController` - Fishing events
- `NpcChatTimeController` - NPC ambient chat
- `LightTimeController` - Day/night cycle
- `HomeTownTimeController` - Town management

## Development Guidelines

### Adding New Packet Handlers

1. Create client packet handler in `server/clientpackets/`:
   ```java
   public class C_YourPacket extends ClientBasePacket {
       public C_YourPacket(byte[] decrypt, ClientThread client) {
           // Parse packet data
       }
   }
   ```

2. Register opcode in `Opcodes.java`

3. Add to `PacketHandler.java` switch statement

4. Create corresponding server packet in `server/serverpackets/` if needed

### Database Access Pattern

1. Get connection from pool:
   ```java
   Connection con = null;
   PreparedStatement pstm = null;
   ResultSet rs = null;
   try {
       con = L1DatabaseFactory.getInstance().getConnection();
       pstm = con.prepareStatement("SELECT ...");
       rs = pstm.executeQuery();
       // Process results
   } catch (SQLException e) {
       _log.log(Level.SEVERE, e.getLocalizedMessage(), e);
   } finally {
       SQLUtil.close(rs, pstm, con);
   }
   ```

2. Always close resources in finally block or use try-with-resources

### Adding New Game Data

1. Add database table/columns to `db/l1jdb_Taiwan.sql`
2. Create or update template class in `server/templates/`
3. Create or update data table loader in `server/datatables/`
4. Initialize in `GameServer.initialize()`

### Character Language Setting

The server supports multiple languages (configured via `ClientLanguage` in server.properties):
- 0: US (UTF8)
- 3: Taiwan (BIG5)
- 4: Japan (SJIS)
- 5: China (GBK)

This affects message encoding and localization.

### Console Commands

Interactive commands available when server is running (if `CmdActive=true`):
- Implemented in `l1j.server.console.ConsoleProcess`
- Allows runtime server management without restart

### GM Commands

GM commands configured in `config/gmcommands.xml`, loaded by `GMCommandsConfig.load()`.

## Common Issues

### Database Connection Failures
- Verify MySQL is running and accessible
- Check `config/server.properties` has correct URL, login, password
- Ensure MySQL Connector/J JAR (`mysql-connector-j-9.4.0.jar`) is in `lib/`
- Check timezone setting matches database server

### Build Failures
- Ensure using Java 8 JDK
- Verify Ant is properly installed in `build/ant/`
- Check all required JARs are in `lib/` directory

### Map Loading Issues
- Enable map caching: `CacheMapFiles=true` in server.properties
- Map cache stored in `data/mapcache/`
- V2 maps optional: `LoadV2MapFiles=false`

## Project Metadata

- **Version:** L1J-TW 3.80c
- **Java Version:** 8
- **Database:** MySQL 5.x or later
- **License:** Creative Commons Public License (CCPL)
- **Team:** L1JTW 99nets
