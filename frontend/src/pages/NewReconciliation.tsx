import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, GitCompare, FileText, CheckCircle2, AlertCircle, ArrowRight, Download } from 'lucide-react';
import api from '../services/api';

export const NewReconciliation: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [dateToleranceDays, setDateToleranceDays] = useState(2);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const navigate = useNavigate();

  const handleDownloadSample = () => {
    const csvContent = "transaction_reference,amount,currency,date,status\n" +
      "TXN-1001,50000.00,INR,2026-10-01,SETTLED\n" +
      "TXN-1002,78500.00,INR,2026-10-01,SETTLED\n" +
      "TXN-1003,20000.00,INR,2026-10-01,SETTLED\n" +
      "EXT-UNKNOWN-99,4800.00,INR,2026-10-02,SETTLED\n";
    
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'sample_bank_settlement.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleStartRecon = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) {
      setError('Please select an external bank or payment gateway settlement CSV file.');
      return;
    }

    setLoading(true);
    setError(null);
    setStatusMessage('1/2 Uploading statement and calculating SHA-256 checksum...');

    try {
      // Step 1: Upload CSV
      const formData = new FormData();
      formData.append('file', file);
      const uploadRes = await api.post('/reconciliations/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      const runId = uploadRes.data?.data?.id;
      if (!runId) throw new Error('Failed to create reconciliation run');

      // Step 2: Execute matching algorithm
      setStatusMessage('2/2 Executing 4-tier deterministic matching algorithm...');
      await api.post(`/reconciliations/${runId}/execute?dateToleranceDays=${dateToleranceDays}`);

      navigate(`/reconciliations/${runId}`);
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Reconciliation execution failed.');
      setLoading(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-100 tracking-tight">Initiate Automated Reconciliation</h1>
        <p className="text-xs text-slate-400 mt-0.5">
          Match external bank or payment acquirer settlement files against internal ledger transactions.
        </p>
      </div>

      {error && (
        <div className="p-3 bg-rose-500/10 border border-rose-500/30 rounded-xl flex items-center space-x-2 text-rose-400 text-xs">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleStartRecon} className="space-y-6">
        {/* Upload Zone */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-8 text-center backdrop-blur">
          <div className="w-12 h-12 rounded-xl bg-brand-500/10 text-brand-400 flex items-center justify-center mx-auto mb-4 border border-brand-500/20">
            <Upload className="w-6 h-6" />
          </div>
          <h2 className="text-sm font-bold text-slate-200 mb-1">Select Bank Settlement Statement CSV</h2>
          <p className="text-xs text-slate-400 max-w-md mx-auto mb-4">
            Upload the raw file provided by your banking partner or gateway. Schema: reference, amount, currency, date, status.
          </p>

          <input
            type="file"
            accept=".csv"
            onChange={(e) => setFile(e.target.files?.[0] || null)}
            className="block mx-auto text-xs text-slate-400 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-bold file:bg-slate-800 file:text-slate-200 hover:file:bg-slate-700 cursor-pointer"
          />

          {file && (
            <div className="mt-4 inline-flex items-center space-x-2 text-xs text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-3 py-1.5 rounded-lg">
              <CheckCircle2 className="w-4 h-4" />
              <span>Selected: <span className="font-mono font-semibold">{file.name}</span> ({(file.size / 1024).toFixed(1)} KB)</span>
            </div>
          )}
        </div>

        {/* Configuration Parameters */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5 space-y-4 text-xs">
          <h3 className="font-bold text-slate-200 uppercase tracking-wider text-[11px]">Matching Rule Configurations</h3>
          <div className="flex items-center justify-between">
            <div>
              <div className="font-semibold text-slate-300">Level 3 Date Tolerance Window</div>
              <div className="text-[11px] text-slate-500">Maximum allowed calendar days drift for interbank clearing settlement delays</div>
            </div>
            <div className="flex items-center space-x-2">
              <input
                type="number"
                min="0"
                max="14"
                value={dateToleranceDays}
                onChange={(e) => setDateToleranceDays(parseInt(e.target.value) || 0)}
                className="w-20 bg-slate-950 border border-slate-800 rounded-lg p-1.5 text-center font-mono text-slate-200 font-bold"
              />
              <span className="text-slate-400">Days</span>
            </div>
          </div>
        </div>

        {/* Sample Template & Help */}
        <div className="flex items-center justify-between p-4 bg-slate-900/40 border border-slate-800/60 rounded-xl text-xs">
          <div className="flex items-center space-x-3">
            <FileText className="w-5 h-5 text-slate-500" />
            <div>
              <div className="font-semibold text-slate-300">Need a sample file to test?</div>
              <div className="text-[11px] text-slate-500">Download our pre-formatted demo bank statement CSV</div>
            </div>
          </div>
          <button
            type="button"
            onClick={handleDownloadSample}
            className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg flex items-center space-x-1.5 font-semibold text-xs transition-colors"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Download Sample CSV</span>
          </button>
        </div>

        {/* Submit */}
        <button
          type="submit"
          disabled={loading || !file}
          className="w-full py-3 bg-brand-500 hover:bg-brand-600 disabled:opacity-40 text-slate-950 font-bold text-xs rounded-xl flex items-center justify-center space-x-2 shadow-xl shadow-brand-500/20 transition-all"
        >
          {loading ? (
            <span>{statusMessage}</span>
          ) : (
            <>
              <span>Execute 4-Tier Reconciliation Match</span>
              <ArrowRight className="w-4 h-4" />
            </>
          )}
        </button>
      </form>
    </div>
  );
};
