grammar OtherSimple;

root
    : word op number instructions?
    ;
    
instructions
    : comma function (comma function)*
    ;
    
comma
    : COMMA
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
    
function
    : Play
    | Pause
    ;
    
Play
    : PLAY;
    
Pause
    : PAUSE;
    
Word : LETTER+;

Number : DIGIT+;

PLAY : 'play';
PAUSE : 'pause';
    
PLUS : '+';
MINUS : '-';
LETTER : [a-zA-Z];
DIGIT : [0-9];

COMMA : ',';

WS
   : [ \r\n\t] + -> channel (HIDDEN)
   ;