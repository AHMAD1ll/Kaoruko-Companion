package com.your_github_username.kaorukocompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.your_github_username.kaorukocompanion.data.AppDatabase
import com.your_github_username.kaorukocompanion.data.UserEntity
import com.your_github_username.kaorukocompanion.llm.LLMClient
import kotlinx.coroutines.launch

data class Message(val text: String, val isUser: Boolean)

class MainActivity : ComponentActivity() {
    private lateinit var db: AppDatabase
    private val llmClient = LLMClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "kaoruko-db"
        ).build()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    KaorukoChatScreen(db, llmClient)
                }
            }
        }
    }
}

@Composable
fun KaorukoChatScreen(db: AppDatabase, llmClient: LLMClient) {
    var userName by remember { mutableStateOf<String?>(null) }
    var favoriteAnime by remember { mutableStateOf<String?>(null) }
    val messages = remember { mutableStateListOf<Message>() }
    var currentMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Load user data on startup
    LaunchedEffect(Unit) {
        db.userDao().getUser().collect { user ->
            if (user != null) {
                userName = user.name
                favoriteAnime = user.favoriteAnime
                messages.add(Message("أهلاً بعودتك، ${user.name}! هل نفعل شيئًا ممتعًا اليوم؟", false))
            } else {
                messages.add(Message("مرحبًا، لم نتعرف على بعضنا بعد. ما اسمك؟", false))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Chat messages display
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true // Display new messages at the bottom
        ) {
            items(messages.reversed()) { message ->
                MessageBubble(message = message)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentMessage,
                onValueChange = { currentMessage = it },
                label = { Text("اكتب هنا...") },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (currentMessage.isNotBlank()) {
                        val userMessage = currentMessage
                        messages.add(Message(userMessage, true))
                        currentMessage = ""

                        coroutineScope.launch {
                            if (userName == null) {
                                val newUser = UserEntity(name = userMessage, favoriteAnime = null)
                                db.userDao().insertUser(newUser)
                                userName = userMessage
                                messages.add(Message("تشرفت بمعرفتك، $userMessage! ما هو الأنمي المفضل لديك؟", false))
                            } else if (favoriteAnime == null) {
                                val updatedUser = UserEntity(id = 0, name = userName!!, favoriteAnime = userMessage)
                                db.userDao().insertUser(updatedUser)
                                favoriteAnime = userMessage
                                messages.add(Message("رائع! ${userMessage} أنمي مفضل ممتاز. أنا هنا لمساعدتك في أي شيء!", false))
                            } else {
                                // Use LLMClient for response
                                val llmResponse = llmClient.generateResponse(userMessage, userName, favoriteAnime)
                                messages.add(Message(llmResponse, false))
                            }
                        }
                    }
                },
                modifier = Modifier.wrapContentWidth()
            ) {
                Text("إرسال")
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Text(
            text = message.text,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (message.isUser) MaterialTheme.colorScheme.primary else Color.Gray)
                .padding(8.dp),
            color = Color.White
        )
    }
}




