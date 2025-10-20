## 📄 Alfa Agent 사용자 매뉴얼 (로그 필터링 에이전트)

Alfa Agent는 지정된 로그 파일들을 주기적으로 모니터링하고, 설정된 키워드(필터 옵션)에 일치하는 로그 라인을 추출하여 사용자 정의 핸들러(`AlfaResultHandler`)로 전달하는 배치 기반 로그 필터링 시스템입니다.

**✅ 주요 특징: 증분 읽기 기능**
최근 업데이트로, Alfa Agent는 로그 파일을 처음부터 끝까지 다시 읽지 않고, 마지막으로 읽은 위치(바이트 오프셋)를 기록하여 다음 배치 실행 시 **새로 추가된 내용만** 읽어 처리합니다. 이는 대용량 로그 파일 처리 시 성능을 크게 향상시킵니다.

-----

### 1\. 사용 준비: 핸들러 구현 및 설정

Alfa Agent를 사용하려면 먼저 **결과 핸들러**를 구현하고 \*\*설정(`AlfaConfig`)\*\*을 정의해야 합니다.

#### 1.1. `AlfaResultHandler` 구현

필터링된 로그가 발견되었을 때 수행할 사용자 정의 로직을 정의합니다. `onLogFiltered` 메서드를 반드시 구현해야 합니다.

```java
import config.AlfaResultHandler;

public class MyLogHandler implements AlfaResultHandler {

    @Override
    public void onLogFiltered(String logLine, String keyword) {
        // 이 곳에 필터링된 로그를 처리하는 로직을 작성합니다.
        // 예: DB에 저장, 알림 전송, 콘솔 출력 등
        System.out.println("[알림] 키워드: " + keyword + " | 로그: " + logLine);
    }
    
    // onBatchComplete는 선택 사항입니다.
    // @Override
    // public void onBatchComplete(List<String> filteredLines) { ... }
}
```

#### 1.2. `AlfaConfig` 설정

로그 파일의 경로, 필터링 옵션, 실행 주기 등을 정의합니다.

```java
import config.AlfaConfig;
import java.util.*;

// 1. 결과 핸들러 생성
AlfaResultHandler myHandler = new MyLogHandler();

// 2. 관리할 로그 파일 경로 정의
Map<String, String> paths = new HashMap<>();
paths.put("APP_LOG", "/var/log/app.log");
paths.put("SYS_LOG", "/var/log/system.log");

// 3. 파일별 필터 옵션(키워드) 정의
Map<String, Set<String>> filters = new HashMap<>();
filters.put("APP_LOG", Set.of("ERROR", "FATAL")); // app.log에서 "ERROR" 또는 "FATAL" 검색
filters.put("SYS_LOG", Set.of("DENIED"));         // system.log에서 "DENIED" 검색

// 4. AlfaConfig 객체 생성 (핸들러, 경로, 필터 옵션 필수)
AlfaConfig config = new AlfaConfig(myHandler, paths, filters, null, null);

// 5. 선택적 설정
config.setBatchTime(10);        // 배치 실행 주기: 10초마다 (기본값: 20초)
config.setThreadPoolSize(5);    // 작업 스레드 풀 크기: 5개 (기본값: 10개)
```

-----

### 2\. 에이전트 실행 및 중지

`AlfaAgent` 클래스는 에이전트의 생성, 시작, 중지 인터페이스를 제공합니다.

#### 2.1. `AlfaAgent` 생성

생성자에 `AlfaConfig` 객체를 전달합니다.

```java
import agent.AlfaAgent;
// ... config 객체가 준비되었다고 가정
AlfaAgent agent = new AlfaAgent(config);
```

#### 2.2. 에이전트 시작

두 가지 시작 방법이 있습니다.

| 메서드 | 설명 |
| :--- | :--- |
| `agent.start()` | 에이전트를 **무기한** 실행합니다. `stop()` 메서드를 명시적으로 호출할 때까지 배치 처리를 계속합니다. |
| `agent.start(int durationInSeconds)` | 에이전트를 지정된 시간(**초 단위**) 동안 실행한 후, 스케줄러를 자동으로 종료합니다. |

**주의:** 실행 전, `PathHandler`를 통해 모든 설정 경로의 존재 여부를 검증합니다. 경로가 존재하지 않으면 `RuntimeException`이 발생하며 에이전트는 시작되지 않습니다.

#### 2.3. 에이전트 수동 중지

`start()`로 무기한 실행 중인 에이전트를 수동으로 중지할 때 사용합니다.

```java
agent.stop();
```
