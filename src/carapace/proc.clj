(ns carapace.proc
  "Wrapper for creating Process objects"
  (:require
   [clojure.java.io :refer [file]]
   [com.palletops.api-builder.api :refer [defn-api]]
   [schema.core :as schema :refer [either eq optional-key]]))

(def ProcArgs
  {(optional-key :clear) schema/Bool
   (optional-key :env) {(either String clojure.lang.Named) String}
   (optional-key :directory) String
   (optional-key :redirect-error-stream) schema/Bool})

(def Proc
  {:in java.io.OutputStream
   :out java.io.InputStream
   (optional-key :err) java.io.InputStream
   :process Process})

(defn-api proc
  "Create a sub-process, returning a map describing the process.
  `command` is a sequence of strings with the command and arguments to
  run.
  The :clear option is a boolean controlling the clearing of inherited
  environment.  The :env option specifies a sequence of environment
  name, value pairs.
  The :directory option specifies the working directory to use.
  The :redirect-error-stream is a boolean option to control merging of
  stdout and stderr.

  The return map contains :in and :out keys, and an :err key unless
  :redirect-error-stream is set.  The :process key returns the Process
  object."
  {:sig [[[String] ProcArgs :- Proc]]}
  [command {:keys [clear directory env redirect-error-stream]}]
  (let [builder (ProcessBuilder. (into-array String command))
        environment (-> (.environment builder)
                        (cond-> clear .clear))]
    (doseq [[k v] env]
      (.put env k v))
    (when directory
      (.directory builder (file directory)))
    (when redirect-error-stream
      (.redirectErrorStream builder true))
    (let [process (.start builder)]
      (cond->
       {:out (.getInputStream process)
        :in  (.getOutputStream process)
        :process process}
       (not redirect-error-stream) (assoc :err (.getErrorStream process))))))

(defn-api wait-for
  "Wait for a process to end, returning the exit code."
  {:sig [[Proc :- schema/Int]]}
  [{:keys [process]}]
  (.waitFor ^Process process))

(defn-api exit-value
  "Return the exit code of the process.
  Throws IllegalThreadStateException if the process hasn't completed."
  {:sig [[Proc :- schema/Int]]}
  [{:keys [process]}]
  (.exitValue ^Process process))

(defn-api destroy
  "Destroy a process started by proc"
  {:sig [[Proc :- (eq nil)]]}
  [{:keys [process]}]
  (.destroy ^Process process))
