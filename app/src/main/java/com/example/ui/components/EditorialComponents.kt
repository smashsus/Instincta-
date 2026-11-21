package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.data.IgMediaKind
import com.example.data.InstagramComment
import com.example.data.InstagramMediaItem
import com.example.data.MediaInsights
import com.example.data.UiState
import com.example.ui.theme.EditorialCrimson
import com.example.ui.theme.EditorialGrey
import com.example.ui.theme.HairlineGrey
import com.example.ui.theme.InkBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftPlinth
import java.text.NumberFormat
import java.util.Locale

@Composable
fun EditorialDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 0.5.dp,
        color = HairlineGrey,
    )
}

@Composable
fun EditorialAvatar(
    imageUrl: String?,
    username: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "$username profile picture",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(0.5.dp, HairlineGrey, CircleShape),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(SoftPlinth)
                .border(0.5.dp, HairlineGrey, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = username.take(1).uppercase().ifEmpty { "I" },
                style = MaterialTheme.typography.titleMedium,
                color = InkBlack,
            )
        }
    }
}

@Composable
fun EditorialErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = EditorialCrimson,
        )
        OutlinedButton(
            onClick = onRetry,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, InkBlack),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("retry_button"),
        ) {
            Text("Retry", color = InkBlack)
        }
    }
}

fun formatCompactCount(count: Int): String = formatCompactCount(count.toLong())

fun formatCompactCount(count: Long): String {
    return when {
        count >= 1_000_000L -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 10_000L -> String.format(Locale.US, "%.1fK", count / 1_000.0)
        else -> NumberFormat.getIntegerInstance(Locale.US).format(count)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MediaDetailBottomSheet(
    media: InstagramMediaItem,
    insightsState: UiState<MediaInsights>?,
    onDismiss: () -> Unit,
    onOpenComments: (InstagramMediaItem) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val items = media.carouselMediaUrls
    val pagerState = rememberPagerState(pageCount = { items.size.coerceAtLeast(1) })

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        contentColor = InkBlack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.kind.name.replace('_', ' '),
                        style = MaterialTheme.typography.labelSmall,
                        color = EditorialGrey,
                    )
                    Text(
                        text = media.timestamp.take(10),
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialGrey,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_media_detail_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close detail",
                        tint = InkBlack,
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(SoftPlinth),
            ) { page ->
                val child = items[page]
                if (child.kind == IgMediaKind.VIDEO && !child.mediaUrl.isNullOrBlank()) {
                    InlineVideoPlayer(
                        videoUrl = child.mediaUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AsyncImage(
                        model = child.mediaUrl ?: child.thumbnailUrl,
                        contentDescription = media.caption.ifBlank { "Instagram media" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (items.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(items.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (pagerState.currentPage == index) 7.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) InkBlack else HairlineGrey,
                                ),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onOpenComments(media) },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, InkBlack),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("detail_open_comments_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Comment,
                        contentDescription = stringResource(R.string.cd_comment),
                        tint = InkBlack,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Comments (${media.commentsCount})",
                        color = InkBlack,
                    )
                }

                if (!media.permalink.isNullOrBlank()) {
                    IconButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, media.permalink)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Instagram post"))
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .border(0.5.dp, HairlineGrey, RoundedCornerShape(4.dp))
                            .testTag("detail_share_permalink_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.cd_share),
                            tint = InkBlack,
                        )
                    }

                    IconButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(media.permalink)))
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .border(0.5.dp, HairlineGrey, RoundedCornerShape(4.dp))
                            .testTag("detail_open_permalink_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInBrowser,
                            contentDescription = "Open on Instagram",
                            tint = InkBlack,
                        )
                    }
                }
            }

            if (media.caption.isNotBlank()) {
                Text(
                    text = media.caption,
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkBlack,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            EditorialDivider(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "MEDIA INSIGHTS",
                style = MaterialTheme.typography.labelSmall,
                color = EditorialGrey,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))

            when (insightsState) {
                null, UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = InkBlack)
                    }
                }

                is UiState.Error -> {
                    Text(
                        text = insightsState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialGrey,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }

                is UiState.Success -> {
                    val data = insightsState.data
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        InsightCell("Reach", data.reach?.let(::formatCompactCount) ?: "—", Modifier.weight(1f))
                        InsightCell("Views", data.views?.let(::formatCompactCount) ?: "—", Modifier.weight(1f))
                        InsightCell("Saved", data.saved?.let(::formatCompactCount) ?: "—", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        InsightCell("Likes", formatCompactCount((data.likes ?: media.likeCount.toLong())), Modifier.weight(1f))
                        InsightCell("Comments", formatCompactCount((data.comments ?: media.commentsCount.toLong())), Modifier.weight(1f))
                        InsightCell("Shares", data.shares?.let(::formatCompactCount) ?: "—", Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = SoftPlinth,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, HairlineGrey),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = InkBlack,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = EditorialGrey,
            )
        }
    }
}

