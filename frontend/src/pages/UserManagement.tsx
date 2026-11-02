import React, { useEffect, useState } from 'react';
import { Users, Shield, CheckCircle2, UserCheck, RefreshCw } from 'lucide-react';
import api from '../services/api';

export const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await api.get('/users');
      if (res.data?.success) {
        setUsers(res.data.data || []);
      }
    } catch (e) {
      console.error('Failed to load users', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-100 tracking-tight">Platform Users &amp; Role-Based Access Control</h1>
          <p className="text-xs text-slate-400 mt-0.5">Manage operator identities, fine-grained financial permissions, and segregation of duties</p>
        </div>
        <button
          onClick={fetchUsers}
          className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Users Table */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden">
        <table className="w-full text-left text-xs text-slate-300">
          <thead className="bg-slate-950/80 text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800 font-sans">
            <tr>
              <th className="py-3 px-4">User</th>
              <th className="py-3 px-4">Username</th>
              <th className="py-3 px-4">Email</th>
              <th className="py-3 px-4">Assigned Roles</th>
              <th className="py-3 px-4">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60">
            {loading ? (
              <tr>
                <td colSpan={5} className="py-12 text-center text-slate-500 font-sans">Loading users...</td>
              </tr>
            ) : (
              users.map((u) => (
                <tr key={u.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="py-3 px-4 font-semibold text-slate-100 flex items-center space-x-2.5">
                    <div className="w-7 h-7 rounded-full bg-slate-800 flex items-center justify-center font-bold text-[11px] text-brand-400 font-mono">
                      {u.username?.substring(0, 2).toUpperCase()}
                    </div>
                    <span>{u.fullName}</span>
                  </td>
                  <td className="py-3 px-4 font-mono text-slate-300">{u.username}</td>
                  <td className="py-3 px-4 text-slate-400">{u.email}</td>
                  <td className="py-3 px-4">
                    <div className="flex flex-wrap gap-1">
                      {(u.roles || []).map((r: string) => (
                        <span key={r} className="px-2 py-0.5 rounded text-[10px] font-bold bg-brand-500/10 text-brand-400 border border-brand-500/20 font-mono">
                          {r}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="py-3 px-4">
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-500/10 text-emerald-400">
                      {u.status}
                    </span>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Permissions Matrix Reference */}
      <div className="bg-slate-900/40 border border-slate-800 rounded-xl p-5 text-xs space-y-3">
        <h3 className="font-bold text-slate-200 uppercase tracking-wider text-[11px]">Enforced RBAC Governance Matrix</h3>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 pt-2">
          <div className="p-3 rounded-lg bg-slate-950 border border-slate-800">
            <div className="font-bold text-purple-400 font-mono text-xs">ADMIN</div>
            <div className="text-[11px] text-slate-400 mt-1">Full platform administration, user provisioning, and configuration.</div>
          </div>
          <div className="p-3 rounded-lg bg-slate-950 border border-slate-800">
            <div className="font-bold text-blue-400 font-mono text-xs">FINANCE_ANALYST</div>
            <div className="text-[11px] text-slate-400 mt-1">Upload statements, run reconciliations, investigate with AI, submit adjustment proposals.</div>
          </div>
          <div className="p-3 rounded-lg bg-slate-950 border border-slate-800">
            <div className="font-bold text-amber-400 font-mono text-xs">FINANCE_APPROVER</div>
            <div className="text-[11px] text-slate-400 mt-1">Maker-Checker second-party authorization, approve/reject adjustment requests.</div>
          </div>
          <div className="p-3 rounded-lg bg-slate-950 border border-slate-800">
            <div className="font-bold text-emerald-400 font-mono text-xs">AUDITOR</div>
            <div className="text-[11px] text-slate-400 mt-1">Strictly read-only access to transactions, ledgers, reconciliations, and immutable audit logs.</div>
          </div>
        </div>
      </div>
    </div>
  );
};
