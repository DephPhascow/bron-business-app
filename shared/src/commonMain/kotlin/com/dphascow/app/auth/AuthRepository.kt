package com.dphascow.app.auth

import com.dphascow.app.expects.PickedPhoto

interface AuthRepository {
    /** Requests a one-time confirmation code to be sent to the given phone (or email). */
    suspend fun requireCode(phoneOrEmail: String): Boolean

    /** Verifies the confirmation code and returns the authenticated session. */
    suspend fun verifyCode(phoneOrEmail: String, code: String): LoginResult

    suspend fun selectBusiness(businessId: String): BusinessSelectionResult

    suspend fun createBusiness(name: String, photo: PickedPhoto?): CreateBusinessResult
}
