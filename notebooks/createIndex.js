

print("************************");
print("update collection us-east");

// Switch default indexes in Sparse mode
db["us-east"].dropIndex( { "ecm:versionSeriesId": 1 });
db["us-east"].createIndex( { "ecm:versionSeriesId": 1 }, { sparse: true , background: true} );
db["us-east"].dropIndex( { "ecm:proxyTargetId": 1 });
db["us-east"].createIndex( { "ecm:proxyTargetId": 1 }, { sparse: true }, { background: true} );
db["us-east"].dropIndex( { "ecm:proxyVersionSeriesId": 1 });
db["us-east"].createIndex( { "ecm:proxyVersionSeriesId": 1 }, { sparse: true , background: true} );
db["us-east"].dropIndex( { "ecm:racl": 1 });
db["us-east"].createIndex( { "ecm:racl": 1 }, { sparse: true , background: true} );
db["us-east"].dropIndex( { "ecm:retainUntil": 1 });
db["us-east"].createIndex( { "ecm:retainUntil": 1 }, { sparse: true , background: true} );
db["us-east"].dropIndex( { "ecm:fulltextJobId": 1 });
db["us-east"].createIndex( { "ecm:fulltextJobId": 1 }, { sparse: true , background: true} );
db["us-east"].dropIndex( { "ecm:acp.acl.user": 1 });
db["us-east"].createIndex( { "ecm:acp.acl.user": 1 }, { sparse: true , background: true} );
db["us-east"].dropIndex( { "ecm:acp.acl.status": 1 });
db["us-east"].createIndex( { "ecm:acp.acl.status": 1 }, { sparse: true , background: true} );
db["us-east"].dropIndex( { "dc:modified_": -1 });
db["us-east"].createIndex( { "dc:modified": -1 }, { sparse: true , background: true} );
db["us-east"].dropIndex( { "rend:renditionName": 1 });
db["us-east"].createIndex( { "rend:renditionName": 1 }, { sparse: true , background: true} );
db["us-east"].dropIndex( { "rend:sourceId": 1 });
db["us-east"].createIndex( { "rend:sourceId": 1 }, { sparse: true , background: true} );
db["us-east"].dropIndex( { "rend:sourceVersionableId": 1 });
db["us-east"].createIndex( { "rend:sourceVersionableId": 1 }, { sparse: true , background: true} );
db["us-east"].dropIndex( { "drv:subscriptions.enabled": 1 });
db["us-east"].createIndex( { "drv:subscriptions.enabled": 1 }, { sparse: true , background: true} );
db["us-east"].dropIndex( { "collectionMember:collectionIds": 1 });
db["us-east"].createIndex( { "collectionMember:collectionIds": 1 }, { sparse: true , background: true} );
db["us-east"].dropIndex( { "nxtag:tags": 1 });
db["us-east"].createIndex( { "nxtag:tags": 1 }, { sparse: true , background: true} );
db["us-east"].dropIndex( { "coldstorage:beingRetrieved": 1 });
db["us-east"].createIndex( { "coldstorage:beingRetrieved": 1 }, { sparse: true , background: true} );

// Application specific index
db["us-east"].dropIndex( { "customer:number": 1 });
db["us-east"].createIndex( { "customer:number": 1 }, { sparse: true , background: true} );
db["us-east"].dropIndex( { "account:number": 1 });
db["us-east"].createIndex( { "account:number": 1 }, { sparse: true , background: true} );




print("************************");
print("update collection us-west");

// Switch default indexes in Sparse mode
db["us-west"].dropIndex( { "ecm:versionSeriesId": 1 });
db["us-west"].createIndex( { "ecm:versionSeriesId": 1 }, { sparse: true , background: true} );
db["us-west"].dropIndex( { "ecm:proxyTargetId": 1 });
db["us-west"].createIndex( { "ecm:proxyTargetId": 1 }, { sparse: true }, { background: true} );
db["us-west"].dropIndex( { "ecm:proxyVersionSeriesId": 1 });
db["us-west"].createIndex( { "ecm:proxyVersionSeriesId": 1 }, { sparse: true , background: true} );
db["us-west"].dropIndex( { "ecm:racl": 1 });
db["us-west"].createIndex( { "ecm:racl": 1 }, { sparse: true , background: true} );
db["us-west"].dropIndex( { "ecm:retainUntil": 1 });
db["us-west"].createIndex( { "ecm:retainUntil": 1 }, { sparse: true , background: true} );
db["us-west"].dropIndex( { "ecm:fulltextJobId": 1 });
db["us-west"].createIndex( { "ecm:fulltextJobId": 1 }, { sparse: true , background: true} );
db["us-west"].dropIndex( { "ecm:acp.acl.user": 1 });
db["us-west"].createIndex( { "ecm:acp.acl.user": 1 }, { sparse: true , background: true} );
db["us-west"].dropIndex( { "ecm:acp.acl.status": 1 });
db["us-west"].createIndex( { "ecm:acp.acl.status": 1 }, { sparse: true , background: true} );
db["us-west"].dropIndex( { "dc:modified_": -1 });
db["us-west"].createIndex( { "dc:modified": -1 }, { sparse: true , background: true} );
db["us-west"].dropIndex( { "rend:renditionName": 1 });
db["us-west"].createIndex( { "rend:renditionName": 1 }, { sparse: true , background: true} );
db["us-west"].dropIndex( { "rend:sourceId": 1 });
db["us-west"].createIndex( { "rend:sourceId": 1 }, { sparse: true , background: true} );
db["us-west"].dropIndex( { "rend:sourceVersionableId": 1 });
db["us-west"].createIndex( { "rend:sourceVersionableId": 1 }, { sparse: true , background: true} );
db["us-west"].dropIndex( { "drv:subscriptions.enabled": 1 });
db["us-west"].createIndex( { "drv:subscriptions.enabled": 1 }, { sparse: true , background: true} );
db["us-west"].dropIndex( { "collectionMember:collectionIds": 1 });
db["us-west"].createIndex( { "collectionMember:collectionIds": 1 }, { sparse: true , background: true} );
db["us-west"].dropIndex( { "nxtag:tags": 1 });
db["us-west"].createIndex( { "nxtag:tags": 1 }, { sparse: true , background: true} );
db["us-west"].dropIndex( { "coldstorage:beingRetrieved": 1 });
db["us-west"].createIndex( { "coldstorage:beingRetrieved": 1 }, { sparse: true , background: true} );

