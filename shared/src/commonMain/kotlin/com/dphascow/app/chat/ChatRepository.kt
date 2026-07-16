package com.dphascow.app.chat

import com.dphascow.app.expects.PickedPhoto
import kotlinx.coroutines.flow.Flow

data class ChatSummary(
    val id: String,
    val name: String,
    val avatarPath: String?,
    val lastMessage: String?,
    val unread: Int,
)

enum class ChatMessageType {
    TEXT,
    MEDIA,
    FILE,
    VOICE,
    FORWARDED,
    UNKNOWN,
}

data class ChatMessage(
    val id: String,
    val text: String?,
    val fileUrl: String?,
    val type: ChatMessageType,
    val isMine: Boolean,
    val authorName: String,
    val createdAt: String,
    val isRead: Boolean,
)

interface ChatRepository {
    suspend fun loadChats(): List<ChatSummary>

    /** Opens (or creates) a direct chat with the given user and returns its id. */
    suspend fun startChatWith(userId: String): String

    suspend fun loadMessages(chatId: String): List<ChatMessage>

    suspend fun sendText(chatId: String, text: String): ChatMessage

    /** Uploads the picked file and sends it as a message. */
    suspend fun sendFile(chatId: String, file: PickedPhoto): ChatMessage

    suspend fun markAsRead(chatId: String, messageId: String)

    /** Emits whenever a new message arrives in the chat (via GraphQL subscription). */
    fun observeNewMessages(chatId: String): Flow<Unit>
}
