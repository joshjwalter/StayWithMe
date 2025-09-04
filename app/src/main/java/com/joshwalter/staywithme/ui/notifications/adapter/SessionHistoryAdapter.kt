package com.joshwalter.staywithme.ui.notifications.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joshwalter.staywithme.databinding.ItemSessionHistoryBinding
import com.joshwalter.staywithme.data.model.CheckInSession
import java.text.SimpleDateFormat
import java.util.*

class SessionHistoryAdapter : ListAdapter<CheckInSession, SessionHistoryAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(
        private val binding: ItemSessionHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())

        fun bind(session: CheckInSession) {
            binding.tvSessionDate.text = dateFormat.format(session.startTime)
            binding.tvSessionDuration.text = "Duration: ${session.durationMinutes} minutes"
            
            binding.tvSessionStatus.text = if (session.isActive) "Active" else "Completed"
            binding.tvSessionStatus.setBackgroundColor(
                if (session.isActive) 
                    binding.root.context.getColor(android.R.color.holo_green_light)
                else 
                    binding.root.context.getColor(android.R.color.darker_gray)
            )
            
            if (!session.substances.isNullOrEmpty()) {
                binding.tvSessionSubstances.text = "Substances: ${session.substances}"
                binding.tvSessionSubstances.visibility = View.VISIBLE
            } else {
                binding.tvSessionSubstances.visibility = View.GONE
            }
            
            if (!session.location.isNullOrEmpty()) {
                binding.tvSessionLocation.text = "Location: ${session.location}"
                binding.tvSessionLocation.visibility = View.VISIBLE
            } else {
                binding.tvSessionLocation.visibility = View.GONE
            }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<CheckInSession>() {
        override fun areItemsTheSame(oldItem: CheckInSession, newItem: CheckInSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CheckInSession, newItem: CheckInSession): Boolean {
            return oldItem == newItem
        }
    }
}