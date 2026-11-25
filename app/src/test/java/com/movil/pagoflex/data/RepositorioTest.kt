package com.movil.pagoflex.data

import android.content.SharedPreferences
import com.movil.pagoflex.DatosPrueba
import com.movil.pagoflex.model.AccionReporte
import com.movil.pagoflex.model.EstadoCompromiso
import com.movil.pagoflex.model.MedioPago
import com.movil.pagoflex.model.MotivoReporte
import com.movil.pagoflex.network.ComprobanteDto
import com.movil.pagoflex.network.CompromisosApi
import com.movil.pagoflex.network.PagoDto
import com.movil.pagoflex.network.ReferidosApi
import com.movil.pagoflex.network.ReportesApi
import com.movil.pagoflex.network.RetrofitService
import com.movil.pagoflex.network.UsuarioDto
import com.movil.pagoflex.network.UsuariosApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class RepositorioTest {

    private val bd = mockk<BaseDatos>(relaxed = true)
    private val prefs = mockk<SharedPreferences>(relaxed = true)
    private val usuariosApi = mockk<UsuariosApi>()
    private val compromisosApi = mockk<CompromisosApi>()
    private val reportesApi = mockk<ReportesApi>()
    private val referidosApi = mockk<ReferidosApi>()
    private lateinit var repo: Repositorio

    private val anaDto = UsuarioDto(2, "15834207-3", "Ana Rojas Perez", "ana@correo.cl", "Independencia", "", "CLIENTE", true)

    @Before
    fun preparar() {
        mockkObject(RetrofitService)
        every { RetrofitService.usuarios } returns usuariosApi
        every { RetrofitService.compromisos } returns compromisosApi
        every { RetrofitService.reportes } returns reportesApi
        every { RetrofitService.referidos } returns referidosApi
        coEvery { usuariosApi.obtener(any()) } throws IOException("sin red")
        repo = Repositorio(bd, prefs)
    }

    @After
    fun limpiar() {
        unmockkAll()
    }

    private fun error401(): HttpException =
        HttpException(Response.error<UsuarioDto>(401, ResponseBody.create(null, "{\"message\":\"RUT o PIN incorrectos\"}")))

    @Test
    fun loginConServidorGuardaElUsuarioEnElTelefono() = runTest {
        coEvery { usuariosApi.login(any()) } returns anaDto
        val usuario = repo.login("15.834.207-3", "1234")
        assertEquals(2L, usuario.id)
        assertTrue(repo.enLinea)
        verify { bd.guardarUsuarioDelServidor(match { it.rut == "15834207-3" }, Validador.hashPin("1234")) }
    }

    @Test
    fun loginConPinMaloEnElServidor() = runTest {
        coEvery { usuariosApi.login(any()) } throws error401()
        val error = runCatching { repo.login("15.834.207-3", "9999") }.exceptionOrNull()
        assertTrue(error is CredencialesInvalidas)
        assertEquals("RUT o PIN incorrectos", error?.message)
    }

    @Test
    fun loginSinServidorUsaLaBaseLocal() = runTest {
        coEvery { usuariosApi.login(any()) } throws IOException("sin red")
        every { bd.usuarioPorRut("15834207-3") } returns DatosPrueba.cliente()
        every { bd.pinHashDe(2) } returns Validador.hashPin("1234")
        val usuario = repo.login("15.834.207-3", "1234")
        assertEquals("Ana Rojas Perez", usuario.nombre)
        assertFalse(repo.enLinea)
    }

    @Test
    fun loginSinServidorConPinMalo() = runTest {
        coEvery { usuariosApi.login(any()) } throws IOException("sin red")
        every { bd.usuarioPorRut(any()) } returns DatosPrueba.cliente()
        every { bd.pinHashDe(2) } returns Validador.hashPin("1234")
        val error = runCatching { repo.login("15.834.207-3", "0000") }.exceptionOrNull()
        assertTrue(error is CredencialesInvalidas)
    }

    @Test
    fun registroConServidorDevuelveTrue() = runTest {
        coEvery { usuariosApi.registrar(any()) } returns anaDto
        assertTrue(repo.registrar("15.834.207-3", "Ana", "ana@correo.cl", "Recoleta", "", "1234"))
    }

    @Test
    fun registroSinServidorSeGuardaLocal() = runTest {
        coEvery { usuariosApi.registrar(any()) } throws IOException("sin red")
        every { bd.usuarioPorRut(any()) } returns null
        every { bd.existeCorreo(any(), any()) } returns false
        every { bd.insertarUsuario(any(), any(), any(), any(), any(), any(), any(), any()) } returns 7L
        assertFalse(repo.registrar("15.834.207-3", "Ana", "ana@correo.cl", "Recoleta", "", "1234"))
    }

    @Test
    fun registroSinServidorConRutRepetido() = runTest {
        coEvery { usuariosApi.registrar(any()) } throws IOException("sin red")
        every { bd.usuarioPorRut(any()) } returns DatosPrueba.cliente()
        val error = runCatching { repo.registrar("15.834.207-3", "Ana", "ana@correo.cl", "Recoleta", "", "1234") }.exceptionOrNull()
        assertEquals("Ya existe una cuenta con ese RUT", error?.message)
    }

    @Test
    fun pagarConServidorMandaElMedioDePago() = runTest {
        val cobro = DatosPrueba.compromiso(dias = 5)
        every { bd.compromisoPorId(1) } returns cobro
        every { bd.compromisosDe(2) } returns listOf(cobro)
        coEvery { compromisosApi.pagar(1, any()) } returns
            ComprobanteDto(9, "C-5009", 1, "PF-1001", 2, "Plaza Oriente", "Arriendo depto", 420000, "2026-11-24", "10:00", "Transferencia", "App", true)
        val comprobante = repo.pagar(1, MedioPago.TRANSFERENCIA)
        assertEquals("C-5009", comprobante.numero)
        coVerify { compromisosApi.pagar(1, PagoDto(2, "Transferencia")) }
    }

    @Test
    fun noSePuedePagarUnCobroYaPagado() = runTest {
        val cobro = DatosPrueba.compromiso(estado = EstadoCompromiso.PAGADO)
        every { bd.compromisoPorId(1) } returns cobro
        every { bd.compromisosDe(2) } returns listOf(cobro)
        val error = runCatching { repo.pagar(1, MedioPago.DEBITO) }.exceptionOrNull()
        assertEquals("Este compromiso ya está pagado.", error?.message)
        coVerify(exactly = 0) { compromisosApi.pagar(any(), any()) }
    }

    @Test
    fun pagarSinServidorGuardaElComprobanteLocal() = runTest {
        val cobro = DatosPrueba.compromiso(dias = 5)
        every { bd.compromisoPorId(1) } returns cobro
        every { bd.compromisosDe(2) } returns listOf(cobro)
        coEvery { compromisosApi.pagar(any(), any()) } throws IOException("sin red")
        every { bd.insertarComprobante(any(), any(), any(), any(), any(), any(), any(), any()) } returns 3L
        every { bd.comprobantePorId(3) } returns DatosPrueba.comprobante(3)
        val comprobante = repo.pagar(1, MedioPago.DEBITO)
        assertEquals("C-5003", comprobante.numero)
        verify { bd.cambiarEstadoCompromiso(1, EstadoCompromiso.PAGADO) }
    }

    @Test
    fun noSePuedeReportarDosVecesElMismoCobro() = runTest {
        every { bd.compromisoPorId(1) } returns DatosPrueba.compromiso(dias = 5)
        every { bd.reportesDe(2) } returns listOf(DatosPrueba.reporte(compromisoId = 1))
        val error = runCatching { repo.reportar(1, MotivoReporte.YA_PAGUE, "") }.exceptionOrNull()
        assertTrue(error!!.message!!.contains("abierto"))
    }

    @Test
    fun maximoCincoRecomendaciones() = runTest {
        every { bd.recomendacionesDe(2) } returns (1L..5L).map { DatosPrueba.recomendacion(it) }
        val error = runCatching { repo.recomendar(2, "Club", "Deporte", "Lampa", "", false) }.exceptionOrNull()
        assertTrue(error!!.message!!.contains("máximo"))
    }

    @Test
    fun resolverReporteCambiaElEstadoDelCobroEnCompromisos() = runTest {
        every { bd.reportePorId(4) } returns DatosPrueba.reporte(id = 4, compromisoId = 1)
        coEvery { reportesApi.resolver(4, "ANULAR_COBRO") } returns mockk(relaxed = true)
        coEvery { compromisosApi.cambiarEstado(1, "Anulado") } returns mockk(relaxed = true)
        repo.resolverReporte(4, AccionReporte.ANULAR_COBRO)
        coVerify { compromisosApi.cambiarEstado(1, "Anulado") }
        verify(exactly = 0) { bd.cerrarReporte(any(), any(), any()) }
    }
}
