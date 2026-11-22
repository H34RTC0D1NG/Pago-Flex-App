package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.model.pesos
import cl.duocuc.miregistro.ui.components.FilaDato
import cl.duocuc.miregistro.ui.components.PantallaPagoFlex
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun ProyeccionScreen(uiState: PagoFlexUiState, viewModel: PagoFlexViewModel) {
    PantallaPagoFlex(
        titulo = Pantalla.PROYECCION.titulo,
        fase = Pantalla.PROYECCION.fase,
        onVolver = viewModel::volver
    ) {
        val porPagar = uiState.compromisos.filter { it.porPagar() }
        val porMes = porPagar.groupBy { it.vencimiento.nombreMes() }
        val ahorro = porPagar.sumOf { it.recargoEmpresa() }

        Text(
            "Así se ven tus próximos meses. Planifica con tiempo y evita recargos.",
            style = MaterialTheme.typography.bodyMedium
        )

        porMes.forEach { (mes, lista) ->
            TarjetaSeccion(titulo = mes + " · " + uiState.montoVisible(lista.sumOf { it.total() })) {
                lista.sortedBy { it.vencimiento.valor() }.forEach {
                    FilaDato(it.vencimiento.texto() + " " + it.empresa, uiState.montoVisible(it.total()))
                }
            }
        }

        TarjetaSeccion(titulo = "Si pagas todo a tiempo") {
            Text(
                "Te ahorras hasta ${pesos(ahorro)} en recargos por atraso en estos meses.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Esta proyección no existe en el portal ni en el agente conversacional: es exclusiva de la app.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
