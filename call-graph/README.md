JVM Dynamic Call Graph
=======================
This project uses Java instrumentation capabilities to captures the dynamic execution flow of a JVM program.

### Compile
The call-graph package is build with maven. Install maven and do:
 ```sh
 $ mvn compile && mvn package
 ```
This will produce a `target` directory with jar: `call-graph-0.1-SNAPSHOT-jar-with-dependencies.jar`

### How to use this library
Instrumenting all methods will produce huge call-graphs which are not necessarily helpful. Call graph supports restricting the set of classes
to be instrumented through `packages` argument.
```
-javaagent:call-graph-0.1-SNAPSHOT-jar-with-dependencies.jar="packages=java.nio;output=/tmp/call-graph.dot"
```
The example above will instrument all classes under the the `java.nio` packages (we use simple `startsWith()` condition to filter classes).

After you stop your application you will find an `output` file (e.g.: `/tmp/call-graph.dot`).
This file contains captured call stack in `graphviz` format.
It can be visualized using the tools like http://www.graphviz.org/ or can be converted to any format: `.svg`, `.png`, `.ps` by `dot` tool:
```sh
$ dot -Tpng /tmp/call-graph.dot > call-graph.png
```

[![Example](callgraph-oss.jpg)](callgraph-oss.jpg)

Arrows represent calls (between methods), labels (numbers) next to arrows stand for number of occurrences.
