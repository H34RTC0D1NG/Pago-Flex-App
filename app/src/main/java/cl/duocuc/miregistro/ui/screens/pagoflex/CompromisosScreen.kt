package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cl.duocuc.miregistro.model.EstadoCompromiso
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.ui.components.PantallaPagoFlex
import cl.duocuc.miregistro.ui.components.TarjetaCompromiso
import cl.duocuc.miregistro.ui.theme.Dimens
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun CompromisosScreen(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    PantallaPagoFlex(
        titulo = Pantalla.COMPROMISOS.titulo,
        fase = Pantalla.COMPROMISOS.fase,
        onVolver = viewModel::volver
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)
        ) {
            FilterChip(
                selected = uiState.filtroEstado == null,
                onClick = { viewModel.onFiltroEstado(null) },
                label = { Text("Todos") }
            )
            EstadoCompromiso.entries.forEach { estado ->
                FilterChip(
                    selected = uiState.filtroEstado == estado,
                    onClick = { viewModel.onFiltroEstado(estado) },
                    label = { Text(estado.etiqueta) }
                )
            }
        }

        val lista = uiState.compromisosFiltrados().sortedBy { it.vencimiento.valor() }
        Text(
            "${lista.size} compromiso(s) de ${lista.map { it.empresa }.distinct().size} empresa(s)",
            style = MaterialTheme.typography.bodyMedium
        )
        if (lista.isEmpty()) {
            Text("No hay compromisos con este filtro.", style = MaterialTheme.typography.bodyMedium)
        }
        lista.forEach { compromiso ->
            TarjetaCompromiso(
                compromiso = compromiso,
                montoTexto = uiState.montoVisible(compromiso.total()),
                onClick = { viewModel.verDetalle(compromiso.folio) }
            )
        }
    }
}
