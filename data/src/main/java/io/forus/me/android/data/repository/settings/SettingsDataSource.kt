package io.forus.me.android.data.repository.settings

interface SettingsDataSource {

    fun clear()

    fun isFingerprintEnabled(): Boolean

    fun setFingerprintEnabled(isFingerprintEnabled: Boolean): Boolean

    fun isPinEnabled(): Boolean

    fun setPin(pin: String): Boolean

    fun getPin(): String
}