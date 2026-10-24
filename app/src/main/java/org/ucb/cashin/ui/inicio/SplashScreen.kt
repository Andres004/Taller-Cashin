package org.ucb.cashin.ui.inicio

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.ucb.cashin.R
import org.ucb.cashin.ui.theme.Espacio

/** Se ve un instante al abrir la app mientras se recupera la sesión guardada. */
@Composable
fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espacio.l),
        ) {
            Image(
                painter = painterResource(R.drawable.logo_cashin),
                contentDescription = stringResource(R.string.logo_descripcion),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 200.dp, height = 170.dp),
            )
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}
