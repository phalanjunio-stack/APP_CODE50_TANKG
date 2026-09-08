package com.srlakes.tone.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srlakes.tone.ui.theme.SR
import com.srlakes.tone.ui.util.Format
import kotlin.math.cos
import kotlin.math.sin

/**
 * Botao gigante de cena. Alvo grande de proposito: precisa ser acertado
 * de pe, com a guitarra na mao, sem mirar.
 */
@Composable
fun SceneButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glow by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        label = "sceneGlow"
    )
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        accent.copy(alpha = 0.18f * glow),
                        SR.Surface
                    )
                )
            )
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) accent else SR.OutlineStrong,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent else SR.TextSecondary,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                color = if (selected) accent else SR.TextSecondary,
                fontSize = 16.sp,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
            if (selected) {
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(width = 18.dp, height = 2.dp)
                        .background(accent)
                )
            }
        }
    }
}


/**
 * Knob de macro. Arrasta para cima aumenta, para baixo diminui.
 *
 * Deslizar na vertical funciona melhor que um giro circular quando o
 * celular esta preso ao pedestal e o dedo chega de lado.
 */
@Composable
fun MacroKnob(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = SR.Electric,
    valueRange: ClosedFloatingPointRange<Float> = 0f..10f
) {
    val currentValue by rememberUpdatedState(value)
    val span = valueRange.endInclusive - valueRange.start
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = SR.TextSecondary,
            fontSize = 11.sp,
            maxLines = 1
        )
        Spacer(Modifier.height(5.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(valueRange) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        // 220 px de arrasto percorre a faixa inteira.
                        val delta = -dragAmount / 220f * span
                        onValueChange((currentValue + delta).coerceIn(valueRange.start, valueRange.endInclusive))
                    }
                }
        ) {
            val stroke = size.minDimension * 0.09f
            val inset = stroke / 2f + size.minDimension * 0.04f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val startAngle = 135f
            val sweepTotal = 270f

            drawArc(
                color = SR.Outline,
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = accent,
                startAngle = startAngle,
                sweepAngle = sweepTotal * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Corpo do knob
            val bodyRadius = size.minDimension / 2f - inset - stroke * 0.7f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = SR.SurfaceSunken, radius = bodyRadius, center = center)
            drawCircle(
                color = SR.Outline,
                radius = bodyRadius,
                center = center,
                style = Stroke(width = 1f)
            )

            // Indicador
            val angleDeg = startAngle + sweepTotal * fraction
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val inner = bodyRadius * 0.30f
            val outer = bodyRadius * 0.86f
            drawLine(
                color = accent,
                start = center + Offset(
                    (cos(angleRad) * inner).toFloat(),
                    (sin(angleRad) * inner).toFloat()
                ),
                end = center + Offset(
                    (cos(angleRad) * outer).toFloat(),
                    (sin(angleRad) * outer).toFloat()
                ),
                strokeWidth = stroke * 0.5f,
                cap = StrokeCap.Round
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = Format.oneDecimal(value),
            color = SR.TextPrimary,
            fontSize = 13.sp
        )
    }
}

/** Botao liga/desliga de efeito. */
@Composable
fun EffectToggle(
    label: String,
    icon: ImageVector,
    on: Boolean,
    accent: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (on) accent.copy(alpha = 0.14f) else SR.Surface)
                .border(
                    width = if (on) 2.dp else 1.dp,
                    color = if (on) accent else SR.Outline,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (on) accent else SR.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = if (on) SR.TextPrimary else SR.TextTertiary,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

/**
 * Barra de progresso fina, usada nas capturas de A/B e BASE x SOLO.
 */
@Composable
fun ThinProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    accent: Color = SR.Electric
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(SR.SurfaceSunken)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxSize()
                .background(accent)
        )
    }
}
