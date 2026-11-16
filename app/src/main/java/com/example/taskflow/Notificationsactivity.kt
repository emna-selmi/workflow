package com.example.taskflow

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.example.taskflow.databinding.ActivityNotificationsBinding

class NotificationsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(b.root)

        // ListView + adaptateur personnalisé : chaque ligne utilise item_notification.xml
        val adapter = object : ArrayAdapter<Notif>(this, R.layout.item_notification, db.notifs(uid)) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = convertView ?: layoutInflater.inflate(R.layout.item_notification, parent, false)
                val n = getItem(position)!!
                v.findViewById<TextView>(R.id.tvMsg).apply {
                    text = n.message
                    setTypeface(null, if (n.read) Typeface.NORMAL else Typeface.BOLD)  // non lue = en gras
                }
                v.findViewById<TextView>(R.id.tvTime).text = n.createdAt
                return v
            }
        }
        b.list.adapter = adapter

        b.btnBack.setOnClickListener { finish() }
        b.btnReadAll.setOnClickListener {
            db.markAllRead(uid)
            adapter.clear()
            adapter.addAll(db.notifs(uid))
            Toast.makeText(this, "Notifications marquées comme lues", Toast.LENGTH_SHORT).show()
        }
    }
}