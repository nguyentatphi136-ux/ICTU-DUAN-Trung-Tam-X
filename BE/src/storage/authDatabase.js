import { randomBytes } from 'node:crypto';
import { mkdir, readFile, rename, writeFile } from 'node:fs/promises';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const defaultDatabasePath = fileURLToPath(new URL('../../data/auth-db.json', import.meta.url));
const databasePath = process.env.AUTH_DB_FILE || defaultDatabasePath;
let databaseCache;
let operationQueue = Promise.resolve();

function createEmptyDatabase() {
  return { users: [], reset_tokens: [] };
}

async function readDatabase() {
  if (databaseCache) return databaseCache;

  try {
    const contents = await readFile(databasePath, 'utf8');
    const parsed = JSON.parse(contents);
    databaseCache = {
      users: Array.isArray(parsed.users) ? parsed.users : [],
      reset_tokens: Array.isArray(parsed.reset_tokens) ? parsed.reset_tokens : [],
    };
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    databaseCache = createEmptyDatabase();
    await persistDatabase(databaseCache);
  }

  return databaseCache;
}

async function persistDatabase(database) {
  await mkdir(dirname(databasePath), { recursive: true });
  const temporaryPath = `${databasePath}.${process.pid}.tmp`;
  await writeFile(temporaryPath, `${JSON.stringify(database, null, 2)}\n`, 'utf8');
  await rename(temporaryPath, databasePath);
}

export function withAuthDatabase(operation) {
  const pendingOperation = operationQueue.then(async () => {
    const currentDatabase = await readDatabase();
    const nextDatabase = structuredClone(currentDatabase);
    const result = await operation(nextDatabase);

    if (JSON.stringify(nextDatabase) !== JSON.stringify(currentDatabase)) {
      await persistDatabase(nextDatabase);
      databaseCache = nextDatabase;
    }

    return result;
  });

  operationQueue = pendingOperation.then(() => undefined, () => undefined);
  return pendingOperation;
}

export async function createPasswordResetToken(email) {
  const normalizedEmail = email.trim().toLowerCase();
  const token = randomBytes(32).toString('hex');
  const expiresAt = new Date(Date.now() + 30 * 60 * 1000).toISOString();

  await withAuthDatabase((database) => {
    database.reset_tokens.push({
      token,
      email: normalizedEmail,
      expires_at: expiresAt,
      is_used: false,
    });
  });

  return { token, email: normalizedEmail, expires_at: expiresAt, is_used: false };
}

export async function provisionUser(email, passwordHash) {
  const normalizedEmail = email.trim().toLowerCase();

  return withAuthDatabase((database) => {
    const existingUser = database.users.find((user) => user.email === normalizedEmail);
    if (existingUser) {
      existingUser.password_hash = passwordHash;
    } else {
      database.users.push({ email: normalizedEmail, password_hash: passwordHash });
    }
  });
}
