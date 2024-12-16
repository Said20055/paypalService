package com.harshrajpurohit.firebaseauthentication

import android.content.Intent
import android.content.SyncAdapterType
import android.health.connect.datatypes.units.Length
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.harshrajpurohit.firebaseauthentication.MainActivity.Companion.auth
import com.harshrajpurohit.firebaseauthentication.MainActivity.Companion.userDB
import com.harshrajpurohit.firebaseauthentication.databinding.ActivityMainBinding
import com.harshrajpurohit.firebaseauthentication.databinding.ActivityTransferBinding

class TransferActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransferBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = ActivityTransferBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fetchUserData()

        binding.returnBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        binding.complite.setOnClickListener{
            var value = binding.valueForTransfer.text
            var targetEmail = binding.emailOtherUser.text
            if (value != null && targetEmail != null) {
                if(value.isNotEmpty() && targetEmail.isNotEmpty()){
                    Transfer(value.toString().toInt(), targetEmail.toString())

                }
            }
        }

    }

    private fun Transfer(value: Int, targetEmail: String) {
        val userEmail = auth.currentUser?.email ?: return
        val userRef = userDB.collection("Users").document(userEmail)
        val targetRef = userDB.collection("Users").document(targetEmail)

        userRef.get().addOnSuccessListener { userDoc ->
            if (userDoc != null && userDoc.exists()) {
                val balanceDoc = userDoc.getLong("balance") ?: 0L
                val balance = balanceDoc.toInt()

                if (balance >= value) {
                    // Обновляем баланс отправителя
                    val newBalance = balance - value
                    userRef.update("balance", newBalance.toLong())
                        .addOnSuccessListener {
                            // Обновляем баланс получателя
                            targetRef.get().addOnSuccessListener { targetDoc ->
                                if (targetDoc != null && targetDoc.exists()) {
                                    val targetBalanceDoc = targetDoc.getLong("balance") ?: 0L
                                    val targetNewBalance = targetBalanceDoc + value
                                    targetRef.update("balance", targetNewBalance)
                                        .addOnSuccessListener {
                                            // Добавляем запись в историю переводов отправителя
                                            addTransferHistory(userEmail, targetEmail, value, false)

                                            // Добавляем запись в историю переводов получателя
                                            addTransferHistory(targetEmail, userEmail, value, true)
                                            fetchUserData()
                                            Toast.makeText(this, "Перевод успешно выполнен!", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            println("Ошибка при обновлении баланса отправителя: ${e.message}")
                        }
                } else {
                    Toast.makeText(this, "Недостаточно средств", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { e ->
            println("Ошибка при получении данных пользователя: ${e.message}")
        }
    }

    private fun addTransferHistory(userEmail: String, targetEmail: String, amount: Int, isRecipient: Boolean) {
        val userRef = userDB.collection("Users").document(userEmail).collection("history").document()
        val timestamp = System.currentTimeMillis()

        val historyEntry = hashMapOf(
            "targetEmail" to targetEmail,
            "amount" to amount,
            "timestamp" to timestamp,
            "isRecipient" to isRecipient
        )

        userRef.set(historyEntry)
            .addOnSuccessListener {
                println("История перевода успешно добавлена для $userEmail")
            }
            .addOnFailureListener { e ->
                println("Ошибка при добавлении истории перевода: ${e.message}")
            }
    }

    private fun fetchUserData() {
        val userEmail = auth.currentUser?.email ?: return
        val userRef = userDB.collection("Users").document(userEmail)


        userRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val balance = document.getLong("balance")
                    binding.balance.text = "$balance USDT"
                } else {
                    Toast.makeText(this, "Документ не найден", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка загрузки данных: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }



}