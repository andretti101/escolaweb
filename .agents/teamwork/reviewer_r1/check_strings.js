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
console.log('Total java files:', javaFiles.length);

// Extract string literals from each file
// Look for strings containing suspected patterns:
// 1. Two consecutive spaces inside a string: "  "
// 2. Common words with missing accented letters:
// e.g. "obrigatrio", "obrigatria", "diviso", "perodo", "concluso", "matrcula", "mdia", "mnimo", "mnima", "mximo", "mxima", "frequncia", "ttulo", "possvel", "invlido", "invlida", "no " (when meaning "não"), " usurio", " cdigo", " descrio", "horrio", "relatrio", "excluso", "alterao", "educao", "padro", "srie", "nmero", "endereo", "telefone", "informaes", "situao", "ao", "es", "rio", "ria"
// 3. Unicode escapes like \u00e7 or \u00e9 or corrupted UTF-8 sequences like Ã, Â, etc.
// 4. Any words with unexpected consonants clusters where a vowel should be, like 'tr' instead of 'tór', etc.

const suspiciousPatterns = [
    /\b\w*(?:ttulo|obrigatr|divis|perod|conclus|matrcul|mdia|mnim|mxim|frequnc|possvel|invlid|usur|cdig|descri|horr|relatr|exclus|altera|educa|padr|srie|nmer|endere|informa|situa)\w*\b/i,
    /\\u[0-9a-fA-F]{4}/,
    /[ÃÂ][\x80-\xBF]/,
    /\b[Nn]o\s+(?:poss|possu|est|foi|pode|encontrad|autorizad|deve|h\b|permitid)/,
    /\s{2,}/
];

let issues = [];

javaFiles.forEach(file => {
    const relPath = path.relative('c:/escolaweb', file).replace(/\\/g, '/');
    const content = fs.readFileSync(file, 'utf8');
    
    // Simple regex to find string literals
    const strRegex = /"((?:\\.|[^"\\])*)"/g;
    let match;
    let lineNum = 1;
    let lastIndex = 0;
    
    while ((match = strRegex.exec(content)) !== null) {
        const strVal = match[1];
        // calculate line number
        const linesBefore = content.substring(lastIndex, match.index).split('\n');
        lineNum += linesBefore.length - 1;
        lastIndex = match.index;
        
        // Check patterns
        for (const pattern of suspiciousPatterns) {
            if (pattern.test(strVal)) {
                issues.push({
                    file: relPath,
                    line: lineNum,
                    pattern: pattern.toString(),
                    str: strVal
                });
                break;
            }
        }
    }
});

console.log('Suspicious strings found:', issues.length);
issues.forEach(iss => {
    console.log(`[${iss.file}:${iss.line}] (${iss.pattern}): "${iss.str}"`);
});
