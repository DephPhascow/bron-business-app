package com.dphascow.app.auth

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Operation
import com.dphascow.app.expects.PickedPhoto
import com.dphascow.app.graphql.AddBusinessMutation
import com.dphascow.app.graphql.MeForAuthQuery
import com.dphascow.app.graphql.TokenAuthMutation

class ApolloAuthRepository(
    private val apolloClient: ApolloClient,
    initialAccessToken: String? = null,
) : AuthRepository {
    private var accessToken: String? = initialAccessToken
    private var knownBusinesses: List<BusinessOption> = emptyList()

    override suspend fun login(email: String, password: String): LoginResult {
        val response = apolloClient
            .mutation(TokenAuthMutation(email = email, password = password))
            .execute()

        val payload = response.data?.tokenAuth
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty login response")

        accessToken = payload.accessToken
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
        val response = apolloClient
            .mutation(AddBusinessMutation(name = name.trim()))
            .withBearerToken()
            .execute()

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
        val response = apolloClient
            .query(MeForAuthQuery())
            .addHttpHeader(AUTHORIZATION_HEADER, bearerValue(token))
            .execute()

        val user = response.data?.meForAuth
            ?: throw IllegalStateException(response.errors?.firstOrNull()?.message ?: "Empty meForAuth response")

        return user.toBusinessOptions()
    }

    private fun <D : Operation.Data> ApolloCall<D>.withBearerToken(): ApolloCall<D> {
        val token = accessToken?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Access token is missing")
        return addHttpHeader(AUTHORIZATION_HEADER, bearerValue(token))
    }

    private fun TokenAuthMutation.User.toBusinessOptions(): List<BusinessOption> = (
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
        const val AUTHORIZATION_HEADER = "Authorization"
        const val ROLE_OWNER = "OWNER"

        fun bearerValue(token: String): String = "Bearer $token"
    }
}
