# Object-stream prototype preserved after merge

These six original files were brought in by main and kept here unchanged for the team.
They depend on absent com.auction.shared model/network classes, missing business services,
event publishers, DAO implementations and SLF4J. ServerRequestDispatcher also contains
placeholder LOGIN/BID handlers that acknowledge success without executing those operations.

They are not part of the current application or its Maven source roots. They cannot talk
to the length-prefixed UTF-8 JSON transport used by the integrated JavaFX client.
Do not add this directory as a source root without completing and deliberately migrating
the entire protocol, its data contracts and its integration tests.

The working entry point is auction-server's com.auction.server.network.Server;
com.auction.server.core.AuctionServer delegates to it. The product classes from main
(Art, Electronics, Vehicle) remain in active source, have .java extensions and are
instantiated by ItemFactory. The remaining model changes from main are preserved.
