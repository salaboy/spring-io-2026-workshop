# Shipping Service

A Go microservice exposing a gRPC API for shipping orders, based on the `shipping-service.proto` contract.

## Project Structure

```
shipping/
├── main.go                # Entry point – starts the gRPC server on port 9091
├── server/
│   └── server.go          # ShippingService implementation (in-memory storage)
├── gen/shippingpb/         # Generated Go code from the proto definition
├── proto/                  # Proto source files
│   ├── shipping-service.proto
│   └── google/type/datetime.proto
├── go.mod
└── go.sum
```

## How It Works

The `ShipOrder` RPC receives a shipping request (order ID + address), assigns a unique shipment ID, stores the shipment in memory, and returns the shipment details with a `SHIPPED` status.

## Prerequisites

- Go 1.21+
- `protoc` compiler and Go plugins (`protoc-gen-go`, `protoc-gen-go-grpc`) – only needed to regenerate the proto code

## Running

```bash
go run .
```

The server listens on **port 9090**.

## Regenerating Proto Code

```bash
protoc --proto_path=proto \
  --go_out=gen/shippingpb --go_opt=module=github.com/spring-io-2026-workshop/shipping/gen/shippingpb \
  --go-grpc_out=gen/shippingpb --go-grpc_opt=module=github.com/spring-io-2026-workshop/shipping/gen/shippingpb \
  proto/shipping-service.proto
```