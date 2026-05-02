package upload

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import java.awt.FileDialog

@Composable
fun UploadDialog(onUpload: (String) -> Unit) {
    val (visibility, setVisibility) = remember { mutableStateOf(false) }
    return try {
        onUpload(FileDialog(ComposeWindow(), "Choose file", FileDialog.LOAD).apply {
            isAlwaysOnTop = visibility
            isVisible = true
        }.run { "${directory ?: ""}${file ?: ""}" })
    } catch (e: Exception) {
        println(e.localizedMessage)
    } finally {
        setVisibility(false)
    }
}