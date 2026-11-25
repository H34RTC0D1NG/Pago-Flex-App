package com.movil.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.movil.pagoflex.data.Repositorio
import com.movil.pagoflex.data.Validador
import com.movil.pagoflex.model.AccionReporte
import com.movil.pagoflex.model.AdminUiState
import com.movil.pagoflex.model.Compromiso
import com.movil.pagoflex.model.EstadoOperacion
import com.movil.pagoflex.model.FormCompromiso
import com.movil.pagoflex.model.FormUsuario
import com.movil.pagoflex.model.Rol
import com.movil.pagoflex.model.Usuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = Repositorio.obtener(application)

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private fun mensajeDe(e: Exception, porDefecto: String): String {
        if (e is CancellationException) throw e
        return e.message ?: porDefecto
    }

    fun cargarTodo() {
        viewModelScope.launch {
            val usuarios = repo.usuarios()
            val compromisos = repo.todosLosCompromisos()
            val reportes = repo.todosLosReportes()
            _uiState.update { it.copy(usuarios = usuarios, compromisos = compromisos, reportes = reportes) }
        }
    }

    private fun accion(exito: String, bloque: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                bloque()
                _uiState.update { it.copy(mensaje = EstadoOperacion.Exito(exito)) }
                cargarTodo()
            } catch (e: Exception) {
                val mensaje = mensajeDe(e, "No se pudo completar la acción")
                _uiState.update { it.copy(mensaje = EstadoOperacion.Error(mensaje)) }
            }
        }
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensaje = EstadoOperacion.Inactivo) }
    }

    fun onFiltroUsuario(id: Long?) {
        _uiState.update { it.copy(filtroUsuarioId = id) }
    }

    fun alternarRol(persona: Usuario) {
        val nuevo = if (persona.rol == Rol.ADMIN) Rol.CLIENTE else Rol.ADMIN
        accion("${persona.primerNombre()} ahora es ${nuevo.etiqueta.lowercase()}") { repo.cambiarRol(persona.id, nuevo) }
    }

    fun alternarActivo(persona: Usuario) {
        val texto = if (persona.activo) "desactivada" else "activada"
        accion("Cuenta de ${persona.primerNombre()} $texto") { repo.cambiarActivo(persona.id, !persona.activo) }
    }

    fun eliminarUsuario(persona: Usuario) {
        accion("Se eliminó a ${persona.nombre}") { repo.eliminarUsuario(persona.id) }
    }

    fun nuevoUsuario() {
        _uiState.update { it.copy(formUsuario = FormUsuario(), estadoFormulario = EstadoOperacion.Inactivo) }
    }

    fun onFormUsuario(cambio: (FormUsuario) -> FormUsuario) {
        _uiState.update { it.copy(formUsuario = cambio(it.formUsuario), estadoFormulario = EstadoOperacion.Inactivo) }
    }

    fun guardarUsuario(alTerminar: () -> Unit) {
        val form = _uiState.value.formUsuario
        val errores = Validador.validarUsuario(form)
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(formUsuario = form.copy(errores = errores)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(estadoFormulario = EstadoOperacion.Cargando) }
            try {
                repo.crearUsuario(form)
                _uiState.update {
                    it.copy(
                        estadoFormulario = EstadoOperacion.Inactivo,
                        mensaje = EstadoOperacion.Exito("Se agregó a ${form.nombre.trim()} como ${form.rol.etiqueta.lowercase()}")
                    )
                }
                cargarTodo()
                alTerminar()
            } catch (e: Exception) {
                val mensaje = mensajeDe(e, "No se pudo guardar")
                _uiState.update { it.copy(estadoFormulario = EstadoOperacion.Error(mensaje)) }
            }
        }
    }

    fun nuevoCompromiso() {
        _uiState.update {
            it.copy(
                formCompromiso = FormCompromiso(usuarioId = it.filtroUsuarioId),
                estadoFormulario = EstadoOperacion.Inactivo
            )
        }
    }

    fun editarCompromiso(c: Compromiso) {
        _uiState.update {
            it.copy(
                formCompromiso = FormCompromiso(
                    id = c.id, usuarioId = c.usuarioId, empresa = c.empresa, tipo = c.tipo, concepto = c.concepto,
                    cuota = c.cuota.toString(), totalCuotas = c.totalCuotas.toString(), monto = c.monto.toString(),
                    vencimiento = c.vencimiento.texto(), estado = c.estado
                ),
                estadoFormulario = EstadoOperacion.Inactivo
            )
        }
    }

    fun onFormCompromiso(cambio: (FormCompromiso) -> FormCompromiso) {
        _uiState.update { it.copy(formCompromiso = cambio(it.formCompromiso), estadoFormulario = EstadoOperacion.Inactivo) }
    }

    fun guardarCompromiso(alTerminar: () -> Unit) {
        val form = _uiState.value.formCompromiso
        val errores = Validador.validarCompromiso(form)
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(formCompromiso = form.copy(errores = errores)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(estadoFormulario = EstadoOperacion.Cargando) }
            try {
                repo.guardarCompromiso(form)
                val texto = if (form.esEdicion()) "Cobro actualizado" else "Cobro creado"
                _uiState.update { it.copy(estadoFormulario = EstadoOperacion.Inactivo, mensaje = EstadoOperacion.Exito(texto)) }
                cargarTodo()
                alTerminar()
            } catch (e: Exception) {
                val mensaje = mensajeDe(e, "No se pudo guardar")
                _uiState.update { it.copy(estadoFormulario = EstadoOperacion.Error(mensaje)) }
            }
        }
    }

    fun eliminarCompromiso(c: Compromiso) {
        accion("Cobro ${c.folio} eliminado") { repo.eliminarCompromiso(c.id) }
    }

    fun resolverReporte(id: Long, accionReporte: AccionReporte) {
        accion("Reporte cerrado: ${accionReporte.etiqueta.lowercase()}") { repo.resolverReporte(id, accionReporte) }
    }
}
