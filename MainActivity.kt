package com.frebee.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FrebeeApp() }
    }
}

private data class ChatMessage(val role: String, val text: String)

@Composable
private fun FrebeeApp() {
    var futuristic by remember { mutableStateOf(false) }
    var roast by remember { mutableFloatStateOf(35f) }
    var provider by remember { mutableStateOf("Demo mode") }
    var endpoint by remember { mutableStateOf("https://api.openai.com/v1/chat/completions") }
    var model by remember { mutableStateOf("gpt-4o-mini") }
    var apiKey by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Demo mode: no API key required") }
    val messages = remember { mutableStateListOf(ChatMessage("assistant", "Hey! I'm Frebee. Choose a mood, connect a compatible model, or chat with demo mode to explore the interface.")) }
    val scope = rememberCoroutineScope()

    val bg = if (futuristic) Color(0xFF080D1A) else Color(0xFFF6F7FB)
    val panel = if (futuristic) Color(0xFF121B30) else Color.White
    val ink = if (futuristic) Color(0xFFEAF1FF) else Color(0xFF172033)
    val accent = if (futuristic) Color(0xFF54E5FF) else Color(0xFF635BFF)

    MaterialTheme(colorScheme = if (futuristic) darkColorScheme(
        primary = accent, background = bg, surface = panel, onSurface = ink, onBackground = ink
    ) else lightColorScheme(primary = accent, background = bg, surface = panel, onSurface = ink, onBackground = ink)) {
        Scaffold(containerColor = bg, topBar = {
            Row(
                Modifier.fillMaxWidth().background(panel).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(42.dp).background(accent, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = if (futuristic) Color.Black else Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Frebee", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ink)
                    Text(if (provider == "Demo mode") "Your AI workspace · Demo" else "Connected · $model",
                        fontSize = 11.sp, color = ink.copy(alpha = .65f))
                }
                IconButton(onClick = { futuristic = !futuristic }) {
                    Icon(if (futuristic) Icons.Default.LightMode else Icons.Default.Bolt, contentDescription = "Switch theme", tint = accent)
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Tune, contentDescription = "Settings", tint = ink)
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(onClick = { provider = "Demo mode"; status = "Demo mode: no API key required" }, label = { Text("Demo") }, leadingIcon = { Icon(Icons.Default.Chat, null) })
                    AssistChip(onClick = { provider = "Cloud API"; showSettings = true }, label = { Text("Cloud API") }, leadingIcon = { Icon(Icons.Default.Cloud, null) })
                    AssistChip(onClick = { provider = "On-device"; status = "On-device models are planned; this starter does not bundle a local model." }, label = { Text("On-device") }, leadingIcon = { Icon(Icons.Default.PhoneAndroid, null) })
                }
                Card(colors = CardDefaults.cardColors(containerColor = panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Mood Studio", fontWeight = FontWeight.SemiBold, color = ink, modifier = Modifier.weight(1f))
                            Text("${roast.toInt()}% roast", color = accent, fontSize = 12.sp)
                        }
                        Slider(value = roast, onValueChange = { roast = it }, valueRange = 0f..100f)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kind", color = ink.copy(alpha = .7f), fontSize = 11.sp)
                            Text("Playful", color = ink.copy(alpha = .7f), fontSize = 11.sp)
                            Text("Savage comedy", color = ink.copy(alpha = .7f), fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(status, fontSize = 11.sp, color = ink.copy(alpha = .6f), modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(messages) { msg ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.role == "user") Arrangement.End else Arrangement.Start) {
                            Surface(
                                color = if (msg.role == "user") accent else panel,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.widthIn(max = 310.dp)
                            ) {
                                Text(msg.text, modifier = Modifier.padding(13.dp),
                                    color = if (msg.role == "user" && !futuristic) Color.White else ink, fontSize = 14.sp)
                            }
                        }
                    }
                    if (busy) item { Text("Frebee is thinking…", color = accent, modifier = Modifier.padding(8.dp)) }
                }
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 10.dp)) {
                    OutlinedTextField(
                        value = prompt, onValueChange = { prompt = it },
                        modifier = Modifier.weight(1f), placeholder = { Text("Message Frebee…") },
                        maxLines = 5, shape = RoundedCornerShape(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        enabled = prompt.isNotBlank() && !busy,
                        onClick = {
                            val text = prompt.trim()
                            prompt = ""
                            messages.add(ChatMessage("user", text))
                            busy = true
                            scope.launch {
                                val answer = if (provider == "Cloud API") {
                                    withContext(Dispatchers.IO) { callCloud(endpoint, model, apiKey, messages.toList(), roast) }
                                } else {
                                    demoReply(text, roast)
                                }
                                messages.add(ChatMessage("assistant", answer))
                                status = if (provider == "Cloud API" && answer.startsWith("Connection error:")) "Cloud connection failed" else "Ready"
                                busy = false
                            }
                        }, modifier = Modifier.size(52.dp)
                    ) { Icon(Icons.Default.Send, contentDescription = "Send") }
                }
            }
        }

        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("Frebee settings") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Provider", fontWeight = FontWeight.SemiBold)
                        Text("Uses an OpenAI-compatible Chat Completions endpoint. Other providers may need a different endpoint or adapter.", fontSize = 12.sp)
                        OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, label = { Text("API endpoint") }, singleLine = true)
                        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model ID") }, singleLine = true)
                        OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API key (memory only)") }, singleLine = true)
                        Text("Security: this starter keeps the key in memory only and does not save it. For public release, route provider calls through a secure backend; never embed a shared secret in an APK.", fontSize = 11.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        provider = "Cloud API"
                        status = if (apiKey.isBlank()) "Cloud selected — enter an API key to connect" else "Cloud configured for $model"
                        showSettings = false
                    }) { Text("Save for this session") }
                },
                dismissButton = { TextButton(onClick = { showSettings = false }) { Text("Cancel") } }
            )
        }
    }
}

