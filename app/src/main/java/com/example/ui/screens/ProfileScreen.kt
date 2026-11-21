package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.data.InstagramProfile
import com.example.data.InstagramSession
import com.example.data.UiState
import com.example.ui.components.EditorialAvatar
import com.example.ui.components.EditorialDivider
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.EditorialCrimson
import com.example.ui.theme.EditorialGrey
import com.example.ui.theme.HairlineGrey
import com.example.ui.theme.InkBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftPlinth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    session: InstagramSession?,
    profileState: UiState<InstagramProfile>,
    onRefreshSessionToken: () -> Unit,
    onRefreshProfile: () -> Unit,
    onSignOut: () -> Unit,
) {
    var showRustBlueprintSheet by remember { mutableStateOf(false) }

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
                text = "Account",
                style = MaterialTheme.typography.headlineMedium,
                color = InkBlack,
            )
            Text(
                text = "OPEN SOURCE · ZERO-SECRET · RUST CORE",
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
            if (profileState is UiState.Success) {
                val p = profileState.data
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, HairlineGrey),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EditorialAvatar(
                                imageUrl = p.profilePictureUrl,
                                username = p.username,
                                size = 56.dp,
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "@${p.username}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = InkBlack,
                                )
                                Text(
                                    text = "${p.accountType} · ID ${p.userId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EditorialGrey,
                                )
                            }
                        }
                        if (p.biography.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = p.biography,
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkBlack,
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        EditorialDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            StatBlock("Posts", formatCompactCount(p.mediaCount))
                            StatBlock("Followers", formatCompactCount(p.followersCount))
                            StatBlock("Following", formatCompactCount(p.followsCount))
                        }
                    }
                }
            }

            Surface(
                color = SoftPlinth,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.5.dp, HairlineGrey),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = InkBlack,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Instagram Session (Zero-Secret APK)",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkBlack,
                        )
                    }
                    Text(
                        text = if (session != null) {
                            "Token stored locally in DataStore · ${session.daysRemaining} days remaining before renewal."
                        } else {
                            "No active session."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialGrey,
                    )
                    Text(
                        text = "MIT Open Source: zero developer app secrets are bundled in this APK.",
                        style = MaterialTheme.typography.labelSmall,
                        color = EditorialGrey,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onRefreshSessionToken,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, InkBlack),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("refresh_token_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint = InkBlack,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh Token", color = InkBlack)
                        }

                        OutlinedButton(
                            onClick = onRefreshProfile,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, HairlineGrey),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("refresh_profile_button"),
                        ) {
                            Text("Sync Profile", color = InkBlack)
                        }
                    }
                }
            }

            Surface(
                color = PureWhite,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.5.dp, HairlineGrey),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Code,
                            contentDescription = null,
                            tint = InkBlack,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Rust Workspace & Axum OAuth Proxy",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkBlack,
                        )
                    }
                    Text(
                        text = "The repository includes rust/crates/instincta-core (UniFFI + reqwest) and rust/server (Axum OAuth proxy holding INSTAGRAM_APP_SECRET off-device).",
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialGrey,
                    )
                    OutlinedButton(
                        onClick = { showRustBlueprintSheet = true },
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, InkBlack),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_rust_blueprint_button"),
                    ) {
                        Text("Inspect Rust Build & Proxy Guide", color = InkBlack)
                    }
                }
            }

            Button(
                onClick = onSignOut,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EditorialCrimson,
                    contentColor = PureWhite,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (showRustBlueprintSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showRustBlueprintSheet = false },
            sheetState = sheetState,
            containerColor = PureWhite,
            contentColor = InkBlack,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Open-Source Rust Workspace (rust/)",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkBlack,
                )
                EditorialDivider()
                Text(
                    text = "1. Compile rust/crates/instincta-core for Android ABIs:\n" +
                        "   cd rust && cargo ndk -t arm64-v8a -t armeabi-v7a -o ../app/src/main/jniLibs build -p instincta-core --release",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = InkBlack,
                )
                Text(
                    text = "2. Run the Axum OAuth Secret Proxy (rust/server/):\n" +
                        "   cd rust && cp server/.env.example server/.env && cargo run -p instincta-auth-server",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = InkBlack,
                )
                Text(
                    text = "3. License & Docs:\n" +
                        "   MIT License at /LICENSE · Full guide at /README.md and /docs/RUST_CORE_EXPORT.md.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EditorialGrey,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
