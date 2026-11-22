package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.FaseHooked
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.ui.components.AvisoSinConexion
import cl.duocuc.miregistro.ui.components.BotonMenu
import cl.duocuc.miregistro.ui.components.BotonPrincipal
import cl.duocuc.miregistro.ui.components.BotonSecundario
import cl.duocuc.miregistro.ui.components.EtiquetaInteres
import cl.duocuc.miregistro.ui.components.ImagenEncabezado
import cl.duocuc.miregistro.ui.components.TarjetaEstadistica
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.ui.theme.Dimens
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun InicioScreen(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    val nombre = uiState.usuario?.primerNombre() ?: ""
    val primerPendiente = uiState.compromisos
        .filter { it.porPagar() }
        .minByOrNull { it.vencimiento.valor() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ImagenEncabezado(
            imagen = R.drawable.portada_perfil,
            descripcion = "Portada de inicio",
            titulo = "Hola, $nombre",
            subtitulo = "Actualizado: " + uiState.ultimaSincronizacion
        )
        Column(
            modifier = Modifier.padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
        ) {
            if (uiState.sinConexion) {
                AvisoSinConexion(uiState.ultimaSincronizacion)
            }

            EtiquetaInteres(texto = "Fase " + FaseHooked.GATILLO.etiqueta + ": " + FaseHooked.GATILLO.descripcion)
            TarjetaSeccion(titulo = "Tu recordatorio de hoy") {
                val aviso = uiState.avisos.firstOrNull()
                Text(
                    aviso?.mensaje ?: "No tienes avisos por ahora. ¡Estás al día!",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            BotonMenu("Recordatorios", "Elige cuándo y cuántas veces te avisamos") { viewModel.irA(Pantalla.RECORDATORIOS) }

            val totalAnimado by animateIntAsState(
                targetValue = uiState.totalMes,
                animationSpec = tween(durationMillis = 800),
                label = "total"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                TarjetaEstadistica(uiState.montoVisible(totalAnimado), "Este mes", Modifier.weight(1.4f))
                TarjetaEstadistica(uiState.vencidos.toString(), "Por ponerte al día", Modifier.weight(1f))
                TarjetaEstadistica(String.format("%.1f", uiState.indicador) + "%", "Cumplimiento", Modifier.weight(1f))
            }

            uiState.proximo?.let {
                Text(
                    "Próximo vencimiento: ${it.vencimiento.texto()} · ${it.empresa}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (primerPendiente != null) {
                BotonPrincipal(
                    texto = "Pagar ahora",
                    onClick = { viewModel.verDetalle(primerPendiente.folio) },
                    icono = R.drawable.ic_check,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            EtiquetaInteres(texto = "Fase " + FaseHooked.ACCION.etiqueta)
            BotonMenu("Mis compromisos", "Todo lo que debes, de todas las empresas") { viewModel.irA(Pantalla.COMPROMISOS) }

            EtiquetaInteres(texto = "Fase " + FaseHooked.RECOMPENSA.etiqueta)
            BotonMenu("Mis logros", "Tu cumplimiento y reconocimientos") { viewModel.irA(Pantalla.LOGROS) }
            BotonMenu("Historial", "Tus pagos y comprobantes") { viewModel.irA(Pantalla.HISTORIAL) }
            BotonMenu("Próximos meses", "Lo que viene y cuánto ahorras pagando a tiempo") { viewModel.irA(Pantalla.PROYECCION) }

            EtiquetaInteres(texto = "Fase " + FaseHooked.INVERSION.etiqueta)
            BotonMenu("Recomendar", "Invita a tu jardín, club o edificio") { viewModel.irA(Pantalla.REFERIDOS) }
            BotonMenu("Mi perfil", "Tus datos y tu privacidad") { viewModel.irA(Pantalla.PERFIL) }

            EtiquetaInteres(texto = "Tu cuenta")
            BotonMenu("Sincronización", "Estado de tus datos y conexión") { viewModel.irA(Pantalla.SINCRONIZACION) }
            BotonMenu("Accesibilidad", "Texto más grande y alto contraste") { viewModel.irA(Pantalla.ACCESIBILIDAD) }

            BotonSecundario(
                texto = "Cerrar sesión",
                onClick = viewModel::cerrarSesion,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Dimens.espacioGrande))
        }
    }
}
