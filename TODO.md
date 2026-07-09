# Motivation Locker 개선 작업 목록

> 기준일: 2026-07-09
> 제외 항목: 공유 기능, AdMob 캐시 절감 (별도 논의)

## 🚀 1단계: 먼저 진행 (코드 작성 완료, 적용 대기)

### 1. 의존성 업데이트
- [x] Kotlin 1.8.20 → 1.9.24, jcenter() → mavenCentral()
- [x] appcompat 1.7.0, core-ktx 1.13.1, preference-ktx 1.2.1, constraintlayout 2.1.4
- [x] play-services-ads 23.6.0, review 2.0.2, 테스트 라이브러리 최신화
- [x] 중복 kotlin 플러그인 제거, buildToolsVersion 제거, kotlinOptions jvmTarget 17 추가
- [x] `build.gradle` / `app/build.gradle`에 직접 반영 (별도 `changes.patch` 파일은 존재하지 않아 수동 적용)
- [x] Gradle sync + clean build 확인 (`./gradlew clean assembleDebug`, `./gradlew test` 모두 BUILD SUCCESSFUL)

### 2. QuoteRepository 분리
- [x] `QuoteRepository.kt` 신규 — JSON 로딩/파싱/캐시를 한 곳으로
- [x] `Quote` data class 도입, `MotivationLockerActivity`의 언어별 중복 코드 제거
- [ ] 실기기에서 잠금화면 명언 표시 정상 동작 확인 (영어/한국어 전환 포함) — 이 세션에서는 대상 기기가 없어 미수행
- 위젯·데일리 알림·언어 추가의 공통 토대

---

## 🔧 버그 수정

### 3. 키가드 해제 로직 수정 (우선)
- [x] `KeyguardManager.isKeyguardSecure()` 분기 적용 (`MotivationLockerActivity.onCreate`)
  - 잠금 없음: 기존처럼 즉시 해제 (스와이프 → 홈)
  - 핀/패턴/비밀번호: 즉시 해제 요청 제거, 스와이프 → `finish()` → 시스템 잠금화면 자연 진입
- [ ] 실기기(핀 설정) 테스트 — 이 세션에서는 대상 기기가 없어 미수행

### 4. 전면광고 ID 확인
- `AdManager`가 `TEST_ADMOB_FULLSCREEN_ID` 문자열 리소스 사용 중
- 실제 광고 단위 ID인지 확인 — 테스트 ID라면 수익 미발생 상태
- 사용자 요청으로 이번 작업 범위에서 제외됨 (별도 진행 필요)

---

## ✨ 신규 기능 (우선순위 순)

### 5. 데일리 알림
- [x] `androidx.work:work-runtime-ktx` 의존성 추가
- [x] `QuoteNotificationWorker` — `QuoteRepository.getRandomQuote()` 재사용, `BigTextStyle`로 명언 전문 표시, 전용 알림 채널(IMPORTANCE_DEFAULT) 생성
- [x] `DailyNotificationScheduler` — WorkManager `PeriodicWorkRequest`(24시간 주기) + `initialDelay`로 다음 오전 10시 계산
- [x] `pref.xml`에 `dailyNotificationEnabled` 스위치 추가 (기본 꺼짐), 켤 때만 Android 13+ `POST_NOTIFICATIONS` 런타임 권한 요청(맥락 있는 요청), 거부 시 스위치 원복
- [x] 기존 `MainActivity.checkPermission()`의 무조건적 `POST_NOTIFICATIONS` 요청 제거 (데일리 알림 스위치로 통합)
- 시간 선택 UI는 만들지 않음 — 매일 오전 10시 고정 (사용자 확정 사항)
- [ ] 실기기에서 오전 10시 알림 실제 수신 확인 — 이 세션에서는 대상 기기가 없어 미수행

