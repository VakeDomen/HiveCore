# HiveCore


HiveCore is a proxy solution designed to unify scattered machines running [Ollama](https://github.com/ollama/ollama) into a single, cohesive API. Whether you have multiple nodes each capable of Ollama inference or want a centralized gateway for distributing client requests, HiveCore helps you coordinate, monitor, and manage these worker nodes with ease.

# Table of Contents

1.  [**Overview**](#1-overview)
2.  [**Key Features**](#2-key-features)
3.  [**Getting Started**](#3-getting-started)
4.  [**Architecture & Components**](#4-architecture--components)
5.  [**Usage & Configuration**](#5-usage--configuration)
6.  [**API Endpoints**](#6-api-endpoints)
      - [Authentication & Admin Requests](#authentication--admin-requests)
      - [\`/key\`](#key)
      - [\`/queue\`](#queue)
      - [\`/worker/connections\`](#workerconnections)
      - [\`/worker/status\`](#workerstatus)
      - [\`/worker/pings\`](#workerpings)
      - [\`/worker/tags\`](#workertags)
      - [\`/worker/version/hive\`](#workerversionhive)
      - [\`/worker/version/ollama\`](#workerversionollama)
7.  [**Contributing**](#7-contributing)
8.  [**License**](#8-license)

* * *

# 1. Overview


**HiveCore** aims to streamline the process of distributing Ollama inference requests across multiple worker nodes. By acting as a proxy, HiveCore allows you to:

*   **Centralize** client requests into one API endpoint, simplifying how your users interact with Ollama across various machines.
*   **Authenticate** and **manage** nodes, ensuring only verified systems can participate.
*   **Distribute** requests intelligently to available nodes, balancing load for optimal performance.
*   **Monitor** and **manage** keys, queues, and node statuses through HTTP endpoints.

* * *

# 2. Key Features


*   **Ollama Proxy & Load Distribution**  
    Collect all your Ollama-ready machines under one unified API to handle inference requests.

*   **Node Management & Monitoring**  
    Keep track of connected worker nodes, verifying their status, ping times, and capabilities.

*   **Queue Management**  
    Incoming requests are queued and served as worker nodes become available, ensuring a smooth, organized flow of tasks.

*   **Admin REST APIs**  
    A suite of administrative endpoints for listing or inserting keys, inspecting worker nodes, and monitoring queue lengths.

*   **SQLite Integration**  
    Authentication keys (and potentially more metadata) are stored and managed in a local SQLite database by default.


* * *

# 3. Getting Started


1.  **Clone the Project**

    ```bash
    git clone https://github.com/VakeDomen/HiveCore.git
    cd HiveCore
    ```

3.  **Set Up the Database**

    *   By default, HiveCore uses SQLite. Ensure your environment is capable of running SQLite. The location of the sqlite db file is configured in the `Config`.
4.  **Build & Run**

    *   Use Maven to compile the code:

        ```bash
        mvn clean install
        ```

    *   Run the generated jar or start the main class that launches **ClientServer**, **ManagementServer**, and **NodeServer**.

* * *

# 4. Architecture & Components


Below is a high-level overview of the key components:

*   **NodeServer**  
    Listens for worker node connections running Ollama. Each node is required to authenticate. Once authenticated, tasks can be dispatched to it.

*   **ClientServer**  
    Receives client requests for Ollama inference. These requests are placed in a queue and assigned to worker nodes as they become available.

*   **ManagementServer**  
    Provides administrative endpoints for managing keys, viewing queue lengths, and checking node statuses.

*   **NodeConnectionMonitor**  
    Periodically checks all connected nodes for timeouts or verification issues, removing inactive or unauthorized connections.

*   **RequestQue**  
    Maintains a queue of incoming requests from clients, ensuring orderly distribution to worker nodes.

*   **DatabaseManager**  
    Handles key storage in SQLite, offering CRUD operations for authentication keys.


* * *

# 5. Usage & Configuration

1.  **Configuration File**
    * When the HiveCore runs it first checks if there exists a `config.ini` file. If not, it creates one.

    *   **`config.ini`**: Contains ports, timeout settings, and flags for authentication. Edit this file to change the default ports or enable/disable user authentication.
3.  **Authentication**

    *   Endpoints require a **Bearer** token. For admin-specific endpoints, the token must belong to a key with the **Admin** role.
4.  **Logs**

    *   Logging is provided by `Logger`. Watch the console or redirect output to a file for insights into request handling, node connections, and potential errors.
5.  **Scaling**

    *   To scale horizontally, run additional worker nodes (each with Ollama) and point them to the same NodeServer. The NodeConnectionMonitor ensures newly connected nodes are recognized after authentication.

* * *

# 6. API Endpoints


### Authentication & Admin Requests

*   **Authorization**
    *   Include the HTTP header `Authorization: Bearer <token>` where `<token>` is a valid key from your database.
    *   **Admin Requests**: Must use a token belonging to an Admin role, or the call will be rejected.

* * *

### `/key`

*   **GET** `/key`

    *   **Purpose**: Retrieve a list of all stored authentication keys.
    *   **Authorization**: Admin only.
    *   **Responses**:
        *   **200 OK** + JSON array of keys.
        *   **404 Not Found** if no keys exist.
        *   **400 Bad Request** if an error occurs.
*   **POST** `/key`

    *   **Purpose**: Insert a new key into the database.
    *   **Body** (JSON):

        

        ```json
        {   "name": "MyNodeKey",   "role": "Admin"  }
        ```

    *   **Authorization**: Admin only.
    *   **Responses**:
        *   **200 OK** + newly generated key value if successful.
        *   **400 Bad Request** if an error occurs (e.g., duplicate name).

* * *

### `/queue`

*   **GET** `/queue`
    *   **Purpose**: Retrieve queue lengths for both model-based and node-based queues.
    *   **Authorization**: Admin only.
    *   **Responses**:
        *   **200 OK** + JSON with queue sizes.
        *   **404 Not Found** if endpoint is invalid.

* * *

### `/worker/connections`

*   **GET** `/worker/connections`
    *   **Purpose**: Retrieve the count of active connections for each worker node.
    *   **Authorization**: Admin only.
    *   **Responses**:
        *   **200 OK** + JSON structure of node names to connection counts.

* * *

### `/worker/status`

*   **GET** `/worker/status`
    *   **Purpose**: View each node’s verification status (e.g., `Verified`, `Waiting`, etc.).
    *   **Authorization**: Admin only.
    *   **Responses**:
        *   **200 OK** + JSON array of statuses per node.

* * *

### `/worker/pings`

*   **GET** `/worker/pings`
    *   **Purpose**: Check the last ping timestamps for each worker.
    *   **Authorization**: Admin only.
    *   **Responses**:
        *   **200 OK** + JSON structure with node names mapped to an array of timestamps.

* * *

### `/worker/tags`

*   **GET** `/worker/tags`
    *   **Purpose**: Retrieve the tags (models) supported by each worker node.
    *   **Authorization**: Admin only.
    *   **Responses**:
        *   **200 OK** + JSON structure of node names to sets of tags.

* * *

### `/worker/version/hive`

*   **GET** `/worker/version/hive`
    *   **Purpose**: Shows which Hive version each worker node is running.
    *   **Authorization**: Admin only.
    *   **Responses**:
        *   **200 OK** + JSON mapping of node names to node versions.

* * *

### `/worker/version/ollama`

*   **GET** `/worker/version/ollama`
    *   **Purpose**: Shows which Ollama version each worker node is running.
    *   **Authorization**: Admin only.
    *   **Responses**:
        *   **200 OK** + JSON mapping of node names to Ollama versions.

* * *

# 7. Contributing

We welcome contributions! Please open an **issue** to discuss proposed changes before submitting a pull request. Make sure to:

*   Keep code style consistent.
*   Write tests for new or modified functionality.
*   Update documentation if you change or add features.

* * *

# 8. License

This project is distributed under the **MIT License**. See the LICENSE file for more details.

* * *

Thank you for considering **HiveCore** for your Ollama proxy needs! If you have questions, encounter any issues, or want to contribute, feel free to open an issue or submit a pull request on the [GitHub repository](https://github.com/VakeDomen/HiveCore). We look forward to collaborating with you!
