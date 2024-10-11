# HTTP Server Module

The `http-server-module` is a core service in the Short Message Service Center (SMSC) environment, designed to handle HTTP communication for various message delivery and service provider interactions. This module integrates with Redis for managing message queues and supports real-time communication through WebSocket. It also provides extensive monitoring capabilities via JMX and utilizes thread pooling for performance optimization.

## Features

- **HTTP-based Communication**: Manages incoming HTTP requests, typically interacting with service providers or external systems.
- **Redis Integration**: Utilizes Redis for queue management, handling delivery receipt (DLR) messages and service provider configurations.
- **WebSocket Real-Time Communication**: Supports WebSocket connections for real-time communication.
- **SMPP Integration**: Works alongside SMPP servers by configuring necessary settings through Redis.
- **Thread Pooling**: Enhances performance and scalability by managing thread usage for handling multiple requests.
- **JMX Monitoring**: Provides monitoring and management capabilities through JMX.

## Key Configurable Variables

### JVM Settings
- **`JVM_XMS`**: Sets the minimum heap size for the JVM (default: `512m`).
- **`JVM_XMX`**: Sets the maximum heap size for the JVM (default: `1024m`).

### Server Settings
- **`APPLICATION_NAME`**: Name of the server instance (default: `http-server-instance-01`).
- **`SERVER_IP`**: IP address of the server (default: `127.0.0.1`).
- **`SERVER_PORT`**: Port on which the HTTP server listens (default: `9500`).
- **`INITIAL_STATUS`**: Initial operational status of the server (`STARTED` by default).

### Redis Cluster Configuration
- **`CLUSTER_NODES`**: List of Redis cluster nodes for queue management (e.g., `localhost:7000, localhost:7001,...,localhost:7009`).

### Message Delivery Queue
- **`DELIVER_SM_QUEUE`**: Redis queue where delivery reports (DLR) are stored (default: `http_dlr`).

### Thread Pool Settings
- **`THREAD_POOL_MAX_TOTAL`**: Maximum number of threads (default: `60`).
- **`THREAD_POOL_MAX_IDLE`**: Maximum number of idle threads (default: `50`).
- **`THREAD_POOL_MIN_IDLE`**: Minimum number of idle threads (default: `10`).
- **`THREAD_POOL_BLOCK_WHEN_EXHAUSTED`**: Whether to block when no threads are available (`true` by default).

### WebSocket Configuration
- **`WEBSOCKET_SERVER_ENABLED`**: Enables WebSocket server (default: `true`).
- **`WEBSOCKET_SERVER_HOST`**: Host IP for the WebSocket server (default: `127.0.0.1`).
- **`WEBSOCKET_SERVER_PORT`**: Port for the WebSocket server (default: `9087`).
- **`WEBSOCKET_SERVER_PATH`**: Path for WebSocket connections (default: `/ws`).
- **`WEBSOCKET_SERVER_RETRY_INTERVAL`**: Retry interval for WebSocket reconnections (in seconds, default: `10`).
- **`WEBSOCKET_HEADER_NAME`**: WebSocket header used for authorization (default: `Authorization`).
- **`WEBSOCKET_HEADER_VALUE`**: Authorization token for WebSocket connections.

### Consumer Settings
- **`CONSUMER_WORKERS`**: Number of workers for processing Redis messages (default: `11`).
- **`CONSUMER_BATCH_SIZE`**: Number of messages to process in each batch (default: `10000`).
- **`CONSUMER_SCHEDULER`**: Frequency in milliseconds for scheduling message consumption (default: `1000`).

### Redis Hash Settings
- **`SERVICE_PROVIDERS_HASH_NAME`**: Redis hash used to manage service providers (default: `service_providers`).
- **`SMPP_SERVER_CONFIGURATIONS_HASH_NAME`**: Redis hash for SMPP server configurations (default: `configurations`).
- **`HTTP_SERVER_GENERAL_SETTINGS_HASH`**: Redis hash for storing general HTTP server settings (default: `general_settings`).
- **`HTTP_SERVER_GENERAL_SETTINGS_KEY`**: Key within Redis for accessing HTTP server settings (default: `smpp_http`).

### JMX Monitoring
- **`ENABLE_JMX`**: Enables JMX for monitoring and management (default: `true`).
- **`IP_JMX`**: IP address for JMX communication (default: `127.0.0.1`).
- **`JMX_PORT`**: Port for the JMX service (default: `9014`).

## Docker Compose Example

```yaml
services:
  http-server-module:
    image: paic/http-server-module:latest
    ulimits:
      nofile:
        soft: 1000000
        hard: 1000000
    environment:
      JVM_XMS: "-Xms512m"
      JVM_XMX: "-Xmx1024m"
      APPLICATION_NAME: "http-server-instance-01"
      SERVER_IP: "127.0.0.1"
      SERVER_PORT: 9500
      INITIAL_STATUS: "STARTED"
      PROTOCOL: "HTTP"
      SCHEMA: "http"
      RATING_REQUEST_API_KEY: "fe34b3ce-877e-4c61-a846-033320a9951f"
      CLUSTER_NODES: "localhost:7000,localhost:7001,localhost:7002,localhost:7003,localhost:7004,localhost:7005,localhost:7006,localhost:7007,localhost:7008,localhost:7009"
      THREAD_POOL_MAX_TOTAL: 60
      THREAD_POOL_MAX_IDLE: 50
      THREAD_POOL_MIN_IDLE: 10
      THREAD_POOL_BLOCK_WHEN_EXHAUSTED: true
      DELIVER_SM_QUEUE: "http_dlr"
      CONSUMER_WORKERS: 11
      CONSUMER_BATCH_SIZE: 10000
      CONSUMER_SCHEDULER: 1000
      WEBSOCKET_SERVER_ENABLED: true
      WEBSOCKET_SERVER_HOST: "127.0.0.1"
      WEBSOCKET_SERVER_PORT: 9087
      WEBSOCKET_SERVER_PATH: "/ws"
      WEBSOCKET_SERVER_RETRY_INTERVAL: 10
      WEBSOCKET_HEADER_NAME: "Authorization"
      WEBSOCKET_HEADER_VALUE: "{WEBSOCKET_HEADER_VALUE}"
      SERVICE_PROVIDERS_HASH_NAME: "service_providers"
      SMPP_SERVER_CONFIGURATIONS_HASH_NAME: "configurations"
      HTTP_SERVER_GENERAL_SETTINGS_HASH: "general_settings"
      HTTP_SERVER_GENERAL_SETTINGS_KEY: "smpp_http"
      ENABLE_JMX: "true"
      IP_JMX: "127.0.0.1"
      JMX_PORT: "9014"
    volumes:
      - /opt/paic/smsc-docker/http/http-server-module-docker/resources/conf/logback.xml:/opt/paic/HTTP_SERVER_MODULE/conf/logback.xml
    network_mode: host
