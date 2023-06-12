// Thanks to https://github.com/mdaines/viz.js for this code and Dockerfile

#include <gvc.h>
#include <emscripten.h>

extern int Y_invert;
extern int Nop;

extern gvplugin_library_t gvplugin_core_LTX_library;
extern gvplugin_library_t gvplugin_dot_layout_LTX_library;
//extern gvplugin_library_t gvplugin_neato_layout_LTX_library;

lt_symlist_t lt_preloaded_symbols[] = {
  { "gvplugin_core_LTX_library", &gvplugin_core_LTX_library},
  { "gvplugin_dot_layout_LTX_library", &gvplugin_dot_layout_LTX_library},
//  { "gvplugin_neato_layout_LTX_library", &gvplugin_neato_layout_LTX_library},
  { 0, 0 }
};

EMSCRIPTEN_KEEPALIVE
void viz_set_yinvert(int invert) {
  Y_invert = invert;
}

EMSCRIPTEN_KEEPALIVE
void viz_set_nop(int value) {
  Nop = value;
}

EMSCRIPTEN_KEEPALIVE
char *render_dot_svg(char *string) {
  GVC_t *context = NULL;
  Agraph_t *graph = NULL;
  Agraph_t *other_graph = NULL;
  char *data = NULL;
  unsigned int length = 0;
  int layout_error = 0;
  int render_error = 0;
  const char *engine = "dot";
  const char *format = "svg";

  // Initialize context

  context = gvContextPlugins(lt_preloaded_symbols, 0);

  // Reset errors

  agseterr(AGWARN);
  agreseterrors();

  // Try to read one graph

  graph = agmemread(string);

  if (!graph) {
    agerrorf("no valid graph in input\n");
  }

  // Consume the rest of the input

  do {
    other_graph = agmemread(NULL);
    if (other_graph) {
      agclose(other_graph);
    }
  } while (other_graph);

  // Layout (if there is a graph)

  if (graph) {
    layout_error = gvLayout(context, graph, engine);
  }

  // Render (if there is a graph and layout was successful)

  if (graph && !layout_error) {
    render_error = gvRenderData(context, graph, format, &data, &length);

    if (render_error) {
      gvFreeRenderData(data);
      data = NULL;
    }
  }

  // Free the layout, graph, and context

  if (graph) {
    gvFreeLayout(context, graph);
  }

  if (graph) {
    agclose(graph);
  }

  gvFinalize(context);
  gvFreeContext(context);

  // Return the result (if successful, the rendered graph; otherwise, null)

  return data;
}