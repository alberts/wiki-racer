# wiki-racer

Finds paths between two wiki pages

## Installation and Building

Install leiningen

```sh
brew install lein
```

Build the uber jar

```sh
lein uberjar
```

## Usage

    $ java -jar wiki-racer-0.1.0-standalone.jar [args]

## Options
```
-h Print help
-n (optional) Number of workers that will scan wiki pages in parallel
-s The start page title
-e The title to end at
```
## Examples

java -jar target/uberjar/wiki-racer-0.1.0-SNAPSHOT-standalone.jar -s "Mike Tyson" -e "Fruit anatomy" -n 10

## General workflow
- The engine has a dispatcher which reads from a job queue that contains unvisited wiki pages and distributes them to workers

- Before sending out a batch of work the dispatcher checks if it has seen a destination link and displays the output by going from the destination link to the source link.

- The entire state of the app(tracked-links, visited urls, pending work) is kept in a thread safe data structure updated by the workers as it scans each page

### Bugs
When it has found a path the program prints a message "++++++++++so done" but while trying to actually spit it out
it seems to run out of heap space on any thing that has a distance of more than 2

So something like (Mike Tyson -> Greek Language) works but (Mike Tyson -> Fruit Anatomy) gets to the destination path but does not print a result

##Strategies tried
Did a simple breadth first search(traverse all the links within a single page before going to the subsequent set of pages) no threads simple job queue to see if the functionality to scrape pages as well as traverse worked.
But then doing it so i can distribute it to multiple threads/go blocks caused all sorts of trouble.
Then I ran into this heap space problem when displaying

