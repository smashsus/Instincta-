package com.example

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.InstagramAuthScreen
import com.example.data.KotlinInstagramCore
import com.example.data.SessionStore
import com.example.ui.components.CommentsModerationSheet
import com.example.ui.components.EditorialDivider
import com.example.ui.components.MediaDetailBottomSheet
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.GridScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.theme.EditorialGrey
import com.example.ui.theme.HairlineGrey
import com.example.ui.theme.InkBlack
import com.example.ui.theme.InstinctaTheme
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.InstinctaClientViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object InstinctaRoutes {
    const val GRID = "grid"
    const val INSIGHTS = "insights"
    const val PUBLISH = "publish"
    const val INBOX = "inbox"
    const val ACCOUNT = "account"
}

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

class MainActivity : ComponentActivity() {
    private val pendingOAuthUri = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.WHITE, AndroidColor.WHITE),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.WHITE, AndroidColor.WHITE),
        )
        setContent {
            InstinctaTheme {
                val uri by pendingOAuthUri.asStateFlow().collectAsStateWithLifecycle()
                InstinctaApp(
                    incomingOAuthUri = uri,
                    onConsumedOAuthUri = { pendingOAuthUri.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "instincta" && data.host == "oauth") {
            pendingOAuthUri.value = data
        }
    }
}

@Composable
private fun InstinctaApp(
    incomingOAuthUri: Uri?,
    onConsumedOAuthUri: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val core = remember(context) {
        KotlinInstagramCore(sessionStore = SessionStore(context))
    }
    val viewModel: InstinctaClientViewModel = viewModel(
        factory = InstinctaClientViewModel.Factory(core),
    )

    LaunchedEffect(incomingOAuthUri) {
        if (incomingOAuthUri != null) {
            viewModel.handleOAuthRedirectUri(incomingOAuthUri)
            onConsumedOAuthUri()
        }
    }

    val sessionLoaded by viewModel.sessionLoaded.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val oauthConfig by viewModel.oauthConfig.collectAsStateWithLifecycle()
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    if (!sessionLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureWhite),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = InkBlack)
        }
        return
    }

    val activeSession = session
    if (activeSession == null || activeSession.isExpired) {
        InstagramAuthScreen(
            oauthConfig = oauthConfig,
            isAuthenticating = isAuthenticating,
            authError = authError,
            onConnectWithAccessToken = { viewModel.connectWithAccessToken(it) },
            onSaveOAuthConfig = { appId, proxyUrl -> viewModel.saveOAuthConfig(appId, proxyUrl) },
            onBuildAuthorizeUrl = { appId -> viewModel.buildAuthorizeUrl(appId) },
            onSubmitAuthCodeOrUrl = { viewModel.submitManualAuthCodeOrUrl(it) },
            onDismissError = { viewModel.clearAuthError() },
        )
        return
    }

    InstinctaClientShell(viewModel = viewModel)
}

