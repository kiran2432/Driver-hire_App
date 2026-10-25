import type { ReactNode } from 'react';
import type { TripStatus } from '../api/types';
import { STATUS_LABEL } from '../utils/format';

export function Alert({ kind = 'error', children }: { kind?: 'error' | 'success' | 'info'; children: ReactNode }) {
  return (
    <div className={`alert alert-${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  );
}

export function StatusBadge({ status }: { status: TripStatus }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{STATUS_LABEL[status]}</span>;
}

export function Spinner({ label = 'Loading…' }: { label?: string }) {
  return (
    <p className="center muted" role="status">
      {label}
    </p>
  );
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="empty">
      <h3>{title}</h3>
      {children}
    </div>
  );
}

interface PaginationProps {
  page: number;
  totalPages: number;
  onPage: (page: number) => void;
}

export function Pagination({ page, totalPages, onPage }: PaginationProps) {
  if (totalPages <= 1) return null;
  return (
    <nav className="pagination" aria-label="Pagination">
      <button type="button" className="btn btn-ghost" disabled={page <= 0} onClick={() => onPage(page - 1)}>
        ← Previous
      </button>
      <span className="muted">
        Page {page + 1} of {totalPages}
      </span>
      <button
        type="button"
        className="btn btn-ghost"
        disabled={page >= totalPages - 1}
        onClick={() => onPage(page + 1)}
      >
        Next →
      </button>
    </nav>
  );
}
