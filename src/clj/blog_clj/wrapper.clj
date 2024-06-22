(ns blog-clj.wrapper)

(def targets #{:html :hiccup})

(def default {:target :hiccup
                      :fn identity})

(def schema {:target {:type :keyword :coerce #(or % (:target default)) :validate #(contains? targets %)}
                     :fn     {:type :fn :coerce #(or % (:fn default))}})