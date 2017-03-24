package net.serenitybdd.cucumber;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public class FeatureFileContents {
    private final List<String> lines;

    public FeatureFileContents(String featureFilePath) {
        this.lines = readFeatureFileFrom(featureFilePath);
    }

    private List<String> readFeatureFileFrom(String uri) {
        try {
            return FileUtils.readLines(new File(uri), Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read feature file " + uri);
        }
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

