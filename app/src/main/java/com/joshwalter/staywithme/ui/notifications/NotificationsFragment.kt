package com.joshwalter.staywithme.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshwalter.staywithme.databinding.FragmentNotificationsBinding
import com.joshwalter.staywithme.ui.notifications.adapter.SessionHistoryAdapter
import com.joshwalter.staywithme.ui.notifications.adapter.NotificationLogAdapter

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var notificationsViewModel: NotificationsViewModel
    private lateinit var sessionHistoryAdapter: SessionHistoryAdapter
    private lateinit var notificationLogAdapter: NotificationLogAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        notificationsViewModel = ViewModelProvider(this, factory)[NotificationsViewModel::class.java]
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        
        setupRecyclerViews()
        observeViewModel()
        
        return binding.root
    }
    
    private fun setupRecyclerViews() {
        sessionHistoryAdapter = SessionHistoryAdapter()
        notificationLogAdapter = NotificationLogAdapter()
        
        binding.rvSessionHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sessionHistoryAdapter
        }
        
        binding.rvNotificationLogs.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationLogAdapter
        }
    }
    
    private fun observeViewModel() {
        notificationsViewModel.text.observe(viewLifecycleOwner) { text ->
            binding.textNotifications.text = text
        }
        
        notificationsViewModel.allSessions.observe(viewLifecycleOwner) { sessions ->
            sessionHistoryAdapter.submitList(sessions)
            binding.tvNoSessions.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
        }
        
        notificationsViewModel.recentNotifications.observe(viewLifecycleOwner) { notifications ->
            notificationLogAdapter.submitList(notifications)
            binding.tvNoNotifications.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}