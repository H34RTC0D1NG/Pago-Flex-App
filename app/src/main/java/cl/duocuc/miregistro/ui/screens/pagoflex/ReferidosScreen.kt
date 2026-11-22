package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.EstadoOperacion
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.nativo.compartirTexto
import cl.duocuc.miregistro.ui.components.BotonPrincipal
import cl.duocuc.miregistro.ui.components.BotonSecundario
import cl.duocuc.miregistro.ui.components.CampoTexto
import cl.duocuc.miregistro.ui.components.CasillaConTexto
import cl.duocuc.miregistro.ui.components.FilaDato
import cl.duocuc.miregistro.ui.components.MensajeOperacion
import cl.duocuc.miregistro.ui.components.PantallaPagoFlex
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun ReferidosScreen(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    val form = uiState.formReferido
    val context = LocalContext.current
    PantallaPagoFlex(
        titulo = Pantalla.REFERIDOS.titulo,
        fase = Pantalla.REFERIDOS.fase,
        onVolver = viewModel::volver
    ) {
        TarjetaSeccion(titulo = "Tu código de invitación") {
            Text(viewModel.codigoReferido(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "¿Le pagas a un jardín, club o edificio por transferencia? Recomiéndalo y, si se une, ganas un reconocimiento.",
                style = MaterialTheme.typography.bodyMedium
            )
            BotonSecundario(
                texto = "Compartir mi código",
                onClick = {
                    compartirTexto(
                        context,
                        "Te invito a PagoFlex",
                        "Paga tus compromisos en un solo lugar con PagoFlex. Usa mi código: " + viewModel.codigoReferido()
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        TarjetaSeccion(titulo = "Recomendar una organización") {
            CampoTexto(
                valor = form.organizacion,
                onValorCambia = viewModel::onOrganizacionCambia,
                etiqueta = "Nombre de la organización",
                icono = R.drawable.ic_seguir,
                error = form.errorOrganizacion
            )
            CampoTexto(
                valor = form.rubro,
                onValorCambia = viewModel::onRubroCambia,
                etiqueta = "Rubro (ej: jardín infantil)",
                icono = R.drawable.ic_check,
                error = form.errorRubro
            )
            CampoTexto(
                valor = form.comuna,
                onValorCambia = viewModel::onComunaCambia,
                etiqueta = "Comuna",
                icono = R.drawable.ic_ubicacion,
                error = form.errorComuna
            )
            CampoTexto(
                valor = form.contacto,
                onValorCambia = viewModel::onContactoCambia,
                etiqueta = "Contacto (opcional)",
                icono = R.drawable.ic_correo
            )
            CasillaConTexto(
                texto = "La persona de contacto autorizó que PagoFlex la contacte",
                marcado = form.autoriza,
                onCambio = viewModel::onAutorizaCambia
            )
            form.errorAutoriza?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        BotonPrincipal(
            texto = "Enviar recomendación",
            onClick = viewModel::onEnviarReferido,
            habilitado = uiState.estadoReferido !is EstadoOperacion.Cargando,
            icono = R.drawable.ic_seguir,
            modifier = Modifier.fillMaxWidth()
        )
        MensajeOperacion(estado = uiState.estadoReferido, onReintentar = viewModel::onEnviarReferido)
        Text("Máximo 5 recomendaciones. No hay premios en dinero.", style = MaterialTheme.typography.bodyMedium)

        TarjetaSeccion(titulo = "Mis recomendaciones") {
            uiState.recomendaciones.forEach { rec ->
                FilaDato(rec.organizacion, rec.estado.etiqueta)
                LinearProgressIndicator(
                    progress = { rec.estado.avance },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
