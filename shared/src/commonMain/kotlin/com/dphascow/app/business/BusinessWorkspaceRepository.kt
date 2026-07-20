package com.dphascow.app.business

import com.dphascow.app.expects.PickedPhoto

interface BusinessWorkspaceRepository {
    suspend fun loadBusinessWorkspace(
        businessId: String,
        lang: String,
    ): BusinessWorkspace

    suspend fun loadSpecialisations(lang: String): List<Specialisation>

    /**
     * Hires by phone number in one call — the user account is created if it does not
     * exist yet, and re-hiring someone previously dismissed reactivates them instead
     * of creating a duplicate.
     */
    suspend fun hireEmployee(
        businessId: String,
        phone: String,
        role: EmployeeRole,
        specialisationIds: List<String>,
        lang: String,
    ): BusinessEmployee

    /** Only the salon owner or an admin may change roles. */
    suspend fun updateEmployeeRole(
        businessId: String,
        employeeId: String,
        role: EmployeeRole,
        lang: String,
    ): BusinessEmployee

    /** Replaces the whole set of specialisations rather than adding to it. */
    suspend fun setEmployeeSpecialisations(
        businessId: String,
        employeeId: String,
        specialisationIds: List<String>,
        lang: String,
    ): List<Specialisation>

    /** Dismissal: the employee is deactivated and their history is kept. */
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

    /** Takes the service off sale (`isActive = false`); past bookings keep it. */
    suspend fun deleteEmployeeService(businessId: String, serviceId: String)

    /**
     * Marks the visit as happened. Revenue only counts `SUCCESS` bookings, so until
     * this is called the booking does not appear in analytics.
     */
    suspend fun completeBooking(businessId: String, bookingId: String)

    /** Marks the client as a no-show. */
    suspend fun markBookingClientMissing(businessId: String, bookingId: String)

    /** Cancellation initiated by the salon (the client has their own mutation). */
    suspend fun cancelBookingBySalon(businessId: String, bookingId: String)

    /**
     * Moves a booking to [date] (ISO date-time). The end time is recomputed from the
     * services in the booking; the server rejects busy slots and out-of-hours times.
     */
    suspend fun rescheduleBooking(bookingId: String, date: String)

    /**
     * Books a walk-in or phone-in client. The client is looked up by phone and created
     * if unknown; the usual "at least an hour ahead" rule does not apply.
     */
    suspend fun bookClientForDate(
        businessId: String,
        employeeId: String,
        serviceIds: List<String>,
        date: String,
        clientPhone: String,
        clientName: String?,
    )

    /** Today's schedule of the signed-in user as a specialist of [businessId]. */
    suspend fun loadMyBookings(
        businessId: String,
        dateFrom: String? = null,
        dateTo: String? = null,
        lang: String,
    ): List<EmployeeBooking>

    /**
     * Salon analytics. [employeeId] narrows it to one specialist — which is also the
     * only form a specialist (as opposed to an owner/admin) is allowed to request.
     */
    suspend fun loadAnalytics(
        businessId: String,
        employeeId: String? = null,
        periodStart: String? = null,
        periodEnd: String? = null,
        lang: String,
    ): BusinessAnalytics

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

    suspend fun deleteGalleryPhoto(businessId: String, imageId: String)
}
