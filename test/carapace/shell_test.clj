(ns carapace.shell-test
  (:require
   [clojure.test :refer :all]
   [carapace.shell :refer :all]))

(deftest sh-test
  (let [s (with-out-str
            (sh ["ls"] {}))]
    (is (.contains s "src"))))

(deftest sh-map-test
  (let [{:keys [exit out err]} (sh-map ["ls"] {})]
    (is (zero? exit))
    (is (.contains out "src"))
    (is (= "" err))))
