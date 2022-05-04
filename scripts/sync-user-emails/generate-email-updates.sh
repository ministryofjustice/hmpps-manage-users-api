#!/usr/bin/env bash
# --------------------------------------------------------------------
# Generate Email Updates - Generate list of user emails in NOMIS 
# with the verified justice.gov.uk or digital.justice.gov.uk email in 
# HMPPS Auth
# --------------------------------------------------------------------
VERSION=0.1.0
SUBJECT=generate-email-updates
USAGE="\n
Generate Email Updates \n
-----------------------------------------------------\n\n
Usage: ./generate-email-updates.sh <dev|preprod|prod> <CLIENT>:<SECRET> <USER_MAKING_REQUEST>\n\n
Example: ./generate-email-updates.sh dev <client>:<secret> JBRIGHTON_ADM | tee output.txt\n\n
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

source ./auth-token-functions.sh
source ./hmpps-manage-users-api.sh

echo "Authenticating..."
AUTH_TOKEN_HEADER=$(authenticate $ENV $CLIENT $USER_MAKING_REQUEST)

echo "Calculating host..."
HOST=$(calculateHmppsManageUsersApiHostname $ENV)

echo "Generating list of affected users..."
mkdir -p tmp
echo "declare -a affected_users=(" > tmp/affected_users.sh
http --timeout=600 GET "$HOST/sync/users?caseSensitive=false&onlyVerified=true&domainFilters=digital.justice.gov.uk&domainFilters=justice.gov.uk" "$AUTH_TOKEN_HEADER" \
| jq '.results[] | .id + ";" + .differences' >> tmp/affected_users.sh
echo ")" >> tmp/affected_users.sh
chmod +x tmp/affected_users.sh
echo "Generated list of affected users!"

echo "Generating list of email updates..."
. tmp/affected_users.sh
echo "declare -a email_updates=(" > tmp/email_updates.sh
for user in "${affected_users[@]}"
do
    IFS=";" read -r -a arr <<< "${user}"
    username="${arr[0]}"
    difference="${arr[1]}"
    new_email=$(echo $difference | sed -e 's/.*email.*[\(,=, ]\(.*justice\.gov\.uk\)[\)]*}/\1/')

    echo "\"$username;$new_email\"" >> tmp/email_updates.sh
done
echo ")" >> tmp/email_updates.sh
chmod +x tmp/email_updates.sh
