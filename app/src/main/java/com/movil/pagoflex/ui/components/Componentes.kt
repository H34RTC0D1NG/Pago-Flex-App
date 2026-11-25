package com.movil.pagoflex.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movil.pagoflex.model.Compromiso
import com.movil.pagoflex.model.EstadoCompromiso
import com.movil.pagoflex.model.EstadoOperacion
import com.movil.pagoflex.ui.theme.Exito
import com.movil.pagoflex.ui.theme.ExitoFondo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaBase(
    titulo: String,
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            contenido()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun Tarjeta(titulo: String? = null, contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (titulo != null) Text(titulo, style = MaterialTheme.typography.titleMedium)
            contenido()
        }
    }
}

@Composable
fun Estadistica(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun BotonPrincipal(texto: String, onClick: () -> Unit, habilitado: Boolean = true) {
    Button(onClick = onClick, enabled = habilitado, modifier = Modifier.fillMaxWidth().height(50.dp)) {
        Text(texto)
    }
}

@Composable
fun BotonSecundario(texto: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(50.dp)) {
        Text(texto)
    }
}

@Composable
fun BotonMenu(titulo: String, descripcion: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun Campo(
    valor: String,
    onCambia: (String) -> Unit,
    etiqueta: String,
    error: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    oculto: Boolean = false
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambia,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else null,
        singleLine = true,
        visualTransformation = if (oculto) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun CampoPin(
    valor: String,
    onCambia: (String) -> Unit,
    etiqueta: String,
    visible: Boolean,
    onAlternar: () -> Unit,
    error: String? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambia,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else null,
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        trailingIcon = {
            TextButton(onClick = onAlternar) { Text(if (visible) "Ocultar" else "Ver") }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun Casilla(texto: String, marcado: Boolean, onCambio: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onCambio(!marcado) }) {
        Checkbox(checked = marcado, onCheckedChange = onCambio)
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Interruptor(texto: String, activado: Boolean, onCambio: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = activado, onCheckedChange = onCambio)
    }
}

@Composable
fun <T> GrupoRadio(opciones: List<T>, seleccionada: T?, onSeleccionar: (T) -> Unit, textoDe: (T) -> String) {
    Column {
        opciones.forEach { opcion ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onSeleccionar(opcion) }
            ) {
                RadioButton(selected = opcion == seleccionada, onClick = { onSeleccionar(opcion) })
                Text(textoDe(opcion), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun <T> Chips(opciones: List<T>, seleccionada: T?, onSeleccionar: (T) -> Unit, textoDe: (T) -> String) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEach { opcion ->
            FilterChip(
                selected = opcion == seleccionada,
                onClick = { onSeleccionar(opcion) },
                label = { Text(textoDe(opcion)) }
            )
        }
    }
}

@Composable
fun FilaDato(etiqueta: String, valor: String, destacado: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.45f))
        Text(
            valor,
            style = if (destacado) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(0.55f)
        )
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
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f), contentColor = color) {
        Text(
            estado.etiqueta,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun Etiqueta(texto: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
        contentColor = MaterialTheme.colorScheme.secondary
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun TarjetaCompromiso(compromiso: Compromiso, montoTexto: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(compromiso.empresa, style = MaterialTheme.typography.titleMedium)
                    Text(compromiso.concepto + " · " + compromiso.textoCuota(), style = MaterialTheme.typography.bodyMedium)
                }
                ChipEstado(compromiso.estadoActual())
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vence el " + compromiso.vencimiento.texto(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(montoTexto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MensajeOperacion(estado: EstadoOperacion, onReintentar: (() -> Unit)? = null) {
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
                    Text("Procesando...", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                }
            }
            is EstadoOperacion.Exito -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ExitoFondo, contentColor = Exito)
                ) {
                    Text(estado.mensaje, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(estado.mensaje, style = MaterialTheme.typography.bodyMedium)
                        if (onReintentar != null) TextButton(onClick = onReintentar) { Text("Reintentar") }
                    }
                }
            }
        }
    }
}

@Composable
fun Logo(tamano: Dp = 72.dp) {
    Box(
        modifier = Modifier.size(tamano).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text("PF", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun Confirmacion(titulo: String, mensaje: String, textoConfirmar: String, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = { TextButton(onClick = onConfirmar) { Text(textoConfirmar) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
