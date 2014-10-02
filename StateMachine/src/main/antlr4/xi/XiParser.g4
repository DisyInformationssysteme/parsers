parser grammar XiParser;
options { tokenVocab = XiLexer; }

file : (STRING | BOOL | INT | RETURN | WHILE | IF | ELSE | LENGTH | BREAK | TRUE | FALSE | ID | INTEGER |
 LBRACKET | RBRACKET | COLON | EQUAL | LPAREN | RPAREN | COMMA | SEMICOLON | LBRACE | RBRACE | STAR | 
 SLASH | PERCENT | PLUS | DASH | LANGLE | RANGLE | BANG | AMPERSAND | PIPE)*;
