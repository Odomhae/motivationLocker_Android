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
- [x] 그라데이션 — ~~기존 20색 조합 16개 고정 프리셋~~ → 14번 항목에서 "선택한 배경색 기반 동적 계산"으로 재설계됨
- [x] 사용자 사진 — `ActivityResultContracts.PickVisualMedia()`(런타임 권한 불필요), `takePersistableUriPermission`으로 URI 영속화
- [x] 사진 위 반투명 스크림(`scrimView`, `activity_motivation_locker.xml`)으로 텍스트 가독성 확보
- [x] `backgroundType`(단색/그라데이션/사진)·`backgroundGradientPreset`·`backgroundPhotoUri`를 `"SETTINGS"` SharedPreferences에 저장, 기본값은 기존과 동일한 단색이라 기존 사용자 영향 없음
- [x] 사진 URI가 더 이상 유효하지 않으면(삭제됨 등) 크래시 대신 흰색 단색 배경으로 자동 대체(`MotivationLockerActivity.setPhotoBackground`)
- [ ] 실기기에서 그라데이션 렌더링/Photo Picker 실행/스크림 가독성 실제 확인 — 이 세션에서는 대상 기기가 없어 미수행

### 9. 홈화면 위젯
- 클래식 `AppWidgetProvider` + RemoteViews (Compose 미사용 프로젝트에 적합)
- 크기 2종: 2x2 카드, 4x2 가로
- WorkManager로 주기 갱신, 탭 시 앱 실행
- `QuoteRepository` 재사용
- 사용자 요청으로 이번 범위에서 제외됨 (별도 진행 필요)

### 11. 설정 화면 디자인 모더나이제이션 (TODO 원안에는 없던 추가 항목)
- [x] `com.google.android.material:material:1.12.0` 의존성 추가
- [x] `AppTheme` parent를 `Theme.AppCompat.Light.DarkActionBar` → `Theme.Material3.Light.NoActionBar`로 전환 (다크 모드는 추가하지 않음 — 사용자 확정 사항, `.DayNight.` 변형 아닌 `.Light.` 명시 사용)
- [x] 설정 화면 전용 Material 3 색상 팔레트 추가(`colors.xml`의 `settings*` 항목) — 배경/글자색 피커용 기존 20색과는 분리
- [x] `activity_main.xml`의 손으로 그린 회색 타이틀바+구분선을 `MaterialToolbar`로 교체, `preferenceContent`에 서피스 색상/여백 적용
- [x] `MainActivity`에서 중복되던 상태바 색상 하드코딩 정리, `setSupportActionBar(binding.toolbar)` 연결
- 범위는 설정 화면(`MainActivity`)로 한정 — 잠금화면(`MotivationLockerActivity`)은 배경/글자색을 사용자가 직접 고르는 구조라 테마 변경의 영향을 받지 않음, 색상/그라데이션 선택 다이얼로그(`ColorSelectorDialog`/`GradientSelectorDialog`)도 이번 범위 밖
- [ ] 실기기에서 툴바 색상/엘리베이션, 프리퍼런스 리스트 여백 등 실제 렌더링 확인 — 이 세션에서는 대상 기기가 없어 미수행

### 12. 색상/그라데이션 선택 다이얼로그 체크 표시 + 초기 기본값 버그 수정 (TODO 원안에는 없던 추가 항목)
- [x] `ColorSelectorDialogPreference`에 `onGetDefaultValue` 오버라이드 추가 — 이게 없어서 `textColorCategory`(기본값 검정)가 최초 실행 시 항상 흰색으로 잘못 초기화되던 버그 수정
- [x] 하드코딩된 hex 문자열 왕복(일부 색상이 `colors.xml`과 불일치했음, 예 `colorRed`)을 제거하고 `ContextCompat.getColor()`로 직접 리소스 색상 해석하도록 변경. 20색 전체에 대해 정확한 역매핑(현재 색 → 팔레트 인덱스) 가능해짐 — 기존엔 5색만 매핑되어 있어 나머지는 항상 흰색으로 오판정되던 버그도 같이 해결됨
- [x] `ColorSelectorDialog`에 주석 처리돼 있던 선택 체크 아이콘(`ic_selected_white`/`ic_selected_black`) 로직 복원
- [x] `GradientSelectorDialog`/`GradientSelectorDialogPreference`에도 동일한 체크 표시 + `onGetDefaultValue` 적용(일관성)
- [ ] 실기기에서 체크 아이콘 실제 렌더링 확인 — 이 세션에서는 대상 기기가 없어 미수행

### 13. 뒤로가기 → 배너 광고 포함 종료 확인 다이얼로그 (TODO 원안에는 없던 추가 항목)
- [x] 기존 "2초 내 두 번 눌러야 종료"(Toast) 패턴 제거
- [x] 뒤로가기 1회 시 `AlertDialog`로 종료 확인(`exit_confirm_message` + 종료/취소 버튼) 표시, 다이얼로그 안에 미리 로드해 둔 배너 광고(`AdSize.BANNER`) 노출
- [x] 배너 광고는 `onStart()`에서 미리 `loadAd()`해 둬서 다이얼로그가 뜰 때 바로 보이도록 함(`exitAdView`), 여러 번 열어도 안전하도록 이전 부모에서 제거 후 재부착
- [x] 리뷰 요청(`reviewApp()`)은 실제 종료를 확정한 시점 한 곳으로 통일(기존엔 첫 번째 누름에서도 매번 시도하던 것 정리)
- [ ] 실기기에서 다이얼로그 내 배너 광고 실제 표시 확인 — 이 세션에서는 대상 기기가 없어 미수행

### 14. 사진 배경 왜곡/회전 수정 + 그라데이션을 선택 배경색 기반으로 재설계 (TODO 원안에는 없던 추가 항목)
- [x] `androidx.exifinterface:exifinterface:1.3.7` 추가 — `content://` URI에서도 EXIF `Orientation`을 안정적으로 읽기 위함
- [x] 사진 배경을 `View.background`(종횡비 무시하고 늘어남)가 아닌 `scaleType="centerCrop"` `ImageView`(`photoBackgroundView`)로 렌더링해 "늘어지는 현상" 수정
- [x] `MotivationLockerActivity.decodeOrientedBitmap()`으로 EXIF `Orientation`에 따라 비트맵을 회전/반전 보정 — "가로사진이 세로로 나오는 현상" 수정
- [x] `GradientPresets`를 고정 리소스-id 목록에서, 선택된 배경색(`backgroundColorCategory`)을 기준으로 HSV 공간에서 8가지 뚜렷이 다른 변형(밝게/어둡게/보색/인접색조/파스텔 등)을 그때그때 계산하는 `forBaseColor(baseColor)` 함수로 재설계 — "그라데이션이 배경색과 무관함" + "옵션들이 비슷해 구별 안 됨" 두 문제를 함께 해결
- [x] `GradientSelectorDialog`/`GradientSelectorDialogPreference`가 `"SETTINGS"`의 `backgroundColorCategory`를 직접 읽어 그라데이션 미리보기/선택 목록을 계산하도록 변경
- [ ] 실기기에서 사진 방향/크롭, 그라데이션 시각적 구별 실제 확인 — 이 세션에서는 대상 기기가 없어 미수행

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
