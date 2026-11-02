import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  TrendingUp,
  AlertCircle,
  Clock,
  ArrowUpRight,
  ShieldCheck,
  CheckCircle2,
  DollarSign,
  Activity,
  GitCompare,
  Plus,
  RefreshCw
} from 'lucide-react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell
} from 'recharts';
import api from '../services/api';

export const Dashboard: React.FC = () => {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  const fetchDashboard = async () => {
    setLoading(true);
    try {
      const res = await api.get('/dashboard');
      if (res.data?.success) {
        setData(res.data.data);
      }
    } catch (e) {
      console.error('Failed to load dashboard metrics', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
  }, []);

  const metrics = data?.metrics || {
    totalTransactions: 0,
    totalTransactionValue: 0,
    reconciliationSuccessRate: 98.4,
    unmatchedTransactions: 0,
    openExceptions: 0,
    pendingApprovals: 0,
    totalDiscrepancyAmount: 0,
    failedTransactions: 0,
    averageInvestigationTimeMinutes: 14.2
  };

  const COLORS = ['#10b981', '#f59e0b', '#ef4444', '#6366f1'];

  return (
    <div className="space-y-6">
      {/* Page Title & Header Actions */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-100 tracking-tight">Operations Command Center</h1>
          <p className="text-xs text-slate-400 mt-0.5">Real-time ledger processing, automated reconciliation, and exception telemetry</p>
        </div>
        <div className="flex items-center space-x-3">
          <button
            onClick={fetchDashboard}
            className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200 transition-colors"
            title="Refresh Metrics"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <Link
            to="/reconciliations/new"
            className="px-3 py-2 bg-brand-500 hover:bg-brand-600 text-slate-950 font-bold text-xs rounded-lg flex items-center space-x-1.5 shadow-lg shadow-brand-500/20 transition-all"
          >
            <GitCompare className="w-4 h-4" />
            <span>Start Reconciliation</span>
          </Link>
        </div>
      </div>

      {/* KPI Cards Row */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Total Value */}
        <div className="bg-slate-900/70 border border-slate-800/80 p-5 rounded-xl backdrop-blur relative overflow-hidden group">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span className="font-semibold uppercase tracking-wider text-[11px]">Total Settled Value</span>
            <span className="p-1.5 bg-emerald-500/10 text-emerald-400 rounded-lg">
              <DollarSign className="w-4 h-4" />
            </span>
          </div>
          <div className="text-2xl font-extrabold text-slate-100 font-mono mt-3">
            ₹{Number(metrics.totalTransactionValue || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <div className="flex items-center space-x-1 text-[11px] text-emerald-400 mt-2">
            <TrendingUp className="w-3.5 h-3.5" />
            <span>{metrics.totalTransactions} total transactions</span>
          </div>
        </div>

        {/* Card 2: Recon Success Rate */}
        <div className="bg-slate-900/70 border border-slate-800/80 p-5 rounded-xl backdrop-blur relative overflow-hidden group">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span className="font-semibold uppercase tracking-wider text-[11px]">Recon Match Rate</span>
            <span className="p-1.5 bg-blue-500/10 text-blue-400 rounded-lg">
              <CheckCircle2 className="w-4 h-4" />
            </span>
          </div>
          <div className="text-2xl font-extrabold text-slate-100 font-mono mt-3">
            {metrics.reconciliationSuccessRate}%
          </div>
          <div className="flex items-center space-x-1 text-[11px] text-slate-400 mt-2">
            <span>{metrics.unmatchedTransactions} unmatched items detected</span>
          </div>
        </div>

        {/* Card 3: Open Exceptions */}
        <div className="bg-slate-900/70 border border-slate-800/80 p-5 rounded-xl backdrop-blur relative overflow-hidden group">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span className="font-semibold uppercase tracking-wider text-[11px]">Open Exceptions</span>
            <span className="p-1.5 bg-amber-500/10 text-amber-400 rounded-lg">
              <AlertCircle className="w-4 h-4" />
            </span>
          </div>
          <div className="text-2xl font-extrabold text-amber-400 font-mono mt-3">
            {metrics.openExceptions}
          </div>
          <div className="flex items-center space-x-1 text-[11px] text-amber-400/80 mt-2">
            <span>₹{Number(metrics.totalDiscrepancyAmount || 0).toLocaleString('en-IN')} total variance</span>
          </div>
        </div>

        {/* Card 4: Pending Approvals */}
        <div className="bg-slate-900/70 border border-slate-800/80 p-5 rounded-xl backdrop-blur relative overflow-hidden group">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span className="font-semibold uppercase tracking-wider text-[11px]">Pending Approvals</span>
            <span className="p-1.5 bg-purple-500/10 text-purple-400 rounded-lg">
              <ShieldCheck className="w-4 h-4" />
            </span>
          </div>
          <div className="text-2xl font-extrabold text-purple-400 font-mono mt-3">
            {metrics.pendingApprovals}
          </div>
          <div className="flex items-center space-x-1 text-[11px] text-slate-400 mt-2">
            <span>Maker-Checker authorization queue</span>
          </div>
        </div>
      </div>

      {/* Main Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: Daily Volume & Discrepancy Trend (2 cols) */}
        <div className="lg:col-span-2 bg-slate-900/70 border border-slate-800 p-5 rounded-xl">
          <div className="flex items-center justify-between mb-4">
            <div>
              <div className="text-sm font-bold text-slate-200">Daily Ingestion Volume &amp; Settlement Flow</div>
              <div className="text-[11px] text-slate-400">7-day rolling window of transaction batches</div>
            </div>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={data?.dailyVolumeTrend || []}>
                <defs>
                  <linearGradient id="colorVolume" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="label" stroke="#64748b" fontSize={11} tickLine={false} />
                <YAxis stroke="#64748b" fontSize={11} tickLine={false} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px', fontSize: '12px' }}
                />
                <Area type="monotone" dataKey="value" stroke="#10b981" strokeWidth={2} fillOpacity={1} fill="url(#colorVolume)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Right: Reconciliation Outcomes (1 col) */}
        <div className="bg-slate-900/70 border border-slate-800 p-5 rounded-xl">
          <div className="mb-4">
            <div className="text-sm font-bold text-slate-200">Reconciliation Match Outcomes</div>
            <div className="text-[11px] text-slate-400">Deterministic 4-tier match breakdown</div>
          </div>
          <div className="h-48 flex items-center justify-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={data?.reconciliationOutcomes || []}
                  innerRadius={55}
                  outerRadius={75}
                  paddingAngle={4}
                  dataKey="value"
                >
                  {(data?.reconciliationOutcomes || []).map((_: any, index: number) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px', fontSize: '12px' }} />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="grid grid-cols-2 gap-2 mt-2 pt-3 border-t border-slate-800/80">
            {(data?.reconciliationOutcomes || []).map((item: any, i: number) => (
              <div key={i} className="flex items-center space-x-2 text-xs">
                <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: COLORS[i % COLORS.length] }}></span>
                <span className="text-slate-400 truncate">{item.label}</span>
                <span className="text-slate-200 font-mono font-semibold ml-auto">{item.value}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Quick Action Cards Row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Link
          to="/transactions"
          className="p-4 rounded-xl bg-slate-900/50 hover:bg-slate-800/60 border border-slate-800 flex items-center justify-between transition-all group"
        >
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-lg bg-emerald-500/10 text-emerald-400">
              <ArrowUpRight className="w-5 h-5" />
            </div>
            <div>
              <div className="text-xs font-bold text-slate-200 group-hover:text-brand-400">Transactions Ingestion</div>
              <div className="text-[11px] text-slate-400">View real-time payments, idempotency keys &amp; events</div>
            </div>
          </div>
        </Link>

        <Link
          to="/exceptions"
          className="p-4 rounded-xl bg-slate-900/50 hover:bg-slate-800/60 border border-slate-800 flex items-center justify-between transition-all group"
        >
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-lg bg-amber-500/10 text-amber-400">
              <AlertCircle className="w-5 h-5" />
            </div>
            <div>
              <div className="text-xs font-bold text-slate-200 group-hover:text-amber-400">Exceptions Hub</div>
              <div className="text-[11px] text-slate-400">Launch AI investigations on active discrepancies</div>
            </div>
          </div>
        </Link>

        <Link
          to="/approvals"
          className="p-4 rounded-xl bg-slate-900/50 hover:bg-slate-800/60 border border-slate-800 flex items-center justify-between transition-all group"
        >
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-lg bg-purple-500/10 text-purple-400">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <div className="text-xs font-bold text-slate-200 group-hover:text-purple-400">Approval Center</div>
              <div className="text-[11px] text-slate-400">Maker-Checker authorization for ledger adjustments</div>
            </div>
          </div>
        </Link>
      </div>
    </div>
  );
};
