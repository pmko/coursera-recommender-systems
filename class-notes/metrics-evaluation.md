# Prediction Accuracy Metrics
Measures that look at how correct or incorrect, or how far off a recommender system is in predicting what a person would have rated an item. Error metrics are computed using a hidden data evaluation methodology which is retrospective in nature. Hiding an existing user's rating and then try to predict it measures how far off the system is from reality. By taking existing data and we see how well the recommender could have generated that data in the absence of having it.

There are three main ways to compute accuracy:
1. MAE - mean absolute error
2. MSE - mean squared error (not commonly used)
3. RMSE - root mean squared error

### Mean Absolute Error
The simplest measure of them all and defines the error as the divergence of prediction from actual rating, prediction minus the rating is the error. An absolute number us used to understand how far off we are in either direction. Doesn't matter if the prediction is higher or lower than the actual rating, just by how much it was off, hence the use of the absolute value.

This is the average of all absolute error values which is the predicted rating minus the actual rating.  The sum of all absolute error values divided by the total number of ratings.  The approach penalizes large and small errors in a relatively similar fashion.  
**_Lower MAE values are better_**
$$
MAE = {\sum_{ratings} |P-R| \over ratings}
$$

### Mean Squared Error
Of the three ways to measure error this one is not commonly used. Although it removes the sign by squaring, squared error is not an intuitive scale.  It's advantage is that large errors are penalized more than small errors.  
**_Lower MSE values are better_**

$$
MSE = {\sum_{ratings} (P-R)^2 \over ratings}
$$

### Root Mean Squared Error
Has all the advantages of MSE, but corrects the scale issue by reversing the square.  So it removes the sign by squaring, penalizes large errors more than small and remains in the original units of measure.  This is the most commonly use of the three.  
**_Lower RMSE values are better_**

$$
RMSE = \sqrt{\sum_{ratings} (P-R)^2 \over ratings}
$$

This can be used on a per user or per item basis.
- For user we would look at that user's predicted rating for and subtract the actual rating, then square that value - do this for all user rated items.  Then sum those values, divide by the total number of user ratings and square root the result.  This provides the RMSE for a single user.  All user RMSEs can be averaged to evaluate the system as a whole.
- For the item we would subtract a user rating by the predicted user rating, square it, sum up all user ratings for that item, divide by total number of user ratings for the item and then get the square root.  This is the RMSE for a single item.  All item RMSEs can be averaged to evaluate the system as a whole.S

# Decision Support Metrics
A type of recommender that helps users make good decisions like choosing "good" items and avoiding "bad" decisions.  This is less about accuracy as long as the error is consistent with the user's preference.  Additionally, how high an item is on the list is key.

_Example:_ If user would have rated an item 2.5 and the prediction is off by 1.5 then if the prediction is 1, all good because the user would have given it a bad rating anyway. But if the prediction was 4 then that's not good because it would mislead the user into thinking something bad was good.

_This would make a good metric for the geospatial recommender._

#####Information Retrieval Metrics:
Good metrics, but have challenges and were originally conceived for search.  Both typically look at all items within a system.  To make them relevant for a recommender, typically use $P@N$ or $R@N$. This limits the evaluation to a subset of all items.
1. Precision:
percentage of selected items that are 'relevant'.  the goal is to return mostly useful stuff, not wasting the user's time.  relevant to search engines.
Number of relevant, selected items divided by number of selected items.
$$
P = {N_{rs} \over N_s}
$$
2. Recall:
percentage of relevant items that are selected.  goal is to not miss stuff or fail to find things that would be good.
Number of relevant selected items divided by number of relevant items.
$$
R = {N_{rs} \over N_r}
$$

#####Receiver Operating Characteristic:
A curve that plots the performance of a filter or classifier at different thresholds. Helps to show the cutoff of what should be recommended. Area under the curve is the overall measure of the effectiveness of the recommendation.

# Rank-Aware Top-N Metrics
Where does the recommender list the items it suggests and how good is the recommender at modeling relative preference.  There are two families of metrics:
  * _Binary relevance_: used to determine if an item is 'good' or not.
  * _Utility_: measurement of absolute or relative 'goodness' of items.

