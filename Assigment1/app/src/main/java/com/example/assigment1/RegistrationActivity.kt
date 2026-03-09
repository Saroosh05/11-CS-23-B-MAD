package com.example.assigment1

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RegistrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registrationMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val rgGender = findViewById<RadioGroup>(R.id.rgGender)
        val cbSports = findViewById<CheckBox>(R.id.cbSports)
        val cbMusic = findViewById<CheckBox>(R.id.cbMusic)
        val cbReading = findViewById<CheckBox>(R.id.cbReading)
        val spinnerCity = findViewById<Spinner>(R.id.spinnerCity)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        val cities = arrayOf("Select City", "Lahore", "Karachi", "Islamabad", "Peshawar", "Quetta")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCity.adapter = adapter

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            val gender = when (rgGender.checkedRadioButtonId) {
                R.id.rbMale -> "Male"
                R.id.rbFemale -> "Female"
                else -> ""
            }

            val interests = mutableListOf<String>()
            if (cbSports.isChecked) interests.add("Sports")
            if (cbMusic.isChecked) interests.add("Music")
            if (cbReading.isChecked) interests.add("Reading")

            val city = spinnerCity.selectedItem.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || gender.isEmpty() || city == "Select City") {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val message = "Registered!\nName: $name\nGender: $gender\nCity: $city\nInterests: ${interests.joinToString(", ")}"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
