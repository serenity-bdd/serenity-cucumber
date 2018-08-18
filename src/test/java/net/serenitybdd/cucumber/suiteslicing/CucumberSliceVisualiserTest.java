package net.serenitybdd.cucumber.suiteslicing;

import com.google.gson.GsonBuilder;

import net.thucydides.core.annotations.Narrative;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.util.EnvironmentVariables;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static net.thucydides.core.ThucydidesSystemProperty.SERENITY_OUTPUT_DIRECTORY;

@Narrative(text = "There are no assertions in this test, but this is a useful tool for generating a visualisation of how a specific test slicing "
                  + "configuration will be executed as part of a running test. The test methods below allow a number of parameters to be input and when the test is run"
                  + "a json file will be produced into the serenity output directory that shows how the tests have been sliced."
                  + "Note that test slicing will always be 100% deterministic given the same set of inputs "
                  + "which makes this mechanism an excellent choice for running on grid of multiple servers, each running a mutually exclusive slice."
                  + "Variables that can be input here include feature root, number of slices, number of forks and test statistics (line count based or actual run data)")
public class CucumberSliceVisualiserTest {

    private final Logger LOGGER = LoggerFactory.getLogger(CucumberSliceVisualiserTest.class);
    private TestStatistics HISTORIC_RUN_STATISTICS;
    private TestStatistics LINE_COUNT_STATISTICS;
    static EnvironmentVariables environmentVariables;
    private static final String FEATURE_ROOT = "smoketests";

    @BeforeClass
    public static void createDirectory() throws IOException {
        environmentVariables = Injectors.getInjector().getInstance(EnvironmentVariables.class);
        String outputDirectory = outputDirectory();
        Files.createDirectories(Paths.get(outputDirectory));
    }

    private static String outputDirectory() {
        return environmentVariables.getProperty(SERENITY_OUTPUT_DIRECTORY, "target/site/serenity");
    }

    @Before
    public void setUp() {
        HISTORIC_RUN_STATISTICS = MultiRunTestStatistics.fromRelativePath("/statistics");
        LINE_COUNT_STATISTICS = ScenarioLineCountStatistics.fromFeaturePath("classpath:" + FEATURE_ROOT);
    }

    @Test
    public void visualise4SlicesWith2Forks() {
        visualise(FEATURE_ROOT, 4, 2, HISTORIC_RUN_STATISTICS);
    }

    @Test
    public void visualise5SlicesWith1ForkBasedOnRunStats() {
        visualise(FEATURE_ROOT, 5, 1, HISTORIC_RUN_STATISTICS);
    }

    @Test
    public void visualise5SlicesWith1ForkBasedOnLineCount() {
        visualise(FEATURE_ROOT, 5, 1, LINE_COUNT_STATISTICS);
    }

    public void visualise(String rootFolder, int sliceCount, int forkCount, TestStatistics testStatistics) {
        List<WeightedCucumberScenarios> slices = new CucumberScenarioLoader(newArrayList("classpath:" + rootFolder), testStatistics).load().sliceInto(sliceCount);
        List<VisualisableCucumberScenarios> visualisedSlices = VisualisableCucumberScenarios.visualise(forkCount, slices);
        String jsonFile = String.format("%s/%s-slice-config-%s-forks-in-each-of-%s-slices-using-%s.json", outputDirectory(), rootFolder, forkCount, sliceCount, testStatistics);
        try {
            Files.write(Paths.get(jsonFile), new GsonBuilder().setPrettyPrinting().create().toJson(visualisedSlices).getBytes());
            LOGGER.info("Wrote fork slice as JSON for {} slices -> {}", visualisedSlices.size(), jsonFile);
        } catch (Exception e) {
            throw new RuntimeException("failed to create suite slices", e);
        }
    }

}