@Composable
private fun InstinctaClientShell(viewModel: InstinctaClientViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: InstinctaRoutes.GRID

    val session by viewModel.session.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val mediaGridState by viewModel.mediaGridState.collectAsStateWithLifecycle()
    val nextMediaCursor by viewModel.nextMediaCursor.collectAsStateWithLifecycle()
    val isLoadingMoreMedia by viewModel.isLoadingMoreMedia.collectAsStateWithLifecycle()
    val accountInsightsState by viewModel.accountInsightsState.collectAsStateWithLifecycle()
    val perMediaInsights by viewModel.perMediaInsights.collectAsStateWithLifecycle()
    val selectedMediaForDetail by viewModel.selectedMediaForDetail.collectAsStateWithLifecycle()
    val selectedMediaForComments by viewModel.selectedMediaForComments.collectAsStateWithLifecycle()
    val commentsState by viewModel.commentsState.collectAsStateWithLifecycle()
    val isCommentActionPending by viewModel.isCommentActionPending.collectAsStateWithLifecycle()
    val publishProgress by viewModel.publishProgress.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val activeThreadState by viewModel.activeThreadState.collectAsStateWithLifecycle()
    val isSendingMessage by viewModel.isSendingMessage.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeSnackbar()
    }

    val navTabs = remember {
        listOf(
            NavTab(InstinctaRoutes.GRID, R.string.nav_grid, Icons.Filled.GridView, Icons.Outlined.GridView, "nav_grid"),
            NavTab(InstinctaRoutes.INSIGHTS, R.string.nav_insights, Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_insights"),
            NavTab(InstinctaRoutes.PUBLISH, R.string.nav_create, Icons.Filled.AddBox, Icons.Outlined.AddBox, "nav_publish"),
            NavTab(InstinctaRoutes.INBOX, R.string.nav_messages, Icons.Filled.Forum, Icons.Outlined.Forum, "nav_inbox"),
            NavTab(InstinctaRoutes.ACCOUNT, R.string.nav_account, Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, "nav_account"),
        )
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite),
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = PureWhite,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    CompactEditorialBottomBar(
                        tabs = navTabs,
                        currentRoute = currentRoute,
                        onSelectTab = { tab -> navigateToTab(tab.route) },
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = PureWhite,
                        contentColor = InkBlack,
                        modifier = Modifier
                            .fillMaxHeight()
                            .navigationBarsPadding(),
                    ) {
                        navTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = InkBlack,
                                    selectedTextColor = InkBlack,
                                    unselectedIconColor = EditorialGrey,
                                    unselectedTextColor = EditorialGrey,
                                    indicatorColor = PureWhite,
                                ),
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                    VerticalDivider(thickness = 0.5.dp, color = HairlineGrey)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(PureWhite),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = InstinctaRoutes.GRID,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp),
                    ) {
                        composable(InstinctaRoutes.GRID) {
                            GridScreen(
                                profileState = profileState,
                                mediaGridState = mediaGridState,
                                hasMoreMedia = nextMediaCursor != null,
                                isLoadingMore = isLoadingMoreMedia,
                                onRefresh = {
                                    viewModel.refreshProfile()
                                    viewModel.refreshMediaGrid()
                                },
                                onLoadMore = { viewModel.loadMoreMedia() },
                                onMediaClick = { media -> viewModel.openMediaDetail(media) },
                            )
                        }

                        composable(InstinctaRoutes.INSIGHTS) {
                            InsightsScreen(
                                accountInsightsState = accountInsightsState,
                                mediaGridState = mediaGridState,
                                perMediaInsights = perMediaInsights,
                                onRefreshInsights = {
                                    viewModel.refreshAccountInsights()
                                    viewModel.refreshMediaGrid()
                                },
                                onLoadMediaInsights = { media -> viewModel.loadInsightsForMedia(media) },
                                onMediaClick = { media -> viewModel.openMediaDetail(media) },
                            )
                        }

                        composable(InstinctaRoutes.PUBLISH) {
                            CreateScreen(
                                publishProgress = publishProgress,
                                onPublish = { request ->
                                    viewModel.publishMedia(request) {
                                        navigateToTab(InstinctaRoutes.GRID)
                                    }
                                },
                                onResetPublishState = { viewModel.resetPublishState() },
                            )
                        }

                        composable(InstinctaRoutes.INBOX) {
                            InboxScreen(
                                conversationsState = conversationsState,
                                activeThreadState = activeThreadState,
                                isSendingMessage = isSendingMessage,
                                onRefreshConversations = { viewModel.refreshConversations() },
                                onOpenConversation = { convo -> viewModel.openConversation(convo) },
                                onRefreshThread = { id -> viewModel.refreshConversationThread(id) },
                                onCloseConversation = { viewModel.closeConversation() },
                                onSendDirectMessage = { convoId, recipientId, text ->
                                    viewModel.sendDirectMessage(convoId, recipientId, text)
                                },
                            )
                        }

                        composable(InstinctaRoutes.ACCOUNT) {
                            AccountScreen(
                                session = session,
                                profileState = profileState,
                                onRefreshSessionToken = { viewModel.refreshSessionToken() },
                                onRefreshProfile = { viewModel.refreshProfile() },
                                onSignOut = { viewModel.signOut() },
                            )
                        }
                    }
                }
            }
        }
    }

    val detailMedia = selectedMediaForDetail
    if (detailMedia != null) {
        MediaDetailBottomSheet(
            media = detailMedia,
            insightsState = perMediaInsights[detailMedia.id],
            onDismiss = { viewModel.closeMediaDetail() },
            onOpenComments = { media ->
                viewModel.openCommentsSheet(media)
            },
        )
    }

    val commentsMedia = selectedMediaForComments
    if (commentsMedia != null) {
        CommentsModerationSheet(
            media = commentsMedia,
            commentsState = commentsState,
            isActionPending = isCommentActionPending,
            onDismiss = { viewModel.closeCommentsSheet() },
            onRefresh = { viewModel.loadComments(commentsMedia.id) },
            onPostComment = { msg -> viewModel.postComment(commentsMedia.id, msg) },
            onReplyToComment = { commentId, msg ->
                viewModel.replyToComment(commentsMedia.id, commentId, msg)
            },
            onToggleHide = { comment ->
                viewModel.toggleHideComment(commentsMedia.id, comment)
            },
            onDeleteComment = { commentId ->
                viewModel.deleteComment(commentsMedia.id, commentId)
            },
        )
    }
}

@Composable
private fun CompactEditorialBottomBar(
    tabs: List<NavTab>,
    currentRoute: String,
    onSelectTab: (NavTab) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureWhite)
            .navigationBarsPadding(),
    ) {
        EditorialDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clickable(
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false),
                            onClick = { onSelectTab(tab) },
                        )
                        .testTag(tab.testTag),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.labelRes),
                        tint = if (selected) InkBlack else EditorialGrey,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
