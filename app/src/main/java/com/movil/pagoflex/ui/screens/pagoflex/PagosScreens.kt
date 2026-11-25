package com.movil.pagoflex.ui.screens.pagoflex

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.movil.pagoflex.model.EstadoCompromiso
import com.movil.pagoflex.model.EstadoOperacion
import com.movil.pagoflex.model.MedioPago
import com.movil.pagoflex.model.MotivoReporte
import com.movil.pagoflex.model.PagoFlexUiState
import com.movil.pagoflex.model.Pantalla
import com.movil.pagoflex.model.pesos
import com.movil.pagoflex.nativo.compartirTexto
import com.movil.pagoflex.ui.components.AvisoSinServidor
import com.movil.pagoflex.ui.components.BotonMenu
import com.movil.pagoflex.ui.components.BotonPrincipal
import com.movil.pagoflex.ui.components.BotonSecundario
import com.movil.pagoflex.ui.components.Campo
import com.movil.pagoflex.ui.components.ChipEstado
import com.movil.pagoflex.ui.components.Chips
import com.movil.pagoflex.ui.components.Estadistica
import com.movil.pagoflex.ui.components.FilaDato
import com.movil.pagoflex.ui.components.GrupoRadio
import com.movil.pagoflex.ui.components.MensajeOperacion
import com.movil.pagoflex.ui.components.PantallaBase
import com.movil.pagoflex.ui.components.Tarjeta
import com.movil.pagoflex.ui.components.TarjetaCompromiso
import com.movil.pagoflex.viewmodel.PagoFlexViewModel
import java.util.Locale

