(ns carapace.shell
  "Execute processes from clojure"
  (:require
   [carapace.proc :as proc]
   [carapace.stream :as stream]))

(defonce default-streamer
  (delay
   (let [s (stream/streamer {})]
     (stream/start s)
     s)))

(defn sh
  "Execute a process, returning the exit code.
  The process executes `command`, a sequence of strings.
  Output goes to *out*."
  [command {:keys [in env clear
                   streamer buffer-size buffer
                   redirect-error-stream
                   flush]
            :as options
            :or {flush true
                 redirect-error-stream true}}]
  (let [p (proc/proc command (assoc (select-keys options [:env :clear])
                               :redirect-error-stream redirect-error-stream))
        s (or streamer @default-streamer)
        stream-maps [(if in
                       (stream/stream s in (:in p) {})
                       (.close (:in p)))
                     (stream/stream
                      s (:out p) *out*
                      (assoc (select-keys options [:buffer-size :buffer])
                        :flush flush))
                     (when-not redirect-error-stream
                       (stream/stream s (:err p) *err* {:flush flush}))]]
    (let [e (proc/wait-for p)]
      (doseq [sm stream-maps
              :when sm]
        (stream/un-stream s sm))
      e)))
