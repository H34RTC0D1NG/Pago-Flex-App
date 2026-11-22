package cl.duocuc.miregistro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duocuc.miregistro.model.Compromiso
import cl.duocuc.miregistro.model.EstadoCompromiso
import cl.duocuc.miregistro.model.EstadoOperacion
import cl.duocuc.miregistro.model.FaseHooked
import cl.duocuc.miregistro.ui.styles.EstilosTarjeta
import cl.duocuc.miregistro.ui.theme.Dimens
import cl.duocuc.miregistro.ui.theme.Exito
import cl.duocuc.miregistro.ui.theme.ExitoFondo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPagoFlex(
    titulo: String,
    fase: FaseHooked,
    onVolver: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    if (onVolver != null) {
                        TextButton(onClick = onVolver) {
                            Text("< Volver", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
        ) {
            EtiquetaInteres(texto = "Fase " + fase.etiqueta + ": " + fase.descripcion)
            contenido()
            Spacer(Modifier.height(Dimens.espacioGrande))
        }
    }
}

fun colorEstado(estado: EstadoCompromiso): Color = when (estado) {
    EstadoCompromiso.PENDIENTE -> Color(0xFF3A6EA5)
    EstadoCompromiso.VENCIDO -> Color(0xFFC2701E)
    EstadoCompromiso.PAGADO -> Color(0xFF2E9E5B)
    EstadoCompromiso.EN_REVISION -> Color(0xFF6B4FB8)
    EstadoCompromiso.ANULADO -> Color(0xFF7A7A7A)
}

@Composable
fun ChipEstado(estado: EstadoCompromiso) {
    val color = colorEstado(estado)
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Text(
            text = estado.etiqueta,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Dimens.espacioChico, vertical = Dimens.espacioMini)
        )
    }
}

@Composable
fun TarjetaCompromiso(
    compromiso: Compromiso,
    montoTexto: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = EstilosTarjeta.forma,
        colors = EstilosTarjeta.colores(),
        elevation = EstilosTarjeta.elevacion()
    ) {
        Column(modifier = Modifier.padding(Dimens.espacioMedio)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(compromiso.empresa, style = MaterialTheme.typography.titleMedium)
                    Text(
                        compromiso.concepto + " · " + compromiso.textoCuota(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                ChipEstado(compromiso.estadoActual())
            }
            Spacer(Modifier.height(Dimens.espacioChico))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Vence el " + compromiso.vencimiento.texto(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(montoTexto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MensajeOperacion(
    estado: EstadoOperacion,
    onReintentar: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = estado !is EstadoOperacion.Inactivo,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        when (estado) {
            is EstadoOperacion.Inactivo -> {}
            is EstadoOperacion.Cargando -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                    Text(
                        "Procesando...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = Dimens.espacioChico)
                    )
                }
            }
            is EstadoOperacion.Exito -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ExitoFondo, contentColor = Exito)
                ) {
                    Text(estado.mensaje, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(Dimens.espacioMedio))
                }
            }
            is EstadoOperacion.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(Dimens.espacioMedio)) {
                        Text(estado.mensaje, style = MaterialTheme.typography.bodyMedium)
                        if (onReintentar != null) {
                            TextButton(onClick = onReintentar) { Text("Reintentar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilaDato(etiqueta: String, valor: String, destacado: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.espacioMini)) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.45f))
        Text(
            valor,
            style = if (destacado) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
fun BotonMenu(titulo: String, descripcion: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = EstilosTarjeta.forma,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(Dimens.espacioMedio)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun AvisoSinConexion(ultimaSincronizacion: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCEBD5), contentColor = Color(0xFF7A4A12))
    ) {
        Text(
            "Estás sin conexión. Estos datos son del $ultimaSincronizacion y podrían no estar al día.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(Dimens.espacioMedio)
        )
    }
}
