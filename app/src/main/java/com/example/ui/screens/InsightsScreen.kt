package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AccountInsights
import com.example.data.InstagramMediaItem
import com.example.data.MediaInsights
import com.example.data.UiState
import com.example.ui.components.EditorialDivider
import com.example.ui.components.EditorialErrorState
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.EditorialGrey
import com.example.ui.theme.HairlineGrey
import com.example.ui.theme.InkBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftPlinth

@Composable
fun InsightsScreen(
    accountInsightsState: UiState<AccountInsights>,
    mediaGridState: UiState<List<InstagramMediaItem>>,
    perMediaInsights: Map<String, UiState<MediaInsights>>,
    onRefreshInsights: () -> Unit,
    onLoadMediaInsights: (InstagramMediaItem) -> Unit,
    onMediaClick: (InstagramMediaItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkBlack,
                )
                Text(
                    text = "LIVE GRAPH API ENGAGEMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = EditorialGrey,
                )
            }
            IconButton(
                onClick = onRefreshInsights,
                modifier = Modifier.testTag("insights_refresh_button"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.cd_refresh),
                    tint = InkBlack,
                )
            }
        }
        EditorialDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = "ACCOUNT OVERVIEW (DAY)",
                    style = MaterialTheme.typography.labelSmall,
                    color = EditorialGrey,
                )
                Spacer(modifier = Modifier.height(8.dp))
                when (accountInsightsState) {
                    UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = InkBlack, modifier = Modifier.size(24.dp))
                        }
                    }

                    is UiState.Error -> {
                        EditorialErrorState(
                            message = accountInsightsState.message,
                            onRetry = onRefreshInsights,
                        )
                    }

                    is UiState.Success -> {
                        val data = accountInsightsState.data
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            AccountMetricCard(
                                label = "Reach",
                                value = data.reach?.let(::formatCompactCount) ?: "—",
                                modifier = Modifier.weight(1f),
                            )
                            AccountMetricCard(
                                label = "Profile Views",
                                value = data.profileViews?.let(::formatCompactCount) ?: "—",
                                modifier = Modifier.weight(1f),
                            )
                            AccountMetricCard(
                                label = "Engaged",
                                value = data.accountsEngaged?.let(::formatCompactCount) ?: "—",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                EditorialDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "POST PERFORMANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = EditorialGrey,
                )
            }

            when (mediaGridState) {
                UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = InkBlack)
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        EditorialErrorState(
                            message = mediaGridState.message,
                            onRetry = onRefreshInsights,
                        )
                    }
                }

                is UiState.Success -> {
                    val items = mediaGridState.data
                    if (items.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Analytics,
                                    contentDescription = null,
                                    tint = EditorialGrey,
                                )
                                Text(
                                    text = "No posts available for per-media insights.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EditorialGrey,
                                )
                            }
                        }
                    } else {
                        items(items, key = { it.id }) { media ->
                            LaunchedEffect(media.id) {
                                onLoadMediaInsights(media)
                            }
                            MediaInsightRow(
                                media = media,
                                insightsState = perMediaInsights[media.id],
                                onClick = { onMediaClick(media) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountMetricCard(
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
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = InkBlack,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = EditorialGrey,
            )
        }
    }
}

@Composable
private fun MediaInsightRow(
    media: InstagramMediaItem,
    insightsState: UiState<MediaInsights>?,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("insight_media_row_${media.id}"),
        color = PureWhite,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, HairlineGrey),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = media.gridThumbnailUrl,
                contentDescription = media.caption.ifBlank { "Media thumbnail" },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .border(0.5.dp, HairlineGrey, RoundedCornerShape(3.dp))
                    .background(SoftPlinth),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.caption.ifBlank { "${media.kind.name.replace('_', ' ')} · ${media.timestamp.take(10)}" },
                    style = MaterialTheme.typography.titleSmall,
                    color = InkBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                when (insightsState) {
                    null, UiState.Loading -> {
                        Text(
                            text = "Likes ${formatCompactCount(media.likeCount)} · Comments ${formatCompactCount(media.commentsCount)} · Loading reach…",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialGrey,
                        )
                    }

                    is UiState.Error -> {
                        Text(
                            text = "Likes ${formatCompactCount(media.likeCount)} · Comments ${formatCompactCount(media.commentsCount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialGrey,
                        )
                    }

                    is UiState.Success -> {
                        val ins = insightsState.data
                        Text(
                            text = "Reach ${ins.reach?.let(::formatCompactCount) ?: "—"} · Views ${ins.views?.let(::formatCompactCount) ?: "—"} · Saves ${ins.saved?.let(::formatCompactCount) ?: "—"} · Shares ${ins.shares?.let(::formatCompactCount) ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialGrey,
                        )
                    }
                }
            }
        }
    }
}
