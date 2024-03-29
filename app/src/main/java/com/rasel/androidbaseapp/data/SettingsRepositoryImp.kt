package com.rasel.androidbaseapp.data

import com.rasel.androidbaseapp.domain.models.SettingType
import com.rasel.androidbaseapp.domain.models.Settings
import com.rasel.androidbaseapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SettingsRepositoryImp @Inject constructor(
    private val appVersion: String,
) : SettingsRepository {
    override suspend fun getSettings(isNightMode: Boolean): Flow<List<Settings>> = flow {
        emit(getData(isNightMode))
    }

    // This should be came from api but we don't have api so we are crating locally
    private fun getData(isNightMode: Boolean): List<Settings> {
        val settingList = mutableListOf<Settings>()
        settingList.add(Settings(1, SettingType.SWITCH, "Theme mode", "", isNightMode))
        settingList.add(Settings(2, SettingType.EMPTY, "Clear cache", ""))
        settingList.add(Settings(3, SettingType.TEXT, "Choose Theme", appVersion))
        settingList.add(Settings(4, SettingType.TEXT, "Choose Language", appVersion))
        settingList.add(Settings(5, SettingType.TEXT, "App version", appVersion))
        return settingList
    }
}
