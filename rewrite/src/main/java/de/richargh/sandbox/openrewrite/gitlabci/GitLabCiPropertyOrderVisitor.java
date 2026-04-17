package de.richargh.sandbox.openrewrite.gitlabci;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class GitLabCiPropertyOrderVisitor extends YamlIsoVisitor<ExecutionContext> {

    private static final Set<String> GITLAB_GLOBAL_KEYS = Set.of(
            "include", "image", "services", "stages", "workflow",
            "variables", "default", "cache", "before_script", "after_script"
    );

    private final List<String> globalKeyOrder;
    private final List<String> jobKeyOrder;

    GitLabCiPropertyOrderVisitor(List<String> globalKeyOrder, List<String> jobKeyOrder) {
        this.globalKeyOrder = globalKeyOrder;
        this.jobKeyOrder = jobKeyOrder;
    }

    @Override
    public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
        // Let children be visited first (bottom-up), so nested job mappings
        // are reordered before we reorder the root mapping that contains them.
        Yaml.Mapping visited = super.visitMapping(mapping, ctx);

        if (isDocumentRootMapping()) {
            return reorderEntries(visited, globalKeyOrder);
        } else if (isJobMapping()) {
            return reorderEntries(visited, jobKeyOrder);
        }

        return visited;
    }

    private boolean isDocumentRootMapping() {
        Cursor parent = getCursor().getParent();
        return parent != null && parent.getValue() instanceof Yaml.Document;
    }

    private boolean isJobMapping() {
        Cursor parentCursor = getCursor().getParent();
        if (parentCursor == null || !(parentCursor.getValue() instanceof Yaml.Mapping.Entry entry)) {
            return false;
        }
        String key = entry.getKey().getValue();
        if (GITLAB_GLOBAL_KEYS.contains(key)) {
            return false;
        }
        Cursor grandparentCursor = parentCursor.getParent();
        if (grandparentCursor == null || !(grandparentCursor.getValue() instanceof Yaml.Mapping)) {
            return false;
        }
        Cursor greatGrandparentCursor = grandparentCursor.getParent();
        return greatGrandparentCursor != null
                && greatGrandparentCursor.getValue() instanceof Yaml.Document;
    }

    private Yaml.Mapping reorderEntries(Yaml.Mapping mapping, List<String> order) {
        List<Yaml.Mapping.Entry> original = mapping.getEntries();
        if (original.size() <= 1) {
            return mapping;
        }

        Map<String, Yaml.Mapping.Entry> byKey = new LinkedHashMap<>();
        List<String> unseenKeys = new ArrayList<>(); // keys not in `order`, in original order

        for (Yaml.Mapping.Entry entry : original) {
            String key = entry.getKey().getValue();
            byKey.put(key, entry);
            if (!order.contains(key)) {
                unseenKeys.add(key);
            }
        }

        List<String> desiredKeySequence = new ArrayList<>();
        for (String k : order) {
            if (byKey.containsKey(k)) {
                desiredKeySequence.add(k);
            }
        }
        desiredKeySequence.addAll(unseenKeys);

        List<String> originalPrefixes = original.stream()
                .map(Yaml.Mapping.Entry::getPrefix)
                .toList();

        List<Yaml.Mapping.Entry> reordered = new ArrayList<>(desiredKeySequence.size());
        for (int i = 0; i < desiredKeySequence.size(); i++) {
            Yaml.Mapping.Entry entry = byKey.get(desiredKeySequence.get(i));
            reordered.add(entry.withPrefix(originalPrefixes.get(i)));
        }

        List<String> originalKeySequence = original.stream()
                .map(e -> e.getKey().getValue())
                .toList();
        if (originalKeySequence.equals(desiredKeySequence)) {
            return mapping;
        }

        return mapping.withEntries(reordered);
    }
}
