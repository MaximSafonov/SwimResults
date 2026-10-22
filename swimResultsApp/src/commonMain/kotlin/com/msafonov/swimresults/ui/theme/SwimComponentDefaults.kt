package com.msafonov.swimresults.ui.theme

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable

object SwimComponentDefaults {
    @Composable
    fun primaryButtonColors() = ButtonDefaults.buttonColors(
        containerColor = Theme.colors.primary,
        contentColor = Theme.colors.textContrast,
        disabledContainerColor = Theme.colors.backGround2,
        disabledContentColor = Theme.colors.textSecondary
    )

    @Composable
    fun secondaryButtonColors() = ButtonDefaults.buttonColors(
        containerColor = Theme.colors.backGround2,
        contentColor = Theme.colors.textPrimary,
        disabledContainerColor = Theme.colors.backGround2,
        disabledContentColor = Theme.colors.textSecondary
    )

    @Composable
    fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Theme.colors.textPrimary,
        unfocusedTextColor = Theme.colors.textPrimary,
        focusedContainerColor = Theme.colors.backGround2,
        unfocusedContainerColor = Theme.colors.backGround2,
        disabledContainerColor = Theme.colors.backGround2,
        errorContainerColor = Theme.colors.backGround2,
        focusedBorderColor = Theme.colors.surface2,
        unfocusedBorderColor = Theme.colors.surface1,
        cursorColor = Theme.colors.surface3,
        focusedLabelColor = Theme.colors.textPrimary,
        unfocusedLabelColor = Theme.colors.textSecondary,
        focusedTrailingIconColor = Theme.colors.textPrimary,
        unfocusedTrailingIconColor = Theme.colors.textPrimary
    )
}
