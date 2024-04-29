/*
 * Copyright © 2024 CurioLoop (curioloops@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.curioloop.linux.socket.probe.test;

import com.curioloop.linux.socket.probe.LinuxSocketProbe;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LinuxSocketTest {

    static final String data = "Hello World\n";

    @SneakyThrows
    static void prepareSocket(String ip, int port, CountDownLatch done) {
        CountDownLatch serverReady = new CountDownLatch(1);
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(port)) {
                serverReady.countDown();
                Socket client = server.accept();
                OutputStream stream = client.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                log.info("server started {} -> {}", client.getLocalPort(), client.getPort());
                while (done.getCount() > 0) {
                    stream.write(data.getBytes());
                    reader.readLine();
                    TimeUnit.MILLISECONDS.sleep(1);
                }
            } catch (Throwable e) {
                log.error("server error", e);
            }
            log.info("server quit");
        }).start();
        Assertions.assertTrue(serverReady.await(1, TimeUnit.SECONDS));

        CountDownLatch clientReady = new CountDownLatch(1);
        new Thread(() -> {
            try (Socket client = new Socket(ip, port)) {
                clientReady.countDown();
                OutputStream stream = client.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                log.info("client started (ip:{}) {} -> {}", ip, client.getLocalPort(), client.getPort());
                while (done.getCount() > 0) {
                    stream.write(data.getBytes());
                    reader.readLine();
                    TimeUnit.MILLISECONDS.sleep(1);
                }
            } catch (Throwable e) {
                log.error("client error", e);
            }
            log.info("client quit");
        }).start();
        Assertions.assertTrue(clientReady.await(1, TimeUnit.SECONDS));
    }

    @BeforeAll
    public static void loadJNI() {
        Assertions.assertNull(LinuxSocketProbe.unavailabilityCause());
    }

}
