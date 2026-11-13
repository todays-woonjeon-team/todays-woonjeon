# 제출용 APK

| 파일 | 내용 |
|---|---|
| `마그마를누가마그나_오늘의운전_release.apk` | 차량 앱(Pleos Connect Emulator, demo 빌드) |
| `마그마를누가마그나_오늘의운전_phone_release.apk` | 휴대폰 앱 |

만드는 방법(저장소 루트에서):

```bash
./gradlew :car-app:copySubmissionApk :phone-app:copySubmissionApk
```

- 팀 이름은 루트 `build.gradle`의 `teamName`, 차량 APK에 넣을 빌드(demo/vehicle)는 `submitFlavor`에서 바꿉니다.
- 휴대폰 연동까지 시연하려면 `local.properties`에 `firebase.dbUrl`을 넣고 빌드합니다.
- 파일이 커서 올릴 수 없으면 GitHub Releases에 올리고 README의 APK 위치 표에 링크를 적습니다.
