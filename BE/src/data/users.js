const { scryptSync } = require('node:crypto');

function createUser(id, email, name, role, password) {
  return {
    id,
    email: email.trim().toLowerCase(),
    name,
    phone: '',
    role,
    status: 'active',
    passwordHash: scryptSync(password, `idttx-44:${id}`, 64),
  };
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

function findUserById(id) {
  return users.find((user) => user.id === id);
}

function findUserByEmail(email, excludeId) {
  const normalizedEmail = email.trim().toLowerCase();
  return users.find((user) => user.email === normalizedEmail && user.id !== excludeId);
}

function updateUser(user, changes) {
  if (changes.email !== undefined) user.email = changes.email.trim().toLowerCase();
  if (changes.name !== undefined) user.name = changes.name.trim();
  if (changes.phone !== undefined) user.phone = changes.phone.trim();
  if (changes.role !== undefined) user.role = changes.role;
  if (changes.status !== undefined) user.status = changes.status;
  return user;
}

module.exports = { findUserByEmail, findUserById, updateUser, users };
