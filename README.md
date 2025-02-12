<div align="center">
  
    <img alt="hivecore" height="200px" src="hivecore.webp">
</div>


# HiveCore

HiveCore is a proxy solution designed to unify scattered machines running [Ollama](https://github.com/ollama/ollama) into a single, cohesive API. Whether you have multiple nodes each capable of Ollama inference or want a centralized gateway for distributing client requests, HiveCore helps you coordinate, monitor, and manage these worker nodes with ease.

One of HiveCore's standout features is its flexibility with network visibility. Worker machines don’t need to be publicly accessible or on the same network as the proxy server. As long as the workers can establish an outgoing connection to the proxy, they can seamlessly integrate into the system. This means you can deploy worker nodes in diverse locations—cloud servers, private data centers, the machine in your living room, or even behind firewalls—and HiveCore will bring them together into a unified API.

By simplifying connectivity requirements, HiveCore eliminates the need for complex network configurations or exposing individual worker nodes to the internet, focusing all visibility and management on the central proxy. This design makes HiveCore scalable and easy to integrate into existing infrastructure.

# Table of Contents

1.  [**Overview**](#1-overview)
2.  [**Key Features**](#2-key-features)
3.  [**Getting Started**](#3-getting-started)
4.  [**Architecture & Components**](#4-architecture--components)
5.  [**Usage & Configuration**](#5-usage--configuration)
6.  [**API Endpoints & Integration**](#6-api-endpoints--integration)
7.  [**Contributing**](#7-contributing)
8.  [**License**](#8-license)

* * *

# 1. Overview


**HiveCore** aims to streamline the process of distributing Ollama inference requests across multiple worker nodes. By acting as a proxy, HiveCore allows you to:

*   **Centralize** client requests into one API endpoint, simplifying how your users interact with Ollama across various machines.
*   **Authenticate** and **manage** nodes, optionally ensuring only verified systems can participate.
*   **Distribute** requests to available nodes, balancing load for optimal performance.
*   **Monitor** and **manage** keys, queues, and node statuses through HTTP endpoints.

* * *

# 2. Key Features


*   **Ollama Proxy & Load Distribution**  
    Collect all your Ollama-ready machines under one unified API to handle inference requests.

*   **Node Management & Monitoring**  
    Keep track of connected worker nodes, verifying their status, ping times, and capabilities.

*   **Queue Management**  
    Incoming requests are queued and served as worker nodes become available, ensuring a smooth, organized flow of tasks.

*   **Administrative API**  
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

    *   By default, HiveCore uses SQLite. Ensure your environment is capable of running SQLite. The SQLite database will be created automatically on the fist run of HiveCore.
4. **Build & Run**

    *   Use Maven to clean and compile the project:
    Run the Maven lifecycle to clean and compile the project:
    ```bash
    mvn clean compile
    ```
    
    *   Generate the Fat JAR with Dependencies: Use the maven-assembly-plugin to package everything into a single JAR:
    ```bash
    mvn compile assembly:single -f pom.xml
    ```

    *   Run the generated jar or start the main class that launches **ClientServer**, **ManagementServer**, and **NodeServer**.
    ```bash
    java -jar target/HiveCore-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```
* * *

# 4. Architecture & Components


*   **NodeServer**  
    Listens for worker node connections running Ollama. Each node is required to authenticate. Once authenticated, tasks can be dispatched to it.

*   **ClientServer**  
    Receives client requests for Ollama inference. These requests are placed in a queue and assigned to worker nodes as they become available.

*   **ManagementServer**  
    Provides administrative endpoints for managing keys, viewing queue lengths, and checking node statuses.

*   **NodeConnectionMonitor**  
    Periodically checks all connected nodes for timeouts or verification issues, removing inactive or unauthorized connections.

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

# 6. API Endpoints & Integration

HiveCore opens two main http endpoints. The main inference endpoint listens on the `PROXY_PORT`(default `6666`). Every request received on will be placed in a queue to be processed by one of the workers. Valid requests to this endpoint are all inference [requests supported](https://github.com/ollama/ollama/blob/main/docs/api.md#generate-a-completion) by Ollama, that specify a target model in the body.
## curl
```bash
curl http://example.com:6666/api/generate -d '{
  "model": "mistral-nemo",
  "prompt": "Why is the sky blue?"
}'
```

Since the requests are piped to the Ollama server, the proxy is compatible with all the main llm libraries that implement the Ollama API.

## Ollama-python

Example using [Ollama-python](https://github.com/ollama/ollama-python):

```python
from ollama_python.endpoints import GenerateAPI

api = GenerateAPI(base_url="http://example.com:6666", model="mistral")
result = api.generate(prompt="Hello World", options=dict(num_tokens=10), format="json")
```

## Langchain

Example using [Langchain](https://github.com/langchain-ai/langchain):

```python
from langchain_ollama.llms import OllamaLLM
from langchain_ollama import OllamaEmbeddings

embedding_model = OllamaEmbeddings(base_url="example.com:6666", model='bge-m3')
llm = OllamaLLM(base_url="example.com:6666", model='mistral-nemo')
```

## LlamaIndex

Example using [LlamaIndex](https://github.com/run-llama/llama_index):

```python
from llama_index.llms.ollama import Ollama

llm = Ollama(base_url="http://example.com:6666", model="mistral-nemo", request_timeout=60.0)

response = llm.complete("What is the capital of France?")
print(response)
```

### `/queue`

*   **GET** `/queue`
    *   **Purpose**: Retrieve queue lengths for both model-based and node-based queues.
    *   **Responses**:
        *   **200 OK** + JSON with queue sizes.
        *   **404 Not Found** if endpoint is invalid.
    *   **Body**:
    ```json
    {
        "Model: mistral-nemo": 63,
        "Model: bge-m3": 0,
        "Node: worker-2xA6000": 0,
        "Node: worker-2xA6000-2": 0
    }
    ```

* * *

### `/worker/connections`

*   **GET** `/worker/connections`
    *   **Purpose**: Retrieve the count of active connections for each worker node. The count represents the amount of concurrent requests each worker node is able to process.
    *   **Responses**:
        *   **200 OK** + JSON structure of node names to connection counts.
    *   **Body**:
    ```json
    {
        "Unauthenticated": 3,
        "worker-2xA6000-1": 8,
        "worker-2x-1080ti-1": 4,
        "worker-2x-1080ti-2": 4,
        "worker-H100-1": 12,
        "worker-H100-2": 12,
        "worker-H100-3": 12,
        "worker-3080-1": 4,
        "worker-3080-4": 4,
        "worker-3080-5": 4,
        "worker-3080-6": 4,
        "worker-cluster-16-1080ti": 2,
        "worker-cluster-17-1080ti": 2,
        "worker-cluster-18-1080ti": 2
    }
    ```

* * *

### `/worker/status`

*   **GET** `/worker/status`
    *   **Purpose**: View each node’s verification status (e.g., `Verified`, `Waiting`, etc.).
    *   **Responses**:
        *   **200 OK** + JSON array of statuses per node.
    *   **Body**:
    ```json
    {
        "Unauthenticated": [
            "SettingUp",
            "SettingUp",
            "SettingUp"
        ],
        "worker-2xA6000-1": [
            "Working",
            "Working",
            "Working",
            "Working",
            "Working",
            "Working",
            "Working",
            "Working"
        ],
        "worker-2x1080ti-1": [
            "Polling",
            "Polling",
            "Working",
            "Working"
        ],
        "worker-2x1080ti-2": [
            "Polling",
            "Working",
            "Working",
            "Working"
        ],
        "worker-cluster-16-1080ti": [
            "Working",
            "Working",
            "Working",
            "Working"
        ]
    }
    ```

* * *

### `/worker/pings`

*   **GET** `/worker/pings`
    *   **Purpose**: Check the last ping timestamps for each worker.
    *   **Responses**:
        *   **200 OK** + JSON structure with node names mapped to an array of timestamps.
    *   **Body**:
    ```json
      
    ```

* * *

### `/worker/tags`

*   **GET** `/worker/tags`
    *   **Purpose**: Retrieve the tags (models) supported by each worker node.
    *   **Responses**:
        *   **200 OK** + JSON structure of node names to sets of tags.
    *   **Body**:
    ```json
      
    ```

* * *

### `/worker/version/hive`

*   **GET** `/worker/version/hive`
    *   **Purpose**: Shows which Hive version each worker node is running.
    *   **Responses**:
        *   **200 OK** + JSON mapping of node names to node versions.
    *   **Body**:
    ```json
      
    ```

* * *

### `/worker/version/ollama`

*   **GET** `/worker/version/ollama`
    *   **Purpose**: Shows which Ollama version each worker node is running.
    *   **Responses**:
        *   **200 OK** + JSON mapping of node names to Ollama versions.
    *   **Body**:
    ```json
      
    ```

* * *


### `/key`

*   **GET** `/key`

    *   **Purpose**: Retrieve a list of all stored authentication keys.
    *   **Responses**:
        *   **200 OK** + JSON array of keys.
        *   **404 Not Found** if no keys exist.
        *   **400 Bad Request** if an error occurs.
      ```json
      [
        {
          "id": 1,
          "name": "Admin",
          "value": "004c0a77-4af8-48cc-8690-4e7ccb33cf08",
          "role": "Admin"
        },
        {
          "id": 2,
          "name": "worker-2xA6000",
          "value": "c228f8df-8df3-4649-a964-966bf816b50b",
          "role": "Worker"
        },
        {
          "id": 3,
          "name": "worker-2xA6000-2",
          "value": "e5a9343c-f643-467f-b1e5-2f0b2a83c367",
          "role": "Worker"
        },
        {
          "id": 4,
          "name": "worker-cluster-16-1080ti",
          "value": "9bd93308-448a-4588-b86d-e92476057957",
          "role": "Worker"
        },
        {
          "id": 5,
          "name": "worker-cluster-17-1080ti",
          "value": "4628b1b9-7d19-4713-8456-ca9a5cf9a6f4",
          "role": "Worker"
        } 
      ]
      ```
*   **POST** `/key`

    *   **Purpose**: Insert a new key into the database.
    *   **Body** (JSON):

        ```json
        {   "name": "MyNodeKey",   "role": "Admin"  }
        ```

    *   **Responses**:
        *   **200 OK** + newly generated key value if successful.
        *   **400 Bad Request** if an error occurs (e.g., duplicate name).

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
