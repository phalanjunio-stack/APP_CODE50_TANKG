package com.srlakes.tone

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.srlakes.tone.ui.SRLakesApp
import com.srlakes.tone.ui.theme.SR
import com.srlakes.tone.ui.theme.SRLakesTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as SRLakesApplication).container

        // Confere se ha versao nova, sem incomodar: no maximo uma vez a
        // cada 6 horas, e so avisa se a checagem realmente achar algo.
        container.updateCoordinator.checkOnStartIfDue(container.appScope)

        // O celular fica preso ao pedestal durante o show inteiro: a tela
        // nao pode apagar no meio de uma musica.
        lifecycleScope.launch {
            container.settingsRepository.settings.collectLatest { settings ->
                if (settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }

        setContent {
            SRLakesTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SR.Background)
                ) {
                    SRLakesApp(container)
                }
            }
        }
    }
}
