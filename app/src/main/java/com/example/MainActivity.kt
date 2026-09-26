package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.cad.engine.CadEngine
import com.example.cad.engine.DefaultCadEngine
import com.example.cad.model.CadFormat
import com.example.cad.repository.DrawingRepository
import com.example.cad.repository.RoomDrawingRepository
import com.example.ui.navigation.CadAppNavigation
import com.example.ui.theme.CadBackgroundDark
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    // Subsystem components instantiated at application scope
    private val cadEngine: CadEngine by lazy { DefaultCadEngine() }
    private val drawingRepository: DrawingRepository by lazy { RoomDrawingRepository(applicationContext) }

    private var incomingDrawingId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CadBackgroundDark
                ) {
                    CadMobileApp(
                        repository = drawingRepository,
                        cadEngine = cadEngine,
                        initialDrawingId = incomingDrawingId
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val uri: Uri? = intent?.data ?: intent?.getParcelableExtra(Intent.EXTRA_STREAM)
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    var displayName = "Opened_CAD_File.dxf"
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0) {
                                displayName = cursor.getString(nameIndex) ?: displayName
                            }
                        }
                    }

                    val isDwg = displayName.endsWith(".dwg", ignoreCase = true)
                    val format = if (isDwg) CadFormat.DWG else CadFormat.DXF

                    val importDir = File(filesDir, "imported_cad").apply { mkdirs() }
                    val sanitizedName = displayName.replace(Regex("""[^a-zA-Z0-9._-]"""), "_")
                    val targetFile = File(importDir, "${System.currentTimeMillis()}_$sanitizedName")

                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    } ?: return@launch

                    val parseResult = targetFile.inputStream().use { stream ->
                        cadEngine.parser.parse(stream, format, displayName)
                    }

                    if (parseResult.isSuccess) {
                        val doc = parseResult.getOrThrow()
                        val imported = drawingRepository.importDrawing(
                            name = displayName,
                            format = format,
                            filePath = targetFile.absolutePath,
                            sizeBytes = targetFile.length(),
                            entityCount = doc.entityCount,
                            layerCount = doc.layerCount,
                            units = doc.units
                        )
                        withContext(Dispatchers.Main) {
                            incomingDrawingId = imported.id
                        }
                    }
                } catch (_: Exception) {
                    // Graceful handling of invalid intent stream
                }
            }
        }
    }
}

@Composable
fun CadMobileApp(
    repository: DrawingRepository,
    cadEngine: CadEngine,
    initialDrawingId: String? = null,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    CadAppNavigation(
        navController = navController,
        repository = repository,
        cadEngine = cadEngine,
        initialDrawingId = initialDrawingId,
        modifier = modifier
    )
}

