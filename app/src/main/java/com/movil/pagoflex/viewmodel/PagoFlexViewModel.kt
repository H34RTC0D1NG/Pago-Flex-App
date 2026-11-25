package com.movil.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.movil.pagoflex.data.CredencialesInvalidas
import com.movil.pagoflex.data.Reglas
import com.movil.pagoflex.data.Repositorio
import com.movil.pagoflex.data.Validador
import com.movil.pagoflex.model.EstadoCompromiso
import com.movil.pagoflex.model.EstadoOperacion
import com.movil.pagoflex.model.FormPerfil
import com.movil.pagoflex.model.FormReferido
import com.movil.pagoflex.model.FormRegistro
import com.movil.pagoflex.model.FormReporte
import com.movil.pagoflex.model.MedioPago
import com.movil.pagoflex.model.MotivoReporte
import com.movil.pagoflex.model.PagoFlexUiState
import com.movil.pagoflex.model.Pantalla
import com.movil.pagoflex.model.pesos
import com.movil.pagoflex.nativo.Vibrador
import com.movil.pagoflex.network.RetrofitService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PagoFlexViewModel(private val repo: Repositorio, private val vibrador: Vibrador) : ViewModel() {

    companion object {
        const val MAX_INTENTOS = 3
        const val BLOQUEO_MS = 30_000L

        fun factory(application: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer { PagoFlexViewModel(Repositorio.obtener(application), Vibrador(application)) }
        }
    }

    private val _uiState = MutableStateFlow(PagoFlexUiState())
    val uiState: StateFlow<PagoFlexUiState> = _uiState.asStateFlow()

    private val mensajesRecompensa = listOf(
        "¡Bien hecho! Un compromiso menos este mes.",
        "Tu historial sigue creciendo. ¡Sigue así!",
        "Pagar a tiempo te ahorra recargos. ¡Buena decisión!",
        "Listo. Ya tienes tu comprobante guardado.",
        "¡Excelente! Tu indicador de cumplimiento se actualizó."
    )

    init {
        _uiState.update {
            it.copy(
                rut = repo.leerRut(),
                config = repo.leerConfig(),
                ocultarMontos = repo.leerOcultarMontos(),
                textoGrande = repo.leerTextoGrande(),
                altoContraste = repo.leerAltoContraste()
            )
        }
    }

    private fun mensajeDe(e: Exception, porDefecto: String): String {
        if (e is CancellationException) throw e
        return e.message ?: porDefecto
    }

    private fun guardarAjustes() {
        val s = _uiState.value
        repo.guardarAjustes(s.config, s.ocultarMontos, s.textoGrande, s.altoContraste)
    }

    fun irA(destino: Pantalla) {
        _uiState.update { it.copy(pantalla = destino, pila = it.pila + it.pantalla) }
    }

    fun volver() {
        _uiState.update {
            if (it.pila.isEmpty()) it else it.copy(pantalla = it.pila.last(), pila = it.pila.dropLast(1))
        }
    }

    private fun irSinVolver(destino: Pantalla) {
        _uiState.update { it.copy(pantalla = destino, pila = emptyList()) }
    }

    fun volverAlInicio() {
        irSinVolver(if (_uiState.value.usuario?.esAdmin() == true) Pantalla.ADMIN_INICIO else Pantalla.INICIO)
    }

    fun refrescar(conServidor: Boolean = false) {
        val usuario = _uiState.value.usuario ?: return
        viewModelScope.launch {
            if (conServidor) repo.sincronizarCliente(usuario.id)
            val compromisos = repo.compromisos(usuario.id)
            val comprobantes = repo.comprobantes(usuario.id)
            val reportes = repo.reportes(usuario.id)
            val recomendaciones = repo.recomendaciones(usuario.id)
            val config = _uiState.value.config
            val indicador = Reglas.indicador(comprobantes)
            _uiState.update {
                it.copy(
                    compromisos = compromisos,
                    comprobantes = comprobantes,
                    reportes = reportes,
                    recomendaciones = recomendaciones,
                    avisos = Reglas.avisos(compromisos, config),
                    logros = Reglas.logros(compromisos, comprobantes, recomendaciones, config),
                    totalMes = Reglas.delMes(compromisos).sumOf { c -> c.total() },
                    vencidos = compromisos.count { c -> c.estadoActual() == EstadoCompromiso.VENCIDO },
                    indicador = indicador,
                    categoria = Reglas.categoria(indicador),
                    sinServidor = !repo.enLinea
                )
            }
        }
    }

    fun onRutCambia(valor: String) {
        _uiState.update { it.copy(rut = valor.take(12), errorRut = null) }
    }

    fun onPinCambia(valor: String) {
        _uiState.update { it.copy(pin = valor.filter { c -> c.isDigit() }.take(4), errorPin = null) }
    }

    fun onAlternarPin() {
        _uiState.update { it.copy(pinVisible = !it.pinVisible) }
    }

    fun usarCuentaCliente() {
        _uiState.update { it.copy(rut = "15.834.207-3", pin = "", errorRut = null, errorPin = null) }
    }

    fun usarCuentaAdmin() {
        _uiState.update { it.copy(rut = "11.111.111-1", pin = "", errorRut = null, errorPin = null) }
    }

    fun onIngresar() {
        val s = _uiState.value
        if (s.bloqueado) return
        val errorRut = Validador.errorRut(s.rut)
        val errorPin = Validador.errorPin(s.pin)
        if (errorRut != null || errorPin != null) {
            vibrador.error()
            _uiState.update { it.copy(errorRut = errorRut, errorPin = errorPin) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(estadoIngreso = EstadoOperacion.Cargando) }
            try {
                val usuario = repo.login(s.rut, s.pin)
                repo.guardarRut(s.rut.trim())
                vibrador.exito()
                _uiState.update {
                    it.copy(usuario = usuario, pin = "", intentos = 0, estadoIngreso = EstadoOperacion.Inactivo, sinServidor = !repo.enLinea)
                }
                if (usuario.esAdmin()) {
                    irSinVolver(Pantalla.ADMIN_INICIO)
                } else {
                    refrescar(true)
                    irSinVolver(Pantalla.INICIO)
                }
                cargarIndicadores()
            } catch (e: CredencialesInvalidas) {
                vibrador.error()
                intentoFallido(e.message ?: "RUT o PIN incorrectos")
            } catch (e: Exception) {
                val mensaje = mensajeDe(e, "No se pudo ingresar")
                vibrador.error()
                _uiState.update { it.copy(pin = "", estadoIngreso = EstadoOperacion.Error(mensaje)) }
            }
        }
    }

    private suspend fun intentoFallido(mensaje: String) {
        val intentos = _uiState.value.intentos + 1
        if (intentos >= MAX_INTENTOS) {
            _uiState.update {
                it.copy(
                    intentos = intentos, pin = "", bloqueado = true,
                    estadoIngreso = EstadoOperacion.Error("Demasiados intentos. Por seguridad espera 30 segundos.")
                )
            }
            delay(BLOQUEO_MS)
            _uiState.update { it.copy(intentos = 0, bloqueado = false, estadoIngreso = EstadoOperacion.Inactivo) }
        } else {
            _uiState.update {
                it.copy(
                    intentos = intentos, pin = "",
                    estadoIngreso = EstadoOperacion.Error("$mensaje. Te quedan ${MAX_INTENTOS - intentos} intentos.")
                )
            }
        }
    }

    fun cerrarSesion() {
        _uiState.update {
            PagoFlexUiState(
                rut = it.rut, config = it.config, ocultarMontos = it.ocultarMontos,
                textoGrande = it.textoGrande, altoContraste = it.altoContraste, indicadores = it.indicadores
            )
        }
    }

    fun irARegistro() {
        _uiState.update { it.copy(formRegistro = FormRegistro(), estadoRegistro = EstadoOperacion.Inactivo) }
        irA(Pantalla.REGISTRO)
    }

    fun onFormRegistro(cambio: (FormRegistro) -> FormRegistro) {
        _uiState.update { it.copy(formRegistro = cambio(it.formRegistro), estadoRegistro = EstadoOperacion.Inactivo) }
    }

    fun onRegistrar() {
        val form = _uiState.value.formRegistro
        val errores = Validador.validarRegistro(form)
        if (errores.isNotEmpty()) {
            vibrador.error()
            _uiState.update { it.copy(formRegistro = form.copy(errores = errores)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(estadoRegistro = EstadoOperacion.Cargando) }
            try {
                val enServidor = repo.registrar(form.rut, form.nombre, form.correo, form.comuna, form.telefono, form.pin)
                vibrador.exito()
                val donde = if (enServidor) "en el teléfono y en el servidor" else "en el teléfono (servidor sin conexión)"
                _uiState.update {
                    it.copy(
                        rut = form.rut.trim(), pin = "", errorRut = null, errorPin = null,
                        formRegistro = FormRegistro(),
                        estadoRegistro = EstadoOperacion.Inactivo,
                        estadoIngreso = EstadoOperacion.Exito("Cuenta creada $donde. Ingresa con tu PIN.")
                    )
                }
                volver()
            } catch (e: Exception) {
                val mensaje = mensajeDe(e, "No se pudo crear la cuenta")
                vibrador.error()
                _uiState.update { it.copy(estadoRegistro = EstadoOperacion.Error(mensaje)) }
            }
        }
    }

    fun onFiltroEstado(estado: EstadoCompromiso?) {
        _uiState.update { it.copy(filtroEstado = estado) }
    }

    fun onFiltroEmpresa(empresa: String?) {
        _uiState.update { it.copy(filtroEmpresa = empresa) }
    }

    fun verDetalle(id: Long) {
        _uiState.update { it.copy(compromisoId = id) }
        irA(Pantalla.DETALLE)
    }

    fun motivoBloqueo(): String? {
        val s = _uiState.value
        val compromiso = s.compromisoSeleccionado() ?: return null
        return Reglas.motivoBloqueo(compromiso, s.compromisos)
    }

    fun irAPagar() {
        _uiState.update { it.copy(medioPago = null, errorMedio = null, confirmarPago = false, estadoPago = EstadoOperacion.Inactivo) }
        irA(Pantalla.PAGO)
    }

    fun onMedioSeleccionado(medio: MedioPago) {
        _uiState.update { it.copy(medioPago = medio, errorMedio = null) }
    }

    fun onSolicitarPago() {
        if (_uiState.value.medioPago == null) {
            vibrador.error()
            _uiState.update { it.copy(errorMedio = "Elige cómo quieres pagar") }
            return
        }
        _uiState.update { it.copy(confirmarPago = true) }
    }

    fun onCancelarPago() {
        _uiState.update { it.copy(confirmarPago = false) }
    }

    fun onConfirmarPago() {
        val s = _uiState.value
        val id = s.compromisoId ?: return
        val medio = s.medioPago ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(confirmarPago = false, estadoPago = EstadoOperacion.Cargando) }
            try {
                val comprobante = repo.pagar(id, medio)
                vibrador.exito()
                refrescar()
                _uiState.update {
                    it.copy(
                        estadoPago = EstadoOperacion.Exito("Pago confirmado por ${pesos(comprobante.monto)}"),
                        comprobanteVisto = comprobante,
                        recompensa = mensajesRecompensa.random(),
                        pantalla = Pantalla.COMPROBANTE
                    )
                }
            } catch (e: Exception) {
                val mensaje = mensajeDe(e, "No se pudo pagar")
                vibrador.error()
                _uiState.update { it.copy(estadoPago = EstadoOperacion.Error(mensaje)) }
            }
        }
    }

    fun verComprobante(id: Long) {
        val comprobante = _uiState.value.comprobantes.find { it.id == id } ?: return
        _uiState.update { it.copy(comprobanteVisto = comprobante, recompensa = null) }
        irA(Pantalla.COMPROBANTE)
    }

    fun irAReportar() {
        _uiState.update { it.copy(formReporte = FormReporte(), estadoReporte = EstadoOperacion.Inactivo) }
        irA(Pantalla.REPORTAR)
    }

    fun onMotivoReporte(motivo: MotivoReporte) {
        _uiState.update { it.copy(formReporte = it.formReporte.copy(motivo = motivo, error = null)) }
    }

    fun onComentarioReporte(valor: String) {
        _uiState.update { it.copy(formReporte = it.formReporte.copy(comentario = valor.take(300), error = null)) }
    }

    fun onEnviarReporte() {
        val s = _uiState.value
        val id = s.compromisoId ?: return
        val form = s.formReporte
        if (form.motivo == MotivoReporte.OTRO && form.comentario.isBlank()) {
            vibrador.error()
            _uiState.update { it.copy(formReporte = form.copy(error = "Si eliges \"Otro\" debes escribir un comentario")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(estadoReporte = EstadoOperacion.Cargando) }
            try {
                val reporte = repo.reportar(id, form.motivo, form.comentario)
                vibrador.exito()
                refrescar()
                _uiState.update {
                    it.copy(
                        formReporte = FormReporte(),
                        estadoReporte = EstadoOperacion.Exito("Reporte ${reporte.numero} enviado. Tu compromiso queda en revisión.")
                    )
                }
            } catch (e: Exception) {
                val mensaje = mensajeDe(e, "No se pudo enviar")
                _uiState.update { it.copy(estadoReporte = EstadoOperacion.Error(mensaje)) }
            }
        }
    }

    fun onFormReferido(cambio: (FormReferido) -> FormReferido) {
        _uiState.update { it.copy(formReferido = cambio(it.formReferido), estadoReferido = EstadoOperacion.Inactivo) }
    }

    fun onEnviarReferido() {
        val s = _uiState.value
        val usuario = s.usuario ?: return
        val form = s.formReferido
        val errores = Validador.validarReferido(form)
        if (errores.isNotEmpty()) {
            vibrador.error()
            _uiState.update { it.copy(formReferido = form.copy(errores = errores)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(estadoReferido = EstadoOperacion.Cargando) }
            try {
                val nueva = repo.recomendar(usuario.id, form.organizacion, form.rubro, form.comuna, form.contacto, form.autoriza)
                vibrador.exito()
                refrescar()
                _uiState.update {
                    it.copy(
                        formReferido = FormReferido(),
                        estadoReferido = EstadoOperacion.Exito("¡Gracias! Recibimos tu recomendación ${nueva.numero}.")
                    )
                }
            } catch (e: Exception) {
                val mensaje = mensajeDe(e, "No se pudo enviar")
                _uiState.update { it.copy(estadoReferido = EstadoOperacion.Error(mensaje)) }
            }
        }
    }

    fun eliminarRecomendacion(id: Long) {
        viewModelScope.launch {
            try {
                repo.eliminarRecomendacion(id)
                refrescar()
                _uiState.update { it.copy(estadoReferido = EstadoOperacion.Exito("Recomendación retirada")) }
            } catch (e: Exception) {
                val mensaje = mensajeDe(e, "No se pudo retirar")
                _uiState.update { it.copy(estadoReferido = EstadoOperacion.Error(mensaje)) }
            }
        }
    }

    fun codigoReferido(): String = "PF-" + (_uiState.value.usuario?.primerNombre()?.uppercase() ?: "AMIGO") + "-2026"

    fun irAEditarPerfil() {
        val u = _uiState.value.usuario ?: return
        _uiState.update {
            it.copy(
                formPerfil = FormPerfil(nombre = u.nombre, correo = u.correo, comuna = u.comuna, telefono = u.telefono),
                estadoPerfil = EstadoOperacion.Inactivo
            )
        }
        irA(Pantalla.EDITAR_PERFIL)
    }

    fun onFormPerfil(cambio: (FormPerfil) -> FormPerfil) {
        _uiState.update { it.copy(formPerfil = cambio(it.formPerfil), estadoPerfil = EstadoOperacion.Inactivo) }
    }

    fun onGuardarPerfil() {
        val s = _uiState.value
        val usuario = s.usuario ?: return
        val form = s.formPerfil
        val errores = Validador.validarPerfil(form)
        if (errores.isNotEmpty()) {
            vibrador.error()
            _uiState.update { it.copy(formPerfil = form.copy(errores = errores)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(estadoPerfil = EstadoOperacion.Cargando) }
            try {
                val actualizado = repo.actualizarPerfil(usuario.id, form.nombre, form.correo, form.comuna, form.telefono, form.nuevoPin)
                vibrador.exito()
                _uiState.update {
                    it.copy(
                        usuario = actualizado,
                        formPerfil = form.copy(nuevoPin = "", errores = emptyMap()),
                        estadoPerfil = EstadoOperacion.Exito("Tus datos se guardaron")
                    )
                }
            } catch (e: Exception) {
                val mensaje = mensajeDe(e, "No se pudieron guardar tus datos")
                vibrador.error()
                _uiState.update { it.copy(estadoPerfil = EstadoOperacion.Error(mensaje)) }
            }
        }
    }

    fun onPedirEliminarCuenta() {
        _uiState.update { it.copy(confirmarEliminarCuenta = true) }
    }

    fun onCancelarEliminarCuenta() {
        _uiState.update { it.copy(confirmarEliminarCuenta = false) }
    }

    fun onConfirmarEliminarCuenta() {
        val usuario = _uiState.value.usuario ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(confirmarEliminarCuenta = false, estadoPerfil = EstadoOperacion.Cargando) }
            try {
                repo.eliminarCuenta(usuario.id)
                repo.guardarRut("")
                _uiState.update {
                    PagoFlexUiState(
                        config = it.config, ocultarMontos = it.ocultarMontos,
                        textoGrande = it.textoGrande, altoContraste = it.altoContraste,
                        estadoIngreso = EstadoOperacion.Exito("Tu cuenta fue eliminada.")
                    )
                }
            } catch (e: Exception) {
                val mensaje = mensajeDe(e, "No se pudo eliminar la cuenta")
                _uiState.update { it.copy(estadoPerfil = EstadoOperacion.Error(mensaje)) }
            }
        }
    }

    fun cargarIndicadores() {
        viewModelScope.launch {
            _uiState.update { it.copy(estadoIndicadores = EstadoOperacion.Cargando) }
            try {
                val lista = RetrofitService.obtenerIndicadores()
                _uiState.update { it.copy(indicadores = lista, estadoIndicadores = EstadoOperacion.Inactivo) }
            } catch (e: Exception) {
                val mensaje = mensajeDe(e, "Error")
                _uiState.update {
                    it.copy(estadoIndicadores = EstadoOperacion.Error("No pudimos consultar mindicador.cl ($mensaje)"))
                }
            }
        }
    }

    fun cargarIndicadoresSiHaceFalta() {
        val s = _uiState.value
        if (s.indicadores.isEmpty() && s.estadoIndicadores !is EstadoOperacion.Cargando) cargarIndicadores()
    }

    private fun cambiarConfig(cambio: (PagoFlexUiState) -> PagoFlexUiState) {
        _uiState.update(cambio)
        guardarAjustes()
        refrescar()
    }

    fun onAvisosActivos(valor: Boolean) = cambiarConfig { it.copy(config = it.config.copy(activos = valor)) }
    fun onDiasAnticipacion(dias: Int) = cambiarConfig { it.copy(config = it.config.copy(diasAnticipacion = dias)) }
    fun onMaximoPorCiclo(maximo: Int) = cambiarConfig { it.copy(config = it.config.copy(maximoPorCiclo = maximo)) }
    fun onRespetarHorario(valor: Boolean) = cambiarConfig { it.copy(config = it.config.copy(respetarHorario = valor)) }
    fun onOcultarMontos(valor: Boolean) = cambiarConfig { it.copy(ocultarMontos = valor) }
    fun onTextoGrande(valor: Boolean) = cambiarConfig { it.copy(textoGrande = valor) }
    fun onAltoContraste(valor: Boolean) = cambiarConfig { it.copy(altoContraste = valor) }
}
