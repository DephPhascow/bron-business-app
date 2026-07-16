package com.dphascow.app.chat

import com.apollographql.apollo.api.Optional
import com.dphascow.app.expects.PickedPhoto
import com.dphascow.app.graphql.ChatMessagesQuery
import com.dphascow.app.graphql.MarkAsReadMutation
import com.dphascow.app.graphql.MyChatsQuery
import com.dphascow.app.graphql.NewMessageSubscription
import com.dphascow.app.graphql.SendMessageMutation
import com.dphascow.app.graphql.ShippingMutation
import com.dphascow.app.graphql.fragment.MessageFragment
import com.dphascow.app.graphql.type.MessageEnumType
import com.dphascow.app.graphql.type.PaginationPageInput
import com.dphascow.app.repositories.FileUploader
import com.dphascow.app.repositories.Requester
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import settings.AuthPref

class ApolloChatRepository(
    private val requester: Requester,
    private val fileUploader: FileUploader,
    private val authPref: AuthPref,
) : ChatRepository {
    override suspend fun loadChats(): List<ChatSummary> {
        val response = requester.requestQuery(MyChatsQuery(filters = Optional.Absent))
        val chats = response.data?.chatInstances
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty chats response")

        return chats.map { chat ->
            ChatSummary(
                id = chat.pk.toString(),
                name = chat.chatName,
                avatarPath = chat.chatAvatar?.path,
                lastMessage = chat.lastMessage?.text,
                unread = chat.getCountUnreadMessages,
            )
        }
    }

    override suspend fun startChatWith(userId: String): String {
        val response = requester.requestMutation(ShippingMutation(userId = userId.toIntRequired()))
        val chat = response.data?.shipping
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty start chat response")
        return chat.pk.toString()
    }

    override suspend fun loadMessages(chatId: String): List<ChatMessage> {
        val response = requester.requestQuery(
            ChatMessagesQuery(
                chatInstanceId = chatId.toIntRequired(),
                pagination = Optional.present(PaginationPageInput(limit = Optional.present(MESSAGE_PAGE_SIZE))),
            )
        )
        val page = response.data?.messages
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty messages response")

        return page.items.map { it.messageFragment.toDomain() }
    }

    override suspend fun sendText(chatId: String, text: String): ChatMessage {
        val response = requester.requestMutation(
            SendMessageMutation(
                chatInstanceId = chatId.toIntRequired(),
                text = Optional.present(text),
                type = MessageEnumType.TEXT,
            )
        )
        val message = response.data?.sendMessage?.messageFragment
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty send message response")

        return message.toDomain()
    }

    override suspend fun sendFile(chatId: String, file: PickedPhoto): ChatMessage {
        val url = fileUploader.uploadImage(file.bytes, file.fileName, file.mimeType)
        val type = if (file.mimeType?.startsWith("image") == true) MessageEnumType.MEDIA else MessageEnumType.FILE

        val response = requester.requestMutation(
            SendMessageMutation(
                chatInstanceId = chatId.toIntRequired(),
                fileUrl = Optional.present(url),
                type = type,
            )
        )
        val message = response.data?.sendMessage?.messageFragment
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty send file response")

        return message.toDomain()
    }

    override suspend fun markAsRead(chatId: String, messageId: String) {
        requester.requestMutation(
            MarkAsReadMutation(chatInstanceId = chatId.toIntRequired(), messageId = messageId.toIntRequired())
        )
    }

    override fun observeNewMessages(chatId: String): Flow<Unit> =
        requester.requestSubscription(
            NewMessageSubscription(
                token = authPref.accessToken.orEmpty(),
                chatInstanceId = chatId.toIntRequired(),
            )
        ).map { }

    private fun MessageFragment.toDomain(): ChatMessage = ChatMessage(
        id = pk.toString(),
        text = text,
        fileUrl = fileUrl,
        type = type.toDomain(),
        isMine = isIAuthor,
        authorName = chatMember.user.fullName,
        createdAt = createdAt,
        isRead = isAnyRead,
    )

    private fun MessageEnumType.toDomain(): ChatMessageType = when (this) {
        MessageEnumType.TEXT -> ChatMessageType.TEXT
        MessageEnumType.MEDIA -> ChatMessageType.MEDIA
        MessageEnumType.FILE -> ChatMessageType.FILE
        MessageEnumType.VOICE -> ChatMessageType.VOICE
        MessageEnumType.FORWARDED -> ChatMessageType.FORWARDED
        MessageEnumType.UNKNOWN__ -> ChatMessageType.UNKNOWN
    }

    private fun String.toIntRequired(): Int =
        toIntOrNull() ?: throw IllegalArgumentException("Chat id must be an integer for API requests")

    private companion object {
        const val MESSAGE_PAGE_SIZE = 50
    }
}
