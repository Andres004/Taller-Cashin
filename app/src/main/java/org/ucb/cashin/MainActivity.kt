package org.ucb.cashin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.ucb.cashin.navigation.CashinRaiz
import org.ucb.cashin.ui.components.EstadoVacio
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.theme.CashinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val configurado = (application as CashinApp).container != null
        setContent {
            CashinTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (configurado) {
                        CashinRaiz()
                    } else {
                        // Falta local.properties: se explica en vez de cerrarse la app.
                        EstadoVacio(
                            titulo = stringResource(R.string.config_falta_titulo),
                            mensaje = stringResource(R.string.config_falta_mensaje),
                            emocion = Emocion.PREOCUPADO,
                            modifier = Modifier.safeDrawingPadding(),
                        )
                    }
                }
            }
        }
    }
}
