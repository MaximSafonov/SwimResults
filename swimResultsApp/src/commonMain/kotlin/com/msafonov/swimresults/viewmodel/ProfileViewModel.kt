package com.msafonov.swimresults.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msafonov.swimresults.auth.AuthSession
import com.msafonov.swimresults.data.model.Profile
import com.msafonov.swimresults.data.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class ProfileUiState(val profile: Profile, val busy: Boolean = false, val error: Boolean = false)

@KoinViewModel
class ProfileViewModel(private val repository: ProfileRepository, val auth: AuthSession) : ViewModel() {
    private val mutable = MutableStateFlow(ProfileUiState(Profile(repository.session.current)))
    val uiState = mutable.asStateFlow()
    val session = repository.session.state

    init {
        viewModelScope.launch {
            try { auth.restore() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.value = mutable.value.copy(error = true) }
        }
        viewModelScope.launch {
            repository.profiles.catch {
                if (it is CancellationException) throw it
                mutable.value = ProfileUiState(Profile(repository.session.current), error = true)
            }.collect { mutable.value = ProfileUiState(it) }
        }
    }

    fun select(id: Long) {
        val snapshot = repository.session.current
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy = true, error = false)
        viewModelScope.launch {
            try { repository.select(snapshot, id) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (repository.session.isCurrent(snapshot)) mutable.value = mutable.value.copy(error = true) }
            finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    fun logout() {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy = true, error = false)
        viewModelScope.launch {
            try { auth.logout() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.value = mutable.value.copy(error = true) }
            finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }
}
