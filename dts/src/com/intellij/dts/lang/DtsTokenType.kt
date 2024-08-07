package com.intellij.dts.lang

import com.intellij.dts.lang.psi.DtsTypes
import com.intellij.psi.tree.IElementType

class DtsTokenType(debugName: String) : IElementType(debugName, DtsLanguage) {
  override fun toString(): String {
    return when (this) {
      DtsTypes.LABEL -> "label"
      DtsTypes.NAME -> "name"
      DtsTypes.INT_LITERAL -> "integer"
      DtsTypes.BYTE_LITERAL -> "byte"
      DtsTypes.STRING_LITERAL -> "string"
      DtsTypes.CHAR_LITERAL -> "char"
      DtsTypes.PATH -> "path"

      DtsTypes.SEMICOLON -> ";"
      DtsTypes.ASSIGN -> "="
      DtsTypes.COMMA -> ","
      DtsTypes.SLASH -> "/"
      DtsTypes.HANDLE -> "&"

      // compiler directives
      DtsTypes.V1 -> "/dts-v1/"
      DtsTypes.PLUGIN -> "/plugin/"
      DtsTypes.INCLUDE -> "/include/"
      DtsTypes.MEMRESERVE -> "/memreserve/"
      DtsTypes.DELETE_NODE -> "/delete-node/"
      DtsTypes.DELETE_PROP -> "/delete-property/"
      DtsTypes.OMIT_NODE -> "/omit-if-no-ref/"
      DtsTypes.BITS -> "/bits/"

      DtsTypes.INCLUDE_PATH -> "include path"

      // braces
      DtsTypes.LBRACE -> "{"
      DtsTypes.RBRACE -> "}"
      DtsTypes.LPAREN -> "("
      DtsTypes.RPAREN -> ")"
      DtsTypes.LBRACKET -> "["
      DtsTypes.RBRACKET -> "]"
      DtsTypes.LANGL -> "<"
      DtsTypes.RANGL -> ">"

      // expressions
      DtsTypes.ADD -> "+"
      DtsTypes.SUB -> "-"
      DtsTypes.MUL -> "*"
      DtsTypes.DIV -> "/"
      DtsTypes.MOD -> "%"
      DtsTypes.AND -> "&"

      DtsTypes.OR -> "|"
      DtsTypes.XOR -> "^"
      DtsTypes.NOT -> "~"
      DtsTypes.LSH -> "<<"
      DtsTypes.RSH -> ">>"

      DtsTypes.L_AND -> "&&"
      DtsTypes.L_OR -> "||"
      DtsTypes.L_NOT -> "!"

      DtsTypes.LES -> "<"
      DtsTypes.GRT -> ">"
      DtsTypes.LEQ -> "<="
      DtsTypes.GEQ -> ">="
      DtsTypes.EQ -> "=="
      DtsTypes.NEQ -> "!="
      DtsTypes.COLON -> ":"
      DtsTypes.TERNARY -> "?"

      // c preprocessor
      DtsTypes.PP_DIRECTIVE -> "directive"
      DtsTypes.PP_IDENTIFIER -> "identifier"
      DtsTypes.PP_HEADER_NAME -> "header"
      DtsTypes.PP_OPERATOR_OR_PUNCTUATOR -> "operator or punctuator"
      DtsTypes.PP_INTEGER_LITERAL -> "integer"
      DtsTypes.PP_CHAR_LITERAL -> "char"
      DtsTypes.PP_FLOAT_LITERAL -> "float"
      DtsTypes.PP_STRING_LITERAL -> "string"

      else -> super.toString()
    }
  }
}