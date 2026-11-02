import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, ArrowLeftRight, Clock, ShieldCheck, CheckCircle2, AlertCircle, FileText, ChevronRight } from 'lucide-react';
import api from '../services/api';

export const TransactionDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [transaction, setTransaction] = useState<any>(null);
  const [events, setEvents] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [txnRes, eventsRes] = await Promise.all([
          api.get(`/transactions/${id}`),
          api.get(`/transactions/${id}/events`)
        ]);
        if (txnRes.data?.success) setTransaction(txnRes.data.data);
        if (eventsRes.data?.success) setEvents(eventsRes.data.data || []);
      } catch (e) {
        console.error('Failed to load transaction details', e);
      } finally {
        setLoading(false);
      }
    };
    if (id) fetchData();
  }, [id]);

  if (loading) {
    return <div className="text-center py-20 text-slate-500 text-xs">Loading transaction telemetry...</div>;
  }

  if (!transaction) {
    return (
      <div className="text-center py-20 text-slate-400 text-xs">
        <p>Transaction not found.</p>
        <Link to="/transactions" className="text-brand-400 font-semibold mt-2 inline-block">Return to transactions</Link>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Back button */}
      <Link to="/transactions" className="inline-flex items-center space-x-1.5 text-xs text-slate-400 hover:text-slate-200">
        <ArrowLeft className="w-4 h-4" />
        <span>Back to Ingestion Feed</span>
      </Link>

      {/* Header Card */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 flex items-start justify-between">
        <div>
          <div className="flex items-center space-x-3">
            <h1 className="text-xl font-bold font-mono text-slate-100">{transaction.id}</h1>
            <span className="px-2.5 py-0.5 rounded text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              {transaction.status}
            </span>
          </div>
          <div className="text-xs text-slate-400 mt-1">
            External Ref: <span className="font-mono text-slate-300">{transaction.externalReference || '—'}</span> &bull; Source: <span className="text-slate-300">{transaction.sourceSystem}</span>
          </div>
        </div>
        <div className="text-right">
          <div className="text-xs text-slate-400 uppercase font-semibold">Total Amount</div>
          <div className="text-2xl font-extrabold text-slate-100 font-mono mt-0.5">
            ₹{Number(transaction.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <div className="text-[11px] text-slate-400 mt-0.5">{transaction.currency}</div>
        </div>
      </div>

      {/* Core Properties Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5 space-y-3 text-xs">
          <h2 className="text-xs font-bold text-slate-200 uppercase tracking-wider mb-2">Technical Properties</h2>
          <div className="flex justify-between py-1 border-b border-slate-800/60">
            <span className="text-slate-400">Transaction Type:</span>
            <span className="text-slate-200 font-mono font-semibold">{transaction.transactionType}</span>
          </div>
          <div className="flex justify-between py-1 border-b border-slate-800/60">
            <span className="text-slate-400">Source Account:</span>
            <span className="text-slate-200 font-mono">{transaction.sourceAccountNumber || '—'}</span>
          </div>
          <div className="flex justify-between py-1 border-b border-slate-800/60">
            <span className="text-slate-400">Destination Account:</span>
            <span className="text-slate-200 font-mono">{transaction.destinationAccountNumber || '—'}</span>
          </div>
          <div className="flex justify-between py-1 border-b border-slate-800/60">
            <span className="text-slate-400">Idempotency Key:</span>
            <span className="text-slate-200 font-mono text-[11px]">{transaction.idempotencyKey || 'None'}</span>
          </div>
          <div className="flex justify-between py-1">
            <span className="text-slate-400">Received Timestamp:</span>
            <span className="text-slate-200 font-mono">{transaction.receivedTimestamp}</span>
          </div>
        </div>

        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5 space-y-3 text-xs">
          <h2 className="text-xs font-bold text-slate-200 uppercase tracking-wider mb-2">Description &amp; Metadata</h2>
          <div>
            <div className="text-slate-400 mb-1">Business Description:</div>
            <div className="p-2.5 rounded bg-slate-950 border border-slate-800 text-slate-200 font-sans">
              {transaction.description || 'No description provided.'}
            </div>
          </div>
          <div>
            <div className="text-slate-400 mb-1">Payload Metadata:</div>
            <pre className="p-2.5 rounded bg-slate-950 border border-slate-800 text-[11px] text-slate-300 font-mono overflow-x-auto">
              {transaction.metadata || '{\n  "source": "api_v1",\n  "verified": true\n}'}
            </pre>
          </div>
        </div>
      </div>

      {/* Lifecycle Event History */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-6">
        <div className="flex items-center space-x-2 mb-4">
          <Clock className="w-4 h-4 text-brand-400" />
          <h2 className="text-sm font-bold text-slate-200">Lifecycle State Transition Audit Log</h2>
        </div>
        <div className="space-y-4 relative before:absolute before:left-3 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-800">
          {events.length === 0 ? (
            <p className="text-xs text-slate-500 pl-6">No historical events recorded.</p>
          ) : (
            events.map((ev, index) => (
              <div key={ev.id || index} className="flex items-start space-x-4 relative pl-6">
                <span className="w-2.5 h-2.5 rounded-full bg-brand-400 ring-4 ring-slate-950 absolute left-2 top-1.5 -translate-x-1/2"></span>
                <div className="flex-1 bg-slate-950/80 border border-slate-800/80 p-3 rounded-lg text-xs">
                  <div className="flex items-center justify-between mb-1">
                    <div className="font-semibold text-slate-200">
                      {ev.fromStatus ? `${ev.fromStatus} → ${ev.toStatus}` : `INIT: ${ev.toStatus}`}
                    </div>
                    <div className="text-[10px] text-slate-500 font-mono">
                      {new Date(ev.createdAt).toLocaleString()}
                    </div>
                  </div>
                  <div className="text-slate-400">{ev.reason || 'Operational transition'}</div>
                  <div className="text-[10px] text-slate-500 mt-1">Actor: <span className="font-mono text-slate-400">{ev.triggeredBy}</span></div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
