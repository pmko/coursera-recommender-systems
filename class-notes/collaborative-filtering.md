<u>Cited Papers</u>

- An Algorithmic Framework for Collaborative Filtering  
file: algs.pdf

- Explaining Collaborative Filtering Recommendations  
file: explain-CSCW-20001.pdf

- Item-Based Top-N Recommendation Algorithms  
file: itemrsTOIS04.pdf

### Personalized Collaborative Filtering
Primarily uses a matrix of user/item ratings and utilizes the opinions of others to predict/recommend.  This does not use the item attributes, but just the opinion or tendency towards an item to make predictions and recommendations.

Both approaches below use the same common core, a sparse matrix of ratings.
- predict: fill in missing values
- recommend: select promising cells

_notation_:  
$u$
: user  
$v$
: another user  
$U$
: all users  
$N$
: neighborhood  
$i$
: item  
$I$
: all items  
$r$
: rating  
$w$
: weight  
$s$
: score

### User-User Collaboration
Select neighborhood of similar-taste users and use their opinions of items to predict an item(s) for the user of interest where they have not previously rated or interacted with that item. Compute a similarity score between a user and other users. Then look for users with high similarity scores (neighborhood) that have rated the item of interest or create a list of possible recommendations.  The challenge with this method is that user tastes can change over time and are usually only limited to a category of items, thus taking more compute resources to keep current.

There is a variant of this that uses trusted users versus similar users and then looks at their ratings / tastes to make predictions and recommendations.

_matrix:_ Each column is an item, each row is a user and each cell is a rating for the user/item combination.

This is a correlation-weighted average formula.  Doesn't address the issue that users rate on different scales (e.g user $u$ rating of 2 would be the same as user $v$ rating of 4)
$$
s(u,i) = {\sum_{v \in U} r_{vi} w_{uv} \over \sum_{v \in U} w_{uv}}
$$

```excel
SUMPRODUCT($B$2:$B$6,C2:C6)/SUMIFS($B$2:$B$6,C2:C6,">0")
```

**numerator**: sum of (user v rating * user-user correlation)

**denominator**: sum of correlations for when user v has rated item

$w$ is the weight that user $v$ should be contributing to the prediction for user $u$ and item $i$ (similarity between the users).  how much $v$ should contribute to $u$

$s(u,i)$ is the prediction score for user $u$ and item $i$

####  User-User Collaboration with Normalization

This addresses a few issues present in the previous formula.  Used to predict socre $s$ for user $u$ against item $i$ based on scores for item $i$ by users $v$.  
$$
s(u,i) = \bar{r_u} + {\sum_{v \in U}(r_{vi} - \bar{r}_v) w_{uv} \over \sum_{v \in U} w_{uv}}
$$

$\bar{r_u}$ is the average of all target user $u$ ratings

**numerator**: sum of ((rating user $v$ gave item $i$ minus user $v$ average rating) * correlation value) for all user $v$ who have a rating for item $i$

```excel
IF(ISBLANK(BF$2),0,(BF$2-AVERAGE($C2:$CX2))*$B$2)+IF(ISBLANK(BF$3),0,(BF$3-AVERAGE($C3:$CX3))*$B$3)+IF(ISBLANK(BF$4),0,(BF$4-AVERAGE($C4:$CX4))*$B$4)+IF(ISBLANK(BF$5),0,(BF$5-AVERAGE($C5:$CX5))*$B$5)+IF(ISBLANK(BF$6),0,(BF$6-AVERAGE($C6:$CX6))*$B$6))
```

**denominator**: sum of correlations for when user $v$ has rated item $i$

```excel
SUMIFS(correclations:vector, neighbor_ratings:vector, ">0")
```

Putting it all together:

