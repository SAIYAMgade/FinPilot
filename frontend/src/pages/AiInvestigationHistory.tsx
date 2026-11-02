import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bot, Sparkles, CheckCircle2, ExternalLink, RefreshCw } from 'lucide-react';
import api from '../services/api';

export const AiInvestigationHistory: React.FC = () => {
  const [investigations, setInvestigations] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchInvestigations = async () => {
    setLoading(true);
    try {
      const res = await api.get('/ai/investigations');
      if (res.data?.success) {
        setInvestigations(res.data.data || []);
      }
    } catch (e) {
      console.error('Failed to load investigations', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvestigations();
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-100 tracking-tight">AI Multi-Agent Investigation Log</h1>
          <p className="text-xs text-slate-400 mt-0.5">Immutable history of LangGraph agent executions, tool citations, and deduction confidence</p>
        </div>
        <button
          onClick={fetchInvestigations}
          className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      <div className="bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden">
        <table className="w-full text-left text-xs text-slate-300">
          <thead className="bg-slate-950/80 text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800 font-sans">
            <tr>
              <th className="py-3 px-4">Investigation ID</th>
              <th className="py-3 px-4">Case #</th>
              <th className="py-3 px-4">Initiated By</th>
              <th className="py-3 px-4 text-center">Confidence</th>
              <th className="py-3 px-4">Root Cause Deduction</th>
              <th className="py-3 px-4">Timestamp</th>
              <th className="py-3 px-4 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-mono">
            {loading ? (
              <tr>
                <td colSpan={7} className="py-12 text-center text-slate-500 font-sans">Loading agent logs...</td>
              </tr>
            ) : investigations.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-12 text-center text-slate-500 font-sans">No AI investigations executed yet.</td>
              </tr>
            ) : (
              investigations.map((inv) => (
                <tr key={inv.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="py-3 px-4 font-bold text-slate-100">{inv.id}</td>
                  <td className="py-3 px-4 text-slate-200 font-semibold">{inv.exceptionNumber}</td>
                  <td className="py-3 px-4 font-sans text-slate-300">{inv.initiatedBy?.fullName || 'Analyst'}</td>
                  <td className="py-3 px-4 text-center">
                    <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                      {inv.confidenceScore}%
                    </span>
                  </td>
                  <td className="py-3 px-4 font-sans text-slate-300 max-w-sm truncate" title={inv.rootCauseAnalysis}>
                    {inv.rootCauseAnalysis}
                  </td>
                  <td className="py-3 px-4 text-slate-400 font-sans text-[11px]">
                    {new Date(inv.createdAt).toLocaleString()}
                  </td>
                  <td className="py-3 px-4 text-right font-sans">
                    <Link
                      to={`/exceptions/${inv.exceptionId}`}
                      className="inline-flex items-center space-x-1 text-brand-400 hover:text-brand-300 font-semibold"
                    >
                      <span>Review Case</span>
                      <ExternalLink className="w-3 h-3" />
                    </Link>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
