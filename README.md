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

## License

Copyright © 2014 Hugo Duncan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
