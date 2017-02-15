(ns nolist.views
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [nolist.components :as c]
            [clojure.string :as str]))

;; Utilities

(defn listen
  [query-v]
  @(subscribe query-v))

;; Views

(defn toggle [status on off click]
  (fn [status on off click]
    [:span.task-button {:on-click click}
     (if status on off)]))

(defn task-filter [showing key label]
  [:li {:class (when (= showing key) "selected")}
   [:a {:on-click #(dispatch [:set-showing key])}
    label]])

(defn filter-panel []
  (let [focus (listen [:focus])
        showing (listen [:showing])]
    [:div#filter-panel
     [:table
      [:tbody
       [:tr
        [:td [toggle focus "\u25C9" "\u25CE" #(dispatch [:toggle-focus])]]
        [:td
         [:span {:on-click #(dispatch [:undo])} "\u21B6"]
         [:span {:on-click #(dispatch [:redo])} "\u21B7"]]
        [:td [:span {:on-click #(dispatch [:clear-completed])} "Clear completed"]]
        [:td [:ul#filter
              [task-filter showing :active "Active"]
              [task-filter showing :done "Done"]
              [task-filter showing :all "All"]]]]]]]))

(defn task-input []
  [:div#task-input
   [c/editable
    {:placeholder "What are we doing today?"
     :style {:width "100%"}
     :keypress-map {13 #(dispatch [:add-task %])}}]])

(defn task-item [{:keys [id title stared done]}]
  (let [editing (r/atom false)]
    (fn [{:keys [id title stared done]}]
      [:tr
       [:td
        [toggle stared "\u2605" "\u2606" #(dispatch [:toggle-task-stared id])]
        [toggle done "\u2611" "\u2610" #(dispatch [:toggle-task-done id])]
        [:span.task-button {:on-click #(dispatch [:complete-and-reentry id])} "\u21F4"]]
       [:td.absorbing-column {:class (when done "done")}
        (if @editing
          [c/editable {:value title
                       :keypress-map {13 #(do (dispatch [:update-task-title id %])
                                              (reset! editing false)
                                              %)}}]
          [:span {:on-double-click #(reset! editing true)} title])]
       [:td [:span {:on-click #(dispatch [:delete-task id])} "\u2715"]]])))

(defn task-list []
  (let [tasks (listen [:visible-tasks])]
    [:table#task-list
     [:tbody
      (for [task tasks]
        ^{:key (:id task)}
        [task-item task])]]))

(defn main-panel []
  (let [tasks (listen [:visible-tasks])]
    [:div#task-panel
     [filter-panel]
     [task-input]
     [task-list]]))
