package com.rasel.androidbaseapp.cache.mapper

import com.rasel.androidbaseapp.cache.entities.CharacterCacheEntity
import com.rasel.androidbaseapp.data2.models.CharacterEntity
import javax.inject.Inject

class CharacterCacheMapper @Inject constructor(
    private val characterLocationCacheMapper: CharacterLocationCacheMapper
) : CacheMapper<CharacterCacheEntity, CharacterEntity> {
    override fun mapFromCached(type: CharacterCacheEntity): CharacterEntity {
        return CharacterEntity(
            created = type.created,
            gender = type.gender,
            id = type.id,
            image = type.image,
            characterLocation = characterLocationCacheMapper.mapFromCached(type.characterLocation),
            name = type.name,
            species = type.species,
            status = type.status,
            type = type.type,
            url = type.url,
            isBookMarked = type.isBookMarked
        )
    }

    override fun mapToCached(type: CharacterEntity): CharacterCacheEntity {
        return CharacterCacheEntity(
            created = type.created,
            gender = type.gender,
            id = type.id,
            image = type.image,
            characterLocation = characterLocationCacheMapper.mapToCached(type.characterLocation),
            name = type.name,
            species = type.species,
            status = type.status,
            type = type.type,
            url = type.url,
            isBookMarked = type.isBookMarked
        )
    }
}
