package com.dphascow.app.business

import com.apollographql.apollo.ApolloClient
import com.dphascow.app.graphql.BusinessWorkspaceQuery

class ApolloBusinessWorkspaceRepository(
    private val apolloClient: ApolloClient,
    private val accessTokenProvider: () -> String?,
) : BusinessWorkspaceRepository {
    override suspend fun loadBusinessWorkspace(
        businessId: String,
        lang: String,
    ): BusinessWorkspace {
        val businessPk = businessId.toIntOrNull()
            ?: throw IllegalArgumentException("Business id must be an integer for API requests")
        val token = accessTokenProvider()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Access token is missing")

        val response = apolloClient
            .query(BusinessWorkspaceQuery(businessId = businessPk, lang = lang))
            .addHttpHeader(AUTHORIZATION_HEADER, bearerValue(token))
            .execute()

        val business = response.data?.business
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty business workspace response")
        val employees = business.employees.map { employee ->
            BusinessEmployee(
                id = employee.pk.toString(),
                name = employee.user.fullName,
                role = employee.role.name,
                phone = employee.user.phone,
                email = employee.user.email,
                isActive = employee.isActive,
            )
        }

        return BusinessWorkspace(
            id = business.pk.toString(),
            name = business.name,
            phone = business.contactPhone,
            address = business.address,
            employees = employees,
            services = business.services.map { service ->
                BusinessService(
                    id = service.pk.toString(),
                    name = service.name.orEmpty().ifBlank { "#${service.pk}" },
                    duration = service.durations.toString(),
                    price = service.cost.toString(),
                    isActive = service.isActive,
                )
            },
            gallery = business.galleryImages.map { photo ->
                BusinessGalleryPhoto(
                    id = photo.pk.toString(),
                    imageUrl = photo.imageUrl,
                )
            },
            orders = business.bookings.map { booking ->
                BusinessOrder(
                    id = booking.pk.toString(),
                    clientName = booking.user.fullName,
                    serviceName = booking.services.firstOrNull()?.service?.name.orEmpty(),
                    employeeId = booking.employeeId.toString(),
                    dateTime = booking.bookingDate.toString(),
                    status = booking.status.name,
                )
            },
        )
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"

        fun bearerValue(token: String): String = "Bearer $token"
    }
}


