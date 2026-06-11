package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"sync"
	"time"
)

// Token store with expiration
var (
	validToken  string
	tokenExpiry time.Time
	tokenMutex  sync.Mutex // To handle concurrent access
)

// Response struct for the /token endpoint
type TokenResponse struct {
	Token string `json:"token"`
}

// Response struct for the /post endpoint
type Response struct {
	Message string `json:"message"`
}

// Token expiration duration
const tokenValidityDuration = 5 * time.Minute

// Handler function for generating a token
func tokenHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		log.Println("Method not allowed on /token")
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Generate a simple token and set its expiration time
	tokenMutex.Lock()
	validToken = fmt.Sprintf("%d", time.Now().UnixNano())
	tokenExpiry = time.Now().Add(tokenValidityDuration)
	tokenMutex.Unlock()

	// Respond with the generated token
	response := TokenResponse{Token: validToken}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(response)

	log.Println("Token generated successfully")
}

// Middleware function for authentication
func authenticate(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		tokenMutex.Lock()
		defer tokenMutex.Unlock()

		token := r.Header.Get("Authorization")
		if token == "" {
			log.Println("Unauthorized access attempt: No token provided")
			http.Error(w, "Unauthorized", http.StatusUnauthorized)
			return
		}
		if token != validToken || time.Now().After(tokenExpiry) {
			log.Println("Unauthorized access attempt: Invalid or expired token")
			http.Error(w, "Unauthorized", http.StatusUnauthorized)
			return
		}
		log.Println("Authorized access")
		next.ServeHTTP(w, r)
	})
}

// Handler function for the POST method
func postHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		log.Println("Method not allowed on /post")
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var data map[string]interface{}
	err := json.NewDecoder(r.Body).Decode(&data)
	if err != nil {
		log.Println("Bad request: Error decoding JSON", err)
		http.Error(w, "Bad request", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	response := Response{Message: "Success"}
	json.NewEncoder(w).Encode(response)

	log.Println("POST /post called successfully")
}

func main() {
	// Register the token endpoint
	http.HandleFunc("/token", tokenHandler)

	// Register the post endpoint with authentication
	http.Handle("/post", authenticate(http.HandlerFunc(postHandler)))

	log.Println("Server starting on port 4080...")
	log.Fatal(http.ListenAndServe(":4080", nil))
}
