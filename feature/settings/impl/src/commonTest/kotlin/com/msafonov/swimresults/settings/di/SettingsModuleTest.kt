package com.msafonov.swimresults.settings.di

import com.msafonov.swimresults.settings.api.Settings
import com.msafonov.swimresults.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertNotSame

class SettingsModuleTest {
    @Test
    fun viewModelRegistrationCreatesDistinctInstances() {
        val app = org.koin.dsl.koinApplication {
            modules(settingsModule, module { single<Settings> { FakeSettings() } })
        }
        try {
            assertNotSame(app.koin.get<SettingsViewModel>(), app.koin.get<SettingsViewModel>())
        } finally {
            app.close()
        }
    }

    private class FakeSettings : Settings {
        override val isDark = MutableStateFlow(false)
        override fun toggleTheme() { isDark.value = !isDark.value }
    }
}
