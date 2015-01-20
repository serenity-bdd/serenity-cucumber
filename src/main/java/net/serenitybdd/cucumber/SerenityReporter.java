package net.serenitybdd.cucumber;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import cucumber.runtime.StepDefinitionMatch;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.*;
import gherkin.formatter.model.DataTableRow;
import net.serenitybdd.core.Serenity;
import net.serenitybdd.core.SerenityListeners;
import net.serenitybdd.core.SerenityReports;
import net.thucydides.core.model.*;
import net.thucydides.core.reports.ReportService;
import net.thucydides.core.steps.BaseStepListener;
import net.thucydides.core.steps.ExecutedStepDescription;
import net.thucydides.core.steps.StepEventBus;
import net.thucydides.core.steps.StepFailure;
import net.thucydides.core.util.Inflector;
import net.thucydides.core.webdriver.Configuration;
import net.thucydides.core.webdriver.ThucydidesWebDriverSupport;
import org.apache.commons.lang3.StringUtils;
import org.junit.internal.AssumptionViolatedException;

import java.io.File;
import java.util.*;

import static ch.lambdaj.Lambda.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Generates Thucydides reports.
 *
 * @author L.Carausu (liviu.carausu@gmail.com)
 */
public class SerenityReporter implements Formatter, Reporter {

    private static final String OPEN_PARAM_CHAR = "\uff5f";
    private static final String CLOSE_PARAM_CHAR = "\uff60";

    private static final List<String> SKIPPED_TAGS = ImmutableList.of("@skip", "@wip");

    private final Queue<Step> stepQueue;

    private Configuration systemConfiguration;

    private ThreadLocal<SerenityListeners> thucydidesListenersThreadLocal;

    private final List<BaseStepListener> baseStepListeners;

    private Feature currentFeature;

    private int currentExample = 0;

    private boolean examplesRunning;

    private List<Map<String, String>> exampleRows;

    private int exampleCount = 0;

    private DataTable table;

    private boolean waitingToProcessBackgroundSteps = false;

    private String currentUri;

    private String defaultFeatureName;
    private String defaultFeatureId;

    private final static String FEATURES_ROOT_PATH = "features";


    private static Optional<TestResult> forcedStoryResult = Optional.absent();
    private static Optional<TestResult> forcedScenarioResult = Optional.absent();

    private void clearStoryResult() {
        forcedStoryResult = Optional.absent();
    }

    private void clearScenarioResult() {
        forcedScenarioResult = Optional.absent();
    }


    private boolean isPendingStory() {
        return ((forcedStoryResult.or(TestResult.UNDEFINED) == TestResult.PENDING)
                || (forcedScenarioResult.or(TestResult.UNDEFINED) == TestResult.PENDING));
    }

    private boolean isSkippedStory() {
        return ((forcedStoryResult.or(TestResult.UNDEFINED) == TestResult.SKIPPED)
                || (forcedScenarioResult.or(TestResult.UNDEFINED) == TestResult.SKIPPED));
    }

    public SerenityReporter(Configuration systemConfiguration) {
        this.systemConfiguration = systemConfiguration;
        this.stepQueue = new LinkedList<>();
        thucydidesListenersThreadLocal = new ThreadLocal<>();
        baseStepListeners = Lists.newArrayList();
        clearStoryResult();
    }

    protected SerenityListeners getThucydidesListeners() {
        if (thucydidesListenersThreadLocal.get() == null) {
            SerenityListeners listeners = SerenityReports.setupListeners(systemConfiguration);
            thucydidesListenersThreadLocal.set(listeners);
            synchronized (baseStepListeners) {
                baseStepListeners.add(listeners.getBaseStepListener());
            }
        }
        return thucydidesListenersThreadLocal.get();
    }

    protected ReportService getReportService() {
        return SerenityReports.getReportService(systemConfiguration);
    }

    @Override
    public void syntaxError(String state, String event, List<String> legalEvents, String uri, Integer line) {
    }

    @Override
    public void uri(String uri) {
        currentUri = uri;
        String featuresRoot = File.separatorChar + FEATURES_ROOT_PATH + File.separatorChar;
        if (uri.contains(featuresRoot)) {
            currentUri = uri.substring(uri.lastIndexOf(featuresRoot) + FEATURES_ROOT_PATH.length() + 2);
        }
        defaultFeatureId = new File(currentUri).getName().replace(".feature","");
        defaultFeatureName = Inflector.getInstance().humanize(defaultFeatureId);
    }


