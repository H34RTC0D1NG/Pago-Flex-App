package cl.duocuc.miregistro.nativo

import android.content.Context
import android.content.Intent

fun compartirTexto(context: Context, titulo: String, texto: String) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_SUBJECT, titulo)
    intent.putExtra(Intent.EXTRA_TEXT, texto)
    context.startActivity(Intent.createChooser(intent, titulo))
}
