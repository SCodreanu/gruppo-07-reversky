/** Types that match the authentication and profile DTOs in reversky-api. */
export type AuthTokenResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer";
  expiresIn: number;
};

export type UserProfile = {
  id: number;
  username: string;
  email: string;
  role: "USER" | "ADMIN";
  emailVerified: boolean;
};

export type FieldViolation = {
  field: string;
  message: string;
};

/** Spring ProblemDetail plus the API's `code` and optional validation errors. */
export type SpringProblemDetail = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  errors?: FieldViolation[];
};

export type ApiResult<T = unknown> = {
  ok: boolean;
  status: number;
  body: T | SpringProblemDetail | null;
};
