const fs = require('fs');
const path = require('path');

const layers = [
    'c:/escolaweb/src/main/java/com/andretti101/escolaweb/dto',
    'c:/escolaweb/src/main/java/com/andretti101/escolaweb/model',
    'c:/escolaweb/src/main/java/com/andretti101/escolaweb/service'
];

function getJavaFiles(dir) {
    let results = [];
    fs.readdirSync(dir).forEach(file => {
        const full = path.join(dir, file);
        if (fs.statSync(full).isDirectory()) results = results.concat(getJavaFiles(full));
        else if (file.endsWith('.java')) results.push(full);
    });
    return results;
}

let files = [];
layers.forEach(l => {
    files = files.concat(getJavaFiles(path.resolve(l)));
});

const report = [];

files.forEach(file => {
    const rel = path.relative('c:/escolaweb', file).replace(/\\/g, '/');
    const content = fs.readFileSync(file, 'utf8');
    const lines = content.split('\n');

    lines.forEach((line, idx) => {
        const strRegex = /"((?:\\.|[^"\\])*)"/g;
        let match;
        while ((match = strRegex.exec(line)) !== null) {
            report.push({
                file: rel,
                line: idx + 1,
                str: match[1],
                code: line.trim()
            });
        }
    });
});

fs.writeFileSync('c:/escolaweb/.agents/teamwork/reviewer_r1/all_layer_strings.json', JSON.stringify(report, null, 2), 'utf8');
console.log('Total strings across DTO, Model, Service:', report.length);
