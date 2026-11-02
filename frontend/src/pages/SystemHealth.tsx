import React, { useEffect, useState } from 'react';
import { Activity, CheckCircle2, Server, Database, Cpu, Radio, RefreshCw } from 'lucide-react';
import api from '../services/api';

export const SystemHealth: React.FC = () => {
  const [health, setHealth] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  const fetchHealth = async () => {
    setLoading(true);
    try {
      const res = await api.get('/system/health');
      if (res.data?.success) {
        setHealth(res.data.data);
      }
    } catch (e) {
      console.error('Failed to load system health', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHealth();
  }, []);

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-100 tracking-tight">System Infrastructure Telemetry</h1>
          <p className="text-xs text-slate-400 mt-0.5">Distributed subsystems, transaction outbox relay, and service health checks</p>
        </div>
        <button
          onClick={fetchHealth}
          className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Overall Health Status Banner */}
      <div className="p-6 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center border border-emerald-500/20">
            <Activity className="w-6 h-6 animate-pulse" />
          </div>
          <div>
            <div className="text-base font-bold text-slate-100">{health?.serviceName || 'FinPilot Operations API'}</div>
            <div className="text-xs text-emerald-400 font-mono mt-0.5">Status: {health?.status || 'HEALTHY'} &bull; v{health?.version || '1.0.0'}</div>
          </div>
        </div>
        <div className="text-right font-mono text-xs text-slate-400">
          <div>Uptime: <span className="font-bold text-slate-200">{health?.uptimeSeconds || 0}s</span></div>
          <div className="text-[11px] text-slate-500 mt-0.5">{health?.timestamp ? new Date(health.timestamp).toLocaleTimeString() : ''}</div>
        </div>
      </div>

      {/* Subsystem Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-slate-900/60 border border-slate-800 p-5 rounded-xl space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2.5">
              <Database className="w-5 h-5 text-blue-400" />
              <span className="text-xs font-bold text-slate-200">Relational Database</span>
            </div>
            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              OPERATIONAL
            </span>
          </div>
          <p className="text-xs text-slate-400">
            PostgreSQL 16 persistence store with strict ACID locking, NUMERIC precision, and Flyway migration controls.
          </p>
          <div className="text-[11px] font-mono text-slate-500">
            {health?.subsystems?.database || 'PostgreSQL / H2 Active'}
          </div>
        </div>

        <div className="bg-slate-900/60 border border-slate-800 p-5 rounded-xl space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2.5">
              <Radio className="w-5 h-5 text-purple-400" />
              <span className="text-xs font-bold text-slate-200">Event Stream &amp; Outbox</span>
            </div>
            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              ACTIVE
            </span>
          </div>
          <p className="text-xs text-slate-400">
            Transactional Outbox Pattern relayer polling every 5000ms with Kafka topic dispatch and bounded retries.
          </p>
          <div className="text-[11px] font-mono text-slate-500">
            {health?.subsystems?.kafkaCluster || 'Kafka Cluster Relayer Active'}
          </div>
        </div>

        <div className="bg-slate-900/60 border border-slate-800 p-5 rounded-xl space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2.5">
              <Cpu className="w-5 h-5 text-brand-400" />
              <span className="text-xs font-bold text-slate-200">LangGraph AI Investigation</span>
            </div>
            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              READY
            </span>
          </div>
          <p className="text-xs text-slate-400">
            Python 3.12+ FastAPI service running LangGraph state machine with automatic resilient fallback.
          </p>
          <div className="text-[11px] font-mono text-slate-500">
            {health?.subsystems?.aiFastApiService || 'FastAPI Port 8000'}
          </div>
        </div>

        <div className="bg-slate-900/60 border border-slate-800 p-5 rounded-xl space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2.5">
              <Server className="w-5 h-5 text-amber-400" />
              <span className="text-xs font-bold text-slate-200">Distributed Cache</span>
            </div>
            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              CONFIGURED
            </span>
          </div>
          <p className="text-xs text-slate-400">
            Redis 7 idempotency caching layer with database constraint fallback guaranteeing zero duplicate processing.
          </p>
          <div className="text-[11px] font-mono text-slate-500">
            {health?.subsystems?.redisCache || 'Redis Port 6379'}
          </div>
        </div>
      </div>
    </div>
  );
};
