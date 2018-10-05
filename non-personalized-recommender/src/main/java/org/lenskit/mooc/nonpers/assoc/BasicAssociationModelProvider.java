package org.lenskit.mooc.nonpers.assoc;

import it.unimi.dsi.fastutil.longs.*;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.inject.Transient;
import org.lenskit.mooc.nonpers.mean.ItemMeanModelProvider;
import org.lenskit.util.IdBox;
import org.lenskit.util.collections.LongUtils;
import org.lenskit.util.io.ObjectStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

/**
 * Build a model for basic association rules.  This class computes the association for all pairs of items.
 */
public class BasicAssociationModelProvider implements Provider<AssociationModel> {
    /**
     * A logger that you can use to emit debug messages.
     */
    private static final Logger logger = LoggerFactory.getLogger(ItemMeanModelProvider.class);

    /**
     * The data access object, to be used when computing the mean ratings.
     */
    private final DataAccessObject dao;

    @Inject
    public BasicAssociationModelProvider(@Transient DataAccessObject dao) {
        this.dao = dao;
    }

    @Override
    public AssociationModel get() {
        // First step: map each item to the set of users who have rated it.

        // This map will map each item ID to the set of users who have rated it.
        Long2ObjectMap<LongSortedSet> itemUsers = new Long2ObjectOpenHashMap<>();
        LongSet allUsers = new LongOpenHashSet();

        // Open a stream, grouping ratings by item ID
        try (ObjectStream<IdBox<List<Rating>>> ratingStream = dao.query(Rating.class)
                                                                 .groupBy(CommonAttributes.ITEM_ID)
                                                                 .stream()) {
            // Process each item's ratings
            for (IdBox<List<Rating>> item: ratingStream) {
                // Build a set of users.  We build an array first, then convert to a set.
                LongList users = new LongArrayList();
                // Add each rating's user ID to the user sets
                for (Rating r: item.getValue()) {
                    long user = r.getUserId();
                    users.add(user);
                    allUsers.add(user);
                }
                // put this item's user set into the item user map
                // a frozen set will be very efficient later
                itemUsers.put(item.getId(), LongUtils.frozenSet(users));
            }
        }

        long numAllUsers = allUsers.size();

        // Second step: compute all association rules

        // We need a map to store them
        Long2ObjectMap<Long2DoubleMap> assocMatrix = new Long2ObjectOpenHashMap<>();

        // then loop over 'x' items
        for (Long2ObjectMap.Entry<LongSortedSet> xEntry: itemUsers.long2ObjectEntrySet()) {
            long xId = xEntry.getLongKey();
            LongSortedSet xUsers = xEntry.getValue();

            // set up a map to hold the scores for each 'y' item for this 'x'
            Long2DoubleMap itemScores = new Long2DoubleOpenHashMap();

            // loop over the 'y' items
            for (Long2ObjectMap.Entry<LongSortedSet> yEntry: itemUsers.long2ObjectEntrySet()) {
                long yId = yEntry.getLongKey();
                LongSortedSet yUsers = yEntry.getValue();

                // percent users that rated both x and y / percent of users that rated x
                long numIntersectUsers = 0;

                //don't compare the same product IDs and count user IDs that are in both xUsers and yUsers
                if (xId != yId) {
                    //for each yUser look for same userId in xUsers list and count the total intersections
                    for (long yUser: yUsers) if (xUsers.contains(yUser)) numIntersectUsers++;

                    double P_x_intersect_y = (double)numIntersectUsers / (double)numAllUsers;
                    double P_x = (double)xUsers.size() / (double)numAllUsers;
                    double P_x_y = P_x_intersect_y / P_x;
                    ((Long2DoubleOpenHashMap) itemScores).addTo(yId,P_x_y);
                }
            }

            // save the score map to the main map
            assocMatrix.put(xId, itemScores);
            /*if (xId==260) {
                logger.info("2571 should be 0.916, calculated as: {}", itemScores.get(2571));
                logger.info("1196 should be 0.899, calculated as: {}", itemScores.get(1196));
                logger.info("4993 should be 0.892, calculated as: {}", itemScores.get(4993));
                logger.info("1210 should be 0.847, calculated as: {}", itemScores.get(1210));
                logger.info("356 should be 0.843, calculated as: {}", itemScores.get(356));
                logger.info("5952 should be 0.841, calculated as: {}", itemScores.get(5952));
                logger.info("7153 should be 0.830, calculated as: {}", itemScores.get(7153));
                logger.info("296 should be 0.828, calculated as: {}", itemScores.get(296));
                logger.info("1198 should be 0.791, calculated as: {}", itemScores.get(1198));
                logger.info("480 should be 0.789, calculated as: {}", itemScores.get(480));
            }*/
        }

        return new AssociationModel(assocMatrix);
    }
}
