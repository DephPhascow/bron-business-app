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
    val services: List<BusinessService>,
    val gallery: List<BusinessGalleryPhoto>,
    val orders: List<BusinessOrder>,
)

data class BusinessReview(
    val mark: Int,
    val comment: String,
)

data class BusinessEmployee(
    val id: String,
    val userId: String,
    val name: String,
    val role: EmployeeRole,
    val specialisationId: String?,
    val specialisationName: String?,
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

data class BusinessService(
    val id: String,
    val name: String,
    val duration: String,
    val price: String,
    val isActive: Boolean,
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

