export type ActivationEmailParams = {
  toEmail: string;
  fullName: string;
  loginEmail: string;
  tempPassword: string;
  roleName?: string;
};

export async function dispatchActivationEmail(
  params: ActivationEmailParams
): Promise<{ success: boolean; message?: string; error?: string }> {
  try {
    const res = await fetch('/api/send-activation-email', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params),
      keepalive: true,
    });
    const data = await res.json();
    return data;
  } catch (e) {
    return { success: false, error: (e as Error).message };
  }
}
