#!/usr/bin/env bash

calculateNomisUserRolesApiHostname() {
  local ENV=$1
  # Set the environment-specific hostname for the oauth2 service
  if [[ "$ENV" == "dev" ]]; then
    echo "https://nomis-user-roles-api-dev.prison.service.justice.gov.uk"
  elif [[ "$ENV" == "preprod" ]]; then
    echo "https://nomis-user-roles-api-preprod.prison.service.justice.gov.uk"
  elif [[ "$ENV" == "prod" ]]; then
    echo "https://nomis-user-roles-api.prison.service.justice.gov.uk"
  elif [[ "$ENV" =~ localhost* ]]; then
    echo "http://$ENV"
  fi
}

