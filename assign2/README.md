# The Price is Right Game

## Description

This program implements a simple version of "The Price is Right" game. Players are asked to guess the price of an item, and the player whose guess is the closest to the actual price (without exceeding) it wins.

## Compilation and Execution

### Prerequisites

- Java Development Kit (JDK) installed on your machine. Needs to be a version greater or equal than 21.
- Terminal or command line access.

### Compilation

1. Open a terminal or command line.
2. Navigate to the `src` directory of the project.
3. Compile the `Server.java` and `Session.java` files using `javac`.

    ```sh
    javac Server.java 
    javac Session.java
    ```

### Running the Game

#### Starting the Server

1. In the terminal, navigate to the `src` directory (if not already there).
2. Run the server with the desired game mode and the number of players in the game.

    ```sh
        java Server <gamemode> <playersInGame>
    ```
    - `<gamemode>`: The mode of the game. 1 - Simple 2 - Ranked
    - `<playersInGame>`: The number of players participating in the game.

#### Starting Player Sessions

1. Open additional terminals for each player session.
2. Navigate to the `src` directory in each terminal.
3. Run the session with a unique session ID for each player.

    ```sh
        java Session <sessionId>
    ```

### Example

To start a server for a game mode `ranked` with 3 players:

1. Navigate to the src directory in 4 different terminals.

2. Write the following line in terminal 1
    ```sh
    java Server 2 3
    ```

3. Write the following line in terminal 2
    ```sh
    java Session 1
    ```
4. Write the following line in terminal 3
    ```sh
    java Session 2
    ```

5. Write the following line in terminal 4
    ```sh
    java Session 3
    ```
