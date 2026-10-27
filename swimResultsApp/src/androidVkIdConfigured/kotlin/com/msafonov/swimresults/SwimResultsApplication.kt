package com.msafonov.swimresults

import android.app.Application
import com.msafonov.swimresults.auth.initializeVkId

class SwimResultsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeVkId(this)
    }
}
