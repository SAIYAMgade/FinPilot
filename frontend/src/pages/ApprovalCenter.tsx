import React, { useEffect, useState } from 'react';
import { ShieldCheck, CheckCircle2, XCircle, AlertCircle, Clock, ArrowRight, UserCheck } from 'lucide-react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

export const ApprovalCenter: React.FC = () => {
  const [requests, setRequests] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedRequest, setSelectedRequest] = useState<any>(null);
  const [comments, setComments] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const { user, quickLoginAs } = useAuth();

  const fetchRequests = async () => {
    setLoading(true);
    try {
      const res = await api.get('/approvals');
      if (res.data?.success) {
        setRequests(res.data.data || []);
      }
    } catch (e) {
      console.error('Failed to load approval requests', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  const handleDecision = async (decision: 'APPROVED' | 'REJECTED') => {
    if (!selectedRequest) return;
    setActionLoading(true);
    setActionError(null);
    try {
      await api.post(`/approvals/${selectedRequest.id}/decide`, {
        decision,
        comments: comments || (decision === 'APPROVED' ? 'Approved after review' : 'Rejected after review')
      });
      setSelectedRequest(null);
      setComments('');
      fetchRequests();
    } catch (err: any) {
      setActionError(err.response?.data?.message || 'Decision failed');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-100 tracking-tight">Maker-Checker Approval Center</h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Strict two-party financial governance: Authorize or reject proposed adjustments, ledger corrections, and write-offs
          </p>
        </div>
      </div>

      {/* Info Callout */}
      <div className="p-4 rounded-xl bg-purple-500/10 border border-purple-500/20 text-xs text-purple-300 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <ShieldCheck className="w-5 h-5 shrink-0" />
          <span>
            <strong>Separation of Duties Protocol:</strong> In accordance with financial audit controls, the analyst who proposed an adjustment cannot approve it. Independent authorization by a Finance Approver is mandatory.
          </span>
        </div>
      </div>

      {/* Requests Table */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden">
        <table className="w-full text-left text-xs text-slate-300">
          <thead className="bg-slate-950/80 text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800 font-sans">
            <tr>
              <th className="py-3 px-4">Request #</th>
              <th className="py-3 px-4">Proposed Action</th>
              <th className="py-3 px-4 text-right">Adjustment Amount</th>
              <th className="py-3 px-4">Submitter (Maker)</th>
              <th className="py-3 px-4">Rationale</th>
              <th className="py-3 px-4">Status</th>
              <th className="py-3 px-4 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-mono">
            {loading ? (
              <tr>
                <td colSpan={7} className="py-12 text-center text-slate-500 font-sans">
                  Loading authorization queue...
                </td>
              </tr>
            ) : requests.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-12 text-center text-slate-500 font-sans">
                  No adjustment requests pending authorization.
                </td>
              </tr>
            ) : (
              requests.map((req) => {
                const isSubmitter = user?.id === req.requestedBy?.id || user?.username === req.requestedBy?.username;
                const canApprove = user?.roles?.includes('FINANCE_APPROVER') || user?.roles?.includes('ADMIN');

                return (
                  <tr key={req.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="py-3 px-4 font-bold text-slate-100">{req.requestNumber}</td>
                    <td className="py-3 px-4 font-sans text-slate-300">
                      {req.proposal?.proposedAction?.replace(/_/g, ' ')}
                    </td>
                    <td className="py-3 px-4 text-right font-bold text-slate-100">
                      ₹{Number(req.proposal?.adjustmentAmount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </td>
                    <td className="py-3 px-4 font-sans text-slate-300">
                      {req.requestedBy?.fullName}
                      {isSubmitter && <span className="ml-1 text-[10px] text-amber-400">(You)</span>}
                    </td>
                    <td className="py-3 px-4 font-sans text-slate-400 max-w-xs truncate" title={req.proposal?.rationale}>
                      {req.proposal?.rationale}
                    </td>
                    <td className="py-3 px-4 font-sans">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                          req.status === 'APPROVED'
                            ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                            : req.status === 'REJECTED'
                            ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                            : 'bg-amber-500/10 text-amber-400 border border-amber-500/20 animate-pulse'
                        }`}
                      >
                        {req.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right font-sans">
                      {req.status === 'PENDING' ? (
                        <button
                          onClick={() => {
                            setSelectedRequest(req);
                            setActionError(null);
                          }}
                          className="px-3 py-1 bg-brand-500 hover:bg-brand-600 text-slate-950 font-bold text-xs rounded transition-all"
                        >
                          Review &amp; Decide
                        </button>
                      ) : (
                        <span className="text-slate-500 text-[11px]">Decided</span>
                      )}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Decision Modal */}
      {selectedRequest && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg p-6 shadow-2xl">
            <h2 className="text-base font-bold text-slate-100 mb-1">Authorization Decision: {selectedRequest.requestNumber}</h2>
            <p className="text-xs text-slate-400 mb-4">
              Review proposed financial mutation before approving deterministic execution.
            </p>

            {/* Submitter conflict check */}
            {(user?.id === selectedRequest.requestedBy?.id || user?.username === selectedRequest.requestedBy?.username) && (
              <div className="mb-4 p-3 bg-amber-500/10 border border-amber-500/30 rounded-xl text-amber-300 text-xs">
                <strong>Separation of Duties Alert:</strong> You are logged in as the submitter (<span className="font-mono">{user?.username}</span>). You cannot approve your own request.
                <button
                  type="button"
                  onClick={() => quickLoginAs('approver')}
                  className="mt-2 block w-full py-1.5 bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 font-bold rounded text-[11px] text-center"
                >
                  Switch to Approver Persona (Sarah Jenkins)
                </button>
              </div>
            )}

            {actionError && (
              <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-400 text-xs">
                {actionError}
              </div>
            )}

            <div className="bg-slate-950 p-4 rounded-xl space-y-2 text-xs mb-4">
              <div className="flex justify-between">
                <span className="text-slate-400">Action:</span>
                <span className="text-slate-200 font-semibold">{selectedRequest.proposal?.proposedAction}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Adjustment Amount:</span>
                <span className="text-amber-400 font-mono font-bold">
                  ₹{Number(selectedRequest.proposal?.adjustmentAmount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Debit Account:</span>
                <span className="text-slate-200 font-mono">{selectedRequest.proposal?.debitAccount?.accountNumber} - {selectedRequest.proposal?.debitAccount?.accountName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Credit Account:</span>
                <span className="text-slate-200 font-mono">{selectedRequest.proposal?.creditAccount?.accountNumber} - {selectedRequest.proposal?.creditAccount?.accountName}</span>
              </div>
              <div className="pt-2 border-t border-slate-800 text-slate-300">
                <div className="text-slate-500 mb-0.5">Rationale:</div>
                {selectedRequest.proposal?.rationale}
              </div>
            </div>

            <div className="mb-4">
              <label className="block text-xs font-semibold text-slate-300 mb-1">Approver Review Comments</label>
              <textarea
                value={comments}
                onChange={(e) => setComments(e.target.value)}
                placeholder="Verified against settlement statement batch..."
                className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-xs text-slate-200 h-20"
              />
            </div>

            <div className="flex items-center justify-between pt-3 border-t border-slate-800">
              <button
                type="button"
                onClick={() => setSelectedRequest(null)}
                className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold rounded-lg text-xs"
              >
                Cancel
              </button>
              <div className="flex items-center space-x-2">
                <button
                  type="button"
                  disabled={actionLoading}
                  onClick={() => handleDecision('REJECTED')}
                  className="px-4 py-2 bg-rose-500/20 hover:bg-rose-500/30 text-rose-300 border border-rose-500/40 font-bold rounded-lg text-xs"
                >
                  Reject Proposal
                </button>
                <button
                  type="button"
                  disabled={actionLoading}
                  onClick={() => handleDecision('APPROVED')}
                  className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold rounded-lg text-xs shadow-lg shadow-emerald-500/20"
                >
                  {actionLoading ? 'Executing...' : 'Authorize & Execute Adjustment'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
