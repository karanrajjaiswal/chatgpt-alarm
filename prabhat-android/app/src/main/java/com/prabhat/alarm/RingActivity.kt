package com.prabhat.alarm

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

class RingActivity : ComponentActivity() {
    private var alarmId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        readIntent(intent)
        setContent {
            PrabhatTheme {
                RingScreen(
                    label = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "Time to wake up",
                    isBrahma = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TYPE) == AlarmScheduler.TYPE_BRAHMA,
                    onDismiss = {
                        AlarmRingingService.stop(this)
                        finishAndRemoveTask()
                    },
                    onSnooze = {
                        AlarmRingingService.stop(this)
                        AlarmScheduler.scheduleSnooze(this)
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: android.content.Intent?) {
        alarmId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L) ?: -1L
    }
}

@Composable
private fun RingScreen(label: String, isBrahma: Boolean, onDismiss: () -> Unit, onSnooze: () -> Unit) {
    val first = remember { Random.nextInt(13, 48) }
    val second = remember { Random.nextInt(12, 39) }
    val answer = first + second
    var typed by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF24203B), Color(0xFF10131A), Color(0xFF10131A)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("PRABHAT", color = Color(0xFFB7A7FF), fontWeight = FontWeight.Bold, letterSpacing = 5.sp, fontSize = 12.sp)
            Spacer(Modifier.height(26.dp))
            Text(if (isBrahma) "Your quiet hour begins" else "Good morning", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(label, color = Color(0xFFB9BDCA), fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF1C202B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF353348))
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WAKE-UP CHALLENGE", color = Color(0xFF8F94A8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
                    Spacer(Modifier.height(18.dp))
                    Text("Solve to dismiss", color = Color.White, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("$first + $second = ?", color = Color(0xFFFFC477), fontSize = 38.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it.filter(Char::isDigit).take(3); error = false },
                        singleLine = true,
                        placeholder = { Text("Your answer") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = error,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error) {
                        Spacer(Modifier.height(6.dp))
                        Text("Not quite. Try again.", color = Color(0xFFFF9B9B), fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { if (typed.toIntOrNull() == answer) onDismiss() else error = true },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7A7FF), contentColor = Color(0xFF161426))
                    ) {
                        Text("Check & dismiss", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSnooze) { Text("Snooze 5 minutes", color = Color(0xFFB9BDCA)) }
            }
            Spacer(Modifier.height(20.dp))
            Text("Take a breath. Begin gently.", color = Color(0xFF777D91), fontSize = 13.sp)
        }
    }
}
