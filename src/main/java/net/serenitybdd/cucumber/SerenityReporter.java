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
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.model.*;
import net.thucydides.core.model.stacktrace.FailureCause;
import net.thucydides.core.model.stacktrace.RootCauseAnalyzer;
import net.thucydides.core.reports.ReportService;
import net.thucydides.core.steps.BaseStepListener;
import net.thucydides.core.steps.ExecutedStepDescription;
import net.thucydides.core.steps.StepEventBus;
import net.thucydides.core.steps.StepFailure;
import net.thucydides.core.util.Inflector;
import net.thucydides.core.webdriver.Configuration;
import net.thucydides.core.webdriver.ThucydidesWebDriverSupport;
import org.junit.internal.AssumptionViolatedException;

import java.io.File;
import java.util.*;

import static ch.lambdaj.Lambda.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Generates Thucydides reports.
 *
 * @author L.Carausu (liviu.carausu@gmail.com)
 */
public class SerenityReporter implements Formatter, Reporter {

    private static final String OPEN_PARAM_CHAR = "\uff5f";
    private static final String CLOSE_PARAM_CHAR = "\uff60";

    private static final List<String> SKIPPED_TAGS = ImmutableList.of("@skip", "@wip","@ignore","@ignored");
    public static final String PENDING_STATUS = "pending";

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

    private boolean uniqueBrowserTag = false;

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

        if (feature.getName().isEmpty()) {
            feature = featureWithDefaultName(feature, defaultFeatureName, defaultFeatureId);
        }

        currentFeature = feature;

        configureDriver(feature);
        getThucydidesListeners();
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

    private void checkForPendingScenario(List<Tag> tags) {
        if (isPending(tags)) {
            forcedScenarioResult = Optional.of(TestResult.PENDING);
            StepEventBus.getEventBus().suspendTest(TestResult.PENDING);
        }
    }

    private void checkForSkippedScenario(List<Tag> tags) {
        if (isSkippedOrWIP(tags)) {
            forcedScenarioResult = Optional.of(TestResult.SKIPPED);
            StepEventBus.getEventBus().suspendTest(TestResult.SKIPPED);
        }
    }

    private void checkForManualScenario(List<Tag> tags) {
        if (isManual(tags)) {
            forcedScenarioResult = Optional.of(TestResult.SKIPPED);
            StepEventBus.getEventBus().testIsManual();
            StepEventBus.getEventBus().suspendTest(TestResult.SKIPPED);
        }
    }

    private boolean isPending(List<Tag> tags) {
        return hasTag("@pending", tags);
    }

    private boolean isManual(List<Tag> tags) {
        return hasTag("@manual", tags);
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
        StepEventBus.getEventBus().setUniqueSession(systemConfiguration.shouldUseAUniqueBrowser());

        List<String> tags = getTagNamesFrom(feature.getTags());

        String requestedDriver = getDriverFrom(tags);
        if (isNotEmpty(requestedDriver)) {
            ThucydidesWebDriverSupport.useDefaultDriver(requestedDriver);
        }

//        if (StringUtils.isNotEmpty(requestedDriver)) {
//            ThucydidesWebDriverSupport.initialize(requestedDriver);
//        } else {
//            ThucydidesWebDriverSupport.initialize();
//        }
        uniqueBrowserTag = getUniqueBrowserTagFrom(tags);
    }

    private List<String> getTagNamesFrom(List<Tag> tags) {
        List<String> tagNames = Lists.newArrayList();
        for (Tag tag : tags) {
            tagNames.add(tag.getName());
        }
        return tagNames;
    }

    private String getDriverFrom(List<String> tags) {
        String requestedDriver = null;
        for (String tag : tags) {
            if (tag.startsWith("@driver:")) {
                requestedDriver = tag.substring(8);
            }
        }
        return requestedDriver;
    }

    private boolean getUniqueBrowserTagFrom(List<String> tags) {
        for (String tag : tags) {
            if (tag.equalsIgnoreCase("@uniqueBrowser")) {
                return true;
            }
        }
        return false;
    }


    boolean addingScenarioOutlineSteps = false;

