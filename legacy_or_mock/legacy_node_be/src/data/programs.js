/**
 * Data Store cho Ch╞░╞íng tr├¼nh ─æ├áo tß║ío & M├┤n hß╗ìc (In-Memory cho BE demo/test)
 * ├ünh xß║í chuß║⌐n theo c├íc bß║úng `programs`, `subjects`, `program_subjects` trong MySQL.
 */

const initialPrograms = [
  {
    id: 1,
    programCode: 'FE-PRO',
    slug: 'frontend',
    programName: 'Lß║¡p tr├¼nh Web Front-end',
    description: '─É├áo tß║ío lß║¡p tr├¼nh vi├¬n Web Frontend chuy├¬n nghiß╗çp vß╗¢i HTML, CSS, JavaScript, React.',
    durationMonths: 6,
    standardTuition: 15000000,
    status: 'ACTIVE',
  },
  {
    id: 2,
    programCode: 'IELTS-PRO',
    slug: 'ielts',
    programName: 'Luyß╗çn thi IELTS cam kß║┐t ─æß║ºu ra',
    description: 'Ch╞░╞íng tr├¼nh luyß╗çn thi IELTS tß╗½ nß╗ün tß║úng ─æß║┐n 6.5+.',
    durationMonths: 4,
    standardTuition: 12000000,
    status: 'ACTIVE',
  },
  {
    id: 3,
    programCode: 'ENG-COMM',
    slug: 'communication',
    programName: 'Tiß║┐ng Anh giao tiß║┐p phß║ún xß║í',
    description: 'N├óng cao kß╗╣ n─âng nghe n├│i, giao tiß║┐p tß╗▒ tin trong m├┤i tr╞░ß╗¥ng l├ám viß╗çc.',
    durationMonths: 3,
    standardTuition: 8000000,
    status: 'ACTIVE',
  },
  {
    id: 4,
    programCode: 'DATA-PRO',
    slug: 'data',
    programName: 'Ph├ón t├¡ch dß╗» liß╗çu kinh doanh',
    description: 'Lß╗Ö tr├¼nh tß╗½ Excel, SQL c╞í bß║ún ─æß║┐n trß╗▒c quan h├│a dß╗» liß╗çu vß╗¢i Power BI.',
    durationMonths: 5,
    standardTuition: 14000000,
    status: 'ACTIVE',
  },
];

