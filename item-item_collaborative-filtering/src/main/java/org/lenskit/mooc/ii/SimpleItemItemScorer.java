package org.lenskit.mooc.ii;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleSortedMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.results.Results;
import org.lenskit.util.ScoredIdAccumulator;
import org.lenskit.util.TopNScoredIdAccumulator;
import org.lenskit.util.math.Vectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleItemItemScorer extends AbstractItemScorer {
    private static final Logger logger = LoggerFactory.getLogger(SimpleItemItemModelProvider.class);

    private final SimpleItemItemModel model;
    private final DataAccessObject dao;
    private final int neighborhoodSize;

    @Inject
    public SimpleItemItemScorer(SimpleItemItemModel m, DataAccessObject dao) {
        model = m;
        this.dao = dao;
        neighborhoodSize = 20;
    }

    /**
     * Score items for a user.
     * @param user The user ID.
     * @param items The score vector.  Its key domain is the items to score, and the scores
     *               (rating predictions) should be written back to this vector.
     */
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        Long2DoubleMap itemMeans = model.getItemMeans();
        Long2DoubleMap ratings = getUserRatingVector(user);

        // **** Normalize the user's ratings by subtracting the item mean from each one.
        for (Map.Entry<Long,Double> rating : ratings.entrySet()) {
            rating.setValue(rating.getValue()-itemMeans.get(rating.getKey()));
        }

        List<Result> results = new ArrayList<>();

        for (long item: items ) {
            // **** Compute the user's score for each item, add it to results
            // get similarity scores for item i
            Long2DoubleMap itemSimilarities = model.getNeighbors(item);
            // exclude this item from list
            itemSimilarities.remove(item);

            // collect values
            // loop over user ratings and collect item similarities
            Map<Long,Double> neighbors = new LinkedHashMap<>();
            for (Map.Entry<Long,Double> rating : ratings.entrySet()) {
                if (itemSimilarities.containsKey(rating.getKey()) && rating.getKey()!=item) {
                    neighbors.put(rating.getKey(), itemSimilarities.get(rating.getKey()));
                }
            }

            // sort and take up to the top 20
            Map<Long,Double> topTwentySortedNeighbors =
                    neighbors.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .limit(neighborhoodSize)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            // calculate predicted score
            double numerator = 0.0;
            double denominator = 0.0;
            for (Map.Entry<Long, Double> entry : topTwentySortedNeighbors.entrySet()) {
                long jItemId = entry.getKey();
                numerator += ratings.get(jItemId) * entry.getValue();
                denominator += entry.getValue();
            }

            // final score for item
            double score = itemMeans.get(item) + (numerator/denominator);
            results.add( Results.create(item, score));
        }

        return Results.newResultMap(results);

    }

    /**
     * Get a user's ratings.
     * @param user The user ID.
     * @return The ratings to retrieve.
     */
    private Long2DoubleOpenHashMap getUserRatingVector(long user) {
        List<Rating> history = dao.query(Rating.class)
                                  .withAttribute(CommonAttributes.USER_ID, user)
                                  .get();

        Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap();
        for (Rating r: history) {
            ratings.put(r.getItemId(), r.getValue());
        }

        return ratings;
    }


}
