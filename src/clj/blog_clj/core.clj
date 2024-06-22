(ns blog-clj.core
  (:require [c3kit.apron.schema :as schema]
            [blog-clj.blog :as blog]))

(defn generate-blogs [settings]
  (let [settings (schema/conform! blog/settings-schema settings)]
    (mapv #(blog/generate % settings) (blog/get-md-files settings))))

(defn blog-list [settings]
  (let [{:keys [src sort-fn]} (schema/conform! blog/list-schema settings)]
    (->> (blog/get-files src)
         (map #(hash-map :name (.getName %) :content (slurp %)))
         (sort sort-fn))))