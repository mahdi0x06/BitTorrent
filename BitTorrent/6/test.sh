#!/bin/bash
source $(dirname "$0")/../setup.sh

# Prepare the environment with 4 agents
prepare 4

# Execute the user tracker inside agent 1 on port 2000
execute_user_tracker 1 2000
sleep 0.1

# Execute first user peer inside agent 2
execute_user_peer 2 1234 $AGENT1_IP:2000 repo1

# Execute second user peer inside agent 3
execute_user_peer 3 1234 $AGENT1_IP:2000 repo2

# Execute third user peer inside agent 4
execute_user_peer 4 1234 $AGENT1_IP:2000 repo3

sleep 0.5

# List files on all peers
send 2 "list"
send 3 "list"
send 4 "list"

# List peers on tracker
send 1 "list_peers"

# List files for each peer on tracker
send 1 "list_files $AGENT2_IP:1234"
send 1 "list_files $AGENT3_IP:1234"
send 1 "list_files $AGENT4_IP:1234"
sleep 0.1

# Start parallel downloads from multiple peers
send 3 "download wood.jpg"
send 4 "download wood.jpg"
sleep 0.5

# Download another file while first downloads are in progress
send 3 "download persist.exe"
send 4 "download persist.exe"
sleep 0.5

# List files again to verify downloads
send 3 "list"
send 4 "list"
sleep 0.1

# Check tracker's peer list and file information
send 1 "list_peers"
send 1 "list_files $AGENT3_IP:1234"
send 1 "list_files $AGENT4_IP:1234"

# Send exit commands to all agents
send 4 "exit"
send 3 "exit"
send 2 "exit"
send 1 "exit"

# Check all outputs
check_all 4