@Composable
fun InicioScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel) {
    val nombre = uiState.usuario?.primerNombre() ?: ""
    val primerPendiente = uiState.compromisos.filter { it.porPagar() }.minByOrNull { it.vencimiento.valor() }
    val proximo = uiState.compromisos
        .filter { it.estadoActual() == EstadoCompromiso.PENDIENTE }
        .minByOrNull { it.vencimiento.valor() }
    val totalAnimado by animateIntAsState(uiState.totalMes, tween(800), label = "total")

    PantallaBase(titulo = "Hola, $nombre") {
        if (uiState.sinServidor) AvisoSinServidor { vm.refrescar(true) }
        Tarjeta(titulo = "Tu recordatorio de hoy") {
            Text(
                uiState.avisos.firstOrNull()?.mensaje ?: "No tienes avisos por ahora. ¡Estás al día!",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Estadistica(uiState.montoVisible(totalAnimado), "Este mes", Modifier.weight(1.4f))
            Estadistica(uiState.vencidos.toString(), "Vencidos", Modifier.weight(1f))
            Estadistica(String.format(Locale.US, "%.1f%%", uiState.indicador), "Cumplimiento", Modifier.weight(1f))
        }
        val uf = uiState.uf()
        if (uf != null && uf.valor > 0) {
            Tarjeta(titulo = "Tu mes en UF") {
                Text(
                    if (uiState.ocultarMontos) "•••• UF" else String.format(Locale.US, "%.2f UF", uiState.totalMes / uf.valor),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("UF de hoy: ${uf.valorTexto()} (mindicador.cl)", style = MaterialTheme.typography.bodyMedium)
            }
        }
        proximo?.let {
            Text("Próximo vencimiento: ${it.vencimiento.texto()} · ${it.empresa}", style = MaterialTheme.typography.bodyMedium)
        }
        if (primerPendiente != null) {
            BotonPrincipal("Pagar ahora", { vm.verDetalle(primerPendiente.id) })
        }
        BotonMenu("Mis compromisos", "Todo lo que debes, de todas las empresas") { vm.irA(Pantalla.COMPROMISOS) }
        BotonMenu("Historial y logros", "Tus pagos, comprobantes y reconocimientos") { vm.irA(Pantalla.HISTORIAL) }
        BotonMenu("Indicadores económicos", "UF, dólar, euro y UTM del día") { vm.irA(Pantalla.INDICADORES) }
        BotonMenu("Recomendar", "Invita a tu jardín, club o edificio") { vm.irA(Pantalla.REFERIDOS) }
        BotonMenu("Mi perfil", "Tus datos y tu privacidad") { vm.irA(Pantalla.PERFIL) }
        BotonMenu("Recordatorios y accesibilidad", "Avisos, texto grande y alto contraste") { vm.irA(Pantalla.AJUSTES) }
        BotonSecundario("Cerrar sesión", vm::cerrarSesion)
    }
}

@Composable
fun CompromisosScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel) {
    PantallaBase(titulo = Pantalla.COMPROMISOS.titulo, onVolver = vm::volver) {
        Chips(
            opciones = listOf<EstadoCompromiso?>(null) + EstadoCompromiso.entries,
            seleccionada = uiState.filtroEstado,
            onSeleccionar = vm::onFiltroEstado,
            textoDe = { it?.etiqueta ?: "Todos" }
        )
        val lista = uiState.compromisosFiltrados().sortedBy { it.vencimiento.valor() }
        Text(
            "${lista.size} compromiso(s) de ${lista.map { it.empresa }.distinct().size} empresa(s)",
            style = MaterialTheme.typography.bodyMedium
        )
        if (lista.isEmpty()) Text("No hay compromisos con este filtro.", style = MaterialTheme.typography.bodyMedium)
        lista.forEach { c ->
            TarjetaCompromiso(c, uiState.montoVisible(c.total())) { vm.verDetalle(c.id) }
        }
    }
}

@Composable
fun DetalleScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel) {
    PantallaBase(titulo = Pantalla.DETALLE.titulo, onVolver = vm::volver) {
        val c = uiState.compromisoSeleccionado()
        if (c == null) {
            Text("No encontramos este compromiso.", style = MaterialTheme.typography.bodyMedium)
            return@PantallaBase
        }
        val bloqueo = vm.motivoBloqueo()
        Tarjeta(titulo = c.empresa) {
            ChipEstado(c.estadoActual())
            FilaDato("Tipo", c.tipo.etiqueta)
            FilaDato("Concepto", c.concepto)
            FilaDato("Cuota", c.textoCuota())
            FilaDato("Folio", c.folio)
            FilaDato("Vencimiento", c.vencimiento.texto())
            FilaDato("Monto", pesos(c.monto))
            if (c.diasAtraso() > 0) {
                FilaDato("Días de atraso", c.diasAtraso().toString())
                FilaDato("Recargo", pesos(c.recargo()))
            }
            FilaDato("Total a pagar", pesos(c.total()), destacado = true)
        }
        if (bloqueo != null) {
            Tarjeta(titulo = "Antes de pagar") { Text(bloqueo, style = MaterialTheme.typography.bodyMedium) }
        }
        if (c.porPagar() && bloqueo == null) BotonPrincipal("Pagar " + pesos(c.total()), vm::irAPagar)
        if (c.porPagar()) BotonSecundario("Tengo un problema con este cobro", vm::irAReportar)
        uiState.comprobantes.find { it.compromisoId == c.id }?.let { comp ->
            BotonSecundario("Ver comprobante") { vm.verComprobante(comp.id) }
        }
        uiState.reportes.find { it.compromisoId == c.id }?.let { r ->
            Tarjeta(titulo = "Tu reporte ${r.numero}") {
                FilaDato("Motivo", r.motivo.etiqueta)
                FilaDato("Estado", r.estado.etiqueta)
                if (r.respuesta.isBlank()) {
                    Text("Te avisaremos cuando tengamos una respuesta.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    FilaDato("Respuesta", r.respuesta)
                }
            }
        }
    }
}

@Composable
fun PagoScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel) {
    val c = uiState.compromisoSeleccionado()
    PantallaBase(titulo = Pantalla.PAGO.titulo, onVolver = vm::volver) {
        if (c == null) {
            Text("No hay compromiso seleccionado.", style = MaterialTheme.typography.bodyMedium)
            return@PantallaBase
        }
        Tarjeta(titulo = "Vas a pagar") {
            FilaDato("Empresa", c.empresa)
            FilaDato("Concepto", c.concepto)
            FilaDato("Total", pesos(c.total()), destacado = true)
        }
        Tarjeta(titulo = "¿Cómo quieres pagar?") {
            GrupoRadio(MedioPago.entries, uiState.medioPago, vm::onMedioSeleccionado) { it.etiqueta }
            uiState.errorMedio?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Text("Pago simulado: no pedimos ni guardamos datos de tarjetas.", style = MaterialTheme.typography.bodyMedium)
        }
        BotonPrincipal("Pagar " + pesos(c.total()), vm::onSolicitarPago, uiState.estadoPago !is EstadoOperacion.Cargando)
        MensajeOperacion(uiState.estadoPago, vm::onSolicitarPago)
    }
    if (uiState.confirmarPago && c != null) {
        AlertDialog(
            onDismissRequest = vm::onCancelarPago,
            title = { Text("Confirmar pago") },
            text = { Text("Vas a pagar ${pesos(c.total())} a ${c.empresa} con ${uiState.medioPago?.etiqueta ?: ""}. ¿Confirmas?") },
            confirmButton = { TextButton(onClick = vm::onConfirmarPago) { Text("Sí, pagar") } },
            dismissButton = { TextButton(onClick = vm::onCancelarPago) { Text("Cancelar") } }
        )
    }
}

