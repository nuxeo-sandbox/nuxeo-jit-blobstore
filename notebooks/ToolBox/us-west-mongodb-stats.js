
print("{ 'dbinfo':")
printjson(db.runCommand( { serverStatus: 1,  repl: 1 } ))
print(", 'collectionInfo':")
printjson(db['us-west'].stats({"scale": 1024, "indexDetails": true}))
print("}")
