(ns carapace.shell-test
  (:require
   [clojure.test :refer :all]
   [carapace.shell :refer :all]))

(deftest sh-test
  (let [s (with-out-str
            (sh ["ls"] {}))]
    (is (.contains s "src"))))
