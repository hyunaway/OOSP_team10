package com.example.habittracker.domain.model

/**
 * 특정 행동(식사·물 섭취 등)의 시간대 집중 패턴을 표현하는 값 객체.
 *
 * @param centerMinutes 하루 기준 peak 중앙값 (분, 0~1439)
 *                      예) 480 = 08:00
 * @param rangeStart    peak 창 시작 (centerMinutes - σ 반영, 분)
 * @param rangeEnd      peak 창 끝   (centerMinutes + σ 반영, 분)
 * @param concentration 신뢰도 / 집중도 (0.0 ~ 1.0).
 *                      표준편차가 작을수록 높은 값.
 *                      게이트 임계값 비교에 사용.
 */
data class PeakWindow(
    val centerMinutes: Int,
    val rangeStart: Int,
    val rangeEnd: Int,
    val concentration: Float,
) {
    companion object {
        /** DataStore에 저장할 수 없는 null을 대신하는 빈 sentinel 값 */
        val EMPTY = PeakWindow(centerMinutes = -1, rangeStart = -1, rangeEnd = -1, concentration = 0f)

        fun fromJson(json: String): PeakWindow? {
            if (json.isBlank()) return null
            return try {
                val map = json.trim('{', '}')
                    .split(",")
                    .associate { pair ->
                        val (k, v) = pair.split(":")
                        k.trim().trim('"') to v.trim()
                    }
                PeakWindow(
                    centerMinutes = map["c"]?.toIntOrNull() ?: return null,
                    rangeStart    = map["s"]?.toIntOrNull() ?: return null,
                    rangeEnd      = map["e"]?.toIntOrNull() ?: return null,
                    concentration = map["p"]?.toFloatOrNull() ?: return null,
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    fun toJson(): String =
        """{"c":$centerMinutes,"s":$rangeStart,"e":$rangeEnd,"p":$concentration}"""

    fun isEmpty(): Boolean = centerMinutes == -1
}
