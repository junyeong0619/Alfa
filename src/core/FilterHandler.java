package core;

import config.AlfaConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        Set<String> filterOpts = config.getFilterOpts().get(pathSymbol);
        for(String line : lines){
            for (String option : filterOpts) {
                if(line.contains(option)){
                    filteredLines.add(line);
                    config.getResultHandler().onLogFiltered(line, option);
                }
            }
        }
        return filteredLines;
    }

}
