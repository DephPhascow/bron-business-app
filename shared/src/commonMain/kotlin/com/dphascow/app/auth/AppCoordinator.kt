package com.dphascow.app.auth

import com.dphascow.app.expects.PickedPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import settings.Prefs

class AppCoordinator(
    private val prefs: Prefs,
    private val authRepository: AuthRepository,
) {
    private val cachedBusinesses = mutableListOf<BusinessOption>()

    private val _state = MutableStateFlow<AppUiState>(AppUiState.Loading)
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    fun bootstrap() {
        val selectedBusinessId = prefs.selectedBusinessId
        val selectedBusinessName = prefs.selectedBusinessName
        val selectedBusinessRole = prefs.selectedBusinessRole
        val hasSession = prefs.accessToken != null && prefs.refreshToken != null
        val email = prefs.lastLoginEmail.orEmpty()

        _state.value = if (
            hasSession &&
            selectedBusinessId != null &&
            selectedBusinessName != null &&
            selectedBusinessRole != null
        ) {
            AppUiState.Authorized(
                email = email,
                business = BusinessOption(
                    id = selectedBusinessId,
                    name = selectedBusinessName,
                    role = selectedBusinessRole,
                ),
                canSwitchBusiness = false,
            )
        } else {
            AppUiState.Auth(email = email)
        }
    }

    fun updateEmail(email: String) {
        val current = _state.value as? AppUiState.Auth ?: return
        _state.value = current.copy(email = email, error = null)
    }

    fun updatePassword(password: String) {
        val current = _state.value as? AppUiState.Auth ?: return
        _state.value = current.copy(password = password, error = null)
    }

    fun openRegistration() {
        val current = _state.value as? AppUiState.Auth
        _state.value = AppUiState.Registration(
            email = current?.email.orEmpty(),
        )
    }

    fun cancelRegistration() {
        val current = _state.value as? AppUiState.Registration
        _state.value = AppUiState.Auth(
            email = current?.email.orEmpty(),
        )
    }

    fun updateRegistrationName(name: String) {
        val current = _state.value as? AppUiState.Registration ?: return
        _state.value = current.copy(name = name, error = null)
    }

    fun updateRegistrationEmail(email: String) {
        val current = _state.value as? AppUiState.Registration ?: return
        _state.value = current.copy(email = email, error = null)
    }

    fun updateRegistrationPassword(password: String) {
        val current = _state.value as? AppUiState.Registration ?: return
        _state.value = current.copy(password = password, error = null)
    }

    fun updateRememberChoice(enabled: Boolean) {
        prefs.rememberBusinessSelection = enabled
        val current = _state.value as? AppUiState.BusinessSelection ?: return
        _state.value = current.copy(rememberChoice = enabled)
    }

    fun openCreateBusiness() {
        val current = _state.value as? AppUiState.BusinessSelection ?: return
        _state.value = AppUiState.BusinessCreation(
            email = current.email,
            rememberChoice = current.rememberChoice,
        )
    }

    fun updateCreateBusinessName(name: String) {
        val current = _state.value as? AppUiState.BusinessCreation ?: return
        _state.value = current.copy(
            draft = current.draft.copy(name = name),
        )
    }

    fun uploadBusinessPhoto() {
        val current = _state.value as? AppUiState.BusinessCreation ?: return
        _state.value = current.copy(
            draft = current.draft.copy(photo = null),
        )
    }

    fun updateCreateBusinessPhoto(photo: PickedPhoto?) {
        val current = _state.value as? AppUiState.BusinessCreation ?: return
        _state.value = current.copy(
            draft = current.draft.copy(photo = photo),
        )
    }

    fun cancelCreateBusiness() {
        val current = _state.value as? AppUiState.BusinessCreation ?: return
        _state.value = AppUiState.BusinessSelection(
            email = current.email,
            businesses = cachedBusinesses.toList(),
            rememberChoice = current.rememberChoice,
        )
    }

    suspend fun login() {
        val current = _state.value as? AppUiState.Auth ?: return
        val email = current.email.trim()
        val password = current.password
        val validationError = validateCredentials(email = email, password = password)
        if (validationError != null) {
            _state.value = current.copy(email = email, error = validationError)
            return
        }

        _state.value = current.copy(
            email = email,
            isSubmitting = true,
            error = null,
        )

        runCatching {
            authRepository.login(email = email, password = password)
        }.onSuccess { result ->
            prefs.accessToken = result.accessToken
            prefs.refreshToken = result.refreshToken
            prefs.lastLoginEmail = email
            cachedBusinesses.clear()
            cachedBusinesses += result.businesses

            val rememberedBusiness = prefs.rememberedBusinessId
                ?.takeIf { prefs.rememberBusinessSelection }
                ?.let { rememberedId ->
                    result.businesses.firstOrNull { it.id == rememberedId }
                }

            when {
                rememberedBusiness != null -> activateBusiness(
                    email = email,
                    business = rememberedBusiness,
                    rememberChoice = true,
                )

                result.businesses.size == 1 && prefs.rememberBusinessSelection -> activateBusiness(
                    email = email,
                    business = result.businesses.single(),
                    rememberChoice = true,
                )

                else -> {
                    prefs.clearSelectedBusiness()
                    if (!prefs.rememberBusinessSelection) {
                        prefs.clearRememberedBusiness()
                    }
                    _state.value = AppUiState.BusinessSelection(
                        email = email,
                        businesses = result.businesses,
                        rememberChoice = prefs.rememberBusinessSelection,
                    )
                }
            }
        }.onFailure {
            _state.value = current.copy(
                email = email,
                isSubmitting = false,
                error = if (it is IllegalArgumentException) AuthError.INVALID_CREDENTIALS else AuthError.UNKNOWN,
            )
        }
    }

    suspend fun register() {
        val current = _state.value as? AppUiState.Registration ?: return
        val email = current.email.trim().ifBlank { "mock-user@example.com" }

        _state.value = current.copy(
            email = email,
            isSubmitting = true,
            error = null,
        )

        prefs.accessToken = "mock-registration-access-${email.lowercase()}"
        prefs.refreshToken = "mock-registration-refresh-${email.lowercase()}"
        prefs.lastLoginEmail = email
        cachedBusinesses.clear()
        prefs.clearSelectedBusiness()

        _state.value = AppUiState.BusinessSelection(
            email = email,
            businesses = emptyList(),
            rememberChoice = prefs.rememberBusinessSelection,
        )
    }

    suspend fun selectBusiness(businessId: String) {
        val current = _state.value as? AppUiState.BusinessSelection ?: return
        _state.value = current.copy(isSelecting = true)

        runCatching {
            authRepository.selectBusiness(businessId)
        }.onSuccess { result ->
            activateBusiness(
                email = current.email,
                business = result.business,
                rememberChoice = current.rememberChoice,
            )
        }.onFailure {
            _state.value = current.copy(isSelecting = false)
        }
    }

    suspend fun createBusiness() {
        val current = _state.value as? AppUiState.BusinessCreation ?: return
        _state.value = current.copy(isSubmitting = true)

        runCatching {
            authRepository.createBusiness(
                name = current.draft.name,
                photo = current.draft.photo,
            )
        }.onSuccess { result ->
            cachedBusinesses.removeAll { it.id == result.business.id }
            cachedBusinesses += result.business
            activateBusiness(
                email = current.email,
                business = result.business,
                rememberChoice = current.rememberChoice,
            )
        }.onFailure {
            _state.value = current.copy(isSubmitting = false)
        }
    }

    fun openBusinessSelection() {
        val current = _state.value as? AppUiState.Authorized ?: return
        if (cachedBusinesses.size <= 1) return

        prefs.clearSelectedBusiness()
        _state.value = AppUiState.BusinessSelection(
            email = current.email,
            businesses = cachedBusinesses.toList(),
            rememberChoice = prefs.rememberBusinessSelection,
        )
    }

    fun logout() {
        cachedBusinesses.clear()
        prefs.clearAuthSession()
        _state.value = AppUiState.Auth(email = prefs.lastLoginEmail.orEmpty())
    }

    private fun activateBusiness(email: String, business: BusinessOption, rememberChoice: Boolean) {
        prefs.selectedBusinessId = business.id
        prefs.selectedBusinessName = business.name
        prefs.selectedBusinessRole = business.role
        if (rememberChoice) {
            prefs.rememberedBusinessId = business.id
        } else {
            prefs.clearRememberedBusiness()
        }
        _state.value = AppUiState.Authorized(
            email = email,
            business = business,
            canSwitchBusiness = cachedBusinesses.size > 1,
        )
    }

    private fun validateCredentials(email: String, password: String): AuthError? {
        if (AuthFeatureFlags.disableStrictValidation) {
            return null
        }

        if (email.isBlank()) return AuthError.EMPTY_EMAIL
        if (!EMAIL_REGEX.matches(email)) return AuthError.INVALID_EMAIL
        if (password.isBlank()) return AuthError.EMPTY_PASSWORD
        return null
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}






