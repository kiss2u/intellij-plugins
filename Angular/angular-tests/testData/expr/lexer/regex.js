// should tokenize a simple regex
/abc/
// should tokenize a regex with flags
/abc/gim
// should tokenize an identifier immediately after a regex
/abc/ g
// should tokenize a regex with an escaped slashes
/^http:\/\/foo\.bar/
// should tokenize a regex with un-escaped slashes in a character class
/[a/]$/
// should tokenize a regex with a backslash
/a\w+/
// should tokenize a regex after an operator
a = /b/
// should tokenize a regex inside parentheses
log(/a/)
// should tokenize a regex at the beggining of an array
[/a/]
// should tokenize a regex in the middle of an array
[1, /a/, 2]
// should tokenize a regex inside an object literal
{a: /b/}
// should tokenize a regex after a negation operator
log(!/a/.test("1"))
// should tokenize a regex after several negation operators
log(!!!!!!/a/.test("1"))
// should tokenize a method call on a regex
/abc/.test("foo")
// should not tokenize a regex preceded by a square bracket
a[0] /= b/
// should not tokenize a regex preceded by an identifier
a /b/
// should not tokenize a regex preceded by a number
1 /b/
// should not tokenize a regex that is preceded by a string
"a" /b/
// should not tokenize a regex preceded by a closing parenthesis
(a) /b/
// should not tokenize a regex that is preceded by a keyword
this / b
// should not tokenize a regex preceded by a non-null assertion on an identifier
foo! / 2
// should not tokenize a regex preceded by a non-null assertion on a function call
foo()! / 2
// should not tokenize a regex preceded by a non-null assertion on an array
[1]! / 2
// should not tokenize consecutive regexes
/ 1 / 2 / 3 / 4
// should not tokenize regex-like characters inside of a pipe
foo / 1000 | date: 'M/d/yy'
//should produce an unterminated regex
/a