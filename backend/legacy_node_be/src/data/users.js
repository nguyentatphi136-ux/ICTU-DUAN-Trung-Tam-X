const { scryptSync } = require('node:crypto');
const { ROLES } = require('../constants/roles');

function createUser(id, email, name, roles, password) {
  return {
    id: String(id),
    email: email.trim().toLowerCase(),
    name,
    phone: '',
    roles: [...roles],
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
    [ROLES.ADMIN],
    process.env.DEMO_ADMIN_PASSWORD || 'admin123',
  ),
  createUser(
    '2',
    process.env.DEMO_INSTRUCTOR_EMAIL || 'instructor@example.com',
    'Instructor',
    [ROLES.INSTRUCTOR],
    process.env.DEMO_INSTRUCTOR_PASSWORD || 'instructor123',
  ),
  createUser(
    '3',
    process.env.DEMO_STUDENT_EMAIL || 'student@example.com',
    'Student',
    [ROLES.STUDENT],
    process.env.DEMO_STUDENT_PASSWORD || 'student123',
  ),
  createUser(
    '4',
    process.env.DEMO_MANAGER_EMAIL || 'manager@example.com',
    'Phạm Thị Quản (Quản lý đào tạo)',
    [ROLES.TRAINING_MANAGER],
    process.env.DEMO_MANAGER_PASSWORD || 'manager123',
  ),
  createUser(
    '5',
    process.env.DEMO_ADMISSIONS_EMAIL || 'admissions@example.com',
    'Lê Thị Thu Hà (Tư vấn viên)',
    [ROLES.ADMISSIONS],
    process.env.DEMO_ADMISSIONS_PASSWORD || 'admissions123',
  ),
  createUser(
    '6',
    process.env.DEMO_TUVAN_EMAIL || 'tuvan@example.com',
    'Hoàng Văn Tư (Tư vấn viên)',
    [ROLES.ADMISSIONS],
    process.env.DEMO_TUVAN_PASSWORD || 'tuvan123',
  ),
];

function findUserById(id) {
  return users.find((user) => String(user.id) === String(id));
}

function findUserByEmail(email, excludeId) {
  const normalizedEmail = email.trim().toLowerCase();
  return users.find((user) => user.email === normalizedEmail && String(user.id) !== String(excludeId));
}

function findUsersByRole(role) {
  return users.filter((user) => user.status === 'active' && user.roles.includes(role));
}

function updateUser(user, changes) {
  if (changes.email !== undefined) user.email = changes.email.trim().toLowerCase();
  if (changes.name !== undefined) user.name = changes.name.trim();
  if (changes.phone !== undefined) user.phone = changes.phone.trim();
  if (changes.status !== undefined) user.status = changes.status;
  return user;
}

function hasRole(user, role) {
  return user.roles.includes(role);
}

function assignRole(user, role) {
  if (!hasRole(user, role)) user.roles.push(role);
  return user;
}

function revokeRole(user, role) {
  user.roles = user.roles.filter((assignedRole) => assignedRole !== role);
  return user;
}

function toPublicUser(user) {
  return {
    id: user.id,
    email: user.email,
    name: user.name,
    phone: user.phone,
    roles: [...user.roles],
    status: user.status,
  };
}

module.exports = {
  assignRole,
  findUserByEmail,
  findUserById,
  findUsersByRole,
  hasRole,
  revokeRole,
  toPublicUser,
  updateUser,
  users,
};
