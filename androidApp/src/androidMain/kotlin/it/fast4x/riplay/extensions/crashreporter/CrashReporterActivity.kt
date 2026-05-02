package it.fast4x.riplay.extensions.crashreporter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.R

class CrashReporterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE) ?: "No stack trace available"

        setContent {
            MaterialTheme {
                CrashScreen(stackTrace = stackTrace)
            }
        }
    }

    companion object {
        const val EXTRA_STACK_TRACE = "extra_stack_trace"

        fun start(context: Context, stackTrace: String) {
            val intent = Intent(context, CrashReporterActivity::class.java).apply {
                putExtra(EXTRA_STACK_TRACE, stackTrace)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashScreen(stackTrace: String) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.crash_title_ops_riplay_is_in_error_state)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.crash_subtitle_the_application_crashed_unexpectedly_you_can_help_us_by_sending_this_log),
                style = MaterialTheme.typography.bodyLarge
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = stackTrace,
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(scrollState),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Pulsanti Azione
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pulsante Copia
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Crash Log", stackTrace)
                        clipboard.setPrimaryClip(clip)
                        SmartMessage(context.resources.getString(R.string.value_copied), context = context)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(painter = painterResource(R.drawable.copy), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.copy_crash_log_to_clipboard))
                }

                // Pulsante Condividi
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Log Crash App")
                            putExtra(Intent.EXTRA_TEXT, stackTrace)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Send crash report"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(painter = painterResource(R.drawable.share_social), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.share_with_external_app))
                }
            }

            Button(
                onClick = {
                    android.os.Process.killProcess(android.os.Process.myPid())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(stringResource(R.string.click_to_close))
            }
        }
    }
}