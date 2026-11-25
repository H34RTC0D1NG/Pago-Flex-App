package com.movil.pagoflex.network

import com.google.gson.annotations.SerializedName
import com.movil.pagoflex.model.Indicador
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

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

data class LoginDto(val rut: String, val pin: String)

data class RegistroDto(
    val rut: String,
    val nombre: String,
    val correo: String,
    val comuna: String,
    val telefono: String,
    val pin: String
)

data class UsuarioDto(
    val id: Long?,
    val rut: String?,
    val nombre: String?,
    val correo: String?,
    val comuna: String?,
    val telefono: String?,
    val rol: String?,
    val activo: Boolean?
)

interface IndicadoresApi {
    @GET("api")
    suspend fun obtener(): IndicadoresDto
}

interface UsuariosApi {
    @POST("api/auth/login")
    suspend fun login(@Body datos: LoginDto): UsuarioDto

    @POST("api/auth/registro")
    suspend fun registrar(@Body datos: RegistroDto): UsuarioDto
}

object Api {
    const val URL_USUARIOS = "http://10.0.2.2:8081/"

    val indicadores: IndicadoresApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://mindicador.cl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IndicadoresApi::class.java)
    }

    val usuarios: UsuariosApi by lazy {
        val cliente = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(URL_USUARIOS)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UsuariosApi::class.java)
    }

    suspend fun obtenerIndicadores(): List<Indicador> {
        val r = indicadores.obtener()
        val lista = listOfNotNull(r.uf?.aModelo(), r.dolar?.aModelo(), r.euro?.aModelo(), r.utm?.aModelo(), r.ipc?.aModelo())
        if (lista.isEmpty()) throw IllegalStateException("La API no devolvió datos")
        return lista
    }
}
