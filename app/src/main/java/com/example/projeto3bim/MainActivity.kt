package com.example.projeto3bim

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var textViewResult: TextView
    private val REQUEST_IMAGE_PICK = 1
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        textViewResult = findViewById(R.id.textViewResult)

        val btnSelectImage = findViewById<Button>(R.id.btnSelectImage)
        val btnSpeak = findViewById<Button>(R.id.btnSpeak)

        // Mensagem de boas-vindas inicial
        textViewResult.text = "Olá! Selecione uma imagem para começar a leitura."

        // Inicializa o TextToSpeech
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                val result = tts.setLanguage(Locale("pt", "BR"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsReady = false
                    Log.e("TTS", "Idioma não suportado.")
                }
            } else {
                isTtsReady = false
                Log.e("TTS", "Falha na inicialização.")
            }
        }

        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }

        btnSpeak.setOnClickListener {
            val textToSpeak = textViewResult.text.toString()
            if (isTtsReady && textToSpeak.isNotEmpty()) {
                tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            if (imageUri != null) {
                val inputStream = contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                imageView.setImageBitmap(bitmap)

                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        // Lógica aprimorada para lidar com tabelas e colunas
                        val blocks = visionText.textBlocks
                        val sortedText = StringBuilder()
                        val sortedBlocks = blocks.sortedBy { it.boundingBox?.top }

                        for (block in sortedBlocks) {
                            val lines = block.lines.sortedBy { it.boundingBox?.top }
                            for (line in lines) {
                                val words = line.elements.sortedBy { it.boundingBox?.left }
                                for (word in words) {
                                    sortedText.append(word.text).append(" ")
                                }
                                sortedText.append("\n")
                            }
                        }
                        textViewResult.text = sortedText.toString()
                    }
                    .addOnFailureListener { e ->
                        textViewResult.text = "Erro: ${e.message}"
                    }
            }
        }
    }

    override fun onDestroy() {
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
