package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.ui.components.AvatarCircular
import cl.duocuc.miregistro.ui.components.BotonSecundario
import cl.duocuc.miregistro.ui.components.FilaDato
import cl.duocuc.miregistro.ui.components.InterruptorConTexto
import cl.duocuc.miregistro.ui.components.PantallaPagoFlex
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun PerfilScreen(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    val usuario = uiState.usuario
    PantallaPagoFlex(
        titulo = Pantalla.PERFIL.titulo,
        fase = Pantalla.PERFIL.fase,
        onVolver = viewModel::volver
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AvatarCircular(imagen = R.drawable.avatar_usuario, descripcion = "Foto de perfil")
        }
        if (usuario != null) {
            TarjetaSeccion(titulo = usuario.nombre) {
                FilaDato("RUT", usuario.rutOculto())
                FilaDato("Correo", usuario.correo)
                FilaDato("Comuna", usuario.comuna)
            }
        }
        TarjetaSeccion(titulo = "Privacidad") {
            InterruptorConTexto(
                texto = "Ocultar montos en pantalla (si alguien más usa tu teléfono)",
                activado = uiState.ocultarMontos,
                onCambio = viewModel::onOcultarMontos
            )
        }
        TarjetaSeccion(titulo = "Qué datos guardamos") {
            Text("• Tu nombre, RUT, correo y comuna.", style = MaterialTheme.typography.bodyMedium)
            Text("• Tus compromisos, pagos y comprobantes.", style = MaterialTheme.typography.bodyMedium)
            Text("• Tus preferencias de avisos y accesibilidad.", style = MaterialTheme.typography.bodyMedium)
        }
        TarjetaSeccion(titulo = "Qué datos NUNCA guardamos") {
            Text("• Números de tarjeta ni de cuentas bancarias.", style = MaterialTheme.typography.bodyMedium)
            Text("• Claves bancarias.", style = MaterialTheme.typography.bodyMedium)
            Text("• Tu PIN como texto.", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Cuidamos tus datos según la Ley 19.628 y la nueva Ley 21.719.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        BotonSecundario(
            texto = "Cerrar sesión",
            onClick = viewModel::cerrarSesion,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
