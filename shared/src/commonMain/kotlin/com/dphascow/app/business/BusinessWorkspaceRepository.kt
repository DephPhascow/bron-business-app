package com.dphascow.app.business

interface BusinessWorkspaceRepository {
    suspend fun loadBusinessWorkspace(
        businessId: String,
        lang: String,
    ): BusinessWorkspace
}

