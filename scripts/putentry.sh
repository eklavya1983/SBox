#!/bin/bash
curl -v -X PUT -H "Content-Type: application/json" --data @test-data/entry.json http://localhost:9000/entry