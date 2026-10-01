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
const wordMap = new Map();

files.forEach(file => {
    const rel = path.relative('c:/escolaweb', file).replace(/\\/g, '/');
    const content = fs.readFileSync(file, 'utf8');
    const lines = content.split('\n');

    lines.forEach((line, idx) => {
        const strRegex = /"((?:\\.|[^"\\])*)"/g;
        let match;
        while ((match = strRegex.exec(line)) !== null) {
            const str = match[1];
            // If string contains letters and space or accents, or is a message
            if (/[a-zA-ZáàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ]/.test(str)) {
                const words = str.match(/[a-zA-ZáàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ]+/g) || [];
                words.forEach(w => {
                    const lower = w.toLowerCase();
                    if (!wordMap.has(lower)) {
                        wordMap.set(lower, []);
                    }
                    wordMap.get(lower).push({ file: rel, line: idx + 1, str: str });
                });
            }
        }
    });
});

console.log('Total unique words in all string literals in src/main/java:', wordMap.size);

// Print all unique words sorted
const sortedWords = Array.from(wordMap.keys()).sort();
fs.writeFileSync('c:/escolaweb/.agents/teamwork/reviewer_r1/all_src_words.json', JSON.stringify(sortedWords, null, 2), 'utf8');
