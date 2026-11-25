package com.movil.pagoflex.network

import com.google.gson.Gson
import com.movil.pagoflex.model.EstadoCompromiso
import com.movil.pagoflex.model.EstadoRecomendacion
import com.movil.pagoflex.model.EstadoReporte
import com.movil.pagoflex.model.Fecha
import com.movil.pagoflex.model.MotivoReporte
import com.movil.pagoflex.model.Rol
import com.movil.pagoflex.model.TipoCompromiso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapeosTest {

    @Test
    fun formatoDeEstadosYTipos() {
        assertEquals("En_Revision", RetrofitService.formato("EN_REVISION"))
        assertEquals("Pendiente", RetrofitService.formato("PENDIENTE"))
        assertEquals("Arriendo", RetrofitService.formato("ARRIENDO"))
    }

    @Test
    fun fechasConElServidor() {
        assertEquals(Fecha(30, 11, 2026), RetrofitService.fechaDesdeServidor("2026-11-30"))
        assertEquals("2026-03-05", RetrofitService.fechaParaServidor(Fecha(5, 3, 2026)))
        assertEquals(Fecha.HOY, RetrofitService.fechaDesdeServidor(null))
    }

    @Test
    fun compromisoDelServidorAModelo() {
        val json = "{\"id\":7,\"folio\":\"PF-1007\",\"usuario_ID\":2,\"empresa\":\"Plaza\",\"tipo\":\"Arriendo\",\"concepto\":\"Arriendo\"," +
            "\"cuota\":3,\"total_Cuotas\":12,\"monto\":420000,\"vencimiento\":\"2026-11-30\",\"estado\":\"En_Revision\"}"
        val c = Gson().fromJson(json, CompromisoDto::class.java).aModelo()
        assertEquals(7L, c.id)
        assertEquals(2L, c.usuarioId)
        assertEquals(12, c.totalCuotas)
        assertEquals(TipoCompromiso.ARRIENDO, c.tipo)
        assertEquals(EstadoCompromiso.EN_REVISION, c.estado)
        assertEquals(Fecha(30, 11, 2026), c.vencimiento)
    }

    @Test
    fun pagoSeEnviaConLosNombresDelMicroservicio() {
        val json = Gson().toJson(PagoDto(2, "Transferencia"))
        assertTrue(json.contains("\"Usuario_ID\":2"))
        assertTrue(json.contains("\"Medio\":\"Transferencia\""))
    }

    @Test
    fun usuarioYReporteAModelo() {
        val u = UsuarioDto(1, "11111111-1", "Admin", "a@b.cl", "Santiago", null, "ADMIN", true).aModelo()
        assertEquals(Rol.ADMIN, u.rol)
        assertEquals("", u.telefono)
        val r = ReporteDto(3, "R-303", 1, "PF-1001", 2, "OTRO", "No es mio", "RECHAZADO", "Debes pagarlo", "2026-11-24").aModelo()
        assertEquals(MotivoReporte.OTRO, r.motivo)
        assertEquals(EstadoReporte.RECHAZADO, r.estado)
        assertEquals("24-11-2026", r.fecha)
    }

    @Test
    fun indicadorSinValorSeDescarta() {
        assertNull(IndicadorDto("uf", "UF", "Pesos", "2026-11-24T03:00:00", null).aModelo())
        val uf = IndicadorDto("uf", "UF", "Pesos", "2026-11-24T03:00:00", 38000.0).aModelo()
        assertEquals("2026-11-24", uf?.fecha)
        assertFalse(uf == null)
    }

    @Test
    fun usuarioDelMicroservicioUsuarios() {
        val json = "{\"ID\":2,\"Rut\":\"15834207-3\",\"Nombre\":\"Ana\",\"Correo\":\"ana@correo.cl\"," +
            "\"Comuna\":\"Independencia\",\"Telefono\":\"\",\"Rol\":\"Cliente\",\"Activo\":true}"
        val u = Gson().fromJson(json, UsuarioDto::class.java).aModelo()
        assertEquals(2L, u.id)
        assertEquals("15834207-3", u.rut)
        assertEquals(Rol.CLIENTE, u.rol)
    }

    @Test
    fun loginYReporteSeEnvianConLosNombresDeLosRecords() {
        val login = Gson().toJson(LoginDto("15834207-3", "1234"))
        assertTrue(login.contains("\"Rut\":\"15834207-3\""))
        assertTrue(login.contains("\"Pin\":\"1234\""))
        val reporte = Gson().toJson(NuevoReporteDto(2, 2, "PF-1002", "YA_PAGUE", ""))
        assertTrue(reporte.contains("\"Compromiso_ID\":2"))
        assertTrue(reporte.contains("\"Folio\":\"PF-1002\""))
        val referido = Gson().toJson(NuevaRecomendacionDto(2, "Jardin", "Jardin", "Recoleta", "", false))
        assertTrue(referido.contains("\"Usuario_ID\":2"))
        assertTrue(referido.contains("\"Autoriza\":false"))
    }

    @Test
    fun recomendacionConEstadoDelMicroservicio() {
        val json = "{\"id\":1,\"numero\":\"REC-101\",\"usuario_ID\":2,\"organizacion\":\"Jardin\"," +
            "\"rubro\":\"Jardin\",\"comuna\":\"Recoleta\",\"contacto\":\"\",\"estado\":\"Se_Unio\"}"
        val r = Gson().fromJson(json, RecomendacionDto::class.java).aModelo()
        assertEquals(2L, r.usuarioId)
        assertEquals(EstadoRecomendacion.SE_UNIO, r.estado)
    }
}
