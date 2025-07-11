#!/bin/bash
source $(dirname "$0")/../setup.sh

# Prepare the environment with 4 agents
prepare 4

# Execute the true tracker inside agent 1 on port 2000
execute_true_tracker 1 2000
sleep 0.1

# Execute the first true peer inside agent 2
execute_true_peer 2 1234 $AGENT1_IP:2000 repo1

# Execute the second true peer inside agent 3
execute_true_peer 3 1234 $AGENT1_IP:2000 repo2

# Execute the user peer inside agent 4
execute_user_peer 4 1234 $AGENT1_IP:2000 repo3

sleep 0.5

# List files on user peer
send 4 "list"

# List peers on tracker
send 1 "list_peers"

# List files for each peer on tracker
send 1 "list_files $AGENT2_IP:1234"
send 1 "list_files $AGENT3_IP:1234"
send 1 "list_files $AGENT4_IP:1234"

send 4 "download google.svg"
sleep 0.5
send 4 "download eslami.ogg"
sleep 0.5

send 4 "exit"
send 3 "exit"
send 2 "exit"
send 1 "exit"

# Check all outputs
check_all 4
