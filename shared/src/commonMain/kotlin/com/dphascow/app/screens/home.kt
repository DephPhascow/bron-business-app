package com.dphascow.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dphascow.app.auth.AppUiState
import com.dphascow.app.resources.Res
import com.dphascow.app.resources.home_business_value
import com.dphascow.app.resources.home_change_business
import com.dphascow.app.resources.home_email_value
import com.dphascow.app.resources.home_logout
import com.dphascow.app.resources.home_role_value
import com.dphascow.app.resources.home_title
import org.jetbrains.compose.resources.stringResource
import ui.theme.T


@Composable
fun HomeScreen(
    state: AppUiState.Authorized,
    onChangeBusinessClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(T.d.paddingMain),
        verticalArrangement = Arrangement.spacedBy(T.d.lg)
    ) {
        Text(
            text = stringResource(Res.string.home_title),
            color = T.c.onBackground,
            style = T.t.headingH3
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = T.c.surface),
            shape = RoundedCornerShape(T.d.lg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(T.d.lg),
                verticalArrangement = Arrangement.spacedBy(T.d.md)
            ) {
                Text(
                    text = stringResource(Res.string.home_business_value, state.business.name),
                    color = T.c.onSurface,
                    style = T.t.t2Bold
                )
                Text(
                    text = stringResource(Res.string.home_role_value, state.business.role),
                    color = T.c.dark7,
                    style = T.t.t2Regular
                )
                Text(
                    text = stringResource(Res.string.home_email_value, state.email),
                    color = T.c.dark7,
                    style = T.t.t2Regular
                )
            }
        }

        if (state.canSwitchBusiness) {
            OutlinedButton(
                onClick = onChangeBusinessClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(Res.string.home_change_business))
            }
        }

        Button(
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(Res.string.home_logout))
        }
    }
}