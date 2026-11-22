package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.ui.components.PantallaPagoFlex
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.ui.theme.Dimens
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun LogrosScreen(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    PantallaPagoFlex(
        titulo = Pantalla.LOGROS.titulo,
        fase = Pantalla.LOGROS.fase,
        onVolver = viewModel::volver
    ) {
        var mostrar by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { mostrar = true }
        val avance by animateFloatAsState(
            targetValue = if (mostrar) (uiState.indicador / 100.0).toFloat() else 0f,
            animationSpec = tween(durationMillis = 1200),
            label = "avance"
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { avance },
                modifier = Modifier.size(160.dp),
                strokeWidth = 12.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    String.format("%.1f", uiState.indicador) + "%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text("cumplimiento", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(uiState.categoria, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(uiState.mensajeCategoria, style = MaterialTheme.typography.bodyMedium)

        TarjetaSeccion(titulo = "Solo tú ves esto") {
            Text(
                "Tus logros son privados. No hay rankings ni comparaciones con otras personas, y un logro obtenido nunca se pierde.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        val obtenidos = uiState.logros.filter { it.obtenido }
        val pendientes = uiState.logros.filter { !it.obtenido }

        TarjetaSeccion(titulo = "Obtenidos (${obtenidos.size})") {
            if (obtenidos.isEmpty()) {
                Text("Aún no tienes logros. ¡El primero está cerca!", style = MaterialTheme.typography.bodyMedium)
            }
            obtenidos.forEach { logro ->
                Row(modifier = Modifier.padding(vertical = Dimens.espacioMini)) {
                    Text("★ ", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.titleMedium)
                    Column {
                        Text(logro.titulo, style = MaterialTheme.typography.titleMedium)
                        Text(logro.descripcion, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (pendientes.isNotEmpty()) {
            TarjetaSeccion(titulo = "Próximas metas") {
                pendientes.forEach { logro ->
                    Row(modifier = Modifier.padding(vertical = Dimens.espacioMini)) {
                        Text("☆ ", style = MaterialTheme.typography.titleMedium)
                        Column {
                            Text(logro.titulo, style = MaterialTheme.typography.titleMedium)
                            Text(logro.descripcion, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
