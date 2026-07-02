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
    EMPTY_EMAIL,
    INVALID_EMAIL,
    EMPTY_PASSWORD,
    INVALID_CREDENTIALS,
    UNKNOWN,
}

sealed interface AppUiState {
    data object Loading : AppUiState

    data class Auth(
        val email: String = "",
        val password: String = "",
        val isSubmitting: Boolean = false,
        val error: AuthError? = null,
    ) : AppUiState

    data class Registration(
        val name: String = "",
        val email: String = "",
        val password: String = "",
        val isSubmitting: Boolean = false,
        val error: AuthError? = null,
    ) : AppUiState

    data class BusinessSelection(
        val email: String,
        val businesses: List<BusinessOption>,
        val rememberChoice: Boolean,
        val isSelecting: Boolean = false,
    ) : AppUiState

    data class BusinessCreation(
        val email: String,
        val draft: CreateBusinessDraft = CreateBusinessDraft(),
        val rememberChoice: Boolean,
        val isSubmitting: Boolean = false,
    ) : AppUiState

    data class Authorized(
        val email: String,
        val business: BusinessOption,
        val canSwitchBusiness: Boolean,
    ) : AppUiState
}





