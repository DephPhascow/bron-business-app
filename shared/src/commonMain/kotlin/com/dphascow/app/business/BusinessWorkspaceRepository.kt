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

    suspend fun addEmployee(
        businessId: String,
        userId: String,
        role: EmployeeRole,
        specialisationId: String?,
        isActive: Boolean,
        lang: String,
    ): BusinessEmployee

    suspend fun updateEmployee(
        employeeId: String,
        role: EmployeeRole?,
        specialisationId: String?,
        isActive: Boolean?,
        lang: String,
    ): BusinessEmployee

    suspend fun deleteEmployee(employeeId: String)

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
