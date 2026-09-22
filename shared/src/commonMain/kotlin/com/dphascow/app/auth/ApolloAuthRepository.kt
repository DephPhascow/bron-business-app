package com.dphascow.app.auth

import com.dphascow.app.expects.PickedPhoto
import com.dphascow.app.graphql.MeForAuthQuery
import com.dphascow.app.graphql.VerifyCodeMutation
import com.dphascow.app.repositories.ApiAuthClient
import com.dphascow.app.resources.*
import com.dphascow.app.resources.Res
import org.jetbrains.compose.resources.getString

class ApolloAuthRepository(
    private val apiAuthClient: ApiAuthClient,
) : AuthRepository {
    private var knownBusinesses: List<BusinessOption> = emptyList()

    override suspend fun requireCode(phoneOrEmail: String): Boolean {
        val response = apiAuthClient.requireCode(phoneOrEmail = phoneOrEmail)
        return response.data?.requireCode ?: false
    }

    override suspend fun verifyCode(phoneOrEmail: String, code: String): LoginResult {
        val response = apiAuthClient.verifyCode(phoneOrEmail = phoneOrEmail, code = code)

        val payload = response.data?.verifyCode
            ?: throw IllegalArgumentException(response.errors?.firstOrNull()?.message ?: getString(Res.string.error_empty_response))

        val businesses = payload.user?.toBusinessOptions().orEmpty()
            .ifEmpty { loadAuthorizedBusinesses(payload.accessToken) }
        knownBusinesses = businesses

        return LoginResult(
            accessToken = payload.accessToken,
            refreshToken = payload.refreshToken,
            businesses = businesses,
        )
    }

    override suspend fun selectBusiness(businessId: String): BusinessSelectionResult {
        return BusinessSelectionResult(
            business = knownBusinesses.firstOrNull { it.id == businessId }
                ?: throw IllegalArgumentException(getString(Res.string.error_business_not_found)),
        )
    }

    override suspend fun createBusiness(name: String, photo: PickedPhoto?): CreateBusinessResult {
        val response = apiAuthClient.addBusiness(name = name.trim())

        val business = response.data?.addBusiness
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: getString(Res.string.error_empty_response))

        val option = BusinessOption(
            id = business.pk.toString(),
            name = business.name,
            role = ROLE_OWNER,
        )
        knownBusinesses = (knownBusinesses + option).distinctBy { it.id }

        return CreateBusinessResult(business = option)
    }

    override suspend fun logout(allDevices: Boolean) {
        knownBusinesses = emptyList()

        val status: Boolean
        val message: String?
        if (allDevices) {
            val response = apiAuthClient.logoutAllDevices()
            val result = response.data?.logoutAllDevices
            status = result?.status == true
            message = result?.message ?: response.errors?.firstOrNull()?.message
        } else {
            val response = apiAuthClient.logout()
            val result = response.data?.logout
            status = result?.status == true
            message = result?.message ?: response.errors?.firstOrNull()?.message
        }

        if (!status) throw IllegalStateException(message?.ifBlank { null } ?: getString(Res.string.error_logout_failed))
    }

    private suspend fun loadAuthorizedBusinesses(token: String): List<BusinessOption> {
        val response = apiAuthClient.meForAuth(token = token)

        val user = response.data?.meForAuth
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: getString(Res.string.error_empty_response))

        return user.toBusinessOptions()
    }

    private fun VerifyCodeMutation.User.toBusinessOptions(): List<BusinessOption> = (
        businesses.map { business ->
            BusinessOption(
                id = business.pk.toString(),
                name = business.name,
                role = ROLE_OWNER,
            )
        } + workedAsEmployee.map { employee ->
            BusinessOption(
                id = employee.business.pk.toString(),
                name = employee.business.name,
                role = employee.role.name,
            )
        }
    ).distinctBy { it.id }

    private fun MeForAuthQuery.MeForAuth.toBusinessOptions(): List<BusinessOption> = (
        businesses.map { business ->
            BusinessOption(
                id = business.pk.toString(),
                name = business.name,
                role = ROLE_OWNER,
            )
        } + workedAsEmployee.map { employee ->
            BusinessOption(
                id = employee.business.pk.toString(),
                name = employee.business.name,
                role = employee.role.name,
            )
        }
    ).distinctBy { it.id }

    private companion object {
        const val ROLE_OWNER = "OWNER"
    }
}
