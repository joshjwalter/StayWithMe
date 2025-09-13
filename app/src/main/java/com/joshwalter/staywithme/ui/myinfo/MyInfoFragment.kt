package com.joshwalter.staywithme.ui.myinfo

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
import com.joshwalter.staywithme.data.model.EmergencyContact
import com.joshwalter.staywithme.databinding.FragmentMyInfoBinding
import kotlinx.coroutines.launch

class MyInfoFragment : Fragment() {

    private var _binding: FragmentMyInfoBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var myInfoViewModel: MyInfoViewModel
    private lateinit var contactsAdapter: EmergencyContactsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val factory = MyInfoViewModelFactory(requireActivity().application)
        myInfoViewModel = ViewModelProvider(this, factory)[MyInfoViewModel::class.java]
        _binding = FragmentMyInfoBinding.inflate(inflater, container, false)
        
        setupUI()
        setupRecyclerView()
        observeViewModel()
        
        return binding.root
    }
    
    private fun setupUI() {
        binding.btnSave.setOnClickListener {
            saveUserInfo()
        }
        
        binding.btnAddContact.setOnClickListener {
            showAddContactDialog()
        }
    }
    
    private fun setupRecyclerView() {
        contactsAdapter = EmergencyContactsAdapter(
            onDeleteClick = { contact ->
                showDeleteContactDialog(contact)
            },
            onEditClick = { contact ->
                showEditContactDialog(contact)
            }
        )
        
        binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvContacts.adapter = contactsAdapter
    }
    
    private fun observeViewModel() {
        myInfoViewModel.userInfo.observe(viewLifecycleOwner) { userInfo ->
            userInfo?.let {
                binding.etName.setText(it.name)
                binding.etMedicalInfo.setText(it.medicalInfo)
                binding.etNotes.setText(it.notes)
                binding.etCustomAlertMessage.setText(it.customAlertMessage)
            }
        }
        
        myInfoViewModel.emergencyContacts.observe(viewLifecycleOwner) { contacts ->
            contactsAdapter.updateContacts(contacts)
        }
    }
    
    private fun saveUserInfo() {
        val name = binding.etName.text.toString().trim()
        val medicalInfo = binding.etMedicalInfo.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()
        val customAlertMessage = binding.etCustomAlertMessage.text.toString().trim()
        
        if (name.isEmpty()) {
            Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            myInfoViewModel.saveUserInfo(name, medicalInfo, notes, customAlertMessage)
            Toast.makeText(context, "Information saved successfully", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showAddContactDialog() {
        val dialogView = layoutInflater.inflate(com.joshwalter.staywithme.R.layout.dialog_add_contact, null)
        val nameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.joshwalter.staywithme.R.id.et_contact_name)
        val phoneInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.joshwalter.staywithme.R.id.et_contact_phone)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    myInfoViewModel.addEmergencyContact(name, phone)
                    Toast.makeText(context, "Contact added successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showEditContactDialog(contact: EmergencyContact) {
        val dialogView = layoutInflater.inflate(com.joshwalter.staywithme.R.layout.dialog_add_contact, null)
        val nameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.joshwalter.staywithme.R.id.et_contact_name)
        val phoneInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.joshwalter.staywithme.R.id.et_contact_phone)
        
        nameInput.setText(contact.name)
        phoneInput.setText(contact.phoneNumber)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    val updatedContact = contact.copy(name = name, phoneNumber = phone)
                    myInfoViewModel.updateEmergencyContact(updatedContact)
                    Toast.makeText(context, "Contact updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showDeleteContactDialog(contact: EmergencyContact) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete ${contact.name}?")
            .setPositiveButton("Delete") { _, _ ->
                myInfoViewModel.deleteEmergencyContact(contact)
                Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}