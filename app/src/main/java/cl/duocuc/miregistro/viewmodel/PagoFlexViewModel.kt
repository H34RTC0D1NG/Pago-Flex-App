package cl.duocuc.miregistro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duocuc.miregistro.data.AlmacenLocal
import cl.duocuc.miregistro.data.RepositorioPagoFlex
import cl.duocuc.miregistro.nativo.Vibrador
import cl.duocuc.miregistro.model.EstadoCompromiso
import cl.duocuc.miregistro.model.EstadoOperacion
import cl.duocuc.miregistro.model.FormReferido
import cl.duocuc.miregistro.model.FormReporte
import cl.duocuc.miregistro.model.MedioPago
import cl.duocuc.miregistro.model.MotivoReporte
import cl.duocuc.miregistro.model.PagoFlexUiState
import cl.duocuc.miregistro.model.Pantalla
import cl.duocuc.miregistro.model.pesos
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PagoFlexViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = RepositorioPagoFlex()
    private val almacen = AlmacenLocal(application)
    private val vibrador = Vibrador(application)

    private val _uiState = MutableStateFlow(PagoFlexUiState())
    val uiState: StateFlow<PagoFlexUiState> = _uiState.asStateFlow()

    init {
        repositorio.restaurar(almacen.leerComprobantes(), almacen.leerReportes(), almacen.leerRecomendaciones())
        _uiState.update {
            it.copy(
                rut = almacen.leerRut(),
                config = almacen.leerConfig(),
                ocultarMontos = almacen.leerOcultarMontos(),
                textoGrande = almacen.leerTextoGrande(),
                altoContraste = almacen.leerAltoContraste(),
                ultimaSincronizacion = almacen.leerUltimaSincronizacion()
            )
        }
    }

    private fun guardarDatos() {
        almacen.guardarComprobantes(repositorio.comprobantesDeLaApp())
        almacen.guardarReportes(repositorio.reportes())
        almacen.guardarRecomendaciones(repositorio.recomendacionesNuevas())
    }

    private fun guardarAjustes() {
        val estado = _uiState.value
        almacen.guardarAjustes(estado.config, estado.ocultarMontos, estado.textoGrande, estado.altoContraste)
    }

    fun resumenGuardado(): String =
        "${repositorio.comprobantesDeLaApp().size} pago(s), ${repositorio.reportes().size} reporte(s) y " +
            "${repositorio.recomendacionesNuevas().size} recomendación(es) guardados en este teléfono."

    fun borrarDatosGuardados() {
        almacen.borrarTodo()
        guardarAjustes()
        _uiState.update { it.copy(estadoSync = EstadoOperacion.Exito("Datos borrados del teléfono. Se aplicará al volver a abrir la app.")) }
    }

    private val mensajesRecompensa = listOf(
        "¡Bien hecho! Un compromiso menos este mes.",
        "Tu historial sigue creciendo. ¡Sigue así!",
        "Pagar a tiempo te ahorra recargos. ¡Buena decisión!",
        "Listo. Ya tienes tu comprobante guardado.",
        "¡Excelente! Tu indicador de cumplimiento se actualizó."
    )

    fun irA(destino: Pantalla) {
        _uiState.update { it.copy(pantalla = destino, pila = it.pila + it.pantalla) }
    }

    fun volver() {
        _uiState.update {
            if (it.pila.isEmpty()) it
            else it.copy(pantalla = it.pila.last(), pila = it.pila.dropLast(1))
        }
    }

    private fun irSinVolver(destino: Pantalla) {
        _uiState.update { it.copy(pantalla = destino, pila = emptyList()) }
    }

    private fun refrescar() {
        val config = _uiState.value.config
        _uiState.update {
            it.copy(
                compromisos = repositorio.compromisos(),
                comprobantes = repositorio.comprobantes(),
                recomendaciones = repositorio.recomendaciones(),
                reportes = repositorio.reportes(),
                avisos = repositorio.avisos(config),
                logros = repositorio.logros(config),
                totalMes = repositorio.totalDelMes(),
                vencidos = repositorio.cantidadVencidos(),
                proximo = repositorio.proximoVencimiento(),
                indicador = repositorio.indicador(),
                categoria = repositorio.categoria(),
                mensajeCategoria = repositorio.mensajeCategoria()
            )
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

    fun usarCuentaPrueba() {
        _uiState.update { it.copy(rut = repositorio.usuario.rut, pin = "", errorRut = null, errorPin = null) }
    }

    fun onIngresar() {
        val estado = _uiState.value
        if (estado.bloqueado) return
        val errorRut = when {
            estado.rut.isBlank() -> "El RUT es obligatorio"
            !repositorio.rutValido(estado.rut) -> "RUT no válido, revisa el dígito verificador"
            else -> null
        }
        val errorPin = if (estado.pin.length != 4) "El PIN debe tener 4 números" else null
        if (errorRut != null || errorPin != null) {
            vibrador.error()
            _uiState.update { it.copy(errorRut = errorRut, errorPin = errorPin) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(estadoIngreso = EstadoOperacion.Cargando) }
            delay(800)
            val usuario = repositorio.usuario
            val rutIngresado = estado.rut.replace(".", "").replace("-", "").uppercase()
            val rutGuardado = usuario.rut.replace(".", "").replace("-", "").uppercase()
            val correcto = rutIngresado == rutGuardado && estado.pin.hashCode() == usuario.pinGuardado
            if (correcto) {
                almacen.guardarRut(usuario.rut)
                vibrador.exito()
                _uiState.update { it.copy(usuario = usuario, pin = "", intentos = 0, estadoIngreso = EstadoOperacion.Inactivo) }
                refrescar()
                irSinVolver(Pantalla.INICIO)
                sincronizar()
            } else {
                vibrador.error()
                val intentos = estado.intentos + 1
                if (intentos >= 3) {
                    _uiState.update {
                        it.copy(
                            intentos = intentos, pin = "", bloqueado = true,
                            estadoIngreso = EstadoOperacion.Error("Demasiados intentos. Por seguridad espera 30 segundos.")
                        )
                    }
                    delay(30_000)
                    _uiState.update { it.copy(intentos = 0, bloqueado = false, estadoIngreso = EstadoOperacion.Inactivo) }
                } else {
                    _uiState.update {
                        it.copy(
                            intentos = intentos, pin = "",
                            estadoIngreso = EstadoOperacion.Error("RUT o PIN incorrectos. Te quedan ${3 - intentos} intentos.")
                        )
                    }
                }
            }
        }
    }

    fun cerrarSesion() {
        _uiState.update {
            PagoFlexUiState(
                pantalla = Pantalla.INGRESO, rut = it.rut, config = it.config, ocultarMontos = it.ocultarMontos,
                textoGrande = it.textoGrande, altoContraste = it.altoContraste, ultimaSincronizacion = it.ultimaSincronizacion
            )
        }
    }

    fun sincronizar() {
        viewModelScope.launch {
            _uiState.update { it.copy(estadoSync = EstadoOperacion.Cargando) }
            try {
                val momento = repositorio.sincronizar(_uiState.value.sinConexion)
                almacen.guardarUltimaSincronizacion(momento)
                refrescar()
                _uiState.update {
                    it.copy(ultimaSincronizacion = momento, estadoSync = EstadoOperacion.Exito("Datos actualizados"))
                }
            } catch (e: IllegalStateException) {
                _uiState.update { it.copy(estadoSync = EstadoOperacion.Error(e.message ?: "Error al sincronizar")) }
            }
        }
    }

    fun onSinConexionCambia(valor: Boolean) {
        _uiState.update { it.copy(sinConexion = valor, estadoSync = EstadoOperacion.Inactivo) }
    }

    fun onFiltroEstado(estado: EstadoCompromiso?) {
        _uiState.update { it.copy(filtroEstado = estado) }
    }

    fun onFiltroEmpresa(empresa: String?) {
        _uiState.update { it.copy(filtroEmpresa = empresa) }
    }

    fun verDetalle(folio: String) {
        _uiState.update { it.copy(folioSeleccionado = folio) }
        irA(Pantalla.DETALLE)
    }

    fun motivoBloqueo(): String? {
        val compromiso = _uiState.value.compromisoSeleccionado() ?: return null
        return repositorio.motivoBloqueo(compromiso)
    }

    fun irAPagar() {
        _uiState.update {
            it.copy(medioPago = null, errorMedio = null, confirmarPago = false, estadoPago = EstadoOperacion.Inactivo)
        }
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
        val estado = _uiState.value
        val folio = estado.folioSeleccionado ?: return
        val medio = estado.medioPago ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(confirmarPago = false, estadoPago = EstadoOperacion.Cargando) }
            try {
                val comprobante = repositorio.pagar(folio, medio, estado.sinConexion)
                guardarDatos()
                vibrador.exito()
                refrescar()
                _uiState.update {
                    it.copy(
                        estadoPago = EstadoOperacion.Exito("Pago confirmado por ${pesos(comprobante.monto)}"),
                        ultimoComprobante = comprobante,
                        recompensa = mensajesRecompensa.random()
                    )
                }
                irA(Pantalla.COMPROBANTE)
            } catch (e: IllegalStateException) {
                vibrador.error()
                _uiState.update { it.copy(estadoPago = EstadoOperacion.Error(e.message ?: "No se pudo pagar")) }
            } catch (e: IllegalArgumentException) {
                vibrador.error()
                _uiState.update { it.copy(estadoPago = EstadoOperacion.Error(e.message ?: "No se pudo pagar")) }
            }
        }
    }

    fun verComprobante(numero: String) {
        val comprobante = _uiState.value.comprobantes.find { it.numero == numero } ?: return
        _uiState.update { it.copy(ultimoComprobante = comprobante, recompensa = null) }
        irA(Pantalla.COMPROBANTE)
    }

    fun volverAlInicio() {
        irSinVolver(Pantalla.INICIO)
    }

    fun irAReportar() {
        _uiState.update { it.copy(formReporte = FormReporte(), estadoReporte = EstadoOperacion.Inactivo) }
        irA(Pantalla.REPORTAR)
    }

    fun onMotivoReporte(motivo: MotivoReporte) {
        _uiState.update { it.copy(formReporte = it.formReporte.copy(motivo = motivo, errorComentario = null)) }
    }

    fun onComentarioReporte(valor: String) {
        _uiState.update { it.copy(formReporte = it.formReporte.copy(comentario = valor.take(300), errorComentario = null)) }
    }

    fun onEnviarReporte() {
        val estado = _uiState.value
        val folio = estado.folioSeleccionado ?: return
        val form = estado.formReporte
        if (form.motivo == MotivoReporte.OTRO && form.comentario.isBlank()) {
            vibrador.error()
            _uiState.update { it.copy(formReporte = form.copy(errorComentario = "Si eliges \"Otro\" debes escribir un comentario")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(estadoReporte = EstadoOperacion.Cargando) }
            try {
                val reporte = repositorio.reportar(folio, form.motivo, form.comentario, estado.sinConexion)
                guardarDatos()
                vibrador.exito()
                refrescar()
                _uiState.update {
                    it.copy(
                        formReporte = FormReporte(),
                        estadoReporte = EstadoOperacion.Exito("Reporte ${reporte.numero} enviado. Tu compromiso queda en revisión.")
                    )
                }
            } catch (e: IllegalStateException) {
                _uiState.update { it.copy(estadoReporte = EstadoOperacion.Error(e.message ?: "No se pudo enviar")) }
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(estadoReporte = EstadoOperacion.Error(e.message ?: "No se pudo enviar")) }
            }
        }
    }

    fun onOrganizacionCambia(valor: String) {
        _uiState.update { it.copy(formReferido = it.formReferido.copy(organizacion = valor, errorOrganizacion = null)) }
    }

    fun onRubroCambia(valor: String) {
        _uiState.update { it.copy(formReferido = it.formReferido.copy(rubro = valor, errorRubro = null)) }
    }

    fun onComunaCambia(valor: String) {
        _uiState.update { it.copy(formReferido = it.formReferido.copy(comuna = valor, errorComuna = null)) }
    }

    fun onContactoCambia(valor: String) {
        _uiState.update { it.copy(formReferido = it.formReferido.copy(contacto = valor, errorAutoriza = null)) }
    }

    fun onAutorizaCambia(valor: Boolean) {
        _uiState.update { it.copy(formReferido = it.formReferido.copy(autoriza = valor, errorAutoriza = null)) }
    }

    fun onEnviarReferido() {
        val estado = _uiState.value
        val form = estado.formReferido
        val conErrores = form.copy(
            errorOrganizacion = if (form.organizacion.isBlank()) "El nombre de la organización es obligatorio" else null,
            errorRubro = if (form.rubro.isBlank()) "El rubro es obligatorio" else null,
            errorComuna = if (form.comuna.isBlank()) "La comuna es obligatoria" else null,
            errorAutoriza = if (form.contacto.isNotBlank() && !form.autoriza) "La persona debe autorizar que la contactemos" else null
        )
        val hayErrores = listOf(conErrores.errorOrganizacion, conErrores.errorRubro, conErrores.errorComuna, conErrores.errorAutoriza)
            .any { it != null }
        if (hayErrores) {
            vibrador.error()
            _uiState.update { it.copy(formReferido = conErrores) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(estadoReferido = EstadoOperacion.Cargando) }
            try {
                val nueva = repositorio.recomendar(form.organizacion, form.rubro, form.comuna, estado.sinConexion)
                guardarDatos()
                vibrador.exito()
                refrescar()
                _uiState.update {
                    it.copy(
                        formReferido = FormReferido(),
                        estadoReferido = EstadoOperacion.Exito("¡Gracias! Recibimos tu recomendación ${nueva.numero}.")
                    )
                }
            } catch (e: IllegalStateException) {
                _uiState.update { it.copy(estadoReferido = EstadoOperacion.Error(e.message ?: "No se pudo enviar")) }
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(estadoReferido = EstadoOperacion.Error(e.message ?: "No se pudo enviar")) }
            }
        }
    }

    fun codigoReferido(): String = "PF-" + (_uiState.value.usuario?.primerNombre()?.uppercase() ?: "AMIGO") + "-2026"

    fun onAvisosActivos(valor: Boolean) {
        _uiState.update { it.copy(config = it.config.copy(activos = valor)) }
        refrescar()
        guardarAjustes()
    }

    fun onDiasAnticipacion(dias: Int) {
        _uiState.update { it.copy(config = it.config.copy(diasAnticipacion = dias)) }
        refrescar()
        guardarAjustes()
    }

    fun onMaximoPorCiclo(maximo: Int) {
        _uiState.update { it.copy(config = it.config.copy(maximoPorCiclo = maximo)) }
        refrescar()
        guardarAjustes()
    }

    fun onRespetarHorario(valor: Boolean) {
        _uiState.update { it.copy(config = it.config.copy(respetarHorario = valor)) }
        guardarAjustes()
    }

    fun onOcultarMontos(valor: Boolean) {
        _uiState.update { it.copy(ocultarMontos = valor) }
        guardarAjustes()
    }

    fun onTextoGrande(valor: Boolean) {
        _uiState.update { it.copy(textoGrande = valor) }
        guardarAjustes()
    }

    fun onAltoContraste(valor: Boolean) {
        _uiState.update { it.copy(altoContraste = valor) }
        guardarAjustes()
    }
}
