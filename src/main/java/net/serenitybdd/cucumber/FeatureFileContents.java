package net.serenitybdd.cucumber;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FeatureFileContents {
    private final List<String> lines;

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    public FeatureFileContents(String featureFilePath) {
        this.lines = readFeatureFileFrom(featureFilePath);
    }

    private List<String> readFeatureFileFrom(String featureFileName) {
        try {
            File featureFile = featureFileWithName(featureFileName);
            return FileUtils.readLines(featureFile, Charset.defaultCharset());
        } catch (IOException e) {
            LOGGER.warn("Could not find feature file " + featureFileName, e);
            return new ArrayList<>();
        }
    }

    private File featureFileWithName(String featureFileName) throws IOException {

        URL featureFileAsAResource = this.getClass().getClassLoader().getResource(featureFileName);

        if (featureFileAsAResource != null) {
            return new File(featureFileAsAResource.getFile());
        } else {
            return featureFileFromConfiguredPaths(featureFileName);
        }
    }

    private File featureFileFromConfiguredPaths(String featureFileName) throws IOException {
        for(String path : CucumberWithSerenity.currentRuntimeOptions().getFeaturePaths()) {
            if (Files.exists(candidatePath(path, featureFileName))) {
                return candidatePath(path, featureFileName).toFile();
            }
        }
        throw new IOException("No such feature file found for " + featureFileName);
    }

    private Path candidatePath(String path, String featureFileName) {
        return Paths.get(Joiner.on(File.separator).join(path, featureFileName));
    }

    public RowSelector betweenLine(int startRow) {
        return new RowSelector(startRow);
    }

    public class RowSelector {
        private final int startRow;

        public RowSelector(int startRow) {
            this.startRow = startRow;
        }

        public String and(Integer endRow) {
            List<String> rows = Lists.newArrayList();
            for(int row = startRow; row < endRow; row++) {
                rows.add(lines.get(row));
            }
            return Joiner.on(System.lineSeparator()).join(rows);
        }
    }
}

