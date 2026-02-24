function test(foo?: { bar?: string; }) {
  <error>foo</error>.<caret><error>b</error>;
}