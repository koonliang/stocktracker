import { useState } from 'react'
import { Modal } from '../common/Modal'
import { TransactionGrid } from './TransactionGrid'
import { TransactionForm } from './TransactionForm'
import type { TransactionResponse, TransactionRequest } from '../../services/api/transactionApi'

export interface TransactionModalProps {
  isOpen: boolean
  onClose: () => void
  isClosing?: boolean
  transactions: TransactionResponse[]
  onCreateTransaction: (request: TransactionRequest) => Promise<void>
  onUpdateTransaction: (id: number, request: TransactionRequest) => Promise<void>
  onDeleteTransaction: (id: number) => Promise<void>
  loading?: boolean
}

export function TransactionModal({
  isOpen,
  onClose,
  isClosing = false,
  transactions,
  onCreateTransaction,
  onUpdateTransaction,
  onDeleteTransaction,
  loading = false,
}: TransactionModalProps) {
  const [showAddForm, setShowAddForm] = useState(false)

  const handleCreate = async (request: TransactionRequest) => {
    await onCreateTransaction(request)
    setShowAddForm(false)
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      isClosing={isClosing}
      title="Manage Transactions"
      size="xl"
      showCloseButton={true}
      closeOnEscape={true}
      closeOnBackdropClick={true}
    >
      {loading ? (
        <div className="flex items-center justify-center py-12">
          <p className="text-slate-500">Loading transactions...</p>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Add Transaction Section - Always at Top */}
          <div className="border-b border-slate-200 pb-6">
            {showAddForm ? (
              <div className="rounded-lg border border-indigo-200 bg-indigo-50 p-4">
                <div className="mb-3 flex items-center justify-between">
                  <h3 className="font-semibold text-indigo-900">New Transaction</h3>
                  <button
                    onClick={() => setShowAddForm(false)}
                    className="text-sm text-slate-500 hover:text-slate-700"
                  >
                    Cancel
                  </button>
                </div>
                <TransactionForm onSubmit={handleCreate} onCancel={() => setShowAddForm(false)} />
              </div>
            ) : (
              <button
                onClick={() => setShowAddForm(true)}
                className="flex w-full items-center justify-center gap-2 rounded-lg border-2 border-dashed
                         border-slate-300 bg-slate-50 py-4 text-slate-600 transition-colors
                         hover:border-indigo-400 hover:bg-indigo-50 hover:text-indigo-600"
              >
                <svg
                  className="h-5 w-5"
                  fill="none"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path d="M12 4v16m8-8H4" />
                </svg>
                <span className="font-medium">Add Transaction</span>
              </button>
            )}
          </div>

          {/* Transaction List Section */}
          <div>
            <h3 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">
              Transaction History
            </h3>
            <TransactionGrid
              transactions={transactions}
              onCreate={onCreateTransaction}
              onUpdate={onUpdateTransaction}
              onDelete={onDeleteTransaction}
            />
          </div>
        </div>
      )}
    </Modal>
  )
}
