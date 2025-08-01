// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.lang.expr.parser

import com.intellij.javascript.web.html.XmlASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lang.javascript.JavascriptParserDefinition
import com.intellij.lang.javascript.parsing.JavaScriptParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import org.angular2.lang.expr.lexer.Angular2Lexer
import org.angular2.lang.expr.psi.impl.Angular2FileImpl
import org.angular2.lang.html.Angular2TemplateSyntax
import org.angular2.lang.html.parser.Angular2HtmlVarAttrTokenType

abstract class Angular2ExprParserDefinitionBase(
  private val templateSyntax: Angular2TemplateSyntax,
) : JavascriptParserDefinition() {
  override fun createLexer(project: Project?): Lexer {
    return Angular2Lexer(Angular2Lexer.RegularBinding(templateSyntax))
  }

  override fun createParser(project: Project?): PsiParser {
    return Angular2PsiParser()
  }

  override fun createJSParser(builder: PsiBuilder): JavaScriptParser {
    return Angular2Parser(templateSyntax.expressionLanguage, builder)
  }

  override fun createFile(viewProvider: FileViewProvider): PsiFile {
    return Angular2FileImpl(viewProvider, templateSyntax.expressionLanguage)
  }

  override fun getFileNodeType(): IFileElementType {
    return templateSyntax.expressionLanguageFileElementType
  }

  override fun createElement(node: ASTNode): PsiElement {
    return if (node.elementType === Angular2HtmlVarAttrTokenType.REFERENCE
               || node.elementType === Angular2HtmlVarAttrTokenType.LET) {
      XmlASTWrapperPsiElement(node)
    }
    else super.createElement(node)
  }

}