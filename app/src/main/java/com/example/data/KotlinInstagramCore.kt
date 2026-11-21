package com.example.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class KotlinInstagramCore(
    private val sessionStore: SessionStore,
    private val authProxyApi: InstagramAuthProxyApi = InstagramApiFactory.authProxyApi,
    private val graphApi: InstagramGraphApi = InstagramApiFactory.graphApi,
) : InstagramCorePort {

    override val sessionFlow: Flow<InstagramSession?> = sessionStore.sessionFlow
    override val oauthConfigFlow: Flow<OAuthAppConfig> = sessionStore.oauthConfigFlow

    override suspend fun saveOAuthConfig(appId: String, proxyBaseUrl: String) {
        sessionStore.saveOAuthConfig(appId, proxyBaseUrl)
    }

    override fun buildAuthorizeUrl(appId: String): String {
        val scopes = listOf(
            "instagram_business_basic",
            "instagram_business_content_publish",
            "instagram_business_manage_comments",
            "instagram_business_manage_messages",
        ).joinToString(",")

        return Uri.Builder()
            .scheme("https")
            .authority("www.instagram.com")
            .path("oauth/authorize")
            .appendQueryParameter("enable_fb_login", "0")
            .appendQueryParameter("force_authentication", "1")
            .appendQueryParameter("client_id", appId.trim())
            .appendQueryParameter("redirect_uri", OAuthAppConfig.DEFAULT_REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", scopes)
            .build()
            .toString()
    }

    override fun extractAuthCode(rawInput: String): String? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null
        val candidate = if (trimmed.contains("://") && trimmed.contains("code=")) {
            Uri.parse(trimmed).getQueryParameter("code")
        } else {
            trimmed
        }
        return candidate
            ?.substringBefore("#_")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    override suspend fun connectWithAccessToken(accessToken: String): Result<InstagramSession> =
        withContext(Dispatchers.IO) {
            runCatchingApi {
                val cleanToken = accessToken.trim()
                require(cleanToken.length >= 20) {
                    "Enter a valid Instagram User Access Token generated from Meta Developer Dashboard or your OAuth proxy."
                }
                val profile = graphApi.getProfile(accessToken = cleanToken)
                val resolvedUserId = profile.userId ?: profile.id
                    ?: error("Instagram did not return a user ID for this token.")
                // Meta dashboard long-lived tokens are valid for 60 days (or refreshable via ig_refresh_token).
                val expiresAt = (System.currentTimeMillis() / 1000L) + 5_184_000L
                val session = InstagramSession(
                    accessToken = cleanToken,
                    userId = resolvedUserId,
                    expiresAtEpochSec = expiresAt,
                )
                sessionStore.saveSession(session)
                session
            }
        }

    override suspend fun exchangeCodeForSession(authCode: String): Result<InstagramSession> =
        withContext(Dispatchers.IO) {
            runCatchingApi {
                val cleanCode = extractAuthCode(authCode)
                    ?: error("Authorization code is empty or malformed.")
                val config = sessionStore.currentOAuthConfig()
                require(config.isConfiguredForBrowserOAuth) {
                    "Configure your numeric Meta App ID and Rust Axum OAuth Proxy URL before exchanging an authorization code."
                }
                val base = config.proxyBaseUrl.trim().trimEnd('/')
                val exchangeUrl = "$base/oauth/instagram/exchange"
                val resp = authProxyApi.exchangeCodeViaProxy(
                    exchangeUrl = exchangeUrl,
                    payload = ProxyCodeExchangeRequestDto(
                        code = cleanCode,
                        redirectUri = config.redirectUri,
                    ),
                )
                val userId = resp.userId?.takeIf { it.isNotBlank() } ?: run {
                    val profile = graphApi.getProfile(accessToken = resp.accessToken)
                    profile.userId ?: profile.id ?: "me"
                }
                val session = InstagramSession(
                    accessToken = resp.accessToken,
                    userId = userId,
                    expiresAtEpochSec = (System.currentTimeMillis() / 1000L) + resp.expiresIn,
                )
                sessionStore.saveSession(session)
                session
            }
        }

    override suspend fun refreshSession(): Result<InstagramSession> =
        withContext(Dispatchers.IO) {
            runCatchingApi {
                val existing = requireSession()
                val refreshed = graphApi.refreshLongLivedToken(accessToken = existing.accessToken)
                val updated = existing.copy(
                    accessToken = refreshed.accessToken,
                    expiresAtEpochSec = (System.currentTimeMillis() / 1000L) + refreshed.expiresIn,
                )
                sessionStore.saveSession(updated)
                updated
            }
        }

    override suspend fun logout() {
        sessionStore.clearSession()
    }

    override suspend fun getProfile(): Result<InstagramProfile> =
        withContext(Dispatchers.IO) {
            runCatchingApi {
                val session = requireSession()
                val dto = graphApi.getProfile(accessToken = session.accessToken)
                val resolvedId = dto.userId ?: dto.id ?: session.userId
                InstagramProfile(
                    id = dto.id ?: resolvedId,
                    userId = resolvedId,
                    username = dto.username ?: "creator",
                    name = dto.name ?: dto.username ?: "Instagram Account",
                    accountType = dto.accountType ?: "BUSINESS",
                    profilePictureUrl = dto.profilePictureUrl,
                    biography = dto.biography.orEmpty(),
                    followersCount = dto.followersCount ?: 0,
                    followsCount = dto.followsCount ?: 0,
                    mediaCount = dto.mediaCount ?: 0,
                )
            }
        }

    override suspend fun getMediaGrid(afterCursor: String?): Result<MediaGridPage> =
        withContext(Dispatchers.IO) {
            runCatchingApi {
                val session = requireSession()
                val resp = graphApi.getMedia(
                    after = afterCursor,
                    accessToken = session.accessToken,
                )
                MediaGridPage(
                    items = resp.data.map { dto ->
                        InstagramMediaItem(
                            id = dto.id,
                            caption = dto.caption.orEmpty(),
                            kind = IgMediaKind.fromWire(dto.mediaType),
                            mediaUrl = dto.mediaUrl,
                            thumbnailUrl = dto.thumbnailUrl,
                            permalink = dto.permalink,
                            timestamp = dto.timestamp.orEmpty(),
                            likeCount = dto.likeCount ?: 0,
                            commentsCount = dto.commentsCount ?: 0,
                            children = dto.children?.data.orEmpty().map { child ->
                                InstagramMediaChild(
                                    id = child.id,
                                    kind = IgMediaKind.fromWire(child.mediaType),
                                    mediaUrl = child.mediaUrl,
                                    thumbnailUrl = child.thumbnailUrl,
                                )
                            },
                        )
                    },
                    nextCursor = resp.paging?.cursors?.after,
                )
            }
        }

    override suspend fun getMediaInsights(mediaId: String, kind: IgMediaKind): Result<MediaInsights> =
        withContext(Dispatchers.IO) {
            runCatchingApi {
                val session = requireSession()
                val primaryMetrics = "reach,views,likes,comments,saved,shares"
                val fallbackMetrics = "reach,saved,likes,comments,shares"
                val response = try {
                    graphApi.getMediaInsights(
                        mediaId = mediaId,
                        metric = primaryMetrics,
                        accessToken = session.accessToken,
                    )
                } catch (_: Throwable) {
                    graphApi.getMediaInsights(
                        mediaId = mediaId,
                        metric = fallbackMetrics,
                        accessToken = session.accessToken,
                    )
                }
                val map = response.data.associate { item ->
                    val v = item.totalValue?.value ?: item.values?.lastOrNull()?.value
                    item.name.lowercase() to v
                }
                MediaInsights(
                    mediaId = mediaId,
                    reach = map["reach"],
                    views = map["views"] ?: map["impressions"],
                    likes = map["likes"],
                    comments = map["comments"],
                    saved = map["saved"],
                    shares = map["shares"],
                )
            }
        }

    override suspend fun getAccountInsights(): Result<AccountInsights> =
        withContext(Dispatchers.IO) {
            runCatchingApi {
                val session = requireSession()
                val response = try {
                    graphApi.getAccountInsights(
                        metric = "reach,profile_views,accounts_engaged",
                        accessToken = session.accessToken,
                    )
                } catch (_: Throwable) {
                    graphApi.getAccountInsights(
                        metric = "reach",
                        accessToken = session.accessToken,
                    )
                }

                val cards = response.data.map { item ->
                    val value = item.totalValue?.value
                        ?: item.values?.sumOf { it.value ?: 0L }
                        ?: 0L
                    InsightMetricCard(
                        key = item.name,
                        title = item.title ?: item.name.replace('_', ' ').replaceFirstChar { it.uppercase() },
                        value = value,
                        description = item.description.orEmpty(),
                    )
                }
                val byName = cards.associateBy { it.key.lowercase() }
                AccountInsights(
                    reach = byName["reach"]?.value,
                    profileViews = byName["profile_views"]?.value,
                    accountsEngaged = byName["accounts_engaged"]?.value,
                    rawMetrics = cards,
                )
            }
        }

    override suspend fun getComments(mediaId: String): Result<List<InstagramComment>> =
        withContext(Dispatchers.IO) {
            runCatchingApi {
                val session = requireSession()
                val resp = graphApi.getComments(
                    mediaId = mediaId,
                    accessToken = session.accessToken,
                )
                resp.data.map { it.toDomain() }
            }
        }

    override suspend fun postComment(mediaId: String, message: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatchingApi {
                val session = requireSession()
                graphApi.postComment(
                    mediaId = mediaId,
                    message = message.trim(),
                    accessToken = session.accessToken,
                )
                Unit
            }
        }

    override suspend fun replyToComment(commentId: String, message: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatchingApi {
                val session = requireSession()
                graphApi.replyToComment(
                    commentId = commentId,
                    message = message.trim(),
                    accessToken = session.accessToken,
                )
                Unit
            }
        }

    override suspend fun setCommentHidden(commentId: String, hide: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatchingApi {
                val session = requireSession()
                graphApi.setCommentHidden(
                    commentId = commentId,
                    hide = hide,
                    accessToken = session.accessToken,
                )
                Unit
            }
        }

    override suspend fun deleteComment(commentId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatchingApi {
                val session = requireSession()
                graphApi.deleteComment(
                    commentId = commentId,
                    accessToken = session.accessToken,
                )
                Unit
            }
        }

    override suspend fun publishMedia(
        request: PublishRequest,
        onProgress: (String) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatchingApi {
            val session = requireSession()
            val containerId = when (request) {
                is PublishRequest.SinglePhoto -> {
                    onProgress("Creating photo container…")
                    val created = graphApi.createMediaContainer(
                        imageUrl = request.imageUrl.trim(),
                        caption = request.caption.trim().ifEmpty { null },
                        accessToken = session.accessToken,
                    )
                    waitForContainerReady(created.id, session.accessToken, onProgress)
                    created.id
                }

                is PublishRequest.Reel -> {
                    onProgress("Creating Reel container…")
                    val created = graphApi.createMediaContainer(
                        videoUrl = request.videoUrl.trim(),
                        mediaType = "REELS",
                        caption = request.caption.trim().ifEmpty { null },
                        shareToFeed = request.shareToFeed,
                        accessToken = session.accessToken,
                    )
                    waitForContainerReady(created.id, session.accessToken, onProgress)
                    created.id
                }

                is PublishRequest.Carousel -> {
                    val cleanUrls = request.itemUrls.map { it.trim() }.filter { it.isNotEmpty() }
                    require(cleanUrls.size in 2..10) {
                        "Carousel publishing requires between 2 and 10 public HTTPS media URLs."
                    }
                    val childIds = mutableListOf<String>()
                    cleanUrls.forEachIndexed { index, url ->
                        onProgress("Preparing carousel item ${index + 1}/${cleanUrls.size}…")
                        val isVideo = MediaStorageHelper.isVideoFile(url)
                        val child = graphApi.createMediaContainer(
                            imageUrl = if (isVideo) null else url,
                            videoUrl = if (isVideo) url else null,
                            mediaType = if (isVideo) "VIDEO" else null,
                            isCarouselItem = true,
                            accessToken = session.accessToken,
                        )
                        waitForContainerReady(child.id, session.accessToken, onProgress)
                        childIds += child.id
                    }
                    onProgress("Assembling carousel container…")
                    val parent = graphApi.createMediaContainer(
                        mediaType = "CAROUSEL",
                        children = childIds.joinToString(","),
                        caption = request.caption.trim().ifEmpty { null },
                        accessToken = session.accessToken,
                    )
                    waitForContainerReady(parent.id, session.accessToken, onProgress)
                    parent.id
                }
            }

            onProgress("Publishing to Instagram…")
            val published = graphApi.publishContainer(
                creationId = containerId,
                accessToken = session.accessToken,
            )
            published.id
        }
    }

    override suspend fun getConversations(myUserId: String): Result<List<InstagramConversation>> =
        withContext(Dispatchers.IO) {
            runCatchingApi {
                val session = requireSession()
                val resp = graphApi.getConversations(accessToken = session.accessToken)
                resp.data.map { it.toDomain(myUserId.ifBlank { session.userId }) }
            }
        }

    override suspend fun getConversationThread(
        conversationId: String,
        myUserId: String,
    ): Result<InstagramConversation> = withContext(Dispatchers.IO) {
        runCatchingApi {
            val session = requireSession()
            val dto = graphApi.getConversationThread(
                conversationId = conversationId,
                accessToken = session.accessToken,
            )
            dto.toDomain(myUserId.ifBlank { session.userId })
        }
    }

    override suspend fun sendDirectMessage(recipientId: String, text: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatchingApi {
                val session = requireSession()
                val resp = graphApi.sendDirectMessage(
                    accessToken = session.accessToken,
                    payload = IgSendMessageRequestDto(
                        recipient = IgSendMessageRecipientDto(id = recipientId),
                        message = IgSendMessagePayloadDto(text = text.trim()),
                    ),
                )
                resp.messageId ?: ""
            }
        }

    private suspend fun waitForContainerReady(
        creationId: String,
        accessToken: String,
        onProgress: (String) -> Unit,
    ) {
        repeat(20) { attempt ->
            val status = graphApi.getContainerStatus(
                creationId = creationId,
                accessToken = accessToken,
            )
            when (status.statusCode?.uppercase()) {
                "FINISHED", "PUBLISHED" -> return
                "ERROR", "EXPIRED" -> error(
                    status.status ?: "Media container processing failed (${status.statusCode}).",
                )
                else -> {
                    onProgress("Processing media on Meta servers (${attempt + 1}/20)…")
                    delay(1_800L)
                }
            }
        }
        error("Timed out waiting for Meta media container to finish processing.")
    }

    private suspend fun requireSession(): InstagramSession {
        return sessionStore.currentSession()
            ?: error("Not signed in to an Instagram account.")
    }

    private inline fun <T> runCatchingApi(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (t: Throwable) {
        Result.failure(IllegalStateException(InstagramApiFactory.parseApiError(t), t))
    }

    private fun IgCommentDto.toDomain(): InstagramComment = InstagramComment(
        id = id,
        text = text.orEmpty(),
        timestamp = timestamp.orEmpty(),
        username = username ?: "instagram_user",
        hidden = hidden ?: false,
        likeCount = likeCount ?: 0,
        replies = replies?.data.orEmpty().map { it.toDomain() },
    )

    private fun IgConversationDto.toDomain(myUserId: String): InstagramConversation {
        val participants = participants?.data.orEmpty()
        val other = participants.firstOrNull { it.id != myUserId } ?: participants.firstOrNull()
        val recipient = InstagramParticipant(
            id = other?.id.orEmpty(),
            username = other?.username.orEmpty(),
            name = other?.name.orEmpty(),
        )
        val domainMessages = messages?.data.orEmpty()
            .map { msg ->
                val senderId = msg.from?.id.orEmpty()
                InstagramMessage(
                    id = msg.id,
                    text = msg.message.orEmpty(),
                    createdTime = msg.createdTime.orEmpty(),
                    senderId = senderId,
                    senderUsername = msg.from?.username ?: msg.from?.name ?: senderId,
                    isFromMe = senderId.isNotBlank() && senderId == myUserId,
                )
            }
            .reversed()

        return InstagramConversation(
            id = id,
            updatedTime = updatedTime.orEmpty(),
            recipient = recipient,
            lastMessagePreview = domainMessages.lastOrNull()?.text.orEmpty(),
            messages = domainMessages,
        )
    }
}
