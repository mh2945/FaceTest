# TestSample Project

## 프로젝트 개요
25-02-01 
CUBOX 사전 과제로 사용자에게 제공할 자동캡처 (.aar) SDK를 만드는 것이 1차 목표이며, 
기능 작동하는 Sample을 만든 뒤 aar로 내보내서 사용가능한 SDK를 만드는 것입니다.

+ 관련 aar을 실행시킬 수 있는 Sample App을 추가적으로 제작할 예정입니다.

25-02-02
Sample 기능 확인 후 camera-library output으로 aar 파일을 내보내기 완료.

## 프로젝트 참고
1. https://developer.android.com/reference 
2. https://stackoverflow.com/
3. Cusor AI
3. Google

## 프로젝트 구조
```
app/
├── manifests/
│   └── AndroidManifest.xml          # 권한 및 액티비티 설정
├── java/
│   └── com.example.testsample/
│       ├── MainActivity.kt          # 메인 화면 및 카메라 제어
│       ├── CircleView.kt           # 원형 프로그레스 뷰
│       ├── CameraActivity.kt       # 카메라 화면 처리
│       ├── ResultActivity.kt       # 결과 화면 처리
│       └── utils/                  # 유틸리티 클래스
└── res/
    ├── layout/
    │   ├── activity_main.xml       # 메인 화면 레이아웃
    │   ├── activity_camera.xml     # 카메라 화면 레이아웃
    │   ├── activity_result.xml     # 결과 화면 레이아웃
    │   └── circle_view.xml         # 원형 프로그레스 레이아웃
    ├── drawable/
    │   └── cubox_logo.xml         # CUBOX 로고
    ├── mipmap/
    │   ├── ic_launcher           # 앱 아이콘
    │   └── ic_launcher_round     # 라운드 앱 아이콘
    ├── values/
    │   ├── themes.xml            # 테마 설정
    │   ├── colors.xml            # 색상 정의
    │   └── strings.xml           # 문자열 리소스
    └── xml/
        ├── backup_rules.xml      # 백업 규칙
        └── data_extraction_rules.xml  # 데이터 추출 규칙
```

## 주요 기능
1. 카메라 제어
   - 전면 카메라 프리뷰
   - 얼굴 인식
   - 자동 촬영

2. 이미지 처리
   - 갤러리 저장
   - 이미지 회전 보정

3. UI/UX
   - 원형 프로그레스 바
   - 자동 촬영 타이머
   - 상태 피드백

## 핵심 클래스 설명
### MainActivity
- 카메라 초기화 및 제어
- 얼굴 인식 처리
- 이미지 저장 로직

### CircleView
- 커스텀 프로그레스 뷰
- 애니메이션 처리
- 상태 표시

## 기술 스택
- Language: Kotlin
- Min SDK: 23 (Android 6.0)
- Target SDK: 34
- Camera API: Android Camera API
- Storage: MediaStore API

## 권한 요구사항
- Camera
- Storage (Android 버전별 차등 적용)

## 빌드 및 실행
1. Android Studio 설정
   - Gradle JDK: Java 17
   - Gradle Plugin: 8.2.2
   - Kotlin Plugin: 1.9.0
   - Java 8 
    android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = '1.8'
    }
}

2. 필수 의존성
```gradle
dependencies {
    implementation "androidx.core:core-ktx:1.12.0"
    implementation "androidx.appcompat:appcompat:1.6.1"
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
}
```

## 주요 로직 흐름
1. 앱 실행
2. 권한 체크 및 요청
3. 카메라 초기화
4. 얼굴 인식 시작
5. 자동 촬영
6. 이미지 저장

## 개발 가이드
1. 카메라 관련 작업
   - Camera.open() 사용
   - SurfaceView 활용
   - FaceDetectionListener 구현

2. 이미지 처리
   - Matrix 활용한 회전 처리
   - MediaStore API 활용

3. UI 커스터마이징
   - CircleView 확장
   - 애니메이션 구현
   - 레이아웃 수정

## 문제 해결
- 카메라 초기화 실패: 권한 확인
- 얼굴 미인식: 조명, 얼굴 외 방해 조건 확인 
- 저장 실패: 저장소 권한 확인

## 참고사항
- 가로 모드 미지원
- 전면 카메라 필수 
- 기능 위주 Sample 메모리 관리 주의
