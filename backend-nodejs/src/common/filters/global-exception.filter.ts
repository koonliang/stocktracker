import {
  ExceptionFilter,
  Catch,
  ArgumentsHost,
  HttpException,
  HttpStatus,
} from '@nestjs/common';
import { Response } from 'express';
import { ApiResponse } from '../dto/api-response.dto';

@Catch()
export class GlobalExceptionFilter implements ExceptionFilter {
  catch(exception: unknown, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();

    let status = HttpStatus.INTERNAL_SERVER_ERROR;
    let message = 'An unexpected error occurred';

    if (exception instanceof HttpException) {
      status = exception.getStatus();
      const exceptionResponse = exception.getResponse();

      // Handle validation errors
      if (
        typeof exceptionResponse === 'object' &&
        'message' in exceptionResponse
      ) {
        const msgArray = (exceptionResponse as { message: string | string[] })
          .message;
        if (Array.isArray(msgArray)) {
          // Validation error with multiple messages
          message = msgArray.join(', ');
        } else if (typeof msgArray === 'string') {
          message = msgArray;
        } else {
          message = exception.message;
        }
      } else if (typeof exceptionResponse === 'string') {
        message = exceptionResponse;
      } else {
        message = exception.message;
      }
    } else if (exception instanceof Error) {
      message = exception.message;
    }

    // Handle specific error types
    if (exception instanceof Error) {
      // JWT errors
      if (exception.name === 'TokenExpiredError') {
        status = HttpStatus.UNAUTHORIZED;
        message = 'Token has expired. Please login again';
      } else if (exception.name === 'JsonWebTokenError') {
        status = HttpStatus.UNAUTHORIZED;
        message = 'Invalid token. Please login again';
      } else if (exception.message.includes('Bad credentials')) {
        status = HttpStatus.UNAUTHORIZED;
        message = 'Invalid email or password';
      }
    }

    response.status(status).json(ApiResponse.error(message));
  }
}
