(ns nolist.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
 :showing
 (fn [db]
   (:showing db)))

(reg-sub
 :focus
 (fn [db _]
   (:focus db)))

(reg-sub
 :sorted-tasks
 (fn [db _]
   (:tasks db)))

(reg-sub
 :tasks
 :<- [:sorted-tasks]
 (fn [sorted-tasks _]
   (vals sorted-tasks)))

(reg-sub
 :visible-tasks
 :<- [:tasks]
 :<- [:showing]
 :<- [:focus]
 (fn [[tasks showing focus] _]
   (let [showing? (case showing
                    :active (complement :done)
                    :done :done
                    :all identity)
         filter-fn (if-not focus
                     showing?
                     (comp :stared showing?))]
     (filter filter-fn tasks))))

(reg-sub
 :all-complete?
 :<- [:tasks]
 (fn [tasks _]
   (seq tasks)))

(reg-sub
 :completed-count
 :<- [:tasks]
 (fn [tasks _]
   (count (filter :done tasks))))

(reg-sub
 :status-counts
 :<- [:tasks]
 :<- [:completed-count]
 (fn [[tasks completed] _]
   (let [all (count tasks)]
     {:all all
      :active (- all completed)
      :completed completed})))
