# Non-Personalized and Content-Based

**Dimensions of Analysis**
- Domain
- Purpose
- Recommendation Context
- Whose Opinions
- Personalization Level
- Privacy and Trustworthiness
- Interfaces
- Recommendation Algorithms

**Level of Personalization**
- Generic / Non-Personalized
- Everyone receives same recommendations
- Demographic
- Matches a target group
- Ephemeral (short duration)
- Matches current activity
- Persistent
- Matches long-term interests

**Types of Output**
- Predictions
- Recommendations
- Filtering
- Organic vs. explicit presentation
- Agent/Discussion Interface

**Types of Input**
- Explicit
- Implicit

**Recommendation Algorithms**
- Non-Personalized Summary Statistics
- Content-Based Filtering
- Collaborative Filtering
  - User-User
  - Item-Item
  - Dimensionality Reduction
- Hybrid

### Non-Personalized
Works well when we don't know anything about the user (cold start problem).  Simple but beneficial, efficient and fast to compute.

#### Summary Statistics
Data from the community can be used to identify best sellers, most popular and trending.  By aggregating behavior (frequency, average) we can get a sense for what the community is doing.

- consumed: most / longest visited
- popularity (can be an important metric)
- count: quantity sold or accepted
- average: rating for an item

__things to consider:__
- averages can be misleading.
- people rate on different scales
    - consider normalizing data  
- credibility of users who rate or provide feedback
- more data is better
- show more information such as count or distribution
- context is also important to consider
    - need to understand relationship between summary stat and user need. (e.g. correct average)
- personalization addresses these limitations

__damped means__  
Solves for low confidence and few ratings or scores.

The standard mean is the sum over users $u$ the user rating for each item $r_{ui}$ and divide by the total number of items $n$.
$$
mean = {\sum_{u} r_{ui} \over n}
$$

$k$ is the damping factor and $\mu$ us the global mean.
$$
damped mean = {\sum_{u} r_{ui} + k\mu \over n + k}
$$

### Weak Personalization
Used when we know a little bit about the user such as zip code or current generic context (e.g. web page you are on, current item being browsed).

#### Demographics and Stereotypes

#### Product Association
Related items

### Content-Based Filtering
Use user rating or score for an item to show preference for the various item attributes.  The user model is a list of attributes (not items) and the specific user score for each.  When an item is rated (or other action that can be scored) that value is used to update the user model for all the item attributes.

Apply user model to new items to get a similarity score for item against user profile vector, essentially make a prediction if the user will like the item or not.

### Personalized Collaborative Filtering
Primarily uses a matrix of user/item ratings and utilizes the opinions of others to predict/recommend.  This does not use the item attributes, but just the opinion or tendency towards an item to make predictions and recommendations.

Both approaches below use the same common core, a sparse matrix of ratings.
- predict: fill in missing values
- recommend: select promising cells

__User-User__  
Select neighborhood of similar-taste users and use their opinions of items to predict an item(s) for the user of interest where they have not previously rated or interacted with that item. Compute a similarity score between a user and other users. Then look for users with high similarity scores (neighborhood) that have rated the item of interest or create a list of possible recommendations.  The challenge with this method is that user tastes can change over time and are usually only limited to a category of items, thus taking more compute resources to keep current.

There is a variant of this that uses trusted users versus similar users and then looks at their ratings / tastes to make predictions and recommendations.

_matrix:_ Each column is an item, each row is a user and each cell is a rating for the user/item combination.

__Item-Item__  
Precompute similarity between an item and other items using user ratings vector for each item.  Find items that are similar to those that the user has already rated or scored.  This method has efficiencies over user-user because items don't really changes, more about availability of item.

_matrix:_ There is row for each item and a column for each item so that all possible item/item combinations are represented.  Each cell is the similarity score for the item/item combination.

### TFIDF
