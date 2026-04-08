package main

import (
	"context"
	"fmt"
	"log"
	"net"
	"os"

	dapr "github.com/dapr/go-sdk/client"
	pb "github.com/spring-io-2026-workshop/shipping/gen/shippingpb"
	"github.com/spring-io-2026-workshop/shipping/server"
	"google.golang.org/grpc"
)

func main() {
	var daprClient dapr.Client
	var err error

	ctx := context.Background()
	if host, port := os.Getenv("DAPR_HOST"), os.Getenv("DAPR_PORT"); host != "" && port != "" {
		daprClient, err = dapr.NewClientWithAddressContext(ctx, fmt.Sprintf("%s:%s", host, port))
	} else {
		daprClient, err = dapr.NewClient()
	}
	if err != nil {
		log.Fatalf("Failed to create Dapr client: %v", err)
	}
	defer daprClient.Close()

	lis, err := net.Listen("tcp", ":9091")
	if err != nil {
		log.Fatalf("Failed to listen: %v", err)
	}

	grpcServer := grpc.NewServer()
	pb.RegisterShippingServiceServer(grpcServer, server.NewShippingServer(daprClient))

	log.Printf("Shipping gRPC server listening on %s", lis.Addr())
	if err := grpcServer.Serve(lis); err != nil {
		log.Fatalf("Failed to serve: %v", err)
	}
}
