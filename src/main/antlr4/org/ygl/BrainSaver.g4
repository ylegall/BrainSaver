// https://github.com/antlr/grammars-v4/blob/master/c/C.g4

grammar BrainSaver;


program
    : declList
    ;

declList
    :   (function | globalVariable | constant)+
    ;

constant
    : 'const' Identifier '=' rhs=exp ';'
    ;

globalVariable
    :   storage Identifier '=' rhs=exp ';'
    ;

storage
    :   'var' #varStorage
    |   'val' #valStorage
    ;

function
    :   FUNCTION name=Identifier params=parameterList body=functionBody
    ;

parameterList
    :   '(' identifierList? ')'
    ;

identifierList
    :   Identifier (',' Identifier)*
    ;

functionBody
    :   '{' statement* ret=returnStatement? '}'
    ;

statementList
    :   statement (statement)*
    ;

statement
    :   printStatement
    |   readStatement
    |   declarationStatement
    |   assignmentStatement
    |   callStatement
    |   ifStatement
    |   whileStatement
    |   forStatement
    |   arrayInitStatement
    |   arrayWriteStatement
    |   debugStatement
    ;

debugStatement
    :   'debug' '(' idList=identifierList ')' ';'
    ;

ifStatement
    :   IF '(' condition=exp ')' '{' trueStatements=statementList '}' ( ELSE '{' falseStatements=statementList '}' )?
    ;

whileStatement
    :   WHILE '(' condition=exp ')' '{' body=statementList '}'
    ;

forStatement
    :   FOR '(' loopVar=Identifier IN start=atom '..' stop=atom (BY step=atom)? ')'
        '{' body=statementList '}'
    ;

declarationStatement
    :   storage lhs=Identifier '=' rhs=exp ';'
    ;

assignmentStatement
    :   lhs=Identifier op=assignmentOperator rhs=exp ';'
    ;

assignmentOperator
    :   '=' | '*=' | '/=' | '%=' | '+=' | '-='
    ;

callStatement
    :   funcName=Identifier '(' args=expList? ')' ';'
    ;

returnStatement
    :   RETURN exp? ';'
    ;

// TODO: remove
readStatement
    :   (rd=READ|rdint=READINT) '(' Identifier ')' ';'
    ;

// TODO remove
printStatement
    :   PRINT '(' exp ')' ';'
    ;

// TODO: allow array size to be constant expression
arrayInitStatement
    :   lhs=Identifier '=' ARRAY '(' arraySize=IntegerLiteral ')' ';'   # arrayConstructor
    |   lhs=Identifier '=' '[' contents=integerList ']' ';'             # arrayLiteral
    ;

arrayWriteStatement
    :   array=Identifier '[' idx=exp ']' '=' rhs=exp ';'
    ;

integerList
    :   IntegerLiteral (',' IntegerLiteral)*
    ;

exp
    : '(' parenExp=exp ')'                                      # parenExp
    | array=Identifier '[' idx=exp ']'                          # arrayReadExp
    | left=exp op=('*'|'/'|'%')                 right=exp       # opExp
    | left=exp op=('+'|'-')                     right=exp       # opExp
    | left=exp op=('=='|'<'|'<='|'>'|'>='|'!=') right=exp       # opExp
    | left=exp op=('&&'|'||')                   right=exp       # opExp
    |          op='!'                           right=exp       # notExp
    | funcName=Identifier '(' args=expList? ')'                 # callExp
    | atom                                                      # atomExp
    ;

expList
    :   exp (',' exp)*
    ;

atom
    :   Identifier      # atomId
    |   StringLiteral   # atomStr
    |   IntegerLiteral  # atomInt
    ;

// keywords

ARRAY   : 'array';
FUNCTION: 'fn';
RETURN  : 'return';
PRINT   : 'print';
READ    : 'read';
READINT : 'readInt';
IF      : 'if';
ELSE    : 'else';
WHILE   : 'while';
FOR     : 'for';
IN      : 'in';
BY      : 'by';

// lexer rules

Identifier
    :   IdentifierNondigit
        (   IdentifierNondigit
        |   Digit
        )*
    ;

IntegerLiteral
    :   '-'? NonzeroDigit Digit*
    |   '0'
    ;

StringLiteral
    :   '"' SCharSequence? '"'
    ;

fragment
SCharSequence
    :   SChar+
    ;

fragment
SChar
    :   ~["\\\r\n]
    |   EscapeSequence
    |   '\\\n'   // Added line
    |   '\\\r\n' // Added line
    ;

fragment
EscapeSequence
    :   SimpleEscapeSequence
    ;

fragment
SimpleEscapeSequence
    :   '\\' ['"?abfnrtv\\]
    ;

fragment
IdentifierNondigit
    :   Nondigit
    ;

fragment
Nondigit
    :   [a-zA-Z_]
    ;

fragment
NonzeroDigit
    :   [1-9]
    ;

fragment
Digit
    :   [0-9]
    ;

Whitespace
    :   [ \t]+
        -> skip
    ;

Newline
    :   (   '\r' '\n'?
        |   '\n'
        )
        -> skip
    ;

BlockComment
    :   '/*' .*? '*/'
        -> skip
    ;

LineComment
    :   '//' ~[\r\n]*
        -> skip
    ;