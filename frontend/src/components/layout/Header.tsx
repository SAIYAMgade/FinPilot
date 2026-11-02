import React from 'react';
import { Search, LogOut, Shield, UserCheck, RefreshCw, Cpu } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export const Header: React.FC = () => {
  const { user, logout, quickLoginAs } = useAuth();

  return (
    <header className="h-16 bg-slate-900/80 backdrop-blur border-b border-slate-800 px-6 flex items-center justify-between sticky top-0 z-20">
      {/* Search Bar */}
      <div className="flex items-center space-x-3 w-96">
        <div className="relative w-full">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search transactions, accounts, journal refs, exceptions..."
            className="w-full bg-slate-950/60 border border-slate-800 rounded-lg pl-9 pr-4 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-brand-500 transition-colors"
          />
        </div>
      </div>

      {/* Demo Persona Switcher & Controls */}
      <div className="flex items-center space-x-4">
        <div className="flex items-center space-x-1.5 bg-slate-950/60 border border-slate-800 p-1 rounded-lg">
          <span className="text-[10px] text-slate-400 font-semibold px-2 uppercase tracking-wider">Switch Persona:</span>
          <button
            onClick={() => quickLoginAs('admin')}
            className={`px-2 py-1 text-[11px] rounded font-medium transition-all ${
              user?.roles?.includes('ADMIN')
                ? 'bg-purple-500/20 text-purple-300 border border-purple-500/40'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Admin
          </button>
          <button
            onClick={() => quickLoginAs('analyst')}
            className={`px-2 py-1 text-[11px] rounded font-medium transition-all ${
              user?.roles?.includes('FINANCE_ANALYST')
                ? 'bg-blue-500/20 text-blue-300 border border-blue-500/40'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Analyst
          </button>
          <button
            onClick={() => quickLoginAs('approver')}
            className={`px-2 py-1 text-[11px] rounded font-medium transition-all ${
              user?.roles?.includes('FINANCE_APPROVER')
                ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Approver
          </button>
          <button
            onClick={() => quickLoginAs('auditor')}
            className={`px-2 py-1 text-[11px] rounded font-medium transition-all ${
              user?.roles?.includes('AUDITOR')
                ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Auditor
          </button>
        </div>

        {/* Live System Indicator */}
        <div className="flex items-center space-x-2 text-xs text-slate-400">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          <span className="text-[11px] font-mono font-medium text-emerald-400">Core Engine Online</span>
        </div>

        {/* Logout */}
        <button
          onClick={logout}
          title="Sign out of FinPilot"
          className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-slate-800 transition-colors"
        >
          <LogOut className="w-4 h-4" />
        </button>
      </div>
    </header>
  );
};
