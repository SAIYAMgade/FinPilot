import pytest
from models import InvestigationRequest
from workflow import MultiAgentInvestigationWorkflow

def test_multi_agent_investigation_workflow():
    workflow = MultiAgentInvestigationWorkflow()
    req = InvestigationRequest(
        exception_id="exc-test-101",
        exception_number="EXP-999",
        category="AMOUNT_DISCREPANCY",
        severity="HIGH",
        financial_impact=1500.0,
        description="Bank settlement discrepancy of INR 1,500"
    )

    report = workflow.run(req)

    assert report.exception_id == "exc-test-101"
    assert report.exception_number == "EXP-999"
    assert report.confidence_score > 90.0
    assert report.requires_human_approval is True
    assert len(report.verified_evidence) >= 2
    assert len(report.agent_traces) == 5
    assert "Interchange & Gateway Fee Expense" in report.recommended_action

    agent_names = [t.agent_name for t in report.agent_traces]
    assert "InvestigationAgent" in agent_names
    assert "EvidenceAgent" in agent_names
    assert "RootCauseAgent" in agent_names
    assert "PolicyValidationAgent" in agent_names
    assert "ResolutionAgent" in agent_names

def test_evidence_citations_present():
    workflow = MultiAgentInvestigationWorkflow()
    req = InvestigationRequest(
        exception_id="exc-test-102",
        exception_number="EXP-1000",
        category="UNMATCHED_SETTLEMENT",
        severity="CRITICAL",
        financial_impact=12000.0,
        description="Unmatched external settlement batch"
    )

    report = workflow.run(req)
    evidence_ids = [e.evidence_id for e in report.verified_evidence]
    assert "EVID-TXN-01" in evidence_ids
    assert "EVID-STMT-02" in evidence_ids
