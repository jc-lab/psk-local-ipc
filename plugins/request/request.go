package request

import (
	"context"
	"errors"
	"github.com/google/uuid"
	ipc "github.com/jc-lab/psk-local-ipc"
	"time"
)

type requestContext struct {
	RequestId uuid.UUID
}

const (
	cMsgTypeRequest  int32 = -2147418111 // 0x80010001
	cMsgTypeResolved int32 = -2147418110 // 0x80010002
	cMsgTypeRejected int32 = -2147418109 // 0x80010003
)

func NewRequestPlugin() *Handler {
	return &Handler{}
}

func (handler *Handler) Request(connection Writable, method string, userdata []byte, timeout time.Duration) (*ResolvedResult, *RejectedResult, error) {
	requestId, _ := uuid.NewRandom()
	resultChannel := make(chan interface{}, 1)
	ctx, _ := context.WithTimeout(context.Background(), timeout)
	ctx = context.WithValue(ctx, "requestId", requestId)
	ctx = context.WithValue(ctx, "resultChannel", resultChannel)

	handler.contexts.Store(requestId, ctx)

	payload := make([]byte, 0)
	bRequestId, _ := requestId.MarshalBinary()
	bMethod := []byte(method)
	payload = append(payload, bRequestId...)
	payload = append(payload, byte(len(bMethod)>>8), byte(len(bMethod)))
	payload = append(payload, bMethod...)
	payload = append(payload, userdata...)
	err := connection.Write(cMsgTypeRequest, payload)
	if err != nil {
		handler.contexts.Delete(requestId)
		return nil, nil, err
	}

	select {
	case result := <-resultChannel:
		resolved, ok := result.(*ResolvedResult)
		if ok {
			return resolved, nil, nil
		}
		rejected, ok := result.(*RejectedResult)
		if ok {
			return nil, rejected, nil
		}
		return nil, nil, errors.New("Something error")
	case <-ctx.Done():
		handler.contexts.Delete(requestId)
		return nil, nil, ctx.Err()
	}
}

func (handler *Handler) HandleMessage(msg *ipc.Message, channel Writable) (bool, *ReceivedRequest) {
	if channel == nil {
		return false, nil
	}
	if msg.MsgType == cMsgTypeRequest || msg.MsgType == cMsgTypeResolved || msg.MsgType == cMsgTypeRejected {
		if len(msg.Data) < 16 {
			return false, nil
		}

		requestId, _ := uuid.FromBytes(msg.Data[0:16])

		if msg.MsgType == cMsgTypeRequest {
			methodLength := int(msg.Data[16])<<8 | int(msg.Data[17])
			method := string(msg.Data[18 : 18+methodLength])
			userdata := msg.Data[18+methodLength:]

			received := &ReceivedRequest{
				RequestId: requestId,
				Method:    method,
				Userdata:  userdata,
				Resolve: func(resolveUserdata []byte) {
					payload := make([]byte, 16+len(resolveUserdata))
					copy(payload[0:], requestId[:])
					copy(payload[16:], resolveUserdata)
					channel.Write(cMsgTypeResolved, payload)
				},
				Reject: func(message string, rejectUserdata []byte) {
					bMessage := []byte(message)
					payload := make([]byte, 18+len(bMessage)+len(rejectUserdata))
					copy(payload[0:], requestId[:])
					payload[16] = byte(len(bMessage) >> 8)
					payload[17] = byte(len(bMessage))
					copy(payload[18:], bMessage)
					copy(payload[18+len(bMessage):], rejectUserdata)
					_ = channel.Write(cMsgTypeRejected, payload)
				},
			}

			return true, received
		} else if msg.MsgType == cMsgTypeResolved {
			v, loaded := handler.contexts.LoadAndDelete(requestId)
			if loaded {
				ctx := v.(context.Context)
				resultChannel, _ := ctx.Value("resultChannel").(chan interface{})
				resultChannel <- &ResolvedResult{
					Userdata: msg.Data[16:],
				}
			}
			return true, nil
		} else if msg.MsgType == cMsgTypeRejected {
			v, loaded := handler.contexts.LoadAndDelete(requestId)
			if loaded {
				ctx := v.(context.Context)
				resultChannel, _ := ctx.Value("resultChannel").(chan interface{})
				msgLength := int(msg.Data[16])<<8 | int(msg.Data[17])
				resultChannel <- &RejectedResult{
					Message:  string(msg.Data[18 : 18+msgLength]),
					Userdata: msg.Data[18+msgLength:],
				}
			}
			return true, nil
		}
	}

	return false, nil
}
