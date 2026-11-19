package com.auction.server.network;

import static com.auction.shared.JsonProtocol.number;
import static com.auction.shared.JsonProtocol.obj;
import static com.auction.shared.JsonProtocol.text;

import com.auction.server.assistant.AuctionAssistant;
import com.auction.server.exception.ApiException;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AuctionService.AssistantContext;
import com.auction.shared.AssistantChat;
import com.auction.shared.JsonProtocol;
import com.google.gson.JsonObject;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Framed TCP JSON, ordered bounded output per client, no I/O in transactions. */
public final class Server implements AutoCloseable {
  public static final int MAX_FRAME = JsonProtocol.MAX_FRAME;
  private static Server instance;

  /** Một Server đang hoạt động cho mỗi tiến trình; chỉ tạo lại sau khi đóng. */
  public static synchronized Server getInstance() throws IOException {
    if (instance == null || instance.closed.get()) {
      instance =
          new Server(
              System.getProperty("auction.bind", "0.0.0.0"),
              Integer.getInteger("auction.port", 8888),
              Path.of(System.getProperty("auction.data", "data")),
              false);
    }
    return instance;
  }

  private final ServerSocket listener;
  private final AuctionService service;
  private final AuctionAssistant assistant;
  private final ExecutorService aiWorkers =
      new ThreadPoolExecutor(
          4,
          4,
          0,
          TimeUnit.SECONDS,
          new SynchronousQueue<>(),
          Thread.ofPlatform().name("auction-ai-", 0).factory());
  private final Set<Client> clients = ConcurrentHashMap.newKeySet();
  private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final AtomicBoolean closed = new AtomicBoolean();
  private final Thread acceptor;

  private Server(String host, int port, Path data, boolean demo) throws IOException {
    this(host, port, data, demo, AuctionAssistant.fromEnvironment());
  }

  private Server(String host, int port, Path data, boolean demo, AuctionAssistant assistant)
      throws IOException {
    this.assistant = assistant;
    listener = new ServerSocket();
    listener.setReuseAddress(true);
    try {
      listener.bind(new InetSocketAddress(host, port));
      service = new AuctionService(data, Clock.system(AuctionService.ZONE), demo, this::publish);
    } catch (IOException | RuntimeException e) {
      listener.close();
      workers.close();
      scheduler.shutdownNow();
      assistant.close();
      throw e;
    }
    acceptor = Thread.ofPlatform().name("auction-accept").unstarted(this::accept);
  }

  /** Returns the bound TCP port, including a port selected automatically for a test. */
  public int port() {
    return listener.getLocalPort();
  }

  /** Starts accepting clients and schedules automatic auction transitions. */
  public void start() {
    acceptor.start();
    scheduler.scheduleAtFixedRate(
        () -> {
          try {
            service.tick();
          } catch (RuntimeException e) {
            System.err.println("Scheduler: " + e.getMessage());
          }
        },
        0,
        250,
        TimeUnit.MILLISECONDS);
  }

  private void accept() {
    while (!closed.get()) {
      try {
        Socket socket = listener.accept();
        if (clients.size() >= 200) {
          socket.close();
          continue;
        }
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(60_000);
        Client c = new Client(socket);
        clients.add(c);
        System.out.println("Client connected: " + socket.getRemoteSocketAddress());
        workers.submit(c::writeLoop);
        workers.submit(c::readLoop);
      } catch (IOException e) {
        if (!closed.get()) {
          System.err.println("Accept: " + e.getMessage());
        }
      }
    }
  }

  private void publish(JsonObject event) {
    for (Client c : clients) {
      String target = text(event, "userId");
      String auction = text(event, "auctionId");
      if (!target.isEmpty()
          ? target.equals(c.userId)
          : c.feed || auction.isEmpty() || c.subscriptions.contains(auction)) {
        c.offer(event);
      }
    }
  }

  // Các test và client dùng cùng một cách đóng gói thông điệp.
  /** Reads a length-prefixed JSON frame using the shared protocol. */
  public static JsonObject readFrame(DataInputStream in) throws IOException {
    return JsonProtocol.readFrame(in);
  }

