// ======================================================
// QUẢN LÝ CHƯƠNG TRÌNH ĐÀO TẠO
// FRONTEND DEMO
// ======================================================


// ======================================================
// 1. DANH MỤC TẤT CẢ MÔN HỌC
// ======================================================

const subjectCatalog = [
  {
    id: "html-css",
    code: "WEB101",
    name: "HTML & CSS cơ bản",
  },

  {
    id: "javascript",
    code: "WEB102",
    name: "JavaScript cơ bản",
  },

  {
    id: "git",
    code: "WEB103",
    name: "Git & GitHub",
  },

  {
    id: "uiux",
    code: "WEB104",
    name: "UI/UX cơ bản",
  },

  {
    id: "react",
    code: "WEB201",
    name: "ReactJS",
  },

  {
    id: "ielts-foundation",
    code: "IELTS101",
    name: "IELTS Foundation",
  },

  {
    id: "ielts-reading",
    code: "IELTS201",
    name: "IELTS Reading",
  },

  {
    id: "ielts-writing",
    code: "IELTS202",
    name: "IELTS Writing",
  },

  {
    id: "communication-basic",
    code: "ENG101",
    name: "Giao tiếp cơ bản",
  },

  {
    id: "communication-advanced",
    code: "ENG201",
    name: "Giao tiếp nâng cao",
  },

  {
    id: "data-basic",
    code: "DATA101",
    name: "Nhập môn phân tích dữ liệu",
  },

  {
    id: "excel",
    code: "DATA102",
    name: "Excel cho phân tích dữ liệu",
  },

  {
    id: "sql",
    code: "DATA201",
    name: "SQL cơ bản",
  },

  {
    id: "powerbi",
    code: "DATA202",
    name: "Power BI",
  },
];


// ======================================================
// 2. CHƯƠNG TRÌNH ĐÀO TẠO MẶC ĐỊNH
// ======================================================

const defaultPrograms = {

  frontend: [
    {
      id: "html-css",
      prerequisiteId: null,
    },

    {
      id: "javascript",
      prerequisiteId: "html-css",
    },

    {
      id: "git",
      prerequisiteId: null,
    },

    {
      id: "react",
      prerequisiteId: "javascript",
    },
  ],


  ielts: [
    {
      id: "ielts-foundation",
      prerequisiteId: null,
    },

    {
      id: "ielts-reading",
      prerequisiteId: "ielts-foundation",
    },

    {
      id: "ielts-writing",
      prerequisiteId: "ielts-foundation",
    },
  ],


  communication: [
    {
      id: "communication-basic",
      prerequisiteId: null,
    },

    {
      id: "communication-advanced",
      prerequisiteId: "communication-basic",
    },
  ],


  data: [
    {
      id: "data-basic",
      prerequisiteId: null,
    },

    {
      id: "excel",
      prerequisiteId: "data-basic",
    },

    {
      id: "sql",
      prerequisiteId: "data-basic",
    },

    {
      id: "powerbi",
      prerequisiteId: "excel",
    },
  ],
};


// ======================================================
// 3. LOCAL STORAGE
// ======================================================

const STORAGE_KEY = "edu-training-programs";

let programs = loadPrograms();


// ======================================================
// 4. LẤY DOM
// ======================================================

const programSelect =
  document.getElementById(
    "training-program-select"
  );

const subjectSelect =
  document.getElementById(
    "subject-select"
  );

const btnAddSubject =
  document.getElementById(
    "btn-add-subject"
  );

const subjectList =
  document.getElementById(
    "subject-list"
  );

const statusElement =
  document.getElementById(
    "training-program-status"
  );


// ======================================================
// 5. CHƯƠNG TRÌNH ĐANG CHỌN
// ======================================================

let currentProgram =
  programSelect?.value || "frontend";


// ======================================================
// 6. LOAD LOCAL STORAGE
// ======================================================

function loadPrograms() {

  const saved =
    localStorage.getItem(
      STORAGE_KEY
    );


  if (!saved) {

    const clone =
      structuredClone(
        defaultPrograms
      );

    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify(clone)
    );

    return clone;
  }


  try {

    return JSON.parse(saved);

  } catch (error) {

    console.error(
      "Không thể đọc dữ liệu chương trình:",
      error
    );

    return structuredClone(
      defaultPrograms
    );
  }
}


