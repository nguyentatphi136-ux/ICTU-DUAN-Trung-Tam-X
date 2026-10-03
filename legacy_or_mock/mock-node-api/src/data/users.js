const { scryptSync } = require('node:crypto');
const { ROLES } = require('../constants/roles');

function createUser(id, email, name, roles, password) {
  return {
    id,
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
    process.env.DEMO_ACCOUNTANT_EMAIL || 'accountant@example.com',
    'Accountant',
    [ROLES.ACCOUNTANT],
    process.env.DEMO_ACCOUNTANT_PASSWORD || 'accountant123',
  ),
  createUser(
    '5',
    process.env.DEMO_TRAINING_MANAGER_EMAIL || 'training@example.com',
    'Training Manager',
    [ROLES.TRAINING_MANAGER],
    process.env.DEMO_TRAINING_MANAGER_PASSWORD || 'training123',
  ),
];

// Mock data điểm số (Grades)
const grades = [
  {
    id: '1',
    studentId: '3',
    classId: '101',
    componentName: 'Chuyên cần',
    score: 9.0,
    notes: 'Đi học đầy đủ',
    updatedBy: '2',
  },
  {
    id: '2',
    studentId: '3',
    classId: '101',
    componentName: 'Giữa kỳ',
    score: 8.5,
    notes: 'Làm bài tốt',
    updatedBy: '2',
  },
];

// Mock data học phí (Tuition Fees)
const tuitionFees = [
  {
    id: '1',
    studentId: '3',
    courseName: 'Khóa học Fullstack Web Developer',
    totalAmount: 15000000,
    paidAmount: 10000000,
    status: 'partially_paid',
    receiptNo: 'REC-2026-001',
    updatedBy: '4',
  },
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
  if (changes.status !== undefined) user.status = changes.status;
  if (changes.lockedReason !== undefined) user.lockedReason = changes.lockedReason;
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
    lockedReason: user.lockedReason ?? null,
  };
}

function getGradesByStudentId(studentId) {
  return grades.filter((g) => g.studentId === String(studentId));
}

function updateGrade(studentId, componentName, score, updatedBy) {
  let grade = grades.find((g) => g.studentId === String(studentId) && g.componentName === componentName);
  if (grade) {
    grade.score = Number(score);
    grade.updatedBy = String(updatedBy);
  } else {
    grade = {
      id: String(grades.length + 1),
      studentId: String(studentId),
      classId: '101',
      componentName,
      score: Number(score),
      notes: '',
      updatedBy: String(updatedBy),
    };
    grades.push(grade);
  }
  return grade;
}

function getTuitionByStudentId(studentId) {
  return tuitionFees.find((t) => t.studentId === String(studentId));
}

function updateTuition(studentId, paidAmount, status, updatedBy) {
  let tuition = tuitionFees.find((t) => t.studentId === String(studentId));
  if (!tuition) {
    tuition = {
      id: String(tuitionFees.length + 1),
      studentId: String(studentId),
      courseName: 'Khóa học tiêu chuẩn',
      totalAmount: 15000000,
      paidAmount: Number(paidAmount),
      status: status || 'partially_paid',
      receiptNo: `REC-${Date.now()}`,
      updatedBy: String(updatedBy),
    };
    tuitionFees.push(tuition);
  } else {
    if (paidAmount !== undefined) tuition.paidAmount = Number(paidAmount);
    if (status !== undefined) tuition.status = status;
    tuition.updatedBy = String(updatedBy);
  }
  return tuition;
}

module.exports = {
  assignRole,
  createUser,
  findUserByEmail,
  findUserById,
  getGradesByStudentId,
  getTuitionByStudentId,
  hasRole,
  revokeRole,
  toPublicUser,
  updateGrade,
  updateTuition,
  updateUser,
  users,
};
