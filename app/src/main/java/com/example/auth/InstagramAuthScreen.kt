package com.example.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.OAuthAppConfig
import com.example.ui.components.EditorialDivider
import com.example.ui.theme.EditorialCrimson
import com.example.ui.theme.EditorialGrey
import com.example.ui.theme.HairlineGrey
import com.example.ui.theme.InkBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftPlinth

@Composable
fun InstagramAuthScreen(
    oauthConfig: OAuthAppConfig,
    isAuthenticating: Boolean,
    authError: String?,
    onConnectWithAccessToken: (String) -> Unit,
    onSaveOAuthConfig: (appId: String, proxyBaseUrl: String) -> Unit,
    onBuildAuthorizeUrl: (appId: String) -> String,
    onSubmitAuthCodeOrUrl: (String) -> Unit,
    onDismissError: () -> Unit,
) {
    val context = LocalContext.current
    var accessTokenInput by rememberSaveable { mutableStateOf("") }
    var showTokenPlaintext by rememberSaveable { mutableStateOf(false) }

    var showSelfHostedOAuth by rememberSaveable { mutableStateOf(false) }
    var appIdInput by rememberSaveable { mutableStateOf(oauthConfig.appId) }
    var proxyUrlInput by rememberSaveable { mutableStateOf(oauthConfig.proxyBaseUrl) }
    var callbackInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(oauthConfig.appId, oauthConfig.proxyBaseUrl) {
        if (appIdInput.isBlank() && oauthConfig.appId.isNotBlank()) {
            appIdInput = oauthConfig.appId
        }
        if (proxyUrlInput.isBlank() && oauthConfig.proxyBaseUrl.isNotBlank()) {
            proxyUrlInput = oauthConfig.proxyBaseUrl
        }
    }

    val draftOAuthValid = OAuthAppConfig(
        appId = appIdInput,
        proxyBaseUrl = proxyUrlInput,
    ).isConfiguredForBrowserOAuth

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = PureWhite,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                Text(
                    text = "OPEN SOURCE · ZERO-SECRET CLIENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = EditorialGrey,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = InkBlack,
                )
                Spacer(modifier = Modifier.height(16.dp))
                EditorialDivider()
                Spacer(modifier = Modifier.height(18.dp))

                Image(
                    painter = painterResource(id = R.drawable.img_editorial_arch_1795246483336),
                    contentDescription = stringResource(R.string.cd_editorial_hero),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(0.5.dp, HairlineGrey, RoundedCornerShape(4.dp)),
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.auth_headline),
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkBlack,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Because Instincta is open-source, no developer App Secret is embedded in this APK. Connect directly with your Meta User Token or your self-hosted Rust Axum OAuth server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EditorialGrey,
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!authError.isNullOrBlank()) {
                    Surface(
                        color = PureWhite,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, EditorialCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_error_banner"),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = authError,
                                style = MaterialTheme.typography.bodySmall,
                                color = EditorialCrimson,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = onDismissError,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .minimumInteractiveComponentSize()
                                    .testTag("dismiss_auth_error_button"),
                            ) {
                                Text("Dismiss", color = InkBlack)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Method 1: Direct Instagram User Token (zero backend required)
                Surface(
                    color = SoftPlinth,
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
                                imageVector = Icons.Outlined.Key,
                                contentDescription = null,
                                tint = InkBlack,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Connect with Instagram User Token",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkBlack,
                            )
                        }
                        Text(
                            text = "1. Open developers.facebook.com -> Your App -> Instagram -> API setup with Instagram login.\n" +
                                "2. Click Generate Token for your Creator/Business account.\n" +
                                "3. Paste it below — verified live against graph.instagram.com/v22.0/me.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialGrey,
                        )

                        OutlinedTextField(
                            value = accessTokenInput,
                            onValueChange = { accessTokenInput = it },
                            label = { Text("Instagram User Access Token") },
                            placeholder = { Text("IGAA...") },
                            singleLine = true,
                            visualTransformation = if (showTokenPlaintext) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showTokenPlaintext = !showTokenPlaintext },
                                    modifier = Modifier.testTag("toggle_token_visibility_button"),
                                ) {
                                    Icon(
                                        imageVector = if (showTokenPlaintext) {
                                            Icons.Outlined.VisibilityOff
                                        } else {
                                            Icons.Outlined.Visibility
                                        },
                                        contentDescription = if (showTokenPlaintext) "Hide token" else "Show token",
                                        tint = EditorialGrey,
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("access_token_input"),
                        )

                        Button(
                            onClick = { onConnectWithAccessToken(accessTokenInput) },
                            enabled = accessTokenInput.trim().length >= 20 && !isAuthenticating,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InkBlack,
                                contentColor = PureWhite,
                                disabledContainerColor = HairlineGrey,
                                disabledContentColor = EditorialGrey,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("connect_with_token_button"),
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = PureWhite,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.VerifiedUser,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = "Verify & Connect",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                EditorialDivider()

                // Method 2: Self-hosted OAuth via Rust Axum Proxy (app_secret stays on server)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clickable { showSelfHostedOAuth = !showSelfHostedOAuth }
                        .padding(vertical = 10.dp)
                        .testTag("toggle_self_hosted_oauth_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Self-Hosted OAuth Login (Browser + Rust Axum Proxy)",
                        style = MaterialTheme.typography.titleSmall,
                        color = InkBlack,
                    )
                    Icon(
                        imageVector = if (showSelfHostedOAuth) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (showSelfHostedOAuth) "Collapse" else "Expand",
                        tint = InkBlack,
                    )
                }

                AnimatedVisibility(visible = showSelfHostedOAuth) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Run rust/server/ so INSTAGRAM_APP_SECRET stays on your server. Only your numeric Meta App ID and proxy URL are saved on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialGrey,
                        )
                        OutlinedTextField(
                            value = appIdInput,
                            onValueChange = { appIdInput = it },
                            label = { Text("Meta Instagram App ID (numeric)") },
                            placeholder = { Text("123456789012345") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("oauth_app_id_input"),
                        )
                        OutlinedTextField(
                            value = proxyUrlInput,
                            onValueChange = { proxyUrlInput = it },
                            label = { Text("Rust Axum OAuth Proxy Base URL") },
                            placeholder = { Text("https://auth.yourdomain.com or http://10.0.2.2:8080") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("oauth_proxy_url_input"),
                        )

                        Button(
                            onClick = {
                                onSaveOAuthConfig(appIdInput, proxyUrlInput)
                                val url = onBuildAuthorizeUrl(appIdInput)
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            enabled = draftOAuthValid && !isAuthenticating,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InkBlack,
                                contentColor = PureWhite,
                                disabledContainerColor = HairlineGrey,
                                disabledContentColor = EditorialGrey,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("continue_with_instagram_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.continue_with_instagram))
                        }

                        OutlinedTextField(
                            value = callbackInput,
                            onValueChange = { callbackInput = it },
                            label = { Text("Manual Callback URL or ?code= (optional)") },
                            placeholder = { Text("instincta://oauth/callback?code=...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_oauth_code_input"),
                        )
                        OutlinedButton(
                            onClick = {
                                onSaveOAuthConfig(appIdInput, proxyUrlInput)
                                onSubmitAuthCodeOrUrl(callbackInput)
                            },
                            enabled = draftOAuthValid && callbackInput.isNotBlank() && !isAuthenticating,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, InkBlack),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_manual_oauth_code_button"),
                        ) {
                            Text("Exchange Code via Proxy", color = InkBlack)
                        }
                    }
                }
            }
        }
    }
}
