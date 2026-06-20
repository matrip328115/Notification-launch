package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.LogEntry
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }
}

// Custom modern palette matching our luxury slate theme
val SpaceBackground = Color(0xFF0F172A)
val GlassSurface = Color(0xFF1E293B)
val BorderAccent = Color(0xFF334155)
val AccentCyan = Color(0xFF06B6D4)
val AccentTurquoise = Color(0xFF14B8A6)
val AccentIndigo = Color(0xFF6366F1)
val DangerRose = Color(0xFFF43F5E)
val SoftText = Color(0xFF94A3B8)
val LightText = Color(0xFFF1F5F9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()
    val isNotificationListenerGranted by viewModel.isNotificationListenerGranted.collectAsStateWithLifecycle()
    val isAccessibilityGranted by viewModel.isAccessibilityGranted.collectAsStateWithLifecycle()
    val isSystemAlertGranted by viewModel.isSystemAlertGranted.collectAsStateWithLifecycle()

    val displayApps by viewModel.displayAppsFlow.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val targetPackage by viewModel.targetPackage.collectAsStateWithLifecycle()
    val targetAppName by viewModel.targetAppName.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Notification AutoLauncher",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = LightText,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SpaceBackground,
                    titleContentColor = LightText
                )
            )
        },
        containerColor = SpaceBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Elegant M3 Tab selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SpaceBackground,
                contentColor = AccentCyan,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("डैशबोर्ड", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Dashboard") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("ऐप चयन", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Apps") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("इवेंट्स", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (logs.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DangerRose),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = logs.size.toString(),
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.List, contentDescription = "Logs") }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when (selectedTab) {
                    0 -> DashboardTab(
                        isServiceActive = isServiceActive,
                        isNotificationListenerGranted = isNotificationListenerGranted,
                        isAccessibilityGranted = isAccessibilityGranted,
                        isSystemAlertGranted = isSystemAlertGranted,
                        targetAppName = targetAppName,
                        targetPackage = targetPackage,
                        onToggleService = { viewModel.toggleServiceActive() },
                        onRequestNotificationPermission = {
                            intentLaunchSettings(context, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        },
                        onRequestAccessibilityPermission = {
                            intentLaunchSettings(context, Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        },
                        onRequestOverlayPermission = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )
                    1 -> AppSelectorTab(
                        apps = displayApps,
                        searchQuery = searchQuery,
                        targetPackage = targetPackage,
                        onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                        onToggleMonitor = { app, checked -> viewModel.toggleMonitoredApp(app.packageName, app.appName, checked) },
                        onSetTarget = { app -> viewModel.setTargetApplication(app.packageName, app.appName) },
                        onSimulate = { app -> viewModel.simulateNotificationMatch(app.packageName) }
                    )
                    2 -> EventsTab(
                        logs = logs,
                        onClearLogs = { com.example.service.LogHistoryManager.clear() }
                    )
                }
            }
        }
    }
}

