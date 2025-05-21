package com.example.finanzaspersonales.data.db.mapper

import com.example.finanzaspersonales.data.local.room.TransactionEntity
import com.example.finanzaspersonales.data.model.TransactionData
import java.util.Date
import java.util.UUID

fun TransactionEntity.toDomain(): TransactionData = TransactionData(
    id = id,
    userId = userId,
    date = Date(date),
    amount = amount,
    isIncome = isIncome,
    description = description,
    provider = provider,
    contactName = contactName,
    accountInfo = accountInfo,
    categoryId = categoryId
)

fun TransactionData.toEntity(): TransactionEntity = TransactionEntity(
    id = this.id ?: UUID.randomUUID().toString(),
    userId = this.userId,
    date = this.date.time,
    amount = this.amount,
    isIncome = this.isIncome,
    description = this.description,
    provider = this.provider,
    contactName = this.contactName,
    accountInfo = this.accountInfo,
    categoryId = this.categoryId
) 