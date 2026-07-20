package com.dphascow.app.business

import com.apollographql.apollo.api.Optional
import com.dphascow.app.expects.PickedPhoto
import com.dphascow.app.graphql.AddEmployeesMutation
import com.dphascow.app.graphql.AddGalleryImageMutation
import com.dphascow.app.graphql.AddServiceToEmployeeMutation
import com.dphascow.app.graphql.BusinessWorkspaceQuery
import com.dphascow.app.graphql.CancelBookingMutation
import com.dphascow.app.graphql.CategoriesQuery
import com.dphascow.app.graphql.DeleteEmployeeMutation
import com.dphascow.app.graphql.DeleteEmployeeServiceMutation
import com.dphascow.app.graphql.SetEmployeeSpecializationsMutation
import com.dphascow.app.graphql.SpecialisationsQuery
import com.dphascow.app.graphql.UpdateBookingMutation
import com.dphascow.app.graphql.UpdateBusinessMutation
import com.dphascow.app.graphql.UpdateEmployeeServiceMutation
import com.dphascow.app.graphql.UpdateEmployeesMutation
import com.dphascow.app.graphql.fragment.ServiceFields
import com.dphascow.app.graphql.fragment.WorkTimeFields
import com.dphascow.app.graphql.type.AddServiceInput
import com.dphascow.app.graphql.type.EmployeeRoleEnum
import com.dphascow.app.graphql.type.FromToTimeInput
import com.dphascow.app.graphql.type.ResultStatus
import com.dphascow.app.graphql.type.UpdateBooking
import com.dphascow.app.graphql.type.UpdateBusiness
import com.dphascow.app.graphql.type.UpdateEmployee
import com.dphascow.app.graphql.type.UpdateServiceInput
import com.dphascow.app.graphql.type.WorkTimeInput
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
                    specialisations = employee.specializations.map { spec ->
                        Specialisation(id = spec.pk.toString(), name = specialisationName(spec.pk, spec.name))
                    },
                    services = employee.services.map { it.serviceFields.toDomain() },
                    avatarUrl = employee.user.imageUrl,
                    phone = employee.user.phone,
                    email = employee.user.email,
                    isActive = employee.isActive,
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
            workTime = business.workTime?.workTimeFields.toSchedule(),
            breakTime = business.breakTime?.workTimeFields.toSchedule(),
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
        specialisationIds: List<String>,
        isActive: Boolean,
        lang: String,
    ): BusinessEmployee {
        val input = UpdateEmployee(
            businessId = Optional.present(businessId.toIntRequired("business id")),
            userId = Optional.present(userId.toIntRequired("user id")),
            role = Optional.present(role.toApi()),
            isActive = Optional.present(isActive),
        )

        val response = requester.requestMutation(AddEmployeesMutation(input = input, lang = lang))
        val employee = response.data?.addEmployees
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty add employee response")

        val specialisations = setSpecialisations(businessId, employee.pk.toString(), specialisationIds, lang)
            ?: employee.specializations.map { Specialisation(it.pk.toString(), specialisationName(it.pk, it.name)) }

        return BusinessEmployee(
            id = employee.pk.toString(),
            userId = employee.user.pk.toString(),
            name = employee.user.fullName,
            role = employee.role.toDomain(),
            specialisations = specialisations,
            services = employee.services.map { it.serviceFields.toDomain() },
            avatarUrl = employee.user.imageUrl,
            phone = employee.user.phone,
            email = employee.user.email,
            isActive = employee.isActive,
        )
    }

    override suspend fun updateEmployee(
        businessId: String,
        employeeId: String,
        role: EmployeeRole?,
        specialisationIds: List<String>?,
        isActive: Boolean?,
        lang: String,
    ): BusinessEmployee {
        val input = UpdateEmployee(
            role = Optional.presentIfNotNull(role?.toApi()),
            isActive = Optional.presentIfNotNull(isActive),
        )

        val response = requester.requestMutation(
            UpdateEmployeesMutation(pk = employeeId.toIntRequired("employee id"), input = input, lang = lang)
        )
        val employee = response.data?.updateEmployees
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty update employee response")

        val specialisations = setSpecialisations(businessId, employeeId, specialisationIds, lang)
            ?: employee.specializations.map { Specialisation(it.pk.toString(), specialisationName(it.pk, it.name)) }

        return BusinessEmployee(
            id = employee.pk.toString(),
            userId = employee.user.pk.toString(),
            name = employee.user.fullName,
            role = employee.role.toDomain(),
            specialisations = specialisations,
            services = employee.services.map { it.serviceFields.toDomain() },
            avatarUrl = employee.user.imageUrl,
            phone = employee.user.phone,
            email = employee.user.email,
            isActive = employee.isActive,
        )
    }

    /**
     * Replaces the employee's specialisations with [specialisationIds] and returns the new list.
     * Returns `null` when [specialisationIds] is `null`, i.e. the caller wants them left as-is.
     */
    private suspend fun setSpecialisations(
        businessId: String,
        employeeId: String,
        specialisationIds: List<String>?,
        lang: String,
    ): List<Specialisation>? {
        if (specialisationIds == null) return null

        val response = requester.requestMutation(
            SetEmployeeSpecializationsMutation(
                businessId = businessId.toIntRequired("business id"),
                employeeId = employeeId.toIntRequired("employee id"),
                specializationIds = specialisationIds.map { it.toIntRequired("specialisation id") },
                lang = lang,
            )
        )
        val list = response.data?.setEmployeeSpecializations
            ?: throw IllegalStateException(
                response.errors?.firstOrNull()?.message ?: "Empty set specialisations response"
            )

        return list.filterNotNull().map { spec ->
            Specialisation(id = spec.pk.toString(), name = specialisationName(spec.pk, spec.name))
        }
    }

    override suspend fun deleteEmployee(businessId: String, employeeId: String) {
        val response = requester.requestMutation(
            DeleteEmployeeMutation(
                businessId = businessId.toIntRequired("business id"),
                employeeId = employeeId.toIntRequired("employee id"),
            )
        )
        if (response.data?.deleteEmployee == null) {
            throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty delete employee response")
        }
    }

    override suspend fun loadCategories(lang: String): List<ServiceCategory> {
        val response = requester.requestQuery(CategoriesQuery(lang = lang))
        val list = response.data?.categories
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty categories response")

        return list.map { item ->
            ServiceCategory(id = item.pk.toString(), name = item.name.orEmpty().ifBlank { "#${item.pk}" })
        }
    }

    override suspend fun addEmployeeService(
        businessId: String,
        employeeId: String,
        name: Map<String, String>,
        cost: Int,
        duration: String,
        categoryId: String?,
        isActive: Boolean,
        lang: String,
    ): BusinessService {
        val input = AddServiceInput(
            name = name,
            cost = cost,
            durations = duration,
            categoryId = Optional.presentIfNotNull(categoryId?.toIntOrNull()),
            isActive = Optional.present(isActive),
        )

        val response = requester.requestMutation(
            AddServiceToEmployeeMutation(
                businessId = businessId.toIntRequired("business id"),
                employeeId = employeeId.toIntRequired("employee id"),
                input = input,
                lang = lang,
            )
        )
        val service = response.data?.addServiceToEmployee
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty add service response")

        return service.serviceFields.toDomain()
    }

    override suspend fun updateEmployeeService(
        businessId: String,
        serviceId: String,
        name: Map<String, String>?,
        cost: Int?,
        duration: String?,
        categoryId: String?,
        isActive: Boolean?,
        lang: String,
    ): BusinessService {
        val input = UpdateServiceInput(
            name = Optional.presentIfNotNull(name),
            cost = Optional.presentIfNotNull(cost),
            durations = Optional.presentIfNotNull(duration),
            categoryId = Optional.presentIfNotNull(categoryId?.toIntOrNull()),
            isActive = Optional.presentIfNotNull(isActive),
        )

        val response = requester.requestMutation(
            UpdateEmployeeServiceMutation(
                businessId = businessId.toIntRequired("business id"),
                serviceId = serviceId.toIntRequired("service id"),
                input = input,
                lang = lang,
            )
        )
        val service = response.data?.updateEmployeeService
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty update service response")

        return service.serviceFields.toDomain()
    }

    override suspend fun deleteEmployeeService(businessId: String, serviceId: String) {
        val response = requester.requestMutation(
            DeleteEmployeeServiceMutation(
                businessId = businessId.toIntRequired("business id"),
                serviceId = serviceId.toIntRequired("service id"),
            )
        )
        if (response.data?.deleteEmployeeService == null) {
            throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty delete service response")
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
        workTime: WeeklySchedule,
        breakTime: WeeklySchedule,
    ) {
        val logoUrl = logoPhoto?.let { fileUploader.uploadImage(it.bytes, it.fileName, it.mimeType) }

        val input = UpdateBusiness(
            name = Optional.presentIfNotNull(name?.trim()?.ifBlank { null }),
            contactPhone = Optional.presentIfNotNull(contactPhone?.trim()?.ifBlank { null }),
            logoUrl = Optional.presentIfNotNull(logoUrl),
            workTime = workTime.toInput(),
            breakTime = breakTime.toInput(),
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

    /** Maps the `ServiceFields` GraphQL fragment into the domain [BusinessService]. */
    private fun ServiceFields.toDomain(): BusinessService = BusinessService(
        id = pk.toString(),
        name = name.orEmpty().ifBlank { "#$pk" },
        // `nameAll` is the JSON scalar — a language-keyed object we only trust to hold strings.
        nameByLang = (nameAll as? Map<*, *>)
            .orEmpty()
            .mapNotNull { (key, value) ->
                val code = key as? String ?: return@mapNotNull null
                val text = value as? String ?: return@mapNotNull null
                code to text
            }
            .toMap(),
        cost = cost,
        duration = durations,
        categoryId = categoryId?.toString(),
        isActive = isActive,
    )

    /** Specialisation names are localised and may be missing for the requested language. */
    private fun specialisationName(pk: Int, name: String?): String = name.orEmpty().ifBlank { "#$pk" }

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

    /** Maps the `WorkTimeFields` GraphQL fragment into the domain [WeeklySchedule]. */
    private fun WorkTimeFields?.toSchedule(): WeeklySchedule {
        if (this == null) return WeeklySchedule()
        val days = buildMap {
            monday?.let { put(Weekday.MONDAY, DayInterval(it.start.toHhMm(), it.stop.toHhMm())) }
            tuesday?.let { put(Weekday.TUESDAY, DayInterval(it.start.toHhMm(), it.stop.toHhMm())) }
            wednesday?.let { put(Weekday.WEDNESDAY, DayInterval(it.start.toHhMm(), it.stop.toHhMm())) }
            thursday?.let { put(Weekday.THURSDAY, DayInterval(it.start.toHhMm(), it.stop.toHhMm())) }
            friday?.let { put(Weekday.FRIDAY, DayInterval(it.start.toHhMm(), it.stop.toHhMm())) }
            saturday?.let { put(Weekday.SATURDAY, DayInterval(it.start.toHhMm(), it.stop.toHhMm())) }
            sunday?.let { put(Weekday.SUNDAY, DayInterval(it.start.toHhMm(), it.stop.toHhMm())) }
        }
        return WeeklySchedule(days)
    }

    /** Builds the GraphQL [WorkTimeInput]; an empty schedule is left absent so the server keeps its value. */
    private fun WeeklySchedule.toInput(): Optional<WorkTimeInput?> {
        if (isEmpty) return Optional.Absent
        return Optional.present(
            WorkTimeInput(
                monday = this[Weekday.MONDAY].toInput(),
                tuesday = this[Weekday.TUESDAY].toInput(),
                wednesday = this[Weekday.WEDNESDAY].toInput(),
                thursday = this[Weekday.THURSDAY].toInput(),
                friday = this[Weekday.FRIDAY].toInput(),
                saturday = this[Weekday.SATURDAY].toInput(),
                sunday = this[Weekday.SUNDAY].toInput(),
            )
        )
    }

    private fun DayInterval?.toInput(): Optional<FromToTimeInput?> =
        if (this == null) Optional.Absent
        else Optional.present(FromToTimeInput(start = start, stop = stop))

    /** The `Time` scalar comes back as ISO time (e.g. "08:00:00"); the UI only needs "HH:mm". */
    private fun String.toHhMm(): String = if (length >= 5) substring(0, 5) else this
}
