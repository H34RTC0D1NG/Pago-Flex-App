package com.movil.pagoflex.data

import com.movil.pagoflex.model.Aviso
import com.movil.pagoflex.model.Comprobante
import com.movil.pagoflex.model.Compromiso
import com.movil.pagoflex.model.ConfigAvisos
import com.movil.pagoflex.model.EstadoCompromiso
import com.movil.pagoflex.model.EstadoRecomendacion
import com.movil.pagoflex.model.Fecha
import com.movil.pagoflex.model.Logro
import com.movil.pagoflex.model.Recomendacion

object Reglas {

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
}
