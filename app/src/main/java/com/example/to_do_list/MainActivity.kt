package com.example.to_do_list

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        setContent { MaterialTheme { TodoApp() } }
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
    var userScore by remember { mutableStateOf(taskStorage.getScore()) }


    var userStreak by remember { mutableStateOf(taskStorage.getStreak()) }


    val streakText = when {
        userStreak >= 30 -> "🔥🔥🔥 🔥"
        userStreak >= 10 -> "🔥🔥🔥"
        userStreak >= 3 -> "🔥🔥"
        userStreak >= 1 -> "🔥"
        else -> "⚪"
    }

    val userLevel = when {
        userScore >= 500 -> "👑 Maître du Temps"
        userScore >= 200 -> "🔥 Machine de Guerre"
        userScore >= 50 -> "🚀 Productif"
        else -> "🌱 Débutant"
    }


    LaunchedEffect(myTasks) {
        val currentTime = System.currentTimeMillis()
        var needsSave = false
        val updatedList = myTasks.map { task ->
            if (task.status == "À faire" && task.dueDateMillis != null && task.dueDateMillis < currentTime) {
                needsSave = true
                task.copy(status = "En retard")
            } else task
        }
        if (needsSave) {
            myTasks = updatedList
            taskStorage.saveTasks(updatedList)
        }
        val overdueCount = updatedList.count { it.status == "En retard" }
        if (overdueCount > 0) Toast.makeText(context, "⚠️ Vous avez $overdueCount tâche(s) en retard !", Toast.LENGTH_LONG).show()
    }

    if (showWowEffect) {
        LaunchedEffect(Unit) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                else @Suppress("DEPRECATION") vibrator.vibrate(500)
            }
            delay(2000)
            showWowEffect = false
        }
        Box(modifier = Modifier.fillMaxSize().zIndex(1f), contentAlignment = Alignment.Center) {
            Card(colors = CardDefaults.cardColors(containerColor = PrimaryAccent), elevation = CardDefaults.cardElevation(20.dp), shape = RoundedCornerShape(24.dp)) {
                Text("🎉 EXCELLENT ! 🎉\nTâche accomplie avec succès.", modifier = Modifier.padding(32.dp), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                if (!showAddScreen && taskToEdit == null) {
                    FloatingActionButton(onClick = { showAddScreen = true }, shape = CircleShape, containerColor = PrimaryAccent, contentColor = Color.White) {
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
                        initialDueDate = taskToEdit?.dueDateMillis,
                        initialPhotoUri = taskToEdit?.photoUri,
                        onSave = { newTitle, newPriority, newPeriodicity, newDueDate, newPhotoUri ->
                            val updatedList = if (taskToEdit == null) {
                                myTasks + Task(title = newTitle, priority = newPriority, periodicity = newPeriodicity, dueDateMillis = newDueDate, photoUri = newPhotoUri)
                            } else {
                                myTasks.map { if (it == taskToEdit) it.copy(title = newTitle, priority = newPriority, periodicity = newPeriodicity, dueDateMillis = newDueDate, photoUri = newPhotoUri) else it }
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
                            Text("Mes Tâches", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = TextMain)


                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "$userLevel • $userScore pts", fontSize = 16.sp, color = PrimaryAccent, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(12.dp))

                                Text(text = "$streakText $userStreak j", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if(userStreak > 0) PriorityMed else Color.Gray)
                            }
                        }

                        LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("Toutes", "À faire", "Réalisée", "En retard")) { filterName ->
                                PremiumFilterChip(text = filterName, isSelected = currentFilter == filterName) { currentFilter = filterName }
                            }
                        }

                        val filteredTasks = if (currentFilter == "Toutes") myTasks else myTasks.filter { it.status == currentFilter }

                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredTasks) { task ->
                                PremiumTaskCard(
                                    task = task,
                                    onStatusChanged = { isChecked ->
                                        val newStatus = if (isChecked) "Réalisée" else "À faire"
                                        var updatedList = myTasks.map { if (it == task) it.copy(status = newStatus) else it }

                                        if (newStatus == "Réalisée") {
                                            showWowEffect = true
                                            val pointsEarned = when(task.priority) { "Haute" -> 30; "Moyenne" -> 20; else -> 10 }
                                            userScore += pointsEarned
                                            taskStorage.saveScore(userScore)
                                            Toast.makeText(context, "+$pointsEarned points ! 🏆", Toast.LENGTH_SHORT).show()


                                            val currentTime = System.currentTimeMillis()
                                            val lastDay = taskStorage.getLastCompletionDay()
                                            val currentDay = (currentTime / 86400000)

                                            if (lastDay == currentDay) {

                                            } else if (lastDay == (currentDay - 1)) {

                                                userStreak += 1
                                                taskStorage.saveStreak(userStreak)
                                                taskStorage.saveLastCompletionDay(currentDay)
                                                Toast.makeText(context, "Série ! $userStreak jours d'affilée ! 🔥", Toast.LENGTH_SHORT).show()
                                            } else {

                                                userStreak = 1
                                                taskStorage.saveStreak(userStreak)
                                                taskStorage.saveLastCompletionDay(currentDay)
                                            }

                                            if (task.periodicity != "Aucune") {
                                                val nextTask = Task(title = task.title, priority = task.priority, periodicity = task.periodicity, status = "À faire", dueDateMillis = task.dueDateMillis?.plus(86400000), photoUri = task.photoUri)
                                                updatedList = updatedList + nextTask
                                            }
                                        } else {
                                            val pointsLost = when(task.priority) { "Haute" -> 30; "Moyenne" -> 20; else -> 10 }
                                            userScore = maxOf(0, userScore - pointsLost)
                                            taskStorage.saveScore(userScore)
                                        }
                                        myTasks = updatedList
                                        taskStorage.saveTasks(updatedList)
                                    },
                                    onEditClicked = { taskToEdit = task },
                                    onDeleteClicked = {
                                        val updatedList = myTasks.filter { it != task }
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
    }
}

@Composable
fun PremiumTaskCard(task: Task, onStatusChanged: (Boolean) -> Unit, onEditClicked: () -> Unit, onDeleteClicked: () -> Unit) {
    val isDone = task.status == "Réalisée"
    val isOverdue = task.status == "En retard"
    val priorityColor = when(task.priority) { "Haute" -> PriorityHigh; "Moyenne" -> PriorityMed; else -> PriorityLow }

    val cardBackgroundColor by animateColorAsState(if (isDone) AppBackground else CardBackground, tween(300), "")
    val titleColor by animateColorAsState(if (isDone) TextSub.copy(alpha = 0.6f) else TextMain, tween(300), "")

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onStatusChanged(!isDone) },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(if (isDone) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Column {
            // NOUVEAU : Affichage de la photo si elle existe
            if (task.photoUri != null && !isDone) {
                NativeImage(
                    uriString = task.photoUri,
                    modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                )
            }

            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onStatusChanged(!isDone) }, modifier = Modifier.size(32.dp)) {
                    if (isDone) {
                        Icon(Icons.Filled.CheckCircle, "Cochée", tint = PrimaryAccent, modifier = Modifier.size(28.dp))
                    } else {
                        Box(modifier = Modifier.size(24.dp).border(2.dp, if (isOverdue) PriorityHigh else TextSub.copy(alpha = 0.5f), CircleShape))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, fontSize = 18.sp, fontWeight = if (isDone) FontWeight.Normal else FontWeight.SemiBold, textDecoration = if (isDone) TextDecoration.LineThrough else null, color = titleColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isDone) Color.LightGray.copy(alpha = 0.3f) else priorityColor.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text(task.priority, color = if (isDone) TextSub else priorityColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (task.dueDateMillis != null) {
                            Text("📅 ${SimpleDateFormat("dd MMM", Locale.FRANCE).format(Date(task.dueDateMillis))}", fontSize = 12.sp, color = if(isOverdue && !isDone) PriorityHigh else TextSub)
                        }
                    }
                }

                Row {
                    if (!isDone) IconButton(onClick = onEditClicked, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Edit, "Modifier", tint = TextSub, modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = onDeleteClicked, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Delete, "Supprimer", tint = PriorityHigh.copy(alpha = 0.8f), modifier = Modifier.size(20.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
    initialTitle: String, initialPriority: String = "Basse", initialPeriodicity: String = "Aucune", initialDueDate: Long? = null, initialPhotoUri: String? = null,
    onSave: (String, String, String, Long?, String?) -> Unit, onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var priority by remember { mutableStateOf(initialPriority) }
    var periodicity by remember { mutableStateOf(initialPeriodicity) }
    var dueDate by remember { mutableStateOf(initialDueDate) }
    var showDatePicker by remember { mutableStateOf(false) }


    var photoUri by remember { mutableStateOf(initialPhotoUri) }
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {

                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { e.printStackTrace() }
            photoUri = uri.toString()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dueDate = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("Confirmer") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } }
        ) { DatePicker(state = datePickerState) }
    }

    Column(modifier = Modifier.fillMaxSize().background(CardBackground).padding(24.dp)) {
        Text(if (initialTitle.isEmpty()) "Nouvelle tâche" else "Modifier la tâche", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(value = title, onValueChange = { title = it }, placeholder = { Text("Que devez-vous faire ?") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true)
        Spacer(Modifier.height(16.dp))

        // --- BOUTONS DATE ET PHOTO ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Filled.DateRange, "Date", modifier = Modifier.padding(end = 4.dp).size(18.dp))
                Text(if (dueDate != null) SimpleDateFormat("dd MMM", Locale.FRANCE).format(Date(dueDate!!)) else "Date", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (photoUri != null) PrimaryAccent else TextMain)
            ) {
                Icon(Icons.Filled.Add, "Photo", modifier = Modifier.padding(end = 4.dp).size(18.dp))
                Text(if (photoUri != null) "Photo OK" else "Photo", fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(16.dp))


        if (photoUri != null) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(16.dp)).background(AppBackground)) {
                NativeImage(uriString = photoUri!!, modifier = Modifier.fillMaxSize())
                IconButton(onClick = { photoUri = null }, modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape).size(32.dp)) {
                    Icon(Icons.Filled.Delete, "Supprimer photo", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text("Priorité", fontWeight = FontWeight.SemiBold, color = TextMain)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PremiumFilterChip("Basse", priority == "Basse") { priority = it }
            PremiumFilterChip("Moyenne", priority == "Moyenne") { priority = it }
            PremiumFilterChip("Haute", priority == "Haute") { priority = it }
        }

        Spacer(Modifier.height(16.dp))
        Text("Répétition", fontWeight = FontWeight.SemiBold, color = TextMain)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PremiumFilterChip("Aucune", periodicity == "Aucune") { periodicity = it }
            PremiumFilterChip("Jour", periodicity == "Jour") { periodicity = it }
            PremiumFilterChip("Semaine", periodicity == "Semaine") { periodicity = it }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(16.dp)) { Text("Annuler", color = TextSub) }
            Button(onClick = { if (title.isNotBlank()) onSave(title, priority, periodicity, dueDate, photoUri) }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)) {
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
    Surface(shape = RoundedCornerShape(50), color = bgColor, border = androidx.compose.foundation.BorderStroke(1.dp, borderColor), modifier = Modifier.clickable { onClick(text) }) {
        Text(text, color = textColor, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 14.sp)
    }
}


@Composable
fun NativeImage(uriString: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(uriString) {
        try {
            val uri = Uri.parse(uriString)
            bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    if (bitmap != null) {
        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = "Photo jointe", modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier = modifier.background(Color.LightGray), contentAlignment = Alignment.Center) {
            Text("Chargement...", color = Color.Gray, fontSize = 12.sp)
        }
    }
}