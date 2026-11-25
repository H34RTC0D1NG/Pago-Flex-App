package com.movil.pagoflex.data

import com.movil.pagoflex.DatosPrueba
import com.movil.pagoflex.model.ConfigAvisos
import com.movil.pagoflex.model.EstadoCompromiso
import com.movil.pagoflex.model.EstadoRecomendacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasTest {

    @Test
    fun noSePuedePagarUnCobroPagadoAnuladoOEnRevision() {
        assertEquals("Este compromiso ya está pagado.", Reglas.motivoBloqueo(DatosPrueba.compromiso(estado = EstadoCompromiso.PAGADO), emptyList()))
        assertEquals("La empresa anuló este compromiso.", Reglas.motivoBloqueo(DatosPrueba.compromiso(estado = EstadoCompromiso.ANULADO), emptyList()))
        assertTrue(Reglas.motivoBloqueo(DatosPrueba.compromiso(estado = EstadoCompromiso.EN_REVISION), emptyList())!!.contains("revisando"))
    }

    @Test
    fun primeroHayQuePagarLaCuotaAnterior() {
        val anterior = DatosPrueba.compromiso(id = 1, cuota = 2, dias = -3)
        val actual = DatosPrueba.compromiso(id = 2, cuota = 3, dias = 20)
        val motivo = Reglas.motivoBloqueo(actual, listOf(anterior, actual))
        assertTrue(motivo!!.startsWith("Primero paga Cuota 2/12 de Plaza Oriente"))
    }

    @Test
    fun cuotaAnteriorDeOtraEmpresaNoBloquea() {
        val otra = DatosPrueba.compromiso(id = 1, empresa = "Caja Los Andes", dias = -3)
        val actual = DatosPrueba.compromiso(id = 2, dias = 20)
        assertNull(Reglas.motivoBloqueo(actual, listOf(otra, actual)))
    }

    @Test
    fun indicadorDeCumplimiento() {
        assertEquals(100.0, Reglas.indicador(emptyList()), 0.01)
        assertEquals(50.0, Reglas.indicador(listOf(DatosPrueba.comprobante(1, true), DatosPrueba.comprobante(2, false))), 0.01)
    }

    @Test
    fun categoriaSegunIndicador() {
        assertEquals("Ejemplar", Reglas.categoria(96.0))
        assertEquals("Al día", Reglas.categoria(85.0))
        assertEquals("En camino", Reglas.categoria(50.0))
    }

    @Test
    fun delMesIncluyeVencidosYLosDeEsteMes() {
        val vencido = DatosPrueba.compromiso(id = 1, dias = -40)
        val hoy = DatosPrueba.compromiso(id = 2, dias = 0)
        val lejano = DatosPrueba.compromiso(id = 3, dias = 40)
        val pagado = DatosPrueba.compromiso(id = 4, dias = 0, estado = EstadoCompromiso.PAGADO)
        assertEquals(listOf(vencido, hoy), Reglas.delMes(listOf(vencido, hoy, lejano, pagado)))
    }

    @Test
    fun avisosRespetanLaConfiguracion() {
        val lista = listOf(
            DatosPrueba.compromiso(id = 1, dias = 0),
            DatosPrueba.compromiso(id = 2, dias = 2),
            DatosPrueba.compromiso(id = 3, dias = 10)
        )
        assertEquals(2, Reglas.avisos(lista, ConfigAvisos(diasAnticipacion = 3, maximoPorCiclo = 5)).size)
        assertEquals(1, Reglas.avisos(lista, ConfigAvisos(diasAnticipacion = 3, maximoPorCiclo = 1)).size)
        assertTrue(Reglas.avisos(lista, ConfigAvisos(activos = false)).isEmpty())
        assertTrue(Reglas.avisos(lista, ConfigAvisos()).first().mensaje.startsWith("Hoy vence"))
    }

    @Test
    fun logrosSegunLaActividad() {
        val logros = Reglas.logros(
            listOf(DatosPrueba.compromiso(dias = 5)),
            listOf(DatosPrueba.comprobante(aTiempo = true)),
            listOf(DatosPrueba.recomendacion(estado = EstadoRecomendacion.SE_UNIO)),
            ConfigAvisos(activos = false)
        )
        assertEquals(6, logros.size)
        assertTrue(logros.first { it.titulo == "Primer pago en la app" }.obtenido)
        assertTrue(logros.first { it.titulo == "Embajador PagoFlex" }.obtenido)
        assertFalse(logros.first { it.titulo == "Organizado" }.obtenido)
    }

    @Test
    fun conCobrosVencidosNoHayMesAlDia() {
        val logros = Reglas.logros(listOf(DatosPrueba.compromiso(dias = -2)), emptyList(), emptyList(), ConfigAvisos())
        assertFalse(logros.first { it.titulo == "Mes al día" }.obtenido)
    }
}
