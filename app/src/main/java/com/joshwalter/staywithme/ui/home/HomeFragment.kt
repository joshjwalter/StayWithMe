package com.joshwalter.staywithme.ui.home

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.joshwalter.staywithme.R
import com.joshwalter.staywithme.databinding.FragmentHomeBinding
import com.joshwalter.staywithme.data.database.StayWithMeDatabase
import com.joshwalter.staywithme.service.CheckInMonitoringService
import com.joshwalter.staywithme.service.LocationTrackingService
import com.joshwalter.staywithme.util.PermissionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var database: StayWithMeDatabase
    private lateinit var locationService: LocationTrackingService
    private var countDownTimer: CountDownTimer? = null
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            setupEmergencyServices()
            Toast.makeText(context, "Permissions granted. StayWithMe is ready!", Toast.LENGTH_SHORT).show()
        } else {
            showPermissionRequiredDialog()
        }
    }
    
    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.ACCESS_BACKGROUND_LOCATION] == true) {
            Toast.makeText(context, "Background location enabled for safety monitoring", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val factory = HomeViewModelFactory(requireActivity().application)
        homeViewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        
        database = StayWithMeDatabase.getDatabase(requireContext())
        locationService = LocationTrackingService(requireContext())
        
        setupUI()
        observeViewModel()
        checkPermissions()
        
        return binding.root
    }
    
    private fun setupUI() {
        binding.btnStartSession.setOnClickListener {
            startCheckInSession()
        }
        
        binding.btnCheckIn.setOnClickListener {
            performCheckIn()
        }
        
        binding.btnEndSession.setOnClickListener {
            endCheckInSession()
        }
        
        // Duration selection buttons
        binding.btn30min.setOnClickListener {
            setDuration(30)
        }
        
        binding.btn60min.setOnClickListener {
            setDuration(60)
        }
        
        binding.btn120min.setOnClickListener {
            setDuration(120)
        }
        
        // Connect UI inputs to ViewModel
        binding.etDuration.setText(homeViewModel.selectedDuration.value?.toString() ?: "120")
        
        binding.etDuration.setOnFocusChangeListener { _, _ ->
            val duration = binding.etDuration.text.toString().toIntOrNull() ?: 120
            homeViewModel.setDuration(duration)
        }
        
        binding.etSubstances.setOnFocusChangeListener { _, _ ->
            homeViewModel.setSubstanceInfo(binding.etSubstances.text.toString())
        }
        
        binding.etNotes.setOnFocusChangeListener { _, _ ->
            homeViewModel.setNotes(binding.etNotes.text.toString())
        }
    }
    
    private fun setDuration(minutes: Int) {
        homeViewModel.setDuration(minutes)
        binding.etDuration.setText(minutes.toString())
        updateDurationButtonStates(minutes)
    }
    
    private fun updateDurationButtonStates(selectedDuration: Int) {
        // Reset all buttons to outlined style
        binding.btn30min.style = com.google.android.material.R.style.Widget_MaterialComponents_Button_OutlinedButton
        binding.btn60min.style = com.google.android.material.R.style.Widget_MaterialComponents_Button_OutlinedButton
        binding.btn120min.style = com.google.android.material.R.style.Widget_MaterialComponents_Button_OutlinedButton
        
        // Set selected button to filled style
        when (selectedDuration) {
            30 -> binding.btn30min.style = com.google.android.material.R.style.Widget_MaterialComponents_Button
            60 -> binding.btn60min.style = com.google.android.material.R.style.Widget_MaterialComponents_Button
            120 -> binding.btn120min.style = com.google.android.material.R.style.Widget_MaterialComponents_Button
        }
    }
    
    private fun observeViewModel() {
        homeViewModel.currentSession.observe(viewLifecycleOwner) { session ->
            updateUI(session != null)
            session?.let { startTimer(it) }
        }
        
        homeViewModel.text.observe(viewLifecycleOwner) { text ->
            binding.textHome.text = text
        }
    }
    
    private fun checkPermissions() {
        if (!PermissionManager.hasAllRequiredPermissions(requireContext())) {
            requestPermissions()
        } else {
            setupEmergencyServices()
        }
    }
    
    private fun requestPermissions() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permissions Required")
            .setMessage("StayWithMe needs location, SMS, and phone permissions to keep you safe during substance use.")
            .setPositiveButton("Grant Permissions") { _, _ ->
                PermissionManager.requestPermissions(this, permissionLauncher)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(context, "Permissions are required for safety features", Toast.LENGTH_LONG).show()
            }
            .show()
    }
    
    private fun setupEmergencyServices() {
        // Request background location if not already granted
        if (PermissionManager.hasLocationPermissions(requireContext()) && 
            !PermissionManager.hasBackgroundLocationPermission(requireContext())) {
            PermissionManager.requestBackgroundLocation(this, backgroundLocationLauncher)
        }
    }
    
    private fun showPermissionRequiredDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permissions Required")
            .setMessage("All permissions are required for StayWithMe to function properly. Please grant them in settings.")
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }
    
    private fun startCheckInSession() {
        lifecycleScope.launch {
            try {
                // End any existing sessions
                database.checkInSessionDao().endAllActiveSessions()
                
                // Get current location
                val location = locationService.getCurrentLocation()
                val locationString = location?.let { "${it.latitude},${it.longitude}" }
                
                // Create new session
                val newSession = com.joshwalter.staywithme.data.model.CheckInSession(
                    startTime = Date(),
                    durationMinutes = homeViewModel.getSelectedDuration(),
                    location = locationString,
                    substances = homeViewModel.getSubstanceInfo(),
                    notes = homeViewModel.getNotes()
                )
                
                val sessionId = database.checkInSessionDao().insert(newSession)
                
                // Start monitoring service
                CheckInMonitoringService.startService(requireContext())
                locationService.startLocationTracking()
                
                homeViewModel.onSessionStarted()
                Toast.makeText(context, "Safety session started. Stay safe!", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to start session: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun performCheckIn() {
        lifecycleScope.launch {
            try {
                val currentSession = database.checkInSessionDao().getCurrentSessionSync()
                currentSession?.let { session ->
                    val updatedSession = session.copy(
                        lastCheckIn = Date(),
                        notificationLevel = 0 // Reset notification level
                    )
                    database.checkInSessionDao().update(updatedSession)
                    homeViewModel.onCheckInPerformed()
                    updateLastCheckInDisplay(updatedSession.lastCheckIn)
                    Toast.makeText(context, "Check-in recorded. Stay safe!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to record check-in: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun endCheckInSession() {
        lifecycleScope.launch {
            try {
                database.checkInSessionDao().endAllActiveSessions()
                CheckInMonitoringService.stopService(requireContext())
                locationService.stopLocationTracking()
                
                homeViewModel.onSessionEnded()
                Toast.makeText(context, "Safety session ended", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to end session: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateUI(hasActiveSession: Boolean) {
        binding.btnStartSession.visibility = if (hasActiveSession) View.GONE else View.VISIBLE
        binding.btnCheckIn.visibility = if (hasActiveSession) View.VISIBLE else View.GONE
        binding.btnEndSession.visibility = if (hasActiveSession) View.VISIBLE else View.GONE
        binding.activeTimerCard.visibility = if (hasActiveSession) View.VISIBLE else View.GONE
        binding.durationCard.visibility = if (hasActiveSession) View.GONE else View.VISIBLE
        binding.sessionCard.visibility = if (hasActiveSession) View.GONE else View.VISIBLE
        
        if (hasActiveSession) {
            binding.sessionStatus.text = "Safety session active"
            binding.sessionStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
        } else {
            binding.sessionStatus.text = "Your safety companion"
            binding.sessionStatus.setTextColor(requireContext().getColor(android.R.color.darker_gray))
            stopTimer()
        }
    }
    
    private fun startTimer(session: com.joshwalter.staywithme.data.model.CheckInSession) {
        stopTimer() // Stop any existing timer
        
        val totalDurationMs = session.durationMinutes * 60 * 1000L
        val elapsedMs = Date().time - session.startTime.time
        val remainingMs = totalDurationMs - elapsedMs
        
        if (remainingMs > 0) {
            countDownTimer = object : CountDownTimer(remainingMs, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    updateTimerDisplay(millisUntilFinished)
                }
                
                override fun onFinish() {
                    binding.tvTimeRemaining.text = "00:00:00"
                    // Timer finished - emergency alerts will be handled by the service
                }
            }.start()
        }
        
        updateLastCheckInDisplay(session.lastCheckIn)
    }
    
    private fun stopTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }
    
    private fun updateTimerDisplay(millisUntilFinished: Long) {
        val hours = millisUntilFinished / (1000 * 60 * 60)
        val minutes = (millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (millisUntilFinished % (1000 * 60)) / 1000
        
        binding.tvTimeRemaining.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
    
    private fun updateLastCheckInDisplay(lastCheckIn: Date?) {
        val lastCheckInText = if (lastCheckIn != null) {
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            "Last check-in: ${formatter.format(lastCheckIn)}"
        } else {
            "Last check-in: Never"
        }
        binding.tvLastCheckin.text = lastCheckInText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimer()
        _binding = null
    }
}