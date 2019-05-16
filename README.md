# akka-http-bidder
Scala Akka simple bidding agent

Using Scala and Akka framework create a real time bidding agent that responds to bid requests with bid responses.
Agent is simply a HTTP server that accepts json requests, does some matching (bidding) logic and responds with a json response.

Campaign protocol:
It's contains TimeRange, Targeting, Banner case class

BidRequest protocol:
It will process incomming POST bid request JSON data for internally checking with campaign protocol, after do some checking 
BidResponse class will return data for JSON request.
If incoming JSON parameter not match with campaign protocol then HTTP 204 response will be return