// ======================================================
// 7. LƯU LOCAL STORAGE
// ======================================================

function savePrograms() {

  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify(programs)
  );
}


// ======================================================
// 8. LẤY DANH SÁCH MÔN CHƯƠNG TRÌNH HIỆN TẠI
// ======================================================

function getCurrentSubjects() {

  if (!programs[currentProgram]) {
    programs[currentProgram] = [];
  }

  return programs[currentProgram];
}


// ======================================================
// 9. TÌM THÔNG TIN MÔN HỌC
// ======================================================

function getSubjectById(id) {

  return subjectCatalog.find(
    (subject) =>
      subject.id === id
  );
}


// ======================================================
// 10. HIỂN THỊ THÔNG BÁO
// ======================================================

let statusTimer = null;


function showStatus(
  message,
  type = "success"
) {

  if (!statusElement) {
    return;
  }


  clearTimeout(
    statusTimer
  );


  statusElement.textContent =
    message;


  statusElement.hidden =
    false;


  statusElement.dataset.type =
    type;


  statusTimer = setTimeout(
    () => {

      statusElement.hidden =
        true;

    },
    2500
  );
}


// ======================================================
// 11. RENDER DROPDOWN THÊM MÔN
// ======================================================

function renderSubjectSelect() {

  if (!subjectSelect) {
    return;
  }


  const currentSubjects =
    getCurrentSubjects();


  subjectSelect.innerHTML = `
    <option value="">
      -- Chọn môn học --
    </option>
  `;


  const availableSubjects =
    subjectCatalog.filter(
      (subject) => {

        return !currentSubjects.some(
          (item) =>
            item.id === subject.id
        );

      }
    );


  availableSubjects.forEach(
    (subject) => {

      const option =
        document.createElement(
          "option"
        );


      option.value =
        subject.id;


      option.textContent =
        `${subject.code} - ${subject.name}`;


      subjectSelect.appendChild(
        option
      );
    }
  );


  if (
    availableSubjects.length === 0
  ) {

    const option =
      document.createElement(
        "option"
      );

    option.disabled =
      true;

    option.textContent =
      "Không còn môn để thêm";

    subjectSelect.appendChild(
      option
    );
  }
}


// ======================================================
// 12. TẠO OPTION MÔN TIÊN QUYẾT
// ======================================================

function createPrerequisiteOptions(
  currentSubjectId,
  prerequisiteId
) {

  const currentSubjects =
    getCurrentSubjects();


  const currentIndex =
    currentSubjects.findIndex(
      (item) =>
        item.id === currentSubjectId
    );


  let html = `
    <option value="">
      Không có
    </option>
  `;


  currentSubjects.forEach(
    (item, index) => {

      // Không được chọn chính môn đó
      if (
        item.id === currentSubjectId
      ) {
        return;
      }


      // Chỉ cho phép chọn các môn
      // đứng trước làm tiên quyết
      if (
        index >= currentIndex
      ) {
        return;
      }


      const subject =
        getSubjectById(
          item.id
        );


      if (!subject) {
        return;
      }


      const selected =
        prerequisiteId === item.id
          ? "selected"
          : "";


      html += `
        <option
          value="${item.id}"
          ${selected}
        >
          ${subject.code}
          -
          ${subject.name}
        </option>
      `;
    }
  );


  return html;
}


// ======================================================
// 13. RENDER BẢNG MÔN HỌC
// ======================================================

