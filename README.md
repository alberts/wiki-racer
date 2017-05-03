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
```
java -jar target/uberjar/wiki-racer-0.1.0-SNAPSHOT-standalone.jar -s "Kevin Bacon" -e "Quentin Tarantino"
.
.
.
-----------------------------------------------------------
Kevin Bacon -> Daniel Day-Lewis
Daniel Day-Lewis -> Quentin Tarantino
---------------------------------------------------------
"Elapsed time: 14194.813605 msecs"
```

## General workflow
- The engine has a dispatcher which reads from a job queue that contains unvisited wiki pages and distributes them to workers. Each page is tracked with the depth. Work is dispatched from the lowest index(depth) first effectively making the wiki scan a BFS

- Before sending out a batch of work the dispatcher checks if it has seen a destination link and displays the output by going from the destination link to the source link.

- The entire state of the app(tracked-links, visited urls, pending work) is kept in a thread safe data structure updated by the workers as it scans each page

## Strategies tried
Did a simple breadth first search(traverse all the links within a single page before going to the subsequent set of pages) no threads simple job queue to see if the functionality to scrape pages as well as traverse worked.
However the BFS does result in a stack overflow when the number of links exceed a certain amount. To avoid this the app simply scans the pages in BFS order and tracks the source from which a page came from. This way it does not need to do a BFS when it finds the destination.
The path from the destination to the source can be found by tracking dest -> source until the source is the start page.

Since a pure BFS accumulates well over a 100K links just at the 3rd level(3 pages away from the start) that a heuristic will need to be applied as in its possible that we do a depth first search starting at a certain level but that is something I haven't tried.

### Bugs
Currently there is no stopping the racer and it will either find you a path or run out of memory.
As a feature it is easy to add a stopping condition that either times out or ends after looking at a certain number of links

Also using the STM to manage a queue is problematic. An STM uses a tests and set method to update a data structure which means it occasionally appears that the racer is not making progress. To fix this I'd have to implement the queue that lines up work in Java and bridge between clojure and java to distribute work.
