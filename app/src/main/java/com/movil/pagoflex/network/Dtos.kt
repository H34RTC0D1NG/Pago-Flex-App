package com.movil.pagoflex.network

import com.google.gson.annotations.SerializedName
import com.movil.pagoflex.model.Comprobante
import com.movil.pagoflex.model.Compromiso
import com.movil.pagoflex.model.EstadoCompromiso
import com.movil.pagoflex.model.EstadoRecomendacion
import com.movil.pagoflex.model.EstadoReporte
import com.movil.pagoflex.model.Fecha
import com.movil.pagoflex.model.Indicador
import com.movil.pagoflex.model.MotivoReporte
import com.movil.pagoflex.model.Recomendacion
import com.movil.pagoflex.model.Reporte
import com.movil.pagoflex.model.Rol
import com.movil.pagoflex.model.TipoCompromiso
import com.movil.pagoflex.model.Usuario

data class IndicadorDto(
    val codigo: String?,
    val nombre: String?,
    @SerializedName("unidad_medida") val unidadMedida: String?,
    val fecha: String?,
    val valor: Double?
) {
    fun aModelo(): Indicador? {
        if (codigo == null || valor == null) return null
        return Indicador(codigo, nombre ?: codigo, unidadMedida ?: "", valor, fecha?.take(10) ?: "")
    }
}

data class IndicadoresDto(
    val uf: IndicadorDto?,
    val dolar: IndicadorDto?,
    val euro: IndicadorDto?,
    val utm: IndicadorDto?,
    val ipc: IndicadorDto?
)

data class LoginDto(
    @SerializedName("Rut") val rut: String,
    @SerializedName("Pin") val pin: String
)

data class RegistroDto(
    @SerializedName("Rut") val rut: String,
    @SerializedName("Nombre") val nombre: String,
    @SerializedName("Correo") val correo: String,
    @SerializedName("Comuna") val comuna: String,
    @SerializedName("Telefono") val telefono: String,
    @SerializedName("Pin") val pin: String
)

data class NuevoUsuarioDto(
    @SerializedName("Rut") val rut: String,
    @SerializedName("Nombre") val nombre: String,
    @SerializedName("Correo") val correo: String,
    @SerializedName("Comuna") val comuna: String,
    @SerializedName("Telefono") val telefono: String,
    @SerializedName("Pin") val pin: String,
    @SerializedName("Rol") val rol: String
)

data class ActualizarUsuarioDto(
    @SerializedName("Nombre") val nombre: String,
    @SerializedName("Correo") val correo: String,
    @SerializedName("Comuna") val comuna: String,
    @SerializedName("Telefono") val telefono: String,
    @SerializedName("Pin") val pin: String
)

data class UsuarioDto(
    @SerializedName(value = "ID", alternate = ["id", "Id"]) val id: Long?,
    @SerializedName(value = "Rut", alternate = ["rut"]) val rut: String?,
    @SerializedName(value = "Nombre", alternate = ["nombre"]) val nombre: String?,
    @SerializedName(value = "Correo", alternate = ["correo"]) val correo: String?,
    @SerializedName(value = "Comuna", alternate = ["comuna"]) val comuna: String?,
    @SerializedName(value = "Telefono", alternate = ["telefono"]) val telefono: String?,
    @SerializedName(value = "Rol", alternate = ["rol"]) val rol: String?,
    @SerializedName(value = "Activo", alternate = ["activo"]) val activo: Boolean?
) {
    fun aModelo(): Usuario = Usuario(
        id = id ?: 0, rut = rut ?: "", nombre = nombre ?: "", correo = correo ?: "", comuna = comuna ?: "",
        telefono = telefono ?: "", rol = if (rol.equals("ADMIN", ignoreCase = true)) Rol.ADMIN else Rol.CLIENTE, activo = activo ?: true
    )
}

data class CompromisoDto(
    @SerializedName(value = "id", alternate = ["ID", "Id"]) val id: Long? = null,
    @SerializedName(value = "folio", alternate = ["Folio"]) val folio: String? = null,
    @SerializedName(value = "usuario_ID", alternate = ["usuarioId", "Usuario_ID"]) val usuarioId: Long?,
    @SerializedName(value = "empresa", alternate = ["Empresa"]) val empresa: String?,
    @SerializedName(value = "tipo", alternate = ["Tipo"]) val tipo: String?,
    @SerializedName(value = "concepto", alternate = ["Concepto"]) val concepto: String?,
    @SerializedName(value = "cuota", alternate = ["Cuota"]) val cuota: Int?,
    @SerializedName(value = "total_Cuotas", alternate = ["totalCuotas", "Total_Cuotas"]) val totalCuotas: Int?,
    @SerializedName(value = "monto", alternate = ["Monto"]) val monto: Int?,
    @SerializedName(value = "vencimiento", alternate = ["Vencimiento"]) val vencimiento: String?,
    @SerializedName(value = "estado", alternate = ["Estado"]) val estado: String?
) {
    fun aModelo(): Compromiso = Compromiso(
        id = id ?: 0, folio = folio ?: "", usuarioId = usuarioId ?: 0, empresa = empresa ?: "",
        tipo = TipoCompromiso.entries.find { it.name.equals(tipo, ignoreCase = true) } ?: TipoCompromiso.ARRIENDO,
        concepto = concepto ?: "", cuota = cuota ?: 0, totalCuotas = totalCuotas ?: 0, monto = monto ?: 0,
        vencimiento = RetrofitService.fechaDesdeServidor(vencimiento),
        estado = EstadoCompromiso.entries.find { it.name.equals(estado, ignoreCase = true) } ?: EstadoCompromiso.PENDIENTE
    )
}

