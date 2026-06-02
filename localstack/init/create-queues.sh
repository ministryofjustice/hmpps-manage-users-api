#!/bin/bash
set -e

echo "Creating SQS queues..."

awslocal sqs create-queue --queue-name bulkjobqueue
awslocal sqs create-queue --queue-name bulkjobdlq
awslocal sqs create-queue --queue-name bulkjobitemqueue
awslocal sqs create-queue --queue-name bulkjobitemdlq

echo "Queues created."
