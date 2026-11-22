package cl.duocuc.miregistro.model

data class Fecha(
    val dia: Int,
    val mes: Int,
    val anio: Int
) {
    companion object {
        val HOY = Fecha(6, 10, 2026)

        private val DIAS_POR_MES = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        val NOMBRES_MESES = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )

        fun esBisiesto(anio: Int): Boolean = (anio % 4 == 0 && anio % 100 != 0) || anio % 400 == 0

        fun horaActual(): String {
            val segundosTotales = System.currentTimeMillis() / 1000
            val horas = ((segundosTotales / 3600) - 3 + 24) % 24
            val minutos = (segundosTotales / 60) % 60
            val segundos = segundosTotales % 60
            return String.format("%02d:%02d:%02d", horas, minutos, segundos)
        }
    }

    fun valor(): Int = anio * 10000 + mes * 100 + dia

    fun esAntesDe(otra: Fecha): Boolean = valor() < otra.valor()

    fun texto(): String = String.format("%02d-%02d-%d", dia, mes, anio)

    fun nombreMes(): String = NOMBRES_MESES[mes - 1] + " " + anio

    fun aDias(): Int {
        var total = 0
        for (a in 2000 until anio) {
            total += if (esBisiesto(a)) 366 else 365
        }
        for (m in 1 until mes) {
            total += if (m == 2 && esBisiesto(anio)) 29 else DIAS_POR_MES[m - 1]
        }
        return total + dia
    }

    fun diasHasta(otra: Fecha): Int = otra.aDias() - aDias()
}
