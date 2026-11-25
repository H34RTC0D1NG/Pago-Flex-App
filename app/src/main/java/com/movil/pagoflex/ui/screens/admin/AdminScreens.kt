package com.movil.pagoflex.ui.screens.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.movil.pagoflex.model.AccionReporte
import com.movil.pagoflex.model.AdminUiState
import com.movil.pagoflex.model.Compromiso
import com.movil.pagoflex.model.EstadoCompromiso
import com.movil.pagoflex.model.EstadoOperacion
import com.movil.pagoflex.model.EstadoReporte
import com.movil.pagoflex.model.PagoFlexUiState
import com.movil.pagoflex.model.Pantalla
import com.movil.pagoflex.model.Rol
import com.movil.pagoflex.model.TipoCompromiso
import com.movil.pagoflex.model.Usuario
import com.movil.pagoflex.model.pesos
import com.movil.pagoflex.ui.components.AvisoSinServidor
import com.movil.pagoflex.ui.components.BotonMenu
import com.movil.pagoflex.ui.components.BotonPrincipal
import com.movil.pagoflex.ui.components.BotonSecundario
import com.movil.pagoflex.ui.components.Campo
import com.movil.pagoflex.ui.components.Chips
import com.movil.pagoflex.ui.components.Confirmacion
import com.movil.pagoflex.ui.components.Estadistica
import com.movil.pagoflex.ui.components.Etiqueta
import com.movil.pagoflex.ui.components.FilaDato
import com.movil.pagoflex.ui.components.Logo
import com.movil.pagoflex.ui.components.MensajeOperacion
import com.movil.pagoflex.ui.components.PantallaBase
import com.movil.pagoflex.ui.components.Tarjeta
import com.movil.pagoflex.ui.components.TarjetaCompromiso
import com.movil.pagoflex.viewmodel.AdminViewModel
import com.movil.pagoflex.viewmodel.PagoFlexViewModel

@Composable
fun AdminInicioScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel, admin: AdminUiState, avm: AdminViewModel) {
    LaunchedEffect(Unit) { avm.cargarTodo() }
    PantallaBase(titulo = Pantalla.ADMIN_INICIO.titulo) {
        if (admin.sinServidor) AvisoSinServidor(avm::cargarTodo)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Logo(56.dp)
            Text(
                "Hola, ${uiState.usuario?.primerNombre() ?: "admin"}. Desde aquí gestionas personas, cobros y reportes.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Estadistica(admin.usuarios.size.toString(), "Personas", Modifier.weight(1f))
            Estadistica(admin.compromisos.count { it.porPagar() }.toString(), "Por pagar", Modifier.weight(1f))
            Estadistica(admin.reportesAbiertos().size.toString(), "Reportes", Modifier.weight(1f))
        }
        Text("Total por cobrar: " + pesos(admin.totalPorCobrar()), style = MaterialTheme.typography.titleMedium)
        BotonMenu("Personas", "Agregar clientes o administradores, cambiar roles, desactivar o eliminar") {
            avm.limpiarMensaje()
            vm.irA(Pantalla.ADMIN_USUARIOS)
        }
        BotonMenu("Cobros", "Crear, editar y eliminar compromisos de cada cliente") {
            avm.limpiarMensaje()
            avm.onFiltroUsuario(null)
            vm.irA(Pantalla.ADMIN_COMPROMISOS)
        }
        BotonMenu("Reportes de clientes", "Revisar y cerrar los problemas que reportan") {
            avm.limpiarMensaje()
            vm.irA(Pantalla.ADMIN_REPORTES)
        }
        BotonMenu("Indicadores económicos", "UF, dólar y UTM del día") { vm.irA(Pantalla.INDICADORES) }
        BotonPrincipal("Actualizar datos", avm::cargarTodo)
        BotonSecundario("Cerrar sesión", vm::cerrarSesion)
    }
}