function renderProgram() {

  if (!subjectList) {
    return;
  }


  const currentSubjects =
    getCurrentSubjects();


  subjectList.innerHTML =
    "";


  if (
    currentSubjects.length === 0
  ) {

    subjectList.innerHTML = `
      <tr>
        <td
          colspan="5"
          class="training-empty"
        >
          Chương trình chưa có môn học.
        </td>
      </tr>
    `;


    renderSubjectSelect();

    return;
  }


  currentSubjects.forEach(
    (item, index) => {

      const subject =
        getSubjectById(
          item.id
        );


      if (!subject) {
        return;
      }


      const row =
        document.createElement(
          "tr"
        );


      row.className =
        "training-subject-row";


      row.draggable =
        true;


      row.dataset.subjectId =
        subject.id;


      row.innerHTML = `

        <td>

          <div
            class="training-order"
          >

            <span
              class="training-drag-handle"
              title="Giữ và kéo để đổi thứ tự"
            >
              <i
                data-lucide="grip-vertical"
                aria-hidden="true"
              ></i>
            </span>


            <span
              class="training-order-number"
            >
              ${index + 1}
            </span>

          </div>

        </td>


        <td class="font-bold">

          ${subject.code}

        </td>


        <td>

          ${subject.name}

        </td>


        <td>

          <select
            class="
              app-select
              app-input-sm
              prerequisite-select
            "
            data-subject-id="${subject.id}"
          >

            ${createPrerequisiteOptions(
              subject.id,
              item.prerequisiteId
            )}

          </select>

        </td>


        <td>

          <button
            type="button"
            class="
              app-btn
              app-btn-outline
              app-btn-sm
              btn-remove-subject
            "
            data-subject-id="${subject.id}"
          >

            <i
              data-lucide="trash-2"
              aria-hidden="true"
            ></i>

            Gỡ

          </button>

        </td>
      `;


      subjectList.appendChild(
        row
      );
    }
  );


  renderSubjectSelect();

  bindRemoveEvents();

  bindPrerequisiteEvents();

  bindDragEvents();


  if (
    window.lucide
  ) {

    window.lucide.createIcons();
  }
}


// ======================================================
// 14. ĐỔI CHƯƠNG TRÌNH
// ======================================================

programSelect?.addEventListener(
  "change",
  () => {

    currentProgram =
      programSelect.value;


    renderProgram();


    showStatus(
      "Đã chuyển chương trình đào tạo."
    );
  }
);


// ======================================================
// 15. THÊM MÔN HỌC
// ======================================================

btnAddSubject?.addEventListener(
  "click",
  () => {

    const subjectId =
      subjectSelect.value;


    if (!subjectId) {

      showStatus(
        "Vui lòng chọn môn học.",
        "warning"
      );

      return;
    }


    const currentSubjects =
      getCurrentSubjects();


    const existed =
      currentSubjects.some(
        (item) =>
          item.id === subjectId
      );


    if (existed) {

      showStatus(
        "Môn học đã có trong chương trình.",
        "warning"
      );

      return;
    }


    currentSubjects.push({

      id: subjectId,

      prerequisiteId: null,

    });


    savePrograms();

    renderProgram();


    showStatus(
      "Đã thêm môn học vào chương trình."
    );
  }
);


// ======================================================
// 16. SỰ KIỆN GỠ MÔN
// ======================================================

function bindRemoveEvents() {

  const buttons =
    document.querySelectorAll(
      ".btn-remove-subject"
    );


  buttons.forEach(
    (button) => {

      button.addEventListener(
        "click",
        () => {

          const subjectId =
            button.dataset.subjectId;


          removeSubject(
            subjectId
          );
        }
      );
    }
  );
}


// ======================================================
// 17. GỠ MÔN
// ======================================================

function removeSubject(
  subjectId
) {

  const subject =
    getSubjectById(
      subjectId
    );


  const confirmed =
    window.confirm(
      `Bạn có chắc muốn gỡ "${subject?.name}" khỏi chương trình?`
    );


  if (!confirmed) {
    return;
  }


  let currentSubjects =
    getCurrentSubjects();


  currentSubjects =
    currentSubjects.filter(
      (item) =>
        item.id !== subjectId
    );


  // Nếu môn bị xóa đang là
  // tiên quyết của môn khác
  // thì bỏ môn tiên quyết đó
  currentSubjects.forEach(
    (item) => {

      if (
        item.prerequisiteId ===
        subjectId
      ) {

        item.prerequisiteId =
          null;
      }
    }
  );


  programs[currentProgram] =
    currentSubjects;


  savePrograms();

  renderProgram();


  showStatus(
    "Đã gỡ môn học khỏi chương trình."
  );
}


// ======================================================
// 18. EVENT MÔN TIÊN QUYẾT
// ======================================================

function bindPrerequisiteEvents() {

  const selects =
    document.querySelectorAll(
      ".prerequisite-select"
    );


  selects.forEach(
    (select) => {

      select.addEventListener(
        "change",
        () => {

          const subjectId =
            select.dataset.subjectId;


          const prerequisiteId =
            select.value || null;


          updatePrerequisite(
            subjectId,
            prerequisiteId
          );
        }
      );
    }
  );
}


