package server

import (
	"context"
	"fmt"
	"log"
	"sync"
	"time"

	dapr "github.com/dapr/go-sdk/client"
	pb "github.com/spring-io-2026-workshop/shipping/gen/shippingpb"
	"google.golang.org/protobuf/types/known/timestamppb"
)

const (
	pubsubName = "pubsub"
	topic      = "shipments"
)

type ShipmentStatusEvent struct {
	ShipmentID string `json:"shipmentId"`
	Status     string `json:"status"`
	StatusDate string `json:"statusDate"`
}

type Shipment struct {
	ShipmentID            string
	OrderID               string
	ShippingAddress       *pb.Address
	ShipmentDate          time.Time
	EstimatedDeliveryDate time.Time
	Status                pb.ShipmentStatus
}

type ShippingServer struct {
	pb.UnimplementedShippingServiceServer

	mu        sync.Mutex
	shipments map[string]*Shipment
	counter   int
	dapr      dapr.Client
}

func NewShippingServer(daprClient dapr.Client) *ShippingServer {
	return &ShippingServer{
		shipments: make(map[string]*Shipment),
		dapr:      daprClient,
	}
}

func (s *ShippingServer) ShipOrder(ctx context.Context, req *pb.ShipOrderRequest) (*pb.ShipOrderResponse, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.counter++
	shipmentID := fmt.Sprintf("SHIP-%06d", s.counter)

	now := time.Now().UTC()
	estimatedDelivery := now.AddDate(0, 0, 5)

	shipment := &Shipment{
		ShipmentID:            shipmentID,
		OrderID:               req.GetOrderId(),
		ShippingAddress:       req.GetShippingAddress(),
		ShipmentDate:          now,
		EstimatedDeliveryDate: estimatedDelivery,
		Status:                pb.ShipmentStatus_PENDING,
	}
	s.shipments[shipmentID] = shipment

	log.Printf("Shipment %s created for order %s", shipmentID, req.GetOrderId())

	// Publish "pending" event immediately
	s.publishStatusEvent(ctx, shipmentID, "pending")

	// Schedule async "shipped" after 3s and "delivered" after 10s
	go func() {
		time.Sleep(3 * time.Second)
		s.updateStatus(shipmentID, pb.ShipmentStatus_SHIPPED)
		s.publishStatusEvent(context.Background(), shipmentID, "shipped")

		time.Sleep(10 * time.Second)
		s.updateStatus(shipmentID, pb.ShipmentStatus_DELIVERED)
		s.publishStatusEvent(context.Background(), shipmentID, "delivered")
	}()

	return &pb.ShipOrderResponse{
		ShipmentId:            shipmentID,
		ShipmentDate:          timestamppb.New(now),
		EstimatedDeliveryDate: timestamppb.New(estimatedDelivery),
		Status:                pb.ShipmentStatus_PENDING,
	}, nil
}

func (s *ShippingServer) updateStatus(shipmentID string, status pb.ShipmentStatus) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if shipment, ok := s.shipments[shipmentID]; ok {
		shipment.Status = status
	}
}

func (s *ShippingServer) publishStatusEvent(ctx context.Context, shipmentID, status string) {
	event := &ShipmentStatusEvent{
		ShipmentID: shipmentID,
		Status:     status,
		StatusDate: time.Now().UTC().Format(time.RFC3339),
	}
	if err := s.dapr.PublishEvent(ctx, pubsubName, topic, event); err != nil {
		log.Printf("Failed to publish %s event for shipment %s: %v", status, shipmentID, err)
		return
	}
	log.Printf("Published %s event for shipment %s", status, shipmentID)
}
