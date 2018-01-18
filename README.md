## Subject

####Input Data
Given json contains 20K articles.
Each article contains set of attributes with one set value.

####Recommendation Engine Requirements
Calculate similarity of articles identified by sku based on their attributes values.
The number of matching attributes is the most important metric for defining similarity.
In case of a draw, attributes with name higher in alphabet (a is higher than z) is weighted with heavier weight.

#####Example 1:
```
{"sku-1": {"att-a": "a1", "att-b": "b1", "att-c": "c1"}} is more similar to
{"sku-2": {"att-a": "a2", "att-b": "b1", "att-c": "c1"}} than to
{"sku-3": {"att-a": "a1", "att-b": "b3", "att-c": "c3"}}
```
#####Example 2:
```
{"sku-1": {"att-a": "a1", "att-b": "b1"}} is more similar to 
{"sku-2": {"att-a": "a1", "att-b": "b2"}} than to
{"sku-3": {"att-a": "a2", "att-b": "b1"}}
```

####Recommendation request example
`sku-123`  > ENGINE > 10 most similar skus based on criteria described above 
with their corresponding weights.

##Implementation

### Additional conditions implemented
In current implementation it is assumed that any article can contain different 
number of attributes and completely different names. If articles contain attributes
with same names - those articles have higher similarity. But it should never have
bigger weight than attributes with same values 

#####Example:
```
{"sku-1": {"att-a": "a1", "att-b": "b1"}} is more similar to 
{"sku-2": {"att-a": "a2", "att-b": "b2"}} than to
{"sku-3": {"att-c": "00", "att-d": "00"}}
```

Also, when searching for the recommendations, articles are validated on minimal similarity
to not show completely irrelevant ones if there are no similar.
 
#####Example:

Given list
```
{"sku-1": {"att-a": "a1", "att-b": "b1"}} 
{"sku-2": {"att-a": "11", "att-b": "b2"}}
{"sku-3": {"att-a": "11", "att-b": "00"}}
{"sku-4": {"att-a": "tt", "att-b": "00"}}
```
Request for `sku-1` will return empty list, for `sku-2` - list that contains
only `sku-3` and for `sku-3` - list with `sku-2` and `sku-4` item, etc.

###To run
`sbt "run <filename>"`

A simple console UI will start, asking to either enter an `{SKU}` to find 10 
recommendations or `q|Q` to quit the application.

###To test
`sbt test`

###Not implemented/can be improved 
* File is completely loaded into the memory right away
* Sorting should be done once during the JSON parsing, for example using ListMap
* UI or configuration for amount of recommendations (10 only)
* Make additional features (check on attributes matching by name, minimal rank 
  filtering for result) configurable
* Optimisation, like parallel computations or precalculation
* Validation of input data is very primitive
