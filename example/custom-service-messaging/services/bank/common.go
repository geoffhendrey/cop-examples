package main

import "os"

type Transaction struct {
	Amount    int    `json:"amount"`
	Operation string `json:"op"`
}

func getEnv(key string, defaultValue string) string {
	var v string
	var ok bool
	if v, ok = os.LookupEnv(key); !ok {
		v = defaultValue
	}
	return v
}
