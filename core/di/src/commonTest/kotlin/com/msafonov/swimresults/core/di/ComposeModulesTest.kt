package com.msafonov.swimresults.core.di

import org.koin.dsl.koinApplication
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeModulesTest {
    @Test
    fun includesBaseAndAdditionalModules() {
        val base = koinConfiguration { modules(module { single { "base" } }) }
        val additional = module { single { 42 } }
        val application = koinApplication(composeModules(base, additional))
        try {
            assertEquals("base", application.koin.get<String>())
            assertEquals(42, application.koin.get<Int>())
        } finally {
            application.close()
        }
    }
}
