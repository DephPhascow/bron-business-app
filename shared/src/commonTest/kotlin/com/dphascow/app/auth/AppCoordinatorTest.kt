package com.dphascow.app.auth

import com.dphascow.app.expects.PickedPhoto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import settings.Prefs

class AppCoordinatorTest {
    @Test
    fun `remembered single business opens authorized state`() = runTest {
        val prefs = Prefs(FakeSettings())
        prefs.rememberBusinessSelection = true
        val coordinator = AppCoordinator(
            prefs = prefs,
            authRepository = FakeAuthRepository(
                businesses = listOf(
                    BusinessOption(id = "solo", name = "Solo Studio", role = "Owner")
                )
            )
        )

        coordinator.bootstrap()
        coordinator.updateEmail("solo@bron.app")
        coordinator.updatePassword("123456")
        coordinator.login()

        val state = assertIs<AppUiState.Authorized>(coordinator.state.value)
        assertEquals("Solo Studio", state.business.name)
        assertEquals("solo", prefs.selectedBusinessId)
    }

    @Test
    fun `multiple businesses opens business selection`() = runTest {
        val prefs = Prefs(FakeSettings())
        val coordinator = AppCoordinator(
            prefs = prefs,
            authRepository = FakeAuthRepository(
                businesses = listOf(
                    BusinessOption(id = "1", name = "First", role = "Owner"),
                    BusinessOption(id = "2", name = "Second", role = "Staff"),
                )
            )
        )

        coordinator.bootstrap()
        coordinator.updateEmail("owner@bron.app")
        coordinator.updatePassword("123456")
        coordinator.login()

        val state = assertIs<AppUiState.BusinessSelection>(coordinator.state.value)
        assertEquals(2, state.businesses.size)
        assertFalse(state.isSelecting)
        assertFalse(state.rememberChoice)
    }

    @Test
    fun `remembered business is auto selected on next login`() = runTest {
        val prefs = Prefs(FakeSettings())
        val coordinator = AppCoordinator(
            prefs = prefs,
            authRepository = FakeAuthRepository(
                businesses = listOf(
                    BusinessOption(id = "first", name = "First", role = "Owner"),
                    BusinessOption(id = "second", name = "Second", role = "Staff"),
                )
            )
        )

        coordinator.bootstrap()
        coordinator.updateEmail("owner@bron.app")
        coordinator.updatePassword("123456")
        coordinator.login()
        coordinator.updateRememberChoice(true)
        coordinator.selectBusiness("second")

        coordinator.logout()
        coordinator.bootstrap()
        coordinator.updateEmail("owner@bron.app")
        coordinator.updatePassword("123456")
        coordinator.login()

        val state = assertIs<AppUiState.Authorized>(coordinator.state.value)
        assertEquals("second", state.business.id)
        assertEquals("second", prefs.rememberedBusinessId)
        assertTrue(prefs.rememberBusinessSelection)
    }

    @Test
    fun `logout clears tokens and selected business but keeps remembered choice`() = runTest {
        val prefs = Prefs(FakeSettings())
        prefs.rememberBusinessSelection = true
        prefs.rememberedBusinessId = "solo"
        val coordinator = AppCoordinator(
            prefs = prefs,
            authRepository = FakeAuthRepository(
                businesses = listOf(
                    BusinessOption(id = "solo", name = "Solo Studio", role = "Owner")
                )
            )
        )

        coordinator.bootstrap()
        coordinator.updateEmail("solo@bron.app")
        coordinator.updatePassword("123456")
        coordinator.login()
        coordinator.logout()

        val state = assertIs<AppUiState.Auth>(coordinator.state.value)
        assertEquals(null, prefs.accessToken)
        assertEquals(null, prefs.selectedBusinessId)
        assertEquals("solo", prefs.rememberedBusinessId)
        assertTrue(prefs.rememberBusinessSelection)
        assertEquals("solo@bron.app", state.email)
    }

    @Test
    fun `create business opens newly created business`() = runTest {
        val prefs = Prefs(FakeSettings())
        val coordinator = AppCoordinator(
            prefs = prefs,
            authRepository = FakeAuthRepository(
                businesses = listOf(
                    BusinessOption(id = "1", name = "First", role = "Owner"),
                )
            )
        )

        coordinator.bootstrap()
        coordinator.updateEmail("owner@bron.app")
        coordinator.updatePassword("123456")
        coordinator.login()
        coordinator.openCreateBusiness()
        coordinator.updateCreateBusinessName("New Salon")
        coordinator.updateCreateBusinessPhoto(PickedPhoto(bytes = byteArrayOf(1, 2, 3), fileName = "photo.jpg"))
        coordinator.createBusiness()

        val state = assertIs<AppUiState.Authorized>(coordinator.state.value)
        assertEquals("New Salon", state.business.name)
        assertEquals("created", state.business.id)
    }
}

private class FakeAuthRepository(
    private val businesses: List<BusinessOption>,
) : AuthRepository {
    override suspend fun login(email: String, password: String): LoginResult = LoginResult(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        businesses = businesses,
    )

    override suspend fun selectBusiness(businessId: String): BusinessSelectionResult = BusinessSelectionResult(
        business = businesses.first { it.id == businessId }
    )

    override suspend fun createBusiness(name: String, photo: PickedPhoto?): CreateBusinessResult = CreateBusinessResult(
        business = BusinessOption(
            id = "created",
            name = name.ifBlank { "New Business" },
            role = if (photo != null) "Owner" else "Administrator",
        )
    )
}

private class FakeSettings : com.russhwolf.settings.Settings {
    private val values = mutableMapOf<String, Any?>()

    override val keys: Set<String>
        get() = values.keys

    override val size: Int
        get() = values.size

    override fun clear() {
        values.clear()
    }

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun hasKey(key: String): Boolean = values.containsKey(key)

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int = values[key] as? Int ?: defaultValue

    override fun getIntOrNull(key: String): Int? = values[key] as? Int

    override fun putLong(key: String, value: Long) {
        values[key] = value
    }

    override fun getLong(key: String, defaultValue: Long): Long = values[key] as? Long ?: defaultValue

    override fun getLongOrNull(key: String): Long? = values[key] as? Long

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getString(key: String, defaultValue: String): String = values[key] as? String ?: defaultValue

    override fun getStringOrNull(key: String): String? = values[key] as? String

    override fun putFloat(key: String, value: Float) {
        values[key] = value
    }

    override fun getFloat(key: String, defaultValue: Float): Float = values[key] as? Float ?: defaultValue

    override fun getFloatOrNull(key: String): Float? = values[key] as? Float

    override fun putDouble(key: String, value: Double) {
        values[key] = value
    }

    override fun getDouble(key: String, defaultValue: Double): Double = values[key] as? Double ?: defaultValue

    override fun getDoubleOrNull(key: String): Double? = values[key] as? Double

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = values[key] as? Boolean ?: defaultValue

    override fun getBooleanOrNull(key: String): Boolean? = values[key] as? Boolean
}






