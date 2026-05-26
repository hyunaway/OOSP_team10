// 경로: com/example/habittracker/ui/home/HomeScreen.kt
package com.example.habittracker.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.habittracker.domain.model.DigitalTodayStatus
import com.example.habittracker.domain.model.MealTodayStatus
import com.example.habittracker.domain.model.StretchTodayStatus
import com.example.habittracker.domain.model.WaterTodayStatus
import com.example.habittracker.ui.avatar.AvatarUiState
import com.example.habittracker.ui.theme.DigitalBackground
import com.example.habittracker.ui.theme.DigitalPrimary
import com.example.habittracker.ui.theme.HabitBackground
import com.example.habittracker.ui.theme.HabitCardWhite
import com.example.habittracker.ui.theme.HabitDeepMint
import com.example.habittracker.ui.theme.HabitElevation
import com.example.habittracker.ui.theme.HabitLineGray
import com.example.habittracker.ui.theme.HabitRadius
import com.example.habittracker.ui.theme.HabitSpacing
import com.example.habittracker.ui.theme.HabitTextPrimary
import com.example.habittracker.ui.theme.HabitTextSecondary
import com.example.habittracker.ui.theme.MealBackground
import com.example.habittracker.ui.theme.MealPrimary
import com.example.habittracker.ui.theme.StretchBackground
import com.example.habittracker.ui.theme.StretchPrimary
import com.example.habittracker.ui.theme.WaterBackground
import com.example.habittracker.ui.theme.WaterPrimary
import kotlinx.coroutines.launch

// ── 홈 메인 화면 ──────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    var showNotificationWarning by remember {
        mutableStateOf(!hasNotificationPermission(context))
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                showNotificationWarning = !hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HabitBackground),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            HomeTopBar(
                onSettingsClick = { navController.navigate("settings") },
                onReportsClick = { navController.navigate("reports") },
            )

            if (showNotificationWarning) {
                NotificationPermissionWarning()
            }

            if (uiState.loading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = HabitDeepMint)
                }
            } else {
                AvatarBubbleSection(
                    avatarUiState = uiState.avatarUiState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HabitSpacing.base, vertical = HabitSpacing.sm),
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                ) { page ->
                    val dash = uiState.dashboardState
                    when (page) {
                        0 -> MealPage(
                            mealStatus = dash?.mealStatus,
                            onNavigate = { navController.navigate("meal") },
                        )
                        1 -> WaterPage(
                            waterStatus = dash?.waterStatus,
                            onNavigate = { navController.navigate("water") },
                        )
                        2 -> DigitalPage(
                            digitalStatus = dash?.digitalStatus,
                            onNavigate = { navController.navigate("digital") },
                        )
                        else -> StretchPage(
                            stretchStatus = dash?.stretchStatus,
                            onNavigate = { navController.navigate("stretch") },
                        )
                    }
                }
            }

            CategoryTabRow(
                currentPage = pagerState.currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                },
            )
        }
    }
}

// ── 상단 바 ───────────────────────────────────────────────────────────────────

private fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun NotificationPermissionWarning() {
    Text(
        text = "알림 권한이 꺼져 있어 원활한 습관 개입이 어렵습니다. 설정에서 알림 권한을 허용해주세요.",
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HabitSpacing.base, vertical = HabitSpacing.xs),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun HomeTopBar(
    onSettingsClick: () -> Unit,
    onReportsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HabitSpacing.xs, vertical = HabitSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "설정",
                tint = HabitTextSecondary,
            )
        }
        Text(
            text = "해빗프렌즈",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = HabitTextPrimary,
        )
        IconButton(onClick = onReportsClick) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = "리포트",
                tint = HabitTextSecondary,
            )
        }
    }
}

// ── 아바타 + 말풍선 ───────────────────────────────────────────────────────────

