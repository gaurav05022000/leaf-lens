package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PointTransactionDao {
    @Query("SELECT * FROM point_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<PointTransactionEntity>>
    
    @Query("SELECT * FROM point_transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSync(): List<PointTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: PointTransactionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<PointTransactionEntity>)

    @Query("DELETE FROM point_transactions")
    suspend fun clearAll()
}

@Dao
interface LocalStateDao {
    @Query("SELECT * FROM local_state WHERE id = 1")
    fun getLocalState(): Flow<LocalStateEntity?>

    @Query("SELECT * FROM local_state WHERE id = 1")
    suspend fun getLocalStateSync(): LocalStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateLocalState(state: LocalStateEntity)
}
