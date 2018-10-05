package org.lenskit.mooc.cbf;

import org.lenskit.data.ratings.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Build a user profile from all positive ratings.
 */
public class ThresholdUserProfileBuilder implements UserProfileBuilder {
    /**
     * The lowest rating that will be considered in the user's profile.
     */
    private static final double RATING_THRESHOLD = 3.5;

    /**
     * The tag model, to get item tag vectors.
     */
    private final TFIDFModel model;

    @Inject
    public ThresholdUserProfileBuilder(TFIDFModel m) {
        model = m;
    }

    @Override
    public Map<String, Double> makeUserProfile(@Nonnull List<Rating> ratings) {
        // Create a new vector over tags to accumulate the user profile
        Map<String,Double> profile = new HashMap<>();

        // Iterate over the user's ratings to build their profile
        for (Rating r: ratings) {
            if (r.getValue() >= RATING_THRESHOLD) {

                //add each of these tags to the profile
                Map<String, Double> iv = model.getItemVector(r.getItemId());

                //loop over iv
                for (Map.Entry<String,Double> e : iv.entrySet()) {
                    if (profile.containsKey(e.getKey())) {  //check to see if it exists in profile
                        //if so then add the value
                        profile.put(e.getKey(), profile.get(e.getKey()) + e.getValue());
                    } else {  //if not then create with value
                        profile.put(e.getKey(), e.getValue());
                    }
                }
            }
        }

        // The profile is accumulated, return it.
        return profile;
    }
}
