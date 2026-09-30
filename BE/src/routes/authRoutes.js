import { Router } from 'express';
import { resetPassword } from '../services/authService.js';

const router = Router();

router.post('/reset-password', async (request, response, next) => {
  try {
    const result = await resetPassword(request.body ?? {});
    return response.status(result.status).json(result.body);
  } catch (error) {
    return next(error);
  }
});

export default router;
