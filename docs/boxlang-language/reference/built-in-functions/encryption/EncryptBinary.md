[comment]: # (Note: This documentation is generated dynamically in the build process.  To modify the contents, change the javadoc on the _invoke method of the BIF class)

# Function: `EncryptBinary`

No description available.

## Method Signature

```
EncryptBinary(string=[string], key=[string], algorithm=[string], encoding=[string], IVorSalt=[any], iterations=[integer], precise=[boolean])
```

### Arguments


| Argument | Type | Required | Description | Default |
|----------|------|----------|-------------|---------|
| `string` | `string` | `true` |  |  |
| `key` | `string` | `true` |  |  |
| `algorithm` | `string` | `false` |  |  |
| `encoding` | `string` | `false` |  | `UU` |
| `IVorSalt` | `any` | `false` |  |  |
| `iterations` | `integer` | `false` |  | `1000` |
| `precise` | `boolean` | `false` |  | `false` |

## Examples



## Related

  * [Decrypt](./Decrypt.md)
  * [Encrypt](./Encrypt.md)
  * [GeneratePDBKDFKey](./GeneratePDBKDFKey.md)
  * [GenerateSecretKey](./GenerateSecretKey.md)
  * [Hash](./Hash.md)
  * [Hash40](./Hash40.md)
  * [Hmac](./Hmac.md)