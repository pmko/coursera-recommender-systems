## Recommender Systems Capstone

#### Scenario Overview

_the situation:_
-  “back to school” is a critical time period for office product sales to consumers in the US
- the six weeks including and surrounding the month of August are responsible for 31% of yearly office product sales
- the surge in office product sales is not limited to traditional school products such as notebooks, pencils, and erasers
- once people are buying school products, they also buy other office products
- most large-dollar office-product purchases include a mix of inexpensive and more expensive products in the same transaction
- data suggests that it may be important to have inexpensive products as an entry point, but more expensive ones that build the transaction size
- alternatively, once someone comes to buy something large, they also fill in smaller items
- potentially the surge in office product sales is due to in-person sales and promotion at retail outlets, with two particularly important prompts:
    - Visits to office products superstores peak during this time of year
    - Once parents are in the store to buy supplies for their children, they see other products of interest
    - Special displays are set up at “Big-Box” stores  with both school supplies and other office products

_the assignment:_
- the site does not experience as large a surge in office product sales during the back-to-school period
- a surge does happen (about double typical sales, or about 23% of annual consumer sales), but it is far below that of your offline competitor
- figures include the results of existing promotions such as back-to-school banners and a free next-day shipping promotion for products sold during the two weeks when schools most commonly start classes (late August and early September)
- develop a recommender system to increase sales of office products during this important time period:
- key goals and constraints:
  - site already has a very effective product-association recommender system
  - focus on recommending products based on customer’s overall profiles, not their current browsing or basket
  - Research shows that additional sales at this time of year are divided fairly broadly among categories of office products (school supplies, consumable supplies, durable office equipment). The recommender should respond to this research appropriately.
  - The recommender should also address the finding above about having both cheaper and more expensive products available to attract customers.
  - The site prides itself on having a much deeper product catalog than the typical big-box store. One of the key drivers of repeat business is customer discovery of new products they likely couldn’t buy at a local store. Your recommender should respond to this information appropriately.

  product recommendations will be displayed in two places on the site:
    - Five products displayed on the “office products” landing page where customers will land if they click on banner ads (back to school shopping!) or select the office products category (from various menus or navigation aids).
    - Five products displayed as part of “other suggestions” that will be displayed as part of the shopping cart display and near the bottom of product pages (primarily will be placed on product pages from the same category, but also related products such as textbooks, school bags, and backpacks).

#### EDA Notes
- some products are missing prices

#### Part 1: Design
should be 2-3 pages in length
- identify metrics and evaluation techniques that could be used to identify potential algorithms
- outline plan to explore both individual and hybrid approaches
- how to tune the selected algorithm
- how will training data be separated from test data
  - hidden data evaluation and is retrospective in nature
  - cross-validation with $k$ partitions
    - no live users required, so good for this assignment
    - very efficient approach
- explain how metrics chosen relate to business goals of the scenario

_what's important:_
- recommendations based on user profile
- diversity is important
- balance of expensive and inexpensive products
- serendipity

_metrics to consider:_
- Top-N vs Prediction accuracy
  - accuracy is not as important, more if the user bought an item that was shown in a Top-N list
- how close is the prediction to the actual rating in aggregate?
  - RMSE?
- did users purchase items in the recommended list?
- what was the ranking in the predicted Top-N of items purchased?
  - MRR would be good to understand how far down the list relevant items start to show up
  - MAP would tell us the how good the list is in term of including relevant items towards the top
  - nDCG would give us an idea of how well the ranking of the items was based on specific user preference.

#### Part 2: Measure
- evaluate three different base algorithms to understand how they performed on the dataset using the metrics outlined in the design section

#### Part 3: Mix
- explore two possible hybrids to improve results

#### Part 4: Proposal and Reflection
- present final approach to recommender
- justify the result and the means to achieve it
- address any questions brought up in exploration
