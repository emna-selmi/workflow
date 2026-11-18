package com.example.taskflow

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import com.example.taskflow.databinding.ActivityProfileBinding
import java.time.LocalDate

class ProfileActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(b.root)

        val u = db.user(uid) ?: run {
            startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            return
        }
        val mine = db.tasks(if (isAdmin) null else uid)
        val today = LocalDate.now().toString()
        b.tvAvatar.text = u.name.split(" ").filter { it.isNotBlank() }.map { it.first() }.joinToString("").take(2).uppercase()
        b.tvAvatar.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#4F46E5")) }
        b.tvName.text = u.name
        b.tvRole.text = "${u.email} · ${if (isAdmin) "Admin" else "Utilisateur"}"
        b.tvAssigned.text = "${mine.size}"
        b.tvDone.text = "${mine.count { it.status == "done" }}"
        b.tvLate.text = "${mine.count { it.status != "done" && it.dueDate < today }}"

        b.btnLogout.setOnClickListener {
            prefs.edit().remove("uid").remove("role").apply()  // garde l'e-mail mémorisé
            startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
        nav(b.nav, R.id.nav_profile)
    }
}