```java
//sort descending user v similarity scores
Collections.sort(similarityScores, (s1, s2) -> Double.compare(s2.getValue(), s1.getValue()));

//find up to 30 closest neighbors who have rated each item
for (long i : items) {
	//store neighbors and their rating vector
	Map<Long, Long2DoubleOpenHashMap> neighbors = new HashMap<>();

	//loop over users in descending order
	for (ScoreEntry<Long, Double> entry : similarityScores) {
		Long2DoubleOpenHashMap vUserVector = ratingVectors.get(entry.getKey());

        //if the user scored this item and the similarity is not negative, then add to neighbors
		if (vUserVector.get(i) != 0.0 && entry.getValue() >=0 ) {
			neighbors.put(entry.getKey(), vUserVector);
			if (neighbors.size()==this.neighborhoodSize) break;
		}
	}

	//if there are less than 2 then don't score item
	if (neighbors.size()<2) continue;

	//score item and add to results
	double numerator = 0;
	double denominator = 0;

	//sum values by looping over all neighbors
	for (Map.Entry<Long, Long2DoubleOpenHashMap> neighbor : neighbors.entrySet()) {
		long nId = neighbor.getKey();

		//get weight
		double nScore = similarityScores.stream()
            .filter(n -> n.getKey()==nId)
            .findFirst().get()
            .getValue();

        //get adjusting rating for item
		double iScore = ratingVectors.get(nId).get(i);

		numerator += nScore * iScore;
		denominator += nScore;
	}

	double score = targetMean + (numerator/denominator);
	results.add( Results.create(i,score));
}
```

#### User-User Correlation
The weight $w$, can be calculated using the Pearson Correlation Formula to calculate similarity between user's mean-centered ratings for all items.  Mean-centered takes the rating for item $i$ and subtracts the average rating for all items.  

$$
\text{mean centered rating} = r_{ui} - \bar{r}_u
$$
User Profile vectors are constructed out of ratings or interest in a set of items.  Use top-$n$ similar users to identify nearest neighbors with greatest similarity in terms of preference.  

In User-User this similarity is not only used to find others who have the same taste as you across all ratings, but as a way to weight item predictions so that those who are more similar to you have more influence.
$$
w_{uv} = {\sum_{i \in I} (r_{ui} - \bar{r_u})(r_{vi} - \bar{r_v}) \over \sigma _{u}\sigma _{v}}
$$

$$
w_{uv} = {\sum (r_{ui} - \bar{r_u})(r_{vi} - \bar{r_v}) \over \sqrt{\sum (r_{ui} - \bar{r_u})^2} \sqrt{\sum (r_{vi} - \bar{r_u})^2}}
$$

In this case we use euclidean norm for standard deviation.

```excel
CORREL('user-u-ratings'B3:CW3, 'user-v-ratings'B2:B101)
```

```java
//raw user rating vector
Long2DoubleOpenHashMap targetUserVector = getUserRatingVector(user);
//adjusted or mean-centered rating vector
Long2DoubleOpenHashMap adjustedTargetUserVector = new Long2DoubleOpenHashMap();

//build profile for all items, not just the ones that were rated.  do the same thing for user v
for (long i : allItems) {
	if (targetUserVector.containsKey(i)) { //adjust existing value
		adjustedTargetUserVector.put(i,(targetUserVector.get(i)-targetMean));
	} else { //if item has not been rated then add with value of 0
		adjustedTargetUserVector.put(i,0.0);
	}
}

//calculate cosine similarity between targetUser and user v
double similarity = Vectors.dotProduct( adjustedTargetUserVector, adjustedVUserVector ) / (Vectors.euclideanNorm(adjustedTargetUserVector) * Vectors.euclideanNorm( adjustedVUserVector ));
```

_note: There are other ways to calculate the weight.  This is necessary if the data is unary (vector cosine is commonly used) or there is only a small amount of overlap between users._

**Neighborhood**  
This formula is comparing against all users $v \in U$.  Most times we don't want to do that because can add too much noise if we put into our neighborhood people who aren't alike enough.
- don't include user $u$.  ($v \neq u$)
- limit size of users being compared (e.g. no more than 50)
  - in theory, the more the better, assuming good similarity metric
  - 25-100 is common
- minimum similarity  

