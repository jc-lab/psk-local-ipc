# Java Example

```java
import kr.jclab.javautils.psklocalipc.Message;
import kr.jclab.javautils.psklocalipc.client.IpcClient;
import org.bouncycastle.tls.TlsPSKIdentity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ExampleApp {
    public static void main(String[] args) throws IOException {
        IpcClient client = IpcClient.builder()
                .name("testtest2")
                .pskIdentity(new TlsPSKIdentity() {
                    @Override
                    public void skipIdentityHint() {
                        System.err.println("skipIdentityHint");
                    }

                    @Override
                    public void notifyIdentityHint(byte[] psk_identity_hint) {
                        System.err.println("notifyIdentityHint : " + new String(psk_identity_hint));
                    }

                    @Override
                    public byte[] getPSKIdentity() {
                        return "hello".getBytes(StandardCharsets.UTF_8);
                    }

                    @Override
                    public byte[] getPSK() {
                        return "world".getBytes(StandardCharsets.UTF_8);
                    }
                })
                .build();

        while (true) {
            client.write(1, "HELLO WORLD".getBytes(StandardCharsets.UTF_8));
            Message data = client.read();
            System.out.println("recevied : " +data.getMsgType() + " / " +  new String(data.getData()));
        }
    }
}
```