package cl.duocuc.miregistro.data

import cl.duocuc.miregistro.model.Aviso
import cl.duocuc.miregistro.model.Comprobante
import cl.duocuc.miregistro.model.Compromiso
import cl.duocuc.miregistro.model.CompromisoAporte
import cl.duocuc.miregistro.model.CompromisoArriendo
import cl.duocuc.miregistro.model.CompromisoCredito
import cl.duocuc.miregistro.model.CompromisoCuotaSocial
import cl.duocuc.miregistro.model.ConfigAvisos
import cl.duocuc.miregistro.model.EstadoCompromiso
import cl.duocuc.miregistro.model.EstadoRecomendacion
import cl.duocuc.miregistro.model.Fecha
import cl.duocuc.miregistro.model.Logro
import cl.duocuc.miregistro.model.MedioPago
import cl.duocuc.miregistro.model.MotivoReporte
import cl.duocuc.miregistro.model.Recomendacion
import cl.duocuc.miregistro.model.Reporte
import cl.duocuc.miregistro.model.Usuario
import kotlinx.coroutines.delay

class RepositorioPagoFlex {

    companion object {
        const val CONSIDERADOS_BASE = 36
        const val A_TIEMPO_BASE = 33
        const val MAXIMO_RECOMENDACIONES = 5
        val FORMATO_RUT = Regex("^[0-9]{1,2}\\.?[0-9]{3}\\.?[0-9]{3}-[0-9kK]$")
    }

    val usuario = Usuario(
        nombre = "Camila Andrea Rojas Peña",
        rut = "15.834.207-3",
        correo = "crojas@correo.ejemplo",
        comuna = "Maipú",
        pinGuardado = "1234".hashCode()
    )

    private val compromisos: MutableList<Compromiso> = mutableListOf(
        CompromisoArriendo("CP-2026-0101", "Plaza Oriente", "Arriendo departamento octubre", 10, 12, 520000, Fecha(10, 10, 2026), EstadoCompromiso.PENDIENTE),
        CompromisoCuotaSocial("CP-2026-0102", "Club Los Halcones", "Cuota social octubre", 10, 12, 15000, Fecha(5, 10, 2026), EstadoCompromiso.PENDIENTE),
        CompromisoAporte("CP-2026-0103", "Manos Abiertas", "Aporte mensual octubre", 0, 0, 10000, Fecha(15, 10, 2026), EstadoCompromiso.PENDIENTE),
        CompromisoCredito("CP-2026-0104", "Crédito Andino", "Crédito de consumo", 7, 24, 86400, Fecha(25, 9, 2026), EstadoCompromiso.PAGADO),
        CompromisoCredito("CP-2026-0105", "Crédito Andino", "Crédito de consumo", 8, 24, 86400, Fecha(25, 10, 2026), EstadoCompromiso.PENDIENTE),
        CompromisoArriendo("CP-2026-0121", "Plaza Oriente", "Arriendo departamento noviembre", 11, 12, 520000, Fecha(10, 11, 2026), EstadoCompromiso.PENDIENTE),
        CompromisoCuotaSocial("CP-2026-0122", "Club Los Halcones", "Cuota social noviembre", 11, 12, 15000, Fecha(5, 11, 2026), EstadoCompromiso.PENDIENTE),
        CompromisoAporte("CP-2026-0123", "Manos Abiertas", "Aporte mensual noviembre", 0, 0, 10000, Fecha(15, 11, 2026), EstadoCompromiso.PENDIENTE),
        CompromisoCredito("CP-2026-0124", "Crédito Andino", "Crédito de consumo", 9, 24, 86400, Fecha(25, 11, 2026), EstadoCompromiso.PENDIENTE),
        CompromisoArriendo("CP-2026-0131", "Plaza Oriente", "Arriendo departamento diciembre", 12, 12, 520000, Fecha(10, 12, 2026), EstadoCompromiso.PENDIENTE),
        CompromisoCuotaSocial("CP-2026-0132", "Club Los Halcones", "Cuota social diciembre", 12, 12, 15000, Fecha(5, 12, 2026), EstadoCompromiso.PENDIENTE),
        CompromisoCredito("CP-2026-0134", "Crédito Andino", "Crédito de consumo", 10, 24, 86400, Fecha(25, 12, 2026), EstadoCompromiso.PENDIENTE)
    )

