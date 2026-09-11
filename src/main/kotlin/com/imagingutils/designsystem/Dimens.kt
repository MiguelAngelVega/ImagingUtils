package com.imagingutils.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout dimension tokens (spacing and component sizes), kept separate from the
 * UI code so sizing can be tuned in one place. Composables reference these
 * instead of hard-coding raw dp values.
 */
object AppDimens {
    // Spacing.
    val spaceXs: Dp = 4.dp
    val spaceSm: Dp = 8.dp
    val spaceMd: Dp = 12.dp

    // Compact buttons.
    val buttonHeight: Dp = 30.dp
    val buttonPaddingH: Dp = 12.dp
    val buttonPaddingV: Dp = 4.dp

    // Compact icon buttons.
    val iconButtonSize: Dp = 32.dp
    val iconSize: Dp = 18.dp

    // Panels and grid.
    val metadataPanelWidth: Dp = 340.dp
    val thumbnailsPanelDefaultWidth: Dp = 480.dp
    val thumbnailsPanelMinWidth: Dp = 220.dp
    val previewPaneMinWidth: Dp = 280.dp
    val thumbnailMinSize: Dp = 160.dp
}
