// https://github.com/antlr/grammars-v4/blob/master/c/C.g4

grammar BrainLove;


tokens { PRINT, RETURN, FUNCTION }

program
    : functionList
    ;

functionList
    :   function
    |   functionList function
    ;

function
    :   FUNCTION name=Identifier params=parameterList body=functionBody
    ;

parameterList
    :   '(' identifierList? ')'
    ;

identifierList
    :   Identifier
    |   identifierList ',' Identifier
    ;

functionBody
    :   '{' statement* '}'
    ;

// TODO
statement
    :   returnStatement
    |   printStatement
    |   assignmentStatement
    ;

assignmentStatement
    :   lhs=Identifier op=assignmentOperator rhs=exp ';'
    ;

assignmentOperator
    :   '=' | '*=' | '/=' | '%=' | '+=' | '-='
    ;

returnStatement
    :   RETURN exp? ';'
    ;

// TODO
printStatement
    :   PRINT '(' exp ')' ';'
    ;

exp : '(' parenExp=exp ')'                          # parenExp
     | left=exp op=('*' | '/' | '%') right=exp      # opExp
     | left=exp op=('+' | '-')       right=exp      # opExp
     | atom                                         # atomExp
     ;

atom
    :   Identifier      # atomId
    |   StringLiteral   # atomStr
    |   IntegerLiteral  # atomInt
    ;

// keywords

FUNCTION: 'fn';
RETURN  : 'return';
PRINT   : 'print';

// lexer rules

Identifier
    :   IdentifierNondigit
        (   IdentifierNondigit
        |   Digit
        )*
    ;

IntegerLiteral
    :   '-'? NonzeroDigit Digit*
    ;

StringLiteral
    :   '"' chars=SCharSequence? '"'
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