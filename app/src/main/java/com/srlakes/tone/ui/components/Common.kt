package com.srlakes.tone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srlakes.tone.ui.theme.SR

/** Painel padrao do aplicativo: fundo levemente elevado e borda fina. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(SR.PanelCorner))
            .background(SR.SurfaceRaised)
            .border(1.dp, accent ?: SR.Outline, RoundedCornerShape(SR.PanelCorner))
            .padding(contentPadding)
    ) {
        content()
    }
}

/** Rotulo de secao: caixa alta, espacado, discreto. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = SR.TextTertiary,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            trailing()
        }
    }
}

/** Bolinha de status com texto: verde conectado, cinza nao. */
@Composable
fun StatusDot(
    connected: Boolean,
    modifier: Modifier = Modifier,
    connectedColor: Color = SR.Green,
    disconnectedColor: Color = SR.TextTertiary
) {
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (connected) connectedColor else disconnectedColor)
    )
}

@Composable
fun StatusPill(
    label: String,
    value: String,
    connected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SR.Surface)
            .border(1.dp, SR.Outline, RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = SR.TextPrimary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(connected)
                Spacer(Modifier.width(5.dp))
                Text(
                    text = value,
                    color = if (connected) SR.Green else SR.TextTertiary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/** Botao retangular grande, pensado para ser acertado sem olhar. */
@Composable
fun StageButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accent: Color = SR.Electric,
    enabled: Boolean = true,
    height: Int = 48
) {
    val border = when {
        !enabled -> SR.Outline
        selected -> accent
        else -> SR.OutlineStrong
    }
    val textColor = when {
        !enabled -> SR.TextTertiary
        selected -> accent
        else -> SR.TextSecondary
    }
    Box(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(alpha = 0.12f) else SR.Surface)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

/** Linha de rotulo + valor, alinhada, para blocos de numeros. */
@Composable
fun ValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = SR.TextPrimary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SR.TextSecondary, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 13.sp)
    }
}

/** Bloco de metrica: rotulo pequeno em cima, numero grande embaixo. */
@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = SR.TextPrimary,
    caption: String? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SR.Surface)
            .border(1.dp, SR.Outline, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            color = SR.TextTertiary,
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            maxLines = 1
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 17.sp,
            maxLines = 1
        )
        if (caption != null) {
            Text(text = caption, color = SR.TextTertiary, fontSize = 9.sp, maxLines = 1)
        }
    }
}