private fun intentLaunchSettings(context: Context, action: String) {
    try {
        val intent = Intent(action)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "इस सेटिंग को सीधे नहीं खोला जा सकता है। कृपया सेटिंग्स से ऑन करें।", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun DashboardTab(
    isServiceActive: Boolean,
    isNotificationListenerGranted: Boolean,
    isAccessibilityGranted: Boolean,
    isSystemAlertGranted: Boolean,
    targetAppName: String,
    targetPackage: String,
    onToggleService: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
    ) {
        // Holographic service status card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (isServiceActive) listOf(
                                AccentCyan.copy(alpha = 0.15f),
                                AccentIndigo.copy(alpha = 0.15f)
                            ) else listOf(
                                GlassSurface, GlassSurface
                            )
                        )
                    )
                    .border(
                        1.dp,
                        if (isServiceActive) AccentCyan.copy(alpha = 0.5f) else BorderAccent,
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (isServiceActive) AccentTurquoise else DangerRose)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isServiceActive) "ऑटो-लॉन्च सक्रिय है" else "ऑटो-लॉन्च रुका हुआ है",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightText
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isServiceActive) "चयनित ऐप्स में नोटिफिकेशन आने पर निर्धारित ऐप लॉन्च होगी।" else "मॉनिटरिंग रुकी हुई है। सर्विस चलाने के लिए ऑन करें।",
                            fontSize = 12.sp,
                            color = SoftText
                        )
                    }

                    Switch(
                        checked = isServiceActive,
                        onCheckedChange = { onToggleService() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentCyan,
                            checkedTrackColor = AccentCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = SoftText,
                            uncheckedTrackColor = GlassSurface
                        ),
                        modifier = Modifier.testTag("service_toggle")
                    )
                }
            }
        }

        // Selected Target App Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GlassSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderAccent)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "लक्ष्य एप्लिकेशन (Target App)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AccentCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (targetPackage.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SpaceBackground)
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = AccentTurquoise,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = targetAppName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )
                                Text(
                                    text = targetPackage,
                                    fontSize = 11.sp,
                                    color = SoftText,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SpaceBackground)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "कोई लक्ष्य ऐप चयनित नहीं है।\nकृपया 'ऐप चयन' टैब से एक ऐप टैप करके सेट करें।",
                                fontSize = 13.sp,
                                color = SoftText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Permissions Checklist Card
        item {
            Text(
                text = "आवश्यक सिस्टम अनुमतियाँ (Permissions)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = LightText,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        item {
            PermissionCard(
                title = "1. नोटिफिकेशन लिस्नर सेवा",
                description = "चुने गए ऐप्स में जब नई नोटिफिकेशन आएगी तो उसे पढ़ने और मॉनिटर करने के लिए आवश्यक है।",
                isGranted = isNotificationListenerGranted,
                onRequest = onRequestNotificationPermission,
                tag = "notif_permission"
            )
        }

        item {
            PermissionCard(
                title = "2. सुगम्यता (Accessibility) सेवा",
                description = "सिस्टम लेवल पर बैकग्राउंड से लक्ष्य ऐप को स्वचालित रूप से तुरंत लॉन्च करने में मदद करता है।",
                isGranted = isAccessibilityGranted,
                onRequest = onRequestAccessibilityPermission,
                tag = "a11y_permission"
            )
        }

        item {
            PermissionCard(
                title = "3. अन्य ऐप्स के ऊपर दिखाएँ (Overlay)",
                description = "Android बैकग्राउंड एक्जीक्यूशन सुरक्षा सीमा को बायपास करके लक्ष्य ऐप को स्वतः ओपन करने की अनुमति देता है।",
                isGranted = isSystemAlertGranted,
                onRequest = onRequestOverlayPermission,
                tag = "overlay_permission"
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderAccent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = LightText
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isGranted) AccentTurquoise.copy(alpha = 0.15f)
                            else DangerRose.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isGranted) "सक्रिय है" else "अनुमति आवश्यक",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) AccentTurquoise else DangerRose
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = SoftText,
                lineHeight = 16.sp
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag(tag)
                ) {
                    Text(text = "परमिशन इनेबल करें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppSelectorTab(
    apps: List<DisplayAppInfo>,
    searchQuery: String,
    targetPackage: String,
    onSearchQueryChanged: (String) -> Unit,
    onToggleMonitor: (DisplayAppInfo, Boolean) -> Unit,
    onSetTarget: (DisplayAppInfo) -> Unit,
    onSimulate: (DisplayAppInfo) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            border = BorderStroke(1.dp, BorderAccent),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "निर्देश: मॉनिटर करने के लिए चेकबॉक्स ☑️ टिक करें।",
                    fontSize = 12.sp,
                    color = AccentTurquoise,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "टैप करें: लक्ष्य एप्लिकेशन (Target App) के रूप में सेट करने के लिए।\nडबल टैप / लॉन्ग प्रेस: त्वरित नोटिफिकेशन लॉन्च सिम्युलेटर चलाएं।",
                    fontSize = 11.sp,
                    color = SoftText,
                    lineHeight = 15.sp
                )
            }
        }

        // Modern Fillable Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("ऐप्स खोजें (जैसे WhatsApp, Messages)...", color = SoftText, fontSize = 13.sp) },
            prefix = { Icon(Icons.Filled.Search, contentDescription = null, tint = SoftText, modifier = Modifier.size(18.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = BorderAccent,
                focusedContainerColor = GlassSurface,
                unfocusedContainerColor = GlassSurface
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = SoftText,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "कोई ऐप नहीं मिला",
                        fontSize = 14.sp,
                        color = SoftText
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(apps, key = { it.packageName }) { appInfo ->
                    val isTagTarget = targetPackage == appInfo.packageName
                    val densityBrush = remember(appInfo.isMonitored, isTagTarget) {
                        Brush.linearGradient(
                            if (isTagTarget) listOf(AccentCyan.copy(alpha = 0.12f), AccentIndigo.copy(alpha = 0.15f))
                            else if (appInfo.isMonitored) listOf(AccentTurquoise.copy(alpha = 0.08f), GlassSurface)
                            else listOf(GlassSurface, GlassSurface)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(densityBrush)
                            .border(
                                1.dp,
                                if (isTagTarget) AccentCyan
                                else if (appInfo.isMonitored) AccentTurquoise.copy(alpha = 0.5f)
                                else BorderAccent,
                                RoundedCornerShape(16.dp)
                            )
                            .combinedClickable(
                                onClick = { onSetTarget(appInfo) },
                                onLongClick = { onSimulate(appInfo) },
                                onDoubleClick = { onSimulate(appInfo) }
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Virtual App Avatar icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isTagTarget) AccentCyan.copy(alpha = 0.2f)
                                        else AccentIndigo.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isTagTarget) Icons.Filled.PlayArrow else Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = if (isTagTarget) AccentCyan else AccentIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = appInfo.appName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LightText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isTagTarget) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(AccentCyan)
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "TARGET",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = appInfo.packageName,
                                    fontSize = 11.sp,
                                    color = SoftText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = { onSimulate(appInfo) },
                                colors = ButtonDefaults.buttonColors(containerColor = SpaceBackground),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .padding(end = 6.dp)
                            ) {
                                Text("सिम्युलेट", fontSize = 10.sp, color = AccentCyan)
                            }

                            Checkbox(
                                checked = appInfo.isMonitored,
                                onCheckedChange = { checked -> onToggleMonitor(appInfo, checked) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AccentTurquoise,
                                    uncheckedColor = SoftText
                                ),
                                modifier = Modifier.testTag("checkbox_${appInfo.packageName}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventsTab(
    logs: List<LogEntry>,
    onClearLogs: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "रीयल-टाइम गतिविधि लॉग्स",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AccentCyan
            )

            if (logs.isNotEmpty()) {
                TextButton(
                    onClick = onClearLogs,
                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRose)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("लॉग साफ़ करें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassSurface)
                    .border(1.dp, BorderAccent, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = null,
                        tint = SoftText,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "अभी तक कोई नोटिफिकेशन/एक्टिविटी लॉग नहीं मिला है।",
                        fontSize = 13.sp,
                        color = SoftText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassSurface)
                    .border(1.dp, BorderAccent, RoundedCornerShape(16.dp)),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(logs) { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpaceBackground.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AccentIndigo.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = entry.sourcePackage.substringAfterLast("."),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentIndigo
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = entry.time,
                                    fontSize = 10.sp,
                                    color = SoftText
                                )
                            }

                            // Action status indicator
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (entry.actionTaken.contains("Launched", ignoreCase = true) || entry.actionTaken.contains("Success", ignoreCase = true))
                                            AccentTurquoise.copy(alpha = 0.15f)
                                        else BorderAccent
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = entry.actionTaken,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (entry.actionTaken.contains("Launched", ignoreCase = true) || entry.actionTaken.contains("Success", ignoreCase = true))
                                        AccentTurquoise
                                    else SoftText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = entry.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                        Text(
                            text = entry.body,
                            fontSize = 11.sp,
                            color = SoftText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    HorizontalDivider(color = BorderAccent.copy(alpha = 0.5f))
                }
            }
        }
    }
}
