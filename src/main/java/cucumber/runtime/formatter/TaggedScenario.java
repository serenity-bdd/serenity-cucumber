package cucumber.runtime.formatter;

import com.google.common.collect.ImmutableMap;
import gherkin.ast.Tag;
import net.thucydides.core.model.TestResult;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;


class TaggedScenario {
    private static final List<String> SKIPPED_TAGS = Arrays.asList("@skip", "@wip");
    private static final List<String> IGNORED_TAGS = Arrays.asList("@ignore", "@ignored");

    private static Map<String, TestResult> MANUAL_TEST_RESULTS =
            ImmutableMap.of(
                    "pass", TestResult.SUCCESS,
                    "passed", TestResult.SUCCESS,
                    "success", TestResult.SUCCESS,
                    "failure", TestResult.FAILURE,
                    "failed", TestResult.FAILURE
            );

    static boolean isPending(List<Tag> tags) {
        return hasTag("@pending", tags);
    }

    static boolean isManual(List<Tag> tags) {
        return tags.stream().anyMatch(tag -> tag.getName().toLowerCase().startsWith("@manual"));
    }

    static Optional<TestResult> manualResultDefinedIn(List<Tag> tags) {
        if (!isManual(tags)) {
            return Optional.empty();
        }
        Optional<Tag> manualTagWithResult = tags.stream().filter(tag -> tag.getName().toLowerCase().startsWith("@manual:")).findFirst();
        if (manualTagWithResult.isPresent()) {
            String result = manualTagWithResult.get().getName().substring(8);
            return Optional.of(MANUAL_TEST_RESULTS.getOrDefault(result.toLowerCase(), TestResult.PENDING));
        } else {
            return Optional.empty();
        }
    }

    static boolean isSkippedOrWIP(List<Tag> tags) {
        for (Tag tag : tags) {
            if (SKIPPED_TAGS.contains(tag.getName().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    static boolean isIgnored(List<Tag> tags) {
        for (Tag tag : tags) {
            if (IGNORED_TAGS.contains(tag.getName().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTag(String tagName, List<Tag> tags) {
        for (Tag tag : tags) {
            if (tag.getName().equalsIgnoreCase(tagName)) {
                return true;
            }
        }
        return false;
    }

}
