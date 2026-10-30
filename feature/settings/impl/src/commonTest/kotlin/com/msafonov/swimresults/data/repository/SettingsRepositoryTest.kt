package com.msafonov.swimresults.data.repository

import com.msafonov.swimresults.data.storage.SettingsStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SettingsRepositoryTest {
    @Test
    fun exposesStorageFlowAndDelegatesToggle() {
        val storage = FakeStorage()
        val repository = SettingsRepository(storage)

        assertSame(storage.isDark, repository.isDark)
        repository.toggleTheme()
        assertEquals(true, storage.isDark.value)
        repository.toggleTheme()
        assertEquals(false, storage.isDark.value)
    }

    private class FakeStorage : SettingsStorage {
        override val isDark = MutableStateFlow(false)
        override fun toggleTheme() { isDark.value = !isDark.value }
    }
}
