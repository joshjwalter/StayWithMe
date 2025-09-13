package com.joshwalter.staywithme.ui.welcome

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.joshwalter.staywithme.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        
        sharedPreferences = requireContext().getSharedPreferences("staywithme_prefs", android.content.Context.MODE_PRIVATE)
        
        setupUI()
        
        return binding.root
    }
    
    private fun setupUI() {
        binding.btnGetStarted.setOnClickListener {
            // Mark that user has seen the welcome screen
            sharedPreferences.edit()
                .putBoolean("has_seen_welcome", true)
                .apply()
            
            // Navigate to My Info screen for setup
            findNavController().navigate(com.joshwalter.staywithme.R.id.navigation_my_info)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}