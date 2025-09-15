package com.joshwalter.staywithme.ui.home

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.joshwalter.staywithme.MainActivity
import com.joshwalter.staywithme.R
import com.joshwalter.staywithme.databinding.FragmentHomeBinding
import com.joshwalter.staywithme.data.database.StayWithMeDatabase
import com.joshwalter.staywithme.service.CheckInMonitoringService
import com.joshwalter.staywithme.service.LocationTrackingService
import com.joshwalter.staywithme.util.PermissionManager
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var database: StayWithMeDatabase
    private lateinit var locationService: LocationTrackingService
    private var countDownTimer: CountDownTimer? = null
    private var checkInTimer: CountDownTimer? = null
    private var urgentTimer: CountDownTimer? = null
    private var lastNotificationTime = 0L
    private var timersInitialized = false
    
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
        
        binding.btnSettings.setOnClickListener {
            navigateToMyInfo()
        }
        
        // Connect UI inputs to ViewModel
        binding.etDuration.setText(homeViewModel.selectedDuration.value?.toString() ?: "120")
        
        // Update ViewModel immediately when text changes (not just on focus change)
        binding.etDuration.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val duration = s?.toString()?.toIntOrNull() ?: 120
                homeViewModel.setDuration(duration)
                
                // Clear error if user enters valid duration
                if (duration >= 5) {
                    binding.etDuration.error = null
                }
            }
        })
        
        binding.etSubstances.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                homeViewModel.setSubstanceInfo(s?.toString() ?: "")
            }
        })
        
        binding.etNotes.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                homeViewModel.setNotes(s?.toString() ?: "")
            }
        })
    }
    
    private fun observeViewModel() {
        homeViewModel.currentSession.observe(viewLifecycleOwner) { session ->
            updateUI(session != null)
            if (session != null) {
                // Only start timers on the FIRST time we see a session, not on every observation
                // This prevents restart during navigation, fragment recreation, or notification clicks
                android.util.Log.d("TimerDebug", "Session detected. Timer states - checkIn: ${checkInTimer != null}, urgent: ${urgentTimer != null}, total: ${countDownTimer != null}")
                
                // Only start timers if they haven't been initialized for this session yet
                if (!timersInitialized && checkInTimer == null && urgentTimer == null && countDownTimer == null) {
                    android.util.Log.d("TimerDebug", "Initializing timers for first time")
                    timersInitialized = true
                    startTimers(session)
                } else {
                    android.util.Log.d("TimerDebug", "Timers already initialized or running, preserving continuity")
                    // Don't restart timers - they should continue running as they were
                }
            } else {
                android.util.Log.d("TimerDebug", "No active session, stopping all timers")
                stopAllTimers()
            }
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
            .setMessage("StayWithMe needs location, SMS, phone, and contacts permissions to keep you safe during substance use.")
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
        // Ensure ViewModel has the latest values from UI before starting session
        val duration = binding.etDuration.text.toString().toIntOrNull() ?: 120
        
        // Validate minimum session duration
        if (duration < 5) {
            binding.etDuration.error = "Session duration must be at least 5 minutes"
            return
        }
        
        homeViewModel.setDuration(duration)
        homeViewModel.setSubstanceInfo(binding.etSubstances.text.toString())
        homeViewModel.setNotes(binding.etNotes.text.toString())
        
        lifecycleScope.launch {
            try {
                // Check if user info is set up
                val userInfo = database.userInfoDao().getUserInfo()
                val hasUserInfo = !userInfo?.name.isNullOrEmpty()
                
                if (!hasUserInfo) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Setup Required")
                        .setMessage("Please set up your personal information first. This ensures emergency contacts have your details if needed.")
                        .setPositiveButton("Setup Info") { _, _ ->
                            navigateToMyInfo()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    startSessionInternal()
                }
                
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to start session: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun startSessionInternal() {
        lifecycleScope.launch {
            try {
                // End any existing sessions
                database.checkInSessionDao().endAllActiveSessions()
                
                // Get current location (with timeout handling)
                val location = try {
                    locationService.getCurrentLocation()
                } catch (e: Exception) {
                    null // Continue without location if it fails
                }
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
                    
                    // Reset timer colors to normal
                    binding.circularTimer.setColors(
                        requireContext().getColor(android.R.color.holo_green_dark),
                        Color.LTGRAY
                    )
                    
                    // Restart the check-in timer with fresh full interval
                    // (avoid using startTimers which might have timing issues)
                    restartCheckInTimerAfterCheckIn(updatedSession)
                    
                    homeViewModel.onCheckInPerformed()
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
                // Stop all timers first to prevent conflicts
                stopAllTimers()
                
                database.checkInSessionDao().endAllActiveSessions()
                CheckInMonitoringService.stopService(requireContext())
                locationService.stopLocationTracking()
                
                // Reset timer colors to normal
                binding.circularTimer.setColors(
                    requireContext().getColor(android.R.color.holo_green_dark),
                    Color.LTGRAY
                )
                
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
        binding.sessionCard.visibility = if (hasActiveSession) View.GONE else View.VISIBLE
        binding.activeSessionContainer.visibility = if (hasActiveSession) View.VISIBLE else View.GONE
        
        if (hasActiveSession) {
            binding.sessionStatus.text = "Active safety session"
            binding.sessionStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
            // Reset check-in button to normal appearance
            binding.btnCheckIn.text = "Check In"
            binding.btnCheckIn.setBackgroundColor(requireContext().getColor(android.R.color.holo_green_dark))
        } else {
            binding.sessionStatus.text = "No active session"
            binding.sessionStatus.setTextColor(requireContext().getColor(android.R.color.darker_gray))
        }
    }
    
    private fun startTimers(session: com.joshwalter.staywithme.data.model.CheckInSession) {
        // CRITICAL: Stop all existing timers to prevent conflicts
        stopAllTimers()
        
        // Get user's check-in interval
        lifecycleScope.launch {
            val userInfo = database.userInfoDao().getUserInfo()
            val checkInIntervalMinutes = userInfo?.checkInIntervalMinutes ?: 30
            val checkInIntervalMs = checkInIntervalMinutes * 60 * 1000L
            
            // Calculate time until next check-in
            val lastCheckIn = session.lastCheckIn?.time ?: session.startTime.time
            val nextCheckInTime = lastCheckIn + checkInIntervalMs
            val currentTime = System.currentTimeMillis()
            val timeUntilNextCheckIn = nextCheckInTime - currentTime
            
            // Calculate remaining session time
            val sessionEndTime = session.startTime.time + (session.durationMinutes * 60 * 1000L)
            val totalRemainingTime = sessionEndTime - currentTime
            
            // Determine if there's enough time for a meaningful check-in cycle
            val urgentDelayMs = (checkInIntervalMinutes / 2.0 * 60 * 1000).toLong()
            val totalCheckInCycleTime = checkInIntervalMs + urgentDelayMs
            
            val actualCheckInInterval = if (totalRemainingTime <= totalCheckInCycleTime) {
                // Not enough time for full check-in cycle - stop check-ins
                null
            } else if (timeUntilNextCheckIn > 0) {
                timeUntilNextCheckIn
            } else {
                // If timeUntilNextCheckIn is 0 or negative, start with small delay
                5000L // 5 seconds minimum
            }
            
            android.util.Log.d("TimerDebug", "Total remaining: ${totalRemainingTime}ms (${totalRemainingTime/1000}s), Check-in interval: ${checkInIntervalMs}ms (${checkInIntervalMs/1000}s), Urgent delay: ${urgentDelayMs}ms (${urgentDelayMs/1000}s), Cycle time: ${totalCheckInCycleTime}ms (${totalCheckInCycleTime/1000}s), Actual interval: ${actualCheckInInterval}ms")
            
            // Start check-in timer with calculated interval (if any)
            if (actualCheckInInterval != null) {
                startCheckInTimer(actualCheckInInterval, checkInIntervalMs)
            } else {
                android.util.Log.d("TimerDebug", "Not enough time for check-in cycle - no more check-ins")
            }
            
            // Start total session timer
            if (totalRemainingTime > 0) {
                startTotalSessionTimer(totalRemainingTime)
            }
        }
    }
    
    private fun startCheckInTimer(timeUntilNextCheckIn: Long?, checkInIntervalMs: Long) {
        if (timeUntilNextCheckIn == null) return
        // Cancel any existing check-in timer first
        checkInTimer?.cancel()
        
        checkInTimer = object : CountDownTimer(timeUntilNextCheckIn, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Check if binding is still valid and fragment is attached
                if (_binding == null || !isAdded) return
                
                // Check if this timer is still the active check-in timer
                if (checkInTimer == null || this != checkInTimer) return
                
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                
                val timeString = String.format("%d:%02d", minutes, seconds)
                binding.circularTimer.setDisplayText(timeString)
                
                // Update progress (0 to 100%) - progress increases as time passes
                val elapsed = checkInIntervalMs - millisUntilFinished
                val progress = (elapsed / checkInIntervalMs.toFloat()) * 100f
                binding.circularTimer.setProgress(progress, 100f)
            }
            
            override fun onFinish() {
                // Check if binding is still valid and fragment is attached
                if (_binding == null || !isAdded) return
                
                // Check if this timer is still the active check-in timer
                if (checkInTimer == null || this != checkInTimer) return
                
                binding.circularTimer.setDisplayText("0:00")
                binding.circularTimer.setProgress(100f, 100f)
                
                // Timer finished - enter URGENT mode
                lifecycleScope.launch {
                    try {
                        val currentSession = database.checkInSessionDao().getCurrentSessionSync()
                        currentSession?.let { session ->
                            // Update session to show it's past check-in time
                            val updatedSession = session.copy(
                                notificationLevel = 1 // Start escalation
                            )
                            database.checkInSessionDao().update(updatedSession)
                            
                            // Get user's check-in interval to calculate urgent timer duration
                            val userInfo = database.userInfoDao().getUserInfo()
                            val checkInIntervalMinutes = userInfo?.checkInIntervalMinutes ?: 30
                            
                            // Start urgent timer with HALF the check-in interval for urgency
                            val urgentDelayMs = (checkInIntervalMinutes / 2.0 * 60 * 1000).toLong() // Half interval in ms
                            
                            // Show initial check-in timeout notification (rate-limited) - URGENT
                            showAndroidNotification(
                                "Check-in Reminder",
                                "Time to check in! Tap here to confirm you're safe.",
                                1001,
                                isUrgent = true
                            )
                            
                            // Start urgent timer countdown
                            startUrgentNotificationTimer(urgentDelayMs)
                        }
                    } catch (e: Exception) {
                        // Log error but don't crash
                    }
                }
            }
        }.start()
    }
    
    private fun startTotalSessionTimer(totalRemainingTime: Long) {
        countDownTimer = object : CountDownTimer(totalRemainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Check if binding is still valid and fragment is attached
                if (_binding == null || !isAdded) return
                
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                
                val timeString = String.format("Total Time: %02d:%02d:%02d", hours, minutes, seconds)
                binding.totalSessionTime.text = timeString
            }
            
            override fun onFinish() {
                // Check if binding is still valid and fragment is attached
                if (_binding == null || !isAdded) return
                
                binding.totalSessionTime.text = "SESSION COMPLETED"
                binding.totalSessionTime.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
                
                // Show notification that session has ended
                showAndroidNotification(
                    "Safety Session Completed",
                    "Your safety session has ended successfully. Thank you for staying safe!",
                    1003,
                    isUrgent = true
                )
                
                // Automatically end the session
                endCheckInSession()
            }
        }.start()
    }
    
    private fun stopAllTimers() {
        android.util.Log.d("TimerDebug", "Stopping all timers")
        countDownTimer?.cancel()
        countDownTimer = null
        checkInTimer?.cancel()
        checkInTimer = null
        urgentTimer?.cancel()
        urgentTimer = null
        timersInitialized = false // Reset the flag when stopping timers
    }
    
    private fun restartCheckInTimerAfterCheckIn(session: com.joshwalter.staywithme.data.model.CheckInSession) {
        lifecycleScope.launch {
            try {
                // Stop all existing timers to prevent conflicts
                stopAllTimers()
                
                // Get user's check-in interval
                val userInfo = database.userInfoDao().getUserInfo()
                val checkInIntervalMinutes = userInfo?.checkInIntervalMinutes ?: 30
                val checkInIntervalMs = checkInIntervalMinutes * 60 * 1000L
                
                // Calculate remaining session time
                val sessionEndTime = session.startTime.time + (session.durationMinutes * 60 * 1000L)
                val currentTime = System.currentTimeMillis()
                val totalRemainingTime = sessionEndTime - currentTime
                
                // Determine next check-in interval
                // Check if there's enough time left for a meaningful check-in cycle
                val urgentDelayMs = (checkInIntervalMinutes / 2.0 * 60 * 1000).toLong()
                val totalCheckInCycleTime = checkInIntervalMs + urgentDelayMs // Regular + urgent time
                
                val nextCheckInInterval = if (totalRemainingTime <= totalCheckInCycleTime) {
                    // Not enough time for a full check-in cycle - stop check-ins, let session end naturally
                    null
                } else {
                    // Normal interval - user just checked in, use full interval
                    checkInIntervalMs
                }
                
                android.util.Log.d("CheckInReset", "Total remaining: ${totalRemainingTime}ms (${totalRemainingTime/1000}s), Check-in interval: ${checkInIntervalMs}ms (${checkInIntervalMs/1000}s), Urgent delay: ${urgentDelayMs}ms (${urgentDelayMs/1000}s), Cycle time: ${totalCheckInCycleTime}ms (${totalCheckInCycleTime/1000}s), Next interval: ${nextCheckInInterval}ms")
                
                // Start check-in timer with calculated interval (if any)
                if (nextCheckInInterval != null) {
                    startCheckInTimer(nextCheckInInterval, checkInIntervalMs)
                } else {
                    android.util.Log.d("CheckInReset", "Not enough time for check-in cycle - no more check-ins")
                }
                
                // Also restart the total session timer (in case it wasn't running)
                if (totalRemainingTime > 0) {
                    startTotalSessionTimer(totalRemainingTime)
                }
                
            } catch (e: Exception) {
                // Fallback to regular startTimers if there's an error
                startTimers(session)
            }
        }
    }
    
    private fun triggerNotificationCheck() {
        try {
            // Show immediate Android notification - URGENT
            showAndroidNotification(
                "Check-in Reminder",
                "Time to check in! Tap here to confirm you're safe.",
                1001,
                isUrgent = true
            )
            
            // Update session in database
            lifecycleScope.launch {
                val currentSession = database.checkInSessionDao().getCurrentSessionSync()
                currentSession?.let { session ->
                    val emergencyService = com.joshwalter.staywithme.service.EmergencyNotificationService(requireContext())
                    emergencyService.executeNotificationLevel(
                        com.joshwalter.staywithme.service.EmergencyNotificationService.LEVEL_GENTLE,
                        session.id
                    )
                }
            }
            
            // Let WorkManager handle escalation to avoid duplicate notifications
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to trigger notification: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun startUrgentNotificationTimer(delayMs: Long) {
        // Cancel any existing urgent timer first
        urgentTimer?.cancel()
        
        urgentTimer = object : CountDownTimer(delayMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update circular timer to show countdown to urgent notification
                if (_binding == null || !isAdded) return
                
                // Check if this timer is still the active urgent timer
                if (urgentTimer == null || this != urgentTimer) return
                
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                
                val timeString = String.format("URGENT: %d:%02d", minutes, seconds)
                binding.circularTimer.setDisplayText(timeString)
                
                // Make progress red to indicate urgency
                binding.circularTimer.setColors(
                    requireContext().getColor(android.R.color.holo_red_dark),
                    requireContext().getColor(android.R.color.darker_gray)
                )
                
                val progress = ((delayMs - millisUntilFinished) / delayMs.toFloat()) * 100f
                binding.circularTimer.setProgress(progress, 100f)
            }
            
            override fun onFinish() {
                if (_binding == null || !isAdded) return
                
                // Check if this timer is still the active urgent timer
                if (urgentTimer == null || this != urgentTimer) return
                
                // Show urgent notification (rate-limited to prevent spam)
                showAndroidNotification(
                    "URGENT: Check-in Required!",
                    "You haven't checked in! Emergency contacts will be alerted if you don't respond soon.",
                    1002,
                    isUrgent = true
                )
                
                // Trigger urgent notification in service
                lifecycleScope.launch {
                    try {
                        val emergencyService = com.joshwalter.staywithme.service.EmergencyNotificationService(requireContext())
                        val currentSession = database.checkInSessionDao().getCurrentSessionSync()
                        currentSession?.let { session ->
                            emergencyService.executeNotificationLevel(
                                com.joshwalter.staywithme.service.EmergencyNotificationService.LEVEL_URGENT,
                                session.id
                            )
                        }
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
            }
        }
        urgentTimer?.start()
    }
    
    private fun navigateToMyInfo() {
        findNavController().navigate(R.id.navigation_my_info)
    }
    
    private fun showAndroidNotification(title: String, message: String, notificationId: Int, isUrgent: Boolean = false) {
        try {
            // Rate limiting: Don't send notifications more than once every 30 seconds
            // BUT allow urgent notifications to bypass rate limiting
            val currentTime = System.currentTimeMillis()
            if (!isUrgent && currentTime - lastNotificationTime < 30000) { // 30 seconds
                return // Skip non-urgent notifications to avoid spam
            }
            lastNotificationTime = currentTime
            
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create notification channel for Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Always use HIGH importance for heads-up notifications
                val channelImportance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(
                    "checkin_channel",
                    "Check-in Reminders",
                    channelImportance
                ).apply {
                    description = "Safety check-in notifications with heads-up display"
                    enableVibration(true)
                    vibrationPattern = if (isUrgent) longArrayOf(500, 500, 500, 500, 500) else longArrayOf(1000, 1000, 1000)
                    setSound(RingtoneManager.getDefaultUri(if (isUrgent) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION), null)
                    enableLights(true)
                    lightColor = if (isUrgent) android.graphics.Color.RED else android.graphics.Color.BLUE
                    // Allow heads-up notifications
                    setShowBadge(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            // Create intent to simply open the app when notification is tapped (no special actions)
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                // Don't add any extra data or actions - just open the app normally
            }
            
            val pendingIntent = PendingIntent.getActivity(
                requireContext(),
                notificationId, // Use notification ID as request code to avoid conflicts
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Build the notification with appropriate priority
            val priority = if (isUrgent) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH
            val category = if (isUrgent) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER
            
            val notification = NotificationCompat.Builder(requireContext(), "checkin_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority)
                .setCategory(category)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(if (isUrgent) longArrayOf(500, 500, 500, 500, 500) else longArrayOf(1000, 1000, 1000))
                .setSound(RingtoneManager.getDefaultUri(if (isUrgent) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Make all notifications heads-up
                .setFullScreenIntent(pendingIntent, false) // Heads-up notification
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
                .apply {
                    if (isUrgent) {
                        setLights(android.graphics.Color.RED, 500, 500)
                        setOngoing(true) // Makes it harder to dismiss for urgent notifications
                    }
                }
                .build()
            
            // Check for notification permission (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, notification)
                } else {
                    Toast.makeText(requireContext(), "Notification permission required for alerts", Toast.LENGTH_LONG).show()
                }
            } else {
                notificationManager.notify(notificationId, notification)
            }
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to show notification: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        
        android.util.Log.d("TimerDebug", "onResume called - preserving timer state")
        
        // Only update UI state, never restart timers on resume
        lifecycleScope.launch {
            val currentSession = database.checkInSessionDao().getCurrentSessionSync()
            updateUI(currentSession != null)
            // IMPORTANT: Never restart timers here - they should continue running
            // All timer management is handled by observeViewModel only
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAllTimers()
        _binding = null
    }
}