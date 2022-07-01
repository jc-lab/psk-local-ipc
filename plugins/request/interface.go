package request

import (
	"github.com/google/uuid"
	"sync"
)

type Handler struct {
	contexts sync.Map
}

type ResolvedResult struct {
	Userdata []byte
}

type RejectedResult struct {
	Message  string
	Userdata []byte
}

type ReceivedRequest struct {
	RequestId uuid.UUID
	Method    string
	Userdata  []byte
	Resolve   func([]byte)
	Reject    func(string, []byte)
}

type Writable interface {
	Write(msgType int32, message []byte) error
}
