grammar Lox; 

// Lox Grammar adapted from https://craftinginterpreters.com/appendix-i.html

@parser::header
{
// DO NOT MODIFY - generated from Lox.g4
}

@lexer::header
{
// DO NOT MODIFY - generated from Lox.g4
}

program        : declaration* EOF ;

declaration    : varDeclStmt | funDeclStmt | statement ;

statement      : exprStmt | printStmt | block | returnStmt
                | forStmt | forOfStmt | forInStmt | ifStmt | whileStmt;

varDecl        : 'var' IDENTIFIER ('=' expression )? ;

varDeclStmt    :  varDecl ';' ;

exprStmt       : expression ';' ;

printStmt      : 'print' expression ';' ;

forStmt        : 'for' '(' (loopVar=varDeclStmt | exprStmt | ';' )
                            condition=expression? ';'
                            increment=expression? ')' body=statement ;

forOfStmt      : 'for' '(' elementVar=varDecl  'of' toIterate=variableExpr ')' body=statement ;

forInStmt      : 'for' '(' indexVar=varDecl 'in' toIterate=variableExpr ')' body=statement ;

ifStmt         : 'if' '(' condition=expression ')' then=statement
                            ( 'else' alt=statement )? ;

whileStmt      : 'while' '(' condition=expression ')' body=statement;

returnStmt     : 'return' expression? ';' ;

funDeclStmt    : 'fun' function ';'? ;

call           : primary callArguments* ;

callArguments  : '(' arguments? ')';

arguments      : expression ( ',' expression )* ;

function       : IDENTIFIER '(' parameters? ')' block ;

parameters     : IDENTIFIER ( ',' IDENTIFIER )* ;

block          : '{' declaration* '}' ;

expression     : assignment ;

assignment     : IDENTIFIER '=' assignment | logic_or | arrayAssignment ;

arrayAssignment: left=variableExpr '\u{1F449}' index=expression '\u{1F448}'
                            '=' right=assignment
                            | other=logic_or;

logic_or       : logic_and ( 'or' logic_and )* ;

logic_and      : equality ( 'and' equality )* ;

equality       : comparison ( ( '!=' | '==' ) comparison )* ;

comparison     : term ( ( '>' | '>=' | '<' | '<=' ) term )* ;

term           : factor ( ( '-' | '+' ) factor )* ;

factor         : unary ( ( '/' | '*' ) unary )* ;

unary          : ( '!' | '-' ) unary | call;

primary        : boolean | string | number | nil | array | '(' expression ')' | variableExpr | arrayExpr;

arrayExpr      : left=variableExpr '\u{1F449}' index=expression '\u{1F448}';

array          : '\u{1F449}' ( expression ( ', ' expression )* )? '\u{1F448}';

variableExpr   : IDENTIFIER;

boolean        : true | false ;

true           : 'true' ;

false          : 'false' ;

string         : STRING ;

STRING         : '"' (~["\\])* '"' ;

nil            : 'nil' ;

// Digit as lexer fragment (not a separate token)
fragment DIGIT : [0-9] ;

NUMBER         : DIGIT+ ('.' DIGIT+)? ;

number         : NUMBER ;

fragment ALPHA : [a-zA-Z_] ;

IDENTIFIER     : ALPHA ( ALPHA | DIGIT )* ;

// more... 
WS             : [ \t\r\n]+ -> skip ;

// Local Variables:
// eval: (add-hook 'after-save-hook (lambda () (if (fboundp 'lsp-workspace-root) (if-let ((workspace (car (gethash (lsp-workspace-root) (lsp-session-folder->servers (lsp-session)))))) (with-lsp-workspace workspace (lsp-notify "workspace/didChangeWatchedFiles" `((changes . [((type . ,(alist-get 'changed lsp--file-change-type)) (uri . ,(lsp--path-to-uri buffer-file-name)))]))))))) nil t)
// End:
