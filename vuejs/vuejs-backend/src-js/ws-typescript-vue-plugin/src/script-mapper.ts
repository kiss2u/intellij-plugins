// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import type {Mapper} from "@volar/language-core"

export type SimpleRange = [
  startOffset: number,
  endOffset: number,
] | undefined

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
    transform: (offset: number) => number | undefined,
  ): SimpleRange => {
    const start = transform(startOffset)
    if (start === undefined) return undefined

    const end = transform(endOffset)
    if (end === undefined) return undefined

    return [start, end]
  }

  #toSourceOffset = (offset: number): number | undefined => {
    for (const [sourceOffset] of this.mapper.toSourceLocation(offset - this.tsShift)) {
      return sourceOffset
    }

    return undefined
  }

  #toGeneratedOffset = (offset: number): number | undefined => {
    for (const [generatedOffset] of this.mapper.toGeneratedLocation(offset)) {
      return generatedOffset + this.tsShift
    }

    return undefined
  }
}
