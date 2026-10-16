// Dạng dữ liệu của POST /api/auth/login, giống backend Java để frontend (src/pages/S1-01-dang-nhap/mockAuth.ts) dùng chung.

export type LoginRequest = { email?: string; password?: string };

export type AuthUser = { id: number; email: string; fullName: string; roles: string[] };

export type LoginResponse = { success: true; code: 'AUTH_LOGIN_SUCCESS'; message: string; data: { user: AuthUser } };
