// This is a generated file. Not intended for manual editing.
package com.intellij.dts.pp.test.impl.psi;


public interface TestTypes {

  com.intellij.psi.tree.IElementType QUOTE = new com.intellij.dts.pp.test.impl.TestElementType("QUOTE");
  com.intellij.psi.tree.IElementType SENTENCE = new com.intellij.dts.pp.test.impl.TestElementType("SENTENCE");

  com.intellij.psi.tree.IElementType COMMENT = new com.intellij.dts.pp.test.impl.TestTokenType("COMMENT");
  com.intellij.psi.tree.IElementType DOT = new com.intellij.dts.pp.test.impl.TestTokenType("DOT");
  com.intellij.psi.tree.IElementType PP_CHAR_LITERAL = new com.intellij.dts.pp.test.impl.TestTokenType("PP_CHAR_LITERAL");
  com.intellij.psi.tree.IElementType PP_COMMENT = new com.intellij.dts.pp.test.impl.TestTokenType("PP_COMMENT");
  com.intellij.psi.tree.IElementType PP_DIRECTIVE = new com.intellij.dts.pp.test.impl.TestTokenType("PP_DIRECTIVE");
  com.intellij.psi.tree.IElementType PP_FLOAT_LITERAL = new com.intellij.dts.pp.test.impl.TestTokenType("PP_FLOAT_LITERAL");
  com.intellij.psi.tree.IElementType PP_HEADER_NAME = new com.intellij.dts.pp.test.impl.TestTokenType("PP_HEADER_NAME");
  com.intellij.psi.tree.IElementType PP_IDENTIFIER = new com.intellij.dts.pp.test.impl.TestTokenType("PP_IDENTIFIER");
  com.intellij.psi.tree.IElementType PP_INACTIVE = new com.intellij.dts.pp.test.impl.TestTokenType("PP_INACTIVE");
  com.intellij.psi.tree.IElementType PP_INTEGER_LITERAL = new com.intellij.dts.pp.test.impl.TestTokenType("PP_INTEGER_LITERAL");
  com.intellij.psi.tree.IElementType PP_LINE_BREAK = new com.intellij.dts.pp.test.impl.TestTokenType("PP_LINE_BREAK");
  com.intellij.psi.tree.IElementType PP_MACRO_ARG = new com.intellij.dts.pp.test.impl.TestTokenType("PP_MACRO_ARG");
  com.intellij.psi.tree.IElementType PP_OPERATOR_OR_PUNCTUATOR = new com.intellij.dts.pp.test.impl.TestTokenType("PP_OPERATOR_OR_PUNCTUATOR");
  com.intellij.psi.tree.IElementType PP_STATEMENT = new com.intellij.dts.pp.test.impl.TestTokenType("PP_STATEMENT");
  com.intellij.psi.tree.IElementType PP_STATEMENT_END = new com.intellij.dts.pp.test.impl.TestTokenType("PP_STATEMENT_END");
  com.intellij.psi.tree.IElementType PP_STATEMENT_MARKER = new com.intellij.dts.pp.test.impl.TestTokenType("PP_STATEMENT_MARKER");
  com.intellij.psi.tree.IElementType PP_STRING_LITERAL = new com.intellij.dts.pp.test.impl.TestTokenType("PP_STRING_LITERAL");
  com.intellij.psi.tree.IElementType QUOTE_END = new com.intellij.dts.pp.test.impl.TestTokenType("QUOTE_END");
  com.intellij.psi.tree.IElementType QUOTE_START = new com.intellij.dts.pp.test.impl.TestTokenType("QUOTE_START");
  com.intellij.psi.tree.IElementType WORD = new com.intellij.dts.pp.test.impl.TestTokenType("WORD");

  class Factory {
    public static com.intellij.psi.PsiElement createElement(com.intellij.lang.ASTNode node) {
      com.intellij.psi.tree.IElementType type = node.getElementType();
      if (type == QUOTE) {
        return new com.intellij.dts.pp.test.impl.psi.TestQuoteImpl(node);
      }
      else if (type == SENTENCE) {
        return new com.intellij.dts.pp.test.impl.psi.TestSentenceImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
