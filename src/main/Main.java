package main;

import main.agent.AlfaAgent;
import main.config.AlfaConfig;
import main.config.AlfaResultHandler;

import java.io.BufferedWriter; // 추가
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption; // 추가
import java.time.LocalDateTime; // 추가
import java.time.format.DateTimeFormatter; // 추가
import java.util.HashMap;
import java.util.Map;
import java.util.Random; // 추가
import java.util.Set;
import java.util.concurrent.ExecutorService; // 추가
import java.util.concurrent.Executors; // 추가

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

    // ========== LogGenerator (결합된 부분) ==========
    /**
     * AlfaAgent 테스트를 위한 다중 로그 파일 생성기.
     * LOG_DIR에 NUM_FILES 개수만큼 로그 파일을 생성합니다.
     */
    public static class LogGenerator {

        // 로그 파일명 접두사 (예: test-logs/app-0.log, test-logs/app-1.log ...)
        private static final String FILE_PREFIX = "app-";
        // 기본 쓰기 간격 (ms)
        private static final int WRITE_INTERVAL_MS = 2000;

        public static void start(int numFiles, String logDir) {
            System.out.println("Log Generator 시작...");
            System.out.println("로그 디렉토리: " + logDir);

            // 1. 로그 디렉토리 생성 (Main.main에서 이미 수행하지만, 확인차)
            Path logDirPath = Paths.get(logDir);
            try {
                if (!Files.exists(logDirPath)) {
                    Files.createDirectories(logDirPath);
                }
            } catch (IOException e) {
                System.err.println("LogGenerator: 로그 디렉토리 생성 실패: " + e.getMessage());
                return;
            }

            // 2. 파일 개수만큼 스레드 풀 생성
            ExecutorService executor = Executors.newFixedThreadPool(numFiles);

            // 3. 각 파일에 대해 LogWriterTask 실행
            for (int i = 0; i < numFiles; i++) {
                String filePath = Paths.get(logDir, FILE_PREFIX + i + ".log").toString();
                executor.submit(new LogWriterTask(filePath));
            }

            System.out.printf("%d개의 로그 파일에 쓰기를 시작합니다. (앱 종료 시 함께 종료됨)\n", numFiles);
        }

        /**
         * 개별 로그 파일에 주기적으로 로그를 쓰는 Runnable 태스크
         */
        static class LogWriterTask implements Runnable {
            private final String filePath;
            private final Random random = new Random();
            private final DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            private final String[] logLevels = {"INFO", "DEBUG", "WARN"};
            private final String[] errorKeywords = {"ERROR", "FATAL", "DENIED"}; // AlfaAgent가 감지할 키워드

            public LogWriterTask(String filePath) {
                this.filePath = filePath;
            }

            @Override
            public void run() {
                try {
                    // Main.main에서 파일을 미리 생성하므로 CREATE 옵션은 백업용.
                    while (true) {
                        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath),
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

                            String line = generateLogLine();
                            writer.write(line);
                            writer.newLine();
                        }

                        // 2000ms ~ 3000ms 사이 랜덤 대기
                        Thread.sleep(WRITE_INTERVAL_MS + random.nextInt(1000));
                    }
                } catch (Exception e) {
                    // InterruptedException 등
                    System.err.println("LogWriterTask 중단됨: " + e.getMessage());
                }
            }

            private String generateLogLine() {
                String timestamp = LocalDateTime.now().format(dtf);
                String level;
                String message = "일반 로그 메시지입니다. ID: " + random.nextInt(10000);

                // 10% 확률로 에러 키워드 중 하나를 포함시킴
                if (random.nextInt(10) == 0) {
                    level = errorKeywords[random.nextInt(errorKeywords.length)];
                    message = "!!! 치명적인 문제가 감지되었습니다 !!!";
                } else {
                    level = logLevels[random.nextInt(logLevels.length)];
                }

                return String.format("[%s] [%s] %s", timestamp, level, message);
            }
        }
    }
    // ========== LogGenerator 끝 ==========

    public static void main(String[] args) {

        String logDir = "test-logs/"; // 경로 수정
        int numFiles = 10;

        try {
            System.out.println("지정된 경로에 로그 파일이 없으면 새로 생성합니다...");
            // 로그 디렉토리 생성
            Files.createDirectories(Paths.get(logDir));

            for (int i = 0; i < numFiles; i++) {
                Path logPath = Paths.get(logDir, "app-" + i + ".log"); // 접두사 일치
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

        // *** LogGenerator 시작 ***
        // AlfaAgent보다 먼저 시작해서 로그를 생성하도록 함
        LogGenerator.start(numFiles, logDir);

        // 3. AlfaConfig 설정
        Map<String, String> paths = new HashMap<>();
        for (int i = 0; i < numFiles; i++) {
            paths.put("APP_" + i, Paths.get(logDir, "app-" + i + ".log").toString());
        }

        Map<String, Set<String>> filters = new HashMap<>();
        Set<String> keywords = Set.of("(?i).*(FATAL|ERROR|DENIED).*");
        for (int i = 0; i < numFiles; i++) {
            filters.put("APP_" + i, keywords);
        }

        AlfaConfig config = new AlfaConfig(new MyTestHandler(), paths, filters, 5, 10,null,null,true);
        AlfaAgent agent = new AlfaAgent(config);

        // 4. 에이전트 시작
        System.out.println("\nAlfa Agent 모니터링을 시작합니다. (대상: " + logDir + ")");
        agent.start(); // 무기한 실행
    }
}