    private val comprobantes: MutableList<Comprobante> = mutableListOf(
        Comprobante("CPR-000451", "CP-2026-0104", "Crédito Andino", "Crédito de consumo", 86400, Fecha(24, 9, 2026), "10:42", "Portal de pagos", "Tarjeta de débito", true),
        Comprobante("CPR-000405", "CP-2026-0091", "Manos Abiertas", "Aporte mensual septiembre", 10000, Fecha(15, 9, 2026), "08:22", "Agente conversacional", "Pago simulado", true),
        Comprobante("CPR-000398", "CP-2026-0089", "Plaza Oriente", "Arriendo departamento septiembre", 520000, Fecha(9, 9, 2026), "13:05", "Portal de pagos", "Pago simulado", true),
        Comprobante("CPR-000402", "CP-2026-0090", "Club Los Halcones", "Cuota social septiembre", 17000, Fecha(7, 9, 2026), "18:40", "Portal de pagos", "Pago simulado", false)
    )

    private val recomendaciones: MutableList<Recomendacion> = mutableListOf(
        Recomendacion("REC-0001", "Jardín Infantil Pequeños Exploradores", "Educación parvularia", "Maipú", EstadoRecomendacion.SE_UNIO),
        Recomendacion("REC-0002", "Comunidad Edificio Parque Los Olmos", "Administración de condominio", "Maipú", EstadoRecomendacion.CONTACTADA)
    )

    private val reportes: MutableList<Reporte> = mutableListOf()

    private var contadorComprobantes = 453
    private var contadorReportes = 12
    private var contadorRecomendaciones = 6
    private var pagosEnLaApp = 0

    fun compromisos(): List<Compromiso> = compromisos.toList()

    fun comprobantes(): List<Comprobante> = comprobantes.toList()

    fun recomendaciones(): List<Recomendacion> = recomendaciones.toList()

    fun reportes(): List<Reporte> = reportes.toList()

    fun buscar(folio: String): Compromiso? = compromisos.find { it.folio == folio }

    fun delMes(): List<Compromiso> = compromisos.filter {
        it.porPagar() && (it.estadoActual() == EstadoCompromiso.VENCIDO || it.vencimiento.mes == Fecha.HOY.mes)
    }

    fun totalDelMes(): Int = delMes().sumOf { it.total() }

    fun cantidadVencidos(): Int = compromisos.count { it.estadoActual() == EstadoCompromiso.VENCIDO }

    fun proximoVencimiento(): Compromiso? = compromisos
        .filter { it.estadoActual() == EstadoCompromiso.PENDIENTE }
        .minByOrNull { it.vencimiento.valor() }

    fun considerados(): Int = CONSIDERADOS_BASE + comprobantes.size - 4

    fun aTiempo(): Int = A_TIEMPO_BASE + comprobantes.count { it.aTiempo && it.canal == "App" }

    fun indicador(): Double = aTiempo() * 100.0 / considerados()

    fun categoria(): String {
        val valor = indicador()
        return when {
            valor >= 95 -> "Ejemplar"
            valor >= 80 -> "Al día"
            else -> "En camino"
        }
    }

    fun mensajeCategoria(): String = when (categoria()) {
        "Ejemplar" -> "Eres un pagador ejemplar. Gracias por mantener todo al día."
        "Al día" -> "Vas muy bien. Cada pago a tiempo suma a tu historial."
        else -> "Vas en camino. Tu próximo pago a tiempo ya mejora tu indicador."
    }

