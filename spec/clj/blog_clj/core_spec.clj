(ns blog-clj.core-spec
  (:require [blog-clj.core :as sut]
            [clojure.java.io :as io]
            [speclj.core :refer :all #_[before context describe it should should= redefs-around
                                        should-have-invoked with-stubs]]))

(def path "spec/tmp")
(def in-path (str path "/in"))
(def out-path (str path "/out"))
(def other-path (str path "/other-dir"))

(def paths [path in-path out-path other-path])

(defn- init-paths []
  (mapv #(.mkdir (io/file %)) paths))

(defn- files [dir] (remove #(.isDirectory %) (.listFiles (io/file dir))))
(defn- file-names [fs] (map #(.getName %) (files fs)))
(defn- file-contents [fs] (map slurp (files fs)))
(defn- clear-files [] (run! io/delete-file (apply concat (map files paths))))
(defn- spit-in [path file-name content] (spit (str path "/" file-name) content))

(def bare-settings {:input-path in-path :output-path out-path})

(describe "core"    
          
  (context "generate blogs"
    (before (clear-files) 
            (init-paths))
    
    (it "in empty input directory"
      (sut/generate-blogs bare-settings)
      (should= [] (files out-path)))
    
    (context "in populated input directory"
      
      (context "empty file"
        
        (it "no output suffix"
          (spit-in in-path "empty.md" "")
          (sut/generate-blogs bare-settings)
          (should= ["empty"] (file-names out-path))
          (should= ["<div></div>"] (file-contents out-path))))
      
      (context "with output suffix"

        (it "\".html\""
            (spit-in in-path "empty.md" "")
            (sut/generate-blogs (assoc bare-settings :output-suffix ".html"))
            (should= ["empty.html"] (file-names out-path))
            (should= ["<div></div>"] (file-contents out-path)))
      
        (it "\".greetings\""
            (spit-in in-path "empty.md" "")
            (sut/generate-blogs (assoc bare-settings :output-suffix ".greetings"))
            (should= ["empty.greetings"] (file-names out-path))
            (should= ["<div></div>"] (file-contents out-path))))
            
      (context "populated file"
        
        (context "with basic markdown"
          (it "\"Hello, World\""
            (spit-in in-path "populated.md" "Hello, World")
            (sut/generate-blogs bare-settings)
            (should= ["populated"] (file-names out-path))
            (should= ["<div><p>Hello, World</p></div>"] (file-contents out-path)))
          
          (it "\"# Hello, World\""
            (spit-in in-path "populated.md" "# Hello, World")
            (sut/generate-blogs bare-settings)
            (should= ["populated"] (file-names out-path))
            (should= ["<div><h1>Hello, World</h1></div>"] (file-contents out-path)))
          
          (it "\"# Hello, World\\n# Goodbye\""
            (spit-in in-path "populated.md" "# Hello, World\n# Goodbye")
            (sut/generate-blogs bare-settings)
            (should= ["populated"] (file-names out-path))
            (should= ["<div><h1>Hello, World</h1><h1>Goodbye</h1></div>"] (file-contents out-path)))))
      
      (context "with wrapper"
      
        (context "target"
      
          (context "none"
            
            (it "li wrapper"
              (spit-in in-path "populated.md" "# Hello, World\n# Goodbye")
              (sut/generate-blogs (assoc bare-settings :wrapper {:fn (fn [hiccup] [:li hiccup])}))
              (should= ["populated"] (file-names out-path))
              (should= ["<li><div><h1>Hello, World</h1><h1>Goodbye</h1></div></li>"] (file-contents out-path)))
            
            (it "styled div wrapper"
              (spit-in in-path "populated.md" "# Hello, World\n# Goodbye")
              (sut/generate-blogs (assoc bare-settings :wrapper {:fn (fn [hiccup] [:div.cool hiccup])}))
              (should= ["populated"] (file-names out-path))
              (should= ["<div class=\"cool\"><div><h1>Hello, World</h1><h1>Goodbye</h1></div></div>"] (file-contents out-path))))
          
          (context "html"
          
            (it "li wrapper"
              (spit-in in-path "populated.md" "# Hello, World\n# Goodbye")
              (sut/generate-blogs (assoc bare-settings :wrapper {:target :html
                                                                 :fn (fn [html] (str "<li>" html "</li>"))}))
              (should= ["populated"] (file-names out-path))
              (should= ["<li><h1>Hello, World</h1>\n<h1>Goodbye</h1>\n</li>"] (file-contents out-path))))
          
          (context "hiccup"
            
            (it "li wrapper"
              (spit-in in-path "populated.md" "# Hello, World\n# Goodbye")
              (sut/generate-blogs (assoc bare-settings :wrapper {:target :hiccup
                                                                 :fn (fn [hiccup] [:li hiccup])}))
              (should= ["populated"] (file-names out-path))
              (should= ["<li><div><h1>Hello, World</h1><h1>Goodbye</h1></div></li>"] (file-contents out-path)))))))
        
        
    (it "from specified input directory"
      (spit-in other-path "greetings.md" "# Greetings")
      (sut/generate-blogs (assoc bare-settings :input-path other-path))
      (should= ["greetings"] (file-names out-path))
      (should= ["<div><h1>Greetings</h1></div>"] (file-contents out-path)))
    
    (it "to specified output directory"
      (spit-in in-path "hello.md" "# hello")
      (sut/generate-blogs (assoc bare-settings :output-path other-path))
      (should= ["hello"] (file-names other-path))
      (should= ["<div><h1>hello</h1></div>"] (file-contents other-path)))
    
    (context "with bad schema"
      
      (it "missing input-path"
        (should-throw (sut/generate-blogs (dissoc bare-settings :input-path))))
      
      (it "missing output-path"
        (should-throw (sut/generate-blogs (dissoc bare-settings :output-path))))
      
      (it "invalid wrapper target"
          (should-throw (sut/generate-blogs (assoc bare-settings :wrapper {:target :nuh-uh})))))))