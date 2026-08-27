package pl.foodhub.pos.core.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JWT + refresh token at rest, in EncryptedSharedPreferences backed by the Android
 * Keystore (ANDROID_POS_ARCHITECTURE.md section 12). Never in plain SharedPreferences.
 */
@Singleton
class TokenStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs by lazy {
            val masterKey =
                MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            EncryptedSharedPreferences.create(
                context,
                "foodhub_pos_session",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        private val _tokens = MutableStateFlow(read())
        val tokens: StateFlow<Tokens?> = _tokens.asStateFlow()

        fun save(
            accessToken: String,
            refreshToken: String?,
        ) {
            prefs.edit()
                .putString(KEY_ACCESS, accessToken)
                .putString(KEY_REFRESH, refreshToken)
                .apply()
            _tokens.value = Tokens(accessToken, refreshToken)
        }

        fun clear() {
            prefs.edit().clear().apply()
            _tokens.value = null
        }

        private fun read(): Tokens? {
            val access = prefs.getString(KEY_ACCESS, null) ?: return null
            return Tokens(access, prefs.getString(KEY_REFRESH, null))
        }

        data class Tokens(val accessToken: String, val refreshToken: String?)

        private companion object {
            const val KEY_ACCESS = "access_token"
            const val KEY_REFRESH = "refresh_token"
        }
    }
