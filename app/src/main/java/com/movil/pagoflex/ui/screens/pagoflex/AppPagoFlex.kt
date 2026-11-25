package com.movil.pagoflex.ui.screens.pagoflex

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movil.pagoflex.model.Pantalla
import com.movil.pagoflex.ui.screens.admin.AdminCompromisoFormScreen
import com.movil.pagoflex.ui.screens.admin.AdminCompromisosScreen
import com.movil.pagoflex.ui.screens.admin.AdminInicioScreen
import com.movil.pagoflex.ui.screens.admin.AdminReportesScreen
import com.movil.pagoflex.ui.screens.admin.AdminUsuarioFormScreen
import com.movil.pagoflex.ui.screens.admin.AdminUsuariosScreen
import com.movil.pagoflex.ui.theme.PagoFlexTheme
import com.movil.pagoflex.viewmodel.AdminViewModel
import com.movil.pagoflex.viewmodel.PagoFlexViewModel

@Composable
fun AppPagoFlex(vm: PagoFlexViewModel, avm: AdminViewModel) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val admin by avm.uiState.collectAsStateWithLifecycle()

    PagoFlexTheme(textoGrande = uiState.textoGrande, altoContraste = uiState.altoContraste) {
        BackHandler(enabled = uiState.pila.isNotEmpty()) { vm.volver() }

        AnimatedContent(
            targetState = uiState.pantalla,
            transitionSpec = { (fadeIn() + slideInHorizontally { it / 4 }) togetherWith fadeOut() },
            label = "pantallas"
        ) { pantalla ->
            when (pantalla) {
                Pantalla.INGRESO -> IngresoScreen(uiState, vm)
                Pantalla.REGISTRO -> RegistroScreen(uiState, vm)
                Pantalla.INICIO -> InicioScreen(uiState, vm)
                Pantalla.COMPROMISOS -> CompromisosScreen(uiState, vm)
                Pantalla.DETALLE -> DetalleScreen(uiState, vm)
                Pantalla.PAGO -> PagoScreen(uiState, vm)
                Pantalla.COMPROBANTE -> ComprobanteScreen(uiState, vm)
                Pantalla.REPORTAR -> ReportarScreen(uiState, vm)
                Pantalla.HISTORIAL -> HistorialScreen(uiState, vm)
                Pantalla.INDICADORES -> IndicadoresScreen(uiState, vm)
                Pantalla.REFERIDOS -> ReferidosScreen(uiState, vm)
                Pantalla.PERFIL -> PerfilScreen(uiState, vm)
                Pantalla.EDITAR_PERFIL -> EditarPerfilScreen(uiState, vm)
                Pantalla.AJUSTES -> AjustesScreen(uiState, vm)
                Pantalla.ADMIN_INICIO -> AdminInicioScreen(uiState, vm, admin, avm)
                Pantalla.ADMIN_USUARIOS -> AdminUsuariosScreen(uiState, vm, admin, avm)
                Pantalla.ADMIN_USUARIO_FORM -> AdminUsuarioFormScreen(vm, admin, avm)
                Pantalla.ADMIN_COMPROMISOS -> AdminCompromisosScreen(vm, admin, avm)
                Pantalla.ADMIN_COMPROMISO_FORM -> AdminCompromisoFormScreen(vm, admin, avm)
                Pantalla.ADMIN_REPORTES -> AdminReportesScreen(vm, admin, avm)
            }
        }
    }
}
