package com.example.finanzaspersonales.data.db.mapper

import com.example.finanzaspersonales.data.local.room.CategoryEntity
import com.example.finanzaspersonales.data.model.Category
import java.util.UUID

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    userId = userId,
    name = name,
    color = color
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = this.id ?: UUID.randomUUID().toString(),
    userId = this.userId,
    name = this.name,
    color = this.color
) 