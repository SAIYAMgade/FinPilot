import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, GitCompare, Download, CheckCircle2, AlertCircle, FileText, ExternalLink, Filter } from 'lucide-react';
import api from '../services/api';

export const ReconciliationDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [report, setReport] = useState<any>(null);
  const [matches, setMatches] = useState<any[]>([]);
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchReport = async () => {
      setLoading(true);
      try {
        const [repRes, matchRes] = await Promise.all([
          api.get(`/reconciliations/${id}/report`),
          api.get(`/reconciliations/${id}/results`, { params: { matchStatus: filterStatus || undefined, size: 50 } })
        ]);
        if (repRes.data?.success) setReport(repRes.data.data);
        if (matchRes.data?.success && matchRes.data?.data) {
          setMatches(matchRes.data.data.content || []);
        }
      } catch (e) {
        console.error('Failed to load reconciliation details', e);
      } finally {
        setLoading(false);
      }
    };
    if (id) fetchReport();
  }, [id, filterStatus]);

  const handleExportCsv = () => {
    window.open(`/api/v1/reconciliations/${id}/export`, '_blank');
  };

  const run = report?.runSummary;

  const getMatchStatusBadge = (status: string) => {
    switch (status) {
      case 'MATCHED':
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">MATCHED</span>;
      case 'AMOUNT_MISMATCH':
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">AMOUNT MISMATCH</span>;
      case 'MISSING_INTERNAL':
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">MISSING INTERNAL</span>;
      case 'MISSING_EXTERNAL':
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-purple-500/10 text-purple-400 border border-purple-500/20">MISSING EXTERNAL</span>;
      case 'DATE_MISMATCH':
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-blue-500/10 text-blue-400 border border-blue-500/20">DATE DRIFT</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-slate-800 text-slate-300">{status}</span>;
    }
  };

  if (loading && !report) {
    return <div className="text-center py-20 text-slate-500 text-xs">Loading reconciliation report...</div>;
  }

  return (
    <div className="space-y-6">
      {/* Back button */}
      <Link to="/reconciliations" className="inline-flex items-center space-x-1.5 text-xs text-slate-400 hover:text-slate-200">
        <ArrowLeft className="w-4 h-4" />
        <span>Back to Reconciliation Runs</span>
      </Link>

      {/* Header Banner */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 flex items-start justify-between">
        <div>
          <div className="flex items-center space-x-3">
            <h1 className="text-xl font-bold font-mono text-slate-100">{run?.runNumber || 'Run'}</h1>
            <span className="px-2.5 py-0.5 rounded text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              {run?.status}
            </span>
          </div>
          <div className="text-xs text-slate-400 mt-1">
            File: <span className="text-slate-200 font-semibold">{run?.fileName}</span> &bull; Checksum: <span className="font-mono text-slate-400">{run?.fileChecksum?.substring(0, 16)}...</span>
          </div>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={handleExportCsv}
            className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-lg flex items-center space-x-1.5 transition-colors"
          >
            <Download className="w-4 h-4" />
            <span>Export CSV Report</span>
          </button>
        </div>
      </div>

      {/* Summary Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-slate-900/60 border border-slate-800 p-4 rounded-xl">
          <div className="text-xs text-slate-400 uppercase font-semibold">Match Rate</div>
          <div className="text-2xl font-bold text-emerald-400 font-mono mt-1">{report?.matchRatePercent}%</div>
          <div className="text-[11px] text-slate-500 mt-1">{run?.matchedCount} of {run?.totalExternalRecords} external items</div>
        </div>

        <div className="bg-slate-900/60 border border-slate-800 p-4 rounded-xl">
          <div className="text-xs text-slate-400 uppercase font-semibold">Discrepancy Count</div>
          <div className="text-2xl font-bold text-amber-400 font-mono mt-1">{run?.discrepancyCount}</div>
          <div className="text-[11px] text-slate-500 mt-1">Exceptions generated</div>
        </div>

        <div className="bg-slate-900/60 border border-slate-800 p-4 rounded-xl">
          <div className="text-xs text-slate-400 uppercase font-semibold">Financial Discrepancy</div>
          <div className="text-2xl font-bold text-slate-100 font-mono mt-1">
            ₹{Number(report?.totalDiscrepancyAmount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">Variance requiring adjustment</div>
        </div>

        <div className="bg-slate-900/60 border border-slate-800 p-4 rounded-xl">
          <div className="text-xs text-slate-400 uppercase font-semibold">Execution Latency</div>
          <div className="text-2xl font-bold text-slate-100 font-mono mt-1">{run?.executionDurationMs} ms</div>
          <div className="text-[11px] text-slate-500 mt-1">Deterministic matching time</div>
        </div>
      </div>

      {/* Filter and Match Results Table */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-bold text-slate-200 uppercase tracking-wider">Itemized Match Ledger</h2>
          <div className="flex items-center space-x-2">
            <span className="text-xs text-slate-400">Filter Outcome:</span>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="bg-slate-950 border border-slate-800 text-slate-300 text-xs rounded-lg px-2.5 py-1 focus:outline-none focus:border-brand-500"
            >
              <option value="">All Outcomes</option>
              <option value="MATCHED">MATCHED</option>
              <option value="AMOUNT_MISMATCH">AMOUNT_MISMATCH</option>
              <option value="MISSING_INTERNAL">MISSING_INTERNAL</option>
              <option value="DATE_MISMATCH">DATE_MISMATCH</option>
            </select>
          </div>
        </div>

        <div className="bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-950/80 text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800 font-sans">
              <tr>
                <th className="py-3 px-4">Level / Method</th>
                <th className="py-3 px-4">Outcome</th>
                <th className="py-3 px-4">Internal Txn</th>
                <th className="py-3 px-4">External Statement Ref</th>
                <th className="py-3 px-4 text-right">Internal (₹)</th>
                <th className="py-3 px-4 text-right">External (₹)</th>
                <th className="py-3 px-4 text-right">Variance</th>
                <th className="py-3 px-4">Match Notes</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono">
              {matches.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-10 text-center text-slate-500 font-sans">
                    No matching records found for selected filter.
                  </td>
                </tr>
              ) : (
                matches.map((m) => (
                  <tr key={m.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="py-3 px-4 font-sans text-slate-300 text-[11px]">
                      {m.matchType?.replace(/_/g, ' ')}
                    </td>
                    <td className="py-3 px-4 font-sans">{getMatchStatusBadge(m.matchStatus)}</td>
                    <td className="py-3 px-4 font-semibold text-slate-200">
                      {m.transaction?.id ? (
                        <Link to={`/transactions/${m.transaction.id}`} className="hover:text-brand-400">
                          {m.transaction.id}
                        </Link>
                      ) : (
                        <span className="text-slate-500 font-sans">None</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-slate-200 font-sans font-medium">
                      {m.externalRecord?.transactionReference || '—'}
                    </td>
                    <td className="py-3 px-4 text-right text-slate-200">
                      {m.transaction?.amount ? `₹${Number(m.transaction.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : '—'}
                    </td>
                    <td className="py-3 px-4 text-right text-slate-200">
                      {m.externalRecord?.amount ? `₹${Number(m.externalRecord.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : '—'}
                    </td>
                    <td className="py-3 px-4 text-right font-bold">
                      {Number(m.amountDifference) > 0 ? (
                        <span className="text-amber-400">₹{Number(m.amountDifference).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                      ) : (
                        <span className="text-emerald-400">₹0.00</span>
                      )}
                    </td>
                    <td className="py-3 px-4 font-sans text-[11px] text-slate-400 max-w-[240px] truncate" title={m.notes}>
                      {m.notes}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
