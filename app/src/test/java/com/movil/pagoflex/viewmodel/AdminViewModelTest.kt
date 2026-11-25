package com.movil.pagoflex.viewmodel

import com.movil.pagoflex.DatosPrueba
import com.movil.pagoflex.data.Repositorio
import com.movil.pagoflex.model.AccionReporte
import com.movil.pagoflex.model.EstadoOperacion
import com.movil.pagoflex.model.Rol
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    private val repo = mockk<Repositorio>(relaxed = true)
    private lateinit var vm: AdminViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repo.enLinea } returns true
        coEvery { repo.usuarios() } returns listOf(DatosPrueba.admin(), DatosPrueba.cliente())
        coEvery { repo.todosLosCompromisos() } returns listOf(DatosPrueba.compromiso())
        coEvery { repo.todosLosReportes() } returns listOf(DatosPrueba.reporte())
        vm = AdminViewModel(repo)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    @Test
    fun cargarTodoSincronizaYLlenaLasListas() {
        vm.cargarTodo()
        val s = vm.uiState.value
        assertEquals(2, s.usuarios.size)
        assertEquals(1, s.compromisos.size)
        assertEquals(1, s.reportesAbiertos().size)
        assertFalse(s.sinServidor)
        coVerify { repo.sincronizarTodo() }
    }

    @Test
    fun personaConDatosMalosNoSeGuarda() {
        vm.nuevoUsuario()
        vm.guardarUsuario { }
        assertTrue(vm.uiState.value.formUsuario.errores.isNotEmpty())
        coVerify(exactly = 0) { repo.crearUsuario(any()) }
    }

    @Test
    fun personaCorrectaSeGuardaYVuelve() {
        var volvio = false
        vm.nuevoUsuario()
        vm.onFormUsuario {
            it.copy(rut = "12.345.678-5", nombre = "Pedro Soto", correo = "pedro@correo.cl", comuna = "Recoleta", pin = "1234", rol = Rol.ADMIN)
        }
        vm.guardarUsuario { volvio = true }
        assertTrue(volvio)
        assertTrue(vm.uiState.value.mensaje is EstadoOperacion.Exito)
        coVerify { repo.crearUsuario(match { it.rol == Rol.ADMIN }) }
    }

    @Test
    fun errorAlEliminarCobroSeMuestra() {
        coEvery { repo.eliminarCompromiso(any()) } throws IllegalArgumentException("No se puede eliminar un cobro que ya tiene comprobante")
        vm.eliminarCompromiso(DatosPrueba.compromiso())
        val mensaje = vm.uiState.value.mensaje as EstadoOperacion.Error
        assertTrue(mensaje.mensaje.contains("comprobante"))
    }

    @Test
    fun resolverReporteLlamaAlRepositorio() {
        vm.resolverReporte(3, AccionReporte.ANULAR_COBRO)
        coVerify { repo.resolverReporte(3, AccionReporte.ANULAR_COBRO) }
        assertTrue(vm.uiState.value.mensaje is EstadoOperacion.Exito)
    }

    @Test
    fun nuevoCobroUsaElClienteFiltrado() {
        vm.onFiltroUsuario(2)
        vm.nuevoCompromiso()
        assertEquals(2L, vm.uiState.value.formCompromiso.usuarioId)
    }

    @Test
    fun alternarRolDeUnCliente() {
        vm.alternarRol(DatosPrueba.cliente())
        coVerify { repo.cambiarRol(2, Rol.ADMIN) }
    }
}
