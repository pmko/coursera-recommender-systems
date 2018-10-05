package org.lenskit.mooc.nonpers.assoc;

import it.unimi.dsi.fastutil.longs.*;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.inject.Transient;
import org.lenskit.util.IdBox;
import org.lenskit.util.collections.LongUtils;
import org.lenskit.util.io.ObjectStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

/**
 * Build an association rule model using a lift metric.
 */
public class LiftAssociationModelProvider implements Provider<AssociationModel> {
    private static final Logger logger = LoggerFactory.getLogger(LiftAssociationModelProvider.class);
    private final DataAccessObject dao;

    @Inject
    public LiftAssociationModelProvider(@Transient DataAccessObject dao) {
        this.dao = dao;
    }

    @Override
    public AssociationModel get() {
        // First step: map each item to the set of users who have rated it.
        // While we're at it, compute the set of all users.

        // This set contains all users.
        LongSet allUsers = new LongOpenHashSet();

        // This map will map each item ID to the set of users who have rated it.
        Long2ObjectMap<LongSortedSet> itemUsers = new Long2ObjectOpenHashMap<>();

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

            // set up a map to hold the scores for each 'y' item
            Long2DoubleMap itemScores = new Long2DoubleOpenHashMap();

            // loop over the 'y' items
            for (Long2ObjectMap.Entry<LongSortedSet> yEntry: itemUsers.long2ObjectEntrySet()) {
                long yId = yEntry.getLongKey();
                LongSortedSet yUsers = yEntry.getValue();

                // percent users that rated both x and y / percent of users that rated y
                long numIntersectUsers = 0;

                //don't compare the same product IDs and count user IDs that are in both xUsers and yUsers
                if (xId != yId) {
                    //for each yUser look for same userId in xUsers list and count the total intersections
                    for (long yUser: yUsers) if (xUsers.contains(yUser)) numIntersectUsers++;

                    double P_x_intersect_y = (double)numIntersectUsers / (double)numAllUsers;
                    double P_y = (double)xUsers.size() / (double)numAllUsers;
                    double P_x = (double)yUsers.size() / (double)numAllUsers;
                    double P_x_y = P_x_intersect_y / (P_y * P_x);
                    ((Long2DoubleOpenHashMap) itemScores).addTo(yId,P_x_y);
                }
            }

            // save the score map to the main map
            assocMatrix.put(xId, itemScores);

            if (xId==2761) {
                logger.info("631 should be 4.898, calculated as: {}", itemScores.get(631));
                logger.info("2532 should be 4.810, calculated as: {}", itemScores.get(2532));
                logger.info("3615 should be 4.546, calculated as: {}", itemScores.get(3615));
                logger.info("1649 should be 4.490, calculated as: {}", itemScores.get(1649));
                logger.info("340 should be 4.490, calculated as: {}", itemScores.get(340));
                logger.info("1016 should be 4.490, calculated as: {}", itemScores.get(1016));
                logger.info("2439 should be 4.490, calculated as: {}", itemScores.get(2439));
                logger.info("332 should be 4.377, calculated as: {}", itemScores.get(332));
                logger.info("2736 should be 4.329, calculated as: {}", itemScores.get(2736));
                logger.info("3213 should be 4.317, calculated as: {}", itemScores.get(3213));
            }
        }

        return new AssociationModel(assocMatrix);
    }
}
