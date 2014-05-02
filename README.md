# carapace

A library to run system commands from clojure.

## Installation

Add the library to you Leiningen `:dependencies`.

```clj
[com.palletops/carapace "0.1.0-SNAPSHOT"]
```

## Usage

The top level functions are `sh` and `sh-map`.  The `sh` function
executes a command specified as a sequence of strings.  It returns the
exit code of the process.  The process standard output is copied to
`*out*`, an the process standard error to `*err*`.  The `:in` option
can be used to pass an InputStream or a Reader, which will be fed to
the process' standard input.

```clj
(require '[carapace.shell :refer [sh]])
(sh ["ls" "-l"] {})
```

The `sh-map` is similar, but returns a map with `:out`, `:err` and
`:exit` keys, instead of copying the process output to `*out` and
`*err*`.

```clj
(require '[carapace.shell :refer [sh sh-map]])
(sh-map ["ls"] {})
```

## Proc Usage

There is also a lower level wrapper around `ProcessBuilder` and
`Process` that is very similar to [conch's][conch] `low-level` api.

```clj
(require '[carapace.proc :refer :all])

(def p (proc ["ls"] {}))
(wait-for p)
```

`proc` returns a map with `:in`, `:out` and `:err` streams, `:process`
with the `Process` object itself.

## Streams

The `sh` and `sh-map` functions are built on top of `proc` using a
`streamer`, which pumps content between streams.

The `streamer` is based on managing a sequence of stream maps, each
specifying an input stream and a function to call when input has been
read from the stream.

## Other libraries

You might want to look at [conch][conch] and [clojure.java.shell][clojure.java.shell].

## License

Copyright © 2014 Hugo Duncan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[conch]: https://github.com/raynes/conch "Conch"
[clojure.java.shell]: https://github.com/clojure/clojure/blob/master/src/clj/clojure/java/shell.clj "clojure.java.shell"
