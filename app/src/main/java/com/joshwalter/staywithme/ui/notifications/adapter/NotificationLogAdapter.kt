package com.joshwalter.staywithme.ui.notifications.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joshwalter.staywithme.databinding.ItemNotificationLogBinding
import com.joshwalter.staywithme.data.model.NotificationLog
import java.text.SimpleDateFormat
import java.util.*

class NotificationLogAdapter : ListAdapter<NotificationLog, NotificationLogAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(
        private val binding: ItemNotificationLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.getDefault())

        fun bind(notification: NotificationLog) {
            binding.tvNotificationType.text = when (notification.notificationType) {
                "gentle" -> "Gentle Reminder"
                "urgent" -> "Urgent Notification"
                "emergency" -> "Emergency Alert"
                "sms" -> "Emergency SMS"
                "call" -> "Emergency Call"
                else -> notification.notificationType.uppercase()
            }
            
            binding.tvNotificationTimestamp.text = dateFormat.format(notification.timestamp)
            
            binding.tvNotificationStatus.text = if (notification.success) "Success" else "Failed"
            binding.tvNotificationStatus.setBackgroundColor(
                if (notification.success)
                    binding.root.context.getColor(android.R.color.holo_green_light)
                else
                    binding.root.context.getColor(android.R.color.holo_red_light)
            )
            
            if (!notification.message.isNullOrEmpty()) {
                binding.tvNotificationMessage.text = notification.message
                binding.tvNotificationMessage.visibility = View.VISIBLE
            } else {
                binding.tvNotificationMessage.visibility = View.GONE
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationLog>() {
        override fun areItemsTheSame(oldItem: NotificationLog, newItem: NotificationLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationLog, newItem: NotificationLog): Boolean {
            return oldItem == newItem
        }
    }
}