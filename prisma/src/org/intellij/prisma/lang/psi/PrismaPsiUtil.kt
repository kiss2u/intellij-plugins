package org.intellij.prisma.lang.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.intellij.prisma.ide.schema.types.PrismaDatasourceProviderType

fun PrismaPathExpression.findTopmostPathParent(): PsiElement? {
  return PsiTreeUtil.skipParentsOfType(this, PrismaPathExpression::class.java)
}

val PrismaQualifiedReferenceElement.leftmostQualifier: PsiElement
  get() {
    var result: PsiElement = this
    while (result is PrismaQualifiedReferenceElement) {
      val child = result.qualifier
      if (child != null) {
        result = child
      }
      else {
        return result
      }
    }
    return result
  }


val PsiElement.isKeyword: Boolean
  get() = elementType in PRISMA_KEYWORDS

fun PsiElement.resolveDatasourceTypes(): Set<PrismaDatasourceProviderType> =
  (containingFile as? PrismaFile)?.metadata?.datasourceTypes ?: emptySet()

fun PsiElement?.skipWhitespacesForwardWithoutNewLines(): PsiElement? =
  PsiTreeUtil.skipMatching(this, { it.nextSibling }, { it is PsiWhiteSpace && !it.textContains('\n') })

fun PsiElement?.skipWhitespacesBackwardWithoutNewLines(): PsiElement? =
  PsiTreeUtil.skipMatching(this, { it.prevSibling }, { it is PsiWhiteSpace && !it.textContains('\n') })

fun isFieldExpression(function: PrismaFunctionCall): Boolean =
  function.pathExpression.resolve() is PrismaFieldDeclaration