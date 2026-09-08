package com.srlakes.tone.feature.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srlakes.tone.model.FollowState
import com.srlakes.tone.ui.components.StageButton
import com.srlakes.tone.ui.theme.SR

/**
 * Faixa do SR Lakes Studio na tela de palco.
 *
 * Ela responde, sem o músico ter que pensar, a uma pergunta só:
 * **o timbre está sendo comandado pelo notebook agora, ou por mim?**
 *
 * Por isso o estado MANUAL tem cor e botão próprios. Numa passagem de
 * som, descobrir tarde que o app parou de seguir é o tipo de coisa que
 * estraga a confiança na ferramenta inteira.
 */
@Composable
fun StudioFollowBar(
    followState: FollowState,
    nowPlaying: String?,
    onToggle: (Boolean) -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (followState) {
        FollowState.FOLLOWING -> SR.Green
        FollowState.MANUAL -> SR.Amber
        FollowState.UNMAPPED -> SR.Red
        FollowState.WAITING -> SR.Electric
        FollowState.DISABLED -> SR.TextTertiary
    }
    val detail = when (followState) {
        FollowState.FOLLOWING -> nowPlaying ?: "seguindo o notebook"
        FollowState.MANUAL -> "você assumiu — solta na próxima música"
        FollowState.UNMAPPED -> "música do Studio não associada"
        FollowState.WAITING -> "aguardando o Studio"
        FollowState.DISABLED -> "toque para seguir o notebook"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (followState == FollowState.DISABLED) SR.Surface
                else accent.copy(alpha = 0.10f)
            )
            .border(
                width = if (followState == FollowState.DISABLED) 1.dp else 1.5.dp,
                color = if (followState == FollowState.DISABLED) SR.Outline else accent.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (followState == FollowState.FOLLOWING) {
                    Icons.Filled.CastConnected
                } else {
                    Icons.Filled.Cast
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SR LAKES STUDIO",
                        color = SR.TextSecondary,
                        fontSize = 9.sp,
                        letterSpacing = 1.6.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Badge(followState.label.uppercase(), accent)
                }
                Text(
                    text = detail,
                    color = SR.TextTertiary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StageButton(
                text = if (followState == FollowState.DISABLED) "SEGUIR" else "SOLTAR",
                onClick = { onToggle(followState == FollowState.DISABLED) },
                selected = followState != FollowState.DISABLED,
                accent = accent,
                height = 34,
                modifier = Modifier.width(84.dp)
            )
        }

        if (followState == FollowState.MANUAL) {
            Spacer(Modifier.height(8.dp))
            StageButton(
                text = "VOLTAR A SEGUIR AGORA",
                onClick = onResume,
                accent = SR.Amber,
                selected = true,
                height = 38,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(text, color = color, fontSize = 8.sp, letterSpacing = 1.sp)
    }
}
