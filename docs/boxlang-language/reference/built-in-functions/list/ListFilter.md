[comment]: # (Note: This documentation is generated dynamically in the build process.  To modify the contents, change the javadoc on the _invoke method of the BIF class)

# Function: `ListFilter`

No description available.

## Method Signature

```
ListFilter(list=[string], filter=[function:Predicate], delimiter=[string], includeEmptyFields=[boolean], multiCharacterDelimiter=[boolean], parallel=[boolean], maxThreads=[integer])
```

### Arguments


| Argument | Type | Required | Description | Default |
|----------|------|----------|-------------|---------|
| `list` | `string` | `true` |  |  |
| `filter` | `function:Predicate` | `true` |  |  |
| `delimiter` | `string` | `false` |  | `,` |
| `includeEmptyFields` | `boolean` | `false` |  | `false` |
| `multiCharacterDelimiter` | `boolean` | `false` |  | `true` |
| `parallel` | `boolean` | `false` |  | `false` |
| `maxThreads` | `integer` | `false` |  |  |

## Examples



## Related

  * [GetToken](./GetToken.md)
  * [ListAppend](./ListAppend.md)
  * [ListAvg](./ListAvg.md)
  * [ListChangeDelims](./ListChangeDelims.md)
  * [ListCompact](./ListCompact.md)
  * [ListContains](./ListContains.md)
  * [ListContainsNoCase](./ListContainsNoCase.md)
  * [ListDeleteAt](./ListDeleteAt.md)
  * [ListEach](./ListEach.md)
  * [ListEvery](./ListEvery.md)
  * [ListFind](./ListFind.md)
  * [ListFindNoCase](./ListFindNoCase.md)
  * [ListFirst](./ListFirst.md)
  * [ListGetAt](./ListGetAt.md)
  * [ListIndexExists](./ListIndexExists.md)
  * [ListInsertAt](./ListInsertAt.md)
  * [ListItemTrim](./ListItemTrim.md)
  * [ListLast](./ListLast.md)
  * [ListLen](./ListLen.md)
  * [ListMap](./ListMap.md)
  * [ListPrepend](./ListPrepend.md)
  * [ListQualify](./ListQualify.md)
  * [ListReduceRight](./ListReduceRight.md)
  * [ListRemoveDuplicates](./ListRemoveDuplicates.md)
  * [ListRest](./ListRest.md)
  * [ListSetAt](./ListSetAt.md)
  * [ListSome](./ListSome.md)
  * [ListSort](./ListSort.md)
  * [ListToArray](./ListToArray.md)
  * [ListTrim](./ListTrim.md)
  * [ListValueCount](./ListValueCount.md)
  * [ListValueCountNoCase](./ListValueCountNoCase.md)