  /** Writes and flushes a length-prefixed UTF-8 JSON frame. */
  public static void writeFrame(DataOutputStream out, JsonObject value) throws IOException {
    JsonProtocol.writeFrame(out, value);
  }

  private final class Client implements AutoCloseable {
    private final Socket socket;
    private final BlockingQueue<JsonObject> outbound = new ArrayBlockingQueue<>(256);
    private final Set<String> subscriptions = ConcurrentHashMap.newKeySet();
    private volatile String userId = "";
    private volatile String session = "";
    private volatile boolean done;
    private volatile boolean feed;
    private ChatWork pendingChat;

    Client(Socket socket) {
      this.socket = socket;
    }

    void offer(JsonObject value) {
      if (!done && !outbound.offer(value)) {
        close();
      }
    }

    void writeLoop() {
      try (DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
        while (!done) {
          JsonObject next = outbound.poll(1, TimeUnit.SECONDS);
          if (next != null) {
            writeFrame(out, next);
          }
        }
      } catch (IOException | InterruptedException e) {
        if (e instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      } finally {
        close();
      }
    }

    void readLoop() {
      try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
        while (!done) {
          JsonObject request = readFrame(in);
          JsonObject response;
          String requestId = "";
          try {
            requestId = text(request, "requestId");
            if (number(request, "protocolVersion") != 1
                || requestId.isBlank()
                || requestId.length() > 100) {
              throw new ApiException("PROTOCOL_ERROR", "protocolVersion=1 and requestId required");
            }
            String action = text(request, "action");
            String token = text(request, "token");
            JsonObject payload =
                request.has("payload") ? request.getAsJsonObject("payload") : new JsonObject();
            if (action.equals(AssistantChat.ACTION)) {
              if (!session.isBlank() && !session.equals(token)) {
                throw new ApiException("UNAUTHENTICATED", "Assistant session does not match");
              }
              AssistantContext facts = service.assistantContext(token, payload);
              String question = validatedQuestion(payload);
              session = token;
              userId = facts.userId();
              startChat(new ChatWork(requestId, token, facts, question));
              continue;
            }
            if (!token.equals(session)) {
              cancelChat();
              subscriptions.clear();
              feed = false;
              userId = "";
              session = token;
            }
            if (action.equals("SUBSCRIBE_FEED")) {
              userId = service.authenticatedUserId(token);
              feed = true;
              response = obj("success", true, "result", new JsonObject());
            } else if (action.equals("SUBSCRIBE_AUCTION")) {
              userId = service.authenticatedUserId(token);
              String id = text(payload, "auctionId");
              subscriptions.add(id);
              response = service.execute("GET_AUCTION", token, "", payload);
              if (!response.get("success").getAsBoolean()) {
                subscriptions.remove(id);
              }
            } else if (action.equals("UNSUBSCRIBE_AUCTION")) {
              subscriptions.remove(text(payload, "auctionId"));
              response = obj("success", true, "result", new JsonObject());
            } else {
              response = service.execute(action, token, text(request, "commandId"), payload);
            }
            if (action.equals("LOGIN") && response.get("success").getAsBoolean()) {
              cancelChat();
              session = text(response.getAsJsonObject("result"), "token");
              userId = service.authenticatedUserId(session);
            } else if (action.equals("LOGOUT")) {
              cancelChat();
              subscriptions.clear();
              feed = false;
              userId = "";
              session = "";
            } else if (!token.isBlank()) {
              userId = service.authenticatedUserId(token);
            }
          } catch (ApiException e) {
            response = rejection(e);
          } catch (RuntimeException e) {
            response =
                obj(
                    "success",
                    false,
                    "errorCode",
                    "INVALID_REQUEST",
                    "message",
                    "Malformed request");
          }
          respond(requestId, response);
        }
      } catch (IOException e) {
        if (!done && !(e instanceof EOFException)) {
          System.err.println(
              "Client read failed: "
                  + socket.getRemoteSocketAddress()
                  + " ("
                  + e.getClass().getSimpleName()
                  + ")");
        }
      } finally {
        close();
      }
    }

