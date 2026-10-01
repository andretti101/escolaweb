const fs = require('fs');
const throws = JSON.parse(fs.readFileSync('c:/escolaweb/.agents/teamwork/reviewer_r1/all_throws.json', 'utf8'));

const lines = [];
throws.forEach(t => {
    if (t.strings.length > 0) {
        lines.push(`${t.file}:${t.line}`);
        t.strings.forEach(s => lines.push(`   "${s}"`));
    }
});

fs.writeFileSync('c:/escolaweb/.agents/teamwork/reviewer_r1/all_throw_strings.txt', lines.join('\n'), 'utf8');
console.log('Saved', lines.length, 'lines to all_throw_strings.txt');
