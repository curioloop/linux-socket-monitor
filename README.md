# linux-socket-probe
Java tool for socket metric collection on Linux

### How to use

Build you own version or try use below maven dependency:
```xml
<project>

    <dependencies>
        <dependency>
            <groupId>com.curioloop</groupId>
            <artifactId>linux-socket-probe</artifactId>
            <version>1.0.0</version>
            <!--  Currently only supports x86_64  -->
            <classifier>${os.detected.arch}</classifier>
        </dependency>
    </dependencies>

    <build>
        <extensions>
            <extension>
                <groupId>kr.motd.maven</groupId>
                <artifactId>os-maven-plugin</artifactId>
                <version>${os-maven-plugin.version}</version>
            </extension>
        </extensions>
    </build>

</project>
```

Then:

- If your socket address set is static, please refer to [Demo1](src/test/java/com/curioloop/linux/socket/probe/demo/MicrometerDemo.java).
- When working with a dynamic address set, please refer to [Demo2](src/test/java/com/curioloop/linux/socket/probe/demo/MicrometerDemoWithAgg.java).


### Build this project with Docker

1. Create builder image
> docker build -t linux-socket-probe-builder:latest - < Dockerfile

2. Enter builder container
> docker run -it --rm -v "$HOME/.m2":/root/.m2 -v "$PWD":/linux-socket-probe -w /linux-socket-probe linux-socket-probe-builder:latest

4. Build with maven
> mvn clean test\
> mvn clean javadoc:jar source:jar install

5. Deploy to central
> gpg --import /root/.m2/private.key\
> mvn clean javadoc:jar source:jar deploy -DcreateChecksum=true -Dgpg.skip=false

### Notes on use ...
- Some code use [abort()](https://man7.org/linux/man-pages/man3/abort.3.html) to handle error
- Error prompts need to be improved

### Many thanks to ...

- [Passive monitoring of sockets on Linux](http://kristrev.github.io/2013/07/26/passive-monitoring-of-sockets-on-linux)
- [Why and How to Use Netlink Socket](https://www.linuxjournal.com/article/7356)
- [netlink](https://manpages.ubuntu.com/manpages/bionic/man7/sock_diag.7.html)
- [ss.c](https://github.com/sivasankariit/iproute2/blob/master/misc/ss.c)
- [inet_diag.c](https://github.com/torvalds/linux/blob/master/net/ipv4/inet_diag.c)

