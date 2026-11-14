# Scenic (시닉)

주행 상황에 어울리는 음악과 코스를 추천하고 주행의 순간을 영상으로 기록하는, 당신만의 드라이브 동반자

Pleos Connect 차량용 앱입니다. 저장소와 패키지 이름은 개발 코드명인 `aCARive`를 그대로 씁니다.

## 주요 기능

| 기능 | 내용 |
|---|---|
| 주행 상황 맞춤 플레이리스트 | 취향 기반 후보곡 100곡 안에서 속도·시간대·날씨·기온으로 곡을 골라 Spotify로 재생합니다. 현재 곡이 시작될 때 다음 곡을 미리 큐에 넣고, 스킵·완청 피드백으로 취향을 보정합니다. 고속 주행 중에는 격한 곡을 빼고, 추천 이유도 보여 줍니다. |
| 풍경 좋은 드라이브 코스 추천 | 한국관광공사 TourAPI 코스 데이터와 선호 풍경(바다·산·야경 등)으로 왕복 2~3시간 코스 5개를 추천합니다. 주행 중에는 코스 1개만 큰 카드로 제안하고, 고른 코스는 Pleos 내비게이션으로 바로 안내합니다. |
| 드라이브 기록 영상 | 차량 위치를 1초 간격으로 기록해 주행이 끝나면 지도 위에 경로가 그려지는 5초 영상(H.264 MP4)을 만듭니다. 날짜·거리·주행 시간이 표시되고, 영상 속 차량을 5종 중에서 고를 수 있습니다. |
| 주행 중 UX 제한 | 코스 탐색, 취향 설정, 차량 선택, 영상 재생은 정차 중에만 열리고 주행 중에는 최소한의 조작만 남깁니다. |

## 실행 방법

1. 저장소를 클론하고 `feature/integrate-spotify` 브랜치로 이동합니다.
   ```sh
   git clone https://github.com/jihi210/aCARive.git
   cd aCARive
   git checkout feature/integrate-spotify
   ```
