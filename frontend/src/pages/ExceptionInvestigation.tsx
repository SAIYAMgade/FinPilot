import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Bot,
  ShieldCheck,
  AlertTriangle,
  CheckCircle2,
  Send,
  FileText,
  Clock,
  Sparkles,
  ArrowRight,
  ShieldAlert
} from 'lucide-react';
import api from '../services/api';

export const ExceptionInvestigation: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [exception, setException] = useState<any>(null);
  const [investigation, setInvestigation] = useState<any>(null);
  const [accounts, setAccounts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [runningAi, setRunningAi] = useState(false);

  // Proposal Modal State
  const [showProposalModal, setShowProposalModal] = useState(false);
  const [proposalForm, setProposalForm] = useState({
    proposedAction: 'POST_LEDGER_ADJUSTMENT',
    adjustmentAmount: '',
    debitAccountId: 'acc-5010',
    creditAccountId: 'acc-1030',
    rationale: '',
  });
  const [proposalSubmitting, setProposalSubmitting] = useState(false);
  const [proposalError, setProposalError] = useState<string | null>(null);

  // Comment State
  const [newComment, setNewComment] = useState('');

  const navigate = useNavigate();

  const fetchData = async () => {
    setLoading(true);
    try {
      const [excRes, accRes] = await Promise.all([
        api.get(`/exceptions/${id}`),
        api.get('/accounts')
      ]);
      if (excRes.data?.success) {
        setException(excRes.data.data);
        setProposalForm((prev) => ({
          ...prev,
          adjustmentAmount: excRes.data.data?.financialImpact || '1500.00',
          rationale: `Adjustment for discrepancy ${excRes.data.data?.exceptionNumber}: ${excRes.data.data?.description}`
        }));
      }
      if (accRes.data?.success) {
        setAccounts(accRes.data.data || []);
      }

      // Check if an existing investigation exists for this exception
      const invRes = await api.get('/ai/investigations');
      if (invRes.data?.success && invRes.data?.data) {
        const found = invRes.data.data.find((inv: any) => inv.exceptionId === id);
        if (found) setInvestigation(found);
      }
    } catch (e) {
      console.error('Failed to load exception details', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (id) fetchData();
  }, [id]);

  const handleRunAiInvestigation = async () => {
    setRunningAi(true);
    try {
      const res = await api.post('/ai/investigations', { exceptionId: id });
      if (res.data?.success) {
        setInvestigation(res.data.data);
        fetchData();
      }
    } catch (err: any) {
      alert(err.response?.data?.message || 'AI Investigation failed');
    } finally {
      setRunningAi(false);
    }
  };

  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim()) return;
    try {
      await api.post(`/exceptions/${id}/comments`, { comment: newComment });
      setNewComment('');
      fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to add comment');
    }
  };

  const handleSubmitProposal = async (e: React.FormEvent) => {
    e.preventDefault();
    setProposalSubmitting(true);
    setProposalError(null);
    try {
      const payload = {
        exceptionId: id,
        investigationId: investigation?.id || null,
        proposedAction: proposalForm.proposedAction,
        adjustmentAmount: parseFloat(proposalForm.adjustmentAmount),
        debitAccountId: proposalForm.debitAccountId,
        creditAccountId: proposalForm.creditAccountId,
        rationale: proposalForm.rationale,
      };
      await api.post('/approvals/proposals', payload);
      setShowProposalModal(false);
      navigate('/approvals');
    } catch (err: any) {
      setProposalError(err.response?.data?.message || 'Failed to submit proposal');
    } finally {
      setProposalSubmitting(false);
    }
  };

  if (loading) {
    return <div className="text-center py-20 text-slate-500 text-xs">Loading investigation environment...</div>;
  }

  if (!exception) {
    return <div className="text-center py-20 text-slate-400 text-xs">Exception not found.</div>;
  }

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Back button */}
      <Link to="/exceptions" className="inline-flex items-center space-x-1.5 text-xs text-slate-400 hover:text-slate-200">
        <ArrowLeft className="w-4 h-4" />
        <span>Back to Exception Queue</span>
      </Link>

      {/* Exception Header */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 flex items-start justify-between">
        <div>
          <div className="flex items-center space-x-3">
            <h1 className="text-xl font-bold font-mono text-slate-100">{exception.exceptionNumber}</h1>
            <span className="px-2.5 py-0.5 rounded text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">
              {exception.status}
            </span>
            <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-rose-500/20 text-rose-300">
              {exception.severity}
            </span>
          </div>
          <div className="text-xs text-slate-300 font-sans mt-2 max-w-2xl">{exception.description}</div>
        </div>

        <div className="text-right">
          <div className="text-xs text-slate-400 uppercase font-semibold">Discrepancy Variance</div>
          <div className="text-2xl font-extrabold text-amber-400 font-mono mt-0.5">
            ₹{Number(exception.financialImpact).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
        </div>
      </div>

      {/* AI Investigation Panel */}
      <div className="bg-slate-900/70 border border-slate-800 rounded-xl p-6 space-y-6 relative overflow-hidden">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 rounded-lg bg-brand-500/10 text-brand-400 border border-brand-500/20">
              <Bot className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-sm font-bold text-slate-100">LangGraph Multi-Agent Investigation Engine</h2>
              <p className="text-[11px] text-slate-400">Autonomous evidence correlation, hypothesis deduction, and policy validation</p>
            </div>
          </div>

          <button
            onClick={handleRunAiInvestigation}
            disabled={runningAi}
            className="px-4 py-2 bg-gradient-to-r from-brand-500 to-emerald-600 hover:from-brand-600 hover:to-emerald-700 disabled:opacity-50 text-slate-950 font-bold text-xs rounded-lg flex items-center space-x-2 shadow-lg shadow-brand-500/20 transition-all"
          >
            <Sparkles className={`w-4 h-4 ${runningAi ? 'animate-spin' : ''}`} />
            <span>{runningAi ? 'Agents Investigating...' : investigation ? 'Re-Run AI Investigation' : 'Launch AI Investigation'}</span>
          </button>
        </div>

        {/* Multi-Agent Execution Pipeline Flow */}
        {investigation && (
          <div className="space-y-6 pt-4 border-t border-slate-800/80">
            {/* Step Trace Timeline */}
            <div>
              <div className="text-xs font-bold text-slate-300 uppercase tracking-wider mb-3">Multi-Agent Execution Audit Trace</div>
              <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
                {[
                  { name: '1. Ingest', agent: 'InvestigationAgent', tool: 'parse_exception' },
                  { name: '2. Evidence', agent: 'EvidenceAgent', tool: 'query_records' },
                  { name: '3. Root Cause', agent: 'RootCauseAgent', tool: 'evaluate_hypotheses' },
                  { name: '4. Policy', agent: 'PolicyValidationAgent', tool: 'check_approval_matrix' },
                  { name: '5. Proposal', agent: 'ResolutionAgent', tool: 'construct_payload' }
                ].map((step, idx) => (
                  <div key={idx} className="bg-slate-950 border border-slate-800 p-3 rounded-lg text-xs space-y-1">
                    <div className="flex items-center justify-between text-[10px] text-slate-400">
                      <span>{step.name}</span>
                      <CheckCircle2 className="w-3.5 h-3.5 text-brand-400" />
                    </div>
                    <div className="font-semibold text-slate-200">{step.agent}</div>
                    <div className="text-[10px] text-slate-400 font-mono">{step.tool}</div>
                  </div>
                ))}
              </div>
            </div>

            {/* Investigation Synthesis Output */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* Left 2 cols: Synthesis & Root Cause */}
              <div className="md:col-span-2 space-y-4">
                <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-2">
                  <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Executive Synthesis</div>
                  <p className="text-xs text-slate-200 leading-relaxed">{investigation.summary}</p>
                </div>

                <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Deduce Probable Root Cause</span>
                    <span className="text-xs font-mono font-bold text-emerald-400">Confidence: {investigation.confidenceScore}%</span>
                  </div>
                  <p className="text-xs text-slate-200 font-medium leading-relaxed">{investigation.rootCauseAnalysis}</p>
                </div>

                <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-2">
                  <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Policy Compliance Verification</div>
                  <p className="text-xs text-slate-300 leading-relaxed font-mono text-[11px]">{investigation.policyValidationNotes}</p>
                </div>
              </div>

              {/* Right 1 col: Resolution Proposal & Action */}
              <div className="bg-slate-950/80 border border-brand-500/30 p-5 rounded-xl flex flex-col justify-between">
                <div>
                  <div className="text-[11px] font-bold text-brand-400 uppercase tracking-wider mb-2">Recommended Corrective Action</div>
                  <p className="text-xs text-slate-200 font-medium leading-relaxed mb-4">
                    {investigation.recommendedAction}
                  </p>
                  <div className="p-3 bg-brand-500/10 border border-brand-500/20 rounded-lg text-[11px] text-brand-300 mb-4">
                    Strict Governance: AI agents are prohibited from posting financial entries autonomously. Maker-Checker human approval required.
                  </div>
                </div>

                <button
                  onClick={() => setShowProposalModal(true)}
                  className="w-full py-2.5 bg-brand-500 hover:bg-brand-600 text-slate-950 font-bold text-xs rounded-lg flex items-center justify-center space-x-2 shadow-lg shadow-brand-500/25 transition-all"
                >
                  <ShieldCheck className="w-4 h-4" />
                  <span>Submit for Maker-Checker Approval</span>
                </button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Analyst Comments Thread */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-6 space-y-4">
        <h3 className="text-xs font-bold text-slate-200 uppercase tracking-wider">Analyst Investigation Notes &amp; Evidence Log</h3>
        <div className="space-y-3">
          {(exception.comments || []).length === 0 ? (
            <p className="text-xs text-slate-500">No notes posted yet. Use form below to record evidence.</p>
          ) : (
            (exception.comments || []).map((c: any) => (
              <div key={c.id} className="p-3 rounded-lg bg-slate-950 border border-slate-800 text-xs">
                <div className="flex items-center justify-between text-[11px] text-slate-400 mb-1">
                  <span className="font-bold text-slate-200">{c.author?.fullName || 'Analyst'}</span>
                  <span className="font-mono text-slate-500">{new Date(c.createdAt).toLocaleString()}</span>
                </div>
                <div className="text-slate-300">{c.commentText}</div>
              </div>
            ))
          )}
        </div>

        <form onSubmit={handleAddComment} className="flex gap-2 pt-2">
          <input
            type="text"
            required
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            placeholder="Add investigation evidence note, bank ticket reference..."
            className="flex-1 bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-brand-500"
          />
          <button
            type="submit"
            className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold rounded-lg flex items-center space-x-1"
          >
            <Send className="w-3.5 h-3.5" />
            <span>Post Note</span>
          </button>
        </form>
      </div>

      {/* Submit Proposal Modal */}
      {showProposalModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg p-6 shadow-2xl">
            <h2 className="text-base font-bold text-slate-100 mb-2">Submit Financial Adjustment Proposal</h2>
            <p className="text-xs text-slate-400 mb-4">
              Requires two-party separation of duties. A Finance Approver must independently validate and authorize this before posting.
            </p>

            {proposalError && (
              <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/30 rounded-lg text-rose-400 text-xs">
                {proposalError}
              </div>
            )}

            <form onSubmit={handleSubmitProposal} className="space-y-4 text-xs">
              <div>
                <label className="block font-semibold text-slate-300 mb-1">Proposed Action</label>
                <select
                  value={proposalForm.proposedAction}
                  onChange={(e) => setProposalForm({ ...proposalForm, proposedAction: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200"
                >
                  <option value="POST_LEDGER_ADJUSTMENT">Post Double-Entry Ledger Adjustment</option>
                  <option value="WRITE_OFF">Reconciliation Discrepancy Write-Off</option>
                  <option value="REVERSE_TRANSACTION">Transaction Reversal</option>
                </select>
              </div>

              <div>
                <label className="block font-semibold text-slate-300 mb-1">Adjustment Amount (INR)</label>
                <input
                  type="number"
                  step="0.01"
                  required
                  value={proposalForm.adjustmentAmount}
                  onChange={(e) => setProposalForm({ ...proposalForm, adjustmentAmount: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200 font-mono font-bold"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block font-semibold text-slate-300 mb-1">Debit Account</label>
                  <select
                    value={proposalForm.debitAccountId}
                    onChange={(e) => setProposalForm({ ...proposalForm, debitAccountId: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200"
                  >
                    {accounts.map((a) => (
                      <option key={a.id} value={a.id}>{a.accountNumber} - {a.accountName}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block font-semibold text-slate-300 mb-1">Credit Account</label>
                  <select
                    value={proposalForm.creditAccountId}
                    onChange={(e) => setProposalForm({ ...proposalForm, creditAccountId: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200"
                  >
                    {accounts.map((a) => (
                      <option key={a.id} value={a.id}>{a.accountNumber} - {a.accountName}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-300 mb-1">Business Rationale</label>
                <textarea
                  required
                  value={proposalForm.rationale}
                  onChange={(e) => setProposalForm({ ...proposalForm, rationale: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200 h-20"
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowProposalModal(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={proposalSubmitting}
                  className="px-4 py-2 bg-brand-500 hover:bg-brand-600 text-slate-950 font-bold rounded-lg shadow-lg shadow-brand-500/20"
                >
                  {proposalSubmitting ? 'Routing...' : 'Route for Approver Authorization'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
