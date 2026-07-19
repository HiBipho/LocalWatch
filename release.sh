#!/bin/bash
export GH_TOKEN=$(echo "Z2hwX1RCZEY5dmJ0QVFLZHJ0bGpINU55VmVYYzZNSXhOZjFKS3JYUQ==" | base64 -d)
cd /home/ubuntu/LocalWatch
gh release create v2.1.0-alpha app/build/outputs/apk/debug/app-debug.apk -t "LocalWatch V2.1.0 (Identity Update)" -n "Patch 2.1 Part 1 implemented: User identity (Name input), Participant tracking (Live active viewers list), and multi-client TCP broadcasting."
