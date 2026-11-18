package com.example.taskflow

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.taskflow.databinding.ActivityTaskDetailBinding
import com.example.taskflow.databinding.DialogReactivateBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate

class TaskDetailActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(b.root)

        val t = db.task(intent.getIntExtra("id", 0)) ?: run {
            Toast.makeText(this, "Tâche introuvable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        b.tvTitle.text = t.title
        b.tvDesc.text = t.description
        b.tvInfo.text = "Assignée à ${t.assignee}\nÉchéance : ${t.dueDate}"
        chip(b.tvPriority, PRIORITY[t.priority]!!, t.priority)

        val buttons = mapOf("todo" to R.id.btnTodo, "doing" to R.id.btnDoing, "review" to R.id.btnReview, "done" to R.id.btnDone)
        b.toggle.check(buttons[t.status]!!)
        b.slider.value = t.progress.toFloat()
        b.tvPct.text = "${t.progress} %"
        b.slider.addOnChangeListener { _, v, _ -> b.tvPct.text = "${v.toInt()} %" }

        // Échéance dépassée : tâche bloquée, seul l'admin peut la réactiver
        if (t.isOverdue()) {
            b.tvLocked.visibility = View.VISIBLE
            listOf(b.btnTodo, b.btnDoing, b.btnReview, b.btnDone).forEach { it.isEnabled = false }
            b.slider.isEnabled = false
            b.btnSave.isEnabled = false
            if (isAdmin) {
                b.btnReactivate.visibility = View.VISIBLE
                b.btnReactivate.setOnClickListener { reactivateDialog(t) }
            }
        }

        // Commentaires
        fun loadComments() {
            b.commentList.removeAllViews()
            val list = db.comments(t.id)
            b.tvNoComment.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            list.forEach { c ->
                val v = layoutInflater.inflate(R.layout.item_comment, b.commentList, false)
                v.findViewById<TextView>(R.id.tvAuthor).text = c.author
                v.findViewById<TextView>(R.id.tvTime).text = c.createdAt
                v.findViewById<TextView>(R.id.tvText).text = c.text
                b.commentList.addView(v)
            }
        }
        loadComments()
        b.btnSend.setOnClickListener {
            val text = b.etComment.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Écrivez un commentaire", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            db.addComment(t.id, uid, text)
            b.etComment.text.clear()
            loadComments()
        }

        b.btnBack.setOnClickListener { finish() }
        b.btnSave.setOnClickListener {
            val status = buttons.entries.first { it.value == b.toggle.checkedButtonId }.key
            val progress = if (status == "done") 100 else b.slider.value.toInt()
            db.updateTask(t.id, status, progress)
            if (status == "done" && t.status != "done") {
                if (isAdmin) db.addNotif(t.assignedTo, "« ${t.title} » a été marquée terminée par l’admin")
                else db.notifyAdmins("${t.assignee} a terminé « ${t.title} »")
            }
            finish()
        }
    }

    /** L'admin choisit une nouvelle échéance pour réactiver la tâche. */
    private fun reactivateDialog(t: Task) {
        val d = DialogReactivateBinding.inflate(layoutInflater)
        d.etDate.setText(LocalDate.now().plusDays(7).toString())

        MaterialAlertDialogBuilder(this).setTitle("Réactiver la tâche").setView(d.root)
            .setPositiveButton("Réactiver") { _, _ ->
                val date = runCatching { LocalDate.parse(d.etDate.text.toString().trim()) }.getOrNull()
                if (date == null || date.isBefore(LocalDate.now())) {
                    Toast.makeText(this, "Entrez une date valide, aujourd’hui ou plus tard (AAAA-MM-JJ)", Toast.LENGTH_LONG).show()
                } else {
                    db.reactivate(t.id, date.toString())
                    db.addNotif(t.assignedTo, "Tâche réactivée : « ${t.title} » — nouvelle échéance : $date")
                    Toast.makeText(this, "Tâche réactivée", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Annuler", null).show()
    }
}