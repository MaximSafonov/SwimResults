package com.msafonov.swimresults

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSThread
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberHistoryShareLauncher(): (String) -> Unit {
    val presenter = LocalUIViewController.current
    return remember(presenter) {
        { text: String ->
            check(NSThread.isMainThread) { "Sharing requires the main thread" }
            val view = presenter.view
            check(view.window != null && presenter.presentedViewController == null) { "Sharing is unavailable right now" }
            val activity = UIActivityViewController(listOf(text), null)
            activity.popoverPresentationController?.apply {
                sourceView = view
                sourceRect = view.bounds.useContents {
                    CGRectMake(origin.x + size.width / 2, origin.y + size.height / 2, 1.0, 1.0)
                }
                permittedArrowDirections = 0uL
            }
            presenter.presentViewController(activity, animated = true, completion = null)
        }
    }
}