@Composable
fun AdminUsuariosScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel, admin: AdminUiState, avm: AdminViewModel) {
    var aEliminar by remember { mutableStateOf<Usuario?>(null) }
    val miId = uiState.usuario?.id
    PantallaBase(titulo = Pantalla.ADMIN_USUARIOS.titulo, onVolver = vm::volver) {
        BotonPrincipal("Agregar persona", {
            avm.nuevoUsuario()
            vm.irA(Pantalla.ADMIN_USUARIO_FORM)
        })
        MensajeOperacion(admin.mensaje)
        Text("${admin.usuarios.size} persona(s) registradas", style = MaterialTheme.typography.bodyMedium)
        admin.usuarios.forEach { p ->
            val soyYo = p.id == miId
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.nombre + if (soyYo) " (tú)" else "", style = MaterialTheme.typography.titleMedium)
                        Text("${p.rutFormateado()} · ${p.correo}", style = MaterialTheme.typography.bodyMedium)
                        Text(p.comuna + if (!p.activo) " · DESACTIVADA" else "", style = MaterialTheme.typography.bodyMedium)
                    }
                    Etiqueta(p.rol.etiqueta)
                }
                if (!soyYo) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        if (p.rol == Rol.CLIENTE) {
                            TextButton(onClick = {
                                avm.onFiltroUsuario(p.id)
                                avm.limpiarMensaje()
                                vm.irA(Pantalla.ADMIN_COMPROMISOS)
                            }) { Text("Ver cobros") }
                        }
                        TextButton(onClick = { avm.alternarRol(p) }) {
                            Text(if (p.rol == Rol.ADMIN) "Hacer cliente" else "Hacer admin")
                        }
                        TextButton(onClick = { avm.alternarActivo(p) }) { Text(if (p.activo) "Desactivar" else "Activar") }
                        TextButton(onClick = { aEliminar = p }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    aEliminar?.let { p ->
        Confirmacion(
            titulo = "¿Eliminar a ${p.primerNombre()}?",
            mensaje = "Se borrará la cuenta de ${p.nombre} (${p.rutFormateado()}) con todos sus cobros.",
            textoConfirmar = "Eliminar",
            onConfirmar = {
                avm.eliminarUsuario(p)
                aEliminar = null
            },
            onCancelar = { aEliminar = null }
        )
    }
}

@Composable
fun AdminUsuarioFormScreen(vm: PagoFlexViewModel, admin: AdminUiState, avm: AdminViewModel) {
    val f = admin.formUsuario
    PantallaBase(titulo = Pantalla.ADMIN_USUARIO_FORM.titulo, onVolver = vm::volver) {
        Tarjeta(titulo = "Datos de la persona") {
            Campo(f.rut, { v -> avm.onFormUsuario { it.copy(rut = v.take(12), errores = it.errores - "rut") } }, "RUT", f.errores["rut"])
            Campo(f.nombre, { v -> avm.onFormUsuario { it.copy(nombre = v, errores = it.errores - "nombre") } }, "Nombre completo", f.errores["nombre"])
            Campo(f.correo, { v -> avm.onFormUsuario { it.copy(correo = v, errores = it.errores - "correo") } }, "Correo", f.errores["correo"], KeyboardType.Email)
            Campo(f.comuna, { v -> avm.onFormUsuario { it.copy(comuna = v, errores = it.errores - "comuna") } }, "Comuna", f.errores["comuna"])
            Campo(f.telefono, { v -> avm.onFormUsuario { it.copy(telefono = v, errores = it.errores - "telefono") } }, "Teléfono (opcional)", f.errores["telefono"], KeyboardType.Phone)
            Campo(
                f.pin, { v -> avm.onFormUsuario { it.copy(pin = v.filter { c -> c.isDigit() }.take(4), errores = it.errores - "pin") } },
                "PIN inicial (4 números)", f.errores["pin"], KeyboardType.NumberPassword
            )
        }
        Tarjeta(titulo = "Rol") {
            Chips(Rol.entries, f.rol, { r -> avm.onFormUsuario { it.copy(rol = r) } }) { it.etiqueta }
        }
        BotonPrincipal("Guardar persona", { avm.guardarUsuario { vm.volver() } }, admin.estadoFormulario !is EstadoOperacion.Cargando)
        MensajeOperacion(admin.estadoFormulario)
    }
}

