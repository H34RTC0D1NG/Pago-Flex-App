package com.movil.pagoflex.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
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

class BaseDatos(private val context: Context) : SQLiteOpenHelper(context, "pagoflex.db", null, 2) {

    companion object {
        const val ARCHIVO_SQL = "BaseDatos.sql"

        fun sentencias(texto: String): List<String> =
            texto.split(";").map { it.trim() }.filter { it.isNotEmpty() }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val texto = context.assets.open(ARCHIVO_SQL).bufferedReader(Charsets.UTF_8).use { it.readText() }
        db.beginTransaction()
        try {
            sentencias(texto).forEach { db.execSQL(it) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS recomendaciones")
        db.execSQL("DROP TABLE IF EXISTS reportes")
        db.execSQL("DROP TABLE IF EXISTS comprobantes")
        db.execSQL("DROP TABLE IF EXISTS compromisos")
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        onCreate(db)
    }

    private fun siguienteId(db: SQLiteDatabase, tabla: String): Long {
        db.rawQuery("SELECT IFNULL(MAX(id), 0) + 1 FROM $tabla", null).use {
            it.moveToFirst()
            return it.getLong(0)
        }
    }

    fun insertarUsuario(
        db: SQLiteDatabase, rut: String, nombre: String, correo: String, comuna: String,
        telefono: String, pinHash: String, rol: Rol
    ): Long {
        val valores = ContentValues()
        valores.put("rut", rut)
        valores.put("nombre", nombre)
        valores.put("correo", correo)
        valores.put("comuna", comuna)
        valores.put("telefono", telefono)
        valores.put("pin_hash", pinHash)
        valores.put("rol", rol.name)
        valores.put("activo", 1)
        return db.insert("usuarios", null, valores)
    }

    fun insertarCompromiso(
        db: SQLiteDatabase, usuarioId: Long, empresa: String, tipo: TipoCompromiso, concepto: String,
        cuota: Int, totalCuotas: Int, monto: Int, vencimiento: Fecha, estado: EstadoCompromiso
    ): Long {
        val valores = ContentValues()
        valores.put("folio", "PF-" + (1000 + siguienteId(db, "compromisos")))
        valores.put("usuario_id", usuarioId)
        valores.put("empresa", empresa)
        valores.put("tipo", tipo.name)
        valores.put("concepto", concepto)
        valores.put("cuota", cuota)
        valores.put("total_cuotas", totalCuotas)
        valores.put("monto", monto)
        valores.put("vencimiento", vencimiento.texto())
        valores.put("estado", estado.name)
        return db.insert("compromisos", null, valores)
    }

    fun insertarComprobante(
        db: SQLiteDatabase, c: Compromiso, monto: Int, fecha: Fecha, hora: String,
        medio: String, canal: String, aTiempo: Boolean
    ): Long {
        val valores = ContentValues()
        valores.put("numero", "C-" + (5000 + siguienteId(db, "comprobantes")))
        valores.put("compromiso_id", c.id)
        valores.put("folio", c.folio)
        valores.put("usuario_id", c.usuarioId)
        valores.put("empresa", c.empresa)
        valores.put("concepto", c.concepto + " · " + c.textoCuota())
        valores.put("monto", monto)
        valores.put("fecha", fecha.texto())
        valores.put("hora", hora)
        valores.put("medio", medio)
        valores.put("canal", canal)
        valores.put("a_tiempo", if (aTiempo) 1 else 0)
        return db.insert("comprobantes", null, valores)
    }

    fun insertarReporte(db: SQLiteDatabase, c: Compromiso, motivo: MotivoReporte, comentario: String): Long {
        val valores = ContentValues()
        valores.put("numero", "R-" + (300 + siguienteId(db, "reportes")))
        valores.put("compromiso_id", c.id)
        valores.put("folio", c.folio)
        valores.put("usuario_id", c.usuarioId)
        valores.put("motivo", motivo.name)
        valores.put("comentario", comentario)
        valores.put("estado", EstadoReporte.ABIERTO.name)
        valores.put("respuesta", "")
        valores.put("fecha", Fecha.HOY.texto())
        return db.insert("reportes", null, valores)
    }

    fun insertarRecomendacion(
        db: SQLiteDatabase, usuarioId: Long, organizacion: String, rubro: String, comuna: String, contacto: String
    ): Long {
        val valores = ContentValues()
        valores.put("numero", "REC-" + (100 + siguienteId(db, "recomendaciones")))
        valores.put("usuario_id", usuarioId)
        valores.put("organizacion", organizacion)
        valores.put("rubro", rubro)
        valores.put("comuna", comuna)
        valores.put("contacto", contacto)
        valores.put("estado", EstadoRecomendacion.RECIBIDA.name)
        return db.insert("recomendaciones", null, valores)
    }

    private fun aUsuario(c: Cursor): Usuario = Usuario(
        id = c.getLong(c.getColumnIndexOrThrow("id")),
        rut = c.getString(c.getColumnIndexOrThrow("rut")),
        nombre = c.getString(c.getColumnIndexOrThrow("nombre")),
        correo = c.getString(c.getColumnIndexOrThrow("correo")),
        comuna = c.getString(c.getColumnIndexOrThrow("comuna")),
        telefono = c.getString(c.getColumnIndexOrThrow("telefono")) ?: "",
        rol = Rol.valueOf(c.getString(c.getColumnIndexOrThrow("rol"))),
        activo = c.getInt(c.getColumnIndexOrThrow("activo")) == 1
    )

    private fun aCompromiso(c: Cursor): Compromiso = Compromiso(
        id = c.getLong(c.getColumnIndexOrThrow("id")),
        folio = c.getString(c.getColumnIndexOrThrow("folio")),
        usuarioId = c.getLong(c.getColumnIndexOrThrow("usuario_id")),
        empresa = c.getString(c.getColumnIndexOrThrow("empresa")),
        tipo = TipoCompromiso.valueOf(c.getString(c.getColumnIndexOrThrow("tipo"))),
        concepto = c.getString(c.getColumnIndexOrThrow("concepto")),
        cuota = c.getInt(c.getColumnIndexOrThrow("cuota")),
        totalCuotas = c.getInt(c.getColumnIndexOrThrow("total_cuotas")),
        monto = c.getInt(c.getColumnIndexOrThrow("monto")),
        vencimiento = Fecha.desdeTexto(c.getString(c.getColumnIndexOrThrow("vencimiento"))) ?: Fecha.HOY,
        estado = EstadoCompromiso.valueOf(c.getString(c.getColumnIndexOrThrow("estado")))
    )

    private fun aComprobante(c: Cursor): Comprobante = Comprobante(
        id = c.getLong(c.getColumnIndexOrThrow("id")),
        numero = c.getString(c.getColumnIndexOrThrow("numero")),
        compromisoId = c.getLong(c.getColumnIndexOrThrow("compromiso_id")),
        folio = c.getString(c.getColumnIndexOrThrow("folio")),
        usuarioId = c.getLong(c.getColumnIndexOrThrow("usuario_id")),
        empresa = c.getString(c.getColumnIndexOrThrow("empresa")),
        concepto = c.getString(c.getColumnIndexOrThrow("concepto")),
        monto = c.getInt(c.getColumnIndexOrThrow("monto")),
        fecha = Fecha.desdeTexto(c.getString(c.getColumnIndexOrThrow("fecha"))) ?: Fecha.HOY,
        hora = c.getString(c.getColumnIndexOrThrow("hora")),
        medio = c.getString(c.getColumnIndexOrThrow("medio")),
        canal = c.getString(c.getColumnIndexOrThrow("canal")),
        aTiempo = c.getInt(c.getColumnIndexOrThrow("a_tiempo")) == 1
    )

    private fun aReporte(c: Cursor): Reporte = Reporte(
        id = c.getLong(c.getColumnIndexOrThrow("id")),
        numero = c.getString(c.getColumnIndexOrThrow("numero")),
        compromisoId = c.getLong(c.getColumnIndexOrThrow("compromiso_id")),
        folio = c.getString(c.getColumnIndexOrThrow("folio")),
        usuarioId = c.getLong(c.getColumnIndexOrThrow("usuario_id")),
        motivo = MotivoReporte.valueOf(c.getString(c.getColumnIndexOrThrow("motivo"))),
        comentario = c.getString(c.getColumnIndexOrThrow("comentario")) ?: "",
        estado = EstadoReporte.valueOf(c.getString(c.getColumnIndexOrThrow("estado"))),
        respuesta = c.getString(c.getColumnIndexOrThrow("respuesta")) ?: "",
        fecha = c.getString(c.getColumnIndexOrThrow("fecha"))
    )

    private fun aRecomendacion(c: Cursor): Recomendacion = Recomendacion(
        id = c.getLong(c.getColumnIndexOrThrow("id")),
        numero = c.getString(c.getColumnIndexOrThrow("numero")),
        usuarioId = c.getLong(c.getColumnIndexOrThrow("usuario_id")),
        organizacion = c.getString(c.getColumnIndexOrThrow("organizacion")),
        rubro = c.getString(c.getColumnIndexOrThrow("rubro")),
        comuna = c.getString(c.getColumnIndexOrThrow("comuna")),
        contacto = c.getString(c.getColumnIndexOrThrow("contacto")) ?: "",
        estado = EstadoRecomendacion.valueOf(c.getString(c.getColumnIndexOrThrow("estado")))
    )

    private fun <T> lista(db: SQLiteDatabase, sql: String, args: Array<String>?, mapear: (Cursor) -> T): List<T> {
        val resultado = mutableListOf<T>()
        db.rawQuery(sql, args).use {
            while (it.moveToNext()) resultado.add(mapear(it))
        }
        return resultado
    }

    private fun listaCompromisos(db: SQLiteDatabase, usuarioId: Long): List<Compromiso> =
        lista(db, "SELECT * FROM compromisos WHERE usuario_id = ?", arrayOf(usuarioId.toString())) { aCompromiso(it) }

    fun usuarioPorRut(rut: String): Usuario? =
        lista(readableDatabase, "SELECT * FROM usuarios WHERE rut = ?", arrayOf(rut)) { aUsuario(it) }.firstOrNull()

    fun usuarioPorId(id: Long): Usuario? =
        lista(readableDatabase, "SELECT * FROM usuarios WHERE id = ?", arrayOf(id.toString())) { aUsuario(it) }.firstOrNull()

    fun pinHashDe(id: Long): String? {
        readableDatabase.rawQuery("SELECT pin_hash FROM usuarios WHERE id = ?", arrayOf(id.toString())).use {
            return if (it.moveToFirst()) it.getString(0) else null
        }
    }

    fun existeCorreo(correo: String, menosId: Long = -1): Boolean {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM usuarios WHERE LOWER(correo) = LOWER(?) AND id <> ?",
            arrayOf(correo, menosId.toString())
        ).use {
            it.moveToFirst()
            return it.getInt(0) > 0
        }
    }

    fun usuarios(): List<Usuario> =
        lista(readableDatabase, "SELECT * FROM usuarios ORDER BY rol, nombre", null) { aUsuario(it) }

    fun actualizarUsuario(id: Long, nombre: String, correo: String, comuna: String, telefono: String, pinHash: String?) {
        val valores = ContentValues()
        valores.put("nombre", nombre)
        valores.put("correo", correo)
        valores.put("comuna", comuna)
        valores.put("telefono", telefono)
        if (pinHash != null) valores.put("pin_hash", pinHash)
        writableDatabase.update("usuarios", valores, "id = ?", arrayOf(id.toString()))
    }

    fun cambiarRol(id: Long, rol: Rol) {
        val valores = ContentValues()
        valores.put("rol", rol.name)
        writableDatabase.update("usuarios", valores, "id = ?", arrayOf(id.toString()))
    }

    fun cambiarActivo(id: Long, activo: Boolean) {
        val valores = ContentValues()
        valores.put("activo", if (activo) 1 else 0)
        writableDatabase.update("usuarios", valores, "id = ?", arrayOf(id.toString()))
    }

    fun eliminarUsuario(id: Long) {
        val db = writableDatabase
        val args = arrayOf(id.toString())
        db.beginTransaction()
        try {
            db.delete("recomendaciones", "usuario_id = ?", args)
            db.delete("reportes", "usuario_id = ?", args)
            db.delete("comprobantes", "usuario_id = ?", args)
            db.delete("compromisos", "usuario_id = ?", args)
            db.delete("usuarios", "id = ?", args)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun compromisosDe(usuarioId: Long): List<Compromiso> =
        listaCompromisos(readableDatabase, usuarioId).sortedBy { it.vencimiento.valor() }

    fun todosLosCompromisos(): List<Compromiso> =
        lista(readableDatabase, "SELECT * FROM compromisos", null) { aCompromiso(it) }.sortedBy { it.vencimiento.valor() }

    fun compromisoPorId(id: Long): Compromiso? =
        lista(readableDatabase, "SELECT * FROM compromisos WHERE id = ?", arrayOf(id.toString())) { aCompromiso(it) }.firstOrNull()

    fun actualizarCompromiso(
        id: Long, usuarioId: Long, empresa: String, tipo: TipoCompromiso, concepto: String,
        cuota: Int, totalCuotas: Int, monto: Int, vencimiento: Fecha, estado: EstadoCompromiso
    ) {
        val valores = ContentValues()
        valores.put("usuario_id", usuarioId)
        valores.put("empresa", empresa)
        valores.put("tipo", tipo.name)
        valores.put("concepto", concepto)
        valores.put("cuota", cuota)
        valores.put("total_cuotas", totalCuotas)
        valores.put("monto", monto)
        valores.put("vencimiento", vencimiento.texto())
        valores.put("estado", estado.name)
        writableDatabase.update("compromisos", valores, "id = ?", arrayOf(id.toString()))
    }

    fun cambiarEstadoCompromiso(id: Long, estado: EstadoCompromiso) {
        val valores = ContentValues()
        valores.put("estado", estado.name)
        writableDatabase.update("compromisos", valores, "id = ?", arrayOf(id.toString()))
    }

    fun tieneComprobante(compromisoId: Long): Boolean {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM comprobantes WHERE compromiso_id = ?", arrayOf(compromisoId.toString())
        ).use {
            it.moveToFirst()
            return it.getInt(0) > 0
        }
    }

    fun eliminarCompromiso(id: Long) {
        val db = writableDatabase
        db.delete("reportes", "compromiso_id = ?", arrayOf(id.toString()))
        db.delete("compromisos", "id = ?", arrayOf(id.toString()))
    }

    fun comprobantesDe(usuarioId: Long): List<Comprobante> =
        lista(readableDatabase, "SELECT * FROM comprobantes WHERE usuario_id = ? ORDER BY id DESC", arrayOf(usuarioId.toString())) { aComprobante(it) }

    fun comprobantePorId(id: Long): Comprobante? =
        lista(readableDatabase, "SELECT * FROM comprobantes WHERE id = ?", arrayOf(id.toString())) { aComprobante(it) }.firstOrNull()

    fun reportesDe(usuarioId: Long): List<Reporte> =
        lista(readableDatabase, "SELECT * FROM reportes WHERE usuario_id = ? ORDER BY id DESC", arrayOf(usuarioId.toString())) { aReporte(it) }

    fun todosLosReportes(): List<Reporte> =
        lista(readableDatabase, "SELECT * FROM reportes ORDER BY id DESC", null) { aReporte(it) }

    fun reportePorId(id: Long): Reporte? =
        lista(readableDatabase, "SELECT * FROM reportes WHERE id = ?", arrayOf(id.toString())) { aReporte(it) }.firstOrNull()

    fun cerrarReporte(id: Long, estado: EstadoReporte, respuesta: String) {
        val valores = ContentValues()
        valores.put("estado", estado.name)
        valores.put("respuesta", respuesta)
        writableDatabase.update("reportes", valores, "id = ?", arrayOf(id.toString()))
    }

    fun recomendacionesDe(usuarioId: Long): List<Recomendacion> =
        lista(readableDatabase, "SELECT * FROM recomendaciones WHERE usuario_id = ? ORDER BY id DESC", arrayOf(usuarioId.toString())) { aRecomendacion(it) }

    fun eliminarRecomendacion(id: Long) {
        writableDatabase.delete("recomendaciones", "id = ?", arrayOf(id.toString()))
    }

    fun pinesGuardados(): Map<String, String> {
        val mapa = mutableMapOf<String, String>()
        readableDatabase.rawQuery("SELECT rut, pin_hash FROM usuarios", null).use {
            while (it.moveToNext()) mapa[it.getString(0)] = it.getString(1)
        }
        return mapa
    }

    private fun copiarUsuario(db: SQLiteDatabase, u: Usuario, pinHash: String) {
        val v = ContentValues()
        v.put("id", u.id)
        v.put("rut", u.rut)
        v.put("nombre", u.nombre)
        v.put("correo", u.correo)
        v.put("comuna", u.comuna)
        v.put("telefono", u.telefono)
        v.put("pin_hash", pinHash)
        v.put("rol", u.rol.name)
        v.put("activo", if (u.activo) 1 else 0)
        db.insertWithOnConflict("usuarios", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun copiarCompromiso(db: SQLiteDatabase, c: Compromiso) {
        val v = ContentValues()
        v.put("id", c.id)
        v.put("folio", c.folio)
        v.put("usuario_id", c.usuarioId)
        v.put("empresa", c.empresa)
        v.put("tipo", c.tipo.name)
        v.put("concepto", c.concepto)
        v.put("cuota", c.cuota)
        v.put("total_cuotas", c.totalCuotas)
        v.put("monto", c.monto)
        v.put("vencimiento", c.vencimiento.texto())
        v.put("estado", c.estado.name)
        db.insertWithOnConflict("compromisos", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun copiarComprobante(db: SQLiteDatabase, c: Comprobante) {
        val v = ContentValues()
        v.put("id", c.id)
        v.put("numero", c.numero)
        v.put("compromiso_id", c.compromisoId)
        v.put("folio", c.folio)
        v.put("usuario_id", c.usuarioId)
        v.put("empresa", c.empresa)
        v.put("concepto", c.concepto)
        v.put("monto", c.monto)
        v.put("fecha", c.fecha.texto())
        v.put("hora", c.hora)
        v.put("medio", c.medio)
        v.put("canal", c.canal)
        v.put("a_tiempo", if (c.aTiempo) 1 else 0)
        db.insertWithOnConflict("comprobantes", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun copiarReporte(db: SQLiteDatabase, r: Reporte) {
        val v = ContentValues()
        v.put("id", r.id)
        v.put("numero", r.numero)
        v.put("compromiso_id", r.compromisoId)
        v.put("folio", r.folio)
        v.put("usuario_id", r.usuarioId)
        v.put("motivo", r.motivo.name)
        v.put("comentario", r.comentario)
        v.put("estado", r.estado.name)
        v.put("respuesta", r.respuesta)
        v.put("fecha", r.fecha)
        db.insertWithOnConflict("reportes", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun copiarRecomendacion(db: SQLiteDatabase, r: Recomendacion) {
        val v = ContentValues()
        v.put("id", r.id)
        v.put("numero", r.numero)
        v.put("usuario_id", r.usuarioId)
        v.put("organizacion", r.organizacion)
        v.put("rubro", r.rubro)
        v.put("comuna", r.comuna)
        v.put("contacto", r.contacto)
        v.put("estado", r.estado.name)
        db.insertWithOnConflict("recomendaciones", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun guardarUsuarioDelServidor(u: Usuario, pinHash: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val anterior = usuarioPorRut(u.rut)
            if (anterior != null && anterior.id != u.id) {
                val args = arrayOf(anterior.id.toString())
                db.delete("recomendaciones", "usuario_id = ?", args)
                db.delete("reportes", "usuario_id = ?", args)
                db.delete("comprobantes", "usuario_id = ?", args)
                db.delete("compromisos", "usuario_id = ?", args)
                db.delete("usuarios", "id = ?", args)
            }
            copiarUsuario(db, u, pinHash)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun reemplazarDatosDe(
        usuarioId: Long, compromisos: List<Compromiso>, comprobantes: List<Comprobante>,
        reportes: List<Reporte>, recomendaciones: List<Recomendacion>
    ) {
        val db = writableDatabase
        val args = arrayOf(usuarioId.toString())
        db.beginTransaction()
        try {
            db.delete("recomendaciones", "usuario_id = ?", args)
            db.delete("reportes", "usuario_id = ?", args)
            db.delete("comprobantes", "usuario_id = ?", args)
            db.delete("compromisos", "usuario_id = ?", args)
            compromisos.forEach { copiarCompromiso(db, it) }
            comprobantes.forEach { copiarComprobante(db, it) }
            reportes.forEach { copiarReporte(db, it) }
            recomendaciones.forEach { copiarRecomendacion(db, it) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun reemplazarTodo(
        usuarios: List<Usuario>, compromisos: List<Compromiso>, comprobantes: List<Comprobante>,
        reportes: List<Reporte>, recomendaciones: List<Recomendacion>
    ) {
        val pines = pinesGuardados()
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("recomendaciones", null, null)
            db.delete("reportes", null, null)
            db.delete("comprobantes", null, null)
            db.delete("compromisos", null, null)
            db.delete("usuarios", null, null)
            usuarios.forEach { copiarUsuario(db, it, pines[it.rut] ?: "") }
            compromisos.forEach { copiarCompromiso(db, it) }
            comprobantes.forEach { copiarComprobante(db, it) }
            reportes.forEach { copiarReporte(db, it) }
            recomendaciones.forEach { copiarRecomendacion(db, it) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
