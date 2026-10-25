import { useCallback, useEffect, useRef, useState } from 'react';
import { errorMessage } from './format';

interface LoadState<T> {
  data: T | null;
  error: string | null;
  loading: boolean;
}

/**
 * Runs `loader` whenever `deps` change and exposes `reload`. Stale responses are ignored.
 * `reload(true)` refreshes in the background without flipping `loading` (used for polling).
 */
export function useLoad<T>(loader: () => Promise<T>, deps: readonly unknown[]) {
  const [state, setState] = useState<LoadState<T>>({ data: null, error: null, loading: true });
  const [tick, setTick] = useState(0);
  const silent = useRef(false);
  const loaderRef = useRef(loader);
  loaderRef.current = loader;

  useEffect(() => {
    let cancelled = false;
    if (!silent.current) setState((s) => ({ ...s, loading: true, error: null }));
    silent.current = false;

    loaderRef.current().then(
      (data) => {
        if (!cancelled) setState({ data, error: null, loading: false });
      },
      (e: unknown) => {
        if (!cancelled) setState((s) => ({ data: s.data, error: errorMessage(e), loading: false }));
      },
    );
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);

  const reload = useCallback((isSilent = false) => {
    silent.current = isSilent;
    setTick((t) => t + 1);
  }, []);

  return { ...state, reload };
}
