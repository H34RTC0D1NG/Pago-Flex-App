package com.movil.pagoflex.model

import java.util.Calendar
import java.util.Locale

data class Fecha(val dia: Int, val mes: Int, val anio: Int) {
    companion object {
        private val DIAS_POR_MES = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val NOMBRES_MESES = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )

        val HOY: Fecha get() = desdeCalendario(Calendar.getInstance())

        fun esBisiesto(anio: Int): Boolean = (anio % 4 == 0 && anio % 100 != 0) || anio % 400 == 0

        fun diasDelMes(mes: Int, anio: Int): Int = if (mes == 2 && esBisiesto(anio)) 29 else DIAS_POR_MES[mes - 1]

        fun desdeCalendario(c: Calendar): Fecha =
            Fecha(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))

        fun enDias(dias: Int): Fecha {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, dias)
            return desdeCalendario(c)
        }

        fun horaActual(): String {
            val c = Calendar.getInstance()
            return String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
        }

        fun desdeTexto(texto: String?): Fecha? {
            val partes = texto?.trim()?.split("-", "/") ?: return null
            if (partes.size != 3) return null
            val dia = partes[0].toIntOrNull()
            val mes = partes[1].toIntOrNull()
            val anio = partes[2].toIntOrNull()
            if (dia == null || mes == null || anio == null) return null
            if (anio < 2000 || mes !in 1..12) return null
            if (dia !in 1..diasDelMes(mes, anio)) return null
            return Fecha(dia, mes, anio)
        }
    }

    fun valor(): Int = anio * 10000 + mes * 100 + dia

    fun esAntesDe(otra: Fecha): Boolean = valor() < otra.valor()

    fun texto(): String = String.format(Locale.US, "%02d-%02d-%d", dia, mes, anio)

    fun nombreMes(): String = NOMBRES_MESES[mes - 1] + " " + anio

    fun aDias(): Int {
        var total = 0
        for (a in 2000 until anio) total += if (esBisiesto(a)) 366 else 365
        for (m in 1 until mes) total += diasDelMes(m, anio)
        return total + dia
    }

    fun diasHasta(otra: Fecha): Int = otra.aDias() - aDias()
}

enum class Pantalla(val titulo: String) {
    INGRESO("Ingresar"),
    REGISTRO("Crear cuenta"),
    INICIO("Mi mes"),
    COMPROMISOS("Mis compromisos"),
    DETALLE("Detalle"),
    PAGO("Pagar"),
    COMPROBANTE("Comprobante"),
    REPORTAR("Reportar problema"),
    HISTORIAL("Historial y logros"),
    INDICADORES("Indicadores económicos"),
    REFERIDOS("Recomendar"),
    PERFIL("Mi perfil"),
    EDITAR_PERFIL("Editar perfil"),
    AJUSTES("Recordatorios y accesibilidad"),
    ADMIN_INICIO("Panel de administración"),
    ADMIN_USUARIOS("Personas"),
    ADMIN_USUARIO_FORM("Agregar persona"),
    ADMIN_COMPROMISOS("Cobros"),
    ADMIN_COMPROMISO_FORM("Cobro"),
    ADMIN_REPORTES("Reportes de clientes")
}

sealed class EstadoOperacion {
    object Inactivo : EstadoOperacion()
    object Cargando : EstadoOperacion()
    data class Exito(val mensaje: String) : EstadoOperacion()
    data class Error(val mensaje: String) : EstadoOperacion()
}

enum class Rol(val etiqueta: String) {
    ADMIN("Administrador"),
    CLIENTE("Cliente")
}

enum class EstadoCompromiso(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    VENCIDO("Vencido"),
    PAGADO("Pagado"),
    EN_REVISION("En revisión"),
    ANULADO("Anulado")
}

enum class TipoCompromiso(val etiqueta: String, val recargo: Int) {
    ARRIENDO("Arriendo", 7500),
    CREDITO("Crédito", 12000),
    CLUB("Club", 2000),
    FUNDACION("Fundación", 0)
}

enum class MedioPago(val etiqueta: String) {
    DEBITO("Tarjeta de débito"),
    CREDITO("Tarjeta de crédito"),
    TRANSFERENCIA("Transferencia")
}

enum class MotivoReporte(val etiqueta: String) {
    YA_PAGUE("Ya pagué por otro medio"),
    NO_RECONOZCO("No reconozco este cobro"),
    MONTO_INCORRECTO("El monto no corresponde"),
    OTRO("Otro")
}

enum class EstadoReporte(val etiqueta: String) {
    ABIERTO("Abierto"),
    RESUELTO("Resuelto"),
    RECHAZADO("Rechazado")
}

enum class AccionReporte(val etiqueta: String) {
    RECHAZAR("Rechazar: el cliente debe pagar"),
    ANULAR_COBRO("Anular el cobro"),
    MARCAR_PAGADO("Marcar como pagado")
}

enum class EstadoRecomendacion(val etiqueta: String, val avance: Float) {
    RECIBIDA("Recibida", 0.25f),
    CONTACTADA("Contactada", 0.5f),
    EN_EVALUACION("En evaluación", 0.75f),
    SE_UNIO("Se unió", 1f),
    DESCARTADA("Descartada", 1f)
}

