package cl.duocuc.miregistro.data

import android.content.Context
import cl.duocuc.miregistro.model.Comprobante
import cl.duocuc.miregistro.model.ConfigAvisos
import cl.duocuc.miregistro.model.EstadoRecomendacion
import cl.duocuc.miregistro.model.Fecha
import cl.duocuc.miregistro.model.MotivoReporte
import cl.duocuc.miregistro.model.Recomendacion
import cl.duocuc.miregistro.model.Reporte

class AlmacenLocal(context: Context) {

    private val prefs = context.getSharedPreferences("pagoflex_datos", Context.MODE_PRIVATE)

    private fun limpiar(texto: String): String = texto.replace(";", ",").replace("\n", " ")

    fun guardarAjustes(config: ConfigAvisos, ocultarMontos: Boolean, textoGrande: Boolean, altoContraste: Boolean) {
        prefs.edit()
            .putBoolean("avisos_activos", config.activos)
            .putInt("dias_anticipacion", config.diasAnticipacion)
            .putInt("maximo_por_ciclo", config.maximoPorCiclo)
            .putBoolean("respetar_horario", config.respetarHorario)
            .putBoolean("ocultar_montos", ocultarMontos)
            .putBoolean("texto_grande", textoGrande)
            .putBoolean("alto_contraste", altoContraste)
            .apply()
    }

    fun leerConfig(): ConfigAvisos = ConfigAvisos(
        activos = prefs.getBoolean("avisos_activos", true),
        diasAnticipacion = prefs.getInt("dias_anticipacion", 3),
        maximoPorCiclo = prefs.getInt("maximo_por_ciclo", 2),
        respetarHorario = prefs.getBoolean("respetar_horario", true)
    )

    fun leerOcultarMontos(): Boolean = prefs.getBoolean("ocultar_montos", false)

    fun leerTextoGrande(): Boolean = prefs.getBoolean("texto_grande", false)

    fun leerAltoContraste(): Boolean = prefs.getBoolean("alto_contraste", false)

    fun guardarRut(rut: String) {
        prefs.edit().putString("rut", rut).apply()
    }

    fun leerRut(): String = prefs.getString("rut", "") ?: ""

    fun guardarUltimaSincronizacion(texto: String) {
        prefs.edit().putString("ultima_sincronizacion", texto).apply()
    }

    fun leerUltimaSincronizacion(): String = prefs.getString("ultima_sincronizacion", "Sin sincronizar") ?: "Sin sincronizar"

    fun guardarComprobantes(lista: List<Comprobante>) {
        val texto = lista.joinToString("\n") {
            listOf(
                it.numero, it.folio, limpiar(it.empresa), limpiar(it.concepto), it.monto.toString(),
                it.fecha.dia.toString(), it.fecha.mes.toString(), it.fecha.anio.toString(),
                it.hora, it.canal, it.medio, it.aTiempo.toString()
            ).joinToString(";")
        }
        prefs.edit().putString("comprobantes", texto).apply()
    }

    fun leerComprobantes(): List<Comprobante> {
        val texto = prefs.getString("comprobantes", "") ?: ""
        if (texto.isBlank()) return emptyList()
        return texto.split("\n").mapNotNull { linea ->
            val p = linea.split(";")
            try {
                Comprobante(
                    numero = p[0], folio = p[1], empresa = p[2], concepto = p[3], monto = p[4].toInt(),
                    fecha = Fecha(p[5].toInt(), p[6].toInt(), p[7].toInt()),
                    hora = p[8], canal = p[9], medio = p[10], aTiempo = p[11].toBoolean()
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun guardarReportes(lista: List<Reporte>) {
        val texto = lista.joinToString("\n") {
            listOf(it.numero, it.folio, it.motivo.name, limpiar(it.comentario), it.estado).joinToString(";")
        }
        prefs.edit().putString("reportes", texto).apply()
    }

    fun leerReportes(): List<Reporte> {
        val texto = prefs.getString("reportes", "") ?: ""
        if (texto.isBlank()) return emptyList()
        return texto.split("\n").mapNotNull { linea ->
            val p = linea.split(";")
            try {
                Reporte(p[0], p[1], MotivoReporte.valueOf(p[2]), p[3], p[4])
            } catch (e: Exception) {
                null
            }
        }
    }

    fun guardarRecomendaciones(lista: List<Recomendacion>) {
        val texto = lista.joinToString("\n") {
            listOf(it.numero, limpiar(it.organizacion), limpiar(it.rubro), limpiar(it.comuna), it.estado.name).joinToString(";")
        }
        prefs.edit().putString("recomendaciones", texto).apply()
    }

    fun leerRecomendaciones(): List<Recomendacion> {
        val texto = prefs.getString("recomendaciones", "") ?: ""
        if (texto.isBlank()) return emptyList()
        return texto.split("\n").mapNotNull { linea ->
            val p = linea.split(";")
            try {
                Recomendacion(p[0], p[1], p[2], p[3], EstadoRecomendacion.valueOf(p[4]))
            } catch (e: Exception) {
                null
            }
        }
    }

    fun borrarTodo() {
        prefs.edit().clear().apply()
    }
}
