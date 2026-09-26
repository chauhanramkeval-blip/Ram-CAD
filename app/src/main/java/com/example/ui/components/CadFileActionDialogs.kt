package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cad.model.CadDrawing
import com.example.ui.theme.CadBorderDark
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadSurfaceDark

@Composable
fun CadRenameDialog(
    drawing: CadDrawing,
    onDismiss: () -> Unit,
    onConfirmRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(drawing.name.substringBeforeLast('.')) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("rename_dialog"),
        shape = RoundedCornerShape(16.dp),
        containerColor = CadSurfaceDark,
        title = {
            Text(
                text = "Rename Drawing",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter a new filename for this drawing:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_input"),
                    trailingIcon = {
                        Text(
                            text = ".${drawing.format.extension}",
                            style = MaterialTheme.typography.labelMedium,
                            color = CadCyan,
                            modifier = Modifier
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CadCyan,
                        unfocusedBorderColor = CadBorderDark
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onConfirmRename(newName.trim())
                    }
                },
                enabled = newName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_rename_btn")
            ) {
                Text("Rename", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}

@Composable
fun CadDeleteConfirmDialog(
    drawing: CadDrawing,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("delete_dialog"),
        shape = RoundedCornerShape(16.dp),
        containerColor = CadSurfaceDark,
        title = {
            Text(
                text = "Delete Drawing?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFFF5252)
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete '${drawing.name}'? This drawing file and any recovery data will be permanently removed from local offline storage.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_delete_btn")
            ) {
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}

@Composable
fun CadCreateProjectDialog(
    onDismiss: () -> Unit,
    onConfirmCreate: (name: String, description: String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("create_project_dialog"),
        shape = RoundedCornerShape(16.dp),
        containerColor = CadSurfaceDark,
        title = {
            Text(
                text = "Create New CAD Project",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Organize related CAD drawings, revisions, and site plans into a local project folder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    placeholder = { Text("e.g. Skyline Tower Phase 2") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CadCyan,
                        unfocusedBorderColor = CadBorderDark
                    )
                )

                OutlinedTextField(
                    value = projectDescription,
                    onValueChange = { projectDescription = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Structural framing and MEP coordination") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CadCyan,
                        unfocusedBorderColor = CadBorderDark
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (projectName.isNotBlank()) {
                        onConfirmCreate(projectName.trim(), projectDescription.trim())
                    }
                },
                enabled = projectName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_create_project_btn")
            ) {
                Text("Create Project", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}
