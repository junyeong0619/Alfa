import agent.AlfaAgent;
import config.AlfaConfig;
import config.AlfaResultHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {

    public static class MyTestHandler implements AlfaResultHandler {
        @Override
        public void onLogFiltered(String logLine, String keyword) {
            System.out.println(">> [AGENT] 키워드 감지 [" + keyword + "]: " + logLine);
        }

        @Override
        public void onError(String pathSymbol, Exception e) {
            System.err.println(">> [AGENT] 오류 발생 (" + pathSymbol + "): " + e.getMessage());
        }

    }

    public static void main(String[] args) {

        // 1. [중요] 로그 디렉토리 경로를 '절대 경로'로 명확하게 지정합니다.
        String logDir = "../alfa_test/test-logs/";
        int numFiles = 10;

        // 2. [핵심] 에이전트 시작 전에 로그 파일을 미리 생성합니다.
        // 이 로직 덕분에 PathHandler가 "파일이 없다"는 오류를 발생시키지 않습니다.
        try {
            System.out.println("지정된 경로에 로그 파일이 없으면 새로 생성합니다...");
            // 로그 디렉토리 생성
            Files.createDirectories(Paths.get(logDir));

            for (int i = 0; i < numFiles; i++) {
                Path logPath = Paths.get(logDir, "app-" + i + ".log");
                if (!Files.exists(logPath)) {
                    // 빈 파일 생성
                    Files.createFile(logPath);
                    System.out.println(" -> " + logPath.getFileName() + " 생성 완료.");
                }
            }
        } catch (IOException e) {
            System.err.println("오류: 로그 파일을 미리 생성할 수 없습니다. 경로를 확인해주세요: " + e.getMessage());
            // 파일 생성에 실패하면 에이전트를 시작하지 않고 종료합니다.
            return;
        }

        // 3. AlfaConfig 설정
        Map<String, String> paths = new HashMap<>();
        for (int i = 0; i < numFiles; i++) {
            paths.put("APP_" + i, Paths.get(logDir, "app-" + i + ".log").toString());
        }

        Map<String, Set<String>> filters = new HashMap<>();
        Set<String> keywords = Set.of("ERROR", "FATAL", "DENIED");
        for (int i = 0; i < numFiles; i++) {
            filters.put("APP_" + i, keywords);
        }

        AlfaConfig config = new AlfaConfig(new MyTestHandler(), paths, filters, 5, 10,null,null);
        AlfaAgent agent = new AlfaAgent(config);

        // 4. 에이전트 시작
        System.out.println("\nAlfa Agent 모니터링을 시작합니다. (대상: " + logDir + ")");
        agent.start(); // 무기한 실행
    }
}
