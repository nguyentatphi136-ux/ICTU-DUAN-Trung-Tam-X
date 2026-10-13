const express = require('express');
const { ROLES } = require('../constants/roles');
const {
  addSubjectToProgram,
  findProgram,
  findSubject,
  getAllPrograms,
  getAllSubjects,
  getProgramSubjects,
  removeSubjectFromProgram,
  reorderProgramSubjects,
  setPrerequisite,
} = require('../data/programs');
const { authenticate, authorize } = require('../middleware/auth');

const router = express.Router();

// Middleware kiß╗âm tra quyß╗ün "Quß║ún l├╜ ─æ├áo tß║ío" hoß║╖c "Admin"
const requireTrainingManager = [authenticate, authorize(ROLES.TRAINING_MANAGER, ROLES.ADMIN)];

/**
 * GET /api/training-programs
 * Lß║Ñy danh s├ích tß║Ñt cß║ú c├íc ch╞░╞íng tr├¼nh ─æ├áo tß║ío
 */
router.get('/', (req, res) => {
  const list = getAllPrograms();
  return res.status(200).json({
    success: true,
    data: list,
  });
});

/**
 * GET /api/training-programs/subjects (danh mß╗Ñc tß║Ñt cß║ú m├┤n hß╗ìc)
 */
router.get('/subjects-catalog', (req, res) => {
  return res.status(200).json({
    success: true,
    data: getAllSubjects(),
  });
});

/**
 * GET /api/training-programs/:programId
 * Lß║Ñy th├┤ng tin mß╗Öt ch╞░╞íng tr├¼nh ─æ├áo tß║ío
 */
router.get('/:programId', (req, res) => {
  const p = findProgram(req.params.programId);
  if (!p) {
    return res.status(404).json({ success: false, message: 'Ch╞░╞íng tr├¼nh ─æ├áo tß║ío kh├┤ng tß╗ôn tß║íi.' });
  }
  return res.status(200).json({
    success: true,
    data: p,
  });
});

/**
 * GET /api/training-programs/:programId/courses
 * GET /api/training-programs/:programId/subjects
 * Lß║Ñy danh s├ích c├íc m├┤n hß╗ìc trong ch╞░╞íng tr├¼nh theo thß╗⌐ tß╗▒ hß╗ìc
 */
function handleGetCourses(req, res) {
  const p = findProgram(req.params.programId);
  if (!p) {
    return res.status(404).json({ success: false, message: 'Ch╞░╞íng tr├¼nh ─æ├áo tß║ío kh├┤ng tß╗ôn tß║íi.' });
  }
  const courses = getProgramSubjects(p.id);
  return res.status(200).json({
    success: true,
    program: {
      id: p.id,
      code: p.programCode,
      name: p.programName,
    },
    totalCourses: courses.length,
    data: courses,
  });
}
router.get('/:programId/courses', handleGetCourses);
router.get('/:programId/subjects', handleGetCourses);

/**
 * POST /api/training-programs/:programId/courses
 * POST /api/training-programs/:programId/subjects
 * Th├¬m m├┤n hß╗ìc v├áo ch╞░╞íng tr├¼nh ─æ├áo tß║ío (Y├¬u cß║ºu TrainingManager / Admin)
 */
function handleAddCourse(req, res) {
  const body = req.body || {};
  const courseId = body.courseId ?? body.subjectId ?? body.id;
  const prerequisiteId = body.prerequisiteId ?? body.prerequisiteSubjectId ?? null;

  if (courseId === undefined || courseId === null || (typeof courseId === 'string' && !courseId.trim())) {
    return res.status(400).json({
      success: false,
      code: 'MISSING_COURSE_ID',
      message: 'Vui l├▓ng cung cß║Ñp m├ú hoß║╖c ID m├┤n hß╗ìc cß║ºn th├¬m (courseId).',
    });
  }

  const result = addSubjectToProgram({
    programId: req.params.programId,
    subjectId: courseId,
    prerequisiteId,
  });

  if (!result.success) {
    let statusCode = 400;
    if (result.code === 'PROGRAM_NOT_FOUND' || result.code === 'SUBJECT_NOT_FOUND') {
      statusCode = 404;
    } else if (result.code === 'SUBJECT_ALREADY_IN_PROGRAM') {
      statusCode = 409;
    }
    return res.status(statusCode).json({
      success: false,
      code: result.code,
      message: result.message,
    });
  }

  return res.status(201).json({
    success: true,
    message: 'Th├¬m m├┤n hß╗ìc v├áo ch╞░╞íng tr├¼nh ─æ├áo tß║ío th├ánh c├┤ng.',
    data: result.data,
  });
}
router.post('/:programId/courses', requireTrainingManager, handleAddCourse);
router.post('/:programId/subjects', requireTrainingManager, handleAddCourse);

/**
 * PUT /api/training-programs/:programId/courses/order
 * PUT /api/training-programs/:programId/subjects/order
 * Sß║»p xß║┐p lß║íi thß╗⌐ tß╗▒ c├íc m├┤n hß╗ìc trong ch╞░╞íng tr├¼nh (Y├¬u cß║ºu TrainingManager / Admin)
 */
