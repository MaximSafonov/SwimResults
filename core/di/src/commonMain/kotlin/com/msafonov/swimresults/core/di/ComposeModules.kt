package com.msafonov.swimresults.core.di

import org.koin.core.module.Module
import org.koin.dsl.KoinConfiguration
import org.koin.dsl.includes
import org.koin.dsl.koinConfiguration

fun composeModules(base: KoinConfiguration, vararg modules: Module): KoinConfiguration =
    koinConfiguration {
        includes(base)
        modules(modules.toList())
    }
