# FREQL : Flexible Recommendation Engine Query Language

## What is FREQL?

FREQL is a realtime, highly scalable recommendation engine with a SQL-like query language.  It can be used both as a huge in-memory distributed graph database and as a "multimodal", "collaborative filtering" rec engine.

## Why FREQL over a Spark-based Library?

This library is directly based on a state-of-the-art Apache Spark Mahout library but improves on it in a number of ways:

1. Any Spark-based solution suffers from immutability; an inability to modify a matrix without creating a whole new copy of it. This is not good when your matrix is billions of rows.  Spark was tossed out early on and replaced with Apache Ignite, a distributed computing engine that allows for changing only the data that is relevant to an incoming update.
2. Mahout and similar libraries generate recommendations by matrix multiplication.  This means O(n^3) operations even for a single update.  FREQL replaces the traditional multiplication approach with a much faster O( n * log(n) ) algorithm, leading to realtime recommendations in practice.
3. FREQL uses a sparse distributed hashtable instead of a dense matrix representation, typically resulting in a ~90% reduction in cluster size since the matricies mostly contain zeros.
4. Extremely multimodal, mix many dimensions of a user's data to get better and better results.
5. A flexible, dummy-proof query lanuage that mixes graph queries with recommendation scores!

## Installation

Luckily the only hard dependency is Apache Ignite.  You can read Ignite's Getting Started documentation to see how to set up a production cluster, otherwise you can simply run the unit tests to use a local single node cluster.

## How to use

Interacting with FREQL simply requires implementing an "App" class in Scala or Java.  The examples folder has a reference "FakeUTubeApp" that will be used for demonstration here.  You simply define the InputStreams (typically from Kafka), OutputStreams (typically over HTTP to some persistent API), and the queries you'd like answered between them.  Writing your App in Scala allows you to write in a type-safe DSL that's familiar to SQL users.

### Step 1: Define your Objects and Relations

Think about your data in graph terms.  For FakeUTube, we might have User nodes with outgoing relations to watched Videos or my Subscribers:

class User() extends Obj()
class Video() extends Obj()

class Watched() extends Edge()
class Subscribed() extends Edge()

val WatchedVideo = Relation(User(), Watched(), User())
val SubscribedToUser = Relation(User(), Subscribed(), User())

### Step 2: Defining your Queries
Now we could do a boring graph query to get all of User 1's watched videos:

Select(Video(),
From(WatchedVideo)
Where InputEquals( Id("1") ) )

Or something more interesting like finding users similar to me based on what I've seen *and* who I subscribe to (multimodal FTW):

val similarUsers =
Select(User(),
From(WatchedVideo) Join SubscribedToUser
Where Not InputEqualsOutput ) //(Exclude myself)

This query will ultimately be passed a User Id to get other users most similar to him/her.  Now we can traverse similarUsers "To" their watched videos as Autoplay recommendations:

val autoplaySuggestions =
Select(Video(),
From(similarUsers) To WatchedVideos
Where Not InputRelatedBy( WatchedVideo )  //Filter out what I've already seen

Functional composition FTW!  Now you can call autoplaySuggestions( Id("1") ) to suggest videos for User 1.

### Step 3: Define your Input and Output Streams

Kafka makes for a great replay-able and scalable stream of events, so a KafkaInputStream is provided by default though you can easily create your own InputStream.  You can choose to trigger output either by explicit request, or by a reaction to recommendations changing in realtime.  For either purpose you'll want to implement RequestedOutputStream or ReactiveOutputStream respectively, both of which have a naive HTTP implementation that sends to some JSON REST endpoint.

## Query API Docs

### Graph Queries

### Recommendation Queries
