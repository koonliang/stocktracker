import { NextRequest, NextResponse } from 'next/server';
import { authenticateRequest } from '@/lib/middleware/auth';
import { csvImportService } from '@/lib/csv-import/csv-import.service';

// POST /api/transactions/import/preview - Preview import with validation (no save)
export async function POST(request: NextRequest) {
  try {
    // Authenticate user
    await authenticateRequest(request);

    // Parse request body
    const body = await request.json();

    // Validate request
    if (!body.rows || !Array.isArray(body.rows)) {
      return NextResponse.json({ success: false, message: 'Rows array is required' }, { status: 400 });
    }

    if (!body.fieldMappings || typeof body.fieldMappings !== 'object') {
      return NextResponse.json(
        { success: false, message: 'Field mappings object is required' },
        { status: 400 },
      );
    }

    // Preview import
    const response = await csvImportService.previewImport({
      rows: body.rows,
      fieldMappings: body.fieldMappings,
    });

    return NextResponse.json(
      {
        success: true,
        data: response,
      },
      { status: 200 },
    );
  } catch (error) {
    console.error('Preview import error:', error);

    if (error instanceof Error && error.message === 'Unauthorized') {
      return NextResponse.json({ success: false, message: 'Unauthorized' }, { status: 401 });
    }

    if (error instanceof Error && error.message.startsWith('Cannot import more than')) {
      return NextResponse.json({ success: false, message: error.message }, { status: 400 });
    }

    if (error instanceof Error && error.message.startsWith('Missing required field mappings')) {
      return NextResponse.json({ success: false, message: error.message }, { status: 400 });
    }

    return NextResponse.json(
      { success: false, message: 'Failed to preview import' },
      { status: 500 },
    );
  }
}
