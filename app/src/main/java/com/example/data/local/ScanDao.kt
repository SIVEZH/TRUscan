package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity)

    @Query("SELECT * FROM scans WHERE userId = :userId ORDER BY timestamp DESC")
    fun getScansForUser(userId: String): Flow<List<ScanEntity>>
    
    @Query("SELECT * FROM scans WHERE scanId = :scanId LIMIT 1")
    suspend fun getScanById(scanId: String): ScanEntity?
}
