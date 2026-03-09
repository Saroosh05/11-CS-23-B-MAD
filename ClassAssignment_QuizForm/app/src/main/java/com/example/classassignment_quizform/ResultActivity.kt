package com.example.classassignment_quizform

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_result)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val username = intent.getStringExtra("USERNAME") ?: "User"
        val score = intent.getIntExtra("SCORE", 0)

        val tvResultName = findViewById<TextView>(R.id.tvResultName)
        val tvScore = findViewById<TextView>(R.id.tvScore)
        val tvMessage = findViewById<TextView>(R.id.tvMessage)
        val btnPlayAgain = findViewById<Button>(R.id.btnPlayAgain)

        tvResultName.text = "Well done, $username!"
        tvScore.text = "$score / 5"

        tvMessage.text = when (score) {
            5 -> "Perfect! You got all answers correct!"
            4 -> "Great job! Almost perfect!"
            3 -> "Good effort! Keep learning!"
            2 -> "Not bad, but you can do better!"
            else -> "Keep trying, practice makes perfect!"
        }

        btnPlayAgain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}
