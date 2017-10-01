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
import net.serenitybdd.cucumber.model.FeatureFileContents;
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
import java.util.stream.Collectors;

import static net.serenitybdd.cucumber.TaggedScenario.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Serenity/Cucumber integration
 *
 * @author L.Carausu (liviu.carausu@gmail.com)
 */
public class SerenityReporter implements Formatter, Reporter {

    private static final String OPEN_PARAM_CHAR = "\uff5f";
    private static final String CLOSE_PARAM_CHAR = "\uff60";

    public static final String PENDING_STATUS = "pending";
    private static final String SCENARIO_OUTLINE_NOT_KNOWN_YET = "";

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

    public SerenityReporter(Configuration systemConfiguration) {
        this.systemConfiguration = systemConfiguration;
        this.stepQueue = new LinkedList<>();
        thucydidesListenersThreadLocal = new ThreadLocal<>();
        baseStepListeners = Lists.newArrayList();
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

    private String uri;

    @Override
    public void uri(String uri) {
        currentUri = uri;
        String featuresRoot = File.separatorChar + FEATURES_ROOT_PATH + File.separatorChar;
        if (uri.contains(featuresRoot)) {
            currentUri = uri.substring(uri.lastIndexOf(featuresRoot) + FEATURES_ROOT_PATH.length() + 2);
        }
        defaultFeatureId = new File(currentUri).getName().replace(".feature", "");
        defaultFeatureName = Inflector.getInstance().humanize(defaultFeatureId);
        this.uri = uri;
    }

    FeatureFileContents featureFileContents() {
        return new FeatureFileContents(uri);
    }

    List<Tag> featureTags;

    @Override
    public void feature(Feature feature) {

        assureTestSuiteFinished();
        if (feature.getName().isEmpty()) {
            feature = featureWithDefaultName(feature, defaultFeatureName, defaultFeatureId);
        }

        currentFeature = feature;
        featureTags = ImmutableList.copyOf(feature.getTags());

        configureDriver(feature);
        getThucydidesListeners();
        Story userStory = Story.withIdAndPath(feature.getId(), feature.getName(), currentUri).asFeature();

        if (!isEmpty(feature.getDescription())) {
            userStory = userStory.withNarrative(feature.getDescription());
        }
        StepEventBus.getEventBus().testSuiteStarted(userStory);
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

    private void configureDriver(Feature feature) {
        StepEventBus.getEventBus().setUniqueSession(systemConfiguration.shouldUseAUniqueBrowser());

        List<String> tags = getTagNamesFrom(feature.getTags());

        String requestedDriver = getDriverFrom(tags);
        if (isNotEmpty(requestedDriver)) {
            ThucydidesWebDriverSupport.useDefaultDriver(requestedDriver);
        }
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

    boolean addingScenarioOutlineSteps = false;

    int scenarioOutlineStartsAt;
    int scenarioOutlineEndsAt;

    @Override
    public void scenarioOutline(ScenarioOutline scenarioOutline) {
        addingScenarioOutlineSteps = true;
        scenarioOutlineStartsAt = scenarioOutline.getLine();
    }

    String currentScenarioId;

    @Override
    public void examples(Examples examples) {

        scenarioOutlineEndsAt = examples.getLine() - 1;

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
                thucydidesTableFrom(SCENARIO_OUTLINE_NOT_KNOWN_YET, headers, rows, examples.getName(), examples.getDescription())
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

    private DataTable thucydidesTableFrom(String scenarioOutline,
                                          List<String> headers,
                                          List<Map<String, String>> rows,
                                          String name,
                                          String description) {
        return DataTable.withHeaders(headers).andScenarioOutline(scenarioOutline).andMappedRows(rows).andTitle(name).andDescription(description).build();
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

    List<Tag> scenarioTags;

    private void startScenario(Scenario scenario) {
        StepEventBus.getEventBus().setTestSource(StepEventBus.TEST_SOURCE_CUCUMBER);
        StepEventBus.getEventBus().testStarted(scenario.getName(), scenario.getId());
        StepEventBus.getEventBus().addDescriptionToCurrentTest(scenario.getDescription());
        StepEventBus.getEventBus().addTagsToCurrentTest(convertCucumberTags(currentFeature.getTags()));
        StepEventBus.getEventBus().addTagsToCurrentTest(convertCucumberTags(scenario.getTags()));

        registerFeatureJiraIssues(currentFeature.getTags());
        registerScenarioJiraIssues(scenario.getTags());

        scenarioTags = tagsForScenario(scenario);

        updateResultsFromTagsIn(scenarioTags);
    }

    private List<Tag> tagsForScenario(Scenario scenario) {
        List<Tag> scenarioTags = new ArrayList<>(featureTags);
        scenarioTags.addAll(scenario.getTags());
        return scenarioTags;
    }

    private void updateResultsFromTagsIn(List<Tag> tags) {
        if (isManual(tags)) {
            StepEventBus.getEventBus().testIsManual();
        }

        if (isPending(tags)) {
            StepEventBus.getEventBus().testPending();
            StepEventBus.getEventBus().getBaseStepListener().overrideResultTo(TestResult.PENDING);
        }
        if (isSkippedOrWIP(tags)) {
            StepEventBus.getEventBus().testSkipped();
            StepEventBus.getEventBus().getBaseStepListener().overrideResultTo(TestResult.SKIPPED);
        }
        if (isIgnored(tags)) {
            StepEventBus.getEventBus().testIgnored();
            StepEventBus.getEventBus().getBaseStepListener().overrideResultTo(TestResult.IGNORED);
        }
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
            if (tag.getName().startsWith("@issue:")) {
                String tagIssueValue = tag.getName().substring("@issue:".length());
                issues.add(tagIssueValue);
            }
            if (tag.getName().startsWith("@issues:")) {
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

            String scenarioOutline = featureFileContents().trimmedContent()
                    .betweenLine(scenarioOutlineStartsAt)
                    .and(scenarioOutlineEndsAt);

            table.setScenarioOutline(scenarioOutline);

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

        recordStepResult(result, currentStep);

        if (stepQueue.isEmpty()) {
            recordFinalResult();
        }
    }

    private void recordFinalResult() {
        if (waitingToProcessBackgroundSteps) {
            waitingToProcessBackgroundSteps = false;
        } else {
            if (examplesRunning) { //finish enclosing step because testFinished resets the queue
                StepEventBus.getEventBus().stepFinished();
            }

            updateResultFromTags(scenarioTags);
        }
    }

    private void updateResultFromTags(List<Tag> scenarioTags) {
        if (isManual(scenarioTags)) {
            StepEventBus.getEventBus().testIsManual();
        }

        if (isPending(scenarioTags)) {
            StepEventBus.getEventBus().testPending();
        }

        if (isSkippedOrWIP(scenarioTags)) {
            StepEventBus.getEventBus().testSkipped();
            StepEventBus.getEventBus().getBaseStepListener().overrideResultTo(TestResult.SKIPPED);
        }

        if (isIgnored(scenarioTags)) {
            StepEventBus.getEventBus().testIgnored();
            StepEventBus.getEventBus().getBaseStepListener().overrideResultTo(TestResult.IGNORED);
        }
    }

    private void recordStepResult(Result result, Step currentStep) {
        switch (result.getStatus()) {
            case Result.PASSED:
                StepEventBus.getEventBus().stepFinished();
                break;

            case Result.FAILED:
                failed(stepTitleFrom(currentStep), result.getError());
                break;

            case "skipped":
                StepEventBus.getEventBus().stepIgnored();
                break;

            case "pending":
                StepEventBus.getEventBus().stepPending();
                break;
            default:
                StepEventBus.getEventBus().stepStarted(ExecutedStepDescription.withTitle(stepTitleFrom(currentStep)));
                StepEventBus.getEventBus().stepPending();
        }
    }

    public void failed(String stepTitle, Throwable cause) {

        if (!errorOrFailureRecordedForStep(stepTitle, cause)) {
            StepEventBus.getEventBus().updateCurrentStepTitle(stepTitle);
            Throwable rootCause = new RootCauseAnalyzer(cause).getRootCause().toException();

            if (isAssumptionFailure(rootCause)) {
                StepEventBus.getEventBus().assumptionViolated(rootCause.getMessage());
            } else {
                StepEventBus.getEventBus().stepFailed(new StepFailure(ExecutedStepDescription.withTitle(normalized(stepTitle)), rootCause));
            }
        }
    }

    private boolean errorOrFailureRecordedForStep(String stepTitle, Throwable cause) {
        if (!latestTestOutcome().isPresent()) {
            return false;
        }
        if (!latestTestOutcome().get().testStepWithDescription(stepTitle).isPresent()) {
            return false;
        }

        Optional<TestStep> matchingTestStep = latestTestOutcome().get().testStepWithDescription(stepTitle);
        if (matchingTestStep.isPresent() && matchingTestStep.get().getException() != null) {
            return (matchingTestStep.get().getException().getOriginalCause() == cause);
        }

        return false;
    }

    private Optional<TestOutcome> latestTestOutcome() {
        List<TestOutcome> recordedOutcomes = StepEventBus.getEventBus().getBaseStepListener().getTestOutcomes();
        return (recordedOutcomes.isEmpty()) ? Optional.<TestOutcome>absent()
                : Optional.of(recordedOutcomes.get(recordedOutcomes.size() - 1));
    }

    private boolean isAssumptionFailure(Throwable rootCause) {
        return (AssumptionViolatedException.class.isAssignableFrom(rootCause.getClass()));
    }

    @Override
    public void after(Match match, Result result) {
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
        return baseStepListeners.stream().map(BaseStepListener::getTestOutcomes).flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private String normalized(String value) {
        return value.replaceAll(OPEN_PARAM_CHAR, "{").replaceAll(CLOSE_PARAM_CHAR, "}");
    }
}
