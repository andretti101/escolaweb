const fs = require('fs');
const path = require('path');

const srcDir = path.resolve('c:/escolaweb/src/main/java/com/andretti101/escolaweb');

function getJavaFiles(dir) {
    let results = [];
    fs.readdirSync(dir).forEach(file => {
        const full = path.join(dir, file);
        if (fs.statSync(full).isDirectory()) results = results.concat(getJavaFiles(full));
        else if (file.endsWith('.java')) results.push(full);
    });
    return results;
}

const files = getJavaFiles(srcDir);
const out = [];

files.forEach(file => {
    const rel = path.relative('c:/escolaweb', file).replace(/\\/g, '/');
    const content = fs.readFileSync(file, 'utf8');
    const lines = content.split('\n');

    lines.forEach((l, idx) => {
        if (l.includes('throw new') || l.includes('message =') || l.includes('Exception(')) {
            out.push(`${rel}:${idx + 1}: ${l.trim()}`);
        }
    });
});

fs.writeFileSync('c:/escolaweb/.agents/teamwork/reviewer_r1/all_messages.txt', out.join('\n'), 'utf8');
console.log('Saved', out.length, 'lines in UTF-8');
