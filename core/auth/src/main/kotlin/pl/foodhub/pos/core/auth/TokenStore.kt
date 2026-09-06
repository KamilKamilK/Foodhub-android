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

        private val _tokens = MutableStateFlow(readTokens())
        val tokens: StateFlow<Tokens?> = _tokens.asStateFlow()

        private val _posSession = MutableStateFlow(readPosSession())
        val posSession: StateFlow<PosSession?> = _posSession.asStateFlow()

        private val _mercureToken = MutableStateFlow(prefs.getString(KEY_MERCURE_TOKEN, null))
        val mercureToken: StateFlow<String?> = _mercureToken.asStateFlow()

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

        /**
         * Scopes the terminal's Mercure/SSE subscription to its place (see
         * `core:realtime`). Unlike the token pair, this is only ever present when the
         * authenticating request carried a device payload (login or a device-aware
         * refresh) -- absent otherwise, e.g. a device-less refresh never overwrites a
         * previously saved value.
         */
        fun saveMercureToken(token: String?) {
            if (null == token) return
            prefs.edit().putString(KEY_MERCURE_TOKEN, token).apply()
            _mercureToken.value = token
        }

        /**
         * Called once, right after a successful PIN login: persists the place/POS
         * context independently of the token pair, since a later token refresh does
         * not re-issue these claims (see [PosSession]).
         */
        fun savePosSession(session: PosSession) {
            prefs.edit()
                .putString(KEY_PLACE_ID, session.placeId)
                .putString(KEY_PLACE_NAME, session.placeName)
                .putString(KEY_POS_ID, session.posId)
                .apply()
            _posSession.value = session
        }

        fun clear() {
            prefs.edit().clear().apply()
            _tokens.value = null
            _posSession.value = null
            _mercureToken.value = null
        }

        private fun readTokens(): Tokens? {
            val access = prefs.getString(KEY_ACCESS, null) ?: return null
            return Tokens(access, prefs.getString(KEY_REFRESH, null))
        }

        private fun readPosSession(): PosSession? {
            val placeId = prefs.getString(KEY_PLACE_ID, null) ?: return null
            return PosSession(
                placeId = placeId,
                placeName = prefs.getString(KEY_PLACE_NAME, null) ?: "",
                posId = prefs.getString(KEY_POS_ID, null),
            )
        }

        data class Tokens(val accessToken: String, val refreshToken: String?)

        private companion object {
            const val KEY_ACCESS = "access_token"
            const val KEY_REFRESH = "refresh_token"
            const val KEY_PLACE_ID = "pos_session_place_id"
            const val KEY_PLACE_NAME = "pos_session_place_name"
            const val KEY_POS_ID = "pos_session_pos_id"
            const val KEY_MERCURE_TOKEN = "mercure_token"
        }
    }
