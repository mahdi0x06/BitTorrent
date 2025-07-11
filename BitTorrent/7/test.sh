#!/bin/bash
source $(dirname "$0")/../setup.sh

# Prepare the environment with 5 agents
prepare 5

# Execute the user tracker inside agent 1 on port 10000
execute_user_tracker 1 10000

sleep 0.1

# Execute first user peer inside agent 2 on port 10000
execute_user_peer 2 10000 $AGENT1_IP:10000 repo2

# Execute second user peer inside agent 3 on port 20000
execute_user_peer 3 20000 $AGENT1_IP:10000 repo3

# Execute third user peer inside agent 4 on port 30000
execute_user_peer 4 30000 $AGENT1_IP:10000 repo4

# Execute fourth user peer inside agent 5 on port 39877
execute_user_peer 5 39877 $AGENT1_IP:10000 repo5

sleep 0.5

# List files on all peers
send 2 "list"
send 3 "list"
send 4 "list"
send 5 "list"

# List peers on tracker
send 1 "list_peers"
# List files for each peer on tracker
send 1 "list_files $AGENT2_IP:10000"
send 1 "list_files $AGENT3_IP:20000"
send 1 "list_files $AGENT4_IP:30000"
send 1 "list_files $AGENT5_IP:39877"
sleep 0.1

# Start parallel downloads from multiple peers

send 2 "download cyan.png"
send 3 "download cyan.png"
send 4 "download cyan.png"
sleep 0.5

send 2 "download eslami.ogg"
send 3 "download test.c"
send 4 "download red.png"
send 5 "download eslami.ogg"
sleep 0.5

# List files again to verify downloads
send 2 "list"
send 3 "list"
send 4 "list"
send 5 "list"

# Check tracker's peer list and file information
send 1 "get_sends $AGENT5_IP:39877"
sleep 0.5

# Simulate file corruption in agent 4
docker exec agent4 bash -c 'echo CORRUPTION > /repos/repo4/test.c' # Simulate file corruption

send 5 "download test.c" # Should error due to file corruption
sleep 0.5

send 1 "refresh_files" # Refresh files on tracker to update the hash of files
sleep 0.8

# Now, test.c are present in agent 4 and 3 but with different hashes
send 5 "download test.c" # Multiple hash found error
sleep 0.2

docker exec agent4 bash -c 'rm /repos/repo4/test.c' # Remove corrupted file from agent 4

send 1 "refresh_files" # Refresh files on tracker to update the hash of files
sleep 0.8

# Now, test.c should be downloadable from agent 3
send 5 "download test.c" # Should download successfully from agent 3
sleep 0.5

# List files again to verify downloads
send 5 "list"

# Send exit commands to all agents
send 5 "exit"
send 4 "exit"
send 3 "exit"
send 2 "exit"
send 1 "exit"

# Check all outputs
check_all 5
