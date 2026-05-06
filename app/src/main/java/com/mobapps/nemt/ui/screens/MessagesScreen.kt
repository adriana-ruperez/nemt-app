package com.mobapps.nemt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.mobapps.nemt.data.ConversationMessage
import com.mobapps.nemt.data.ConversationRecord
import com.mobapps.nemt.data.ConversationsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.awaitCancellation

private val MessagesBackground = Color(0xFFF3F4F7)
private val ThreadCard = Color(0xFFFFFFFF)
private val ThreadBorder = Color(0xFFE7E8EE)
private val AccentBlue = Color(0xFF2F8FFF)
private val MutedText = Color(0xFF7A7F8C)
private val StrongText = Color(0xFF111318)

@Composable
fun MessagesScreen(
    contentPadding: PaddingValues,
    onOpenConversation: (String) -> Unit
) {
    val riderUid = remember { FirebaseAuth.getInstance().currentUser?.uid }
    val conversations by ConversationsRepository.conversations.collectAsState()

    LaunchedEffect(riderUid) {
        if (riderUid == null) {
            ConversationsRepository.stopConversationsListener()
            return@LaunchedEffect
        }
        ConversationsRepository.startConversationsListener(riderUid)
        try {
            awaitCancellation()
        } finally {
            ConversationsRepository.stopConversationsListener()
        }
    }

    Surface(color = MessagesBackground, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Messages",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = StrongText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Support and driver conversations for your rides.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (conversations.isEmpty()) {
                EmptyMessagesState()
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(conversations, key = { it.id }) { conversation ->
                        ConversationListItem(
                            conversation = conversation,
                            onClick = { onOpenConversation(conversation.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationScreen(
    conversationId: String,
    onBack: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val riderUid = auth.currentUser?.uid.orEmpty()
    val riderName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: auth.currentUser?.email?.substringBefore("@")?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }.orEmpty()
    val conversations by ConversationsRepository.conversations.collectAsState()
    val conversation = conversations.firstOrNull { it.id == conversationId }
    val messages by ConversationsRepository.messages(conversationId).collectAsState()
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    var sendError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(riderUid, conversationId) {
        if (riderUid.isNotBlank()) {
            ConversationsRepository.startConversationsListener(riderUid)
        }
        ConversationsRepository.startMessagesListener(conversationId)
        try {
            awaitCancellation()
        } finally {
            if (riderUid.isNotBlank()) {
                ConversationsRepository.stopConversationsListener()
            }
            ConversationsRepository.stopMessagesListener(conversationId)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun submitMessage() {
        val textToSend = draft.trim()
        if (textToSend.isBlank()) return
        draft = ""
        sendError = null
        ConversationsRepository.sendMessage(
            conversationId = conversationId,
            senderId = riderUid,
            senderName = riderName.ifBlank { "Passenger" },
            text = textToSend
        ) { result ->
            result.onFailure {
                draft = textToSend
                sendError = it.message ?: "Unable to send message."
            }
        }
    }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MessagesBackground)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                sendError?.let { error ->
                    Text(
                        text = error,
                        color = Color(0xFFB3261E),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = {
                            draft = it
                            if (sendError != null) sendError = null
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Message") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submitMessage() })
                    )
                    Spacer(modifier = Modifier.height(0.dp))
                    TextButton(onClick = { submitMessage() }) {
                        Text("Send")
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(color = MessagesBackground, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Column {
                        Text(
                            text = conversation?.displayName() ?: "Conversation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = conversation?.displaySubtitle() ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            isOwn = message.senderId == riderUid
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationListItem(
    conversation: ConversationRecord,
    onClick: () -> Unit
) {
    val icon = when (conversation.type) {
        "driver" -> Icons.Outlined.LocalShipping
        else -> Icons.Outlined.SupportAgent
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ThreadCard, RoundedCornerShape(18.dp))
            .border(1.dp, ThreadBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(AccentBlue.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentBlue
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.displayName(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = conversation.displaySubtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
            Text(
                text = formatTimestamp(conversation.updatedAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MutedText
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = conversation.lastMessageText.ifBlank { "Open conversation" },
            style = MaterialTheme.typography.bodyMedium,
            color = StrongText
        )
    }
}

private fun ConversationRecord.displayName(): String = when (type) {
    "support" -> "Administracion"
    "driver" -> driverName?.takeIf { it.isNotBlank() }
        ?: counterpartName.takeIf { it.isNotBlank() }
        ?: title.takeIf { it.isNotBlank() }
        ?: "Driver"
    else -> counterpartName.takeIf { it.isNotBlank() }
        ?: title.takeIf { it.isNotBlank() }
        ?: "Conversation"
}

private fun ConversationRecord.displaySubtitle(): String = when (type) {
    "support" -> "Support chat"
    "driver" -> "Assigned driver"
    else -> subtitle
}

@Composable
private fun MessageBubble(
    message: ConversationMessage,
    isOwn: Boolean
) {
    val bubbleColor = if (isOwn) AccentBlue else ThreadCard
    val textColor = if (isOwn) Color.White else StrongText
    val align = if (isOwn) Alignment.End else Alignment.Start
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                if (!isOwn) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun EmptyMessagesState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ThreadCard, RoundedCornerShape(20.dp))
            .border(1.dp, ThreadBorder, RoundedCornerShape(20.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = AccentBlue
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No conversations yet",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your support and driver chats will appear here after you book a ride.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }
    }
}

private fun formatTimestamp(value: Long): String {
    if (value <= 0L) return ""
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(value))
}
