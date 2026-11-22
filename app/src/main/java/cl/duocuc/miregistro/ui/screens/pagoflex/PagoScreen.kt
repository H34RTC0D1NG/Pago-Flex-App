package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.EstadoOperacion
import cl.duocuc.miregistro.model.MedioPago
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.model.pesos
import cl.duocuc.miregistro.ui.components.BotonPrincipal
import cl.duocuc.miregistro.ui.components.FilaDato
import cl.duocuc.miregistro.ui.components.GrupoRadio
import cl.duocuc.miregistro.ui.components.MensajeOperacion
import cl.duocuc.miregistro.ui.components.PantallaPagoFlex
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun PagoScreen(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    val compromiso = uiState.compromisoSeleccionado()
    PantallaPagoFlex(
        titulo = Pantalla.PAGO.titulo,
        fase = Pantalla.PAGO.fase,
        onVolver = viewModel::volver
    ) {
        if (compromiso == null) {
            Text("No hay compromiso seleccionado.", style = MaterialTheme.typography.bodyMedium)
            return@PantallaPagoFlex
        }
        TarjetaSeccion(titulo = "Vas a pagar") {
            FilaDato("Empresa", compromiso.empresa)
            FilaDato("Concepto", compromiso.concepto)
            FilaDato("Total", pesos(compromiso.total()), destacado = true)
        }
        TarjetaSeccion(titulo = "¿Cómo quieres pagar?") {
            GrupoRadio<MedioPago?>(
                opciones = MedioPago.entries,
                seleccionada = uiState.medioPago,
                onSeleccionar = { medio -> medio?.let { viewModel.onMedioSeleccionado(it) } },
                textoDe = { it?.etiqueta ?: "" }
            )
            uiState.errorMedio?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Pago simulado: no pedimos ni guardamos datos de tarjetas o cuentas.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        BotonPrincipal(
            texto = "Pagar " + pesos(compromiso.total()),
            onClick = viewModel::onSolicitarPago,
            habilitado = uiState.estadoPago !is EstadoOperacion.Cargando,
            icono = R.drawable.ic_check,
            modifier = Modifier.fillMaxWidth()
        )
        MensajeOperacion(estado = uiState.estadoPago, onReintentar = viewModel::onSolicitarPago)
    }

    if (uiState.confirmarPago && compromiso != null) {
        AlertDialog(
            onDismissRequest = viewModel::onCancelarPago,
            title = { Text("Confirmar pago") },
            text = {
                Text(
                    "Vas a pagar ${pesos(compromiso.total())} a ${compromiso.empresa} con " +
                        (uiState.medioPago?.etiqueta ?: "") + ". ¿Confirmas?"
                )
            },
            confirmButton = { TextButton(onClick = viewModel::onConfirmarPago) { Text("Sí, pagar") } },
            dismissButton = { TextButton(onClick = viewModel::onCancelarPago) { Text("Cancelar") } }
        )
    }
}
