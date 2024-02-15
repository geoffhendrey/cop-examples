package main

import (
	"context"
	"fmt"
	cloudevents "github.com/cloudevents/sdk-go/v2"
	"github.com/gin-gonic/gin"
	"github.com/rs/zerolog/log"
	"net/http"
	"sync/atomic"
)

var (
	eventGateway string
	address      string
	balance      atomic.Int32
)

func receive(event cloudevents.Event) {
	t := &Transaction{}
	if err := event.DataAs(t); err != nil {
		fmt.Printf("Got Data Error: %s\n", err.Error())
	}
	log.Info().Any("transaction", t)
	log.Info().Any("balance before", balance.Load())
	if t.Operation == "credit" {
		balance.Add(int32(t.Amount))
	} else if t.Operation == "debit" {
		balance.Add(int32(-t.Amount))
	}
	log.Debug().Any("balance after", balance.Load())
	audit(fmt.Sprint(balance.Load()))
}

func audit(e string) {
	c, err := cloudevents.NewClientHTTP()
	if err != nil {
		log.Fatal().Err(err).Msg("failed to create client")
	}

	event := cloudevents.NewEvent()
	event.SetSource("s2sqdemo.bank")
	event.SetType("s2sqdemo:audit")
	event.SetData(cloudevents.ApplicationJSON, map[string]string{"balance": e})

	ctx := cloudevents.ContextWithTarget(context.Background(), eventGateway)

	if result := c.Send(ctx, event); cloudevents.IsUndelivered(result) {
		log.Fatal().Any("failed to send", result)
	} else {
		log.Printf("sent: %v", event)
		log.Printf("result: %v", result)
	}
}

func init() {
	eventGateway = getEnv("GATEWAY_URL", "http://localhost:8081/")
	address = getEnv("BIND_ADDRESS", ":8080")
}

func index(c *gin.Context) {
	c.JSON(http.StatusOK, "Welcome to CloudEvents")
}

func healthz(c *gin.Context) {
	c.String(http.StatusOK, "OK")
}

func cloudEventsHandler() gin.HandlerFunc {
	return func(c *gin.Context) {
		p, err := cloudevents.NewHTTP()
		if err != nil {
			log.Fatal().
				Err(err).
				Msg("Failed to create protocol")
		}

		ceh, err := cloudevents.NewHTTPReceiveHandler(c, p, receive)
		if err != nil {
			log.Fatal().
				Err(err).
				Msg("failed to create handler")
		}

		ceh.ServeHTTP(c.Writer, c.Request)
	}
}

func main() {

	log.Info().Msg("starting server...")
	log.Info().Str("eventGateway", eventGateway)
	log.Info().Str("address", address)

	r := gin.Default()
	r.SetTrustedProxies(nil)

	r.GET("/", index)
	r.GET("/healthz", healthz)
	r.POST("/", cloudEventsHandler())

	err := http.ListenAndServe(address, r)
	if err != nil {
		log.Err(err).Msg("Error starting server")
		return
	}
}
