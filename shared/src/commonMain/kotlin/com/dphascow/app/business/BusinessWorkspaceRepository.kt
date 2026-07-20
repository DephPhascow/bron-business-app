package com.dphascow.app.business

import com.dphascow.app.expects.PickedPhoto

interface BusinessWorkspaceRepository {
    suspend fun loadBusinessWorkspace(
        businessId: String,
        lang: String,
    ): BusinessWorkspace

    suspend fun loadSpecialisations(lang: String): List<Specialisation>

    /** Step 1 of inviting an employee: send a one-time code to their phone. */
    suspend fun requireEmployeeCode(phone: String): Boolean

    /**
     * Step 2 of inviting an employee: verify the code they received and return
     * their user id (pk). The returned auth tokens are intentionally discarded so
     * that the manager's own session is preserved.
     */
    suspend fun verifyEmployeePhone(phone: String, code: String): String

    /**
     * Creates the employee and then assigns [specialisationIds] — the server takes
     * specialisations through a separate mutation, so this is two round-trips.
     */
    suspend fun addEmployee(
        businessId: String,
        userId: String,
        role: EmployeeRole,
        specialisationIds: List<String>,
        isActive: Boolean,
        lang: String,
    ): BusinessEmployee

    /** A `null` [specialisationIds] leaves the current specialisations untouched. */
    suspend fun updateEmployee(
        businessId: String,
        employeeId: String,
        role: EmployeeRole?,
        specialisationIds: List<String>?,
        isActive: Boolean?,
        lang: String,
    ): BusinessEmployee

    suspend fun deleteEmployee(businessId: String, employeeId: String)

    suspend fun loadCategories(lang: String): List<ServiceCategory>

    /** Services belong to the employee who performs them. [name] is keyed by language code. */
    suspend fun addEmployeeService(
        businessId: String,
        employeeId: String,
        name: Map<String, String>,
        cost: Int,
        duration: String,
        categoryId: String?,
        isActive: Boolean,
        lang: String,
    ): BusinessService

    /** Every `null` argument leaves the corresponding value untouched on the server. */
    suspend fun updateEmployeeService(
        businessId: String,
        serviceId: String,
        name: Map<String, String>?,
        cost: Int?,
        duration: String?,
        categoryId: String?,
        isActive: Boolean?,
        lang: String,
    ): BusinessService

    suspend fun deleteEmployeeService(businessId: String, serviceId: String)

    suspend fun cancelBooking(bookingId: String)

    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus)

    /**
     * Saves business details. If [logoPhoto] is provided it is uploaded first and stored as the logo.
     * An empty [workTime]/[breakTime] schedule leaves the corresponding value untouched on the server.
     */
    suspend fun saveBusinessDetails(
        businessId: String,
        name: String?,
        contactPhone: String?,
        logoPhoto: PickedPhoto?,
        workTime: WeeklySchedule = WeeklySchedule(),
        breakTime: WeeklySchedule = WeeklySchedule(),
    )

    /** Uploads a photo and attaches it to the business gallery. */
    suspend fun addGalleryPhoto(businessId: String, photo: PickedPhoto)
}
