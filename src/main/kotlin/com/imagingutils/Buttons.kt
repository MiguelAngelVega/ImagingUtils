package com.imagingutils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Reusable buttons that apply the app's compact sizing tokens ([AppDimens]), so
 * button dimensions live in the theme layer rather than in each call site.
 */

private val compactPadding = PaddingValues(
    horizontal = AppDimens.buttonPaddingH,
    vertical = AppDimens.buttonPaddingV,
)

/** Filled button with compact height and padding. */
@Composable
fun CompactButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = Button(
    onClick = onClick,
    modifier = modifier.height(AppDimens.buttonHeight),
    enabled = enabled,
    contentPadding = compactPadding,
    content = content,
)

/** Text button with compact height and padding. */
@Composable
fun CompactTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = TextButton(
    onClick = onClick,
    modifier = modifier.height(AppDimens.buttonHeight),
    enabled = enabled,
    contentPadding = compactPadding,
    content = content,
)
