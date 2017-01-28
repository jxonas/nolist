(ns nolist.db
  (:require [cljs.spec :as s]))

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
