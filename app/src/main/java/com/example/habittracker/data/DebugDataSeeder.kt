package com.example.habittracker.data

import com.example.habittracker.data.entity.*
import com.example.habittracker.data.local.room.dao.*
import com.example.habittracker.data.model.MealType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 디버그용 가상 데이터 시더.
 * 페르소나의 데이터를 삽입하여 개인화 시스템 게이트를 통과하도록 한다.
 * 모든 데이터는 어제(오늘 - 1일) 날짜까지만 주입되며, 오늘 날짜는 빈 상태로 유지됩니다.
 */
@Singleton
class DebugDataSeeder @Inject constructor(
    private val appDatabase: AppDatabase,
    private val waterDao: WaterDao,
    private val mealDao: MealDao,
    private val stretchDao: StretchDao,
    private val digitalSessionDao: DigitalSessionDao,
    private val digitalInterventionLogDao: DigitalInterventionLogDao,
) {

    /**
     * Room Database의 모든 테이블을 초기화합니다.
     */
    suspend fun clearAllData() {
        appDatabase.clearAllTables()
    }

    /**
     * 어제(6.4) 기준 14일치 [규칙적인 페르소나 "김민준"] 데이터를 삽입합니다.
     */
    suspend fun seedPersonaData() {
        val calendar = Calendar.getInstance()
        val yesterdayMs = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // 어제
        val oneDayMs = 24 * 60 * 60 * 1000L

        // 14일 전부터 어제까지 데이터를 채워넣음
        for (dayOffset in 0..13) {
            calendar.timeInMillis = yesterdayMs - (dayOffset * oneDayMs)
            val yyyyMMdd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            // 1. 물 데이터 시딩: 매일 8회, 250ml씩 총 2000ml (9시, 11시, 13시, 15시, 17시, 19시, 20시, 21시)
            val waterHours = listOf(9, 11, 13, 15, 17, 19, 20, 21)
            for (hour in waterHours) {
                val cal = calendar.clone() as Calendar
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                waterDao.insert(
                    WaterLogEntity(
                        timestamp = cal.timeInMillis,
                        amountMl = 250,
                        source = "MANUAL"
                    )
                )
            }

            // 2. 식사 데이터 시딩: BREAKFAST(08:00), LUNCH(12:30), DINNER(19:00) 3끼 기록. 추가로 3일에 한 번 야식(22:30) 기록.
            val mealTimes = listOf(
                Triple(8, 0, MealType.BREAKFAST),
                Triple(12, 30, MealType.LUNCH),
                Triple(19, 0, MealType.DINNER)
            )
            for (time in mealTimes) {
                val cal = calendar.clone() as Calendar
                cal.set(Calendar.HOUR_OF_DAY, time.first)
                cal.set(Calendar.MINUTE, time.second)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                mealDao.insert(
                    MealLogEntity(
                        timestamp = cal.timeInMillis,
                        type = time.third,
                        isLateNight = false,
                        viaDeliveryApp = false,
                        source = "MANUAL",
                        mealDate = yyyyMMdd,
                        recordedTime = String.format("%02d:%02d", time.first, time.second),
                        inputMethod = "MANUAL"
                    )
                )
            }

            if (dayOffset % 3 == 0) {
                val cal = calendar.clone() as Calendar
                cal.set(Calendar.HOUR_OF_DAY, 22)
                cal.set(Calendar.MINUTE, 30)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                mealDao.insert(
                    MealLogEntity(
                        timestamp = cal.timeInMillis,
                        type = MealType.LATE_NIGHT,
                        isLateNight = true,
                        viaDeliveryApp = true,
                        source = "MANUAL",
                        mealDate = yyyyMMdd,
                        recordedTime = "22:30",
                        inputMethod = "MANUAL"
                    )
                )
            }

            // 3. 스트레칭 데이터 시딩: 하루 3회 (아침 09:30, 점심 13:30, 저녁 17:30)
            val stretchSlots = listOf(
                Pair("아침", "09:30:00"),
                Pair("점심", "13:30:00"),
                Pair("저녁", "17:30:00")
            )
            for (slot in stretchSlots) {
                stretchDao.insert(
                    StretchingRecord(
                        date = yyyyMMdd,
                        timeSlot = slot.first,
                        createdAt = "$yyyyMMdd ${slot.second}"
                    )
                )
            }

            // 4. 디지털 사용 데이터 시딩: 매일 YouTube 60분 (21:00 ~ 22:00)
            val calStart = calendar.clone() as Calendar
            calStart.set(Calendar.HOUR_OF_DAY, 21)
            calStart.set(Calendar.MINUTE, 0)
            calStart.set(Calendar.SECOND, 0)
            calStart.set(Calendar.MILLISECOND, 0)

            val calEnd = calendar.clone() as Calendar
            calEnd.set(Calendar.HOUR_OF_DAY, 22)
            calEnd.set(Calendar.MINUTE, 0)
            calEnd.set(Calendar.SECOND, 0)
            calEnd.set(Calendar.MILLISECOND, 0)

            digitalSessionDao.insert(
                DigitalSessionEntity(
                    appPackage = "com.google.android.youtube",
                    startTime = calStart.timeInMillis,
                    endTime = calEnd.timeInMillis,
                    durationMinutes = 60
                )
            )

            // 5. 디지털 개입 로그 시딩: 3일에 한 번씩 알림 및 반응 기록 (HUMOR 반응률 높음)
            if (dayOffset % 3 == 0) {
                digitalInterventionLogDao.insert(
                    DigitalInterventionLogEntity(
                        appPackage = "com.google.android.youtube",
                        triggerDurationMinutes = 30,
                        timestamp = calStart.timeInMillis + 15 * 60 * 1000L,
                        reacted = true,
                        messageTone = "HUMOR",
                        actionType = "EXIT"
                    )
                )
                digitalInterventionLogDao.insert(
                    DigitalInterventionLogEntity(
                        appPackage = "com.google.android.youtube",
                        triggerDurationMinutes = 30,
                        timestamp = calStart.timeInMillis + 45 * 60 * 1000L,
                        reacted = false,
                        messageTone = "EMPATHY",
                        actionType = "IGNORED"
                    )
                )
            }
        }
    }

    /**
     * 어제(6.4) 기준 14일치 [불규칙적인 페르소나 "박지우"] 데이터를 삽입합니다.
     */
    suspend fun seedIrregularPersonaData() {
        val calendar = Calendar.getInstance()
        val yesterdayMs = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // 어제
        val oneDayMs = 24 * 60 * 60 * 1000L

        // 14일 전부터 어제까지 데이터를 채워넣음
        for (dayOffset in 0..13) {
            calendar.timeInMillis = yesterdayMs - (dayOffset * oneDayMs)
            val yyyyMMdd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            // 1. 물 데이터 시딩: 매일 5~7회 불규칙 섭취. 분 단위 오프셋(-25분 ~ +25분) 적용
            val waterCount = if (dayOffset % 2 == 0) 6 else 7
            val baseHours = listOf(9, 11, 14, 16, 18, 20, 21)
            for (i in 0 until waterCount.coerceAtMost(baseHours.size)) {
                val hour = baseHours[i]
                val minuteOffset = (dayOffset * 13 + i * 7) % 50 - 25 // -25분 ~ +25분
                val amount = 150 + ((dayOffset * 31 + i * 17) % 4) * 50 // 150ml ~ 300ml

                val cal = calendar.clone() as Calendar
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, 0)
                cal.add(Calendar.MINUTE, minuteOffset)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                waterDao.insert(
                    WaterLogEntity(
                        timestamp = cal.timeInMillis,
                        amountMl = amount,
                        source = "MANUAL"
                    )
                )
            }

            // 2. 식사 데이터 시딩:
            // 아침(BREAKFAST): 3일에 한 번 결식 (dayOffset % 3 != 0 일 때만 먹음). 시각은 07:50 ~ 08:30 사이
            if (dayOffset % 3 != 0) {
                val cal = calendar.clone() as Calendar
                cal.set(Calendar.HOUR_OF_DAY, 8)
                val minOffset = (dayOffset * 17) % 40 - 20
                cal.set(Calendar.MINUTE, 0)
                cal.add(Calendar.MINUTE, minOffset)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                mealDao.insert(
                    MealLogEntity(
                        timestamp = cal.timeInMillis,
                        type = MealType.BREAKFAST,
                        isLateNight = false,
                        viaDeliveryApp = false,
                        source = "MANUAL",
                        mealDate = yyyyMMdd,
                        recordedTime = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)),
                        inputMethod = "MANUAL"
                    )
                )
            }

            // 점심(LUNCH): 매일 먹음. 시각은 12:15 ~ 13:05 사이
            val lunchCal = calendar.clone() as Calendar
            lunchCal.set(Calendar.HOUR_OF_DAY, 12)
            val lunchMinOffset = 15 + (dayOffset * 11) % 50
            lunchCal.set(Calendar.MINUTE, 0)
            lunchCal.add(Calendar.MINUTE, lunchMinOffset)
            lunchCal.set(Calendar.SECOND, 0)
            lunchCal.set(Calendar.MILLISECOND, 0)
            mealDao.insert(
                MealLogEntity(
                    timestamp = lunchCal.timeInMillis,
                    type = MealType.LUNCH,
                    isLateNight = false,
                    viaDeliveryApp = false,
                    source = "MANUAL",
                    mealDate = yyyyMMdd,
                    recordedTime = String.format("%02d:%02d", lunchCal.get(Calendar.HOUR_OF_DAY), lunchCal.get(Calendar.MINUTE)),
                    inputMethod = "MANUAL"
                )
            )

            // 저녁(DINNER): 매일 먹음. 시각은 18:40 ~ 19:40 사이
            val dinnerCal = calendar.clone() as Calendar
            dinnerCal.set(Calendar.HOUR_OF_DAY, 19)
            val dinnerMinOffset = (dayOffset * 23) % 60 - 20
            dinnerCal.set(Calendar.MINUTE, 0)
            dinnerCal.add(Calendar.MINUTE, dinnerMinOffset)
            dinnerCal.set(Calendar.SECOND, 0)
            dinnerCal.set(Calendar.MILLISECOND, 0)
            mealDao.insert(
                MealLogEntity(
                    timestamp = dinnerCal.timeInMillis,
                    type = MealType.DINNER,
                    isLateNight = false,
                    viaDeliveryApp = false,
                    source = "MANUAL",
                    mealDate = yyyyMMdd,
                    recordedTime = String.format("%02d:%02d", dinnerCal.get(Calendar.HOUR_OF_DAY), dinnerCal.get(Calendar.MINUTE)),
                    inputMethod = "MANUAL"
                )
            )

            // 야식(LATE_NIGHT): 4일에 한 번만 먹음 (dayOffset % 4 == 0). 시각은 23:00 ~ 23:45 사이
            if (dayOffset % 4 == 0) {
                val cal = calendar.clone() as Calendar
                cal.set(Calendar.HOUR_OF_DAY, 23)
                val minOffset = (dayOffset * 7) % 45
                cal.set(Calendar.MINUTE, 0)
                cal.add(Calendar.MINUTE, minOffset)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                mealDao.insert(
                    MealLogEntity(
                        timestamp = cal.timeInMillis,
                        type = MealType.LATE_NIGHT,
                        isLateNight = true,
                        viaDeliveryApp = true,
                        source = "MANUAL",
                        mealDate = yyyyMMdd,
                        recordedTime = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)),
                        inputMethod = "MANUAL"
                    )
                )
            }

            // 3. 스트레칭 데이터 시딩: 격일로 하루 2회(9시, 15시) 또는 4회(8시, 12시, 16시, 20시) 완료
            val stretchHours = if (dayOffset % 2 == 0) listOf(9, 15) else listOf(8, 12, 16, 20)
            for (hour in stretchHours) {
                val slot = when (hour) {
                    in 5..10 -> "아침"
                    in 11..16 -> "점심"
                    in 17..21 -> "저녁"
                    else -> "기타"
                }
                stretchDao.insert(
                    StretchingRecord(
                        date = yyyyMMdd,
                        timeSlot = slot,
                        createdAt = String.format("%s %02d:00:00", yyyyMMdd, hour)
                    )
                )
            }

            // 4. 디지털 사용 데이터 시딩: 유튜브를 하루 40분 ~ 90분 사용. 시작 시각 20:00 ~ 21:30 사이
            val startHour = 20
            val startMin = (dayOffset * 19) % 90
            val duration = 40 + (dayOffset * 11) % 50

            val calStart = calendar.clone() as Calendar
            calStart.set(Calendar.HOUR_OF_DAY, startHour)
            calStart.set(Calendar.MINUTE, 0)
            calStart.add(Calendar.MINUTE, startMin)
            calStart.set(Calendar.SECOND, 0)
            calStart.set(Calendar.MILLISECOND, 0)

            val calEnd = calStart.clone() as Calendar
            calEnd.add(Calendar.MINUTE, duration)

            digitalSessionDao.insert(
                DigitalSessionEntity(
                    appPackage = "com.google.android.youtube",
                    startTime = calStart.timeInMillis,
                    endTime = calEnd.timeInMillis,
                    durationMinutes = duration
                )
            )

            // 5. 디지털 개입 로그 시딩: 3일에 한 번씩 알림 및 반응 기록 (PRAISE 반응률 높음)
            if (dayOffset % 3 == 0) {
                digitalInterventionLogDao.insert(
                    DigitalInterventionLogEntity(
                        appPackage = "com.google.android.youtube",
                        triggerDurationMinutes = 30,
                        timestamp = calStart.timeInMillis + 10 * 60 * 1000L,
                        reacted = true,
                        messageTone = "PRAISE",
                        actionType = "EXIT"
                    )
                )
                digitalInterventionLogDao.insert(
                    DigitalInterventionLogEntity(
                        appPackage = "com.google.android.youtube",
                        triggerDurationMinutes = 30,
                        timestamp = calStart.timeInMillis + 30 * 60 * 1000L,
                        reacted = false,
                        messageTone = "HUMOR",
                        actionType = "IGNORED"
                    )
                )
            }
        }
    }
}
