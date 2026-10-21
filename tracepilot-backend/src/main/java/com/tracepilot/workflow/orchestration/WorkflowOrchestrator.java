
package com.tracepilot.workflow.orchestration;

import com.tracepilot.investigation.domain.InvestigationStatus;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.investigator.InvestigatorStepResult;
import com.tracepilot.workflow.investigator.InvestigatorWorkflowService;
import com.tracepilot.workflow.persistence.WorkflowLease;
import com.tracepilot.workflow.persistence.WorkflowLimits;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;
import com.tracepilot.workflow.reviewer.*;
import com.tracepilot.workflow.rootcause.*;
import com.tracepilot.workflow.triage.TriageWorkflowService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import com.tracepilot.workflow.reviewer.ReviewEvidenceProvider;
import com.tracepilot.workflow.persistence.WorkflowLeaseService;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@Profile("postgres")
@ConditionalOnBean(ReviewEvidenceProvider.class)
public class WorkflowOrchestrator {

    private static final int MAX_FOLLOW_UPS = 1;
    private static final Duration WORKFLOW_DEADLINE =
            WorkflowLimits.MAX_DURATION;

    private final WorkflowPersistenceService workflows;
    private final TriageWorkflowService triage;
    private final InvestigatorWorkflowService investigator;
    private final RootCauseWorkflowService rootCause;
    private final ReviewerWorkflowService reviewer;
    private final ReviewEvidenceProvider evidenceProvider;
    private final WorkflowLeaseService leases;

    public WorkflowOrchestrator(
            WorkflowPersistenceService workflows,
            TriageWorkflowService triage,
            InvestigatorWorkflowService investigator,
            RootCauseWorkflowService rootCause,
            ReviewerWorkflowService reviewer,
            ReviewEvidenceProvider evidenceProvider,
            WorkflowLeaseService leases) {

        this.workflows = Objects.requireNonNull(workflows);
        this.triage = Objects.requireNonNull(triage);
        this.investigator = Objects.requireNonNull(investigator);
        this.rootCause = Objects.requireNonNull(rootCause);
        this.reviewer = Objects.requireNonNull(reviewer);
        this.evidenceProvider =
                Objects.requireNonNull(evidenceProvider);
        this.leases = Objects.requireNonNull(leases);
    }