### 6. 언어 추가
- [x] 코드 인프라 준비 완료 — `QuoteRepository`가 `parseQuotes(json)`을 별도 순수 함수로 분리해 asset 없이도 테스트 가능(`QuoteRepositoryTest`), 개별 항목이 깨져 있어도 해당 항목만 건너뛰고 전체 언어 로딩은 실패하지 않음, asset 자체가 없거나 못 열리면 기본 언어(영어)로 자동 폴백
- [ ] **실제 콘텐츠는 미착수** — 언어권 원어 명언은 LLM이 임의로 생성하면 실존 인물 오귀속 위험이 있어 별도로 데이터를 수집/검증해야 함(직접 준비하거나 다음 세션에서 출처와 함께 진행)
- 순서: 일본어 → 스페인어 → 인도네시아어 → 포르투갈어(브라질)
- 번역이 아닌 각 언어권 원어 명언 수집 (품질)
- 작업: assets에 JSON 추가 → `QuoteRepository.LANGUAGE_FILES`에 새 인덱스로 항목 추가(기존 인덱스 재배치 금지 — `language` 값이 SharedPreferences에 정수로 영속됨) → `pref.xml`/`strings.xml`의 `languageCategory` 배열 끝에 추가
- JSON 형식: `[{ "id": 1, "quote": "...", "writer": "- ..." }]`
- 언어 추가 시 스토어 페이지도 해당 언어로 현지화 등록

### 7. 명언 수 확대
- 현재: 한국어 241개 / 영어 236개
- Quotable API 등 오픈 데이터셋 활용 (퍼블릭 도메인/CC 라이선스 확인)
- 목표: 언어당 1,000개 이상
- [ ] **미착수** — 6번과 동일한 이유로 실제 데이터 수집은 이번 세션 범위 밖. `QuoteRepository.parseQuotes`가 대량 데이터의 개별 오류를 허용하도록 이미 대비되어 있어, 데이터만 준비되면 asset 교체만으로 반영 가능

### 8. 배경 커스텀
- 그라데이션 프리셋: 기존 20색 2개 조합 → `GradientDrawable`, 15~20개 프리셋
- 사용자 사진: Photo Picker(`PickVisualMedia`) — 권한 불필요
- 사진 위 반투명 스크림 오버레이로 텍스트 가독성 확보
- URI는 SharedPreferences에 저장 (persistable URI permission 필요)

### 9. 홈화면 위젯
- 클래식 `AppWidgetProvider` + RemoteViews (Compose 미사용 프로젝트에 적합)
- 크기 2종: 2x2 카드, 4x2 가로
- WorkManager로 주기 갱신, 탭 시 앱 실행
- `QuoteRepository` 재사용

---

## 📈 스토어 개선 (코드 외)

### 10. ASO
- [ ] 앱 이름에 검색 키워드: "명언 잠금화면 - 동기부여 문구, Motivation Quotes" 형태
- [ ] 스토어 설명 확충 — "잠금화면 명언", "동기부여 앱", "daily quotes" 등 자연스럽게 포함
- [ ] 스크린샷에 텍스트 캡션 ("매일 새로운 명언", "20가지 컬러 테마")
- [ ] 카테고리 재검토: Education → Lifestyle 또는 Personalization
- [ ] 언어별 스토어 페이지 현지화 (6번과 연동)

---

## 권장 작업 순서

```
[1단계] 의존성 업데이트 + QuoteRepository 분리 ← 먼저 진행
[2단계] 키가드 수정 → 데일리 알림
[3단계] 언어 추가 + 명언 확대 (+스토어 현지화)
[4단계] 배경 커스텀 → 위젯
※ ASO(스토어 개선)는 전 단계와 병행
```

## 빌드 확인 사항
- 의존성 업데이트 후 Android Studio에서 clean build 1회 필수
- appcompat 1.7.0은 compileSdk 34+ 요구 — 현재 35라 문제없음
- `MotivationLockerActivity`에서 JSONArray/JSONObject/Random import 제거됨 — 컴파일 확인
