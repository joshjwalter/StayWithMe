package com.joshwalter.staywithme.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.joshwalter.staywithme.R
import com.joshwalter.staywithme.databinding.FragmentDashboardBinding
import com.joshwalter.staywithme.data.model.EmergencyContact
import com.joshwalter.staywithme.ui.dashboard.adapter.EmergencyContactsAdapter
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var contactsAdapter: EmergencyContactsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        dashboardViewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        
        setupRecyclerView()
        setupUI()
        observeViewModel()
        
        return binding.root
    }
    
    private fun setupRecyclerView() {
        contactsAdapter = EmergencyContactsAdapter(
            onEditContact = { contact -> showEditContactDialog(contact) },
            onDeleteContact = { contact -> deleteContact(contact) }
        )
        
        binding.rvEmergencyContacts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = contactsAdapter
        }
    }
    
    private fun setupUI() {
        binding.fabAddContact.setOnClickListener {
            showAddContactDialog()
        }
    }
    
    private fun observeViewModel() {
        dashboardViewModel.emergencyContacts.observe(viewLifecycleOwner) { contacts ->
            contactsAdapter.submitList(contacts)
            binding.tvNoContacts.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
        }
        
        dashboardViewModel.text.observe(viewLifecycleOwner) { text ->
            binding.textDashboard.text = text
        }
    }
    
    private fun showAddContactDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_contact, null)
        val nameField = dialogView.findViewById<TextInputEditText>(R.id.et_contact_name)
        val phoneField = dialogView.findViewById<TextInputEditText>(R.id.et_contact_phone)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameField.text.toString().trim()
                val phone = phoneField.text.toString().trim()
                
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    addContact(name, phone)
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showEditContactDialog(contact: EmergencyContact) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_contact, null)
        val nameField = dialogView.findViewById<TextInputEditText>(R.id.et_contact_name)
        val phoneField = dialogView.findViewById<TextInputEditText>(R.id.et_contact_phone)
        
        nameField.setText(contact.name)
        phoneField.setText(contact.phoneNumber)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val name = nameField.text.toString().trim()
                val phone = phoneField.text.toString().trim()
                
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    updateContact(contact.copy(name = name, phoneNumber = phone))
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addContact(name: String, phone: String) {
        lifecycleScope.launch {
            try {
                dashboardViewModel.addEmergencyContact(name, phone)
                Toast.makeText(context, "Contact added", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to add contact: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateContact(contact: EmergencyContact) {
        lifecycleScope.launch {
            try {
                dashboardViewModel.updateEmergencyContact(contact)
                Toast.makeText(context, "Contact updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to update contact: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun deleteContact(contact: EmergencyContact) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete ${contact.name}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        dashboardViewModel.deleteEmergencyContact(contact)
                        Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to delete contact: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}