    @Override
    public void feature(Feature feature) {

        assureTestSuiteFinished();
        if (feature.getName().isEmpty()) {
            feature = featureWithDefaultName(feature, defaultFeatureName, defaultFeatureId);
        }

        currentFeature = feature;

        configureDriver(feature);
        getThucydidesListeners().withDriver(ThucydidesWebDriverSupport.getDriver());
        Story userStory = Story.withIdAndPath(feature.getId(), feature.getName(), currentUri).asFeature();

        if (!isEmpty(feature.getDescription())) {
            userStory = userStory.withNarrative(feature.getDescription());
        }
        StepEventBus.getEventBus().testSuiteStarted(userStory);

        checkForPending(feature);
        checkForSkipped(feature);
    }

    private Feature featureWithDefaultName(Feature feature, String defaultName, String id) {
        return new Feature(feature.getComments(),
                feature.getTags(),
                feature.getKeyword(),
                defaultName,
                feature.getDescription(),
                feature.getLine(),
                id);
    }

    private void checkForPending(Feature feature) {
        if (isPending(feature.getTags())) {
            forcedStoryResult = Optional.of(TestResult.PENDING);
            StepEventBus.getEventBus().suspendTest();
        }
    }

    private void checkForSkipped(Feature feature) {
        if (isSkippedOrWIP(feature.getTags())) {
            forcedStoryResult = Optional.of(TestResult.SKIPPED);
            StepEventBus.getEventBus().suspendTest();
        }
    }

    private void checkForPending(Scenario scenario) {
        if (isPending(scenario.getTags())) {
            forcedScenarioResult = Optional.of(TestResult.PENDING);
            StepEventBus.getEventBus().suspendTest();
        }
    }

    private void checkForSkipped(Scenario scenario) {
        if (isSkippedOrWIP(scenario.getTags())) {
            forcedScenarioResult = Optional.of(TestResult.SKIPPED);
            StepEventBus.getEventBus().suspendTest();
        }
    }

    private boolean isPending(List<Tag> tags) {
        return hasTag("@pending", tags);
    }

