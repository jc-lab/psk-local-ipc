package request_test

import (
	"crypto/rand"
	"encoding/hex"
	"errors"
	"github.com/jc-lab/go-tls-psk"
	ipc "github.com/jc-lab/psk-local-ipc"
	"github.com/jc-lab/psk-local-ipc/plugins/request"
	"github.com/stretchr/testify/assert"
	"testing"
	"time"
)

var defaultPskConfig = tls.PSKConfig{
	GetIdentity: func() string {
		return "hello"
	},
	GetKey: func(identity string) ([]byte, error) {
		if identity == "hello" {
			return []byte("world"), nil
		}
		return nil, errors.New("INVALID IDENTITY: " + identity)
	},
}
var defaultServerConfig = &ipc.ServerConfig{PskConfig: defaultPskConfig}
var defaultClientConfig = &ipc.ClientConfig{PskConfig: defaultPskConfig}

func generateRandom() string {
	b := make([]byte, 8)
	rand.Read(b)
	return hex.EncodeToString(b)
}

var RAND_VALUE = generateRandom()

func TestRequestResolve(t *testing.T) {
	sc, err := ipc.StartServer(RAND_VALUE+"test01", defaultServerConfig)
	if err != nil {
		t.Error(err)
	}
	waitServerReady(t, sc)

	sr := request.NewRequestPlugin()

	go func() {
		count := 0

		for {
			msg, err := sc.Read()
			if err != nil {
				t.Fatal(err)
			}

			handled, receivedRequest := sr.HandleMessage(msg, msg.Connection)
			if handled && receivedRequest != nil {
				if receivedRequest.Method == "hello" {
					receivedRequest.Resolve([]byte("world"))
					count++
				} else {
					receivedRequest.Reject("invalid method", make([]byte, 0))
					count++
				}
			}

			if count == 2 {
				break
			}
		}
	}()

	// ==================================================

	cc, err2 := ipc.StartClient(RAND_VALUE+"test01", defaultClientConfig)
	if err2 != nil {
		t.Error(err)
	}

	cr := request.NewRequestPlugin()

	waitClientConnected(t, cc)

	go func() {
		for {
			msg, err := cc.Read()
			if err != nil {
				break
			}

			handled, _ := cr.HandleMessage(msg, cc)
			_ = handled
		}
	}()

	resolved, rejected, err := cr.Request(cc, "hello", make([]byte, 0), time.Second)
	if err != nil {
		t.Fatal(err)
	}
	if rejected != nil {
		t.Fatal(rejected)
	}
	assert.Equal(t, []byte("world"), resolved.Userdata)
}

func TestRequestReject(t *testing.T) {
	sc, err := ipc.StartServer(RAND_VALUE+"test02", defaultServerConfig)
	if err != nil {
		t.Error(err)
	}
	waitServerReady(t, sc)

	sr := request.NewRequestPlugin()

	go func() {
		count := 0

		for {
			msg, err := sc.Read()
			if err != nil {
				t.Fatal(err)
			}

			handled, receivedRequest := sr.HandleMessage(msg, msg.Connection)
			if handled && receivedRequest != nil {
				if receivedRequest.Method == "hello" {
					receivedRequest.Resolve([]byte("world"))
					count++
				} else {
					receivedRequest.Reject("invalid method", make([]byte, 0))
					count++
				}
			}

			if count == 2 {
				break
			}
		}
	}()

	// ==================================================

	cc, err2 := ipc.StartClient(RAND_VALUE+"test01", defaultClientConfig)
	if err2 != nil {
		t.Error(err)
	}

	cr := request.NewRequestPlugin()

	waitClientConnected(t, cc)

	go func() {
		for {
			msg, err := cc.Read()
			if err != nil {
				break
			}

			_, _ = cr.HandleMessage(msg, cc)
		}
	}()

	resolved, rejected, err := cr.Request(cc, "dummy", make([]byte, 0), time.Second)
	if err != nil {
		t.Fatal(err)
	}
	if resolved != nil {
		t.Fatal("Invalid resolve")
	}
	assert.Equal(t, rejected.Message, "invalid method")
}

type Readable interface {
	Read() (*ipc.Message, error)
}

func waitServerReady(t *testing.T, sc *ipc.Server) {
	for {
		m, err := sc.Read()
		if err != nil {
			t.Fatal(err)
		}

		if m.MsgType < 0 && m.Status == ipc.Listening {
			break
		} else {
			t.Error("status = " + m.Status.String())
		}
	}
}

func waitClientConnected(t *testing.T, cc *ipc.Client) {
	for {
		m, err := cc.Read()
		if err != nil {
			t.Error(err)
			break
		}

		if m.MsgType < 0 && m.Status == ipc.Connected {
			break
		} else if m.Status != ipc.Connecting {
			t.Error("status = " + m.Status.String())
		}
	}
}