const initialSubjects = [
  {
    id: 1,
    subjectCode: 'WEB101',
    slug: 'html-css',
    subjectName: 'HTML & CSS c╞í bß║ún',
    totalSessions: 15,
    creditWeight: 1.0,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 2,
    subjectCode: 'WEB102',
    slug: 'javascript',
    subjectName: 'JavaScript c╞í bß║ún',
    totalSessions: 20,
    creditWeight: 1.5,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 3,
    subjectCode: 'WEB103',
    slug: 'git',
    subjectName: 'Git & GitHub',
    totalSessions: 8,
    creditWeight: 0.5,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 4,
    subjectCode: 'WEB104',
    slug: 'uiux',
    subjectName: 'UI/UX c╞í bß║ún',
    totalSessions: 12,
    creditWeight: 1.0,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 5,
    subjectCode: 'WEB201',
    slug: 'react',
    subjectName: 'ReactJS',
    totalSessions: 25,
    creditWeight: 2.0,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 6,
    subjectCode: 'IELTS101',
    slug: 'ielts-foundation',
    subjectName: 'IELTS Foundation',
    totalSessions: 20,
    creditWeight: 1.5,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 7,
    subjectCode: 'IELTS201',
    slug: 'ielts-reading',
    subjectName: 'IELTS Reading',
    totalSessions: 15,
    creditWeight: 1.0,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 8,
    subjectCode: 'IELTS202',
    slug: 'ielts-writing',
    subjectName: 'IELTS Writing',
    totalSessions: 20,
    creditWeight: 1.5,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 9,
    subjectCode: 'ENG101',
    slug: 'communication-basic',
    subjectName: 'Giao tiß║┐p c╞í bß║ún',
    totalSessions: 15,
    creditWeight: 1.0,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 10,
    subjectCode: 'ENG201',
    slug: 'communication-advanced',
    subjectName: 'Giao tiß║┐p n├óng cao',
    totalSessions: 20,
    creditWeight: 1.5,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 11,
    subjectCode: 'DATA101',
    slug: 'data-basic',
    subjectName: 'Nhß║¡p m├┤n ph├ón t├¡ch dß╗» liß╗çu',
    totalSessions: 15,
    creditWeight: 1.0,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 12,
    subjectCode: 'DATA102',
    slug: 'excel',
    subjectName: 'Excel cho ph├ón t├¡ch dß╗» liß╗çu',
    totalSessions: 12,
    creditWeight: 1.0,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 13,
    subjectCode: 'DATA201',
    slug: 'sql',
    subjectName: 'SQL c╞í bß║ún',
    totalSessions: 18,
    creditWeight: 1.5,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 14,
    subjectCode: 'DATA202',
    slug: 'powerbi',
    subjectName: 'Power BI',
    totalSessions: 16,
    creditWeight: 1.5,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
];

let nextProgramSubjectId = 1;

function makeInitialProgramSubjects() {
  nextProgramSubjectId = 1;
  return [
    // Frontend (programId: 1)
    { id: nextProgramSubjectId++, programId: 1, subjectId: 1, orderIndex: 1, prerequisiteSubjectId: null },
    { id: nextProgramSubjectId++, programId: 1, subjectId: 2, orderIndex: 2, prerequisiteSubjectId: 1 },
    { id: nextProgramSubjectId++, programId: 1, subjectId: 3, orderIndex: 3, prerequisiteSubjectId: null },
    { id: nextProgramSubjectId++, programId: 1, subjectId: 5, orderIndex: 4, prerequisiteSubjectId: 2 },

    // IELTS (programId: 2)
    { id: nextProgramSubjectId++, programId: 2, subjectId: 6, orderIndex: 1, prerequisiteSubjectId: null },
    { id: nextProgramSubjectId++, programId: 2, subjectId: 7, orderIndex: 2, prerequisiteSubjectId: 6 },
    { id: nextProgramSubjectId++, programId: 2, subjectId: 8, orderIndex: 3, prerequisiteSubjectId: 6 },

    // Communication (programId: 3)
    { id: nextProgramSubjectId++, programId: 3, subjectId: 9, orderIndex: 1, prerequisiteSubjectId: null },
    { id: nextProgramSubjectId++, programId: 3, subjectId: 10, orderIndex: 2, prerequisiteSubjectId: 9 },

    // Data (programId: 4)
    { id: nextProgramSubjectId++, programId: 4, subjectId: 11, orderIndex: 1, prerequisiteSubjectId: null },
    { id: nextProgramSubjectId++, programId: 4, subjectId: 12, orderIndex: 2, prerequisiteSubjectId: 11 },
    { id: nextProgramSubjectId++, programId: 4, subjectId: 13, orderIndex: 3, prerequisiteSubjectId: 11 },
    { id: nextProgramSubjectId++, programId: 4, subjectId: 14, orderIndex: 4, prerequisiteSubjectId: 12 },
  ];
}

let programs = JSON.parse(JSON.stringify(initialPrograms));
let subjects = JSON.parse(JSON.stringify(initialSubjects));
let programSubjects = makeInitialProgramSubjects();

/**
 * T├¼m ch╞░╞íng tr├¼nh theo ID sß╗æ, m├ú code hoß║╖c slug (v├¡ dß╗Ñ 1, 'FE-PRO', 'frontend')
 */
function findProgram(identifier) {
  if (identifier === undefined || identifier === null) return null;
  const str = String(identifier).trim();
  const num = Number(str);
  return programs.find(
    (p) =>
      (!Number.isNaN(num) && p.id === num) ||
      (p.slug && p.slug.toLowerCase() === str.toLowerCase()) ||
      (p.programCode && p.programCode.toLowerCase() === str.toLowerCase())
  );
}

/**
 * T├¼m m├┤n hß╗ìc theo ID sß╗æ, m├ú code hoß║╖c slug (v├¡ dß╗Ñ 1, 'WEB101', 'html-css')
 */
function findSubject(identifier) {
  if (identifier === undefined || identifier === null) return null;
  const str = String(identifier).trim();
  const num = Number(str);
  return subjects.find(
    (s) =>
      (!Number.isNaN(num) && s.id === num) ||
      (s.slug && s.slug.toLowerCase() === str.toLowerCase()) ||
      (s.subjectCode && s.subjectCode.toLowerCase() === str.toLowerCase())
  );
}

/**
 * Lß║Ñy danh s├ích c├íc m├┤n hß╗ìc trong mß╗Öt ch╞░╞íng tr├¼nh ─æ├áo tß║ío, sß║»p xß║┐p theo `orderIndex`.
 * K├¿m th├┤ng tin m├┤n hß╗ìc v├á th├┤ng tin m├┤n ti├¬n quyß║┐t.
 */
function getProgramSubjects(programId) {
  const p = findProgram(programId);
  if (!p) return null;

  const relations = programSubjects
    .filter((ps) => ps.programId === p.id)
    .sort((a, b) => a.orderIndex - b.orderIndex);

  return relations.map((rel) => {
    const subj = subjects.find((s) => s.id === rel.subjectId);
    const prereq = rel.prerequisiteSubjectId
      ? subjects.find((s) => s.id === rel.prerequisiteSubjectId)
      : null;

    return {
      relationId: rel.id,
      programId: p.id,
      programCode: p.programCode,
      programName: p.programName,
      subjectId: subj ? subj.id : rel.subjectId,
      subjectCode: subj ? subj.subjectCode : null,
      subjectName: subj ? subj.subjectName : null,
      slug: subj ? subj.slug : null,
      totalSessions: subj ? subj.totalSessions : null,
      creditWeight: subj ? subj.creditWeight : null,
      orderIndex: rel.orderIndex,
      prerequisiteSubjectId: rel.prerequisiteSubjectId,
      prerequisiteSubjectCode: prereq ? prereq.subjectCode : null,
      prerequisiteSubjectName: prereq ? prereq.subjectName : null,
    };
  });
}

/**
 * Kiß╗âm tra xem c├│ tß║ío v├▓ng lß║╖p phß╗Ñ thuß╗Öc (circular dependency) hay kh├┤ng.
 * Quan hß╗ç: subjectId cß║ºn prerequisiteId (subjectId -> prerequisiteId).
 * Nß║┐u ─æi tß╗½ prerequisiteId theo c├íc li├¬n kß║┐t ti├¬n quyß║┐t m├á gß║╖p lß║íi subjectId th├¼ c├│ v├▓ng lß║╖p.
 */
function wouldCreatePrerequisiteCycle(programId, subjectId, candidatePrereqId) {
  if (subjectId === candidatePrereqId) return true;

  // Lß║Ñy bß║ún ─æß╗ô phß╗Ñ thuß╗Öc hiß╗çn tß║íi trong ch╞░╞íng tr├¼nh: Map(subjId -> prereqId)
  const relations = programSubjects.filter((ps) => ps.programId === programId);
  const depMap = new Map();
  for (const rel of relations) {
    if (rel.subjectId === subjectId) {
      depMap.set(rel.subjectId, candidatePrereqId);
    } else {
      depMap.set(rel.subjectId, rel.prerequisiteSubjectId);
    }
  }
  depMap.set(subjectId, candidatePrereqId);

  // DFS tß╗½ candidatePrereqId
  const visited = new Set();
  let curr = candidatePrereqId;
  while (curr) {
    if (curr === subjectId) {
      return true; // Gß║╖p lß║íi ch├¡nh subjectId -> V├▓ng lß║╖p!
    }
    if (visited.has(curr)) {
      return true; // V├▓ng lß║╖p nß╗Öi bß╗Ö kh├íc
    }
    visited.add(curr);
    curr = depMap.get(curr);
  }

  return false;
}

/**
 * Th├¬m mß╗Öt m├┤n hß╗ìc v├áo ch╞░╞íng tr├¼nh ─æ├áo tß║ío.
 */
function addSubjectToProgram({ programId, subjectId, prerequisiteId = null }) {
  const p = findProgram(programId);
  if (!p) {
    return { success: false, code: 'PROGRAM_NOT_FOUND', message: 'Ch╞░╞íng tr├¼nh ─æ├áo tß║ío kh├┤ng tß╗ôn tß║íi.' };
  }

  const subj = findSubject(subjectId);
  if (!subj) {
    return { success: false, code: 'SUBJECT_NOT_FOUND', message: 'M├┤n hß╗ìc kh├┤ng tß╗ôn tß║íi.' };
  }

  // Kiß╗âm tra m├┤n ─æ├ú thuß╗Öc ch╞░╞íng tr├¼nh ch╞░a
  const existing = programSubjects.find((ps) => ps.programId === p.id && ps.subjectId === subj.id);
  if (existing) {
    return {
      success: false,
      code: 'SUBJECT_ALREADY_IN_PROGRAM',
      message: `M├┤n hß╗ìc '${subj.subjectName}' ─æ├ú c├│ trong ch╞░╞íng tr├¼nh n├áy.`,
    };
  }

  let finalPrereqId = null;
  if (prerequisiteId) {
    const prereqSubj = findSubject(prerequisiteId);
    if (!prereqSubj) {
      return { success: false, code: 'PREREQUISITE_NOT_FOUND', message: 'M├┤n ti├¬n quyß║┐t kh├┤ng tß╗ôn tß║íi.' };
    }
    if (prereqSubj.id === subj.id) {
      return {
        success: false,
        code: 'SELF_PREREQUISITE',
        message: 'M├┤n hß╗ìc kh├┤ng thß╗â l├ám ti├¬n quyß║┐t cho ch├¡nh n├│.',
      };
    }
    // M├┤n ti├¬n quyß║┐t phß║úi thuß╗Öc c├╣ng ch╞░╞íng tr├¼nh
    const prereqInProg = programSubjects.find((ps) => ps.programId === p.id && ps.subjectId === prereqSubj.id);
    if (!prereqInProg) {
      return {
        success: false,
        code: 'PREREQUISITE_NOT_IN_PROGRAM',
        message: `M├┤n ti├¬n quyß║┐t '${prereqSubj.subjectName}' phß║úi thuß╗Öc ch╞░╞íng tr├¼nh ─æ├áo tß║ío n├áy.`,
      };
    }
    finalPrereqId = prereqSubj.id;
  }

  // T├¡nh orderIndex kß║┐ tiß║┐p
  const currentProgramSubs = programSubjects.filter((ps) => ps.programId === p.id);
  const maxOrder = currentProgramSubs.reduce((max, ps) => Math.max(max, ps.orderIndex), 0);
  const newOrder = maxOrder + 1;

  const newRelation = {
    id: nextProgramSubjectId++,
    programId: p.id,
    subjectId: subj.id,
    orderIndex: newOrder,
    prerequisiteSubjectId: finalPrereqId,
  };

  programSubjects.push(newRelation);

  return {
    success: true,
    data: {
      relationId: newRelation.id,
      programId: p.id,
      programName: p.programName,
      subjectId: subj.id,
      subjectCode: subj.subjectCode,
      subjectName: subj.subjectName,
      orderIndex: newRelation.orderIndex,
      prerequisiteSubjectId: newRelation.prerequisiteSubjectId,
    },
  };
}

/**
 * Gß╗í m├┤n hß╗ìc khß╗Åi ch╞░╞íng tr├¼nh ─æ├áo tß║ío.
 */
function removeSubjectFromProgram({ programId, subjectId, force = false }) {
  const p = findProgram(programId);
  if (!p) {
    return { success: false, code: 'PROGRAM_NOT_FOUND', message: 'Ch╞░╞íng tr├¼nh ─æ├áo tß║ío kh├┤ng tß╗ôn tß║íi.' };
  }

  const subj = findSubject(subjectId);
  if (!subj) {
    return { success: false, code: 'SUBJECT_NOT_FOUND', message: 'M├┤n hß╗ìc kh├┤ng tß╗ôn tß║íi.' };
  }

  const relIndex = programSubjects.findIndex((ps) => ps.programId === p.id && ps.subjectId === subj.id);
  if (relIndex === -1) {
    return {
      success: false,
      code: 'SUBJECT_NOT_IN_PROGRAM',
      message: `M├┤n hß╗ìc '${subj.subjectName}' kh├┤ng thuß╗Öc ch╞░╞íng tr├¼nh n├áy.`,
    };
  }

  // Kiß╗âm tra xem m├┤n n├áy c├│ ─æang l├ám ti├¬n quyß║┐t cho m├┤n n├áo kh├íc trong ch╞░╞íng tr├¼nh kh├┤ng
  const dependentRels = programSubjects.filter(
    (ps) => ps.programId === p.id && ps.prerequisiteSubjectId === subj.id
  );

  if (dependentRels.length > 0 && !force) {
    const dependentNames = dependentRels
      .map((rel) => {
        const s = subjects.find((x) => x.id === rel.subjectId);
        return s ? s.subjectName : `M├┤n #${rel.subjectId}`;
      })
      .join(', ');

    return {
      success: false,
      code: 'IS_PREREQUISITE_OF_OTHERS',
      message: `Kh├┤ng thß╗â gß╗í m├┤n '${subj.subjectName}' v├¼ ─æang l├á m├┤n ti├¬n quyß║┐t cß╗ºa: ${dependentNames}. Vui l├▓ng gß╗í r├áng buß╗Öc ti├¬n quyß║┐t tr╞░ß╗¢c.`,
      dependents: dependentRels.map((r) => r.subjectId),
    };
  }

  // Nß║┐u force = true hoß║╖c schema tß╗▒ ─æß╗Öng SET NULL
  for (const dep of dependentRels) {
    dep.prerequisiteSubjectId = null;
  }

  // X├│a quan hß╗ç
  programSubjects.splice(relIndex, 1);

  // Sß║»p xß║┐p lß║íi thß╗⌐ tß╗▒ li├¬n tß╗Ñc 1, 2, 3... cho c├íc m├┤n c├▓n lß║íi
  const remaining = programSubjects
    .filter((ps) => ps.programId === p.id)
    .sort((a, b) => a.orderIndex - b.orderIndex);

  remaining.forEach((rel, idx) => {
    rel.orderIndex = idx + 1;
  });

  return {
    success: true,
    message: `─É├ú gß╗í m├┤n '${subj.subjectName}' khß╗Åi ch╞░╞íng tr├¼nh th├ánh c├┤ng.`,
  };
}

/**
 * Cß║¡p nhß║¡t thß╗⌐ tß╗▒ c├íc m├┤n hß╗ìc trong ch╞░╞íng tr├¼nh (Reorder).
 * Input c├│ thß╗â l├á mß║úng object: [{ courseId, order }] hoß║╖c mß║úng ID: [id1, id2, ...]
 */
function reorderProgramSubjects({ programId, courses }) {
  const p = findProgram(programId);
  if (!p) {
    return { success: false, code: 'PROGRAM_NOT_FOUND', message: 'Ch╞░╞íng tr├¼nh ─æ├áo tß║ío kh├┤ng tß╗ôn tß║íi.' };
  }

  if (!Array.isArray(courses) || courses.length === 0) {
    return { success: false, code: 'INVALID_ORDER_DATA', message: 'Danh s├ích thß╗⌐ tß╗▒ m├┤n hß╗ìc kh├┤ng hß╗úp lß╗ç.' };
  }

  const currentRelations = programSubjects.filter((ps) => ps.programId === p.id);
  if (currentRelations.length === 0) {
    return { success: false, code: 'PROGRAM_EMPTY', message: 'Ch╞░╞íng tr├¼nh ch╞░a c├│ m├┤n hß╗ìc n├áo ─æß╗â sß║»p xß║┐p.' };
  }

  // Chuß║⌐n h├│a input: mß║úng { subjectId, orderIndex }
  let normalizedList = [];
  if (typeof courses[0] === 'object' && courses[0] !== null) {
    // Dß║íng { courseId / subjectId, order / orderIndex }
    normalizedList = courses.map((item, idx) => {
      const targetId = item.courseId ?? item.subjectId ?? item.id;
      const order = Number(item.order ?? item.orderIndex ?? idx + 1);
      const subj = findSubject(targetId);
      return {
        subjectId: subj ? subj.id : null,
        orderIndex: order,
        rawId: targetId,
      };
    });
  } else {
    // Dß║íng mß║úng IDs: [1, 2, 3] hoß║╖c ['html-css', 'javascript']
    normalizedList = courses.map((targetId, idx) => {
      const subj = findSubject(targetId);
      return {
        subjectId: subj ? subj.id : null,
        orderIndex: idx + 1,
        rawId: targetId,
      };
    });
  }

  // Kiß╗âm tra m├┤n kh├┤ng tß╗ôn tß║íi
  for (const item of normalizedList) {
    if (!item.subjectId) {
      return {
        success: false,
        code: 'SUBJECT_NOT_FOUND',
        message: `M├┤n hß╗ìc '${item.rawId}' kh├┤ng tß╗ôn tß║íi trong hß╗ç thß╗æng.`,
      };
    }
  }

  // Kiß╗âm tra tr├╣ng lß║╖p m├┤n hß╗ìc trong request
  const subjectIds = normalizedList.map((x) => x.subjectId);
  const uniqueIds = new Set(subjectIds);
  if (uniqueIds.size !== subjectIds.length) {
    return {
      success: false,
      code: 'DUPLICATE_SUBJECT_IN_ORDER',
      message: 'Danh s├ích sß║»p xß║┐p chß╗⌐a m├┤n hß╗ìc tr├╣ng lß║╖p.',
    };
  }

  // Kiß╗âm tra tß║Ñt cß║ú m├┤n c├│ thuß╗Öc ch╞░╞íng tr├¼nh n├áy kh├┤ng
  const programSubjectIdSet = new Set(currentRelations.map((ps) => ps.subjectId));
  for (const sid of uniqueIds) {
    if (!programSubjectIdSet.has(sid)) {
      const s = subjects.find((x) => x.id === sid);
      return {
        success: false,
        code: 'SUBJECT_NOT_IN_PROGRAM',
        message: `M├┤n hß╗ìc '${s ? s.subjectName : sid}' kh├┤ng thuß╗Öc ch╞░╞íng tr├¼nh n├áy.`,
      };
    }
  }

  // Kiß╗âm tra thß╗⌐ tß╗▒ hß╗úp lß╗ç (sß╗æ nguy├¬n d╞░╞íng)
  for (const item of normalizedList) {
    if (!Number.isInteger(item.orderIndex) || item.orderIndex <= 0) {
      return {
        success: false,
        code: 'INVALID_ORDER_VALUE',
        message: 'Thß╗⌐ tß╗▒ m├┤n hß╗ìc phß║úi l├á sß╗æ nguy├¬n d╞░╞íng lß╗¢n h╞ín 0.',
      };
    }
  }

  // Kiß╗âm tra t├¡nh nhß║Ñt qu├ín vß╗¢i m├┤n ti├¬n quyß║┐t:
  // M├┤n ti├¬n quyß║┐t P phß║úi ─æß╗⌐ng TR╞»ß╗ÜC m├┤n S trong thß╗⌐ tß╗▒ hß╗ìc mß╗¢i!
  // Sß║»p xß║┐p tß║ím thß╗¥i theo orderIndex mß╗¢i ─æß╗â kiß╗âm tra
  const tempOrderMap = new Map();
  normalizedList.forEach((item) => {
    tempOrderMap.set(item.subjectId, item.orderIndex);
  });

  for (const rel of currentRelations) {
    if (rel.prerequisiteSubjectId) {
      const prereqOrder = tempOrderMap.get(rel.prerequisiteSubjectId);
      const subjectOrder = tempOrderMap.get(rel.subjectId);

      if (prereqOrder !== undefined && subjectOrder !== undefined && prereqOrder >= subjectOrder) {
        const subj = subjects.find((s) => s.id === rel.subjectId);
        const prereq = subjects.find((s) => s.id === rel.prerequisiteSubjectId);
        return {
          success: false,
          code: 'PREREQUISITE_ORDER_VIOLATION',
          message: `Thß╗⌐ tß╗▒ kh├┤ng hß╗úp lß╗ç: M├┤n ti├¬n quyß║┐t '${prereq ? prereq.subjectName : ''}' (thß╗⌐ tß╗▒ ${prereqOrder}) phß║úi ─æß╗⌐ng tr╞░ß╗¢c m├┤n '${subj ? subj.subjectName : ''}' (thß╗⌐ tß╗▒ ${subjectOrder}).`,
        };
      }
    }
  }

  // Cß║¡p nhß║¡t nguy├¬n tß╗¡ (atomic update)
  for (const item of normalizedList) {
    const rel = currentRelations.find((ps) => ps.subjectId === item.subjectId);
    if (rel) {
      rel.orderIndex = item.orderIndex;
    }
  }

  return {
    success: true,
    message: 'Cß║¡p nhß║¡t thß╗⌐ tß╗▒ m├┤n hß╗ìc th├ánh c├┤ng.',
    data: getProgramSubjects(p.id),
  };
}

/**
 * Khai b├ío hoß║╖c cß║¡p nhß║¡t m├┤n hß╗ìc ti├¬n quyß║┐t cho mß╗Öt m├┤n trong ch╞░╞íng tr├¼nh.
 */
function setPrerequisite({ programId, subjectId, prerequisiteId }) {
  const p = findProgram(programId);
  if (!p) {
    return { success: false, code: 'PROGRAM_NOT_FOUND', message: 'Ch╞░╞íng tr├¼nh ─æ├áo tß║ío kh├┤ng tß╗ôn tß║íi.' };
  }

  const subj = findSubject(subjectId);
  if (!subj) {
    return { success: false, code: 'SUBJECT_NOT_FOUND', message: 'M├┤n hß╗ìc kh├┤ng tß╗ôn tß║íi.' };
  }

  const rel = programSubjects.find((ps) => ps.programId === p.id && ps.subjectId === subj.id);
  if (!rel) {
    return {
      success: false,
      code: 'SUBJECT_NOT_IN_PROGRAM',
      message: `M├┤n hß╗ìc '${subj.subjectName}' kh├┤ng thuß╗Öc ch╞░╞íng tr├¼nh n├áy.`,
    };
  }

  // Nß║┐u prerequisiteId l├á null hoß║╖c 0 -> Gß╗í m├┤n ti├¬n quyß║┐t
  if (prerequisiteId === null || prerequisiteId === undefined || prerequisiteId === '' || prerequisiteId === 0) {
    rel.prerequisiteSubjectId = null;
    return {
      success: true,
      message: `─É├ú gß╗í m├┤n ti├¬n quyß║┐t cß╗ºa m├┤n '${subj.subjectName}'.`,
      data: {
        programId: p.id,
        subjectId: subj.id,
        prerequisiteSubjectId: null,
      },
    };
  }

  const prereq = findSubject(prerequisiteId);
  if (!prereq) {
    return { success: false, code: 'PREREQUISITE_NOT_FOUND', message: 'M├┤n ti├¬n quyß║┐t kh├┤ng tß╗ôn tß║íi.' };
  }

  // 1. Kh├┤ng cho ph├⌐p tß╗▒ l├ám ti├¬n quyß║┐t cho ch├¡nh m├¼nh
  if (prereq.id === subj.id) {
    return {
      success: false,
      code: 'SELF_PREREQUISITE',
      message: 'M├┤n hß╗ìc kh├┤ng thß╗â l├ám ti├¬n quyß║┐t cho ch├¡nh n├│.',
    };
  }

  // 2. M├┤n ti├¬n quyß║┐t phß║úi thuß╗Öc c├╣ng ch╞░╞íng tr├¼nh ─æ├áo tß║ío
  const prereqRel = programSubjects.find((ps) => ps.programId === p.id && ps.subjectId === prereq.id);
  if (!prereqRel) {
    return {
      success: false,
      code: 'PREREQUISITE_NOT_IN_PROGRAM',
      message: `M├┤n ti├¬n quyß║┐t '${prereq.subjectName}' phß║úi thuß╗Öc c├╣ng ch╞░╞íng tr├¼nh ─æ├áo tß║ío.`,
    };
  }

  // 3. Kiß╗âm tra m├┤n ti├¬n quyß║┐t phß║úi ─æß╗⌐ng tr╞░ß╗¢c m├┤n hiß╗çn tß║íi trong lß╗Ö tr├¼nh
  if (prereqRel.orderIndex >= rel.orderIndex) {
    return {
      success: false,
      code: 'PREREQUISITE_ORDER_INVALID',
      message: `M├┤n ti├¬n quyß║┐t '${prereq.subjectName}' (thß╗⌐ tß╗▒ ${prereqRel.orderIndex}) phß║úi ─æß╗⌐ng tr╞░ß╗¢c m├┤n '${subj.subjectName}' (thß╗⌐ tß╗▒ ${rel.orderIndex}) trong lß╗Ö tr├¼nh hß╗ìc.`,
    };
  }

  // 4. Kiß╗âm tra v├▓ng lß║╖p phß╗Ñ thuß╗Öc (Cycle detection)
  if (wouldCreatePrerequisiteCycle(p.id, subj.id, prereq.id)) {
    return {
      success: false,
      code: 'CIRCULAR_PREREQUISITE',
      message: `Kh├┤ng thß╗â thiß║┐t lß║¡p '${prereq.subjectName}' l├ám m├┤n ti├¬n quyß║┐t v├¼ tß║ío th├ánh v├▓ng lß║╖p phß╗Ñ thuß╗Öc.`,
    };
  }

  rel.prerequisiteSubjectId = prereq.id;

  return {
    success: true,
    message: `─É├ú thiß║┐t lß║¡p '${prereq.subjectName}' l├á m├┤n ti├¬n quyß║┐t cß╗ºa '${subj.subjectName}'.`,
    data: {
      programId: p.id,
      subjectId: subj.id,
      prerequisiteSubjectId: prereq.id,
      prerequisiteSubjectName: prereq.subjectName,
    },
  };
}

/**
 * Lß║Ñy danh s├ích tß║Ñt cß║ú c├íc ch╞░╞íng tr├¼nh ─æ├áo tß║ío
 */
function getAllPrograms() {
  return programs.map((p) => ({
    id: p.id,
    code: p.programCode,
    slug: p.slug,
    name: p.programName,
    description: p.description,
    durationMonths: p.durationMonths,
    standardTuition: p.standardTuition,
    status: p.status,
    totalSubjects: programSubjects.filter((ps) => ps.programId === p.id).length,
  }));
}

/**
 * Lß║Ñy danh s├ích tß║Ñt cß║ú c├íc m├┤n hß╗ìc trong hß╗ç thß╗æng
 */
function getAllSubjects() {
  return [...subjects];
}

/**
 * Kh├┤i phß╗Ñc trß║íng th├íi mß║╖c ─æß╗ïnh (d├ánh cho unit test)
 */
function resetToDefaults() {
  programs = JSON.parse(JSON.stringify(initialPrograms));
  subjects = JSON.parse(JSON.stringify(initialSubjects));
  programSubjects = makeInitialProgramSubjects();
}

module.exports = {
  addSubjectToProgram,
  findProgram,
  findSubject,
  getAllPrograms,
  getAllSubjects,
  getProgramSubjects,
  removeSubjectFromProgram,
  reorderProgramSubjects,
  resetToDefaults,
  setPrerequisite,
  wouldCreatePrerequisiteCycle,
};
