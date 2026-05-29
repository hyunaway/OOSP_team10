// 경로: com/example/habittracker/ui/avatar/AvatarState.kt
package com.example.habittracker.ui.avatar

// 우선순위: 숫자가 낮을수록 높은 우선순위
// 식사 부족 > 물 부족 > 디지털 과사용 > 스트레칭 부족 > 양호
enum class AvatarState(val priority: Int) {
    WARNING(0),
    MEAL_LACK(1),
    WATER_LACK(2),
    DIGITAL_OVERUSE(3),
    STRETCH_LACK(4),
    GOOD(5);

    val label: String get() = when (this) {
        WARNING         -> "경고"
        MEAL_LACK       -> "식사 부족"
        WATER_LACK      -> "물 부족"
        DIGITAL_OVERUSE -> "디지털 과사용"
        STRETCH_LACK    -> "스트레칭 부족"
        GOOD            -> "양호"
    }

    val bubbleMessage: String get() = when (this) {
        WARNING         -> "끼니를 계속 거르고 있어요!\n건강을 위해 식사를 꼭 챙겨주세요. ⚠️"
        MEAL_LACK       -> "아직 정기적으로 먹지 못했어요.\n가볍게 뭔가 챙겨볼까요? 🍽️"
        WATER_LACK      -> "물이 조금 부족해요.\n한 잔만 마셔도 좋아요! 💧"
        DIGITAL_OVERUSE -> "눈이 피곤해 보여요.\n5분만 쉬어갈까요? 📱"
        STRETCH_LACK    -> "어깨가 굳어 있어요.\n잠깐 풀어볼까요? 🧘"
        GOOD            -> "오늘 습관을 잘 지키고 있어요!\n이대로 가볼까요? 😊"
    }
}
