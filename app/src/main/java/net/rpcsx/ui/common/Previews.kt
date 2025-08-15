package net.rpcsx.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import net.rpcsx.RPCSXTheme

/**
 * Lightweight preview wrapper that applies the app theme and (optionally) a Material3 Surface.
 *
 * Notes:
 * - Android 10+ compatible (no dynamic color usage).
 * - Keeps previews visually consistent with the in-app theme.
 *
 * @param modifier        Modifier applied to the root container (Surface or direct content).
 * @param useSurface      When true, wraps content in a Material3 Surface.
 * @param darkTheme       Force dark or light mode for preview. If null, uses system setting.
 * @param tonalElevation  Optional tonal elevation when using Surface.
 * @param shape           Optional shape when using Surface.
 * @param contentPadding  Optional padding for the content area (for previews that need spacing).
 * @param content         Composable content to preview.
 */
@Composable
fun ComposePreview(
    modifier: Modifier = Modifier.fillMaxSize(),
    useSurface: Boolean = true,
    darkTheme: Boolean? = null,
    tonalElevation: Float = 0f,
    shape: Shape = MaterialTheme.shapes.medium,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    RPCSXTheme(darkTheme = darkTheme ?: false) {
        if (useSurface) {
            Surface(
                modifier = modifier,
                tonalElevation = tonalElevation,
                shape = shape,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                // keep it minimal; consumers can add their own padding/containers
                content()
            }
        } else {
            content()
        }
    }
}
