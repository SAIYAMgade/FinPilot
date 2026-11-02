import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  ArrowLeftRight,
  BookOpen,
  FileSpreadsheet,
  Scale,
  GitCompare,
  AlertTriangle,
  Bot,
  ShieldCheck,
  History,
  Users,
  Activity,
  Settings,
  PlusCircle,
  FileCheck2,
  ChevronRight
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export const Sidebar: React.FC = () => {
  const { user } = useAuth();

  const navSections = [
    {
      title: 'OPERATIONS',
      items: [
        { label: 'Overview Dashboard', path: '/', icon: LayoutDashboard },
        { label: 'Transactions Feed', path: '/transactions', icon: ArrowLeftRight },
        { label: 'Reconciliation Runs', path: '/reconciliations', icon: GitCompare },
        { label: 'New Reconciliation', path: '/reconciliations/new', icon: PlusCircle },
        { label: 'Exceptions Queue', path: '/exceptions', icon: AlertTriangle, badge: 'Active' },
      ],
    },
    {
      title: 'DOUBLE-ENTRY LEDGER',
      items: [
        { label: 'Chart of Accounts', path: '/accounts', icon: BookOpen },
        { label: 'Journal Entries', path: '/journals', icon: FileSpreadsheet },
        { label: 'Trial Balance', path: '/trial-balance', icon: Scale },
      ],
    },
    {
      title: 'AGENTIC AI & GOVERNANCE',
      items: [
        { label: 'AI Investigations', path: '/investigations', icon: Bot, highlight: true },
        { label: 'Approval Center', path: '/approvals', icon: ShieldCheck, badge: 'Maker-Checker' },
        { label: 'Immutable Audit Logs', path: '/audit-logs', icon: History },
      ],
    },
    {
      title: 'ADMINISTRATION',
      items: [
        { label: 'User Management', path: '/users', icon: Users },
        { label: 'System Health', path: '/health', icon: Activity },
        { label: 'Platform Settings', path: '/settings', icon: Settings },
      ],
    },
  ];

  return (
    <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col h-screen fixed top-0 left-0 z-30 select-none">
      {/* Brand Header */}
      <div className="h-16 px-6 flex items-center justify-between border-b border-slate-800 bg-slate-900/50 backdrop-blur">
        <div className="flex items-center space-x-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-brand-500 to-emerald-700 flex items-center justify-center shadow-lg shadow-brand-500/20">
            <span className="font-extrabold text-white text-base">FP</span>
          </div>
          <div>
            <div className="font-extrabold text-slate-100 text-lg tracking-tight leading-none">FinPilot</div>
            <div className="text-[10px] text-emerald-400 font-semibold tracking-wider uppercase mt-0.5">Enterprise Ops</div>
          </div>
        </div>
      </div>

      {/* Navigation Sections */}
      <div className="flex-1 overflow-y-auto px-3 py-4 space-y-6">
        {navSections.map((sec, idx) => (
          <div key={idx}>
            <div className="px-3 text-[10px] font-bold tracking-wider text-slate-400 uppercase mb-2">
              {sec.title}
            </div>
            <div className="space-y-0.5">
              {sec.items.map((item, itemIdx) => {
                const Icon = item.icon;
                return (
                  <NavLink
                    key={itemIdx}
                    to={item.path}
                    end={item.path === '/'}
                    className={({ isActive }) =>
                      `flex items-center justify-between px-3 py-2 rounded-lg text-xs font-medium transition-all group ${
                        isActive
                          ? 'bg-brand-500/10 text-brand-400 font-semibold border border-brand-500/20'
                          : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                      }`
                    }
                  >
                    <div className="flex items-center space-x-2.5">
                      <Icon className="w-4 h-4 transition-transform group-hover:scale-110" />
                      <span>{item.label}</span>
                    </div>
                    {item.badge && (
                      <span className="text-[9px] font-semibold px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        {item.badge}
                      </span>
                    )}
                  </NavLink>
                );
              })}
            </div>
          </div>
        ))}
      </div>

      {/* User Session Footer */}
      <div className="p-3 border-t border-slate-800 bg-slate-950/40">
        <div className="flex items-center space-x-3 p-2 rounded-lg bg-slate-800/40 border border-slate-800">
          <div className="w-8 h-8 rounded-full bg-slate-700 flex items-center justify-center font-bold text-xs text-brand-400">
            {user?.username?.substring(0, 2).toUpperCase() || 'FP'}
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-xs font-semibold text-slate-200 truncate">{user?.fullName || 'Active User'}</div>
            <div className="text-[10px] text-emerald-400 font-mono truncate">{user?.roles?.[0] || 'ANALYST'}</div>
          </div>
        </div>
      </div>
    </aside>
  );
};
