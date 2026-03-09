package com.example.practiceassignment

import android.os.Bundle
import android.widget.Button
import android.widget.SearchView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Connect widgets
        val textView = findViewById<TextView>(R.id.textView)
        val textClock = findViewById<TextClock>(R.id.textClock)
        val searchView = findViewById<SearchView>(R.id.Searchview)
        val inputMessage = findViewById<TextInputEditText>(R.id.textInputEditText) // You forgot ID in XML, give it one like: "
        val btnShow = findViewById<Button>(R.id.button)

        // Button click logic
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Attention")
        builder.setMessage("Do you want to continue?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            Toast.makeText(this, "You clicked Yes", Toast.LENGTH_SHORT).show()
            dialog.dismiss() // closes dialog
        }
        builder.setNegativeButton("No") { dialog, _ ->
            Toast.makeText(this, "You clicked No", Toast.LENGTH_SHORT).show()
            dialog.dismiss() // closes dialog
        }
        builder.setCancelable(true) // allows user to tap outside to dismiss
        val dialog = builder.create()
        dialog.show()

        // Optional: handle SearchView query text change
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Toast.makeText(this@MainActivity, "Search: $query", Toast.LENGTH_SHORT).show()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Can update UI as user types
                return true
            }
        })

        // Edge-to-edge layout padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}