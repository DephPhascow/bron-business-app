package com.dphascow.app.auth

import com.dphascow.app.expects.PickedPhoto
import kotlinx.coroutines.delay

class MockAuthRepository : AuthRepository {
    private var lastBusinesses: List<BusinessOption> = emptyList()
    private var createdBusinessCounter: Int = 1

    override suspend fun requireCode(phoneOrEmail: String): Boolean {
        delay(450)
        return phoneOrEmail.isNotBlank()
    }

    override suspend fun verifyCode(phoneOrEmail: String, code: String): LoginResult {
        delay(450)

        val businesses = buildBusinesses(phoneOrEmail)
        lastBusinesses = businesses

        val normalizedPhone = phoneOrEmail.trim().ifBlank { "mock-user" }
        return LoginResult(
            accessToken = "access-$normalizedPhone",
            refreshToken = "refresh-$normalizedPhone",
            businesses = businesses,
        )
    }

    override suspend fun selectBusiness(businessId: String): BusinessSelectionResult {
        delay(250)
        val business = lastBusinesses.firstOrNull { it.id == businessId }
            ?: lastBusinesses.firstOrNull()
            ?: buildBusinesses(phone = "").first()

        return BusinessSelectionResult(business = business)
    }

    override suspend fun createBusiness(name: String, photo: PickedPhoto?): CreateBusinessResult {
        delay(300)

        val businessName = name.trim().ifBlank { "New Business" }
        val newBusiness = BusinessOption(
            id = "business-created-${createdBusinessCounter++}",
            name = businessName,
            role = if (photo != null) "Owner" else "Administrator",
        )

        lastBusinesses = (lastBusinesses + newBusiness).distinctBy { it.id }

        return CreateBusinessResult(business = newBusiness)
    }

    override suspend fun logout(allDevices: Boolean) {
        delay(150)
        lastBusinesses = emptyList()
    }

    private fun buildBusinesses(phone: String): List<BusinessOption> {
        val normalizedPhone = phone.trim().lowercase()
        return when {
            normalizedPhone.contains("solo") || normalizedPhone.contains("single") -> listOf(
                BusinessOption(
                    id = "business-solo",
                    name = "Solo Studio",
                    role = "Owner",
                )
            )

            normalizedPhone.contains("owner") -> listOf(
                BusinessOption(
                    id = "business-bron-center",
                    name = "Bron Center",
                    role = "Owner",
                ),
                BusinessOption(
                    id = "business-bron-north",
                    name = "Bron North",
                    role = "Owner",
                ),
            )

            else -> listOf(
                BusinessOption(
                    id = "business-team-main",
                    name = "Beauty Team Main",
                    role = "Staff",
                ),
                BusinessOption(
                    id = "business-team-downtown",
                    name = "Beauty Team Downtown",
                    role = "Administrator",
                ),
            )
        }
    }
}
