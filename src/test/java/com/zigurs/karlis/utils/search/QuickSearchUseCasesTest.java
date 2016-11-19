package com.zigurs.karlis.utils.search;

import org.junit.Test;

import java.util.*;

import static com.zigurs.karlis.utils.search.QuickSearch.MergePolicy.INTERSECTION;
import static com.zigurs.karlis.utils.search.QuickSearch.UnmatchedPolicy.IGNORE;
import static org.junit.Assert.assertEquals;

/**
 * A few example use cases for {@link QuickSearch}
 */
public class QuickSearchUseCasesTest {

    @Test
    public void shouldFindAllMammals() {
        /* create default instance */
        QuickSearch<String> quickSearch = new QuickSearch<>();

        /* Add a few animals (no animals were harmed during execution of this test) */
        quickSearch.addItem("Cat", "cat domestic mammal");
        quickSearch.addItem("Dog", "dog domestic mammal");
        quickSearch.addItem("Lobster", "lobster crustacean");
        quickSearch.addItem("Shrimp", "shrimp crustacean");
        quickSearch.addItem("Pigeon", "pigeon urban menace");

        /* ask for top 10 items matching "mammal" */
        List<String> mammals = quickSearch.findItems("mammal", 10);

        /* And verify that we got Cat and Dog in return */
        assertEquals(Arrays.asList(new String[]{"Cat", "Dog"}), mammals);
    }

    @Test
    public void shouldFindOnlyExactFacets() {
        /*
         * Configure a QuickSearch instance for navigating
         * for objects matching _all_ and _exactly_ provided keywords
         * over a large collection.
         * Use builder to access configuration options.
         */
        QuickSearch<FacetedObject<String>> facetedStringsSearch = QuickSearch
                .builder()
                    /* Don't try to find partial matches (e.g. 'loc' in 'location' from 'locust->locus->locu->loc' */
                .withUnmatchedPolicy(IGNORE)
                    /* return objects that match all criteria supplied */
                .withMergePolicy(INTERSECTION)
                    /* potentially (very) large collection, use all available resources.
                     * This speeds things up a bit if there are more than 10 or so search
                     * terms to look for. Small penalty for queries of less than that.
                     */
                .withParallelProcessing()
                    /* we know the format, so can use a simpler extractor function.
                     *
                     * TODO: A version in the future will probably include Set<String> interfaces
                     * for keywords to avoid the split/join operations around these.
                     */
                .withKeywordsExtractor(s -> new HashSet<>(Arrays.asList(s.split(","))))
                    /* simplest normaliser */
                .withKeywordNormalizer(String::toLowerCase)
                    /* Only allow exact matches (not matching 'loc' against 'location' in example */
                .withKeywordMatchScorer(QuickSearch.EXACT_MATCH_SCORER)
                    /* it's getting a bit tedious... */
                .build();

        /* Create a few 'faceted' objects */
        FacetedObject<String> server1 = new FacetedObject<>("Server 1", new HashSet<>(Arrays.asList("location:dc1", "type:small", "host:io.startup.signups", "rootauth:Password1")));
        FacetedObject<String> server2 = new FacetedObject<>("Server 2", new HashSet<>(Arrays.asList("location:dc1", "type:large", "host:io.startup.site", "rootauth:Pa$$word!")));
        FacetedObject<String> server3 = new FacetedObject<>("Server 3", new HashSet<>(Arrays.asList("location:dc2", "type:large", "host:io.startup.site-qa", "rootauth:password")));
        FacetedObject<String> server4 = new FacetedObject<>("Server 4", new HashSet<>(Arrays.asList("location:dc8", "type:gigantic", "host:com.zigurs.karlis")));

        /* Add them to search index */
        facetedStringsSearch.addItem(server1, String.join(",", server1.getFacets()));
        facetedStringsSearch.addItem(server2, String.join(",", server2.getFacets()));
        facetedStringsSearch.addItem(server3, String.join(",", server3.getFacets()));
        facetedStringsSearch.addItem(server4, String.join(",", server4.getFacets()));

        /* Find all servers in dc1 */
        assertEquals(2, facetedStringsSearch.findItems("location:dc1", Integer.MAX_VALUE).size());

        /* Find all large instances in dc2 (there's only one) */
        assertEquals("Server 3", facetedStringsSearch.findItems("location:dc2,type:large", Integer.MAX_VALUE).get(0).getObject());

        /* Find nothing in case of only partial match */
        assertEquals(0, facetedStringsSearch.findItems("location:dc", Integer.MAX_VALUE).size());
    }

