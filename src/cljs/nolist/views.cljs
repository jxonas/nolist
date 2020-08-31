(ns nolist.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as r]
            [re-com.core :as rc]
            [nolist.components :as c]))

(defn listen
  [query-v]
  @(subscribe query-v))

(defn simple-button
  []
  (fn [& {:keys [icon on-click disabled?]}]
    [rc/md-icon-button
     :md-icon-name icon
     :on-click on-click
     :disabled? disabled?
     :size :smaller]))

(defn undo-redo-buttons
  []
  (fn []
    (let [undos? (listen [:undos?])
          redos? (listen [:redos?])]
      [rc/h-box
       :children [[simple-button
                   :icon "zmdi-undo"
                   :disabled? (not undos?)
                   :on-click #(dispatch [:undo])]
                  [simple-button
                   :icon "zmdi-redo"
                   :disabled? (not redos?)
                   :on-click #(dispatch [:redo])]]])))

(defn showing-selector []
  (fn []
    (let [showing (listen [:showing])
          n-completed (listen [:completed-count])
          focus (listen [:focus])
          a-fn (fn [filter-kw txt]
                 [rc/box
                  :size "none"
                  :child [rc/hyperlink
                          :label txt
                          :class (str "small showing-kw "
                                      (if (= filter-kw showing)
                                        "text-primary"
                                        "text-muted"))
                          :style {:text-decoration "none"
                                  :font-weight (when (= filter-kw showing) "bold")}
                          :on-click #(dispatch [:set-showing filter-kw])]])]
      [rc/h-box :children [[rc/box
                            :size "none"
                            :align :center
                            :child [rc/md-icon-button
                                    :md-icon-name (if focus
                                                    "zmdi-center-focus-strong"
                                                    "zmdi-center-focus-weak")
                                    :on-click #(dispatch [:toggle-focus])
                                    :size :smaller]]
                           [rc/gap :size "20px"]
                           [undo-redo-buttons]
                           [rc/gap :size "20px"]
                           (when-not (zero? n-completed)
                             [rc/hyperlink
                              :label "clear completed"
                              :on-click #(dispatch [:clear-completed])
                              :style {:text-decoration "none"}])
                           [rc/h-box
                            :gap "10px"
                            :size "auto"
                            :justify :end
                            :children [[a-fn :all "All"]
                                       [a-fn :active "Active"]
                                       [a-fn :done "Completed"]]]]])))

(defn task-input []
  [c/input
   {:on-save #(dispatch [:add-task %])
    :placeholder "What are we doing today?"}])

;; Task buttons

(defn task-button []
  (fn [& {:keys [icon class on-click]
         :or {class "btn-default"}
         :as args}]
    [rc/box
     :size "none"
     :child [rc/md-icon-button
             :md-icon-name icon
             :class (str "task-action " class)
             :on-click on-click
             :size :smaller]]))

(defn toggle-task-done [{:keys [id done]}]
  [task-button
   :icon "zmdi-check"
   :class (str "task-done-toggle " (if done "on " "off "))
   :on-click #(dispatch [:toggle-task-done id])])

(defn toggle-task-stared [{:keys [id stared]}]
  [task-button
   :icon (if stared "zmdi-star" "zmdi-star-outline")
   :class (str "task-stared-toggle " (if stared "on " "off "))
   :on-click #(dispatch [:toggle-task-stared id])])

(defn complete-and-reentry-task [{:keys [id title]}]
  [task-button
   :icon "zmdi-rotate-left"
   :on-click #(dispatch [:complete-and-reentry id])])

(defn delete-task [{:keys [id]}]
  [task-button
   :icon "zmdi-delete"
   :on-click #(dispatch [:delete-task id])])

(defn task-link []
  (let [show-link-modal (r/atom false)]
    (fn [{:keys [id link]}]
      [:div
       [rc/box
        :size "none"
        :child [rc/md-icon-button
                :md-icon-name "zmdi-link"
                :class "task-action"
                :on-click #(reset! show-link-modal true)
                :size :smaller]]
       (when @show-link-modal
         [rc/modal-panel
          :backdrop-on-click #(reset! show-link-modal false)
          :child [c/input
                  {:value (or link "")
                   :placeholder "task link..."
                   :on-save #(do (dispatch [:set-task-link id %])
                                 (reset! show-link-modal false))}]])])))

(defn todo-input [{:keys [title on-save on-stop]}]
  (let [val (r/atom title)
        stop #(do (reset! val "")
                  (when on-stop (on-stop)))
        save #(let [v (-> @val str clojure.string/trim)]
                (when (seq v) (on-save v))
                (stop))]
    (fn [props]
      [:input (merge props
                     {:type "text"
                      :value @val
                      :auto-focus true
                      :on-blur save
                      :style {:width "100%"}
                      :on-change #(reset! val (-> % .-target .-value))
                      :on-key-down #(case (.-which %)
                                      13 (save)
                                      27 (stop)
                                      nil)})])))

(defn task-title []
  (let [editing (r/atom false)]
    (fn [{:keys [id title done link]}]
      [rc/box
       :size "auto"
       :attr {:on-double-click #(reset! editing true)}
       :child (if @editing
                [todo-input
                 {:class "edit"
                  :title title
                  :on-save #(dispatch [:update-task-title id %])
                  :on-stop #(reset! editing false)}]
                [rc/label
                 :label (if-not (seq link) title [:a {:href link, :target "_blank"} title])
                 :class (when done "done")])])))

(defn task-item []
  (fn [{:keys [done] :as task} focus]
    [rc/h-box
     :class (when (:done task) "task-done")
     :gap "2px"
     :children [[toggle-task-stared task]
                [toggle-task-done task]
                [complete-and-reentry-task task]
                [task-title task]
                [task-link task]
                [delete-task task]]]))

(defn task-list []
  (fn []
    (let [tasks (listen [:visible-tasks])
          all-complete? (listen [:all-complete?])
          focus (listen [:focus])]
      [rc/v-box
       :attr {:id "task-list"}
       :children
       (for [{:keys [id title] :as task} tasks]
         ^{:key id}
         [task-item task focus])])))

(defn main-panel []
  (fn []
    [rc/v-box
     :height "100%"
     :align :center
     :padding "2px"
     :children [[rc/v-box
                 :children [[rc/gap :size "30px"]
                            [showing-selector]
                            [rc/gap :size "5px"]
                            [task-input]
                            [rc/gap :size "10px"]
                            [task-list]]]]]))
