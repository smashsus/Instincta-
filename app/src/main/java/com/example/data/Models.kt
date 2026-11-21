package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---------------------------------------------------------------------------
// Wire DTOs (Meta Instagram API with Instagram Login & Graph API v22.0)
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class ProxyCodeExchangeRequestDto(
    val code: String,
    @Json(name = "redirect_uri") val redirectUri: String,
)

@JsonClass(generateAdapter = true)
data class ProxyCodeExchangeResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "expires_in") val expiresIn: Long = 5_184_000L,
)

@JsonClass(generateAdapter = true)
data class TokenDataItemDto(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    val permissions: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class ShortLivedTokenResponseDto(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    val data: List<TokenDataItemDto>? = null,
)

@JsonClass(generateAdapter = true)
data class LongLivedTokenResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Long = 5_184_000L,
)

@JsonClass(generateAdapter = true)
data class IgProfileDto(
    val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    val username: String? = null,
    val name: String? = null,
    @Json(name = "account_type") val accountType: String? = null,
    @Json(name = "profile_picture_url") val profilePictureUrl: String? = null,
    val biography: String? = null,
    @Json(name = "followers_count") val followersCount: Int? = null,
    @Json(name = "follows_count") val followsCount: Int? = null,
    @Json(name = "media_count") val mediaCount: Int? = null,
)

@JsonClass(generateAdapter = true)
data class IgPagingCursorsDto(
    val before: String? = null,
    val after: String? = null,
)

@JsonClass(generateAdapter = true)
data class IgPagingDto(
    val cursors: IgPagingCursorsDto? = null,
    val next: String? = null,
)

