# TourAPI 코스 수집 스크립트

한국관광공사 TourAPI(KorService2)에서 전국 여행코스와 자연 관광지를 모아
`app/src/main/assets/course_catalog.json` (`CourseCatalog`) 을 만든다. 앱과 별개로 PC 에서 실행한다.

## 처음 한 번

1. [data.go.kr](https://www.data.go.kr) 에서 **한국관광공사_국문 관광정보 서비스_GW** 활용신청 → 인증키 발급
2. 설정 파일 만들기
   ```sh
   cp collector/collector.properties.example collector/collector.properties
   ```
3. `collector.properties` 의 `serviceKey=` 에 인증키 입력 (Encoding/Decoding 아무거나)

## 실행

```sh
./gradlew :collector:run                      # 이어서 수집 (없으면 새로 시작)
./gradlew :collector:run --args="--fresh"     # 처음부터 새 주기로
```

- 하루 호출 한도에 걸리면 진행 상태를 `collector/data/state.json` 에 저장하고 멈춘다.
  **다음 날 같은 명령을 다시 실행하면 이어서** 수집한다. 전국 첫 수집은 며칠 걸린다.
- 실행이 끝날 때마다 지금까지 완성된 코스로 카탈로그를 다시 쓴다 (중간 결과도 앱에서 쓸 수 있음).
- 수집을 끝내고 `refreshDays`(기본 30일)가 지나서 실행하면 새 주기를 시작한다.
  목록만 다시 받고, 수정일(`modifiedtime`)이 그대로인 코스는 상세를 다시 부르지 않고 재사용한다.

## 호출 수

| 단계 | 호출 |
|---|---|
| 코스 목록 | 전체 코스 수 ÷ pageSize |
| 풍경 지점 목록 | 자연 관광지 수 ÷ pageSize |
| 코스 하나 | 공통·소개·경유지 3번 + 처음 보는 경유지마다 1번 |

경유지 좌표는 풍경 지점 목록에 이미 있으면 그걸 쓰고, 한 번 받은 경유지는 다시 부르지 않는다.

## 파라미터 조정

`collector.properties` 에서 `courseList.<이름>=값`, `spotList.<이름>=값` 으로 목록 호출에 파라미터를 더하거나 덮어쓴다.

```properties
courseList.lDongRegnCd=51     # 코스를 특정 시도만
spotList.cat1=A01             # 풍경 지점 분류
spotList.cat2=A0101
```

코드 값은 TourAPI 의 `ldongCode2`(법정동), `categoryCode2`·`lclsSystmCode2`(분류) 로 확인한다.

## 오류

| 메시지 | 원인 |
|---|---|
| 오늘 호출 한도를 다 썼어요 | 일일 트래픽 초과 → 내일 다시 실행 |
| 인증 실패 / 오류 30 | 인증키 오타, 활용신청 미승인(승인까지 1~2시간 걸릴 수 있음) |
| 건너뜀 ... | 코스 하나만 실패. 다음 실행 때 다시 시도하고 3번 실패하면 이번 주기엔 뺀다 |
