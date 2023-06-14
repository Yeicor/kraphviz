# Kraphviz

Graphviz for Kotlin Multiplatform.

It works on all kotlin targets because the original Graphviz program is:

- Compiled to WebAssembly (thanks to [this](https://github.com/mdaines/viz.js/) project), which is then...
- Interpreted using kotlin common code (thanks to [this](https://github.com/Yeicor/ktmpwasm) project).

## [Yet Another Graphviz Editor](https://github.com/Yeicor/yage)

This is a demo project showing how to use this library to create a Graphviz editor using [KorGE](https://korge.org/).
