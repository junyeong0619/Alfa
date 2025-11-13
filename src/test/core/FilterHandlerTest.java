package test.core;

import main.config.AlfaConfig;
import main.config.AlfaResultHandler;
import main.core.FilterHandler;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class FilterHandlerTest {
    private static Path tempLogFile; // temporary file path
    private AlfaConfig testConfig;
    private FilterHandler filterHandler;
    private MockResultHandler mockResultHandler; // mock handler for result capture

    // Mock implementation of AlfaResultHandler to capture test results
    static class MockResultHandler implements AlfaResultHandler {
        final List<String> filteredLogs = Collections.synchronizedList(new ArrayList<>());
        final List<String> keywordsFound = Collections.synchronizedList(new ArrayList<>());
        final List<String> errors = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void onLogFiltered(String logLine, String keyword) {
            filteredLogs.add(logLine);
            keywordsFound.add(keyword);
        }

        @Override
        public void onError(String pathSymbol, Exception e) {
            errors.add(pathSymbol + ": " + e.getMessage());
        }

        public void clear() {
            filteredLogs.clear();
            keywordsFound.clear();
            errors.clear();
        }
    }


    @BeforeAll
    static void setupClass() throws IOException {
        tempLogFile = Files.createTempFile("alfa-test-log-", ".log");
        System.out.println("Temporary log file created: " + tempLogFile.toString());
    }

    @AfterAll
    static void cleanupClass() throws IOException {
        Files.deleteIfExists(tempLogFile);
        System.out.println("Temporary log file deleted.");
    }

    @BeforeEach
    void setup() throws IOException {
        Files.write(tempLogFile, "".getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);

        mockResultHandler = new MockResultHandler();
        Map<String, String> paths = new HashMap<>();
        paths.put("TEST_LOG", tempLogFile.toString());
        Map<String, Set<String>> filters = new HashMap<>();
        filters.put("TEST_LOG", Set.of("ERROR", "FATAL", "DENIED"));

        testConfig = new AlfaConfig(mockResultHandler, paths, filters,
                null, null, null, StandardCharsets.UTF_8, false);

        // make new instance of FilterHandler and reset lastReadPoint
        filterHandler = new FilterHandler(testConfig, "TEST_LOG");

        // clear lastReadPoint from previous test
        testConfig.getLastReadPositions().remove("TEST_LOG");
    }

    @AfterEach
    void cleanup() {
        if (filterHandler != null) {
            filterHandler.close();
        }
        mockResultHandler.clear();
    }

    /**
     * Initial read: filtering test for lines containing configured keywords.
     */
    @Test
    @DisplayName("Initial Read: Should accurately filter lines containing configured keywords")
    void doFilter_InitialRead_ShouldFilterMatchingLines() throws IOException {
        List<String> lines = Arrays.asList(
                "[INFO] Application started successfully.",
                "[WARN] Configuration loaded with warnings.",
                "[ERROR] Database connection failed!",
                "[INFO] User logged in: testuser",
                "[FATAL] Critical system failure detected!",
                "[DEBUG] Processing request data..."
        );
        Files.write(tempLogFile, lines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        long fileSize = Files.size(tempLogFile);

        List<String> filteredResult = filterHandler.doFilter();

        assertEquals(2, filteredResult.size(), "The number of filtered lines must be 2.");
        assertEquals(2, mockResultHandler.filteredLogs.size(), "The number of logs recorded in the Mock Handler must also be 2.");

        assertTrue(filteredResult.get(0).contains("ERROR"), "The first filtered log must contain ERROR.");
        assertTrue(filteredResult.get(1).contains("FATAL"), "The second filtered log must contain FATAL.");

        assertTrue(mockResultHandler.keywordsFound.contains("ERROR"), "The Mock Handler must detect the 'ERROR' keyword.");
        assertTrue(mockResultHandler.keywordsFound.contains("FATAL"), "The Mock Handler must detect the 'FATAL' keyword.");

        assertEquals(fileSize, testConfig.getLastReadPositions().get("TEST_LOG"),
                "The last read position must equal the total file size.");
        assertTrue(mockResultHandler.errors.isEmpty(), "No errors should occur.");
    }

    /**
     * Test when new contents are added to the file (incremental read).
     */
    @Test
    @DisplayName("Incremental Read: Should process only newly added logs")
    void doFilter_IncrementalRead_ShouldFilterOnlyNewLines() throws IOException {
        List<String> initialLines = Arrays.asList("[INFO] Initial log", "[ERROR] First error detected");
        Files.write(tempLogFile, initialLines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        filterHandler.doFilter();
        long positionAfterFirstRead = testConfig.getLastReadPositions().get("TEST_LOG");
        mockResultHandler.clear();


        //after first execution. Add new logs
        List<String> newLines = Arrays.asList(
                "[INFO] Processing update...",
                "[DENIED] Access denied for user 'guest'", //filtering object
                "[WARN] Disk space low" // not filtered
        );
        Files.write(tempLogFile, newLines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        long finalFileSize = Files.size(tempLogFile);

        List<String> filteredResult = filterHandler.doFilter();

        assertEquals(1, filteredResult.size(), "Only 1 line among the newly added lines should be filtered.");
        assertEquals(1, mockResultHandler.filteredLogs.size(), "Only 1 log should be recorded in the Mock Handler.");

        assertTrue(filteredResult.get(0).contains("DENIED"), "The filtered log must contain DENIED.");
        assertEquals("DENIED", mockResultHandler.keywordsFound.get(0), "The Mock Handler must detect the 'DENIED' keyword.");

        // Check if the last read position was updated and is different from the initial position
        long positionAfterSecondRead = testConfig.getLastReadPositions().get("TEST_LOG");
        assertNotEquals(positionAfterFirstRead, positionAfterSecondRead, "The last read position must be changed.");
        assertEquals(finalFileSize, positionAfterSecondRead, "The last read position must equal the final file size.");
        assertTrue(mockResultHandler.errors.isEmpty(), "No errors should occur.");
    }

    /**
     * When the log file is replaced and its size decreases (simulating log rotation),
     * test if it starts reading from the beginning of the file again.
     */
    @Test
    @DisplayName("File Rollover Simulation: Should reset position and read from start when file size decreases")
    void doFilter_FileRollover_ShouldResetPositionAndReadFromStart() throws IOException {
        List<String> initialLines = Arrays.asList("[INFO] Log line 1", "[ERROR] Error line 1", "[INFO] Log line 2");
        Files.write(tempLogFile, initialLines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        filterHandler.doFilter();
        long positionAfterFirstRead = testConfig.getLastReadPositions().get("TEST_LOG");
        mockResultHandler.clear();

        // rewrite the file in small size.
        List<String> rolledLines = Arrays.asList("[FATAL] New critical error", "[INFO] New start");
        Files.write(tempLogFile, rolledLines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        long rolledFileSize = Files.size(tempLogFile);
        assertTrue(rolledFileSize < positionAfterFirstRead, "After rollover, the file size must be smaller than the previous position.");

        List<String> filteredResult = filterHandler.doFilter();

        assertEquals(1, filteredResult.size(), "1 line should be filtered from the rolled-over file.");
        assertEquals(1, mockResultHandler.filteredLogs.size());
        assertTrue(filteredResult.get(0).contains("FATAL"));
        assertEquals("FATAL", mockResultHandler.keywordsFound.get(0));

        assertEquals(rolledFileSize, testConfig.getLastReadPositions().get("TEST_LOG"),
                "The last read position must equal the rolled-over file size.");
        assertTrue(mockResultHandler.errors.isEmpty());
    }

    /**
     * test if there is no matched keyword return empty list
     */
    @Test
    @DisplayName("No Matching Keywords: Should return an empty filtered result")
    void doFilter_NoMatchingKeywords_ShouldReturnEmptyList() throws IOException {
        List<String> lines = Arrays.asList("[INFO] Info message", "[DEBUG] Debug details", "[WARN] Just a warning");
        Files.write(tempLogFile, lines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        List<String> filteredResult = filterHandler.doFilter();

        // Assert: result verification
        assertTrue(filteredResult.isEmpty(), "The filtered result must be empty.");
        assertTrue(mockResultHandler.filteredLogs.isEmpty(), "No logs should be recorded in the Mock Handler.");
        assertTrue(mockResultHandler.keywordsFound.isEmpty());
        // The last read position should be at the end of the file
        assertEquals(Files.size(tempLogFile), testConfig.getLastReadPositions().get("TEST_LOG"));
        assertTrue(mockResultHandler.errors.isEmpty());
    }

    /**
     * test when the file is empty
     */
    @Test
    @DisplayName("Empty File Handling: Should return empty result without errors")
    void doFilter_EmptyFile_ShouldReturnEmptyListWithoutErrors() throws IOException {
        assertEquals(0, Files.size(tempLogFile), "File must be empty at the start of the test.");

        List<String> filteredResult = filterHandler.doFilter();

        assertTrue(filteredResult.isEmpty());
        assertTrue(mockResultHandler.filteredLogs.isEmpty());
        assertEquals(0, testConfig.getLastReadPositions().getOrDefault("TEST_LOG", 0L));
        assertTrue(mockResultHandler.errors.isEmpty());
    }

    /**
     * Test if a single line containing multiple filter keywords is processed only once with the first matching keyword.
     * (Since FilterHandler's current logic breaks after the first match)
     */
    @Test
    @DisplayName("Multiple Keywords in Line: Should filter only once with the first match")
    void doFilter_MultipleKeywordsInLine_ShouldFilterOnceWithFirstMatch() throws IOException {
        List<String> lines = Arrays.asList("[ERROR] This is a FATAL error condition.");
        Files.write(tempLogFile, lines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        Set<String> expectedKeywords = Set.of("ERROR", "FATAL");

        List<String> filteredResult = filterHandler.doFilter();

        assertEquals(1, filteredResult.size(), "The line must be filtered once.");
        assertEquals(1, mockResultHandler.filteredLogs.size());
        assertTrue(filteredResult.get(0).contains("ERROR") && filteredResult.get(0).contains("FATAL"));
        assertEquals(1, mockResultHandler.keywordsFound.size());
        assertTrue(expectedKeywords.contains(mockResultHandler.keywordsFound.get(0)),
                "The detected keyword must be 'ERROR' or 'FATAL'. Actual: " + mockResultHandler.keywordsFound.get(0));
        assertEquals(Files.size(tempLogFile), testConfig.getLastReadPositions().get("TEST_LOG"));
        assertTrue(mockResultHandler.errors.isEmpty());
    }

    /**
     * file encoding test.
     */
    @Test
    @DisplayName("Different Encoding Handling (EUC-KR): Should filter Korean text correctly without corruption")
    void doFilter_DifferentEncoding_ShouldHandleCorrectly() throws IOException {
        filterHandler.close();

        Map<String, String> paths = new HashMap<>();
        paths.put("TEST_LOG_EUCKR", tempLogFile.toString());
        Map<String, Set<String>> filters = new HashMap<>();
        // Note: Keywords are intentionally left in Korean to match the EUC-KR log content for testing
        filters.put("TEST_LOG_EUCKR", Set.of("오류", "치명적"));

        testConfig = new AlfaConfig(mockResultHandler, paths, filters,
                null, null, null, Charset.forName("EUC-KR"), false);
        filterHandler = new FilterHandler(testConfig, "TEST_LOG_EUCKR");
        testConfig.getLastReadPositions().remove("TEST_LOG_EUCKR");

        // Korean log lines translated for context, but written as string literals in the source:
        String log1 = "[정보] 정상 처리되었습니다."; // [INFO] Processed successfully.
        String log2 = "[오류] 예외가 발생했습니다!"; // [ERROR] An exception occurred!
        String log3 = "[치명적] 시스템 중단!"; // [FATAL] System shutdown!
        List<String> lines = Arrays.asList(log1, log2, log3);
        Files.write(tempLogFile, lines, Charset.forName("EUC-KR"), StandardOpenOption.APPEND);
        long fileSize = Files.size(tempLogFile);

        List<String> filteredResult = filterHandler.doFilter();

        assertEquals(2, filteredResult.size(), "2 lines must be filtered using the Korean keywords.");
        assertEquals(2, mockResultHandler.filteredLogs.size());
        assertTrue(filteredResult.get(0).contains("오류"));
        assertTrue(filteredResult.get(1).contains("치명적"));
        assertTrue(mockResultHandler.keywordsFound.contains("오류"));
        assertTrue(mockResultHandler.keywordsFound.contains("치명적"));
        assertEquals(fileSize, testConfig.getLastReadPositions().get("TEST_LOG_EUCKR"));
        assertTrue(mockResultHandler.errors.isEmpty());
    }
}