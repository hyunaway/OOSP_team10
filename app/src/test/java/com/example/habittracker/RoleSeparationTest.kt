package com.example.habittracker

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 역할 분리 검증 테스트.
 *
 * 원칙: 알림(util/Notification*) · UI(ui/) 코드는 Room DAO를 직접 import하지 않는다.
 *  - Room DAO → UseCase/Repository 레이어가 추상화
 *  - UI/알림 → DataStore 또는 도메인 레이어만 참조
 *
 * Worker 디렉토리는 제외: Worker는 조작 목적으로 직접 DAO 접근 허용.
 */
class RoleSeparationTest {

    private val DAO_IMPORT_PATTERN = "import com.example.habittracker.data.local.room.dao"
    private val projectRoot        = File("src/main/java/com/example/habittracker")

    // ─── UI 레이어 ─────────────────────────────────────────────────────────────
    @Test
    fun `ui layer must not import Room DAO directly`() {
        val violations = scanForImport(
            dir     = File(projectRoot, "ui"),
            pattern = DAO_IMPORT_PATTERN,
        )
        assertTrue(
            "UI 파일에 Room DAO import 발견 (역할 분리 위반):\n" +
            violations.joinToString("\n") { "  • ${it.name}" },
            violations.isEmpty(),
        )
    }

    // ─── NotificationHelper / util 레이어 ──────────────────────────────────────
    // NotificationHelper.kt 는 알림 액션 로깅(NotificationActionLogDao) 목적으로
    // DAO를 합법적으로 사용하는 유일한 util 파일 → 제외.
    // 나머지 util 파일(MessageToneSelector 등)은 DAO 직접 접근 금지.
    @Test
    fun `notification util must not import Room DAO directly`() {
        val violations = scanForImport(
            dir     = File(projectRoot, "util"),
            pattern = DAO_IMPORT_PATTERN,
            // NotificationHelper는 알림 액션 로깅용 DAO 허용 예외
            excludeFiles = setOf("NotificationHelper.kt"),
        )
        assertTrue(
            "util 파일에 Room DAO import 발견 (역할 분리 위반):\n" +
            violations.joinToString("\n") { "  • ${it.name}" },
            violations.isEmpty(),
        )
    }

    // ─── Widget 레이어 ─────────────────────────────────────────────────────────
    @Test
    fun `widget layer must not import Room DAO directly`() {
        val violations = scanForImport(
            dir     = File(projectRoot, "widget"),
            pattern = DAO_IMPORT_PATTERN,
        )
        assertTrue(
            "widget 파일에 Room DAO import 발견 (역할 분리 위반):\n" +
            violations.joinToString("\n") { "  • ${it.name}" },
            violations.isEmpty(),
        )
    }

    // ─── domain 레이어는 data.local.room 전체 접근 금지 ─────────────────────────
    @Test
    fun `domain layer must not import any data layer room classes`() {
        // domain/model, domain/analysis 는 순수 Kotlin (Room 의존 금지)
        val forbidden = listOf("domain/model", "domain/analysis")
            .map { File(projectRoot, it) }

        val violations = forbidden.flatMap { dir ->
            scanForImport(dir, "import com.example.habittracker.data.local.room")
        }
        assertTrue(
            "domain 순수 레이어에 Room import 발견:\n" +
            violations.joinToString("\n") { "  • ${it.name}" },
            violations.isEmpty(),
        )
    }

    // ─── 개인화 분석 엔진은 외부 의존 없이 순수 함수만 사용 ─────────────────────
    @Test
    fun `analysis engine must not import android or data layer`() {
        val forbiddenPrefixes = listOf(
            "import android.",
            "import androidx.",
            "import com.example.habittracker.data.",
        )
        val analysisDir = File(projectRoot, "domain/analysis")
        if (!analysisDir.exists()) return  // 디렉토리 없으면 skip

        val violations = analysisDir.walkTopDown()
            .filter { it.extension == "kt" }
            .filter { file ->
                // PersonalizationResolver는 DataStore 읽기 허용 (단 Room은 불가)
                if (file.name == "PersonalizationResolver.kt") {
                    file.readText().contains("import com.example.habittracker.data.local.room")
                } else {
                    val text = file.readText()
                    forbiddenPrefixes.any { prefix -> text.contains(prefix) }
                }
            }
            .toList()

        assertTrue(
            "analysis engine 에 외부 의존성 발견:\n" +
            violations.joinToString("\n") { "  • ${it.name}" },
            violations.isEmpty(),
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────────────────

    private fun scanForImport(
        dir          : File,
        pattern      : String,
        excludeFiles : Set<String> = emptySet(),
    ): List<File> {
        if (!dir.exists()) return emptyList()
        return dir.walkTopDown()
            .filter { it.extension == "kt" }
            .filter { it.name !in excludeFiles }
            .filter { it.readText().contains(pattern) }
            .toList()
    }
}
