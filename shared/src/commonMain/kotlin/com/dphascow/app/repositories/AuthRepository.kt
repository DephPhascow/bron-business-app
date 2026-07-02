package com.dphascow.messenger.repositories

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.dphascow.BuildKonfig
import com.dphascow.graphql.RefreshTokenMutation
import com.dphascow.graphql.RegistrationMutation
import com.dphascow.graphql.ResendCodeMutation
import com.dphascow.graphql.TokenAuthMutation
import com.dphascow.graphql.VerifyCodeMutation
import com.dphascow.graphql.type.AuthInput
import com.dphascow.graphql.type.CreateUserInput
import com.dphascow.messenger.Screens
import com.dphascow.messenger.expects.PlatformLogger
import settings.AuthPref

class AuthRepository(prefs: AuthPref) {
    private val logger = PlatformLogger("GRAPHQL")

    private val apollo = ApolloClient.Builder()
        .serverUrl(BuildKonfig.API_URL)
        .addHttpHeader("Authorization", "Bearer ${prefs.accessToken ?: ""}")
        .build()

    suspend fun login(email: String, password: String): ApolloResponse<TokenAuthMutation.Data> {
        val mutation = TokenAuthMutation(
            input = AuthInput(
                email = email,
                password = password
            )
        )
        logger.log("mutation: $mutation")
        val response: ApolloResponse<TokenAuthMutation.Data> = apollo.mutation(mutation).execute()
        logger.log("Errors: ${response.errors.toString()}")
        logger.log("Exceptions: ${response.exception.toString()}")
        return response
    }
    suspend fun registration(email: String, password: String, firstName: String): ApolloResponse<RegistrationMutation.Data> {
        val mutation = RegistrationMutation(
            input = CreateUserInput(
                email = email,
                password = password,
                firstName = firstName
            )
        )
        logger.log("mutation: $mutation")
        val response: ApolloResponse<RegistrationMutation.Data> = apollo.mutation(mutation).execute()
        logger.log("Errors: ${response.errors.toString()}")
        logger.log("Exceptions: ${response.exception.toString()}")
        return response
    }
    suspend fun verifyCode(email: String, code: String): ApolloResponse<VerifyCodeMutation.Data> {
        val mutation = VerifyCodeMutation(email = email, code = code)
        logger.log("mutation: $mutation")
        val response: ApolloResponse<VerifyCodeMutation.Data> = apollo.mutation(mutation).execute()
        logger.log("Errors: ${response.errors.toString()}")
        logger.log("Exceptions: ${response.exception.toString()}")
        return response
    }
    suspend fun resendCode(email: String): ApolloResponse<ResendCodeMutation.Data> {
        val mutation = ResendCodeMutation(email = email)
        logger.log("mutation: $mutation")
        val response: ApolloResponse<ResendCodeMutation.Data> = apollo.mutation(mutation).execute()
        logger.log("Errors: ${response.errors.toString()}")
        logger.log("Exceptions: ${response.exception.toString()}")
        return response
    }
}
