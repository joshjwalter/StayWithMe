package com.joshwalter.staywithme.ui.welcome

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.joshwalter.staywithme.R
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
        
        sharedPreferences = requireContext().getSharedPreferences("staywithme_prefs", Context.MODE_PRIVATE)
        
        setupUI()
        
        return binding.root
    }
    
    private fun setupUI() {
        binding.btnGetStarted.setOnClickListener {
            markWelcomeCompleted()
            navigateToMyInfo()
        }
    }
    
    private fun markWelcomeCompleted() {
        sharedPreferences.edit()
            .putBoolean("welcome_completed", true)
            .apply()
    }
    
    private fun navigateToMyInfo() {
        findNavController().navigate(R.id.navigation_my_info)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun shouldShowWelcome(context: Context): Boolean {
            val prefs = context.getSharedPreferences("staywithme_prefs", Context.MODE_PRIVATE)
            return !prefs.getBoolean("welcome_completed", false)
        }
    }
}
