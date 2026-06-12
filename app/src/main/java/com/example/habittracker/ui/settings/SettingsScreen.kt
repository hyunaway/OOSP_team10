// 경로: com/example/habittracker/ui/settings/SettingsScreen.kt
package com.example.habittracker.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.habittracker.ui.avatar.AvatarGender
import com.example.habittracker.ui.avatar.AvatarImageMapper
import com.example.habittracker.ui.avatar.AvatarState
import com.example.habittracker.ui.theme.HabitBackground
import com.example.habittracker.ui.theme.HabitCardWhite
import com.example.habittracker.ui.theme.HabitDeepMint
import com.example.habittracker.ui.theme.HabitElevation
import com.example.habittracker.ui.theme.HabitLineGray
import com.example.habittracker.ui.theme.HabitMint
import com.example.habittracker.ui.theme.HabitRadius
import com.example.habittracker.ui.theme.HabitSpacing
import com.example.habittracker.ui.theme.HabitTextPrimary
import com.example.habittracker.ui.theme.HabitTextSecondary
import com.example.habittracker.BuildConfig

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var lateNightStart by remember { mutableStateOf("22:00") }
    var thresholdText by remember { mutableStateOf(uiState.digitalInterventionThresholdMinutes.toString()) }
    var cooldownText by remember { mutableStateOf(uiState.digitalInterventionCooldownMinutes.toString()) }
    var thresholdError by remember { mutableStateOf<String?>(null) }
    var cooldownError by remember { mutableStateOf<String?>(null) }
    var waterNotifEnabled by remember { mutableStateOf(true) }
    var mealNotifEnabled by remember { mutableStateOf(true) }
    var stretchNotifEnabled by remember { mutableStateOf(true) }
    var digitalNotifEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.digitalInterventionThresholdMinutes) {
        thresholdText = uiState.digitalInterventionThresholdMinutes.toString()
    }
    LaunchedEffect(uiState.digitalInterventionCooldownMinutes) {
        cooldownText = uiState.digitalInterventionCooldownMinutes.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HabitBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HabitSpacing.base),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "설정",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HabitSpacing.base),
            verticalArrangement = Arrangement.spacedBy(HabitSpacing.md),
        ) {
            SettingsCard(title = "아바타 설정") {
                OutlinedTextField(
                    value = uiState.userName,
                    onValueChange = { viewModel.updateUserName(it) },
                    label = { Text("이름") },
                    placeholder = { Text("나") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(HabitRadius.md),
                )
                Spacer(modifier = Modifier.height(HabitSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HabitSpacing.base),
                ) {
                    AvatarGenderCard(
                        gender = AvatarGender.MALE,
                        selected = uiState.avatarGender == AvatarGender.MALE,
                        onClick = { viewModel.updateAvatarGender(AvatarGender.MALE) },
                        modifier = Modifier.weight(1f),
                    )
                    AvatarGenderCard(
                        gender = AvatarGender.FEMALE,
                        selected = uiState.avatarGender == AvatarGender.FEMALE,
                        onClick = { viewModel.updateAvatarGender(AvatarGender.FEMALE) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // 신체 정보 카드
            var heightText by remember(uiState.userHeightCm) {
                mutableStateOf(if (uiState.userHeightCm > 0f) uiState.userHeightCm.toInt().toString() else "")
            }
            var weightText by remember(uiState.userWeightKg) {
                mutableStateOf(if (uiState.userWeightKg > 0f) uiState.userWeightKg.toInt().toString() else "")
            }
            SettingsCard(title = "신체 정보") {
                Text(
                    text = "물 섭취량 추천을 위해 사용해요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = HabitTextSecondary,
                )
                Spacer(modifier = Modifier.height(HabitSpacing.sm))
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { v ->
                        heightText = v
                        v.toFloatOrNull()?.let { viewModel.updateHeight(it) }
                    },
                    label = { Text("키 (cm)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(HabitRadius.md),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = heightText.isNotEmpty() && (heightText.toFloatOrNull()?.let { it < 120f || it > 250f } ?: true),
                    supportingText = {
                        if (heightText.isNotEmpty() && (heightText.toFloatOrNull()?.let { it < 120f || it > 250f } ?: true)) {
                            Text("120 ~ 250 cm 범위로 입력해주세요.")
                        }
                    },
                )
                Spacer(modifier = Modifier.height(HabitSpacing.sm))
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { v ->
                        weightText = v
                        v.toFloatOrNull()?.let { viewModel.updateWeight(it) }
                    },
                    label = { Text("몸무게 (kg)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(HabitRadius.md),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = weightText.isNotEmpty() && (weightText.toFloatOrNull()?.let { it < 30f || it > 250f } ?: true),
                    supportingText = {
                        if (weightText.isNotEmpty() && (weightText.toFloatOrNull()?.let { it < 30f || it > 250f } ?: true)) {
                            Text("30 ~ 250 kg 범위로 입력해주세요.")
                        }
                    },
                )
            }

            SettingsCard(title = "대표 아바타 우선순위") {
                Text(
                    text = "아바타에 먼저 표시될 습관 순서를 설정해요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = HabitTextSecondary,
                )
                Spacer(modifier = Modifier.height(HabitSpacing.sm))
                uiState.categoryPriorityOrder.forEachIndexed { index, category ->
                    PriorityItemRow(
                        rank = index + 1,
                        category = category,
                        isFirst = index == 0,
                        isLast = index == uiState.categoryPriorityOrder.lastIndex,
                        onMoveUp = { viewModel.moveCategoryPriorityUp(index) },
                        onMoveDown = { viewModel.moveCategoryPriorityDown(index) },
                    )
                }
            }

            SettingsCard(title = "수면 시간") {
                OutlinedTextField(
                    value = uiState.bedTime,
                    onValueChange = { viewModel.updateBedTime(it) },
                    label = { Text("취침 시간 (HH:mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(HabitSpacing.sm))
                OutlinedTextField(
                    value = uiState.wakeTime,
                    onValueChange = { viewModel.updateWakeTime(it) },
                    label = { Text("기상 시간 (HH:mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            SettingsCard(title = "알림 주기") {
                OutlinedTextField(
                    value = uiState.waterReminderIntervalMinutes.toString(),
                    onValueChange = { it.toIntOrNull()?.let { v -> viewModel.updateWaterReminderInterval(v) } },
                    label = { Text("물 알림 주기 (분)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            SettingsCard(title = "야식 설정") {
                OutlinedTextField(
                    value = lateNightStart,
                    onValueChange = { lateNightStart = it },
                    label = { Text("야식 시작 시간 (HH:mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            SettingsCard(title = "디지털 기준") {
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { value ->
                        thresholdText = value
                        val minutes = value.toIntOrNull()
                        if (minutes == null || minutes <= 0) {
                            thresholdError = "1분 이상의 숫자를 입력해주세요."
                        } else {
                            thresholdError = null
                            viewModel.updateDigitalInterventionThresholdMinutes(minutes)
                        }
                    },
                    label = { Text("개입 기준 시간 (분)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = thresholdError != null,
                    supportingText = { thresholdError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(modifier = Modifier.height(HabitSpacing.sm))
                OutlinedTextField(
                    value = cooldownText,
                    onValueChange = { value ->
                        cooldownText = value
                        val minutes = value.toIntOrNull()
                        if (minutes == null || minutes <= 0) {
                            cooldownError = "1분 이상의 숫자를 입력해주세요."
                        } else {
                            cooldownError = null
                            viewModel.updateDigitalInterventionCooldownMinutes(minutes)
                        }
                    },
                    label = { Text("알림 쿨다운 (분)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = cooldownError != null,
                    supportingText = { cooldownError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            SettingsCard(title = "관리 앱 선택") {
                Text(
                    text = "관리 앱 ${uiState.selectedDigitalPackages.size}개 선택됨",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = HabitTextPrimary,
                )
                Spacer(modifier = Modifier.height(HabitSpacing.xs))
                Text(
                    text = "디지털 화면에서 관리 앱을 설정할 수 있어요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = HabitTextSecondary,
                )
            }

            SettingsCard(title = "권한") {
                PermissionStatusRow(
                    label = "알림 권한",
                    granted = uiState.notificationPermissionGranted,
                )
                Spacer(modifier = Modifier.height(HabitSpacing.sm))
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.refreshPermissionStates()
                        }
                    },
                    enabled = !uiState.notificationPermissionGranted,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HabitRadius.button),
                    colors = ButtonDefaults.buttonColors(containerColor = HabitDeepMint),
                ) {
                    Text("알림 권한 요청", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(HabitSpacing.md))

                PermissionStatusRow(
                    label = "사용 기록 접근 권한",
                    granted = uiState.usageAccessGranted,
                )
                Spacer(modifier = Modifier.height(HabitSpacing.sm))
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    enabled = !uiState.usageAccessGranted,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HabitRadius.button),
                ) {
                    Text("사용 기록 접근 권한 허용", color = HabitTextSecondary)
                }
            }

            SettingsCard(title = "메시지 톤") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
                ) {
                    MessageToneButton(
                        label = "칭찬",
                        selected = uiState.preferredMessageTone == "PRAISE",
                        onClick = { viewModel.updatePreferredMessageTone("PRAISE") },
                        modifier = Modifier.weight(1f),
                    )
                    MessageToneButton(
                        label = "유머",
                        selected = uiState.preferredMessageTone == "HUMOR",
                        onClick = { viewModel.updatePreferredMessageTone("HUMOR") },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(HabitSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
                ) {
                    MessageToneButton(
                        label = "공감",
                        selected = uiState.preferredMessageTone == "EMPATHY",
                        onClick = { viewModel.updatePreferredMessageTone("EMPATHY") },
                        modifier = Modifier.weight(1f),
                    )
                    MessageToneButton(
                        label = "도전",
                        selected = uiState.preferredMessageTone == "CHALLENGE",
                        onClick = { viewModel.updatePreferredMessageTone("CHALLENGE") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SettingsCard(title = "카테고리별 알림") {
                NotifToggleRow(label = "💧 물", enabled = waterNotifEnabled, onToggle = { waterNotifEnabled = it })
                NotifToggleRow(label = "🍽 식사", enabled = mealNotifEnabled, onToggle = { mealNotifEnabled = it })
                NotifToggleRow(label = "🧘 스트레칭", enabled = stretchNotifEnabled, onToggle = { stretchNotifEnabled = it })
                NotifToggleRow(label = "📱 디지털", enabled = digitalNotifEnabled, onToggle = { digitalNotifEnabled = it })
            }

            if (BuildConfig.DEBUG) {
                SettingsCard(title = "🛠 디버그 개인화 검증 (방법 A)") {
                    Text(
                        text = "어제(6.4)까지의 가상 데이터를 DB에 주입하고 개인화 분석 파이프라인을 실행합니다. 오늘(6.5) 데이터는 빈 채로 유지되어 하루의 시작 시점을 재현합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = HabitTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(HabitSpacing.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("💧 물 게이트: ${if (uiState.isWaterReady) "Ready ✅" else "Waiting ⏳"}", style = MaterialTheme.typography.bodySmall)
                            Text("🍽 식사 게이트: ${if (uiState.isMealReady) "Ready ✅" else "Waiting ⏳"}", style = MaterialTheme.typography.bodySmall)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🧘 스트레칭: ${if (uiState.isStretchReady) "Ready ✅" else "Waiting ⏳"}", style = MaterialTheme.typography.bodySmall)
                            Text("📱 디지털: ${if (uiState.isDigitalReady) "Ready ✅" else "Waiting ⏳"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(HabitSpacing.sm))

                    Text("🍽 식사 Peak 시간:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("- 아침: ${uiState.resolvedBreakfastTime}  |  점심: ${uiState.resolvedLunchTime}", style = MaterialTheme.typography.bodySmall)
                    Text("- 저녁: ${uiState.resolvedDinnerTime}  |  야식: ${uiState.resolvedLateNightTime}", style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(modifier = Modifier.height(HabitSpacing.xs))
                    Text("💧 물 개인화 상세:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("- 일일 목표: ${uiState.resolvedWaterGoalMl}ml  |  알림 주기: ${uiState.resolvedWaterInterval}분", style = MaterialTheme.typography.bodySmall)
                    Text("- 피크: ${uiState.resolvedWaterPeak}", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(HabitSpacing.xs))
                    Text("🧘 스트레칭 개인화 상세:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("- 일일 목표: ${uiState.resolvedStretchGoal}회  |  선호 시간대: ${uiState.resolvedStretchPreferredSlot}", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(HabitSpacing.xs))
                    Text("📱 디지털 개인화 상세:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("- 유튜브 임계치: ${uiState.resolvedYoutubeThreshold}분 (평균 세션: ${String.format("%.1f", uiState.resolvedYoutubeAvgSession)}분의 80%)", style = MaterialTheme.typography.bodySmall)
                    Text("- 선호 톤: ${uiState.resolvedMessageTone}", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(HabitSpacing.md))

                    if (uiState.debugInfoText.isNotEmpty()) {
                        Text(
                            text = uiState.debugInfoText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = HabitDeepMint,
                        )
                        Spacer(modifier = Modifier.height(HabitSpacing.sm))
                    }

                    Button(
                        onClick = { viewModel.seedDebugData() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(HabitRadius.button),
                        colors = ButtonDefaults.buttonColors(containerColor = HabitDeepMint),
                    ) {
                        Text("1-A. 규칙 데이터 주입 (어제까지)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(HabitSpacing.xs))
                    Button(
                        onClick = { viewModel.seedIrregularDebugData() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(HabitRadius.button),
                        colors = ButtonDefaults.buttonColors(containerColor = HabitDeepMint),
                    ) {
                        Text("1-B. 불규칙 데이터 주입 (어제까지)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(HabitSpacing.xs))
                    Button(
                        onClick = { viewModel.runPersonalizationAnalysis() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(HabitRadius.button),
                        colors = ButtonDefaults.buttonColors(containerColor = HabitDeepMint),
                    ) {
                        Text("2. 개인화 분석 즉시 실행", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(HabitSpacing.md))

            Button(
                onClick = { viewModel.saveSettings() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HabitRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = HabitDeepMint),
            ) {
                Text("저장", color = Color.White, fontWeight = FontWeight.Bold)
            }

            if (uiState.isSaved) {
                Snackbar { Text("설정이 저장되었습니다.") }
            }

            uiState.errorMessage?.let { msg ->
                Text(text = msg, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(HabitSpacing.xl))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HabitRadius.card),
        colors = CardDefaults.cardColors(containerColor = HabitCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.card),
    ) {
        Column(modifier = Modifier.padding(HabitSpacing.lg)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )
            Spacer(modifier = Modifier.height(HabitSpacing.md))
            content()
        }
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    granted: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = if (granted) "허용됨" else "필요함",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (granted) HabitDeepMint else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun MessageToneButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(HabitRadius.md),
            colors = ButtonDefaults.buttonColors(containerColor = HabitDeepMint),
        ) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(HabitRadius.md),
            border = BorderStroke(1.dp, HabitLineGray),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = HabitTextSecondary),
        ) {
            Text(label)
        }
    }
}

@Composable
private fun AvatarGenderCard(
    gender: AvatarGender,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageResId = AvatarImageMapper.resolve(gender, AvatarState.GOOD)

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(HabitRadius.card),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) HabitMint else HabitCardWhite,
        ),
        border = if (selected) BorderStroke(2.dp, HabitDeepMint) else BorderStroke(1.dp, Color(0xFFE8E2DA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HabitSpacing.base),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
        ) {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = gender.label,
                modifier = Modifier.size(80.dp),
            )
            Text(
                text = gender.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) HabitDeepMint else HabitTextPrimary,
            )
        }
    }
}

@Composable
private fun NotifToggleRow(
    label: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun PriorityItemRow(
    rank: Int,
    category: String,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val label = when (category) {
        "MEAL" -> "🍽 식사"
        "WATER" -> "💧 수분"
        "DIGITAL" -> "📱 디지털"
        "STRETCH" -> "🧘 스트레칭"
        else -> category
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = HabitSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "$rank.  $label",
            style = MaterialTheme.typography.bodyMedium,
            color = HabitTextPrimary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(HabitSpacing.xs)) {
            OutlinedButton(
                onClick = onMoveUp,
                enabled = !isFirst,
                modifier = Modifier.size(36.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(HabitRadius.xs),
                border = BorderStroke(1.dp, if (!isFirst) HabitDeepMint else HabitLineGray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HabitDeepMint),
            ) {
                Text("▲", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(
                onClick = onMoveDown,
                enabled = !isLast,
                modifier = Modifier.size(36.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(HabitRadius.xs),
                border = BorderStroke(1.dp, if (!isLast) HabitDeepMint else HabitLineGray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HabitDeepMint),
            ) {
                Text("▼", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
