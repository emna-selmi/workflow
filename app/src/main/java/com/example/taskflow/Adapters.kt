package com.example.taskflow

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator

/** Adaptateur générique : un layout + une fonction qui remplit chaque ligne. */
class SimpleAdapter<T>(private var items: List<T>, private val layout: Int, private val bind: (View, T) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(p: ViewGroup, type: Int): RecyclerView.ViewHolder =
        object : RecyclerView.ViewHolder(LayoutInflater.from(p.context).inflate(layout, p, false)) {}
    override fun onBindViewHolder(h: RecyclerView.ViewHolder, i: Int) = bind(h.itemView, items[i])
    override fun getItemCount() = items.size
    fun submit(list: List<T>) { items = list; notifyDataSetChanged() }
}

/** Remplit une carte item_task.xml (rouge et bloquée si l'échéance est dépassée). */
fun bindTask(v: View, t: Task, onClick: (Task) -> Unit) {
    val late = t.isOverdue()
    v.findViewById<TextView>(R.id.tvTitle).text = t.title
    v.findViewById<TextView>(R.id.tvMeta).apply {
        text = if (late) "🔒 Bloquée · échéance dépassée (${t.dueDate}) · ${t.assignee}"
        else "Échéance : ${t.dueDate} · ${t.assignee}"
        setTextColor(Color.parseColor(if (late) "#A3202A" else "#5B6275"))
    }
    v.findViewById<TextView>(R.id.tvPct).text = "${t.progress} %"
    v.findViewById<LinearProgressIndicator>(R.id.bar).progress = t.progress
    chip(v.findViewById(R.id.tvPriority), PRIORITY[t.priority]!!, t.priority)
    (v as MaterialCardView).apply {
        setCardBackgroundColor(Color.parseColor(if (late) "#FDE8E9" else "#FFFFFF"))
        strokeColor = Color.parseColor(if (late) "#D9363E" else "#E6E8F0")
    }
    v.setOnClickListener { onClick(t) }
}

/** Pastille colorée selon la priorité. */
fun chip(tv: TextView, text: String, priority: String) {
    val (bg, fg) = when (priority) {
        "urgent" -> "#FDE8E9" to "#A3202A"
        "high" -> "#FEEBD6" to "#8F4806"
        "medium" -> "#E8EAFD" to "#3730A3"
        else -> "#E1F3EA" to "#1F6B49"
    }
    tv.text = text
    tv.setTextColor(Color.parseColor(fg))
    tv.background = GradientDrawable().apply { cornerRadius = 100f; setColor(Color.parseColor(bg)) }
}