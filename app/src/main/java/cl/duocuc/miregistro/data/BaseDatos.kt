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

class BaseDatos(context: Context) : SQLiteOpenHelper(context, "pagoflex.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, rut TEXT NOT NULL UNIQUE, nombre TEXT NOT NULL, " +
                "correo TEXT NOT NULL UNIQUE, comuna TEXT NOT NULL, telefono TEXT, pin_hash TEXT NOT NULL, " +
                "rol TEXT NOT NULL, activo INTEGER NOT NULL DEFAULT 1)"
        )
        db.execSQL(
            "CREATE TABLE compromisos (id INTEGER PRIMARY KEY AUTOINCREMENT, folio TEXT NOT NULL UNIQUE, " +
                "usuario_id INTEGER NOT NULL, empresa TEXT NOT NULL, tipo TEXT NOT NULL, concepto TEXT NOT NULL, " +
                "cuota INTEGER NOT NULL, total_cuotas INTEGER NOT NULL, monto INTEGER NOT NULL, vencimiento TEXT NOT NULL, " +
                "estado TEXT NOT NULL, FOREIGN KEY (usuario_id) REFERENCES usuarios(id))"
        )
        db.execSQL(
            "CREATE TABLE comprobantes (id INTEGER PRIMARY KEY AUTOINCREMENT, numero TEXT NOT NULL UNIQUE, " +
                "compromiso_id INTEGER NOT NULL, folio TEXT NOT NULL, usuario_id INTEGER NOT NULL, empresa TEXT NOT NULL, " +
                "concepto TEXT NOT NULL, monto INTEGER NOT NULL, fecha TEXT NOT NULL, hora TEXT NOT NULL, medio TEXT NOT NULL, " +
                "canal TEXT NOT NULL, a_tiempo INTEGER NOT NULL, FOREIGN KEY (compromiso_id) REFERENCES compromisos(id))"
        )
        db.execSQL(
            "CREATE TABLE reportes (id INTEGER PRIMARY KEY AUTOINCREMENT, numero TEXT NOT NULL UNIQUE, " +
                "compromiso_id INTEGER NOT NULL, folio TEXT NOT NULL, usuario_id INTEGER NOT NULL, motivo TEXT NOT NULL, " +
                "comentario TEXT, estado TEXT NOT NULL, respuesta TEXT, fecha TEXT NOT NULL, " +
                "FOREIGN KEY (compromiso_id) REFERENCES compromisos(id))"
        )
        db.execSQL(
            "CREATE TABLE recomendaciones (id INTEGER PRIMARY KEY AUTOINCREMENT, numero TEXT NOT NULL UNIQUE, " +
                "usuario_id INTEGER NOT NULL, organizacion TEXT NOT NULL, rubro TEXT NOT NULL, comuna TEXT NOT NULL, " +
                "contacto TEXT, estado TEXT NOT NULL, FOREIGN KEY (usuario_id) REFERENCES usuarios(id))"
        )
        cargarDatosIniciales(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS recomendaciones")
        db.execSQL("DROP TABLE IF EXISTS reportes")
        db.execSQL("DROP TABLE IF EXISTS comprobantes")
        db.execSQL("DROP TABLE IF EXISTS compromisos")
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        onCreate(db)
    }

    private fun cargarDatosIniciales(db: SQLiteDatabase) {
        val pin = Validador.hashPin("1234")
        val admin = insertarUsuario(db, "11111111-1", "Administrador PagoFlex", "admin@pagoflex.cl", "Santiago", "", pin, Rol.ADMIN)
        val ana = insertarUsuario(db, "15834207-3", "Ana Rojas Pérez", "ana.rojas@correo.cl", "Independencia", "912345678", pin, Rol.CLIENTE)
        insertarUsuario(db, "12345678-5", "Pedro Soto Muñoz", "pedro.soto@correo.cl", "Recoleta", "", pin, Rol.CLIENTE)
        if (admin < 0 || ana < 0) return

        insertarCompromiso(db, ana, "Plaza Oriente", TipoCompromiso.ARRIENDO, "Arriendo depto", 2, 12, 420000, Fecha.enDias(-36), EstadoCompromiso.PAGADO)
        insertarCompromiso(db, ana, "Plaza Oriente", TipoCompromiso.ARRIENDO, "Arriendo depto", 3, 12, 420000, Fecha.enDias(-6), EstadoCompromiso.PENDIENTE)
        insertarCompromiso(db, ana, "Plaza Oriente", TipoCompromiso.ARRIENDO, "Arriendo depto", 4, 12, 420000, Fecha.enDias(24), EstadoCompromiso.PENDIENTE)
        insertarCompromiso(db, ana, "Caja Los Andes", TipoCompromiso.CREDITO, "Crédito de consumo", 6, 24, 89900, Fecha.enDias(-28), EstadoCompromiso.PAGADO)
        insertarCompromiso(db, ana, "Caja Los Andes", TipoCompromiso.CREDITO, "Crédito de consumo", 7, 24, 89900, Fecha.enDias(2), EstadoCompromiso.PENDIENTE)
        insertarCompromiso(db, ana, "Club Deportivo Las Mercedes", TipoCompromiso.CLUB, "Cuota social", 10, 12, 15000, Fecha.enDias(9), EstadoCompromiso.PENDIENTE)
        insertarCompromiso(db, ana, "Fundación Las Rosas", TipoCompromiso.FUNDACION, "Aporte mensual", 0, 0, 10000, Fecha.enDias(15), EstadoCompromiso.PENDIENTE)

        val pagados = listaCompromisos(db, ana).filter { it.estado == EstadoCompromiso.PAGADO }
        pagados.forEachIndexed { i, c ->
            val fecha = if (i == 0) Fecha.enDias(-38) else Fecha.enDias(-25)
            insertarComprobante(db, c, c.monto, fecha, "10:15", "Transferencia", if (i == 0) "App" else "Presencial", i == 0)
        }
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
}
