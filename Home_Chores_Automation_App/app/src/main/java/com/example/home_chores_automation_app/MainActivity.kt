package com.example.home_chores_automation_app

import com.example.home_chores_automation_app.R

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.model.Member
import com.example.home_chores_automation_app.model.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var vm: MainViewModel
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var memberAdapter: MemberAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        vm = ViewModelProvider(this).get(MainViewModel::class.java)

        val membersRv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.membersList)
        val tasksRv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.tasksList)
        val fabAddMember = findViewById<FloatingActionButton>(R.id.fabAddMember)
        val fabAddTask = findViewById<FloatingActionButton>(R.id.fabAddTask)

        memberAdapter = MemberAdapter(emptyList())
        membersRv.adapter = memberAdapter
        membersRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        taskAdapter = TaskAdapter(emptyList(), emptyMap()) { id -> vm.toggleComplete(id) }
        tasksRv.adapter = taskAdapter
        tasksRv.layoutManager = LinearLayoutManager(this)

        vm.members.observe(this) { list ->
            memberAdapter.update(list)
            refreshTaskAdapter()
        }

        vm.tasks.observe(this) { list ->
            taskAdapter.update(list)
            refreshTaskAdapter()
        }

        fabAddMember.setOnClickListener { showAddMemberDialog() }
        fabAddTask.setOnClickListener { showAddTaskDialog() }
    }

    private fun refreshTaskAdapter() {
        val membersMap = vm.members.value?.associateBy({ it.id }, { it.name }) ?: emptyMap()
        taskAdapter = TaskAdapter(vm.tasks.value ?: emptyList(), membersMap) { id -> vm.toggleComplete(id) }
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.tasksList).adapter = taskAdapter
    }

    private fun showAddMemberDialog() {
        val input = EditText(this)
        input.hint = "Name"
        AlertDialog.Builder(this)
            .setTitle("Add family member")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) vm.addMember(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddTaskDialog() {
        val members = vm.members.value ?: emptyList()
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val titleInput = view.findViewById<EditText>(R.id.etTaskTitle)
        val assigneeSpinner = view.findViewById<Spinner>(R.id.spAssignee)
        val dueMinutes = view.findViewById<EditText>(R.id.etDueMinutes)

        val names = listOf("Unassigned") + members.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        assigneeSpinner.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Add task")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString().trim()
                val assigneePos = assigneeSpinner.selectedItemPosition
                val assigneeId = if (assigneePos <= 0) null else members[assigneePos - 1].id
                val minutes = dueMinutes.text.toString().toLongOrNull()
                val due = minutes?.let { Date(System.currentTimeMillis() + it * 60_000) }
                if (title.isNotEmpty()) {
                    vm.addTask(title, assigneeId, due)
                    due?.let { scheduleReminder(title, "Due now: $title", it) }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun scheduleReminder(title: String, text: String, whenDate: Date) {
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("text", text)
        }
        val pi = PendingIntent.getBroadcast(this, (System.currentTimeMillis() % 100000).toInt(), intent, PendingIntent.FLAG_IMMUTABLE)
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setExact(AlarmManager.RTC_WAKEUP, whenDate.time, pi)
    }
}
