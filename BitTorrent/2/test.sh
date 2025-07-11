#!/bin/bash
source $(dirname "$0")/../setup.sh

# Prepare the environment with 4 agents
prepare 4

# Execute the true tracker inside agent 1 on port 2000
execute_true_tracker 1 2000
sleep 0.1

# Execute the first true peer inside agent 2
execute_true_peer 2 1234 $AGENT1_IP:2000 repo2

# Execute the user peer inside agent 3
execute_user_peer 3 1234 $AGENT1_IP:2000 repo3

# Execute the user peer inside agent 4
execute_user_peer 4 1234 $AGENT1_IP:2000 repo4

sleep 0.5

# Send command "list" to each peer
send 2 "list"
send 3 "list"
send 4 "list"

# List peers on tracker
send 1 "list_peers"

# List files for each peer on tracker
send 1 "list_files $AGENT2_IP:1234"
send 1 "get_sends $AGENT2_IP:1234"
sleep 0.3
send 1 "list_files $AGENT3_IP:1234"
send 1 "list_files $AGENT4_IP:1234"

send 4 "download persist.exe"   # User peer downloads persist.exe (it's in repo3 - agent 3)
send 2 "download persist.exe"   # agent2 downloads persist.exe from agent 3
send 3 "download persist.exe"   # agent3 already has persist.exe, so it should not download it again
sleep 0.5
send 4 "download google.svg"    # User peer downloads google.svg (it's in repo2 - agent 2)
sleep 0.5

send 1 "get_sends $AGENT3_IP:1234"
sleep 0.5
send 1 "get_sends $AGENT4_IP:1234"
sleep 0.5

send 1 "get_receives $AGENT3_IP:1234"
sleep 0.5
send 1 "get_receives $AGENT4_IP:1234"
sleep 0.5

# Send exit commands to all agents
send 4 "exit"
send 3 "exit"
send 2 "exit"
send 1 "exit"

# Check all outputs
check_all 4
