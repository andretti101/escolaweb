const fs = require('fs');

const allStrings = JSON.parse(fs.readFileSync('c:/escolaweb/.agents/teamwork/reviewer_r1/all_layer_strings.json', 'utf8'));

const userFacing = [];
const nonUserFacing = [];

allStrings.forEach(item => {
    const code = item.code;
    const str = item.str;

    // Check if it's an annotation message or exception or human-readable message
    if (
        code.includes('message =') ||
        code.includes('Exception(') ||
        code.includes('throw new') ||
        code.includes('return ') ||
        code.includes('description') ||
        str.includes(' ') || // Any string with a space is likely human text
        /[áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ]/.test(str)
    ) {
        // Exclude SQL, @Table, @Column, @JoinTable, @SequenceGenerator, format strings, logger names, etc.
        if (
            code.startsWith('@Table(') ||
            code.startsWith('@Column(') ||
            code.startsWith('@JoinColumn(') ||
            code.startsWith('@JoinTable(') ||
            code.startsWith('@Json') ||
            code.startsWith('@SequenceGenerator') ||
            code.startsWith('@ForeignKey') ||
            code.startsWith('@UniqueConstraint') ||
            str.startsWith('select ') ||
            str.startsWith('SELECT ') ||
            str.startsWith('update ') ||
            str.startsWith('delete ') ||
            str.startsWith('INSERT ')
        ) {
            nonUserFacing.push(item);
        } else {
            userFacing.push(item);
        }
    } else {
        nonUserFacing.push(item);
    }
});

console.log('User facing strings:', userFacing.length);
console.log('Non user facing strings:', nonUserFacing.length);

const output = userFacing.map(item => `[${item.file}:${item.line}] ${item.str}`).join('\n');
fs.writeFileSync('c:/escolaweb/.agents/teamwork/reviewer_r1/user_facing_strings.txt', output, 'utf8');

const nonOutput = nonUserFacing.map(item => `[${item.file}:${item.line}] ${item.str}  |  ${item.code}`).join('\n');
fs.writeFileSync('c:/escolaweb/.agents/teamwork/reviewer_r1/non_user_facing_strings.txt', nonOutput, 'utf8');
