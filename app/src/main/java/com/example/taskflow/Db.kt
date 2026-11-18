package com.example.taskflow

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate

data class User(val id: Int, val name: String, val email: String, val role: String)
data class Task(val id: Int, val title: String, val description: String, val priority: String,
                val status: String, val progress: Int, val dueDate: String, val assignedTo: Int, val assignee: String)
data class Notif(val id: Int, val message: String, val createdAt: String, val read: Boolean)
data class Comment(val id: Int, val author: String, val text: String, val createdAt: String)

/** Une tâche non terminée dont l'échéance est passée est en retard : elle est bloquée. */
fun Task.isOverdue(): Boolean =
    status != "done" && runCatching { LocalDate.parse(dueDate).isBefore(LocalDate.now()) }.getOrDefault(false)

val STATUS = linkedMapOf("todo" to "À faire", "doing" to "En cours", "review" to "En revue", "done" to "Terminée")
val PRIORITY = linkedMapOf("urgent" to "Urgente", "high" to "Haute", "medium" to "Moyenne", "low" to "Basse")

class Db(ctx: Context) : SQLiteOpenHelper(ctx, "taskflow.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE users(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT UNIQUE, password TEXT, role TEXT)")
        db.execSQL("""CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, description TEXT,
            priority TEXT, status TEXT DEFAULT 'todo', progress INTEGER DEFAULT 0, due_date TEXT,
            assigned_to INTEGER REFERENCES users(id),
            reminded INTEGER DEFAULT 0, expired_notified INTEGER DEFAULT 0)""")
        db.execSQL("""CREATE TABLE notifications(id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER,
            message TEXT, created_at TEXT DEFAULT CURRENT_TIMESTAMP, is_read INTEGER DEFAULT 0)""")

        db.execSQL("""CREATE TABLE comments(id INTEGER PRIMARY KEY AUTOINCREMENT, task_id INTEGER REFERENCES tasks(id),
            user_id INTEGER REFERENCES users(id), text TEXT, created_at TEXT DEFAULT CURRENT_TIMESTAMP)""")

        // Données de démonstration (mots de passe en clair : démo uniquement)
        db.execSQL("""INSERT INTO users(name,email,password,role) VALUES
            ('Admin','admin@taskflow.com','admin123','admin'),
            ('Sarra B.','sarra@taskflow.com','1234','user'),
            ('Karim M.','karim@taskflow.com','1234','user')""")
        db.execSQL("""INSERT INTO tasks(title,description,priority,status,progress,due_date,assigned_to) VALUES
            ('Écran de connexion','Formulaire e-mail et mot de passe','urgent','doing',75,date('now','+1 day'),2),
            ('Base de données SQLite','Tables users, tasks, notifications','high','doing',40,date('now','+3 day'),3),
            ('Système de notifications','Notifier à l’assignation','medium','todo',10,date('now','+10 day'),3),
            ('Charte graphique','Couleurs et logo','low','done',100,date('now','-2 day'),2),
            ('Page profil','Écran profil utilisateur','low','todo',0,date('now','+14 day'),2)""")
        db.execSQL("INSERT INTO notifications(user_id,message) VALUES (2,'Nouvelle tâche assignée : Écran de connexion')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
        if (oldV < 2) {  // ajoute les colonnes de suivi des notifications d'échéance
            db.execSQL("ALTER TABLE tasks ADD COLUMN reminded INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE tasks ADD COLUMN expired_notified INTEGER DEFAULT 0")
        }
        if (oldV < 3) {  // ajoute la table des commentaires
            db.execSQL("""CREATE TABLE comments(id INTEGER PRIMARY KEY AUTOINCREMENT, task_id INTEGER REFERENCES tasks(id),
                user_id INTEGER REFERENCES users(id), text TEXT, created_at TEXT DEFAULT CURRENT_TIMESTAMP)""")
        }
    }

    private fun <T> query(sql: String, args: Array<String> = emptyArray(), map: (Cursor) -> T): List<T> =
        readableDatabase.rawQuery(sql, args).use { c ->
            val out = mutableListOf<T>()
            while (c.moveToNext()) out += map(c)
            out
        }

    private fun toUser(c: Cursor) = User(c.getInt(0), c.getString(1), c.getString(2), c.getString(3))

    // ---------- Utilisateurs ----------
    fun login(email: String, pass: String): User? =
        query("SELECT id,name,email,role FROM users WHERE email=? AND password=?", arrayOf(email, pass), ::toUser).firstOrNull()

    fun user(id: Int): User? = query("SELECT id,name,email,role FROM users WHERE id=?", arrayOf("$id"), ::toUser).firstOrNull()

    fun users(): List<User> = query("SELECT id,name,email,role FROM users WHERE role='user'", map = ::toUser)

    /** Crée un compte utilisateur (admin). Retourne false si l'e-mail existe déjà. */
    fun addUser(name: String, email: String, password: String): Boolean =
        writableDatabase.insert("users", null, ContentValues().apply {
            put("name", name); put("email", email); put("password", password); put("role", "user")
        }) != -1L

    // ---------- Tâches ----------
    fun tasks(userId: Int? = null, status: String? = null, extra: String? = null): List<Task> {
        val where = mutableListOf("1=1")
        val args = mutableListOf<String>()
        userId?.let { where += "t.assigned_to=?"; args += "$it" }
        status?.let { where += "t.status=?"; args += it }
        extra?.let { where += it }
        return query(
            """SELECT t.id,t.title,t.description,t.priority,t.status,t.progress,t.due_date,t.assigned_to,u.name
               FROM tasks t JOIN users u ON u.id=t.assigned_to WHERE ${where.joinToString(" AND ")}
               ORDER BY CASE t.priority WHEN 'urgent' THEN 0 WHEN 'high' THEN 1 WHEN 'medium' THEN 2 ELSE 3 END, t.due_date""",
            args.toTypedArray()
        ) { Task(it.getInt(0), it.getString(1), it.getString(2), it.getString(3), it.getString(4),
            it.getInt(5), it.getString(6), it.getInt(7), it.getString(8)) }
    }

    fun task(id: Int): Task? = tasks(extra = "t.id=$id").firstOrNull()

    fun addTask(title: String, desc: String, priority: String, due: String, assignedTo: Int) {
        writableDatabase.insert("tasks", null, ContentValues().apply {
            put("title", title); put("description", desc); put("priority", priority)
            put("due_date", due); put("assigned_to", assignedTo)
        })
        addNotif(assignedTo, "Nouvelle tâche assignée : $title")
    }

    fun updateTask(id: Int, status: String, progress: Int) {
        writableDatabase.update("tasks", ContentValues().apply { put("status", status); put("progress", progress) }, "id=?", arrayOf("$id"))
    }

    // ---------- Échéances ----------
    fun markReminded(id: Int) = writableDatabase.execSQL("UPDATE tasks SET reminded=1 WHERE id=?", arrayOf<Any>(id))
    fun markExpired(id: Int) = writableDatabase.execSQL("UPDATE tasks SET expired_notified=1 WHERE id=?", arrayOf<Any>(id))

    /** Réactivation par l'admin : nouvelle échéance, et les notifications repartent à zéro. */
    fun reactivate(id: Int, newDate: String) =
        writableDatabase.execSQL("UPDATE tasks SET due_date=?, reminded=0, expired_notified=0 WHERE id=?", arrayOf<Any>(newDate, id))

    fun adminIds(): List<Int> = query("SELECT id FROM users WHERE role='admin'") { it.getInt(0) }

    // ---------- Commentaires ----------
    /** Ajoute un commentaire et notifie l'assigné et les admins (sauf l'auteur). */
    fun addComment(taskId: Int, userId: Int, text: String) {
        writableDatabase.insert("comments", null, ContentValues().apply {
            put("task_id", taskId); put("user_id", userId); put("text", text)
        })
        val t = task(taskId) ?: return
        val author = user(userId)?.name ?: return
        val preview = if (text.length > 60) text.take(60) + "…" else text
        (adminIds() + t.assignedTo).distinct().filter { it != userId }
            .forEach { addNotif(it, "$author a commenté « ${t.title} » : $preview") }
    }

    fun comments(taskId: Int): List<Comment> =
        query("SELECT c.id,u.name,c.text,datetime(c.created_at,'localtime') FROM comments c JOIN users u ON u.id=c.user_id WHERE c.task_id=? ORDER BY c.id",
            arrayOf("$taskId")) { Comment(it.getInt(0), it.getString(1), it.getString(2), it.getString(3)) }

    // ---------- Notifications ----------
    fun addNotif(userId: Int, msg: String) {
        writableDatabase.insert("notifications", null, ContentValues().apply { put("user_id", userId); put("message", msg) })
    }

    fun notifyAdmins(msg: String) =
        writableDatabase.execSQL("INSERT INTO notifications(user_id,message) SELECT id,? FROM users WHERE role='admin'", arrayOf<Any>(msg))

    fun notifs(userId: Int): List<Notif> =
        query("SELECT id,message,datetime(created_at,'localtime'),is_read FROM notifications WHERE user_id=? ORDER BY id DESC", arrayOf("$userId"))
        { Notif(it.getInt(0), it.getString(1), it.getString(2), it.getInt(3) == 1) }

    fun markAllRead(userId: Int) =
        writableDatabase.execSQL("UPDATE notifications SET is_read=1 WHERE user_id=?", arrayOf<Any>(userId))
}