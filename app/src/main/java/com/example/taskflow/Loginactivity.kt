package com.example.taskflow
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.example.taskflow.databinding.ActivityLoginBinding

class LoginActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // « Se souvenir de moi » : connexion automatique si la case était cochée
        if (uid != -1 && prefs.getBoolean("remember", false) && db.user(uid) != null) { goHome(); return }

        val b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        // Pré-remplit l'e-mail mémorisé (SharedPreferences)
        b.cbRemember.isChecked = prefs.getBoolean("remember", false)
        if (b.cbRemember.isChecked) b.etEmail.setText(prefs.getString("email", ""))

        b.btnLogin.setOnClickListener {
            val email = b.etEmail.text.toString().trim()
            val pass = b.etPassword.text.toString()

            // Validation affichée directement sous les champs
            b.tilEmail.error = if (email.isEmpty()) "Entrez votre e-mail" else null
            b.tilPassword.error = if (pass.isEmpty()) "Entrez votre mot de passe" else null
            if (email.isEmpty() || pass.isEmpty()) return@setOnClickListener

            val u = db.login(email, pass)
            if (u == null) {
                Toast.makeText(this, "E-mail ou mot de passe incorrect", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Session + préférence « Se souvenir de moi » (on ne garde que l'e-mail, pas le mot de passe)
            prefs.edit()
                .putInt("uid", u.id)
                .putString("role", u.role)
                .putBoolean("remember", b.cbRemember.isChecked)
                .putString("email", if (b.cbRemember.isChecked) email else "")
                .apply()
            Toast.makeText(this, "Bienvenue, ${u.name}", Toast.LENGTH_SHORT).show()
            goHome()
        }
    }

    private fun goHome() {
        startActivity(Intent(this, if (isAdmin) AdminDashboardActivity::class.java else UserHomeActivity::class.java))
        finish()
    }
}