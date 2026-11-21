package com.example.data

import kotlinx.coroutines.flow.Flow

/**
 * Trait-mirror interface designed to match the Rust `instincta-core` UniFFI contract.
 * Currently fulfilled by [KotlinInstagramCore] inside this Kotlin/Compose APK; swapping to the
 * compiled Rust library (`cargo-ndk` + `uniffi`) requires only implementing this interface via
 * UniFFI bindings without modifying UI or ViewModel layers.
 */
interface InstagramCorePort {
    val sessionFlow: Flow<InstagramSession?>
    val oauthConfigFlow: Flow<OAuthAppConfig>

    suspend fun saveOAuthConfig(appId: String, proxyBaseUrl: String)
    fun buildAuthorizeUrl(appId: String): String
    fun extractAuthCode(rawInput: String): String?

    suspend fun connectWithAccessToken(accessToken: String): Result<InstagramSession>
    suspend fun exchangeCodeForSession(authCode: String): Result<InstagramSession>
    suspend fun refreshSession(): Result<InstagramSession>
    suspend fun logout()

    suspend fun getProfile(): Result<InstagramProfile>
    suspend fun getMediaGrid(afterCursor: String? = null): Result<MediaGridPage>
    suspend fun getMediaInsights(mediaId: String, kind: IgMediaKind): Result<MediaInsights>
    suspend fun getAccountInsights(): Result<AccountInsights>

    suspend fun getComments(mediaId: String): Result<List<InstagramComment>>
    suspend fun postComment(mediaId: String, message: String): Result<Unit>
    suspend fun replyToComment(commentId: String, message: String): Result<Unit>
    suspend fun setCommentHidden(commentId: String, hide: Boolean): Result<Unit>
    suspend fun deleteComment(commentId: String): Result<Unit>

    suspend fun publishMedia(
        request: PublishRequest,
        onProgress: (String) -> Unit,
    ): Result<String>

    suspend fun getConversations(myUserId: String): Result<List<InstagramConversation>>
    suspend fun getConversationThread(conversationId: String, myUserId: String): Result<InstagramConversation>
    suspend fun sendDirectMessage(recipientId: String, text: String): Result<String>
}
