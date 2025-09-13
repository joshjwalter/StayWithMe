package com.joshwalter.staywithme.ui.myinfo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshwalter.staywithme.data.model.EmergencyContact
import com.joshwalter.staywithme.databinding.ItemEmergencyContactBinding

class EmergencyContactsAdapter(
    private val onDeleteClick: (EmergencyContact) -> Unit,
    private val onEditClick: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<EmergencyContactsAdapter.ContactViewHolder>() {

    private var contacts = listOf<EmergencyContact>()

    fun updateContacts(newContacts: List<EmergencyContact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemEmergencyContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int = contacts.size

    inner class ContactViewHolder(private val binding: ItemEmergencyContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: EmergencyContact) {
            binding.tvContactName.text = contact.name
            binding.tvContactPhone.text = contact.phoneNumber
            
            binding.btnDelete.setOnClickListener {
                onDeleteClick(contact)
            }
            
            binding.btnEdit.setOnClickListener {
                onEditClick(contact)
            }
        }
    }
}