    fun motivoBloqueo(compromiso: Compromiso): String? {
        if (!compromiso.porPagar()) {
            return when (compromiso.estadoActual()) {
                EstadoCompromiso.PAGADO -> "Este compromiso ya está pagado."
                EstadoCompromiso.EN_REVISION -> "Estamos revisando tu reporte. Mientras tanto no se puede pagar."
                EstadoCompromiso.ANULADO -> "La empresa anuló este compromiso."
                else -> null
            }
        }
        val anterior = compromisos
            .filter { it.empresa == compromiso.empresa && it.porPagar() && it.vencimiento.esAntesDe(compromiso.vencimiento) }
            .minByOrNull { it.vencimiento.valor() }
        return anterior?.let { "Primero paga ${it.textoCuota()} de ${it.empresa} (vence ${it.vencimiento.texto()})." }
    }

    suspend fun sincronizar(sinConexion: Boolean): String {
        delay(1500)
        if (sinConexion) {
            throw IllegalStateException("No hay conexión. Te mostramos tus datos guardados.")
        }
        return Fecha.HOY.texto() + " " + Fecha.horaActual()
    }

    suspend fun pagar(folio: String, medio: MedioPago, sinConexion: Boolean): Comprobante {
        delay(2000)
        if (sinConexion) {
            throw IllegalStateException("No pudimos confirmar el pago. Tu compromiso sigue sin pagar, inténtalo de nuevo.")
        }
        val compromiso = buscar(folio) ?: throw IllegalArgumentException("No encontramos el compromiso $folio")
        val bloqueo = motivoBloqueo(compromiso)
        if (bloqueo != null) {
            throw IllegalArgumentException(bloqueo)
        }
        val aTiempo = !compromiso.vencimiento.esAntesDe(Fecha.HOY)
        contadorComprobantes++
        pagosEnLaApp++
        val comprobante = Comprobante(
            numero = "CPR-000$contadorComprobantes",
            folio = compromiso.folio,
            empresa = compromiso.empresa,
            concepto = compromiso.concepto,
            monto = compromiso.total(),
            fecha = Fecha.HOY,
            hora = Fecha.horaActual().take(5),
            canal = "App",
            medio = medio.etiqueta,
            aTiempo = aTiempo
        )
        comprobantes.add(0, comprobante)
        reemplazar(compromiso.conEstado(EstadoCompromiso.PAGADO))
        return comprobante
    }

    suspend fun reportar(folio: String, motivo: MotivoReporte, comentario: String, sinConexion: Boolean): Reporte {
        delay(1000)
        if (sinConexion) {
            throw IllegalStateException("Sin conexión. Tu reporte no se envió, inténtalo otra vez.")
        }
        val compromiso = buscar(folio) ?: throw IllegalArgumentException("No encontramos el compromiso $folio")
        val existente = reportes.find { it.folio == folio && it.estado != "Resuelto" }
        if (existente != null) {
            throw IllegalArgumentException("Ya tienes el reporte ${existente.numero} abierto para este compromiso.")
        }
        contadorReportes++
        val reporte = Reporte("REP-00$contadorReportes", folio, motivo, comentario.trim(), "Abierto")
        reportes.add(0, reporte)
        reemplazar(compromiso.conEstado(EstadoCompromiso.EN_REVISION))
        return reporte
    }

    suspend fun recomendar(organizacion: String, rubro: String, comuna: String, sinConexion: Boolean): Recomendacion {
        delay(1000)
        if (sinConexion) {
            throw IllegalStateException("Sin conexión. Tu recomendación no se envió.")
        }
        val repetida = recomendaciones.find { it.organizacion.lowercase() == organizacion.trim().lowercase() }
        if (repetida != null) {
            throw IllegalArgumentException("Ya recomendaste a ${repetida.organizacion}. Estado: ${repetida.estado.etiqueta}.")
        }
        if (recomendaciones.size >= MAXIMO_RECOMENDACIONES) {
            throw IllegalArgumentException("Ya hiciste $MAXIMO_RECOMENDACIONES recomendaciones, que es el máximo.")
        }
        contadorRecomendaciones++
        val nueva = Recomendacion("REC-000$contadorRecomendaciones", organizacion.trim(), rubro.trim(), comuna.trim(), EstadoRecomendacion.RECIBIDA)
        recomendaciones.add(0, nueva)
        return nueva
    }

