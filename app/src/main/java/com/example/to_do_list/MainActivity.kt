package com.example.to_do_list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TodoApp()
            }
        }
    }
}

@Composable
fun TodoApp() {
    var showAddScreen by remember { mutableStateOf(false) }
    // NOUVEAU : Une variable pour mémoriser la tâche qu'on est en train de modifier
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    var mockTasks by remember {
        mutableStateOf(
            listOf(
                Task("Créer le diagramme de classes", "À faire"),
                Task("Trouver un nom extraordinairement vendeur", "Réalisée")
            )
        )
    }

    Scaffold(
        floatingActionButton = {
            if (!showAddScreen && taskToEdit == null) {
                FloatingActionButton(onClick = { showAddScreen = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Ajouter")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (showAddScreen) {

                AddTaskScreen(
                    onTaskAdded = { title ->
                        // On ajoute la tâche à notre fausse liste
                        mockTasks = mockTasks + Task(title)
                        // On retourne à la liste
                        showAddScreen = false
                    },
                    onCancel = { showAddScreen = false }
                )
            } else {

                TaskListScreen(mockTasks)
            }
        }
    }
}

@Composable
fun TaskListScreen(tasks: List<Task>) {

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(tasks) { task ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = task.title, style = MaterialTheme.typography.titleLarge)
                    Text(text = "État: ${task.status}", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun AddTaskScreen(onTaskAdded: (String) -> Unit, onCancel: () -> Unit) {
    var title by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Nouvelle tâche", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titre de la tâche") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel) { Text("Annuler") }
            Button(onClick = { if (title.isNotBlank()) onSave(title) }) {
                Text("Sauvegarder")
            }
        }
    }
}