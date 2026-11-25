package com.movil.pagoflex.model

import com.movil.pagoflex.DatosPrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelosTest {

    @Test
    fun diasEntreDosFechas() {
        assertEquals(31, Fecha(1, 1, 2026).diasHasta(Fecha(1, 2, 2026)))
        assertEquals(-31, Fecha(1, 2, 2026).diasHasta(Fecha(1, 1, 2026)))
    }

    @Test
    fun leeFechaDesdeTexto() {
        assertEquals(Fecha(5, 3, 2026), Fecha.desdeTexto("05-03-2026"))
        assertEquals(Fecha(5, 3, 2026), Fecha.desdeTexto("5/3/2026"))
        assertNull(Fecha.desdeTexto("31-02-2026"))
        assertNull(Fecha.desdeTexto("hola"))
    }

    @Test
    fun anioBisiesto() {
        assertTrue(Fecha.esBisiesto(2024))
        assertFalse(Fecha.esBisiesto(2026))
        assertEquals(29, Fecha.diasDelMes(2, 2024))
    }

    @Test
    fun fechaEnTextoYOrden() {
        assertEquals("05-03-2026", Fecha(5, 3, 2026).texto())
        assertTrue(Fecha(5, 3, 2026).esAntesDe(Fecha(6, 3, 2026)))
        assertEquals("Marzo 2026", Fecha(5, 3, 2026).nombreMes())
    }

    @Test
    fun compromisoAtrasadoQuedaVencidoConRecargo() {
        val c = DatosPrueba.compromiso(dias = -5)
        assertEquals(EstadoCompromiso.VENCIDO, c.estadoActual())
        assertEquals(5, c.diasAtraso())
        assertEquals(7500, c.recargo())
        assertEquals(427500, c.total())
        assertTrue(c.porPagar())
    }

    @Test
    fun compromisoAlDiaNoTieneRecargo() {
        val c = DatosPrueba.compromiso(dias = 5)
        assertEquals(EstadoCompromiso.PENDIENTE, c.estadoActual())
        assertEquals(0, c.recargo())
        assertEquals(420000, c.total())
        assertEquals(5, c.diasParaVencer())
    }

    @Test
    fun compromisoPagadoNoSePuedePagar() {
        val c = DatosPrueba.compromiso(dias = -5, estado = EstadoCompromiso.PAGADO)
        assertEquals(EstadoCompromiso.PAGADO, c.estadoActual())
        assertFalse(c.porPagar())
        assertEquals(0, c.recargo())
    }

    @Test
    fun textoDeLaCuota() {
        assertEquals("Cuota 3/12", DatosPrueba.compromiso().textoCuota())
        assertEquals("Pago único", DatosPrueba.compromiso(cuota = 0, totalCuotas = 0).textoCuota())
    }

    @Test
    fun formatosDelRut() {
        val u = DatosPrueba.cliente()
        assertEquals("15.834.207-3", u.rutFormateado())
        assertEquals("15.***.***-3", u.rutOculto())
        assertEquals("Ana", u.primerNombre())
        assertFalse(u.esAdmin())
        assertTrue(DatosPrueba.admin().esAdmin())
    }

    @Test
    fun formatoDePesosEIndicadores() {
        assertEquals("$1.234.567", pesos(1234567))
        assertEquals("$ 38.123,45", Indicador("uf", "UF", "Pesos", 38123.45, "").valorTexto())
        assertEquals("0,4%", Indicador("ipc", "IPC", "Porcentaje", 0.4, "").valorTexto())
    }

    @Test
    fun estadoDeLaPantallaFiltraYOculta() {
        val pendiente = DatosPrueba.compromiso(id = 1, dias = 5)
        val pagado = DatosPrueba.compromiso(id = 2, estado = EstadoCompromiso.PAGADO)
        val s = PagoFlexUiState(compromisos = listOf(pendiente, pagado), filtroEstado = EstadoCompromiso.PAGADO, compromisoId = 1)
        assertEquals(listOf(pagado), s.compromisosFiltrados())
        assertEquals(pendiente, s.compromisoSeleccionado())
        assertEquals("$420.000", s.montoVisible(420000))
        assertEquals("$ ••••••", s.copy(ocultarMontos = true).montoVisible(420000))
    }

    @Test
    fun estadoAdminCalculaTotales() {
        val s = AdminUiState(
            usuarios = listOf(DatosPrueba.admin(), DatosPrueba.cliente()),
            compromisos = listOf(DatosPrueba.compromiso(id = 1, dias = 5), DatosPrueba.compromiso(id = 2, estado = EstadoCompromiso.PAGADO)),
            reportes = listOf(DatosPrueba.reporte(1), DatosPrueba.reporte(2, estado = EstadoReporte.RESUELTO))
        )
        assertEquals(1, s.clientes().size)
        assertEquals(420000, s.totalPorCobrar())
        assertEquals(1, s.reportesAbiertos().size)
        assertEquals("Ana Rojas Perez", s.nombreDe(2))
        assertEquals("Persona #9", s.nombreDe(9))
    }
}
