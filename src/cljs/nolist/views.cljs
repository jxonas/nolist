(ns nolist.views
  (:require goog.object
            [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [cljsjs.semantic-ui-react]
            [reagent.core :as r]))

;; Semantic UI React helpers

;; Easy handle to the top-level extern for semantic-ui-react
(def semantic-ui js/semanticUIReact)

(defn component
  "Get a component from sematic-ui-react:

    (component \"Button\")
    (component \"Menu\" \"Item\")"
  [k & ks]
  (if (seq ks)
    (apply goog.object/getValueByKeys semantic-ui k ks)
    (goog.object/get semantic-ui k)))

(def container         (component "Container"))
(def segment           (component "Segment"))
(def header            (component "Header"))
(def button            (component "Button"))
(def button-group      (component "Button" "Group"))
(def button-content    (component "Button" "Content"))
(def checkbox          (component "Checkbox"))
(def input             (component "Input"))
(def icon              (component "Icon"))
(def icon-group        (component "Icon" "Group"))
(def menu              (component "Menu"))
(def menu-item         (component "Menu" "Item"))
(def message           (component "Message"))
(def dropdown          (component "Dropdown"))
(def table             (component "Table"))
(def table-body        (component "Table" "Body"))
(def table-row         (component "Table" "Row"))
(def table-header-cell (component "Table" "HeaderCell"))
(def table-cell        (component "Table" "Cell"))
(def grid              (component "Grid"))
(def column            (component "Grid" "Column"))
(def row               (component "Grid" "Row"))

;; Utilities

(defn listen
  [query-v]
  @(subscribe query-v))

;; UI

(defn main-container []
  [:div.ui.main.container
   [:h1.ui.header "All tasks are made equal"]])

(defn footer []
  [:div.ui.inverted.footer.segment
   "This is the footer =)"])

(defn add-task-field []
  (let [value (r/atom "")]
    (fn []
      [:div
       [:> input
        {:placeholder "Add new task..."
         :value @value
         :fluid true
         :icon "plus"
         :onChange #(reset! value (-> %2 .-value js->clj))
         :on-key-press #(when (= 13 (.-charCode %))
                          (dispatch [:add-task (.. % -target -value)])
                          (reset! value ""))}]])))

(defn icon-button [name color on-click]
  [:a {:href "#" :on-click on-click}
   [:> icon {:name name :color color}]])

(defn toggle [state [on on-color] [off off-color] on-click]
  (fn [state [on on-color] [off off-color] on-click]
    [:a {:href "#" :on-click on-click}
     [:> icon {:name (if state on off) :color (if state on-color off-color)}]]))

(defn clone-task-button [{:keys [id]}]
  [:a {:href "#" :on-click #(dispatch [:complete-and-reentry id])}
   [:> icon {:name "recycle"}]])

(defn delete-task-button [{:keys [id]}]
  [:a {:href "#" :on-click #(dispatch [:delete-task id])}
   [:> icon {:name "remove"}]])


(defn task-title [{:keys [id title done]}]
  (let [editing (r/atom false)
        elt-id (str "task" id)]
    (fn [{:keys [id title done]}]
      [:> table-cell
       (let [edit-input [:> input
                         {:value (or title "")
                          :fluid true
                          :autoFocus true
                          :id elt-id
                          :onChange #(dispatch [:update-task-title id (-> %2 .-value js->clj)])
                          :onBlur #(reset! editing false)
                          :on-key-press #(when (= 13 (.-charCode %))
                                           (reset! editing false))}]]
         (if @editing
           edit-input
           [:span.task-title
            {:class (when done "done")
             :on-double-click #(reset! editing true)}
            title]))])))

(defn task-item [{:keys [id title stared done] :as task}]
  (fn [{:keys [id title stared done] :as task}]
    [:> table-row
     [:> table-cell {:collapsing true}
      [toggle stared ["star" "yellow"] ["empty star" nil] #(dispatch [:toggle-task-stared id])]
      [toggle done ["checkmark" "green"] ["circle thin" nil] #(dispatch [:toggle-task-done id])]
      [clone-task-button task]]
     [task-title task]
     [:> table-cell {:collapsing true} [delete-task-button task]]]))

(defn task-list []
  (fn []
    (let [tasks (listen [:visible-tasks])]
      [:> table
       {:compact false
        :basic "very"
        :unstackable true}
       [:> table-body
        (for [task tasks]
          ^{:key (:id task)}
          [task-item task])]])))

(defn showing-select-dropdown []
  (fn []
    (let [showing (listen [:showing])]
      [:> dropdown
       {:inline true
        :class "showing-select"
        :value showing
        :options [{:text "Active" :value :active}
                  {:text "All" :value :all}
                  {:text "Done" :value :done}]
        :on-change (fn [_ data]
                     (let [selected (keyword (js->clj (.-value data)))]
                       (dispatch [:set-showing selected])))}])))

(defn filters-panel []
  (fn []
    (let [focus         (listen [:focus])
          undo?         (listen [:undos?])
          redo?         (listen [:redos?])
          has-completed (listen [:has-completed])]
      [:> grid
       [:> row {:columns 4}
        [:> column {:width 2}
         [:> button-group {:size "mini"}
          (let [color (when focus "yellow")]
            [:> button
             {:labelPosition "left"
              :color color
              :label {:color color :content "filter"}
              :icon {:name "star"}
              :onClick #(dispatch [:toggle-focus])}])]]
        [:> column {:width 2}
         [:> button-group {:size "mini"}
          [:> button {:icon "arrow left" :disabled (not undo?) :onClick #(dispatch [:undo])}]
          [:> button {:icon "arrow right" :disabled (not redo?) :onClick #(dispatch [:redo])}]]]
        [:> column {:width 2}
         (when has-completed
           [:> button-group {:size "mini"}
            [:> button {:basic true :size "mini" :onClick #(dispatch [:clear-completed])} "clear completed"]])]
        [:> column {:floated "right" :textAlign "right"} [showing-select-dropdown]]]])))

(defn main-panel []
  (fn []
    [:> container {:class "main-container" :text true}
     [filters-panel]
     [:> segment {:stacked true}
      [add-task-field]
      [task-list]]]))
