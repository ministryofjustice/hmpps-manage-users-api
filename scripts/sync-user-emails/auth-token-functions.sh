#!/usr/bin/env bash

calculateAuthHostname() {
  local ENV=$1
  # Set the environment-specific hostname for the oauth2 service
  if [[ "$ENV" == "dev" ]]; then
    echo "https://sign-in-dev.hmpps.service.justice.gov.uk"
  elif [[ "$ENV" == "preprod" ]]; then
    echo "https://sign-in-preprod.hmpps.service.justice.gov.uk"
  elif [[ "$ENV" == "prod" ]]; then
    echo "https://sign-in.hmpps.service.justice.gov.uk"
  elif [[ "$ENV" =~ localhost* ]]; then
    echo "http://localhost:8080"
  fi
}

checkFile() {
  local FILE=$1
  # Check whether the file exists and is readable
  if [[ ! -f "$FILE" ]]; then
    echo "Unable to find file $FILE"
    exit 1
  fi
}

authenticate() {
  local ENV=$1
  local CLIENT=$2
  local USER=$3
  local AUTH_HOST=$(calculateAuthHostname $ENV)

  # Get token for the client name / secret and store it in the environment variable TOKEN
  if echo | base64 -w0 >/dev/null 2>&1; then
    AUTH=$(echo -n "$CLIENT" | base64 -w0)
  else
    AUTH=$(echo -n "$CLIENT" | base64)
  fi

  if ! TOKEN_RESPONSE=$(curl -sS -d "" -X POST "$AUTH_HOST/auth/oauth/token?grant_type=client_credentials&username=$USER" -H "Authorization: Basic $AUTH"); then
    echo "Failed to read token from credentials response"
    echo "$TOKEN_RESPONSE"
    exit 1
  fi
  TOKEN=$(echo "$TOKEN_RESPONSE" | jq -er .access_token)

  echo "Authorization: Bearer $TOKEN"
}
