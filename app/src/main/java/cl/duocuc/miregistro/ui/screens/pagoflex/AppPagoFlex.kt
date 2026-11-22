package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.ui.screens.FormularioScreen
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun AppPagoFlex(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    AnimatedContent(
        targetState = uiState.pantalla,
        transitionSpec = { (fadeIn() + slideInHorizontally { it / 4 }) togetherWith fadeOut() },
        label = "pantallas"
    ) { pantalla ->
        when (pantalla) {
            Pantalla.BIENVENIDA -> BienvenidaScreen(viewModel)
            Pantalla.REGISTRO -> FormularioScreen(onContinuar = { viewModel.irA(Pantalla.INGRESO) })
            Pantalla.INGRESO -> IngresoScreen(uiState, viewModel)
            Pantalla.INICIO -> InicioScreen(uiState, viewModel)
            Pantalla.RECORDATORIOS -> RecordatoriosScreen(uiState, viewModel)
            Pantalla.COMPROMISOS -> CompromisosScreen(uiState, viewModel)
            Pantalla.DETALLE -> DetalleScreen(uiState, viewModel)
            Pantalla.PAGO -> PagoScreen(uiState, viewModel)
            Pantalla.REPORTAR -> ReportarScreen(uiState, viewModel)
            Pantalla.COMPROBANTE -> ComprobanteScreen(uiState, viewModel)
            Pantalla.HISTORIAL -> HistorialScreen(uiState, viewModel)
            Pantalla.LOGROS -> LogrosScreen(uiState, viewModel)
            Pantalla.PROYECCION -> ProyeccionScreen(uiState, viewModel)
            Pantalla.REFERIDOS -> ReferidosScreen(uiState, viewModel)
            Pantalla.PERFIL -> PerfilScreen(uiState, viewModel)
            Pantalla.SINCRONIZACION -> SincronizacionScreen(uiState, viewModel)
            Pantalla.ACCESIBILIDAD -> AccesibilidadScreen(uiState, viewModel)
        }
    }
}
