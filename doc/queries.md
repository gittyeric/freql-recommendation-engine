## Query API Docs

All queries receive as input a single Id.  **"From"** represents a recommendation rank query while **"FromItems"** is a simple graph search to get the input Id's "Related" "Nodes", aka "Traversing" the input Id's Relation edges and returning a list of the "Destination" nodes.

### Select & FromItems

A **Select** query is essentially a useless SQL-and-readability-friendly front for the inner **FromItems** it uses:

**FromItems** gets the destination nodes of all Relations that match the input Id.  For example, get all of a user's videos:

    Select(Video, FromItems( UserWatchedVideo ) )
    
**FromItems** may appear without a matching **Select** when nested inside a complex **Suggest** query.

### Suggest & From

Any suggestion rank-based query starts with specifying the output type using **Suggest**.

The **From** clause specifies the Relation to base suggestions from. For example, getting similar Users based on watched videos:

    Suggest(User, From( UserWatchedVideo ) )


### To

**To** takes a From's collection of recommended Origin Ids and traverses their Relation to their Destination Ids.  Example: Select Users similar to me by watched videos, then get their corresponding Videos:

    Select(Video, From WatchedVideos To WatchedVideos)


### Where

**Where** is used to filter results.  There are many pre-defined filters but you can also create your own, these include:

**Booleans**: Or, And, Not

**Helpers**: InputEqualsOutput, InputRelatedTo(Relation), In(mustMatchIdList)

For example, filter out videos I've already watched and not blacklisted:

    Select(Video,
    From( SubscribedToUser ) To WatchedVideo )
    Where ( Not InputRelatedTo( WatchedVideo ) And Not In( Id("bannedId1"), Id("bannedId2") ) )

## Composing Queries

You can chain queries together to traverse large graphs in clever ways.  For example, lets recommend similar users to me based on subscribers, then use the result to suggest videos:

    val similarUsers = Select( User,
        From( SubscribedToUser ) )
        Where( Not InputEqualsOutput )

    val videoRecs = Select( Video,
        From(similarUsers) To WatchedVideos )
        Where( Not InputRelatedTo( WatchedVideos ) )
