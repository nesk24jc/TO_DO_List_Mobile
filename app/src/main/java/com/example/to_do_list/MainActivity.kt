package com.example.to_do_list

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay


val AppBackground = Color(0xFFF3F4F6)
val CardBackground = Color(0xFFFFFFFF)
val PrimaryAccent = Color(0xFF6366F1)
val TextMain = Color(0xFF1F2937)
val TextSub = Color(0xFF6B7280)
val PriorityHigh = Color(0xFFEF4444)
val PriorityMed = Color(0xFFF59E0B)
val PriorityLow = Color(0xFF10B981)

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
            Toast.makeText(context, "⚠️ Vous avez $overdueCount tâche(s) en retard !", Toast.LENGTH_LONG).show()
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
                colors = CardDefaults.cardColors(containerColor = PrimaryAccent),
                elevation = CardDefaults.cardElevation(20.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "🎉 EXCELLENT ! 🎉\nTâche accomplie avec succès.",
                    modifier = Modifier.padding(32.dp),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                if (!showAddScreen && taskToEdit == null) {
                    FloatingActionButton(
                        onClick = { showAddScreen = true },
                        shape = CircleShape,
                        containerColor = PrimaryAccent,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter", modifier = Modifier.size(28.dp))
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (showAddScreen || taskToEdit != null) {
                    TaskFormScreen(
                        initialTitle = taskToEdit?.title ?: "",
                        initialPriority = taskToEdit?.priority ?: "Basse",
                        initialPeriodicity = taskToEdit?.periodicity ?: "Aucune",
                        onSave = { newTitle, newPriority, newPeriodicity ->
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
                    Column(modifier = Modifier.fillMaxSize()) {

                        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 16.dp)) {
                            Text(
                                text = "Mes Tâches",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain
                            )
                            val remainingCount = myTasks.count { it.status != "Réalisée" }
                            Text(
                                text = if (remainingCount == 0) "Tout est terminé ! ✨" else "$remainingCount tâche(s) en attente",
                                fontSize = 16.sp,
                                color = TextSub
                            )
                        }


                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filters = listOf("Toutes", "À faire", "Réalisée", "En retard")
                            items(filters) { filterName ->
                                PremiumFilterChip(
                                    text = filterName,
                                    isSelected = currentFilter == filterName,
                                    onClick = { currentFilter = filterName }
                                )
                            }
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

                                if (newStatus == "Réalisée") {
                                    showWowEffect = true
                                    if (taskToUpdate.periodicity != "Aucune") {
                                        val nextTask = Task(
                                            title = taskToUpdate.title,
                                            priority = taskToUpdate.priority,
                                            periodicity = taskToUpdate.periodicity,
                                            status = "À faire"
                                        )
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
}

@Composable
fun TaskListScreen(
    tasks: List<Task>,
    onTaskUpdated: (Task, String) -> Unit,
    onEditClicked: (Task) -> Unit,
    onDeleteClicked: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 80.dp), // Espace en bas pour le bouton
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tasks) { task ->
            PremiumTaskCard(
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
fun PremiumTaskCard(task: Task, onStatusChanged: (Boolean) -> Unit, onEditClicked: () -> Unit, onDeleteClicked: () -> Unit) {
    val isDone = task.status == "Réalisée"

    val priorityColor = when(task.priority) {
        "Haute" -> PriorityHigh
        "Moyenne" -> PriorityMed
        else -> PriorityLow
    }


    val cardBackgroundColor by animateColorAsState(
        targetValue = if (isDone) AppBackground else CardBackground,
        animationSpec = tween(durationMillis = 300), label = ""
    )

    val titleColor by animateColorAsState(
        targetValue = if (isDone) TextSub.copy(alpha = 0.6f) else TextMain,
        animationSpec = tween(durationMillis = 300), label = ""
    )

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onStatusChanged(!isDone) }, // On peut cliquer sur toute la carte !
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Case à cocher circulaire personnalisée
            IconButton(
                onClick = { onStatusChanged(!isDone) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Outlined.Check,
                    contentDescription = "Cocher",
                    tint = if (isDone) PrimaryAccent else TextSub.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))


            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 18.sp,
                    fontWeight = if (isDone) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                    color = titleColor
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Badge Priorité
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDone) Color.LightGray.copy(alpha = 0.3f) else priorityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.priority,
                            color = if (isDone) TextSub else priorityColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Badge Répétition
                    if (task.periodicity != "Aucune") {
                        Text(
                            text = "↻ ${task.periodicity}",
                            fontSize = 12.sp,
                            color = TextSub
                        )
                    }
                }
            }


            Row {
                if (!isDone) {
                    IconButton(onClick = onEditClicked, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = TextSub, modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(onClick = onDeleteClicked, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = PriorityHigh.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                }
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

    Column(modifier = Modifier.fillMaxSize().background(CardBackground).padding(24.dp)) {
        Text(
            text = if (initialTitle.isEmpty()) "Nouvelle tâche" else "Modifier la tâche",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Que devez-vous faire ?") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryAccent,
                unfocusedBorderColor = Color.LightGray
            ),
            singleLine = true
        )

        Spacer(Modifier.height(24.dp))
        Text("Priorité", fontWeight = FontWeight.SemiBold, color = TextMain)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PremiumFilterChip("Basse", priority == "Basse") { priority = it }
            PremiumFilterChip("Moyenne", priority == "Moyenne") { priority = it }
            PremiumFilterChip("Haute", priority == "Haute") { priority = it }
        }

        Spacer(Modifier.height(24.dp))
        Text("Répétition", fontWeight = FontWeight.SemiBold, color = TextMain)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PremiumFilterChip("Aucune", periodicity == "Aucune") { periodicity = it }
            PremiumFilterChip("Jour", periodicity == "Jour") { periodicity = it }
            PremiumFilterChip("Semaine", periodicity == "Semaine") { periodicity = it }
        }

        Spacer(modifier = Modifier.weight(1f)) 

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Annuler", color = TextSub)
            }
            Button(
                onClick = { if (title.isNotBlank()) onSave(title, priority, periodicity) },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Text("Enregistrer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
fun PremiumFilterChip(text: String, isSelected: Boolean, onClick: (String) -> Unit) {
    val bgColor by animateColorAsState(if (isSelected) PrimaryAccent else Color.Transparent, label = "")
    val textColor by animateColorAsState(if (isSelected) Color.White else TextSub, label = "")
    val borderColor by animateColorAsState(if (isSelected) PrimaryAccent else Color.LightGray, label = "")

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.clickable { onClick(text) }
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 14.sp
        )
    }
}