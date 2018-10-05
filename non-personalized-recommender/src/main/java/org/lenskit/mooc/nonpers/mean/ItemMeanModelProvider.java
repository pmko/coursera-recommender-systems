package org.lenskit.mooc.nonpers.mean;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.lenskit.api.Result;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.ratings.Rating;
import org.lenskit.mooc.nonpers.structures.RatingCount;
import org.lenskit.inject.Transient;
import org.lenskit.util.io.ObjectStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Provider class that builds the mean rating item scorer, computing item means from the
 * ratings in the DAO.
 */
public class ItemMeanModelProvider implements Provider<ItemMeanModel> {
    /**
     * A logger that you can use to emit debug messages.
     */
    private static final Logger logger = LoggerFactory.getLogger(ItemMeanModelProvider.class);

    /**
     * The data access object, to be used when computing the mean ratings.
     */
    private final DataAccessObject dao;

    /**
     * Constructor for the mean item score provider.
     *
     * <p>The {@code @Inject} annotation tells LensKit to use this constructor.
     *
     * @param dao The data access object (DAO), where the builder will get ratings.  The {@code @Transient}
     *            annotation on this parameter means that the DAO will be used to build the model, but the
     *            model will <strong>not</strong> retain a reference to the DAO.  This is standard procedure
     *            for LensKit models.
     */
    @Inject
    public ItemMeanModelProvider(@Transient DataAccessObject dao) {
        this.dao = dao;
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

        try (ObjectStream<Rating> ratings = dao.query(Rating.class).stream()) {
            for (Rating r: ratings) {
                // this loop will run once for each rating in the data set
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
            }
        }

        //calculate mean for specific item id
        //RatingCount rc = ratingsList.stream().filter(o -> 1203==o.getItemId()).findFirst().get();
        //double mean = rc.getSum()/rc.getCount();

        Long2DoubleOpenHashMap means = new Long2DoubleOpenHashMap();

        for (RatingCount rc: ratingsList) {
            double mean = rc.getSum()/rc.getCount();
            long itemId = rc.getItemId();
            means.addTo(itemId,mean);
        }

        logger.info("computed mean ratings for {} items", means.size());
        ItemMeanModel model = new ItemMeanModel(means);
        return model;
    }
}
