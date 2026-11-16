package com.example.taskflow

import android.content.Intent
import android.os.Bundle
import com.example.taskflow.databinding.ActivityUserHomeBinding

class UserHomeActivity : BaseActivity() {
    private lateinit var b: ActivityUserHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityUserHomeBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.tvHello.text = "Bonjour, ${db.user(uid)?.name}"
        b.btnBell.setOnClickListener { startActivity(Intent(this, NotificationsActivity::class.java)) }
        b.btnAll.setOnClickListener { startActivity(Intent(this, TaskListActivity::class.java)) }
        nav(b.nav, R.id.nav_home)
    }

    override fun onResume() {
        super.onResume()
        val mine = db.tasks(uid)
        val done = mine.count { it.status == "done" }
        val pct = if (mine.isEmpty()) 0 else done * 100 / mine.size
        b.tvWeekPct.text = "$pct %"
        b.weekBar.progress = pct
        b.tvWeekInfo.text = "$done tâches terminées sur ${mine.size}"
        b.btnBell.text = "🔔 ${db.notifs(uid).count { !it.read }}"
        b.todayList.removeAllViews()
        mine.filter { it.status != "done" }.take(3).forEach { taskCard(b.todayList, it) }
    }
}