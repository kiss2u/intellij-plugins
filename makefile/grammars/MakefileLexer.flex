package com.jetbrains.lang.makefile;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.jetbrains.lang.makefile.psi.MakefileTypes;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.jetbrains.lang.makefile.psi.MakefileTypes.*;

%%

%{
  private static char getRecipePrefix(String variable) {
    final var index = variable.indexOf('=');
    assert index != -1;

    final var value = variable.substring(index + 1, variable.length()).trim();
    if (value.startsWith("\\t")) {
      return '\t';
    }

    return value.charAt(0);
  }

  private char recipePrefix = '\t';

  private int lastState = YYINITIAL;

  private void setState(int state) {
      lastState = yystate();
      yybegin(state);
  }

  private void resetState() {
      yybegin(lastState);
  }

  public _MakefileLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _MakefileLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\n
SPACES=" "+
BACKSLASHCRLF="\\"(\n)(" "|\t)*
DOCCOMMENT="##"[^\n]*
COMMENT="#"[^\n]*
MULTILINECOMMENT="#"[^\n\\]*(("\\"\n[^\n\\]*|"\\"[^\n\\]*))+
FUNCTIONS=("error"|"warning"|"info"|"shell"|"subst"|"patsubst"|"strip"|"findstring"|
  "filter"|"filter-out"|"sort"|"word"|"wordlist"|"words"|"firstword"|"lastword"|"dir"|"notdir"|"suffix"|
  "basename"|"addsuffix"|"addprefix"|"join"|"wildcard"|"realpath"|"abspath"|"if"|"or"|"and"|
  "foreach"|"file"|"call"|"value"|"eval"|"origin"|"flavor"|"guile")
MACRO="@"[^@ \n]+"@"
ASSIGN=("="|":="|"::="|"?="|"!="|"+=")

CHARS = [0-9\p{L}.!\-?%@/_\[\]+~*\^&+<>] | (\\[\\:\(\)#])

RECIPEPREFIX=[\t ]*"\.RECIPEPREFIX"[\t ]*"="[\t ]*(\S)+


%state SQSTRING DQSTRING DEFINE LINE

%%

<SQSTRING> {
  "'"   { resetState(); return QUOTE; }
  "\""  { return CHARS; }
  "#"+  { return CHARS; }
  {EOL} { resetState(); return EOL; }
}

<DQSTRING> {
  "\""  { resetState(); return DOUBLEQUOTE; }
  "'"   { return CHARS; }
  "#"+  { return CHARS; }
  {EOL} { resetState(); return EOL; }
}

<DEFINE> {
  "endef"  { resetState(); return KEYWORD_ENDEF; }
  {CHARS}+ { return CHARS; }
  "\""     { return CHARS; }
  "'"      { return CHARS; }
  "#"+     { return CHARS; }
}

<YYINITIAL, LINE> {
  ^[ ]*{COMMENT}\n       { return COMMENT; }
  {DOCCOMMENT}           { return DOC_COMMENT; }
  {MULTILINECOMMENT}     { return COMMENT; }
  {COMMENT}              { return COMMENT; }
}

^{MACRO} { return MACRO; }

<YYINITIAL> {
  ^. {
    if (yytext().charAt(0) == recipePrefix) {
      return RECIPE_PREFIX;
    } else {
      setState(LINE);
      yypushback(1);
    }
  }

  ^{RECIPEPREFIX} {
    recipePrefix = getRecipePrefix(yytext().toString());

    setState(LINE);
    yypushback(yylength());
  }
}

<LINE> {
  {EOL}            { setState(YYINITIAL); return EOL; }
}

// it is not possible to match more then 1 tab at the time, because otherweise this rule would have a higher priority then RECIPE_PREFIX
\t                 { return WHITE_SPACE; }

{EOL}              { return EOL; }
{SPACES}           { return WHITE_SPACE; }
":"                { return COLON; }
","                { return COMMA; }
"`"                { return BACKTICK; }
{ASSIGN}           { return ASSIGN; }
{BACKSLASHCRLF}    { return SPLIT; }
"|"                { return PIPE; }
";"                { return SEMICOLON; }
"include"          { return KEYWORD_INCLUDE; }
"-include"         { return KEYWORD_INCLUDE; }
"sinclude"         { return KEYWORD_INCLUDE; }
"vpath"            { return KEYWORD_VPATH; }
^"define"          { setState(DEFINE); return KEYWORD_DEFINE; }
"undefine"         { return KEYWORD_UNDEFINE; }
"ifeq"             { return KEYWORD_IFEQ; }
"ifneq"            { return KEYWORD_IFNEQ; }
"ifdef"            { return KEYWORD_IFDEF; }
"ifndef"           { return KEYWORD_IFNDEF; }
"else"             { return KEYWORD_ELSE; }
"endif"            { return KEYWORD_ENDIF; }
"override"         { return KEYWORD_OVERRIDE; }
"export"           { return KEYWORD_EXPORT; }
"unexport"         { return KEYWORD_UNEXPORT; }
"private"          { return KEYWORD_PRIVATE; }
"$"                { return DOLLAR; }
{FUNCTIONS}        { return FUNCTION_NAME; }
"("                { return OPEN_PAREN; }
")"                { return CLOSE_PAREN; }
"{"                { return OPEN_CURLY; }
"}"                { return CLOSE_CURLY; }
\\\"               { return ESCAPED_DOUBLEQUOTE; }
"'"                { setState(SQSTRING); return QUOTE; }
"\""               { setState(DQSTRING); return DOUBLEQUOTE; }

{CHARS}+           { return CHARS; }
\\                 { return CHARS; }

[^] { return BAD_CHARACTER; }
