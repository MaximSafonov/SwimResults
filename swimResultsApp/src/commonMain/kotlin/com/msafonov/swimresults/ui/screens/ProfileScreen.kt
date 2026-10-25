package com.msafonov.swimresults.ui.screens

import org.jetbrains.compose.resources.stringResource
import swimresults.swimresultsapp.generated.resources.*
import com.msafonov.swimresults.ui.strings.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msafonov.swimresults.auth.AuthSignIn
import com.msafonov.swimresults.auth.AuthState
import com.msafonov.swimresults.auth.AuthError
import com.msafonov.swimresults.ui.theme.Theme
import com.msafonov.swimresults.viewmodel.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(onSettings: () -> Unit, onAddAthlete: () -> Unit, onEditAthlete: (Long) -> Unit,
                  viewModel: ProfileViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    var confirmLogout by rememberSaveable(session.epoch) { mutableStateOf(false) }
    val profile = state.profile.takeIf { it.session == session }
    val principal = session.principal
    Box(Modifier.fillMaxSize().background(Theme.colors.backGround), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(Res.string.profileScreenTitle), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Theme.colors.textPrimary)
            Text(if (principal == null) stringResource(Res.string.profileGuest) else principal.displayName.ifBlank { stringResource(Res.string.profileAccountFallback) },
                color = Theme.colors.textPrimary, fontSize = 20.sp)
            Text(stringResource(Res.string.profileLocalData), color = Theme.colors.textSecondary)
            when (val auth = session.auth) {
                AuthState.NotConfigured -> Text(stringResource(Res.string.authNotConfigured), color = Theme.colors.textSecondary)
                AuthState.Unsupported -> Text(stringResource(Res.string.authUnsupported), color = Theme.colors.textSecondary)
                AuthState.Restoring -> { CircularProgressIndicator(); Text(stringResource(Res.string.authRestoring), color = Theme.colors.textPrimary) }
                is AuthState.Guest -> {
                    Text(stringResource(Res.string.authSignInHint), color = Theme.colors.textPrimary)
                    auth.error?.let { Text(authErrorText(it), color = Theme.colors.errorPrimary) }
                    AuthSignIn(viewModel.auth)
                    if (auth.error == AuthError.LogoutUnconfirmed) TextButton(onClick = viewModel::logout, enabled = !state.busy) { Text(stringResource(Res.string.authRetryLogout)) }
                }
                is AuthState.ReauthenticationRequired -> {
                    Text(stringResource(Res.string.authReauthenticate), color = Theme.colors.textPrimary)
                    Text(authErrorText(auth.error), color = Theme.colors.errorPrimary)
                    AuthSignIn(viewModel.auth)
                }
                is AuthState.Authenticated -> {
                    Text(stringResource(Res.string.profileAthletes), fontWeight = FontWeight.SemiBold, color = Theme.colors.textPrimary)
                    if (profile?.athletes.isNullOrEmpty()) Text(stringResource(Res.string.profileFirstAthleteHint), color = Theme.colors.textSecondary)
                    profile?.athletes?.forEach { athlete ->
                        Column(Modifier.fillMaxWidth().background(Theme.colors.backGround2, Theme.shapes.medium).padding(12.dp)) {
                            Text(athlete.name, color = Theme.colors.textPrimary, fontWeight = FontWeight.Medium)
                            Text(athleteMetadata(athlete.birthDate, athlete.rank, athlete.sportsGender), color = Theme.colors.textSecondary)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = { viewModel.select(athlete.id) }, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) {
                                    Text(if (athlete.id == profile?.activeAthleteId) stringResource(Res.string.athleteSelected) else stringResource(Res.string.athleteSelect))
                                }
                                TextButton(onClick = { onEditAthlete(athlete.id) }, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.athleteEdit)) }
                            }
                        }
                    }
                    Button(onClick = onAddAthlete, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(Res.string.athleteAdd)) }
                    TextButton(onClick = { confirmLogout = true }, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.profileLogout)) }
                }
            }
            if (state.error) Text(stringResource(Res.string.profileActionError), color = Theme.colors.errorPrimary)
            HorizontalDivider()
            TextButton(onClick = onSettings, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(Res.string.profileThemeSettings)) }
            if (principal == null) Text(stringResource(Res.string.profileGuestFeatures), color = Theme.colors.textSecondary)
        }
    }
    if (confirmLogout && principal != null) AlertDialog(
        onDismissRequest = { confirmLogout = false }, title = { Text(stringResource(Res.string.profileLogoutTitle)) },
        text = { Text(stringResource(Res.string.profileLogoutDescription)) },
        confirmButton = { TextButton(onClick = { confirmLogout = false; viewModel.logout() }) { Text(stringResource(Res.string.logout)) } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(stringResource(Res.string.cancel)) } })
}

@Composable
private fun authErrorText(error: AuthError): String = when (error) {
    AuthError.Storage -> stringResource(Res.string.authStorageError)
    AuthError.Network -> stringResource(Res.string.authNetworkError)
    AuthError.Provider -> stringResource(Res.string.authProviderError)
    AuthError.Expired -> stringResource(Res.string.authExpiredError)
    AuthError.LogoutUnconfirmed -> stringResource(Res.string.authLogoutUnconfirmedError)
}
