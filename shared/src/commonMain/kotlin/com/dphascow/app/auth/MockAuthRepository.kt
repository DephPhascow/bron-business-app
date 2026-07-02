package com.dphascow.app.auth

import com.dphascow.app.expects.PickedPhoto
import kotlinx.coroutines.delay

class MockAuthRepository : AuthRepository {
    private var lastBusinesses: List<BusinessOption> = emptyList()
    private var createdBusinessCounter: Int = 1

    override suspend fun login(email: String, password: String): LoginResult {
        delay(450)

        val businesses = buildBusinesses(email)
        lastBusinesses = businesses

        val normalizedEmail = email.trim().lowercase().ifBlank { "mock-user" }
        return LoginResult(
            accessToken = "access-$normalizedEmail",
            refreshToken = "refresh-$normalizedEmail",
            businesses = businesses,
        )
    }

    override suspend fun selectBusiness(businessId: String): BusinessSelectionResult {
        delay(250)
        val business = lastBusinesses.firstOrNull { it.id == businessId }
            ?: lastBusinesses.firstOrNull()
            ?: buildBusinesses(email = "").first()

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

    private fun buildBusinesses(email: String): List<BusinessOption> {
        val normalizedEmail = email.trim().lowercase()
        return when {
            normalizedEmail.contains("solo") || normalizedEmail.contains("single") -> listOf(
                BusinessOption(
                    id = "business-solo",
                    name = "Solo Studio",
                    role = "Owner",
                )
            )

            normalizedEmail.contains("owner") -> listOf(
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




