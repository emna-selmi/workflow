package com.example.taskflow

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.example.taskflow.databinding.ActivityAdminDashboardBinding
import com.example.taskflow.databinding.DialogNewUserBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.time.LocalDate

class AdminDashboardActivity : BaseActivity() {
    private lateinit var b: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.btnBell.setOnClickListener { startActivity(Intent(this, NotificationsActivity::class.java)) }
        b.btnAddUser.setOnClickListener { newUserDialog() }
        nav(b.nav, R.id.nav_home)
    }

    override fun onResume() { super.onResume(); load() }

    private fun load() {
        val all = db.tasks()
        val today = LocalDate.now().toString()
        b.tvTotal.text = "${all.size}"
        b.tvDoing.text = "${all.count { it.status == "doing" }}"
        b.tvDone.text = "${all.count { it.status == "done" }}"
        b.tvLate.text = "${all.count { it.status != "done" && it.dueDate < today }}"
        val pct = if (all.isEmpty()) 0 else all.sumOf { it.progress } / all.size
        b.tvGlobalPct.text = "$pct %"
        b.globalBar.progress = pct
        b.btnBell.text = "🔔 ${db.notifs(uid).count { !it.read }}"

        // Charge par membre
        b.teamList.removeAllViews()
        db.users().forEach { u ->
            val mine = all.filter { it.assignedTo == u.id }
            val row = layoutInflater.inflate(R.layout.item_member, b.teamList, false)
            row.findViewById<TextView>(R.id.tvMember).text = u.name
            row.findViewById<TextView>(R.id.tvCount).text = "${mine.size}"
            row.findViewById<LinearProgressIndicator>(R.id.memberBar).progress =
                if (mine.isEmpty()) 0 else mine.sumOf { it.progress } / mine.size
            b.teamList.addView(row)
        }

        // Tâches à traiter en priorité
        b.urgentList.removeAllViews()
        all.filter { it.status != "done" && (it.priority == "urgent" || it.dueDate < today) }
            .forEach { taskCard(b.urgentList, it) }
    }

    /** Création d'un compte utilisateur par l'admin. */
    private fun newUserDialog() {
        val d = DialogNewUserBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(this).setTitle("Nouveau membre").setView(d.root)
            .setPositiveButton("Créer") { _, _ ->
                val name = d.etName.text.toString().trim()
                val email = d.etEmail.text.toString().trim()
                val pass = d.etPassword.text.toString()
                when {
                    name.isEmpty() || email.isEmpty() || pass.length < 4 ->
                        Toast.makeText(this, "Remplissez tous les champs (mot de passe : 4 caractères minimum)", Toast.LENGTH_LONG).show()
                    !db.addUser(name, email, pass) ->
                        Toast.makeText(this, "Cet e-mail existe déjà", Toast.LENGTH_SHORT).show()
                    else -> {
                        Toast.makeText(this, "Compte créé pour $name", Toast.LENGTH_SHORT).show()
                        load()
                    }
                }
            }
            .setNegativeButton("Annuler", null).show()
    }
}