package com.msafonov.swimresults.viewmodel

import androidx.lifecycle.viewModelScope
import com.msafonov.swimresults.settings.api.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Test
    fun observesSettingsAndDelegatesToggle() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var viewModel: SettingsViewModel? = null
        var collector: kotlinx.coroutines.Job? = null
        try {
            val settings = FakeSettings()
            val model = SettingsViewModel(settings)
            viewModel = model
            collector = backgroundScope.launch { model.isDark.collect {} }
            advanceUntilIdle()
            assertFalse(model.isDark.value)
            model.toggleTheme()
            advanceUntilIdle()
            assertTrue(model.isDark.value)
        } finally {
            try {
                collector?.cancelAndJoin()
                viewModel?.viewModelScope?.cancel()
                runCurrent()
            } finally {
                Dispatchers.resetMain()
            }
        }
    }

    private class FakeSettings : Settings {
        override val isDark = MutableStateFlow(false)
        override fun toggleTheme() { isDark.value = !isDark.value }
    }
}
