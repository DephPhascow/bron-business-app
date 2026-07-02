package com.dphascow.app.navigation

sealed interface AppRoute {
    data object Dashboard : AppRoute
    data object BusinessSettings : AppRoute

    data object Employees : AppRoute
    data class EmployeeDetails(val employeeId: String) : AppRoute
    data class EmployeeEdit(val employeeId: String? = null) : AppRoute

    data object Services : AppRoute
    data class ServiceDetails(val serviceId: String) : AppRoute
    data class ServiceEdit(val serviceId: String? = null) : AppRoute

    data object Gallery : AppRoute
    data object GalleryUpload : AppRoute

    data object Orders : AppRoute
    data class OrderDetails(val orderId: String) : AppRoute

    data object Analytics : AppRoute
}

