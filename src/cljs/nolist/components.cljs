(ns nolist.components
  (:require [reagent.core :as r]))

;; Auxiliary components

(defn- auto-focus-input [{:keys [auto-focus] :as props}]
  (r/create-class
   {:displayName "auto-focus-input"
    :component-did-mount (fn [component]
                           (when auto-focus
                             (.focus (r/dom-node component))))
    :reagent-render (fn [props]
                      [:input (dissoc props :auto-focus)])}))

;; Actual components

(defn editable [{:keys [keypress-map type on-change value]
                 :or [type "text", keypress-map {}] :as props}]
  (let [text (r/atom (or value ""))]
    (fn [{:keys [keypress-map type on-change value]
         :or [type "text", keypress-map {}] :as props}]
      [auto-focus-input
       (merge (apply dissoc props [:keypress-map :on-change])
              {:value @text
               :on-change #(cond->> (.. % -target -value)
                             on-change on-change
                             :else (reset! text))
               :on-key-press #(when-let [action (keypress-map (.-charCode %))]
                                (reset! text (action @text)))})])))
