package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.InstagramConversation
import com.example.data.InstagramMessage
import com.example.data.UiState
import com.example.ui.components.EditorialAvatar
import com.example.ui.components.EditorialDivider
import com.example.ui.components.EditorialErrorState
import com.example.ui.theme.EditorialGrey
import com.example.ui.theme.HairlineGrey
import com.example.ui.theme.InkBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftPlinth

@Composable
fun InboxScreen(
    conversationsState: UiState<List<InstagramConversation>>,
    activeThreadState: UiState<InstagramConversation>?,
    isSendingMessage: Boolean,
    onRefreshConversations: () -> Unit,
    onOpenConversation: (InstagramConversation) -> Unit,
    onRefreshThread: (String) -> Unit,
    onCloseConversation: () -> Unit,
    onSendDirectMessage: (conversationId: String, recipientId: String, text: String) -> Unit,
) {
    if (activeThreadState != null) {
        BackHandler { onCloseConversation() }
        ConversationThreadPane(
            threadState = activeThreadState,
            isSendingMessage = isSendingMessage,
            onBack = onCloseConversation,
            onRefresh = onRefreshThread,
            onSendMessage = onSendDirectMessage,
        )
        return
    }

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
                    text = "Inbox",
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkBlack,
                )
                Text(
                    text = "INSTAGRAM DIRECT MESSAGES",
                    style = MaterialTheme.typography.labelSmall,
                    color = EditorialGrey,
                )
            }
            IconButton(
                onClick = onRefreshConversations,
                modifier = Modifier.testTag("inbox_refresh_button"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.cd_refresh),
                    tint = InkBlack,
                )
            }
        }
        EditorialDivider()

        when (conversationsState) {
            UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = InkBlack)
                }
            }

            is UiState.Error -> {
                EditorialErrorState(
                    message = conversationsState.message,
                    onRetry = onRefreshConversations,
                )
            }

            is UiState.Success -> {
                val conversations = conversationsState.data
                if (conversations.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Forum,
                            contentDescription = null,
                            tint = EditorialGrey,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Direct Conversations",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkBlack,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Messages sent to your Professional Instagram account appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialGrey,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(conversations, key = { it.id }) { convo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenConversation(convo) }
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                                    .testTag("conversation_row_${convo.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                EditorialAvatar(
                                    imageUrl = null,
                                    username = convo.recipient.username.ifBlank { convo.recipient.name },
                                    size = 46.dp,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = convo.recipient.displayHandle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkBlack,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = convo.lastMessagePreview.ifBlank { "Tap to open conversation" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EditorialGrey,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    text = convo.updatedTime.take(10),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EditorialGrey,
                                )
                            }
                            EditorialDivider(modifier = Modifier.padding(horizontal = 20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationThreadPane(
    threadState: UiState<InstagramConversation>,
    isSendingMessage: Boolean,
    onBack: () -> Unit,
    onRefresh: (String) -> Unit,
    onSendMessage: (conversationId: String, recipientId: String, text: String) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite),
    ) {
        val conversation = (threadState as? UiState.Success)?.data
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("thread_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = InkBlack,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation?.recipient?.displayHandle ?: "Conversation",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "INSTAGRAM MESSAGING API",
                    style = MaterialTheme.typography.labelSmall,
                    color = EditorialGrey,
                )
            }
            if (conversation != null) {
                IconButton(
                    onClick = { onRefresh(conversation.id) },
                    modifier = Modifier.testTag("thread_refresh_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.cd_refresh),
                        tint = InkBlack,
                    )
                }
            }
        }
        EditorialDivider()

        Box(modifier = Modifier.weight(1f)) {
            when (threadState) {
                UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = InkBlack)
                    }
                }

                is UiState.Error -> {
                    EditorialErrorState(
                        message = threadState.message,
                        onRetry = onBack,
                    )
                }

                is UiState.Success -> {
                    val messages = threadState.data.messages
                    if (messages.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No messages in this thread.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EditorialGrey,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                DirectMessageBubble(message = msg)
                            }
                        }
                    }
                }
            }
        }

        EditorialDivider()
        if (conversation != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Reply to ${conversation.recipient.displayHandle}…") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dm_composer_input"),
                )
                IconButton(
                    onClick = {
                        onSendMessage(conversation.id, conversation.recipient.id, draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank() && conversation.recipient.id.isNotBlank() && !isSendingMessage,
                    modifier = Modifier
                        .size(48.dp)
                        .background(InkBlack, RoundedCornerShape(4.dp))
                        .testTag("dm_send_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send direct message",
                        tint = PureWhite,
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectMessageBubble(message: InstagramMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (message.isFromMe) InkBlack else SoftPlinth,
            contentColor = if (message.isFromMe) PureWhite else InkBlack,
            shape = RoundedCornerShape(4.dp),
            border = if (message.isFromMe) null else BorderStroke(0.5.dp, HairlineGrey),
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.text.ifBlank { "[Attachment]" },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (message.createdTime.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = message.createdTime.take(16).replace('T', ' '),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.isFromMe) PureWhite.copy(alpha = 0.7f) else EditorialGrey,
                    )
                }
            }
        }
    }
}
