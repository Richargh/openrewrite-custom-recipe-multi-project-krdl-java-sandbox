package de.richargh.sandbox.openrewrite.gitlabci;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class GitLabCiPropertyOrderRecipe extends Recipe {

    static final List<String> DEFAULT_GLOBAL_ORDER = List.of(
            "include",
            "image",
            "services",
            "stages",
            "workflow",
            "variables",
            "default",
            "cache",
            "before_script",
            "after_script"
    );

    static final List<String> DEFAULT_JOB_ORDER = List.of(
            "extends",
            "image",
            "services",
            "stage",
            "needs",
            "dependencies",
            "variables",
            "environment",
            "cache",
            "before_script",
            "script",
            "after_script",
            "artifacts",
            "rules",
            "only",
            "except",
            "when",
            "timeout",
            "retry",
            "tags",
            "allow_failure",
            "interruptible",
            "parallel",
            "trigger",
            "resource_group",
            "coverage",
            "release",
            "pages",
            "inherit"
    );

    @Option(
            displayName = "Global key order",
            description = "Ordered list of top-level GitLab CI keys. Keys found in the file but "
                    + "absent from this list are appended at the end in their original relative order.",
            required = false,
            example = """
                ["include", "image", "stages", "variables", "before_script"]"""
    )
    @Nullable
    private final List<String> globalKeyOrder;

    @Option(
            displayName = "Job key order",
            description = "Ordered list of keys within each job definition. Keys found in a job but "
                    + "absent from this list are appended at the end in their original relative order.",
            required = false,
            example = """
                ["extends", "image", "stage", "script", "artifacts"]"""
    )
    @Nullable
    private final List<String> jobKeyOrder;

    @JsonCreator
    public GitLabCiPropertyOrderRecipe(
            @Nullable @JsonProperty("globalKeyOrder") List<String> globalKeyOrder,
            @Nullable @JsonProperty("jobKeyOrder") List<String> jobKeyOrder) {
        this.globalKeyOrder = globalKeyOrder;
        this.jobKeyOrder = jobKeyOrder;
    }

    @Override
    public String getDisplayName() {
        return "Reorder GitLab CI YAML properties";
    }

    @Override
    public String getDescription() {
        return "Reorders top-level keys and job-level keys in `.gitlab-ci.yml` files "
                + "according to a configurable order for improved readability and consistency.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        List<String> effectiveGlobalOrder = globalKeyOrder != null ? globalKeyOrder : DEFAULT_GLOBAL_ORDER;
        List<String> effectiveJobOrder = jobKeyOrder != null ? jobKeyOrder : DEFAULT_JOB_ORDER;
        return Preconditions.check(
                new FindSourceFiles("**/.gitlab-ci.yml"),
                new GitLabCiPropertyOrderVisitor(effectiveGlobalOrder, effectiveJobOrder)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GitLabCiPropertyOrderRecipe other)) return false;
        return java.util.Objects.equals(globalKeyOrder, other.globalKeyOrder)
                && java.util.Objects.equals(jobKeyOrder, other.jobKeyOrder);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(globalKeyOrder, jobKeyOrder);
    }
}
