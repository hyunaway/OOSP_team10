package com.example.habittracker.ui.digital

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.data.usage.InstalledAppInfo
import com.example.habittracker.ui.theme.DigitalPrimary
import com.example.habittracker.ui.theme.HabitBackground
import com.example.habittracker.ui.theme.HabitCardWhite
import com.example.habittracker.ui.theme.HabitDeepMint
import com.example.habittracker.ui.theme.HabitElevation
import com.example.habittracker.ui.theme.HabitRadius
import com.example.habittracker.ui.theme.HabitSpacing
import com.example.habittracker.ui.theme.HabitTextPrimary
import com.example.habittracker.ui.theme.HabitTextSecondary

@Composable
fun DigitalAppSelectionScreen(
    onBack: () -> Unit,
    viewModel: DigitalAppSelectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredApps = remember(uiState.installedApps, uiState.searchQuery) {
        val query = uiState.searchQuery.trim()
        if (query.isEmpty()) {
            uiState.installedApps
        } else {
            uiState.installedApps.filter { it.appName.contains(query, ignoreCase = true) }
        }
    }
    val selectedApps = remember(uiState.installedApps, uiState.selectedPackages) {
        uiState.installedApps.filter { it.packageName in uiState.selectedPackages }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HabitBackground)
            .padding(HabitSpacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "디지털 관리 앱",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(HabitRadius.button),
            ) {
                Text("닫기", color = HabitTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(HabitSpacing.md))
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            label = { Text("앱 이름 검색") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(HabitRadius.md),
        )

        Spacer(modifier = Modifier.height(HabitSpacing.md))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
        ) {
            item {
                SectionHeader("선택된 앱")
                if (selectedApps.isEmpty()) {
                    Text(
                        text = "아직 선택된 앱이 없어요.",
                        modifier = Modifier.padding(vertical = HabitSpacing.sm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = HabitTextSecondary,
                    )
                }
            }

            items(selectedApps, key = { "selected-${it.packageName}" }) { app ->
                AppSelectionRow(
                    app = app,
                    selected = true,
                    onToggle = { viewModel.togglePackage(app.packageName) },
                )
            }

            item {
                Spacer(modifier = Modifier.height(HabitSpacing.md))
                SectionHeader("전체 앱")
                if (uiState.loading) {
                    Text(
                        text = "앱 목록을 불러오는 중입니다.",
                        modifier = Modifier.padding(vertical = HabitSpacing.sm),
                        color = HabitTextSecondary,
                    )
                } else if (filteredApps.isEmpty()) {
                    Text(
                        text = "검색 결과가 없습니다.",
                        modifier = Modifier.padding(vertical = HabitSpacing.sm),
                        color = HabitTextSecondary,
                    )
                }
            }

            items(filteredApps, key = { "all-${it.packageName}" }) { app ->
                AppSelectionRow(
                    app = app,
                    selected = app.packageName in uiState.selectedPackages,
                    onToggle = { viewModel.togglePackage(app.packageName) },
                )
            }

            uiState.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = DigitalPrimary,
    )
}

@Composable
private fun AppSelectionRow(
    app: InstalledAppInfo,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val iconBitmap = remember(app.packageName, app.icon) {
        app.icon.toBitmap(width = 44, height = 44).asImageBitmap()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(HabitRadius.card),
        colors = CardDefaults.cardColors(containerColor = HabitCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HabitSpacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
        ) {
            Image(
                bitmap = iconBitmap,
                contentDescription = app.appName,
                modifier = Modifier.size(44.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = HabitTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = HabitTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
            )
        }
    }
}
