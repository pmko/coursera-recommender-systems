package org.lenskit.mooc.nonpers.mean;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.lenskit.baseline.MeanDamping;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.ratings.Rating;
import org.lenskit.inject.Transient;
import org.lenskit.mooc.nonpers.structures.RatingCount;
import org.lenskit.util.io.ObjectStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

/**
 * Provider class that builds the mean rating item scorer, computing damped item means from the
 * ratings in the DAO.
 */
public class DampedItemMeanModelProvider implements Provider<ItemMeanModel> {
    /**
     * A logger that you can use to emit debug messages.
     */
    private static final Logger logger = LoggerFactory.getLogger(DampedItemMeanModelProvider.class);

    /**
     * The data access object, to be used when computing the mean ratings.
     */
    private final DataAccessObject dao;
    /**
     * The damping factor.
     */
    private final double damping;

    /**
     * Constructor for the mean item score provider.
     *
     * <p>The {@code @Inject} annotation tells LensKit to use this constructor.
     *
     * @param dao The data access object (DAO), where the builder will get ratings.  The {@code @Transient}
     *            annotation on this parameter means that the DAO will be used to build the model, but the
     *            model will <strong>not</strong> retain a reference to the DAO.  This is standard procedure
     *            for LensKit models.
     * @param damping The damping factor for Bayesian damping.  This is number of fake global-mean ratings to
     *                assume.  It is provided as a parameter so that it can be reconfigured.  See the file
     *                {@code damped-mean.groovy} for how it is used.
     */
    @Inject
    public DampedItemMeanModelProvider(@Transient DataAccessObject dao,
                                       @MeanDamping double damping) {
        this.dao = dao;
        this.damping = damping;
    }

    /**
     * Construct an item mean model.
     *
     * <p>The {@link Provider#get()} method constructs whatever object the provider class is intended to build.</p>
     *
     * @return The item mean model with mean ratings for all items.
     */
    @Override
    public ItemMeanModel get() {
        List<RatingCount> ratingsList = new ArrayList<RatingCount>();
        double globalCount = 0;
        double globalSum = 0;

        try (ObjectStream<Rating> ratings = dao.query(Rating.class).stream()) {
            for (Rating r: ratings) {
                //count and sum for each item
                long itemID = r.getItemId();

                //check if rating count for item exists in ratingsList
                if (ratingsList.stream().filter(o -> itemID==o.getItemId()).findFirst().isPresent() ) {
                    RatingCount rc = ratingsList.stream().filter(o -> itemID==o.getItemId()).findFirst().get();
                    rc.incrementCount();
                    rc.runningTotal(r.getValue());
                } else {
                    RatingCount rc = new RatingCount();
                    rc.setItemId(r.getItemId());
                    rc.setCount(1);
                    rc.setSum(r.getValue());
                    ratingsList.add(rc);
                }

                //count and sum for all items
                globalCount += 1;
                globalSum += r.getValue();
            }
        }

        //compute global mean
        double gMean = globalSum/globalCount;

        Long2DoubleOpenHashMap means = new Long2DoubleOpenHashMap();

        //compute damped mean for each item
        for (RatingCount rc: ratingsList) {
            long count = rc.getCount();
            double sum = rc.getSum();
            double dampedMean = (sum + (gMean * this.damping)) / (count + this.damping);
            means.addTo(rc.getItemId(),dampedMean);
        }

        logger.info("computed mean ratings for {} items", means.size());
        ItemMeanModel model = new ItemMeanModel(means);
        return model;
    }
}
