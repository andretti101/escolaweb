const fs = require('fs');
const path = require('path');

const respDir = path.resolve('c:/escolaweb/src/main/java/com/andretti101/escolaweb/dto/response');
const files = fs.readdirSync(respDir).filter(f => f.endsWith('.java'));

files.forEach(f => {
    const content = fs.readFileSync(path.join(respDir, f), 'utf8');
    const matches = content.match(/"([^"\\]*)"/g);
    if (matches) {
        console.log(f + ': ' + matches.join(', '));
    }
});
