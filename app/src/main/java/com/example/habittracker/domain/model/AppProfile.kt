package com.example.habittracker.domain.model

/**
 * 디지털 앱 1개에 대한 사용 프로필 (perAppProfileJson 직렬화 단위).
 *
 * @param packageName           앱 패키지명
 * @param suggestedThresholdMinutes 평균 세션 × 0.8 로 산출한 권장 개입 임계값(분)
 * @param avgSessionMinutes     30일 평균 세션 시간(분)
 */
data class AppProfile(
    val packageName: String,
    val suggestedThresholdMinutes: Int,
    val avgSessionMinutes: Float,
) {
    fun toJson(): String =
        """{"pkg":"${packageName.replace("\"", "\\\"")}","thr":$suggestedThresholdMinutes,"avg":$avgSessionMinutes}"""

    companion object {
        fun listToJson(profiles: List<AppProfile>): String =
            "[${profiles.joinToString(",") { it.toJson() }}]"

        fun listFromJson(json: String): List<AppProfile> {
            if (json.isBlank() || json == "[]") return emptyList()
            return try {
                // 간단한 수동 파싱: [{"pkg":"...","thr":N,"avg":N.N}, ...]
                val content = json.trim('[', ']')
                content.split("},{").map { raw ->
                    val obj = raw.trim('{', '}')
                    val fields = obj.split(",")
                        .associate { pair ->
                            val idx = pair.indexOf(':')
                            val k = pair.substring(0, idx).trim().trim('"')
                            val v = pair.substring(idx + 1).trim()
                            k to v
                        }
                    AppProfile(
                        packageName = fields["pkg"]?.trim('"') ?: "",
                        suggestedThresholdMinutes = fields["thr"]?.toIntOrNull() ?: 30,
                        avgSessionMinutes = fields["avg"]?.toFloatOrNull() ?: 0f,
                    )
                }.filter { it.packageName.isNotBlank() }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
