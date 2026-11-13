# 차량 신호 연결

## 1. 신호별 사용 API

| 신호 | 쓰는 곳 | API | 코드 | 상태 |
|---|---|---|---|---|
| 차속 | 급가속·급감속·급출발, 회전 판정, 거리 | AAOS `CarPropertyManager` `PERF_VEHICLE_SPEED`(m/s) | `CarPropertySignals` | 연결 완료 |
| 기어 | 주차 리포트 시점, 휴식 | AAOS `GEAR_SELECTION` | `CarPropertySignals` | 연결 완료 |
| 주행 제한 | 화면 가림 | `CarUxRestrictionsManager` | `CarPropertySignals` | 연결 완료 |
| 방향지시등 | 회전 시 사용, 미리 켜기 | Vehicle SDK `TurnSignal` | `PleosSignals` | 시그니처 확인 중 |
| 핸들 각도 | 회전·급회전 판정 | Vehicle SDK `SteeringWheel` | `PleosSignals` | 시그니처 확인 중 |
| 브레이크 페달 | 급브레이크 보완 | Vehicle SDK `Brake` | `PleosSignals` | 시그니처 확인 중 |
| 와이퍼 | 빗길 운전 | Vehicle SDK `Wiper` | `PleosSignals` | 선택 |
| 안전벨트 | 안전벨트 미션 | Vehicle SDK `Safety` | `PleosSignals` | 선택 |
| 경로 정보 | 장거리 미션 | NaviHelper SDK `getRouteStateInfo` 등 | - | 선택 |

주행 중에는 신호가 바뀔 때 오는 콜백만 받습니다. 주기적으로 값을 읽지 않아 차량 자원을 아낍니다.

앱 내부 기준: 속도 km/h, 핸들 각도는 **왼쪽 음수·오른쪽 양수**(도), 시각은 `System.currentTimeMillis()`.

## 2. Vehicle SDK 연결 방식

공식 문서와 샘플에서 확인한 것:

- 진입점: `Vehicle(context)`, `initialize()`, `release()`, `getTurnSignal()`, `getSteeringWheel()`, `getBrake()`, `getWiper()`, `getSafety()`
- `TurnSignal` 메서드: `checkTurnSignalStateCapabilities`, `getCurrentTurnSignalState`, `registerTurnSignalState`, `unregisterTurnSignalState`
- 리스너 이름: `TurnSignalStateListener`, `SteeringWheelAngleListener`, `BrakePedalStatusListener`, `WiperStateListener`, `SeatBeltBuckledStateListener`
- 샘플 앱의 리스너 형태: `onPositionUpdated(pos: Int?)`, `onFailed(exception: Exception)`. 등록과 해제에 같은 리스너 객체를 넘기고, `release()` 전에 해제한다.
- 지원 확인: `checkXXXCapabilities({ config, areaMap -> ... }, { e -> ... })`, `config.getAccessType()`이 `READ` 또는 `READ_WRITE`면 읽기 가능

리스너 콜백의 정확한 이름과 값 타입은 공개 문서에서 확인하지 못했습니다. 그래서 `PleosBridge`가 등록 메서드를 이름으로 찾고, 리스너를 `java.lang.reflect.Proxy`로 구현해 값을 받습니다. 연결할 때 실제 시그니처를 Logcat에 남깁니다.

```
I PleosBridge: 연결됨: public void ...TurnSignal.registerTurnSignalState(...TurnSignalStateListener)
I PleosBridge: 콜백 시그니처: public abstract void ...TurnSignalStateListener.onXxx(java.lang.Integer)
```

## 3. 확인 체크리스트(11/16 ~ 11/23)

- [ ] `vehicleDebug`로 실행해 Logcat `PleosBridge` 태그의 "연결됨", "콜백 시그니처" 줄을 모두 기록한다.
- [ ] "찾지 못했습니다"가 나온 신호는 함께 출력된 메서드 목록에서 맞는 이름을 찾아 `PleosSignals`의 메서드 이름을 고친다.
- [ ] Virtual Console에서 방향지시등을 왼쪽·오른쪽·끔·비상등으로 바꾸며 값이 `toTurnSignal()`에서 맞게 바뀌는지 본다(숫자면 0 끔, 1 오른쪽, 2 왼쪽으로 가정함).
- [ ] 핸들을 왼쪽으로 돌렸을 때 값이 음수인지 본다. 양수면 `STEERING_LEFT_IS_POSITIVE = true`로 바꾼다.
- [ ] 핸들 각도가 핸들 자체 각도인지 바퀴 각도인지 확인한다. 바퀴 각도면 `vehicle.steeringRatio=1`로 둔다.
- [ ] 브레이크·와이퍼 값이 숫자·enum 중 무엇인지 보고 `PleosBridge.toBoolean()`이 맞게 읽는지 본다.
- [ ] 각 API 문서의 Permission 항목과 `src/vehicle/AndroidManifest.xml`의 권한 이름을 맞춘다.
- [ ] 확인이 끝나면 `PleosSignals`를 타입이 있는 리스너 구현으로 바꾸고 `PleosBridge`를 지운다.

## 4. 에뮬레이터에서 속도·기어 넣기

```bash
adb shell cmd car_service inject-vhal-event 0x11400400 8      # 기어 D (P=4, R=2, N=1, D=8)
adb shell cmd car_service inject-vhal-event 0x11600207 0      # 정지
adb shell cmd car_service inject-vhal-event 0x11600207 2.8    # 약 10km/h
adb shell cmd car_service inject-vhal-event 0x11600207 8.3    # 약 30km/h
adb shell cmd car_service inject-vhal-event 0x11600207 5.0    # 1초 안에 30→18km/h: 급감속
adb shell cmd car_service inject-vhal-event 0x11400400 4      # 기어 P → 주차 리포트
```

- `PERF_VEHICLE_SPEED` = `0x11600207`(m/s), `GEAR_SELECTION` = `0x11400400`
- 명령 형식은 에뮬레이터 버전에 따라 다를 수 있습니다. `adb shell cmd car_service help`로 확인합니다.
- 신호가 15분 넘게 끊기면 시동이 꺼진 것으로 보고 휴식으로 처리합니다. 오래 시험할 때는 속도 값을 조금씩 바꿔 넣습니다.
