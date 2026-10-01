const fs = require('fs');

const words = JSON.parse(fs.readFileSync('c:/escolaweb/.agents/teamwork/reviewer_r1/all_src_words.json', 'utf8'));

// Common suffixes or patterns in Portuguese where accents might be missing:
// - Words ending in 'ao' (unless 'ao' or 'grao' or English) where standard Portuguese has 'ão'
// - Words ending in 'oes' where standard Portuguese has 'ões'
// - Words containing 'rio' or 'ria' where standard Portuguese has 'tório'/'tória'/'tário' etc.
// - Words with missing accents: 'numero' -> 'número', 'valido' -> 'válido', 'invalido' -> 'inválido', 'periodo' -> 'período', etc.

const suspected = [];

words.forEach(w => {
    // skip very short words
    if (w.length <= 1) return;

    // Check specific known words that require accents in Portuguese:
    // e.g. nao, ja, ate, voce, so, tambem, esta (when verb está), e (when verb é), etc.
    const knownMissingAccents = [
        'obrigatrio', 'obrigatria', 'diviso', 'perodo', 'perodos', 'concluso', 'matrcula', 'matrculas',
        'mdia', 'mdias', 'mnimo', 'mnima', 'mnimos', 'mnimas', 'mximo', 'mxima', 'mximos', 'mximas',
        'frequncia', 'frequncias', 'possvel', 'possveis', 'ttulo', 'ttulos', 'invlido', 'invlida',
        'usurio', 'usurios', 'cdigo', 'cdigos', 'descrio', 'horrio', 'relatrio', 'relatrios',
        'excluso', 'alterao', 'alteraes', 'educao', 'padro', 'srie', 'sries', 'nmero', 'nmeros',
        'endereo', 'endereos', 'informaes', 'situao', 'situaes', 'avaliao', 'avaliaes', 'presena',
        'presenas', 'atribuio', 'histrico', 'histricos', 'exceo', 'excees', 'validao', 'autenticao',
        'redefinio', 'sesso', 'sesses', 'autorizao', 'autorizaes', 'publicao', 'publicaes',
        'notcia', 'notcias', 'comentrio', 'comentrios', 'dicionrio', 'disciplina', 'poltica',
        'especfico', 'especfica', 'mdulo', 'mdulos', 'automtico', 'automtica', 'automticos',
        'critrio', 'critrios', 'acadmico', 'acadmicos', 'acadmica', 'acadmicas', 'prprio', 'prpria',
        'prprios', 'prprias', 'incio', 'trmino', 'concludo', 'concluda', 'concludos', 'concludas'
    ];

    if (knownMissingAccents.includes(w)) {
        suspected.push({ word: w, reason: 'known corrupted word' });
        return;
    }

    // Check words ending in 'ao' that might be missing tilde (excluding valid ones like ao, cao, nao?)
    if (w.endsWith('ao') && w.length > 3 && !['chao', 'grao', 'acao'].includes(w)) {
        suspected.push({ word: w, reason: 'ends with ao without tilde?' });
    }

    // Check words with consecutive consonants where vowel/accent was stripped:
    // e.g. tr -> tor, dr -> dor, etc.
    if (/[bcdfghjklmnpqrstvwxyz]{4,}/.test(w)) {
        suspected.push({ word: w, reason: '4+ consecutive consonants' });
    }
});

console.log('Suspected words count:', suspected.length);
console.log(JSON.stringify(suspected, null, 2));
