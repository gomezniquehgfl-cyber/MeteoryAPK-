package com.meteory.optimizer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// Entities
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "game_profiles")
data class GameProfileEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val cpuGovernor: String   = "performance",
    val renderer: String      = "auto",
    val refreshHz: Int        = 60,
    val dndEnabled: Boolean   = true,
    val hudEnabled: Boolean   = true,
    val backgroundKill: Boolean = true,
    val updatedAt: Long       = System.currentTimeMillis()
)

@Entity(tableName = "system_health_log")
data class SystemHealthLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val score: Int,
    val cpuUsage: Int,
    val ramUsageMb: Int,
    val temperatureC: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cleaning_history")
data class CleaningHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val freedMb: Long,
    val type: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "battery_history")
data class BatteryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val level: Int,
    val temperatureC: Float,
    val isCharging: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────────────────────────
// DAOs
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface GameProfileDao {
    @Query("SELECT * FROM game_profiles ORDER BY appName ASC")
    fun getAllProfiles(): Flow<List<GameProfileEntity>>

    @Query("SELECT * FROM game_profiles WHERE packageName = :pkg LIMIT 1")
    suspend fun getProfile(pkg: String): GameProfileEntity?

    @Upsert
    suspend fun upsert(profile: GameProfileEntity)

    @Delete
    suspend fun delete(profile: GameProfileEntity)
}

@Dao
interface SystemHealthLogDao {
    @Query("SELECT * FROM system_health_log ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<SystemHealthLogEntity>>

    @Insert
    suspend fun insert(log: SystemHealthLogEntity)

    @Query("DELETE FROM system_health_log WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

@Dao
interface CleaningHistoryDao {
    @Query("SELECT * FROM cleaning_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<CleaningHistoryEntity>>

    @Insert
    suspend fun insert(entry: CleaningHistoryEntity)

    @Query("SELECT SUM(freedMb) FROM cleaning_history")
    suspend fun totalFreedMb(): Long?
}

@Dao
interface BatteryHistoryDao {
    @Query("SELECT * FROM battery_history ORDER BY timestamp DESC LIMIT 200")
    fun getRecentHistory(): Flow<List<BatteryHistoryEntity>>

    @Insert
    suspend fun insert(entry: BatteryHistoryEntity)

    @Query("DELETE FROM battery_history WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

// ─────────────────────────────────────────────────────────────────────────────
// Database
// ─────────────────────────────────────────────────────────────────────────────

@Database(
    entities = [
        GameProfileEntity::class,
        SystemHealthLogEntity::class,
        CleaningHistoryEntity::class,
        BatteryHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameProfileDao(): GameProfileDao
    abstract fun systemHealthLogDao(): SystemHealthLogDao
    abstract fun cleaningHistoryDao(): CleaningHistoryDao
    abstract fun batteryHistoryDao(): BatteryHistoryDao
}
