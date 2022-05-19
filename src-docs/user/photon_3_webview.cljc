(ns user.photon-3-webview
  "Photon fullstack query/view composition with client/server transfer"
  (:require [datascript.core :as d]                         ; photon cljsbuild needs to see the vars, fixme
            [hyperfiddle.logger :as log]
            [hyperfiddle.photon :as p]
            [hyperfiddle.photon-dom :as dom]
            [hyperfiddle.rcf :refer [tests ! % with]]
            user devkit)
  (:import (hyperfiddle.photon Pending)))


(hyperfiddle.rcf/enable!)

#?(:clj (def conn (d/create-conn {:order/email {}})))
#?(:clj (d/transact! conn [{:order/email "alice@example.com"}
                           {:order/email "bob@example.com"}
                           {:order/email "charlie@example.com"}]))

(defn orders [db ?email]
  #?(:clj
     (sort
       (d/q '[:find [?email ...]
              :in $ ?needle :where
              [?e :order/email ?email]
              [(user/includes-str? ?email ?needle)]]
            db (or ?email "")))))

#?(:clj
   (tests
     (orders @conn "") := ["alice@example.com"
                           "bob@example.com"
                           "charlie@example.com"]
     (orders @conn "alice") := ["alice@example.com"]))

(p/def db)

(p/defn View [state]
  (dom/table
    (let [email (:email state)]
      (dom/for [x ~@(orders db email)]
        (dom/tr (dom/text x))))))

(def !db #?(:clj (atom @conn)))                             ; Photon cljsbuild unable to resolve !db
(def !state #?(:cljs (atom {:email ""})))

(p/defn App []
  (binding [dom/parent (dom/by-id "root")]
    (let [state (p/watch !state)]
      ~@(binding [db (p/watch !db)]
          ~@(View. state)))))

(def main #?(:cljs (p/client (p/main (log/info "starting")
                                     (log/info (pr-str 
                                                 (try (App.)
                                                   (catch Pending _ :pending))))))))

(comment
  #?(:clj (devkit/main :main `main))
  #?(:clj (d/transact conn [{:order/email "dan@example.com"}]))
  #?(:clj (d/transact conn [{:order/email "erin@example.com"}]))
  #?(:clj (d/transact conn [{:order/email "frank@example.com"}]))
  #?(:clj (reset! !db @conn))

  (shadow.cljs.devtools.api/repl :app)
  (type 1)
  (swap! !state assoc :email "bob")
  (swap! !state assoc :email "")

  #?(:clj (log/info "a"))

  )
