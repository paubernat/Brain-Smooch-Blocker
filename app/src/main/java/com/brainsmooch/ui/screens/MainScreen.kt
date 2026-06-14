package com.brainsmooch.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.brainsmooch.R
import com.brainsmooch.data.AppInfo
import com.brainsmooch.domain.Sfx
import com.brainsmooch.ui.theme.OrnateCardShape
import com.brainsmooch.viewmodel.BlockTab
import com.brainsmooch.viewmodel.UiState
import java.util.Locale

private const val UNLIMITED_THRESHOLD_MS = 31536000000L

@Composable
private fun animatedGradient(): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")

    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave1"
    )

    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave2"
    )

    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave3"
    )

    val wave4 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave4"
    )

    // Colors - more black weight, subtle warm tones
    val voidBlack = Color(0xFF020406)
    val deepBlack = Color(0xFF040810)
    val deepOcean = Color(0xFF061525)
    val electricBlue = Color(0xFF0A2848)
    val bluePurple = Color(0xFF151838)
    val royalPurple = Color(0xFF1E1445)
    val subtleMagenta = Color(0xFF3A1A28)

    // Color mixing - smoother transitions
    val c1 = lerp(voidBlack, deepBlack, wave1 * 0.8f)
    val c2 = lerp(deepBlack, deepOcean, wave2 * 0.9f)
    val c3 = lerp(deepOcean, electricBlue, wave3)
    val c4 = lerp(electricBlue, bluePurple, wave4 * 0.85f)
    val c5 = lerp(bluePurple, royalPurple, wave1 * 0.9f)
    val c6 = lerp(royalPurple, subtleMagenta, wave2 * 0.85f)

    // Movement
    val startX = -100f + 600f * wave2
    val startY = -300f + 500f * wave3
    val endX = 500f + 1000f * wave1
    val endY = 1800f + 600f * wave4

    return Brush.linearGradient(
        colorStops = arrayOf(
            0f to c1,
            (0.12f + wave3 * 0.08f) to c2,
            (0.3f + wave1 * 0.1f) to c3,
            (0.5f + wave4 * 0.08f) to c4,
            (0.68f - wave2 * 0.08f) to c5,
            (0.85f + wave3 * 0.05f) to c6,
            1f to c6
        ),
        start = Offset(startX, startY),
        end = Offset(endX, endY)
    )
}

