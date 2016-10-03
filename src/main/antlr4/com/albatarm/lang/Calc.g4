grammar Calc;

equation
   : expression relop expression
   ;

expression
   : multiplyingExpression ((PLUS | MINUS) multiplyingExpression)*
   ;

multiplyingExpression
   : powExpression ((TIMES | DIV) powExpression)*
   ;

powExpression
   : atom (POW multiplyingExpression)?
   ;

atom
   : scientific
   | variable
   | LPAREN expression RPAREN
   | func
   ;

scientific
   : number (E number)?
   ;

func
   : funcname LPAREN expression RPAREN
   ;

funcname
   : COS
   | TAN
   | SIN
   | ACOS
   | ATAN
   | ASIN
   | LOG
   | LN
   ;

relop
   : EQ
   | GT
   | LT
   ;

number
   : Number
   ;

Number
   : MINUS? DIGIT + (POINT DIGIT +)?
   ;

variable
   : Variable
   ;


COS
   : 'cos'
   ;


SIN
   : 'sin'
   ;


TAN
   : 'tan'
   ;


ACOS
   : 'acos'
   ;


ASIN
   : 'asin'
   ;


ATAN
   : 'atan'
   ;


LN
   : 'ln'
   ;


LOG
   : 'log'
   ;
   
Variable
   : MINUS? LETTER (LETTER | DIGIT)*
   ;


LPAREN
   : '('
   ;


RPAREN
   : ')'
   ;


PLUS
   : '+'
   ;


MINUS
   : '-'
   ;


TIMES
   : '*'
   ;


DIV
   : '/'
   ;


GT
   : '>'
   ;


LT
   : '<'
   ;


EQ
   : '='
   ;


POINT
   : '.'
   ;


E
   : 'e' | 'E'
   ;


POW
   : '^'
   ;


LETTER
   : ('a' .. 'z') | ('A' .. 'Z')
   ;


DIGIT
   : ('0' .. '9')
   ;


WS
   : [ \r\n\t] + -> channel (HIDDEN)
   ;