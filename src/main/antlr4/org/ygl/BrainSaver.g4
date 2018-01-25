// https://github.com/antlr/grammars-v4/blob/master/c/C.g4

grammar BrainSaver;

program
    : declList
    ;

declList
    :   (function | globalVariable | constant)+
    ;

globalVariable
    :   VAR lhs=Identifier '=' rhs=exp ';'
    ;

constant
    :   VAL lhs=Identifier '=' rhs=exp ';'
    ;

function
    :   FUNCTION name=Identifier '(' (params+=Identifier (',' params+=Identifier)*)? ')' body=functionBody
    ;

functionBody
    :   '{' statement* ret=returnStatement? '}'
    ;

statement
    :   assignmentStatement
    |   declarationStatement
    |   callStatement
    |   ifStatement
    |   whileStatement
    |   forStatement
    |   arrayInitStatement
    |   arrayWriteStatement
    ;

ifStatement
    :   IF '(' condition=exp ')' '{' trueStmts+=statement* '}' ( ELSE '{' falseStmts+=statement* '}' )?
    ;

whileStatement
    :   WHILE '(' condition=exp ')' '{' body+=statement* '}'
    ;

forStatement
    :   FOR '(' loopVar=Identifier IN start=atom '..' stop=atom (BY step=atom)? ')'
        '{' body+=statement* '}'
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

arrayInitStatement
    :   storage lhs=Identifier '=' ARRAY '(' arraySize=IntegerLiteral ')' ';'   # arrayConstructor
    |   storage lhs=Identifier '=' '[' items+=exp (',' items+=exp)* ']' ';'     # arrayLiteral
    ;

arrayWriteStatement
    :   array=Identifier '[' idx=exp ']' '=' rhs=exp ';'
    ;

exp
    : '(' parenExp=exp ')'                                      # parenExp
    | array=Identifier '[' idx=exp ']'                          # arrayReadExp
    |          op='!'                           right=exp       # notExp
    | left=exp op=('*'|'/'|'%')                 right=exp       # opExp
    | left=exp op=('+'|'-')                     right=exp       # opExp
    | left=exp op=('=='|'<'|'<='|'>'|'>='|'!=') right=exp       # opExp
    | condition=exp '?' trueExp=exp ':' falseExp=exp            # conditionalExp
    | left=exp op=('&&'|'||')                   right=exp       # opExp
    | funcName=Identifier '(' args=expList? ')'                 # callExp
    | atom                                                      # atomExp
    ;

expList
    :   exp (',' exp)*
    ;

storage
    :   (VAR|VAL)
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
IF      : 'if';
ELSE    : 'else';
WHILE   : 'while';
FOR     : 'for';
IN      : 'in';
BY      : 'by';
VAR     : 'var';
VAL     : 'val';

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
