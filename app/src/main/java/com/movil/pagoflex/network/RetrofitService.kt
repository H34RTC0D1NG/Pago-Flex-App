package com.movil.pagoflex.network

import com.google.gson.Gson
import com.movil.pagoflex.model.Fecha
import com.movil.pagoflex.model.Indicador
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Locale
import java.util.concurrent.TimeUnit

interface IndicadoresApi {
    @GET("api")
    suspend fun obtener(): IndicadoresDto
}

interface UsuariosApi {
    @POST("Api/Usuario/Login")
    suspend fun login(@Body datos: LoginDto): UsuarioDto

    @POST("Api/Usuario/Registro")
    suspend fun registrar(@Body datos: RegistroDto): UsuarioDto

    @GET("Api/Usuario/Listar")
    suspend fun listar(): List<UsuarioDto>

    @GET("Api/Usuario/Obtener/{id}")
    suspend fun obtener(@Path("id") id: Long): UsuarioDto

    @POST("Api/Usuario/Crear")
    suspend fun crear(@Body datos: NuevoUsuarioDto): UsuarioDto

    @PUT("Api/Usuario/Actualizar/{id}")
    suspend fun actualizar(@Path("id") id: Long, @Body datos: ActualizarUsuarioDto): UsuarioDto

    @PATCH("Api/Usuario/Modificar/{id}/Rol")
    suspend fun cambiarRol(@Path("id") id: Long, @Query("Valor") rol: String): UsuarioDto

    @PATCH("Api/Usuario/Modificar/{id}/Activo")
    suspend fun cambiarActivo(@Path("id") id: Long, @Query("Valor") activo: Boolean): UsuarioDto

    @DELETE("Api/Usuario/Eliminar/{id}")
    suspend fun eliminar(@Path("id") id: Long): Response<Unit>
}

interface CompromisosApi {
    @GET("Api/Compromiso/Listar")
    suspend fun listar(@Query("Usuario_ID") usuarioId: Long?): List<CompromisoDto>

    @POST("Api/Compromiso/Crear")
    suspend fun crear(@Body datos: CompromisoDto): CompromisoDto

    @PUT("Api/Compromiso/Actualizar/{id}")
    suspend fun actualizar(@Path("id") id: Long, @Body datos: CompromisoDto): CompromisoDto

    @PATCH("Api/Compromiso/Modificar/{id}/Estado")
    suspend fun cambiarEstado(@Path("id") id: Long, @Query("Valor") estado: String): CompromisoDto

    @DELETE("Api/Compromiso/Eliminar/{id}")
    suspend fun eliminar(@Path("id") id: Long): Response<Unit>

    @POST("Api/Compromiso/Pagar/{id}")
    suspend fun pagar(@Path("id") id: Long, @Body datos: PagoDto): ComprobanteDto

    @GET("Api/Compromiso/Comprobantes")
    suspend fun comprobantes(@Query("Usuario_ID") usuarioId: Long?): List<ComprobanteDto>
}

interface ReportesApi {
    @GET("Api/Reporte/Listar")
    suspend fun listar(@Query("Usuario_ID") usuarioId: Long?): List<ReporteDto>

    @POST("Api/Reporte/Crear")
    suspend fun crear(@Body datos: NuevoReporteDto): ReporteDto

    @PATCH("Api/Reporte/Resolver/{id}")
    suspend fun resolver(@Path("id") id: Long, @Query("Accion") accion: String): ReporteDto
}

interface ReferidosApi {
    @GET("Api/Recomendaciones/Listar")
    suspend fun listar(@Query("Usuario_ID") usuarioId: Long?): List<RecomendacionDto>

    @POST("Api/Recomendaciones/Crear")
    suspend fun crear(@Body datos: NuevaRecomendacionDto): RecomendacionDto

    @DELETE("Api/Recomendaciones/Eliminar/{id}")
    suspend fun eliminar(@Path("id") id: Long): Response<Unit>
}

object RetrofitService {
    const val SERVIDOR = "http://10.0.2.2"

    private val cliente: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    private fun retrofit(url: String): Retrofit = Retrofit.Builder()
        .baseUrl(url)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val indicadores: IndicadoresApi by lazy { retrofit("https://mindicador.cl/").create(IndicadoresApi::class.java) }
    val usuarios: UsuariosApi by lazy { retrofit("$SERVIDOR:8081/").create(UsuariosApi::class.java) }
    val compromisos: CompromisosApi by lazy { retrofit("$SERVIDOR:8082/").create(CompromisosApi::class.java) }
    val reportes: ReportesApi by lazy { retrofit("$SERVIDOR:8083/").create(ReportesApi::class.java) }
    val referidos: ReferidosApi by lazy { retrofit("$SERVIDOR:8084/").create(ReferidosApi::class.java) }

    suspend fun obtenerIndicadores(): List<Indicador> {
        val r = indicadores.obtener()
        val lista = listOfNotNull(r.uf?.aModelo(), r.dolar?.aModelo(), r.euro?.aModelo(), r.utm?.aModelo(), r.ipc?.aModelo())
        if (lista.isEmpty()) throw IllegalStateException("La API no devolvió datos")
        return lista
    }

    fun fechaDesdeServidor(texto: String?): Fecha {
        val partes = texto?.take(10)?.split("-") ?: return Fecha.HOY
        if (partes.size != 3) return Fecha.HOY
        val anio = partes[0].toIntOrNull() ?: return Fecha.HOY
        val mes = partes[1].toIntOrNull() ?: return Fecha.HOY
        val dia = partes[2].toIntOrNull() ?: return Fecha.HOY
        return Fecha(dia, mes, anio)
    }

    fun formato(nombre: String): String =
        nombre.lowercase().split("_").joinToString("_") { it.replaceFirstChar { c -> c.uppercase() } }

    fun fechaParaServidor(fecha: Fecha): String =
        String.format(Locale.US, "%04d-%02d-%02d", fecha.anio, fecha.mes, fecha.dia)

    fun mensajeDe(e: HttpException): String {
        val cuerpo = try {
            e.response()?.errorBody()?.string()
        } catch (ex: Exception) {
            null
        }
        val mensaje = try {
            Gson().fromJson(cuerpo, ErrorDto::class.java)?.message
        } catch (ex: Exception) {
            null
        }
        return mensaje ?: "El servidor respondió con error ${e.code()}"
    }
}
