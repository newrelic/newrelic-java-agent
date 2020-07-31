Thread profile format version 2
=====

The top level element of a thread profile is a map consisting with the following keys:

Key  | Value
------------- | -------------
version  | the numeric version number of this format (2)
[threads](#thread-trees) | a map of the thread trees
[transactions](#transactions) | a map of the transaction trees
agent_thread_ids | a list of agent thread ids
[string_map](#string-map) | a map of strings

## Thread trees

Thread trees are a map of normalized thread name to [profile trees](#profile-trees).  CPU time is reported for each of these trees.

## Transactions

This is a map of transaction names to profile trees.  No `cpu_time` is reported for these profile trees.

## String Map

To reduce the size of the uncompressed thread profile json structure a string table is used.  The string map is a map of string keys to the original string.  The implementation of the key may vary as long as it uniquely represents every string in the profile.  An agent could, for instance, just number strings from 1 to n as strings are added to the map.  The java agent's implementation creates durable key ids - the id of any given string will be the same across different applications and agent runs.

## Profile Trees

At its root a profile tree is a list of two elements.  The first element in the list is a map of attributes, namely `cpu_time` and `threads`.  `cpu_time` is a long representing the total cpu time in milliseconds for the tree.  `threads` is an array of thread ids.  The second element is an array of profile nodes.
