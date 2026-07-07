package com.bolo101.dermavision.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class Classifier(private val context: Context) {

    companion object {
        private const val MODEL_FILE    = "dermavision.tflite"
        private const val IMG_SIZE      = 224      // taille d'entrée d'EfficientNetB4
        const val        THRESHOLD      = 0.35f    // seuil déterminé à la cellule 13b
        private const val NUM_CHANNELS  = 3        // R, G, B
        private const val BYTES_PER_FLOAT = 4      // float32 = 4 octets
    }

    // lazy = le modèle est chargé seulement au premier appel de classify()
    private val interpreter: Interpreter by lazy { loadModel() }

    // ── Chargement du modèle depuis les assets ─────────────────
    private fun loadModel(): Interpreter {
        val afd = context.assets.openFd(MODEL_FILE)
        val fileChannel = FileInputStream(afd.fileDescriptor).channel
        val modelBuffer = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            afd.startOffset,
            afd.declaredLength
        )
        return Interpreter(modelBuffer)
    }

    // ── Pipeline complet : URI → ClassificationResult ──────────
    fun classify(uri: Uri): ClassificationResult {
        val bitmap  = loadBitmapFromUri(uri)
        val resized = Bitmap.createScaledBitmap(bitmap, IMG_SIZE, IMG_SIZE, true)
        val input   = bitmapToByteBuffer(resized)

        // Tableau de sortie : 1 image → 1 score sigmoid
        val output = Array(1) { FloatArray(1) }
        interpreter.run(input, output)

        val score = output[0][0]
        return ClassificationResult(score, score >= THRESHOLD)
    }

    // ── Chargement de l'image depuis l'URI ─────────────────────
    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        // file:// = image dans le cache interne → accès direct
        // content:// = accès via ContentResolver
        return if (uri.scheme == "file") {
            BitmapFactory.decodeFile(uri.path)
                ?: throw IllegalArgumentException("Image illisible : ${uri.path}")
        } else {
            context.contentResolver.openInputStream(uri).use { stream ->
                BitmapFactory.decodeStream(stream)
                    ?: throw IllegalArgumentException("Image illisible : $uri")
            }
        }
    }

    // ── Conversion Bitmap → ByteBuffer ─────────────────────────
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        // Taille : 1 image × 224px × 224px × 3 canaux × 4 octets
        val buffer = ByteBuffer
            .allocateDirect(1 * IMG_SIZE * IMG_SIZE * NUM_CHANNELS * BYTES_PER_FLOAT)
            .also { it.order(ByteOrder.nativeOrder()) }

        val pixels = IntArray(IMG_SIZE * IMG_SIZE)
        bitmap.getPixels(pixels, 0, IMG_SIZE, 0, 0, IMG_SIZE, IMG_SIZE)

        for (pixel in pixels) {
            // On passe les valeurs brutes [0, 255] — EfficientNetB4
            // gère son propre preprocessing en interne (identique à l'entraînement)
            buffer.putFloat(((pixel shr 16) and 0xFF).toFloat())  // Rouge
            buffer.putFloat(((pixel shr  8) and 0xFF).toFloat())  // Vert
            buffer.putFloat(( pixel         and 0xFF).toFloat())  // Bleu
        }
        return buffer
    }

    // Libère la mémoire du modèle quand on a fini
    fun close() = interpreter.close()
}