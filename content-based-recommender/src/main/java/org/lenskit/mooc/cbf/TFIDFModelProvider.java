package org.lenskit.mooc.cbf;

import it.unimi.dsi.fastutil.longs.LongSet;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonTypes;
import org.lenskit.data.entities.Entity;
import org.lenskit.inject.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

/**
 * Builder for computing {@linkplain TFIDFModel TF-IDF models} from item tag data.  Each item is
 * represented by a normalized TF-IDF vector.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class TFIDFModelProvider implements Provider<TFIDFModel> {
    private static final Logger logger = LoggerFactory.getLogger(TFIDFModelProvider.class);

    private final DataAccessObject dao;

    private double squareSum;

    /**
     * Construct a model builder.  The {@link Inject} annotation on this constructor tells LensKit
     * that it can be used to build the model builder.
     *
     * @param dao The data access object.
     */
    @Inject
    public TFIDFModelProvider(@Transient DataAccessObject dao) {
        this.dao = dao;
    }

    /**
     * This method is where the model should actually be computed.
     * @return The TF-IDF model (a model of item tag vectors).
     */
    @Override
    public TFIDFModel get() {
        logger.info("Building TF-IDF model");

        // Create a map to accumulate document frequencies for the IDF computation
        Map<String, Double> docFreq = new HashMap<>();

        // We now proceed in 2 stages. First, we build a TF vector for each item.
        // While we do this, we also build the DF vector.
        // We will then apply the IDF to each TF vector and normalize it to a unit vector.

        // Create a map to store the item TF vectors.
        Map<Long, Map<String, Double>> itemVectors = new HashMap<>();

        // Iterate over the items to compute each item's vector.
        LongSet items = dao.getEntityIds(CommonTypes.ITEM);
        for (long item : items) {
            // Create a work vector to accumulate this item's tag vector.
            Map<String, Double> work = new HashMap<>();

            for (Entity tagApplication : dao.query(TagData.ITEM_TAG_TYPE)
                                            .withAttribute(TagData.ITEM_ID, item)
                                            .get()) {
                String tag = tagApplication.get(TagData.TAG);

                if (work.containsKey(tag)) {  //if the tag is already in the work map
                    work.put(tag, work.get(tag)+1);
                } else {  // if not then add it
                    work.put(tag,1.0);

                    //increment DF for the tag and this item
                    if (docFreq.containsKey(tag)) { //if already in DF, increment count
                        docFreq.put(tag, docFreq.get(tag)+1);
                    } else { // if not then add it
                        docFreq.put(tag,1.0);
                    }
                }
            }

            itemVectors.put(item, work);
        }

        logger.info("Computed TF vectors for {} items", itemVectors.size());

        // Now we've seen all the items, so we have each item's TF vector and a global vector
        // of document frequencies.
        // Invert and log the document frequency.  We can do this in-place.
        final double logN = Math.log(items.size());

        for (Map.Entry<String, Double> e : docFreq.entrySet()) {
            e.setValue(logN - Math.log(e.getValue()));
        }

        // Now docFreq is a log-IDF vector.  Its values can therefore be multiplied by TF values.
        // So we can use it to apply IDF to each item vector to put it in the final model.
        // Create a map to store the final model data.
        Map<Long, Map<String, Double>> modelData = new HashMap<>();
        for (Map.Entry<Long, Map<String, Double>> entry : itemVectors.entrySet()) {
            Map<String, Double> tv = new HashMap<>(entry.getValue());

            // multiply each term value by the log of the document frequency
            tv.replaceAll((k, v) -> v * docFreq.get(k));

            // Normalize it by dividing each element by its Euclidean norm, which is the
            // square root of the sum of the squares of the values.

            //compute the length (Euclidean norm) of the TF-IDF vector
            squareSum = 0.0;
            Collection<Double> values = tv.values();
            for (Double v : values) {
                squareSum += Math.pow(v, 2);
            }

            final double length = Math.sqrt(squareSum);

            //divide each element of it by the length to yield a unit vector
            for (Map.Entry<String, Double> e : tv.entrySet()) {
                e.setValue(e.getValue() / length);
            }

            modelData.put(entry.getKey(), tv);

            /*if (entry.getKey()==2231) {
                logger.info("cards: " + tv.get("cards"));
                logger.info("John Malkovich should be:(0.23970853283741034)  got: " + tv.get("John Malkovich"));
                logger.info("Matt Damon should be:(0.3159509700099918)  got: " + tv.get("Matt Damon"));
                logger.info("poker should be:(0.5969723646885582)  got: " + tv.get("poker"));
                logger.info("gambling should be:(0.24572008949856772)  got: " + tv.get("gambling"));
                logger.info("library vhs should be:(0.06053343286414036)  got: " + tv.get("library vhs"));
                logger.info("card games should be:(0.12526237038499663)  got: " + tv.get("card games"));
                logger.info("2.5 should be:(0.06789050985047351)  got: " + tv.get("2.5"));
                logger.info("John Turturro should be:(0.2606167586076272)  got: " + tv.get("John Turturro"));
                logger.info("Edward Norton should be:(0.5529141975174856)  got: " + tv.get("Edward Norton"));
                logger.info("John Dahl should be:(0.1141651377329722)  got: " + tv.get("John Dahl"));
                logger.info("watched 2006 should be:(0.061105276598972115)  got: " + tv.get("watched 2006"));
                logger.info("END");
            }*/
        }

        // We don't need the IDF vector anymore, as long as as we have no new tags
        return new TFIDFModel(modelData);
    }
}
