(ns app.hiccup
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]))

;; taken from
;; https://github.com/lispcast/domain-specific-languages-in-clojure/blob/002-after/src/domain_specific_languages_clojure/core.clj#L4

(defn normalize [hiccup]
  (let [[tag attr? & children] hiccup
          attr (if (map? attr?)
                 attr?
                 {})
          children (if (map? attr?)
                     children
                     (cons attr? children))]
    (into [tag attr] children)))

 ;; hiccup -> String (HTML)
(defn eval-hiccup [hiccup]
  (cond
    (nil? hiccup)
    ""

    (string? hiccup)
    hiccup

    (number? hiccup)
    (str hiccup)

    (vector? hiccup)
    (let [[tag attr & children] (normalize hiccup)]
         (str "<" (name tag)
           (str/join
             (for [[k v] attr]
               (str " " (name k) "=\"" v "\"")))
           ">"
           (str/join (map eval-hiccup children))
           "</" (name tag) ">"))

    (seq? hiccup)
    (str/join (map eval-hiccup hiccup))

    :else
    (throw (ex-info "I don't know how to handle this data as hiccup." {:hiccup hiccup}))))

;;;;;;;;;;;;;;;;;;;mecca-mei;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
 (s/def ::params (s/map-of keyword? any?))

 (s/def ::element
    (s/cat
     :tag keyword?
     :attrs (s/? ::params)
     :body (s/* ::node)))

 (s/def ::node
   (s/or
    :element ::element
    :number number?
    :string string?))

 (declare stringify-mei)

 (defn- stringify-params [params]
   (let [kvs (map #(str (name (first %)) "=\"" (second %) "\"" ) params)]
     (if (seq kvs)
       (str " " (str/join " " kvs))
       nil)))

 (defn- stringify-element [data]
   (let [tag (name (:tag data))
         params (:attrs data)
         content (:body data)]
     (if (nil? content)
       (str "<" tag (stringify-params params) "/>")
       (let [children (map stringify-mei content)]
         (str "<" tag (stringify-params params) ">"
             (str/join "" children)
             "</" tag ">")))))

 (defn- stringify-mei [mei-ast]
   (let [[type data] mei-ast]
     (case type
       :string (str data)
       :number (str data)
       :element (stringify-element data))))

 (defn hiccup->mei [mei]
   (let [parsed-mei (s/conform ::node mei)]
     (if (= :s/invalid parsed-mei)
       (s/explain-str ::node mei)
       (stringify-mei parsed-mei)))))