@Composable
fun MainScreen(
    uiState: UiState,
    onTabChange: (BlockTab) -> Unit,
    onDomainInputChange: (String) -> Unit,
    onAddDomain: () -> Unit,
    onAddDomainToActiveBlock: () -> Unit,
    onRemoveDomain: (String) -> Unit,
    onAppSearchChange: (String) -> Unit,
    onToggleApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onDaysChange: (Int) -> Unit,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onBlockPasswordChange: (String) -> Unit,
    onBlockPasswordConfirmChange: (String) -> Unit,
    onStartBlock: () -> Unit,
    onStartBlockAfterSmooch: () -> Unit,
    onConfirmStep1: () -> Unit,
    onCancelConfirmation: () -> Unit,
    onUnlimitedModeChange: (Boolean) -> Unit,
    onHardcoreModeChange: (Boolean) -> Unit,
    onEnableAdmin: () -> Unit,
    onEnableAccessibility: () -> Unit,
    onEnableUsageStats: () -> Unit,
    onReleaseBlock: () -> Unit,
    onPanicPasswordSubmit: (String, (Boolean) -> Unit) -> Unit,
    onPanicDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSmoochOverlay by remember { mutableStateOf(false) }
    var showHeartExplosion by remember { mutableStateOf(false) }
    var waitingForBlock by remember { mutableStateOf(false) }

    // Clear waiting state once block is active
    LaunchedEffect(uiState.blockState.isActive) {
        if (uiState.blockState.isActive) {
            waitingForBlock = false
        }
    }

    if (uiState.showPanicDialog) {
        ReleaseDialog(
            hasPassword = uiState.blockState.hasPanicPassword,
            onSubmit = onPanicPasswordSubmit,
            onDismiss = onPanicDismiss
        )
    }

    if (uiState.confirmationStep == 1) {
        ConfirmDialog1(
            onConfirm = onConfirmStep1,
            onDismiss = onCancelConfirmation
        )
    }

    if (uiState.confirmationStep == 2) {
        ConfirmDialog2(
            isUnlimited = uiState.unlimitedMode,
            onConfirm = {
                onCancelConfirmation()
                showSmoochOverlay = true
            },
            onDismiss = onCancelConfirmation
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(animatedGradient())
    ) {
        // Hide content during heart explosion and while waiting for block to activate
        if (!showHeartExplosion && !waitingForBlock) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlowingTitle()

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.hardcoreMode || !uiState.isFullyProtected) {
                    ProtectionStatus(
                        isAdminActive = uiState.isAdminActive,
                        isAccessibilityEnabled = uiState.isAccessibilityEnabled,
                        onEnableAdmin = onEnableAdmin,
                        onEnableAccessibility = onEnableAccessibility
                    )
                }

                if (uiState.blockState.isActive) {
                    ActiveBlockScreen(
                        remainingMillis = uiState.remainingMillis,
                        blockedDomains = uiState.blockState.blockedDomains,
                        blockedApps = uiState.blockState.blockedApps,
                        installedApps = uiState.installedApps,
                        domainInput = uiState.domainInput,
                        hasPassword = uiState.blockState.hasPanicPassword,
                        isUnlimited = uiState.remainingMillis > UNLIMITED_THRESHOLD_MS,
                        onDomainInputChange = onDomainInputChange,
                        onAddDomain = onAddDomainToActiveBlock,
                        onReleaseBlock = onReleaseBlock
                    )
                } else {
                    SetupScreen(
                        currentTab = uiState.currentTab,
                        onTabChange = onTabChange,
                        domainInput = uiState.domainInput,
                        domains = uiState.domains,
                        apps = uiState.apps,
                        installedApps = uiState.filteredApps,
                        appSearchQuery = uiState.appSearchQuery,
                        onAppSearchChange = onAppSearchChange,
                        onToggleApp = onToggleApp,
                        onRemoveApp = onRemoveApp,
                        days = uiState.days,
                        hours = uiState.hours,
                        minutes = uiState.minutes,
                        unlimitedMode = uiState.unlimitedMode,
                        blockPassword = uiState.blockPassword,
                        blockPasswordConfirm = uiState.blockPasswordConfirm,
                        passwordsMatch = uiState.passwordsMatch,
                        requiresPassword = uiState.requiresPassword,
                        hasUsageStatsPermission = uiState.hasUsageStatsPermission,
                        canStart = uiState.hasContent && uiState.hasDuration && uiState.passwordsMatch &&
                            (!uiState.requiresPassword || uiState.blockPassword.isNotBlank()),
                        onDomainInputChange = onDomainInputChange,
                        onAddDomain = onAddDomain,
                        onRemoveDomain = onRemoveDomain,
                        onDaysChange = onDaysChange,
                        onHoursChange = onHoursChange,
                        onMinutesChange = onMinutesChange,
                        onUnlimitedModeChange = onUnlimitedModeChange,
                        onBlockPasswordChange = onBlockPasswordChange,
                        onBlockPasswordConfirmChange = onBlockPasswordConfirmChange,
                        onEnableUsageStats = onEnableUsageStats,
                        onStartBlock = {
                            if (uiState.needsConfirmation) {
                                onStartBlock()
                            } else {
                                showSmoochOverlay = true
                            }
                        },
                        hardcoreMode = uiState.hardcoreMode,
                        onHardcoreModeChange = onHardcoreModeChange
                    )
                }
            }
        }

        if (showSmoochOverlay && !showHeartExplosion) {
            val context = LocalContext.current
            SmoochOverlay(
                onSmooch = {
                    Sfx.smooch(context)
                    showHeartExplosion = true
                },
                onDismiss = { showSmoochOverlay = false }
            )
        }

        if (showHeartExplosion) {
            HeartExplosion(
                onFinished = {
                    showHeartExplosion = false
                    showSmoochOverlay = false
                    waitingForBlock = true
                    onStartBlockAfterSmooch()
                }
            )
        }
    }
}

