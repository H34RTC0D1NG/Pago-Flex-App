package cl.duocuc.miregistro.ui.theme

import android.app.Activity
import android.graphics.drawable.shapes.Shape
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val EsquemaOscuro = darkColorScheme(
    primary = PrimarioOscuro,
    onPrimary = PrimarioOscuro,
    primaryContainer = PrimarioOscuro,
    onPrimaryContainer = PrimarioClaro,
    secondary = Secundario,
    onSecondary = Blanco,
    tertiary = Acento,
    onTertiary = Blanco,
    background = FondoOscuro,
    onBackground = TextoClaro,
    surface = SuperficieOscura,
    onSurface = TextoClaro
)

private val EsquemaClaro = lightColorScheme(
    primary = Primario,
    onPrimary = Blanco,
    primaryContainer = PrimarioClaro,
    onPrimaryContainer = PrimarioOscuro,
    secondary = Secundario,
    onSecondary = Blanco,
    tertiary = Acento,
    onTertiary = Blanco,
    background = FondoClaro,
    onBackground = TextoOscuro,
    surface = SuperficieClara,
    onSurface = TextoOscuro
)


private val EsquemaAltoContraste = darkColorScheme(
    primary = ContrastePrimario,
    onPrimary = ContrasteFondo,
    primaryContainer = ContrasteSuperficie,
    onPrimaryContainer = ContrastePrimario,
    secondary = ContrastePrimario,
    onSecondary = ContrasteFondo,
    tertiary = ContrastePrimario,
    onTertiary = ContrasteFondo,
    background = ContrasteFondo,
    onBackground = ContrasteTexto,
    surface = ContrasteSuperficie,
    onSurface = ContrasteTexto
)

private val Formas = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp)
)
@Composable
fun MiRegistroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    textoGrande: Boolean = false,
    altoContraste: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        altoContraste -> EsquemaAltoContraste
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> EsquemaOscuro
        else -> EsquemaClaro
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = if (textoGrande) TipografiaGrande else Tipografia,
        shapes = Formas,
        content = content
    )
}