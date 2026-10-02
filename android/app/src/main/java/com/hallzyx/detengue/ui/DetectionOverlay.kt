package com.hallzyx.detengue.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hallzyx.detengue.core.model.NormalizedBox

/** Una caja lista para dibujar. La semántica (color/etiqueta) la decide quien la construye. */
data class OverlayBox(
    val box: NormalizedBox,
    val label: String,
    val color: Color,
    val labelColor: Color = Color.White,
)

/**
 * Dibuja cajas con coordenadas normalizadas 0..1 sobre un área que tiene exactamente la misma
 * geometría que el frame analizado (AC-06): x_px = x * ancho, y_px = y * alto.
 */
@Composable
fun DetectionOverlay(
    boxes: List<OverlayBox>,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)

    Canvas(modifier = modifier) {
        val strokeWidth = 4.dp.toPx()
        val padding = 6.dp.toPx()

        boxes.forEach { item ->
            val left = item.box.x * size.width
            val top = item.box.y * size.height
            val boxSize = Size(item.box.width * size.width, item.box.height * size.height)

            drawRect(
                color = item.color,
                topLeft = Offset(left, top),
                size = boxSize,
                style = Stroke(width = strokeWidth),
            )

            val layout = textMeasurer.measure(item.label, labelStyle.copy(color = item.labelColor))
            val labelW = layout.size.width + padding * 2
            val labelH = layout.size.height + padding
            // Etiqueta sobre la caja; si no hay espacio arriba, dentro de ella.
            val labelTop = if (top - labelH >= 0f) top - labelH else top
            val labelLeft = left.coerceIn(0f, (size.width - labelW).coerceAtLeast(0f))

            drawRect(
                color = item.color,
                topLeft = Offset(labelLeft, labelTop),
                size = Size(labelW, labelH),
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(labelLeft + padding, labelTop + padding / 2),
            )
        }
    }
}
