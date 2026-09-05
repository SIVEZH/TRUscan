package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ComplaintDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity)

    @Update
    suspend fun updateComplaint(complaint: ComplaintEntity)

    @Query("SELECT * FROM complaints WHERE category = :category ORDER BY createdAt DESC")
    fun getComplaintsByCategory(category: String): Flow<List<ComplaintEntity>>

    @Query("SELECT * FROM complaints WHERE complaintId = :complaintId")
    suspend fun getComplaintById(complaintId: String): ComplaintEntity?

    @Query("SELECT * FROM complaints WHERE sourceScanId = :scanId LIMIT 1")
    suspend fun getComplaintByScanId(scanId: String): ComplaintEntity?
}