@Composable
private fun InlineVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            VideoView(ctx).apply {
                val controller = MediaController(ctx)
                controller.setAnchorView(this)
                setMediaController(controller)
                setVideoURI(Uri.parse(videoUrl))
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    start()
                }
            }
        },
        update = { view ->
            if (view.tag != videoUrl) {
                view.tag = videoUrl
                view.setVideoURI(Uri.parse(videoUrl))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsModerationSheet(
    media: InstagramMediaItem,
    commentsState: UiState<List<InstagramComment>>,
    isActionPending: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onPostComment: (String) -> Unit,
    onReplyToComment: (commentId: String, message: String) -> Unit,
    onToggleHide: (InstagramComment) -> Unit,
    onDeleteComment: (commentId: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var composerText by rememberSaveable(media.id) { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<InstagramComment?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        contentColor = InkBlack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(540.dp)
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Comment Moderation",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkBlack,
                )
                TextButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_comments_button"),
                ) {
                    Text("Refresh", color = InkBlack)
                }
            }
            EditorialDivider()

            Box(modifier = Modifier.weight(1f)) {
                when (commentsState) {
                    UiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = InkBlack)
                        }
                    }

                    is UiState.Error -> {
                        EditorialErrorState(
                            message = commentsState.message,
                            onRetry = onRefresh,
                        )
                    }

                    is UiState.Success -> {
                        if (commentsState.data.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No comments on this post yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EditorialGrey,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                item { Spacer(Modifier.height(4.dp)) }
                                items(commentsState.data, key = { it.id }) { comment ->
                                    CommentModerationRow(
                                        comment = comment,
                                        isActionPending = isActionPending,
                                        onReplyClick = { replyingTo = comment },
                                        onToggleHide = { onToggleHide(comment) },
                                        onDelete = { onDeleteComment(comment.id) },
                                    )
                                }
                                item { Spacer(Modifier.height(8.dp)) }
                            }
                        }
                    }
                }
            }

            EditorialDivider()
            if (replyingTo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Replying to @${replyingTo?.username}",
                        style = MaterialTheme.typography.labelMedium,
                        color = EditorialGrey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelMedium,
                        color = EditorialCrimson,
                        modifier = Modifier
                            .clickable { replyingTo = null }
                            .padding(4.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = composerText,
                    onValueChange = { composerText = it },
                    placeholder = {
                        Text(
                            if (replyingTo != null) "Write a reply…" else "Add a comment…",
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_composer_input"),
                )
                IconButton(
                    onClick = {
                        val target = replyingTo
                        if (target != null) {
                            onReplyToComment(target.id, composerText)
                            replyingTo = null
                        } else {
                            onPostComment(composerText)
                        }
                        composerText = ""
                    },
                    enabled = composerText.isNotBlank() && !isActionPending,
                    modifier = Modifier
                        .size(48.dp)
                        .background(InkBlack, RoundedCornerShape(4.dp))
                        .testTag("send_comment_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send comment",
                        tint = PureWhite,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentModerationRow(
    comment: InstagramComment,
    isActionPending: Boolean,
    onReplyClick: () -> Unit,
    onToggleHide: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (comment.hidden) SoftPlinth else PureWhite, RoundedCornerShape(4.dp))
            .border(0.5.dp, HairlineGrey, RoundedCornerShape(4.dp))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "@${comment.username}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = InkBlack,
            )
            if (comment.hidden) {
                Text(
                    text = "HIDDEN",
                    style = MaterialTheme.typography.labelSmall,
                    color = EditorialCrimson,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = comment.text,
            style = MaterialTheme.typography.bodyMedium,
            color = InkBlack,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(enabled = !isActionPending, onClick = onReplyClick)
                    .testTag("reply_comment_${comment.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Reply,
                    contentDescription = "Reply to comment",
                    tint = EditorialGrey,
                    modifier = Modifier.size(16.dp),
                )
                Text("Reply", style = MaterialTheme.typography.labelMedium, color = EditorialGrey)
            }

            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(enabled = !isActionPending, onClick = onToggleHide)
                    .testTag("toggle_hide_comment_${comment.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = if (comment.hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = if (comment.hidden) "Unhide comment" else "Hide comment",
                    tint = EditorialGrey,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = if (comment.hidden) "Unhide" else "Hide",
                    style = MaterialTheme.typography.labelMedium,
                    color = EditorialGrey,
                )
            }

            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(enabled = !isActionPending, onClick = onDelete)
                    .testTag("delete_comment_${comment.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete comment",
                    tint = EditorialCrimson,
                    modifier = Modifier.size(16.dp),
                )
                Text("Delete", style = MaterialTheme.typography.labelMedium, color = EditorialCrimson)
            }
        }

        if (comment.replies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                comment.replies.forEach { reply ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, HairlineGrey, RoundedCornerShape(4.dp))
                            .padding(8.dp),
                    ) {
                        Text(
                            text = "@${reply.username}",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkBlack,
                        )
                        Text(
                            text = reply.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialGrey,
                        )
                    }
                }
            }
        }
    }
}
