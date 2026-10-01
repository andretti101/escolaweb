const fs = require('fs');

const lines = fs.readFileSync('c:/escolaweb/.agents/teamwork/reviewer_r1/non_user_facing_strings.txt', 'utf8').split('\n');

const suspicious = [
    /\b\w*(?:ttulo|obrigatr|divis|perod|conclus|matrcul|mdia|mnim|mxim|frequnc|possvel|invlid|usur|cdig|descri|horr|relatr|exclus|altera|educa|padr|srie|nmer|endere|informa|situa)\w*\b/i,
    /\\u[0-9a-fA-F]{4}/,
    /\s{2,}/
];

lines.forEach(l => {
    for (const pat of suspicious) {
        if (pat.test(l)) {
            console.log(l);
            break;
        }
    }
});
