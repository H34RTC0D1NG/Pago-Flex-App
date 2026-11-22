package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.EstadoOperacion
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.ui.components.BotonPrincipal
import cl.duocuc.miregistro.ui.components.CampoContrasena
import cl.duocuc.miregistro.ui.components.CampoTexto
import cl.duocuc.miregistro.ui.components.MensajeOperacion
import cl.duocuc.miregistro.ui.components.PantallaPagoFlex
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun IngresoScreen(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    PantallaPagoFlex(
        titulo = Pantalla.INGRESO.titulo,
        fase = Pantalla.INGRESO.fase,
        onVolver = viewModel::volver
    ) {
        TarjetaSeccion(titulo = "Ingresa con tu RUT y PIN") {
            CampoTexto(
                valor = uiState.rut,
                onValorCambia = viewModel::onRutCambia,
                etiqueta = "RUT (ej: 15.834.207-3)",
                icono = R.drawable.ic_persona,
                error = uiState.errorRut
            )
            CampoContrasena(
                valor = uiState.pin,
                onValorCambia = viewModel::onPinCambia,
                etiqueta = "PIN de 4 números",
                visible = uiState.pinVisible,
                onAlternarVisible = viewModel::onAlternarPin,
                error = uiState.errorPin
            )
        }
        BotonPrincipal(
            texto = "Ingresar",
            onClick = viewModel::onIngresar,
            habilitado = !uiState.bloqueado && uiState.estadoIngreso !is EstadoOperacion.Cargando,
            icono = R.drawable.ic_candado,
            modifier = Modifier.fillMaxWidth()
        )
        MensajeOperacion(estado = uiState.estadoIngreso)
        TextButton(onClick = viewModel::usarCuentaPrueba) {
            Text("Usar cuenta de prueba de Camila (PIN 1234)")
        }
        TarjetaSeccion(titulo = "Tu seguridad") {
            Text(
                "Esta es una sesión simulada para el prototipo. Tu PIN no se guarda como texto y después de 3 intentos fallidos la app se bloquea 30 segundos.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Nunca te pediremos datos de tu tarjeta ni claves bancarias.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
