package org.lenskit.mooc.ii;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.apache.commons.lang3.tuple.Pair;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.entities.CommonTypes;
import org.lenskit.data.entities.EntityType;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.ratings.Ratings;
import org.lenskit.inject.Transient;
import org.lenskit.util.IdBox;
import org.lenskit.util.collections.LongUtils;
import org.lenskit.util.io.ObjectStream;
import org.lenskit.util.math.Vectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleItemItemModelProvider implements Provider<SimpleItemItemModel> {
    private static final Logger logger = LoggerFactory.getLogger(SimpleItemItemModelProvider.class);

    private final DataAccessObject dao;

    /**
     * Construct the model provider.
     * @param dao The data access object.
     */
    @Inject
    public SimpleItemItemModelProvider(@Transient DataAccessObject dao) {
        this.dao = dao;
    }

    /**
     * Construct the item-item model.
     * @return The item-item model.
     */
    @Override
    public SimpleItemItemModel get() {
        // mean centered item vectors
        Map<Long,Long2DoubleMap> itemVectors = Maps.newHashMap();
        // list of each item's uncentered means
        Long2DoubleMap itemMeans = new Long2DoubleOpenHashMap();

        //get all ratings data
        try (ObjectStream<IdBox<List<Rating>>> stream = dao.query(Rating.class)
                                                           .groupBy(CommonAttributes.ITEM_ID)
                                                           .stream()) {
            // build itemVectors
            for (IdBox<List<Rating>> item : stream) {
                // current item id
                long itemId = item.getId();
                // user ratings for this item
                List<Rating> itemRatings = item.getValue();
                Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap(Ratings.itemRatingVector(itemRatings));

                // Compute and store the item's mean.
                double mean = Vectors.mean(ratings);
                itemMeans.put(itemId, mean);

                // Mean center the ratings.
                for (Map.Entry<Long, Double> entry : ratings.entrySet()) {
                    entry.setValue(entry.getValue() - mean);
                }

                itemVectors.put(itemId, LongUtils.frozenMap(ratings));
            }
        }

        // Map items to vectors (maps) of item similarities.
        Map<Long,Long2DoubleMap> itemSimilarities = Maps.newHashMap();

        // **** Compute the similarities between each pair of items

        // loop over itemVectors
        for (Map.Entry<Long, Long2DoubleMap> iEntry : itemVectors.entrySet()) {
            // mean centered ratings for current item i
            Long2DoubleMap iItemRatingVector = iEntry.getValue();
            // item i norm
            double iNorm = Vectors.euclideanNorm(iItemRatingVector);

            // similarity scores between current item and all items in itemVectors
            // this is item i's neighborhood
            Long2DoubleMap simScores = new Long2DoubleOpenHashMap();

            // loop over all items and calc similarities
            for (Map.Entry<Long, Long2DoubleMap> jEntry : itemVectors.entrySet()) {
                // calc the similarity score
                double similarity = Vectors.dotProduct( iItemRatingVector, jEntry.getValue() ) /
                        (iNorm * Vectors.euclideanNorm( jEntry.getValue() ));

                // Ignore non-positive similarities
                if (similarity > 0) simScores.put(jEntry.getKey().longValue(),similarity);
            }

            // stash the item i and its similarity scores to all other items j
            itemSimilarities.put(iEntry.getKey(),simScores);
        }

        return new SimpleItemItemModel(LongUtils.frozenMap(itemMeans), itemSimilarities);
    }
}
