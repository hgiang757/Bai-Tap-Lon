# Member 1 / Member 3 network contract

This is the boundary proposed for the business owner to implement. This branch
ships only transport, concurrency coordination, UI and client adapters. It does
not ship the business provider, account database, wallet engine, auto-bid engine,
anti-sniping policy or settlement implementation.

## Wire framing

TCP on 127.0.0.1:8888 by default. A frame is a 4-byte big-endian signed length,
then UTF-8 JSON. Length must be positive and <= 8 MiB. No ObjectInputStream and
no readUTF/writeUTF. Each client has a single response reader; requestId routes
responses to their waiting calls. One bounded writer queue preserves outbound
order and drops a slow connection rather than blocking the backend.

Request:

```json
{"protocolVersion":1,"requestId":"request-1","action":"PING","token":"","commandId":"","payload":{}}
```

Response:

```json
{"protocolVersion":1,"type":"response","requestId":"request-1","success":true,"result":{"status":"OK","backendConfigured":false}}
```

Error responses have success=false, errorCode and message. If no BackendGateway
provider is installed, every business operation returns BACKEND_NOT_CONFIGURED.
No real accounts or seeded login success exist in this network-only server.

## Provider responsibilities (member 2, collaborating with member 4)

Implement BackendGateway; register the fully qualified provider name at
META-INF/services/com.auction.server.network.BackendGateway. The provider uses
existing model/service/DAO implementations and must not put that logic in the
transport or JavaFX controller.

- execute(action,token,commandId,payload) validates permissions, invokes the right
  service, commits/rolls back and produces an immutable JSON response snapshot.
- authenticatedUserId(token) validates the session and returns its true userId.
  Throw NetworkException with UNAUTHENTICATED for invalid/expired sessions.
- setEventSink(consumer) registers an Observer callback. Emit events only after
  successful commit. Never write directly to client sockets inside a transaction.
- tick() is an entry point for the provider's own scheduler logic. The transport
  calls it every 250 ms but does not open/close auctions or choose winners itself.
- close() releases resources owned by the provider.

All gateway entry points are guarded by a fair ReentrantLock in
ServerRequestDispatcher. This serializes cross-client updates entering through
this boundary, including scheduler ticks. Domain validation, atomic persistence,
rollback, winner uniqueness and idempotent financial operations still require
implementation by the provider; a network lock alone cannot supply them.

## Realtime / Observer

SUBSCRIBE_FEED validates the token and enables public auction events plus private
events for the same authenticated user. SUBSCRIBE_AUCTION with payload auctionId
subscribes before requesting GET_AUCTION, so the client can reconcile a snapshot
with any queued events. UNSUBSCRIBE_AUCTION releases that interest. Token changes,
logout and disconnected sockets release prior subscriptions.

Events use type=event, eventId, event, version, auctionId, userId, serverTime and
an auction snapshot if relevant. Non-empty userId means a private event: it is
never broadcast to another user even if they subscribe to the auction feed.
BID_PLACED also carries bid. Provider increments versions and ensures event
snapshots are consistent with the committed transaction.

Clients discard older versions or refresh authoritative snapshots on reconnect.
A single heartbeat every 5 seconds keeps the connection alive, restores its feed
and reports disconnection. This is not polling for auction prices. The server
closes connections idle for 60 seconds; outbound capacity is 256 frames/client
and the connection limit is 200.

## Actions expected by the existing frontend adapters

These names and DTO fields describe the client-side contract, not completed
business operations in this branch:

- Auth: REGISTER(username,password,fullName,email,role), LOGIN(username,password),
  LOGOUT, GET_ME. LOGIN returns token and user profile (id,username,fullName,email,
  role,balance); registration profile is followed by login. Never send password
  hashes in a profile.
- Auctions: LIST_AUCTIONS(status?), GET_AUCTION(auctionId),
  GET_BID_HISTORY(auctionId), GET_ROOM_SNAPSHOT(auctionId) returning auction+bids.
- Products: LIST_MY_PRODUCTS, CREATE_PRODUCT_AUCTION, UPDATE_PRODUCT_AUCTION,
  DELETE_PRODUCT_AUCTION, GET_IMAGE. Upload imageBase64 in CREATE/UPDATE; an empty
  string removes the image. The network layer bounds frames, while the provider
  must check file size, dimensions, ownership and allowed content.
- Bidding: PLACE_BID(auctionId,amount), SET_MY_AUTO_BID(auctionId,maxBid,increment),
  CANCEL_MY_AUTO_BID, GET_MY_AUTO_BID. The provider and member 4 own the actual
  validation, budget, ordering and automatic-bid policies.
- Wallet UI: GET_MY_WALLET_SNAPSHOT returns balance,held,available,transactions;
  DEPOSIT_SIMULATED, WITHDRAW_SIMULATED. These are UI adapter calls, not a wallet
  service implemented here.
- Admin UI: LIST_USERS, ADMIN_LIST_AUCTIONS, LOCK_USER, UNLOCK_USER,
  CHANGE_USER_ROLE, DELETE_USER, CANCEL_AUCTION(reason), DELETE_AUCTION.
- Command recovery: GET_COMMAND_STATUS; all business writes carry stable UUID
  commandId. The provider must persist and deduplicate by authenticated user and
  commandId. The client saves pending commands without passwords/tokens, and
  replays the same commandId after an uncertain response. It must never create a
  fresh bid merely because an acknowledgement was lost.

Auction snapshot fields: id,itemId,name,description,category,sellerUsername,
startingPrice,currentPrice,minBidStep,startTime,endTime,status,
highestBidderUsername,totalBids; cancellationReason when canceled. Bid fields:
id,auctionId,bidderUsername,amount,timestamp,auto. Category codes: ELECTRONICS,
ART,VEHICLE,JEWELRY,REAL_ESTATE,OTHER. Prices are integer VND; times are ISO local
in Asia/Ho_Chi_Minh. State names: OPEN,RUNNING,FINISHED,PAID,CANCELED. These choices
must be agreed with the backend owner before switching the UI to SERVER.

## What is unchanged

The branch starts at origin/main. No DAO, server domain model, business service,
Factory Method, auto-bid/anti-sniping algorithm, existing quality test or CI
workflow is modified. The two POM edits only add the JSON dependency and the
launcher needed by the network/client code. Old incomplete Object-stream source
is preserved unchanged in module-local reference folders with .java.txt names.

## Verification

Build network: mvn -B -f auction-server/pom.xml verify
Build UI: mvn -B -f auction-client-ui/pom.xml test -Dauction.ui.tests=true

Transport tests inject a test-only gateway, exercise real TCP connections,
private/public push isolation and concurrent updates. They do not claim that
the real business provider is already integrated or validated.
