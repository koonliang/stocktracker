import { ApiError } from '@/api/client';
import { useToastStore, type ToastTone } from '@/stores/toastStore';

export type ActionFeedbackScope =
  | 'transaction'
  | 'watchlist'
  | 'watchlist_ticker'
  | 'alert'
  | 'transaction_import'
  | 'transaction_export';

export type ActionFeedbackOperation = 'add' | 'update' | 'delete' | 'import' | 'export';
export type ActionFeedbackOutcome = 'success' | 'failure';

type ActionFeedbackDescriptor = {
  success: string;
  failure: string;
};

const DESCRIPTORS: Record<
  `${ActionFeedbackScope}:${ActionFeedbackOperation}`,
  ActionFeedbackDescriptor
> = {
  'transaction:add': {
    success: 'Transaction created',
    failure: 'Transaction could not be created',
  },
  'transaction:update': {
    success: 'Transaction updated',
    failure: 'Transaction could not be updated',
  },
  'transaction:delete': {
    success: 'Transaction deleted',
    failure: 'Transaction could not be deleted',
  },
  'watchlist:add': { success: 'Watchlist created', failure: 'Watchlist could not be created' },
  'watchlist:update': { success: 'Watchlist updated', failure: 'Watchlist could not be updated' },
  'watchlist:delete': { success: 'Watchlist deleted', failure: 'Watchlist could not be deleted' },
  'watchlist_ticker:add': {
    success: 'Ticker added to watchlist',
    failure: 'Ticker could not be added',
  },
  'watchlist_ticker:delete': {
    success: 'Ticker removed from watchlist',
    failure: 'Ticker could not be removed',
  },
  'alert:add': { success: 'Alert created', failure: 'Alert could not be created' },
  'alert:update': { success: 'Alert updated', failure: 'Alert could not be updated' },
  'alert:delete': { success: 'Alert deleted', failure: 'Alert could not be deleted' },
  'transaction_import:import': {
    success: 'Transactions imported',
    failure: 'Import needs attention',
  },
  'transaction_export:export': { success: 'Transactions exported', failure: 'Export failed' },
};

export function messageFromError(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return 'Request failed';
}

type NotifyOptions = {
  scope: ActionFeedbackScope;
  operation: ActionFeedbackOperation;
  outcome: ActionFeedbackOutcome;
  title?: string;
  message?: string;
  id?: string;
};

export function notifyActionFeedback({
  scope,
  operation,
  outcome,
  title,
  message,
  id,
}: NotifyOptions) {
  const descriptor = DESCRIPTORS[`${scope}:${operation}`];
  const tone: ToastTone = outcome === 'success' ? 'success' : 'error';
  return useToastStore.getState().pushToast({
    id,
    tone,
    title: title ?? descriptor[outcome],
    message,
  });
}
