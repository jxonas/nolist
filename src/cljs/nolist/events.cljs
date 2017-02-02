(ns nolist.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx after path trim-v debug]]
            [day8.re-frame.undo :as undo :refer [undoable]]
            [nolist.db :refer [default-db tasks->local-storage]]
            [cljs.spec :as s]))

;; Utilities

(defn get-next-id [tasks]
  ((fnil inc 0) (apply max (keys tasks))))

(defn now []
  (js/Date.))

;; Interceptors

(defn check-and-throw
  "throw an exception if db doesn't match the spec"
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw :nolist.db/db)))

;; this interceptor stores tasks into local storage
;; we attach it to each event handler which could update tasks
(def ->local-storage (after tasks->local-storage))

(def task-interceptors
  [check-spec-interceptor
   (path :tasks)
   (undoable)
   ->local-storage
   (when ^boolean js/goog.DEBUG debug)
   trim-v])

;; Event handlers

(reg-event-db
 :set-showing
 [check-spec-interceptor (path :showing) trim-v]
 (fn [old-kw [new-kw]]
   new-kw))

(reg-event-db
 :toggle-focus
 [check-spec-interceptor (path :focus) trim-v]
 (fn [old]
   (not old)))

(reg-event-db
 :add-task
 task-interceptors
 (fn [tasks [title]]
   (let [id (get-next-id tasks)]
     (assoc tasks id {:id id :title title :done false :created (now)}))))

(reg-event-db
 :toggle-task-done
 task-interceptors
 (fn [tasks [id]]
   (update-in tasks [id :done] not)))

(reg-event-db
 :toggle-task-stared
 task-interceptors
 (fn [tasks [id]]
   (update-in tasks [id :stared] not)))

(reg-event-db
 :set-task-url
 task-interceptors
 (fn [tasks [id url]]
   (assoc-in tasks [id :url] url)))

(reg-event-db
 :delete-task
 task-interceptors
 (fn [tasks [id]]
   (dissoc tasks id)))

(reg-event-db
 :update-task-title
 task-interceptors
 (fn [tasks [id title]]
   (assoc-in tasks [id :title] title)))

(reg-event-fx
 :complete-and-reentry
 (fn [{:keys [db]} [_ id title]]
   {:db (assoc-in db [:tasks id :done] true)
    :dispatch [:add-task title]}))

(reg-event-fx
 :initialize-db
 [(inject-cofx :local-storage-tasks)
  check-spec-interceptor]
 (fn [{:keys [db local-storage-tasks]} _]
   {:db (assoc default-db :tasks local-storage-tasks)}))

;; Undo/Redo

(day8.re-frame.undo/undo-config!
 {:harvest-fn  (fn [app-db] (:tasks @app-db))    ;; save just tasks!
  :reinstate-fn (fn [app-db tasks]
                  (tasks->local-storage tasks)
                  (swap! app-db assoc :tasks tasks))})
