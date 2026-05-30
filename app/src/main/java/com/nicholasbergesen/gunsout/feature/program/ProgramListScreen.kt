package com.nicholasbergesen.gunsout.feature.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nicholasbergesen.gunsout.ui.components.ChipButton
import com.nicholasbergesen.gunsout.ui.components.MockupScreenColumn
import com.nicholasbergesen.gunsout.ui.components.ScreenTitle
import com.nicholasbergesen.gunsout.ui.components.StatusChip
import com.nicholasbergesen.gunsout.ui.components.ThemedCard

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
            ChipButton("+ New", selected = true, onClick = { newDialogOpen = true })
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChipButton("Edit", onClick = { onEdit(program.id) })
                    if (!program.isActive) {
                        ChipButton("Activate", onClick = { vm.activate(program.id) })
                    }
                    ChipButton("Duplicate", onClick = { vm.duplicate(program.id, "${program.name} (copy)") })
                    if (!program.isActive && !program.isTemplate) {
                        ChipButton("Delete", onClick = { vm.delete(program.id) })
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
