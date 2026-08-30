package com.example.minimaxcreator.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GenerationRecordDao {
    @Query("SELECT * FROM generation_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GenerationRecordEntity>>

    @Query("SELECT * FROM generation_records WHERE id = :id")
    suspend fun getById(id: String): GenerationRecordEntity?

    @Upsert
    suspend fun upsert(record: GenerationRecordEntity)

    @Query("DELETE FROM generation_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM generation_records")
    suspend fun deleteAll()

    @Query("DELETE FROM generation_records WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