@Composable
private fun AvatarBubbleSection(
    avatarUiState: AvatarUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(160.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (avatarUiState.imageResId != 0) {
            Image(
                painter = painterResource(id = avatarUiState.imageResId),
                contentDescription = "아바타",
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier.size(120.dp, 160.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("👤", style = MaterialTheme.typography.displayMedium)
            }
        }

        Spacer(modifier = Modifier.width(HabitSpacing.sm))

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = HabitSpacing.base),
            shape = RoundedCornerShape(
                topStart = HabitRadius.xs,
                topEnd = HabitRadius.card,
                bottomStart = HabitRadius.card,
                bottomEnd = HabitRadius.card,
            ),
            colors = CardDefaults.cardColors(containerColor = HabitCardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.card),
        ) {
            Column(modifier = Modifier.padding(HabitSpacing.base)) {
                Text(
                    text = "${avatarUiState.userName}님,",
                    style = MaterialTheme.typography.labelMedium,
                    color = HabitTextSecondary,
                )
                Spacer(modifier = Modifier.height(HabitSpacing.xxs))
                Text(
                    text = avatarUiState.bubbleMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = HabitTextPrimary,
                )
            }
        }
    }
}

// ── 하단 카테고리 탭 ──────────────────────────────────────────────────────────

private data class CategoryTabInfo(
    val label: String,
    val icon: ImageVector,
    val selectedColor: Color,
)

private val categoryTabs = listOf(
    CategoryTabInfo("식사", Icons.Default.Restaurant, MealPrimary),
    CategoryTabInfo("물", Icons.Default.LocalDrink, WaterPrimary),
    CategoryTabInfo("디지털", Icons.Default.Smartphone, DigitalPrimary),
    CategoryTabInfo("스트레칭", Icons.Default.SelfImprovement, StretchPrimary),
)

@Composable
private fun CategoryTabRow(
    currentPage: Int,
    onTabSelected: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HabitCardWhite,
        shadowElevation = HabitElevation.dialog,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = HabitSpacing.xs),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            categoryTabs.forEachIndexed { index, tab ->
                CategoryTab(
                    selected = currentPage == index,
                    label = tab.label,
                    icon = tab.icon,
                    selectedColor = tab.selectedColor,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CategoryTab(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = HabitSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HabitSpacing.xxs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) selectedColor else HabitTextSecondary,
            modifier = Modifier.size(if (selected) 26.dp else 22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) selectedColor else HabitTextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// ── 카테고리 페이지 공통 카드 틀 ─────────────────────────────────────────────

@Composable
private fun CategoryPageCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(HabitSpacing.base),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(HabitRadius.card),
            colors = CardDefaults.cardColors(containerColor = HabitCardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.card),
        ) {
            Column(modifier = Modifier.padding(HabitSpacing.lg)) {
                content()
            }
        }
    }
}

@Composable
private fun PageHeader(
    emoji: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    bgColor: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = bgColor,
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(emoji, style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(modifier = Modifier.width(HabitSpacing.sm))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = HabitTextSecondary,
            )
        }
    }
    Spacer(modifier = Modifier.height(HabitSpacing.base))
    HorizontalDivider(color = HabitLineGray)
    Spacer(modifier = Modifier.height(HabitSpacing.base))
}