@Composable
fun ComprobanteScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel) {
    val context = LocalContext.current
    PantallaBase(titulo = Pantalla.COMPROBANTE.titulo, onVolver = vm::volver) {
        val comp = uiState.comprobanteVisto
        if (comp == null) {
            Text("No hay comprobante para mostrar.", style = MaterialTheme.typography.bodyMedium)
            return@PantallaBase
        }
        uiState.recompensa?.let {
            Tarjeta(titulo = "¡Pago confirmado!") {
                Text(it, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Tu cumplimiento ahora es " + String.format(Locale.US, "%.1f", uiState.indicador) + "% (" + uiState.categoria + ")",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        val usuario = uiState.usuario
        Tarjeta(titulo = "Comprobante N° ${comp.numero}") {
            FilaDato("Fecha y hora", comp.fecha.texto() + " " + comp.hora)
            FilaDato("Pagado por", (usuario?.nombre ?: "") + " · RUT " + (usuario?.rutOculto() ?: ""))
            FilaDato("Empresa", comp.empresa)
            FilaDato("Folio", comp.folio)
            FilaDato("Concepto", comp.concepto)
            FilaDato("Medio de pago", comp.medio)
            FilaDato("Canal", comp.canal)
            FilaDato("¿A tiempo?", if (comp.aTiempo) "Sí" else "No")
            FilaDato("Total pagado", pesos(comp.monto), destacado = true)
        }
        BotonPrincipal("Ver mis logros", { vm.irA(Pantalla.HISTORIAL) })
        BotonSecundario("Compartir comprobante") {
            val texto = "Comprobante PagoFlex N° ${comp.numero}\n" +
                "Fecha: ${comp.fecha.texto()} ${comp.hora}\n" +
                "RUT: ${usuario?.rutOculto() ?: ""}\n" +
                "Empresa: ${comp.empresa}\n" +
                "Concepto: ${comp.concepto}\n" +
                "Total: ${pesos(comp.monto)}"
            compartirTexto(context, "Comprobante PagoFlex", texto)
        }
        BotonSecundario("Volver al inicio", vm::volverAlInicio)
    }
}

@Composable
fun ReportarScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel) {
    val f = uiState.formReporte
    PantallaBase(titulo = Pantalla.REPORTAR.titulo, onVolver = vm::volver) {
        uiState.compromisoSeleccionado()?.let {
            Tarjeta(titulo = "Compromiso") {
                FilaDato("Empresa", it.empresa)
                FilaDato("Concepto", it.concepto)
                FilaDato("Folio", it.folio)
            }
        }
        Tarjeta(titulo = "¿Qué pasó?") {
            GrupoRadio(MotivoReporte.entries, f.motivo, vm::onMotivoReporte) { it.etiqueta }
            Campo(
                f.comentario, vm::onComentarioReporte,
                if (f.motivo == MotivoReporte.OTRO) "Comentario (obligatorio)" else "Comentario (opcional)",
                f.error
            )
        }
        BotonPrincipal("Enviar reporte", vm::onEnviarReporte, uiState.estadoReporte !is EstadoOperacion.Cargando)
        MensajeOperacion(uiState.estadoReporte)
        Text("Mientras revisamos tu caso el compromiso queda en revisión y no se puede pagar.", style = MaterialTheme.typography.bodyMedium)
        if (uiState.reportes.isNotEmpty()) {
            Tarjeta(titulo = "Mis reportes") {
                uiState.reportes.forEach { FilaDato(it.numero, it.motivo.etiqueta + " · " + it.estado.etiqueta) }
            }
        }
    }
}
