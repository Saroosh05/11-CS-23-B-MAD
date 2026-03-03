package com.example.assignment2

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val name = intent.getStringExtra("name") ?: ""
        val age = intent.getStringExtra("age") ?: ""
        val heightCm = intent.getDoubleExtra("height", 0.0)
        val weight = intent.getDoubleExtra("weight", 0.0)

        val heightM = heightCm / 100.0
        val bmi = weight / (heightM * heightM)

        findViewById<TextView>(R.id.tvName).text = "Name: $name"
        findViewById<TextView>(R.id.tvAge).text = "Age: $age"
        findViewById<TextView>(R.id.tvHeight).text = "Height: ${heightCm} cm"
        findViewById<TextView>(R.id.tvWeight).text = "Weight: ${weight} kg"

        val tvBMI = findViewById<TextView>(R.id.tvBMI)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        tvBMI.text = String.format("%.1f", bmi)

        when {
            bmi < 18.5 -> {
                tvBMI.setTextColor(Color.BLUE)
                tvStatus.text = "Underweight"
                tvStatus.setTextColor(Color.BLUE)
            }
            bmi in 18.5..24.9 -> {
                tvBMI.setTextColor(Color.parseColor("#4CAF50"))
                tvStatus.text = "Normal"
                tvStatus.setTextColor(Color.parseColor("#4CAF50"))
            }
            else -> {
                tvBMI.setTextColor(Color.RED)
                tvStatus.text = "Overweight"
                tvStatus.setTextColor(Color.RED)
            }
        }

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}
