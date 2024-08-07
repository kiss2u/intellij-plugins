package com.intellij.dts.lang

import com.intellij.dts.lang.psi.DtsTypes
import com.intellij.psi.tree.TokenSet

object DtsTokenSets {
  val comments = TokenSet.create(
    DtsTypes.COMMENT_EOL,
    DtsTypes.COMMENT_C,
    DtsTypes.PP_COMMENT,
  )

  val strings = TokenSet.create(
    DtsTypes.STRING_LITERAL,
    DtsTypes.CHAR_LITERAL,
    DtsTypes.PP_STRING_LITERAL,
    DtsTypes.PP_CHAR_LITERAL,
  )

  val numbers = TokenSet.create(
    DtsTypes.BYTE_LITERAL,
    DtsTypes.INT_LITERAL,
    DtsTypes.PP_INTEGER_LITERAL,
    DtsTypes.PP_FLOAT_LITERAL,
  )

  val compilerDirectives = TokenSet.create(
    DtsTypes.V1,
    DtsTypes.PLUGIN,
    DtsTypes.DELETE_NODE,
    DtsTypes.DELETE_PROP,
    DtsTypes.OMIT_NODE,
    DtsTypes.MEMRESERVE,
    DtsTypes.BITS,
    DtsTypes.INCLUDE,
    DtsTypes.PP_DIRECTIVE,
  )

  val includePath = TokenSet.create(
    DtsTypes.INCLUDE_PATH,
    DtsTypes.PP_HEADER_NAME,
  )

  val operators = TokenSet.create(
    DtsTypes.ADD,
    DtsTypes.SUB,
    DtsTypes.MUL,
    DtsTypes.DIV,
    DtsTypes.MOD,

    DtsTypes.AND,
    DtsTypes.OR,
    DtsTypes.XOR,
    DtsTypes.NOT,
    DtsTypes.LSH,
    DtsTypes.RSH,

    DtsTypes.L_AND,
    DtsTypes.L_OR,
    DtsTypes.L_NOT,

    DtsTypes.LES,
    DtsTypes.GRT,
    DtsTypes.LEQ,
    DtsTypes.GEQ,
    DtsTypes.EQ,
    DtsTypes.NEQ,
  )
}