@Composable
private fun GlowingTitle() {
    val glow = MaterialTheme.colorScheme.primary
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(
                Brush.radialGradient(
                    listOf(glow.copy(alpha = 0.18f), Color.Transparent)
                )
            )
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ReleaseDialog(
    hasPassword: Boolean,
    onSubmit: (String, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.release_title)) },
        text = {
            Column {
                if (hasPassword) {
                    Text(stringResource(R.string.release_prompt))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = false },
                        isError = error,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                    if (error) {
                        Text(stringResource(R.string.release_wrong_password), color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Text(stringResource(R.string.release_no_password))
                }
            }
        },
        confirmButton = {
            if (hasPassword) {
                TextButton(onClick = {
                    onSubmit(password) { ok -> error = !ok }
                    password = ""
                }) { Text(stringResource(R.string.release_submit)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.release_cancel)) }
        }
    )
}

@Composable
private fun ConfirmDialog1(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_title_1)) },
        text = { Text(stringResource(R.string.confirm_message_1)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.confirm_yes)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.confirm_no)) } }
    )
}

@Composable
private fun ConfirmDialog2(isUnlimited: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var countdown by remember { mutableIntStateOf(5) }
    val canConfirm = countdown == 0

    LaunchedEffect(Unit) {
        while (countdown > 0) { delay(1000L); countdown-- }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_title_2), color = MaterialTheme.colorScheme.error) },
        text = {
            Text(stringResource(if (isUnlimited) R.string.confirm_message_2_unlimited else R.string.confirm_message_2_password))
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = canConfirm) {
                Text(if (canConfirm) stringResource(R.string.confirm_final) else stringResource(R.string.confirm_wait, countdown))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.confirm_no)) } }
    )
}

@Composable
private fun SmoochOverlay(onSmooch: () -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(enabled = false) {}
            ) {
                Text(
                    text = stringResource(R.string.smooch_question),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                SmoochBrainButton(onClick = onSmooch)
            }
        }
    }
}

@Composable
private fun SmoochBrainButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(140.dp).clickable(onClick = onClick)
    ) {
        Text(text = "🧠", fontSize = 80.sp)
    }
}

private data class NeonSparkle(val t: Float, val offset: Float, val size: Float, val color: Color)