    fun comprobantesDeLaApp(): List<Comprobante> = comprobantes.filter { it.canal == "App" }

    fun recomendacionesNuevas(): List<Recomendacion> =
        recomendaciones.filter { it.numero != "REC-0001" && it.numero != "REC-0002" }

    fun restaurar(guardados: List<Comprobante>, reportesGuardados: List<Reporte>, recomendacionesGuardadas: List<Recomendacion>) {
        guardados.reversed().forEach { comprobante ->
            if (comprobantes.none { it.numero == comprobante.numero }) {
                comprobantes.add(0, comprobante)
                pagosEnLaApp++
                val numero = comprobante.numero.filter { it.isDigit() }.toIntOrNull() ?: 0
                if (numero > contadorComprobantes) contadorComprobantes = numero
                buscar(comprobante.folio)?.let { reemplazar(it.conEstado(EstadoCompromiso.PAGADO)) }
            }
        }
        reportesGuardados.reversed().forEach { reporte ->
            if (reportes.none { it.numero == reporte.numero }) {
                reportes.add(0, reporte)
                val numero = reporte.numero.filter { it.isDigit() }.toIntOrNull() ?: 0
                if (numero > contadorReportes) contadorReportes = numero
                val compromiso = buscar(reporte.folio)
                if (compromiso != null && compromiso.porPagar()) {
                    reemplazar(compromiso.conEstado(EstadoCompromiso.EN_REVISION))
                }
            }
        }
        recomendacionesGuardadas.reversed().forEach { recomendacion ->
            if (recomendaciones.none { it.numero == recomendacion.numero }) {
                recomendaciones.add(0, recomendacion)
                val numero = recomendacion.numero.filter { it.isDigit() }.toIntOrNull() ?: 0
                if (numero > contadorRecomendaciones) contadorRecomendaciones = numero
            }
        }
    }

    private fun reemplazar(nuevo: Compromiso) {
        val indice = compromisos.indexOfFirst { it.folio == nuevo.folio }
        if (indice >= 0) {
            compromisos[indice] = nuevo
        }
    }

    fun avisos(config: ConfigAvisos): List<Aviso> {
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

    fun logros(config: ConfigAvisos): List<Logro> = listOf(
        Logro("Primer pago en la app", "Pagaste un compromiso desde PagoFlex", pagosEnLaApp > 0),
        Logro("Pago puntual", "Pagaste antes del vencimiento", comprobantes.any { it.canal == "App" && it.aTiempo }),
        Logro("Mes al día", "No tienes compromisos vencidos", cantidadVencidos() == 0),
        Logro("Embajador PagoFlex", "Una organización que recomendaste se unió", recomendaciones.any { it.estado == EstadoRecomendacion.SE_UNIO }),
        Logro("Organizado", "Tienes tus recordatorios activos", config.activos),
        Logro("Buen historial", "Tu cumplimiento es 80% o más", indicador() >= 80)
    )

    fun rutValido(rut: String): Boolean {
        if (!FORMATO_RUT.matches(rut.trim())) return false
        val limpio = rut.replace(".", "").replace("-", "").uppercase()
        val cuerpo = limpio.dropLast(1)
        val dv = limpio.last()
        var suma = 0
        var multiplicador = 2
        for (i in cuerpo.length - 1 downTo 0) {
            suma += (cuerpo[i] - '0') * multiplicador
            multiplicador = if (multiplicador == 7) 2 else multiplicador + 1
        }
        val resto = 11 - (suma % 11)
        val esperado = when (resto) {
            11 -> '0'
            10 -> 'K'
            else -> '0' + resto
        }
        return esperado == dv
    }
}
