package cl.duocuc.miregistro.ui.screens.pagoflex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.FaseHooked
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.ui.components.BotonPrincipal
import cl.duocuc.miregistro.ui.components.BotonSecundario
import cl.duocuc.miregistro.ui.components.ImagenEncabezado
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.ui.theme.Dimens
import cl.duocuc.miregistro.viewmodel.PagoFlexViewModel

@Composable
fun BienvenidaScreen(viewModel: PagoFlexViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ImagenEncabezado(
            imagen = R.drawable.banner_registro,
            descripcion = "Ilustración de bienvenida",
            titulo = "PagoFlex",
            subtitulo = "Todos tus pagos del mes en un solo lugar"
        )
        Column(
            modifier = Modifier.padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
        ) {
            Text(
                "Pagas una vez al mes, pero aquí vas a querer volver. Así funciona:",
                style = MaterialTheme.typography.bodyMedium
            )
            TarjetaSeccion(titulo = "Tu ciclo PagoFlex") {
                FaseHooked.entries.filter { it != FaseHooked.BASE }.forEachIndexed { indice, fase ->
                    Text(
                        "${indice + 1}. ${fase.etiqueta}: ${fase.descripcion}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            BotonPrincipal(
                texto = "Crear cuenta",
                onClick = { viewModel.irA(Pantalla.REGISTRO) },
                icono = R.drawable.ic_persona,
                modifier = Modifier.fillMaxWidth()
            )
            BotonSecundario(
                texto = "Ya tengo cuenta",
                onClick = { viewModel.irA(Pantalla.INGRESO) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Dimens.espacioGrande))
        }
    }
}
