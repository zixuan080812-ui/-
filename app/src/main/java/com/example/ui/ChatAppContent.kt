package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.data.repository.AiPersona
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAppContent(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()
    val activePersonaId by viewModel.activePersonaId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()

    val user1Name by viewModel.user1Name.collectAsStateWithLifecycle()
    val user2Name by viewModel.user2Name.collectAsStateWithLifecycle()
    val user1Status by viewModel.user1Status.collectAsStateWithLifecycle()
    val user2Status by viewModel.user2Status.collectAsStateWithLifecycle()
    val currentUserLocal by viewModel.currentUserLocal.collectAsStateWithLifecycle()

    var showNameDialog by remember { mutableStateOf(false) }
    var showExplanationDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Automatically trigger initial AI greeting if the message log is initialized empty
    LaunchedEffect(activeMode, activePersonaId) {
        if (activeMode == "AI") {
            delay(150) // wait for database callback to fill flowing state
            if (activeMessages.isEmpty()) {
                val currentPersona = viewModel.personas.find { it.id == activePersonaId }
                if (currentPersona != null) {
                    viewModel.triggerInitialGreeting(currentPersona)
                }
            }
        }
    }

    val isDark = isSystemInDarkTheme()
    val backgroundBrush = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(GlassDarkBgStart, GlassDarkBgCenter, GlassDarkBgEnd)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(GlassLightBgStart, GlassLightBgCenter, GlassLightBgEnd)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Scaffold(
            modifier = modifier.testTag("app_scaffold"),
            topBar = {
                // Do not show the master top-bar in Face-To-Face mode to maximize screen space for rotated views
                if (activeMode != "FACE_TO_FACE") {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "聊天伴侣 (Chat Companion)",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), // Glassy translucent top bar
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        actions = {
                            IconButton(
                                onClick = { showExplanationDialog = true },
                                modifier = Modifier.testTag("action_info")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "说明",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { showNameDialog = true },
                                modifier = Modifier.testTag("action_edit_names")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "设置名字",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.clearChat(
                                        mode = activeMode,
                                        personaId = if (activeMode == "AI") activePersonaId else null
                                    )
                                },
                                modifier = Modifier.testTag("action_clear_chat")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "重置聊天",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (activeMode == "FACE_TO_FACE") PaddingValues(0.dp) else innerPadding)
        ) {
            // Header Toggle pill only shown in Classic and AI companion modes
            if (activeMode != "FACE_TO_FACE") {
                ModeSelectionTab(
                    activeMode = activeMode,
                    onModeSelected = { viewModel.setMode(it) }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (activeMode) {
                    "LOCAL" -> {
                        ClassicDuetChat(
                            messages = activeMessages,
                            user1Name = user1Name,
                            user2Name = user2Name,
                            user1Status = user1Status,
                            user2Status = user2Status,
                            currentUserLocal = currentUserLocal,
                            onToggleSender = { viewModel.toggleLocalSender() },
                            onSendMessage = { viewModel.sendLocalMessage(it) },
                            onDeleteMessage = { viewModel.deleteMessage(it) },
                            onUser1StatusChanged = { viewModel.setUser1Status(it) },
                            onUser2StatusChanged = { viewModel.setUser2Status(it) },
                            onInteract = { viewModel.markIncomingMessagesAsRead() }
                        )
                    }
                    "FACE_TO_FACE" -> {
                        FaceToFaceChat(
                            messages = activeMessages,
                            user1Name = user1Name,
                            user2Name = user2Name,
                            user1Status = user1Status,
                            user2Status = user2Status,
                            onSendMessage = { text, sender -> viewModel.sendFaceToFaceMessage(text, sender) },
                            onBackToMain = { viewModel.setMode("LOCAL") },
                            onClearChat = { viewModel.clearChat("FACE_TO_FACE") },
                            onUser1StatusChanged = { viewModel.setUser1Status(it) },
                            onUser2StatusChanged = { viewModel.setUser2Status(it) },
                            onPaneInteract = { viewModel.markMessagesAsReadForUser(it) }
                        )
                    }
                    "AI" -> {
                        AiCompanionChat(
                            messages = activeMessages,
                            personas = viewModel.personas,
                            activePersonaId = activePersonaId,
                            isGenerating = isGenerating,
                            onPersonaSelected = { viewModel.setPersonaId(it) },
                            onSendMessage = { viewModel.sendAiUserMessage(it) },
                            onDeleteMessage = { viewModel.deleteMessage(it) },
                            onInteract = { viewModel.markIncomingMessagesAsRead() }
                        )
                    }
                    "CLOUD" -> {
                        CloudSyncChat(
                            viewModel = viewModel,
                            messages = activeMessages,
                            cloudUserId = viewModel.cloudUserId.collectAsState().value,
                            cloudUserName = viewModel.cloudUserName.collectAsState().value,
                            cloudUserStatus = viewModel.cloudUserStatus.collectAsState().value,
                            activeRoomCode = viewModel.activeRoomCode.collectAsState().value,
                            cloudRoomMembers = viewModel.cloudRoomMembers.collectAsState().value,
                            onSendMessage = { viewModel.sendCloudMessage(it) },
                            onDeleteMessage = { viewModel.deleteMessage(it) }
                        )
                    }
                }
            }
        }
    }

    // Modal dialogs
    if (showNameDialog) {
        NameCustomizationDialog(
            initialUser1 = user1Name,
            initialUser2 = user2Name,
            onDismiss = { showNameDialog = false },
            onSave = { u1, u2 ->
                viewModel.setUserNames(u1, u2)
                showNameDialog = false
            }
        )
    }

    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            title = { Text("💡 关于聊天伴侣 (About)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "本应用特别为「聊天交流」精心设计，支持以下三种各具特色、充满交互温度的聊天模式：",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "1. 经典双人对聊 (Duet Chat)：单机传递模式。可以随时点击输入框旁边的头像，在两个用户身份（如 Alice 和 Bob）之间一键自由切换，适合两个人用一台设备记录或推演对话情景，支持离线存储。",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "2. 面对面双向对聊 (Split Screen)：黑科技分屏模式！手机屏幕一分为二，上方画面旋转180°朝向对面，下方正常朝向你。两位坐对面的好友可同时使用各自的输入框聊天，文字气泡智能重组，不用传递手机即可共创对话！",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "3. AI 拟人伴侣 (AI Companion)：如果没有玩伴在身旁，应用提供 5 位性格独特、头像活泼的高拟真 AI 角色（贴心男闺蜜、极客科技控、智慧哲人苏格拉底等），由 Gemini 3.5 智能模型驱动，随时倾听您的内心世界！",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showExplanationDialog = false }) {
                    Text("好，我知道了")
                }
            }
        )
    }
    }
}

