package org.lenskit.mooc.cbf;

import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.results.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class TFIDFItemScorer extends AbstractItemScorer {
    private final DataAccessObject dao;
    private final TFIDFModel model;
    private final UserProfileBuilder profileBuilder;
    private static final Logger logger = LoggerFactory.getLogger(TFIDFModelProvider.class);

    /**
     * Construct a new item scorer.  LensKit's dependency injector will call this constructor and
     * provide the appropriate parameters.
     *
     * @param dao The data access object, for looking up users' ratings.
     * @param m   The precomputed model containing the item tag vectors.
     * @param upb The user profile builder for building user tag profiles.
     */
    @Inject
    public TFIDFItemScorer(DataAccessObject dao, TFIDFModel m, UserProfileBuilder upb) {
        this.dao = dao;
        model = m;
        profileBuilder = upb;
    }

    /**
     * Generate item scores personalized for a particular user.  For the TFIDF scorer, this will
     * prepare a user profile and compare it to item tag vectors to produce the score.
     *
     * @param user   The user to score for.
     * @param items  A collection of item ids that should be scored.
     */
    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items){
        // Get the user's ratings
        List<Rating> ratings = dao.query(Rating.class)
                                  .withAttribute(CommonAttributes.USER_ID, user)
                                  .get();

        if (ratings == null) {
            // the user doesn't exist, so return an empty ResultMap
            return Results.newResultMap();
        }

        // Create a place to store the results of our score computations
        List<Result> results = new ArrayList<>();

        // Get the user's profile, which is a vector with their 'like' for each tag
        Map<String, Double> userVector = profileBuilder.makeUserProfile(ratings);

        for (Long item: items) {
            Map<String, Double> iv = model.getItemVector(item);

            // Compute the cosine of this item and the user's profile, store it in the output list
            // If the denominator of the cosine similarity is 0, skip the item

            // find keys that are in iv, but not userVector
            Set<String> diff = new HashSet<String>(iv.keySet());
            diff.removeAll(userVector.keySet());

            // combine diff keys and userVector keys
            Set<String> union = new HashSet<String>(userVector.keySet());
            union.addAll(diff);

            // loop over union and create new item and user vectors to compare
            Map<String,Double> niv = new HashMap<>();
            Map<String,Double> nuv = new HashMap<>();
            for (String key : union) {
                // add to niv
                if (iv.containsKey(key)) niv.put(key, iv.get(key));
                else niv.put(key, 0.0);

                // add to nuv
                if (userVector.containsKey(key)) nuv.put(key, userVector.get(key));
                else nuv.put(key, 0.0);
            }

            //now that we have matching vectors for user and item we can compute the cosine similarity
            double dotProduct = 0.0;
            double normA = 0.0;
            double normB = 0.0;

            Collection<Double> va = nuv.values();
            Double[] vectorA = va.toArray(new Double[va.size()]);
            Collection<Double> vb = niv.values();
            Double[] vectorB = vb.toArray(new Double[vb.size()]);

            for (int i = 0; i < vectorA.length; i++) {
                dotProduct += vectorA[i] * vectorB[i];
                normA += Math.pow(vectorA[i], 2);
                normB += Math.pow(vectorB[i], 2);
            }

            double denominator = (Math.sqrt(normA) * Math.sqrt(normB));
            if (denominator==0) continue;

            double similarity = dotProduct / denominator;

            //output test results
            //if (item==48394) logger.info("48394 (Pan's Labyrinth (Laberinto del fauno, El) (2006)) weighted should be:(0.204)  got: {}", similarity);
            //if (item==1210) logger.info("1210 (Star Wars: Episode VI - Return of the Jedi (1983)) weighted should be:(0.178)  got: {}", similarity);
            //if (item==1089) logger.info("1089 (Reservoir Dogs (1992)) weighted should be:(0.165)  got: {}", similarity);
            /*if (item==32) logger.info("32 (Twelve Monkeys (a.k.a. 12 Monkeys) (1995)) should be:(0.349)  got: {}", similarity);
            if (item==1748) logger.info("1748 (Dark City (1998)) should be:(0.307)  got: {}", similarity);
            if (item==1206) logger.info("1206 (Clockwork Orange, A (1971)) should be:(0.298)  got: {}", similarity);
            if (item==48394) logger.info("48394 (Pan's Labyrinth (Laberinto del fauno, El) (2006)) should be:(0.288)  got: {}", similarity);
            if (item==1199) logger.info("1199 (Brazil (1985)) should be:(0.288)  got: {}", similarity);
            if (item==32587) logger.info("32587 (Sin City (2005)) should be:(0.287)  got: {}", similarity);
            if (item==1270) logger.info("1270 (Back to the Future (1985)) should be:(0.283)  got: {}", similarity);
            if (item==1089) logger.info("1089 (Reservoir Dogs (1992)) should be:(0.278)  got: {}", similarity);
            if (item==1210) logger.info("1210 (Star Wars: Episode VI - Return of the Jedi (1983)) should be:(0.277)  got: {}", similarity);
            if (item==7147) logger.info("7147 (Big Fish (2003)) should be:(0.264)  got: {}", similarity);*/

            results.add(Results.create(item, similarity));
        }

        return Results.newResultMap(results);
    }
}
































































