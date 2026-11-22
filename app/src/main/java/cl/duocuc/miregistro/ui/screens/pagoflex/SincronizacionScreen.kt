package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.EstadoOperacion
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.ui.components.AvisoSinConexion
import cl.duocuc.miregistro.ui.components.BotonPrincipal
import cl.duocuc.miregistro.ui.components.BotonSecundario
import cl.duocuc.miregistro.ui.components.FilaDato
import cl.duocuc.miregistro.ui.components.InterruptorConTexto
import cl.duocuc.miregistro.ui.components.MensajeOperacion
import cl.duocuc.miregistro.ui.components.PantallaPagoFlex
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun SincronizacionScreen(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    PantallaPagoFlex(
        titulo = Pantalla.SINCRONIZACION.titulo,
        fase = Pantalla.SINCRONIZACION.fase,
        onVolver = viewModel::volver
    ) {
        if (uiState.sinConexion) {
            AvisoSinConexion(uiState.ultimaSincronizacion)
        }
        TarjetaSeccion(titulo = "Estado de tus datos") {
            FilaDato("Última actualización", uiState.ultimaSincronizacion)
            FilaDato("Compromisos", uiState.compromisos.size.toString())
            FilaDato("Comprobantes", uiState.comprobantes.size.toString())
            FilaDato("Conexión", if (uiState.sinConexion) "Sin conexión" else "Conectado")
        }
        BotonPrincipal(
            texto = "Sincronizar ahora",
            onClick = viewModel::sincronizar,
            habilitado = uiState.estadoSync !is EstadoOperacion.Cargando,
            icono = R.drawable.ic_check,
            modifier = Modifier.fillMaxWidth()
        )
        MensajeOperacion(estado = uiState.estadoSync, onReintentar = viewModel::sincronizar)
        TarjetaSeccion(titulo = "Guardado en este teléfono") {
            Text(viewModel.resumenGuardado(), style = MaterialTheme.typography.bodyMedium)
            Text(
                "Tus ajustes, tu RUT y tus pagos quedan guardados aunque cierres la app.",
                style = MaterialTheme.typography.bodyMedium
            )
            BotonSecundario(
                texto = "Borrar datos guardados",
                onClick = viewModel::borrarDatosGuardados,
                modifier = Modifier.fillMaxWidth()
            )
        }
        TarjetaSeccion(titulo = "Modo de prueba") {
            InterruptorConTexto(
                texto = "Simular que no hay conexión",
                activado = uiState.sinConexion,
                onCambio = viewModel::onSinConexionCambia
            )
            Text(
                "Activa esta opción para ver qué pasa si falla la red: la app no inventa datos, te avisa y te deja reintentar.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
