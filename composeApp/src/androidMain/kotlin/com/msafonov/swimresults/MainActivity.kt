package com.msafonov.swimresults

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.msafonov.swimresults.ui.screens.CheckResultScreen
import com.msafonov.swimresults.viewmodel.CheckResultViewModel

class MainActivity : ComponentActivity() {
    val viewModel = CheckResultViewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CheckResultScreen(viewModel)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    CheckResultScreen(CheckResultViewModel())
}