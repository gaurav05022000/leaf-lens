package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "point_transactions")
data class PointTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Int,
    val timestamp: Long,
    val isEarned: Boolean
)

@Entity(tableName = "local_state")
data class LocalStateEntity(
    @PrimaryKey val id: Int = 1,
    val availablePoints: Int = 50,
    val totalScans: Int = 0,
    val firstRunGranted: Boolean = false
)
