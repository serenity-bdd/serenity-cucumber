package cucumber.runtime.formatter;

import gherkin.ast.Tag;

import java.util.Arrays;
import java.util.List;


class TaggedScenario {
    private static final List<String> SKIPPED_TAGS = Arrays.asList("@skip", "@wip");
    private static final List<String> IGNORED_TAGS = Arrays.asList("@ignore", "@ignored");

    static boolean isPending(List<Tag> tags) {
        return hasTag("@pending", tags);
    }

    static boolean isManual(List<Tag> tags) {
        return hasTag("@manual", tags);
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
