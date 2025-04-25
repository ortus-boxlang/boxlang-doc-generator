[comment]: # (Note: This documentation is generated dynamically in the build process.  To modify the contents, change the javadoc on the _invoke method of the Component class)
# Component: `Execute`

No description available.

## Component Signature

```
<bx:Execute variable=[string]
name=[string]
arguments=[any]
timeout=[long]
terminateOnTimeout=[boolean]
directory=[string]
outputFile=[string]
errorFile=[string]
errorVariable=[string] />
```

### Attributes


| Atrribute | Type | Required | Description | Default |
|----------|------|----------|-------------|---------|
| `variable` | `string` | `true` |  |  |
| `name` | `string` | `true` |  |  |
| `arguments` | `any` | `false` |  |  |
| `timeout` | `long` | `false` |  |  |
| `terminateOnTimeout` | `boolean` | `false` |  | `false` |
| `directory` | `string` | `false` |  |  |
| `outputFile` | `string` | `false` |  |  |
| `errorFile` | `string` | `false` |  |  |
| `errorVariable` | `string` | `false` |  |  |

## Examples

```
<bx:Execute variable=[string]
name=[string]
arguments=[any]
timeout=[long]
terminateOnTimeout=[boolean]
directory=[string]
outputFile=[string]
errorFile=[string]
errorVariable=[string] />
```