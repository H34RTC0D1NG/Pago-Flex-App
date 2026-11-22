package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.ui.components.PantallaPagoFlex
import cl.duocuc.miregistro.ui.components.TarjetaEstadistica
import cl.duocuc.miregistro.ui.styles.EstilosTarjeta
import cl.duocuc.miregistro.ui.theme.Dimens
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun HistorialScreen(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    PantallaPagoFlex(
        titulo = Pantalla.HISTORIAL.titulo,
        fase = Pantalla.HISTORIAL.fase,
        onVolver = viewModel::volver
    ) {
        val lista = uiState.comprobantesFiltrados()
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
            TarjetaEstadistica(uiState.montoVisible(lista.sumOf { it.monto }), "Total pagado", Modifier.weight(1.4f))
            TarjetaEstadistica("${lista.count { it.aTiempo }}/${lista.size}", "A tiempo", Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)
        ) {
            FilterChip(
                selected = uiState.filtroEmpresa == null,
                onClick = { viewModel.onFiltroEmpresa(null) },
                label = { Text("Todas") }
            )
            uiState.empresas().forEach { empresa ->
                FilterChip(
                    selected = uiState.filtroEmpresa == empresa,
                    onClick = { viewModel.onFiltroEmpresa(empresa) },
                    label = { Text(empresa) }
                )
            }
        }
        Text(
            "Incluye pagos hechos en la app, el portal y el agente conversacional.",
            style = MaterialTheme.typography.bodyMedium
        )
        lista.forEach { comprobante ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.verComprobante(comprobante.numero) },
                shape = EstilosTarjeta.forma,
                colors = EstilosTarjeta.colores(),
                elevation = EstilosTarjeta.elevacion()
            ) {
                Row(modifier = Modifier.padding(Dimens.espacioMedio), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(comprobante.empresa, style = MaterialTheme.typography.titleMedium)
                        Text(comprobante.concepto, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            comprobante.fecha.texto() + " · " + comprobante.canal,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(uiState.montoVisible(comprobante.monto), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (comprobante.aTiempo) "A tiempo" else "Con atraso", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