    @Test
    public void shouldFindAllMatchingCategories() {
        /* Create a mock hierarchical menu tree */
        List<StoreCategory> categories = new ArrayList<>();

        StoreCategory electronics = new StoreCategory("electronics");
        categories.add(electronics);
        StoreCategory e_dvd = new StoreCategory("dvd players", electronics);
        categories.add(e_dvd);
        StoreCategory e_d_b1 = new StoreCategory("somy", e_dvd);
        categories.add(e_d_b1);
        StoreCategory e_d_b2 = new StoreCategory("phijips", e_dvd);
        categories.add(e_d_b2);
        StoreCategory e_d_fantasy = new StoreCategory("bang and olaf", e_dvd);
        categories.add(e_d_fantasy);

        StoreCategory books = new StoreCategory("books");
        categories.add(books);
        StoreCategory thrillers = new StoreCategory("thrillers", books);
        categories.add(thrillers);
        StoreCategory diy = new StoreCategory("diy", books);
        categories.add(diy);
        StoreCategory repair = new StoreCategory("dvd repair", diy);
        categories.add(repair);

        /* Create defaults search instance */
        QuickSearch<StoreCategory> categoriesSearch = new QuickSearch<>();

        /* Hierarchy generates breadcrumbs path like this: */
        assertEquals("books » diy » dvd repair", repair.getDisplayPath());

        /* Add all items with their category path as the keywords */
        for (StoreCategory category : categories)
            categoriesSearch.addItem(category, category.getDisplayPath());

        /* Find all items matching dvd */
        List<StoreCategory> foundCategories = categoriesSearch.findItems("dvd", 10);

        /* Inspect and process the results here */

        assertEquals(5, foundCategories.size());
    }

    private class StoreCategory {

        private final String displayName;
        private final StoreCategory parentCategory;
        private final int menuDepth;

        private StoreCategory(final String displayName) {
            this(displayName, null);
        }

        private StoreCategory(final String displayName,
                              final StoreCategory parentCategory) {
            Objects.requireNonNull(displayName);

            if (displayName.trim().isEmpty())
                throw new IllegalArgumentException("Name cannot be effectively empty");

            this.displayName = displayName.trim();
            this.parentCategory = parentCategory;

            if (parentCategory != null)
                menuDepth = parentCategory.getMenuDepth() + 1;
            else
                menuDepth = 0;
        }

        private String getDisplayName() {
            return displayName;
        }

        private StoreCategory getParentCategory() {
            return parentCategory;
        }

        private String getDisplayPath() {
            return constructDisplayPath(new StringBuilder()).toString();
        }

        private StringBuilder constructDisplayPath(StringBuilder builder) {
            if (getParentCategory() != null) {
                getParentCategory().constructDisplayPath(builder);
                builder.append(" » ");
                builder.append(getDisplayName());
            } else {
                builder.append(getDisplayName());
            }
            return builder;
        }

        private int getMenuDepth() {
            return menuDepth;
        }
    }

    private class FacetedObject<T> {

        private final T object;
        private final Set<String> facets;

        private FacetedObject(T object, Set<String> facets) {
            Objects.requireNonNull(object);
            Objects.requireNonNull(facets);

            this.object = object;
            this.facets = facets;
        }

        private T getObject() {
            return object;
        }

        private Set<String> getFacets() {
            return facets;
        }
    }
}
