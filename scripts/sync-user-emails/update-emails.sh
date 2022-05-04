#!/usr/bin/env bash
# --------------------------------------------------------------------
# Email Updates - Update user emails in NOMIS 
# with the verified justice.gov.uk or digital.justice.gov.uk email in 
# HMPPS Auth
# --------------------------------------------------------------------
VERSION=0.1.0
SUBJECT=update-emails
USAGE="\n
Email Updates \n
-----------------------------------------------------\n\n
Usage: ./update-emails.sh <dev|preprod|prod> <CLIENT>:<SECRET> <USER_MAKING_REQUEST>\n\n
Example: ./update-emails.sh dev <client>:<secret> JBRIGHTON_ADM | tee output.txt\n\n
"

# --- Terminate immediately after error ---------------------------
set -e

while getopts ":hv" optname
  do
    case "$optname" in
      "v") echo "Version: $VERSION"; exit 0 ;;
      "h") echo -e $USAGE; exit 0 ;;
      "?") echo "Unknown option: -$OPTARG" >&2; exit 1 ;;
    esac
  done

shift $(($OPTIND - 1))

if [ $# -lt 3 ] ; then
  echo -e $USAGE >&2
  exit 1
fi

ENV=$1
CLIENT=$2
USER_MAKING_REQUEST=$3

# --- Locks -------------------------------------------------------
LOCK_FILE=/tmp/$SUBJECT.lock
if [[ -f $LOCK_FILE ]]; then
  echo "Script is already running" >&2
  exit 1
fi

trap "rm -f $LOCK_FILE" EXIT
touch $LOCK_FILE

# --- Body --------------------------------------------------------

# Calculating API endpoint
source ./nomis-user-roles-api.sh
source ./auth-token-functions.sh
HOST=$(calculateNomisUserRolesApiHostname $ENV)
echo "Using host: $HOST for $ENV..."

source ./tmp/email_updates.sh
number_of_updates=${#email_updates[@]}
echo "Updating $number_of_updates email(s)..."

for (( i=0; i<${number_of_updates}; i++ ));
do
  IFS=";" read -r -a arr <<< "${email_updates[$i]}"
  username="${arr[0]}"
  new_email="${arr[1]}"
  echo "Updating email for user: '$username' to '$new_email'"

  if ! ((i % 200)); then
    echo "Authenticating..."
    AUTH_TOKEN_HEADER=$(authenticate $ENV $CLIENT $USER_MAKING_REQUEST)
  fi

  echo -n "$new_email" | http --quiet PUT "$HOST/users/$username/change-email" "$AUTH_TOKEN_HEADER"
done

