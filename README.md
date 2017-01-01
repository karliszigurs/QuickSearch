[![Build Status](https://travis-ci.org/karliszigurs/QuickSearch.svg?branch=master)](https://travis-ci.org/karliszigurs/QuickSearch) [![SonarQube Coverage](https://img.shields.io/sonar/http/sonarqube.com/com.zigurs.karlis.utils.search:quicksearch/coverage.svg)](https://sonarqube.com/dashboard/index?id=com.zigurs.karlis.utils.search%3Aquicksearch) [![SonarQube Tech Debt](https://img.shields.io/sonar/http/sonarqube.com/com.zigurs.karlis.utils.search:quicksearch/tech_debt.svg)](https://sonarqube.com/dashboard/index?id=com.zigurs.karlis.utils.search%3Aquicksearch) [![Coverty Scan](https://scan.coverity.com/projects/10587/badge.svg)](https://scan.coverity.com/projects/karliszigurs-quicksearch) [![Codacy grade](https://img.shields.io/codacy/grade/c0340f5e099740d4b27c830c282d073e.svg)](https://www.codacy.com/app/homolupus/QuickSearch/dashboard)
# QuickSearch

Simple and lightweight in-memory search library for Java 8 web and desktop applications. Single 30KB jar. 100% test coverage. Fast.

## Demo

Live demo available at http://zigurs.com/qs (instance of https://github.com/karliszigurs/QuickSearchServer).

## Description

QuickSearch provides pretty instant search over arbitrary items and their keywords.
This is achieved by adding items and pre-calculating their keywords and partial matching
fragments that are then matched against users input during search.

It is well suited for small to medium data sets (e.g. up to a 10-20GB of JVM heap) that can fit into memory, e.g.

  * Contacts lookup (keywords of name, department, position, location, ...)
  * Online store stock search (keywords of category, raw description, brand names, reviews, ...)
  * Complex category navigation (keywords of category tree, category items, category aliases, ...)
  * Faceted search of sparsely distributed categories
  * ...

## Performance and footprint

Circa 500k - 1 million lookups per second on a commodity server. Search request processing times
in single digit microsecond range. Less than 20MB of heap required for 100'000 unique items. Thread safe.

Benchmarks available at https://github.com/karliszigurs/QuickSearchBenchmarks

## Include it in your project

```Maven
<dependency>
    <groupId>com.zigurs.karlis.utils.search</groupId>
    <artifactId>QuickSearch</artifactId>
    <version>1.8</version>
</dependency>
```

## Use example

```Java
/* create instance with all defaults */
QuickSearch<String> qs = new QuickSearch<>();

/* add a few example movies and associated keywords */
qs.addItem("The Da Vinci Code", "The Da Vinci Code Wedding Will Tu...");
qs.addItem("Ice Age: The Meltdown", "Ice Age: The Meltdown  Family...");
qs.addItem("Casino Royale", "Casino Royale Prague, James Bond MI6 ...");
qs.addItem("Night at the Museum", "Family Larry Daley Ben Stiller ...");
qs.addItem("Cars", "Cars Family last race Piston Cup tie Strip The...");

/* find a top result for query */
Optional<String> foundItem = qs.findItem("bond");
System.out.println(foundItem.get());

/* find up to 10 top items */
List<String> foundItems = qs.findItems("family ca", 10);
foundItems.forEach(i -> System.out.println(String.format("- %s", i)));
```

## Caveats

All internal data structures are pre-calculated on add/remove and reside in memory. Following considerations apply:

  * Searchable items should be relatively lightweight as they will be long lived.
    If you want to search over heavyweight items (e.g. pictures or ORM) it is best
    to create an lightweight immutable object that contains only the minimum required to refer to fill payload.
  * QuickSearch does not contain any protection against running out of memory. This shouldn't be a problem,
    as in normal use even 10s of thousands of searchable items and keywords don't require more than a few
    tens of megabytes of heap. If you cannot ensure that you are removing no longer relevant entries it is
    recommended to periodically clear and repopulate the index (e.g. daily).

### Credits

```
                              //
(C) 2016 Karlis Zigurs (http://zigurs.com)
                            //
```
