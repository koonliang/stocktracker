import { TransactionType } from '../database/types';
import Decimal from 'decimal.js';
import { transactionService } from '../transaction/transaction.service';
import { yahooFinanceService } from '../external/yahoo-finance.service';

// Configure Decimal for financial calculations
Decimal.set({ rounding: Decimal.ROUND_HALF_UP, precision: 20 });

const MAX_ROWS = 1000;

// Field mapping aliases (40+ variations)
const FIELD_ALIASES: Record<string, string[]> = {
  type: [
    'action',
    'type',
    'transaction type',
    'trade type',
    'buy/sell',
    'side',
    'order type',
    'transaction',
  ],
  symbol: [
    'symbol',
    'ticker',
    'stock',
    'security',
    'instrument',
    'stock symbol',
    'ticker symbol',
    'code',
  ],
  exchange: [
    'exchange',
    'listing exchange',
    'market',
    'listingexchange',
    'stock exchange',
  ],
  transactionDate: [
    'date',
    'trade date',
    'transaction date',
    'settlement date',
    'exec date',
    'execution date',
    'activity date',
    'tradedate',
  ],
  shares: ['shares', 'quantity', 'qty', 'units', 'amount', 'volume', 'share quantity'],
  pricePerShare: [
    'price',
    'share price',
    'unit price',
    'execution price',
    'trade price',
    'cost per share',
    'price per share',
    't. price',
  ],
  brokerFee: [
    'ibcommission',
    'commission',
    'fee',
    'fees',
    'broker fee',
    'brokerage',
    'brokerage fee',
    'trading fee',
    'transaction fee',
    'clearing fee',
    'tax',
    'stamp duty',
    'transaction cost',
    'cost',
  ],
  notes: ['notes', 'memo', 'description', 'comment', 'remarks'],
};

// Exchange code to Yahoo Finance suffix mapping (16 exchanges)
const EXCHANGE_SUFFIX_MAP: Record<string, string> = {
  // London Stock Exchange
  LSE: '.L',
  LSEETF: '.L',
  LON: '.L',
  // Hong Kong Stock Exchange
  SEHK: '.HK',
  HKG: '.HK',
  // Toronto Stock Exchange
  TSE: '.TO',
  TSX: '.TO',
  // Australian Securities Exchange
  ASX: '.AX',
  // Deutsche Börse
  XETRA: '.DE',
  FRA: '.F',
  // Euronext Paris
  EPA: '.PA',
  // Swiss Exchange
  SIX: '.SW',
  // Amsterdam
  AMS: '.AS',
  // Brussels
  EBR: '.BR',
  // Milan
  MIL: '.MI',
  // Madrid
  MCE: '.MC',
  // Copenhagen
  CSE: '.CO',
  // Stockholm
  STO: '.ST',
  // Oslo
  OSE: '.OL',
  // Singapore
  SGX: '.SI',
  // Tokyo
  TYO: '.T',
  // No suffix needed for US exchanges
  NASDAQ: '',
  NYSE: '',
  AMEX: '',
  ARCA: '',
};

// Transaction type mapping
const TYPE_MAPPINGS: Record<string, TransactionType> = {
  buy: TransactionType.BUY,
  b: TransactionType.BUY,
  purchase: TransactionType.BUY,
  bought: TransactionType.BUY,
  'you bought': TransactionType.BUY,
  bot: TransactionType.BUY,
  sell: TransactionType.SELL,
  s: TransactionType.SELL,
  sale: TransactionType.SELL,
  sold: TransactionType.SELL,
  'you sold': TransactionType.SELL,
  sld: TransactionType.SELL,
};

const MONTH_ABBR: Record<string, number> = {
  jan: 1,
  feb: 2,
  mar: 3,
  apr: 4,
  may: 5,
  jun: 6,
  jul: 7,
  aug: 8,
  sep: 9,
  oct: 10,
  nov: 11,
  dec: 12,
};

// Types
export interface CsvImportError {
  rowNumber: number;
  field: string | null;
  message: string;
  value: string | null;
}

