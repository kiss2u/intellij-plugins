// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import type ts from "typescript/lib/tsserverlibrary"
import type {Language} from "@volar/language-core"
import type {ReverseMapper} from "tsc-ide-plugin/ide-get-element-type"
import type {Position, Range} from "tsc-ide-plugin/protocol"
import {SimpleRange} from "./script-mapper"
import {toGeneratedRangeTransform, toSourceRangeTransform} from "./range-transform"

type TypeScript = typeof ts

export function createReverseMapper(
  ts: TypeScript,
  language: Language<string>,
): ReverseMapper {
  return (sourceFile, generatedRange) => {
    const sourceRange = toSourceRange(language, sourceFile, generatedRange)

    if (!sourceRange)
      return undefined

    return {
      sourceRange,
      fileName: sourceFile.fileName,
    }
  }
}

function toSourceRange(
  language: Language<string>,
  sourceFile: ts.SourceFile,
  generatedRange: Range,
): Range | undefined {
  const toSourceRange = toSourceRangeTransform(language, sourceFile.fileName)

  const sourceRange = toSourceRange(
    getOffset(sourceFile, generatedRange.start),
    getOffset(sourceFile, generatedRange.end),
  )

  if (sourceRange === undefined)
    return undefined

  return {
    start: sourceFile.getLineAndCharacterOfPosition(sourceRange[0]),
    end: sourceFile.getLineAndCharacterOfPosition(sourceRange[1]),
  }
}

function getOffset(
  sourceFile: ts.SourceFile,
  position: Position,
): number {
  return sourceFile.getPositionOfLineAndCharacter(position.line, position.character)
}

export function toGeneratedRange(
  language: Language<string>,
  fileName: string,
  startOffset: number,
  endOffset: number,
): SimpleRange {
  const toGeneratedRange = toGeneratedRangeTransform(language, fileName)
  return toGeneratedRange(startOffset, endOffset)
}
