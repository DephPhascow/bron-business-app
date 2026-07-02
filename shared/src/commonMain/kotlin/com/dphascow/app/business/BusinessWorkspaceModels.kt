package com.dphascow.app.business

data class BusinessWorkspace(
    val id: String,
    val name: String,
    val phone: String?,
    val address: String?,
    val employees: List<BusinessEmployee>,
    val services: List<BusinessService>,
    val gallery: List<BusinessGalleryPhoto>,
    val orders: List<BusinessOrder>,
)

data class BusinessEmployee(
    val id: String,
    val name: String,
    val role: String,
    val phone: String?,
    val email: String?,
    val isActive: Boolean,
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
    val serviceName: String,
    val employeeId: String,
    val dateTime: String,
    val status: String,
)