@Composable
private fun PageActionButton(label: String, color: Color, onClick: () -> Unit) {
    Spacer(modifier = Modifier.height(HabitSpacing.base))
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HabitRadius.button),
        colors = ButtonDefaults.buttonColors(containerColor = color),
    ) {
        Text(text = label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

// ── 식사 페이지 ───────────────────────────────────────────────────────────────

@Composable
private fun MealPage(
    mealStatus: MealTodayStatus?,
    onNavigate: () -> Unit,
) {
    CategoryPageCard {
        PageHeader(
            emoji = "🍽️",
            title = "식사",
            subtitle = "균형 잡힌 하루 한 끼",
            accentColor = MealPrimary,
            bgColor = MealBackground,
        )
        if (mealStatus != null) {
            MealCheckRow("아침", mealStatus.breakfastLogged)
            Spacer(modifier = Modifier.height(HabitSpacing.sm))
            MealCheckRow("점심", mealStatus.lunchLogged)
            Spacer(modifier = Modifier.height(HabitSpacing.sm))
            MealCheckRow("저녁", mealStatus.dinnerLogged)
        } else {
            Text("데이터 로딩 중...", color = HabitTextSecondary)
        }
        PageActionButton("기록하기", MealPrimary, onNavigate)
    }
}

@Composable
private fun MealCheckRow(label: String, logged: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = HabitTextPrimary,
        )
        if (logged) {
            Surface(shape = CircleShape, color = MealPrimary, modifier = Modifier.size(24.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "완료",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        } else {
            Surface(shape = CircleShape, color = HabitLineGray, modifier = Modifier.size(24.dp)) {}
        }
    }
}

// ── 물 페이지 ─────────────────────────────────────────────────────────────────

@Composable
private fun WaterPage(
    waterStatus: WaterTodayStatus?,
    onNavigate: () -> Unit,
) {
    CategoryPageCard {
        PageHeader(
            emoji = "💧",
            title = "물",
            subtitle = "매일 ${waterStatus?.goalMl ?: 2000}ml 목표",
            accentColor = WaterPrimary,
            bgColor = WaterBackground,
        )
        if (waterStatus != null) {
            Text(
                text = "${waterStatus.totalMl}ml / ${waterStatus.goalMl}ml",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = WaterPrimary,
            )
            Spacer(modifier = Modifier.height(HabitSpacing.sm))
            LinearProgressIndicator(
                progress = { waterStatus.achievementRate.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = WaterPrimary,
                trackColor = WaterBackground,
            )
            Spacer(modifier = Modifier.height(HabitSpacing.xs))
            Text(
                text = "${(waterStatus.achievementRate * 100).toInt()}% 달성",
                style = MaterialTheme.typography.bodySmall,
                color = HabitTextSecondary,
            )
        } else {
            Text("데이터 로딩 중...", color = HabitTextSecondary)
        }
        PageActionButton("추가하기", WaterPrimary, onNavigate)
    }
}

// ── 디지털 페이지 ─────────────────────────────────────────────────────────────

@Composable
private fun DigitalPage(
    digitalStatus: DigitalTodayStatus?,
    onNavigate: () -> Unit,
) {
    CategoryPageCard {
        PageHeader(
            emoji = "📱",
            title = "디지털",
            subtitle = "스마트폰 · PC · 태블릿 사용 습관",
            accentColor = DigitalPrimary,
            bgColor = DigitalBackground,
        )
        if (digitalStatus != null) {
            val hours = digitalStatus.totalUsageMinutes / 60
            val mins = digitalStatus.totalUsageMinutes % 60
            Text(
                text = if (hours > 0) "${hours}시간 ${mins}분" else "${mins}분",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DigitalPrimary,
            )
            Spacer(modifier = Modifier.height(HabitSpacing.xs))
            Text(
                text = "오늘 디지털 기기 사용 시간",
                style = MaterialTheme.typography.bodySmall,
                color = HabitTextSecondary,
            )
        } else {
            Text("데이터 로딩 중...", color = HabitTextSecondary)
        }
        PageActionButton("상세 보기", DigitalPrimary, onNavigate)
    }
}

// ── 스트레칭 페이지 ───────────────────────────────────────────────────────────

@Composable
private fun StretchPage(
    stretchStatus: StretchTodayStatus?,
    onNavigate: () -> Unit,
) {
    CategoryPageCard {
        PageHeader(
            emoji = "🧘",
            title = "스트레칭",
            subtitle = "몸도 마음도 가볍게, 오늘도 스트레칭!",
            accentColor = StretchPrimary,
            bgColor = StretchBackground,
        )
        if (stretchStatus != null) {
            Text(
                text = "오늘 ${stretchStatus.totalCount}회 완료",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = StretchPrimary,
            )
            Spacer(modifier = Modifier.height(HabitSpacing.sm))
            LinearProgressIndicator(
                progress = { stretchStatus.avatarHealthScore.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = StretchPrimary,
                trackColor = StretchBackground,
            )
            Spacer(modifier = Modifier.height(HabitSpacing.xs))
            Text(
                text = "연속 달성 ${stretchStatus.totalCount}일째",
                style = MaterialTheme.typography.bodySmall,
                color = HabitTextSecondary,
            )
        } else {
            Text("데이터 로딩 중...", color = HabitTextSecondary)
        }
        PageActionButton("스트레칭하기", StretchPrimary, onNavigate)
    }
}
