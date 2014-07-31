package cucumber.api.thucydides;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import cucumber.runtime.StepDefinitionMatch;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.*;
import net.thucydides.core.ThucydidesListeners;
import net.thucydides.core.ThucydidesReports;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestTag;
import net.thucydides.core.reports.ReportService;
import net.thucydides.core.steps.BaseStepListener;
import net.thucydides.core.steps.ExecutedStepDescription;
import net.thucydides.core.steps.StepEventBus;
import net.thucydides.core.steps.StepFailure;
import net.thucydides.core.webdriver.Configuration;
import net.thucydides.core.webdriver.ThucydidesWebDriverSupport;
import org.apache.commons.lang3.StringUtils;
import org.junit.internal.AssumptionViolatedException;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static ch.lambdaj.Lambda.*;

/**
 * Generates Thucydides reports.
 *
 * @author L.Carausu (liviu.carausu@gmail.com)
 */
public class ThucydidesReporter implements Formatter, Reporter {


    private static final String OPEN_PARAM_CHAR = "\uff5f";
    private static final String CLOSE_PARAM_CHAR = "\uff60";

    private final Queue<Step> stepQueue;

    private Configuration systemConfiguration;

    private ThreadLocal<ThucydidesListeners> thucydidesListenersThreadLocal;

    private final List<BaseStepListener> baseStepListeners;

    private Feature currentFeature;


    public ThucydidesReporter(Configuration systemConfiguration)
    {
        this.systemConfiguration = systemConfiguration;
        this.stepQueue = new LinkedList<>();
        thucydidesListenersThreadLocal = new ThreadLocal<>();
        baseStepListeners = Lists.newArrayList();

    }

    protected ThucydidesListeners getThucydidesListeners() {
        if (thucydidesListenersThreadLocal.get() == null) {
            ThucydidesListeners listeners = ThucydidesReports.setupListeners(systemConfiguration);
            thucydidesListenersThreadLocal.set(listeners);
            synchronized (baseStepListeners) {
                baseStepListeners.add(listeners.getBaseStepListener());
            }
        }
        return thucydidesListenersThreadLocal.get();
    }

    protected ReportService getReportService() {
        return ThucydidesReports.getReportService(systemConfiguration);
    }

    @Override
    public void syntaxError(String state, String event, List<String> legalEvents, String uri, Integer line) {}

    @Override
    public void uri(String uri) {

    }

    @Override
    public void feature(Feature feature)
    {
        if(currentFeature != null)
        {
            StepEventBus.getEventBus().testSuiteFinished();
        }
        currentFeature =  feature;
        System.out.println("ThucydidesReporter:Feature called " + feature.getName());
        configureDriver(feature);
        getThucydidesListeners().withDriver(ThucydidesWebDriverSupport.getDriver());
        net.thucydides.core.model.Story userStory = net.thucydides.core.model.Story.withId(feature.getName(), feature.getId());
        StepEventBus.getEventBus().testSuiteStarted(userStory);

    }

    private void configureDriver(Feature feature) {
        StepEventBus.getEventBus().setUniqueSession(systemConfiguration.getUseUniqueBrowser());
        String requestedDriver = null; // TODO get from Feature eventually
        if (StringUtils.isNotEmpty(requestedDriver)) {
            ThucydidesWebDriverSupport.initialize(requestedDriver);
        } else {
            ThucydidesWebDriverSupport.initialize();
        }
    }


    @Override
    public void scenarioOutline(ScenarioOutline scenarioOutline) {
    }

    @Override
    public void examples(Examples examples) {

    }

    @Override
    public void startOfScenarioLifeCycle(Scenario scenario) {
         System.out.println("ThucydidesReporter:startofScenarioLifeCycle called " + scenario.getName());
         StepEventBus.getEventBus().testStarted(scenario.getName());
         StepEventBus.getEventBus().addTagsToCurrentTest(ImmutableList.of(tagForCurrentFeature()));
         getThucydidesListeners().withDriver(ThucydidesWebDriverSupport.getDriver());
    }

    private TestTag tagForCurrentFeature() {
        return TestTag.withName(currentFeature.getName()).andType("feature");
    }

    @Override
    public void endOfScenarioLifeCycle(Scenario scenario) {
        System.out.println("ThucydidesReporter:endOfScenarioLifecycle called " + scenario.getName());
        generateReports();
    }

    @Override
    public void background(Background background) {
    }

    @Override
    public void scenario(Scenario scenario) {
    }

    @Override
    public void step(Step step) {
        stepQueue.add(step);

    }


    @Override
    public void done() {
        if(currentFeature != null)
        {
            StepEventBus.getEventBus().testSuiteFinished();
        }
    }

    @Override
    public void close() {

    }

    @Override
    public void eof() {

    }

    @Override
    public void before(Match match, Result result) {}

    @Override
    public void result(Result result) {
        Step currentStep = stepQueue.poll();
        System.out.println("Result " + result.getStatus() + " for step " + currentStep.getName());
        if (Result.PASSED.equals(result.getStatus())) {
            StepEventBus.getEventBus().stepFinished();
        } else if (Result.FAILED.equals(result.getStatus())) {
            failed(stepTitleFrom(currentStep),result.getError());
        } else if (Result.SKIPPED.equals(result)) {
            StepEventBus.getEventBus().stepIgnored();
        } else if (Result.UNDEFINED.equals(result)) {
            StepEventBus.getEventBus().stepStarted(ExecutedStepDescription.withTitle(stepTitleFrom(currentStep)));
            StepEventBus.getEventBus().stepPending();
        }

        if (stepQueue.isEmpty()) {
            StepEventBus.getEventBus().testFinished();
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
    public void after(Match match, Result result) {}

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
        return currentStep.getKeyword() + currentStep.getName();
    }

    @Override
    public void embedding(String mimeType, byte[] data) {}

    @Override
    public void write(String text) {}

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
