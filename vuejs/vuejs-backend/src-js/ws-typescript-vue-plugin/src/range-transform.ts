// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import type {Language} from "@volar/language-core"
import {forEachEmbeddedCode} from "@volar/language-core"
import {RangeTransform, ScriptMapper} from "./script-mapper"
import {firstNotNull} from "./generators"

export function toSourceRangeTransform(
  language: Language<string>,
  fileName: string,
): RangeTransform {
  return toRangeTransform(language, fileName, mapper => mapper.toSourceRange)
}

export function toGeneratedRangeTransform(
  language: Language<string>,
  fileName: string,
): RangeTransform {
  return toRangeTransform(language, fileName, mapper => mapper.toGeneratedRange)
}

function toRangeTransform(
  language: Language<string>,
  fileName: string,
  getRangeTransform: (mapper: ScriptMapper) => RangeTransform,
): RangeTransform {
  return (startOffset, endOffset) =>
    firstNotNull(
      scriptMappers(language, fileName),
      mapper => getRangeTransform(mapper)(startOffset, endOffset),
    )
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
