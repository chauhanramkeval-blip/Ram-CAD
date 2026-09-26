package com.example.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.CadBorderDark
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadSurfaceDark
import com.example.ui.theme.CadSurfaceVariantDark

/**
 * Storage permission helper for Android CAD file access.
 */
object CadPermissionHelper {

    /**
     * Determines whether the app has the appropriate external storage permissions.
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) uses SAF OpenDocument for general documents (DWG/DXF).
            // Check READ_EXTERNAL_STORAGE if granted or default to true for modern scoped storage
            val readGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            // On API 33+, Storage Access Framework provides permission via URI grants
            true
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // Android 9 and below: READ + WRITE external storage
            val read = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val write = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            read && write
        } else {
            // Android 10 - 12L (API 29-32)
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Required permissions array depending on Android OS version.
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }
}

/**
 * State holder for CAD storage permission requests.
 */
@Stable
class CadStoragePermissionState(
    val context: Context,
    val permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    val onDirectAction: () -> Unit
) {
    var showRationaleDialog by mutableStateOf(false)
    var showDeniedDialog by mutableStateOf(false)
    var pendingAction: (() -> Unit)? by mutableStateOf(null)

    fun requestAccess(action: () -> Unit) {
        pendingAction = action
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // On API 33+, open directly with SAF
            action()
        } else if (CadPermissionHelper.hasStoragePermission(context)) {
            action()
        } else {
            // Show rationale dialog before requesting runtime permission
            showRationaleDialog = true
        }
    }

    fun launchSystemPermissionRequest() {
        showRationaleDialog = false
        val perms = CadPermissionHelper.getRequiredPermissions()
        permissionLauncher.launch(perms)
    }

    fun onPermissionResult(results: Map<String, Boolean>) {
        val allGranted = results.values.all { it }
        if (allGranted || results.isEmpty()) {
            val action = pendingAction
            pendingAction = null
            action?.invoke()
        } else {
            showDeniedDialog = true
        }
    }
}

/**
 * Composable that manages storage permission request flows, rationale dialogs,
 * and error fallbacks for opening DWG/DXF CAD files from device storage.
 */
@Composable
fun rememberCadStoragePermissionState(
    onDirectAction: () -> Unit = {}
): CadStoragePermissionState {
    val context = LocalContext.current
    var stateRef by remember { mutableStateOf<CadStoragePermissionState?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        stateRef?.onPermissionResult(results)
    }

    val state = remember(context, permissionLauncher) {
        CadStoragePermissionState(
            context = context,
            permissionLauncher = permissionLauncher,
            onDirectAction = onDirectAction
        ).also { stateRef = it }
    }

    // 1. Rationale Dialog
    if (state.showRationaleDialog) {
        CadStorageRationaleDialog(
            onConfirm = {
                state.launchSystemPermissionRequest()
            },
            onDismiss = {
                state.showRationaleDialog = false
                state.pendingAction = null
            }
        )
    }

    // 2. Denied Dialog with option to open system settings or fallback to picker
    if (state.showDeniedDialog) {
        CadStorageDeniedDialog(
            onOpenSettings = {
                state.showDeniedDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            onProceedAnyway = {
                state.showDeniedDialog = false
                val action = state.pendingAction
                state.pendingAction = null
                action?.invoke()
            },
            onDismiss = {
                state.showDeniedDialog = false
                state.pendingAction = null
            }
        )
    }

    return state
}

/**
 * Material 3 Rationale Dialog explaining why storage permissions are needed for CAD files.
 */
@Composable
fun CadStorageRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("storage_permission_rationale_dialog"),
        containerColor = CadSurfaceDark,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CadCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FolderOpen,
                    contentDescription = "Device Storage",
                    tint = CadCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Storage Permission Required",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "CAD Mobile Viewer needs storage permission to browse, read, and open AutoCAD .DWG and .DXF drawings directly from your device storage and SD card.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = CadSurfaceVariantDark),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = CadCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Read AutoCAD .DWG & .DXF drawings",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = CadCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Export PDF, SVG & DXF to device storage",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LockOpen,
                                contentDescription = null,
                                tint = CadCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "100% Offline & Private (no cloud upload)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = CadCyan
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                modifier = Modifier.testTag("grant_storage_permission_btn")
            ) {
                Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_storage_permission_btn")
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

/**
 * Dialog shown when storage permission was denied by the user.
 */
@Composable
fun CadStorageDeniedDialog(
    onOpenSettings: () -> Unit,
    onProceedAnyway: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("storage_permission_denied_dialog"),
        containerColor = CadSurfaceDark,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Permission Denied",
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Storage Permission Denied",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Storage permission was not granted. Without it, CAD Mobile cannot directly scan your device storage for DWG/DXF files.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "You can still use the Android system file picker to select a single drawing, or enable permissions in App Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                modifier = Modifier.testTag("open_app_settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("App Settings", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onProceedAnyway,
                    modifier = Modifier.testTag("use_system_picker_btn")
                ) {
                    Text("Use File Picker", color = CadCyan)
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_permission_denied_btn")
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}
