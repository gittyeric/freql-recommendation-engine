## Query API Docs

All queries receive as input a single Id.  From queries will returns recommendations while FromItems is a simple graph search to get the input Id's Related Nodes.

### Select

Any query starts with specifying the output type using **Select**.  This ensures that the inner From matches up to what you're trying to get:

    Select( [Node Type in output], ...)

### From

The **From** clause specifies the Relation to get from. For example, getting similar Users based on watched videos:

    Select(User, From( UserWatchedVideo ) )

### To

**To** takes a From's collection of recommended Origin Ids and traverses their Relation to their Destination Ids.  Example: Select Users similar to me by watched videos, then get their corresponding Videos:

    Select(Video, From WatchedVideos To WatchedVideos)

### FromItems

**FromItems** gets the destination nodes of all Relations that match the input Id.  For example, get all of a user's videos:

    Select(Video, FromItems( UserWatchedVideo ) )

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