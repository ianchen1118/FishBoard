package com.example.androidtest

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "scan_sessions")
data class ScanSessionEntity(
    @PrimaryKey val sessionId: String,
    val dateCode: String,
    val locationCode: String,
    val deviceCode: String,
    val sessionNumber: Int,
    val startedAtMillis: Long
)

@Entity(tableName = "fish_records")
data class FishRecordEntity(
    @PrimaryKey val internalId: String,
    val sessionId: String,
    val fishNumber: Int,
    val displayFishId: String,
    val timestampMillis: Long,
    val species: String,
    val speciesConfidence: Double?,
    val lengthMm: Int,
    val lengthConfidence: Double?,
    val correctedSpecies: String?,
    val correctedLengthMm: Int?,
    val reviewed: Boolean,
    val exportedAtMillis: Long?,
    val notes: String?,
    val photoFilename: String,
    val photoRelativePath: String,
    val photoUri: String?
)

@Dao
interface FishBoardDao {
    @Query("SELECT * FROM scan_sessions ORDER BY startedAtMillis DESC")
    fun observeSessions(): Flow<List<ScanSessionEntity>>

    @Query("SELECT * FROM fish_records ORDER BY timestampMillis DESC")
    fun observeRecords(): Flow<List<FishRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: ScanSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: FishRecordEntity)

    @Update
    suspend fun updateRecord(record: FishRecordEntity)

    @Query("UPDATE fish_records SET exportedAtMillis = :exportedAtMillis WHERE internalId IN (:recordIds)")
    suspend fun markRecordsExported(recordIds: List<String>, exportedAtMillis: Long)

    @Query("DELETE FROM fish_records WHERE exportedAtMillis IS NOT NULL")
    suspend fun deleteExportedRecords()
}

@Database(
    entities = [
        ScanSessionEntity::class,
        FishRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FishBoardDatabase : RoomDatabase() {
    abstract fun fishBoardDao(): FishBoardDao

    companion object {
        @Volatile
        private var instance: FishBoardDatabase? = null

        fun getDatabase(context: Context): FishBoardDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FishBoardDatabase::class.java,
                    "fishboard.db"
                ).build().also { instance = it }
            }
        }
    }
}

class FishBoardRepository(
    private val dao: FishBoardDao
) {
    val sessions: Flow<List<ScanSession>> = dao.observeSessions().map { entities ->
        entities.map { it.toDomain() }
    }

    val records: Flow<List<FishRecord>> = dao.observeRecords().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun saveSession(session: ScanSession) {
        dao.upsertSession(session.toEntity())
    }

    suspend fun addRecord(record: FishRecord) {
        dao.insertRecord(record.toEntity())
    }

    suspend fun updateRecord(record: FishRecord) {
        dao.updateRecord(record.toEntity())
    }

    suspend fun markRecordsExported(records: List<FishRecord>, exportedAtMillis: Long) {
        dao.markRecordsExported(
            recordIds = records.map { it.internalId },
            exportedAtMillis = exportedAtMillis
        )
    }

    suspend fun deleteExportedRecords() {
        dao.deleteExportedRecords()
    }
}

fun ScanSessionEntity.toDomain(): ScanSession {
    return ScanSession(
        sessionId = sessionId,
        dateCode = dateCode,
        locationCode = locationCode,
        deviceCode = deviceCode,
        sessionNumber = sessionNumber,
        startedAtMillis = startedAtMillis
    )
}

fun ScanSession.toEntity(): ScanSessionEntity {
    return ScanSessionEntity(
        sessionId = sessionId,
        dateCode = dateCode,
        locationCode = locationCode,
        deviceCode = deviceCode,
        sessionNumber = sessionNumber,
        startedAtMillis = startedAtMillis
    )
}

fun FishRecordEntity.toDomain(): FishRecord {
    return FishRecord(
        internalId = internalId,
        sessionId = sessionId,
        fishNumber = fishNumber,
        displayFishId = displayFishId,
        timestampMillis = timestampMillis,
        species = species,
        speciesConfidence = speciesConfidence,
        lengthMm = lengthMm,
        lengthConfidence = lengthConfidence,
        correctedSpecies = correctedSpecies,
        correctedLengthMm = correctedLengthMm,
        reviewed = reviewed,
        exportedAtMillis = exportedAtMillis,
        notes = notes,
        photoFilename = photoFilename,
        photoRelativePath = photoRelativePath,
        photoUri = photoUri
    )
}

fun FishRecord.toEntity(): FishRecordEntity {
    return FishRecordEntity(
        internalId = internalId,
        sessionId = sessionId,
        fishNumber = fishNumber,
        displayFishId = displayFishId,
        timestampMillis = timestampMillis,
        species = species,
        speciesConfidence = speciesConfidence,
        lengthMm = lengthMm,
        lengthConfidence = lengthConfidence,
        correctedSpecies = correctedSpecies,
        correctedLengthMm = correctedLengthMm,
        reviewed = reviewed,
        exportedAtMillis = exportedAtMillis,
        notes = notes,
        photoFilename = photoFilename,
        photoRelativePath = photoRelativePath,
        photoUri = photoUri
    )
}
