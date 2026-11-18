// extract-states.js — reads the exception registry from 08-coverage.md §3.1
// and suppresses documented, intentional omissions from the "MISSING MANDATORY STATES" report.
const fs = require('fs');
const path = require('path');

const dir = path.resolve(__dirname);
const domainFiles = fs.readdirSync(dir)
  .filter(f => /^0[1-6]-.*\.md$/.test(f))
  .sort();

// Mandatory state budget from 00-state-model.md §8.
const CLASS = {
  list: ['/vault', '/vault/timeline', '/vault/collection/{id}', '/vault/search', '/shares', '/settings/cloud/sync/log'],
  detail: ['/vault/file/{id}', '/shared/{id}'],
  form: ['/vault/share/create', '/shared/open', '/recovery/enter', '/onboarding/pattern', '/onboarding/confirm'],
  secret: ['/vault/share/phrase', '/recovery/display', '/settings/security/duress', '/vault/camera/review', '/vault/camera/review/video'],
  oneshot: ['/', '/welcome', '/onboarding/phrase', '/onboarding/done', '/recovery/reset-pattern', '/restore/progress'],
  settings: ['/settings', '/settings/security', '/settings/security/duress/guidance', '/settings/security/hidden-vaults', '/settings/security-info', '/settings/privacy', '/settings/about', '/settings/appearance', '/settings/cloud/backup', '/settings/cloud/sync'],
  task: ['/vault/import'],
  proonly: ['/pro', '/settings/appearance/icons'],
  camera: ['/vault/camera', '/vault/camera/permission'],
};
const MANDATORY = {
  list: ['S0', 'S1', 'S3', 'S4', 'S5', 'S6', 'S9', 'A4', 'V1', 'V4'],
  detail: ['S0', 'S1', 'S3', 'S6', 'S7', 'S9'],
  form: ['S0', 'S3', 'S13', 'S15', 'S16', 'S6', 'S9'],
  secret: ['S3', 'S9'],
  oneshot: ['S0', 'S1', 'S2', 'S3', 'S6', 'S9', 'S15', 'S16'],
  settings: ['S0', 'S3', 'S9'],
  task: ['S0', 'S1', 'S3', 'S5', 'S6', 'S9', 'S15', 'S16'],
  proonly: ['S3', 'S10', 'S15', 'S16', 'S6', 'S9'],
  camera: ['S0', 'S1', 'S3', 'S6', 'S7', 'S9', 'S11', 'S12', 'X4', 'X5'],
};
const routeClass = {};
for (const [cls, routes] of Object.entries(CLASS)) for (const r of routes) routeClass[r] = cls;

// Parse the exception registry from 08-coverage.md §3.1.
// Format: `| \`/route\`` | `S0` | §3.x — reason |`  (codes comma-separated, no backticks)
const EXC = new Map(); // route -> Set(missing mandatory state codes)
const cov = fs.readFileSync(path.join(dir, '08-coverage.md'), 'utf8');
const regStart = cov.indexOf('## 3.1 Machine-Readable Exception Registry');
// The registry table is terminated by a line that is exactly "---" (horizontal rule).
const afterHeader = cov.indexOf('\n| Route', regStart);
let regEnd = cov.indexOf('\n---\n', afterHeader);
if (regEnd === -1) regEnd = cov.indexOf('\n---', afterHeader);
const regBlock = cov.slice(regStart, regEnd === -1 ? undefined : regEnd);
const regLines = regBlock.split(/\r?\n/);
for (const line of regLines) {
  const m = line.match(/^\|\s*`([^`]+)`\s*\|\s*([^|`]+?)\s*\|/);
  if (!m) continue;
  const route = m[1];
  const codes = m[2].split(',').map(s => s.trim()).filter(Boolean);
  if (!EXC.has(route)) EXC.set(route, new Set());
  for (const c of codes) EXC.get(route).add(c);
}