    public WorkflowExecutionResult execute(String incidentId) {

        var workflow = workflows.start(incidentId);
        WorkflowLease lease = leases.acquire(
                workflow.id(),
                UUID.randomUUID()
        ).orElseThrow(() ->
                new IllegalStateException(
                        "Workflow is already owned"));
        UUID id = workflow.id();

        int followUps = 0;
        RootCauseResult analysis = null;
        ReviewerResult review = null;
        FollowUpFocus nextFocus = null;

        try {
            checkDeadline(id,lease);

            triage.execute(id);

            // The adapter may itself finalize the workflow
            // if its execution fails.
            requireRunning(id);

            for (int pass = 0; pass <= MAX_FOLLOW_UPS; pass++) {

                checkDeadline(id,lease);

                InvestigatorStepResult investigation =
                        nextFocus == null
                                ? investigator.execute(id)
                                : investigator.execute(id, nextFocus);

                if (investigation.status()
                        != InvestigationStatus.COMPLETED) {

                    WorkflowStatus terminal = switch (
                            investigation.status()) {
                        case INSUFFICIENT_EVIDENCE ->
                                WorkflowStatus.INSUFFICIENT_EVIDENCE;
                        case TIMED_OUT -> WorkflowStatus.TIMED_OUT;
                        default -> WorkflowStatus.FAILED;
                    };

                    finishIfRunning(id, terminal,lease);
                    return result(id, followUps, analysis, review);
                }

                requireRunning(id);
                checkDeadline(id,lease);

                analysis = rootCause.execute(id, investigation);

                requireRunning(id);
                checkDeadline(id,lease);

                // This provider must enforce investigation-scoped
                // provenance and load actual source excerpts.
                var citedIds = analysis.hypotheses().stream()
                        .flatMap(h -> java.util.stream.Stream.concat(
                                h.supportingEvidenceIds().stream(),
                                h.contradictingEvidenceIds().stream()
                        ))
                        .distinct()
                        .toList();

                if (citedIds.isEmpty() || citedIds.size() > 12
                        || !investigation.evidenceIds().containsAll(citedIds)) {

                    finishIfRunning(
                            id, WorkflowStatus.INSUFFICIENT_EVIDENCE,lease);

                    return result(id, followUps, analysis, review);
                }

                var excerpts = evidenceProvider.load(
                        investigation.investigationRunId(),
                        citedIds
                );

                // Missing sources cannot justify approval.
                if (excerpts == null || excerpts.isEmpty()) {
                    finishIfRunning(
                            id, WorkflowStatus.INSUFFICIENT_EVIDENCE,lease);
                    return result(id, followUps, analysis, review);
                }
                var loadedIds = excerpts.stream()
                        .map(EvidenceExcerpt::evidenceId)
                        .collect(java.util.stream.Collectors.toSet());

                if (!loadedIds.containsAll(citedIds)) {
                    finishIfRunning(
                            id, WorkflowStatus.INSUFFICIENT_EVIDENCE,lease);

                    return result(id, followUps, analysis, review);
                }

                review = reviewer.execute(id, analysis, excerpts);

                requireRunning(id);
                checkDeadline(id,lease);

                switch (review.decision()) {
                    case ACCEPT -> {
                        workflows.finishWorkflow(
                                id, WorkflowStatus.COMPLETED,lease);
                        return result(id, followUps, analysis, review);
                    }

                    case FOLLOW_UP -> {
                        if (pass >= MAX_FOLLOW_UPS
                                || review.unresolvedQuestions().isEmpty()) {
                            workflows.finishWorkflow(
                                    id,
                                    WorkflowStatus.INSUFFICIENT_EVIDENCE,
                                    lease
                            );
                            return result(id, followUps, analysis, review);
                        }

                        // Validate before reserving the allowance.
                        FollowUpFocus focus = new FollowUpFocus(
                                review.unresolvedQuestions()
                        );

                        checkDeadline(id,lease);

                        // Database enforces at most one follow-up.
                        if (!workflows.reserveFollowUp(id,lease)) {
                            workflows.finishWorkflow(
                                    id,
                                    WorkflowStatus.INSUFFICIENT_EVIDENCE,
                                    lease
                            );
                            return result(id, followUps, analysis, review);
                        }

                        followUps = workflows.get(id).followUpsUsed();

                        // Next loop iteration uses this focus.
                        nextFocus = focus;
                    }

                    case INSUFFICIENT_EVIDENCE, REJECT -> {
                        workflows.finishWorkflow(
                                id,
                                WorkflowStatus.INSUFFICIENT_EVIDENCE,
                                lease);
                        return result(id, followUps, analysis, review);
                    }
                }
            }

            finishIfRunning(
                    id, WorkflowStatus.INSUFFICIENT_EVIDENCE,lease);

        } catch (RuntimeException ex) {

            try {
                var state = workflows.get(id);

                if (!state.status().isTerminal()
                        && state.currentRole() == null) {

                    WorkflowStatus status =
                            deadlineExceeded(state.startedAt())
                                    ? WorkflowStatus.TIMED_OUT
                                    : WorkflowStatus.FAILED;

                    workflows.finishWorkflow(id, status,lease);
                }
                // If an agent step remains active, leave it
                // for the dedicated recovery mechanism.

            } catch (RuntimeException cleanup) {
                ex.addSuppressed(cleanup);
            }

            throw ex;
        }

        return result(id, followUps, analysis, review);
    }

    private void requireRunning(UUID workflowId) {
        if (workflows.get(workflowId).status()
                != WorkflowStatus.RUNNING) {
            throw new IllegalStateException(
                    "Workflow is no longer running");
        }
    }

    private void checkDeadline(UUID workflowId, WorkflowLease lease) {
        var run = workflows.get(workflowId);

        if (deadlineExceeded(run.startedAt())) {
            finishIfRunning(workflowId, WorkflowStatus.TIMED_OUT, lease);
            throw new IllegalStateException(
                    "Workflow deadline exceeded");
        }

        if (run.status().isTerminal()) {
            throw new IllegalStateException(
                    "Workflow already finalized");
        }
    }

    private boolean deadlineExceeded(Instant startedAt) {
        return !Instant.now().isBefore(
                startedAt.plus(WORKFLOW_DEADLINE));
    }

    private void finishIfRunning(
            UUID workflowId, WorkflowStatus desired, WorkflowLease lease) {

        var run = workflows.get(workflowId);

        if (!run.status().isTerminal()
                && run.currentRole() == null) {
            workflows.finishWorkflow(workflowId, desired,lease);
        }
    }

    private WorkflowExecutionResult result(
            UUID workflowId,
            int followUps,
            RootCauseResult analysis,
            ReviewerResult review) {

        return new WorkflowExecutionResult(
                workflowId,
                workflows.get(workflowId).status(),
                followUps,
                analysis,
                review
        );
    }
}
