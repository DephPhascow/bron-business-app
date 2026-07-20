package com.dphascow.app.auth

import com.dphascow.app.expects.PickedPhoto
import com.dphascow.app.repositories.RateLimitException
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
        val phone = prefs.lastLoginPhone.orEmpty()

        _state.value = if (
            hasSession &&
            selectedBusinessId != null &&
            selectedBusinessName != null &&
            selectedBusinessRole != null
        ) {
            AppUiState.Authorized(
                phone = phone,
                business = BusinessOption(
                    id = selectedBusinessId,
                    name = selectedBusinessName,
                    role = selectedBusinessRole,
                ),
                canSwitchBusiness = false,
            )
        } else {
            AppUiState.Auth(phone = phone)
        }
    }

    fun updatePhone(phone: String) {
        val current = _state.value as? AppUiState.Auth ?: return
        _state.value = current.copy(phone = phone, error = null)
    }

    fun updateCode(code: String) {
        val current = _state.value as? AppUiState.Auth ?: return
        _state.value = current.copy(code = code, error = null)
    }

    /** Return from the code stage back to phone entry. */
    fun backToPhone() {
        val current = _state.value as? AppUiState.Auth ?: return
        _state.value = current.copy(
            stage = AuthStage.PHONE,
            code = "",
            isSubmitting = false,
            error = null,
        )
    }

    fun updateRememberChoice(enabled: Boolean) {
        prefs.rememberBusinessSelection = enabled
        val current = _state.value as? AppUiState.BusinessSelection ?: return
        _state.value = current.copy(rememberChoice = enabled)
    }

    /**
     * The same preference as the "remember my choice" box on the business picker,
     * toggled from settings while already inside a business.
     *
     * Turning it on has to record the business that is open right now: the flag on
     * its own remembers nothing, so the next login would still ask.
     */
    fun updateAutoEnterBusiness(enabled: Boolean) {
        prefs.rememberBusinessSelection = enabled
        if (enabled) {
            (_state.value as? AppUiState.Authorized)?.let { prefs.rememberedBusinessId = it.business.id }
        } else {
            prefs.clearRememberedBusiness()
        }
    }

    fun openCreateBusiness() {
        val current = _state.value as? AppUiState.BusinessSelection ?: return
        _state.value = AppUiState.BusinessCreation(
            phone = current.phone,
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
            phone = current.phone,
            businesses = cachedBusinesses.toList(),
            rememberChoice = current.rememberChoice,
        )
    }

    /** Request a one-time code for the entered phone and advance to the code stage. */
    suspend fun requestCode() {
        val current = _state.value as? AppUiState.Auth ?: return
        val phone = current.phone.trim()
        val validationError = validatePhone(phone)
        if (validationError != null) {
            _state.value = current.copy(phone = phone, error = validationError)
            return
        }

        _state.value = current.copy(
            phone = phone,
            isSubmitting = true,
            error = null,
        )

        runCatching {
            authRepository.requireCode(phoneOrEmail = phone)
        }.onSuccess { sent ->
            _state.value = if (sent) {
                current.copy(
                    phone = phone,
                    stage = AuthStage.CODE,
                    code = "",
                    isSubmitting = false,
                    error = null,
                )
            } else {
                current.copy(
                    phone = phone,
                    isSubmitting = false,
                    error = AuthError.CODE_SEND_FAILED,
                )
            }
        }.onFailure { failure ->
            _state.value = current.copy(
                phone = phone,
                isSubmitting = false,
                error = failure.toAuthError(AuthError.CODE_SEND_FAILED),
            )
        }
    }

    /** Resend the code while staying on the code stage. */
    suspend fun resendCode() {
        val current = _state.value as? AppUiState.Auth ?: return
        if (current.stage != AuthStage.CODE) return
        runCatching {
            authRepository.requireCode(phoneOrEmail = current.phone.trim())
        }.onFailure { failure ->
            // Resending is the easiest way to hit the 3-per-5-minutes limit, so the
            // failure has to reach the screen — it drives the cooldown timer.
            _state.value = current.copy(error = failure.toAuthError(AuthError.CODE_SEND_FAILED))
        }
    }

    /** Verify the entered code and continue into the business flow. */
    suspend fun verifyCode() {
        val current = _state.value as? AppUiState.Auth ?: return
        if (current.stage != AuthStage.CODE) return
        val phone = current.phone.trim()
        val code = current.code.trim()
        if (code.length != CODE_LENGTH) {
            _state.value = current.copy(error = AuthError.EMPTY_CODE)
            return
        }

        _state.value = current.copy(
            isSubmitting = true,
            error = null,
        )

        runCatching {
            authRepository.verifyCode(phoneOrEmail = phone, code = code)
        }.onSuccess { result ->
            prefs.accessToken = result.accessToken
            prefs.refreshToken = result.refreshToken
            prefs.lastLoginPhone = phone
            cachedBusinesses.clear()
            cachedBusinesses += result.businesses

            val rememberedBusiness = prefs.rememberedBusinessId
                ?.takeIf { prefs.rememberBusinessSelection }
                ?.let { rememberedId ->
                    result.businesses.firstOrNull { it.id == rememberedId }
                }

            when {
                rememberedBusiness != null -> activateBusiness(
                    phone = phone,
                    business = rememberedBusiness,
                    rememberChoice = true,
                )

                result.businesses.size == 1 && prefs.rememberBusinessSelection -> activateBusiness(
                    phone = phone,
                    business = result.businesses.single(),
                    rememberChoice = true,
                )

                else -> {
                    prefs.clearSelectedBusiness()
                    if (!prefs.rememberBusinessSelection) {
                        prefs.clearRememberedBusiness()
                    }
                    _state.value = AppUiState.BusinessSelection(
                        phone = phone,
                        businesses = result.businesses,
                        rememberChoice = prefs.rememberBusinessSelection,
                    )
                }
            }
        }.onFailure { failure ->
            _state.value = current.copy(
                phone = phone,
                stage = AuthStage.CODE,
                isSubmitting = false,
                error = failure.toAuthError(AuthError.INVALID_CODE),
            )
        }
    }

    /** A rate limit is not a wrong code — it must not read as "invalid code". */
    private fun Throwable.toAuthError(fallback: AuthError): AuthError =
        if (this is RateLimitException) AuthError.TOO_MANY_REQUESTS else fallback

    suspend fun selectBusiness(businessId: String) {
        val current = _state.value as? AppUiState.BusinessSelection ?: return
        _state.value = current.copy(isSelecting = true)

        runCatching {
            authRepository.selectBusiness(businessId)
        }.onSuccess { result ->
            activateBusiness(
                phone = current.phone,
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
                phone = current.phone,
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
            phone = current.phone,
            businesses = cachedBusinesses.toList(),
            rememberChoice = prefs.rememberBusinessSelection,
        )
    }

    /**
     * Drops the local session without touching the server. Used when the session is
     * already dead — an expired refresh token has nothing left to invalidate.
     */
    fun clearSession() {
        cachedBusinesses.clear()
        prefs.clearAuthSession()
        _state.value = AppUiState.Auth(phone = prefs.lastLoginPhone.orEmpty())
    }

    /**
     * Signs out: asks the server to invalidate the refresh token, then drops the local
     * session. The local session is cleared even if the server call fails — tapping
     * "log out" must sign you out offline too.
     */
    suspend fun logout(allDevices: Boolean = false) {
        runCatching { authRepository.logout(allDevices) }
        clearSession()
    }

    private fun activateBusiness(phone: String, business: BusinessOption, rememberChoice: Boolean) {
        prefs.selectedBusinessId = business.id
        prefs.selectedBusinessName = business.name
        prefs.selectedBusinessRole = business.role
        if (rememberChoice) {
            prefs.rememberedBusinessId = business.id
        } else {
            prefs.clearRememberedBusiness()
        }
        _state.value = AppUiState.Authorized(
            phone = phone,
            business = business,
            canSwitchBusiness = cachedBusinesses.size > 1,
        )
    }

    private fun validatePhone(phone: String): AuthError? {
        if (phone.isBlank()) return AuthError.EMPTY_PHONE
        val digits = phone.count { it.isDigit() }
        if (digits < MIN_PHONE_DIGITS) return AuthError.INVALID_PHONE
        return null
    }

    private companion object {
        const val CODE_LENGTH = 4
        const val MIN_PHONE_DIGITS = 9
    }
}
