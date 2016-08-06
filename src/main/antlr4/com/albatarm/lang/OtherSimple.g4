grammar OtherSimple;

root
    : word op number
    ;
    
op
    : MINUS
    | PLUS
    ;
    
word
    : Word
    ;
    
number
    : Number
    ;
    
Word : LETTER+;

Number : DIGIT+;
    
PLUS : '+';
MINUS : '-';
LETTER : [a-zA-Z];
DIGIT : [0-9];

WS
   : [ \r\n\t] + -> channel (HIDDEN)
   ;