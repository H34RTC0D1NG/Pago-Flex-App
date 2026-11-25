package com.movil.pagoflex

import com.movil.pagoflex.model.Comprobante
import com.movil.pagoflex.model.Compromiso
import com.movil.pagoflex.model.EstadoCompromiso
import com.movil.pagoflex.model.EstadoRecomendacion
import com.movil.pagoflex.model.EstadoReporte
import com.movil.pagoflex.model.Fecha
import com.movil.pagoflex.model.MotivoReporte
import com.movil.pagoflex.model.Recomendacion
import com.movil.pagoflex.model.Reporte
import com.movil.pagoflex.model.Rol
import com.movil.pagoflex.model.TipoCompromiso
import com.movil.pagoflex.model.Usuario

object DatosPrueba {

    fun cliente(id: Long = 2) = Usuario(id, "15834207-3", "Ana Rojas Perez", "ana@correo.cl", "Independencia", "912345678", Rol.CLIENTE, true)

    fun admin() = Usuario(1, "11111111-1", "Administrador PagoFlex", "admin@pagoflex.cl", "Santiago", "", Rol.ADMIN, true)

    fun compromiso(
        id: Long = 1,
        empresa: String = "Plaza Oriente",
        tipo: TipoCompromiso = TipoCompromiso.ARRIENDO,
        cuota: Int = 3,
        totalCuotas: Int = 12,
        monto: Int = 420000,
        dias: Int = 10,
        estado: EstadoCompromiso = EstadoCompromiso.PENDIENTE
    ) = Compromiso(id, "PF-" + (1000 + id), 2, empresa, tipo, "Arriendo depto", cuota, totalCuotas, monto, Fecha.enDias(dias), estado)

    fun comprobante(id: Long = 1, aTiempo: Boolean = true, canal: String = "App") =
        Comprobante(id, "C-" + (5000 + id), 1, "PF-1001", 2, "Plaza Oriente", "Arriendo depto", 420000, Fecha.HOY, "10:15", "Transferencia", canal, aTiempo)

    fun reporte(id: Long = 1, compromisoId: Long = 1, estado: EstadoReporte = EstadoReporte.ABIERTO) =
        Reporte(id, "R-" + (300 + id), compromisoId, "PF-1001", 2, MotivoReporte.YA_PAGUE, "", estado, "", "24-11-2026")

    fun recomendacion(id: Long = 1, estado: EstadoRecomendacion = EstadoRecomendacion.RECIBIDA) =
        Recomendacion(id, "REC-" + (100 + id), 2, "Jardin Los Peques", "Jardin infantil", "Recoleta", "", estado)
}
