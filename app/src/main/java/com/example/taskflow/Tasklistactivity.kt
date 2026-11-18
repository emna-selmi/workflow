package com.example.taskflow

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.databinding.ActivityTaskListBinding
import com.example.taskflow.databinding.DialogNewTaskBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate

class TaskListActivity : BaseActivity() {
    private lateinit var b: ActivityTaskListBinding
    private val adapter = SimpleAdapter(emptyList<Task>(), R.layout.item_task) { v, t -> bindTask(v, t, ::openTask) }
    private var status: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTaskListBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.list.layoutManager = LinearLayoutManager(this)
        b.list.adapter = adapter
        b.tvTitle.text = if (isAdmin) "Tâches" else "Mes tâches"
        b.btnNew.visibility = if (isAdmin) View.VISIBLE else View.GONE
        b.btnNew.setOnClickListener { newTaskDialog() }
        b.chips.setOnCheckedStateChangeListener { _, ids ->
            status = when (ids.firstOrNull()) {
                R.id.chipTodo -> "todo"; R.id.chipDoing -> "doing"; R.id.chipReview -> "review"; R.id.chipDone -> "done"; else -> null
            }
            load()
        }
        nav(b.nav, R.id.nav_tasks)
    }

    override fun onResume() { super.onResume(); load() }

    private fun load() = adapter.submit(db.tasks(if (isAdmin) null else uid, status))

    /** Création + assignation d'une tâche (admin). */
    private fun newTaskDialog() {
        val d = DialogNewTaskBinding.inflate(layoutInflater)
        val users = db.users()
        if (users.isEmpty()) {
            Toast.makeText(this, "Créez d’abord un membre depuis le tableau de bord", Toast.LENGTH_LONG).show()
            return
        }
        d.spUser.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, users.map { it.name })
        d.spPriority.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, PRIORITY.values.toList())
        MaterialAlertDialogBuilder(this).setTitle("Nouvelle tâche").setView(d.root)
            .setPositiveButton("Créer") { _, _ ->
                val title = d.etTitle.text.toString().trim()
                if (title.isEmpty()) return@setPositiveButton
                val dueText = d.etDue.text.toString().trim().ifBlank { LocalDate.now().plusDays(7).toString() }
                val due = runCatching { LocalDate.parse(dueText) }.getOrNull()
                if (due == null || due.isBefore(LocalDate.now())) {
                    Toast.makeText(this, "Date invalide : aujourd’hui ou plus tard (AAAA-MM-JJ)", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val priority = PRIORITY.keys.toList()[d.spPriority.selectedItemPosition]
                db.addTask(title, "", priority, due.toString(), users[d.spUser.selectedItemPosition].id)
                // Démo locale : une vraie app enverrait la notification à l'appareil de l'utilisateur (serveur / FCM)
                Notifier.show(this, "Nouvelle tâche assignée", title)
                load()
            }
            .setNegativeButton("Annuler", null).show()
    }
}