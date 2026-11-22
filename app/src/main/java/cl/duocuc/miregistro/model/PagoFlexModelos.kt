package cl.duocuc.miregistro.model

enum class FaseHooked(val etiqueta: String, val descripcion: String) {
    GATILLO("Gatillo", "Te recordamos a tiempo"),
    ACCION("Acción", "Pagas en pocos pasos"),
    RECOMPENSA("Recompensa", "Ves tu avance"),
    INVERSION("Inversión", "Haces crecer tu cuenta"),
    BASE("Cuenta", "Tu sesión y tus datos")
}

enum class Pantalla(val titulo: String, val fase: FaseHooked) {
    BIENVENIDA("PagoFlex", FaseHooked.BASE),
    REGISTRO("Crear cuenta", FaseHooked.BASE),
    INGRESO("Ingresar", FaseHooked.BASE),
    INICIO("Mi mes", FaseHooked.GATILLO),
    RECORDATORIOS("Recordatorios", FaseHooked.GATILLO),
    COMPROMISOS("Mis compromisos", FaseHooked.ACCION),
    DETALLE("Detalle", FaseHooked.ACCION),
    PAGO("Pagar", FaseHooked.ACCION),
    REPORTAR("Reportar problema", FaseHooked.ACCION),
    COMPROBANTE("Comprobante", FaseHooked.RECOMPENSA),
    HISTORIAL("Historial", FaseHooked.RECOMPENSA),
    LOGROS("Mis logros", FaseHooked.RECOMPENSA),
    PROYECCION("Próximos meses", FaseHooked.RECOMPENSA),
    REFERIDOS("Recomendar", FaseHooked.INVERSION),
    PERFIL("Mi perfil", FaseHooked.INVERSION),
    SINCRONIZACION("Sincronización", FaseHooked.BASE),
    ACCESIBILIDAD("Accesibilidad", FaseHooked.BASE)
}

sealed class EstadoOperacion {
    object Inactivo : EstadoOperacion()
    object Cargando : EstadoOperacion()
    data class Exito(val mensaje: String) : EstadoOperacion()
    data class Error(val mensaje: String) : EstadoOperacion()
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

enum class EstadoRecomendacion(val etiqueta: String, val avance: Float) {
    RECIBIDA("Recibida", 0.25f),
    CONTACTADA("Contactada", 0.5f),
    EN_EVALUACION("En evaluación", 0.75f),
    SE_UNIO("Se unió", 1f),
    DESCARTADA("Descartada", 1f)
}

data class Usuario(
    val nombre: String,
    val rut: String,
    val correo: String,
    val comuna: String,
    val pinGuardado: Int
) {
    fun rutOculto(): String {
        val partes = rut.split("-")
        val inicio = partes[0].split(".").first()
        return "$inicio.***.***-${partes.last()}"
    }

    fun primerNombre(): String = nombre.split(" ").first()
}

data class Comprobante(
    val numero: String,
    val folio: String,
    val empresa: String,
    val concepto: String,
    val monto: Int,
    val fecha: Fecha,
    val hora: String,
    val canal: String,
    val medio: String,
    val aTiempo: Boolean
)

data class Recomendacion(
    val numero: String,
    val organizacion: String,
    val rubro: String,
    val comuna: String,
    val estado: EstadoRecomendacion
)

data class Reporte(
    val numero: String,
    val folio: String,
    val motivo: MotivoReporte,
    val comentario: String,
    val estado: String
)

data class Logro(
    val titulo: String,
    val descripcion: String,
    val obtenido: Boolean
)

data class Aviso(
    val folio: String,
    val mensaje: String
)

data class ConfigAvisos(
    val activos: Boolean = true,
    val diasAnticipacion: Int = 3,
    val maximoPorCiclo: Int = 2,
    val respetarHorario: Boolean = true
)

data class FormReferido(
    val organizacion: String = "",
    val rubro: String = "",
    val comuna: String = "",
    val contacto: String = "",
    val autoriza: Boolean = false,
    val errorOrganizacion: String? = null,
    val errorRubro: String? = null,
    val errorComuna: String? = null,
    val errorAutoriza: String? = null
)

data class FormReporte(
    val motivo: MotivoReporte = MotivoReporte.YA_PAGUE,
    val comentario: String = "",
    val errorComentario: String? = null
)

fun pesos(monto: Int): String = "$" + String.format("%,d", monto).replace(",", ".")
