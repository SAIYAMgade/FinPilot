from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from models import InvestigationRequest, InvestigationReport
from workflow import MultiAgentInvestigationWorkflow

app = FastAPI(
    title="FinPilot AI Investigation Service",
    description="Multi-agent financial discrepancy investigation service with controlled, bounded tools and deterministic audit trails.",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

workflow = MultiAgentInvestigationWorkflow()

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "service": "FinPilot AI Investigation Service",
        "version": "1.0.0",
        "agents": [
            "InvestigationAgent",
            "EvidenceAgent",
            "RootCauseAgent",
            "PolicyValidationAgent",
            "ResolutionAgent"
        ]
    }

@app.post("/api/v1/investigate", response_model=InvestigationReport)
def investigate_exception(request: InvestigationRequest):
    try:
        report = workflow.run(request)
        return report
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Multi-agent investigation failed: {str(e)}")

@app.get("/api/v1/models")
def get_model_config():
    return {
        "orchestration_framework": "LangGraph State Machine",
        "prompt_version": "v2.1-compliance-strict",
        "temperature": 0.0,
        "max_tool_retries": 3,
        "guardrails": [
            "Prohibit direct financial ledger balance mutation",
            "Mandate human-in-the-loop approval checkpoint",
            "Distinguish verified evidence from probabilistic hypothesis",
            "Require explicit evidence ID citations"
        ]
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
