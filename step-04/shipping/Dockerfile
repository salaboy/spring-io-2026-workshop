# Build stage
FROM golang:1.26-alpine AS builder

WORKDIR /app

# Copy go mod files
COPY go.mod go.sum ./

# Download dependencies
RUN go mod download

# Copy source code
COPY . .

# Build the shipping service
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/bin/shipping .

# Runtime stage
FROM alpine:latest

WORKDIR /app

# Copy the binary from builder
COPY --from=builder /app/bin/shipping .

# Expose port
EXPOSE 8088

# Run the service
CMD ["./shipping"]