package com.meteory.optimizer.screens.assistant

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meteory.optimizer.ui.components.GlowButton
import com.meteory.optimizer.ui.theme.*
import com.meteory.optimizer.viewmodels.AssistantMessage
import com.meteory.optimizer.viewmodels.AssistantViewModel

@Composable
fun AssistantScreen(vm: AssistantViewModel = hiltViewModel()) {
    val state = vm.state.collectAsState().value

    if (!state.isValidated) {
        KeyActivationScreen(
            keyInput       = state.keyInput,
            error          = state.validationError,
            onKeyChange    = vm::updateKeyInput,
            onValidate     = vm::validateKey
        )
    } else {
        ChatScreen(
            messages       = state.messages,
            inputText      = state.inputText,
            isProcessing   = state.isProcessing,
            deviceModel    = state.deviceModel,
            currentProfile = state.currentProfile,
            onInputChange  = vm::updateInput,
            onSend         = vm::sendMessage
        )
    }
}

@Composable
private fun KeyActivationScreen(
    keyInput: String,
    error: String,
    onKeyChange: (String) -> Unit,
    onValidate: () -> Unit
) {
    var showKey by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Neutral950, Primary900, Neutral950))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(Icons.Default.SmartToy, null, tint = Primary400, modifier = Modifier.size(56.dp))
            Text(
                "Meteory Asistente FPS",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                "Introduce tu clave de activación exclusiva para acceder al asistente de optimización y rendimiento.",
                style = MaterialTheme.typography.bodyMedium,
                color = Neutral400
            )

            OutlinedTextField(
                value          = keyInput,
                onValueChange  = onKeyChange,
                label          = { Text("Clave de Activación") },
                placeholder    = { Text("AQ.Ab8...", color = Neutral500) },
                visualTransformation = if (showKey) androidx.compose.ui.text.input.VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon   = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null, tint = Neutral400
                        )
                    }
                },
                isError        = error.isNotBlank(),
                supportingText = { if (error.isNotBlank()) Text(error, color = Error400) },
                modifier       = Modifier.fillMaxWidth(),
                shape          = RoundedCornerShape(14.dp),
                colors         = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor    = Primary400,
                    unfocusedBorderColor  = Neutral700,
                    focusedLabelColor     = Primary400,
                    cursorColor           = Primary400
                )
            )

            GlowButton(
                text     = "Activar Asistente",
                onClick  = onValidate,
                enabled  = keyInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                color    = Primary400
            )

            Text(
                "Solo responde y gestiona optimización y rendimiento.",
                style = MaterialTheme.typography.bodySmall,
                color = Neutral500
            )
        }
    }
}

@Composable
private fun ChatScreen(
    messages: List<AssistantMessage>,
    inputText: String,
    isProcessing: Boolean,
    deviceModel: String,
    currentProfile: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Neutral950)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.SmartToy, null, tint = Primary400, modifier = Modifier.size(24.dp))
                Column {
                    Text("Meteory Asistente FPS", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(deviceModel, color = Neutral400, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.weight(1f))
                if (currentProfile.isNotBlank()) {
                    Badge(containerColor = Accent500.copy(0.2f)) {
                        Text(currentProfile, color = Accent500, fontSize = 10.sp)
                    }
                }
            }
        }

        // Quick prompts
        val quickPrompts = listOf("Optimizar FPS", "Liberar RAM", "Mi Xiaomi Redmi Note", "Samsung Galaxy S23", "Estado Shizuku")
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickPrompts.forEach { prompt ->
                            SuggestionChip(
                                onClick = { onInputChange(prompt); onSend() },
                                label   = { Text(prompt, fontSize = 12.sp) },
                                colors  = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Primary400.copy(0.1f),
                                    labelColor     = Primary400
                                ),
                                border  = BorderStroke(1.dp, Primary400.copy(0.3f))
                            )
                        }
                    }
                }
            }
            items(messages) { msg ->
                ChatBubble(msg)
            }
            if (isProcessing) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(3) { i ->
                            TypingDot(delay = i * 150)
                        }
                    }
                }
            }
        }

        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value          = inputText,
                onValueChange  = onInputChange,
                placeholder    = { Text("Pregunta sobre rendimiento...", color = Neutral500, fontSize = 14.sp) },
                modifier       = Modifier.weight(1f),
                shape          = RoundedCornerShape(24.dp),
                maxLines       = 3,
                colors         = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor    = Primary400,
                    unfocusedBorderColor  = Neutral700,
                    cursorColor           = Primary400
                )
            )
            FloatingActionButton(
                onClick           = { if (inputText.isNotBlank()) onSend() },
                containerColor    = if (inputText.isNotBlank()) Primary400 else Neutral700,
                contentColor      = Neutral950,
                modifier          = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: AssistantMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!msg.isUser) {
            Icon(
                Icons.Default.SmartToy, null,
                tint     = Primary400,
                modifier = Modifier.size(22.dp).padding(top = 4.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart  = if (msg.isUser) 16.dp else 4.dp,
                        topEnd    = 16.dp,
                        bottomEnd = if (msg.isUser) 4.dp else 16.dp,
                        bottomStart = 16.dp
                    )
                )
                .background(if (msg.isUser) Primary400.copy(0.9f) else SurfaceElevated)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text  = msg.content,
                color = if (msg.isUser) Neutral950 else Neutral100,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TypingDot(delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue   = 0.3f,
        targetValue    = 1f,
        animationSpec  = infiniteRepeatable(
            animation  = androidx.compose.animation.core.tween(600, delayMillis = delay),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Primary400.copy(alpha = alpha))
    )
}