@Composable
private fun HeartExplosion(onFinished: () -> Unit) {
    val configuration = LocalConfiguration.current
    val maxDimension = maxOf(configuration.screenWidthDp, configuration.screenHeightDp) * 2.5f

    val neonPink = Color(0xFFFF10F0)
    val neonGlow = Color(0xFFFF69B4)
    val sparkleColors = listOf(Color.White, Color(0xFFFFB6C1), Color(0xFFFF69B4), neonPink)

    val sparkles = remember {
        List(40) {
            NeonSparkle(it / 40f, (-5..5).random().toFloat(), 2f + Math.random().toFloat() * 3f, sparkleColors.random())
        }
    }

    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(1f) }
    val glowPulse = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { glowPulse.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
            scale.animateTo(maxDimension / 80f, tween(900, easing = FastOutSlowInEasing))
        }
        alpha.animateTo(0f, tween(250))
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier.size(80.dp).graphicsLayer {
                scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value
            }
        ) {
            val w = size.width; val h = size.height
            val strokeWidth = 8.dp.toPx()

            val heartPath = Path().apply {
                moveTo(w / 2, h * 0.22f)
                cubicTo(w * 0.12f, h * -0.12f, -w * 0.12f, h * 0.48f, w / 2, h * 0.88f)
                moveTo(w / 2, h * 0.22f)
                cubicTo(w * 0.88f, h * -0.12f, w * 1.12f, h * 0.48f, w / 2, h * 0.88f)
            }

            drawPath(heartPath, neonGlow.copy(alpha = 0.4f * glowPulse.value), style = Stroke(width = strokeWidth * 3))
            drawPath(heartPath, neonPink.copy(alpha = 0.6f), style = Stroke(width = strokeWidth * 1.5f))
            drawPath(heartPath, neonPink, style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            drawPath(heartPath, Color.White.copy(alpha = 0.7f), style = Stroke(width = strokeWidth * 0.3f))

            sparkles.forEach { sparkle ->
                val t = sparkle.t
                val point = when {
                    t < 0.5f -> {
                        val tt = t * 2
                        Offset(
                            w / 2 + (w * 0.12f - w / 2) * 3 * (1 - tt) * (1 - tt) * tt + (-w * 0.12f - w / 2) * 3 * (1 - tt) * tt * tt + sparkle.offset,
                            h * 0.22f + (h * -0.12f - h * 0.22f) * 3 * (1 - tt) * (1 - tt) * tt + (h * 0.48f - h * 0.22f) * 3 * (1 - tt) * tt * tt + (h * 0.88f - h * 0.22f) * tt * tt * tt + sparkle.offset
                        )
                    }
                    else -> {
                        val tt = (t - 0.5f) * 2
                        Offset(
                            w / 2 + (w * 0.88f - w / 2) * 3 * (1 - tt) * (1 - tt) * tt + (w * 1.12f - w / 2) * 3 * (1 - tt) * tt * tt + sparkle.offset,
                            h * 0.22f + (h * -0.12f - h * 0.22f) * 3 * (1 - tt) * (1 - tt) * tt + (h * 0.48f - h * 0.22f) * 3 * (1 - tt) * tt * tt + (h * 0.88f - h * 0.22f) * tt * tt * tt + sparkle.offset
                        )
                    }
                }
                val shimmer = (glowPulse.value + sparkle.t) % 1f
                val sparkleAlpha = if (shimmer > 0.7f) (shimmer - 0.7f) / 0.3f else 0f
                drawCircle(sparkle.color.copy(alpha = sparkleAlpha * 0.9f), sparkle.size.dp.toPx(), point)
            }
        }
    }
}

