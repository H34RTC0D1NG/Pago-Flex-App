package cl.duocuc.miregistro.model

data class PagoFlexUiState(
    val pantalla: Pantalla = Pantalla.BIENVENIDA,
    val pila: List<Pantalla> = emptyList(),

    val rut: String = "",
    val pin: String = "",
    val pinVisible: Boolean = false,
    val errorRut: String? = null,
    val errorPin: String? = null,
    val intentos: Int = 0,
    val bloqueado: Boolean = false,
    val estadoIngreso: EstadoOperacion = EstadoOperacion.Inactivo,

    val usuario: Usuario? = null,
    val compromisos: List<Compromiso> = emptyList(),
    val comprobantes: List<Comprobante> = emptyList(),
    val recomendaciones: List<Recomendacion> = emptyList(),
    val reportes: List<Reporte> = emptyList(),
    val avisos: List<Aviso> = emptyList(),
    val logros: List<Logro> = emptyList(),

    val totalMes: Int = 0,
    val vencidos: Int = 0,
    val proximo: Compromiso? = null,
    val indicador: Double = 0.0,
    val categoria: String = "",
    val mensajeCategoria: String = "",

    val filtroEstado: EstadoCompromiso? = null,
    val filtroEmpresa: String? = null,

    val folioSeleccionado: String? = null,
    val medioPago: MedioPago? = null,
    val errorMedio: String? = null,
    val confirmarPago: Boolean = false,
    val estadoPago: EstadoOperacion = EstadoOperacion.Inactivo,
    val ultimoComprobante: Comprobante? = null,
    val recompensa: String? = null,

    val formReporte: FormReporte = FormReporte(),
    val estadoReporte: EstadoOperacion = EstadoOperacion.Inactivo,

    val formReferido: FormReferido = FormReferido(),
    val estadoReferido: EstadoOperacion = EstadoOperacion.Inactivo,

    val config: ConfigAvisos = ConfigAvisos(),

    val sinConexion: Boolean = false,
    val ultimaSincronizacion: String = "Sin sincronizar",
    val estadoSync: EstadoOperacion = EstadoOperacion.Inactivo,

    val ocultarMontos: Boolean = false,
    val textoGrande: Boolean = false,
    val altoContraste: Boolean = false
) {
    fun compromisoSeleccionado(): Compromiso? = compromisos.find { it.folio == folioSeleccionado }

    fun compromisosFiltrados(): List<Compromiso> =
        if (filtroEstado == null) compromisos else compromisos.filter { it.estadoActual() == filtroEstado }

    fun comprobantesFiltrados(): List<Comprobante> =
        if (filtroEmpresa == null) comprobantes else comprobantes.filter { it.empresa == filtroEmpresa }

    fun empresas(): List<String> = comprobantes.map { it.empresa }.distinct().sorted()

    fun montoVisible(monto: Int): String = if (ocultarMontos) "$ ••••••" else pesos(monto)
}
