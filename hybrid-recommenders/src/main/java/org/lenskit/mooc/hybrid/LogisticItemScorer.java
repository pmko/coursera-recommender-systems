package org.lenskit.mooc.hybrid;

import it.unimi.dsi.fastutil.longs.LongSet;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.lenskit.api.ItemScorer;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.bias.BiasModel;
import org.lenskit.bias.UserBiasModel;
import org.lenskit.data.ratings.RatingSummary;
import org.lenskit.results.Results;
import org.lenskit.util.collections.LongUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Item scorer that does a logistic blend of a subsidiary item scorer and popularity.  It tries to predict
 * whether a user has rated a particular item.
 */
public class LogisticItemScorer extends AbstractItemScorer {
    private static final Logger logger = LoggerFactory.getLogger(LogisticModelProvider.class);
    private final LogisticModel logisticModel;
    private final BiasModel biasModel;
    private final RecommenderList recommenders;
    private final RatingSummary ratingSummary;

    @Inject
    public LogisticItemScorer(LogisticModel model, UserBiasModel bias, RecommenderList recs, RatingSummary rs) {
        logisticModel = model;
        biasModel = bias;
        recommenders = recs;
        ratingSummary = rs;
    }

    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        // TODO Implement item scorer

        List<ItemScorer> itemscorers = recommenders.getItemScorers();
        double[]vars = new double[recommenders.getRecommenderCount() + 2];
        LongSet itemSet = LongUtils.asLongSet(items);
        List<Result> results = new ArrayList<>();

        for (long item : itemSet) {
            double userItemBias = biasModel.getIntercept() + biasModel.getUserBias(user) + biasModel.getItemBias(item);
            vars[0] = userItemBias;
            double logPopularity = Math.log10(ratingSummary.getItemRatingCount(item));
            vars[1] = logPopularity;

            int x = 2;
            for(ItemScorer scorer:itemscorers) {
                if(scorer.score(user, item) != null) {
                    vars[x] = scorer.score(user, item).getScore() - userItemBias;
                } else {
                    vars[x] = 0;
                }
                x++;
            }

            RealVector coef = logisticModel.getCoefficients();
            int vLength = coef.getDimension();
            double score = logisticModel.getIntercept() + coef.dotProduct(new ArrayRealVector(vars, false));

            double finalScore = LogisticModel.sigmoid(score);

            if (item==318) {
                logger.info("318: {}",finalScore);
            }

            results.add(Results.create(item, finalScore));
        }

        return Results.newResultMap(results);
    }
}
