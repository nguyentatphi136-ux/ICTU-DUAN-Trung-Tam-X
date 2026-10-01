const { scryptSync } = require('node:crypto');

function normalizeEmail(email) {
  return email.trim().toLowerCase();
}

function createUser(id, email, name, role, password) {
  return {
    id,
    email: normalizeEmail(email),
    name,
    role,
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

function findUserByEmail(email, excludeId) {
  const normalizedEmail = normalizeEmail(email);
  return users.find((user) => user.email === normalizedEmail && user.id !== excludeId);
}

function findUserById(id) {
  return users.find((user) => user.id === id);
}

function nextUserId() {
  return String(Math.max(0, ...users.map((user) => Number(user.id) || 0)) + 1);
}

function addUser({ email, name, role, password }) {
  const user = createUser(nextUserId(), email, name, role, password);
  users.push(user);
  return user;
}

function updateUser(user, changes) {
  if (changes.email !== undefined) user.email = normalizeEmail(changes.email);
  if (changes.name !== undefined) user.name = changes.name.trim();
  if (changes.role !== undefined) user.role = changes.role;
  if (changes.password !== undefined) {
    user.passwordHash = scryptSync(changes.password, `idttx-44:${user.id}`, 64);
  }
  return user;
}

module.exports = { addUser, findUserByEmail, findUserById, updateUser, users };
