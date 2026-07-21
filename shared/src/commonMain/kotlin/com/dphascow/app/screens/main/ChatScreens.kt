package com.dphascow.app.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dphascow.app.chat.ChatMessage
import com.dphascow.app.chat.ChatMessageType
import com.dphascow.app.chat.ChatRepository
import com.dphascow.app.chat.ChatSummary
import com.dphascow.app.expects.PickedPhoto
import com.dphascow.app.expects.openUrl
import com.dphascow.BuildKonfig
import com.dphascow.app.expects.rememberPhotoPickerLauncher
import com.dphascow.app.ui.NetworkImage
import com.dphascow.app.utils.resolveFullUrl
import com.dphascow.app.resources.Res
import com.dphascow.app.resources.*
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ui.theme.T

@Composable
fun ChatListScreen(
    repository: ChatRepository?,
    onOpenChat: (String) -> Unit,
    /** Null when chats is reached as a bottom-bar tab — there is nothing to go back to. */
    onBack: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
) {
    var chats by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository) {
        if (repository == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        runCatching { repository.loadChats() }
            .onSuccess { chats = it; loading = false }
            .onFailure { error = it.message; loading = false }
    }

    var query by remember { mutableStateOf("") }
    val filtered = remember(chats, query) {
        chats.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }
    val openLabel = stringResource(Res.string.common_open)
    LazyPageLayout(stringResource(Res.string.chat_title), stringResource(Res.string.chat_subtitle), onBack, onMenu) {
        item { SearchField(query) { query = it } }
        when {
            loading -> item { CircularProgressIndicator(color = T.c.primary) }
            error != null -> item { EmptyStateCard(error ?: "") }
            filtered.isEmpty() -> item { EmptyStateCard(stringResource(Res.string.chat_empty)) }
            else -> items(filtered, key = { it.id }) { chat ->
                val subtitle = chat.lastMessage.orEmpty().ifBlank { " " }
                val title = if (chat.unread > 0) "${chat.name} (${chat.unread})" else chat.name
                InfoCard(title, subtitle, openLabel) { onOpenChat(chat.id) }
            }
        }
    }
}

