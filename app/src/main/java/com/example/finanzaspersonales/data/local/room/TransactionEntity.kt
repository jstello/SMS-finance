package com.example.finanzaspersonales.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
 data class TransactionEntity(
     @PrimaryKey val id: String,
     val userId: String?,
     val date: Long,
     val amount: Float,
     val isIncome: Boolean,
     val description: String?,
     val provider: String?,
     val contactName: String?,
     val accountInfo: String?,
     val categoryId: String?
 ) 