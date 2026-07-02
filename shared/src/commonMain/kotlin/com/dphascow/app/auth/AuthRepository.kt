package com.dphascow.app.auth

import com.dphascow.app.expects.PickedPhoto

interface AuthRepository {
    suspend fun login(email: String, password: String): LoginResult

    suspend fun selectBusiness(businessId: String): BusinessSelectionResult

    suspend fun createBusiness(name: String, photo: PickedPhoto?): CreateBusinessResult
}



