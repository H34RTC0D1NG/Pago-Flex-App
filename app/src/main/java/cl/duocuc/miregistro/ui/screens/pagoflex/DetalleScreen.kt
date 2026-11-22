package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.EstadoCompromiso
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.model.pesos
import cl.duocuc.miregistro.ui.components.BotonPrincipal
import cl.duocuc.miregistro.ui.components.BotonSecundario
import cl.duocuc.miregistro.ui.components.ChipEstado
import cl.duocuc.miregistro.ui.components.FilaDato
import cl.duocuc.miregistro.ui.components.PantallaPagoFlex
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun DetalleScreen(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    PantallaPagoFlex(
        titulo = Pantalla.DETALLE.titulo,
        fase = Pantalla.DETALLE.fase,
        onVolver = viewModel::volver
    ) {
        val compromiso = uiState.compromisoSeleccionado()
        if (compromiso == null) {
            Text("No encontramos este compromiso.", style = MaterialTheme.typography.bodyMedium)
            return@PantallaPagoFlex
        }
        val bloqueo = viewModel.motivoBloqueo()

        TarjetaSeccion(titulo = compromiso.empresa) {
            ChipEstado(compromiso.estadoActual())
            FilaDato("Tipo", compromiso.tipo())
            FilaDato("Concepto", compromiso.concepto)
            FilaDato("Cuota", compromiso.textoCuota())
            FilaDato("Folio", compromiso.folio)
            FilaDato("Vencimiento", compromiso.vencimiento.texto())
            FilaDato("Monto", pesos(compromiso.monto))
            if (compromiso.diasAtraso() > 0) {
                FilaDato("Días de atraso", compromiso.diasAtraso().toString())
                FilaDato("Recargo", pesos(compromiso.recargo()))
            }
            FilaDato("Total a pagar", pesos(compromiso.total()), destacado = true)
        }

        if (compromiso.estadoActual() == EstadoCompromiso.VENCIDO) {
            Text(
                "Te falta este pago para quedar al día. Puedes hacerlo ahora en menos de un minuto.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (bloqueo != null) {
            TarjetaSeccion(titulo = "Antes de pagar") {
                Text(bloqueo, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (compromiso.porPagar() && bloqueo == null) {
            BotonPrincipal(
                texto = "Pagar " + pesos(compromiso.total()),
                onClick = viewModel::irAPagar,
                icono = R.drawable.ic_check,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (compromiso.porPagar()) {
            BotonSecundario(
                texto = "Tengo un problema con este cobro",
                onClick = viewModel::irAReportar,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val comprobante = uiState.comprobantes.find { it.folio == compromiso.folio }
        if (comprobante != null) {
            BotonSecundario(
                texto = "Ver comprobante",
                onClick = { viewModel.verComprobante(comprobante.numero) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        val reporte = uiState.reportes.find { it.folio == compromiso.folio }
        if (reporte != null) {
            TarjetaSeccion(titulo = "Tu reporte ${reporte.numero}") {
                FilaDato("Motivo", reporte.motivo.etiqueta)
                FilaDato("Estado", reporte.estado)
                Text("Te avisaremos cuando tengamos una respuesta.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
