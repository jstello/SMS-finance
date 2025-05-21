package com.example.finanzaspersonales.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val userId: String?,
    val name: String,
    val color: Int
) 