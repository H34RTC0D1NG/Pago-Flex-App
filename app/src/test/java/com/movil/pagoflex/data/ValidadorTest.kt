package com.movil.pagoflex.data

import com.movil.pagoflex.model.FormCompromiso
import com.movil.pagoflex.model.FormReferido
import com.movil.pagoflex.model.FormRegistro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorTest {

    @Test
    fun rutConDigitoVerificador() {
        assertTrue(Validador.rutValido("15.834.207-3"))
        assertTrue(Validador.rutValido("11111111-1"))
        assertFalse(Validador.rutValido("15.834.207-4"))
        assertFalse(Validador.rutValido("abc"))
        assertEquals("15834207-3", Validador.normalizarRut(" 15.834.207-3 "))
    }

    @Test
    fun pinCorreoYTelefono() {
        assertNull(Validador.errorPin("1234"))
        assertNotNull(Validador.errorPin("12a4"))
        assertNotNull(Validador.errorPin("123"))
        assertNull(Validador.errorCorreo("ana@correo.cl"))
        assertNotNull(Validador.errorCorreo("ana@correo"))
        assertNull(Validador.errorTelefono(""))
        assertNotNull(Validador.errorTelefono("123"))
    }

    @Test
    fun pinSeGuardaCifrado() {
        assertEquals(Validador.hashPin("1234"), Validador.hashPin("1234"))
        assertNotEquals(Validador.hashPin("1234"), Validador.hashPin("4321"))
        assertNotEquals("1234", Validador.hashPin("1234"))
    }

    @Test
    fun registroVacioTieneTodosLosErrores() {
        val errores = Validador.validarRegistro(FormRegistro())
        assertTrue(errores.keys.containsAll(listOf("rut", "nombre", "correo", "comuna", "pin", "terminos")))
    }

    @Test
    fun registroConPinDistinto() {
        val form = FormRegistro("15.834.207-3", "Ana Rojas", "ana@correo.cl", "Recoleta", "", "1234", "4321", true)
        val errores = Validador.validarRegistro(form)
        assertEquals(setOf("confirmarPin"), errores.keys)
    }

    @Test
    fun registroCorrectoNoTieneErrores() {
        val form = FormRegistro("15.834.207-3", "Ana Rojas", "ana@correo.cl", "Recoleta", "912345678", "1234", "1234", true)
        assertTrue(Validador.validarRegistro(form).isEmpty())
    }

    @Test
    fun cobroConCuotaFueraDeRango() {
        val form = FormCompromiso(usuarioId = 2, empresa = "Plaza", concepto = "Arriendo", cuota = "13", totalCuotas = "12", monto = "1000", vencimiento = "30-11-2026")
        assertEquals(setOf("cuota"), Validador.validarCompromiso(form).keys)
    }

    @Test
    fun cobroSinDatos() {
        val errores = Validador.validarCompromiso(FormCompromiso(vencimiento = "2026-11-30"))
        assertTrue(errores.keys.containsAll(listOf("usuario", "empresa", "concepto", "monto", "vencimiento")))
    }

    @Test
    fun pagoUnicoEsValido() {
        val form = FormCompromiso(usuarioId = 2, empresa = "Fundacion", concepto = "Aporte", cuota = "0", totalCuotas = "0", monto = "10000", vencimiento = "30-11-2026")
        assertTrue(Validador.validarCompromiso(form).isEmpty())
    }

    @Test
    fun referidoConContactoSinAutorizacion() {
        val form = FormReferido("Jardin", "Jardin infantil", "Recoleta", "912345678", false)
        assertEquals(setOf("autoriza"), Validador.validarReferido(form).keys)
    }
}
