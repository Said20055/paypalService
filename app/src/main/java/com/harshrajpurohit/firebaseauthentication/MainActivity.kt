package com.harshrajpurohit.firebaseauthentication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.harshrajpurohit.firebaseauthentication.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object{
        lateinit var auth: FirebaseAuth
        lateinit var userDB: FirebaseFirestore
    }
    private val handler = Handler(Looper.getMainLooper())
    private val interval: Long = 5000
    private val runnable = object : Runnable {
        override fun run() {
            fetchUserData() // Ваш метод, который нужно вызывать каждые 5 секунд
            handler.postDelayed(this, interval) // Повторный вызов через 5 секунд
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        userDB = FirebaseFirestore.getInstance()

        if(auth.currentUser == null){
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handler.post(runnable)

        fetchUserData()
        binding.signOut.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        binding.transferBtn.setOnClickListener {
            startActivity(Intent(this, TransferActivity::class.java))
            finish()
        }
    }



    private fun fetchUserData() {
        val userEmail = auth.currentUser?.email ?: return
        val userRef = userDB.collection("Users").document(userEmail)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val balance = document.getLong("balance")
                    binding.userBalance.text = "$balance USDT"
                    loadTransferHistory()

                } else {
                    Toast.makeText(this, "Документ не найден", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка загрузки данных: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadTransferHistory() {
        val userEmail = auth.currentUser?.email ?: return
        val userRef = userDB.collection("Users").document(userEmail).collection("history")

        userRef.get()
            .addOnSuccessListener { documents ->
                val historyList = mutableListOf<String>()
                for (document in documents) {
                    val targetEmail = document.getString("targetEmail") ?: "Unknown"
                    val amount = document.getLong("amount") ?: 0L
                    val timestamp = document.getLong("timestamp") ?: 0L
                    val isRecipient = document.getBoolean("isRecipient")

                    // Преобразование timestamp в читаемый формат
                    val formattedTime = formatTimestamp(timestamp)

                    var historyItem: String = ""
                    if (isRecipient != null) {
                        if (isRecipient) {
                            historyItem = "Перевод: +$amount USDT от $targetEmail\nВремя: $formattedTime"
                        } else {
                            historyItem = "Перевод: -$amount USDT на $targetEmail\nВремя: $formattedTime"
                        }
                    }

                    historyList.add(historyItem)
                }

                updateListView(historyList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка загрузки истории: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateListView(historyList: List<String>) {
        val listView = findViewById<ListView>(R.id.historyListView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historyList)
        listView.adapter = adapter
    }
    private fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val date = Date(timestamp)
        return dateFormat.format(date)
    }
    override fun onResume() {
        super.onResume()
        binding.userDetails.text = updateData()
    }

    private fun updateData(): String{
        return "${auth.currentUser?.email}"
    }
}