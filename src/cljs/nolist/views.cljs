(ns nolist.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as r]
            [re-com.core :as rc]))

(defn showing-selector []
  (let [showing @(subscribe [:showing])
        focus @(subscribe [:focus])
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
                         [rc/h-box
                          :gap "10px"
                          :size "auto"
                          :justify :end
                          :children [[a-fn :all "All"]
                                     [a-fn :active "Active"]
                                     [a-fn :done "Completed"]]]]]))

(defn task-input []
  (let [text (r/atom "")]
    (fn []
      [rc/input-text
       :model text
       :placeholder "What are we doing today?"
       :on-change #(reset! text %)
       :change-on-blur? false
       :width "500px"
       :attr {:on-key-up (fn [e]
                           (when (= 13 (.-keyCode e)) ; `Enter` key
                             (dispatch [:add-task @text])
                             (reset! text "")))}])))

(defn toggle-task-done [id done]
  [rc/box
   :size "none"
   :child [rc/md-icon-button
           :md-icon-name "zmdi-check"
           :class (str "task-action task-done-toggle " (if done "on " "off "))
           :on-click #(dispatch [:toggle-task-done id])
           :size :smaller]])

(defn toggle-task-stared [id stared]
  [rc/box
   :size "none"
   :child [rc/md-icon-button
           :md-icon-name (if stared "zmdi-star" "zmdi-star-outline")
           :class (str "task-action task-stared-toggle " (if stared "on " "off "))
           :on-click #(dispatch [:toggle-task-stared id])
           :size :smaller]])

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
    (fn [id title done]
      [rc/box
       :size "auto"
       :child (if @editing
                [todo-input
                 {:class "edit"
                  :title title
                  :on-save #(dispatch [:update-task-title id %])
                  :on-stop #(reset! editing false)}]
                [rc/label
                 :label title
                 :attr {:on-double-click #(reset! editing true)}
                 :class (when done "done")])])))

(defn complete-and-reentry-task [id title]
  [rc/box
   :size "none"
   :child [rc/md-icon-button
           :md-icon-name "zmdi-rotate-left"
           :class "task-action"
           :on-click #(dispatch [:complete-and-reentry id title])
           :size :smaller]])

(defn delete-task [id]
  [rc/box
   :size "none"
   :child [rc/md-icon-button
           :md-icon-name "zmdi-delete"
           :class "task-action"
           :on-click #(dispatch [:delete-task id])
           :size :smaller]])

(defn task-item []
  (fn [{:keys [id title stared done]} focus]
    [rc/h-box
     :class (when done "task-done")
     :gap "2px"
     :children [(when-not focus [toggle-task-stared id stared])
                (when focus [toggle-task-done id done])
                (when focus [complete-and-reentry-task id title])
                [task-title id title done]
                [delete-task id]]]))

(defn task-list []
  (fn []
    (let [tasks @(subscribe [:visible-tasks])
          all-complete? @(subscribe [:all-complete?])
          focus @(subscribe [:focus])]
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
