package org.lenskit.mooc.uu;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.entities.CommonTypes;
import org.lenskit.results.Results;
import org.lenskit.util.math.Vectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User-user item scorer.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleUserUserItemScorer extends AbstractItemScorer {
    private final DataAccessObject dao;
    private final int neighborhoodSize;
    private static final Logger logger = LoggerFactory.getLogger(SimpleUserUserItemScorer.class);

    /**
     * Instantiate a new user-user item scorer.
     * @param dao The data access object.
     */
    @Inject
    public SimpleUserUserItemScorer(DataAccessObject dao) {
        this.dao = dao;
        this.neighborhoodSize = 30;
    }

    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        // TODO Score the items for the user with user-user CF
        List<Result> results = new LinkedList<>();

        //list of all items
        LongSet allItems = this.dao.getEntityIds(CommonTypes.ITEM);

        //user rating vector
        Long2DoubleOpenHashMap targetUserVector = getUserRatingVector(user);
        Long2DoubleOpenHashMap adjustedTargetUserVector = new Long2DoubleOpenHashMap();

        //get target user mean rating
        double targetMean = Vectors.mean(targetUserVector);

        //build profile for all 2500 items
        for (long i : allItems) {
            if (targetUserVector.containsKey(i)) { //adjust existing value
                adjustedTargetUserVector.put(i,(targetUserVector.get(i)-targetMean));
            } else { //if item has not been rated then add with value of 0
                adjustedTargetUserVector.put(i,0.0);
            }
        }

        //get target user norm
        double targetNorm = Vectors.euclideanNorm(adjustedTargetUserVector);

        //get all users
        LongSet users = this.dao.getEntityIds(CommonTypes.USER);

        //keep a list of all adjusted user ratings
        Map<Long, Long2DoubleOpenHashMap> ratingVectors = new HashMap<>();

        //store similarity scores for target user against all other users
        List<ScoreEntry<Long, Double>> similarityScores = new LinkedList<>();

        //loop over users and adjust each user’s rating vector by subtracting that user’s mean rating from each of their ratings
        for (long v : users) {
            //if v is targetUser then skip
            if (v==user) continue;

            //get current user's ratings
            Long2DoubleOpenHashMap vUserVector = getUserRatingVector(v);
            Long2DoubleOpenHashMap adjustedVUserVector = new Long2DoubleOpenHashMap();

            //get user mean rating
            double userMean = Vectors.mean(vUserVector);

            //create adjusted user vector
            for (long i : allItems) {
                if (vUserVector.containsKey(i)) {
                    adjustedVUserVector.put(i,(vUserVector.get(i)-userMean));
                } else {
                    adjustedVUserVector.put(i,0.0);
                }
            }
            //save for later
            ratingVectors.put(v,adjustedVUserVector);

            //calculate cosine similarity between targetUser and this user v
            double similarity = Vectors.dotProduct( adjustedTargetUserVector, adjustedVUserVector ) /
                    (targetNorm * Vectors.euclideanNorm( adjustedVUserVector ));

            //add to list
            similarityScores.add(new ScoreEntry<>(v, similarity));
        }

        //sort descending
        Collections.sort(similarityScores, (s1, s2) -> Double.compare(s2.getValue(), s1.getValue()));

        //find up to 30 closest neighbors who have rated each item
        for (long i : items) {
            //store neighbors and their rating vector
            Map<Long, Long2DoubleOpenHashMap> neighbors = new HashMap<>();

            //loop over users in descending order
            for (ScoreEntry<Long, Double> entry : similarityScores) {

                Long2DoubleOpenHashMap vUserVector = ratingVectors.get(entry.getKey());
                if (vUserVector.get(i) != 0.0 && entry.getValue() >=0 ) { //if the user scored this item and the similarity is not negative, then add to neighbors
                    neighbors.put(entry.getKey(), vUserVector);

                    if (neighbors.size()==this.neighborhoodSize) break;
                }
            }

            //if there are less than 2 then don't score item
            if (neighbors.size()<2) {
                continue;
            }

            //score item and add to results
            double numerator = 0;
            double denominator = 0;

            //sum values
            //loop over all neighbors
            for (Map.Entry<Long, Long2DoubleOpenHashMap> neighbor : neighbors.entrySet()) {
                long nId = neighbor.getKey();
                //get weight
                double nScore = similarityScores.stream().filter(n -> n.getKey()==nId).findFirst().get().getValue();
                //get adjusting rating for item
                double iScore = ratingVectors.get(nId).get(i);

                numerator += nScore * iScore;
                denominator += nScore;
            }

            double score = targetMean + (numerator/denominator);

            results.add( Results.create(i,score));
        }

        return Results.newResultMap( results );
    }

    /**
     * Get a user's rating vector.
     * @param user The user ID.
     * @return The rating vector, mapping item IDs to the user's rating
     *         for that item.
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
