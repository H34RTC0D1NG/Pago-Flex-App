package com.movil.pagoflex.data

import android.content.Context
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
import com.movil.pagoflex.network.Api
import com.movil.pagoflex.network.LoginDto
import com.movil.pagoflex.network.RegistroDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Repositorio(context: Context) {

    companion object {
        const val MAXIMO_RECOMENDACIONES = 5

        @Volatile
        private var instancia: Repositorio? = null

        fun obtener(context: Context): Repositorio =
            instancia ?: synchronized(this) {
                instancia ?: Repositorio(context.applicationContext).also { instancia = it }
            }
    }

    private val bd = BaseDatos(context)
    private val prefs = context.getSharedPreferences("pagoflex_ajustes", Context.MODE_PRIVATE)

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

    suspend fun login(rut: String, pin: String): Usuario = withContext(Dispatchers.IO) {
        val rutLimpio = Validador.normalizarRut(rut)
        var usuario = bd.usuarioPorRut(rutLimpio)
        if (usuario == null) {
            val remoto = try {
                Api.usuarios.login(LoginDto(rutLimpio, pin))
            } catch (e: Exception) {
                null
            }
            if (remoto != null) {
                bd.insertarUsuario(
                    bd.writableDatabase, rutLimpio, remoto.nombre ?: "Sin nombre", remoto.correo ?: "",
                    remoto.comuna ?: "", remoto.telefono ?: "", Validador.hashPin(pin),
                    if (remoto.rol == "ADMIN") Rol.ADMIN else Rol.CLIENTE
                )
                usuario = bd.usuarioPorRut(rutLimpio)
            }
        }
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
        if (bd.usuarioPorRut(rutLimpio) != null) throw IllegalArgumentException("Ya existe una cuenta con ese RUT")
        if (bd.existeCorreo(correo.trim())) throw IllegalArgumentException("Ese correo ya está registrado")
        val id = bd.insertarUsuario(
            bd.writableDatabase, rutLimpio, nombre.trim(), correo.trim(), comuna.trim(),
            telefono.trim(), Validador.hashPin(pin), Rol.CLIENTE
        )
        if (id < 0) throw IllegalStateException("No se pudo guardar la cuenta")
        try {
            Api.usuarios.registrar(RegistroDto(rutLimpio, nombre.trim(), correo.trim(), comuna.trim(), telefono.trim(), pin))
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun actualizarPerfil(
        usuarioId: Long, nombre: String, correo: String, comuna: String, telefono: String, nuevoPin: String
    ): Usuario = withContext(Dispatchers.IO) {
        if (bd.existeCorreo(correo.trim(), usuarioId)) throw IllegalArgumentException("Ese correo ya lo usa otra persona")
        val hash = if (nuevoPin.isBlank()) null else Validador.hashPin(nuevoPin)
        bd.actualizarUsuario(usuarioId, nombre.trim(), correo.trim(), comuna.trim(), telefono.trim(), hash)
        bd.usuarioPorId(usuarioId) ?: throw IllegalStateException("No encontramos tu cuenta")
    }

    suspend fun eliminarCuenta(usuarioId: Long) = withContext(Dispatchers.IO) {
        bd.eliminarUsuario(usuarioId)
    }

    suspend fun compromisos(usuarioId: Long): List<Compromiso> = withContext(Dispatchers.IO) { bd.compromisosDe(usuarioId) }
    suspend fun comprobantes(usuarioId: Long): List<Comprobante> = withContext(Dispatchers.IO) { bd.comprobantesDe(usuarioId) }
    suspend fun reportes(usuarioId: Long): List<Reporte> = withContext(Dispatchers.IO) { bd.reportesDe(usuarioId) }
    suspend fun recomendaciones(usuarioId: Long): List<Recomendacion> = withContext(Dispatchers.IO) { bd.recomendacionesDe(usuarioId) }

    fun motivoBloqueo(compromiso: Compromiso, todos: List<Compromiso>): String? {
        if (!compromiso.porPagar()) {
            return when (compromiso.estadoActual()) {
                EstadoCompromiso.PAGADO -> "Este compromiso ya está pagado."
                EstadoCompromiso.EN_REVISION -> "Estamos revisando tu reporte. Mientras tanto no se puede pagar."
                EstadoCompromiso.ANULADO -> "La empresa anuló este compromiso."
                else -> null
            }
        }
        val anterior = todos
            .filter { it.empresa == compromiso.empresa && it.porPagar() && it.vencimiento.esAntesDe(compromiso.vencimiento) }
            .minByOrNull { it.vencimiento.valor() }
        return anterior?.let { "Primero paga ${it.textoCuota()} de ${it.empresa} (vence ${it.vencimiento.texto()})." }
    }

    suspend fun pagar(compromisoId: Long, medio: MedioPago): Comprobante = withContext(Dispatchers.IO) {
        val compromiso = bd.compromisoPorId(compromisoId) ?: throw IllegalArgumentException("No encontramos el compromiso")
        val db = bd.writableDatabase
        var id = -1L
        db.beginTransaction()
        try {
            val actual = bd.compromisoPorId(compromisoId) ?: throw IllegalArgumentException("No encontramos el compromiso")
            motivoBloqueo(actual, bd.compromisosDe(actual.usuarioId))?.let { throw IllegalArgumentException(it) }
            val aTiempo = !compromiso.vencimiento.esAntesDe(Fecha.HOY)
            id = bd.insertarComprobante(db, compromiso, compromiso.total(), Fecha.HOY, Fecha.horaActual(), medio.etiqueta, "App", aTiempo)
            bd.cambiarEstadoCompromiso(compromiso.id, EstadoCompromiso.PAGADO)
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
        val id = bd.insertarReporte(bd.writableDatabase, compromiso, motivo, comentario.trim())
        bd.cambiarEstadoCompromiso(compromiso.id, EstadoCompromiso.EN_REVISION)
        bd.reportePorId(id) ?: throw IllegalStateException("No se pudo guardar el reporte")
    }

    suspend fun recomendar(
        usuarioId: Long, organizacion: String, rubro: String, comuna: String, contacto: String
    ): Recomendacion = withContext(Dispatchers.IO) {
        if (bd.recomendacionesDe(usuarioId).size >= MAXIMO_RECOMENDACIONES) {
            throw IllegalArgumentException("Ya hiciste $MAXIMO_RECOMENDACIONES recomendaciones, que es el máximo.")
        }
        bd.insertarRecomendacion(bd.writableDatabase, usuarioId, organizacion.trim(), rubro.trim(), comuna.trim(), contacto.trim())
        bd.recomendacionesDe(usuarioId).first()
    }

    suspend fun eliminarRecomendacion(id: Long) = withContext(Dispatchers.IO) { bd.eliminarRecomendacion(id) }

    fun delMes(compromisos: List<Compromiso>): List<Compromiso> = compromisos.filter {
        it.porPagar() && (it.estadoActual() == EstadoCompromiso.VENCIDO ||
            (it.vencimiento.mes == Fecha.HOY.mes && it.vencimiento.anio == Fecha.HOY.anio))
    }

    fun indicador(comprobantes: List<Comprobante>): Double =
        if (comprobantes.isEmpty()) 100.0 else comprobantes.count { it.aTiempo } * 100.0 / comprobantes.size

    fun categoria(indicador: Double): String = when {
        indicador >= 95 -> "Ejemplar"
        indicador >= 80 -> "Al día"
        else -> "En camino"
    }

    fun avisos(compromisos: List<Compromiso>, config: ConfigAvisos): List<Aviso> {
        if (!config.activos) return emptyList()
        return compromisos
            .filter { it.porPagar() && it.diasParaVencer() <= config.diasAnticipacion }
            .sortedBy { it.vencimiento.valor() }
            .take(config.maximoPorCiclo)
            .map {
                val dias = it.diasParaVencer()
                val mensaje = when {
                    dias < 0 -> "Te falta ${it.concepto.lowercase()} de ${it.empresa} para quedar al día."
                    dias == 0 -> "Hoy vence ${it.concepto.lowercase()} de ${it.empresa}. Es el último día sin recargo."
                    else -> "En $dias días vence ${it.concepto.lowercase()} de ${it.empresa}. ¡Vas bien!"
                }
                Aviso(it.folio, mensaje)
            }
    }

    fun logros(
        compromisos: List<Compromiso>, comprobantes: List<Comprobante>,
        recomendaciones: List<Recomendacion>, config: ConfigAvisos
    ): List<Logro> = listOf(
        Logro("Primer pago en la app", "Pagaste un compromiso desde PagoFlex", comprobantes.any { it.canal == "App" }),
        Logro("Pago puntual", "Pagaste antes del vencimiento", comprobantes.any { it.canal == "App" && it.aTiempo }),
        Logro("Mes al día", "No tienes compromisos vencidos", compromisos.none { it.estadoActual() == EstadoCompromiso.VENCIDO }),
        Logro("Embajador PagoFlex", "Una organización que recomendaste se unió", recomendaciones.any { it.estado == EstadoRecomendacion.SE_UNIO }),
        Logro("Organizado", "Tienes tus recordatorios activos", config.activos),
        Logro("Buen historial", "Tu cumplimiento es 80% o más", indicador(comprobantes) >= 80)
    )

    suspend fun usuarios(): List<Usuario> = withContext(Dispatchers.IO) { bd.usuarios() }
    suspend fun todosLosCompromisos(): List<Compromiso> = withContext(Dispatchers.IO) { bd.todosLosCompromisos() }
    suspend fun todosLosReportes(): List<Reporte> = withContext(Dispatchers.IO) { bd.todosLosReportes() }

    suspend fun crearUsuario(form: FormUsuario) = withContext(Dispatchers.IO) {
        val rutLimpio = Validador.normalizarRut(form.rut)
        if (bd.usuarioPorRut(rutLimpio) != null) throw IllegalArgumentException("Ya existe una persona con ese RUT")
        if (bd.existeCorreo(form.correo.trim())) throw IllegalArgumentException("Ese correo ya está registrado")
        bd.insertarUsuario(
            bd.writableDatabase, rutLimpio, form.nombre.trim(), form.correo.trim(), form.comuna.trim(),
            form.telefono.trim(), Validador.hashPin(form.pin), form.rol
        )
    }

    suspend fun cambiarRol(id: Long, rol: Rol) = withContext(Dispatchers.IO) { bd.cambiarRol(id, rol) }
    suspend fun cambiarActivo(id: Long, activo: Boolean) = withContext(Dispatchers.IO) { bd.cambiarActivo(id, activo) }
    suspend fun eliminarUsuario(id: Long) = withContext(Dispatchers.IO) { bd.eliminarUsuario(id) }

    suspend fun guardarCompromiso(form: FormCompromiso) = withContext(Dispatchers.IO) {
        val usuarioId = form.usuarioId ?: throw IllegalArgumentException("Elige a quién se le cobra")
        val cliente = bd.usuarioPorId(usuarioId) ?: throw IllegalArgumentException("La persona no existe")
        if (cliente.rol != Rol.CLIENTE || !cliente.activo) throw IllegalArgumentException("Solo se cobra a clientes activos")
        val vence = Fecha.desdeTexto(form.vencimiento) ?: throw IllegalArgumentException("Fecha no válida")
        val id = form.id
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

    suspend fun eliminarCompromiso(id: Long) = withContext(Dispatchers.IO) {
        if (bd.tieneComprobante(id)) throw IllegalArgumentException("No se puede eliminar un cobro que ya tiene comprobante")
        bd.eliminarCompromiso(id)
    }

    suspend fun resolverReporte(id: Long, accion: AccionReporte) = withContext(Dispatchers.IO) {
        val reporte = bd.reportePorId(id) ?: throw IllegalArgumentException("No existe el reporte")
        if (reporte.estado != EstadoReporte.ABIERTO) throw IllegalArgumentException("Este reporte ya fue cerrado")
        when (accion) {
            AccionReporte.RECHAZAR -> {
                bd.cerrarReporte(id, EstadoReporte.RECHAZADO, "Revisamos el cobro y está correcto. Debes pagarlo.")
                bd.cambiarEstadoCompromiso(reporte.compromisoId, EstadoCompromiso.PENDIENTE)
            }
            AccionReporte.ANULAR_COBRO -> {
                bd.cerrarReporte(id, EstadoReporte.RESUELTO, "Tenías razón. Anulamos el cobro.")
                bd.cambiarEstadoCompromiso(reporte.compromisoId, EstadoCompromiso.ANULADO)
            }
            AccionReporte.MARCAR_PAGADO -> {
                bd.cerrarReporte(id, EstadoReporte.RESUELTO, "Confirmamos tu pago. El cobro queda pagado.")
                bd.cambiarEstadoCompromiso(reporte.compromisoId, EstadoCompromiso.PAGADO)
            }
        }
    }
}

class CredencialesInvalidas(mensaje: String) : Exception(mensaje)
