# Original Object-stream drafts

The files here are unchanged copies from main, renamed .java.txt to preserve their
content outside the compiled source set. They depend on absent com.auction.shared,
business service/event classes and SLF4J. They do not match the integrated JSON
transport. No domain model, DAO or business algorithm is replaced by this move.
The active transport delegates business operations through BackendGateway.
