package main.core;

import main.config.AlfaConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class PathHandler {

    AlfaConfig alfaConfig;
    public PathHandler(AlfaConfig alfaConfig) {
        this.alfaConfig = alfaConfig;
    }

    public boolean verifyAllPaths(){
        Map<String, String> absPaths = alfaConfig.getAbsPaths();
        for (String symbol : alfaConfig.getAbsPathSymbols()) {
            String path = absPaths.get(symbol);
            if(Files.exists(Path.of(path))){
                // Continue checking other paths
            }else{
                throw new RuntimeException("Path " + path + " does not exist");
            }
        }
        // If loop completes without exception, all paths exist
        return true;
    }
}