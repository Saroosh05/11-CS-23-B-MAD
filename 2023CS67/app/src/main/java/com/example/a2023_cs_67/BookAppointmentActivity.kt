package com.example.a2023_cs_67

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class BookAppointmentActivity : AppCompatActivity() {

    lateinit var name: EditText
    lateinit var phone: EditText
    lateinit var email: EditText
    lateinit var spinner: Spinner
    lateinit var dateBtn: Button
    lateinit var timeBtn: Button
    lateinit var confirmBtn: Button
    lateinit var terms: CheckBox
    lateinit var genderGroup: RadioGroup

    var selectedDate = ""
    var selectedTime = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_appointment)

        name = findViewById(R.id.name)
        phone = findViewById(R.id.phone)
        email = findViewById(R.id.email)
        spinner = findViewById(R.id.spinnerType)
        dateBtn = findViewById(R.id.dateBtn)
        timeBtn = findViewById(R.id.timeBtn)
        confirmBtn = findViewById(R.id.confirmBtn)
        terms = findViewById(R.id.terms)
        genderGroup = findViewById(R.id.genderGroup)

        val types = arrayOf(
            "Doctor Consultation",
            "Dentist Appointment",
            "Eye Specialist",
            "Skin Specialist",
            "General Checkup"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spinner.adapter = adapter

        dateBtn.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this,
                { _, y, m, d ->
                    selectedDate = "$d/${m+1}/$y"
                    dateBtn.text = selectedDate
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        timeBtn.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this,
                { _, h, m ->
                    selectedTime = "$h:$m"
                    timeBtn.text = selectedTime
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        confirmBtn.setOnClickListener {

            if(name.text.isEmpty()){
                name.error="Enter Name"
                return@setOnClickListener
            }

            if(phone.text.isEmpty()){
                phone.error="Enter Phone"
                return@setOnClickListener
            }

            if(email.text.isEmpty()){
                email.error="Enter Email"
                return@setOnClickListener
            }

            if(!terms.isChecked){
                Toast.makeText(this,"Accept Terms",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val genderId = genderGroup.checkedRadioButtonId
            val gender = findViewById<RadioButton>(genderId).text.toString()

            val intent = Intent(this, ConfirmationActivity::class.java)

            intent.putExtra("name",name.text.toString())
            intent.putExtra("phone",phone.text.toString())
            intent.putExtra("email",email.text.toString())
            intent.putExtra("type",spinner.selectedItem.toString())
            intent.putExtra("date",selectedDate)
            intent.putExtra("time",selectedTime)
            intent.putExtra("gender",gender)

            startActivity(intent)
        }
    }
}