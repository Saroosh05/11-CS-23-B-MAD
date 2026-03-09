package com.example.a2023_cs_67

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ConfirmationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)

        val nameText = findViewById<TextView>(R.id.nameText)
        val phoneText = findViewById<TextView>(R.id.phoneText)
        val emailText = findViewById<TextView>(R.id.emailText)
        val typeText = findViewById<TextView>(R.id.typeText)
        val dateText = findViewById<TextView>(R.id.dateText)
        val timeText = findViewById<TextView>(R.id.timeText)
        val genderText = findViewById<TextView>(R.id.genderText)
        val homeBtn = findViewById<Button>(R.id.homeBtn)

        nameText.text = "Name: ${intent.getStringExtra("name")}"
        phoneText.text = "Phone: ${intent.getStringExtra("phone")}"
        emailText.text = "Email: ${intent.getStringExtra("email")}"
        typeText.text = "Type: ${intent.getStringExtra("type")}"
        dateText.text = "Date: ${intent.getStringExtra("date")}"
        timeText.text = "Time: ${intent.getStringExtra("time")}"
        genderText.text = "Gender: ${intent.getStringExtra("gender")}"

        homeBtn.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(i)
            finish()
        }
    }
}
