Configure Jaeger adddress in `docker-compose.yml`

Start reshapr:
```sh
$ docker compose up -d
[+] up 18/18
 ✔ Image registry.reshapr.io/reshapr/reshapr-ctrl:nightly  Pulled                                    19.6s
 ✔ Image registry.reshapr.io/reshapr/reshapr-proxy:nightly Pulled                                    10.2s
 ✔ Network shipping-mcp_default                            Created                                   0.0s
 ✔ Container reshapr-postgres                              Created                                   0.2s
 ✔ Container reshapr-control-plane                         Healthy                                   5.8s
 ✔ Container reshapr-gateway-01                            Created                                   0.0s
```

Connect to reshapr control-plane:

```sh
$ reshapr login -s http://localhost:5555 -u admin -p password
ℹ️  Logging in to Reshapr at http://localhost:5555...
✅ Login successful!
ℹ️  Welcome, admin!
✅ Configuration saved to /Users/laurent/.reshapr/config
```

Import the proto file:
```sh
reshapr import -f ./shipping-service.proto --be http://host.docker.internal:9091
```