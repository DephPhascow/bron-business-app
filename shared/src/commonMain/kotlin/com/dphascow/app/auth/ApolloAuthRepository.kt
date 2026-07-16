package com.dphascow.app.auth

import com.dphascow.app.expects.PickedPhoto
import com.dphascow.app.graphql.MeForAuthQuery
import com.dphascow.app.graphql.VerifyCodeMutation
import com.dphascow.app.repositories.ApiAuthClient

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
            ?: throw IllegalArgumentException(response.errors?.firstOrNull()?.message ?: "Empty verify code response")

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
                ?: throw IllegalArgumentException("Business not found"),
        )
    }

    override suspend fun createBusiness(name: String, photo: PickedPhoto?): CreateBusinessResult {
        val response = apiAuthClient.addBusiness(name = name.trim())

        val business = response.data?.addBusiness
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty add business response")

        val option = BusinessOption(
            id = business.pk.toString(),
            name = business.name,
            role = ROLE_OWNER,
        )
        knownBusinesses = (knownBusinesses + option).distinctBy { it.id }

        return CreateBusinessResult(business = option)
    }

    private suspend fun loadAuthorizedBusinesses(token: String): List<BusinessOption> {
        val response = apiAuthClient.meForAuth(token = token)

        val user = response.data?.meForAuth
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty meForAuth response")

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
