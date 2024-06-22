(ns blog-clj.main
  (:require [blog-clj.core :as core]))

(def settings {:input-path "tmp/md"
               :output-path "tmp/html"
               :output-suffix ".html"
               :wrapper {:target :hiccup :fn (fn [hiccup] [:hello hiccup])}})

(defn -main [& args]
  (core/generate-blogs settings))