[![Build Status](https://travis-ci.org/karliszigurs/QuickSearch.svg?branch=master)](https://travis-ci.org/karliszigurs/QuickSearch) [![SonarQube Coverage](https://img.shields.io/sonar/http/sonarqube.com/com.zigurs.karlis.utils.search:quicksearch/coverage.svg)](https://sonarqube.com/dashboard/index?id=com.zigurs.karlis.utils.search%3Aquicksearch) [![SonarQube Tech Debt](https://img.shields.io/sonar/http/sonarqube.com/com.zigurs.karlis.utils.search:quicksearch/tech_debt.svg)](https://sonarqube.com/dashboard/index?id=com.zigurs.karlis.utils.search%3Aquicksearch) [![Coverty Scan](https://scan.coverity.com/projects/10587/badge.svg)](https://scan.coverity.com/projects/karliszigurs-quicksearch) [![Codacy grade](https://img.shields.io/codacy/grade/c0340f5e099740d4b27c830c282d073e.svg)](https://www.codacy.com/app/homolupus/QuickSearch/dashboard)
# QuickSearch
Simple and lightweight in-memory search library for Java 8 web and desktop applications. Single 20KB jar. 100% test coverage.

QuickSearch provides pretty instant incremental search over arbitrary items and their keywords.
This is achieved by adding items and pre-calculating corresponding matching keywords and their
fragments that are then matched against users input during search.

It is well suited for small to medium datasets (e.g. up to a few tens of thousands items) that can fit into memory, e.g.

  * Contacts lookup (keywords of department, position, location, etc)
  * Online shop stock (keywords of category, raw description, brand names, reviews)
  * Complex category navigation (keywords of category tree, category items, category aliases)
  * Movie search (keywords of genre, plot summary, actors, main landmarks, studio and producers)
  * ...

## Performance and footprint

Circa 500k - 1 million lookups per second on a commodity server. Search request processing times
in single digit microsecond range. Less than 50MB of heap required for 100'000 unique items. Thread safe.

## Demo

Live demo available at http://zigurs.com/qs/ (instance of https://github.com/karliszigurs/QuickSearchServer).

## Include it in your project

```Maven
<dependency>
    <groupId>com.zigurs.karlis.utils.search</groupId>
    <artifactId>quicksearch</artifactId>
    <version>LATEST</version>
</dependency>
```

## Use example

```Java
// Create search instance. Could be injected as a bean or accessed globally via singleton.
QuickSearch<String> quickSearchInstance = new QuickSearch<>();

// Populate the index with display item (String in this instance) and associated keywords.
quickSearchInstance.addItem("The Da Vinci Code", "The Da Vinci Code Wedding Will Turner Elizabeth Swann Lord Cutler Beckett East India Trading Company");
quickSearchInstance.addItem("Ice Age: The Meltdown", "Ice Age: The Meltdown  Family Jacques Saunière Louvre curator pursued  Great Gallery albino Catholic monk Silas Priory keystone");
quickSearchInstance.addItem("Casino Royale", "Casino Royale Prague, James Bond MI6 station chief Dryden terrorist contact license to kill 00 agent");
quickSearchInstance.addItem("Night at the Museum", "Family Larry Daley Ben Stiller divorced man stable job failed business -wife Kim Raver bad example");
quickSearchInstance.addItem("Cars", "Cars Family last race Piston Cup tie Strip The King Weathers Chick Hicks Lightning McQueen tiebreaker");

// The following will return top hit of Casino Royale.
String topName = quickSearchInstance.findItem("license"); // Finds Casino Royale

/* The following will return Cars, Ice Age and Night at the Museum
   Cars will be the top scoring result due to 'cars' match. */
List<String> topFamilyMovies = quickSearchInstance.findItems("family ca", 10);
        
// Forget known items
quickSearchInstance.clear();
```

## Caveats

All internal data structures are pre-calculated and reside in memory. Following considerations apply:

  * Searchable items should be relatively lightweight as they will be long lived.
    If you want to search over heavyweight items (e.g. pictures or ORM) it is best
    to create an lightweight immutable object that contains only the minimum required to refer to fill payload.
  * QuickSearch does not contain any protection against running out of memory. This shouldn't be a problem,
    as in normal use even 10s of thousands of searchable items and keywords don't require more than a few
    tens of megabytes of heap. If you cannot ensure that you are removing no longer relevant entries it is
    recommended to periodically clear and repopulate the index (e.g. daily).
    
## Future features

  * [ ] .js/go version
  * [X] web service and quick search API (https://github.com/karliszigurs/QuickSearchServer)
  * [ ] fuzzy text matching

## Credits

Karlis Zigurs, 2016
