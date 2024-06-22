(ns blog-clj.core-spec
  (:require [blog-clj.core :as sut]
            [c3kit.apron.schema :as schema]
            [clojure.java.io :as io]
            [speclj.core :refer :all]))

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

(def blog-settings {:src in-path :dest out-path})

(def list-settings {:src in-path})

(describe "core"    
          
  (context "generate blogs"
    (before (clear-files) 
            (init-paths))
    
    (it "in empty input directory"
      (sut/generate-blogs blog-settings)
      (should= [] (files out-path)))
    
    (context "in populated input directory"
      
      (context "empty file"
        
        (it "no output suffix"
          (spit-in in-path "empty.md" "")
          (sut/generate-blogs blog-settings)
          (should= ["empty"] (file-names out-path))
          (should= ["<div></div>"] (file-contents out-path))))
      
      (context "with output suffix"

        (it "\".html\""
            (spit-in in-path "empty.md" "")
            (sut/generate-blogs (assoc blog-settings :output-suffix ".html"))
            (should= ["empty.html"] (file-names out-path))
            (should= ["<div></div>"] (file-contents out-path)))
      
        (it "\".greetings\""
            (spit-in in-path "empty.md" "")
            (sut/generate-blogs (assoc blog-settings :output-suffix ".greetings"))
            (should= ["empty.greetings"] (file-names out-path))
            (should= ["<div></div>"] (file-contents out-path))))
            
      (context "populated file"
        
        (context "with basic markdown"
          (it "\"Hello, World\""
            (spit-in in-path "populated.md" "Hello, World")
            (sut/generate-blogs blog-settings)
            (should= ["populated"] (file-names out-path))
            (should= ["<div><p>Hello, World</p></div>"] (file-contents out-path)))
          
          (it "\"# Hello, World\""
            (spit-in in-path "populated.md" "# Hello, World")
            (sut/generate-blogs blog-settings)
            (should= ["populated"] (file-names out-path))
            (should= ["<div><h1>Hello, World</h1></div>"] (file-contents out-path)))
          
          (it "\"# Hello, World\\n# Goodbye\""
            (spit-in in-path "populated.md" "# Hello, World\n# Goodbye")
            (sut/generate-blogs blog-settings)
            (should= ["populated"] (file-names out-path))
            (should= ["<div><h1>Hello, World</h1><h1>Goodbye</h1></div>"] (file-contents out-path)))))
      
      (context "with wrapper"
      
        (context "target"
      
          (context "none"
            
            (it "li wrapper"
              (spit-in in-path "populated.md" "# Hello, World\n# Goodbye")
              (sut/generate-blogs (assoc blog-settings :wrapper {:fn (fn [hiccup] [:li hiccup])}))
              (should= ["populated"] (file-names out-path))
              (should= ["<li><div><h1>Hello, World</h1><h1>Goodbye</h1></div></li>"] (file-contents out-path)))
            
            (it "styled div wrapper"
              (spit-in in-path "populated.md" "# Hello, World\n# Goodbye")
              (sut/generate-blogs (assoc blog-settings :wrapper {:fn (fn [hiccup] [:div.cool hiccup])}))
              (should= ["populated"] (file-names out-path))
              (should= ["<div class=\"cool\"><div><h1>Hello, World</h1><h1>Goodbye</h1></div></div>"] (file-contents out-path))))
          
          (context "html"
          
            (it "li wrapper"
              (spit-in in-path "populated.md" "# Hello, World\n# Goodbye")
              (sut/generate-blogs (assoc blog-settings :wrapper {:target :html
                                                                 :fn (fn [html] (str "<li>" html "</li>"))}))
              (should= ["populated"] (file-names out-path))
              (should= ["<li><h1>Hello, World</h1>\n<h1>Goodbye</h1>\n</li>"] (file-contents out-path))))
          
          (context "hiccup"
            
            (it "li wrapper"
              (spit-in in-path "populated.md" "# Hello, World\n# Goodbye")
              (sut/generate-blogs (assoc blog-settings :wrapper {:target :hiccup
                                                                 :fn (fn [hiccup] [:li hiccup])}))
              (should= ["populated"] (file-names out-path))
              (should= ["<li><div><h1>Hello, World</h1><h1>Goodbye</h1></div></li>"] (file-contents out-path)))))))
        
        
    (it "from specified source"
      (spit-in other-path "greetings.md" "# Greetings")
      (sut/generate-blogs (assoc blog-settings :src other-path))
      (should= ["greetings"] (file-names out-path))
      (should= ["<div><h1>Greetings</h1></div>"] (file-contents out-path)))
    
    (it "to specified destination"
      (spit-in in-path "hello.md" "# hello")
      (sut/generate-blogs (assoc blog-settings :dest other-path))
      (should= ["hello"] (file-names other-path))
      (should= ["<div><h1>hello</h1></div>"] (file-contents other-path)))
    
    (context "with bad schema"
      
      (it "missing src"
        (should-throw clojure.lang.ExceptionInfo "Unconformable entity" (sut/generate-blogs (dissoc blog-settings :src))))
      
      (it "missing dest"
        (should-throw clojure.lang.ExceptionInfo "Unconformable entity" (sut/generate-blogs (dissoc blog-settings :dest))))
      
      (it "invalid wrapper target"
        (should-throw clojure.lang.ExceptionInfo "Unconformable entity" (sut/generate-blogs (assoc blog-settings :wrapper {:target :nuh-uh}))))))
        
  (context "blog list"
    
    (it "is empty if no blogs"
      (should= [] (sut/blog-list list-settings)))
    
    (it "finds a blog if there is one"
      (spit-in in-path "blog1.md" "# Hello, there.")
      (should= [{:name "blog1.md" :content "# Hello, there."}] (sut/blog-list list-settings)))
    
    (context "sorted"
      (it "sorts greatest to least by default"
        (spit-in in-path "blog1.md" "# Hello, there.")
        (spit-in in-path "blog2.md" "# What's up?")
        (should= [{:name "blog2.md" :content "# What's up?"}
                  {:name "blog1.md" :content "# Hello, there."}] 
                 (sut/blog-list list-settings)))
      
      (it "sorts by custom function if specified"
        (spit-in in-path "blog1.md" "# Hello, there.")
        (spit-in in-path "blog2.md" "# What's up?")
        (should= [{:name "blog1.md" :content "# Hello, there."}
                  {:name "blog2.md" :content "# What's up?"}] 
                 (sut/blog-list (assoc list-settings :sort-fn #(compare (:content %1) (:content %2)))))))
    
    (it "looks in specified directory"
      (spit-in other-path "blog1.md" "# Hello, there.")
      (should= [{:name "blog1.md" :content "# Hello, there."}] (sut/blog-list (assoc list-settings :src other-path))))
    
    (context "with invalid schema"
      
      (it "missing src"
        (should-throw clojure.lang.ExceptionInfo "Unconformable entity" (sut/blog-list (dissoc list-settings :src))))
      
      (it "bad sort-fn"
          (should-throw clojure.lang.ExceptionInfo "Unconformable entity" (sut/blog-list (assoc list-settings :sort-fn 4)))))))