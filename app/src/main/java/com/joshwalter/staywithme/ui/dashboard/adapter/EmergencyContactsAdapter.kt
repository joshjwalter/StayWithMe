package com.joshwalter.staywithme.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joshwalter.staywithme.databinding.ItemEmergencyContactBinding
import com.joshwalter.staywithme.data.model.EmergencyContact

class EmergencyContactsAdapter(
    private val onEditContact: (EmergencyContact) -> Unit,
    private val onDeleteContact: (EmergencyContact) -> Unit
) : ListAdapter<EmergencyContact, EmergencyContactsAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemEmergencyContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(
        private val binding: ItemEmergencyContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: EmergencyContact) {
            binding.tvContactName.text = contact.name
            binding.tvContactPhone.text = contact.phoneNumber
            binding.tvContactPriority.text = "Priority: ${contact.priority}"
            
            binding.btnEditContact.setOnClickListener {
                onEditContact(contact)
            }
            
            binding.btnDeleteContact.setOnClickListener {
                onDeleteContact(contact)
            }
        }
    }

    class ContactDiffCallback : DiffUtil.ItemCallback<EmergencyContact>() {
        override fun areItemsTheSame(oldItem: EmergencyContact, newItem: EmergencyContact): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EmergencyContact, newItem: EmergencyContact): Boolean {
            return oldItem == newItem
        }
    }
}