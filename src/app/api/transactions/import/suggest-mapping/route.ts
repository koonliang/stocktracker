import { NextRequest, NextResponse } from 'next/server';
import { authenticateRequest } from '@/lib/middleware/auth';
import { csvImportService } from '@/lib/csv-import/csv-import.service';

// POST /api/transactions/import/suggest-mapping - Suggest field mappings based on CSV headers
export async function POST(request: NextRequest) {
  try {
    // Authenticate user
    await authenticateRequest(request);

    // Parse request body
    const body = await request.json();

    // Validate headers parameter
    if (!body.headers || !Array.isArray(body.headers)) {
      return NextResponse.json(
        { success: false, message: 'Headers array is required' },
        { status: 400 },
      );
    }

    // Suggest mappings
    const response = csvImportService.suggestMappings(body.headers);

    return NextResponse.json(
      {
        success: true,
        data: response,
      },
      { status: 200 },
    );
  } catch (error) {
    console.error('Suggest mapping error:', error);

    if (error instanceof Error && error.message === 'Unauthorized') {
      return NextResponse.json({ success: false, message: 'Unauthorized' }, { status: 401 });
    }

    return NextResponse.json(
      { success: false, message: 'Failed to suggest field mappings' },
      { status: 500 },
    );
  }
}
