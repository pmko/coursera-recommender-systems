package org.lenskit.mooc.hybrid;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.lenskit.api.ItemScorer;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.bias.BiasModel;
import org.lenskit.results.Results;
import org.lenskit.util.collections.LongUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Item scorer that computes a linear blend of two scorers' scores.
 *
 * <p>This scorer takes two underlying scorers and blends their scores.
 */
public class LinearBlendItemScorer extends AbstractItemScorer {
    private final BiasModel biasModel;
    private final ItemScorer leftScorer, rightScorer;
    private final double blendWeight;

    /**
     * Construct a popularity-blending item scorer.
     *
     * @param bias The baseline bias model to use.
     * @param left The first item scorer to use.
     * @param right The second item scorer to use.
     * @param weight The weight to give popularity when ranking.
     */
    @Inject
    public LinearBlendItemScorer(BiasModel bias,
                                 @Left ItemScorer left,
                                 @Right ItemScorer right,
                                 @BlendWeight double weight) {
        Preconditions.checkArgument(weight >= 0 && weight <= 1, "weight out of range");
        biasModel = bias;
        leftScorer = left;
        rightScorer = right;
        blendWeight = weight;
    }

    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        List<Result> results = new ArrayList<>();
        LongSet itemSet = LongUtils.asLongSet(items);

        // **** Compute hybrid scores
        // loop over items
        for (long item : itemSet) {
            // get user bias
            double bias = biasModel.getIntercept() + biasModel.getUserBias(user) + biasModel.getItemBias(item);

            // grab scores for item from each scorer
            double leftScore = 0;
            if (leftScorer.score(user,item) != null) leftScore = leftScorer.score(user,item).getScore() - bias;
            double rightScore = 0;
            if (rightScorer.score(user,item) != null) rightScore = rightScorer.score(user,item).getScore() - bias;

            // compute hybrid score
            double hybridScore = bias + ((1 - blendWeight)*leftScore) + (blendWeight*rightScore);

            // stash the result
            results.add(Results.create(item,hybridScore));
        }

        return Results.newResultMap(results);
    }
}