// Application specific index
db["us-west"].dropIndex( { "customer:number": 1 });
db["us-west"].createIndex( { "customer:number": 1 }, { sparse: true , background: true} );
db["us-west"].dropIndex( { "account:number": 1 });
db["us-west"].createIndex( { "account:number": 1 }, { sparse: true , background: true} );




print("************************");
print("update collection archives");

// Switch default indexes in Sparse mode
db["archives"].dropIndex( { "ecm:versionSeriesId": 1 });
db["archives"].createIndex( { "ecm:versionSeriesId": 1 }, { sparse: true , background: true} );
db["archives"].dropIndex( { "ecm:proxyTargetId": 1 });
db["archives"].createIndex( { "ecm:proxyTargetId": 1 }, { sparse: true }, { background: true} );
db["archives"].dropIndex( { "ecm:proxyVersionSeriesId": 1 });
db["archives"].createIndex( { "ecm:proxyVersionSeriesId": 1 }, { sparse: true , background: true} );
db["archives"].dropIndex( { "ecm:racl": 1 });
db["archives"].createIndex( { "ecm:racl": 1 }, { sparse: true , background: true} );
db["archives"].dropIndex( { "ecm:retainUntil": 1 });
db["archives"].createIndex( { "ecm:retainUntil": 1 }, { sparse: true , background: true} );
db["archives"].dropIndex( { "ecm:fulltextJobId": 1 });
db["archives"].createIndex( { "ecm:fulltextJobId": 1 }, { sparse: true , background: true} );
db["archives"].dropIndex( { "ecm:acp.acl.user": 1 });
db["archives"].createIndex( { "ecm:acp.acl.user": 1 }, { sparse: true , background: true} );
db["archives"].dropIndex( { "ecm:acp.acl.status": 1 });
db["archives"].createIndex( { "ecm:acp.acl.status": 1 }, { sparse: true , background: true} );
db["archives"].dropIndex( { "dc:modified_": -1 });
db["archives"].createIndex( { "dc:modified": -1 }, { sparse: true , background: true} );
db["archives"].dropIndex( { "rend:renditionName": 1 });
db["archives"].createIndex( { "rend:renditionName": 1 }, { sparse: true , background: true} );
db["archives"].dropIndex( { "rend:sourceId": 1 });
db["archives"].createIndex( { "rend:sourceId": 1 }, { sparse: true , background: true} );
db["archives"].dropIndex( { "rend:sourceVersionableId": 1 });
db["archives"].createIndex( { "rend:sourceVersionableId": 1 }, { sparse: true , background: true} );
db["archives"].dropIndex( { "drv:subscriptions.enabled": 1 });
db["archives"].createIndex( { "drv:subscriptions.enabled": 1 }, { sparse: true , background: true} );
db["archives"].dropIndex( { "collectionMember:collectionIds": 1 });
db["archives"].createIndex( { "collectionMember:collectionIds": 1 }, { sparse: true , background: true} );
db["archives"].dropIndex( { "nxtag:tags": 1 });
db["archives"].createIndex( { "nxtag:tags": 1 }, { sparse: true , background: true} );
db["archives"].dropIndex( { "coldstorage:beingRetrieved": 1 });
db["archives"].createIndex( { "coldstorage:beingRetrieved": 1 }, { sparse: true , background: true} );

// Application specific index
db["archives"].dropIndex( { "customer:number": 1 });
db["archives"].createIndex( { "customer:number": 1 }, { sparse: true , background: true} );
db["archives"].dropIndex( { "account:number": 1 });
db["archives"].createIndex( { "account:number": 1 }, { sparse: true , background: true} );


print("************************"); 
print("All updates completed");