#!/bin/bash
set -e
mkdir -p out
echo "Compiling Chess Engine..."
find src -name "*.java" | sort > sources.txt
javac --release 17 -d out @sources.txt
echo ""
echo "Build successful!"
echo "  Local game:     java -cp out chess.ui.Main"
echo "  Perft test:     java -cp out chess.util.PerftTest"
echo "  Network server: java -cp out chess.network.ChessServer"
echo "  Network client: java -cp out chess.network.ChessClient"
