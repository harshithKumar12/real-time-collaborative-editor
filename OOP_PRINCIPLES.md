# OOP Principles Implementation Guide

This document explains how Object-Oriented Programming principles are implemented in this project, useful for college viva/examination.

## 1. Encapsulation

**Definition**: Bundling data and methods that operate on that data within a single unit (class), and restricting access to internal details.

### Examples in this project:

#### `ClientHandler.java`
```java
private Socket socket;              // Private field - hidden from outside
private ServerBroadcaster broadcaster;
private String username;
private boolean connected;

// Controlled access through methods
public String getUsername() { return username; }
public boolean isConnected() { return connected; }
```

#### `ServerBroadcaster.java`
```java
private final CopyOnWriteArrayList<ClientHandler> clients;  // Private collection

// Public methods provide controlled access
public void addClient(ClientHandler client) { ... }
public void removeClient(ClientHandler client) { ... }
```

#### `SharedConstants.java`
```java
private SharedConstants() {  // Private constructor prevents instantiation
    throw new IllegalStateException("Utility class - cannot be instantiated");
}
```

**Viva Question**: "How is encapsulation implemented?"
**Answer**: "We use private fields to hide internal implementation details. For example, in `ClientHandler`, the socket and connection state are private, and we provide controlled access through public getter methods. This prevents external classes from directly modifying internal state."

---

## 2. Abstraction

**Definition**: Hiding complex implementation details and showing only essential features.

### Examples in this project:

#### `MessageListener` Interface (`ClientConnection.java`)
```java
public interface MessageListener {
    void onMessageReceived(String message);
    void onConnectionLost();
}
```
- **Abstraction**: UI doesn't need to know HOW messages are received, only that they WILL be received
- `UIController` implements this interface without knowing socket implementation details

#### `SharedConstants.java`
- Abstracts protocol details - other classes don't need to know message format internals
- Provides utility methods like `buildMessage()` and `parseMessage()`

**Viva Question**: "What is abstraction in your project?"
**Answer**: "We use the `MessageListener` interface to abstract network communication. The `UIController` doesn't need to know about sockets or streams - it just implements the interface and receives callbacks when messages arrive. This separation of concerns is abstraction."

---

## 3. Interfaces

**Definition**: Contracts that define what methods a class must implement, enabling polymorphism and loose coupling.

### Examples in this project:

#### `MessageListener` Interface
```java
// In ClientConnection.java
public interface MessageListener {
    void onMessageReceived(String message);
    void onConnectionLost();
}

// UIController implements it
public class UIController implements ClientConnection.MessageListener {
    @Override
    public void onMessageReceived(String message) { ... }
    @Override
    public void onConnectionLost() { ... }
}
```

**Benefits**:
- Loose coupling: `ClientConnection` doesn't depend on `UIController` directly
- Easy to test: Can create mock implementations
- Flexible: Can have multiple listeners

**Viva Question**: "Why use interfaces?"
**Answer**: "Interfaces provide a contract. `ClientConnection` doesn't need to know about `UIController` - it just calls methods on the `MessageListener` interface. This makes the code more flexible and testable. We can easily swap implementations without changing `ClientConnection`."

---

## 4. Composition

**Definition**: "Has-a" relationship - a class contains references to other classes as instance variables.

### Examples in this project:

#### `ServerMain` composes `ServerBroadcaster`
```java
public class ServerMain {
    private ServerBroadcaster broadcaster;  // Composition - "has-a" relationship
    
    public ServerMain() {
        this.broadcaster = new ServerBroadcaster();  // Creates the object
    }
}
```

#### `UIController` composes `ClientConnection`
```java
public class UIController {
    private ClientConnection connection;  // Composition
    
    private void handleConnect() {
        connection = new ClientConnection(this);  // Creates the object
    }
}
```

#### `ClientHandler` uses `ServerBroadcaster` (composition via constructor)
```java
public class ClientHandler {
    private final ServerBroadcaster broadcaster;  // Reference to composed object
    
    public ClientHandler(Socket socket, ServerBroadcaster broadcaster) {
        this.broadcaster = broadcaster;  // Receives reference
    }
}
```

**Viva Question**: "What is composition?"
**Answer**: "Composition is when a class contains references to other classes. For example, `ServerMain` has a `ServerBroadcaster` object. This is a 'has-a' relationship. The server uses the broadcaster to send messages, but they are separate objects that work together."

---

## 5. Polymorphism

**Definition**: Same interface, different implementations - one interface, multiple forms.

### Examples in this project:

