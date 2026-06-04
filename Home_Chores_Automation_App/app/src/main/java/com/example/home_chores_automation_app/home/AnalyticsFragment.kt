package com.example.home_chores_automation_app.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.example.home_chores_automation_app.databinding.FragmentAnalyticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private val repo = FirebaseRepository.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupId = arguments?.getString("groupId") ?: return
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        viewLifecycleOwner.lifecycleScope.launch {
            val group = repo.getGroupById(groupId) ?: return@launch
            if (_binding == null) return@launch
            binding.tvGroupName.text = group.name

            val tasks = repo.getTasksForGroup(groupId)
            if (_binding == null) return@launch

            val now      = System.currentTimeMillis()
            val completed = tasks.filter { it.isCompleted }
            val pending   = tasks.filter { !it.isCompleted }
            val overdue   = tasks.count { it.dueDate > 0L && it.dueDate < now && !it.isCompleted }

            // Summary numbers
            binding.tvTotalTasks.text   = tasks.size.toString()
            binding.tvDoneTasks.text    = completed.size.toString()
            binding.tvOverdueTasks.text = overdue.toString()

            setupPieChart(completed.size, pending.size)
            setupBarChart(completed)
        }
    }

    // ── Pie chart: completed vs pending ──────────────────────────────────────

    private fun setupPieChart(done: Int, pending: Int) {
        if (_binding == null) return
        val chart = binding.pieChart

        if (done + pending == 0) {
            chart.setNoDataText("No tasks yet")
            chart.invalidate()
            return
        }

        val entries = mutableListOf<PieEntry>()
        if (done > 0)    entries.add(PieEntry(done.toFloat(),    "Done"))
        if (pending > 0) entries.add(PieEntry(pending.toFloat(), "Pending"))

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#4CAF50"),
                Color.parseColor("#FF7043")
            )
            valueTextSize = 13f
            valueTextColor = Color.WHITE
        }

        chart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 40f
            setUsePercentValues(true)
            legend.textSize = 13f
            legend.textColor = context.getColor(com.example.home_chores_automation_app.R.color.on_surface)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
            animateY(900)
            invalidate()
        }
    }

    // ── Bar chart: completions per day last 7 days ───────────────────────────

    private fun setupBarChart(completed: List<com.example.home_chores_automation_app.data.model.Task>) {
        if (_binding == null) return
        val chart = binding.barChart

        val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
        val cal    = Calendar.getInstance()

        // Build day labels and count completions per day (index 0 = 6 days ago, index 6 = today)
        val labels = mutableListOf<String>()
        val entries = mutableListOf<BarEntry>()

        for (i in 6 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dayStart = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + 86_400_000L
            val count  = completed.count { it.completedAt in dayStart until dayEnd }
            labels.add(dayFmt.format(cal.time))
            entries.add(BarEntry((6 - i).toFloat(), count.toFloat()))
        }

        val dataSet = BarDataSet(entries, "Completions").apply {
            color = Color.parseColor("#00897B")
            valueTextSize = 10f
            valueTextColor = context?.getColor(com.example.home_chores_automation_app.R.color.on_surface) ?: Color.BLACK
        }

        chart.apply {
            data = BarData(dataSet).apply { barWidth = 0.7f }
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = context.getColor(com.example.home_chores_automation_app.R.color.on_surface)
            }
            axisLeft.apply {
                granularity = 1f
                axisMinimum = 0f
                textColor = context.getColor(com.example.home_chores_automation_app.R.color.on_surface)
            }
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            setFitBars(true)
            animateY(900)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
