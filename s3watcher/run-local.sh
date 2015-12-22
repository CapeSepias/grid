#!/bin/bash

set -e

if [ $# -ne 1 ]
then
	echo 'Usage: run-local.sh <image>'
	exit 1
fi

# Note: annoyingly package.json run-scripts don't support args, hence this script
node scripts/upload.js "$1" event.json

cd lambda
npm run local
