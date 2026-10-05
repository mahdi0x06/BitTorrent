# Peer-to-Peer File Sharing in Java

A coursework implementation of a tracker-assisted peer-to-peer file-sharing system. The tracker maintains peer and file information; peers exchange file data directly using socket connections and concurrent connection handlers.

This is a custom educational protocol, not a client claiming interoperability with the public BitTorrent protocol.

## Implementation

- Separate tracker and peer entry points.
- Java socket connections and threaded listeners/connection handlers.
- JSON messages using the bundled Gson dependency.
- Shared-directory file discovery and tracker registration.
- Peer file-download commands and MD5-based file-integrity checks.

MD5 here is a coursework integrity mechanism, not a guarantee against maliciously constructed collisions.

## Build

Install a JDK providing `javac` and `jar`. Run both build scripts from the nested `BitTorrent/` directory, since they use relative source and library paths:

```bash
git clone https://github.com/mahdi0x06/BitTorrent.git
cd BitTorrent/BitTorrent
bash build_tracker.sh
bash build_peer.sh
```

The scripts produce `tracker.jar` and `peer.jar` and package the dependency in `lib/` into those JARs.

## Run locally

Start the tracker in one terminal:

```bash
java -jar tracker.jar 10000
```

Create a shared folder and start a peer in another terminal:

```bash
mkdir -p shared-peer-1
java -jar peer.jar 127.0.0.1:10001 127.0.0.1:10000 shared-peer-1
```

For another peer, use a different listening port and shared folder. The peer arguments are **self address, tracker address, shared folder**. Both processes accept commands interactively; consult `peer/controllers/PeerCLIController.java` and `tracker/controllers/TrackerCLIController.java` for the implemented command syntax.

## Repository contents

- `BitTorrent/common/`: shared message definitions, JSON utilities, file utilities, and hashing.
- `BitTorrent/peer/`: peer application, socket handlers, and CLI controllers.
- `BitTorrent/tracker/`: tracker application, listener, connection handlers, and CLI controllers.
- `BitTorrent/lib/`: bundled Gson JAR.
- `BitTorrent/1/`, `2/`, `6/`, `7/`: supplied shell-based scenario tests.

The scenario scripts depend on a parent `setup.sh` and an external Docker test harness that are not present in this repository. They are retained as test scenarios, not advertised as self-contained tests that can run immediately after cloning. Compiled files under `BitTorrent/out/` are preexisting generated artifacts; rebuild from source using the scripts above.
