package ipc

import (
	"bytes"
	"encoding/binary"
)

func intToBytes(mLen int32) []byte {
	b := make([]byte, 4)
	binary.BigEndian.PutUint32(b, uint32(mLen))
	return b
}

func bytesToInt(b []byte) int32 {
	var mlen uint32
	binary.Read(bytes.NewReader(b[:]), binary.BigEndian, &mlen) // message length
	return int32(mlen)
}
