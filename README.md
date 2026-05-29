# ☘️ 해빗프렌즈 (Habit Friends)

> **"나의 습관을 함께 가꿀 아바타와 함께, 건강한 라이프스타일을 만들어보세요!"**  
> **해빗프렌즈**는 아바타 육성 요소를 결합한 모던 안드로이드 습관 추적(Habit Tracking) 애플리케이션입니다. 

---

## 📌 주요 핵심 기능

### 1. 👤 아바타 기반 건강 상태 (Avatar-based Health Status)
* **실시간 아바타 변화**: 4대 카테고리(식사, 물, 디지털, 스트레칭)의 당일 습관 달성률에 따라 아바타의 건강도 상태가 시각적으로 변합니다.
* **아바타 성향 및 규칙성 분석**: 3일 연속 특정 끼니를 거르면 아바타 건강 상태가 악화되고, 규칙적으로 다 챙기면 최상 상태가 되는 등 사용자의 습관 규칙성에 직관적으로 반응합니다.

### 2. 🎯 4대 습관 트래킹 (Habit Categories)
* **💧 물 (Water)**:
  * 하루 목표 섭취량 달성도 관리 및 이력 데이터 관리.
  * 원터치 기록 버튼(250ml / 500ml 추가) 제공.
* **🍚 식사 & 야식 (Meal & Late Night)**:
  * 아침, 점심, 저녁 식사 여부를 간편하게 체크.
  * **배달앱 자동 감지 개입**: 밤 10시 이후 배달앱이 실행될 경우, 야식 자제를 유도하기 위해 스트레칭이나 물 마시기를 권하는 팝업 개입(Intervention) 제공.
  * 야식 참기/먹기 여부를 판별하여 기록 보관.
* **🤸 스트레칭 (Stretch - 4대 슬롯 원터치)**:
  * 하루를 4개 시간대 슬롯(오전 10:30, 점심 후 14:00, 저녁 19:30, 취침 전 22:00)으로 세분화하여 관리.
  * 슬롯별 원터치 빠른 기록 기능 제공 (기본 부위 "전신"으로 원터치 저장).
  * 실수 기록을 방지하기 위한 **기록 취소 다이얼로그(예/아니오)** 팝업 및 Toast 알림 처리.
* **📱 디지털 (Digital)**:
  * Android `UsageStatsManager` API를 연동하여 기기 및 앱 사용 시간 추적.
  * 설정된 일일 사용 기준 시각 초과 시, 알림 및 강제 휴식 유도 화면으로 라우팅.

### 3. ⏰ 백그라운드 스케줄러 & 알림 (WorkManager & Push)
* **리마인더 푸시**: 각 습관별 리마인더 푸시 전송 (`WaterReminderWorker`, `MealReminderWorker` 등).
* **스트레칭 슬롯 알림**: 슬롯별 알림 켜기/끄기 설정값에 맞춰 실시간으로 알림을 발행하며, 어제 하루 동안 스트레칭을 1회도 실시하지 않았을 경우 오늘 첫 슬롯에 경고를 더해 푸시를 보냅니다.
* **야식 자제 성공 칭찬**: 어젯밤 야식을 참는 데 성공했을 경우, 다음 날 아침 8시에 사용자 칭찬 푸시 알림을 발송합니다.
* **디바이스 사용 감시**: 15분 주기 주기적 모니터링 및 개입 알림 (`DigitalUsageWorker`).
* **개인화 메시지 톤**: 사용자 성향 분석에 따라 알림 톤(칭찬형, 유머형, 공감형, 도전형)을 동적으로 맞춤 제공 (`PersonalizationWorker`).
* **하루 마무리 요약**: 밤 22:00에 오늘 하루 전체 습관의 통계를 요약하여 분석 (`DailySummaryWorker`).

### 4. 📊 대시보드 및 리포트 (Dashboard & Reports)
* 메인 대시보드를 통해 오늘의 4대 습관 달성률을 한눈에 시각적으로 확인.
* 주간/월간 달성률 통계 차트, 연속 달성 스트릭(Streak), 최우수 요일 하이라이트 정보 및 개선 팁 제공.

### 5. 🖼️ 홈 화면 위젯 (App Widget)
* 안드로이드 홈 화면에서 앱을 직접 켜지 않고도 당일 습관 달성도 및 아바타 상태를 실시간 확인하는 위젯 제공.

---

## 🛠️ 기술 스택 (Tech Stack)

| 구분 | 기술 / 라이브러리 | 설명 |
| :--- | :--- | :--- |
| **UI** | **Jetpack Compose** | 선언형 UI 프레임워크 기반 모던 화면 구현 |
| **Architecture** | **Clean Architecture + MVVM** | UI ↔ Domain ↔ Data 레이어 분리 및 단방향 데이터 흐름(UDF) 설계 |
| **DI** | **Dagger Hilt** | 의존성 주입을 통한 모듈 결합도 완화 및 테스트 용이성 확보 |
| **Local DB** | **Room Database** | SQLite 기반 습관 로그 로컬 저장 관리 |
| **Local Settings** | **DataStore Preferences** | 알림 시각, 주기 설정값 및 아바타 정보 로컬 저장 |
| **Background** | **WorkManager** | OS 백그라운드 스케줄링 및 리마인더 푸시 제어 |
| **Annotation** | **KSP (Kotlin Symbol Processing)** | Hilt 및 Room 코드 생성을 위한 컴파일 최적화 |

---

## 📂 프로젝트 패키지 구조 (Package Architecture)

