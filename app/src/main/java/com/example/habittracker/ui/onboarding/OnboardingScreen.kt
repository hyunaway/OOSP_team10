// 경로: com/example/habittracker/ui/onboarding/OnboardingScreen.kt
package com.example.habittracker.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittracker.ui.avatar.AvatarGender
import com.example.habittracker.ui.avatar.AvatarImageMapper
import com.example.habittracker.ui.avatar.AvatarState
import com.example.habittracker.ui.theme.HabitBackground
import com.example.habittracker.ui.theme.HabitCardWhite
import com.example.habittracker.ui.theme.HabitDeepMint
import com.example.habittracker.ui.theme.HabitMint
import com.example.habittracker.ui.theme.HabitRadius
import com.example.habittracker.ui.theme.HabitSpacing
import com.example.habittracker.ui.theme.HabitTextPrimary
import com.example.habittracker.ui.theme.HabitTextSecondary

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.completeOnboarding(onComplete)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HabitBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HabitSpacing.base, vertical = HabitSpacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(HabitSpacing.base),
    ) {
        Text(
            text = "해빗프렌즈 시작하기",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = HabitTextPrimary,
        )
        Text(
            text = "나의 습관을 함께 고칠 아바타를 설정해볼까요?",
            style = MaterialTheme.typography.bodyMedium,
            color = HabitTextSecondary,
        )

        Spacer(modifier = Modifier.height(HabitSpacing.lg))

        Text(
            text = "이름",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = HabitTextPrimary,
        )
        OutlinedTextField(
            value = viewModel.userName,
            onValueChange = { viewModel.updateUserName(it) },
            label = { Text("이름") },
            placeholder = { Text("나", color = HabitTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(HabitRadius.md),
        )

        Spacer(modifier = Modifier.height(HabitSpacing.lg))

        Text(
            text = "아바타 선택",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = HabitTextPrimary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HabitSpacing.base),
        ) {
            AvatarGenderCard(
                gender = AvatarGender.MALE,
                selected = viewModel.selectedGender == AvatarGender.MALE,
                onClick = { viewModel.selectGender(AvatarGender.MALE) },
                modifier = Modifier.weight(1f),
            )
            AvatarGenderCard(
                gender = AvatarGender.FEMALE,
                selected = viewModel.selectedGender == AvatarGender.FEMALE,
                onClick = { viewModel.selectGender(AvatarGender.FEMALE) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(HabitSpacing.xl))

        Button(
            onClick = {
                if (needsNotificationPermission(context)) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.completeOnboarding(onComplete)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(HabitRadius.button),
            colors = ButtonDefaults.buttonColors(containerColor = HabitDeepMint),
        ) {
            Text(
                text = "시작하기",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

private fun needsNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) != PackageManager.PERMISSION_GRANTED
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
                modifier = Modifier.size(96.dp),
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
