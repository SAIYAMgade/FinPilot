import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { GitCompare, Plus, FileSpreadsheet, CheckCircle2, AlertTriangle, ExternalLink, RefreshCw } from 'lucide-react';
import api from '../services/api';

export const ReconciliationRuns: React.FC = () => {
  const [runs, setRuns] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchRuns = async () => {
    setLoading(true);
    try {
      const res = await api.get('/reconciliations');
      if (res.data?.success) {
        setRuns(res.data.data || []);
      }
    } catch (e) {
      console.error('Failed to load reconciliation runs', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRuns();
  }, []);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-100 tracking-tight">Reconciliation Operations</h1>
          <p className="text-xs text-slate-400 mt-0.5">Historical batch reconciliation runs and deterministic matching audit records</p>
        </div>
        <div className="flex items-center space-x-3">
          <button
            onClick={fetchRuns}
            className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200"
            title="Refresh"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <Link
            to="/reconciliations/new"
            className="px-3 py-2 bg-brand-500 hover:bg-brand-600 text-slate-950 font-bold text-xs rounded-lg flex items-center space-x-1.5 shadow-lg shadow-brand-500/20"
          >
            <Plus className="w-4 h-4" />
            <span>Upload Statement &amp; Run</span>
          </Link>
        </div>
      </div>

      {/* Runs Table */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden">
        <table className="w-full text-left text-xs text-slate-300">
          <thead className="bg-slate-950/80 text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800 font-sans">
            <tr>
              <th className="py-3 px-4">Run #</th>
              <th className="py-3 px-4">Statement File</th>
              <th className="py-3 px-4">SHA-256 Checksum</th>
              <th className="py-3 px-4 text-center">Status</th>
              <th className="py-3 px-4 text-center">Matched</th>
              <th className="py-3 px-4 text-center">Discrepancies</th>
              <th className="py-3 px-4 text-right">Duration</th>
              <th className="py-3 px-4 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-mono">
            {loading ? (
              <tr>
                <td colSpan={8} className="py-12 text-center text-slate-500 font-sans">
                  Loading reconciliation history...
                </td>
              </tr>
            ) : runs.length === 0 ? (
              <tr>
                <td colSpan={8} className="py-12 text-center text-slate-500 font-sans">
                  No reconciliation runs yet. Click "Upload Statement &amp; Run" to begin.
                </td>
              </tr>
            ) : (
              runs.map((run) => (
                <tr key={run.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="py-3 px-4 font-bold text-slate-100">{run.runNumber}</td>
                  <td className="py-3 px-4 text-slate-200 font-sans">{run.fileName}</td>
                  <td className="py-3 px-4 text-[10px] text-slate-500 truncate max-w-[120px]" title={run.fileChecksum}>
                    {run.fileChecksum?.substring(0, 16)}...
                  </td>
                  <td className="py-3 px-4 text-center font-sans">
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                        run.status === 'COMPLETED'
                          ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                          : run.status === 'PROCESSING'
                          ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20 animate-pulse'
                          : 'bg-slate-800 text-slate-300'
                      }`}
                    >
                      {run.status}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-center font-bold text-emerald-400">
                    {run.matchedCount}
                  </td>
                  <td className="py-3 px-4 text-center font-bold text-amber-400">
                    {run.discrepancyCount}
                  </td>
                  <td className="py-3 px-4 text-right text-slate-400">
                    {run.executionDurationMs} ms
                  </td>
                  <td className="py-3 px-4 text-right font-sans">
                    <Link
                      to={`/reconciliations/${run.id}`}
                      className="inline-flex items-center space-x-1 text-brand-400 hover:text-brand-300 font-semibold"
                    >
                      <span>Report</span>
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
