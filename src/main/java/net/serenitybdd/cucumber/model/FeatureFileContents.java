package net.serenitybdd.cucumber.model;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class FeatureFileContents {
    private final List<String> lines;

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureFileContents.class);

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

    public RowSelector betweenLine(int startRow) {
        return new RowSelector(startRow, false);
    }

    public RowSelectorBuilder trimmedContent() {
        return new RowSelectorBuilder(true);
    }

    public class RowSelector {
        private final int startRow;
        private final boolean trim;

        public RowSelector(int startRow, boolean trim) {
            this.trim = trim;
            this.startRow = startRow;
        }

        public String and(Integer endRow) {
            if (endRow >= lines.size()) {
                return "";
            }

            List<String> rows = Lists.newArrayList();
            for (int row = startRow; row < endRow; row++) {
                String line = (trim) ? lines.get(row).trim() : lines.get(row);
                rows.add(line);
            }
            return Joiner.on(System.lineSeparator()).join(rows);
        }
    }

    private File featureFileWithName(String featureFileName) throws IOException {

        StoredFeatureFile theStoredFeatureFile = StoredFeatureFile.withName(featureFileName);

        if (theStoredFeatureFile.existsOnTheClasspath()) {
            return StoredFeatureFile.withName(featureFileName).onTheClasspath();
        } else if (theStoredFeatureFile.existsOnTheFileSystem()) {
            return theStoredFeatureFile.onTheFileSystem();
        } else {
            return theStoredFeatureFile.fromTheConfiguredPaths();
        }
    }

    public class RowSelectorBuilder {
        private final boolean trim;

        public RowSelectorBuilder(boolean trim) {
            this.trim = trim;
        }

        public RowSelector betweenLine(int startRow) {
            return new RowSelector(startRow, trim);
        }
    }
}

