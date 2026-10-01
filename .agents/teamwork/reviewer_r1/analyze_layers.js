const fs = require('fs');
const path = require('path');

const srcDir = path.resolve('c:/escolaweb/src/main/java/com/andretti101/escolaweb');

function getJavaFiles(dir) {
    let results = [];
    const list = fs.readdirSync(dir);
    list.forEach(file => {
        const fullPath = path.join(dir, file);
        const stat = fs.statSync(fullPath);
        if (stat && stat.isDirectory()) {
            results = results.concat(getJavaFiles(fullPath));
        } else if (file.endsWith('.java')) {
            results.push(fullPath);
        }
    });
    return results;
}

const javaFiles = getJavaFiles(srcDir);

// Collect all string literals with their context
const allStrings = [];

javaFiles.forEach(file => {
    const relPath = path.relative('c:/escolaweb', file).replace(/\\/g, '/');
    const content = fs.readFileSync(file, 'utf8');
    const lines = content.split('\n');

    lines.forEach((line, idx) => {
        const strRegex = /"((?:\\.|[^"\\])*)"/g;
        let match;
        while ((match = strRegex.exec(line)) !== null) {
            const val = match[1];
            // Ignore imports, annotations like @Table(name = "..."), column names, loggers, URLs, etc.
            // But keep validation messages and exception strings
            allStrings.push({
                file: relPath,
                line: idx + 1,
                str: val,
                rawLine: line.trim()
            });
        }
    });
});

console.log('Total string literals found:', allStrings.length);

// Filter strings in DTO, Model, Service layers
const layerStrings = allStrings.filter(s => 
    s.file.includes('/dto/') || 
    s.file.includes('/model/') || 
    s.file.includes('/service/')
);

console.log('Strings in DTO/Model/Service:', layerStrings.length);

// Let's print any string with double space:
console.log('\n--- STRINGS WITH DOUBLE SPACES ---');
layerStrings.forEach(s => {
    if (s.str.includes('  ')) {
        console.log(`[${s.file}:${s.line}] "${s.str}"`);
    }
});

// Let's print any string that has suspicious words or non-ascii unicode escapes
console.log('\n--- STRINGS WITH UNICODE ESCAPES ---');
layerStrings.forEach(s => {
    if (/\\u[0-9a-fA-F]{4}/.test(s.str)) {
        console.log(`[${s.file}:${s.line}] "${s.str}"`);
    }
});

// Let's tokenize all words in these strings and list words that look like broken Portuguese
console.log('\n--- TOKENIZED WORD CHECK ---');
const words = new Map();
layerStrings.forEach(s => {
    // Only user-facing messages: annotations (message = "..."), throw new ...Exception("..."), etc.
    const isUserFacing = s.rawLine.includes('message =') ||
                         s.rawLine.includes('Exception(') ||
                         s.rawLine.includes('throw new') ||
                         s.rawLine.includes('return ') ||
                         s.rawLine.includes('description =') ||
                         s.rawLine.includes('String ') ||
                         s.rawLine.includes('+ "');

    if (!isUserFacing && !s.file.includes('/dto/') && !s.file.includes('/model/entity/')) {
        return;
    }

    // tokenize
    const tokens = s.str.match(/[a-zA-ZáàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ]+/g) || [];
    tokens.forEach(t => {
        const lower = t.toLowerCase();
        if (!words.has(lower)) {
            words.set(lower, []);
        }
        words.get(lower).push(`${s.file}:${s.line}: "${s.str}"`);
    });
});

const wordList = Array.from(words.keys()).sort();
fs.writeFileSync('c:/escolaweb/.agents/teamwork/reviewer_r1/extracted_words.json', JSON.stringify({
    uniqueWords: wordList,
    totalCount: wordList.length
}, null, 2));

console.log('Unique words extracted:', wordList.length);
