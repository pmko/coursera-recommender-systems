package org.lenskit.mooc.cbf;

import org.lenskit.data.ratings.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Build a user profile from all positive ratings.
 */
public class WeightedUserProfileBuilder implements UserProfileBuilder {
    /**
     * The tag model, to get item tag vectors.
     */
    private final TFIDFModel model;
    private double rSum;
    private static final Logger logger = LoggerFactory.getLogger(TFIDFModelProvider.class);

    @Inject
    public WeightedUserProfileBuilder(TFIDFModel m) {
        model = m;
    }

    @Override
    public Map<String, Double> makeUserProfile(@Nonnull List<Rating> ratings) {
        // Create a new vector over tags to accumulate the user profile
        Map<String,Double> profile = new HashMap<>();

        // TODO Normalize the user's ratings
        // get global mean of user's ratings
        rSum = 0.0;
        for (Rating r: ratings) rSum += r.getValue();
        double grm = rSum / ratings.size();

        //calculate normalized ratings
        Map<Long,Double> nRatings = new HashMap<>();
        for (Rating r: ratings) {
            nRatings.put(r.getItemId(),r.getValue()-grm);
        }

        // TODO Build the user's weighted profile
        // Iterate over the user's ratings to build their profile
        for (Rating r: ratings) {
            //get the tags for the rated item
            Map<String, Double> iv = model.getItemVector(r.getItemId());

            // User u's rating for item i minus the global mean of all user ratings multiplied by value for a particular tag t
            // r.getValue()

            //loop over iv and calculate unweighted profile vector
            for (Map.Entry<String,Double> e : iv.entrySet()) {
                double wv = e.getValue() * nRatings.get(r.getItemId());
                if (profile.containsKey(e.getKey())) {  //check to see if it exists in profile
                    //if so then add the value
                    profile.put(e.getKey(), profile.get(e.getKey()) + wv);
                } else {  //if not then create with value
                    profile.put(e.getKey(), wv);
                }
            }
        }

        // The profile is accumulated, return it.
        return profile;
    }
}
