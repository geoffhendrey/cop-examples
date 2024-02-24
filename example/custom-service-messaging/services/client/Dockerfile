FROM golang:1.21 as build

WORKDIR /go/src/client
COPY . .

RUN go mod download
RUN go vet -v
RUN go test -v

RUN CGO_ENABLED=0 go build -o /go/bin/client

FROM gcr.io/distroless/static-debian11

COPY --from=build /go/bin/client /
CMD ["/client"]
