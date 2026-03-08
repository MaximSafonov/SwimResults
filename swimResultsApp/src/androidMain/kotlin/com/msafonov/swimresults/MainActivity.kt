package com.msafonov.swimresults

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.msafonov.swimresults.data.storage.SettingsStorage
import com.msafonov.swimresults.di.initKoin
import com.msafonov.swimresults.ui.screens.SwimResultsApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initKoin()
        setContent {
            SwimResultsApp()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    SwimResultsApp()
}