# http server module project

## Table of Contents

1. [System Requirements](#system-requirements)
2. [Package Manager](#package-manager)
3. [Dependencies](#dependencies)
4. [Running](#running)

## System Requirements

- Spring Webflux = 3.1.5
- JDK = 21

## Package Manager

- Maven = 4.0.0

## Dependencies

```bash
<!-- https://mvnrepository.com/artifact/redis.clients/jedis -->
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

## Running

### Prerequisite Command

```bash
mvn clean package
```

### Api

```bash
mvn spring-boot:run
```