@Composable
fun AdminCompromisosScreen(vm: PagoFlexViewModel, admin: AdminUiState, avm: AdminViewModel) {
    var aEliminar by remember { mutableStateOf<Compromiso?>(null) }
    PantallaBase(titulo = Pantalla.ADMIN_COMPROMISOS.titulo, onVolver = vm::volver) {
        BotonPrincipal("Nuevo cobro", {
            avm.nuevoCompromiso()
            vm.irA(Pantalla.ADMIN_COMPROMISO_FORM)
        })
        MensajeOperacion(admin.mensaje)
        Chips(listOf<Long?>(null) + admin.clientes().map { it.id }, admin.filtroUsuarioId, avm::onFiltroUsuario) { id ->
            if (id == null) "Todos" else admin.nombreDe(id).split(" ").first()
        }
        val lista = admin.compromisosFiltrados()
        Text(
            "${lista.size} cobro(s) · por pagar: ${pesos(lista.filter { it.porPagar() }.sumOf { it.total() })}",
            style = MaterialTheme.typography.bodyMedium
        )
        lista.forEach { c ->
            Text(admin.nombreDe(c.usuarioId), style = MaterialTheme.typography.labelLarge)
            TarjetaCompromiso(c, pesos(c.total())) {
                if (c.estado != EstadoCompromiso.PAGADO) {
                    avm.editarCompromiso(c)
                    vm.irA(Pantalla.ADMIN_COMPROMISO_FORM)
                }
            }
            if (c.estado != EstadoCompromiso.PAGADO) {
                TextButton(onClick = { aEliminar = c }) { Text("Eliminar ${c.folio}", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    aEliminar?.let { c ->
        Confirmacion(
            titulo = "¿Eliminar el cobro ${c.folio}?",
            mensaje = "${c.empresa} · ${c.concepto} · ${pesos(c.monto)}",
            textoConfirmar = "Eliminar",
            onConfirmar = {
                avm.eliminarCompromiso(c)
                aEliminar = null
            },
            onCancelar = { aEliminar = null }
        )
    }
}

@Composable
fun AdminCompromisoFormScreen(vm: PagoFlexViewModel, admin: AdminUiState, avm: AdminViewModel) {
    val f = admin.formCompromiso
    val titulo = if (f.esEdicion()) "Editar cobro" else "Nuevo cobro"
    PantallaBase(titulo = titulo, onVolver = vm::volver) {
        Tarjeta(titulo = "¿A quién se le cobra?") {
            val clientes = admin.clientes()
            if (clientes.isEmpty()) Text("No hay clientes activos.", style = MaterialTheme.typography.bodyMedium)
            Chips(clientes.map { it.id }, f.usuarioId, { id -> avm.onFormCompromiso { it.copy(usuarioId = id, errores = it.errores - "usuario") } }) { id ->
                admin.nombreDe(id)
            }
            f.errores["usuario"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        Tarjeta(titulo = "Datos del cobro") {
            Chips(TipoCompromiso.entries, f.tipo, { t -> avm.onFormCompromiso { it.copy(tipo = t) } }) { it.etiqueta }
            Campo(f.empresa, { v -> avm.onFormCompromiso { it.copy(empresa = v, errores = it.errores - "empresa") } }, "Empresa", f.errores["empresa"])
            Campo(f.concepto, { v -> avm.onFormCompromiso { it.copy(concepto = v, errores = it.errores - "concepto") } }, "Concepto", f.errores["concepto"])
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Campo(f.cuota, { v -> avm.onFormCompromiso { it.copy(cuota = v.filter { c -> c.isDigit() }, errores = it.errores - "cuota") } }, "Cuota", null, KeyboardType.Number)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Campo(f.totalCuotas, { v -> avm.onFormCompromiso { it.copy(totalCuotas = v.filter { c -> c.isDigit() }, errores = it.errores - "cuota") } }, "Total cuotas", null, KeyboardType.Number)
                }
            }
            f.errores["cuota"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Campo(f.monto, { v -> avm.onFormCompromiso { it.copy(monto = v.filter { c -> c.isDigit() }, errores = it.errores - "monto") } }, "Monto en pesos", f.errores["monto"], KeyboardType.Number)
            Campo(f.vencimiento, { v -> avm.onFormCompromiso { it.copy(vencimiento = v, errores = it.errores - "vencimiento") } }, "Vencimiento (dd-mm-aaaa)", f.errores["vencimiento"])
            Text("Pago único: cuota 0 y total 0.", style = MaterialTheme.typography.bodyMedium)
        }
        if (f.esEdicion()) {
            Tarjeta(titulo = "Estado") {
                Chips(
                    listOf(EstadoCompromiso.PENDIENTE, EstadoCompromiso.EN_REVISION, EstadoCompromiso.ANULADO),
                    f.estado,
                    { e -> avm.onFormCompromiso { it.copy(estado = e) } }
                ) { it.etiqueta }
            }
        }
        BotonPrincipal("Guardar cobro", { avm.guardarCompromiso { vm.volver() } }, admin.estadoFormulario !is EstadoOperacion.Cargando)
        MensajeOperacion(admin.estadoFormulario)
    }
}

@Composable
fun AdminReportesScreen(vm: PagoFlexViewModel, admin: AdminUiState, avm: AdminViewModel) {
    PantallaBase(titulo = Pantalla.ADMIN_REPORTES.titulo, onVolver = vm::volver) {
        MensajeOperacion(admin.mensaje)
        Text("${admin.reportesAbiertos().size} abierto(s) de ${admin.reportes.size}", style = MaterialTheme.typography.bodyMedium)
        if (admin.reportes.isEmpty()) Text("No hay reportes de clientes.", style = MaterialTheme.typography.bodyMedium)
        admin.reportes.sortedBy { it.estado != EstadoReporte.ABIERTO }.forEach { r ->
            Tarjeta(titulo = "${r.numero} · ${r.estado.etiqueta}") {
                FilaDato("Cliente", admin.nombreDe(r.usuarioId))
                FilaDato("Cobro", r.folio)
                FilaDato("Motivo", r.motivo.etiqueta)
                if (r.comentario.isNotBlank()) FilaDato("Comentario", r.comentario)
                FilaDato("Fecha", r.fecha)
                if (r.respuesta.isNotBlank()) FilaDato("Respuesta", r.respuesta)
                if (r.estado == EstadoReporte.ABIERTO) {
                    AccionReporte.entries.forEach { a ->
                        TextButton(onClick = { avm.resolverReporte(r.id, a) }) { Text(a.etiqueta) }
                    }
                }
            }
        }
    }
}
