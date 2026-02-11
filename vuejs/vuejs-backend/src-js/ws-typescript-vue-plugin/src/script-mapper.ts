// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import type {CodeInformation, Mapper} from "@volar/language-core"

export type SimpleRange = [
  startOffset: number,
  endOffset: number,
] | undefined

const LOCATION_FILTER: (data: CodeInformation) => boolean =
  data => !!data.verification

type OffsetTransform = (offset: number) => number | undefined

export class ScriptMapper {
  constructor(
    readonly mapper: Mapper,
    readonly tsShift: number,
  ) {
  }

  toSourceRange = (
    startOffset: number,
    endOffset: number,
  ): SimpleRange =>
    this.#toRange(startOffset, endOffset, this.#toSourceOffset)

  toGeneratedRange = (
    startOffset: number,
    endOffset: number,
  ): SimpleRange =>
    this.#toRange(startOffset, endOffset, this.#toGeneratedOffset)

  #toRange = (
    startOffset: number,
    endOffset: number,
    transformOffset: OffsetTransform,
  ): SimpleRange => {
    const start = transformOffset(startOffset)
    if (start === undefined) return undefined

    const end = transformOffset(endOffset)
    if (end === undefined) return undefined

    return [start, end]
  }

  #toSourceOffset: OffsetTransform = offset => {
    for (const [sourceOffset] of this.mapper.toSourceLocation(offset - this.tsShift, LOCATION_FILTER)) {
      return sourceOffset
    }

    return undefined
  }

  #toGeneratedOffset: OffsetTransform = offset => {
    for (const [generatedOffset] of this.mapper.toGeneratedLocation(offset, LOCATION_FILTER)) {
      return generatedOffset + this.tsShift
    }

    return undefined
  }
}
