(ns app.views
  (:require [app.state :refer [app-state]]
            [app.hiccup :refer [eval-hiccup]]
            [app.parsing :refer [build-notes parse-music-input]]
            [reagent.core :as r]
            [hickory.core :as h]
            [clojure.string :as str]))


(defn header []
  [:div
   [:h1 "Display music with Verovio"]
   [:h3 "Enter some music"]
   [:p.explanation "This is incomplete in so far, as meter and beaming is not implemeted yet."]
   [:p "Input a duration first (1 for whole note, 2 for half note etc.); dotting a value is possible.
   Then type note names (c, d, e etc.). For accidentals use: '#' (sharp), '-' (flat), 'n' (natural), 'x' (double sharp) and '--' (double flat).
  c … b is middle octave, C … B lower, cc … bb upper. Type 'r' for a rest, _ (underscore) for a tie."]
   [:p.explanation "Try: Select one flat from the key menu. Type '8 g f 4 f_ 2f 4 r 8 a b cc# dd ee ff 4 ee 8 dd dd _ 2. dd ' (You can leave out the spaces)."]])

(defn key-input []
  (fn []
    [:div
     [:label {:for "key"} "Key: "]
     [:select#key {:on-change #(swap! app-state
                                      assoc :key.sig
                                      (str (-> % .-target .-value)))}
       (for [[choice acc] [["-" "0"]
                           ["#" "1s"]
                           ["##" "2s"]
                           ["###" "3s"]
                           ["b" "1f"]
                           ["bb" "2f"]
                           ["bbb" "3f"]]]
         [:option {:value acc :key choice} choice])]]))

;; Verovio seems to use a subset of MEI, see:
;; https://www.verovio.org/structure.xhtml
;; check this ...

(defn build-mei [input]
    [:mei {:xmlns "http://www.music-encoding.org/ns/mei"}
          [:meiHead [:fileDesc [:titleStmt [:title]] [:pubStmt]]] ;; minimal <meiHead>
          [:music
           [:body
            [:mdiv
              [:score
                 [:scoreDef {:key.sig (:key.sig @app-state)}
                     [:staffGrp
                       [:staffDef {:n 1
                                   :lines 5
                                   :clef.shape "G"
                                   :clef.line 2}]]]

                 [:section
                   [:measure {:n 1}
                     [:staff {:n 1}
                       [:layer {:n 1}
                          (build-notes input)]]]]]]]]])

(defn note-input []
  (let [notes (r/atom "4 c")]
    (fn []
      [:div
        [:textarea.input {:rows 1
                          :cols 50
                          :value @notes
                          :on-change (fn [e]
                                       (reset! notes (-> e .-target .-value))
                                       (swap! app-state assoc
                                         :mei (build-mei (str/trim (-> e .-target .-value)))))}]])))

(defn zoom []
      (fn []
        [:div
          [:br]
          [:input {:type "range"
                   :value (:scale @app-state)
                   :min 10
                   :max 100
                   :on-change (fn [e]
                                (swap! app-state assoc :scale (-> e .-target .-value))
                                (.setOptions js/vrvToolkit
                                             (clj->js {:scale (int (:scale @app-state))})))}]

          [:span (str "  Zoom: " (:scale @app-state) "%")]]))

(defn render-score [hiccup]
  (.renderData js/vrvToolkit (eval-hiccup hiccup)
                             (clj->js {:scale (int (:scale @app-state))})))

;; These should work in principle, but had several problems with them:
;;
;; (defn xml->hicc [xml]
;;   (map h/as-hiccup (h/parse-fragment xml)))
;;
;; (defn embed-svg [hiccup]
;;   (->> hiccup
;;        render-score
;;        xml->hicc))
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn score []
  [:div.svg
    {:dangerouslySetInnerHTML
      {:__html (render-score (:mei @app-state))}}])

;; trying to implement event listeners

(defn clickables [class]
  (.querySelectorAll js/document class))

(defn clicked! [e]
  (let [v (str (-> e .-target))]
    (.alert js/window (str "clicked:" v))))

(map (.addEventListener js/document "click" clicked!)
     (clickables ".note"))



;; helpers to look into data:
(defn result-hiccup []
    [:div.look
     [:hr]
     [:p (str (:mei @app-state))]])

(defn result-xml []
    [:div.look
     [:hr]
     [:p (str (eval-hiccup (:mei @app-state)))]])

(defn result-svg []
  [:div.look
   [:hr]
   [:p (render-score (:mei @app-state))]])
;;_________________________________________

(defn app []
  [:div
   [header]
   [key-input]
   [note-input]
   [zoom]
   [score]

   #_[result-hiccup]
   #_[result-xml]
   #_[result-svg]])