@JsonClass(generateAdapter = true)
data class IgMediaChildDto(
    val id: String,
    @Json(name = "media_type") val mediaType: String? = null,
    @Json(name = "media_url") val mediaUrl: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class IgMediaChildrenWrapperDto(
    val data: List<IgMediaChildDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class IgMediaDto(
    val id: String,
    val caption: String? = null,
    @Json(name = "media_type") val mediaType: String = "IMAGE",
    @Json(name = "media_url") val mediaUrl: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    val permalink: String? = null,
    val timestamp: String? = null,
    @Json(name = "like_count") val likeCount: Int? = null,
    @Json(name = "comments_count") val commentsCount: Int? = null,
    val children: IgMediaChildrenWrapperDto? = null,
)

@JsonClass(generateAdapter = true)
data class IgMediaListResponseDto(
    val data: List<IgMediaDto> = emptyList(),
    val paging: IgPagingDto? = null,
)

@JsonClass(generateAdapter = true)
data class IgInsightValueDto(
    val value: Long? = null,
    @Json(name = "end_time") val endTime: String? = null,
)

@JsonClass(generateAdapter = true)
data class IgTotalValueDto(
    val value: Long? = null,
)

@JsonClass(generateAdapter = true)
data class IgInsightItemDto(
    val name: String,
    val period: String? = null,
    val title: String? = null,
    val description: String? = null,
    val values: List<IgInsightValueDto>? = null,
    @Json(name = "total_value") val totalValue: IgTotalValueDto? = null,
)

@JsonClass(generateAdapter = true)
data class IgInsightsResponseDto(
    val data: List<IgInsightItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class IgCommentRepliesWrapperDto(
    val data: List<IgCommentDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class IgCommentDto(
    val id: String,
    val text: String? = null,
    val timestamp: String? = null,
    val username: String? = null,
    val hidden: Boolean? = null,
    @Json(name = "like_count") val likeCount: Int? = null,
    val replies: IgCommentRepliesWrapperDto? = null,
)

@JsonClass(generateAdapter = true)
data class IgCommentListResponseDto(
    val data: List<IgCommentDto> = emptyList(),
    val paging: IgPagingDto? = null,
)

@JsonClass(generateAdapter = true)
data class IgContainerResponseDto(
    val id: String,
)

@JsonClass(generateAdapter = true)
data class IgContainerStatusResponseDto(
    val id: String? = null,
    @Json(name = "status_code") val statusCode: String? = null,
    val status: String? = null,
)

@JsonClass(generateAdapter = true)
data class IgSuccessResponseDto(
    val success: Boolean? = null,
    val id: String? = null,
)

@JsonClass(generateAdapter = true)
data class IgParticipantDto(
    val id: String,
    val username: String? = null,
    val name: String? = null,
)

@JsonClass(generateAdapter = true)
data class IgParticipantsWrapperDto(
    val data: List<IgParticipantDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class IgMessageDto(
    val id: String,
    val message: String? = null,
    @Json(name = "created_time") val createdTime: String? = null,
    val from: IgParticipantDto? = null,
)

@JsonClass(generateAdapter = true)
data class IgMessagesWrapperDto(
    val data: List<IgMessageDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class IgConversationDto(
    val id: String,
    @Json(name = "updated_time") val updatedTime: String? = null,
    val participants: IgParticipantsWrapperDto? = null,
    val messages: IgMessagesWrapperDto? = null,
)

@JsonClass(generateAdapter = true)
data class IgConversationsResponseDto(
    val data: List<IgConversationDto> = emptyList(),
    val paging: IgPagingDto? = null,
)

@JsonClass(generateAdapter = true)
data class IgSendMessageRecipientDto(
    val id: String,
)

@JsonClass(generateAdapter = true)
data class IgSendMessagePayloadDto(
    val text: String,
)

@JsonClass(generateAdapter = true)
data class IgSendMessageRequestDto(
    val recipient: IgSendMessageRecipientDto,
    val message: IgSendMessagePayloadDto,
)

@JsonClass(generateAdapter = true)
data class IgSendMessageResponseDto(
    @Json(name = "recipient_id") val recipientId: String? = null,
    @Json(name = "message_id") val messageId: String? = null,
)

@JsonClass(generateAdapter = true)
data class IgErrorBodyDto(
    val message: String? = null,
    val type: String? = null,
    val code: Int? = null,
    @Json(name = "error_subcode") val errorSubcode: Int? = null,
    @Json(name = "error_user_msg") val errorUserMsg: String? = null,
)

@JsonClass(generateAdapter = true)
data class IgErrorEnvelopeDto(
    val error: IgErrorBodyDto? = null,
    @Json(name = "error_message") val errorMessage: String? = null,
)

// ---------------------------------------------------------------------------
// Domain / Core Models (Mirrors Rust `instincta-core` UniFFI Records)
// ---------------------------------------------------------------------------

data class InstagramSession(
    val accessToken: String,
    val userId: String,
    val expiresAtEpochSec: Long,
) {
    val isExpired: Boolean
        get() = accessToken.isBlank() || (expiresAtEpochSec > 0L && System.currentTimeMillis() / 1000L >= expiresAtEpochSec)

    val daysRemaining: Long
        get() {
            if (expiresAtEpochSec <= 0L) return 0L
            val diff = expiresAtEpochSec - (System.currentTimeMillis() / 1000L)
            return (diff / 86_400L).coerceAtLeast(0L)
        }
}

data class OAuthAppConfig(
    val appId: String = "",
    val proxyBaseUrl: String = "",
    val redirectUri: String = DEFAULT_REDIRECT_URI,
) {
    val isConfiguredForBrowserOAuth: Boolean
        get() = appId.trim().let { it.length >= 8 && it.all(Char::isDigit) } &&
            proxyBaseUrl.trim().let { it.startsWith("https://") || it.startsWith("http://10.0.2.2") || it.startsWith("http://localhost") || it.startsWith("http://127.0.0.1") }

    companion object {
        const val DEFAULT_REDIRECT_URI = "instincta://oauth/callback"
    }
}

data class InstagramProfile(
    val id: String,
    val userId: String,
    val username: String,
    val name: String,
    val accountType: String,
    val profilePictureUrl: String?,
    val biography: String,
    val followersCount: Int,
    val followsCount: Int,
    val mediaCount: Int,
)

enum class IgMediaKind {
    IMAGE,
    VIDEO,
    CAROUSEL_ALBUM,
    ;

    companion object {
        fun fromWire(value: String?): IgMediaKind = when (value?.uppercase()) {
            "VIDEO", "REELS" -> VIDEO
            "CAROUSEL_ALBUM" -> CAROUSEL_ALBUM
            else -> IMAGE
        }
    }
}

data class InstagramMediaChild(
    val id: String,
    val kind: IgMediaKind,
    val mediaUrl: String?,
    val thumbnailUrl: String?,
) {
    val displayUrl: String?
        get() = thumbnailUrl ?: mediaUrl
}

data class InstagramMediaItem(
    val id: String,
    val caption: String,
    val kind: IgMediaKind,
    val mediaUrl: String?,
    val thumbnailUrl: String?,
    val permalink: String?,
    val timestamp: String,
    val likeCount: Int,
    val commentsCount: Int,
    val children: List<InstagramMediaChild>,
) {
    val gridThumbnailUrl: String?
        get() = when {
            kind == IgMediaKind.VIDEO -> thumbnailUrl ?: mediaUrl
            !mediaUrl.isNullOrBlank() -> mediaUrl
            else -> children.firstOrNull()?.displayUrl
        }

    val carouselMediaUrls: List<InstagramMediaChild>
        get() = if (children.isNotEmpty()) {
            children
        } else {
            listOf(
                InstagramMediaChild(
                    id = id,
                    kind = kind,
                    mediaUrl = mediaUrl,
                    thumbnailUrl = thumbnailUrl,
                ),
            )
        }
}

data class MediaGridPage(
    val items: List<InstagramMediaItem>,
    val nextCursor: String?,
)

data class MediaInsights(
    val mediaId: String,
    val reach: Long?,
    val views: Long?,
    val likes: Long?,
    val comments: Long?,
    val saved: Long?,
    val shares: Long?,
)

data class AccountInsights(
    val reach: Long?,
    val profileViews: Long?,
    val accountsEngaged: Long?,
    val rawMetrics: List<InsightMetricCard>,
)

data class InsightMetricCard(
    val key: String,
    val title: String,
    val value: Long,
    val description: String,
)

data class InstagramComment(
    val id: String,
    val text: String,
    val timestamp: String,
    val username: String,
    val hidden: Boolean,
    val likeCount: Int,
    val replies: List<InstagramComment>,
)

enum class PublishMode(val label: String) {
    PHOTO("Photo"),
    CAROUSEL("Carousel"),
    REEL("Reel"),
}

sealed interface PublishRequest {
    val caption: String

    data class SinglePhoto(
        val imageUrl: String,
        override val caption: String,
    ) : PublishRequest

    data class Carousel(
        val itemUrls: List<String>,
        override val caption: String,
    ) : PublishRequest

    data class Reel(
        val videoUrl: String,
        override val caption: String,
        val shareToFeed: Boolean,
    ) : PublishRequest
}

sealed interface PublishProgressState {
    data object Idle : PublishProgressState
    data class Working(val stepLabel: String) : PublishProgressState
    data class Published(val mediaId: String) : PublishProgressState
    data class Failed(val message: String) : PublishProgressState
}

data class InstagramParticipant(
    val id: String,
    val username: String,
    val name: String,
) {
    val displayHandle: String
        get() = if (username.isNotBlank()) "@$username" else name.ifBlank { id }
}

data class InstagramMessage(
    val id: String,
    val text: String,
    val createdTime: String,
    val senderId: String,
    val senderUsername: String,
    val isFromMe: Boolean,
)

data class InstagramConversation(
    val id: String,
    val updatedTime: String,
    val recipient: InstagramParticipant,
    val lastMessagePreview: String,
    val messages: List<InstagramMessage>,
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
