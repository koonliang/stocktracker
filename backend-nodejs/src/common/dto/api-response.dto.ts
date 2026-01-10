export class ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;

  constructor(success: boolean, message?: string, data?: T) {
    this.success = success;
    if (message) this.message = message;
    if (data !== undefined) this.data = data;
  }

  static success<T>(data?: T): ApiResponse<T> {
    return new ApiResponse(true, undefined, data);
  }

  static successWithMessage<T>(message: string, data?: T): ApiResponse<T> {
    return new ApiResponse(true, message, data);
  }

  static error(message: string): ApiResponse<null> {
    return new ApiResponse(false, message);
  }

  static errorWithData<T>(message: string, data: T): ApiResponse<T> {
    return new ApiResponse(false, message, data);
  }
}
