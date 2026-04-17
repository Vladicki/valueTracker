package com.griffith.valuetracker.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.min

class FoodClassifier(private val context: Context) {
    fun classify(imageUri: String): String {
        val interpreter = Interpreter(loadModelFile("food_inceptionv3.tflite"))
        interpreter.use { tflite ->
            val inputShape = tflite.getInputTensor(0).shape()
            val inputType = tflite.getInputTensor(0).dataType()
            val outputShape = tflite.getOutputTensor(0).shape()
            val outputType = tflite.getOutputTensor(0).dataType()

            val bitmap = loadBitmap(imageUri)
            val inputBuffer = preprocess(bitmap, inputShape, inputType)
            val output = Array(outputShape[0]) { FloatArray(outputShape.drop(1).reduce(Int::times)) }
            tflite.run(inputBuffer, output)

            val scores = output[0]
            // Model ships without a label map here, so raw indices and scores are returned for inspection.
            val topResults = scores.withIndex()
                .sortedByDescending { indexedValue -> indexedValue.value }
                .take(min(5, scores.size))
                .joinToString("\n") { indexedValue -> "#${indexedValue.index} -> ${"%.4f".format(indexedValue.value)}" }

            return buildString {
                appendLine("Input shape: ${inputShape.joinToString(prefix = "[", postfix = "]")}")
                appendLine("Input type: $inputType")
                appendLine("Output shape: ${outputShape.joinToString(prefix = "[", postfix = "]")}")
                appendLine("Output type: $outputType")
                appendLine()
                appendLine("Top outputs:")
                append(topResults)
            }
        }
    }

    private fun loadBitmap(imageUri: String): Bitmap {
        val uri = Uri.parse(imageUri)
        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: error("Failed to open image: $imageUri")
    }

    private fun preprocess(bitmap: Bitmap, inputShape: IntArray, inputType: DataType): ByteBuffer {
        val height = inputShape.getOrNull(1) ?: error("Unexpected input shape")
        val width = inputShape.getOrNull(2) ?: error("Unexpected input shape")
        val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val bytesPerChannel = if (inputType == DataType.FLOAT32) 4 else 1
        val buffer = ByteBuffer.allocateDirect(width * height * 3 * bytesPerChannel).order(ByteOrder.nativeOrder())
        val pixels = IntArray(width * height)
        resized.getPixels(pixels, 0, width, 0, 0, width, height)

        pixels.forEach { pixel ->
            val r = (pixel shr 16 and 0xFF)
            val g = (pixel shr 8 and 0xFF)
            val b = (pixel and 0xFF)
            if (inputType == DataType.FLOAT32) {
                buffer.putFloat(r / 255f)
                buffer.putFloat(g / 255f)
                buffer.putFloat(b / 255f)
            } else {
                buffer.put(r.toByte())
                buffer.put(g.toByte())
                buffer.put(b.toByte())
            }
        }
        buffer.rewind()
        return buffer
    }

    private fun loadModelFile(assetName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(assetName)
        FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
        }
    }
}