const sections = [];
for (const f of domainFiles) {
  const text = fs.readFileSync(path.join(dir, f), 'utf8');
  const heads = [...text.matchAll(/^###\s+(.+)$/gm)].map(m => m[1].trim());
  for (const h of heads) {
    if (/^R\d+\b/.test(h)) continue; // family rule, not a screen
    if (/^Overlay:/i.test(h)) continue; // global overlay, not a screen route
    if (/state on `/i.test(h)) continue; // state section, same route
    const m = h.match(/`([^`]+)`\s*$/);
    const route = m ? m[1] : null;
    if (!route) continue;
    const start = text.indexOf(`### ${h}`);
    const next = text.indexOf('\n### ', start + 4);
    const block = text.slice(start, next === -1 ? undefined : next);
    // Extract states from the States table (first column = state code, e.g. `| S3 | Content | ...`)
    // plus environment codes (A4, V1, V4) from anywhere in the block.
    const states = new Set();
    const lines = block.split('\n');
    let inStates = false;
    for (const line of lines) {
      if (/^\*\*States\*\*/i.test(line) || /^\|\s*State\b/i.test(line)) { inStates = true; continue; }
      if (inStates) {
        if (/^\s*```mermaid/i.test(line) || /^\*\*(Transitions|Flows|Edge cases)/i.test(line) || /^\s*##\s/i.test(line)) inStates = false;
        if (line.startsWith('|') && !/^\|[\s\-:|]+\|$/.test(line)) {
          const cells = line.split('|').map(s => s.trim()).filter(Boolean);
          if (cells.length) states.add(cells[0]);
          for (const code of line.matchAll(/\b([A-Z]\d+)\b/g)) states.add(code[1]);
        }
      }
    }
    // Also check the rest of the section block for environment codes like A4, V1, V4
    for (const code of block.matchAll(/\b(A\d+|V\d+)\b/g)) states.add(code[1]);
    sections.push({ file: f, route, class: routeClass[route], states: [...states].sort() });
  }
}

const problems = [];
const missing = [];
for (const s of sections) {
  const cls = MANDATORY[s.class];
  if (!cls) continue;
  for (const code of cls) {
    if (s.states.includes(code)) continue;
    const exc = EXC.get(s.route);
    if (exc && exc.has(code)) continue;
    missing.push(`${s.route} (${s.class}) missing ${code}`);
  }
}
// Flag registry entries with no matching section (stale or invented).
const sectionRoutes = new Set(sections.map(s => s.route));
for (const [route, codes] of EXC) {
  if (!sectionRoutes.has(route)) problems.push(`exception registry entry for unknown route ${route}`);
  for (const code of codes) {
    const cls = MANDATORY[routeClass[route]];
    if (cls && !cls.includes(code)) problems.push(`exception registry: ${route} lists ${code} which is not in its class mandatory budget`);
  }
}

console.log('FILES');
for (const f of domainFiles) {
  const text = fs.readFileSync(path.join(dir, f), 'utf8');
  const lines = text.split(/\r?\n/).length;
  const heads = [...text.matchAll(/^###\s+(.+)$/gm)].map(m => m[1].trim())
    .filter(h => !/^R\d+\b/.test(h) && !/^Overlay:/i.test(h) && !/state on `/i.test(h) && /`([^`]+)`\s*$/.test(h));
  console.log(`  ${f.padEnd(38)} ${String(lines).padStart(5)} lines  ${heads.length} screens`);
}
console.log(`\nSECTIONS: ${sections.length}`);
console.log(`\nSECTIONS WITH MISSING MANDATORY STATES: ${missing.length} / ${sections.length}`);
for (const m of missing) console.log('  - ' + m);
console.log(`\nPROBLEMS: ${problems.length}`);
for (const p of problems.slice(0, 60)) console.log('  - ' + p);
fs.writeFileSync(path.join(dir, 'coverage.json'),
  JSON.stringify({ sections, missing, problems }, null, 2));
console.log('Wrote coverage.json');