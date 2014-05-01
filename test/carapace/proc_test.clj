(ns carapace.proc-test
  (:require
   [clojure.test :refer :all]
   [carapace.proc :refer :all]))

(deftest proc-test
  (let [p (proc ["ls"] {})]
    (is (zero? (wait-for p)) "wait-for returns exit code")))
