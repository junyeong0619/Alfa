package core;

import config.AlfaConfig;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class FilterHandler {
    AlfaConfig config;

    public FilterHandler(AlfaConfig config) {
        this.config = config;
    }


    public List<String> doFilter(String pathSymbol) throws IOException {
        List<String> filteredLines = new ArrayList<>();
        String path = config.getAbsPaths().get(pathSymbol);
        if(path == null){
            throw new RuntimeException("Path not found");
        }

       //get last read point from AlfaConfig
        Map<String, Long> positions = config.getLastReadPositions();
        long startPosition = positions.getOrDefault(pathSymbol, 0L);

        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {

            long currentFileSize = raf.length();

            if (startPosition > currentFileSize) {
                startPosition = 0L;
            }

            //move the file pointer to the last point
            raf.seek(startPosition);

            String line;


            while ((line = raf.readLine()) != null) {

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

            //update last read point
            long currentPosition = raf.getFilePointer();
            positions.put(pathSymbol, currentPosition);

        }

        return filteredLines;
    }

}