Select a neighborhood of users $V \subset U$ with highest $w_{uv}$.  
Now our UUCF formula looks like this:

$$
s(u,i) = \bar{r_u} + {\sum_{v \in N}(r_{vi} - \bar{r}_v) \cdot w_{uv} \over \sum_{v \in N} w_{uv}}
$$

_challenges with neighborhoods:_
- minimum similarity -> you may not get very many neighbors
  - noise from dissimilar neighbors decreases usefulness
- limit the size -> you may not have very good similarity  
- What if the correlation is negative?  Then users disagree.
- computation is a bottleneck given $m=|U|$ users and $n=|I|$ items:
    - correlation between two users is $O(n)$
    - all correlations for a user is $O(mn)$
    - all pairwise correlations is $O(m^2n)$

_possible solutions:_
- persistent neighborhoods: update on longer intervals

### Item-Item Collaboration
Precompute similarity between an item and other items using user ratings vector for each item.  Find items that are similar to those that the user has already rated or scored.  This method has efficiencies over user-user because items don't really changes, more about availability of item.

_matrix:_ There is row for each item and a column for each item so that all possible item/item combinations are represented.  Each cell is the similarity score for the item/item combination.

predicting un-normailized score for user $u$ and item $i$ wherer $r_{uj}$ is a users rating for item $j$ and $w_{ij}$ is the similarity score between items $i$ and $j$
$$
s(i,u) = {\sum_{j \in N(i;u)}r_{uj} w_{ij} \over \sum_{j \in N(i;u)} w_{ij}}
$$
```excel
SUMPRODUCT(Matrix!$B18:$U18,Ratings!$B$3:$U$3)/SUMIFS(Matrix!$B18:$U18,Ratings!$B$3:$U$3,">0")
```

#### Item-Item Collaboration with Normaliztion
very similar to user-user except we use item mean to normailze rating and we use the item-item similarity as the weight.  We only sum the similarity weights for items that user $u$ has rating in the denominator.
$$
s(i,u) = {\sum_{j \in I_u}(r_{uj}-\mu_j)w_{ij} \over \sum_{j \in I_u} |w_{ij}|} + \mu_i
$$

```excel
(SUMPRODUCT(NormRatings!$B$3:$U$3,FilterNormMatrix!$B8:$U8)/SUMIFS(FilterNormMatrix!$B8:$U8,Ratings!$B$3:$U$3,">0"))+Ratings!$V$3
```

```java
/**
 * Score items for a user.
 * @param user The user ID.
 * @param items The score vector.  Its key domain is the items to score, and the scores
 *               (rating predictions) should be written back to this vector.
 */
@Override
public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
    Long2DoubleMap itemMeans = model.getItemMeans();
    Long2DoubleMap ratings = getUserRatingVector(user);

    // **** Normalize the user's ratings by subtracting the item mean from each one.
    for (Map.Entry<Long,Double> rating : ratings.entrySet()) {
        rating.setValue(rating.getValue()-itemMeans.get(rating.getKey()));
    }

    List<Result> results = new ArrayList<>();

    for (long item: items ) {
        // **** Compute the user's score for each item, add it to results
        // get similarity scores for item i
        Long2DoubleMap itemSimilarities = model.getNeighbors(item);
        // exclude this item from list
        itemSimilarities.remove(item);

        // collect values
        // loop over user ratings and collect item similarities
        Map<Long,Double> neighbors = new LinkedHashMap<>();
        for (Map.Entry<Long,Double> rating : ratings.entrySet()) {
            if (itemSimilarities.containsKey(rating.getKey()) && rating.getKey()!=item) {
                neighbors.put(rating.getKey(), itemSimilarities.get(rating.getKey()));
            }
        }

        // sort and take up to the top 20
        Map<Long,Double> topTwentySortedNeighbors =
                neighbors.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .limit(neighborhoodSize)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // calculate predicted score
        double numerator = 0.0;
        double denominator = 0.0;
        for (Map.Entry<Long, Double> entry : topTwentySortedNeighbors.entrySet()) {
            long jItemId = entry.getKey();
            numerator += ratings.get(jItemId) * entry.getValue();
            denominator += entry.getValue();
        }

        // final score for item
        double score = itemMeans.get(item) + (numerator/denominator);
        results.add( Results.create(item, score));
    }

    return Results.newResultMap(results);
}
```

