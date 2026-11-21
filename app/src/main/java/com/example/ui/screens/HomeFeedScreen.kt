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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.IgMediaKind
import com.example.data.InstagramMediaItem
import com.example.data.InstagramProfile
import com.example.data.UiState
import com.example.ui.components.EditorialAvatar
import com.example.ui.components.EditorialDivider
import com.example.ui.components.EditorialErrorState
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.EditorialGrey
import com.example.ui.theme.HairlineGrey
import com.example.ui.theme.InkBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftPlinth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridScreen(
    profileState: UiState<InstagramProfile>,
    mediaGridState: UiState<List<InstagramMediaItem>>,
    hasMoreMedia: Boolean,
    isLoadingMore: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onMediaClick: (InstagramMediaItem) -> Unit,
) {
    val isRefreshing = profileState is UiState.Loading || mediaGridState is UiState.Loading

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
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkBlack,
                )
                Text(
                    text = "INSTAGRAM CLIENT · EDITORIAL GRID",
                    style = MaterialTheme.typography.labelSmall,
                    color = EditorialGrey,
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("grid_refresh_button"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.cd_refresh),
                    tint = InkBlack,
                )
            }
        }
        EditorialDivider()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when (mediaGridState) {
                UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = InkBlack)
                    }
                }

                is UiState.Error -> {
                    EditorialErrorState(
                        message = mediaGridState.message,
                        onRetry = onRefresh,
                    )
                }

                is UiState.Success -> {
                    val items = mediaGridState.data
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                        verticalArrangement = Arrangement.spacedBy(1.5.dp),
                    ) {
                        if (profileState is UiState.Success) {
                            item(span = { GridItemSpan(3) }) {
                                ProfileSummaryStrip(profile = profileState.data)
                            }
                        }

                        if (items.isEmpty()) {
                            item(span = { GridItemSpan(3) }) {
                                EmptyMediaGridState()
                            }
                        } else {
                            items(items, key = { it.id }) { media ->
                                GridMediaTile(
                                    media = media,
                                    onClick = { onMediaClick(media) },
                                )
                            }

                            if (hasMoreMedia) {
                                item(span = { GridItemSpan(3) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        OutlinedButton(
                                            onClick = onLoadMore,
                                            enabled = !isLoadingMore,
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, InkBlack),
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("load_more_media_button"),
                                        ) {
                                            if (isLoadingMore) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = InkBlack,
                                                )
                                            } else {
                                                Text("Load Earlier Posts", color = InkBlack)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSummaryStrip(profile: InstagramProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureWhite),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditorialAvatar(
                imageUrl = profile.profilePictureUrl,
                username = profile.username,
                size = 54.dp,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${profile.username}",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (profile.name.isNotBlank()) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialGrey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatCompactCount(profile.mediaCount)} posts · ${formatCompactCount(profile.followersCount)} followers · ${formatCompactCount(profile.followsCount)} following",
                    style = MaterialTheme.typography.labelSmall,
                    color = EditorialGrey,
                )
            }
        }
        if (profile.biography.isNotBlank()) {
            Text(
                text = profile.biography,
                style = MaterialTheme.typography.bodySmall,
                color = InkBlack,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
            )
        }
        EditorialDivider()
    }
}

@Composable
private fun GridMediaTile(
    media: InstagramMediaItem,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(SoftPlinth)
            .clickable(onClick = onClick)
            .testTag("grid_media_tile_${media.id}"),
    ) {
        AsyncImage(
            model = media.gridThumbnailUrl,
            contentDescription = media.caption.ifBlank { "Instagram post ${media.id}" },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        if (media.kind == IgMediaKind.VIDEO || media.kind == IgMediaKind.CAROUSEL_ALBUM) {
            Surface(
                color = InkBlack.copy(alpha = 0.72f),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            ) {
                Icon(
                    imageVector = if (media.kind == IgMediaKind.VIDEO) {
                        Icons.Outlined.PlayArrow
                    } else {
                        Icons.Outlined.Collections
                    },
                    contentDescription = media.kind.name,
                    tint = PureWhite,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyMediaGridState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .border(0.5.dp, HairlineGrey, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                tint = EditorialGrey,
            )
        }
        Text(
            text = "No Published Media Yet",
            style = MaterialTheme.typography.titleMedium,
            color = InkBlack,
        )
        Text(
            text = "Publish a Photo, Carousel, or Reel from the Publish tab to see it appear here.",
            style = MaterialTheme.typography.bodySmall,
            color = EditorialGrey,
        )
    }
}