function handleReorderCourses(req, res) {
  const body = req.body;
  let coursesList = [];

  if (Array.isArray(body)) {
    coursesList = body;
  } else if (body && typeof body === 'object') {
    coursesList = body.courses ?? body.subjects ?? body.orderedIds;
  }

  if (!Array.isArray(coursesList) || coursesList.length === 0) {
    return res.status(400).json({
      success: false,
      code: 'INVALID_REQUEST_BODY',
      message: 'Vui l├▓ng cung cß║Ñp danh s├ích thß╗⌐ tß╗▒ m├┤n hß╗ìc (mß║úng courses hoß║╖c orderedIds).',
    });
  }

  const result = reorderProgramSubjects({
    programId: req.params.programId,
    courses: coursesList,
  });

  if (!result.success) {
    const statusCode = result.code === 'PROGRAM_NOT_FOUND' ? 404 : 400;
    return res.status(statusCode).json({
      success: false,
      code: result.code,
      message: result.message,
    });
  }

  return res.status(200).json({
    success: true,
    message: result.message,
    data: result.data,
  });
}
router.put('/:programId/courses/order', requireTrainingManager, handleReorderCourses);
router.put('/:programId/subjects/order', requireTrainingManager, handleReorderCourses);

/**
 * PUT /api/training-programs/:programId/courses/:courseId/prerequisite
 * PUT /api/training-programs/:programId/subjects/:courseId/prerequisite
 * Khai b├ío hoß║╖c cß║¡p nhß║¡t m├┤n hß╗ìc ti├¬n quyß║┐t (Y├¬u cß║ºu TrainingManager / Admin)
 */
function handleSetPrerequisite(req, res) {
  const body = req.body || {};
  const prerequisiteId = body.prerequisiteId ?? body.prerequisiteSubjectId;

  const result = setPrerequisite({
    programId: req.params.programId,
    subjectId: req.params.courseId,
    prerequisiteId,
  });

  if (!result.success) {
    let statusCode = 400;
    if (result.code === 'PROGRAM_NOT_FOUND' || result.code === 'SUBJECT_NOT_FOUND') {
      statusCode = 404;
    }
    return res.status(statusCode).json({
      success: false,
      code: result.code,
      message: result.message,
    });
  }

  return res.status(200).json({
    success: true,
    message: result.message,
    data: result.data,
  });
}
router.put('/:programId/courses/:courseId/prerequisite', requireTrainingManager, handleSetPrerequisite);
router.put('/:programId/subjects/:courseId/prerequisite', requireTrainingManager, handleSetPrerequisite);

/**
 * DELETE /api/training-programs/:programId/courses/:courseId/prerequisite
 * Gß╗í bß╗Å ─æiß╗üu kiß╗çn ti├¬n quyß║┐t cß╗ºa mß╗Öt m├┤n hß╗ìc (Y├¬u cß║ºu TrainingManager / Admin)
 */
function handleRemovePrerequisite(req, res) {
  const result = setPrerequisite({
    programId: req.params.programId,
    subjectId: req.params.courseId,
    prerequisiteId: null,
  });

  if (!result.success) {
    const statusCode = result.code.includes('NOT_FOUND') ? 404 : 400;
    return res.status(statusCode).json({
      success: false,
      code: result.code,
      message: result.message,
    });
  }

  return res.status(200).json({
    success: true,
    message: result.message,
    data: result.data,
  });
}
router.delete('/:programId/courses/:courseId/prerequisite', requireTrainingManager, handleRemovePrerequisite);
router.delete('/:programId/subjects/:courseId/prerequisite', requireTrainingManager, handleRemovePrerequisite);

/**
 * DELETE /api/training-programs/:programId/courses/:courseId
 * DELETE /api/training-programs/:programId/subjects/:courseId
 * Gß╗í m├┤n hß╗ìc khß╗Åi ch╞░╞íng tr├¼nh ─æ├áo tß║ío (Y├¬u cß║ºu TrainingManager / Admin)
 */
function handleRemoveCourse(req, res) {
  const force = req.query.force === 'true' || req.query.force === '1';

  const result = removeSubjectFromProgram({
    programId: req.params.programId,
    subjectId: req.params.courseId,
    force,
  });

  if (!result.success) {
    let statusCode = 400;
    if (result.code === 'PROGRAM_NOT_FOUND' || result.code === 'SUBJECT_NOT_FOUND' || result.code === 'SUBJECT_NOT_IN_PROGRAM') {
      statusCode = 404;
    } else if (result.code === 'IS_PREREQUISITE_OF_OTHERS') {
      statusCode = 400;
    }
    return res.status(statusCode).json({
      success: false,
      code: result.code,
      message: result.message,
      dependents: result.dependents,
    });
  }

  return res.status(200).json({
    success: true,
    message: result.message,
  });
}
router.delete('/:programId/courses/:courseId', requireTrainingManager, handleRemoveCourse);
router.delete('/:programId/subjects/:courseId', requireTrainingManager, handleRemoveCourse);

module.exports = router;
