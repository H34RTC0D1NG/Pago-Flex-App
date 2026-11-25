package com.movil.pagoflex.ui.screens.pagoflex

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.movil.pagoflex.model.EstadoOperacion
import com.movil.pagoflex.model.EstadoRecomendacion
import com.movil.pagoflex.model.PagoFlexUiState
import com.movil.pagoflex.model.Pantalla
import com.movil.pagoflex.nativo.compartirTexto
import com.movil.pagoflex.ui.components.BotonPrincipal
import com.movil.pagoflex.ui.components.BotonSecundario
import com.movil.pagoflex.ui.components.Campo
import com.movil.pagoflex.ui.components.Casilla
import com.movil.pagoflex.ui.components.Chips
import com.movil.pagoflex.ui.components.Confirmacion
import com.movil.pagoflex.ui.components.Estadistica
import com.movil.pagoflex.ui.components.FilaDato
import com.movil.pagoflex.ui.components.Interruptor
import com.movil.pagoflex.ui.components.Logo
import com.movil.pagoflex.ui.components.MensajeOperacion
import com.movil.pagoflex.ui.components.PantallaBase
import com.movil.pagoflex.ui.components.Tarjeta
import com.movil.pagoflex.viewmodel.PagoFlexViewModel
import java.util.Locale

