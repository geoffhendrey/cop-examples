package main

import (
	"context"
	"fmt"
	cloudevents "github.com/cloudevents/sdk-go/v2"
	cehttp "github.com/cloudevents/sdk-go/v2/protocol/http"
	"log"
	"math/rand"
	"net/http"
	"time"
)

var (
	eventGateway string
	address      string
)

func print(w http.ResponseWriter, r *http.Request) {
	event, err := cloudevents.NewEventFromHTTPRequest(r)
	if err != nil {
		log.Printf("failed to parse CloudEvent from request: %v", err)
		http.Error(w, http.StatusText(http.StatusBadRequest), http.StatusBadRequest)
	}
	w.Write([]byte(event.String()))
	log.Println(event.String())
}

func send(t *Transaction) {
	c, err := cloudevents.NewClientHTTP()
	if err != nil {
		log.Fatalf("failed to create client, %v", err)
	}

	event := cloudevents.NewEvent()
	event.SetSource("s2sqdemo.client")
	event.SetType("s2sqdemo.txn")
	event.SetData(cloudevents.ApplicationJSON, t)

	ctx := cloudevents.ContextWithTarget(context.Background(), eventGateway)

	if result := c.Send(ctx, event); cloudevents.IsUndelivered(result) {
		log.Fatalf("failed to send, %v", result)
	} else {
		var httpResult *cehttp.Result
		if cloudevents.ResultAs(result, &httpResult) {
			var _ error
			if httpResult.StatusCode != http.StatusOK {
				err = fmt.Errorf(httpResult.Format, httpResult.Args...)
			}
			log.Printf("Sent %s with status code %d, error: %v", event, httpResult.StatusCode, err)
		} else {
			log.Printf("Send did not return an HTTP response: %s", result)
		}
	}
}

func sim() *time.Ticker {
	log.Println("starting sim...")
	rand.NewSource(time.Now().UnixNano())
	ticker := time.NewTicker(15 * time.Second)
	go func() {
		for range ticker.C {
			a := rand.Intn(500)
			t := &Transaction{
				Amount: a,
			}
			if rand.Intn(2) == 1 {
				t.Operation = "credit"
			} else {
				t.Operation = "debit"
			}
			fmt.Printf("%+v\n", t)
			send(t)
		}
	}()
	return ticker
}

func init() {
	eventGateway = getEnv("GATEWAY_URL", "http://localhost:8085/")
	address = getEnv("BIND_ADDRESS", ":8080")
}

func main() {
	log.Println("starting server...")
	log.Printf("gateway location %s\n", eventGateway)
	t := sim()
	http.HandleFunc("/", print)
	http.ListenAndServe(address, nil)
	t.Stop()
}
