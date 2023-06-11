package com.github.yeicor.kraphviz

import kotlin.test.Test

class KraphvizTest {
  @Test
  fun renderSimple() {
    val kraphviz = Kraphviz()
    val svg = kraphviz.render("digraph { a -> b }")
    println(svg)
  }
}
