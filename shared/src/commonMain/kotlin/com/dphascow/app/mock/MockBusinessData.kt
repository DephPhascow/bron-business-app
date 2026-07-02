package com.dphascow.app.mock

data class MockEmployee(
    val id: String,
    val name: String,
    val role: String,
    val phone: String,
    val email: String,
)

data class MockService(
    val id: String,
    val name: String,
    val durationMinutes: Int,
    val price: String,
)

data class MockOrder(
    val id: String,
    val clientName: String,
    val serviceName: String,
    val employeeName: String,
    val dateTime: String,
    val status: String,
)

data class MockGalleryPhoto(
    val id: String,
    val title: String,
)

object MockBusinessData {
    val employees = listOf(
        MockEmployee(
            id = "employee-1",
            name = "employee-1",
            role = "role-1",
            phone = "+998 90 123 45 67",
            email = "anna@example.com",
        ),
        MockEmployee(
            id = "employee-2",
            name = "employee-2",
            role = "role-2",
            phone = "+998 91 222 33 44",
            email = "maria@example.com",
        ),
        MockEmployee(
            id = "employee-3",
            name = "employee-3",
            role = "role-3",
            phone = "+998 93 555 77 88",
            email = "dilnoza@example.com",
        ),
    )

    val services = listOf(
        MockService(
            id = "service-1",
            name = "service-1",
            durationMinutes = 60,
            price = "150 000 UZS",
        ),
        MockService(
            id = "service-2",
            name = "service-2",
            durationMinutes = 90,
            price = "220 000 UZS",
        ),
        MockService(
            id = "service-3",
            name = "service-3",
            durationMinutes = 150,
            price = "450 000 UZS",
        ),
    )

    val orders = listOf(
        MockOrder(
            id = "order-1",
            clientName = "client-1",
            serviceName = "service-1",
            employeeName = "employee-1",
            dateTime = "order-time-1",
            status = "status-1",
        ),
        MockOrder(
            id = "order-2",
            clientName = "client-2",
            serviceName = "service-2",
            employeeName = "employee-1",
            dateTime = "order-time-2",
            status = "status-2",
        ),
        MockOrder(
            id = "order-3",
            clientName = "client-3",
            serviceName = "service-3",
            employeeName = "employee-3",
            dateTime = "order-time-3",
            status = "status-3",
        ),
    )

    val gallery = listOf(
        MockGalleryPhoto(id = "photo-1", title = "photo-1"),
        MockGalleryPhoto(id = "photo-2", title = "photo-2"),
        MockGalleryPhoto(id = "photo-3", title = "photo-3"),
    )
}


