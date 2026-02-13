/abc/
/[a/]$/
/a\w+/
/^http:\/\/foo\.bar/
/abc/g
/[a/]$/gi
/abc/.test("foo")
"foo".match(/(abc)/)[1].toUpperCase()
/abc/.test("foo") && something || somethingElse
