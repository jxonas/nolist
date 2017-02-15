(ns nolist.storage
  (:require [cljs.reader]
            [re-frame.core :refer [reg-cofx]]))

(def default-db
  {:tasks (sorted-map)
   :showing :all
   :focus false})

(def ls-key "nolist")

(defn tasks->local-storage
  "Puts tasks into localStorage"
  [tasks]
  (.setItem js/localStorage ls-key (str tasks)))

(reg-cofx
 :local-storage-tasks
 (fn [cofx _]
   "Read in tasks from localStorage and process into a map we can merge into app-db."
   (assoc cofx :local-storage-tasks
          (into (sorted-map)
                (some->> (.getItem js/localStorage ls-key)
                         (cljs.reader/read-string))))))