```
com.example.habittracker
├── HabitTrackerApplication.kt  # Hilt 및 알림 채널 초기화
├── MainActivity.kt             # Compose NavHost 진입 및 딥링크 라우팅
├── BootReceiver.kt             # 디바이스 부팅 완료 시 알림 스케줄 재등록
│
├── di/                         # Hilt DI 모듈 (Database, DataStore, Repository 등)
│
├── data/                       # 데이터 레이어
│   ├── AppDatabase.kt          # Room 데이터베이스 선언 및 마이그레이션(MIGRATION_3_4)
│   ├── entity/                 # Room DB Entity (MealEntity, StretchingRecord 등)
│   ├── local/                  # Room DAO 및 DataStore Preference 매니저
│   ├── repository/             # domain repository 인터페이스 구현체
│   └── usage/                  # UsageStats API 연동 기기 사용 시간 측정 도구
│
├── domain/                     # 비즈니스 로직 레이어 (순수 코틀린 모듈)
│   ├── model/                  # 비즈니스 도메인 모델
│   ├── repository/             # repository 인터페이스
│   └── usecase/                # 기능별 유스케이스 (AddWaterLog, GetWeeklyReport 등)
│
├── ui/                         # UI 프레젠테이션 레이어 (MVVM)
│   ├── home/                   # 메인 대시보드 화면 및 뷰모델
│   ├── onboarding/             # 이름 및 캐릭터 설정 온보딩
│   ├── water/                  # 물 섭취 기록 화면
│   ├── meal/                   # 식사 / 야식 제어 및 배달앱 개입 팝업 화면
│   ├── stretch/                # 스트레칭 기록 화면 (슬롯 원터치 & 취소)
│   ├── digital/                # 사용량 관리 및 개입 화면
│   ├── reports/                # 주간/월간 리포트 분석 화면
│   ├── settings/               # 알림 주기 및 설정 화면
│   ├── components/             # 공통 UI 컴포저블 (ProgressIndicator 등)
│   └── theme/                  # Color, Type, Theme 정의
│
├── worker/                     # 백그라운드 리마인더 및 배달앱 감지 Worker
│
└── widget/                     # 안드로이드 홈화면 상태 위젯 관련 클래스
```

---

## 🚀 로컬 빌드 및 실행 방법

### 1. 전제 조건 (Prerequisites)
* Android Studio (버전 Ladybug / Koala 이상 권장)
* JDK 21 및 Android SDK API 36 호환 환경

### 2. 빌드
프로젝트 루트 디렉토리에서 아래 명령어로 디버그용 APK를 컴파일 및 빌드합니다:
```bash
./gradlew assembleDebug
```
* 빌드된 APK 경로: `app/build/outputs/apk/debug/app-debug.apk`

### 3. 에뮬레이터 또는 기기 실행
```bash
# 앱 설치 및 실행
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.habittracker/com.example.habittracker.MainActivity
```
* **디바이스 사용 시간 기능 연동 시 주의**: 앱 첫 진입 및 구동 시 안드로이드 시스템의 **'사용 정보 접근 권한(Usage Access Permission)'**을 허용해 주셔야 디지털 사용량 기능이 정상 동작합니다.

---

## 🛠️ 최근 리팩토링 및 코드 최적화 사항

1. **SharedAvatarViewModel 비동기 블로킹 버그 수정**
   * `init` 블록에서 `combine(...).collect`로 인해 발생했던 자식 코루틴 블로킹 문제를 해결하기 위해, 각 수집(Collect) 로직을 독립적인 `viewModelScope.launch`로 분리하여 병렬 동작을 확보했습니다.
2. **StretchViewModel 슬림화**
   * UI가 시간 슬롯 원터치 기록 및 취소 팝업 방식으로 고도화됨에 따라 사용되지 않게 된 미사용 메서드(`onStretchButtonClick`, `onStretchSlotClick`, `onCancelStretchSlotClick`, `deleteStretchRecord`)를 완전 삭제하였습니다.
   * `AddStretchLogUseCase`를 거치지 않게 변경됨에 따라 의존성을 제거하고 `NotificationHelper`를 직접 주입받게 리팩토링했습니다.
3. **비즈니스 로직(50% 달성 알림) 안전 이관**
   * 미사용 UseCase를 삭제하면서 유실될 뻔했던 **"오늘의 스트레칭 50% 이상 달성 시 축하 푸시 알림 발송"** 로직을 `StretchViewModel.addStretchRecord` 내부로 정상 이관하였습니다.
4. **Repository 미사용 API 정리**
   * `StretchRepository` 인터페이스와 `StretchRepositoryImpl` 구현체에서 더 이상 쓰이지 않는 `addLog`, `updateLog`, `deleteLog` 메서드를 지워 클래스를 경량화했습니다.

---

## 🗄️ 데이터베이스 스키마 변경 이력

* **`MealType.LATE_NIGHT` 추가**: 삼시세끼와 야식을 식사 카테고리 내에서 통합 관리.
* **`stretching_records` 테이블 신규 구축**: 기존의 구조가 불명확했던 `stretch_logs` 테이블을 삭제하고, 신규 스트레칭 스키마에 맞춘 `stretching_records` 테이블로 대체 및 마이그레이션(MIGRATION_3_4) 처리.
  * `id`: 자동 증가 Primary Key
  * `date`: 기록이 귀속되는 날짜 (`YYYY-MM-DD`)
  * `time_slot`: 스트레칭을 수행한 시간대 슬롯 (`'아침'`, `'점심'`, `'저녁'`, `'기타'`)
  * `body_parts`: 스트레칭을 실시한 신체 부위 목록 (JSON Array)
  * `created_at`: 데이터 생성 시간 기록 (`YYYY-MM-DD HH:MM:SS`)
