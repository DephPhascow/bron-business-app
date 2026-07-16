package com.dphascow.app.profile

import com.apollographql.apollo.api.Optional
import com.dphascow.app.expects.PickedPhoto
import com.dphascow.app.graphql.MeQuery
import com.dphascow.app.graphql.UpdateMeMutation
import com.dphascow.app.graphql.type.UpdateUser
import com.dphascow.app.repositories.FileUploader
import com.dphascow.app.repositories.Requester

class ApolloProfileRepository(
    private val requester: Requester,
    private val fileUploader: FileUploader,
) : ProfileRepository {
    override suspend fun loadMe(): MeProfile {
        val response = requester.requestQuery(MeQuery())
        val me = response.data?.me
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty me response")

        return MeProfile(
            id = me.pk.toString(),
            firstName = me.firstName,
            lastName = me.lastName,
            patronymic = me.patronymic,
            fullName = me.fullName,
            phone = me.phone,
            email = me.email,
            imageUrl = me.imageUrl,
            confirmNotifications = me.confirmNotifications,
        )
    }

    override suspend fun updateProfile(
        firstName: String?,
        lastName: String?,
        patronymic: String?,
        confirmNotifications: Boolean,
        avatar: PickedPhoto?,
    ): MeProfile {
        val imageUrl = avatar?.let { fileUploader.uploadImage(it.bytes, it.fileName, it.mimeType) }

        val input = UpdateUser(
            firstName = Optional.presentIfNotNull(firstName?.trim()?.ifBlank { null }),
            lastName = Optional.presentIfNotNull(lastName?.trim()?.ifBlank { null }),
            patronymic = Optional.presentIfNotNull(patronymic?.trim()?.ifBlank { null }),
            confirmNotifications = Optional.present(confirmNotifications),
            imageUrl = Optional.presentIfNotNull(imageUrl),
        )
        val response = requester.requestMutation(UpdateMeMutation(input = input))
        val me = response.data?.updateMe
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty update me response")

        return MeProfile(
            id = me.pk.toString(),
            firstName = me.firstName,
            lastName = me.lastName,
            patronymic = me.patronymic,
            fullName = me.fullName,
            phone = me.phone,
            email = me.email,
            imageUrl = me.imageUrl,
            confirmNotifications = me.confirmNotifications,
        )
    }
}