data class Usuario(
    val id: Long,
    val rut: String,
    val nombre: String,
    val correo: String,
    val comuna: String,
    val telefono: String,
    val rol: Rol,
    val activo: Boolean
) {
    fun rutFormateado(): String {
        val partes = rut.replace(".", "").split("-")
        val cuerpo = partes.first()
        val conPuntos = cuerpo.reversed().chunked(3).joinToString(".").reversed()
        return conPuntos + "-" + partes.getOrElse(1) { "" }
    }

    fun rutOculto(): String {
        val partes = rut.replace(".", "").split("-")
        val cuerpo = partes.first()
        return cuerpo.take(cuerpo.length - 6) + ".***.***-" + partes.getOrElse(1) { "" }
    }

    fun primerNombre(): String = nombre.trim().split(" ").first()

    fun esAdmin(): Boolean = rol == Rol.ADMIN
}

data class Compromiso(
    val id: Long,
    val folio: String,
    val usuarioId: Long,
    val empresa: String,
    val tipo: TipoCompromiso,
    val concepto: String,
    val cuota: Int,
    val totalCuotas: Int,
    val monto: Int,
    val vencimiento: Fecha,
    val estado: EstadoCompromiso
) {
    fun estadoActual(): EstadoCompromiso =
        if (estado == EstadoCompromiso.PENDIENTE && vencimiento.esAntesDe(Fecha.HOY)) EstadoCompromiso.VENCIDO else estado

    fun diasAtraso(): Int = if (estadoActual() == EstadoCompromiso.VENCIDO) vencimiento.diasHasta(Fecha.HOY) else 0

    fun diasParaVencer(): Int = Fecha.HOY.diasHasta(vencimiento)

    fun recargo(): Int = if (estadoActual() == EstadoCompromiso.VENCIDO) tipo.recargo else 0

    fun total(): Int = monto + recargo()

    fun porPagar(): Boolean =
        estadoActual() == EstadoCompromiso.PENDIENTE || estadoActual() == EstadoCompromiso.VENCIDO

    fun textoCuota(): String = if (totalCuotas == 0) "Pago único" else "Cuota $cuota/$totalCuotas"
}

data class Comprobante(
    val id: Long,
    val numero: String,
    val compromisoId: Long,
    val folio: String,
    val usuarioId: Long,
    val empresa: String,
    val concepto: String,
    val monto: Int,
    val fecha: Fecha,
    val hora: String,
    val medio: String,
    val canal: String,
    val aTiempo: Boolean
)

data class Reporte(
    val id: Long,
    val numero: String,
    val compromisoId: Long,
    val folio: String,
    val usuarioId: Long,
    val motivo: MotivoReporte,
    val comentario: String,
    val estado: EstadoReporte,
    val respuesta: String,
    val fecha: String
)

data class Recomendacion(
    val id: Long,
    val numero: String,
    val usuarioId: Long,
    val organizacion: String,
    val rubro: String,
    val comuna: String,
    val contacto: String,
    val estado: EstadoRecomendacion
)

data class Indicador(val codigo: String, val nombre: String, val unidad: String, val valor: Double, val fecha: String) {
    fun valorTexto(): String = when (unidad) {
        "Porcentaje" -> String.format(Locale.US, "%.1f%%", valor).replace(".", ",")
        else -> "$ " + String.format(Locale.US, "%,.2f", valor).replace(",", "#").replace(".", ",").replace("#", ".")
    }
}

data class Logro(val titulo: String, val descripcion: String, val obtenido: Boolean)

data class Aviso(val folio: String, val mensaje: String)

data class ConfigAvisos(
    val activos: Boolean = true,
    val diasAnticipacion: Int = 3,
    val maximoPorCiclo: Int = 2,
    val respetarHorario: Boolean = true
)

data class FormRegistro(
    val rut: String = "",
    val nombre: String = "",
    val correo: String = "",
    val comuna: String = "",
    val telefono: String = "",
    val pin: String = "",
    val confirmarPin: String = "",
    val aceptaTerminos: Boolean = false,
    val errores: Map<String, String> = emptyMap()
)

data class FormPerfil(
    val nombre: String = "",
    val correo: String = "",
    val comuna: String = "",
    val telefono: String = "",
    val nuevoPin: String = "",
    val errores: Map<String, String> = emptyMap()
)

data class FormReporte(
    val motivo: MotivoReporte = MotivoReporte.YA_PAGUE,
    val comentario: String = "",
    val error: String? = null
)

data class FormReferido(
    val organizacion: String = "",
    val rubro: String = "",
    val comuna: String = "",
    val contacto: String = "",
    val autoriza: Boolean = false,
    val errores: Map<String, String> = emptyMap()
)

data class FormUsuario(
    val rut: String = "",
    val nombre: String = "",
    val correo: String = "",
    val comuna: String = "",
    val telefono: String = "",
    val pin: String = "",
    val rol: Rol = Rol.CLIENTE,
    val errores: Map<String, String> = emptyMap()
)