export interface TransactionPreviewRow {
  rowNumber: number;
  valid: boolean;
  type?: TransactionType;
  symbol?: string;
  transactionDate?: Date;
  shares?: number;
  pricePerShare?: number;
  brokerFee?: number | null;
  notes?: string | null;
  errors: CsvImportError[];
}

export interface CsvRowData {
  rowNumber: number;
  values: Record<string, string>;
}

export interface CsvMappingSuggestionResponse {
  suggestedMappings: Record<string, string>;
  confidenceScores: Record<string, number>;
  unmappedColumns: string[];
}

export interface CsvImportPreviewResponse {
  validRows: TransactionPreviewRow[];
  errorRows: TransactionPreviewRow[];
  totalRows: number;
  validCount: number;
  errorCount: number;
}

export interface CsvImportRequest {
  rows: CsvRowData[];
  fieldMappings: Record<string, string>;
}

export interface ImportedTransactionResponse {
  id: number;
  type: TransactionType;
  symbol: string;
  companyName: string;
  transactionDate: string;
  shares: number;
  pricePerShare: number;
  brokerFee: number | null;
  totalAmount: number;
  notes: string | null;
}

export interface CsvImportResultResponse {
  importedCount: number;
  skippedCount: number;
  errors: CsvImportError[];
  importedTransactions: ImportedTransactionResponse[];
}

interface FieldMatch {
  fieldName: string;
  confidence: number;
}

export class CsvImportService {
  /**
   * Calculate Levenshtein distance between two strings
   */
  private levenshteinDistance(s1: string, s2: string): number {
    const dp: number[][] = Array(s1.length + 1)
      .fill(null)
      .map(() => Array(s2.length + 1).fill(0));

    for (let i = 0; i <= s1.length; i++) {
      dp[i][0] = i;
    }

    for (let j = 0; j <= s2.length; j++) {
      dp[0][j] = j;
    }

    for (let i = 1; i <= s1.length; i++) {
      for (let j = 1; j <= s2.length; j++) {
        const cost = s1.charAt(i - 1) === s2.charAt(j - 1) ? 0 : 1;
        dp[i][j] = Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost);
      }
    }

