(ns nolist.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]))

(defn listen
  [query-v]
  @(subscribe query-v))

(defn main-panel []
  [:h1 "Hello Chestnut!"])
