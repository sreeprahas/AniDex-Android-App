package com.anidex

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.anidex.auth.MalAuth
import com.anidex.data.AnimeEntry
import com.anidex.data.MalApi
import kotlinx.coroutines.launch

private val Ink = Color(0xFF10111A)
private val Panel = Color(0xFF1D1E2B)
private val Violet = Color(0xFFB7A2FF)

class MainActivity : ComponentActivity() {
    private val api = MalApi()
    private var verifier: String? = null
    private val clientId = "ADD_YOUR_MAL_CLIENT_ID"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleCallback(intent)
        setContent {
            val dark = isSystemInDarkTheme()
            val scheme = if (dark) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
            MaterialTheme(colorScheme = scheme) {
                AniDexApp(api, clientId, { v -> verifier = v }, { code ->
                    val v = verifier
                    if (v != null) lifecycleScope.launch {
                        runCatching { api.exchangeCode(clientId, code, v) }
                            .onSuccess { /* token stored in API instance */ }
                    }
                })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCallback(intent)
    }

    private fun handleCallback(intent: Intent?) {
        val code = intent?.data?.getQueryParameter("code") ?: return
        val v = verifier ?: return
        lifecycleScope.launch { runCatching { api.exchangeCode(clientId, code, v) } }
    }
}

@Composable
private fun AniDexApp(api: MalApi, clientId: String, saveVerifier: (String) -> Unit, onCode: (String) -> Unit) {
    var entries by remember { mutableStateOf<List<AnimeEntry>>(sampleAnime) }
    var selected by remember { mutableStateOf("Home") }
    var search by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Connect your MAL account to import your list.") }
    val context = LocalContext.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                listOf("Home", "Library", "Discover", "Stats").forEach { item ->
                    NavigationBarItem(selected == item, onClick = { selected = item },
                        icon = { Icon(when(item) { "Home" -> Icons.Default.Home; "Library" -> Icons.Default.CollectionsBookmark; "Discover" -> Icons.Default.Search; else -> Icons.Default.BarChart }, null) },
                        label = { Text(item) })
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("YOUR ANIME UNIVERSE", style = MaterialTheme.typography.labelSmall, color = Violet)
                    Text("AniDex", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = {
                    val verifierNow = MalAuth.createVerifier()
                    saveVerifier(verifierNow)
                    runCatching { MalAuth.openLogin(context, clientId, verifierNow) }
                        .onFailure { message = it.message ?: "Could not open MAL sign in" }
                }) { Icon(Icons.Default.AccountCircle, "Connect MAL", tint = Violet) }
            }
            when (selected) {
                "Home" -> {
                    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.padding(18.dp)) {
                            Text("YOUR JOURNEY", color = Violet, style = MaterialTheme.typography.labelMedium)
                            Text("${entries.count { it.status == "completed" }} completed", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("${entries.sumOf { it.watched }} episodes watched · MAL sync", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(progress = { (entries.count { it.status == "completed" } / entries.size.coerceAtLeast(1).toFloat()).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth(), color = Violet)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Continue watching", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    AnimeList(entries.filter { it.status == "watching" }.take(4), onStep = { entry, delta ->
                        val changed = entry.copy(watched = (entry.watched + delta).coerceAtLeast(0))
                        entries = entries.map { if (it.id == entry.id) changed else it }
                        message = "Updated locally. Connect MAL and sync to save online."
                    })
                }
                "Library" -> {
                    OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search your anime") }, leadingIcon = { Icon(Icons.Default.Search, null) })
                    Spacer(Modifier.height(8.dp))
                    AnimeList(entries.filter { it.title.contains(search, true) }, onStep = { entry, delta ->
                        entries = entries.map { if (it.id == entry.id) it.copy(watched = (it.watched + delta).coerceAtLeast(0)) else it }
                    })
                }
                "Discover" -> Column {
                    Text("Discover", style = MaterialTheme.typography.headlineMedium)
                    Text("Connect MAL to import your list. Anime discovery via MAL search can be added with your client ID.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> Column {
                    Text("Your statistics", style = MaterialTheme.typography.headlineMedium)
                    StatRow("Completed", entries.count { it.status == "completed" }.toString())
                    StatRow("Watching", entries.count { it.status == "watching" }.toString())
                    StatRow("Episodes watched", entries.sumOf { it.watched }.toString())
                    StatRow("Average score", entries.map { it.score }.filter { it > 0 }.average().takeIf { it.isFinite() }?.let { "%.1f".format(it) } ?: "—")
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (clientId == "ADD_YOUR_MAL_CLIENT_ID") Text("Setup required: add your MAL Client ID in MainActivity.kt", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AnimeList(items: List<AnimeEntry>, onStep: (AnimeEntry, Int) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items, key = { it.id }) { anime ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(anime.image, contentDescription = anime.title, modifier = Modifier.size(width = 64.dp, height = 88.dp).clip(RoundedCornerShape(12.dp)))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(anime.title, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Text("${anime.watched} / ${anime.total ?: "?"} episodes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(anime.status.replaceFirstChar { it.uppercase() }, color = Violet, style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onStep(anime, 1) }) { Icon(Icons.Default.AddCircle, "Add episode", tint = Violet) }
                    Text("+1 ep", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

private val sampleAnime = listOf(
    AnimeEntry(1, "Frieren: Beyond Journey's End", null, 18, 28, "watching", 9),
    AnimeEntry(2, "Jujutsu Kaisen", null, 14, 23, "watching", 8),
    AnimeEntry(3, "Mob Psycho 100", null, 37, 37, "completed", 9),
    AnimeEntry(4, "One Piece", null, 1120, null, "watching", 10),
    AnimeEntry(5, "Violet Evergarden", null, 13, 13, "completed", 9)
)
