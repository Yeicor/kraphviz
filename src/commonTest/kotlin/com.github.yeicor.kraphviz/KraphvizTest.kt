package com.github.yeicor.kraphviz

import kotlin.test.Test

class KraphvizTest {
  @Test
  fun renderSimple() {
    val svg = Kraphviz.render("digraph {\n\ta -> b\n}")
    println(svg)
  }

  @Test
  fun renderComplex() {
    val svg =
        Kraphviz.render(
            """
      // EXAMPLE FROM https://graphviz.org/Gallery/gradient/cluster.html
      digraph G {
        bgcolor="#0000FF44:#FF000044" gradientangle=90
      	// FIXME: fontname="Helvetica,Arial,sans-serif"
      	//node [fontname="Helvetica,Arial,sans-serif"]
      	//edge [fontname="Helvetica,Arial,sans-serif"]

      	subgraph cluster_0 {
      		style=filled;
      		color=lightgrey;
      		fillcolor="darkgray:gold";
      		gradientangle=0
      		node [fillcolor="yellow:green" style=filled gradientangle=270] a0;
      		node [fillcolor="lightgreen:red"] a1;
      		node [fillcolor="lightskyblue:darkcyan"] a2;
      		node [fillcolor="cyan:lightslateblue"] a3;

      		a0 -> a1 -> a2 -> a3;
      		label = "process #1";
      	}

      	subgraph cluster_1 {
      		node [fillcolor="yellow:magenta" 
      			 style=filled gradientangle=270] b0;
      		node [fillcolor="violet:darkcyan"] b1;
      		node [fillcolor="peachpuff:red"] b2;
      		node [fillcolor="mediumpurple:purple"] b3;

      		b0 -> b1 -> b2 -> b3;
      		label = "process #2";
      		color=blue
      		fillcolor="darkgray:gold";
      		gradientangle=0
      		style=filled;
      	}
      
      	start -> a0;
      	start -> b0;
      	a1 -> b3;
      	b2 -> a3;
      	a3 -> a0;
      	a3 -> end;
      	b3 -> end;

      	start [shape=Mdiamond ,
      		fillcolor="pink:red",
      		gradientangle=90,
      		style=radial];
      	end [shape=Msquare,
      		fillcolor="lightyellow:orange",
      		style=radial,
      		gradientangle=90];
      }
    """
                .trimIndent())
    println(svg)
  }

  @Test
  fun renderFailure() {
    try {
      val svg = Kraphviz.render("digraphInvalid { a -> b }")
      println(svg)
      throw AssertionError("Expected render to fail")
    } catch (e: Throwable) {
      println("Received expected error: $e")
    }
  }
}
