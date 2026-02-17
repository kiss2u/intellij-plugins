// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import type {CodeInformation, Mapper} from "@volar/language-core"
import {firstNotNull} from "./generators"

export type SimpleRange = [
  startOffset: number,
  endOffset: number,
] | undefined

const LOCATION_FILTER: (data: CodeInformation) => boolean =
  data => !!data.verification

export type RangeTransform = (
  startOffset: number,
  endOffset: number,
) => SimpleRange

type OffsetTransform = (offset: number) => number | undefined

export class ScriptMapper {
  constructor(
    readonly mapper: Mapper,
    readonly tsShift: number,
  ) {
  }

  toSourceRange: RangeTransform = (startOffset, endOffset) =>
    this.#toRange(startOffset, endOffset, this.#toSourceOffset)

  toGeneratedRange: RangeTransform = (startOffset, endOffset) =>
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

  #toSourceOffset: OffsetTransform = offset =>
    firstNotNull(
      this.mapper.toSourceLocation(offset - this.tsShift, LOCATION_FILTER),
      ([sourceOffset]) => sourceOffset,
    )

  #toGeneratedOffset: OffsetTransform = offset =>
    firstNotNull(
      this.mapper.toGeneratedLocation(offset, LOCATION_FILTER),
      ([generatedOffset]) => generatedOffset + this.tsShift,
    )
}
