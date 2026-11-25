package com.movil.pagoflex.data

import android.content.Context
import android.content.SharedPreferences
import com.movil.pagoflex.model.AccionReporte
import com.movil.pagoflex.model.Aviso
import com.movil.pagoflex.model.Comprobante
import com.movil.pagoflex.model.Compromiso
import com.movil.pagoflex.model.ConfigAvisos
import com.movil.pagoflex.model.EstadoCompromiso
import com.movil.pagoflex.model.EstadoRecomendacion
import com.movil.pagoflex.model.EstadoReporte
import com.movil.pagoflex.model.Fecha
import com.movil.pagoflex.model.FormCompromiso
import com.movil.pagoflex.model.FormUsuario
import com.movil.pagoflex.model.Logro
import com.movil.pagoflex.model.MedioPago
import com.movil.pagoflex.model.MotivoReporte
import com.movil.pagoflex.model.Recomendacion
import com.movil.pagoflex.model.Reporte
import com.movil.pagoflex.model.Rol
import com.movil.pagoflex.model.Usuario
import com.movil.pagoflex.network.ActualizarUsuarioDto
import com.movil.pagoflex.network.RetrofitService
import com.movil.pagoflex.network.CompromisoDto
import com.movil.pagoflex.network.LoginDto
import com.movil.pagoflex.network.NuevaRecomendacionDto
import com.movil.pagoflex.network.NuevoReporteDto
import com.movil.pagoflex.network.NuevoUsuarioDto
import com.movil.pagoflex.network.PagoDto
import com.movil.pagoflex.network.RegistroDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class Repositorio(private val bd: BaseDatos, private val prefs: SharedPreferences) {

    companion object {
        const val MAXIMO_RECOMENDACIONES = 5

        @Volatile
        private var instancia: Repositorio? = null

        fun obtener(context: Context): Repositorio =
            instancia ?: synchronized(this) {
                instancia ?: Repositorio(
                    BaseDatos(context.applicationContext),
                    context.applicationContext.getSharedPreferences("pagoflex_ajustes", Context.MODE_PRIVATE)
                ).also { instancia = it }
            }
    }

    var enLinea = true
        private set

    fun leerRut(): String = prefs.getString("rut", "") ?: ""

    fun guardarRut(rut: String) {
        prefs.edit().putString("rut", rut).apply()
    }

    fun leerConfig(): ConfigAvisos = ConfigAvisos(
        activos = prefs.getBoolean("avisos", true),
        diasAnticipacion = prefs.getInt("dias", 3),
        maximoPorCiclo = prefs.getInt("maximo", 2),
        respetarHorario = prefs.getBoolean("horario", true)
    )

    fun leerOcultarMontos(): Boolean = prefs.getBoolean("ocultar", false)
    fun leerTextoGrande(): Boolean = prefs.getBoolean("texto_grande", false)
    fun leerAltoContraste(): Boolean = prefs.getBoolean("contraste", false)

    fun guardarAjustes(config: ConfigAvisos, ocultar: Boolean, textoGrande: Boolean, contraste: Boolean) {
        prefs.edit()
            .putBoolean("avisos", config.activos)
            .putInt("dias", config.diasAnticipacion)
            .putInt("maximo", config.maximoPorCiclo)
            .putBoolean("horario", config.respetarHorario)
            .putBoolean("ocultar", ocultar)
            .putBoolean("texto_grande", textoGrande)
            .putBoolean("contraste", contraste)
            .apply()
    }

    private suspend fun <T> servidor(bloque: suspend () -> T): T? = try {
        val r = bloque()
        enLinea = true
        r
    } catch (e: HttpException) {
        enLinea = true
        throw IllegalArgumentException(RetrofitService.mensajeDe(e))
    } catch (e: IOException) {
        enLinea = false
        null
    }

    private fun revisar(r: Response<Unit>): Boolean {
        if (!r.isSuccessful) throw HttpException(r)
        return true
    }

    suspend fun sincronizarCliente(usuarioId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val usuario = RetrofitService.usuarios.obtener(usuarioId).aModelo()
            val compromisos = RetrofitService.compromisos.listar(usuarioId).map { it.aModelo() }
            val comprobantes = RetrofitService.compromisos.comprobantes(usuarioId).map { it.aModelo() }
            val reportes = RetrofitService.reportes.listar(usuarioId).map { it.aModelo() }
            val recomendaciones = RetrofitService.referidos.listar(usuarioId).map { it.aModelo() }
            bd.guardarUsuarioDelServidor(usuario, bd.pinHashDe(usuarioId) ?: "")
            bd.reemplazarDatosDe(usuarioId, compromisos, comprobantes, reportes, recomendaciones)
            enLinea = true
            true
        } catch (e: IOException) {
            enLinea = false
            false
        } catch (e: HttpException) {
            false
        }
    }

    suspend fun sincronizarTodo(): Boolean = withContext(Dispatchers.IO) {
        try {
            val usuarios = RetrofitService.usuarios.listar().map { it.aModelo() }
            val compromisos = RetrofitService.compromisos.listar(null).map { it.aModelo() }
            val comprobantes = RetrofitService.compromisos.comprobantes(null).map { it.aModelo() }
            val reportes = RetrofitService.reportes.listar(null).map { it.aModelo() }
            val recomendaciones = RetrofitService.referidos.listar(null).map { it.aModelo() }
            bd.reemplazarTodo(usuarios, compromisos, comprobantes, reportes, recomendaciones)
            enLinea = true
            true
        } catch (e: IOException) {
            enLinea = false
            false
        } catch (e: HttpException) {
            false
        }
    }

    suspend fun login(rut: String, pin: String): Usuario = withContext(Dispatchers.IO) {
        val rutLimpio = Validador.normalizarRut(rut)
        val remoto = try {
            RetrofitService.usuarios.login(LoginDto(rutLimpio, pin)).aModelo().also { enLinea = true }
        } catch (e: HttpException) {
            enLinea = true
            val mensaje = RetrofitService.mensajeDe(e)
            when (e.code()) {
                401 -> throw CredencialesInvalidas(mensaje)
                403 -> throw IllegalStateException(mensaje)
                else -> throw IllegalArgumentException(mensaje)
            }
        } catch (e: IOException) {
            enLinea = false
            null
        }
        if (remoto != null) {
            bd.guardarUsuarioDelServidor(remoto, Validador.hashPin(pin))
            return@withContext remoto
        }
        val usuario = bd.usuarioPorRut(rutLimpio)
        if (usuario == null || bd.pinHashDe(usuario.id) != Validador.hashPin(pin)) {
            throw CredencialesInvalidas("RUT o PIN incorrectos")
        }
        if (!usuario.activo) throw IllegalStateException("Tu cuenta está desactivada. Habla con el administrador.")
        usuario
    }

    suspend fun registrar(
        rut: String, nombre: String, correo: String, comuna: String, telefono: String, pin: String
    ): Boolean = withContext(Dispatchers.IO) {
        val rutLimpio = Validador.normalizarRut(rut)
        val remoto = servidor {
            RetrofitService.usuarios.registrar(RegistroDto(rutLimpio, nombre.trim(), correo.trim(), comuna.trim(), telefono.trim(), pin))
        }
        if (remoto != null) {
            bd.guardarUsuarioDelServidor(remoto.aModelo(), Validador.hashPin(pin))
            return@withContext true
        }
        if (bd.usuarioPorRut(rutLimpio) != null) throw IllegalArgumentException("Ya existe una cuenta con ese RUT")
        if (bd.existeCorreo(correo.trim())) throw IllegalArgumentException("Ese correo ya está registrado")
        val id = bd.insertarUsuario(
            bd.writableDatabase, rutLimpio, nombre.trim(), correo.trim(), comuna.trim(),
            telefono.trim(), Validador.hashPin(pin), Rol.CLIENTE
        )
        if (id < 0) throw IllegalStateException("No se pudo guardar la cuenta")
        false
    }

    suspend fun actualizarPerfil(
        usuarioId: Long, nombre: String, correo: String, comuna: String, telefono: String, nuevoPin: String
    ): Usuario = withContext(Dispatchers.IO) {
        val hash = if (nuevoPin.isBlank()) null else Validador.hashPin(nuevoPin)
        val remoto = servidor {
            RetrofitService.usuarios.actualizar(usuarioId, ActualizarUsuarioDto(nombre.trim(), correo.trim(), comuna.trim(), telefono.trim(), nuevoPin))
        }
        if (remoto != null) {
            bd.guardarUsuarioDelServidor(remoto.aModelo(), hash ?: bd.pinHashDe(usuarioId) ?: "")
        } else {
            if (bd.existeCorreo(correo.trim(), usuarioId)) throw IllegalArgumentException("Ese correo ya lo usa otra persona")
            bd.actualizarUsuario(usuarioId, nombre.trim(), correo.trim(), comuna.trim(), telefono.trim(), hash)
        }
        bd.usuarioPorId(usuarioId) ?: throw IllegalStateException("No encontramos tu cuenta")
    }

    suspend fun eliminarCuenta(usuarioId: Long): Unit = withContext(Dispatchers.IO) {
        servidor { revisar(RetrofitService.usuarios.eliminar(usuarioId)) }
        bd.eliminarUsuario(usuarioId)
    }

    suspend fun compromisos(usuarioId: Long): List<Compromiso> = withContext(Dispatchers.IO) { bd.compromisosDe(usuarioId) }
    suspend fun comprobantes(usuarioId: Long): List<Comprobante> = withContext(Dispatchers.IO) { bd.comprobantesDe(usuarioId) }
    suspend fun reportes(usuarioId: Long): List<Reporte> = withContext(Dispatchers.IO) { bd.reportesDe(usuarioId) }
    suspend fun recomendaciones(usuarioId: Long): List<Recomendacion> = withContext(Dispatchers.IO) { bd.recomendacionesDe(usuarioId) }

    suspend fun pagar(compromisoId: Long, medio: MedioPago): Comprobante = withContext(Dispatchers.IO) {
        val compromiso = bd.compromisoPorId(compromisoId) ?: throw IllegalArgumentException("No encontramos el compromiso")
        Reglas.motivoBloqueo(compromiso, bd.compromisosDe(compromiso.usuarioId))?.let { throw IllegalArgumentException(it) }
        val remoto = servidor { RetrofitService.compromisos.pagar(compromisoId, PagoDto(compromiso.usuarioId, medio.etiqueta)) }
        if (remoto != null) {
            sincronizarCliente(compromiso.usuarioId)
            return@withContext remoto.aModelo()
        }
        val db = bd.writableDatabase
        var id = -1L
        db.beginTransaction()
        try {
            val actual = bd.compromisoPorId(compromisoId) ?: throw IllegalArgumentException("No encontramos el compromiso")
            Reglas.motivoBloqueo(actual, bd.compromisosDe(actual.usuarioId))?.let { throw IllegalArgumentException(it) }
            val aTiempo = !actual.vencimiento.esAntesDe(Fecha.HOY)
            id = bd.insertarComprobante(db, actual, actual.total(), Fecha.HOY, Fecha.horaActual(), medio.etiqueta, "App", aTiempo)
            bd.cambiarEstadoCompromiso(actual.id, EstadoCompromiso.PAGADO)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        bd.comprobantePorId(id) ?: throw IllegalStateException("No se pudo generar el comprobante")
    }

    suspend fun reportar(compromisoId: Long, motivo: MotivoReporte, comentario: String): Reporte = withContext(Dispatchers.IO) {
        val compromiso = bd.compromisoPorId(compromisoId) ?: throw IllegalArgumentException("No encontramos el compromiso")
        if (!compromiso.porPagar()) throw IllegalArgumentException("Solo puedes reportar compromisos pendientes de pago")
        val abierto = bd.reportesDe(compromiso.usuarioId)
            .find { it.compromisoId == compromisoId && it.estado == EstadoReporte.ABIERTO }
        if (abierto != null) throw IllegalArgumentException("Ya tienes el reporte ${abierto.numero} abierto para este cobro")
        val remoto = servidor {
            val r = RetrofitService.reportes.crear(NuevoReporteDto(compromisoId, compromiso.usuarioId, compromiso.folio, motivo.name, comentario.trim()))
            RetrofitService.compromisos.cambiarEstado(compromisoId, RetrofitService.formato(EstadoCompromiso.EN_REVISION.name))
            r
        }
        if (remoto != null) {
            sincronizarCliente(compromiso.usuarioId)
            return@withContext remoto.aModelo()
        }
        val id = bd.insertarReporte(bd.writableDatabase, compromiso, motivo, comentario.trim())
        bd.cambiarEstadoCompromiso(compromiso.id, EstadoCompromiso.EN_REVISION)
        bd.reportePorId(id) ?: throw IllegalStateException("No se pudo guardar el reporte")
    }

    suspend fun recomendar(
        usuarioId: Long, organizacion: String, rubro: String, comuna: String, contacto: String, autoriza: Boolean
    ): Recomendacion = withContext(Dispatchers.IO) {
        if (bd.recomendacionesDe(usuarioId).size >= MAXIMO_RECOMENDACIONES) {
            throw IllegalArgumentException("Ya hiciste $MAXIMO_RECOMENDACIONES recomendaciones, que es el máximo.")
        }
        val remoto = servidor {
            RetrofitService.referidos.crear(NuevaRecomendacionDto(usuarioId, organizacion.trim(), rubro.trim(), comuna.trim(), contacto.trim(), autoriza))
        }
        if (remoto != null) {
            sincronizarCliente(usuarioId)
            return@withContext remoto.aModelo()
        }
        bd.insertarRecomendacion(bd.writableDatabase, usuarioId, organizacion.trim(), rubro.trim(), comuna.trim(), contacto.trim())
        bd.recomendacionesDe(usuarioId).first()
    }

    suspend fun eliminarRecomendacion(id: Long): Unit = withContext(Dispatchers.IO) {
        servidor { revisar(RetrofitService.referidos.eliminar(id)) }
        bd.eliminarRecomendacion(id)
    }

    suspend fun usuarios(): List<Usuario> = withContext(Dispatchers.IO) { bd.usuarios() }
    suspend fun todosLosCompromisos(): List<Compromiso> = withContext(Dispatchers.IO) { bd.todosLosCompromisos() }
    suspend fun todosLosReportes(): List<Reporte> = withContext(Dispatchers.IO) { bd.todosLosReportes() }

    suspend fun crearUsuario(form: FormUsuario): Unit = withContext(Dispatchers.IO) {
        val rutLimpio = Validador.normalizarRut(form.rut)
        val remoto = servidor {
            RetrofitService.usuarios.crear(
                NuevoUsuarioDto(rutLimpio, form.nombre.trim(), form.correo.trim(), form.comuna.trim(), form.telefono.trim(), form.pin, form.rol.name)
            )
        }
        if (remoto != null) {
            bd.guardarUsuarioDelServidor(remoto.aModelo(), Validador.hashPin(form.pin))
            return@withContext
        }
        if (bd.usuarioPorRut(rutLimpio) != null) throw IllegalArgumentException("Ya existe una persona con ese RUT")
        if (bd.existeCorreo(form.correo.trim())) throw IllegalArgumentException("Ese correo ya está registrado")
        bd.insertarUsuario(
            bd.writableDatabase, rutLimpio, form.nombre.trim(), form.correo.trim(), form.comuna.trim(),
            form.telefono.trim(), Validador.hashPin(form.pin), form.rol
        )
    }

    suspend fun cambiarRol(id: Long, rol: Rol): Unit = withContext(Dispatchers.IO) {
        servidor { RetrofitService.usuarios.cambiarRol(id, rol.name) }
        bd.cambiarRol(id, rol)
    }

    suspend fun cambiarActivo(id: Long, activo: Boolean): Unit = withContext(Dispatchers.IO) {
        servidor { RetrofitService.usuarios.cambiarActivo(id, activo) }
        bd.cambiarActivo(id, activo)
    }

    suspend fun eliminarUsuario(id: Long): Unit = withContext(Dispatchers.IO) {
        servidor { revisar(RetrofitService.usuarios.eliminar(id)) }
        bd.eliminarUsuario(id)
    }

    suspend fun guardarCompromiso(form: FormCompromiso): Unit = withContext(Dispatchers.IO) {
        val usuarioId = form.usuarioId ?: throw IllegalArgumentException("Elige a quién se le cobra")
        val cliente = bd.usuarioPorId(usuarioId) ?: throw IllegalArgumentException("La persona no existe")
        if (cliente.rol != Rol.CLIENTE || !cliente.activo) throw IllegalArgumentException("Solo se cobra a clientes activos")
        val vence = Fecha.desdeTexto(form.vencimiento) ?: throw IllegalArgumentException("Fecha no válida")
        val dto = CompromisoDto(
            usuarioId = usuarioId, empresa = form.empresa.trim(), tipo = RetrofitService.formato(form.tipo.name), concepto = form.concepto.trim(),
            cuota = form.cuota.toInt(), totalCuotas = form.totalCuotas.toInt(), monto = form.monto.toInt(),
            vencimiento = RetrofitService.fechaParaServidor(vence), estado = RetrofitService.formato(form.estado.name)
        )
        val id = form.id
        val remoto = servidor { if (id == null) RetrofitService.compromisos.crear(dto) else RetrofitService.compromisos.actualizar(id, dto) }
        if (remoto != null) return@withContext
        if (id == null) {
            bd.insertarCompromiso(
                bd.writableDatabase, usuarioId, form.empresa.trim(), form.tipo, form.concepto.trim(),
                form.cuota.toInt(), form.totalCuotas.toInt(), form.monto.toInt(), vence, form.estado
            )
        } else {
            bd.actualizarCompromiso(
                id, usuarioId, form.empresa.trim(), form.tipo, form.concepto.trim(),
                form.cuota.toInt(), form.totalCuotas.toInt(), form.monto.toInt(), vence, form.estado
            )
        }
    }

    suspend fun eliminarCompromiso(id: Long): Unit = withContext(Dispatchers.IO) {
        if (bd.tieneComprobante(id)) throw IllegalArgumentException("No se puede eliminar un cobro que ya tiene comprobante")
        servidor { revisar(RetrofitService.compromisos.eliminar(id)) }
        bd.eliminarCompromiso(id)
    }

    suspend fun resolverReporte(id: Long, accion: AccionReporte): Unit = withContext(Dispatchers.IO) {
        val reporte = bd.reportePorId(id) ?: throw IllegalArgumentException("No existe el reporte")
        if (reporte.estado != EstadoReporte.ABIERTO) throw IllegalArgumentException("Este reporte ya fue cerrado")
        val estadoCobro = when (accion) {
            AccionReporte.RECHAZAR -> EstadoCompromiso.PENDIENTE
            AccionReporte.ANULAR_COBRO -> EstadoCompromiso.ANULADO
            AccionReporte.MARCAR_PAGADO -> EstadoCompromiso.PAGADO
        }
        val remoto = servidor {
            RetrofitService.reportes.resolver(id, accion.name)
            RetrofitService.compromisos.cambiarEstado(reporte.compromisoId, RetrofitService.formato(estadoCobro.name))
        }
        if (remoto != null) return@withContext
        when (accion) {
            AccionReporte.RECHAZAR -> bd.cerrarReporte(id, EstadoReporte.RECHAZADO, "Revisamos el cobro y está correcto. Debes pagarlo.")
            AccionReporte.ANULAR_COBRO -> bd.cerrarReporte(id, EstadoReporte.RESUELTO, "Tenías razón. Anulamos el cobro.")
            AccionReporte.MARCAR_PAGADO -> bd.cerrarReporte(id, EstadoReporte.RESUELTO, "Confirmamos tu pago. El cobro queda pagado.")
        }
        bd.cambiarEstadoCompromiso(reporte.compromisoId, estadoCobro)
    }
}

class CredencialesInvalidas(mensaje: String) : Exception(mensaje)
