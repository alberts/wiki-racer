(ns wiki-racer.scraper-test
  (:require [wiki-racer.scraper :refer :all]
            [net.cgrand.enlive-html :as html]
            [midje.sweet :refer :all]))

(fact "enlive works and i understand it"
      (let [page (html/html-resource (java.io.StringReader. "<html>
      <body>
      <h1 id=\"firstHeading\">text</h1>
      </body>
      </html>"))]
        (get-title page) => "text"))

(fact "scraper should pick up distinct links"
       (let [page (html/html-resource (java.io.StringReader. "<html>
      <body>
      <div>
      <a href=\"/wiki/page1\" title=\"Page 1\"/>
      </div>
      <div>
      <a href=\"/wiki/page1\" title=\"Page 1\"/>
      <a href=\"/wiki/page2\" />
      <a href=\"/not-wiki/page3\" title=\"has a title\" />
      </div>
      </body>
      </html>"))]
        (get-links page) => [{:href "/wiki/page1" :title "Page 1"}]))
