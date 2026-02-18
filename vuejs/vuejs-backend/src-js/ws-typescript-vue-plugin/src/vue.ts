// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import type ts from "typescript/lib/tsserverlibrary"

export function isVueFile(
  fileName: string,
): boolean {
  return fileName.endsWith(".vue")
}
