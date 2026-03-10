package com.example.to_do_list

data class Task(
    val title: String,
    val status: String = "À faire",
    val priority: String = "Basse",
    val periodicity: String ="Aucune",
    val dueDateMillis: Long? = null,
    val photoUri: String? = null



)