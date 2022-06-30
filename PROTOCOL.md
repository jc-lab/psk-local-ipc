## Communication Layer

```
|-----------------------|
|   Application Layer   |
|-----------------------|
|   Secure Layer (TLS)  |
|                       |
| TLS_ECDHE_PSK_...     |
|-----------------------|
|    TRANSPORT LAYER    |
|                       |
| Windows : Named Pipe  | 
| Other   : Unix Socket |
|-----------------------|
```

## Protocol

### Handshake

#### 1. Server -> Client

```
| byte offset | fields                          |
| 00          | version (8 bits) | rev (24bits) |
| 04          | max length (4b)                 |
```

#### 2. Client -> Server

```
| byte offset | fields           |
| 00          | result (8 bits)  |
```

### Message

```
| byte offset | fields       |
| 00          | message size |
| 04          | message type |
| 08          | message      |
```