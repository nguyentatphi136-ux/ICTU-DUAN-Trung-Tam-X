const fs = require('fs');

const html = fs.readFileSync('frontend/admin.html', 'utf8');
const js = fs.readFileSync('frontend/app.js', 'utf8');

function createElement(id = '', tag = 'div', classes = '') {
  return {
    id,
    tagName: tag.toUpperCase(),
    value: '',
    textContent: '',
    innerHTML: '',
    className: classes,
    classList: {
      add: () => {},
      remove: () => {},
      toggle: () => {},
      contains: () => false,
    },
    setAttribute: () => {},
    getAttribute: () => '',
    addEventListener: () => {},
    dataset: {},
    focus: () => {},
    reset: () => {},
    querySelector: (sel) => {
      if (sel === 'tbody') return createElement('tbody', 'tbody');
      return mockQuerySelector(sel);
    },
    querySelectorAll: (sel) => [],
    closest: () => null,
  };
}

function mockQuerySelector(sel) {
  if (!sel) return null;
  const cleanId = sel.startsWith('#') ? sel.slice(1) : sel;
  if (html.includes(`id="${cleanId}"`)) {
    return createElement(cleanId);
  }
  if (html.includes(`class="${sel.startsWith('.') ? sel.slice(1) : sel}"`)) {
    return createElement('', 'div', sel);
  }
  return null;
}

global.document = {
  body: {
    dataset: { role: 'admin' },
    classList: { add: () => {}, remove: () => {}, toggle: () => {} },
  },
  title: '',
  querySelector: mockQuerySelector,
  querySelectorAll: (sel) => {
    return [];
  },
  addEventListener: () => {},
};

global.window = {
  document: global.document,
  location: { href: 'http://localhost:5173/admin.html' },
  localStorage: {
    getItem: (k) => {
      if (k === 'edumanager-session') return JSON.stringify({ role: 'admin', fullName: 'Quản trị viên', email: 'admin@edumanager.vn', expiresAt: Date.now() + 1000000 });
      return null;
    },
    setItem: () => {},
    removeItem: () => {},
  },
  sessionStorage: {
    getItem: (k) => {
      if (k === 'edumanager-session') return JSON.stringify({ role: 'admin', fullName: 'Quản trị viên', email: 'admin@edumanager.vn', expiresAt: Date.now() + 1000000 });
      return null;
    },
    setItem: () => {},
    removeItem: () => {},
  },
  lucide: { createIcons: () => {} },
  setTimeout: setTimeout,
  clearTimeout: clearTimeout,
  fetch: () => Promise.resolve({ ok: true, json: () => Promise.resolve({}) }),
};

global.IntersectionObserver = class {
  constructor() {}
  observe() {}
  unobserve() {}
  disconnect() {}
};
global.localStorage = global.window.localStorage;
global.sessionStorage = global.window.sessionStorage;
global.fetch = global.window.fetch;

try {
  eval(js);
  console.log('✅ JS executed on admin.html with NO runtime errors!');
} catch (e) {
  console.error('❌ JS Error on admin.html:', e);
}