    private boolean isSkippedOrWIP(List<Tag> tags) {
        for (Tag tag : tags) {
            if (SKIPPED_TAGS.contains(tag.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasTag(String tagName, List<Tag> tags) {
        for (Tag tag : tags) {
            if (tag.getName().equals(tagName)) {
                return true;
            }
        }
        return false;
    }

    private void configureDriver(Feature feature) {
        StepEventBus.getEventBus().setUniqueSession(systemConfiguration.getUseUniqueBrowser());
        String requestedDriver = getDriverFrom(feature);
        if (StringUtils.isNotEmpty(requestedDriver)) {
            ThucydidesWebDriverSupport.initialize(requestedDriver);
        } else {
            ThucydidesWebDriverSupport.initialize();
        }
    }

    private String getDriverFrom(Feature feature) {
        List<Tag> tags = feature.getTags();
        String requestedDriver = null;
        for (Tag tag : tags) {
            if (tag.getName().startsWith("@driver:")) {
                requestedDriver = tag.getName().substring(8);
            }
        }
        return requestedDriver;
    }


    boolean addingScenarioOutlineSteps = false;

    @Override
    public void scenarioOutline(ScenarioOutline scenarioOutline) {
        addingScenarioOutlineSteps = true;
    }

    String currentScenarioId;

    @Override
    public void examples(Examples examples) {
        addingScenarioOutlineSteps = false;
        reinitializeExamples();
        List<ExamplesTableRow> examplesTableRows = examples.getRows();
        List<String> headers = getHeadersFrom(examplesTableRows);
        List<Map<String, String>> rows = getValuesFrom(examplesTableRows, headers);

        for (int i = 1; i < examplesTableRows.size(); i++) {
            addRow(exampleRows, headers, examplesTableRows.get(i));
        }

        String scenarioId = scenarioIdFrom(examples.getId());
        boolean newScenario = !scenarioId.equals(currentScenarioId);

         table = (newScenario) ?
                thucydidesTableFrom(headers, rows, examples.getName(), examples.getDescription())
                : addTableRowsTo(table, headers, rows, examples.getName(), examples.getDescription());
        exampleCount = examples.getRows().size() - 1;

        currentScenarioId = scenarioId;
    }

    private String scenarioIdFrom(String scenarioExampleId) {
        String[] idElements = scenarioExampleId.split(";");
        return (idElements.length >= 2) ? idElements[1] : "";
    }

    private void reinitializeExamples() {
        examplesRunning = true;
        currentExample = 0;
        exampleRows = new ArrayList<>();
    }

    private List<String> getHeadersFrom(List<ExamplesTableRow> examplesTableRows) {
        ExamplesTableRow headerRow = examplesTableRows.get(0);
        return headerRow.getCells();
    }

    private List<Map<String, String>> getValuesFrom(List<ExamplesTableRow> examplesTableRows, List<String> headers) {

        List<Map<String, String>> rows = Lists.newArrayList();

        for (int row = 1; row < examplesTableRows.size(); row++) {
            Map<String, String> rowValues = Maps.newHashMap();
            int column = 0;
            for (String cellValue : examplesTableRows.get(row).getCells()) {
                String columnName = headers.get(column++);
                rowValues.put(columnName, cellValue);
            }
            rows.add(rowValues);
        }
        return rows;
    }

    private void addRow(List<Map<String, String>> exampleRows,
                        List<String> headers,
                        ExamplesTableRow currentTableRow) {
        Map<String, String> row = new HashMap<>();
        for (int j = 0; j < headers.size(); j++) {
            row.put(headers.get(j), currentTableRow.getCells().get(j));
        }
        exampleRows.add(row);
    }


    private DataTable thucydidesTableFrom(List<String> headers,
                                          List<Map<String, String>> rows,
                                          String name,
                                          String description) {
        return DataTable.withHeaders(headers).andMappedRows(rows).andTitle(name).andDescription(description).build();
    }

    private DataTable addTableRowsTo(DataTable table, List<String> headers,
                                     List<Map<String, String>> rows,
                                     String name,
                                     String description) {
        table.startNewDataSet(name, description);
        for (Map<String, String> row : rows) {
            table.appendRow(rowValuesFrom(headers, row));
        }
        table.nextRow();
        return table;
    }

    private Map<String, String> rowValuesFrom(List<String> headers, Map<String, String> row) {
        Map<String, String> rowValues = Maps.newHashMap();
        for (String header : headers) {
            rowValues.put(header, row.get(header));
        }
        return ImmutableMap.copyOf(rowValues);
    }

    String currentScenario;

    @Override
    public void startOfScenarioLifeCycle(Scenario scenario) {

        boolean newScenario = !scenario.getName().equals(currentScenario);
        currentScenario = scenario.getName();

        if (examplesRunning) {

            if (newScenario) {
                startScenario(scenario);
                StepEventBus.getEventBus().useExamplesFrom(table);
            } else {
                StepEventBus.getEventBus().addNewExamplesFrom(table);
            }
            startExample();
        } else {
            startScenario(scenario);
        }
    }

    private void startScenario(Scenario scenario) {
        StepEventBus.getEventBus().testStarted(scenario.getName());
        StepEventBus.getEventBus().addDescriptionToCurrentTest(scenario.getDescription());
        StepEventBus.getEventBus().addTagsToCurrentTest(convertCucumberTags(currentFeature.getTags()));
        StepEventBus.getEventBus().addTagsToCurrentTest(convertCucumberTags(scenario.getTags()));
        getThucydidesListeners().withDriver(ThucydidesWebDriverSupport.getDriver());
    }


    private List<TestTag> convertCucumberTags(List<Tag> cucumberTags) {
        List<TestTag> tags = Lists.newArrayList();
        for (Tag tag : cucumberTags) {
            tags.add(TestTag.withValue(tag.getName().substring(1)));
        }
        return ImmutableList.copyOf(tags);
    }

    @Override
    public void endOfScenarioLifeCycle(Scenario scenario) {
        if (examplesRunning) {
            finishExample();
        } else {
            generateReports();
        }
    }

    private void startExample() {
        Map<String, String> data = exampleRows.get(currentExample);
        StepEventBus.getEventBus().clearStepFailures();
        StepEventBus.getEventBus().exampleStarted(data);
        currentExample++;
    }

    private void finishExample() {
        StepEventBus.getEventBus().exampleFinished();
        exampleCount--;
        if (exampleCount == 0) {
            examplesRunning = false;
            generateReports();
        } else {
            examplesRunning = true;
        }
    }

    @Override
    public void background(Background background) {
        waitingToProcessBackgroundSteps = true;
        StepEventBus.getEventBus().setBackgroundTitle(background.getName());
        StepEventBus.getEventBus().setBackgroundDescription(background.getDescription());
    }

    @Override
    public void scenario(Scenario scenario) {
        clearScenarioResult();
        checkForPending(scenario);
        checkForSkipped(scenario);
    }

    @Override
    public void step(Step step) {
        if (!addingScenarioOutlineSteps) {
            stepQueue.add(step);
        }
    }

    @Override
    public void done() {
        assureTestSuiteFinished();
//        if (nestedResult != null) {
//            throw new RuntimeException(nestedResult.toException());
//        }
    }

    @Override
    public void close() {
        assureTestSuiteFinished();
    }

    private void assureTestSuiteFinished() {
        if (currentFeature != null) {
            stepQueue.clear();
            StepEventBus.getEventBus().testSuiteFinished();
            StepEventBus.getEventBus().clear();
            Serenity.done();
            table = null;
        }
    }

    @Override
    public void eof() {

    }

    @Override
    public void before(Match match, Result result) {
        nestedResult = null;
    }

    FailureCause nestedResult;

    @Override
    public void result(Result result) {

//        Optional<TestResult> nestedStepResult = latestNestedStepResult();
//        if (nestedStepResult.or(TestResult.SUCCESS) != TestResult.SUCCESS) {
//            result = SerenityResultOverride.override(result, latestNestedStep());
//            if (latestNestedStep().getNestedException() != null) {
//                nestedResult = latestNestedStep().getNestedException();
//            }
//        }

        Step currentStep = stepQueue.poll();
        if (Result.PASSED.equals(result.getStatus())) {
            StepEventBus.getEventBus().stepFinished();
        } else if (Result.FAILED.equals(result.getStatus())) {
            failed(stepTitleFrom(currentStep), result.getError());
        } else if (Result.SKIPPED.equals(result)) {
            StepEventBus.getEventBus().stepIgnored();
        } else if (Result.UNDEFINED.equals(result)) {
            StepEventBus.getEventBus().stepStarted(ExecutedStepDescription.withTitle(stepTitleFrom(currentStep)));
            StepEventBus.getEventBus().stepPending();
        }

        if (stepQueue.isEmpty()) {
            if (waitingToProcessBackgroundSteps) {
                waitingToProcessBackgroundSteps = false;
            } else {
                if (examplesRunning) { //finish enclosing step because testFinished resets the queue
                    StepEventBus.getEventBus().stepFinished();
                }
                updatePendingResults();
                updateSkippedResults();
                StepEventBus.getEventBus().testFinished();
            }
        }

    }

    private void updatePendingResults() {
        if (isPendingStory()) {
            StepEventBus.getEventBus().setAllStepsTo(TestResult.PENDING);
        }
    }

    private void updateSkippedResults() {
        if (isSkippedStory()) {
            StepEventBus.getEventBus().setAllStepsTo(TestResult.SKIPPED);
        }
    }

    public void failed(String stepTitle, Throwable cause) {
        Throwable rootCause = cause.getCause() != null ? cause.getCause() : cause;
        StepEventBus.getEventBus().updateCurrentStepTitle(stepTitle);
        if (isAssumptionFailure(rootCause)) {
            StepEventBus.getEventBus().assumptionViolated(rootCause.getMessage());
        } else {
            StepEventBus.getEventBus().stepFailed(new StepFailure(ExecutedStepDescription.withTitle(normalized(stepTitle)), rootCause));
        }
    }

    private boolean isAssumptionFailure(Throwable rootCause) {
        return (AssumptionViolatedException.class.isAssignableFrom(rootCause.getClass()));
    }

    @Override
    public void after(Match match, Result result) {
//        if (nestedResult != null) {
//            throw new CucumberException(nestedResult.toException());
//        }
    }

    @Override
    public void match(Match match) {
        if (match instanceof StepDefinitionMatch) {
            Step currentStep = stepQueue.peek();
            String stepTitle = stepTitleFrom(currentStep);
            StepEventBus.getEventBus().stepStarted(ExecutedStepDescription.withTitle(stepTitle));
            StepEventBus.getEventBus().updateCurrentStepTitle(normalized(stepTitle));
        }
    }

    private String stepTitleFrom(Step currentStep) {
        return currentStep.getKeyword()
                + currentStep.getName()
                + embeddedTableDataIn(currentStep);
    }

    private String embeddedTableDataIn(Step currentStep) {
        return (currentStep.getRows() == null || currentStep.getRows().isEmpty()) ?
                "" : convertToTextTable(currentStep.getRows());
    }

    private String convertToTextTable(List<DataTableRow> rows) {
        StringBuilder textTable = new StringBuilder();
        textTable.append(System.lineSeparator());
        for (DataTableRow row : rows) {
            textTable.append("|");
            for (String cell : row.getCells()) {
                textTable.append(" ");
                textTable.append(cell);
                textTable.append(" |");
            }
            if (row != rows.get(rows.size() - 1)) {
                textTable.append(System.lineSeparator());
            }
        }
        return textTable.toString();
    }

    @Override
    public void embedding(String mimeType, byte[] data) {
    }

    @Override
    public void write(String text) {
    }

    private synchronized void generateReports() {
        getReportService().generateReportsFor(getAllTestOutcomes());
    }

    public List<TestOutcome> getAllTestOutcomes() {
        return flatten(extract(baseStepListeners, on(BaseStepListener.class).getTestOutcomes()));
    }

    private String normalized(String value) {
        return value.replaceAll(OPEN_PARAM_CHAR, "{").replaceAll(CLOSE_PARAM_CHAR, "}");
    }
}
