package org.lenskit.mooc.hybrid;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.api.ItemScorer;
import org.lenskit.api.Result;
import org.lenskit.bias.BiasModel;
import org.lenskit.bias.UserBiasModel;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.ratings.RatingSummary;
import org.lenskit.inject.Transient;
import org.lenskit.util.ProgressLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;

/**
 * Trainer that builds logistic models.
 */
public class LogisticModelProvider implements Provider<LogisticModel> {
    private static final Logger logger = LoggerFactory.getLogger(LogisticModelProvider.class);
    private static final double LEARNING_RATE = 0.00005;
    private static final int ITERATION_COUNT = 100;

    private final LogisticTrainingSplit dataSplit;
    private final BiasModel baseline;
    private final RecommenderList recommenders;
    private final RatingSummary ratingSummary;
    private final int parameterCount;
    private final Random random;

    @Inject
    public LogisticModelProvider(@Transient LogisticTrainingSplit split,
                                 @Transient UserBiasModel bias,
                                 @Transient RecommenderList recs,
                                 @Transient RatingSummary rs,
                                 @Transient Random rng) {
        dataSplit = split;
        baseline = bias;
        recommenders = recs;
        ratingSummary = rs;
        parameterCount = 1 + recommenders.getRecommenderCount() + 1;
        random = rng;
    }

    @Override
    public LogisticModel get() {
        List<ItemScorer> scorers = recommenders.getItemScorers();
        double intercept = 0;
        double[] params = new double[parameterCount];

        LogisticModel current = LogisticModel.create(intercept, params);

        // TODO Implement model training
        // get the tuning ratings
        List<Rating> ratings = dataSplit.getTuneRatings();

        // scorers
        List<ItemScorer> itemscorers = recommenders.getItemScorers();

        // score cache
        // Map<scorer,Map<userID,<item,score>>>
        Map<ItemScorer,Map<Long,Long2DoubleOpenHashMap>> cache = new HashMap<>();

        // run through the iterations
        for(int i = 0;i < ITERATION_COUNT;i ++) {
            Collections.shuffle(ratings);

            for(Rating rating:ratings) {
                // explanatory variable values
                double[] vars = new double[parameterCount];

                // this is x1
                double userItemBias = baseline.getIntercept() + baseline.getUserBias(rating.getUserId()) + baseline.getItemBias(rating.getItemId());
                vars[0] = userItemBias;
                // this is x2
                double logPopularity = Math.log10(ratingSummary.getItemRatingCount(rating.getItemId()));
                vars[1] = logPopularity;

                // x3 through xN
                int x = 2;
                for (ItemScorer scorer : itemscorers) {
                    if (i==0) {
                        double score = 0;
                        vars[x] = 0;
                        if (scorer.score(rating.getUserId(), rating.getItemId()) != null) {
                            score = scorer.score(rating.getUserId(), rating.getItemId()).getScore();
                            vars[x] = score - userItemBias;
                        }

                        if (cache.containsKey(scorer)) {
                            Map<Long,Long2DoubleOpenHashMap> scorerUserItemScores = cache.get(scorer);
                            if (scorerUserItemScores.containsKey(rating.getUserId())) {
                                Long2DoubleOpenHashMap userScores = scorerUserItemScores.get(rating.getUserId());
                                userScores.put(rating.getItemId(),score);
                            } else {
                                Long2DoubleOpenHashMap userScores = new Long2DoubleOpenHashMap();
                                userScores.put(rating.getItemId(),score);
                                scorerUserItemScores.put(rating.getUserId(),userScores);
                            }
                        } else {
                            Long2DoubleOpenHashMap userScores = new Long2DoubleOpenHashMap();
                            userScores.put(rating.getItemId(),score);
                            Map<Long,Long2DoubleOpenHashMap> scorerUserItemScores = new HashMap<>();
                            scorerUserItemScores.put(rating.getUserId(),userScores);
                            cache.put(scorer,scorerUserItemScores);
                        }
                    } else {
                        Map<Long,Long2DoubleOpenHashMap> scorerScores = cache.get(scorer);
                        vars[x] = scorerScores.get(rating.getUserId()).get(rating.getItemId()) - userItemBias;
                    }
                    x++;
                }

                // update the intercept
                intercept += LEARNING_RATE * rating.getValue() * current.evaluate(-rating.getValue(), vars);

                // update the params
                for (int k = 0; k < parameterCount; k++) {
                    params[k] += LEARNING_RATE * rating.getValue() * vars[k] * current.evaluate(-rating.getValue(), vars);
                }
            }
            // update before the next iteration
            current = LogisticModel.create(intercept, params);
        }

        return current;
    }

}