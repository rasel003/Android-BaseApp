package com.rasel.androidbaseapp.data2.repository

import com.rasel.androidbaseapp.data2.models.CharacterEntity

interface CharacterRemote {
    suspend fun getCharacters(): List<CharacterEntity>
    suspend fun getCharacter(characterId: Long): CharacterEntity
}
