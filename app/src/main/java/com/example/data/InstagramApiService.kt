package com.example.data

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface InstagramAuthProxyApi {
    @POST
    suspend fun exchangeCodeViaProxy(
        @Url exchangeUrl: String,
        @Body payload: ProxyCodeExchangeRequestDto,
    ): ProxyCodeExchangeResponseDto
}

interface InstagramGraphApi {
    @GET("refresh_access_token")
    suspend fun refreshLongLivedToken(
        @Query("grant_type") grantType: String = "ig_refresh_token",
        @Query("access_token") accessToken: String,
    ): LongLivedTokenResponseDto

    @GET("v22.0/me")
    suspend fun getProfile(
        @Query("fields") fields: String = PROFILE_FIELDS,
        @Query("access_token") accessToken: String,
    ): IgProfileDto

    @GET("v22.0/me/media")
    suspend fun getMedia(
        @Query("fields") fields: String = MEDIA_FIELDS,
        @Query("limit") limit: Int = 24,
        @Query("after") after: String? = null,
        @Query("access_token") accessToken: String,
    ): IgMediaListResponseDto

    @GET("v22.0/{mediaId}/insights")
    suspend fun getMediaInsights(
        @Path("mediaId") mediaId: String,
        @Query("metric") metric: String,
        @Query("access_token") accessToken: String,
    ): IgInsightsResponseDto

    @GET("v22.0/me/insights")
    suspend fun getAccountInsights(
        @Query("metric") metric: String,
        @Query("period") period: String = "day",
        @Query("metric_type") metricType: String = "total_value",
        @Query("access_token") accessToken: String,
    ): IgInsightsResponseDto

    @GET("v22.0/{mediaId}/comments")
    suspend fun getComments(
        @Path("mediaId") mediaId: String,
        @Query("fields") fields: String = COMMENT_FIELDS,
        @Query("access_token") accessToken: String,
    ): IgCommentListResponseDto

    @FormUrlEncoded
    @POST("v22.0/{mediaId}/comments")
    suspend fun postComment(
        @Path("mediaId") mediaId: String,
        @Field("message") message: String,
        @Field("access_token") accessToken: String,
    ): IgContainerResponseDto

    @FormUrlEncoded
    @POST("v22.0/{commentId}/replies")
    suspend fun replyToComment(
        @Path("commentId") commentId: String,
        @Field("message") message: String,
        @Field("access_token") accessToken: String,
    ): IgContainerResponseDto

    @FormUrlEncoded
    @POST("v22.0/{commentId}")
    suspend fun setCommentHidden(
        @Path("commentId") commentId: String,
        @Field("hide") hide: Boolean,
        @Field("access_token") accessToken: String,
    ): IgSuccessResponseDto

    @DELETE("v22.0/{commentId}")
    suspend fun deleteComment(
        @Path("commentId") commentId: String,
        @Query("access_token") accessToken: String,
    ): IgSuccessResponseDto

    @FormUrlEncoded
    @POST("v22.0/me/media")
    suspend fun createMediaContainer(
        @Field("image_url") imageUrl: String? = null,
        @Field("video_url") videoUrl: String? = null,
        @Field("media_type") mediaType: String? = null,
        @Field("caption") caption: String? = null,
        @Field("is_carousel_item") isCarouselItem: Boolean? = null,
        @Field("children") children: String? = null,
        @Field("share_to_feed") shareToFeed: Boolean? = null,
        @Field("access_token") accessToken: String,
    ): IgContainerResponseDto

    @GET("v22.0/{creationId}")
    suspend fun getContainerStatus(
        @Path("creationId") creationId: String,
        @Query("fields") fields: String = "id,status_code,status",
        @Query("access_token") accessToken: String,
    ): IgContainerStatusResponseDto

    @FormUrlEncoded
    @POST("v22.0/me/media_publish")
    suspend fun publishContainer(
        @Field("creation_id") creationId: String,
        @Field("access_token") accessToken: String,
    ): IgContainerResponseDto

    @GET("v22.0/me/conversations")
    suspend fun getConversations(
        @Query("platform") platform: String = "instagram",
        @Query("fields") fields: String = CONVERSATION_FIELDS,
        @Query("access_token") accessToken: String,
    ): IgConversationsResponseDto

    @GET("v22.0/{conversationId}")
    suspend fun getConversationThread(
        @Path("conversationId") conversationId: String,
        @Query("fields") fields: String = CONVERSATION_FIELDS,
        @Query("access_token") accessToken: String,
    ): IgConversationDto

    @POST("v22.0/me/messages")
    suspend fun sendDirectMessage(
        @Query("access_token") accessToken: String,
        @Body payload: IgSendMessageRequestDto,
    ): IgSendMessageResponseDto

    companion object {
        const val PROFILE_FIELDS =
            "id,user_id,username,name,account_type,profile_picture_url,biography,followers_count,follows_count,media_count"
        const val MEDIA_FIELDS =
            "id,caption,media_type,media_url,thumbnail_url,permalink,timestamp,like_count,comments_count,children{id,media_type,media_url,thumbnail_url}"
        const val COMMENT_FIELDS =
            "id,text,timestamp,username,hidden,like_count,replies{id,text,timestamp,username,hidden,like_count}"
        const val CONVERSATION_FIELDS =
            "id,updated_time,participants{id,username,name},messages{id,message,created_time,from{id,username,name}}"
    }
}

object InstagramApiFactory {
    private val moshi: Moshi = Moshi.Builder().build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            },
        )
        .build()

    val authProxyApi: InstagramAuthProxyApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://graph.instagram.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(InstagramAuthProxyApi::class.java)
    }

    val graphApi: InstagramGraphApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://graph.instagram.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(InstagramGraphApi::class.java)
    }

    fun parseApiError(throwable: Throwable): String {
        if (throwable is HttpException) {
            val body: ResponseBody? = throwable.response()?.errorBody()
            val raw = runCatching { body?.string() }.getOrNull()
            if (!raw.isNullOrBlank()) {
                val parsed = runCatching {
                    moshi.adapter(IgErrorEnvelopeDto::class.java).fromJson(raw)
                }.getOrNull()
                val msg = parsed?.error?.errorUserMsg
                    ?: parsed?.error?.message
                    ?: parsed?.errorMessage
                if (!msg.isNullOrBlank()) {
                    val code = parsed?.error?.code
                    return if (code != null) "Meta API ($code): $msg" else msg
                }
            }
            return "HTTP ${throwable.code()} ${throwable.message()}"
        }
        return throwable.localizedMessage ?: "Unexpected network error"
    }
}
