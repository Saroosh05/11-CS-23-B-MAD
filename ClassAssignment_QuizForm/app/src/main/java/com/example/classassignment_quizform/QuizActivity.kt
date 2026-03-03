package com.example.classassignment_quizform

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class QuizActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quiz)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val username = intent.getStringExtra("USERNAME") ?: "User"
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        tvWelcome.text = "Welcome, $username!"

        val rg1 = findViewById<RadioGroup>(R.id.rgQuestion1)
        val rg2 = findViewById<RadioGroup>(R.id.rgQuestion2)
        val rg3 = findViewById<RadioGroup>(R.id.rgQuestion3)
        val rg4 = findViewById<RadioGroup>(R.id.rgQuestion4)
        val rg5 = findViewById<RadioGroup>(R.id.rgQuestion5)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            if (rg1.checkedRadioButtonId == -1 || rg2.checkedRadioButtonId == -1 ||
                rg3.checkedRadioButtonId == -1 || rg4.checkedRadioButtonId == -1 ||
                rg5.checkedRadioButtonId == -1
            ) {
                Toast.makeText(this, "Please answer all questions", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var score = 0

            // Q1: Capital of France -> B) Paris
            if (rg1.checkedRadioButtonId == R.id.q1b) score++
            // Q2: Red Planet -> C) Mars
            if (rg2.checkedRadioButtonId == R.id.q2c) score++
            // Q3: Largest ocean -> C) Pacific Ocean
            if (rg3.checkedRadioButtonId == R.id.q3c) score++
            // Q4: Continents -> C) 7
            if (rg4.checkedRadioButtonId == R.id.q4c) score++
            // Q5: Mona Lisa -> B) Leonardo da Vinci
            if (rg5.checkedRadioButtonId == R.id.q5b) score++

            val intent = Intent(this, ResultActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("SCORE", score)
            startActivity(intent)
            finish()
        }
    }
}
