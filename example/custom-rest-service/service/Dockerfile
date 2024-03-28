FROM golang:1.21 as build

WORKDIR /go/src/restdemo
COPY . .

RUN go mod download
RUN go vet -v
RUN go test -v

RUN CGO_ENABLED=0 go build -o /go/bin/restdemo

FROM gcr.io/distroless/static-debian11

COPY --from=build /go/bin/restdemo /
CMD ["/restdemo"]