2. Android Studio로 프로젝트를 엽니다.
3. 프로젝트 루트의 `local.properties`에 API 키를 넣습니다. (아래 [필요한 설정](#필요한-설정) 참고)
4. Pleos Connect 에뮬레이터를 켜고 `app` 구성을 실행합니다.
   ```sh
   ./gradlew :app:installDebug
   ```
5. 실제로 달리지 않고 확인하려면 **마이 > 데모 주행**에서 출발 지점을 고르세요. 가상 위치로 주행을 재현해 코스 진행, 주행 중 추천 카드, 음악 전환, 기록 영상까지 볼 수 있습니다.

## 테스트 환경 및 필요한 설정

### 테스트 환경

| 항목 | 버전 |
|---|---|
| Android Studio | Rabbit 2 (2026.2.2) |
| JDK (Gradle Daemon) | 25 |
| Android Gradle Plugin | 9.5.0 |
| Kotlin | 2.2.10 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 28 |

테스트 실행:
```sh
./gradlew :app:testDebugUnitTest          # 단위 테스트 (코스 추천, 주행 통계, 경로 단순화, 주행 제한)
./gradlew :app:connectedDebugAndroidTest  # 에뮬레이터 계측 테스트 (영상 생성, 즐겨찾기)
```

### 필요한 설정

#### 1. `local.properties` (git에 올라가지 않음)

```properties
SPOTIFY_CLIENT_ID=발급받은_Spotify_Client_ID
KMA_SERVICE_KEY=발급받은_기상청_인증키
```

| 키 | 발급처 | 없을 때 |
|---|---|---|
| `SPOTIFY_CLIENT_ID` | [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) | Spotify에 연결하지 않음 (음악 외 기능은 정상 동작) |
| `KMA_SERVICE_KEY` | [공공데이터포털](https://www.data.go.kr) > 기상청_단기예보 조회서비스 | 고정된 Mock 날씨(18°, 비) 사용 |

#### 2. Spotify 연동

Scenic는 곡을 직접 재생하지 않고, Spotify App Remote SDK로 차량에 설치된 Spotify 앱을 원격 조종합니다.

1. Spotify Developer Dashboard에서 앱을 만들고 아래를 등록합니다.
    - Redirect URI: `acarive://spotify-auth`
    - Android Package name: `com.example.acarive`
    - SHA1 fingerprint: 빌드 키스토어의 SHA1 (`./gradlew signingReport`로 확인)
2. 에뮬레이터에 **Spotify 앱을 설치하고 Premium 계정으로 로그인**합니다.
3. 앱의 **마이 > Spotify 연결**을 눌러 권한을 허용합니다.

> Spotify App Remote SDK(`app/libs/spotify-app-remote-release-0.8.0.aar`)는 저장소에 포함돼 있습니다.

#### 3. 코스 데이터 다시 수집

코스 데이터(`app/src/main/assets/course_catalog.json`)는 이미 포함돼 있습니다. 새로 모으려면 [`collector/README.md`](collector/README.md)를 참고하세요. (한국관광공사 TourAPI 인증키 필요)

## 개발 환경 (Pleos Connect 에뮬레이터)

| 항목 | 값 |
|---|---|
| 에뮬레이터 버전 | Pleos Connect Emulator v2.1.0 (AVD: `Pleos_Connect_v2.1.0`) |
| 시스템 이미지 | Pleos Connect v2.1.0 APIs Intel x86_64 Atom System Image (Android 14, API 34, Google APIs) |
| 화면 크기 | 12.8″ |
| 화면 해상도 | 2560 × 1440 px (= 2560 × 1440 dp) |
| 밀도(dpi) | 160 dpi (mdpi, 1dp = 1px) |

- 화면은 **가로 840dp 창**을 기준으로 디자인했고, 창 너비가 1200dp 이상이면 2단 레이아웃으로 바뀝니다.
- 폭이 600dp 미만인 화면(폰 등)에서는 840dp 기준으로 축소해 같은 비율로 보여 줍니다.

### 사용 SDK / API

| 용도 | SDK / API |
|---|---|
| 차량 위치 수신 | Pleos FusedLocation SDK 1.1.3 |
| 차량 정보·주행 상태 | Pleos Vehicle SDK 2.3.2 |
| 내비게이션 연동 | Pleos NaviHelper 3.0.5 |
| 주행 중 UI 제한 | CarUxRestrictionsManager (`android.car`) |
| 음악 재생 | Spotify App Remote SDK 0.8.0, Spotify Auth 5.0.0 |
| 날씨 | 기상청 단기예보 조회서비스 |
| 코스 데이터 | 한국관광공사 TourAPI (KorService2) |
| 주행 기록 저장 | Room |
| 지도 배경 | MapLibre Android SDK 13.6.1 (OpenFreeMap positron) |
| 영상 생성·재생 | AndroidX Media3 1.8.0 (Transformer, Effect, ExoPlayer) |
| 설정 저장 | DataStore Preferences |

## APK

- **제출본**: `scenic_debug.apk` (공모전 제출 이메일에 첨부)
- **직접 빌드**: `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`

```sh
adb install scenic_debug.apk
```

## 프로젝트 구조

```
app/        차량용 Android 앱 (Jetpack Compose)
  ├─ ui/         화면 (홈, 코스, 기록, 저장, 마이, 주행)
  ├─ data/       코스 추천, 주행 기록(Room), 즐겨찾기, 프로필
  ├─ scenic/     음악 추천 엔진 (점수 계산, 큐 예약, 피드백)
  ├─ music/      Spotify 연결 관리
  ├─ navi/       Pleos 내비게이션 연동
  ├─ vehicle/    차량 정보, 주행 중 UX 제한
  ├─ car/        영상 속 차량 모델 5종
  └─ video/      주행 경로 영상 생성
collector/  TourAPI 코스 수집 스크립트 (PC에서 실행)
shared/     app·collector 공용 모델
design/     Figma Make 디자인 시안
```