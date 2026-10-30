package com.msafonov.swimresults.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

@KoinApplication
object KoinApp

@Module
@ComponentScan("com.msafonov.swimresults")
@Configuration
class AppModule
