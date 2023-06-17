ARG PREFIX=/prefix
ARG OUTPUT=/output
ARG GRAPHVIZ_VERSION=8.0.4
ARG OPTIMIZATION_LEVEL=z
ARG BINARYEN_RELEASE_LINK="https://github.com/WebAssembly/binaryen/releases/download/version_113/binaryen-version_113-x86_64-linux.tar.gz"


FROM emscripten/emsdk:latest AS graphviz

ARG PREFIX
ARG GRAPHVIZ_VERSION
ARG OPTIMIZATION_LEVEL

ADD "https://gitlab.com/api/v4/projects/4207231/packages/generic/graphviz-releases/$GRAPHVIZ_VERSION/graphviz-$GRAPHVIZ_VERSION.tar.gz" ./graphviz.tar.gz

RUN mkdir -p graphviz && tar -zxf ./graphviz.tar.gz --strip-components 1 --directory graphviz
RUN cd graphviz && emconfigure ./configure --host=wasm32 --disable-ltdl --prefix="$PREFIX" --libdir="$PREFIX/lib" \
    CFLAGS="-O$OPTIMIZATION_LEVEL" CXXFLAGS="-O$OPTIMIZATION_LEVEL"
RUN cd graphviz/lib && emmake make -j install
RUN cd graphviz/plugin && emmake make -j install


FROM emscripten/emsdk:latest AS kraphviz

ARG PREFIX
ARG OUTPUT
ARG OPTIMIZATION_LEVEL

COPY --from=graphviz "$PREFIX" "$PREFIX"
COPY api.c .

RUN mkdir -p "${OUTPUT}"
RUN emcc -O$OPTIMIZATION_LEVEL api.c -o "${OUTPUT}/graphviz.wasm" \
    --no-entry -sEXPORTED_FUNCTIONS="['_malloc', '_free']" \
    -I"$PREFIX/include" -I"$PREFIX/include/graphviz" -L"$PREFIX/lib" -L"$PREFIX/lib/graphviz" \
    -lgvplugin_dot_layout -lgvplugin_neato_layout -lgvplugin_core -lgvc -lpathplan -lcgraph -lxdot -lcdt


FROM alpine:latest AS wasmopt

ARG BINARYEN_RELEASE_LINK
ARG OUTPUT
ARG OPTIMIZATION_LEVEL

RUN wget "$BINARYEN_RELEASE_LINK" -O binaryen.tar.gz
RUN mkdir -p binaryen && tar -zxf ./binaryen.tar.gz --strip-components 1 --directory binaryen

COPY --from=kraphviz "${OUTPUT}" "${OUTPUT}"
RUN binaryen/bin/wasm-opt -O$OPTIMIZATION_LEVEL "${OUTPUT}/graphviz.wasm" -o "${OUTPUT}/graphviz.wasm"


FROM scratch AS export

ARG OUTPUT

COPY --from=wasmopt "${OUTPUT}" /