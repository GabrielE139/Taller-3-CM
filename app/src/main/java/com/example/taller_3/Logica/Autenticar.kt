package com.example.taller_3.Logica

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taller_3.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Autenticar : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_autenticar)

        auth = Firebase.auth

        emailEditText = findViewById(R.id.editTextCorreo)
        passwordEditText = findViewById(R.id.editTextPassword)
        loginButton = findViewById(R.id.buttonLogin)

        loginButton.setOnClickListener {
            if (validateForm()) {
                if (isEmailValid(emailEditText.text.toString())) {
                    signIn(emailEditText.text.toString(), passwordEditText.text.toString())
                } else {
                    Toast.makeText(baseContext, "Correo invalido.", Toast.LENGTH_SHORT).show()
                    emailEditText.setText("")
                    passwordEditText.setText("")
                }

            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseLogin", "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Log.w("FirebaseLogin", "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val intent = Intent(this, Inicio::class.java)
            intent.putExtra("user", currentUser.email)
            startActivity(intent)
        } else {
            emailEditText.setText("")
            passwordEditText.setText("")
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        val email = emailEditText.text.toString()
        if (TextUtils.isEmpty(email)) {
            emailEditText.error = "Required."
            valid = false
        } else {
            emailEditText.error = null
        }
        val password = passwordEditText.text.toString()
        if (TextUtils.isEmpty(password)) {
            passwordEditText.error = "Required."
            valid = false
        } else {
            passwordEditText.error = null
        }
        return valid
    }

    private fun isEmailValid(email: String): Boolean {
        if (!email.contains("@") ||
            !email.contains(".") ||
            email.length < 5)
            return false
        return true
    }
}