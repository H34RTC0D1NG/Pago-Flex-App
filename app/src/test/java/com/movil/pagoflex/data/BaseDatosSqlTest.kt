package com.movil.pagoflex.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BaseDatosSqlTest {

    private val archivo = File("src/main/assets/" + BaseDatos.ARCHIVO_SQL)

    @Test
    fun elArchivoSqlExisteDentroDeLaApp() {
        assertTrue(archivo.exists())
    }

    @Test
    fun separaLasSentenciasPorPuntoYComa() {
        val lista = BaseDatos.sentencias("CREATE TABLE a (id INTEGER);\n\n INSERT INTO a VALUES (1);\n")
        assertEquals(2, lista.size)
        assertEquals("INSERT INTO a VALUES (1)", lista[1])
    }

    @Test
    fun creaLasCincoTablas() {
        val sentencias = BaseDatos.sentencias(archivo.readText())
        val tablas = sentencias.filter { it.startsWith("CREATE TABLE") }.map { it.split(" ")[2] }
        assertEquals(listOf("usuarios", "compromisos", "comprobantes", "reportes", "recomendaciones"), tablas)
    }

    @Test
    fun traeLosDatosDePrueba() {
        val texto = archivo.readText()
        assertTrue(texto.contains("'15834207-3'"))
        assertTrue(texto.contains(Validador.hashPin("1234")))
        assertEquals(7, Regex("\\(\\d, 'PF-10\\d\\d'").findAll(texto).count())
    }
}
