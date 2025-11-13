package main.core;

import main.config.AlfaConfig;

import java.io.*;
import java.util.*;

public class FilterHandler {
    private AlfaConfig config;
    private String pathSymbol;
    private String path;

    private RandomAccessFile raf;
    private BufferedReader br;

    /**
     * Constructor: Opens file resources when the FilterHandler is created.
     * @param config AlfaConfig
     * @param pathSymbol The file symbol this handler is responsible for.
     * @throws IOException If the file path is not found or resources cannot be opened.
     */
    public FilterHandler(AlfaConfig config, String pathSymbol) throws IOException {
        this.config = config;
        this.pathSymbol = pathSymbol;
        this.path = config.getAbsPaths().get(pathSymbol);

        if (path == null) {
            throw new IOException("Path not found for symbol: " + pathSymbol);
        }

        this.raf = new RandomAccessFile(path, "r");
        FileInputStream fis = new FileInputStream(raf.getFD());
        InputStreamReader isr = new InputStreamReader(fis, config.getFileEncoding());
        this.br = new BufferedReader(isr);
    }


    /**
     * Performs the filtering operation (reuses resources).
     * @return A list of filtered log lines.
     */
    public List<String> doFilter() {
        List<String> filteredLines = new ArrayList<>();
        Map<String, Long> positions = config.getLastReadPositions();
        long startPosition = positions.getOrDefault(pathSymbol, 0L);

        try {
            long currentFileSize = raf.length();

            if (startPosition > currentFileSize) {
                startPosition = 0L;
            }

            raf.seek(startPosition);

            String line;
            while ((line = br.readLine()) != null) {
                Set<String> filterOpts = config.getFilterOpts().get(pathSymbol);
                if (filterOpts != null) {
                    for (String option : filterOpts) {
                        if (line.contains(option)) {
                            filteredLines.add(line);
                            config.getResultHandler().onLogFiltered(line, option);
                            break;
                        }
                    }
                }
            }

            long currentPosition = raf.getFilePointer();
            positions.put(pathSymbol, currentPosition);

        } catch (IOException e) {
            config.getResultHandler().onError(pathSymbol, e);
        }

        return filteredLines;
    }

    /**
     * Closes the file resources when they are no longer in use.
     */
    public void close() {
        try {
            if (br != null) {
                br.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (raf != null) {
                raf.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}