const fs = require('fs');
const path = require('path');

const dir = path.resolve(__dirname);
const canonical = [
  '/', '/welcome', '/onboarding/pattern', '/onboarding/confirm', '/onboarding/phrase',
  '/onboarding/done', '/lock', '/recovery/enter', '/recovery/display', '/recovery/reset-pattern',
  '/restore/progress', '/vault', '/vault/timeline', '/vault/collection/{id}', '/vault/search',
  '/vault/file/{id}', '/vault/import', '/vault/camera', '/vault/camera/review',
  '/vault/camera/review/video', '/vault/camera/permission', '/settings', '/settings/security',
  '/settings/security/duress', '/settings/security/duress/guidance',
  '/settings/security/hidden-vaults', '/settings/security-info', '/settings/privacy',
  '/settings/about', '/settings/cloud/backup', '/settings/cloud/sync', '/settings/cloud/sync/log',
  '/shares', '/vault/share/create', '/vault/share/phrase', '/shared/open', '/shared/{id}',
  '/settings/appearance', '/settings/appearance/icons', '/pro',
];

const required = [
  /entry points/i, /exit points/i, /actions/i, /states/i, /edge cases/i, /data contract/i,
];

const files = fs.readdirSync(dir).filter(f => /^\d\d-.*\.md$/.test(f) && !f.startsWith('00') && !f.startsWith('07') && f !== '08-coverage.md');
const rows = [];
const seen = new Map();
let problems = [];

for (const f of files.sort()) {
  const text = fs.readFileSync(path.join(dir, f), 'utf8');
  const lines = text.split(/\r?\n/).length;
  const heads = [...text.matchAll(/^###\s+(.+)$/gm)].map(m => m[1].trim());
  rows.push({ file: f, lines, screens: heads.length });
  for (const h of heads) {
    if (/^R\d+\b/.test(h)) continue; // family rule, not a screen
    if (/^Overlay:/i.test(h)) { rows[rows.length - 1].screens--; continue; } // global overlay, not a screen route
    if (/^Rule:/i.test(h)) { rows[rows.length - 1].screens--; continue; } // cross-cutting rule, not a screen route
    if (/state on `/i.test(h)) { rows[rows.length - 1].screens--; continue; } // state section, same route
    const m = h.match(/`([^`]+)`\s*$/);
    const route = m ? m[1] : null;
    if (!route) { problems.push(`${f}: heading without backticked route -> ${h}`); continue; }
    if (!canonical.includes(route)) problems.push(`${f}: unknown route ${route}`);
    if (seen.has(route)) problems.push(`duplicate route ${route} in ${f} and ${seen.get(route)}`);
    seen.set(route, f);
    // section presence within the block
    const start = text.indexOf(`### ${h}`);
    const next = text.indexOf('\n### ', start + 4);
    const block = text.slice(start, next === -1 ? undefined : next);
    for (const r of required) {
      if (!r.test(block)) problems.push(`${f} :: ${route} missing /${r.source}/`);
    }
    if (!/stateDiagram/.test(block)) problems.push(`${f} :: ${route} missing mermaid`);
  }
}

const missing = canonical.filter(r => !seen.has(r));
console.log('FILES');
for (const r of rows) console.log(`  ${r.file.padEnd(38)} ${String(r.lines).padStart(5)} lines  ${r.screens} screens`);
console.log(`\nTOTAL screens documented: ${seen.size} / ${canonical.length}`);
if (missing.length) console.log('MISSING ROUTES:\n  ' + missing.join('\n  '));
console.log(`\nPROBLEMS: ${problems.length}`);
for (const p of problems.slice(0, 60)) console.log('  - ' + p);