// ======================================================
// 19. CẬP NHẬT MÔN TIÊN QUYẾT
// ======================================================

function updatePrerequisite(
  subjectId,
  prerequisiteId
) {

  const currentSubjects =
    getCurrentSubjects();


  const subject =
    currentSubjects.find(
      (item) =>
        item.id === subjectId
    );


  if (!subject) {
    return;
  }


  subject.prerequisiteId =
    prerequisiteId;


  savePrograms();


  showStatus(
    "Đã cập nhật môn tiên quyết."
  );
}


// ======================================================
// 20. KÉO THẢ
// ======================================================

let draggedSubjectId =
  null;


function bindDragEvents() {

  const rows =
    document.querySelectorAll(
      ".training-subject-row"
    );


  rows.forEach(
    (row) => {

      row.addEventListener(
        "dragstart",
        (event) => {

          draggedSubjectId =
            row.dataset.subjectId;


          row.classList.add(
            "is-dragging"
          );


          if (
            event.dataTransfer
          ) {

            event.dataTransfer.effectAllowed =
              "move";


            event.dataTransfer.setData(
              "text/plain",
              draggedSubjectId
            );
          }
        }
      );


      row.addEventListener(
        "dragend",
        () => {

          draggedSubjectId =
            null;


          clearDragStyles();
        }
      );


      row.addEventListener(
        "dragover",
        (event) => {

          event.preventDefault();


          row.classList.add(
            "is-drag-over"
          );
        }
      );


      row.addEventListener(
        "dragleave",
        () => {

          row.classList.remove(
            "is-drag-over"
          );
        }
      );


      row.addEventListener(
        "drop",
        (event) => {

          event.preventDefault();


          const targetId =
            row.dataset.subjectId;


          row.classList.remove(
            "is-drag-over"
          );


          if (
            !draggedSubjectId
          ) {
            return;
          }


          if (
            draggedSubjectId ===
            targetId
          ) {
            return;
          }


          moveSubject(
            draggedSubjectId,
            targetId
          );
        }
      );

    }
  );
}


// ======================================================
// 21. XÓA STYLE KÉO THẢ
// ======================================================

function clearDragStyles() {

  document
    .querySelectorAll(
      ".training-subject-row"
    )
    .forEach(
      (row) => {

        row.classList.remove(
          "is-dragging",
          "is-drag-over"
        );
      }
    );
}


// ======================================================
// 22. DI CHUYỂN MÔN
// ======================================================

function moveSubject(
  draggedId,
  targetId
) {

  const currentSubjects =
    getCurrentSubjects();


  const draggedIndex =
    currentSubjects.findIndex(
      (item) =>
        item.id === draggedId
    );


  const targetIndex =
    currentSubjects.findIndex(
      (item) =>
        item.id === targetId
    );


  if (
    draggedIndex === -1 ||
    targetIndex === -1
  ) {

    return;
  }


  const [draggedItem] =
    currentSubjects.splice(
      draggedIndex,
      1
    );


  currentSubjects.splice(
    targetIndex,
    0,
    draggedItem
  );


  fixInvalidPrerequisites(
    currentSubjects
  );


  savePrograms();

  renderProgram();


  showStatus(
    "Đã lưu thứ tự môn học mới."
  );
}


// ======================================================
// 23. KIỂM TRA MÔN TIÊN QUYẾT SAU KHI KÉO
// ======================================================

function fixInvalidPrerequisites(
  subjects
) {

  subjects.forEach(
    (subject, subjectIndex) => {

      if (
        !subject.prerequisiteId
      ) {
        return;
      }


      const prerequisiteIndex =
        subjects.findIndex(
          (item) =>
            item.id ===
            subject.prerequisiteId
        );


      // Nếu môn tiên quyết bị đẩy xuống
      // sau môn hiện tại thì bỏ quan hệ
      if (
        prerequisiteIndex === -1 ||
        prerequisiteIndex >=
          subjectIndex
      ) {

        subject.prerequisiteId =
          null;
      }
    }
  );
}


// ======================================================
// 24. KHỞI TẠO
// ======================================================

renderProgram();