@Composable
fun ConversationScreen(
    repository: ChatRepository?,
    chatId: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var messages by remember(chatId) { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var input by remember(chatId) { mutableStateOf("") }
    var sending by remember(chatId) { mutableStateOf(false) }
    var error by remember(chatId) { mutableStateOf<String?>(null) }
    val listScroll = rememberScrollState()
    val photoPicker = rememberPhotoPickerLauncher(onPhotoPicked = { picked ->
        val repo = repository
        if (picked != null && repo != null) {
            sending = true
            scope.launch {
                runCatching { repo.sendFile(chatId, picked) }
                    .onSuccess { messages = repo.loadMessages(chatId); sending = false }
                    .onFailure { sending = false; error = it.message }
            }
        }
    })

    // Initial load + live updates via subscription; mark newest message read after each load.
    LaunchedEffect(chatId, repository) {
        val repo = repository ?: return@LaunchedEffect
        suspend fun reload() {
            val loaded = repo.loadMessages(chatId)
            messages = loaded
            loaded.maxByOrNull { it.id.toIntOrNull() ?: 0 }?.let { newest ->
                runCatching { repo.markAsRead(chatId, newest.id) }
            }
        }
        runCatching { reload() }.onFailure { error = it.message }
        runCatching { repo.observeNewMessages(chatId).collect { reload() } }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listScroll.scrollTo(listScroll.maxValue)
    }

    fun sendText() {
        val text = input.trim()
        val repo = repository
        if (text.isEmpty() || repo == null) return
        val current = input
        input = ""
        // Optimistic: show the message immediately, reconcile with server on success.
        val temp = ChatMessage(
            id = "temp-${text.hashCode()}-${messages.size}",
            text = text,
            fileUrl = null,
            type = ChatMessageType.TEXT,
            isMine = true,
            authorName = "",
            createdAt = "",
            isRead = false,
        )
        messages = messages + temp
        sending = true
        error = null
        scope.launch {
            runCatching { repo.sendText(chatId, text) }
                .onSuccess { messages = repo.loadMessages(chatId); sending = false }
                .onFailure {
                    sending = false
                    error = it.message
                    messages = messages.filterNot { m -> m.id == temp.id }
                    input = current
                }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(T.d.paddingMain)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(Res.string.common_back)) }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(listScroll),
            verticalArrangement = Arrangement.spacedBy(T.d.sm),
        ) {
            if (messages.isEmpty()) {
                Text(
                    text = stringResource(Res.string.chat_conversation_empty),
                    color = T.c.dark5,
                    style = T.t.t3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(T.d.lg),
                )
            }
            messages.forEach { message -> MessageBubble(message) }
        }

        error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold, modifier = Modifier.fillMaxWidth()) }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = T.d.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(T.d.sm),
        ) {
            OutlinedButton(
                onClick = photoPicker::launch,
                enabled = photoPicker.isAvailable && !sending,
            ) {
                Text("+")
            }
            OutlinedButton(
                onClick = {
                    val repo = repository ?: return@OutlinedButton
                    scope.launch {
                        val file = FileKit.openFilePicker() ?: return@launch
                        sending = true
                        error = null
                        runCatching {
                            val bytes = file.readBytes()
                            repo.sendFile(chatId, PickedPhoto(bytes = bytes, fileName = file.name, mimeType = null))
                        }.onSuccess { messages = repo.loadMessages(chatId); sending = false }
                            .onFailure { sending = false; error = it.message }
                    }
                },
                enabled = !sending,
            ) {
                Text("📎")
            }
            // The composer stays a compact inline row, so it keeps its own field
            // rather than the full-width AppTextField — only the shape is matched.
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(Res.string.chat_input_hint), color = T.c.dark5) },
                enabled = !sending,
                maxLines = 4,
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = T.c.borderColor,
                    unfocusedBorderColor = T.c.borderColor,
                ),
            )
            Button(onClick = { sendText() }, enabled = !sending && input.isNotBlank()) {
                Text(stringResource(Res.string.chat_send))
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isMine) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isMine) T.c.primary else T.c.surface
    val textColor = if (message.isMine) T.c.onPrimary else T.c.dark10
    val fileUrl = message.fileUrl

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(T.d.md))
                .background(bubbleColor)
                .then(if (fileUrl != null) Modifier.clickable { openUrl(resolveFullUrl(BuildKonfig.API_HOST, fileUrl) ?: fileUrl) } else Modifier)
                .padding(T.d.md),
            verticalArrangement = Arrangement.spacedBy(T.d.xs),
        ) {
            if (!message.isMine) {
                Text(message.authorName, color = T.c.dark5, style = T.t.t4SamiBold)
            }
            when (message.type) {
                ChatMessageType.TEXT -> Text(message.text.orEmpty(), color = textColor, style = T.t.t2Regular)
                else -> {
                    if (message.type == ChatMessageType.MEDIA && fileUrl != null) {
                        NetworkImage(
                            url = fileUrl,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.3f).clip(RoundedCornerShape(T.d.sm)),
                        )
                    } else {
                        Text("📎 ${message.fileName()}", color = textColor, style = T.t.t2Regular)
                    }
                    message.text?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = textColor, style = T.t.t2Regular)
                    }
                }
            }
            val time = message.time()
            if (time.isNotBlank()) {
                Text(
                    text = time,
                    color = textColor,
                    style = T.t.t4,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

private fun ChatMessage.time(): String = createdAt.substringAfter('T', "").take(5)

private fun ChatMessage.fileName(): String =
    fileUrl?.substringAfterLast('/')?.ifBlank { "file" } ?: "file"
