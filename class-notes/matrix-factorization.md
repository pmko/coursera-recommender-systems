# Linear Blending Hybrid

linear blending hybrid item scorer formula.  Each scorer (lef and right) can be implemented by any algorithm.
$$
s(u,i)=b_{ui} + (1-\beta)(s_{left}(u,i)-b_{ui})+\beta(s_{right}(u,i)-b_{ui})
$$
baseline is calculated with the following formula where $b$ is the global mean or intercept, $b_{i}$ is the item bias or mean and $b_{u}$ is the user bias or mean.
$$
b_{ui} = b + b_{u} + b_{i}
$$

java implementation:
```java
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
```
# Logistic Recommender
