package com.ayaz.jarvis

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.*
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

private val cyan = Color(0xFF43D9FF)
private val dark = Color(0xFF050B14)
data class Line(val who: String, val text: String)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var recognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent {
            JarvisUi(
                context = this,
                speak = { if (ready) tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "jarvis") },
                listen = { onText, onStatus -> listen(onText, onStatus) }
            )
        }
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            val lang = tts?.setLanguage(Locale("tr", "TR"))
            if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) ready = false
        }
    }

    private fun listen(onText: (String) -> Unit, onStatus: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { onStatus("Bu cihazda konuşma tanıma servisi yok."); return }
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) { onStatus("Seni dinliyorum…") }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() { onStatus("İşleniyor…") }
                override fun onError(e: Int) { onStatus("Ses tanıma hatası ($e). Tekrar dene.") }
                override fun onResults(b: Bundle?) {
                    val s = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (s.isNullOrBlank()) onStatus("Seni anlayamadım.") else onText(s)
                }
                override fun onPartialResults(b: Bundle?) {}
                override fun onEvent(t: Int, b: Bundle?) {}
            })
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            recognizer?.startListening(i)
        } catch (e: Exception) { onStatus("Dinleme başlatılamadı: ${e.localizedMessage}") }
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun JarvisUi(context: Context, speak: (String) -> Unit, listen: ((String)->Unit, (String)->Unit)->Unit) {
    val prefs = remember { context.getSharedPreferences("jarvis_local", Context.MODE_PRIVATE) }
    var command by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Sistem hazır. Komutunu bekliyorum.") }
    var isListening by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(readArray(prefs, "notes")) }
    var history by remember { mutableStateOf(readHistory(prefs)) }

    fun respond(text: String, spoken: String = text) {
        status = spoken
        speak(spoken)
        history = (history + Line("SEN", text) + Line("JARVIS", spoken)).takeLast(60)
        writeHistory(prefs, history)
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            isListening = true
            listen({ heard ->
                isListening = false
                command = heard
                execute(heard, context, prefs, { respond(heard, it) })
            }, { status = it; isListening = false })
        } else status = "Mikrofon izni verilmedi."
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = cyan, background = dark, surface = Color(0xFF0B1B2B))) {
        Column(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF102C43), dark))).padding(18.dp)
        ) {
            Text("J.A.R.V.I.S.", color = cyan, fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Text("PERSONAL ASSISTANT • LOCAL CORE", color = Color(0xFF8AA9BC), fontSize = 10.sp)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.fillMaxWidth().weight(.65f).background(Color(0xFF091827), CircleShape).border(1.dp, cyan.copy(alpha=.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isListening) "◉" else "◎", color = cyan, fontSize = 82.sp)
                    Text(if (isListening) "LISTENING" else "STANDBY", color = cyan, letterSpacing = 3.sp)
                    Text("TURKISH VOICE INTERFACE", color = Color(0xFF8AA9BC), fontSize = 9.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(status, color = Color.White, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showNotes = !showNotes }, modifier = Modifier.weight(1f)) { Text(if (showNotes) "SOHBET" else "NOTLAR") }
                OutlinedButton(onClick = { history = emptyList(); writeHistory(prefs, history); status = "Geçmiş temizlendi." }, modifier = Modifier.weight(1f)) { Text("GEÇMİŞİ TEMİZLE") }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().background(Color(0x990B1B2B), RoundedCornerShape(12.dp)).padding(12.dp)) {
                if (showNotes) items(notes) { Text("• $it", color = Color.White, modifier = Modifier.padding(5.dp)) }
                else items(history) { line ->
                    Text(line.who, color = cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(line.text, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                }
            }
            OutlinedTextField(value = command, onValueChange = { command = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Komut yaz…") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    val c = command.trim()
                    if (c.isNotEmpty()) { execute(c, context, prefs, { respond(c, it) }); command = "" }
                }, modifier = Modifier.weight(1f)) { Text("KOMUTU GÖNDER") }
                Button(onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        isListening = true
                        listen({ heard -> isListening = false; command = heard; execute(heard, context, prefs, { respond(heard, it) }) }, { status = it; isListening = false })
                    } else permission.launch(Manifest.permission.RECORD_AUDIO)
                }, shape = CircleShape, modifier = Modifier.size(58.dp)) { Text("🎙", fontSize = 20.sp) }
            }
            Text("Örnek: saat kaç • not al süt al • hesapla 25 * 4 • YouTube aç • Google'da hava durumu ara", color = Color(0xFF8AA9BC), fontSize = 9.sp)
        }
    }
}

