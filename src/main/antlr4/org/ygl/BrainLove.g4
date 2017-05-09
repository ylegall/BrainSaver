// https://github.com/antlr/grammars-v4/blob/master/c/C.g4

grammar BrainLove;


program
    : functionList
    ;

functionList
    :   function+
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
    :   '{' statement* '}'
    ;

statementList
    :   statement (statement)*
    ;

// TODO
statement
    :   returnStatement
    |   printStatement
    |   readStatement
    |   assignmentStatement
    |   callStatement
    |   ifStatement
    |   whileStatement
    ;

ifStatement
    :   IF '(' condition=exp ')' '{' trueStatements=statementList '}' ( ELSE '{' falseStatements=statementList '}' )?
    ;

whileStatement
    :   WHILE '(' condition=exp ')' '{' body=statementList '}'
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

readStatement
    :   (rd=READ|rdint=READINT) '(' Identifier ')' ';'
    ;

// TODO
printStatement
    :   PRINT '(' exp ')' ';'
    ;

exp
    : '(' parenExp=exp ')'                                      # parenExp
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

FUNCTION: 'fn';
RETURN  : 'return';
PRINT   : 'print';
READ    : 'read';
READINT : 'readInt';
IF      : 'if';
ELSE    : 'else';
WHILE   : 'while';

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