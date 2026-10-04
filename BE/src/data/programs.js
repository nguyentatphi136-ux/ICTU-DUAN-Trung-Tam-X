/**
 * Data Store cho Chương trình đào tạo & Môn học (In-Memory cho BE demo/test)
 * Ánh xạ chuẩn theo các bảng `programs`, `subjects`, `program_subjects` trong MySQL.
 */

const initialPrograms = [
  {
    id: 1,
    programCode: 'FE-PRO',
    slug: 'frontend',
    programName: 'Lập trình Web Front-end',
    description: 'Đào tạo lập trình viên Web Frontend chuyên nghiệp với HTML, CSS, JavaScript, React.',
    durationMonths: 6,
    standardTuition: 15000000,
    status: 'ACTIVE',
  },
  {
    id: 2,
    programCode: 'IELTS-PRO',
    slug: 'ielts',
    programName: 'Luyện thi IELTS cam kết đầu ra',
    description: 'Chương trình luyện thi IELTS từ nền tảng đến 6.5+.',
    durationMonths: 4,
    standardTuition: 12000000,
    status: 'ACTIVE',
  },
  {
    id: 3,
    programCode: 'ENG-COMM',
    slug: 'communication',
    programName: 'Tiếng Anh giao tiếp phản xạ',
    description: 'Nâng cao kỹ năng nghe nói, giao tiếp tự tin trong môi trường làm việc.',
    durationMonths: 3,
    standardTuition: 8000000,
    status: 'ACTIVE',
  },
  {
    id: 4,
    programCode: 'DATA-PRO',
    slug: 'data',
    programName: 'Phân tích dữ liệu kinh doanh',
    description: 'Lộ trình từ Excel, SQL cơ bản đến trực quan hóa dữ liệu với Power BI.',
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
    subjectName: 'HTML & CSS cơ bản',
    totalSessions: 15,
    creditWeight: 1.0,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 2,
    subjectCode: 'WEB102',
    slug: 'javascript',
    subjectName: 'JavaScript cơ bản',
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
    subjectName: 'UI/UX cơ bản',
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
    subjectName: 'Giao tiếp cơ bản',
    totalSessions: 15,
    creditWeight: 1.0,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 10,
    subjectCode: 'ENG201',
    slug: 'communication-advanced',
    subjectName: 'Giao tiếp nâng cao',
    totalSessions: 20,
    creditWeight: 1.5,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 11,
    subjectCode: 'DATA101',
    slug: 'data-basic',
    subjectName: 'Nhập môn phân tích dữ liệu',
    totalSessions: 15,
    creditWeight: 1.0,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 12,
    subjectCode: 'DATA102',
    slug: 'excel',
    subjectName: 'Excel cho phân tích dữ liệu',
    totalSessions: 12,
    creditWeight: 1.0,
    minPassScore: 5.0,
    status: 'ACTIVE',
  },
  {
    id: 13,
    subjectCode: 'DATA201',
    slug: 'sql',
    subjectName: 'SQL cơ bản',
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
 * Tìm chương trình theo ID số, mã code hoặc slug (ví dụ 1, 'FE-PRO', 'frontend')
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
 * Tìm môn học theo ID số, mã code hoặc slug (ví dụ 1, 'WEB101', 'html-css')
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
 * Lấy danh sách các môn học trong một chương trình đào tạo, sắp xếp theo `orderIndex`.
 * Kèm thông tin môn học và thông tin môn tiên quyết.
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
 * Kiểm tra xem có tạo vòng lặp phụ thuộc (circular dependency) hay không.
 * Quan hệ: subjectId cần prerequisiteId (subjectId -> prerequisiteId).
 * Nếu đi từ prerequisiteId theo các liên kết tiên quyết mà gặp lại subjectId thì có vòng lặp.
 */
function wouldCreatePrerequisiteCycle(programId, subjectId, candidatePrereqId) {
  if (subjectId === candidatePrereqId) return true;

  // Lấy bản đồ phụ thuộc hiện tại trong chương trình: Map(subjId -> prereqId)
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

  // DFS từ candidatePrereqId
  const visited = new Set();
  let curr = candidatePrereqId;
  while (curr) {
    if (curr === subjectId) {
      return true; // Gặp lại chính subjectId -> Vòng lặp!
    }
    if (visited.has(curr)) {
      return true; // Vòng lặp nội bộ khác
    }
    visited.add(curr);
    curr = depMap.get(curr);
  }

  return false;
}

/**
 * Thêm một môn học vào chương trình đào tạo.
 */
function addSubjectToProgram({ programId, subjectId, prerequisiteId = null }) {
  const p = findProgram(programId);
  if (!p) {
    return { success: false, code: 'PROGRAM_NOT_FOUND', message: 'Chương trình đào tạo không tồn tại.' };
  }

  const subj = findSubject(subjectId);
  if (!subj) {
    return { success: false, code: 'SUBJECT_NOT_FOUND', message: 'Môn học không tồn tại.' };
  }

  // Kiểm tra môn đã thuộc chương trình chưa
  const existing = programSubjects.find((ps) => ps.programId === p.id && ps.subjectId === subj.id);
  if (existing) {
    return {
      success: false,
      code: 'SUBJECT_ALREADY_IN_PROGRAM',
      message: `Môn học '${subj.subjectName}' đã có trong chương trình này.`,
    };
  }

  let finalPrereqId = null;
  if (prerequisiteId) {
    const prereqSubj = findSubject(prerequisiteId);
    if (!prereqSubj) {
      return { success: false, code: 'PREREQUISITE_NOT_FOUND', message: 'Môn tiên quyết không tồn tại.' };
    }
    if (prereqSubj.id === subj.id) {
      return {
        success: false,
        code: 'SELF_PREREQUISITE',
        message: 'Môn học không thể làm tiên quyết cho chính nó.',
      };
    }
    // Môn tiên quyết phải thuộc cùng chương trình
    const prereqInProg = programSubjects.find((ps) => ps.programId === p.id && ps.subjectId === prereqSubj.id);
    if (!prereqInProg) {
      return {
        success: false,
        code: 'PREREQUISITE_NOT_IN_PROGRAM',
        message: `Môn tiên quyết '${prereqSubj.subjectName}' phải thuộc chương trình đào tạo này.`,
      };
    }
    finalPrereqId = prereqSubj.id;
  }

  // Tính orderIndex kế tiếp
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
 * Gỡ môn học khỏi chương trình đào tạo.
 */
function removeSubjectFromProgram({ programId, subjectId, force = false }) {
  const p = findProgram(programId);
  if (!p) {
    return { success: false, code: 'PROGRAM_NOT_FOUND', message: 'Chương trình đào tạo không tồn tại.' };
  }

  const subj = findSubject(subjectId);
  if (!subj) {
    return { success: false, code: 'SUBJECT_NOT_FOUND', message: 'Môn học không tồn tại.' };
  }

  const relIndex = programSubjects.findIndex((ps) => ps.programId === p.id && ps.subjectId === subj.id);
  if (relIndex === -1) {
    return {
      success: false,
      code: 'SUBJECT_NOT_IN_PROGRAM',
      message: `Môn học '${subj.subjectName}' không thuộc chương trình này.`,
    };
  }

  // Kiểm tra xem môn này có đang làm tiên quyết cho môn nào khác trong chương trình không
  const dependentRels = programSubjects.filter(
    (ps) => ps.programId === p.id && ps.prerequisiteSubjectId === subj.id
  );

  if (dependentRels.length > 0 && !force) {
    const dependentNames = dependentRels
      .map((rel) => {
        const s = subjects.find((x) => x.id === rel.subjectId);
        return s ? s.subjectName : `Môn #${rel.subjectId}`;
      })
      .join(', ');

    return {
      success: false,
      code: 'IS_PREREQUISITE_OF_OTHERS',
      message: `Không thể gỡ môn '${subj.subjectName}' vì đang là môn tiên quyết của: ${dependentNames}. Vui lòng gỡ ràng buộc tiên quyết trước.`,
      dependents: dependentRels.map((r) => r.subjectId),
    };
  }

  // Nếu force = true hoặc schema tự động SET NULL
  for (const dep of dependentRels) {
    dep.prerequisiteSubjectId = null;
  }

  // Xóa quan hệ
  programSubjects.splice(relIndex, 1);

  // Sắp xếp lại thứ tự liên tục 1, 2, 3... cho các môn còn lại
  const remaining = programSubjects
    .filter((ps) => ps.programId === p.id)
    .sort((a, b) => a.orderIndex - b.orderIndex);

  remaining.forEach((rel, idx) => {
    rel.orderIndex = idx + 1;
  });

  return {
    success: true,
    message: `Đã gỡ môn '${subj.subjectName}' khỏi chương trình thành công.`,
  };
}

/**
 * Cập nhật thứ tự các môn học trong chương trình (Reorder).
 * Input có thể là mảng object: [{ courseId, order }] hoặc mảng ID: [id1, id2, ...]
 */
function reorderProgramSubjects({ programId, courses }) {
  const p = findProgram(programId);
  if (!p) {
    return { success: false, code: 'PROGRAM_NOT_FOUND', message: 'Chương trình đào tạo không tồn tại.' };
  }

  if (!Array.isArray(courses) || courses.length === 0) {
    return { success: false, code: 'INVALID_ORDER_DATA', message: 'Danh sách thứ tự môn học không hợp lệ.' };
  }

  const currentRelations = programSubjects.filter((ps) => ps.programId === p.id);
  if (currentRelations.length === 0) {
    return { success: false, code: 'PROGRAM_EMPTY', message: 'Chương trình chưa có môn học nào để sắp xếp.' };
  }

  // Chuẩn hóa input: mảng { subjectId, orderIndex }
  let normalizedList = [];
  if (typeof courses[0] === 'object' && courses[0] !== null) {
    // Dạng { courseId / subjectId, order / orderIndex }
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
    // Dạng mảng IDs: [1, 2, 3] hoặc ['html-css', 'javascript']
    normalizedList = courses.map((targetId, idx) => {
      const subj = findSubject(targetId);
      return {
        subjectId: subj ? subj.id : null,
        orderIndex: idx + 1,
        rawId: targetId,
      };
    });
  }

  // Kiểm tra môn không tồn tại
  for (const item of normalizedList) {
    if (!item.subjectId) {
      return {
        success: false,
        code: 'SUBJECT_NOT_FOUND',
        message: `Môn học '${item.rawId}' không tồn tại trong hệ thống.`,
      };
    }
  }

  // Kiểm tra trùng lặp môn học trong request
  const subjectIds = normalizedList.map((x) => x.subjectId);
  const uniqueIds = new Set(subjectIds);
  if (uniqueIds.size !== subjectIds.length) {
    return {
      success: false,
      code: 'DUPLICATE_SUBJECT_IN_ORDER',
      message: 'Danh sách sắp xếp chứa môn học trùng lặp.',
    };
  }

  // Kiểm tra tất cả môn có thuộc chương trình này không
  const programSubjectIdSet = new Set(currentRelations.map((ps) => ps.subjectId));
  for (const sid of uniqueIds) {
    if (!programSubjectIdSet.has(sid)) {
      const s = subjects.find((x) => x.id === sid);
      return {
        success: false,
        code: 'SUBJECT_NOT_IN_PROGRAM',
        message: `Môn học '${s ? s.subjectName : sid}' không thuộc chương trình này.`,
      };
    }
  }

  // Kiểm tra thứ tự hợp lệ (số nguyên dương)
  for (const item of normalizedList) {
    if (!Number.isInteger(item.orderIndex) || item.orderIndex <= 0) {
      return {
        success: false,
        code: 'INVALID_ORDER_VALUE',
        message: 'Thứ tự môn học phải là số nguyên dương lớn hơn 0.',
      };
    }
  }

  // Kiểm tra tính nhất quán với môn tiên quyết:
  // Môn tiên quyết P phải đứng TRƯỚC môn S trong thứ tự học mới!
  // Sắp xếp tạm thời theo orderIndex mới để kiểm tra
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
          message: `Thứ tự không hợp lệ: Môn tiên quyết '${prereq ? prereq.subjectName : ''}' (thứ tự ${prereqOrder}) phải đứng trước môn '${subj ? subj.subjectName : ''}' (thứ tự ${subjectOrder}).`,
        };
      }
    }
  }

  // Cập nhật nguyên tử (atomic update)
  for (const item of normalizedList) {
    const rel = currentRelations.find((ps) => ps.subjectId === item.subjectId);
    if (rel) {
      rel.orderIndex = item.orderIndex;
    }
  }

  return {
    success: true,
    message: 'Cập nhật thứ tự môn học thành công.',
    data: getProgramSubjects(p.id),
  };
}

