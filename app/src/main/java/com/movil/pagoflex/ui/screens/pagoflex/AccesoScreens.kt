package com.movil.pagoflex.ui.screens.pagoflex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.movil.pagoflex.model.EstadoOperacion
import com.movil.pagoflex.model.PagoFlexUiState
import com.movil.pagoflex.model.Pantalla
import com.movil.pagoflex.ui.components.BotonPrincipal
import com.movil.pagoflex.ui.components.BotonSecundario
import com.movil.pagoflex.ui.components.Campo
import com.movil.pagoflex.ui.components.CampoPin
import com.movil.pagoflex.ui.components.Casilla
import com.movil.pagoflex.ui.components.Logo
import com.movil.pagoflex.ui.components.MensajeOperacion
import com.movil.pagoflex.ui.components.PantallaBase
import com.movil.pagoflex.ui.components.Tarjeta
import com.movil.pagoflex.viewmodel.PagoFlexViewModel

@Composable
fun IngresoScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel) {
    PantallaBase(titulo = Pantalla.INGRESO.titulo) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Logo(96.dp)
        }
        Text(
            "Paga en un solo lugar tu arriendo, créditos, cuotas de club y aportes.",
            style = MaterialTheme.typography.bodyLarge
        )
        Tarjeta(titulo = "Ingresa con tu RUT y PIN") {
            Campo(uiState.rut, vm::onRutCambia, "RUT (ej: 15.834.207-3)", uiState.errorRut)
            CampoPin(uiState.pin, vm::onPinCambia, "PIN de 4 números", uiState.pinVisible, vm::onAlternarPin, uiState.errorPin)
        }
        BotonPrincipal(
            texto = "Ingresar",
            onClick = vm::onIngresar,
            habilitado = !uiState.bloqueado && uiState.estadoIngreso !is EstadoOperacion.Cargando
        )
        MensajeOperacion(uiState.estadoIngreso)
        Tarjeta(titulo = "Cuentas de prueba (PIN 1234)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::usarCuentaCliente, modifier = Modifier.weight(1f)) { Text("Cliente") }
                OutlinedButton(onClick = vm::usarCuentaAdmin, modifier = Modifier.weight(1f)) { Text("Administrador") }
            }
        }
        BotonSecundario("No tengo cuenta, quiero crearla", vm::irARegistro)
        Tarjeta(titulo = "Tu seguridad") {
            Text(
                "Tu PIN se guarda cifrado. Después de 3 intentos fallidos la app se bloquea 30 segundos.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("Nunca te pediremos datos de tu tarjeta ni claves bancarias.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun RegistroScreen(uiState: PagoFlexUiState, vm: PagoFlexViewModel) {
    val f = uiState.formRegistro
    PantallaBase(titulo = Pantalla.REGISTRO.titulo, onVolver = vm::volver) {
        Tarjeta(titulo = "Tus datos") {
            Campo(f.rut, { v -> vm.onFormRegistro { it.copy(rut = v.take(12), errores = it.errores - "rut") } }, "RUT", f.errores["rut"])
            Campo(f.nombre, { v -> vm.onFormRegistro { it.copy(nombre = v, errores = it.errores - "nombre") } }, "Nombre completo", f.errores["nombre"])
            Campo(
                f.correo, { v -> vm.onFormRegistro { it.copy(correo = v, errores = it.errores - "correo") } },
                "Correo", f.errores["correo"], KeyboardType.Email
            )
            Campo(f.comuna, { v -> vm.onFormRegistro { it.copy(comuna = v, errores = it.errores - "comuna") } }, "Comuna", f.errores["comuna"])
            Campo(
                f.telefono, { v -> vm.onFormRegistro { it.copy(telefono = v, errores = it.errores - "telefono") } },
                "Teléfono (opcional)", f.errores["telefono"], KeyboardType.Phone
            )
        }
        Tarjeta(titulo = "Crea tu PIN") {
            Campo(
                f.pin, { v -> vm.onFormRegistro { it.copy(pin = v.filter { c -> c.isDigit() }.take(4), errores = it.errores - "pin") } },
                "PIN de 4 números", f.errores["pin"], KeyboardType.NumberPassword, oculto = true
            )
            Campo(
                f.confirmarPin,
                { v -> vm.onFormRegistro { it.copy(confirmarPin = v.filter { c -> c.isDigit() }.take(4), errores = it.errores - "confirmarPin") } },
                "Repite el PIN", f.errores["confirmarPin"], KeyboardType.NumberPassword, oculto = true
            )
            Casilla("Acepto los términos y el uso de mis datos", f.aceptaTerminos) { v ->
                vm.onFormRegistro { it.copy(aceptaTerminos = v, errores = it.errores - "terminos") }
            }
            f.errores["terminos"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        BotonPrincipal("Crear cuenta", vm::onRegistrar, uiState.estadoRegistro !is EstadoOperacion.Cargando)
        MensajeOperacion(uiState.estadoRegistro)
        Text(
            "La cuenta se guarda en el teléfono y, si ms-usuarios está encendido, también en el servidor.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
