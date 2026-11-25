package com.movil.pagoflex.viewmodel

import com.movil.pagoflex.DatosPrueba
import com.movil.pagoflex.data.CredencialesInvalidas
import com.movil.pagoflex.data.Repositorio
import com.movil.pagoflex.model.ConfigAvisos
import com.movil.pagoflex.model.EstadoOperacion
import com.movil.pagoflex.model.MedioPago
import com.movil.pagoflex.model.MotivoReporte
import com.movil.pagoflex.model.Pantalla
import com.movil.pagoflex.nativo.Vibrador
import com.movil.pagoflex.network.RetrofitService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PagoFlexViewModelTest {

    private val repo = mockk<Repositorio>(relaxed = true)
    private val vibrador = mockk<Vibrador>(relaxed = true)
    private lateinit var vm: PagoFlexViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkObject(RetrofitService)
        coEvery { RetrofitService.obtenerIndicadores() } returns emptyList()
        every { repo.leerRut() } returns ""
        every { repo.leerConfig() } returns ConfigAvisos()
        every { repo.enLinea } returns true
        coEvery { repo.compromisos(any()) } returns listOf(DatosPrueba.compromiso(dias = 5))
        coEvery { repo.comprobantes(any()) } returns emptyList()
        coEvery { repo.reportes(any()) } returns emptyList()
        coEvery { repo.recomendaciones(any()) } returns emptyList()
        vm = PagoFlexViewModel(repo, vibrador)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun escribir(rut: String, pin: String) {
        vm.onRutCambia(rut)
        vm.onPinCambia(pin)
    }

    @Test
    fun rutMaloNoLlamaAlRepositorio() {
        escribir("12.345.678-9", "1234")
        vm.onIngresar()
        assertNotNull(vm.uiState.value.errorRut)
        coVerify(exactly = 0) { repo.login(any(), any()) }
        verify { vibrador.error() }
    }

    @Test
    fun pinSoloAceptaCuatroNumeros() {
        vm.onPinCambia("12ab3456")
        assertEquals("1234", vm.uiState.value.pin)
    }

    @Test
    fun clienteEntraAlInicioConSusDatos() {
        coEvery { repo.login(any(), any()) } returns DatosPrueba.cliente()
        escribir("15.834.207-3", "1234")
        vm.onIngresar()
        val s = vm.uiState.value
        assertEquals(Pantalla.INICIO, s.pantalla)
        assertEquals("Ana Rojas Perez", s.usuario?.nombre)
        assertEquals(1, s.compromisos.size)
        assertTrue(s.logros.isNotEmpty())
        verify { repo.guardarRut("15.834.207-3") }
    }

    @Test
    fun adminEntraAlPanel() {
        coEvery { repo.login(any(), any()) } returns DatosPrueba.admin()
        escribir("11.111.111-1", "1234")
        vm.onIngresar()
        assertEquals(Pantalla.ADMIN_INICIO, vm.uiState.value.pantalla)
    }

    @Test
    fun tresIntentosMalosBloqueanElIngreso() {
        coEvery { repo.login(any(), any()) } throws CredencialesInvalidas("RUT o PIN incorrectos")
        repeat(3) {
            escribir("15.834.207-3", "9999")
            vm.onIngresar()
        }
        val s = vm.uiState.value
        assertTrue(s.bloqueado)
        assertTrue((s.estadoIngreso as EstadoOperacion.Error).mensaje.contains("30 segundos"))
    }

    @Test
    fun navegacionConPila() {
        vm.irA(Pantalla.REGISTRO)
        assertEquals(Pantalla.REGISTRO, vm.uiState.value.pantalla)
        vm.volver()
        assertEquals(Pantalla.INGRESO, vm.uiState.value.pantalla)
        assertTrue(vm.uiState.value.pila.isEmpty())
    }

    @Test
    fun registroConErroresNoSeEnvia() {
        vm.irARegistro()
        vm.onRegistrar()
        assertTrue(vm.uiState.value.formRegistro.errores.isNotEmpty())
        coVerify(exactly = 0) { repo.registrar(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun registroCorrectoVuelveAlIngreso() {
        coEvery { repo.registrar(any(), any(), any(), any(), any(), any()) } returns true
        vm.irARegistro()
        vm.onFormRegistro {
            it.copy(rut = "15.834.207-3", nombre = "Ana Rojas", correo = "ana@correo.cl", comuna = "Recoleta",
                pin = "1234", confirmarPin = "1234", aceptaTerminos = true)
        }
        vm.onRegistrar()
        val s = vm.uiState.value
        assertEquals(Pantalla.INGRESO, s.pantalla)
        assertEquals("15.834.207-3", s.rut)
        assertTrue(s.estadoIngreso is EstadoOperacion.Exito)
    }

    @Test
    fun pagarSinElegirMedioMuestraError() {
        vm.verDetalle(1)
        vm.irAPagar()
        vm.onSolicitarPago()
        assertEquals("Elige cómo quieres pagar", vm.uiState.value.errorMedio)
        assertFalse(vm.uiState.value.confirmarPago)
    }

    @Test
    fun pagoConfirmadoMuestraElComprobante() {
        coEvery { repo.pagar(1, MedioPago.DEBITO) } returns DatosPrueba.comprobante(5)
        vm.verDetalle(1)
        vm.irAPagar()
        vm.onMedioSeleccionado(MedioPago.DEBITO)
        vm.onSolicitarPago()
        assertTrue(vm.uiState.value.confirmarPago)
        vm.onConfirmarPago()
        val s = vm.uiState.value
        assertEquals(Pantalla.COMPROBANTE, s.pantalla)
        assertEquals("C-5005", s.comprobanteVisto?.numero)
        assertNotNull(s.recompensa)
    }

    @Test
    fun pagoRechazadoMuestraElMensaje() {
        coEvery { repo.pagar(any(), any()) } throws IllegalArgumentException("Este compromiso ya está pagado.")
        vm.verDetalle(1)
        vm.onMedioSeleccionado(MedioPago.DEBITO)
        vm.onConfirmarPago()
        assertEquals("Este compromiso ya está pagado.", (vm.uiState.value.estadoPago as EstadoOperacion.Error).mensaje)
    }

    @Test
    fun reporteOtroSinComentarioNoSeEnvia() {
        vm.verDetalle(1)
        vm.irAReportar()
        vm.onMotivoReporte(MotivoReporte.OTRO)
        vm.onEnviarReporte()
        assertNotNull(vm.uiState.value.formReporte.error)
        coVerify(exactly = 0) { repo.reportar(any(), any(), any()) }
    }

    @Test
    fun cerrarSesionLimpiaElUsuario() {
        coEvery { repo.login(any(), any()) } returns DatosPrueba.cliente()
        escribir("15.834.207-3", "1234")
        vm.onIngresar()
        vm.cerrarSesion()
        assertNull(vm.uiState.value.usuario)
        assertEquals(Pantalla.INGRESO, vm.uiState.value.pantalla)
    }

    @Test
    fun ocultarMontosSeGuarda() {
        vm.onOcultarMontos(true)
        assertTrue(vm.uiState.value.ocultarMontos)
        verify { repo.guardarAjustes(any(), true, any(), any()) }
    }
}
