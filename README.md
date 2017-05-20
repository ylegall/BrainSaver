
![BrainSaver Logo](https://github.com/ylegall/BrainSaver/blob/master/logo.png)

# BrainSaver
[![Build Status](https://travis-ci.org/ylegall/BrainSaver.svg?branch=master)](https://travis-ci.org/ylegall/BrainSaver)
[![GPL Licence](https://badges.frapsoft.com/os/gpl/gpl.svg?v=103)](https://opensource.org/licenses/GPL-3.0/)

BrainSaver is a toy language that compiles to [BrainFuck](https://en.wikipedia.org/wiki/Brainfuck).
The compiler code is written in Kotlin and uses the [ANTLR4 parser generator](http://www.antlr.org/).
Inspiration is drawn from
[BrainFix](https://www.codeproject.com/Articles/558979/BrainFix-the-language-that-translates-to-fluent-Br) and the
[esolang wiki](https://esolangs.org/wiki/Brainfuck_algorithms).

## Features/Limitations
- [x] functions (always inlined)
- [x] static sized arrays
- [x] while loops, for loops
- [x] basic constant folding and constant branch elimination

## TODO
* algorithms for wrapping implementations
* automatic memory management
