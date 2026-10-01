const { randomBytes, scryptSync } = require('node:crypto');

const passwordKeyLength = 64;

function hashPassword(password) {
  const salt = randomBytes(16);
  return {
    passwordSalt: salt.toString('hex'),
    passwordHash: scryptSync(password, salt, passwordKeyLength),
  };
}

function createUser(id, email, name, role, password) {
  const passwordFields = hashPassword(password);
  return {
    id,
    email: email.trim().toLowerCase(),
    name,
    role,
    ...passwordFields,
  };
}

function updatePassword(id, password) {
  const user = users.find((candidate) => candidate.id === id);
  if (!user) return null;

  Object.assign(user, hashPassword(password));
  return user;
}

// Demo users are loaded from environment variables so credentials are not
// stored in source code. Replace this in-memory list with the real user store.
const users = [
  createUser(
    '1',
    process.env.DEMO_ADMIN_EMAIL || 'admin@example.com',
    'Admin',
    'Admin',
    process.env.DEMO_ADMIN_PASSWORD || 'admin123',
  ),
  createUser(
    '2',
    process.env.DEMO_TEACHER_EMAIL || 'teacher@example.com',
    'Teacher',
    'Teacher',
    process.env.DEMO_TEACHER_PASSWORD || 'teacher123',
  ),
  createUser(
    '3',
    process.env.DEMO_STUDENT_EMAIL || 'student@example.com',
    'Student',
    'Student',
    process.env.DEMO_STUDENT_PASSWORD || 'student123',
  ),
];

module.exports = { updatePassword, users };
