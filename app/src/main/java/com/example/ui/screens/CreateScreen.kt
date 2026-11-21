package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.PublishMode
import com.example.data.PublishProgressState
import com.example.data.PublishRequest
import com.example.ui.components.EditorialDivider
import com.example.ui.theme.EditorialCrimson
import com.example.ui.theme.EditorialGrey
import com.example.ui.theme.HairlineGrey
import com.example.ui.theme.InkBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftPlinth

@Composable
fun CreateScreen(
    publishProgress: PublishProgressState,
    onPublish: (PublishRequest) -> Unit,
    onResetPublishState: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(PublishMode.PHOTO) }
    var singleUrl by rememberSaveable { mutableStateOf("") }
    val carouselUrls = remember { mutableStateListOf("", "") }
    var caption by rememberSaveable { mutableStateOf("") }
    var shareReelToFeed by rememberSaveable { mutableStateOf(true) }
    var previewLocalUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        previewLocalUri = uri
    }

    val isWorking = publishProgress is PublishProgressState.Working
    val canSubmit = when (mode) {
        PublishMode.PHOTO, PublishMode.REEL -> singleUrl.trim().startsWith("https://")
        PublishMode.CAROUSEL -> carouselUrls.count { it.trim().startsWith("https://") } >= 2
    } && !isWorking

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Text(
                text = "Publish",
                style = MaterialTheme.typography.headlineMedium,
                color = InkBlack,
            )
            Text(
                text = "META CONTAINER PUBLISHING STUDIO",
                style = MaterialTheme.typography.labelSmall,
                color = EditorialGrey,
            )
        }
        EditorialDivider()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("publish_mode_selector"),
            ) {
                PublishMode.entries.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = mode == item,
                        onClick = {
                            mode = item
                            onResetPublishState()
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = PublishMode.entries.size,
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = InkBlack,
                            activeContentColor = PureWhite,
                            inactiveContainerColor = PureWhite,
                            inactiveContentColor = InkBlack,
                        ),
                        modifier = Modifier.testTag("publish_mode_${item.name.lowercase()}"),
                    ) {
                        Text(item.label)
                    }
                }
            }

            Surface(
                color = SoftPlinth,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.5.dp, HairlineGrey),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = InkBlack,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Meta's Instagram Graph API fetches media directly from a public HTTPS URL during container creation. Pick a local photo to preview layout framing, then paste your public HTTPS media URL(s) below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialGrey,
                    )
                }
            }

            if (mode != PublishMode.CAROUSEL) {
                OutlinedTextField(
                    value = singleUrl,
                    onValueChange = { singleUrl = it },
                    label = {
                        Text(
                            if (mode == PublishMode.REEL) "Public HTTPS MP4/MOV Video URL" else "Public HTTPS JPEG/PNG Image URL",
                        )
                    },
                    placeholder = { Text("https://cdn.yourdomain.com/editorial.jpg") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_media_url_input"),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    carouselUrls.forEachIndexed { index, value ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = value,
                                onValueChange = { carouselUrls[index] = it },
                                label = { Text("Slide ${index + 1} HTTPS URL") },
                                placeholder = { Text("https://cdn.yourdomain.com/slide-${index + 1}.jpg") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("publish_carousel_url_$index"),
                            )
                            if (carouselUrls.size > 2) {
                                IconButton(
                                    onClick = { carouselUrls.removeAt(index) },
                                    modifier = Modifier.testTag("remove_carousel_slide_$index"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Remove slide",
                                        tint = EditorialCrimson,
                                    )
                                }
                            }
                        }
                    }

                    if (carouselUrls.size < 10) {
                        OutlinedButton(
                            onClick = { carouselUrls.add("") },
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, InkBlack),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("add_carousel_slide_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = InkBlack,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Slide (${carouselUrls.size}/10)", color = InkBlack)
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                    )
                },
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.5.dp, HairlineGrey),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pick_local_preview_button"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = InkBlack,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Preview Local Photo/Video Framing", color = InkBlack)
            }

            val activePreviewModel: Any? = when {
                mode != PublishMode.CAROUSEL && singleUrl.trim().startsWith("https://") -> singleUrl.trim()
                mode == PublishMode.CAROUSEL && carouselUrls.firstOrNull()?.trim()?.startsWith("https://") == true ->
                    carouselUrls.first().trim()
                else -> previewLocalUri
            }
            if (activePreviewModel != null) {
                AsyncImage(
                    model = activePreviewModel,
                    contentDescription = "Media preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(0.5.dp, HairlineGrey, RoundedCornerShape(4.dp)),
                )
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption & Hashtags") },
                placeholder = { Text("Write an editorial caption…") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("publish_caption_input"),
            )

            if (mode == PublishMode.REEL) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, HairlineGrey, RoundedCornerShape(4.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Share Reel to Profile Grid",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkBlack,
                        )
                        Text(
                            text = "Includes the Reel in your main profile grid feed",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialGrey,
                        )
                    }
                    Switch(
                        checked = shareReelToFeed,
                        onCheckedChange = { shareReelToFeed = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = InkBlack,
                            checkedThumbColor = PureWhite,
                        ),
                        modifier = Modifier.testTag("share_reel_to_feed_switch"),
                    )
                }
            }

            when (publishProgress) {
                PublishProgressState.Idle -> Unit
                is PublishProgressState.Working -> {
                    Surface(
                        color = SoftPlinth,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, HairlineGrey),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = InkBlack,
                            )
                            Text(
                                text = publishProgress.stepLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkBlack,
                            )
                        }
                    }
                }

                is PublishProgressState.Published -> {
                    Surface(
                        color = SoftPlinth,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, InkBlack),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = InkBlack,
                            )
                            Text(
                                text = "Published (Media ID: ${publishProgress.mediaId})",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkBlack,
                            )
                        }
                    }
                }

                is PublishProgressState.Failed -> {
                    Surface(
                        color = PureWhite,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, EditorialCrimson),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = publishProgress.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialCrimson,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val req = when (mode) {
                        PublishMode.PHOTO -> PublishRequest.SinglePhoto(
                            imageUrl = singleUrl.trim(),
                            caption = caption,
                        )
                        PublishMode.REEL -> PublishRequest.Reel(
                            videoUrl = singleUrl.trim(),
                            caption = caption,
                            shareToFeed = shareReelToFeed,
                        )
                        PublishMode.CAROUSEL -> PublishRequest.Carousel(
                            itemUrls = carouselUrls.map { it.trim() }.filter { it.startsWith("https://") },
                            caption = caption,
                        )
                    }
                    onPublish(req)
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = InkBlack,
                    contentColor = PureWhite,
                    disabledContainerColor = HairlineGrey,
                    disabledContentColor = EditorialGrey,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("publish_submit_button"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Publish,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isWorking) "Publishing…" else "Publish ${mode.label}",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