data class FormCompromiso(
    val id: Long? = null,
    val usuarioId: Long? = null,
    val empresa: String = "",
    val tipo: TipoCompromiso = TipoCompromiso.ARRIENDO,
    val concepto: String = "",
    val cuota: String = "1",
    val totalCuotas: String = "12",
    val monto: String = "",
    val vencimiento: String = "",
    val estado: EstadoCompromiso = EstadoCompromiso.PENDIENTE,
    val errores: Map<String, String> = emptyMap()
) {
    fun esEdicion(): Boolean = id != null
}

data class PagoFlexUiState(
    val pantalla: Pantalla = Pantalla.INGRESO,
    val pila: List<Pantalla> = emptyList(),

    val rut: String = "",
    val pin: String = "",
    val pinVisible: Boolean = false,
    val errorRut: String? = null,
    val errorPin: String? = null,
    val intentos: Int = 0,
    val bloqueado: Boolean = false,
    val estadoIngreso: EstadoOperacion = EstadoOperacion.Inactivo,

    val formRegistro: FormRegistro = FormRegistro(),
    val estadoRegistro: EstadoOperacion = EstadoOperacion.Inactivo,

    val usuario: Usuario? = null,
    val compromisos: List<Compromiso> = emptyList(),
    val comprobantes: List<Comprobante> = emptyList(),
    val reportes: List<Reporte> = emptyList(),
    val recomendaciones: List<Recomendacion> = emptyList(),
    val avisos: List<Aviso> = emptyList(),
    val logros: List<Logro> = emptyList(),
    val totalMes: Int = 0,
    val vencidos: Int = 0,
    val indicador: Double = 100.0,
    val categoria: String = "",

    val filtroEstado: EstadoCompromiso? = null,
    val filtroEmpresa: String? = null,

    val compromisoId: Long? = null,
    val medioPago: MedioPago? = null,
    val errorMedio: String? = null,
    val confirmarPago: Boolean = false,
    val estadoPago: EstadoOperacion = EstadoOperacion.Inactivo,
    val comprobanteVisto: Comprobante? = null,
    val recompensa: String? = null,

    val formReporte: FormReporte = FormReporte(),
    val estadoReporte: EstadoOperacion = EstadoOperacion.Inactivo,

    val formReferido: FormReferido = FormReferido(),
    val estadoReferido: EstadoOperacion = EstadoOperacion.Inactivo,

    val formPerfil: FormPerfil = FormPerfil(),
    val estadoPerfil: EstadoOperacion = EstadoOperacion.Inactivo,
    val confirmarEliminarCuenta: Boolean = false,

    val indicadores: List<Indicador> = emptyList(),
    val estadoIndicadores: EstadoOperacion = EstadoOperacion.Inactivo,

    val config: ConfigAvisos = ConfigAvisos(),
    val ocultarMontos: Boolean = false,
    val textoGrande: Boolean = false,
    val altoContraste: Boolean = false,
    val sinServidor: Boolean = false
) {
    fun compromisoSeleccionado(): Compromiso? = compromisos.find { it.id == compromisoId }

    fun compromisosFiltrados(): List<Compromiso> =
        if (filtroEstado == null) compromisos else compromisos.filter { it.estadoActual() == filtroEstado }

    fun comprobantesFiltrados(): List<Comprobante> =
        if (filtroEmpresa == null) comprobantes else comprobantes.filter { it.empresa == filtroEmpresa }

    fun empresas(): List<String> = comprobantes.map { it.empresa }.distinct().sorted()

    fun montoVisible(monto: Int): String = if (ocultarMontos) "$ ••••••" else pesos(monto)

    fun uf(): Indicador? = indicadores.find { it.codigo == "uf" }
}

data class AdminUiState(
    val usuarios: List<Usuario> = emptyList(),
    val compromisos: List<Compromiso> = emptyList(),
    val reportes: List<Reporte> = emptyList(),
    val filtroUsuarioId: Long? = null,
    val mensaje: EstadoOperacion = EstadoOperacion.Inactivo,
    val formUsuario: FormUsuario = FormUsuario(),
    val formCompromiso: FormCompromiso = FormCompromiso(),
    val estadoFormulario: EstadoOperacion = EstadoOperacion.Inactivo,
    val sinServidor: Boolean = false
) {
    fun clientes(): List<Usuario> = usuarios.filter { it.rol == Rol.CLIENTE && it.activo }

    fun nombreDe(usuarioId: Long): String = usuarios.find { it.id == usuarioId }?.nombre ?: "Persona #$usuarioId"

    fun compromisosFiltrados(): List<Compromiso> =
        if (filtroUsuarioId == null) compromisos else compromisos.filter { it.usuarioId == filtroUsuarioId }

    fun reportesAbiertos(): List<Reporte> = reportes.filter { it.estado == EstadoReporte.ABIERTO }

    fun totalPorCobrar(): Int = compromisos.filter { it.porPagar() }.sumOf { it.total() }
}

fun pesos(monto: Int): String = "$" + String.format(Locale.US, "%,d", monto).replace(",", ".")
