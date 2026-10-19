
package com.tracepilot.investigation.agent;

import com.tracepilot.investigation.domain.EvidenceReference;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class EvidenceCitationValidator {

    private static final Pattern CITATION_PATTERN =
            Pattern.compile(
                    "\\b(?:LOG|DEP|HIST)-[A-Z0-9-]+\\b"
                            + "|\\bSVC-[A-Za-z0-9-]+\\b"
            );

    public CitationValidation validate(
            String analysis,
            List<EvidenceReference> evidence) {

        if (analysis == null || analysis.isBlank()
                || evidence == null || evidence.isEmpty()) {
            return new CitationValidation(
                    false, Set.of(), Set.of()
            );
        }

        Set<String> allowed = new HashSet<>();

        for (EvidenceReference reference : evidence) {
            allowed.add(reference.evidenceId());
        }

        Set<String> cited = new HashSet<>();

        var matcher = CITATION_PATTERN.matcher(analysis);

        while (matcher.find()) {
            cited.add(matcher.group());
        }

        Set<String> invalid = new HashSet<>(cited);
        invalid.removeAll(allowed);

        boolean valid =
                !cited.isEmpty() && invalid.isEmpty();

        return new CitationValidation(
                valid,
                Set.copyOf(cited),
                Set.copyOf(invalid)
        );
    }

    public record CitationValidation(
            boolean valid,
            Set<String> citedEvidenceIds,
            Set<String> invalidEvidenceIds
    ) {}
}
