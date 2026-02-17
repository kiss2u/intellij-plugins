// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import type {Language} from "@volar/language-core"
import {forEachEmbeddedCode} from "@volar/language-core"
import {RangeTransform, ScriptMapper} from "./script-mapper"

export function toSourceRangeTransform(
  language: Language<string>,
  fileName: string,
): RangeTransform {
  return (startOffset, endOffset) => {
    for (const {toSourceRange} of scriptMappers(language, fileName)) {
      const range = toSourceRange(startOffset, endOffset)

      if (range !== undefined)
        return range
    }

    return undefined
  }
}

export function toGeneratedRangeTransform(
  language: Language<string>,
  fileName: string,
): RangeTransform {
  return (startOffset, endOffset) => {
    for (const {toGeneratedRange} of scriptMappers(language, fileName)) {
      const range = toGeneratedRange(startOffset, endOffset)

      if (range !== undefined)
        return range
    }

    return undefined
  }
}

function* scriptMappers(
  language: Language<string>,
  fileName: string,
): Generator<ScriptMapper, void, undefined> {
  const sourceScript = language.scripts.get(fileName)
  if (!sourceScript)
    return

  const virtualCode = sourceScript.generated?.root
  if (!virtualCode)
    return

  for (const code of forEachEmbeddedCode(virtualCode)) {
    if (!code.id.startsWith('script_'))
      continue

    yield new ScriptMapper(
      language.maps.get(code, sourceScript),
      sourceScript.snapshot.getLength(),
    )
  }
}
