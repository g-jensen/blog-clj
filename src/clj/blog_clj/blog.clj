(ns blog-clj.blog
  (:require [blog-clj.wrapper :as wrapper]
            [c3kit.apron.schema :as schema]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup-bridge.core :as bridge]
            [hiccup2.core :as hiccup])
  (:import [org.commonmark.ext.gfm.strikethrough StrikethroughExtension]
           [org.commonmark.ext.gfm.tables TablesExtension]
           [org.commonmark.ext.heading.anchor HeadingAnchorExtension]
           org.commonmark.parser.Parser
           (org.commonmark.renderer.html HtmlRenderer)))

(defn reverse-name [a b]
  (compare (:name b) (:name a)))

(def list-schema {:src     {:type :string :validate schema/present? :message "must be present"}
                  :sort-fn {:type :fn :coerce #(or % reverse-name)}})

(def settings-schema {:src           {:type :string :validate schema/present? :message "must be present!"}
                      :dest          {:type :string :validate schema/present? :message "must be present!"}
                      :output-suffix {:type :string}
                      :wrapper       {:type :map :schema wrapper/schema :coerce #(or % wrapper/default)}})

(def table-extension (TablesExtension/create))
(def strikethrough-extension (StrikethroughExtension/create))
(def heading-anchor-extension (HeadingAnchorExtension/create))

(def extensions [table-extension strikethrough-extension])

(def parser (-> (Parser/builder) 
                (.extensions extensions)
                (.build)))

(defn- parse-markdown [s] (.parse parser s))

(def renderer (-> (HtmlRenderer/builder) 
                  (.extensions extensions)
                  (.build)))

(defmulti md->target (fn [target & _] target))

(defmethod md->target :html [_ md]
  (.render renderer (parse-markdown md)))

(defmethod md->target :hiccup [_ md]
  (as-> md $ (md->target :html $) (bridge/html->hiccup $) (first $) (second $) (assoc $ 0 :div)))

(defn get-files
  ([input-path]
   (->> (.listFiles (io/file input-path))
        (remove #(.isDirectory %))))
  ([input-path ext]
   (->> (get-files input-path)
        (filter #(str/ends-with? (.getName %) (str "." ext))))))

(defn get-md-files [{:keys [src]}]
  (get-files src "md"))

(defn generate [file settings]
  (let [{:keys [dest output-suffix wrapper]} settings
        wrap-fn (:fn wrapper)
        content (md->target (:target wrapper) (slurp file))
        wrapped-content (hiccup/html {:escape-strings? false} (wrap-fn content))
        new-name (str/replace (.getName file) #"[.][^.]+$" "")]
    (spit (str dest "/" new-name output-suffix) wrapped-content)))