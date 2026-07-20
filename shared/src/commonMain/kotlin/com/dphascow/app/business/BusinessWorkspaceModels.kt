package com.dphascow.app.business

data class BusinessWorkspace(
    val id: String,
    val name: String,
    val phone: String?,
    val address: String?,
    val logoUrl: String?,
    val rating: Double,
    val reviewsCount: Int,
    val reviews: List<BusinessReview>,
    val employees: List<BusinessEmployee>,
    val gallery: List<BusinessGalleryPhoto>,
    val orders: List<BusinessOrder>,
    val workTime: WeeklySchedule = WeeklySchedule(),
    val breakTime: WeeklySchedule = WeeklySchedule(),
)

enum class Weekday {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY,
}

/** A single day's opening interval. [start] and [stop] are "HH:mm" strings. */
data class DayInterval(
    val start: String,
    val stop: String,
)

/** A week of opening (or break) intervals, keyed by weekday. Days without an interval are closed. */
data class WeeklySchedule(
    val days: Map<Weekday, DayInterval> = emptyMap(),
) {
    operator fun get(day: Weekday): DayInterval? = days[day]

    fun with(day: Weekday, interval: DayInterval?): WeeklySchedule =
        WeeklySchedule(if (interval == null) days - day else days + (day to interval))

    val isEmpty: Boolean get() = days.isEmpty()
}

data class BusinessReview(
    val mark: Int,
    val comment: String,
)

data class BusinessEmployee(
    val id: String,
    val userId: String,
    val name: String,
    val role: EmployeeRole,
    val specialisations: List<Specialisation>,
    /** Services are owned by the employee who performs them, not by the business. */
    val services: List<BusinessService>,
    val avatarUrl: String?,
    val phone: String?,
    val email: String?,
    val isActive: Boolean,
)

enum class EmployeeRole {
    ADMIN,
    SPECIALIST,
}

data class Specialisation(
    val id: String,
    val name: String,
)

/** Human-readable list of an employee's specialisations, or `null` when they have none. */
val BusinessEmployee.specialisationSummary: String?
    get() = specialisations.joinToString(", ") { it.name }.ifBlank { null }

data class BusinessService(
    val id: String,
    /** Name in the currently requested language; may be blank if untranslated. */
    val name: String,
    /** All translations keyed by language code — what the edit form works with. */
    val nameByLang: Map<String, String>,
    val cost: Int,
    /** "HH:mm:ss", matching the `Time` scalar the API expects. */
    val duration: String,
    val categoryId: String?,
    val isActive: Boolean,
)

/** Language codes a service name can be entered in. */
val SERVICE_NAME_LANGS = listOf("ru", "uz", "en")

data class ServiceCategory(
    val id: String,
    val name: String,
)

data class BusinessGalleryPhoto(
    val id: String,
    val imageUrl: String,
)

data class BusinessOrder(
    val id: String,
    val clientName: String,
    val clientUserId: String,
    val services: List<BusinessOrderService>,
    val employeeId: String,
    val dateTime: String,
    val status: BookingStatus,
)

data class BusinessOrderService(
    val name: String,
    val cost: Int,
)

val BusinessOrder.total: Int
    get() = services.sumOf { it.cost }

val BusinessOrder.serviceSummary: String
    get() = services.joinToString(", ") { it.name }.ifBlank { "—" }

enum class BookingStatus {
    WAITING,
    SUCCESS,
    CANCELLED,
    CLIENT_MISSING,
    UNKNOWN,
}

