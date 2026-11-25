package com.movil.pagoflex.data

import com.movil.pagoflex.model.Fecha
import com.movil.pagoflex.model.FormCompromiso
import com.movil.pagoflex.model.FormPerfil
import com.movil.pagoflex.model.FormRegistro
import com.movil.pagoflex.model.FormReferido
import com.movil.pagoflex.model.FormUsuario
import java.security.MessageDigest

object Validador {

    private val FORMATO_RUT = Regex("^[0-9]{1,2}\\.?[0-9]{3}\\.?[0-9]{3}-[0-9kK]$")
    private val FORMATO_CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val FORMATO_TELEFONO = Regex("^\\+?[0-9]{8,12}$")
    private val FORMATO_PIN = Regex("^[0-9]{4}$")

    fun rutValido(rut: String): Boolean {
        if (!FORMATO_RUT.matches(rut.trim())) return false
        val limpio = rut.trim().replace(".", "").replace("-", "").uppercase()
        val cuerpo = limpio.dropLast(1)
        val dv = limpio.last()
        var suma = 0
        var multiplicador = 2
        for (i in cuerpo.length - 1 downTo 0) {
            suma += (cuerpo[i] - '0') * multiplicador
            multiplicador = if (multiplicador == 7) 2 else multiplicador + 1
        }
        val esperado = when (val resto = 11 - (suma % 11)) {
            11 -> '0'
            10 -> 'K'
            else -> '0' + resto
        }
        return esperado == dv
    }

    fun normalizarRut(rut: String): String = rut.trim().replace(".", "").uppercase()

    fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(("pagoflex-" + pin).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun errorRut(rut: String): String? = when {
        rut.isBlank() -> "El RUT es obligatorio"
        !rutValido(rut) -> "RUT no válido, revisa el dígito verificador"
        else -> null
    }

    fun errorNombre(nombre: String): String? = when {
        nombre.isBlank() -> "El nombre es obligatorio"
        nombre.trim().length < 3 -> "Escribe el nombre completo"
        else -> null
    }

    fun errorCorreo(correo: String): String? = when {
        correo.isBlank() -> "El correo es obligatorio"
        !FORMATO_CORREO.matches(correo.trim()) -> "Ingresa un correo válido"
        else -> null
    }

    fun errorComuna(comuna: String): String? = if (comuna.isBlank()) "La comuna es obligatoria" else null

    fun errorTelefono(telefono: String): String? =
        if (telefono.isNotBlank() && !FORMATO_TELEFONO.matches(telefono.trim().replace(" ", ""))) "Teléfono no válido" else null

    fun errorPin(pin: String): String? = if (!FORMATO_PIN.matches(pin)) "El PIN debe tener 4 números" else null

    private fun agregar(errores: MutableMap<String, String>, campo: String, error: String?) {
        if (error != null) errores[campo] = error
    }

    fun validarRegistro(form: FormRegistro): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        agregar(errores, "rut", errorRut(form.rut))
        agregar(errores, "nombre", errorNombre(form.nombre))
        agregar(errores, "correo", errorCorreo(form.correo))
        agregar(errores, "comuna", errorComuna(form.comuna))
        agregar(errores, "telefono", errorTelefono(form.telefono))
        agregar(errores, "pin", errorPin(form.pin))
        if (form.pin != form.confirmarPin) errores["confirmarPin"] = "Los PIN no coinciden"
        if (!form.aceptaTerminos) errores["terminos"] = "Debes aceptar los términos"
        return errores
    }

    fun validarPerfil(form: FormPerfil): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        agregar(errores, "nombre", errorNombre(form.nombre))
        agregar(errores, "correo", errorCorreo(form.correo))
        agregar(errores, "comuna", errorComuna(form.comuna))
        agregar(errores, "telefono", errorTelefono(form.telefono))
        if (form.nuevoPin.isNotEmpty()) agregar(errores, "pin", errorPin(form.nuevoPin))
        return errores
    }

    fun validarReferido(form: FormReferido): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        if (form.organizacion.isBlank()) errores["organizacion"] = "El nombre de la organización es obligatorio"
        if (form.rubro.isBlank()) errores["rubro"] = "El rubro es obligatorio"
        if (form.comuna.isBlank()) errores["comuna"] = "La comuna es obligatoria"
        if (form.contacto.isNotBlank() && !form.autoriza) errores["autoriza"] = "La persona debe autorizar que la contactemos"
        return errores
    }

    fun validarUsuario(form: FormUsuario): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        agregar(errores, "rut", errorRut(form.rut))
        agregar(errores, "nombre", errorNombre(form.nombre))
        agregar(errores, "correo", errorCorreo(form.correo))
        agregar(errores, "comuna", errorComuna(form.comuna))
        agregar(errores, "telefono", errorTelefono(form.telefono))
        agregar(errores, "pin", errorPin(form.pin))
        return errores
    }

    fun validarCompromiso(form: FormCompromiso): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        if (form.usuarioId == null) errores["usuario"] = "Elige a quién se le cobra"
        if (form.empresa.isBlank()) errores["empresa"] = "La empresa es obligatoria"
        if (form.concepto.isBlank()) errores["concepto"] = "El concepto es obligatorio"
        val monto = form.monto.toIntOrNull()
        if (monto == null || monto <= 0) errores["monto"] = "Ingresa un monto mayor a 0"
        val cuota = form.cuota.toIntOrNull()
        val total = form.totalCuotas.toIntOrNull()
        if (cuota == null || total == null) {
            errores["cuota"] = "Cuota y total deben ser números"
        } else if (total == 0 && cuota != 0) {
            errores["cuota"] = "Un pago único lleva cuota 0 y total 0"
        } else if (total > 0 && cuota !in 1..total) {
            errores["cuota"] = "La cuota debe estar entre 1 y $total"
        }
        if (Fecha.desdeTexto(form.vencimiento) == null) errores["vencimiento"] = "Usa el formato dd-mm-aaaa"
        return errores
    }
}
