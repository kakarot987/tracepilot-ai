
ALTER TABLE investigation_evidence
DROP CONSTRAINT chk_evidence_type;

ALTER TABLE investigation_evidence
ADD CONSTRAINT chk_evidence_type
CHECK (evidence_type IN (
    'LOG',
    'DEPLOYMENT',
    'SERVICE_INFO',
    'INCIDENT_HISTORY',
    'RUNBOOK_CHUNK'
));
