package com.example.to_do_list

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class TaskStorage(context: Context) {

    private val prefs = context.getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)



    fun saveTasks(tasks: List<Task>) {
        val jsonArray = JSONArray()
        for (task in tasks) {
            val jsonObject = JSONObject()
            jsonObject.put("title", task.title)
            jsonObject.put("status", task.status)
            jsonObject.put("priority", task.priority)
            jsonObject.put("periodicity", task.periodicity)

            if (task.dueDateMillis != null) {
                jsonObject.put("dueDateMillis", task.dueDateMillis)
            }
            jsonArray.put(jsonObject)
        }
        prefs.edit().putString("tasks_data", jsonArray.toString()).apply()
    }


    fun loadTasks(): List<Task> {
        val tasksList = mutableListOf<Task>()
        val jsonString = prefs.getString("tasks_data", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            tasksList.add(
                Task(
                    title = jsonObject.getString("title"),
                    status = jsonObject.getString("status"),
                    priority = if (jsonObject.has("priority")) jsonObject.getString("priority") else "Basse",
                    periodicity = if (jsonObject.has("periodicity")) jsonObject.getString("periodicity") else "Aucune",

                    dueDateMillis = if (jsonObject.has("dueDateMillis")) jsonObject.getLong("dueDateMillis") else null
                )
            )
        }
        return tasksList
    }



    fun saveScore(score: Int) {
        prefs.edit().putInt("user_score", score).apply()
    }


    fun getScore(): Int {
        return prefs.getInt("user_score", 0)
    }
}