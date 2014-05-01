(ns carapace.stream
  "Stream processing for Process input and output streams."
  (:require
   [clojure.core.async :refer [<! alts! chan go-loop put! timeout]]
   [com.palletops.api-builder.api :refer [defn-api]]
   [schema.core :as schema :refer [either optional-key validate]]))

(def StreamOptions
  {(optional-key :period) schema/Int
   (optional-key :buffer-size) schema/Int})

(def Streamer
  {:streams clojure.lang.Atom
   :channel (schema/protocol clojure.core.async.impl.protocols/ReadPort)
   :buffer-size schema/Int
   :period schema/Int})

(def StreamMap
  {:in (either java.io.InputStream java.io.Reader)
   :out (either java.io.OutputStream java.io.Writer)
   :buffer schema/Any
   :buffer-size schema/Int
   :flush schema/Bool})

(defn-api streamer
  "Return a streamer, that copies input streams to output streams.
  Use `start` to start the streamer.
  Will use `:buffer-size` as the default buffer size when copying.
  Will poll every `:period` milliseconds."
  {:sig [[StreamOptions :- Streamer]]}
  [{:keys [buffer-size period]
    :or {buffer-size (* 16 1024)
         period 100}}]
  {:streams (atom [])
   :channel (chan)
   :period period
   :buffer-size buffer-size})

(defmulti stream-map
  "Return a stream map for the given inputs"
  (fn [{:keys [in out buffer buffer-size]} options]
    [(type in) (type out)]))

(defmethod stream-map [java.io.InputStream java.io.OutputStream]
  [{:keys [in out buffer buffer-size] :as m} options]
  (let [buffer-size (or (and buffer (count buffer))
                        buffer-size
                        (:buffer-size options))
        buffer (or buffer (byte-array buffer-size))]
    (assoc m :buffer-size buffer-size :buffer buffer)))

(defn- ^String encoding [opts]
  (or (:encoding opts) "UTF-8"))

(defmethod stream-map [java.io.InputStream java.io.Writer]
  [{:keys [in out buffer buffer-size] :as m} options]
  (let [in (java.io.InputStreamReader. in (encoding m))
        buffer-size (or (and buffer (count buffer))
                        buffer-size
                        (:buffer-size options))
        buffer (or buffer (char-array buffer-size))]
    (assoc m :in in :buffer-size buffer-size :buffer buffer)))

(defmethod stream-map [java.io.Reader java.io.OutputStream]
  [{:keys [in out buffer buffer-size] :as m} options]
  (let [out (java.io.OutputStreamWriter. out (encoding m))
        buffer-size (or (and buffer (count buffer))
                        buffer-size
                        (:buffer-size options))
        buffer (or buffer (char-array buffer-size))]
    (assoc m :out out :buffer-size buffer-size :buffer buffer)))

(defmulti poll
  (fn [{:keys [in out buffer buffer-size flush]}]
    [(type in) (type out)]))

(defmethod poll [java.io.InputStream java.io.OutputStream]
  [{:keys [in out buffer buffer-size flush]}]
  (when (pos? (.available in))
    (let [num-read (.read in buffer 0 buffer-size)]
      (.write out buffer 0 num-read)
      (when flush
        (.flush out))
      true)))

(defmethod poll [java.io.Reader java.io.Writer]
  [{:keys [in out buffer buffer-size flush]}]
  (when (.ready in)
    (let [num-read (.read in buffer 0 buffer-size)]
      (.write out buffer 0 num-read)
      (when flush
        (.flush out))
      true)))

(defn-api start
  "Start streaming"
  {:sig [[Streamer :- clojure.core.async.impl.protocols.ReadPort]]}
  [{:keys [channel period streams] :as streamer}]
  (go-loop []
    (if (reduce
         (fn [x stream-map]
           (or
            (try (poll stream-map)
                 (catch Exception e
                   (.printStackTrace e)))
            x))
         nil
         @streams)
      (recur)
      (let [v (alts! [channel (timeout period)])]
        (if-not (first v)
          (recur))))))

(defn-api stop
  "Stop streaming.  Returns true unless already stopped."
  {:sig [[Streamer :- schema/Bool]]}
  [{:keys [channel] :as streamer}]
  (put! channel :done))

(def StreamOptions
  {(optional-key :flush) schema/Bool
   (optional-key :buffer-size) schema/Int
   (optional-key :buffer) bytes
   (optional-key :encoding) String})

(defn-api stream
  "Stream the input stream `in` to `out` using any specified options.
  Return the stream-map."
  {:sig [[Streamer
          (either java.io.InputStream java.io.Reader)
          (either java.io.OutputStream java.io.Writer)
          StreamOptions
          :- StreamMap]]}
  [{:keys [buffer-size streams] :as streamer} in out options]
  (let [stream-map (validate
                    StreamMap
                    (stream-map (merge {:flush false} options {:in in :out out})
                                {:buffer-size buffer-size}))]
    (swap! streams conj stream-map)
    stream-map))

(defn-api un-stream
  "Stop streaming a stream-map."
  {:sig [[Streamer StreamMap :- Streamer]]}
  [{:keys [streams] :as streamer} stream-map]
  (poll stream-map)
  (swap! streams (fn [s] (remove #(= stream-map %) s)))
  streamer)
