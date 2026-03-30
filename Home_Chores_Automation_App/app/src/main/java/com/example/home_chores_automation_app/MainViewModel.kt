package com.example.home_chores_automation_app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.home_chores_automation_app.model.Member
import com.example.home_chores_automation_app.model.Task
import java.util.*

class MainViewModel : ViewModel() {
    private val _members = MutableLiveData<List<Member>>(emptyList())
    val members: LiveData<List<Member>> = _members

    private val _tasks = MutableLiveData<List<Task>>(emptyList())
    val tasks: LiveData<List<Task>> = _tasks

    private var nextMemberId = 1L
    private var nextTaskId = 1L

    fun addMember(name: String) {
        val m = Member(nextMemberId++, name)
        _members.value = _members.value!!.plus(m)
    }

    fun addTask(title: String, assigneeId: Long?, due: Date?) {
        val t = Task(nextTaskId++, title, assigneeId, due)
        _tasks.value = _tasks.value!!.plus(t)
    }

    fun toggleComplete(taskId: Long) {
        _tasks.value = _tasks.value!!.map { if (it.id == taskId) it.copy(completed = !it.completed) else it }
    }
}
