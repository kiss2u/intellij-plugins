package org.intellij.prisma.lang.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.intellij.prisma.lang.lexer.PrismaLexer
import org.intellij.prisma.lang.psi.*
import org.intellij.prisma.lang.psi.impl.PrismaDocCommentImpl

class PrismaParserDefinition : ParserDefinition {
  override fun createLexer(project: Project?): Lexer = PrismaLexer()

  override fun createParser(project: Project?): PsiParser = PrismaParser()

  override fun getFileNodeType(): IFileElementType = PrismaFileElementType

  override fun getCommentTokens(): TokenSet = PRISMA_COMMENTS

  override fun getStringLiteralElements(): TokenSet = PRISMA_STRINGS

  override fun createElement(node: ASTNode): PsiElement {
    val type = node.elementType
    if (type == DOC_COMMENT) return PrismaDocCommentImpl(node)
    return PrismaElementTypes.Factory.createElement(node)
  }

  override fun createFile(viewProvider: FileViewProvider): PsiFile = PrismaFile(viewProvider)
}