(ns blog-clj.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup-bridge.core :as bridge]
            [hiccup2.core :as hiccup]
            [c3kit.apron.schema :as schema]
            [blog-clj.wrapper :as wrapper])
  (:import org.commonmark.parser.Parser
           (org.commonmark.renderer.html HtmlRenderer)))

(def generate-blogs-schema {:input-path    {:type :string :validate schema/present? :message "must be present!"}
                            :output-path   {:type :string :validate schema/present? :message "must be present!"}
                            :output-suffix {:type :string}
                            :wrapper       {:type :map :schema wrapper/schema :coerce #(or % wrapper/default)}})

(def parser (-> (Parser/builder) (.build)))

(defn- parse-markdown [s] (.parse parser s))

(def renderer (-> (HtmlRenderer/builder) (.build)))

(defmulti md->target (fn [target & _] target))

(defmethod md->target :html [_ md]
  (.render renderer (parse-markdown md)))

(defmethod md->target :hiccup [_ md] 
  (as-> md $ (md->target :html $) (bridge/html->hiccup $) (first $) (second $) (assoc $ 0 :div)))

(defn- get-files 
  ([input-path]
   (->> (.listFiles (io/file input-path))
        (remove #(.isDirectory %))))
  ([input-path ext]
   (->> (get-files input-path)
        (filter #(str/ends-with? (.getName %) (str "." ext))))))

(defn- get-md-files [{:keys [input-path]}]
  (get-files input-path "md"))

(defn- generate-blog [file settings]
  (let [{:keys [output-path output-suffix wrapper]} settings
        wrap-fn (:fn wrapper)
        content (md->target (:target wrapper) (slurp file))
        wrapped-content (hiccup/html {:escape-strings? false} (wrap-fn content))
        new-name (str/replace (.getName file) #"[.][^.]+$" "")]
    (spit (str output-path "/" new-name output-suffix) wrapped-content)))

(defn generate-blogs [settings]
  (let [settings (schema/conform! generate-blogs-schema settings)]
    (mapv #(generate-blog % settings) (get-md-files settings))))