package com.prabhat.alarm

import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val Night = Color(0xFF10131A)
private val CardColor = Color(0xFF1B202B)
private val CardRaised = Color(0xFF242A37)
private val Muted = Color(0xFF9CA3B5)
private val Lavender = Color(0xFFB7A7FF)
private val Saffron = Color(0xFFFFC477)
private val Mint = Color(0xFF81E0C3)
private val Line = Color(0xFF343A4A)

@Composable
fun PrabhatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Lavender,
            onPrimary = Color(0xFF171428),
            secondary = Saffron,
            onSecondary = Color(0xFF25190B),
            background = Night,
            onBackground = Color(0xFFF4F3FB),
            surface = CardColor,
            onSurface = Color(0xFFF4F3FB),
            surfaceVariant = CardRaised,
            onSurfaceVariant = Muted,
            error = Color(0xFFFF9B9B)
        ),
        content = content
    )
}

@Composable
fun MainScreen(
    latitude: Double?,
    longitude: Double?,
    locationStatus: String,
    onRequestLocation: () -> Unit,
    onOpenFullScreenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(0) }
    var alarms by remember { mutableStateOf(AlarmStore.getAlarms(context)) }
    var brahmaEnabled by remember { mutableStateOf(AlarmStore.isBrahmaEnabled(context)) }
    var offset by remember { mutableFloatStateOf(AlarmStore.brahmaOffset(context).toFloat()) }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = ZonedDateTime.now()
        }
    }

    val savedLocation = AlarmStore.location(context)
    val lat = latitude ?: savedLocation?.first
    val lon = longitude ?: savedLocation?.second
    val solarInfo = if (lat != null && lon != null) {
        SolarCalculator.info(now.toLocalDate(), lat, lon, offset.roundToInt())
    } else null
    val nextSolarInfo = if (lat != null && lon != null) {
        val today = solarInfo
        if (today != null && !today.recommended.isAfter(now)) {
            SolarCalculator.info(now.toLocalDate().plusDays(1), lat, lon, offset.roundToInt()) ?: today
        } else today
    } else null

    fun updateBrahma(enabled: Boolean) {
        if (enabled && (lat == null || lon == null)) {
            scope.launch { snackbar.showSnackbar("Set your location first to calculate sunrise") }
            onRequestLocation()
            return
        }
        AlarmStore.setBrahmaEnabled(context, enabled)
        brahmaEnabled = enabled
        if (enabled) {
            if (!AlarmScheduler.scheduleBrahma(context)) {
                AlarmStore.setBrahmaEnabled(context, false)
                brahmaEnabled = false
                scope.launch { snackbar.showSnackbar("Could not calculate sunrise for this location") }
            } else {
                scope.launch { snackbar.showSnackbar("Brahma Muhurta alarm enabled") }
            }
        } else {
            AlarmScheduler.cancelBrahma(context)
            scope.launch { snackbar.showSnackbar("Brahma Muhurta alarm turned off") }
        }
    }

    fun persistOffset() {
        val minutes = offset.roundToInt().coerceIn(48, 96)
        AlarmStore.setBrahmaOffset(context, minutes)
        if (brahmaEnabled) AlarmScheduler.scheduleBrahma(context)
    }

    fun editAlarm(existing: UserAlarm? = null) {
        val initialHour = existing?.hour ?: 6
        val initialMinute = existing?.minute ?: 0
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val edited = existing?.copy(hour = hour, minute = minute)
                    ?: UserAlarm(id = System.currentTimeMillis(), hour = hour, minute = minute, enabled = true, label = "Daily alarm")
                val next = (alarms.filterNot { it.id == edited.id } + edited).sortedWith(compareBy<UserAlarm> { it.hour }.thenBy { it.minute })
                AlarmStore.saveAlarms(context, next)
                alarms = next
                AlarmScheduler.scheduleRegular(context, edited)
            },
            initialHour,
            initialMinute,
            false
        ).show()
    }

    fun changeAlarmEnabled(alarm: UserAlarm, enabled: Boolean) {
        val edited = alarm.copy(enabled = enabled)
        val next = alarms.map { if (it.id == alarm.id) edited else it }
        AlarmStore.saveAlarms(context, next)
        alarms = next
        if (enabled) AlarmScheduler.scheduleRegular(context, edited) else AlarmScheduler.cancelRegular(context, alarm.id)
    }

    Scaffold(
        containerColor = Night,
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF151923),
                contentColor = Muted,
                windowInsets = WindowInsets.navigationBars
            ) {
                val items = listOf(
                    Triple("Alarms", Icons.Filled.Alarm, 0),
                    Triple("Muhurta", Icons.Filled.SelfImprovement, 1),
                    Triple("Settings", Icons.Filled.Settings, 2)
                )
                items.forEach { item ->
                    NavigationBarItem(
                        selected = selectedTab == item.third,
                        onClick = { selectedTab = item.third },
                        icon = { Icon(item.second, contentDescription = item.first) },
                        label = { Text(item.first, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Lavender,
                            selectedTextColor = Color.White,
                            indicatorColor = Color(0xFF302B49),
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> AlarmHome(
                modifier = Modifier.padding(padding), now = now, locationStatus = locationStatus,
                solarInfo = nextSolarInfo, lat = lat, lon = lon,
                brahmaEnabled = brahmaEnabled, onBrahmaChange = ::updateBrahma,
                alarms = alarms, onAddAlarm = { editAlarm() }, onEditAlarm = { editAlarm(it) },
                onToggleAlarm = ::changeAlarmEnabled,
                onDeleteAlarm = { alarm ->
                    AlarmScheduler.cancelRegular(context, alarm.id)
                    AlarmStore.removeAlarm(context, alarm.id)
                    alarms = alarms.filterNot { it.id == alarm.id }
                }, onRequestLocation = onRequestLocation
            )
            1 -> MuhurtaPage(
                modifier = Modifier.padding(padding), solarInfo = solarInfo, lat = lat, lon = lon,
                offset = offset, onOffsetChange = { offset = it }, onOffsetCommit = { persistOffset() },
                enabled = brahmaEnabled, onEnabledChange = ::updateBrahma,
                locationStatus = locationStatus, onRequestLocation = onRequestLocation
            )
            else -> SettingsPage(
                modifier = Modifier.padding(padding), locationStatus = locationStatus,
                lat = lat, lon = lon, onRequestLocation = onRequestLocation,
                onOpenFullScreenSettings = onOpenFullScreenSettings,
                offset = offset, onOffsetChange = { offset = it }, onOffsetCommit = { persistOffset() },
                enabled = brahmaEnabled, onEnabledChange = ::updateBrahma
            )
        }
    }
}

@Composable
private fun AlarmHome(
    modifier: Modifier,
    now: ZonedDateTime,
    locationStatus: String,
    solarInfo: MuhurtaInfo?,
    lat: Double?, lon: Double?,
    brahmaEnabled: Boolean,
    onBrahmaChange: (Boolean) -> Unit,
    alarms: List<UserAlarm>,
    onAddAlarm: () -> Unit,
    onEditAlarm: (UserAlarm) -> Unit,
    onToggleAlarm: (UserAlarm, Boolean) -> Unit,
    onDeleteAlarm: (UserAlarm) -> Unit,
    onRequestLocation: () -> Unit
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(greeting(now.hour), color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(5.dp))
                Text("Make mornings\nmeaningful.", color = Color.White, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(now.format(DateTimeFormatter.ofPattern("h:mm")), color = Color.White, fontSize = 33.sp, fontWeight = FontWeight.Light)
                Text(now.format(DateTimeFormatter.ofPattern("a")), color = Lavender, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(now.toLocalDate().toReadableDate(), color = Muted, fontSize = 10.sp, textAlign = TextAlign.End)
            }
        }

        BrahmaCard(
            solarInfo = solarInfo,
            brahmaEnabled = brahmaEnabled,
            onBrahmaChange = onBrahmaChange,
            locationStatus = locationStatus,
            hasLocation = lat != null && lon != null,
            onRequestLocation = onRequestLocation
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Your alarms", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Simple routines, reliably on time", color = Muted, fontSize = 12.sp)
            }
            Button(
                onClick = onAddAlarm,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lavender, contentColor = Color(0xFF171428))
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("Add alarm", fontWeight = FontWeight.SemiBold)
            }
        }

        if (alarms.isEmpty()) {
            Surface(shape = RoundedCornerShape(22.dp), color = CardColor, border = BorderStroke(1.dp, Line)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 26.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Alarm, contentDescription = null, tint = Color(0xFF727A91), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("No regular alarms yet", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Add an everyday alarm, or activate Brahma Muhurta above.", color = Muted, textAlign = TextAlign.Center, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        } else {
            alarms.forEach { alarm ->
                AlarmRow(alarm, onEdit = { onEditAlarm(alarm) }, onToggle = { onToggleAlarm(alarm, it) }, onDelete = { onDeleteAlarm(alarm) })
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            Icon(Icons.Filled.LocationOn, tint = Muted, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (lat != null && lon != null) "Sunrise calculated for your saved location" else "Set location for sunrise-based timing", color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("Update", color = Lavender, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onRequestLocation() }.padding(6.dp))
        }
    }
}

@Composable
private fun BrahmaCard(
    solarInfo: MuhurtaInfo?,
    brahmaEnabled: Boolean,
    onBrahmaChange: (Boolean) -> Unit,
    locationStatus: String,
    hasLocation: Boolean,
    onRequestLocation: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(
            Brush.linearGradient(listOf(Color(0xFF302A4B), Color(0xFF24283B), Color(0xFF1B302F)))
        ).padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(43.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFF484061)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.WbTwilight, contentDescription = null, tint = Saffron, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("BRAHMA MUHURTA", color = Saffron, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text("Your sunrise alarm", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
                Switch(
                    checked = brahmaEnabled,
                    onCheckedChange = onBrahmaChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161426), checkedTrackColor = Mint, uncheckedThumbColor = Muted, uncheckedTrackColor = Color(0xFF444A59))
                )
            }
            if (solarInfo != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("NEXT WAKE-UP", color = Color(0xFFB9B3D8), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        Text(solarInfo.recommended.format(DateTimeFormatter.ofPattern("h:mm")), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light, lineHeight = 48.sp)
                    }
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 6.dp)) {
                        Text(solarInfo.recommended.format(DateTimeFormatter.ofPattern("a")), color = Saffron, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(if (brahmaEnabled) "Alarm enabled" else "Alarm is off", color = if (brahmaEnabled) Mint else Muted, fontSize = 11.sp)
                    }
                }
                HorizontalDivider(color = Color(0x446D7287))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TimeMiniCard("MUHURTA WINDOW", "${solarInfo.windowStart.format(DateTimeFormatter.ofPattern("h:mm"))} – ${solarInfo.windowEnd.format(DateTimeFormatter.ofPattern("h:mm a"))}", Modifier.weight(1.2f))
                    TimeMiniCard("SUNRISE", solarInfo.sunrise.format(DateTimeFormatter.ofPattern("h:mm a")), Modifier.weight(0.8f))
                }
                Text("Calculated daily from your location · 48-minute muhurta", color = Color(0xFFB9BDCA), fontSize = 11.sp)
            } else {
                Text("Let sunrise set your morning rhythm.", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text("Allow location access to calculate tomorrow's timing for your area.", color = Color(0xFFB9BDCA), fontSize = 12.sp, lineHeight = 18.sp)
                Button(
                    onClick = onRequestLocation,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Saffron, contentColor = Color(0xFF281A0B))
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Use current location", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TimeMiniCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(15.dp)).background(Color(0x26373D54)).padding(horizontal = 11.dp, vertical = 10.dp)) {
        Text(label, color = Color(0xFFAAAFC2), fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AlarmRow(alarm: UserAlarm, onEdit: () -> Unit, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = CardColor, border = BorderStroke(1.dp, if (alarm.enabled) Color(0xFF353A4B) else Color(0xFF252A35))) {
        Row(Modifier.fillMaxWidth().padding(start = 17.dp, end = 10.dp, top = 15.dp, bottom = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(if (alarm.enabled) Color(0xFF302B49) else Color(0xFF252A35)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AccessTime, contentDescription = null, tint = if (alarm.enabled) Lavender else Muted)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f).clickable { onEdit() }) {
                Text(alarm.label, color = if (alarm.enabled) Color.White else Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(String.format("%d:%02d", if (alarm.hour % 12 == 0) 12 else alarm.hour % 12, alarm.minute), color = if (alarm.enabled) Color.White else Muted, fontSize = 29.sp, fontWeight = FontWeight.Light)
                    Spacer(Modifier.width(5.dp))
                    Text(if (alarm.hour < 12) "AM" else "PM", color = Lavender, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 5.dp))
                }
                Text("Every day", color = Muted, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF171428), checkedTrackColor = Lavender, uncheckedThumbColor = Muted, uncheckedTrackColor = Color(0xFF3C4250))
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete alarm", tint = Color(0xFF8E94A6), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun MuhurtaPage(
    modifier: Modifier,
    solarInfo: MuhurtaInfo?, lat: Double?, lon: Double?, offset: Float,
    onOffsetChange: (Float) -> Unit, onOffsetCommit: () -> Unit, enabled: Boolean, onEnabledChange: (Boolean) -> Unit,
    locationStatus: String, onRequestLocation: () -> Unit
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageHeading("Brahma Muhurta", "A sunrise-aware start to your day")
        if (solarInfo != null) {
            Surface(shape = RoundedCornerShape(26.dp), color = CardColor, border = BorderStroke(1.dp, Line)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("TODAY'S WINDOW", color = Saffron, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text("${solarInfo.windowStart.format(DateTimeFormatter.ofPattern("h:mm a"))} – ${solarInfo.windowEnd.format(DateTimeFormatter.ofPattern("h:mm a"))}", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Medium)
                    HorizontalDivider(color = Line)
                    DetailLine("Astronomical sunrise", solarInfo.sunrise.format(DateTimeFormatter.ofPattern("h:mm a")))
                    DetailLine("Selected wake-up", solarInfo.recommended.format(DateTimeFormatter.ofPattern("h:mm a")))
                    DetailLine("Calculation location", if (lat != null && lon != null) "%.4f, %.4f".format(lat, lon) else "Not set")
                }
            }
        } else {
            EmptyLocationCard(locationStatus, onRequestLocation)
        }
        Surface(shape = RoundedCornerShape(24.dp), color = CardColor, border = BorderStroke(1.dp, Line)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Daily Brahma alarm", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("Recomputed for local sunrise", color = Muted, fontSize = 12.sp)
                    }
                    Switch(checked = enabled, onCheckedChange = onEnabledChange)
                }
                Spacer(Modifier.height(20.dp))
                Text("Alarm offset", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("${offset.roundToInt()} minutes before sunrise", color = Lavender, fontSize = 13.sp)
                Slider(value = offset, onValueChange = onOffsetChange, onValueChangeFinished = onOffsetCommit, valueRange = 48f..96f, steps = 47)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("48 min", color = Muted, fontSize = 11.sp)
                    Text("72 min · suggested", color = Saffron, fontSize = 11.sp)
                    Text("96 min", color = Muted, fontSize = 11.sp)
                }
            }
        }
        Text("Brahma Muhurta is commonly described as the period beginning 96 minutes before sunrise and ending 48 minutes before sunrise. The selected alarm defaults to the midpoint (72 minutes before sunrise); this is a traditional timing convention, not a medical or sleep recommendation.", color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun SettingsPage(
    modifier: Modifier,
    locationStatus: String,
    lat: Double?, lon: Double?, onRequestLocation: () -> Unit,
    onOpenFullScreenSettings: () -> Unit,
    offset: Float, onOffsetChange: (Float) -> Unit, onOffsetCommit: () -> Unit,
    enabled: Boolean, onEnabledChange: (Boolean) -> Unit
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageHeading("Settings", "Make your alarm work for your routine")
        Surface(shape = RoundedCornerShape(24.dp), color = CardColor, border = BorderStroke(1.dp, Line)) {
            Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF2E3543)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Mint)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Location for sunrise", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(locationStatus, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                        if (lat != null && lon != null) Text("${"%.5f".format(lat)}, ${"%.5f".format(lon)}", color = Color(0xFFBFC4D1), fontSize = 11.sp)
                    }
                    IconButton(onClick = onRequestLocation) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh location", tint = Lavender)
                    }
                }
                Button(onClick = onRequestLocation, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = CardRaised, contentColor = Color.White)) {
                    Text("Refresh current location")
                }
            }
        }
        Surface(shape = RoundedCornerShape(24.dp), color = CardColor, border = BorderStroke(1.dp, Line)) {
            Column(Modifier.fillMaxWidth().padding(19.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WbTwilight, contentDescription = null, tint = Saffron, modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Brahma Muhurta", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Daily sunrise-based alarm", color = Muted, fontSize = 12.sp)
                    }
                    Switch(checked = enabled, onCheckedChange = onEnabledChange)
                }
                Spacer(Modifier.height(15.dp))
                Text("Wake-up offset · ${offset.roundToInt()} min before sunrise", color = Lavender, fontSize = 13.sp)
                Slider(value = offset, onValueChange = onOffsetChange, onValueChangeFinished = onOffsetCommit, valueRange = 48f..96f, steps = 47)
            }
        }
        Surface(shape = RoundedCornerShape(24.dp), color = CardColor, border = BorderStroke(1.dp, Line)) {
            Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Lavender, modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Full-screen alarm", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Show the ringing challenge over the lock screen", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
                Button(onClick = onOpenFullScreenSettings, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = CardRaised, contentColor = Color.White)) {
                    Text("Open Android alarm display settings")
                }
            }
        }
        Surface(shape = RoundedCornerShape(24.dp), color = CardColor, border = BorderStroke(1.dp, Line)) {
            Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Privacy & reliability", color = Color.White, fontWeight = FontWeight.SemiBold)
                DetailLine("Location", "Foreground permission only; coordinates stay on this device")
                DetailLine("Sunrise", "Calculated offline with a NOAA-style solar equation")
                DetailLine("Alarm sound", "Uses the system alarm ringtone")
                DetailLine("Daily refresh", "Recalculates on app launch, date/time changes and reboot")
            }
        }
        Text("For reliable delivery, allow notifications and full-screen alarms if your Android version asks. Some phone manufacturers also have battery or auto-start controls that can affect background alarms.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun EmptyLocationCard(locationStatus: String, onRequestLocation: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = CardColor, border = BorderStroke(1.dp, Line)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Saffron, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(10.dp))
            Text("Location needed", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text(locationStatus, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRequestLocation, shape = RoundedCornerShape(14.dp)) { Text("Set current location") }
        }
    }
}

@Composable
private fun PageHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(0.9f))
        Spacer(Modifier.width(8.dp))
        Text(value, color = Color(0xFFEAEAF3), fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
    }
}

private fun greeting(hour: Int): String = when (hour) {
    in 4..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