#### Message Handling (Runtime Polymorphism)
```java
// In ClientHandler.java and UIController.java
private void handleMessage(String message) {
    String[] parts = parseMessage(message);
    String messageType = parts[0];
    
    switch (messageType) {  // Same method, different behaviors
        case MSG_JOIN:
            handleJoin(parts);
            break;
        case MSG_EDIT:
            handleEdit(parts);
            break;
        case MSG_DELETE:
            handleDelete(parts);
            break;
        // ... different message types handled differently
    }
}
```

#### Interface Polymorphism
```java
// ClientConnection can work with any MessageListener implementation
MessageListener listener = new UIController();  // Polymorphic reference
connection = new ClientConnection(listener);
```

**Viva Question**: "How is polymorphism used?"
**Answer**: "We use polymorphism in message handling. The same `handleMessage()` method processes different message types (JOIN, EDIT, DELETE) differently. Also, `ClientConnection` accepts any `MessageListener` implementation, not just `UIController`. This is interface polymorphism."

---

## 6. MVC/MVVM Pattern

**Definition**: Separates application into three components:
- **Model**: Data and business logic
- **View**: User interface
- **Controller**: Mediates between Model and View

### Implementation in this project:

#### Model Layer
- `ClientConnection.java` - Handles data/network operations
- `ServerBroadcaster.java` - Manages data distribution

#### View Layer
- JavaFX UI components in `UIController.java` (TextArea, Buttons, Labels)

#### Controller Layer
- `UIController.java` - Mediates between UI (View) and `ClientConnection` (Model)
- `ClientHandler.java` - Server-side controller handling client requests

**Flow**:
```
User types in TextArea (View)
    ↓
UIController detects change (Controller)
    ↓
ClientConnection sends to server (Model)
    ↓
Server processes and broadcasts
    ↓
ClientConnection receives (Model)
    ↓
UIController updates TextArea (Controller → View)
```

**Viva Question**: "Explain MVC pattern in your project."
**Answer**: "We follow MVC architecture. The View is the JavaFX UI (TextArea, buttons). The Model is `ClientConnection` which handles network communication. The Controller is `UIController` which listens to user actions, updates the model, and updates the view when data changes. This separation makes the code maintainable and testable."

---

## Additional Design Patterns

### 1. Thread-per-Client Pattern
- Each client connection gets its own thread
- Implemented in `ServerMain.java`:
```java
ClientHandler clientHandler = new ClientHandler(clientSocket, broadcaster);
Thread clientThread = new Thread(clientHandler);
clientThread.start();
```

### 2. Observer Pattern (via Interface)
- `MessageListener` interface acts as observer
- `ClientConnection` notifies observers when messages arrive

### 3. Singleton-like Pattern
- `SharedConstants` - utility class with static methods, no instances

---

## Key Points for Viva

1. **Encapsulation**: Private fields, public methods, controlled access
2. **Abstraction**: Interfaces hide implementation details
3. **Interfaces**: `MessageListener` enables loose coupling
4. **Composition**: Classes contain references to other objects
5. **Polymorphism**: Same method handles different message types
6. **MVC**: Clear separation of Model, View, and Controller

## Common Viva Questions

**Q: Why did you use composition instead of inheritance?**
A: "Composition provides more flexibility. For example, `ServerMain` uses `ServerBroadcaster` but they are separate concerns. If we used inheritance, we'd have tight coupling. Composition allows us to change implementations easily."

**Q: How do you prevent infinite loops in text synchronization?**
A: "We use flags like `isApplyingExternalEdit` in `UIController`. When we receive an external edit, we set this flag, apply the change, then clear it. This prevents our own text change listener from triggering and sending the edit back to the server."

**Q: How is multithreading implemented?**
A: "We use the thread-per-client model. Each `ClientHandler` runs in its own thread. The server's main thread accepts connections and spawns new threads. We use `CopyOnWriteArrayList` for thread-safe operations in `ServerBroadcaster`."

**Q: What is the protocol format?**
A: "We use a simple text-based protocol: `MESSAGE_TYPE|DATA1|DATA2|...`. For example, `EDIT|5|Hello|username` means insert 'Hello' at position 5. The delimiter is `|` and we parse messages using `parseMessage()` utility method."

---

## File Structure and Responsibilities

| File | Responsibility | OOP Principle |
|------|---------------|---------------|
| `ServerMain.java` | Server entry point, accepts connections | Composition |
| `ClientHandler.java` | Handles individual client | Encapsulation, Polymorphism |
| `ServerBroadcaster.java` | Broadcasts to all clients | Encapsulation |
| `ClientMain.java` | Client entry point | MVC (View setup) |
| `ClientConnection.java` | Network communication | Abstraction, Interfaces |
| `UIController.java` | UI control and logic | MVC (Controller), Polymorphism |
| `SharedConstants.java` | Protocol definitions | Abstraction, Encapsulation |

---

*This document is prepared for educational purposes and college project viva examination.*

