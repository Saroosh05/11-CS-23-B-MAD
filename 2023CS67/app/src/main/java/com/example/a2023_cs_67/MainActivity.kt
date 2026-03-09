package com.example.a2023_cs_67

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bookBtn = findViewById<Button>(R.id.bookBtn)

        bookBtn.setOnClickListener {
            val intent = Intent(this, BookAppointmentActivity::class.java)
            startActivity(intent)
        }
    }
}