    return dp[s1.length][s2.length];
  }

  /**
   * Calculate confidence score for field matching (3-tier scoring)
   */
  private calculateConfidence(header: string, alias: string): number {
    // Exact match
    if (header === alias) {
      return 1.0;
    }

    // Contains match
    if (header.includes(alias) || alias.includes(header)) {
      return 0.9;
    }

    // Levenshtein distance
    const distance = this.levenshteinDistance(header, alias);
    const maxLength = Math.max(header.length, alias.length);

    if (maxLength === 0) {
      return 0.0;
    }

    const similarity = 1.0 - distance / maxLength;
    return similarity > 0.7 ? similarity : 0.0;
  }

  /**
   * Find the best field match using fuzzy matching
   */
  private findBestFieldMatch(normalizedHeader: string): FieldMatch | null {
    let bestMatch: FieldMatch | null = null;
    let bestConfidence = 0.0;

    for (const [fieldName, aliases] of Object.entries(FIELD_ALIASES)) {
      for (const alias of aliases) {
        const confidence = this.calculateConfidence(normalizedHeader, alias);

        if (confidence > bestConfidence) {
          bestConfidence = confidence;
          bestMatch = { fieldName, confidence };
        }
      }
    }

    return bestMatch;
  }

  /**
   * Suggest field mappings based on CSV headers
   */
  suggestMappings(headers: string[]): CsvMappingSuggestionResponse {
    console.log('Suggesting mappings for headers:', headers);

    const suggestedMappings: Record<string, string> = {};
    const confidenceScores: Record<string, number> = {};
    const bestMatchPerField: Record<string, string> = {};
    const bestConfidencePerField: Record<string, number> = {};
    const unmappedColumns: string[] = [];

    // First pass: find the best CSV column for each standard field
    for (const header of headers) {
      const normalizedHeader = header.toLowerCase().trim();

      // Try to find best match
      const bestMatch = this.findBestFieldMatch(normalizedHeader);

      if (bestMatch && bestMatch.confidence >= 0.6) {
        const { fieldName, confidence } = bestMatch;

        // Only keep this mapping if it's better than what we already have for this field
        if (!bestMatchPerField[fieldName] || confidence > bestConfidencePerField[fieldName]) {
          // Remove old mapping if it exists
          const oldHeader = bestMatchPerField[fieldName];
          if (oldHeader) {
            delete suggestedMappings[oldHeader];
            delete confidenceScores[oldHeader];
            unmappedColumns.push(oldHeader);
          }

          // Add new mapping
          bestMatchPerField[fieldName] = header;
          bestConfidencePerField[fieldName] = confidence;
          suggestedMappings[header] = fieldName;
          confidenceScores[header] = confidence;
        } else {
          unmappedColumns.push(header);
        }
      } else {
        unmappedColumns.push(header);
      }
    }

    return {
      suggestedMappings,
      confidenceScores,
      unmappedColumns,
    };
  }

  /**
   * Parse transaction type
   */
  private parseType(
    value: string | undefined,
    rowNumber: number,
    errors: CsvImportError[],
  ): TransactionType | null {
    if (!value || value.trim() === '') {
      errors.push({
        rowNumber,
        field: 'type',
        message: 'Type is required',
        value: value || null,
      });
      return null;
    }

    const normalized = value.toLowerCase().trim();
    const type = TYPE_MAPPINGS[normalized];

    if (!type) {
      errors.push({
        rowNumber,
        field: 'type',
        message: 'Invalid type. Must be BUY or SELL (or common variations)',
        value,
      });
    }

    return type || null;
  }

  /**
   * Parse symbol with optional exchange suffix
   */
  private parseSymbol(
    value: string | undefined,
    exchange: string | undefined,
    rowNumber: number,
    errors: CsvImportError[],
  ): string | null {
    if (!value || value.trim() === '') {
      errors.push({
        rowNumber,
        field: 'symbol',
        message: 'Symbol is required',
        value: value || null,
      });
      return null;
    }

    let symbol = value.toUpperCase().trim();

    // Add exchange suffix if exchange is provided and mapped
    if (exchange && exchange.trim() !== '') {
      const exchangeUpper = exchange.toUpperCase().trim();
      const suffix = EXCHANGE_SUFFIX_MAP[exchangeUpper];

      if (suffix && suffix !== '') {
        // Only add suffix if not already present
        if (!symbol.includes('.')) {
          symbol = symbol + suffix;
        }
      }
    }

    if (symbol.length > 15 || !/^[A-Z0-9.]+$/.test(symbol)) {
      errors.push({
        rowNumber,
        field: 'symbol',
        message: 'Invalid symbol format',
        value,
      });
      return null;
    }

    return symbol;
  }

  /**
   * Parse transaction date (supports 8 formats)
   */
  private parseDate(
    value: string | undefined,
    rowNumber: number,
    errors: CsvImportError[],
  ): Date | null {
    if (!value || value.trim() === '') {
      errors.push({
        rowNumber,
        field: 'transactionDate',
        message: 'Date is required',
        value: value || null,
      });
      return null;
    }

    // Try M/D/YYYY or MM/DD/YYYY
    const mdyMatch = value.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
    if (mdyMatch) {
      const month = parseInt(mdyMatch[1], 10);
      const day = parseInt(mdyMatch[2], 10);
      const year = parseInt(mdyMatch[3], 10);
      const date = new Date(year, month - 1, day);
      if (date > new Date()) {
        errors.push({
          rowNumber,
          field: 'transactionDate',
          message: 'Transaction date cannot be in the future',
          value,
        });
        return null;
      }
      return date;
    }

    // Try YYYY-MM-DD
    const ymdMatch = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (ymdMatch) {
      const year = parseInt(ymdMatch[1], 10);
      const month = parseInt(ymdMatch[2], 10);
      const day = parseInt(ymdMatch[3], 10);
      const date = new Date(year, month - 1, day);
      if (date > new Date()) {
        errors.push({
          rowNumber,
          field: 'transactionDate',
          message: 'Transaction date cannot be in the future',
          value,
        });
        return null;
      }
      return date;
    }

    // Try DD-MMM-YYYY
    const dmmyMatch = value.match(/^(\d{2})-([A-Za-z]{3})-(\d{4})$/);
    if (dmmyMatch) {
      const day = parseInt(dmmyMatch[1], 10);
      const monthStr = dmmyMatch[2].toLowerCase();
      const year = parseInt(dmmyMatch[3], 10);
      const month = MONTH_ABBR[monthStr];
      if (month) {
        const date = new Date(year, month - 1, day);
        if (date > new Date()) {
          errors.push({
            rowNumber,
            field: 'transactionDate',
            message: 'Transaction date cannot be in the future',
            value,
          });
          return null;
        }
        return date;
      }
    }

    // Try YYYYMMDD
    const yyyymmddMatch = value.match(/^(\d{4})(\d{2})(\d{2})$/);
    if (yyyymmddMatch) {
      const year = parseInt(yyyymmddMatch[1], 10);
      const month = parseInt(yyyymmddMatch[2], 10);
      const day = parseInt(yyyymmddMatch[3], 10);
      const date = new Date(year, month - 1, day);
      if (date > new Date()) {
        errors.push({
          rowNumber,
          field: 'transactionDate',
          message: 'Transaction date cannot be in the future',
          value,
        });
        return null;
      }
      return date;
    }

    // Try M/D/YY or MM/DD/YY (assumes 20xx for years < 70, 19xx otherwise)
    const mdyShortMatch = value.match(/^(\d{1,2})\/(\d{1,2})\/(\d{2})$/);
    if (mdyShortMatch) {
      const month = parseInt(mdyShortMatch[1], 10);
      const day = parseInt(mdyShortMatch[2], 10);
      let year = parseInt(mdyShortMatch[3], 10);
      year = year < 70 ? 2000 + year : 1900 + year;
      const date = new Date(year, month - 1, day);
      if (date > new Date()) {
        errors.push({
          rowNumber,
          field: 'transactionDate',
          message: 'Transaction date cannot be in the future',
          value,
        });
        return null;
      }
      return date;
    }

    errors.push({
      rowNumber,
      field: 'transactionDate',
      message: 'Invalid date format. Expected formats: MM/DD/YYYY, YYYY-MM-DD, etc.',
      value,
    });
    return null;
  }

  /**
   * Parse shares (allows negative values for IBKR pattern where negative = sell)
   */
  private parseSharesWithSign(
    value: string | undefined,
    rowNumber: number,
    errors: CsvImportError[],
  ): Decimal | null {
    if (!value || value.trim() === '') {
      errors.push({
        rowNumber,
        field: 'shares',
        message: 'Shares is required',
        value: value || null,
      });
      return null;
    }

    try {
      // Remove commas and whitespace (but keep negative sign)
      const cleaned = value.replace(/[,\s]/g, '');
      const shares = new Decimal(cleaned);

      // Allow zero or negative (will be handled by caller)
      if (shares.isZero()) {
        errors.push({
          rowNumber,
          field: 'shares',
          message: 'Shares cannot be zero',
          value,
        });
        return null;
      }

      return shares;
    } catch {
      errors.push({
        rowNumber,
        field: 'shares',
        message: 'Invalid number format for shares',
        value,
      });
      return null;
    }
  }

  /**
   * Parse price per share
   */
  private parsePrice(
    value: string | undefined,
    rowNumber: number,
    errors: CsvImportError[],
  ): number | null {
    if (!value || value.trim() === '') {
      errors.push({
        rowNumber,
        field: 'pricePerShare',
        message: 'Price per share is required',
        value: value || null,
      });
      return null;
    }

    try {
      // Remove currency symbols, commas, and whitespace
      const cleaned = value.replace(/[$,\s]/g, '');
      const price = new Decimal(cleaned);

      if (price.lessThanOrEqualTo(0)) {
        errors.push({
          rowNumber,
          field: 'pricePerShare',
          message: 'Price per share must be greater than zero',
          value,
        });
        return null;
      }

      return price.toNumber();
    } catch {
      errors.push({
        rowNumber,
        field: 'pricePerShare',
        message: 'Invalid number format for price',
        value,
      });
      return null;
    }
  }

  /**
   * Parse broker fee (optional, can be negative in some exports - convert to absolute value)
   */
  private parseBrokerFee(
    value: string | undefined,
    rowNumber: number,
    errors: CsvImportError[],
  ): number | null {
    if (!value || value.trim() === '') {
      return null; // Optional field
    }

    try {
      // Remove currency symbols, commas, and whitespace
      const cleaned = value.replace(/[$€£,\s]/g, '');
      let fee = new Decimal(cleaned);

      // IBKR and some brokers report fees as negative values
      // Convert to positive (absolute value) for storage
      fee = fee.abs();

      return fee.toNumber();
    } catch {
      errors.push({
        rowNumber,
        field: 'brokerFee',
        message: 'Invalid number format for broker fee',
        value,
      });
      return null;
    }
  }

  /**
   * Extract mapped values from row data
   */
  private extractMappedValues(
    rowValues: Record<string, string>,
    fieldMappings: Record<string, string>,
  ): Record<string, string> {
    const mappedValues: Record<string, string> = {};

    for (const [csvColumn, standardField] of Object.entries(fieldMappings)) {
      const value = rowValues[csvColumn];

      if (value && value.trim() !== '') {
        mappedValues[standardField] = value.trim();
      }
    }

    return mappedValues;
  }

  /**
   * Validate and map a single row
   */
  private validateAndMapRow(
    rowData: CsvRowData,
    fieldMappings: Record<string, string>,
  ): TransactionPreviewRow {
    const previewRow: TransactionPreviewRow = {
      rowNumber: rowData.rowNumber,
      valid: false,
      errors: [],
    };

    const rowErrors: CsvImportError[] = [];

    try {
      // Extract mapped values
      const mappedValues = this.extractMappedValues(rowData.values, fieldMappings);

      // Parse shares first (may be negative for SELL transactions)
      const sharesValue = mappedValues.shares;
      const shares = this.parseSharesWithSign(sharesValue, rowData.rowNumber, rowErrors);

      // Infer type from shares sign if not explicitly provided (IBKR pattern)
      let type: TransactionType | null;
      const typeValue = mappedValues.type;

      if (!typeValue || typeValue === '') {
        // No explicit type - infer from shares sign
        if (shares && shares.lessThan(0)) {
          type = TransactionType.SELL;
          // Convert to positive
          previewRow.shares = shares.abs().toNumber();
        } else {
          type = TransactionType.BUY;
          previewRow.shares = shares?.toNumber() || 0;
        }
      } else {
        // Explicit type provided
        type = this.parseType(typeValue, rowData.rowNumber, rowErrors);
        // If type is SELL and shares are negative, make them positive
        if (type === TransactionType.SELL && shares && shares.lessThan(0)) {
          previewRow.shares = shares.abs().toNumber();
        } else {
          previewRow.shares = shares?.toNumber() || 0;
        }
      }

      const exchange = mappedValues.exchange;
      const symbol = this.parseSymbol(
        mappedValues.symbol,
        exchange,
        rowData.rowNumber,
        rowErrors,
      );
      const transactionDate = this.parseDate(
        mappedValues.transactionDate,
        rowData.rowNumber,
        rowErrors,
      );
      const pricePerShare = this.parsePrice(
        mappedValues.pricePerShare,
        rowData.rowNumber,
        rowErrors,
      );
      const brokerFee = this.parseBrokerFee(
        mappedValues.brokerFee,
        rowData.rowNumber,
        rowErrors,
      );
      const notes = mappedValues.notes || null;

      previewRow.type = type || undefined;
      previewRow.symbol = symbol || undefined;
      previewRow.transactionDate = transactionDate || undefined;
      previewRow.pricePerShare = pricePerShare || undefined;
      previewRow.brokerFee = brokerFee;
      previewRow.notes = notes;
      previewRow.valid = rowErrors.length === 0;
      previewRow.errors = rowErrors;
    } catch (error) {
      console.error(`Error mapping row ${rowData.rowNumber}:`, error);
      rowErrors.push({
        rowNumber: rowData.rowNumber,
        field: null,
        message: `Mapping error: ${error instanceof Error ? error.message : 'Unknown error'}`,
        value: null,
      });
      previewRow.valid = false;
      previewRow.errors = rowErrors;
    }

    return previewRow;
  }

  /**
   * Validate that all required fields are mapped
   */
  private validateRequiredMappings(fieldMappings: Record<string, string>): void {
    const requiredFields = ['symbol', 'transactionDate', 'shares', 'pricePerShare'];
    const mappedFields = Object.values(fieldMappings);

    const missingFields = requiredFields.filter((field) => !mappedFields.includes(field));

    if (missingFields.length > 0) {
      throw new Error(`Missing required field mappings: ${missingFields.join(', ')}`);
    }
  }

  /**
   * Preview import with validation
   */
  async previewImport(request: CsvImportRequest): Promise<CsvImportPreviewResponse> {
    console.log(`Previewing import of ${request.rows.length} rows`);

    if (request.rows.length > MAX_ROWS) {
      throw new Error(`Cannot import more than ${MAX_ROWS} rows at once`);
    }

    this.validateRequiredMappings(request.fieldMappings);

    const validRows: TransactionPreviewRow[] = [];
    const errorRows: TransactionPreviewRow[] = [];

    for (const rowData of request.rows) {
      const previewRow = this.validateAndMapRow(rowData, request.fieldMappings);
      if (previewRow.valid) {
        validRows.push(previewRow);
      } else {
        errorRows.push(previewRow);
      }
    }

    return {
      validRows,
      errorRows,
      totalRows: request.rows.length,
      validCount: validRows.length,
      errorCount: errorRows.length,
    };
  }

  /**
   * Execute the import of validated transactions
   */
  async executeImport(
    userId: number,
    request: CsvImportRequest,
  ): Promise<CsvImportResultResponse> {
    console.log(`Executing import for user ${userId} with ${request.rows.length} rows`);

    if (request.rows.length > MAX_ROWS) {
      throw new Error(`Cannot import more than ${MAX_ROWS} rows at once`);
    }

    this.validateRequiredMappings(request.fieldMappings);

    const importedTransactions: ImportedTransactionResponse[] = [];
    const errors: CsvImportError[] = [];
    let skippedCount = 0;

    for (const rowData of request.rows) {
      try {
        const previewRow = this.validateAndMapRow(rowData, request.fieldMappings);

        if (!previewRow.valid) {
          errors.push(...previewRow.errors);
          skippedCount++;
          continue;
        }

        // Validate ticker with Yahoo Finance
        const validation = await yahooFinanceService.getQuote(previewRow.symbol!);
        const companyName = validation.shortName || previewRow.symbol!;

        // Create transaction
        const transaction = await transactionService.create({
          userId,
          type: previewRow.type!,
          symbol: previewRow.symbol!,
          companyName,
          shares: previewRow.shares!,
          pricePerShare: previewRow.pricePerShare!,
          transactionDate: previewRow.transactionDate!,
          brokerFee: previewRow.brokerFee || undefined,
          notes: previewRow.notes || undefined,
        });

        importedTransactions.push({
          id: Number(transaction.id),
          type: transaction.type,
          symbol: transaction.symbol,
          companyName: transaction.company_name,
          transactionDate: transaction.transaction_date.toISOString().split('T')[0],
          shares: Number(transaction.shares),
          pricePerShare: Number(transaction.price_per_share),
          brokerFee: transaction.broker_fee ? Number(transaction.broker_fee) : null,
          totalAmount: Number(transaction.total_amount),
          notes: transaction.notes,
        });
      } catch (error) {
        console.error(`Error importing row ${rowData.rowNumber}:`, error);
        errors.push({
          rowNumber: rowData.rowNumber,
          field: null,
          message: `Import failed: ${error instanceof Error ? error.message : 'Unknown error'}`,
          value: null,
        });
        skippedCount++;
      }
    }

    console.log(`Import completed: ${importedTransactions.length} imported, ${skippedCount} skipped`);

    return {
      importedCount: importedTransactions.length,
      skippedCount,
      errors,
      importedTransactions,
    };
  }
}

// Singleton instance
export const csvImportService = new CsvImportService();
