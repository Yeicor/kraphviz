FROM emscripten/emsdk:3.1.33 AS graphviz

ARG PREFIX=/prefix
ARG GRAPHVIZ_VERSION=8.0.4
ARG OPTIMIZATION_LEVEL=z

#ADD "https://github.com/libexpat/libexpat/releases/download/R_2_5_0/expat-2.5.0.tar.gz" ./expat.tar.gz
#
#RUN mkdir -p expat && tar -zxf ./expat.tar.gz --strip-components 1 --directory expat
#RUN cd expat && emconfigure ./configure --host=wasm32 --disable-shared --prefix="$PREFIX" --libdir="$PREFIX/lib" CFLAGS="-O$OPTIMIZATION_LEVEL" CXXFLAGS="-O$OPTIMIZATION_LEVEL"
#RUN cd expat/lib && emmake make all install

ADD "https://gitlab.com/api/v4/projects/4207231/packages/generic/graphviz-releases/$GRAPHVIZ_VERSION/graphviz-$GRAPHVIZ_VERSION.tar.gz" ./graphviz.tar.gz

RUN mkdir -p graphviz && tar -zxf ./graphviz.tar.gz --strip-components 1 --directory graphviz
RUN cd graphviz && emconfigure ./configure --host=wasm32 --disable-ltdl --prefix="$PREFIX" --libdir="$PREFIX/lib" CFLAGS="-O$OPTIMIZATION_LEVEL" CXXFLAGS="-O$OPTIMIZATION_LEVEL"
RUN cd graphviz/lib && emmake make -j install
RUN cd graphviz/plugin && emmake make -j install


FROM emscripten/emsdk:3.1.33 AS kraphviz

ARG PREFIX=/prefix
ARG OUTPUT=/output

COPY --from=graphviz "$PREFIX" "$PREFIX"
COPY api.c .

RUN mkdir -p "${OUTPUT}"
RUN emcc -O$OPTIMIZATION_LEVEL --closure=0 --no-entry -sFILESYSTEM=0 -sASSERTIONS=0 -sALLOW_MEMORY_GROWTH=1 -sENVIRONMENT=web -sEXPORT_KEEPALIVE=1 -sEXPORTED_FUNCTIONS="['_malloc', '_free']" -o "${OUTPUT}/graphviz.wasm" api.c -I"$PREFIX/include" -I"$PREFIX/include/graphviz" -L"$PREFIX/lib" -L"$PREFIX/lib/graphviz" -lgvplugin_dot_layout -lgvplugin_neato_layout -lgvplugin_core -lgvc -lpathplan -lcgraph -lxdot -lcdt # -lexpat


FROM scratch AS export

ARG OUTPUT=/output

COPY --from=kraphviz "${OUTPUT}" /