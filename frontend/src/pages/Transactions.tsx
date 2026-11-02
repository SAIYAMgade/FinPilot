import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowLeftRight,
  Search,
  Filter,
  Plus,
  Upload,
  ArrowUpDown,
  ExternalLink,
  CheckCircle,
  Clock,
  AlertCircle,
  XCircle,
  FileSpreadsheet
} from 'lucide-react';
import api from '../services/api';

export const Transactions: React.FC = () => {
  const [transactions, setTransactions] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Modal states
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [createForm, setCreateForm] = useState({
    externalReference: '',
    sourceSystem: 'PAYGATE-IN',
    amount: '',
    currency: 'INR',
    transactionType: 'PAYMENT',
    idempotencyKey: '',
    description: '',
  });
  const [importFile, setImportFile] = useState<File | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const fetchTransactions = async () => {
    setLoading(true);
    try {
      const params: any = { page, size: 15 };
      if (statusFilter) params.status = statusFilter;
      if (search) params.search = search;

      const res = await api.get('/transactions', { params });
      if (res.data?.success && res.data?.data) {
        setTransactions(res.data.data.content || []);
        setTotalPages(res.data.data.totalPages || 1);
      }
    } catch (e) {
      console.error('Failed to fetch transactions', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, [page, statusFilter]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchTransactions();
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionLoading(true);
    setActionMessage(null);
    try {
      const payload = {
        ...createForm,
        amount: parseFloat(createForm.amount),
        idempotencyKey: createForm.idempotencyKey || `idemp-${Date.now()}`
      };
      await api.post('/transactions', payload);
      setShowCreateModal(false);
      fetchTransactions();
    } catch (err: any) {
      setActionMessage(err.response?.data?.message || 'Failed to create transaction');
    } finally {
      setActionLoading(false);
    }
  };

  const handleImport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!importFile) return;
    setActionLoading(true);
    setActionMessage(null);
    try {
      const formData = new FormData();
      formData.append('file', importFile);
      const res = await api.post('/transactions/import', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setShowImportModal(false);
      fetchTransactions();
    } catch (err: any) {
      setActionMessage(err.response?.data?.message || 'CSV import failed');
    } finally {
      setActionLoading(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'POSTED':
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">POSTED</span>;
      case 'PENDING':
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-blue-500/10 text-blue-400 border border-blue-500/20">PENDING</span>;
      case 'REQUIRES_REVIEW':
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">REQUIRES REVIEW</span>;
      case 'FAILED':
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">FAILED</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-slate-800 text-slate-300">{status}</span>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Title & Action Bar */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-100 tracking-tight">Financial Transactions Ingestion</h1>
          <p className="text-xs text-slate-400 mt-0.5">High-throughput ingestion feed with strict idempotency and lifecycle tracking</p>
        </div>
        <div className="flex items-center space-x-3">
          <button
            onClick={() => setShowImportModal(true)}
            className="px-3 py-2 bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-200 font-medium text-xs rounded-lg flex items-center space-x-1.5 transition-all"
          >
            <Upload className="w-4 h-4 text-slate-400" />
            <span>Import CSV</span>
          </button>
          <button
            onClick={() => setShowCreateModal(true)}
            className="px-3 py-2 bg-brand-500 hover:bg-brand-600 text-slate-950 font-bold text-xs rounded-lg flex items-center space-x-1.5 shadow-lg shadow-brand-500/20 transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>Ingest Transaction</span>
          </button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-slate-900/60 border border-slate-800 p-4 rounded-xl flex flex-wrap items-center justify-between gap-4">
        <form onSubmit={handleSearch} className="flex items-center space-x-2 flex-1 max-w-md">
          <div className="relative w-full">
            <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by ID, External Reference, or description..."
              className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-brand-500"
            />
          </div>
          <button type="submit" className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-lg transition-colors">
            Search
          </button>
        </form>

        <div className="flex items-center space-x-3">
          <span className="text-xs font-semibold text-slate-400">Status:</span>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-slate-950 border border-slate-800 text-slate-300 text-xs rounded-lg px-3 py-1.5 focus:outline-none focus:border-brand-500"
          >
            <option value="">All Statuses</option>
            <option value="POSTED">POSTED</option>
            <option value="PENDING">PENDING</option>
            <option value="REQUIRES_REVIEW">REQUIRES_REVIEW</option>
            <option value="FAILED">FAILED</option>
          </select>
        </div>
      </div>

      {/* Transactions Data Table */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-950/80 text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800">
              <tr>
                <th className="py-3 px-4">Transaction ID</th>
                <th className="py-3 px-4">External Reference</th>
                <th className="py-3 px-4">Source System</th>
                <th className="py-3 px-4 text-right">Amount</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Idempotency Key</th>
                <th className="py-3 px-4">Timestamp</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono">
              {loading ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-500 font-sans">
                    Loading transactions...
                  </td>
                </tr>
              ) : transactions.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-500 font-sans">
                    No transactions match current filters.
                  </td>
                </tr>
              ) : (
                transactions.map((txn) => (
                  <tr key={txn.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="py-3 px-4 font-semibold text-slate-100">{txn.id}</td>
                    <td className="py-3 px-4 text-slate-300 font-sans">{txn.externalReference || '—'}</td>
                    <td className="py-3 px-4 text-slate-400 font-sans">{txn.sourceSystem}</td>
                    <td className="py-3 px-4 text-right font-bold text-slate-100">
                      ₹{Number(txn.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </td>
                    <td className="py-3 px-4 font-sans">{getStatusBadge(txn.status)}</td>
                    <td className="py-3 px-4 text-[11px] text-slate-400 truncate max-w-[140px]" title={txn.idempotencyKey}>
                      {txn.idempotencyKey || '—'}
                    </td>
                    <td className="py-3 px-4 text-[11px] text-slate-400 font-sans">
                      {txn.transactionTimestamp ? new Date(txn.transactionTimestamp).toLocaleDateString() : '—'}
                    </td>
                    <td className="py-3 px-4 text-right font-sans">
                      <Link
                        to={`/transactions/${txn.id}`}
                        className="inline-flex items-center space-x-1 text-brand-400 hover:text-brand-300 font-semibold"
                      >
                        <span>Details</span>
                        <ExternalLink className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        <div className="p-4 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
          <span>Page {page + 1} of {totalPages}</span>
          <div className="flex items-center space-x-2">
            <button
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="px-3 py-1 bg-slate-800 disabled:opacity-40 rounded text-slate-300"
            >
              Previous
            </button>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="px-3 py-1 bg-slate-800 disabled:opacity-40 rounded text-slate-300"
            >
              Next
            </button>
          </div>
        </div>
      </div>

      {/* Ingest Transaction Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg p-6 shadow-2xl">
            <h2 className="text-base font-bold text-slate-100 mb-4">Ingest New Financial Transaction</h2>
            {actionMessage && (
              <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/30 rounded-lg text-rose-400 text-xs">
                {actionMessage}
              </div>
            )}
            <form onSubmit={handleCreate} className="space-y-4 text-xs">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block font-semibold text-slate-300 mb-1">External Reference</label>
                  <input
                    type="text"
                    required
                    value={createForm.externalReference}
                    onChange={(e) => setCreateForm({ ...createForm, externalReference: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200 font-mono"
                    placeholder="e.g. TXN-9988"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-300 mb-1">Source System</label>
                  <input
                    type="text"
                    required
                    value={createForm.sourceSystem}
                    onChange={(e) => setCreateForm({ ...createForm, sourceSystem: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block font-semibold text-slate-300 mb-1">Amount (INR)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={createForm.amount}
                    onChange={(e) => setCreateForm({ ...createForm, amount: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200 font-mono"
                    placeholder="5000.00"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-300 mb-1">Idempotency Key (Optional)</label>
                  <input
                    type="text"
                    value={createForm.idempotencyKey}
                    onChange={(e) => setCreateForm({ ...createForm, idempotencyKey: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200 font-mono"
                    placeholder="idemp-custom-key"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-300 mb-1">Description</label>
                <textarea
                  value={createForm.description}
                  onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200 h-20"
                  placeholder="Customer checkout payment..."
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-4 py-2 bg-brand-500 hover:bg-brand-600 text-slate-950 font-bold rounded-lg shadow-lg shadow-brand-500/20"
                >
                  {actionLoading ? 'Ingesting...' : 'Ingest Transaction'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Bulk CSV Import Modal */}
      {showImportModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 shadow-2xl">
            <h2 className="text-base font-bold text-slate-100 mb-2">Import Transactions via CSV</h2>
            <p className="text-xs text-slate-400 mb-4">
              Format: <code className="bg-slate-950 p-1 rounded font-mono text-brand-400">external_reference,amount,currency,source_system,transaction_type</code>
            </p>
            {actionMessage && (
              <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/30 rounded-lg text-rose-400 text-xs">
                {actionMessage}
              </div>
            )}
            <form onSubmit={handleImport} className="space-y-4">
              <input
                type="file"
                accept=".csv"
                required
                onChange={(e) => setImportFile(e.target.files?.[0] || null)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-xs text-slate-300"
              />
              <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowImportModal(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold rounded-lg text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading || !importFile}
                  className="px-4 py-2 bg-brand-500 hover:bg-brand-600 disabled:opacity-50 text-slate-950 font-bold rounded-lg text-xs"
                >
                  {actionLoading ? 'Uploading...' : 'Process Import'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
