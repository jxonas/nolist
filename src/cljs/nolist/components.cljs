(ns nolist.components
  (:require [re-com.core :as rc]
           [reagent.core :as r]))

(defn input [{:keys [value on-save placeholder width]
              :or {width "500px", value ""}}]
  (let [text (r/atom value)]
    (fn []
      [rc/input-text
       :model text
       :placeholder placeholder
       :on-change #(reset! text %)
       :change-on-blur? false
       :width width
       :attr {:on-key-up (fn [e]
                           (when (= 13 (.-keyCode e)) ; `Enter` key
                             (when on-save (on-save @text))
                             (reset! text "")))}])))
