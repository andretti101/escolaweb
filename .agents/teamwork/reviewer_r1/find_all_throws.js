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
const throws = [];

files.forEach(file => {
    const rel = path.relative('c:/escolaweb', file).replace(/\\/g, '/');
    const content = fs.readFileSync(file, 'utf8');

    // Match throw new ... ( ... );
    const regex = /throw\s+new\s+[\w\.]+\s*\(([\s\S]*?)\);/g;
    let match;
    while ((match = regex.exec(content)) !== null) {
        const arg = match[1].trim();
        // find string literals inside arg
        const strRegex = /"((?:\\.|[^"\\])*)"/g;
        let sm;
        const strings = [];
        while ((sm = strRegex.exec(arg)) !== null) {
            strings.push(sm[1]);
        }
        
        // compute line number
        const line = content.substring(0, match.index).split('\n').length;
        throws.push({
            file: rel,
            line: line,
            arg: arg,
            strings: strings
        });
    }
});

fs.writeFileSync('c:/escolaweb/.agents/teamwork/reviewer_r1/all_throws.json', JSON.stringify(throws, null, 2), 'utf8');
console.log('Saved', throws.length, 'throws');
