# Request Plugin

Request-Response

# Protocol

## Request

- MsgType : `0x80010001`

**Protocol:**
```
requestId (16 bytes)
methodSize (2 bytes)
method (variable)
requestBody (variable)
```

## Resolved Response

- MsgType : `0x80010002`

**Protocol:**
```
requestId (16 bytes)
messageSize (2 bytes)
message (variable)
user data
```

## Rejected Response

- MsgType : `0x80010003`

**Protocol:**
```
requestId (16 bytes)
user data
```
