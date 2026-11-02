import React, { useEffect, useState } from 'react';
import { Scale, CheckCircle2, AlertTriangle, Printer, RefreshCw } from 'lucide-react';
import api from '../services/api';

export const TrialBalance: React.FC = () => {
  const [report, setReport] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  const fetchTrialBalance = async () => {
    setLoading(true);
    try {
      const res = await api.get('/ledger/trial-balance');
      if (res.data?.success) {
        setReport(res.data.data);
      }
    } catch (e) {
      console.error('Failed to load trial balance', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTrialBalance();
  }, []);

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-100 tracking-tight">General Ledger Trial Balance</h1>
          <p className="text-xs text-slate-400 mt-0.5">Authoritative statement of all debit and credit balances proving double-entry equilibrium</p>
        </div>
        <div className="flex items-center space-x-3">
          <button
            onClick={fetchTrialBalance}
            className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200"
            title="Recalculate"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button
            onClick={() => window.print()}
            className="px-3 py-2 bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-200 font-semibold text-xs rounded-lg flex items-center space-x-1.5"
          >
            <Printer className="w-4 h-4 text-slate-400" />
            <span>Print Report</span>
          </button>
        </div>
      </div>

      {/* Verification Card */}
      <div
        className={`p-4 rounded-xl border flex items-center justify-between ${
          report?.balanced
            ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
            : 'bg-rose-500/10 border-rose-500/30 text-rose-400'
        }`}
      >
        <div className="flex items-center space-x-3">
          {report?.balanced ? (
            <CheckCircle2 className="w-5 h-5 shrink-0" />
          ) : (
            <AlertTriangle className="w-5 h-5 shrink-0" />
          )}
          <div>
            <div className="text-xs font-bold uppercase tracking-wider">
              {report?.balanced ? 'Double-Entry Invariant Satisfied: Ledger In Balance' : 'Discrepancy Detected in Trial Balance!'}
            </div>
            <div className="text-[11px] opacity-80 mt-0.5">
              Generated at: {report?.generatedAt ? new Date(report.generatedAt).toLocaleString() : 'Just now'}
            </div>
          </div>
        </div>
        <div className="text-right font-mono text-xs font-bold">
          <div>Debits: ₹{Number(report?.totalDebits || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</div>
          <div>Credits: ₹{Number(report?.totalCredits || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</div>
        </div>
      </div>

      {/* Trial Balance Table */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden">
        <table className="w-full text-left text-xs text-slate-300">
          <thead className="bg-slate-950/80 text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800 font-sans">
            <tr>
              <th className="py-3 px-4">Account Code</th>
              <th className="py-3 px-4">Account Title</th>
              <th className="py-3 px-4">Type</th>
              <th className="py-3 px-4 text-right">Debit Balance (INR)</th>
              <th className="py-3 px-4 text-right">Credit Balance (INR)</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-mono">
            {loading ? (
              <tr>
                <td colSpan={5} className="py-12 text-center text-slate-500 font-sans">
                  Computing ledger balances...
                </td>
              </tr>
            ) : !report?.accounts || report.accounts.length === 0 ? (
              <tr>
                <td colSpan={5} className="py-12 text-center text-slate-500 font-sans">
                  No accounts found.
                </td>
              </tr>
            ) : (
              report.accounts.map((acc: any) => (
                <tr key={acc.accountId} className="hover:bg-slate-800/30 transition-colors">
                  <td className="py-3 px-4 font-bold text-slate-100">{acc.accountNumber}</td>
                  <td className="py-3 px-4 text-slate-200 font-sans">{acc.accountName}</td>
                  <td className="py-3 px-4 font-sans text-slate-400 text-[11px]">{acc.accountType}</td>
                  <td className="py-3 px-4 text-right font-bold text-slate-100">
                    {Number(acc.netDebitBalance) > 0 ? `₹${Number(acc.netDebitBalance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : '—'}
                  </td>
                  <td className="py-3 px-4 text-right font-bold text-slate-100">
                    {Number(acc.netCreditBalance) > 0 ? `₹${Number(acc.netCreditBalance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : '—'}
                  </td>
                </tr>
              ))
            )}
          </tbody>
          <tfoot className="bg-slate-950/90 font-mono font-bold text-slate-100 border-t-2 border-slate-700">
            <tr>
              <td colSpan={3} className="py-3 px-4 uppercase text-slate-400 font-sans text-xs">
                Total Double-Entry Equilibrium
              </td>
              <td className="py-3 px-4 text-right text-emerald-400">
                ₹{Number(report?.totalDebits || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </td>
              <td className="py-3 px-4 text-right text-emerald-400">
                ₹{Number(report?.totalCredits || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
};
