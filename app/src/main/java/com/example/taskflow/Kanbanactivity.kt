package com.example.taskflow

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.taskflow.databinding.ActivityKanbanBinding

class KanbanActivity : BaseActivity() {
    private lateinit var b: ActivityKanbanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityKanbanBinding.inflate(layoutInflater)
        setContentView(b.root)
        nav(b.nav, R.id.nav_kanban)
    }

    override fun onResume() {
        super.onResume()
        b.board.removeAllViews()
        val tasks = db.tasks(if (isAdmin) null else uid)
        val px = { n: Int -> (n * resources.displayMetrics.density).toInt() }
        STATUS.forEach { (key, label) ->
            val inColumn = tasks.filter { it.status == key }
            val cards = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            inColumn.forEach { taskCard(cards, it) }
            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#ECEEF6"))
                setPadding(px(10), px(10), px(10), px(10))
                addView(TextView(context).apply { text = "$label · ${inColumn.size}"; setTypeface(null, Typeface.BOLD) })
                addView(ScrollView(context).apply { addView(cards) })
            }
            b.board.addView(column, LinearLayout.LayoutParams(px(270), MATCH_PARENT).apply { setMargins(px(6), 0, px(6), 0) })
        }
    }
}