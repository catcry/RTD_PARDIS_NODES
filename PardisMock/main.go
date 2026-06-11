package main

import (
	"encoding/json"
	"fmt"
	"log"
	"math/rand"
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
	Token  string `json:"access_token"`
	Expire string `json:"expires_in"`
}

// Request struct for the /api/v2/sendBulk endpoint
type SMSRequest struct {
	DestinationList []struct {
		MobileNo string `json:"mobileNo"`
	} `json:"destinationList"`
	Message  string `json:"message"`
	SMSClass string `json:"smsClass"`
	Source   string `json:"source"`
}

// Response struct for the /api/v2/sendBulk endpoint
type SMSResponse struct {
	SMSIDList []string `json:"smsIdList"`
}

// Token expiration duration
const tokenValidityDuration = 5 * time.Minute

// Dummy user data for authentication
var validUsername = "catcry"
var validPassword = "catcry"

// Handler function for generating a token
func tokenHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		log.Println("Method not allowed on /token")
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Parse the form data
	err := r.ParseForm()
	if err != nil {
		log.Println("Error parsing form data", err)
		http.Error(w, "Bad request", http.StatusBadRequest)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	// Validate username and password
	if username != validUsername || password != validPassword {
		log.Println("Unauthorized access attempt: Invalid username or password")
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	// Generate a simple token and set its expiration time
	tokenMutex.Lock()
	validToken = fmt.Sprintf("%d", time.Now().UnixNano())
	tokenExpiry = time.Now().Add(tokenValidityDuration)
	tokenMutex.Unlock()

	// Respond with the generated token
	response := TokenResponse{Token: validToken, Expire: "300"}
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
		log.Println("Method not allowed on /api/v2/sendBulk")
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var smsRequest SMSRequest
	err := json.NewDecoder(r.Body).Decode(&smsRequest)
	if err != nil {
		log.Println("Bad request: Error decoding JSON", err)
		http.Error(w, "Bad request", http.StatusBadRequest)
		return
	}

	// Process the request and generate the response
	smsResponse := SMSResponse{SMSIDList: make([]string, len(smsRequest.DestinationList))}
	for i, destination := range smsRequest.DestinationList {
		smsResponse.SMSIDList[i] = sendSMS(destination.MobileNo, smsRequest.Message)
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(smsResponse)

	log.Println("POST /api/v2/sendBulk called successfully")
}

// Function to simulate sending an SMS and generating the required response
func sendSMS(mobileNo, message string) string {
	// With 1 in 50 probability, return a number between 1 and 255
	if rand.Intn(10) == 0 {
		return fmt.Sprintf("%d", rand.Intn(255)+1)
	}
	// Otherwise, return a 12-character alphanumeric string starting with "a"
	return generateUUID()
}

// Function to generate a 12-character alphanumeric string starting with "a"
func generateUUID() string {
	const chars = "abcdefghijklmnopqrstuvwxyz0123456789"
	result := make([]byte, 12)
	result[0] = 'a'
	for i := 1; i < 12; i++ {
		result[i] = chars[rand.Intn(len(chars))]
	}
	return string(result)
}

func main() {
	rand.Seed(time.Now().UnixNano())

	// Register the token endpoint
	http.HandleFunc("/token", tokenHandler)

	// Register the post endpoint with authentication
	http.Handle("/api/v2/sendBulk", authenticate(http.HandlerFunc(postHandler)))

	log.Println("Server starting on port 4080...")
	log.Fatal(http.ListenAndServe(":4080", nil))
}
