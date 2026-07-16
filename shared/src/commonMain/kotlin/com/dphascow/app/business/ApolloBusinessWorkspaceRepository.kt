package com.dphascow.app.business

import com.apollographql.apollo.api.Optional
import com.dphascow.app.expects.PickedPhoto
import com.dphascow.app.graphql.AddEmployeesMutation
import com.dphascow.app.graphql.AddGalleryImageMutation
import com.dphascow.app.graphql.BusinessWorkspaceQuery
import com.dphascow.app.graphql.CancelBookingMutation
import com.dphascow.app.graphql.DeleteEmployeesMutation
import com.dphascow.app.graphql.SpecialisationsQuery
import com.dphascow.app.graphql.UpdateBookingMutation
import com.dphascow.app.graphql.UpdateBusinessMutation
import com.dphascow.app.graphql.UpdateEmployeesMutation
import com.dphascow.app.graphql.type.EmployeeRoleEnum
import com.dphascow.app.graphql.type.ResultStatus
import com.dphascow.app.graphql.type.UpdateBooking
import com.dphascow.app.graphql.type.UpdateBusiness
import com.dphascow.app.graphql.type.UpdateEmployee
import com.dphascow.app.repositories.ApiAuthClient
import com.dphascow.app.repositories.FileUploader
import com.dphascow.app.repositories.Requester

