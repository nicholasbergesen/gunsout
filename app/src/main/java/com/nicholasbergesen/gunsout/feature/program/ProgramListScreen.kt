package com.nicholasbergesen.gunsout.feature.program

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nicholasbergesen.gunsout.ui.components.MockupScreenColumn
import com.nicholasbergesen.gunsout.ui.components.ScreenTitle
import com.nicholasbergesen.gunsout.ui.components.StatusChip
import com.nicholasbergesen.gunsout.ui.components.ThemedCard
import com.nicholasbergesen.gunsout.ui.components.WrappingRow

@Composable
fun ProgramListScreen(
    onEdit: (Long) -> Unit,
    vm: ProgramListViewModel = hiltViewModel()
) {
    val programs by vm.programs.collectAsState()
    var newDialogOpen by remember { mutableStateOf(false) }

    MockupScreenColumn(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ScreenTitle("Programs")
            ProgramActionButton("New", Icons.Filled.Add, emphasized = true, onClick = { newDialogOpen = true })
        }

        if (programs.isEmpty()) {
            ThemedCard { Text("No programs yet. Create one to get started.") }
        }

        programs.forEach { program ->
            ThemedCard(accent = program.isActive) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(program.name, fontWeight = FontWeight.Bold)
                    if (program.isActive) StatusChip("Active", selected = true)
                }
                program.notes?.let { Text(it) }
                WrappingRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProgramActionButton("Edit", Icons.Filled.Edit, onClick = { onEdit(program.id) })
                    if (!program.isActive) {
                        ProgramActionButton(
                            "Activate",
                            Icons.Filled.PlayArrow,
                            emphasized = true,
                            onClick = { vm.activate(program.id) }
                        )
                    }
                    ProgramActionButton(
                        "Duplicate",
                        Icons.Filled.ContentCopy,
                        onClick = { vm.duplicate(program.id, "${program.name} (copy)") }
                    )
                    if (!program.isActive && !program.isTemplate) {
                        ProgramActionButton(
                            "Delete",
                            Icons.Filled.Delete,
                            destructive = true,
                            onClick = { vm.delete(program.id) }
                        )
                    }
                }
            }
        }
    }

    if (newDialogOpen) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newDialogOpen = false },
            title = { Text("New program") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        vm.createBlank(name) { id ->
                            newDialogOpen = false
                            onEdit(id)
                        }
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { newDialogOpen = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProgramActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    emphasized: Boolean = false,
    destructive: Boolean = false
) {
    val shape = MaterialTheme.shapes.small
    if (emphasized) {
        Button(onClick = onClick, shape = shape) {
            ProgramActionButtonContent(text, icon)
        }
    } else {
        val contentColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        OutlinedButton(
            onClick = onClick,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
            border = BorderStroke(1.dp, contentColor.copy(alpha = 0.7f))
        ) {
            ProgramActionButtonContent(text, icon)
        }
    }
}

@Composable
private fun ProgramActionButtonContent(text: String, icon: ImageVector) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    Text(text)
}
