package com.example.taskflow

import android.app.Application
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Vérifie les échéances : rappel la veille, et notification quand la date est dépassée. */
object DeadlineChecker {
    @Synchronized
    fun run(ctx: Context) {
        val db = Db(ctx)
        val sessionUid = ctx.getSharedPreferences("session", Context.MODE_PRIVATE).getInt("uid", -1)
        val today = LocalDate.now()

        // Enregistre la notification dans la liste, et l'affiche à l'écran si c'est l'utilisateur connecté
        fun send(userId: Int, title: String, msg: String) {
            db.addNotif(userId, msg)
            if (userId == sessionUid) Notifier.show(ctx, title, msg)
        }

        // 1) Échéance dépassée : la tâche est bloquée
        db.tasks(extra = "t.status<>'done' AND t.expired_notified=0").forEach { t ->
            val due = runCatching { LocalDate.parse(t.dueDate) }.getOrNull() ?: return@forEach
            if (due.isBefore(today)) {
                send(t.assignedTo, "Échéance dépassée", "« ${t.title} » est bloquée : l’échéance du ${t.dueDate} est dépassée.")
                db.adminIds().forEach {
                    send(it, "Tâche bloquée", "« ${t.title} » (${t.assignee}) a dépassé l’échéance. Vous pouvez la réactiver.")
                }
                db.markExpired(t.id)
            }
        }

        // 2) Rappel la veille de l'échéance : date limite + état de la tâche
        db.tasks(extra = "t.status<>'done' AND t.reminded=0").forEach { t ->
            val due = runCatching { LocalDate.parse(t.dueDate) }.getOrNull() ?: return@forEach
            if (!due.isBefore(today) && !due.isAfter(today.plusDays(1))) {
                send(t.assignedTo, "Rappel d’échéance",
                    "« ${t.title} » : échéance le ${t.dueDate} — état : ${STATUS[t.status]}, ${t.progress} %.")
                db.markReminded(t.id)
            }
        }
        db.close()
    }
}

/** Tâche de fond : lance la vérification même quand l'application est fermée. */
class DeadlineWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        DeadlineChecker.run(applicationContext)
        return Result.success()
    }
}

/** Planifie la vérification toutes les 15 minutes (minimum autorisé par Android). */
class TaskFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val request = PeriodicWorkRequestBuilder<DeadlineWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("deadlines", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}