package com.movil.pagoflex

import com.movil.pagoflex.data.Validador
import com.movil.pagoflex.model.Fecha
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun rutValido() {
        assertTrue(Validador.rutValido("15.834.207-3"))
        assertTrue(Validador.rutValido("11111111-1"))
        assertFalse(Validador.rutValido("15.834.207-4"))
    }

    @Test
    fun pinDeCuatroNumeros() {
        assertEquals(null, Validador.errorPin("1234"))
        assertTrue(Validador.errorPin("12a4") != null)
    }

    @Test
    fun diasEntreFechas() {
        assertEquals(31, Fecha(1, 1, 2026).diasHasta(Fecha(1, 2, 2026)))
        assertEquals(Fecha(5, 3, 2026), Fecha.desdeTexto("05-03-2026"))
    }
}