class ApolloBusinessWorkspaceRepository(
    private val requester: Requester,
    private val apiAuthClient: ApiAuthClient,
    private val fileUploader: FileUploader,
) : BusinessWorkspaceRepository {
    override suspend fun loadBusinessWorkspace(
        businessId: String,
        lang: String,
    ): BusinessWorkspace {
        val businessPk = businessId.toIntOrNull()
            ?: throw IllegalArgumentException("Business id must be an integer for API requests")

        val response = requester.requestQuery(BusinessWorkspaceQuery(businessId = businessPk, lang = lang))

        val business = response.data?.business
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty business workspace response")

        return BusinessWorkspace(
            id = business.pk.toString(),
            name = business.name,
            phone = business.contactPhone,
            address = business.address,
            logoUrl = business.logoUrl,
            rating = business.middleStars,
            reviewsCount = business.reviewsCount,
            reviews = business.reviews.map { review ->
                BusinessReview(mark = review.reviewMark, comment = review.comments)
            },
            employees = business.employees.map { employee ->
                BusinessEmployee(
                    id = employee.pk.toString(),
                    userId = employee.userId.toString(),
                    name = employee.user.fullName,
                    role = employee.role.toDomain(),
                    specialisationId = employee.specialization.pk.toString(),
                    specialisationName = employee.specialization.name,
                    avatarUrl = employee.user.imageUrl,
                    phone = employee.user.phone,
                    email = employee.user.email,
                    isActive = employee.isActive,
                )
            },
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
                    clientUserId = booking.userId.toString(),
                    services = booking.services.map { s ->
                        BusinessOrderService(name = s.service.name.orEmpty().ifBlank { "#${s.service.pk}" }, cost = s.cost)
                    },
                    employeeId = booking.employeeId.toString(),
                    dateTime = booking.bookingDate.toString(),
                    status = booking.status.toDomain(),
                )
            },
        )
    }

    override suspend fun loadSpecialisations(lang: String): List<Specialisation> {
        val response = requester.requestQuery(SpecialisationsQuery(lang = lang))
        val list = response.data?.specialisations
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty specialisations response")

        return list.map { item ->
            Specialisation(
                id = item.pk.toString(),
                name = item.name.orEmpty().ifBlank { "#${item.pk}" },
            )
        }
    }

    override suspend fun requireEmployeeCode(phone: String): Boolean {
        val response = apiAuthClient.requireCode(phoneOrEmail = phone.trim())
        return response.data?.requireCode ?: false
    }

    override suspend fun verifyEmployeePhone(phone: String, code: String): String {
        // Dedicated mutation that only selects user.pk — the employee's tokens are
        // never requested, so the manager's session stays intact.
        val response = apiAuthClient.verifyEmployeeCode(phoneOrEmail = phone.trim(), code = code.trim())

        val userPk = response.data?.verifyCode?.user?.pk
            ?: throw IllegalArgumentException(response.errors?.firstOrNull()?.message ?: "Invalid code")

        return userPk.toString()
    }

    override suspend fun addEmployee(
        businessId: String,
        userId: String,
        role: EmployeeRole,
        specialisationId: String?,
        isActive: Boolean,
        lang: String,
    ): BusinessEmployee {
        val input = UpdateEmployee(
            businessId = Optional.present(businessId.toIntRequired("business id")),
            userId = Optional.present(userId.toIntRequired("user id")),
            role = Optional.present(role.toApi()),
            specializationId = Optional.presentIfNotNull(specialisationId?.toIntOrNull()),
            isActive = Optional.present(isActive),
        )

        val response = requester.requestMutation(AddEmployeesMutation(input = input, lang = lang))
        val employee = response.data?.addEmployees
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty add employee response")

        return BusinessEmployee(
            id = employee.pk.toString(),
            userId = employee.user.pk.toString(),
            name = employee.user.fullName,
            role = employee.role.toDomain(),
            specialisationId = employee.specialization.pk.toString(),
            specialisationName = employee.specialization.name,
            avatarUrl = employee.user.imageUrl,
            phone = employee.user.phone,
            email = employee.user.email,
            isActive = employee.isActive,
        )
    }

    override suspend fun updateEmployee(
        employeeId: String,
        role: EmployeeRole?,
        specialisationId: String?,
        isActive: Boolean?,
        lang: String,
    ): BusinessEmployee {
        val input = UpdateEmployee(
            role = Optional.presentIfNotNull(role?.toApi()),
            specializationId = Optional.presentIfNotNull(specialisationId?.toIntOrNull()),
            isActive = Optional.presentIfNotNull(isActive),
        )

        val response = requester.requestMutation(
            UpdateEmployeesMutation(pk = employeeId.toIntRequired("employee id"), input = input, lang = lang)
        )
        val employee = response.data?.updateEmployees
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty update employee response")

        return BusinessEmployee(
            id = employee.pk.toString(),
            userId = employee.user.pk.toString(),
            name = employee.user.fullName,
            role = employee.role.toDomain(),
            specialisationId = employee.specialization.pk.toString(),
            specialisationName = employee.specialization.name,
            avatarUrl = employee.user.imageUrl,
            phone = employee.user.phone,
            email = employee.user.email,
            isActive = employee.isActive,
        )
    }

    override suspend fun deleteEmployee(employeeId: String) {
        val response = requester.requestMutation(
            DeleteEmployeesMutation(pk = employeeId.toIntRequired("employee id"))
        )
        if (response.data?.deleteEmployees == null) {
            throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty delete employee response")
        }
    }

    override suspend fun cancelBooking(bookingId: String) {
        val response = requester.requestMutation(
            CancelBookingMutation(pk = bookingId.toIntRequired("booking id"))
        )
        if (response.data?.cancelBooking == null) {
            throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty cancel booking response")
        }
    }

    override suspend fun updateBookingStatus(bookingId: String, status: BookingStatus) {
        val input = UpdateBooking(status = Optional.present(status.toApi()))
        val response = requester.requestMutation(
            UpdateBookingMutation(pk = bookingId.toIntRequired("booking id"), input = input)
        )
        if (response.data?.updateBooking == null) {
            throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty update booking response")
        }
    }

    override suspend fun saveBusinessDetails(
        businessId: String,
        name: String?,
        contactPhone: String?,
        logoPhoto: PickedPhoto?,
    ) {
        val logoUrl = logoPhoto?.let { fileUploader.uploadImage(it.bytes, it.fileName, it.mimeType) }

        val input = UpdateBusiness(
            name = Optional.presentIfNotNull(name?.trim()?.ifBlank { null }),
            contactPhone = Optional.presentIfNotNull(contactPhone?.trim()?.ifBlank { null }),
            logoUrl = Optional.presentIfNotNull(logoUrl),
        )
        val response = requester.requestMutation(
            UpdateBusinessMutation(pk = businessId.toIntRequired("business id"), input = input)
        )
        if (response.data?.updateBusiness == null) {
            throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty update business response")
        }
    }

    override suspend fun addGalleryPhoto(businessId: String, photo: PickedPhoto) {
        val imageUrl = fileUploader.uploadImage(photo.bytes, photo.fileName, photo.mimeType)
        val response = requester.requestMutation(
            AddGalleryImageMutation(businessId = businessId.toIntRequired("business id"), imageUrl = imageUrl)
        )
        if (response.data?.addGalleryImage == null) {
            throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty add gallery image response")
        }
    }

    private fun String.toIntRequired(field: String): Int =
        toIntOrNull() ?: throw IllegalArgumentException("$field must be an integer for API requests")

    private fun EmployeeRoleEnum.toDomain(): EmployeeRole = when (this) {
        EmployeeRoleEnum.ADMIN -> EmployeeRole.ADMIN
        EmployeeRoleEnum.SPECIALIST -> EmployeeRole.SPECIALIST
        EmployeeRoleEnum.UNKNOWN__ -> EmployeeRole.SPECIALIST
    }

    private fun EmployeeRole.toApi(): EmployeeRoleEnum = when (this) {
        EmployeeRole.ADMIN -> EmployeeRoleEnum.ADMIN
        EmployeeRole.SPECIALIST -> EmployeeRoleEnum.SPECIALIST
    }

    private fun ResultStatus.toDomain(): BookingStatus = when (this) {
        ResultStatus.WAITING -> BookingStatus.WAITING
        ResultStatus.SUCCESS -> BookingStatus.SUCCESS
        ResultStatus.CANCELLED -> BookingStatus.CANCELLED
        ResultStatus.CLIENT_MISSING -> BookingStatus.CLIENT_MISSING
        ResultStatus.UNKNOWN__ -> BookingStatus.UNKNOWN
    }

    private fun BookingStatus.toApi(): ResultStatus = when (this) {
        BookingStatus.WAITING -> ResultStatus.WAITING
        BookingStatus.SUCCESS -> ResultStatus.SUCCESS
        BookingStatus.CANCELLED -> ResultStatus.CANCELLED
        BookingStatus.CLIENT_MISSING -> ResultStatus.CLIENT_MISSING
        BookingStatus.UNKNOWN -> ResultStatus.WAITING
    }
}
