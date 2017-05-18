
![BrainSaver Logo](https://github.com/ylegall/BrainSaver/blob/master/logo.png)

# BrainSaver
BrainSaver is a toy language that compiles to [BrainFuck](https://en.wikipedia.org/wiki/Brainfuck).
The compiler code is written in Kotlin and uses the [ANTLR4 parser generator](http://www.antlr.org/).
Inspiration is drawn from
[BrainFix](https://www.codeproject.com/Articles/558979/BrainFix-the-language-that-translates-to-fluent-Br) and the
[esolang wiki](https://esolangs.org/wiki/Brainfuck_algorithms).

## Features/Limitations
- [x] functions (always inlined)
- [x] static sized arrays
- [x] while loops
- [x] basic constant folding and constant branch elimination

## TODO
* foreach loops
* constant loop unrolling
* algorithms for wrapping implementations
* automatic memory management
