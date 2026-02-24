const foo: Partial<{ bar: string; baz: number; }> = {};
foo.<caret><error>b</error>;