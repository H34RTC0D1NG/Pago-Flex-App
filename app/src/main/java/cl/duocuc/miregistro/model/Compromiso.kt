package cl.duocuc.miregistro.model

enum class EstadoCompromiso(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    VENCIDO("Vencido"),
    PAGADO("Pagado"),
    EN_REVISION("En revisión"),
    ANULADO("Anulado")
}

abstract class Compromiso(
    val folio: String,
    val empresa: String,
    val concepto: String,
    val cuota: Int,
    val totalCuotas: Int,
    val monto: Int,
    val vencimiento: Fecha,
    val estado: EstadoCompromiso
) {
    abstract fun recargoEmpresa(): Int
    abstract fun tipo(): String
    abstract fun conEstado(nuevo: EstadoCompromiso): Compromiso

    fun estadoActual(): EstadoCompromiso =
        if (estado == EstadoCompromiso.PENDIENTE && vencimiento.esAntesDe(Fecha.HOY)) EstadoCompromiso.VENCIDO
        else estado

    fun diasAtraso(): Int =
        if (estadoActual() == EstadoCompromiso.VENCIDO) vencimiento.diasHasta(Fecha.HOY) else 0

    fun diasParaVencer(): Int = Fecha.HOY.diasHasta(vencimiento)

    fun recargo(): Int = if (estadoActual() == EstadoCompromiso.VENCIDO) recargoEmpresa() else 0

    fun total(): Int = monto + recargo()

    fun porPagar(): Boolean =
        estadoActual() == EstadoCompromiso.PENDIENTE || estadoActual() == EstadoCompromiso.VENCIDO

    fun textoCuota(): String = if (totalCuotas == 0) "Pago único" else "Cuota $cuota/$totalCuotas"

    override fun toString(): String = "$folio - $empresa - ${estadoActual().etiqueta}"
}

class CompromisoArriendo(
    folio: String, empresa: String, concepto: String, cuota: Int, totalCuotas: Int,
    monto: Int, vencimiento: Fecha, estado: EstadoCompromiso
) : Compromiso(folio, empresa, concepto, cuota, totalCuotas, monto, vencimiento, estado) {
    override fun recargoEmpresa(): Int = 7500
    override fun tipo(): String = "Arriendo"
    override fun conEstado(nuevo: EstadoCompromiso): Compromiso =
        CompromisoArriendo(folio, empresa, concepto, cuota, totalCuotas, monto, vencimiento, nuevo)
}

class CompromisoCredito(
    folio: String, empresa: String, concepto: String, cuota: Int, totalCuotas: Int,
    monto: Int, vencimiento: Fecha, estado: EstadoCompromiso
) : Compromiso(folio, empresa, concepto, cuota, totalCuotas, monto, vencimiento, estado) {
    override fun recargoEmpresa(): Int = 12000
    override fun tipo(): String = "Crédito"
    override fun conEstado(nuevo: EstadoCompromiso): Compromiso =
        CompromisoCredito(folio, empresa, concepto, cuota, totalCuotas, monto, vencimiento, nuevo)
}

class CompromisoCuotaSocial(
    folio: String, empresa: String, concepto: String, cuota: Int, totalCuotas: Int,
    monto: Int, vencimiento: Fecha, estado: EstadoCompromiso
) : Compromiso(folio, empresa, concepto, cuota, totalCuotas, monto, vencimiento, estado) {
    override fun recargoEmpresa(): Int = 2000
    override fun tipo(): String = "Club"
    override fun conEstado(nuevo: EstadoCompromiso): Compromiso =
        CompromisoCuotaSocial(folio, empresa, concepto, cuota, totalCuotas, monto, vencimiento, nuevo)
}

class CompromisoAporte(
    folio: String, empresa: String, concepto: String, cuota: Int, totalCuotas: Int,
    monto: Int, vencimiento: Fecha, estado: EstadoCompromiso
) : Compromiso(folio, empresa, concepto, cuota, totalCuotas, monto, vencimiento, estado) {
    override fun recargoEmpresa(): Int = 0
    override fun tipo(): String = "Fundación"
    override fun conEstado(nuevo: EstadoCompromiso): Compromiso =
        CompromisoAporte(folio, empresa, concepto, cuota, totalCuotas, monto, vencimiento, nuevo)
}
