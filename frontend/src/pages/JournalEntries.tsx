import React, { useEffect, useState } from 'react';
import { FileSpreadsheet, Plus, RotateCcw, CheckCircle2, AlertCircle, Trash2 } from 'lucide-react';
import api from '../services/api';

export const JournalEntries: React.FC = () => {
  const [journals, setJournals] = useState<any[]>([]);
  const [accounts, setAccounts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [selectedJournal, setSelectedJournal] = useState<any>(null);

  // Form State
  const [referenceId, setReferenceId] = useState('');
  const [description, setDescription] = useState('');
  const [lines, setLines] = useState<any[]>([
    { accountId: '', debitAmount: '0.00', creditAmount: '0.00', description: '' },
    { accountId: '', debitAmount: '0.00', creditAmount: '0.00', description: '' }
  ]);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [jRes, accRes] = await Promise.all([
        api.get('/ledger/journals'),
        api.get('/accounts')
      ]);
      if (jRes.data?.success && jRes.data?.data) {
        setJournals(jRes.data.data.content || []);
      }
      if (accRes.data?.success) {
        setAccounts(accRes.data.data || []);
      }
    } catch (e) {
      console.error('Failed to load journals', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const totalDebits = lines.reduce((acc, l) => acc + (parseFloat(l.debitAmount) || 0), 0);
  const totalCredits = lines.reduce((acc, l) => acc + (parseFloat(l.creditAmount) || 0), 0);
  const isBalanced = Math.abs(totalDebits - totalCredits) < 0.0001 && totalDebits > 0;

  const addLine = () => {
    setLines([...lines, { accountId: '', debitAmount: '0.00', creditAmount: '0.00', description: '' }]);
  };

  const removeLine = (index: number) => {
    if (lines.length <= 2) return;
    setLines(lines.filter((_, i) => i !== index));
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isBalanced) {
      setError(`Journal is not balanced! Total Debits (₹${totalDebits.toFixed(2)}) must equal Total Credits (₹${totalCredits.toFixed(2)}).`);
      return;
    }
    setError(null);
    try {
      const payload = {
        referenceId,
        description,
        currency: 'INR',
        autoPost: true,
        lines: lines.map((l) => ({
          accountId: l.accountId,
          debitAmount: parseFloat(l.debitAmount) || 0,
          creditAmount: parseFloat(l.creditAmount) || 0,
          description: l.description,
        })),
      };
      await api.post('/ledger/journals', payload);
      setShowModal(false);
      fetchData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to post journal entry');
    }
  };

  const handleReverse = async (journalId: string) => {
    if (!window.confirm('Are you sure you want to reverse this posted journal entry?')) return;
    try {
      await api.post(`/ledger/journals/${journalId}/reverse?reason=Operational+correction`);
      fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Reversal failed');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-100 tracking-tight">Double-Entry Journal Entries</h1>
          <p className="text-xs text-slate-400 mt-0.5">Immutable financial ledger records governed by mathematical double-entry balance constraints</p>
        </div>
        <button
          onClick={() => {
            setShowModal(true);
            setReferenceId(`JNL-${Date.now().toString().substring(7)}`);
          }}
          className="px-3 py-2 bg-brand-500 hover:bg-brand-600 text-slate-950 font-bold text-xs rounded-lg flex items-center space-x-1.5 shadow-lg shadow-brand-500/20 transition-all"
        >
          <Plus className="w-4 h-4" />
          <span>Post Balanced Journal</span>
        </button>
      </div>

      {/* Journal Cards List */}
      <div className="space-y-4">
        {loading ? (
          <div className="py-12 text-center text-slate-500 text-xs">Loading journal records...</div>
        ) : journals.length === 0 ? (
          <div className="py-12 text-center text-slate-500 text-xs">No journal entries found.</div>
        ) : (
          journals.map((jnl) => (
            <div key={jnl.id} className="bg-slate-900/60 border border-slate-800 rounded-xl p-5 space-y-3">
              <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
                <div className="flex items-center space-x-3">
                  <span className="font-mono font-bold text-slate-100">{jnl.id}</span>
                  <span className="text-xs text-slate-400 font-mono">Ref: {jnl.referenceId}</span>
                  <span
                    className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                      jnl.postingStatus === 'POSTED'
                        ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                        : jnl.postingStatus === 'REVERSED'
                        ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                        : 'bg-slate-800 text-slate-300'
                    }`}
                  >
                    {jnl.postingStatus}
                  </span>
                </div>
                <div className="flex items-center space-x-4">
                  <div className="text-xs font-mono font-bold text-slate-200">
                    ₹{Number(jnl.totalDebits || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </div>
                  {jnl.postingStatus === 'POSTED' && (
                    <button
                      onClick={() => handleReverse(jnl.id)}
                      className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-amber-400 hover:text-amber-300 text-xs font-semibold rounded flex items-center space-x-1"
                      title="Post offsetting reversal entry"
                    >
                      <RotateCcw className="w-3 h-3" />
                      <span>Reverse</span>
                    </button>
                  )}
                </div>
              </div>

              <div className="text-xs text-slate-300 font-sans">{jnl.description}</div>

              {/* Line Items Table */}
              <div className="bg-slate-950/80 rounded-lg border border-slate-800/60 overflow-hidden mt-2">
                <table className="w-full text-left text-xs font-mono">
                  <thead className="bg-slate-900/60 text-[10px] text-slate-400 uppercase border-b border-slate-800/60 font-sans">
                    <tr>
                      <th className="py-2 px-3">#</th>
                      <th className="py-2 px-3">Account Number &amp; Name</th>
                      <th className="py-2 px-3 text-right">Debit (INR)</th>
                      <th className="py-2 px-3 text-right">Credit (INR)</th>
                      <th className="py-2 px-3">Memo</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/40">
                    {(jnl.lines || []).map((line: any) => (
                      <tr key={line.id}>
                        <td className="py-2 px-3 text-slate-500">{line.lineNumber}</td>
                        <td className="py-2 px-3 text-slate-200 font-sans">
                          <span className="font-mono font-bold text-brand-400 mr-2">{line.accountNumber}</span>
                          {line.accountName}
                        </td>
                        <td className="py-2 px-3 text-right font-bold text-slate-100">
                          {Number(line.debitAmount) > 0 ? `₹${Number(line.debitAmount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : '—'}
                        </td>
                        <td className="py-2 px-3 text-right font-bold text-slate-100">
                          {Number(line.creditAmount) > 0 ? `₹${Number(line.creditAmount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : '—'}
                        </td>
                        <td className="py-2 px-3 text-slate-400 font-sans text-[11px]">{line.description || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Modal to Post New Journal Entry */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-2xl p-6 shadow-2xl">
            <h2 className="text-base font-bold text-slate-100 mb-2">Create &amp; Post Balanced Journal Entry</h2>
            <p className="text-xs text-slate-400 mb-4">
              Enforces the strict double-entry rule: Total Debits must mathematically equal Total Credits.
            </p>

            {error && (
              <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/30 rounded-lg text-rose-400 text-xs">
                {error}
              </div>
            )}

            <form onSubmit={handleCreate} className="space-y-4 text-xs">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block font-semibold text-slate-300 mb-1">Reference ID</label>
                  <input
                    type="text"
                    required
                    value={referenceId}
                    onChange={(e) => setReferenceId(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200 font-mono"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-300 mb-1">Description / Memo</label>
                  <input
                    type="text"
                    required
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200"
                    placeholder="Monthly adjustment..."
                  />
                </div>
              </div>

              {/* Dynamic Lines */}
              <div className="space-y-2 border-t border-b border-slate-800 py-3">
                <div className="flex justify-between items-center mb-1">
                  <span className="font-semibold text-slate-300 uppercase tracking-wider text-[11px]">Journal Line Items</span>
                  <button
                    type="button"
                    onClick={addLine}
                    className="text-xs text-brand-400 hover:text-brand-300 font-semibold flex items-center space-x-1"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Add Line</span>
                  </button>
                </div>

                {lines.map((line, idx) => (
                  <div key={idx} className="grid grid-cols-12 gap-2 items-center bg-slate-950 p-2 rounded-lg border border-slate-800/60">
                    <div className="col-span-5">
                      <select
                        required
                        value={line.accountId}
                        onChange={(e) => {
                          const updated = [...lines];
                          updated[idx].accountId = e.target.value;
                          setLines(updated);
                        }}
                        className="w-full bg-slate-900 border border-slate-800 rounded p-1.5 text-xs text-slate-200"
                      >
                        <option value="">Select Account...</option>
                        {accounts.map((a) => (
                          <option key={a.id} value={a.id}>
                            {a.accountNumber} - {a.accountName} ({a.accountType})
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="col-span-3">
                      <input
                        type="number"
                        step="0.01"
                        placeholder="Debit"
                        value={line.debitAmount}
                        onChange={(e) => {
                          const updated = [...lines];
                          updated[idx].debitAmount = e.target.value;
                          if (parseFloat(e.target.value) > 0) updated[idx].creditAmount = '0.00';
                          setLines(updated);
                        }}
                        className="w-full bg-slate-900 border border-slate-800 rounded p-1.5 text-xs text-slate-200 font-mono text-right"
                      />
                    </div>
                    <div className="col-span-3">
                      <input
                        type="number"
                        step="0.01"
                        placeholder="Credit"
                        value={line.creditAmount}
                        onChange={(e) => {
                          const updated = [...lines];
                          updated[idx].creditAmount = e.target.value;
                          if (parseFloat(e.target.value) > 0) updated[idx].debitAmount = '0.00';
                          setLines(updated);
                        }}
                        className="w-full bg-slate-900 border border-slate-800 rounded p-1.5 text-xs text-slate-200 font-mono text-right"
                      />
                    </div>
                    <div className="col-span-1 text-center">
                      <button
                        type="button"
                        onClick={() => removeLine(idx)}
                        disabled={lines.length <= 2}
                        className="text-slate-500 hover:text-rose-400 disabled:opacity-30"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>

              {/* Balance Summary Invariant Check */}
              <div className="flex items-center justify-between p-3 rounded-lg bg-slate-950 font-mono">
                <div className="flex items-center space-x-4">
                  <div>Debits: <span className="font-bold text-slate-100">₹{totalDebits.toFixed(2)}</span></div>
                  <div>Credits: <span className="font-bold text-slate-100">₹{totalCredits.toFixed(2)}</span></div>
                </div>
                <div className="flex items-center space-x-1.5 font-sans font-semibold">
                  {isBalanced ? (
                    <span className="text-emerald-400 flex items-center space-x-1 text-xs">
                      <CheckCircle2 className="w-4 h-4" />
                      <span>Balanced &amp; Valid</span>
                    </span>
                  ) : (
                    <span className="text-rose-400 flex items-center space-x-1 text-xs">
                      <AlertCircle className="w-4 h-4" />
                      <span>Unbalanced (Diff: ₹{Math.abs(totalDebits - totalCredits).toFixed(2)})</span>
                    </span>
                  )}
                </div>
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={!isBalanced}
                  className="px-4 py-2 bg-brand-500 hover:bg-brand-600 disabled:opacity-40 text-slate-950 font-bold rounded-lg shadow-lg shadow-brand-500/20"
                >
                  Post Journal
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
