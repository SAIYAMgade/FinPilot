import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../services/api';

export interface User {
  id: string;
  username: string;
  email: string;
  fullName: string;
  status: string;
  roles: string[];
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (usernameOrEmail: string, password: string) => Promise<void>;
  logout: () => void;
  quickLoginAs: (role: 'admin' | 'analyst' | 'approver' | 'auditor') => Promise<void>;
  hasRole: (role: string) => boolean;
  hasAnyRole: (roles: string[]) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('finpilot_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('finpilot_token'));

  const login = async (usernameOrEmail: string, password: string) => {
    const res = await api.post('/auth/login', { usernameOrEmail, password });
    if (res.data?.success && res.data?.data) {
      const { accessToken, user: userData } = res.data.data;
      localStorage.setItem('finpilot_token', accessToken);
      localStorage.setItem('finpilot_user', JSON.stringify(userData));
      setToken(accessToken);
      setUser(userData);
    }
  };

  const logout = () => {
    localStorage.removeItem('finpilot_token');
    localStorage.removeItem('finpilot_user');
    setToken(null);
    setUser(null);
  };

  const quickLoginAs = async (role: 'admin' | 'analyst' | 'approver' | 'auditor') => {
    await login(role, 'Password@123');
  };

  const hasRole = (role: string) => {
    if (!user || !user.roles) return false;
    return user.roles.includes(role);
  };

  const hasAnyRole = (targetRoles: string[]) => {
    if (!user || !user.roles) return false;
    return targetRoles.some((r) => user.roles.includes(r));
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        login,
        logout,
        quickLoginAs,
        hasRole,
        hasAnyRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
