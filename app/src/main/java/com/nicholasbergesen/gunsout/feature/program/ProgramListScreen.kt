package com.nicholasbergesen.gunsout.feature.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProgramListScreen(
    onEdit: (Long) -> Unit,
    vm: ProgramListViewModel = hiltViewModel()
) {
    val programs by vm.programs.collectAsState()
    var newDialogOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { newDialogOpen = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New program") }
            )
        }
    ) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Programs", style = MaterialTheme.typography.headlineMedium)

            if (programs.isEmpty()) {
                Text("No programs yet. Create one to get started.")
            }

            programs.forEach { program ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(program.name, style = MaterialTheme.typography.titleMedium)
                            if (program.isActive) AssistChip(onClick = {}, label = { Text("Active") })
                        }
                        program.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onEdit(program.id) }) { Text("Edit") }
                            if (!program.isActive) {
                                OutlinedButton(onClick = { vm.activate(program.id) }) { Text("Activate") }
                            }
                            TextButton(onClick = { vm.duplicate(program.id, "${program.name} (copy)") }) {
                                Text("Duplicate")
                            }
                            if (!program.isActive && !program.isTemplate) {
                                TextButton(onClick = { vm.delete(program.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (newDialogOpen) {
        var name by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { newDialogOpen = false },
            title = { Text("New program") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
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
