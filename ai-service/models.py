from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime, timezone

class InvestigationRequest(BaseModel):
    exception_id: str
    exception_number: str
    category: str
    severity: str
    financial_impact: float
    description: str
    transaction_id: Optional[str] = None
    reconciliation_run_id: Optional[str] = None

class EvidenceItem(BaseModel):
    evidence_id: str
    source: str
    description: str
    confidence: float
    raw_data: Optional[Dict[str, Any]] = None

class AgentStepTrace(BaseModel):
    agent_name: str
    step_name: str
    tool_called: str
    tool_input: str
    tool_output: str
    duration_ms: int

class InvestigationReport(BaseModel):
    exception_id: str
    exception_number: str
    summary: str
    root_cause: str
    verified_evidence: List[EvidenceItem]
    inferred_explanation: str
    recommended_action: str
    confidence_score: float = Field(..., ge=0.0, le=100.0)
    missing_information: List[str]
    policy_notes: str
    requires_human_approval: bool = True
    evidence_payload: Dict[str, Any]
    agent_traces: List[AgentStepTrace]
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
