package com.rasel.androidbaseapp.cache.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rasel.androidbaseapp.cache.utils.CacheConstants

@Entity(tableName = CacheConstants.CHARACTER_TABLE_NAME)
data class CharacterCacheEntity(
    val created: String,
    val gender: String,
    @PrimaryKey
    val id: Int,
    val image: String,
    @Embedded
    val characterLocation: CharacterLocationCacheEntity,
    val name: String,
    val species: String,
    val status: String,
    val type: String,
    val url: String,
    @ColumnInfo(name = "is_bookmarked")
    var isBookMarked: Boolean
)
