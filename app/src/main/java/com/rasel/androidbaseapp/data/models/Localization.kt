package com.rasel.androidbaseapp.data.models

import java.io.Serializable

class Localization : Serializable {

    /**
     * Properties
     */
    var lblGreeting : String = ""
    var lblSelectedLanguage : String = ""
    var lblEnglish : String = ""
    var lblBurmese : String = ""
    var lblChinese : String = ""

    companion object {
        const val TEMPLATE_HANDLE = "%@"

        private val dummy = Localization().apply {
            lblGreeting = "Hello"
            lblSelectedLanguage = "Selected Language : %@"
            lblEnglish = "English"
            lblChinese = "Chinese"
            lblBurmese = "Burmese"
        }
        fun getDefaultLocalization() : Localization = dummy

        fun getTemplatedString(format: String, vararg params: String): String =
            if (params.isNotEmpty() && format.contains(TEMPLATE_HANDLE))
                getTemplatedString(
                    format.replaceFirst(TEMPLATE_HANDLE, params.first()),
                    *params.drop(1).toTypedArray()
                )
        else
            format
    }
}