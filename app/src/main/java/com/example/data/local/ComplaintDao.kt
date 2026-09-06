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

    @Query("UPDATE complaints SET status = :status WHERE complaintId = :complaintId")
    suspend fun updateStatus(complaintId: String, status: String)

    @Query("UPDATE complaints SET status = :status, rejectionReason = :rejectionReason, authorityAction = :authorityAction, actionTimestamp = :actionTimestamp WHERE complaintId = :complaintId")
    suspend fun updateRejection(complaintId: String, status: String, rejectionReason: String, authorityAction: String, actionTimestamp: String)
}