@Composable
private fun ProtectionStatus(
    isAdminActive: Boolean,
    isAccessibilityEnabled: Boolean,
    onEnableAdmin: () -> Unit,
    onEnableAccessibility: () -> Unit
) {
    if (isAdminActive && isAccessibilityEnabled) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.protection_active), modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(16.dp))
        return
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(stringResource(R.string.protection_setup_prompt), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            if (!isAdminActive) {
                Button(onClick = onEnableAdmin, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.enable_admin)) }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (!isAccessibilityEnabled) {
                Button(onClick = onEnableAccessibility, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.enable_accessibility)) }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DomainInput(value: String, label: String, onValueChange: (String) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun submit() { onAdd(); focusManager.clearFocus(); keyboard?.hide() }

    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) }, placeholder = { Text(stringResource(R.string.domain_placeholder)) },
        trailingIcon = { IconButton(onClick = ::submit) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add)) } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        singleLine = true, modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharmChips(domains: List<String>, onRemove: ((String) -> Unit)? = null) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        domains.forEach { domain ->
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(domain, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (onRemove != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp).clickable { onRemove(domain) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppChips(apps: List<String>, installedApps: List<AppInfo>, onRemove: ((String) -> Unit)? = null) {
    val appLabels = installedApps.associateBy { it.packageName }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        apps.forEach { pkg ->
            val label = appLabels[pkg]?.label ?: pkg.substringAfterLast('.')
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text("📱", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (onRemove != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp).clickable { onRemove(pkg) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveBlockScreen(
    remainingMillis: Long, blockedDomains: List<String>, blockedApps: List<String>, installedApps: List<AppInfo>,
    domainInput: String, hasPassword: Boolean, isUnlimited: Boolean,
    onDomainInputChange: (String) -> Unit, onAddDomain: () -> Unit, onReleaseBlock: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.block_active), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))

        Card(shape = OrnateCardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary), modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 16.dp)) {
                if (isUnlimited) {
                    Text(stringResource(R.string.unlimited_display), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                } else {
                    Text(formatRemaining(remainingMillis), style = MaterialTheme.typography.displayMedium.copy(fontSize = 52.sp), color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.remaining), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        if (hasPassword) {
            OutlinedButton(onClick = onReleaseBlock, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.release_button)) }
            Spacer(modifier = Modifier.height(16.dp))
        }

        DomainInput(value = domainInput, label = stringResource(R.string.domain_label_active), onValueChange = onDomainInputChange, onAdd = onAddDomain)
        Spacer(modifier = Modifier.height(24.dp))

        if (blockedDomains.isNotEmpty()) {
            Text(stringResource(R.string.blocked_domains), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.height(12.dp))
            CharmChips(domains = blockedDomains)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (blockedApps.isNotEmpty()) {
            Text(stringResource(R.string.blocked_apps), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.height(12.dp))
            AppChips(apps = blockedApps, installedApps = installedApps)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun formatRemaining(remainingMillis: Long): String {
    val totalSeconds = remainingMillis / 1000
    val days = totalSeconds / 86_400
    val hours = (totalSeconds % 86_400) / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (days > 0) String.format(Locale.US, "%dd %02d:%02d:%02d", days, hours, minutes, seconds)
    else String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

@Composable
private fun SetupScreen(
    currentTab: BlockTab, onTabChange: (BlockTab) -> Unit,
    domainInput: String, domains: List<String>,
    apps: List<String>, installedApps: List<AppInfo>, appSearchQuery: String,
    onAppSearchChange: (String) -> Unit, onToggleApp: (String) -> Unit, onRemoveApp: (String) -> Unit,
    days: Int, hours: Int, minutes: Int, unlimitedMode: Boolean,
    blockPassword: String, blockPasswordConfirm: String, passwordsMatch: Boolean, requiresPassword: Boolean,
    hasUsageStatsPermission: Boolean, canStart: Boolean,
    onDomainInputChange: (String) -> Unit, onAddDomain: () -> Unit, onRemoveDomain: (String) -> Unit,
    onDaysChange: (Int) -> Unit, onHoursChange: (Int) -> Unit, onMinutesChange: (Int) -> Unit,
    onUnlimitedModeChange: (Boolean) -> Unit, onBlockPasswordChange: (String) -> Unit,
    onBlockPasswordConfirmChange: (String) -> Unit, onEnableUsageStats: () -> Unit,
    onStartBlock: () -> Unit, hardcoreMode: Boolean, onHardcoreModeChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        // Compact tabs as segmented buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            SegmentedTabButton(
                text = stringResource(R.string.tab_domains),
                selected = currentTab == BlockTab.DOMAINS,
                onClick = { onTabChange(BlockTab.DOMAINS) },
                modifier = Modifier.weight(1f)
            )
            SegmentedTabButton(
                text = stringResource(R.string.tab_apps),
                selected = currentTab == BlockTab.APPS,
                onClick = { onTabChange(BlockTab.APPS) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input bar changes based on selected tab
        when (currentTab) {
            BlockTab.DOMAINS -> {
                DomainInput(value = domainInput, label = stringResource(R.string.domain_label), onValueChange = onDomainInputChange, onAdd = onAddDomain)
            }
            BlockTab.APPS -> {
                if (!hasUsageStatsPermission) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.usage_stats_needed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                            TextButton(onClick = onEnableUsageStats) { Text(stringResource(R.string.enable_usage_stats), style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                AppSearchInput(
                    searchQuery = appSearchQuery,
                    installedApps = installedApps,
                    selectedApps = apps,
                    onSearchChange = onAppSearchChange,
                    onToggleApp = onToggleApp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Combined blocked items (websites + apps together)
        if (domains.isNotEmpty() || apps.isNotEmpty()) {
            BlockedItemsChips(
                domains = domains,
                apps = apps,
                installedApps = installedApps,
                onRemoveDomain = onRemoveDomain,
                onRemoveApp = onRemoveApp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Duration section - more compact
        Text(stringResource(R.string.block_duration), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.tertiary)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.then(if (unlimitedMode) Modifier.graphicsLayer { alpha = 0.4f } else Modifier)) {
            DurationFieldCompact(days, stringResource(R.string.days_label), onDaysChange, !unlimitedMode)
            Text(":", style = MaterialTheme.typography.titleLarge)
            DurationFieldCompact(hours, stringResource(R.string.hours_label), onHoursChange, !unlimitedMode)
            Text(":", style = MaterialTheme.typography.titleLarge)
            DurationFieldCompact(minutes, stringResource(R.string.minutes_label), onMinutesChange, !unlimitedMode)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onUnlimitedModeChange(!unlimitedMode) }) {
            Checkbox(checked = unlimitedMode, onCheckedChange = onUnlimitedModeChange)
            Text(stringResource(R.string.unlimited_mode), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = blockPassword, onValueChange = onBlockPasswordChange,
            label = { Text(stringResource(if (requiresPassword) R.string.emergency_password_required else R.string.emergency_password_label), style = MaterialTheme.typography.bodySmall) },
            isError = requiresPassword && blockPassword.isBlank(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        if (blockPassword.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = blockPasswordConfirm, onValueChange = onBlockPasswordConfirmChange,
                label = { Text(stringResource(R.string.emergency_password_confirm_label), style = MaterialTheme.typography.bodySmall) },
                isError = blockPasswordConfirm.isNotEmpty() && !passwordsMatch,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().clickable { onHardcoreModeChange(!hardcoreMode) }, verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = hardcoreMode, onCheckedChange = onHardcoreModeChange)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.hardcore_title), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = if (hardcoreMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onStartBlock, enabled = canStart, shape = CircleShape, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(stringResource(R.string.start_block), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SegmentedTabButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        modifier = modifier.padding(horizontal = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp).fillMaxWidth()
        )
    }
}

@Composable
private fun AppSearchInput(
    searchQuery: String,
    installedApps: List<AppInfo>,
    selectedApps: List<String>,
    onSearchChange: (String) -> Unit,
    onToggleApp: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val displayApps = if (searchQuery.isBlank()) installedApps.take(10) else installedApps.take(20)

    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { onSearchChange(it); expanded = true },
            placeholder = { Text(stringResource(R.string.search_apps)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove))
                    }
                } else if (expanded) {
                    IconButton(onClick = { expanded = false; focusManager.clearFocus() }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) expanded = true }
        )

        if (expanded && displayApps.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LazyColumn {
                    items(displayApps) { app ->
                        val isSelected = app.packageName in selectedApps
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onToggleApp(app.packageName)
                                    if (!isSelected) {
                                        onSearchChange("")
                                        expanded = false
                                        focusManager.clearFocus()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            } else {
                                Spacer(modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                                color = if (app.isInstalled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!app.isInstalled) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Play Store",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        } else if (expanded && installedApps.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "Loading apps...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockedItemsChips(
    domains: List<String>,
    apps: List<String>,
    installedApps: List<AppInfo>,
    onRemoveDomain: (String) -> Unit,
    onRemoveApp: (String) -> Unit
) {
    val appLabels = installedApps.associateBy { it.packageName }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        domains.forEach { domain ->
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("🌐", fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(domain, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp).clickable { onRemoveDomain(domain) })
                }
            }
        }
        apps.forEach { pkg ->
            val label = appLabels[pkg]?.label ?: pkg.substringAfterLast('.')
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("📱", fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp).clickable { onRemoveApp(pkg) })
                }
            }
        }
    }
}

@Composable
private fun DurationFieldCompact(value: Int, label: String, onValueChange: (Int) -> Unit, enabled: Boolean = true) {
    OutlinedTextField(
        value = value.toString(), onValueChange = { onValueChange(it.toIntOrNull() ?: 0) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) }, enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true, modifier = Modifier.width(70.dp)
    )
}

