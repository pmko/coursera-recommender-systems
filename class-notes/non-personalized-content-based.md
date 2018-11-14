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

### Non-Personalized Summary Statistics
Data from the community can be used to identify best sellers, most popular and trending.
- most / longest visited
- quantity sold or accepted
- average rating for an item

### Content-Based Filtering
Use user rating or score for an item to show preference for the various item attributes.  The user model is a list of attributes (not items) and the specific user score for each.  When an item is rated (or other action that can be scored) that value is used to update the user model for all the item attributes.

Apply user model to new items to get a similarity score for item against user profile vector, essentially make a prediction if the user will like the item or not.

### Personalized Collaborative Filtering
Primarily uses a matrix of user/item ratings and utilizes the opinions of others to predict/recommend.  This does not use the item attributes, but just the opinion or tendency towards an item to make predictions and recommendations.

Both approaches below use the same common core, a sparse matrix of ratings.
- predict: fill in missing values
- recommend: select promising cells

__User-User__  
Select neighborhood of similar-taste users and use their opinions of items to predict or rank for the user of interest. Compute a similarity score between a user and other users. Then look for users with high similarity scores that have rated the item of interest or create a list of possible recommendations.

There is a variant of this that uses trusted users versus similar users and then looks at their ratings / tastes to make predictions and recommendations.

_matrix:_ Each column is an item, each row is a user and each cell is a rating for the user/item combination.

__Item-Item__  
Precompute similarity between an item and other items using user ratings for an item.

_matrix:_ There is row for each item and a column for each item so that all possible item/item combinations are represented.  Each cell is the similarity score for the item/item combination.

### TFIDF