#### Item-Item Correlation

Item similarity is computed using the pearson correlation, same as before with users, but now we are substracting item means and not user means to provide normalization for each item rating.
$$
w_{ij} = {\sum_u (r_{ui} - \bar{r_i})(r_{uj} - \bar{r_j}) \over \sqrt{\sum_u (r_{ui} - \bar{r_i})^2} \sqrt{\sum_u (r_{uj} - \bar{r_j})^2}}
$$

**numerator**: sum product of item mean-centered ratings for item $i$ and the item mean centered ratings for item $j$.

**denominator**: square root of the sum of the squared mean-centered ratings for item $i$ multiplied by the square root of the sum of the squared mean-centered ratings for item $j$.  This can also be explained as the Euclidean Norm of the mean-centered rating vector of item $i$ multiplied by the Euclidean Norm of the mean-centered rating vector of item $j$.

```excel
SUMPRODUCT(NormRatings!$C$2:$C$21,NormRatings!B$2:B$21)/(SQRT(SUMSQ(NormRatings!$C$2:$C$21))*SQRT(SUMSQ(NormRatings!B$2:B$21)))
```

```java
public SimpleItemItemModel get() {
    // mean centered item vectors
    Map<Long,Long2DoubleMap> itemVectors = Maps.newHashMap();
    // list of each item's uncentered means
    Long2DoubleMap itemMeans = new Long2DoubleOpenHashMap();

    //get all ratings data
    try (ObjectStream <IdBox <List <Rating>>> stream = dao.query(Rating.class)
                                                       .groupBy(CommonAttributes.ITEM_ID)
                                                       .stream()) {
        // build itemVectors
        for (IdBox<List<Rating>> item : stream) {
            // current item id
            long itemId = item.getId();
            // user ratings for this item
            List<Rating> itemRatings = item.getValue();
            Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap(Ratings.itemRatingVector(itemRatings));

            // Compute and store the item's mean.
            double mean = Vectors.mean(ratings);
            itemMeans.put(itemId, mean);

            // Mean center the ratings.
            for (Map.Entry<Long, Double> entry : ratings.entrySet()) {
                entry.setValue(entry.getValue() - mean);
            }

            itemVectors.put(itemId, LongUtils.frozenMap(ratings));
        }
    }

    // Map items to vectors (maps) of item similarities.
    Map<Long,Long2DoubleMap> itemSimilarities = Maps.newHashMap();

    // **** Compute the similarities between each pair of items

    // loop over itemVectors
    for (Map.Entry<Long, Long2DoubleMap> iEntry : itemVectors.entrySet()) {
        // mean centered ratings for current item i
        Long2DoubleMap iItemRatingVector = iEntry.getValue();
        // item i norm
        double iNorm = Vectors.euclideanNorm(iItemRatingVector);

        // similarity scores between current item and all items in itemVectors
        // this is item i's neighborhood
        Long2DoubleMap simScores = new Long2DoubleOpenHashMap();

        // loop over all items and calc similarities
        for (Map.Entry<Long, Long2DoubleMap> jEntry : itemVectors.entrySet()) {
            // calc the similarity score
            double similarity = Vectors.dotProduct( iItemRatingVector, jEntry.getValue() ) /
                    (iNorm * Vectors.euclideanNorm( jEntry.getValue() ));

            // Ignore non-positive similarities
            if (similarity > 0) simScores.put(jEntry.getKey().longValue(),similarity);
        }

        // stash the item i and its similarity scores to all other items j
        itemSimilarities.put(iEntry.getKey(),simScores);
    }

    return new SimpleItemItemModel(LongUtils.frozenMap(itemMeans), itemSimilarities);
}
```
