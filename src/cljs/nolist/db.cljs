(ns nolist.db
  (:require [cljs.spec :as s]
            [re-frame.core :refer [reg-cofx]]))

(s/def ::date #(instance? js/Date %))

(s/def ::id int?)
(s/def ::title string?)
(s/def ::done boolean?)
(s/def ::stared boolean?)
(s/def ::created ::date)
(s/def ::updated ::date)

(s/def ::task
  (s/keys :req-un [::id ::title ::done]
          :opt-un [::created ::updated ::stared]))

(s/def ::tasks
  (s/map-of ::id ::task))

(s/def ::showing #{:all :active :done})
(s/def ::focus boolean?)

(s/def ::db (s/keys :req-un [::tasks ::showing ::focus]))

(def default-db
  {:tasks (sorted-map)
   :showing :all
   :focus false})

;; Local storage

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
