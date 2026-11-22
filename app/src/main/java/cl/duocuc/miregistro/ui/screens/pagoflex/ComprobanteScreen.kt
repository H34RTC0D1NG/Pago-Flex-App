package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.model.pesos
import cl.duocuc.miregistro.nativo.compartirTexto
import cl.duocuc.miregistro.ui.components.BotonPrincipal
import cl.duocuc.miregistro.ui.components.BotonSecundario
import cl.duocuc.miregistro.ui.components.FilaDato
import cl.duocuc.miregistro.ui.components.PantallaPagoFlex
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun ComprobanteScreen(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    val context = LocalContext.current
    PantallaPagoFlex(
        titulo = Pantalla.COMPROBANTE.titulo,
        fase = Pantalla.COMPROBANTE.fase,
        onVolver = viewModel::volver
    ) {
        val comprobante = uiState.ultimoComprobante
        if (comprobante == null) {
            Text("No hay comprobante para mostrar.", style = MaterialTheme.typography.bodyMedium)
            return@PantallaPagoFlex
        }
        uiState.recompensa?.let {
            TarjetaSeccion(titulo = "¡Pago confirmado!") {
                Text(it, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Tu cumplimiento ahora es " + String.format("%.1f", uiState.indicador) + "% (" + uiState.categoria + ")",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        TarjetaSeccion(titulo = "Comprobante N° ${comprobante.numero}") {
            FilaDato("Fecha y hora", comprobante.fecha.texto() + " " + comprobante.hora)
            FilaDato("Pagado por", (uiState.usuario?.nombre ?: "") + " · RUT " + (uiState.usuario?.rutOculto() ?: ""))
            FilaDato("Empresa", comprobante.empresa)
            FilaDato("Folio", comprobante.folio)
            FilaDato("Concepto", comprobante.concepto)
            FilaDato("Medio de pago", comprobante.medio)
            FilaDato("Canal", comprobante.canal)
            FilaDato("¿A tiempo?", if (comprobante.aTiempo) "Sí" else "No")
            FilaDato("Total pagado", pesos(comprobante.monto), destacado = true)
            FilaDato("Estado", "Pago confirmado")
        }
        Text(
            "Este comprobante queda guardado y no se modifica.",
            style = MaterialTheme.typography.bodyMedium
        )
        BotonPrincipal(
            texto = "Ver mis logros",
            onClick = { viewModel.irA(Pantalla.LOGROS) },
            icono = R.drawable.ic_check,
            modifier = Modifier.fillMaxWidth()
        )
        BotonSecundario(
            texto = "Compartir comprobante",
            onClick = {
                val texto = "Comprobante PagoFlex N° ${comprobante.numero}\n" +
                    "Fecha: ${comprobante.fecha.texto()} ${comprobante.hora}\n" +
                    "RUT: ${uiState.usuario?.rutOculto() ?: ""}\n" +
                    "Empresa: ${comprobante.empresa}\n" +
                    "Concepto: ${comprobante.concepto}\n" +
                    "Total: ${pesos(comprobante.monto)}"
                compartirTexto(context, "Comprobante PagoFlex", texto)
            },
            modifier = Modifier.fillMaxWidth()
        )
        BotonSecundario(
            texto = "Volver al inicio",
            onClick = viewModel::volverAlInicio,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
