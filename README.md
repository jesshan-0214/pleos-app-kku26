# pleos-app-kku26
건국대 현대자동차 차량용 앱 개발 공모전 팀



1. 앱 한 줄 소개
앱을 한 문장으로 소개해 주세요.
 

2. 기획 배경
어떤 고객의 니즈나 불편한 순간에 주목했으며, 왜 해결이 필요한지 작성해 주세요.
 

3. 앱 주요 기능 및 차량 특화 요소
앱의 핵심 기능을 소개하고, 이를 통해 어떤 문제를 해결하며 차량에서 어떤 새로운 경험을 제공하는지 설명해 주세요.
 

4. 주요 사용 시나리오
누가, 어떤 상황에서, 어떻게 앱을 사용하는지 구체적인 이용 흐름을 설명해 주세요.
 

5. 개발 계획
Pleos Connect 환경에서 앱을 어떻게 구현할지 주요 개발 범위와 SDK/API 사용계획을 구체적으로 작성해 주세요.
 

6. 기대효과 및 발전 가능성
사용자에게 제공하는 가치와 향후 서비스 확대·발전 가능성을 작성해 주세요. (타깃 고객 분석, 예상 MAU, Retention Rate, 향후 서비스 확대 계획 등)
 

7. 참고자료(선택 · 가산점 부여)
앱 화면 시안, 시연 영상, GitHub 저장소 등 아이디어와 개발 결과물을 보여줄 수 있는 자료가 있다면 파일 또는 링크로 제출해 주세요  
[개발관련](https://docs.google.com/document/d/118wVqMwQNtzuYLvIzHT3PGuMJpahQeUbsrJBTbR_Guc/edit?tab=t.0)  
[아이디어 뭉치기](https://docs.google.com/document/d/1r60zudzlV0Rg4t6QkVQesgZZjVxwTwaxOSL0PQepnKY/edit?tab=t.hsrt1ja3mxvd)  
[통계정리](https://docs.google.com/spreadsheets/d/1JDVZkEcHzK12YYo_r9_SBTqWCEnqwf36G2_JAiGvNM0/edit?gid=2069014705#gid=2069014705)

---

# 라이드메이트 (FamilyJourney) — 데모 앱

함께 탄 사람 기준으로 여정을 짜고, 그 여정을 퀘스트처럼 즐기는 차량용 동승자 앱.
시연 시나리오: **한 가족, 한 여정** — 아빠·엄마·지우(8)·서준(4), 서울 → 강릉 경포해변 (어린이 모드).

## 실행

**바로 설치:** `apk/ridemate-demo-release.apk` (release 빌드, 서명 완료, 약 20MB)

```bash
adb install -r apk/ridemate-demo-release.apk
```

**직접 빌드** (Android Studio 내장 JDK 사용). 빌드 후 APK를 `apk/`로 복사한다:

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew assembleRelease
cp app/build/outputs/apk/release/app-release.apk apk/ridemate-demo-release.apk
```

release는 데모 제출용으로 디버그 키로 서명한다 (스토어 배포 시 전용 키로 교체). `local.properties`는 커밋하지 않는다 (`sdk.dir`만 들어 있음).

## 시연 순서

| 단계 | 화면 | 하는 일 |
|---|---|---|
| 메인 | 4단계 카드 | 오늘의 여정 · 배지 도감 요약 |
| 1. 출발 | 탑승자 설정 → 맞춤 일정 | [지난번 탑승자 불러오기] → [맞춤 일정 만들기] → [내비에 경로 보내기] |
| 2. 이동 중 | 퀘스트 | "노래 약 n곡" 안내, 지역이 바뀌면 그 지역 퀴즈. 주행 중엔 화면 터치 잠금 → 폰(QR)으로 답 |
| 3. 경유지 | 들를까 투표 | 양떼목장 투표(과반이면 경유지 추가) · 점심 메뉴 익명 투표 → 식당 → 경유지 |
| 4. 도착 | 보상 | 도착하면 자동 이동 · 강릉 배지 · 구성원별 체크리스트 음성 안내 |

하단 바의 **[▶ 주행] [+10분] [도착] [처음부터]** 로 2시간 30분 여행을 몇 분 안에 빨리 감는다 (데모 모드에서만 보임).
음성은 소리와 함께 하단 🔊 자막으로도 나온다.

## 구조 (6. demo tree)

```
app/src/main/java/com/pleos/familyjourney/
├── FamilyJourneyApp.kt        # 데모 ↔ 실제 구현을 바꾸는 유일한 곳
├── MainActivity.kt
├── navigation/                # Routes(시연 순서), AppNavGraph(도착 시 자동 이동)
├── data/
│   ├── Models.kt              # 데이터 형식
│   ├── JourneyRepository.kt   # 화면이 데이터를 받는 창구 (인터페이스)
│   └── DemoJourneyRepository.kt  # 하드코딩 데이터 전부 + 폰 투표/답 흉내
├── pleos/                     # Pleos SDK 래퍼 (지금은 가짜 구현)
│   ├── NaviClient.kt          # NaviClient 인터페이스 + FakeNaviClient(빨리감기 가능)
│   └── TtsClient.kt           # TtsClient 인터페이스 + DemoTtsClient(Android 기본 TTS)
├── ui/                        # 테마, 공통 화면 틀(ScreenFrame), QR·배지·카드
└── feature/
    ├── home/                  # 메인메뉴
    ├── departure/             # 탑승자 설정, 일정
    ├── quest/                 # 남은 시간 안내, 퀴즈 (+ 주행 중 터치 잠금)
    ├── vote/                  # 들를까 투표, 메뉴 투표
    └── arrival/               # 배지, 체크리스트
```

호출 방향: Screen → ViewModel → `JourneyRepository` / `NaviClient` / `TtsClient`. 화면은 SDK를 직접 부르지 않는다.

## 실제 구현으로 바꿀 때

`FamilyJourneyApp.onCreate` 의 세 줄만 바꾼다: `FakeNaviClient` → NaviHelper 래퍼, `DemoTtsClient` → Pleos TextToSpeech 래퍼,
`DemoJourneyRepository` → TourAPI·Gleo LLM·카카오 로컬·Firebase·Room 구현. 화면 코드는 바뀌지 않는다.
아래는 실제 래퍼를 만들 때 참고할 SDK 확인 결과다.

## SDK를 직접 열어 보고 확인한 점 (기획서와 다른 부분)

- **남은 시간은 `getRouteStateInfo`에 없다.** `RouteStateInfo`에는 경로 상태·충전 정보뿐이고,
  남은 시간/거리는 리스너 `onDrivingInfo(DrivingInfo)`의 `destination.duration / distance`로 들어온다.
- **경유지는 최대 2개** (`WaypointIndex.FIRST / SECOND`). 휴게소 + 투표 경유지 + 식당을 한꺼번에 넣을 수 없어서,
  지나간 경유지가 빠진 뒤에 추가하도록 데모를 짰다. 꽉 차면 안내 문구를 보여 준다.
- `RouteInfo`, `RequestWaypointInfo`는 `poiId`, `poiSubId`가 필수다.
- 도착 판정은 거리 계산 없이 `onDestinationArrived` 이벤트를 쓴다.
- 차속은 NaviHelper에 없다 → 실제 터치 잠금은 Vehicle SDK 연동이 필요 (지금은 가짜 내비 속도 사용).
- TextToSpeech SDK 3.3.1: `TextToSpeech(context, Mode)`, 권한 `pleos.car.permission.TTS_SERVICE`.
  `SERVER/HYBRID` 모드는 `registerApp(clientId, clientSecret)` 필요 → 처음엔 `ON_DEVICE` 권장.
- DrivingInfo의 `duration`, `distance` 단위는 초/미터로 가정했다. 실제 에뮬레이터 값으로 확인이 필요하다.

## 빌드 환경

Gradle 9.1.0 · AGP 8.13.0 · Kotlin 2.2.20 · compileSdk 36 / targetSdk 34 / minSdk 33 · Jetpack Compose.
Pleos SDK 의존성은 없다 (가짜 구현만 사용). 설치된 JDK가 25 (Android Studio JBR)라서 샘플 앱의 Gradle 8.10.2 대신 9.1.0을 쓴다.