@Composable
fun HistorialScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel) {
    var mostrar by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mostrar = true }
    val avance by animateFloatAsState(
        targetValue = if (mostrar) (uiState.indicador / 100.0).toFloat() else 0f,
        animationSpec = tween(1200),
        label = "avance"
    )
    PantallaBase(titulo = Pantalla.HISTORIAL.titulo, onVolver = vm::volver) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { avance }, modifier = Modifier.size(150.dp), strokeWidth = 12.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(String.format(Locale.US, "%.1f%%", uiState.indicador), style = MaterialTheme.typography.titleLarge)
                Text(uiState.categoria, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Tarjeta(titulo = "Logros") {
            uiState.logros.forEach { logro ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(if (logro.obtenido) "★ " else "☆ ", color = MaterialTheme.colorScheme.tertiary)
                    Column {
                        Text(logro.titulo, style = MaterialTheme.typography.titleMedium)
                        Text(logro.descripcion, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text("Tus logros son privados y nunca se pierden.", style = MaterialTheme.typography.bodyMedium)
        }
        val lista = uiState.comprobantesFiltrados()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Estadistica(uiState.montoVisible(lista.sumOf { it.monto }), "Total pagado", Modifier.weight(1.4f))
            Estadistica("${lista.count { it.aTiempo }}/${lista.size}", "A tiempo", Modifier.weight(1f))
        }
        Chips(listOf<String?>(null) + uiState.empresas(), uiState.filtroEmpresa, vm::onFiltroEmpresa) { it ?: "Todas" }
        if (lista.isEmpty()) Text("Aún no tienes pagos registrados.", style = MaterialTheme.typography.bodyMedium)
        lista.forEach { comp ->
            Box(modifier = Modifier.clickable { vm.verComprobante(comp.id) }) {
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(comp.empresa, style = MaterialTheme.typography.titleMedium)
                            Text(comp.concepto, style = MaterialTheme.typography.bodyMedium)
                            Text(comp.fecha.texto() + " · " + comp.canal, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(uiState.montoVisible(comp.monto), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (comp.aTiempo) "A tiempo" else "Con atraso", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IndicadoresScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel) {
    LaunchedEffect(Unit) { vm.cargarIndicadoresSiHaceFalta() }
    PantallaBase(titulo = Pantalla.INDICADORES.titulo, onVolver = vm::volver) {
        Text(
            "Valores del día publicados por mindicador.cl. Te ayudan a entender cobros en UF o dólares.",
            style = MaterialTheme.typography.bodyMedium
        )
        MensajeOperacion(uiState.estadoIndicadores, vm::cargarIndicadores)
        uiState.indicadores.forEach { ind ->
            Tarjeta(titulo = ind.nombre) {
                Text(ind.valorTexto(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                FilaDato("Fecha del valor", ind.fecha)
                FilaDato("Unidad", ind.unidad)
            }
        }
        val uf = uiState.uf()
        if (uf != null && uf.valor > 0 && uiState.usuario?.esAdmin() == false) {
            Tarjeta(titulo = "Tu mes convertido") {
                FilaDato("Total del mes", uiState.montoVisible(uiState.totalMes))
                FilaDato(
                    "Equivale a",
                    if (uiState.ocultarMontos) "•••• UF" else String.format(Locale.US, "%.2f UF", uiState.totalMes / uf.valor),
                    destacado = true
                )
            }
        }
        BotonPrincipal("Actualizar indicadores", vm::cargarIndicadores, uiState.estadoIndicadores !is EstadoOperacion.Cargando)
    }
}

@Composable
fun ReferidosScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel) {
    val f = uiState.formReferido
    val context = LocalContext.current
    PantallaBase(titulo = Pantalla.REFERIDOS.titulo, onVolver = vm::volver) {
        Tarjeta(titulo = "Tu código de invitación") {
            Text(vm.codigoReferido(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "¿Le pagas a un jardín, club o edificio por transferencia? Recomiéndalo y, si se une, ganas un logro.",
                style = MaterialTheme.typography.bodyMedium
            )
            BotonSecundario("Compartir mi código") {
                compartirTexto(
                    context, "Te invito a PagoFlex",
                    "Paga tus compromisos en un solo lugar con PagoFlex. Usa mi código: " + vm.codigoReferido()
                )
            }
        }
        Tarjeta(titulo = "Recomendar una organización") {
            Campo(f.organizacion, { v -> vm.onFormReferido { it.copy(organizacion = v, errores = it.errores - "organizacion") } }, "Nombre de la organización", f.errores["organizacion"])
            Campo(f.rubro, { v -> vm.onFormReferido { it.copy(rubro = v, errores = it.errores - "rubro") } }, "Rubro (ej: jardín infantil)", f.errores["rubro"])
            Campo(f.comuna, { v -> vm.onFormReferido { it.copy(comuna = v, errores = it.errores - "comuna") } }, "Comuna", f.errores["comuna"])
            Campo(f.contacto, { v -> vm.onFormReferido { it.copy(contacto = v, errores = it.errores - "autoriza") } }, "Contacto (opcional)")
            Casilla("La persona de contacto autorizó que la contactemos", f.autoriza) { v ->
                vm.onFormReferido { it.copy(autoriza = v, errores = it.errores - "autoriza") }
            }
            f.errores["autoriza"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        BotonPrincipal("Enviar recomendación", vm::onEnviarReferido, uiState.estadoReferido !is EstadoOperacion.Cargando)
        MensajeOperacion(uiState.estadoReferido)
        Text("Máximo 5 recomendaciones. No hay premios en dinero.", style = MaterialTheme.typography.bodyMedium)
        Tarjeta(titulo = "Mis recomendaciones") {
            if (uiState.recomendaciones.isEmpty()) {
                Text("Aún no recomiendas a ninguna organización.", style = MaterialTheme.typography.bodyMedium)
            }
            uiState.recomendaciones.forEach { rec ->
                FilaDato(rec.organizacion, rec.estado.etiqueta)
                LinearProgressIndicator(progress = { rec.estado.avance }, modifier = Modifier.fillMaxWidth())
                if (rec.estado == EstadoRecomendacion.RECIBIDA) {
                    TextButton(onClick = { vm.eliminarRecomendacion(rec.id) }) { Text("Retirar recomendación") }
                }
            }
        }
    }
}

@Composable
fun PerfilScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel) {
    val u = uiState.usuario
    PantallaBase(titulo = Pantalla.PERFIL.titulo, onVolver = vm::volver) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Logo(88.dp) }
        if (u != null) {
            Tarjeta(titulo = u.nombre) {
                FilaDato("RUT", u.rutOculto())
                FilaDato("Correo", u.correo)
                FilaDato("Comuna", u.comuna)
                FilaDato("Teléfono", u.telefono.ifBlank { "No registrado" })
                FilaDato("Tipo de cuenta", u.rol.etiqueta)
            }
            BotonPrincipal("Editar mis datos", vm::irAEditarPerfil)
        }
        Tarjeta(titulo = "Privacidad") {
            Interruptor("Ocultar montos en pantalla", uiState.ocultarMontos, vm::onOcultarMontos)
        }
        Tarjeta(titulo = "Qué datos guardamos") {
            Text("• Tu nombre, RUT, correo, comuna y teléfono.", style = MaterialTheme.typography.bodyMedium)
            Text("• Tus compromisos, pagos y comprobantes (base de datos del teléfono).", style = MaterialTheme.typography.bodyMedium)
            Text("• Nunca números de tarjeta, claves bancarias ni tu PIN como texto.", style = MaterialTheme.typography.bodyMedium)
        }
        BotonSecundario("Cerrar sesión", vm::cerrarSesion)
    }
}

@Composable
fun EditarPerfilScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel) {
    val f = uiState.formPerfil
    PantallaBase(titulo = Pantalla.EDITAR_PERFIL.titulo, onVolver = vm::volver) {
        Tarjeta(titulo = "Tus datos") {
            Campo(f.nombre, { v -> vm.onFormPerfil { it.copy(nombre = v, errores = it.errores - "nombre") } }, "Nombre completo", f.errores["nombre"])
            Campo(f.correo, { v -> vm.onFormPerfil { it.copy(correo = v, errores = it.errores - "correo") } }, "Correo", f.errores["correo"], KeyboardType.Email)
            Campo(f.comuna, { v -> vm.onFormPerfil { it.copy(comuna = v, errores = it.errores - "comuna") } }, "Comuna", f.errores["comuna"])
            Campo(f.telefono, { v -> vm.onFormPerfil { it.copy(telefono = v, errores = it.errores - "telefono") } }, "Teléfono", f.errores["telefono"], KeyboardType.Phone)
        }
        Tarjeta(titulo = "Cambiar PIN") {
            Campo(
                f.nuevoPin, { v -> vm.onFormPerfil { it.copy(nuevoPin = v.filter { c -> c.isDigit() }.take(4), errores = it.errores - "pin") } },
                "Nuevo PIN (déjalo vacío para no cambiarlo)", f.errores["pin"], KeyboardType.NumberPassword, oculto = true
            )
        }
        BotonPrincipal("Guardar cambios", vm::onGuardarPerfil, uiState.estadoPerfil !is EstadoOperacion.Cargando)
        MensajeOperacion(uiState.estadoPerfil)
        Tarjeta(titulo = "Zona de peligro") {
            Text("Si eliminas tu cuenta se borran tus compromisos, pagos y recomendaciones.", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = vm::onPedirEliminarCuenta) {
                Text("Eliminar mi cuenta", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (uiState.confirmarEliminarCuenta) {
        Confirmacion(
            titulo = "¿Eliminar tu cuenta?",
            mensaje = "Esta acción no se puede deshacer.",
            textoConfirmar = "Eliminar",
            onConfirmar = vm::onConfirmarEliminarCuenta,
            onCancelar = vm::onCancelarEliminarCuenta
        )
    }
}

@Composable
fun AjustesScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel) {
    val config = uiState.config
    PantallaBase(titulo = Pantalla.AJUSTES.titulo, onVolver = vm::volver) {
        Tarjeta(titulo = "Recordatorios") {
            Interruptor("Recibir recordatorios", config.activos, vm::onAvisosActivos)
            Text("Avisarme con estos días de anticipación:", style = MaterialTheme.typography.bodyMedium)
            Chips(listOf(1, 3, 5, 7), config.diasAnticipacion, vm::onDiasAnticipacion) { "$it días" }
            Text("Máximo de avisos por ciclo:", style = MaterialTheme.typography.bodyMedium)
            Chips(listOf(1, 2, 3), config.maximoPorCiclo, vm::onMaximoPorCiclo) { it.toString() }
            Interruptor("No avisar de noche (22:00 a 8:00)", config.respetarHorario, vm::onRespetarHorario)
        }
        Tarjeta(titulo = "Así se verían tus avisos") {
            if (uiState.avisos.isEmpty()) Text("Sin avisos con esta configuración.", style = MaterialTheme.typography.bodyMedium)
            uiState.avisos.forEach { Text("• " + it.mensaje, style = MaterialTheme.typography.bodyMedium) }
        }
        Tarjeta(titulo = "Accesibilidad") {
            Interruptor("Texto más grande", uiState.textoGrande, vm::onTextoGrande)
            Interruptor("Alto contraste", uiState.altoContraste, vm::onAltoContraste)
            Interruptor("Ocultar montos", uiState.ocultarMontos, vm::onOcultarMontos)
        }
        Text("Los cambios se guardan solos en este teléfono.", style = MaterialTheme.typography.bodyMedium)
    }
}