    @Override
    public void scenarioOutline(ScenarioOutline scenarioOutline) {
        addingScenarioOutlineSteps = true;
//        startScenario(scenarioOutline.getName(), scenarioOutline.getDescription(), scenarioOutline.getTags());
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

    private String scenarioIdFrom(String scenarioIdOrExampleId) {
        String[] idElements = scenarioIdOrExampleId.split(";");
        return (idElements.length >= 2) ? String.format("%s;%s", defaultFeatureId, idElements[1]) : "";
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
            Map<String, String> rowValues = Maps.newLinkedHashMap();
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
        Map<String, String> row = new LinkedHashMap<>();
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

        boolean newScenario = !scenarioIdFrom(scenario.getId()).equals(currentScenario);
        currentScenario = scenarioIdFrom(scenario.getId());

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
        StepEventBus.getEventBus().setTestSource(StepEventBus.TEST_SOURCE_CUCUMBER);
        StepEventBus.getEventBus().testStarted(scenario.getName());
        StepEventBus.getEventBus().addDescriptionToCurrentTest(scenario.getDescription());
        StepEventBus.getEventBus().addTagsToCurrentTest(convertCucumberTags(currentFeature.getTags()));
        StepEventBus.getEventBus().addTagsToCurrentTest(convertCucumberTags(scenario.getTags()));

        registerFeatureJiraIssues(currentFeature.getTags());
        registerScenarioJiraIssues(scenario.getTags());

        checkForSkipped(currentFeature);
        checkForPending(currentFeature);
    }

    private void registerFeatureJiraIssues(List<Tag> tags) {
        List<String> issues = extractJiraIssueTags(tags);
        if (!issues.isEmpty()) {
            StepEventBus.getEventBus().addIssuesToCurrentStory(issues);
        }
    }

    private void registerScenarioJiraIssues(List<Tag> tags) {
        List<String> issues = extractJiraIssueTags(tags);
        if (!issues.isEmpty()) {
            StepEventBus.getEventBus().addIssuesToCurrentTest(issues);
        }
    }

    private List<TestTag> convertCucumberTags(List<Tag> cucumberTags) {
        List<TestTag> tags = Lists.newArrayList();
        for (Tag tag : cucumberTags) {
            tags.add(TestTag.withValue(tag.getName().substring(1)));
        }
        return ImmutableList.copyOf(tags);
    }

    private List<String> extractJiraIssueTags(List<Tag> cucumberTags) {
        List<String> issues = Lists.newArrayList();
        for (Tag tag : cucumberTags) {
            if(tag.getName().startsWith("@issue:")) {
                String tagIssueValue = tag.getName().substring("@issue:".length());
                issues.add(tagIssueValue);
            }
            if(tag.getName().startsWith("@issues:")) {
                String tagIssuesValues = tag.getName().substring("@issues:".length());
                issues.addAll(Arrays.asList(tagIssuesValues.split(",")));
            }
        }
        return issues;
    }

    @Override
    public void endOfScenarioLifeCycle(Scenario scenario) {
        if (examplesRunning) {
            finishExample();
        } else {
            generateReports();
        }
        if (!useUniqueBrowser(scenario)) {
            ThucydidesWebDriverSupport.closeAllDrivers();
        }
    }

    private boolean useUniqueBrowser(Scenario scenario) {
        return (uniqueBrowserTag
                || ThucydidesSystemProperty.THUCYDIDES_USE_UNIQUE_BROWSER.booleanFrom(systemConfiguration.getEnvironmentVariables(), false)
                || useUniqueBrowserForThisScenario(scenario));
    }

    private boolean useUniqueBrowserForThisScenario(Scenario scenario) {
        return getUniqueBrowserTagFrom(getTagNamesFrom(scenario.getTags()));
    }


    private void startExample() {
        Map<String, String> data = exampleRows.get(currentExample);
//        getThucydidesListeners().withDriver(ThucydidesWebDriverSupport.getDriver());
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
        configureDriver(currentFeature);
        clearScenarioResult();
        checkForPendingScenario(scenario.getTags());
        checkForSkippedScenario(scenario.getTags());
        checkForManualScenario(scenario.getTags());
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
        Step currentStep = stepQueue.poll();
        if (Result.PASSED.equals(result.getStatus())) {
            StepEventBus.getEventBus().stepFinished();
        } else if (Result.FAILED.equals(result.getStatus())) {
            failed(stepTitleFrom(currentStep), result.getError());
        } else if (Result.SKIPPED.equals(result)) {
            StepEventBus.getEventBus().stepIgnored();
        } else if (PENDING_STATUS.equals(result.getStatus())) {
            StepEventBus.getEventBus().stepPending();
        }
        else if (Result.UNDEFINED.equals(result)) {
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
        Throwable rootCause = new RootCauseAnalyzer(cause).getRootCause().toException();
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
        ThucydidesWebDriverSupport.clearDefaultDriver();
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
        StepEventBus.getEventBus().stepStarted(ExecutedStepDescription.withTitle("message"));
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
