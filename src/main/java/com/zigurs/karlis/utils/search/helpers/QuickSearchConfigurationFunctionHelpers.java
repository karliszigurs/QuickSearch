package com.zigurs.karlis.utils.search.helpers;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Utility class to consolidate functions verifying minimum
 * behavior requirements of the configuration functions supplied to
 * {@link com.zigurs.karlis.utils.search.QuickSearch.QuickSearchBuilder}.
 */
public final class QuickSearchConfigurationFunctionHelpers {

    private QuickSearchConfigurationFunctionHelpers() {
        // Not instantiable
    }

    /**
     * //TODO - update doc
     */
    public static void testKeywordsExtractorFunction(final Function<String, Set<String>> function) {
        try {
            if (function.apply("") == null || function.apply("testinput") == null)
                throw new IllegalArgumentException("Keywords extractor function failed non-null result test");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception while testing keywords extractor function", e);
        }
    }

    /**
     * //TODO - update doc
     */
    public static void testKeywordNormalizerFunction(final Function<String, String> function) {
        try {
            if (function.apply("") == null || function.apply("testinput") == null)
                throw new IllegalArgumentException("Keyword normalizer function failed non-null output test");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception while testing keyword normalizer function", e);
        }
    }

    /**
     * //TODO - update doc
     */
    public static void testKeywordMatchScorerFunction(final BiFunction<String, String, Double> function) {
        try {
            function.apply("testinput", "testinput");
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception while testing keyword match scorer function", e);
        }
    }

}
