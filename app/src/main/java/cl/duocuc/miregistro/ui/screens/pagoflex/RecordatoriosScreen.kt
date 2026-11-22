package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.ui.components.GrupoRadio
import cl.duocuc.miregistro.ui.components.InterruptorConTexto
import cl.duocuc.miregistro.ui.components.PantallaPagoFlex
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun RecordatoriosScreen(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    val config = uiState.config
    PantallaPagoFlex(
        titulo = Pantalla.RECORDATORIOS.titulo,
        fase = Pantalla.RECORDATORIOS.fase,
        onVolver = viewModel::volver
    ) {
        TarjetaSeccion(titulo = "Avisos de vencimiento") {
            InterruptorConTexto(
                texto = "Recibir recordatorios",
                activado = config.activos,
                onCambio = viewModel::onAvisosActivos
            )
        }
        if (config.activos) {
            TarjetaSeccion(titulo = "¿Con cuánta anticipación?") {
                GrupoRadio(
                    opciones = listOf(1, 3, 5),
                    seleccionada = config.diasAnticipacion,
                    onSeleccionar = viewModel::onDiasAnticipacion,
                    textoDe = { if (it == 1) "1 día antes" else "$it días antes" }
                )
            }
            TarjetaSeccion(titulo = "¿Cuántos avisos como máximo por mes?") {
                GrupoRadio(
                    opciones = listOf(1, 2),
                    seleccionada = config.maximoPorCiclo,
                    onSeleccionar = viewModel::onMaximoPorCiclo,
                    textoDe = { if (it == 1) "Solo 1 aviso" else "Hasta 2 avisos" }
                )
                InterruptorConTexto(
                    texto = "No avisarme entre las 22:00 y las 08:00",
                    activado = config.respetarHorario,
                    onCambio = viewModel::onRespetarHorario
                )
            }
        }
        TarjetaSeccion(titulo = "Así se verán tus avisos") {
            if (uiState.avisos.isEmpty()) {
                Text("Con esta configuración no tienes avisos por ahora.", style = MaterialTheme.typography.bodyMedium)
            }
            uiState.avisos.forEach {
                Text("• " + it.mensaje, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(
            "Pocos avisos y en buen tono: así no se vuelven molestos y siguen siendo útiles.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