data class PagoDto(
    @SerializedName("Usuario_ID") val usuarioId: Long,
    @SerializedName("Medio") val medio: String
)

data class ComprobanteDto(
    @SerializedName(value = "id", alternate = ["ID", "Id"]) val id: Long?,
    @SerializedName(value = "numero", alternate = ["Numero"]) val numero: String?,
    @SerializedName(value = "compromiso_ID", alternate = ["compromisoId", "Compromiso_ID"]) val compromisoId: Long?,
    @SerializedName(value = "folio", alternate = ["Folio"]) val folio: String?,
    @SerializedName(value = "usuario_ID", alternate = ["usuarioId", "Usuario_ID"]) val usuarioId: Long?,
    @SerializedName(value = "empresa", alternate = ["Empresa"]) val empresa: String?,
    @SerializedName(value = "concepto", alternate = ["Concepto"]) val concepto: String?,
    @SerializedName(value = "monto", alternate = ["Monto"]) val monto: Int?,
    @SerializedName(value = "fecha", alternate = ["Fecha"]) val fecha: String?,
    @SerializedName(value = "hora", alternate = ["Hora"]) val hora: String?,
    @SerializedName(value = "medio", alternate = ["Medio"]) val medio: String?,
    @SerializedName(value = "canal", alternate = ["Canal"]) val canal: String?,
    @SerializedName(value = "puntual", alternate = ["Puntual", "aTiempo", "a_Tiempo"]) val puntual: Boolean?
) {
    fun aModelo(): Comprobante = Comprobante(
        id = id ?: 0, numero = numero ?: "", compromisoId = compromisoId ?: 0, folio = folio ?: "",
        usuarioId = usuarioId ?: 0, empresa = empresa ?: "", concepto = concepto ?: "", monto = monto ?: 0,
        fecha = RetrofitService.fechaDesdeServidor(fecha), hora = hora ?: "", medio = medio ?: "", canal = canal ?: "App",
        aTiempo = puntual ?: true
    )
}

data class NuevoReporteDto(
    @SerializedName("Compromiso_ID") val compromisoId: Long,
    @SerializedName("Usuario_ID") val usuarioId: Long,
    @SerializedName("Folio") val folio: String,
    @SerializedName("Motivo") val motivo: String,
    @SerializedName("Comentario") val comentario: String
)

data class ReporteDto(
    @SerializedName(value = "id", alternate = ["ID", "Id"]) val id: Long?,
    @SerializedName(value = "numero", alternate = ["Numero"]) val numero: String?,
    @SerializedName(value = "compromiso_ID", alternate = ["compromisoId", "Compromiso_ID"]) val compromisoId: Long?,
    @SerializedName(value = "folio", alternate = ["Folio"]) val folio: String?,
    @SerializedName(value = "usuario_ID", alternate = ["usuarioId", "Usuario_ID"]) val usuarioId: Long?,
    @SerializedName(value = "motivo", alternate = ["Motivo"]) val motivo: String?,
    @SerializedName(value = "comentario", alternate = ["Comentario"]) val comentario: String?,
    @SerializedName(value = "estado", alternate = ["Estado"]) val estado: String?,
    @SerializedName(value = "respuesta", alternate = ["Respuesta"]) val respuesta: String?,
    @SerializedName(value = "fecha", alternate = ["Fecha"]) val fecha: String?
) {
    fun aModelo(): Reporte = Reporte(
        id = id ?: 0, numero = numero ?: "", compromisoId = compromisoId ?: 0, folio = folio ?: "",
        usuarioId = usuarioId ?: 0,
        motivo = MotivoReporte.entries.find { it.name.equals(motivo, ignoreCase = true) } ?: MotivoReporte.OTRO,
        comentario = comentario ?: "",
        estado = EstadoReporte.entries.find { it.name.equals(estado, ignoreCase = true) } ?: EstadoReporte.ABIERTO,
        respuesta = respuesta ?: "", fecha = RetrofitService.fechaDesdeServidor(fecha).texto()
    )
}

data class NuevaRecomendacionDto(
    @SerializedName("Usuario_ID") val usuarioId: Long,
    @SerializedName("Organizacion") val organizacion: String,
    @SerializedName("Rubro") val rubro: String,
    @SerializedName("Comuna") val comuna: String,
    @SerializedName("Contacto") val contacto: String,
    @SerializedName("Autoriza") val autoriza: Boolean
)

data class RecomendacionDto(
    @SerializedName(value = "id", alternate = ["ID", "Id"]) val id: Long?,
    @SerializedName(value = "numero", alternate = ["Numero"]) val numero: String?,
    @SerializedName(value = "usuario_ID", alternate = ["usuarioId", "Usuario_ID"]) val usuarioId: Long?,
    @SerializedName(value = "organizacion", alternate = ["Organizacion"]) val organizacion: String?,
    @SerializedName(value = "rubro", alternate = ["Rubro"]) val rubro: String?,
    @SerializedName(value = "comuna", alternate = ["Comuna"]) val comuna: String?,
    @SerializedName(value = "contacto", alternate = ["Contacto"]) val contacto: String?,
    @SerializedName(value = "estado", alternate = ["Estado"]) val estado: String?
) {
    fun aModelo(): Recomendacion = Recomendacion(
        id = id ?: 0, numero = numero ?: "", usuarioId = usuarioId ?: 0, organizacion = organizacion ?: "",
        rubro = rubro ?: "", comuna = comuna ?: "", contacto = contacto ?: "",
        estado = EstadoRecomendacion.entries.find { it.name.equals(estado, ignoreCase = true) } ?: EstadoRecomendacion.RECIBIDA
    )
}

data class ErrorDto(val message: String?)
