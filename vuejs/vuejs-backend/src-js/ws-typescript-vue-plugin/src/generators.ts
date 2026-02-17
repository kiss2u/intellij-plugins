// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

export function firstNotNull<T, R>(
  generator: Generator<T, void, undefined>,
  transform: (value: T) => R,
): R | undefined {
  for (const item of generator) {
    const result = transform(item)

    if (result !== undefined)
      return result
  }

  return undefined
}