private fun execute(raw: String, context: Context, prefs: android.content.SharedPreferences, done: (String)->Unit) {
    val q = raw.lowercase(Locale("tr", "TR")).trim()
    val answer = when {
        q.contains("saat") -> "Saat ${SimpleDateFormat("HH:mm", Locale("tr", "TR")).format(Date())}."
        q.contains("tarih") || q.contains("bugün günlerden") -> "Bugün ${SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr", "TR")).format(Date())}."
        q.startsWith("not al ") || q.startsWith("not et ") -> {
            val note = raw.substringAfter(" ").trim()
            val list = readArray(prefs, "notes").toMutableList()
            list.add(0, note); writeArray(prefs, "notes", list.take(100))
            "Notunu cihazına kaydettim: $note"
        }
        q.startsWith("hesapla ") -> {
            val expr = raw.substringAfter(" ").replace(" ", "").replace(",", ".")
            val m = Regex("^(-?\\d+(?:\\.\\d+)?)([+*/-])(-?\\d+(?:\\.\\d+)?)$").matchEntire(expr)
            if (m == null) "Örnek biçim: hesapla 25 * 4." else {
                val a = m.groupValues[1].toDouble(); val b = m.groupValues[3].toDouble()
                val r = when (m.groupValues[2]) { "+" -> a+b; "-" -> a-b; "*" -> a*b; "/" -> if (b == 0.0) Double.NaN else a/b; else -> Double.NaN }
                if (r.isNaN()) "Bu işlem yapılamaz." else "Sonuç: ${if (r % 1.0 == 0.0) r.toLong() else r}"
            }
        }
        q.contains("youtube") && q.contains("aç") -> launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com")), "YouTube'u açıyorum.")
        q.contains("ayarlar") && q.contains("aç") -> launch(context, Intent(android.provider.Settings.ACTION_SETTINGS), "Telefon ayarlarını açıyorum.")
        q.contains("harita") || q.contains("yol tarifi") -> launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(raw))), "Haritaları açıyorum.")
        q.contains("google") || q.contains("ara") -> {
            val term = raw.replace(Regex("(?i)google'da|googlede|google|ara|arama yap"), "").trim().ifBlank { raw }
            launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(term))), "Arama sonuçlarını açıyorum.")
        }
        q.contains("yardım") || q == "ne yapabilirsin" -> "Saat, tarih, basit hesap, not kaydı, YouTube, ayarlar, harita ve Google araması yapabilirim."
        else -> "Bu komutu henüz bilmiyorum. 'Yardım' diyerek komutları öğrenebilirsin."
    }
    done(answer)
}

private fun launch(context: Context, intent: Intent, success: String): String = try {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent); success
} catch (_: Exception) { "Bu işlem için uygun uygulama bulunamadı veya açılamadı." }

private fun readArray(p: android.content.SharedPreferences, key: String): List<String> = try {
    val a = JSONArray(p.getString(key, "[]")); (0 until a.length()).map { a.getString(it) }
} catch (_: Exception) { emptyList() }
private fun writeArray(p: android.content.SharedPreferences, key: String, values: List<String>) {
    val a = JSONArray(); values.forEach { a.put(it) }; p.edit().putString(key, a.toString()).apply()
}
private fun readHistory(p: android.content.SharedPreferences): List<Line> = try {
    val a = JSONArray(p.getString("history", "[]"))
    (0 until a.length()).map { Line(a.getJSONObject(it).optString("who"), a.getJSONObject(it).optString("text")) }
} catch (_: Exception) { emptyList() }
private fun writeHistory(p: android.content.SharedPreferences, values: List<Line>) {
    val a = JSONArray()
    values.takeLast(60).forEach { a.put(org.json.JSONObject().put("who", it.who).put("text", it.text)) }
    p.edit().putString("history", a.toString()).apply()
}