private fun demoReply(text: String, roast: Float): String {
    val intro = when {
        roast < 20 -> "Happy to help! "
        roast < 55 -> "Alright, let's figure this out. "
        roast < 80 -> "Okay, chaos coordinator 😄 — "
        else -> "Savage-comedy mode activated (keeping it playful): "
    }
    return intro + "I'm currently running in demo mode, so I don't have a real language model connected yet. Your message was: “$text”. Open Settings to connect an OpenAI-compatible API, or keep building Frebee's features."
}

private fun callCloud(endpoint: String, model: String, key: String, messages: List<ChatMessage>, roast: Float): String {
    if (key.isBlank()) return "Connection error: add an API key in Settings. Don't share it with anyone."
    return try {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 60000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer $key")
        val mood = when {
            roast < 20 -> "Be kind, patient, and supportive."
            roast < 55 -> "Be friendly and lightly humorous."
            roast < 80 -> "Use sarcastic, playful comedy, but do not use slurs or target protected traits."
            else -> "Use bold savage comedy and witty roasts, but avoid slurs, threats, and degrading abuse."
        }
        val arr = JSONArray()
        arr.put(JSONObject().put("role", "system").put("content", "You are Frebee, a helpful all-round assistant for coding, learning, reasoning, and research. $mood Be honest about uncertainty."))
        messages.takeLast(24).forEach { arr.put(JSONObject().put("role", it.role).put("content", it.text)) }
        val body = JSONObject().put("model", model).put("messages", arr).put("temperature", 0.7).toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        connection.disconnect()
        if (code !in 200..299) "Connection error: HTTP $code — ${response.take(500)}"
        else JSONObject(response).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
            ?: "The provider returned an unexpected response format."
    } catch (e: Exception) {
        "Connection error: ${e.message ?: "Unknown network error"}"
    }
}