/**
 * Khai báo hoặc cập nhật môn học tiên quyết cho một môn trong chương trình.
 */
function setPrerequisite({ programId, subjectId, prerequisiteId }) {
  const p = findProgram(programId);
  if (!p) {
    return { success: false, code: 'PROGRAM_NOT_FOUND', message: 'Chương trình đào tạo không tồn tại.' };
  }

  const subj = findSubject(subjectId);
  if (!subj) {
    return { success: false, code: 'SUBJECT_NOT_FOUND', message: 'Môn học không tồn tại.' };
  }

  const rel = programSubjects.find((ps) => ps.programId === p.id && ps.subjectId === subj.id);
  if (!rel) {
    return {
      success: false,
      code: 'SUBJECT_NOT_IN_PROGRAM',
      message: `Môn học '${subj.subjectName}' không thuộc chương trình này.`,
    };
  }

  // Nếu prerequisiteId là null hoặc 0 -> Gỡ môn tiên quyết
  if (prerequisiteId === null || prerequisiteId === undefined || prerequisiteId === '' || prerequisiteId === 0) {
    rel.prerequisiteSubjectId = null;
    return {
      success: true,
      message: `Đã gỡ môn tiên quyết của môn '${subj.subjectName}'.`,
      data: {
        programId: p.id,
        subjectId: subj.id,
        prerequisiteSubjectId: null,
      },
    };
  }

  const prereq = findSubject(prerequisiteId);
  if (!prereq) {
    return { success: false, code: 'PREREQUISITE_NOT_FOUND', message: 'Môn tiên quyết không tồn tại.' };
  }

  // 1. Không cho phép tự làm tiên quyết cho chính mình
  if (prereq.id === subj.id) {
    return {
      success: false,
      code: 'SELF_PREREQUISITE',
      message: 'Môn học không thể làm tiên quyết cho chính nó.',
    };
  }

  // 2. Môn tiên quyết phải thuộc cùng chương trình đào tạo
  const prereqRel = programSubjects.find((ps) => ps.programId === p.id && ps.subjectId === prereq.id);
  if (!prereqRel) {
    return {
      success: false,
      code: 'PREREQUISITE_NOT_IN_PROGRAM',
      message: `Môn tiên quyết '${prereq.subjectName}' phải thuộc cùng chương trình đào tạo.`,
    };
  }

  // 3. Kiểm tra môn tiên quyết phải đứng trước môn hiện tại trong lộ trình
  if (prereqRel.orderIndex >= rel.orderIndex) {
    return {
      success: false,
      code: 'PREREQUISITE_ORDER_INVALID',
      message: `Môn tiên quyết '${prereq.subjectName}' (thứ tự ${prereqRel.orderIndex}) phải đứng trước môn '${subj.subjectName}' (thứ tự ${rel.orderIndex}) trong lộ trình học.`,
    };
  }

  // 4. Kiểm tra vòng lặp phụ thuộc (Cycle detection)
  if (wouldCreatePrerequisiteCycle(p.id, subj.id, prereq.id)) {
    return {
      success: false,
      code: 'CIRCULAR_PREREQUISITE',
      message: `Không thể thiết lập '${prereq.subjectName}' làm môn tiên quyết vì tạo thành vòng lặp phụ thuộc.`,
    };
  }

  rel.prerequisiteSubjectId = prereq.id;

  return {
    success: true,
    message: `Đã thiết lập '${prereq.subjectName}' là môn tiên quyết của '${subj.subjectName}'.`,
    data: {
      programId: p.id,
      subjectId: subj.id,
      prerequisiteSubjectId: prereq.id,
      prerequisiteSubjectName: prereq.subjectName,
    },
  };
}

/**
 * Lấy danh sách tất cả các chương trình đào tạo
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
 * Lấy danh sách tất cả các môn học trong hệ thống
 */
function getAllSubjects() {
  return [...subjects];
}

/**
 * Khôi phục trạng thái mặc định (dành cho unit test)
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
