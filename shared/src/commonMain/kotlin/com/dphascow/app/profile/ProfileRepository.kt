package com.dphascow.app.profile

import com.dphascow.app.expects.PickedPhoto

data class MeProfile(
    val id: String,
    val firstName: String?,
    val lastName: String?,
    val patronymic: String?,
    val fullName: String,
    val phone: String?,
    val email: String?,
    val imageUrl: String?,
    val confirmNotifications: Boolean,
)

interface ProfileRepository {
    suspend fun loadMe(): MeProfile

    suspend fun updateProfile(
        firstName: String?,
        lastName: String?,
        patronymic: String?,
        confirmNotifications: Boolean,
        avatar: PickedPhoto?,
    ): MeProfile
}
