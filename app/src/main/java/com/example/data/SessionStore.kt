package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.instinctaSessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "instincta_instagram_session",
)

class SessionStore(private val context: Context) {
    private val keyAccessToken = stringPreferencesKey("access_token")
    private val keyUserId = stringPreferencesKey("user_id")
    private val keyExpiresAt = longPreferencesKey("expires_at_epoch_sec")
    private val keyOAuthAppId = stringPreferencesKey("oauth_app_id")
    private val keyOAuthProxyUrl = stringPreferencesKey("oauth_proxy_base_url")

    val sessionFlow: Flow<InstagramSession?> = context.instinctaSessionDataStore.data.map { prefs ->
        val token = prefs[keyAccessToken]
        val userId = prefs[keyUserId]
        val expiresAt = prefs[keyExpiresAt] ?: 0L
        if (token.isNullOrBlank() || userId.isNullOrBlank()) {
            null
        } else {
            InstagramSession(
                accessToken = token,
                userId = userId,
                expiresAtEpochSec = expiresAt,
            )
        }
    }

    val oauthConfigFlow: Flow<OAuthAppConfig> = context.instinctaSessionDataStore.data.map { prefs ->
        OAuthAppConfig(
            appId = prefs[keyOAuthAppId].orEmpty(),
            proxyBaseUrl = prefs[keyOAuthProxyUrl].orEmpty(),
            redirectUri = OAuthAppConfig.DEFAULT_REDIRECT_URI,
        )
    }

    suspend fun currentSession(): InstagramSession? = sessionFlow.first()

    suspend fun currentOAuthConfig(): OAuthAppConfig = oauthConfigFlow.first()

    suspend fun saveSession(session: InstagramSession) {
        context.instinctaSessionDataStore.edit { prefs ->
            prefs[keyAccessToken] = session.accessToken
            prefs[keyUserId] = session.userId
            prefs[keyExpiresAt] = session.expiresAtEpochSec
        }
    }

    suspend fun saveOAuthConfig(appId: String, proxyBaseUrl: String) {
        context.instinctaSessionDataStore.edit { prefs ->
            prefs[keyOAuthAppId] = appId.trim()
            prefs[keyOAuthProxyUrl] = proxyBaseUrl.trim().trimEnd('/')
        }
    }

    suspend fun clearSession() {
        context.instinctaSessionDataStore.edit { prefs ->
            prefs.remove(keyAccessToken)
            prefs.remove(keyUserId)
            prefs.remove(keyExpiresAt)
        }
    }
}
