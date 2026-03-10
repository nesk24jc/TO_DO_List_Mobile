package com.example.to_do_list

import androidx.compose.material.icons.filled.Delete
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
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
import androidx.compose.ui.platform.LocalContext

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
    val context = LocalContext.current
    val taskStorage = remember { TaskStorage(context) }

    var showAddScreen by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var myTasks by remember { mutableStateOf(taskStorage.loadTasks()) }
    var currentFilter by remember { mutableStateOf("Toutes") }


    var showWowEffect by remember { mutableStateOf(false) }


    LaunchedEffect(myTasks) {
        val overdueCount = myTasks.count { it.status == "En retard" }
        if (overdueCount > 0) {

            Toast.makeText(context, "⚠️ Alerte : Vous avez $overdueCount tâche(s) en retard !", Toast.LENGTH_LONG).show()
        }
    }


    if (showWowEffect) {
        LaunchedEffect(Unit) {

            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            }

            delay(2000)
            showWowEffect = false
        }


        Box(modifier = Modifier.fillMaxSize().zIndex(1f), contentAlignment = Alignment.Center) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                elevation = CardDefaults.cardElevation(10.dp)
            ) {
                Text(
                    text = "🎉 TÂCHE ACCOMPLIE ! 🎉\nBravo, continuez comme ça !",
                    modifier = Modifier.padding(32.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
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
            if (showAddScreen || taskToEdit != null) {
                TaskFormScreen(
                    initialTitle = taskToEdit?.title ?: "",
                    initialPriority = taskToEdit?.priority ?: "Basse",
                    initialPeriodicity = taskToEdit?.periodicity ?: "Aucune", // NOUVEAU
                    onSave = { newTitle, newPriority, newPeriodicity -> // NOUVEAU
                        val updatedList = if (taskToEdit == null) {
                            myTasks + Task(title = newTitle, priority = newPriority, periodicity = newPeriodicity)
                        } else {
                            myTasks.map {
                                if (it == taskToEdit) it.copy(title = newTitle, priority = newPriority, periodicity = newPeriodicity) else it
                            }
                        }
                        myTasks = updatedList
                        taskStorage.saveTasks(updatedList)
                        showAddScreen = false
                        taskToEdit = null
                    },
                    onCancel = { showAddScreen = false; taskToEdit = null }
                )
            } else {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterButton("Toutes", currentFilter) { currentFilter = it }
                        FilterButton("À faire", currentFilter) { currentFilter = it }
                        FilterButton("Réalisée", currentFilter) { currentFilter = it }
                        // NOUVEAU : On ajoute le bouton de filtre pour les tâches en retard !
                        FilterButton("En retard", currentFilter) { currentFilter = it }
                    }

                    val filteredTasks = if (currentFilter == "Toutes") {
                        myTasks
                    } else {
                        myTasks.filter { it.status == currentFilter }
                    }

                    TaskListScreen(
                        tasks = filteredTasks,
                        onTaskUpdated = { taskToUpdate, newStatus ->
                            var updatedList = myTasks.map {
                                if (it == taskToUpdate) it.copy(status = newStatus) else it
                            }

                            // NOUVEAU : LA MAGIE DE LA PÉRIODICITÉ
                            if (newStatus == "Réalisée") {
                                showWowEffect = true // On garde l'effet Waouh !

                                // Si la tâche doit se répéter, on crée un clone en mode "À faire"
                                if (taskToUpdate.periodicity != "Aucune") {
                                    val nextTask = Task(
                                        title = taskToUpdate.title,
                                        priority = taskToUpdate.priority,
                                        periodicity = taskToUpdate.periodicity,
                                        status = "À faire"
                                    )
                                    // On ajoute le clone à la liste
                                    updatedList = updatedList + nextTask
                                    Toast.makeText(context, "Tâche périodique recréée !", Toast.LENGTH_SHORT).show()
                                }
                            }

                            myTasks = updatedList
                            taskStorage.saveTasks(updatedList)
                        },
                        onEditClicked = { task -> taskToEdit = task },
                        onDeleteClicked = { taskToDelete ->
                            val updatedList = myTasks.filter { it != taskToDelete }
                            myTasks = updatedList
                            taskStorage.saveTasks(updatedList)
                        }


                    )
                }
            }
        }
    }
}

@Composable
fun TaskListScreen(
    tasks: List<Task>,
    onTaskUpdated: (Task, String) -> Unit,
    onEditClicked: (Task) -> Unit,
    onDeleteClicked: (Task) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(tasks) { task ->
            TaskItem(
                task = task,
                onStatusChanged = { isChecked ->
                    val newStatus = if (isChecked) "Réalisée" else "À faire"
                    onTaskUpdated(task, newStatus)
                },
                onEditClicked = { onEditClicked(task) },
                onDeleteClicked = { onDeleteClicked(task) }
            )
        }
    }
}

@Composable
fun TaskItem(task: Task, onStatusChanged: (Boolean) -> Unit, onEditClicked: () -> Unit, onDeleteClicked: () -> Unit) {
    val priorityColor = when(task.priority){
        "Haute" -> MaterialTheme.colorScheme.error
        "Moyenne" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }


    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.titleLarge)
                Text(text = "État: ${task.status}", color = MaterialTheme.colorScheme.primary)
                Text(text = "Priorité: ${task.priority}", color = priorityColor, style = MaterialTheme.typography.labelLarge)
                Text(text = "Répétition: ${task.periodicity}", style = MaterialTheme.typography.labelSmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {

                IconButton(onClick = onEditClicked) {
                    Icon(Icons.Filled.Edit, contentDescription = "Modifier")
                }


                IconButton(onClick = onDeleteClicked) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                }


                Checkbox(
                    checked = task.status == "Réalisée",
                    onCheckedChange = { isChecked -> onStatusChanged(isChecked) }
                )
            }
        }
    }
}


@Composable
fun TaskFormScreen(
    initialTitle: String,
    initialPriority: String = "Basse",
    initialPeriodicity: String = "Aucune",
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var priority by remember { mutableStateOf(initialPriority) }
    var periodicity by remember { mutableStateOf(initialPeriodicity) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = if (initialTitle.isEmpty()) "Nouvelle tâche" else "Modifier la tâche",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titre de la tâche") },
            modifier = Modifier.fillMaxWidth()
        )


        Spacer(Modifier.height(16.dp))
        Text("Priorité :", style = MaterialTheme.typography.bodyLarge)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            FilterButton("Basse", priority) { priority = it }
            FilterButton("Moyenne", priority) { priority = it }
            FilterButton("Haute", priority) { priority = it }
        }


        Spacer(Modifier.height(16.dp))
        Text("Répéter :", style = MaterialTheme.typography.bodyLarge)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            FilterButton("Aucune", periodicity) { periodicity = it }
            FilterButton("Jour", periodicity) { periodicity = it }
            FilterButton("Semaine", periodicity) { periodicity = it }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel) { Text("Annuler") }

            Button(onClick = { if (title.isNotBlank()) onSave(title, priority, periodicity) }) {
                Text("Sauvegarder")
            }
        }
    }
}
@Composable
fun FilterButton(text: String, currentFilter: String, onClick: (String) -> Unit) {
    Button(
        onClick = { onClick(text) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (currentFilter == text) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        )
    ) {
        Text(text)
    }
}