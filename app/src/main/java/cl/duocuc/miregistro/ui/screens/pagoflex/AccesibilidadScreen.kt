package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.ui.components.InterruptorConTexto
import cl.duocuc.miregistro.ui.components.PantallaPagoFlex
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun AccesibilidadScreen(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    PantallaPagoFlex(
        titulo = Pantalla.ACCESIBILIDAD.titulo,
        fase = Pantalla.ACCESIBILIDAD.fase,
        onVolver = viewModel::volver
    ) {
        TarjetaSeccion(titulo = "Ver mejor") {
            InterruptorConTexto(
                texto = "Texto más grande",
                activado = uiState.textoGrande,
                onCambio = viewModel::onTextoGrande
            )
            InterruptorConTexto(
                texto = "Alto contraste",
                activado = uiState.altoContraste,
                onCambio = viewModel::onAltoContraste
            )
        }
        TarjetaSeccion(titulo = "Vista previa") {
            Text("Total por pagar este mes", style = MaterialTheme.typography.bodyMedium)
            Text(uiState.montoVisible(uiState.totalMes), style = MaterialTheme.typography.titleLarge)
            Text("Próximo vencimiento: ${uiState.proximo?.vencimiento?.texto() ?: "-"}", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "La app usa botones grandes, textos simples y descripciones para lectores de pantalla, pensando en personas de todas las edades.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
