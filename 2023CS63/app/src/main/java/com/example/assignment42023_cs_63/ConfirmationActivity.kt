package com.example.assignment42023_cs_63

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ConfirmationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)

        val tvName = findViewById<TextView>(R.id.tvConfirmName)
        val tvPhone = findViewById<TextView>(R.id.tvConfirmPhone)
        val tvEmail = findViewById<TextView>(R.id.tvConfirmEmail)
        val tvType = findViewById<TextView>(R.id.tvConfirmType)
        val tvDate = findViewById<TextView>(R.id.tvConfirmDate)
        val tvTime = findViewById<TextView>(R.id.tvConfirmTime)
        val tvGender = findViewById<TextView>(R.id.tvConfirmGender)
        val btnBackHome = findViewById<Button>(R.id.btnBackHome)

        val name = intent.getStringExtra("NAME") ?: ""
        val phone = intent.getStringExtra("PHONE") ?: ""
        val email = intent.getStringExtra("EMAIL") ?: ""
        val type = intent.getStringExtra("TYPE") ?: ""
        val date = intent.getStringExtra("DATE") ?: ""
        val time = intent.getStringExtra("TIME") ?: ""
        val gender = intent.getStringExtra("GENDER") ?: ""

        tvName.text = getString(R.string.confirm_name, name)
        tvPhone.text = getString(R.string.confirm_phone, phone)
        tvEmail.text = getString(R.string.confirm_email, email)
        tvType.text = getString(R.string.confirm_type, type)
        tvDate.text = getString(R.string.confirm_date, date)
        tvTime.text = getString(R.string.confirm_time, time)
        tvGender.text = getString(R.string.confirm_gender, gender)

        btnBackHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}