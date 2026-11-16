package com.example.taskflow

import android.content.Intent
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

open class BaseActivity : AppCompatActivity() {
    val db by lazy { Db(this) }
    val prefs by lazy { getSharedPreferences("session", MODE_PRIVATE) }
    val uid get() = prefs.getInt("uid", -1)
    val isAdmin get() = prefs.getString("role", "") == "admin"

    override fun onResume() { super.onResume(); DeadlineChecker.run(this) }

    fun openTask(t: Task) = startActivity(Intent(this, TaskDetailActivity::class.java).putExtra("id", t.id))

    /** Ajoute une carte tâche dans un conteneur. */
    fun taskCard(parent: ViewGroup, t: Task) {
        val v = layoutInflater.inflate(R.layout.item_task, parent, false)
        bindTask(v, t, ::openTask)
        parent.addView(v)
    }

    /** Barre de navigation du bas (Accueil / Tâches / Kanban / Profil). */
    fun nav(bar: BottomNavigationView, selected: Int) {
        bar.selectedItemId = selected
        bar.setOnItemSelectedListener { item ->
            if (item.itemId != selected) {
                val target = when (item.itemId) {
                    R.id.nav_home -> if (isAdmin) AdminDashboardActivity::class.java else UserHomeActivity::class.java
                    R.id.nav_tasks -> TaskListActivity::class.java
                    R.id.nav_kanban -> KanbanActivity::class.java
                    else -> ProfileActivity::class.java
                }
                startActivity(Intent(this, target))
                overridePendingTransition(0, 0)
                finish()
            }
            true
        }
    }
}