#####Mean Reciprocal Rank
What is the index $k$ of the first relevant item $u$ within a list of recommendations.  Captures how quickly a user can find a relevant item. Gives the overall algorithm performance.  Reciprocal Rank as ${1 \over k_u}$.  score gets significantly smaller as the index gets higher (e.g. item is further down the list).
Overall this is fairly simple to use, but is intended for evaluating a target search or recommendation (i.e. user looking for something specific).  Doesn't work as well for general results.  
**_scores are between 0 and 1.  higher scores are better._**

_Example:_ Given a list of 5 items, it is identified that the first relevant item is #2 in the list, then the reciprocal rank would be ${1 \over k_u} = {1 \over 2}$.  Then average this across all users to get the _MRR_.
$$
MMR(O,U) = \sum_{u \in U} {1 \over k_u}
$$

#####Mean Average Precision (MAP)
What is the fraction of $n$ recommendations that are 'good'?  challenges are that it requires a fixed $n$ and treats all errors equally.  This would be fine in a Top-5 list as it measures accuracy at the top of the recommendation list.

For each user determine the precision of the list through each relevant item's index.  This results in relevant items higher in the list count more towards the scores of subsequent relevant items.  
**_scores are between 0 and 1.  higher scores are better._**

Average Precision is the sum of the precision values for all relevant items in list over total number of relevant items.  $N_r$ is the number of relevant items in the list up to rank $k$.

_Example:_ The precision of the item at the third index in the list, assuming it's the first relevant item in the list, would be $1\over3$.  Then if the fifth item was the next relevant one its precision would be $2\over5$.
$$
Average Precision = AP = {{\sum_{k \in N}^N {N_r \over k}} \over k}
$$

To get MAP we then average all the APs or the sum of the Average Precision for each system order $O$ for a user $U$ and divide by the total number of relevant items.

$$
MAP(O,U) = \sum_{u \in U}AP(O(u))
$$

#####Rank Correlation
Compares recommender system order $O$ to the user's preference order $O_u$.  To do this we use the Spearman correlation which is the Pearson correlation over item ranks.  Ties get the average of ranks.  Tells how aligned two rankings are.
$$
{\sum_i (k_o(i)-\bar k_o)(k_{o_u}(i)-\bar k_{o_u})} \over {\sqrt{\sum_i(k_o(i)-\bar k_o)^2} \sqrt{\sum_i(k_{o_u}(i)-\bar k_{o_u})^2}}
$$
challenges:
* all misplacements are treated equally
* down care as much with higher indexes
* Goal: weight things at top of list more heavily, putter more emphasis on how good the recommender is at the top of the list.

#####Normalized Discounted Cumulative Gain (nDCG)
Measures the utility of items at each position in the list.  This discounts by position so items at the top are more important.  The DCG is normalized by comparing the list the recommender produced vs. the DCG of a perfect list.  This effectively measures the fraction of utility that it's possible to achieve that the recommender actually achieved.

The discount is calculated as show below.  If the index of the item in the list is $\leq2$, then the discounted value of $i$ is 1.  If the index of the item is $>2$, then the discounted value of $i$ is $\log_2(i)$. Other types of discounting are possible such as decay.

$$
disc(i) = \left\{ \begin{array}{cc} 1, & i\leq2 \\
\log_2(i), & i>2 \end{array} \right\}
$$

To get the Discounted Cumulative Gain (DCG) we then sum the rating the user gave for an item divided by the discount of $i$ per the above method for each item in the list.

$$
DCG(O,u) = \sum_i {r_{ui} \over disc(i)}
$$

The nDCG is then calculated by dividing the recommended list's DCG value by the DCG value of a perfect list.  A perfect list is defined by taking the recommended list and ordering it by highest user rankings at the top to lowest and then computing the DCG.
**_scores are between 0 and 1.  higher scores are better._**

$$
nDCG = {DCG_{achieved} \over DCG_{perfect}}
$$

# Evaluation Protocols

There are a couple of primary way to conduct offline evaluations in this context. These techniques are also used in other aspects of machine learning.
1. Hidden data:  
Try to predict data that you already have by hiding it from the recommender.
2. Cross-Validation
