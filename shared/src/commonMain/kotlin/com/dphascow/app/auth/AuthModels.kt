package com.dphascow.app.auth

import com.dphascow.app.expects.PickedPhoto

data class BusinessOption(
    val id: String,
    val name: String,
    val role: String,
)

data class LoginResult(
    val accessToken: String,
    val refreshToken: String,
    val businesses: List<BusinessOption>,
)

data class BusinessSelectionResult(
    val business: BusinessOption,
)

data class CreateBusinessResult(
    val business: BusinessOption,
)

data class CreateBusinessDraft(
    val name: String = "",
    val photo: PickedPhoto? = null,
)

val CreateBusinessDraft.hasPhoto: Boolean
    get() = photo != null

enum class AuthError {
    EMPTY_PHONE,
    INVALID_PHONE,
    CODE_SEND_FAILED,
    EMPTY_CODE,
    INVALID_CODE,
    UNKNOWN,
}

/** Stage of the passwordless phone authentication flow. */
enum class AuthStage {
    PHONE,
    CODE,
}

sealed interface AppUiState {
    data object Loading : AppUiState

    data class Auth(
        val phone: String = "",
        val code: String = "",
        val stage: AuthStage = AuthStage.PHONE,
        val isSubmitting: Boolean = false,
        val error: AuthError? = null,
    ) : AppUiState

    data class BusinessSelection(
        val phone: String,
        val businesses: List<BusinessOption>,
        val rememberChoice: Boolean,
        val isSelecting: Boolean = false,
    ) : AppUiState

    data class BusinessCreation(
        val phone: String,
        val draft: CreateBusinessDraft = CreateBusinessDraft(),
        val rememberChoice: Boolean,
        val isSubmitting: Boolean = false,
    ) : AppUiState

    data class Authorized(
        val phone: String,
        val business: BusinessOption,
        val canSwitchBusiness: Boolean,
    ) : AppUiState
}
