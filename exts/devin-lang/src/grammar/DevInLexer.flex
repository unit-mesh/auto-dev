// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.sdk.language;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import cc.unitmesh.language.psi.DevInTypes;
import com.intellij.psi.TokenType;

%%

%class DevInLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

CRLF=\R
WHITE_SPACE=[\ \n\t\f]
// $ variable
STRING=\"([^\\\"\r\n]|\\[^\r\n])*\"?
VARIABLE="$" {STRING}

%state WAITING_VALUE

%%
<YYINITIAL> {
    {STRING}           { return DevInTypes.STRING; }
    {VARIABLE}         { return DevInTypes.VARIABLE; }
}

({CRLF}|{WHITE_SPACE})+                                     { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }

[^]                                                         { return TokenType.BAD_CHARACTER; }
