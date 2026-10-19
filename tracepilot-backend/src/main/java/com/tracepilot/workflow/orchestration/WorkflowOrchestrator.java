
package com.tracepilot.workflow.orchestration;

import com.tracepilot.investigation.domain.InvestigationStatus;
import com.tracepilot.workflow.domain.WorkflowStatus;
import com.tracepilot.workflow.investigator.InvestigatorStepResult;
import com.tracepilot.workflow.investigator.InvestigatorWorkflowService;
import com.tracepilot.workflow.persistence.WorkflowPersistenceService;
import com.tracepilot.workflow.reviewer.*;
import com.tracepilot.workflow.rootcause.*;
import com.tracepilot.workflow.triage.TriageWorkflowService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import com.tracepilot.workflow.reviewer.ReviewEvidenceProvider;

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
            Duration.ofMinutes(4);

    private final WorkflowPersistenceService workflows;
    private final TriageWorkflowService triage;
    private final InvestigatorWorkflowService investigator;
    private final RootCauseWorkflowService rootCause;
    private final ReviewerWorkflowService reviewer;
    private final ReviewEvidenceProvider evidenceProvider;

    public WorkflowOrchestrator(
            WorkflowPersistenceService workflows,
            TriageWorkflowService triage,
            InvestigatorWorkflowService investigator,
            RootCauseWorkflowService rootCause,
            ReviewerWorkflowService reviewer,
            ReviewEvidenceProvider evidenceProvider) {

        this.workflows = Objects.requireNonNull(workflows);
        this.triage = Objects.requireNonNull(triage);
        this.investigator = Objects.requireNonNull(investigator);
        this.rootCause = Objects.requireNonNull(rootCause);
        this.reviewer = Objects.requireNonNull(reviewer);
        this.evidenceProvider =
                Objects.requireNonNull(evidenceProvider);
    }

    public WorkflowExecutionResult execute(String incidentId) {

        var workflow = workflows.start(incidentId);
        UUID id = workflow.id();

        int followUps = 0;
        RootCauseResult analysis = null;
        ReviewerResult review = null;

        try {
            checkDeadline(id);

            triage.execute(id);

            // The adapter may itself finalize the workflow
            // if its execution fails.
            requireRunning(id);

            for (int pass = 0; pass <= MAX_FOLLOW_UPS; pass++) {

                checkDeadline(id);

                InvestigatorStepResult investigation =
                        investigator.execute(id);

                if (investigation.status()
                        != InvestigationStatus.COMPLETED) {

                    WorkflowStatus terminal = switch (
                            investigation.status()) {
                        case INSUFFICIENT_EVIDENCE ->
                                WorkflowStatus.INSUFFICIENT_EVIDENCE;
                        case TIMED_OUT -> WorkflowStatus.TIMED_OUT;
                        default -> WorkflowStatus.FAILED;
                    };

                    finishIfRunning(id, terminal);
                    return result(id, followUps, analysis, review);
                }

                requireRunning(id);
                checkDeadline(id);

                analysis = rootCause.execute(id, investigation);

                requireRunning(id);
                checkDeadline(id);

                // This provider must enforce investigation-scoped
                // provenance and load actual source excerpts.
                var excerpts = evidenceProvider.load(
                        investigation.investigationRunId(),
                        investigation.evidenceIds()
                );

                // Missing sources cannot justify approval.
                if (excerpts == null || excerpts.isEmpty()) {
                    finishIfRunning(
                            id, WorkflowStatus.INSUFFICIENT_EVIDENCE);
                    return result(id, followUps, analysis, review);
                }

                review = reviewer.execute(id, analysis, excerpts);

                requireRunning(id);
                checkDeadline(id);

                switch (review.decision()) {
                    case ACCEPT -> {
                        workflows.finishWorkflow(
                                id, WorkflowStatus.COMPLETED);
                        return result(id, followUps, analysis, review);
                    }

                    case FOLLOW_UP -> {
                        if (pass >= MAX_FOLLOW_UPS
                                || review.unresolvedQuestions().isEmpty()) {
                            workflows.finishWorkflow(
                                    id,
                                    WorkflowStatus.INSUFFICIENT_EVIDENCE);
                            return result(
                                    id, followUps, analysis, review);
                        }

                        // Step 7 persistence enhancement will
                        // reserve this allowance atomically.
                        if (!workflows.reserveFollowUp(id)) {
                            workflows.finishWorkflow(
                                    id, WorkflowStatus.INSUFFICIENT_EVIDENCE);
                            return result(id, followUps, analysis, review);
                        }

                        followUps = workflows.get(id).followUpsUsed();
                    }

                    case INSUFFICIENT_EVIDENCE, REJECT -> {
                        workflows.finishWorkflow(
                                id,
                                WorkflowStatus.INSUFFICIENT_EVIDENCE);
                        return result(id, followUps, analysis, review);
                    }
                }
            }

            finishIfRunning(
                    id, WorkflowStatus.INSUFFICIENT_EVIDENCE);

        } catch (RuntimeException ex) {

            try {
                var state = workflows.get(id);

                if (!state.status().isTerminal()
                        && state.currentRole() == null) {

                    WorkflowStatus status =
                            deadlineExceeded(state.startedAt())
                                    ? WorkflowStatus.TIMED_OUT
                                    : WorkflowStatus.FAILED;

                    workflows.finishWorkflow(id, status);
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

    private void checkDeadline(UUID workflowId) {
        var run = workflows.get(workflowId);

        if (deadlineExceeded(run.startedAt())) {
            finishIfRunning(workflowId, WorkflowStatus.TIMED_OUT);
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
            UUID workflowId, WorkflowStatus desired) {

        var run = workflows.get(workflowId);

        if (!run.status().isTerminal()
                && run.currentRole() == null) {
            workflows.finishWorkflow(workflowId, desired);
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
