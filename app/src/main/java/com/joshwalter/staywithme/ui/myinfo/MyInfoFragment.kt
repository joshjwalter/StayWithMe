package com.joshwalter.staywithme.ui.myinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.joshwalter.staywithme.databinding.FragmentMyInfoBinding
import kotlinx.coroutines.launch

class MyInfoFragment : Fragment() {

    private var _binding: FragmentMyInfoBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var myInfoViewModel: MyInfoViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val factory = MyInfoViewModelFactory(requireActivity().application)
        myInfoViewModel = ViewModelProvider(this, factory)[MyInfoViewModel::class.java]
        _binding = FragmentMyInfoBinding.inflate(inflater, container, false)
        
        setupUI()
        observeViewModel()
        loadUserInfo()
        
        return binding.root
    }
    
    private fun setupUI() {
        binding.btnSaveInfo.setOnClickListener {
            saveUserInfo()
        }
    }
    
    private fun observeViewModel() {
        myInfoViewModel.userInfo.observe(viewLifecycleOwner) { userInfo ->
            userInfo?.let {
                binding.etName.setText(it.name)
                binding.etMedicalInfo.setText(it.medicalInfo)
                binding.etAdditionalNotes.setText(it.additionalNotes)
                binding.etCustomAlertMessage.setText(it.customAlertMessage)
                binding.etCheckInInterval.setText(it.checkInIntervalMinutes.toString())
            }
        }
        
        myInfoViewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Information saved successfully!", Toast.LENGTH_SHORT).show()
                // Navigate back to home screen after successful save
                findNavController().navigateUp()
            }
        }
    }
    
    private fun loadUserInfo() {
        lifecycleScope.launch {
            myInfoViewModel.loadUserInfo()
        }
    }
    
    private fun saveUserInfo() {
        val name = binding.etName.text.toString().trim()
        val medicalInfo = binding.etMedicalInfo.text.toString().trim()
        val additionalNotes = binding.etAdditionalNotes.text.toString().trim()
        val customAlertMessage = binding.etCustomAlertMessage.text.toString().trim()
        val checkInInterval = binding.etCheckInInterval.text.toString().trim().toIntOrNull() ?: 30
        
        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            return
        }
        
        if (checkInInterval < 2 || checkInInterval > 120) {
            binding.etCheckInInterval.error = "Check-in interval must be between 2 and 120 minutes"
            return
        }
        
        lifecycleScope.launch {
            myInfoViewModel.saveUserInfo(name, medicalInfo, additionalNotes, customAlertMessage, checkInInterval)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
