package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AccountInsights
import com.example.data.InstagramComment
import com.example.data.InstagramConversation
import com.example.data.InstagramCorePort
import com.example.data.InstagramMediaItem
import com.example.data.InstagramProfile
import com.example.data.InstagramSession
import com.example.data.MediaInsights
import com.example.data.OAuthAppConfig
import com.example.data.PublishProgressState
import com.example.data.PublishRequest
import com.example.data.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InstinctaClientViewModel(
    private val core: InstagramCorePort,
) : ViewModel() {

    private val _session = MutableStateFlow<InstagramSession?>(null)
    val session: StateFlow<InstagramSession?> = _session.asStateFlow()

    private val _sessionLoaded = MutableStateFlow(false)
    val sessionLoaded: StateFlow<Boolean> = _sessionLoaded.asStateFlow()

    private val _oauthConfig = MutableStateFlow(OAuthAppConfig())
    val oauthConfig: StateFlow<OAuthAppConfig> = _oauthConfig.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _profileState = MutableStateFlow<UiState<InstagramProfile>>(UiState.Loading)
    val profileState: StateFlow<UiState<InstagramProfile>> = _profileState.asStateFlow()

    private val _mediaGridState = MutableStateFlow<UiState<List<InstagramMediaItem>>>(UiState.Loading)
    val mediaGridState: StateFlow<UiState<List<InstagramMediaItem>>> = _mediaGridState.asStateFlow()

    private val _nextMediaCursor = MutableStateFlow<String?>(null)
    val nextMediaCursor: StateFlow<String?> = _nextMediaCursor.asStateFlow()

    private val _isLoadingMoreMedia = MutableStateFlow(false)
    val isLoadingMoreMedia: StateFlow<Boolean> = _isLoadingMoreMedia.asStateFlow()

    private val _accountInsightsState = MutableStateFlow<UiState<AccountInsights>>(UiState.Loading)
    val accountInsightsState: StateFlow<UiState<AccountInsights>> = _accountInsightsState.asStateFlow()

    private val _perMediaInsights = MutableStateFlow<Map<String, UiState<MediaInsights>>>(emptyMap())
    val perMediaInsights: StateFlow<Map<String, UiState<MediaInsights>>> = _perMediaInsights.asStateFlow()

    private val _selectedMediaForDetail = MutableStateFlow<InstagramMediaItem?>(null)
    val selectedMediaForDetail: StateFlow<InstagramMediaItem?> = _selectedMediaForDetail.asStateFlow()

    private val _selectedMediaForComments = MutableStateFlow<InstagramMediaItem?>(null)
    val selectedMediaForComments: StateFlow<InstagramMediaItem?> = _selectedMediaForComments.asStateFlow()

    private val _commentsState = MutableStateFlow<UiState<List<InstagramComment>>>(UiState.Loading)
    val commentsState: StateFlow<UiState<List<InstagramComment>>> = _commentsState.asStateFlow()

    private val _isCommentActionPending = MutableStateFlow(false)
    val isCommentActionPending: StateFlow<Boolean> = _isCommentActionPending.asStateFlow()

    private val _publishProgress = MutableStateFlow<PublishProgressState>(PublishProgressState.Idle)
    val publishProgress: StateFlow<PublishProgressState> = _publishProgress.asStateFlow()

    private val _conversationsState = MutableStateFlow<UiState<List<InstagramConversation>>>(UiState.Loading)
    val conversationsState: StateFlow<UiState<List<InstagramConversation>>> = _conversationsState.asStateFlow()

    private val _activeThreadState = MutableStateFlow<UiState<InstagramConversation>?>(null)
    val activeThreadState: StateFlow<UiState<InstagramConversation>?> = _activeThreadState.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var lastLoadedToken: String? = null

    init {
        viewModelScope.launch {
            core.oauthConfigFlow.collectLatest { cfg ->
                _oauthConfig.value = cfg
            }
        }
        viewModelScope.launch {
            core.sessionFlow.collectLatest { incoming ->
                _session.value = incoming
                _sessionLoaded.value = true
                if (incoming != null && !incoming.isExpired && incoming.accessToken != lastLoadedToken) {
                    lastLoadedToken = incoming.accessToken
                    refreshAll()
                } else if (incoming == null) {
                    lastLoadedToken = null
                }
            }
        }
    }

    fun buildAuthorizeUrl(appId: String): String = core.buildAuthorizeUrl(appId)

    fun saveOAuthConfig(appId: String, proxyBaseUrl: String) {
        viewModelScope.launch {
            core.saveOAuthConfig(appId, proxyBaseUrl)
        }
    }

    fun connectWithAccessToken(accessToken: String) {
        if (_isAuthenticating.value) return
        viewModelScope.launch {
            _isAuthenticating.value = true
            _authError.value = null
            core.connectWithAccessToken(accessToken)
                .onSuccess {
                    _snackbarMessage.value = "Connected to Instagram."
                }
                .onFailure { err ->
                    _authError.value = err.message ?: "Could not verify Instagram token."
                }
            _isAuthenticating.value = false
        }
    }

    fun handleOAuthRedirectUri(uri: Uri?) {
        if (uri == null) return
        val errorDesc = uri.getQueryParameter("error_description") ?: uri.getQueryParameter("error")
        if (!errorDesc.isNullOrBlank()) {
            _authError.value = "Instagram authorization failed: $errorDesc"
            return
        }
        val code = uri.getQueryParameter("code")
        if (!code.isNullOrBlank()) {
            submitManualAuthCodeOrUrl(code)
        }
    }

    fun submitManualAuthCodeOrUrl(rawInput: String) {
        val code = core.extractAuthCode(rawInput)
        if (code == null) {
            _authError.value = "Enter a valid Instagram authorization code or callback URL."
            return
        }
        viewModelScope.launch {
            _isAuthenticating.value = true
            _authError.value = null
            core.exchangeCodeForSession(code)
                .onSuccess {
                    _snackbarMessage.value = "Connected to Instagram."
                }
                .onFailure { err ->
                    _authError.value = err.message ?: "Token exchange via OAuth proxy failed."
                }
            _isAuthenticating.value = false
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun refreshSessionToken() {
        viewModelScope.launch {
            core.refreshSession()
                .onSuccess {
                    _snackbarMessage.value = "Long-lived token refreshed (${it.daysRemaining}d remaining)."
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Could not refresh token."
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            core.logout()
            _selectedMediaForDetail.value = null
            _selectedMediaForComments.value = null
            _activeThreadState.value = null
            _perMediaInsights.value = emptyMap()
            _publishProgress.value = PublishProgressState.Idle
            _snackbarMessage.value = "Signed out."
        }
    }

    fun refreshAll() {
        refreshProfile()
        refreshMediaGrid()
        refreshAccountInsights()
        refreshConversations()
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _profileState.value = UiState.Loading
            core.getProfile()
                .onSuccess { _profileState.value = UiState.Success(it) }
                .onFailure { _profileState.value = UiState.Error(it.message ?: "Failed to load profile.") }
        }
    }

    fun refreshMediaGrid() {
        viewModelScope.launch {
            _mediaGridState.value = UiState.Loading
            core.getMediaGrid(afterCursor = null)
                .onSuccess { page ->
                    _mediaGridState.value = UiState.Success(page.items)
                    _nextMediaCursor.value = page.nextCursor
                }
                .onFailure { err ->
                    _mediaGridState.value = UiState.Error(err.message ?: "Failed to load Instagram media.")
                }
        }
    }

    fun loadMoreMedia() {
        val cursor = _nextMediaCursor.value ?: return
        if (_isLoadingMoreMedia.value) return
        viewModelScope.launch {
            _isLoadingMoreMedia.value = true
            core.getMediaGrid(afterCursor = cursor)
                .onSuccess { page ->
                    val existing = (_mediaGridState.value as? UiState.Success)?.data.orEmpty()
                    val combined = (existing + page.items).distinctBy { it.id }
                    _mediaGridState.value = UiState.Success(combined)
                    _nextMediaCursor.value = page.nextCursor
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Could not load older posts."
                }
            _isLoadingMoreMedia.value = false
        }
    }

    fun refreshAccountInsights() {
        viewModelScope.launch {
            _accountInsightsState.value = UiState.Loading
            core.getAccountInsights()
                .onSuccess { _accountInsightsState.value = UiState.Success(it) }
                .onFailure { err ->
                    _accountInsightsState.value = UiState.Error(
                        err.message ?: "Account insights require Creator/Business insights permission.",
                    )
                }
        }
    }

    fun loadInsightsForMedia(media: InstagramMediaItem) {
        if (_perMediaInsights.value[media.id] is UiState.Success) return
        viewModelScope.launch {
            _perMediaInsights.update { it + (media.id to UiState.Loading) }
            core.getMediaInsights(media.id, media.kind)
                .onSuccess { insights ->
                    _perMediaInsights.update { it + (media.id to UiState.Success(insights)) }
                }
                .onFailure { err ->
                    _perMediaInsights.update {
                        it + (media.id to UiState.Error(err.message ?: "Insights unavailable."))
                    }
                }
        }
    }

    fun openMediaDetail(media: InstagramMediaItem) {
        _selectedMediaForDetail.value = media
        loadInsightsForMedia(media)
    }

    fun closeMediaDetail() {
        _selectedMediaForDetail.value = null
    }

    fun openCommentsSheet(media: InstagramMediaItem) {
        _selectedMediaForComments.value = media
        loadComments(media.id)
    }

    fun closeCommentsSheet() {
        _selectedMediaForComments.value = null
    }

    fun loadComments(mediaId: String) {
        viewModelScope.launch {
            _commentsState.value = UiState.Loading
            core.getComments(mediaId)
                .onSuccess { _commentsState.value = UiState.Success(it) }
                .onFailure { err ->
                    _commentsState.value = UiState.Error(err.message ?: "Could not load comments.")
                }
        }
    }

    fun postComment(mediaId: String, message: String) {
        if (message.isBlank() || _isCommentActionPending.value) return
        viewModelScope.launch {
            _isCommentActionPending.value = true
            core.postComment(mediaId, message)
                .onSuccess {
                    _snackbarMessage.value = "Comment posted."
                    loadComments(mediaId)
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Could not post comment."
                }
            _isCommentActionPending.value = false
        }
    }

    fun replyToComment(mediaId: String, commentId: String, message: String) {
        if (message.isBlank() || _isCommentActionPending.value) return
        viewModelScope.launch {
            _isCommentActionPending.value = true
            core.replyToComment(commentId, message)
                .onSuccess {
                    _snackbarMessage.value = "Reply sent."
                    loadComments(mediaId)
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Could not post reply."
                }
            _isCommentActionPending.value = false
        }
    }

    fun toggleHideComment(mediaId: String, comment: InstagramComment) {
        if (_isCommentActionPending.value) return
        viewModelScope.launch {
            _isCommentActionPending.value = true
            core.setCommentHidden(comment.id, hide = !comment.hidden)
                .onSuccess {
                    _snackbarMessage.value = if (comment.hidden) "Comment unhidden." else "Comment hidden."
                    loadComments(mediaId)
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Could not update comment visibility."
                }
            _isCommentActionPending.value = false
        }
    }

    fun deleteComment(mediaId: String, commentId: String) {
        if (_isCommentActionPending.value) return
        viewModelScope.launch {
            _isCommentActionPending.value = true
            core.deleteComment(commentId)
                .onSuccess {
                    _snackbarMessage.value = "Comment deleted."
                    loadComments(mediaId)
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Could not delete comment."
                }
            _isCommentActionPending.value = false
        }
    }

    fun publishMedia(request: PublishRequest, onCompleteNavigateToGrid: () -> Unit) {
        viewModelScope.launch {
            _publishProgress.value = PublishProgressState.Working("Starting Meta container workflow…")
            core.publishMedia(request) { step ->
                _publishProgress.value = PublishProgressState.Working(step)
            }.onSuccess { mediaId ->
                _publishProgress.value = PublishProgressState.Published(mediaId)
                _snackbarMessage.value = "Published to Instagram."
                refreshMediaGrid()
                refreshProfile()
                onCompleteNavigateToGrid()
            }.onFailure { err ->
                _publishProgress.value = PublishProgressState.Failed(
                    err.message ?: "Publishing failed.",
                )
            }
        }
    }

    fun resetPublishState() {
        _publishProgress.value = PublishProgressState.Idle
    }

    fun refreshConversations() {
        viewModelScope.launch {
            _conversationsState.value = UiState.Loading
            val myUserId = currentUserId()
            core.getConversations(myUserId)
                .onSuccess { _conversationsState.value = UiState.Success(it) }
                .onFailure { err ->
                    _conversationsState.value = UiState.Error(
                        err.message ?: "Direct messages require Instagram Messaging permission.",
                    )
                }
        }
    }

    fun openConversation(conversation: InstagramConversation) {
        _activeThreadState.value = UiState.Success(conversation)
        refreshConversationThread(conversation.id)
    }

    fun refreshConversationThread(conversationId: String) {
        viewModelScope.launch {
            val myUserId = currentUserId()
            core.getConversationThread(conversationId, myUserId)
                .onSuccess { _activeThreadState.value = UiState.Success(it) }
                .onFailure { err ->
                    if (_activeThreadState.value == null) {
                        _activeThreadState.value = UiState.Error(err.message ?: "Unable to load thread.")
                    } else {
                        _snackbarMessage.value = err.message ?: "Could not refresh thread."
                    }
                }
        }
    }

    fun closeConversation() {
        _activeThreadState.value = null
    }

    fun sendDirectMessage(conversationId: String, recipientId: String, text: String) {
        if (text.isBlank() || _isSendingMessage.value) return
        viewModelScope.launch {
            _isSendingMessage.value = true
            core.sendDirectMessage(recipientId, text)
                .onSuccess {
                    refreshConversationThread(conversationId)
                    refreshConversations()
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Failed to send message."
                }
            _isSendingMessage.value = false
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    private fun currentUserId(): String {
        val profile = (_profileState.value as? UiState.Success)?.data
        return profile?.userId ?: _session.value?.userId.orEmpty()
    }

    class Factory(
        private val core: InstagramCorePort,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InstinctaClientViewModel(core) as T
        }
    }
}
