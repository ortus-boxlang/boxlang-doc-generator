[comment]: # (Note: This documentation is generated dynamically in the build process.  To modify the contents, change the javadoc on the _invoke method of the Component class)
# Component: `Query`

No description available.

## Component Signature

```
<bx:Query name=[string]
datasource=[string]
returnType=[string]
columnKey=[string]
dbtype=[string]
maxRows=[integer]
blockfactor=[integer]
fetchsize=[integer]
timeout=[integer]
cache=[boolean]
cacheTimeout=[duration]
cacheLastAccessTimeout=[duration]
cacheKey=[string]
cacheProvider=[string]
result=[string]
clientInfo=[struct] />
```

### Attributes


| Atrribute | Type | Required | Description | Default |
|----------|------|----------|-------------|---------|
| `name` | `string` | `false` |  |  |
| `datasource` | `string` | `false` |  |  |
| `returnType` | `string` | `false` |  | `query` |
| `columnKey` | `string` | `false` |  |  |
| `dbtype` | `string` | `false` |  |  |
| `maxRows` | `integer` | `false` |  | `-1` |
| `blockfactor` | `integer` | `false` |  | `[ortus.boxlang.runtime.validation.dynamic.Min@2d35442b, ortus.boxlang.runtime.validation.dynamic.Max@27f9e982]` |
| `fetchsize` | `integer` | `false` |  |  |
| `timeout` | `integer` | `false` |  |  |
| `cache` | `boolean` | `false` |  | `false` |
| `cacheTimeout` | `duration` | `false` |  |  |
| `cacheLastAccessTimeout` | `duration` | `false` |  |  |
| `cacheKey` | `string` | `false` |  |  |
| `cacheProvider` | `string` | `false` |  |  |
| `result` | `string` | `false` |  |  |
| `clientInfo` | `struct` | `false` |  |  |

## Examples

```
<bx:Query name=[string]
datasource=[string]
returnType=[string]
columnKey=[string]
dbtype=[string]
maxRows=[integer]
blockfactor=[integer]
fetchsize=[integer]
timeout=[integer]
cache=[boolean]
cacheTimeout=[duration]
cacheLastAccessTimeout=[duration]
cacheKey=[string]
cacheProvider=[string]
result=[string]
clientInfo=[struct] />
```