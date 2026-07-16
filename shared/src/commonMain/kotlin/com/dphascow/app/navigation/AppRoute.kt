package com.dphascow.app.navigation

sealed interface AppRoute {
    data object Dashboard : AppRoute
    data object BusinessSettings : AppRoute

    data object Employees : AppRoute
    data class EmployeeDetails(val employeeId: String) : AppRoute
    data class EmployeeEdit(val employeeId: String? = null) : AppRoute

    data object Services : AppRoute
    data class ServiceDetails(val serviceId: String) : AppRoute

    data object Gallery : AppRoute
    data object GalleryUpload : AppRoute

    data object Orders : AppRoute
    data class OrderDetails(val orderId: String) : AppRoute

    data object Analytics : AppRoute
    data object Reviews : AppRoute
    data object Account : AppRoute

    data object Chats : AppRoute
    data class Conversation(val chatId: String) : AppRoute
}

