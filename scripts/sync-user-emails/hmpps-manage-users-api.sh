#!/usr/bin/env bash

calculateHmppsManageUsersApiHostname() {
  local ENV=$1
  # Set the environment-specific hostname for the oauth2 service
  if [[ "$ENV" == "dev" ]]; then
    echo "https://manage-users-api-dev.hmpps.service.justice.gov.uk"
  elif [[ "$ENV" == "preprod" ]]; then
    echo "https://manage-users-api-preprod.hmpps.service.justice.gov.uk"
  elif [[ "$ENV" == "prod" ]]; then
    echo "https://manage-users-api.hmpps.service.justice.gov.uk"
  elif [[ "$ENV" =~ localhost* ]]; then
    echo "http://$ENV"
  fi
}

