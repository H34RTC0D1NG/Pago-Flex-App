package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.EstadoOperacion
import cl.duocuc.miregistro.model.MotivoReporte
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.ui.components.BotonPrincipal
import cl.duocuc.miregistro.ui.components.CampoTexto
import cl.duocuc.miregistro.ui.components.FilaDato
import cl.duocuc.miregistro.ui.components.GrupoRadio
import cl.duocuc.miregistro.ui.components.MensajeOperacion
import cl.duocuc.miregistro.ui.components.PantallaPagoFlex
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun ReportarScreen(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    PantallaPagoFlex(
        titulo = Pantalla.REPORTAR.titulo,
        fase = Pantalla.REPORTAR.fase,
        onVolver = viewModel::volver
    ) {
        uiState.compromisoSeleccionado()?.let {
            TarjetaSeccion(titulo = "Compromiso") {
                FilaDato("Empresa", it.empresa)
                FilaDato("Concepto", it.concepto)
                FilaDato("Folio", it.folio)
            }
        }
        TarjetaSeccion(titulo = "¿Qué pasó?") {
            GrupoRadio(
                opciones = MotivoReporte.entries,
                seleccionada = uiState.formReporte.motivo,
                onSeleccionar = viewModel::onMotivoReporte,
                textoDe = { it.etiqueta }
            )
            CampoTexto(
                valor = uiState.formReporte.comentario,
                onValorCambia = viewModel::onComentarioReporte,
                etiqueta = if (uiState.formReporte.motivo == MotivoReporte.OTRO) "Comentario (obligatorio)" else "Comentario (opcional)",
                icono = R.drawable.ic_correo,
                error = uiState.formReporte.errorComentario
            )
        }
        BotonPrincipal(
            texto = "Enviar reporte",
            onClick = viewModel::onEnviarReporte,
            habilitado = uiState.estadoReporte !is EstadoOperacion.Cargando,
            icono = R.drawable.ic_check,
            modifier = Modifier.fillMaxWidth()
        )
        MensajeOperacion(estado = uiState.estadoReporte, onReintentar = viewModel::onEnviarReporte)
        Text(
            "Mientras revisamos tu caso el compromiso queda en revisión y no se puede pagar.",
            style = MaterialTheme.typography.bodyMedium
        )
        if (uiState.reportes.isNotEmpty()) {
            TarjetaSeccion(titulo = "Mis reportes") {
                uiState.reportes.forEach {
                    FilaDato(it.numero, it.motivo.etiqueta + " · " + it.estado)
                }
            }
        }
    }
}
