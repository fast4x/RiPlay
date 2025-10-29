package it.fast4x.riplay.utils

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.fast4x.riplay.ui.components.themed.SmartMessage
import java.util.Locale


@Composable
fun StartVoiceInput(
    onTextRecognized: (String) -> Unit,
    onRecognitionError: () -> Unit,
    onListening: (Boolean) -> Unit
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListening(true)
                SmartMessage("Puoi parlare ora...", context = context)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                onListening(false)
                speechRecognizer.stopListening()
            }
            override fun onError(error: Int) {
                onListening(false)
                onRecognitionError()
                SmartMessage("Ops, forse non hai detto niente", context = context)
                speechRecognizer.stopListening()
            }
            override fun onResults(results: Bundle?) {
                onListening(false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onTextRecognized(matches[0])
                    SmartMessage("Hai detto: ${matches[0]}", context = context)
                    speechRecognizer.stopListening()
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose {
            speechRecognizer.destroy() // Pulizia quando il Composable esce dallo schermo
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                speechRecognizer.stopListening()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    StartVoiceInputListener(context, speechRecognizer, onRecognitionError)

}

@Composable
fun StartVoiceInputListener(
    context: Context,
    speechRecognizer: SpeechRecognizer,
    onRecognitionError: () -> Unit
) {
    // Launcher per richiedere il permesso del microfono
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Il permesso è stato concesso, puoi iniziare ad ascoltare
            // Qui potresti chiamare una funzione per avviare il riconoscimento
            SmartMessage( "Permesso microfono concesso", context = context)
        } else {
            // Permesso negato
            SmartMessage( "Permesso microfono negato", context = context)
            onRecognitionError()
        }
    }

    // Check for permission
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer.startListening(intent)
    } else {
        // No permission granted, request it
        SideEffect {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        SmartMessage("Permessi microfono mancanti, concedi per favore", context = context)
    }
}

@Composable
fun startExternalVoiceInput(): String {

    val context = LocalContext.current
    var textState by remember { mutableStateOf("") }
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                textState = results[0]
                SmartMessage("Hai detto: $textState", context = context)
            }
        } else {
            SmartMessage("Riconoscimento vocale fallito", context = context)
        }
    }

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Parla ora...")
    }

    try {
        speechRecognizerLauncher.launch(intent)
    } catch (e: Exception) {
        SmartMessage( "Il tuo dispositivo non supporta l'input vocale", context = context)
    }

    return textState
}