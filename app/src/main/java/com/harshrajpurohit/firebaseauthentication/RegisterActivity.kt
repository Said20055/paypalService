package com.harshrajpurohit.firebaseauthentication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.harshrajpurohit.firebaseauthentication.MainActivity.Companion.auth
import com.harshrajpurohit.firebaseauthentication.MainActivity.Companion.userDB
import com.harshrajpurohit.firebaseauthentication.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Button click to navigate to LoginActivity
        binding.loginTV.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Button click to create account
        binding.createAccountBtn.setOnClickListener {
            val email = binding.emailRegister.text.toString()
            val password = binding.passwordRegister.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                MainActivity.auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            saveUserData()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
                    }
            }
        }

        // Button click to sign in with Google
        binding.googleBtn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, 13)
        }
    }

    // Handle Google sign-in result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 13 && resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Authenticate with Firebase using the Google account
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        MainActivity.auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Authentication failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }
    private fun saveUserData() {
        val user = auth.currentUser ?: return
        val userRef = userDB.collection("Users").document(user.email ?: return) // email пользователя как имя документа

        // Данные пользователя (баланс и UID)
        val userData = hashMapOf(
            "balance" to 1000, // Пример начального баланса
            "uid" to user.uid
        )

        // Сохраняем данные о пользователе в Firestore
        userRef.set(userData)
            /*.addOnSuccessListener {
                // После успешного сохранения данных, добавляем коллекцию history
                val historyData = hashMapOf(
                    "targetEmail" to "initial", // Пример начального значения для первого элемента истории
                    "amount" to 0, // Это может быть 0, пока нет транзакций
                    "timestamp" to System.currentTimeMillis() // Время создания записи
                )

                // Добавляем первый элемент в коллекцию history
                userRef.collection("history").add(historyData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Документ пользователя и история успешно созданы", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Ошибка при добавлении истории: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

                Toast.makeText(this, "Данные успешно сохранены", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка при сохранении данных: ${e.message}", Toast.LENGTH_LONG).show()
            }*/
    }

}
