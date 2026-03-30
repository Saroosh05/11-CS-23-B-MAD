package com.example.assignment42023_cs_63

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class BookAppointmentActivity : AppCompatActivity() {

    private var selectedDate: String = ""
    private var selectedTime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_appointment)

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val spAppointmentType = findViewById<Spinner>(R.id.spAppointmentType)
        val btnPickDate = findViewById<Button>(R.id.btnPickDate)
        val tvSelectedDate = findViewById<TextView>(R.id.tvSelectedDate)
        val btnPickTime = findViewById<Button>(R.id.btnPickTime)
        val tvSelectedTime = findViewById<TextView>(R.id.tvSelectedTime)
        val rgGender = findViewById<RadioGroup>(R.id.rgGender)
        val cbTerms = findViewById<CheckBox>(R.id.cbTerms)
        val btnConfirmBooking = findViewById<Button>(R.id.btnConfirmBooking)

        btnPickDate.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(this, { _, year, monthOfYear, dayOfMonth ->
                selectedDate = "$dayOfMonth/${monthOfYear + 1}/$year"
                tvSelectedDate.text = getString(R.string.selected_date_label, selectedDate)
            }, year, month, day)
            dpd.show()
        }

        btnPickTime.setOnClickListener {
            val c = Calendar.getInstance()
            val hour = c.get(Calendar.HOUR_OF_DAY)
            val minute = c.get(Calendar.MINUTE)

            val tpd = TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                tvSelectedTime.text = getString(R.string.selected_time_label, selectedTime)
            }, hour, minute, true)
            tpd.show()
        }

        btnConfirmBooking.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val type = spAppointmentType.selectedItem.toString()
            val genderId = rgGender.checkedRadioButtonId
            val termsChecked = cbTerms.isChecked

            if (name.isEmpty()) {
                etFullName.error = getString(R.string.err_name)
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                etPhone.error = getString(R.string.err_phone)
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                etEmail.error = getString(R.string.err_email)
                return@setOnClickListener
            }
            if (selectedDate.isEmpty()) {
                Toast.makeText(this, R.string.err_date, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedTime.isEmpty()) {
                Toast.makeText(this, R.string.err_time, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (genderId == -1) {
                Toast.makeText(this, R.string.err_gender, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!termsChecked) {
                Toast.makeText(this, R.string.err_terms, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gender = findViewById<RadioButton>(genderId).text.toString()

            val intent = Intent(this, ConfirmationActivity::class.java).apply {
                putExtra("NAME", name)
                putExtra("PHONE", phone)
                putExtra("EMAIL", email)
                putExtra("TYPE", type)
                putExtra("DATE", selectedDate)
                putExtra("TIME", selectedTime)
                putExtra("GENDER", gender)
            }
            startActivity(intent)
        }
    }
}