    private void respond(String requestId, JsonObject response) {
      response.addProperty("type", "response");
      response.addProperty("protocolVersion", 1);
      response.addProperty("requestId", requestId);
      offer(response);
    }

    private synchronized void startChat(ChatWork work) {
      if (done) {
        return;
      }
      if (pendingChat != null) {
        throw new ApiException("AI_BUSY", "An assistant request is already running");
      }
      pendingChat = work;
      try {
        aiWorkers.execute(work.task);
      } catch (RejectedExecutionException e) {
        pendingChat = null;
        throw new ApiException("AI_BUSY", "Assistant workers are busy");
      }
    }

    private synchronized void cancelChat() {
      if (pendingChat != null) {
        pendingChat.task.cancel(true);
        pendingChat = null;
      }
    }

    private synchronized void finishChat(ChatWork work, JsonObject response) {
      if (pendingChat == work) {
        pendingChat = null;
        respond(work.requestId, response);
      }
    }

    private final class ChatWork {
      private final String requestId;
      private final String token;
      private final AssistantContext facts;
      private final String question;
      private final FutureTask<Void> task = new FutureTask<>(this::run, null);

      ChatWork(String requestId, String token, AssistantContext facts, String question) {
        this.requestId = requestId;
        this.token = token;
        this.facts = facts;
        this.question = question;
      }

      private void run() {
        JsonObject response;
        try {
          var reply =
              assistant.ask(
                  facts.userId(),
                  facts.product(),
                  question,
                  () ->
                      service.assistantContext(
                          token, obj("auctionId", facts.product().auctionId())));
          response =
              obj(
                  "success",
                  true,
                  "result",
                  obj(
                      "answer",
                      reply.answer(),
                      "currentPrice",
                      reply.currentPrice(),
                      "hasBids",
                      reply.hasBids(),
                      "marketSummary",
                      reply.marketSummary()));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        } catch (ApiException e) {
          response = rejection(e);
        } catch (RuntimeException e) {
          response = rejection(new ApiException("AI_PROVIDER_ERROR", "Assistant request failed"));
        }
        finishChat(this, response);
      }
    }

    @Override
    public synchronized void close() {
      if (done) {
        return;
      }
      done = true;
      cancelChat();
      clients.remove(this);
      System.out.println("Client disconnected: " + socket.getRemoteSocketAddress());
      subscriptions.clear();
      try {
        socket.close();
      } catch (IOException ignored) {
        // The connection is already failing or closing; there is no recovery here.
      }
    }
  }

  private static String validatedQuestion(JsonObject payload) {
    var value = payload.get("question");
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
      throw new ApiException("VALIDATION_ERROR", "Invalid question");
    }
    String question = value.getAsString().strip();
    if (question.isEmpty() || question.length() > AssistantChat.MAX_QUESTION_LENGTH) {
      throw new ApiException("VALIDATION_ERROR", "Invalid question");
    }
    return question;
  }

  private static JsonObject rejection(ApiException e) {
    return obj("success", false, "errorCode", e.code(), "message", e.getMessage());
  }

  @Override
  public void close() throws IOException {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    listener.close();
    scheduler.shutdownNow();
    clients.forEach(Client::close);
    aiWorkers.shutdownNow();
    assistant.close();
    workers.shutdownNow();
    try {
      scheduler.awaitTermination(5, TimeUnit.SECONDS);
      workers.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    service.close();
  }

  /** Starts the TCP server and waits for its acceptor thread. */
  public static void main(String[] args) throws Exception {
    Server server = getInstance();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    server.close();
                  } catch (IOException e) {
                    System.err.println(e.getMessage());
                  }
                }));
    server.start();
    System.out.println(
        "Auction JSON server listening on " + server.listener.getLocalSocketAddress());
    server.acceptor.join();
  }
}
