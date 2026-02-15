package com.msafonov.swimresults

import androidx.compose.ui.window.ComposeUIViewController
import com.msafonov.swimresults.ui.screens.CheckResultScreen
import com.msafonov.swimresults.viewmodel.CheckResultViewModel

fun MainViewController() = ComposeUIViewController { CheckResultScreen( CheckResultViewModel()) }