(ns app.state
  (:require [reagent.core :refer [atom]]))

(defonce app-state
  (atom {:scale 60
         :key.sig "0"
         :mei [:mei {:xmlns "http://www.music-encoding.org/ns/mei"}
               [:meiHead [:fileDesc [:titleStmt [:title]] [:pubStmt]]] ;; minimal <meiHead>
               [:music
                [:body
                 [:mdiv
                   [:score
                      [:scoreDef
                          [:staffGrp
                            [:staffDef {:n 1
                                        :lines 5
                                        :clef.shape "G"
                                        :clef.line 2}]]]

                      [:section
                       [:measure {:n 1}
                        [:staff {:n 1}
                          [:layer {:n 1}
                           [:note {:pname "c"
                                   :oct 4
                                   :dur 4}]]]]]]]]]]}))