@Composable
fun ModeSelectionTab(
    activeMode: String,
    onModeSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(width = 1.dp, color = Color(0x33FFFFFF), shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val modes = listOf(
                Triple("LOCAL", "双人对聊", Icons.Default.Person),
                Triple("FACE_TO_FACE", "面对面分屏", Icons.Default.Refresh),
                Triple("AI", "AI 拟人伴侣", Icons.Default.Face),
                Triple("CLOUD", "跨端联聊", Icons.Default.Share)
            )

            modes.forEach { (mode, name, icon) ->
                val isSelected = activeMode == mode
                val bgCo by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                )
                val contentCo by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgCo)
                        .clickable { onModeSelected(mode) }
                        .testTag("mode_tab_$mode"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = name,
                            tint = contentCo,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = name,
                            color = contentCo,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// MODE 1: CLASSIC DUET CHAT (LOCAL SENDER TOGGLING)
// ----------------------------------------------------
@Composable
fun ClassicDuetChat(
    messages: List<ChatMessage>,
    user1Name: String,
    user2Name: String,
    user1Status: String,
    user2Status: String,
    currentUserLocal: String,
    onToggleSender: () -> Unit,
    onSendMessage: (String) -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit,
    onUser1StatusChanged: (String) -> Unit,
    onUser2StatusChanged: (String) -> Unit,
    onInteract: () -> Unit
) {
    val listState = rememberLazyListState()
    var textInput by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Frosted Conversational Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.15f))
                .border(1.dp, Color(0x19FFFFFF), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "用户状态指示:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusSelectorRow(
                    userName = user1Name,
                    currentStatus = user1Status,
                    onStatusSelected = onUser1StatusChanged
                )
                StatusSelectorRow(
                    userName = user2Name,
                    currentStatus = user2Status,
                    onStatusSelected = onUser2StatusChanged
                )
            }
        }

        // Message list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                EmptyStateHelper(
                    title = "开启本地聊天",
                    subtitle = "两个人在同一个手机上聊天的乐园！\n下方可以随时点击身份头像切换写字的人。"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onInteract() },
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        // In Duet Chat, sender1 is on the right (user1), sender2 is on the left (user2)
                        val isSelf = msg.senderId == "user1"
                        MessageBubbleRow(
                            message = msg,
                            isSelf = isSelf,
                            onLongClick = { onDeleteMessage(msg) }
                        )
                    }
                }
            }
        }

        // Active Speaker Indicators + Input
        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color(0x2BFFFFFF),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                // Identity Switcher Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "发言身份切到:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                    ) {
                        val activeU1 = currentUserLocal == "user1"
                        val activeU2 = currentUserLocal == "user2"

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (activeU1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { if (!activeU1) onToggleSender() }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("select_user1"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👤 $user1Name",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeU1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (activeU2) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent)
                                        .clickable { if (!activeU2) onToggleSender() }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("select_user2"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👥 $user2Name",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeU2) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Core send bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textInput,
                        onValueChange = {
                            textInput = it
                            onInteract()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .border(width = 1.dp, color = Color(0x33FFFFFF), shape = RoundedCornerShape(24.dp))
                            .testTag("duet_input_field"),
                        placeholder = {
                            Text(
                                text = "用 ${if (currentUserLocal == "user1") user1Name else user2Name} 的身份发送...",
                                fontSize = 14.sp
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { showEmojiPicker = !showEmojiPicker },
                                modifier = Modifier.testTag("duet_emoji_toggle")
                            ) {
                                Text(text = if (showEmojiPicker) "⌨️" else "😀", fontSize = 18.sp)
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.isNotBlank()) {
                                onSendMessage(textInput)
                                textInput = ""
                                focusManager.clearFocus()
                            }
                        }),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onSendMessage(textInput)
                                textInput = ""
                                focusManager.clearFocus()
                            }
                        },
                        enabled = textInput.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (textInput.isNotBlank()) {
                                    if (currentUserLocal == "user1") MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .testTag("duet_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "发送",
                            tint = if (textInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (showEmojiPicker) {
                    Spacer(modifier = Modifier.height(8.dp))
                    EmojiPickerPanel(
                        onEmojiSelected = { emoji ->
                            textInput = textInput + emoji
                            onInteract()
                        }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// MODE 2: INTERACTIVE FACE-TO-FACE SPLIT CHAT
// ----------------------------------------------------
@Composable
fun FaceToFaceChat(
    messages: List<ChatMessage>,
    user1Name: String,
    user2Name: String,
    user1Status: String,
    user2Status: String,
    onSendMessage: (String, String) -> Unit,
    onBackToMain: () -> Unit,
    onClearChat: () -> Unit,
    onUser1StatusChanged: (String) -> Unit,
    onUser2StatusChanged: (String) -> Unit,
    onPaneInteract: (String) -> Unit
) {
    // We split screen into top half (rotated 180 degrees) and bottom half (0 degrees)
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ---- TOP PLAYER PANEL (Rotated 180 deg) ----
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .rotate(180f)
                    .background(JetBlack.copy(alpha = 0.05f))
            ) {
                FaceToFacePane(
                    myPaneId = "user1",
                    myName = user1Name,
                    opponentName = user2Name,
                    myStatus = user1Status,
                    opponentStatus = user2Status,
                    messages = messages,
                    onSend = { text -> onSendMessage(text, "user1") },
                    primaryColor = BubbleFaceToFaceUser1,
                    onStatusSelected = onUser1StatusChanged,
                    onInteract = { onPaneInteract("user1") }
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.dp
            )

            // ---- BOTTOM PLAYER PANEL (0 deg) ----
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(JetBlack.copy(alpha = 0.02f))
            ) {
                FaceToFacePane(
                    myPaneId = "user2",
                    myName = user2Name,
                    opponentName = user1Name,
                    myStatus = user2Status,
                    opponentStatus = user1Status,
                    messages = messages,
                    onSend = { text -> onSendMessage(text, "user2") },
                    primaryColor = BubbleFaceToFaceUser2,
                    onStatusSelected = onUser2StatusChanged,
                    onInteract = { onPaneInteract("user2") }
                )
            }
        }

        // Floating Control Overlay in middle intersection to adjust configuration
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .size(46.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBackToMain,
                    modifier = Modifier.testTag("f2f_exit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FaceToFacePane(
    myPaneId: String,
    myName: String,
    opponentName: String,
    myStatus: String,
    opponentStatus: String,
    messages: List<ChatMessage>,
    onSend: (String) -> Unit,
    primaryColor: Color,
    onStatusSelected: (String) -> Unit,
    onInteract: () -> Unit
) {
    val listState = rememberLazyListState()
    var text by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Advanced interactive status-enabled pane title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
                Text(
                    text = "$myName:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatusSelectorRow(
                    userName = "",
                    currentStatus = myStatus,
                    onStatusSelected = onStatusSelected
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$opponentName",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusBadge(status = opponentStatus)
            }
        }

        // Scrolling view
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "面对面双屏已激活！\n在下方打字，消息会在两端同时出现 😊",
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onInteract() },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isMyMessage = msg.senderId == myPaneId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateBubbleEntry(),
                            horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
                        ) {
                            Column(
                                horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
                            ) {
                                Text(
                                    text = if (isMyMessage) "我 ($myName)" else msg.senderName,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                                Card(
                                    shape = RoundedCornerShape(
                                        topStart = 10.dp,
                                        topEnd = 10.dp,
                                        bottomStart = if (isMyMessage) 10.dp else 2.dp,
                                        bottomEnd = if (isMyMessage) 2.dp else 10.dp
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMyMessage) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = msg.content,
                                            color = if (isMyMessage) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                        if (isMyMessage) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End,
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                if (msg.isRead) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Done,
                                                            contentDescription = "已读",
                                                            tint = Color(0xFF10B981),
                                                            modifier = Modifier.size(9.dp)
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Default.Done,
                                                            contentDescription = "已读",
                                                            tint = Color(0xFF10B981),
                                                            modifier = Modifier
                                                                .size(9.dp)
                                                                .offset(x = (-3).dp)
                                                        )
                                                    }
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "已送达",
                                                        tint = Color.White.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(9.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pane input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = {
                    text = it
                    onInteract()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 50.dp)
                    .testTag("f2f_input_$myPaneId"),
                placeholder = { Text("在此输入消息...", fontSize = 12.sp) },
                trailingIcon = {
                    IconButton(
                        onClick = { showEmojiPicker = !showEmojiPicker },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("f2f_emoji_toggle_$myPaneId")
                    ) {
                        Text(text = if (showEmojiPicker) "⌨️" else "😀", fontSize = 15.sp)
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                        focusManager.clearFocus()
                    }
                }),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                        focusManager.clearFocus()
                    }
                },
                enabled = text.isNotBlank(),
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (text.isNotBlank()) primaryColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .testTag("f2f_send_btn_$myPaneId")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "发送",
                    tint = if (text.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (showEmojiPicker) {
            Spacer(modifier = Modifier.height(4.dp))
            EmojiPickerPanel(
                height = 105.dp,
                onEmojiSelected = { emoji ->
                    text = text + emoji
                    onInteract()
                }
            )
        }
    }
}

// ----------------------------------------------------
// MODE 3: AI COMPANION CHAT
// ----------------------------------------------------
@Composable
fun AiCompanionChat(
    messages: List<ChatMessage>,
    personas: List<AiPersona>,
    activePersonaId: String,
    isGenerating: Boolean,
    onPersonaSelected: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit,
    onInteract: () -> Unit
) {
    val listState = rememberLazyListState()
    var textInput by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val currentPersona = personas.find { it.id == activePersonaId } ?: personas.first()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal Persona selector
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(personas) { persona ->
                val isSelected = persona.id == activePersonaId
                val borderCo = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

                Card(
                    modifier = Modifier
                        .width(135.dp)
                        .height(68.dp)
                        .clickable { onPersonaSelected(persona.id) }
                        .border(1.5.dp, borderCo, RoundedCornerShape(12.dp))
                        .testTag("persona_card_${persona.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = persona.avatarEmoji,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = persona.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = persona.title,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Divider(
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Messages list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onInteract() },
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isUser = msg.senderId == "user1"
                    MessageBubbleRow(
                        message = msg,
                        isSelf = isUser,
                        avatarOverride = if (!isUser) currentPersona.avatarEmoji else null,
                        borderColorOverride = if (!isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else null,
                        onLongClick = { onDeleteMessage(msg) }
                    )
                }

                if (isGenerating) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentPersona.avatarEmoji,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${currentPersona.name} 正在思考并输入中...",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Send chat field
        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color(0x2BFFFFFF),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textInput,
                        onValueChange = {
                            textInput = it
                            onInteract()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .border(width = 1.dp, color = Color(0x33FFFFFF), shape = RoundedCornerShape(24.dp))
                            .testTag("ai_input_field"),
                        placeholder = {
                            Text(
                                text = "给 ${currentPersona.name} 发送消息...",
                                fontSize = 14.sp
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { showEmojiPicker = !showEmojiPicker },
                                modifier = Modifier.testTag("ai_emoji_toggle"),
                                enabled = !isGenerating
                            ) {
                                Text(text = if (showEmojiPicker) "⌨️" else "😀", fontSize = 18.sp)
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.isNotBlank() && !isGenerating) {
                                onSendMessage(textInput)
                                textInput = ""
                                focusManager.clearFocus()
                            }
                        }),
                        maxLines = 4,
                        enabled = !isGenerating
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank() && !isGenerating) {
                                onSendMessage(textInput)
                                textInput = ""
                                focusManager.clearFocus()
                            }
                        },
                        enabled = textInput.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (textInput.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("ai_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "发送",
                            tint = if (textInput.isNotBlank() && !isGenerating) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (showEmojiPicker) {
                    Spacer(modifier = Modifier.height(8.dp))
                    EmojiPickerPanel(
                        onEmojiSelected = { emoji ->
                            textInput = textInput + emoji
                            onInteract()
                        }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// UI HELPERS & COMPONENTS
// ----------------------------------------------------

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleRow(
    message: ChatMessage,
    isSelf: Boolean,
    avatarOverride: String? = null,
    borderColorOverride: Color? = null,
    onLongClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { sdf.format(Date(message.timestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateBubbleEntry()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
    ) {
        if (!isSelf) {
            // Left Avatar
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarOverride ?: "👤",
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Sender name tag
            Text(
                text = message.senderName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )

            val isDarkTheme = isSystemInDarkTheme()
            val bubbleBg = remember(isSelf, isDarkTheme) {
                if (isSelf) {
                    if (isDarkTheme) BubbleOutgoingDark else BubbleOutgoingLight
                } else {
                    if (isDarkTheme) BubbleIncomingDark else BubbleIncomingLight
                }
            }
            val bubbleTextColor = remember(isSelf, isDarkTheme) {
                if (isSelf) {
                    Color.White
                } else {
                    if (isDarkTheme) FrostedLightText else FrostedDarkText
                }
            }
            val bubbleTimeColor = remember(bubbleTextColor) {
                bubbleTextColor.copy(alpha = 0.6f)
            }
            val glassBorderColor = remember(isDarkTheme) {
                if (isDarkTheme) Color(0x24FFFFFF) else Color(0x40FFFFFF)
            }

            // Bubble body
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isSelf) 16.dp else 4.dp,
                    bottomEnd = if (isSelf) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = bubbleBg
                ),
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .border(
                        width = 1.dp,
                        color = borderColorOverride ?: glassBorderColor,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isSelf) 16.dp else 4.dp,
                            bottomEnd = if (isSelf) 4.dp else 16.dp
                        )
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                // Long press deletion supports custom messages list
                Box(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {},
                            onLongClick = onLongClick
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = message.content,
                            color = bubbleTextColor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = formattedTime,
                                color = bubbleTimeColor,
                                fontSize = 9.sp,
                                textAlign = TextAlign.End
                            )
                            if (isSelf) {
                                Spacer(modifier = Modifier.width(4.dp))
                                if (message.isRead) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = "已读",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = "已读",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier
                                                .size(10.dp)
                                                .offset(x = (-4).dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已送达",
                                        tint = bubbleTimeColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isSelf) {
            Spacer(modifier = Modifier.width(8.dp))
            // Right Avatar
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🧑‍⚕️",
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun EmptyStateHelper(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier.size(100.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "无消息",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameCustomizationDialog(
    initialUser1: String,
    initialUser2: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var u1 by remember { mutableStateOf(initialUser1) }
    var u2 by remember { mutableStateOf(initialUser2) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "✍️ 自定义对聊名称",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = u1,
                    onValueChange = { u1 = it },
                    label = { Text("玩家 A 名称 (User A)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_user1_name"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = u2,
                    onValueChange = { u2 = it },
                    label = { Text("玩家 B 名称 (User B)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_user2_name"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(u1, u2) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("save_names_button")
            ) {
                Text("保存设置", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_names_button")
            ) {
                Text("取消")
            }
        }
    )
}

// ----------------------------------------------------
// UTILS & STATUS PLUGINS
// ----------------------------------------------------
@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        "ONLINE" -> "在线" to Color(0xFF10B981) // Vibrant Emerald Green
        "AWAY" -> "离开" to Color(0xFFF59E0B)   // Warm Amber
        "BUSY" -> "忙碌" to Color(0xFFEF4444)   // Crimson
        else -> "在线" to Color(0xFF10B981)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun StatusSelectorRow(
    userName: String,
    currentStatus: String,
    onStatusSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (userName.isNotEmpty()) {
                Text(
                    text = userName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            StatusBadge(status = currentStatus)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "选择状态",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            listOf("ONLINE", "AWAY", "BUSY").forEach { choice ->
                val label = when (choice) {
                    "ONLINE" -> "🟢 在线 (Online)"
                    "AWAY" -> "🟡 离开 (Away)"
                    "BUSY" -> "🔴 忙碌 (Busy)"
                    else -> ""
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (currentStatus == choice) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onStatusSelected(choice)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ----------------------------------------------------
// MODE 4: CLOUD SYNC CHAT COMPOSABLE
// ----------------------------------------------------
@Composable
fun CloudSyncChat(
    viewModel: ChatViewModel,
    messages: List<ChatMessage>,
    cloudUserId: String,
    cloudUserName: String,
    cloudUserStatus: String,
    activeRoomCode: String,
    cloudRoomMembers: Map<String, com.example.data.api.MemberStatusDto>,
    onSendMessage: (String) -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit
) {
    val listState = rememberLazyListState()
    var textInput by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // For joining rooms
    var roomCodeInput by remember { mutableStateOf("") }
    var nickInput by remember { mutableStateOf(cloudUserName) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (activeRoomCode.isEmpty()) {
        // --- LOBBY SCREEN (Not in room) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp)
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Cloud Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "跨设备多端联聊 (Cloud Chat)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "本模式使用公网 REST 后端自动同步，输入相同房号即可在多个不同手机/平板上实现高保真互连畅聊！",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = Color(0x19FFFFFF))

                    // Set nickname first
                    OutlinedTextField(
                        value = nickInput,
                        onValueChange = {
                            nickInput = it
                            viewModel.setCloudUserName(it)
                        },
                        label = { Text("给自己起一个昵称 (Nickname)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Choose status in lobby
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "选择您当前的状态:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatusSelectorRow(
                            userName = "",
                            currentStatus = cloudUserStatus,
                            onStatusSelected = { viewModel.setCloudUserStatus(it) }
                        )
                    }

                    HorizontalDivider(color = Color(0x19FFFFFF))

                    // Buttons to create or join
                    Button(
                        onClick = {
                            viewModel.setCloudUserName(nickInput)
                            viewModel.createRoom()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("🆕 创建新聊天室 (Create Room)", color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { if (it.length <= 6) roomCodeInput = it },
                            placeholder = { Text("输入6位房号") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (roomCodeInput.length == 6) {
                                    viewModel.setCloudUserName(nickInput)
                                    viewModel.joinRoom(roomCodeInput)
                                }
                            },
                            enabled = roomCodeInput.length == 6,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("加入 Room", color = Color.White)
                        }
                    }
                }
            }
        }
    } else {
        // --- CHAT ROOM PANEL (Inside active synced room) ---
        Column(modifier = Modifier.fillMaxSize()) {
            // Synced Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Active room",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "联聊中・房号: ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = activeRoomCode,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // My status quick modifier right in the chat topbar
                            StatusSelectorRow(
                                userName = "我的状态",
                                currentStatus = cloudUserStatus,
                                onStatusSelected = { viewModel.setCloudUserStatus(it) }
                            )

                            // Exit room button
                            IconButton(
                                onClick = { viewModel.leaveRoom() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "离开房间",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Online Members List with green/orange status dots
                    val activeOtherMembers = cloudRoomMembers.values.filter { it.userId != cloudUserId }
                    if (activeOtherMembers.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "在线群员: ",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(activeOtherMembers.toList()) { member ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x0CFFFFFF))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        val dotColor = when (member.status) {
                                            "ONLINE" -> Color(0xFF10B981)
                                            "AWAY" -> Color(0xFFF59E0B)
                                            "BUSY" -> Color(0xFFEF4444)
                                            else -> Color.Gray
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = member.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "⏳ 房间只有你一人，将房号分享给其它设备加入聊天吧！",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Message Display Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    EmptyStateHelper(
                        title = "无同步消息",
                        subtitle = "输入内容发送第一条同步消息！支持多端高保真极速互连。"
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isSelf = msg.senderId == cloudUserId
                            MessageBubbleRowWithReadCheck(
                                message = msg,
                                isSelf = isSelf,
                                onLongClick = { onDeleteMessage(msg) }
                            )
                        }
                    }
                }
            }

            // Bottom Sending Bar
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0x2BFFFFFF),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .border(width = 1.dp, color = Color(0x33FFFFFF), shape = RoundedCornerShape(24.dp))
                                .testTag("cloud_input_field"),
                            placeholder = {
                                Text(
                                    text = "说些什么（支持不同设备实时同步）...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showEmojiPicker = !showEmojiPicker },
                                    modifier = Modifier.testTag("cloud_emoji_toggle")
                                ) {
                                    Text(text = if (showEmojiPicker) "⌨️" else "😀", fontSize = 18.sp)
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (textInput.isNotBlank()) {
                                    onSendMessage(textInput)
                                    textInput = ""
                                    focusManager.clearFocus()
                                }
                            }),
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    onSendMessage(textInput)
                                    textInput = ""
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = textInput.isNotBlank(),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (textInput.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .testTag("cloud_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "发送",
                                tint = if (textInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (showEmojiPicker) {
                        Spacer(modifier = Modifier.height(8.dp))
                        EmojiPickerPanel(
                            onEmojiSelected = { emoji ->
                                textInput = textInput + emoji
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleRowWithReadCheck(
    message: ChatMessage,
    isSelf: Boolean,
    onLongClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { formatter.format(Date(message.timestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateBubbleEntry(),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isSelf) {
            // Friend Avatar
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.senderName.take(1).uppercase(Locale.getDefault()),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Column(
            horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
        ) {
            // Sender name tag
            Text(
                text = message.senderName,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )

            val isDarkTheme = isSystemInDarkTheme()
            val bubbleBg = remember(isSelf, isDarkTheme) {
                if (isSelf) {
                    if (isDarkTheme) BubbleOutgoingDark else BubbleOutgoingLight
                } else {
                    if (isDarkTheme) BubbleIncomingDark else BubbleIncomingLight
                }
            }
            val bubbleTextColor = remember(isSelf, isDarkTheme) {
                if (isSelf) {
                    Color.White
                } else {
                    if (isDarkTheme) FrostedLightText else FrostedDarkText
                }
            }
            val bubbleTimeColor = remember(bubbleTextColor) {
                bubbleTextColor.copy(alpha = 0.6f)
            }
            val glassBorderColor = remember(isDarkTheme) {
                if (isDarkTheme) Color(0x24FFFFFF) else Color(0x40FFFFFF)
            }

            // Bubble body
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isSelf) 16.dp else 4.dp,
                    bottomEnd = if (isSelf) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = bubbleBg
                ),
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .combinedClickable(
                        onLongClick = onLongClick,
                        onClick = {}
                    )
                    .border(
                        width = 1.dp,
                        color = glassBorderColor,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isSelf) 16.dp else 4.dp,
                            bottomEnd = if (isSelf) 4.dp else 16.dp
                        )
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = message.content,
                            color = bubbleTextColor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = formattedTime,
                                color = bubbleTimeColor,
                                fontSize = 9.sp,
                                textAlign = TextAlign.End
                            )
                            if (isSelf) {
                                Spacer(modifier = Modifier.width(4.dp))
                                if (message.isRead) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = "已读",
                                            tint = Color(0xFF10B981), // Green double check signifier
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = "已读",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(10.dp).offset(x = (-4).dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已送达",
                                        tint = bubbleTimeColor, // Gray single check
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isSelf) {
            // Self Avatar
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.senderName.take(1).uppercase(Locale.getDefault()),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun EmojiPickerPanel(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp
) {
    val emojis = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
        "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
        "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
        "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
        "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬",
        "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗",
        "🤔", "🤭", "🤫", "🤥", "😶", "😐", "😑", "😬", "🙄", "😯",
        "😴", "🤤", "😪", "😵", "🤐", "🥴", "🤢", "🤮", "🤧", "😷",
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💖",
        "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "🤙", "🖐️", "✋",
        "🤝", "👏", "🙌", "👐", "🤲", "🙏", "✍️", "💅", "🤳", "💪"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0x19FFFFFF))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "常用表情 (Emojis)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${emojis.size} 个可用",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            HorizontalDivider(color = Color(0x11FFFFFF), modifier = Modifier.padding(bottom = 8.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 38.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(emojis) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable { onEmojiSelected(emoji) }
                            .testTag("emoji_item_$emoji"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.animateBubbleEntry(): Modifier {
    val animScale = remember { Animatable(0.85f) }
    val animAlpha = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        launch {
            animScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            animAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220)
            )
        }
        launch {
            animOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    return this.graphicsLayer {
        scaleX = animScale.value
        scaleY = animScale.value
        alpha = animAlpha.value
        translationY = animOffsetY.value * density
    }
}
