package com.dphascow.app.business

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.json.buildJsonString
import com.dphascow.app.graphql.AddServiceToEmployeeMutation
import com.dphascow.app.graphql.type.AddServiceInput
import kotlin.test.Test
import kotlin.test.assertTrue

class ServiceMutationVariablesTest {
    /**
     * The API stores a service name as a language-keyed JSON object. Guards against the
     * JSON scalar being mapped back to a Kotlin String, which would send it quoted instead.
     */
    @Test
    fun localisedNameIsSentAsJsonObject() {
        val mutation = AddServiceToEmployeeMutation(
            businessId = 1,
            employeeId = 2,
            input = AddServiceInput(
                name = mapOf("ru" to "Стрижка", "uz" to "Soch olish", "en" to "Haircut"),
                cost = 150000,
                durations = "01:00:00",
                categoryId = Optional.present(7),
                isActive = Optional.present(true),
            ),
            lang = "ru",
        )

        val variables = buildJsonString {
            beginObject()
            mutation.serializeVariables(this, CustomScalarAdapters.Empty, withDefaultValues = false)
            endObject()
        }

        assertTrue(
            variables.contains("\"name\":{\"ru\":\"Стрижка\""),
            "name must serialise as a JSON object, got: $variables",
        )
        assertTrue(variables.contains("\"durations\":\"01:00:00\""), variables)
        assertTrue(variables.contains("\"cost\":150000"), variables)
    }
}
