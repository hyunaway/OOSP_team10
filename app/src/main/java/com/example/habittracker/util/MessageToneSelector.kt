// 경로: com/example/habittracker/util/MessageToneSelector.kt
package com.example.habittracker.util

import javax.inject.Inject
import javax.inject.Singleton

enum class MessageTone { PRAISE, HUMOR, EMPATHY, CHALLENGE }

@Singleton
class MessageToneSelector @Inject constructor() {

    fun selectMessage(category: String, tone: MessageTone): String {
        val pool = messagePool[category] ?: messagePool["default"]!!
        val messages = pool[tone] ?: pool[MessageTone.EMPATHY]!!
        return messages.random()
    }

    fun selectByPreference(
        category: String,
        preferredTone: String,
        fatigueScore: Float,
    ): String {
        val tone = if (fatigueScore > 0.7f) {
            MessageTone.EMPATHY
        } else {
            MessageTone.entries.find { it.name == preferredTone } ?: MessageTone.EMPATHY
        }
        return selectMessage(category, tone)
    }

    private val messagePool: Map<String, Map<MessageTone, List<String>>> = mapOf(
        "water" to mapOf(
            MessageTone.PRAISE to listOf(
                "수분 섭취 습관이 훌륭해요! 오늘도 한 잔 어때요?",
                "꾸준히 물을 마시고 있군요. 정말 대단해요!",
                "건강한 수분 습관을 유지하고 있어요. 오늘도 화이팅!",
            ),
            MessageTone.HUMOR to listOf(
                "당신의 세포들이 목말라하고 있어요. 구해주세요!",
                "식물도 물 없이는 못 살아요. 당신도요!",
                "커피는 물이 아니에요… 그래도 물 한 잔 마셔봐요!",
            ),
            MessageTone.EMPATHY to listOf(
                "바쁜 하루지만, 잠깐 멈추고 물 한 잔 마셔요.",
                "몸이 힘들 때일수록 수분이 중요해요.",
                "오늘 고생 많았죠? 물 한 잔으로 충전해요.",
            ),
            MessageTone.CHALLENGE to listOf(
                "오늘 목표 달성까지 조금만 더! 물 한 잔 마셔봐요.",
                "수분 목표, 포기하지 마세요. 지금 당장 한 잔!",
                "챌린지를 완수하세요. 물 한 잔이 차이를 만들어요!",
            ),
        ),
        "meal" to mapOf(
            MessageTone.PRAISE to listOf(
                "규칙적인 식사 습관, 정말 잘하고 있어요!",
                "식사 기록을 꾸준히 남기고 있군요. 훌륭해요!",
                "건강한 식습관을 지키고 있어요. 계속 이어가요!",
            ),
            MessageTone.HUMOR to listOf(
                "뱃속에서 신호가 왔어요. 밥 먹을 시간이에요!",
                "위장이 파업 예고했어요. 밥 먹어주세요!",
                "배가 꼬르륵하죠? 그건 몸의 SOS 신호예요!",
            ),
            MessageTone.EMPATHY to listOf(
                "바쁘더라도 식사는 챙겨야 해요.",
                "몸을 위해 제때 식사하는 것이 중요해요.",
                "잠깐 시간 내어 밥 한 끼 드세요. 당신은 그럴 자격이 있어요.",
            ),
            MessageTone.CHALLENGE to listOf(
                "식사 기록 도전! 오늘 세 끼 모두 기록해봐요.",
                "규칙적인 식사 챌린지, 오늘도 완수해봐요!",
                "목표 식사 시간을 지켜봐요. 할 수 있어요!",
            ),
        ),
        "digital" to mapOf(
            MessageTone.PRAISE to listOf(
                "스마트폰 사용을 잘 조절하고 있어요!",
                "디지털 절제력이 대단해요. 계속 유지해요!",
                "화면 시간 관리 능력이 훌륭해요!",
            ),
            MessageTone.HUMOR to listOf(
                "스마트폰이 당신보다 더 많이 쉬어야 할 것 같아요!",
                "앱들이 '잠깐만요'라고 하네요. 지금 내려놓아봐요!",
                "스크롤이 운동은 아니에요. 잠깐 쉬어가요!",
            ),
            MessageTone.EMPATHY to listOf(
                "디지털 기기 사용을 잠깐 멈추고 쉬어가요.",
                "눈도, 뇌도 휴식이 필요해요. 잠깐 내려놓아요.",
                "스크린에서 눈을 떼고 주변을 둘러봐요.",
            ),
            MessageTone.CHALLENGE to listOf(
                "30분 디지털 디톡스 챌린지! 지금 시작해봐요.",
                "스마트폰 없이 1시간, 도전해봐요!",
                "오늘 목표 사용 시간을 지켜봐요. 할 수 있어요!",
            ),
        ),
        "stretch" to mapOf(
            MessageTone.PRAISE to listOf(
                "스트레칭 습관이 정말 훌륭해요!",
                "꾸준한 스트레칭으로 몸이 감사하고 있을 거예요!",
                "건강한 스트레칭 루틴을 유지하고 있어요. 대단해요!",
            ),
            MessageTone.HUMOR to listOf(
                "몸이 '삐걱삐걱' 소리를 내고 있어요. 스트레칭 시간이에요!",
                "로봇처럼 굳기 전에 스트레칭해요!",
                "당신의 근육들이 '좀 풀어줘요'라고 외치고 있어요!",
            ),
            MessageTone.EMPATHY to listOf(
                "오래 앉아 있었죠? 몸을 한번 풀어줘요.",
                "피로가 쌓이기 전에 가볍게 스트레칭해요.",
                "몸이 무겁게 느껴진다면 스트레칭이 도움이 될 거예요.",
            ),
            MessageTone.CHALLENGE to listOf(
                "5분 스트레칭 챌린지! 지금 바로 시작해봐요.",
                "오늘 스트레칭 목표를 달성해봐요. 화이팅!",
                "매 시간 스트레칭 챌린지, 지금 이 순간이 기회예요!",
            ),
        ),
        "default" to mapOf(
            MessageTone.PRAISE to listOf("잘하고 있어요! 계속 이어가요."),
            MessageTone.HUMOR to listOf("몸이 신호를 보내고 있어요. 반응해줘요!"),
            MessageTone.EMPATHY to listOf("잠깐 멈추고 자신을 돌봐요."),
            MessageTone.CHALLENGE to listOf("오늘의 목표를 달성해봐요!"),
        ),
    )
}
