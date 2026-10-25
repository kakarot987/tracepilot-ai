#!/usr/bin/env python3
"""Apply six test-only regression cases; verify Maven; create one local commit.
Run from TracePilot AI repository root. Does not push automatically.
"""
from pathlib import Path
import subprocess
import sys

ROOT = Path.cwd()
FILES = {
    'tracepilot-backend/src/test/java/com/tracepilot/workflow/rootcause/RootCauseAgentTest.java': r'''
    @Test
    void rejectsCitationNotPresentInInvestigationEvenWhenOtherCitationsAreValid() {
        response("""
                {"hypotheses":[{"description":"Mixed citations",
                  "supportingEvidenceIds":["LOG-DB-001","LOG-OTHER-999"],
                  "contradictingEvidenceIds":[],"confidence":"MEDIUM"}],
                 "openQuestions":[],"sufficientEvidence":false}
                """);
        assertThrows(IllegalArgumentException.class, () -> agent.analyze(investigation()));
    }

    @Test
    void rejectsSameEvidenceAsSupportingAndContradicting() {
        response("""
                {"hypotheses":[{"description":"Contradictory attribution",
                  "supportingEvidenceIds":["LOG-DB-001"],
                  "contradictingEvidenceIds":["LOG-DB-001"],"confidence":"LOW"}],
                 "openQuestions":[],"sufficientEvidence":false}
                """);
        assertThrows(IllegalArgumentException.class, () -> agent.analyze(investigation()));
    }
''',
    'tracepilot-backend/src/test/java/com/tracepilot/workflow/reviewer/ReviewerAgentTest.java': r'''
    @Test
    void rejectsOutOfRangeHypothesisIndex() {
        mockResponse("""
                {"decision":"REJECT","claimReviews":[{"hypothesisIndex":1,
                  "supported":false,"evidenceIds":[],"explanation":"Unproven"}],
                 "unresolvedQuestions":[],"rationale":"Insufficient support"}
                """);
        assertThrows(IllegalArgumentException.class,
                () -> reviewer.review(supportedRootCause(), operationalEvidence()));
    }

    @Test
    void acceptsExplicitInsufficientEvidenceWhenHypothesisIsUnproven() {
        mockResponse("""
                {"decision":"INSUFFICIENT_EVIDENCE",
                 "claimReviews":[{"hypothesisIndex":0,"supported":false,
                   "evidenceIds":["LOG-DB-001"],"explanation":"Symptom does not prove cause"}],
                 "unresolvedQuestions":["What consumed the pool?"],
                 "rationale":"More causal evidence is needed"}
                """);
        var outcome = reviewer.review(supportedRootCause(), operationalEvidence());
        assertEquals(ReviewDecision.INSUFFICIENT_EVIDENCE, outcome.decision());
        assertFalse(outcome.claimReviews().get(0).supported());
    }
''',
    'tracepilot-backend/src/test/java/com/tracepilot/security/PromptRedactorTest.java': r'''
    @Test
    void masksAccessTokenAndRefreshToken() {
        String result = PromptRedactor.redact("access_token=abc123 refresh_token:xyz890");
        assertFalse(result.contains("abc123"));
        assertFalse(result.contains("xyz890"));
        assertTrue(result.contains("access_token=[REDACTED]"));
        assertTrue(result.contains("refresh_token:[REDACTED]"));
    }

    @Test
    void handlesNullInputWithoutThrowing() {
        assertNull(PromptRedactor.redact(null));
    }
''',
}

def run(*args):
    print('+', ' '.join(map(str, args)), flush=True)
    subprocess.run(args, cwd=ROOT, check=True)

def main():
    if not (ROOT / '.git').exists():
        sys.exit('Run this script from the tracepilot-ai repository root.')
    current_branch = subprocess.check_output(['git', 'branch', '--show-current'], cwd=ROOT, text=True).strip()
    if current_branch != 'feature/milestone-6':
        sys.exit(f'Expected feature/milestone-6, found {current_branch!r}.')
    status = subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT, text=True).strip()
    if status:
        sys.exit('Working tree is not clean. Commit/stash unrelated changes before running.')
    pending = {}
    for relative, snippet in FILES.items():
        file = ROOT / relative
        source = file.read_text(encoding='utf-8')
        if any('void ' + name + '(' in source for name in [
            line.strip().split('void ', 1)[1].split('(', 1)[0]
            for line in snippet.splitlines() if line.strip().startswith('void ')
        ]):
            sys.exit(f'Test method already exists in {relative}; no changes made.')
        last = source.rfind('}')
        if last < 0:
            sys.exit(f'Unexpected Java class format in {relative}; no changes made.')
        pending[file] = source[:last].rstrip() + '\n\n' + snippet.strip('\n') + '\n' + source[last:]
    for file, content in pending.items():
        file.write_text(content, encoding='utf-8')
        print(f'Updated {file.relative_to(ROOT)}', flush=True)
    backend = ROOT / 'tracepilot-backend'
    mvnw = backend / ('mvnw.cmd' if sys.platform.startswith('win') else 'mvnw')
    print('Running Maven clean verify; commit only after success.', flush=True)
    subprocess.run([str(mvnw), 'clean', 'verify'], cwd=backend, check=True)
    run('git', 'add', '--', *FILES.keys())
    run('git', 'commit', '-m', 'test(regression): batch agent validation and security edge cases')
    print('Success: one test-only commit created locally. Push with: git push origin feature/milestone-6')

if __name__ == '__main__':
    main()
