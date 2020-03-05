(ns app.parsing
  (:require [instaparse.core :as insta]))

(def music-ebnf-english
  "<S> = CHUNK*
CHUNK = DURATION PITCHES
DURATION = dur dots* WS*
dur = '1' | '2' | '4' | '8' | '16' | '32' | '64'
dots = #'\\.*'
PITCHES = PITCH*
PITCH = (step accid* WS* tie? | rest) WS*
step = 'C' | 'D' | 'E' | 'F' | 'G' | 'A' | 'B' |
        'c' | 'd' | 'e' | 'f' | 'g' | 'a' | 'b' |
        'cc' | 'dd' | 'ee' | 'ff' | 'gg' | 'aa' | 'bb'
accid = '#' | 'n' | '-' | '--' | 'x'
rest = 'r'
tie = '_'
<WS> = <#'\\s+'>")

(def accidental-table
  {"-"  "f"
   "--" "ff"
   "n"  "n"
   "#"  "s"
   "x"  "x"})

(def pitch-table
  {"C" {:pname "c", :oct 3}
   "D" {:pname "d", :oct 3}
   "E" {:pname "e", :oct 3}
   "F" {:pname "f", :oct 3}
   "G" {:pname "g", :oct 3}
   "A" {:pname "a", :oct 3}
   "B" {:pname "b", :oct 3}

   "c" {:pname "c", :oct 4}
   "d" {:pname "d", :oct 4}
   "e" {:pname "e", :oct 4}
   "f" {:pname "f", :oct 4}
   "g" {:pname "g", :oct 4}
   "a" {:pname "a", :oct 4}
   "b" {:pname "b", :oct 4}

   "cc" {:pname "c", :oct 5}
   "dd" {:pname "d", :oct 5}
   "ee" {:pname "e", :oct 5}
   "ff" {:pname "f", :oct 5}
   "gg" {:pname "g", :oct 5}
   "aa" {:pname "a", :oct 5}
   "bb" {:pname "b", :oct 5}})

(def parse-music-input (insta/parser music-ebnf-english))

(defn- vec->map [v] (apply hash-map v))

(defn- duration-transform [& es] ;; as transform-f for Instaparse
  (-> (apply merge (map vec->map es))))

(defn- pitch-transform [& vs]
  (-> (apply merge (map vec->map vs))))

(defn- accidental-transform [a]
  [:accid (get accidental-table a)])

(defn- tie-transform [t]
  [:tie "i"])

(def transformations
  {:PITCH pitch-transform
   :DURATION duration-transform
   :accid accidental-transform
   :tie tie-transform})

(def transform (partial insta/transform transformations))

(defn build-notes-from-chunk [C]
; a "chunk" is just a series of one duration and a arbitrary series of pitches/rests.
  (let [[dur P] (rest C)
        pitches (rest P)]
    (map merge (repeat dur) pitches)))

(defn- specify-pitch [e]
    (-> (into e (get pitch-table (:step e)))
        (dissoc :step)))

(defn count-dots [e]
  (if (:dots e)
    (update e :dots count)
    e))

(defn complete-ties [es]
  (reduce (fn [acc e]
            (if (:tie (last acc)) ;; this is a rather crude method
              (if (:tie e)        ;; if there is a tie starting on the former note ...
                (conj acc (assoc e :tie "m")) ;; we assume it should find it's end in the next one
                (conj acc (assoc e :tie "t")))
              (conj acc e)))
    [] es))

;; generate IDs for the notes and rests, if we want/need our own
;; ... otherwise there is an ID generated for each event automatically
#_(def id-gen (atom 1))

#_(defn note-id []
    (swap! id-gen inc)
    {:xml:id (str "note-" @id-gen)})

#_(defn rest-id []
    (swap! id-gen inc)
    {:xml:id (str "rest-" @id-gen)})

(defn make-note-or-rest [e]
  (cond
    (:step e) [:note (merge (specify-pitch e)
                            #_(note-id))]

    (:rest e) [:rest (merge (dissoc e :rest)
                            #_(rest-id))]))

(defn build-notes [s]
     (->> (parse-music-input s)
          transform
          (mapcat build-notes-from-chunk)
          (map count-dots)
          complete-ties
          (map make-note-or-rest)))
