package com.rasel.androidbaseapp.data.network.responses

data class Settings(
    val id: Int,
    val type: SettingType,
    val settingLabel: String,
    val settingValue: String,
    var selectedValue: Boolean = false
)

enum class SettingType {
    SWITCH,
    TEXT,
    EMPTY
}
