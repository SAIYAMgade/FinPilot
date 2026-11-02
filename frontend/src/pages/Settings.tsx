import React, { useState } from 'react';
import { Settings as SettingsIcon, Save, CheckCircle2, ShieldCheck, Database, Key } from 'lucide-react';

export const Settings: React.FC = () => {
  const [currency, setCurrency] = useState('INR');
  const [dateTolerance, setDateTolerance] = useState('2');
  const [aiUrl, setAiUrl] = useState('http://localhost:8000');
  const [pollInterval, setPollInterval] = useState('5000');
  const [saved, setSaved] = useState(false);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  return (
    <div className="space-y-6 max-w-3xl mx-auto">
      <div>
        <h1 className="text-xl font-bold text-slate-100 tracking-tight">Platform Configuration &amp; Policies</h1>
        <p className="text-xs text-slate-400 mt-0.5">Financial invariants, reconciliation engine parameters, and API integration settings</p>
      </div>

      {saved && (
        <div className="p-3 bg-emerald-500/10 border border-emerald-500/30 rounded-xl flex items-center space-x-2 text-emerald-400 text-xs">
          <CheckCircle2 className="w-4 h-4 shrink-0" />
          <span>Configuration parameters persisted successfully.</span>
        </div>
      )}

      <form onSubmit={handleSave} className="space-y-6 text-xs">
        {/* Section 1: Accounting Parameters */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-6 space-y-4">
          <div className="flex items-center space-x-2 text-slate-200 font-bold uppercase tracking-wider text-[11px]">
            <Database className="w-4 h-4 text-brand-400" />
            <span>Accounting &amp; Ledger Invariants</span>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block font-semibold text-slate-300 mb-1">Base Currency</label>
              <select
                value={currency}
                onChange={(e) => setCurrency(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200 font-mono"
              >
                <option value="INR">INR (₹ - Indian Rupee)</option>
                <option value="USD">USD ($ - United States Dollar)</option>
                <option value="EUR">EUR (€ - Euro)</option>
              </select>
            </div>
            <div>
              <label className="block font-semibold text-slate-300 mb-1">Reconciliation Date Tolerance (Days)</label>
              <input
                type="number"
                value={dateTolerance}
                onChange={(e) => setDateTolerance(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200 font-mono"
              />
            </div>
          </div>
        </div>

        {/* Section 2: AI & Messaging */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-6 space-y-4">
          <div className="flex items-center space-x-2 text-slate-200 font-bold uppercase tracking-wider text-[11px]">
            <Key className="w-4 h-4 text-purple-400" />
            <span>Distributed Services &amp; AI LangGraph</span>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block font-semibold text-slate-300 mb-1">AI LangGraph Service Base URL</label>
              <input
                type="text"
                value={aiUrl}
                onChange={(e) => setAiUrl(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200 font-mono"
              />
            </div>
            <div>
              <label className="block font-semibold text-slate-300 mb-1">Transactional Outbox Poller Interval (ms)</label>
              <input
                type="number"
                value={pollInterval}
                onChange={(e) => setPollInterval(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-slate-200 font-mono"
              />
            </div>
          </div>
        </div>

        <div className="flex justify-end">
          <button
            type="submit"
            className="px-5 py-2.5 bg-brand-500 hover:bg-brand-600 text-slate-950 font-bold rounded-lg flex items-center space-x-2 shadow-lg shadow-brand-500/20 transition-all"
          >
            <Save className="w-4 h-4" />
            <span>Save Configuration</span>
          </button>
        </div>
      </form>
    </div>
  );
};
