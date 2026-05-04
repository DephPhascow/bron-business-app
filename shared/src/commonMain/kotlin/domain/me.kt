package domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.DrawableResource


data class MeMiniProfile(
    val avatar: DrawableResource?,
    val username: String
)

interface MeRepository {
    suspend fun getMe(): MeMiniProfile
}

class MeRepositoryMock : MeRepository {
    private val _me = MutableStateFlow(initialData())
    val meFlow: StateFlow<MeMiniProfile> = _me

    override suspend fun getMe(): MeMiniProfile = meFlow.value


    private fun initialData() = MeMiniProfile(
        avatar = null,
        username = "Anisa"
    )
}